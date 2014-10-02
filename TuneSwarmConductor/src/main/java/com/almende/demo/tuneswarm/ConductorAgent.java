/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.scheduling.SyncScheduler;
import com.almende.eve.scheduling.SyncSchedulerConfig;
import com.almende.eve.transform.rpc.annotation.Access;
import com.almende.eve.transform.rpc.annotation.AccessType;
import com.almende.eve.transform.rpc.annotation.Name;
import com.almende.eve.transform.rpc.annotation.Namespace;
import com.almende.eve.transform.rpc.annotation.Optional;
import com.almende.eve.transform.rpc.annotation.Sender;
import com.almende.eve.transport.http.HttpTransportConfig;
import com.almende.eve.transport.ws.WebsocketTransportConfig;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class ConductorAgent.
 */
@Access(AccessType.PUBLIC)
public class ConductorAgent extends Agent {
	private static final Logger						LOG			= Logger.getLogger(ConductorAgent.class
																		.getName());
	private static final ConductorAgent				SINGLETON	= new ConductorAgent();
	private static final Map<Tone, List<ToneAgent>>	agents		= new HashMap<Tone, List<ToneAgent>>();
	private static final Map<URI, ToneAgent>		agents2		= new HashMap<URI, ToneAgent>();
	private static final URI						monitor		= URI.create("wsclient:monitor");

	/**
	 * Gets the agents.
	 *
	 * @return the agents
	 */
	public ArrayList<ToneAgent> getAgents(){
		return new ArrayList<ToneAgent>(agents2.values());
	}
	
	/**
	 * On agents change.
	 */
	public void onAgentsChange(){
		try {
			caller.call(monitor, "onAgentsChange", JOM.createObjectNode());
		} catch (IOException e) {
			LOG.warning("Monitor not available?");
		}
	}
	
	/**
	 * Do agents change.
	 */
	public void doAgentsChange(){
		onAgentsChange();
		schedule("doAgentsChange",JOM.createObjectNode(),DateTime.now().plus(5000));
	}
	
	/**
	 * On note.
	 *
	 * @param event
	 *            the event
	 * @param senderUrl
	 *            the sender url
	 */
	public void onNote(@Name("toneEvent") ToneEvent event,
			@Sender String senderUrl) {
		final ObjectNode params = JOM.createObjectNode();
		params.put("start", new DateTime(event.getTimestamp()).toString());
		params.put("duration", event.getDuration());
		String tone = event.getTone();
		if (tone != null) {
			params.put("note", tone);
		} else {
			params.put("note", agents2.get(senderUrl).tone.toString());
		}
		try {
			caller.call(monitor, "onNote", params);
		} catch (IOException e) {
			LOG.warning("Monitor not available?");
		}
	}

	/**
	 * The Class ToneAgent.
	 */
	class ToneAgent {
		URI					address;
		Tone				tone;
		String				name;
		boolean	offline;
		public ToneAgent(){}
		public URI getAddress() {
			return address;
		}
		public void setAddress(URI address) {
			this.address = address;
		}
		public Tone getTone() {
			return tone;
		}
		public void setTone(Tone tone) {
			this.tone = tone;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public boolean isOffline() {
			return offline;
		}
		public void setOffline(boolean offline) {
			this.offline = offline;
		}
		
	}

	private Tone getTone() {
		// Add to existing but empty lists first;
		Entry<Tone, List<ToneAgent>> leastMembers = null;
		for (Entry<Tone, List<ToneAgent>> tone : agents.entrySet()) {
			List<ToneAgent> value = tone.getValue();
			if (value.size() == 0) {
				return tone.getKey();
			}
			if (leastMembers == null
					|| value.size() < leastMembers.getValue().size()) {
				leastMembers = tone;
			}
		}
		// Add missing tones:
		for (Tone tone : Tone.values()) {
			if (!agents.containsKey(tone)) {
				agents.put(tone, new ArrayList<ToneAgent>(3));
				return tone;
			}
		}
		// Double to the leastMembers tone:
		return leastMembers.getKey();
	}

