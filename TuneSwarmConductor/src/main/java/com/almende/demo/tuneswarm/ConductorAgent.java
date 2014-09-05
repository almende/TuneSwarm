/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.transform.rpc.annotation.Access;
import com.almende.eve.transform.rpc.annotation.AccessType;
import com.almende.eve.transform.rpc.annotation.Sender;
import com.almende.eve.transport.http.HttpTransportConfig;
import com.almende.eve.transport.ws.WebsocketTransportConfig;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class ConductorAgent.
 */
@Access(AccessType.PUBLIC)
public class ConductorAgent extends Agent {
	private static final Logger			LOG			= Logger.getLogger(ConductorAgent.class
															.getName());
	private static final ConductorAgent	SINGLETON	= new ConductorAgent();
	private static final Map<Tone,List<ToneAgent>> agents = new HashMap<Tone,List<ToneAgent>>();
	
	private enum Tone {
		C(261.63), D(293.66), E(329.63), F(349.23), G(392.00);
		private double	frequency;

		private Tone(final double frequency) {
			this.setFrequency(frequency);
		}

		public double getFrequency() {
			return frequency;
		}

		public void setFrequency(double frequency) {
			this.frequency = frequency;
		}
	};

	class ToneAgent {
		URI		address;
		Tone	tone;
	}

	private Tone getTone(){
		//Add to existing but empty lists first;
		Entry<Tone,List<ToneAgent>> leastMembers = null;
		for (Entry<Tone,List<ToneAgent>> tone : agents.entrySet()){
			List<ToneAgent> value = tone.getValue();
			if (value.size() == 0){
				return tone.getKey();
			}
			if (leastMembers == null || value.size() < leastMembers.getValue().size()){
				leastMembers = tone;
			}
		}
		//Add missing tones:
		for (Tone tone : Tone.values()){
			if (!agents.containsKey(tone)){
				agents.put(tone, new ArrayList<ToneAgent>(3));
				return tone;
			}
		}
		//Double to the leastMembers tone:
		return leastMembers.getKey();
	}
	
	/**
	 * Inits the Conference Cloud Agent.
	 */
	public void init() {
		final String id = "conductor";
		final AgentConfig config = new AgentConfig(id);

		final WebsocketTransportConfig serverConfig = new WebsocketTransportConfig();
		serverConfig.setServer(true);
		serverConfig.setAddress("ws://192.168.1.122:8082/ws/" + id);

		final HttpTransportConfig debugConfig = new HttpTransportConfig();
		debugConfig.setDoAuthentication(false);
		debugConfig.setServletUrl("http://192.168.1.122:8082/www/" + id);
		debugConfig
				.setServletClass(com.almende.eve.transport.http.DebugServlet.class
						.getName());

		serverConfig.setServletLauncher("JettyLauncher");
		final ObjectNode jettyParms = JOM.createObjectNode();
		jettyParms.put("port", 8082);
		serverConfig.set("jetty", jettyParms);

		config.setTransport(serverConfig);

		setConfig(config);

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
	 * @return the double
	 */
	public double registerAgent(@Sender String senderUrl) {
		
		ToneAgent agent = new ToneAgent();
		agent.address = URI.create(senderUrl);
		agent.tone = getTone();
		synchronized (agents){
			List<ToneAgent> value = agents.get(agent.tone);
			value.add(agent);
			agents.put(agent.tone, value);
		}
		LOG.warning("Registering:" + senderUrl+ " will be tone:"+agent.tone);
		return agent.tone.getFrequency();
	}
}