	private String getHostAddress() throws SocketException {
		Enumeration<NetworkInterface> e = NetworkInterface
				.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface n = (NetworkInterface) e.nextElement();
			if (!n.isLoopback() && n.isUp() && !n.isVirtual()) {

				Enumeration<InetAddress> ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					if (i instanceof Inet4Address && !i.isLinkLocalAddress()
							&& !i.isMulticastAddress()) {
						return i.getHostAddress().trim();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Inits the Conference Cloud Agent.
	 */
	public void init() {
		String host;
		try {
			host = getHostAddress() + ":8082";
		} catch (SocketException e) {
			LOG.warning("Couldn't determine ipaddress, defaulting to 10.10.1.105");
			host = "10.10.1.105:8082";
		}
		final String id = "conductor";
		final AgentConfig config = new AgentConfig(id);

		final ArrayNode transports = JOM.createArrayNode();
		final WebsocketTransportConfig serverConfig = new WebsocketTransportConfig();
		serverConfig.setId("conductor");
		serverConfig.setServer(true);
		serverConfig.setAddress("ws://" + host + "/ws/" + id);
		serverConfig.setServletLauncher("JettyLauncher");
		final ObjectNode jettyParms = JOM.createObjectNode();
		jettyParms.put("port", 8082);
		serverConfig.set("jetty", jettyParms);
		transports.add(serverConfig);

		final HttpTransportConfig debugConfig = new HttpTransportConfig();
		debugConfig.setId("conductor");
		debugConfig.setDoAuthentication(false);
		debugConfig.setServletUrl("http://" + host + "/www/");
		debugConfig
				.setServletClass("com.almende.eve.transport.http.DebugServlet");
		debugConfig.setServletLauncher("JettyLauncher");
		debugConfig.set("jetty", jettyParms);
		transports.add(debugConfig);

		config.setTransport(transports);

		final SyncSchedulerConfig schedulerConfig = new SyncSchedulerConfig();
		config.setScheduler(schedulerConfig);

		setConfig(config);

		final SyncScheduler scheduler = (SyncScheduler) getScheduler();
		scheduler.setCaller(caller);
		LOG.warning("Started Conductor at:" + host);

		schedule("pingAgents", JOM.createObjectNode(),
				DateTime.now().plus(10000));
		
		schedule("doAgentsChange",JOM.createObjectNode(),DateTime.now().plus(5000));
	}

	/**
	 * Gets the sync scheduler.
	 *
	 * @return the sync scheduler
	 */
	@Namespace("*")
	public SyncScheduler getSyncScheduler() {
		return (SyncScheduler) getScheduler();
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(final String[] args) {
		SINGLETON.init();
	}

	/**
	 * Register agent.
	 *
	 * @param senderUrl
	 *            the sender url
	 * @param name
	 *            the name
	 * @return the double
	 */
	public double registerAgent(final @Sender String senderUrl,
			final @Optional @Name("name") String name) {
		final URI address = URI.create(senderUrl);
		if (agents2.containsKey(address)) {
			final Tone tone = agents2.get(address).tone;
			LOG.warning("Re-registering:" + senderUrl + "(" + name + ")"
					+ " was already tone:" + tone);
			return tone.getFrequency();
		}
		// New agent
		ToneAgent agent = new ToneAgent();
		agent.address = URI.create(senderUrl);
		agent.tone = getTone();
		agent.name = name;
		synchronized (agents) {
			List<ToneAgent> value = agents.get(agent.tone);
			value.add(agent);
			agents.put(agent.tone, value);
		}
		agents2.put(address, agent);
		LOG.warning("Registering:" + senderUrl + "(" + name + ")"
				+ " will be tone:" + agent.tone);
		return agent.tone.getFrequency();
	}

	/**
	 * Send tune to agents.
	 *
	 * @param tune
	 *            the tune
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void sendTuneToAgents(@Name("tune") TuneDescription tune)
			throws IOException {
		final Map<Tone, TuneDescription> tunes = tune.splitToTones();
		final Map<Tone, TuneDescription> plan = new HashMap<Tone, TuneDescription>();
		for (Tone tone : agents.keySet()) {
			final TuneDescription description = tunes.remove(tone);
			if (description != null) {
				LOG.warning("Adding tone:" + tone + " to plan");
				plan.put(tone, description);
			}
		}
		Iterator<TuneDescription> iter = tunes.values().iterator();
		TuneDescription td = null;
		while (iter.hasNext()) {
			boolean progress = false;
			for (Entry<Tone, TuneDescription> item : plan.entrySet()) {
				if (iter.hasNext()) {
					td = iter.next();
				} else {
					break;
				}
				LOG.warning("Merging tones:"
						+ JOM.getInstance().valueToTree(td.getTones())
						+ " to plan:" + JOM.getInstance().valueToTree(item));

				if (item.getValue().tryMerge(td)) {
					td = null;
					progress = true;
				}
			}
			if (!progress) {
				break;
			}
		}
		for (Entry<Tone, TuneDescription> item : plan.entrySet()) {
			for (final ToneAgent agent : agents.get(item.getKey())) {
				if (agent.offline) {
					continue;
				}
				final ObjectNode params = JOM.createObjectNode();
				params.set("description",
						JOM.getInstance().valueToTree(item.getValue()));
				LOG.warning("Sending:" + agent.address + " storeTune:"
						+ params.toString());
				caller.call(agent.address, "storeTune", params);
			}
		}
	}

	/**
	 * Start tune at agents.
	 *
	 * @param id
	 *            the id
	 * @param delay
	 *            the delay
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void startTuneAtAgents(@Name("tuneId") String id,
			@Name("delay") Integer delay) throws IOException {
		final Long startTime = getScheduler().now() + delay;
		for (final List<ToneAgent> list : agents.values()) {
			for (final ToneAgent agent : list) {
				if (agent.offline) {
					continue;
				}
				final ObjectNode params = JOM.createObjectNode();
				params.put("id", id);
				params.put("timestamp", startTime);
				LOG.warning("Sending:" + agent.address + " startTune:"
						+ params.toString());
				caller.call(agent.address, "startTune", params);
			}
		}
	}

	/**
	 * Configure agents.
	 *
	 * @param config
	 *            the config
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void configureAgents(@Name("config") ObjectNode config)
			throws IOException {
		final ObjectNode params = JOM.createObjectNode();
		params.set("config", config);
		for (final List<ToneAgent> list : agents.values()) {
			for (final ToneAgent agent : list) {
				if (agent.offline) {
					continue;
				}
				caller.call(agent.address, "configure", params);
			}
		}
	}

	/**
	 * Ping agents.
	 */
	public void pingAgents() {
		for (final ToneAgent agent : agents2.values()) {
			try {
				caller.call(agent.address, "ping", null,
						new AsyncCallback<Long>() {

							@Override
							public void onSuccess(Long result) {
								// GOOD, nothing to do!
								agent.offline = false;
							}

							@Override
							public void onFailure(Exception exception) {
								// Oops, disable agent in list
								agent.offline = true;
							}
						});
			} catch (IOException e) {
				agent.offline = true;
			}
		}

		schedule("pingAgents", JOM.createObjectNode(),
				DateTime.now().plus(10000));
	}

	/**
	 * Call other agent.
	 *
	 * @param url
	 *            the url
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 * @return the object node
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String callOtherAgent(@Name("url") String url,
			@Name("method") String method, @Name("params") ObjectNode params)
			throws IOException {
		LOG.warning("Call other agent called:" + url + " " + method
				+ " params:" + params.toString());
		return caller.callSync(URI.create(url), method, params);
	}
}
