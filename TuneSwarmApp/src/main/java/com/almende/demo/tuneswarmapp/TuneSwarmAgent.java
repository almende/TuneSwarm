package com.almende.demo.tuneswarmapp;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.almende.demo.tuneswarm.ToneDescription;
import com.almende.demo.tuneswarm.ToneEvent;
import com.almende.demo.tuneswarm.TuneDescription;
import com.almende.demo.tuneswarmapp.util.ShakeSensor;
import com.almende.demo.tuneswarmapp.util.SoundPlayer;
import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.scheduling.SyncScheduler;
import com.almende.eve.scheduling.SyncSchedulerConfig;
import com.almende.eve.state.file.FileStateConfig;
import com.almende.eve.transform.rpc.annotation.Access;
import com.almende.eve.transform.rpc.annotation.AccessType;
import com.almende.eve.transform.rpc.annotation.Name;
import com.almende.eve.transport.ws.WebsocketTransportConfig;
import com.almende.util.TypeUtil;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.greenrobot.event.EventBus;

@Access(AccessType.PUBLIC)
public class TuneSwarmAgent extends Agent {
	private static final Logger	LOG					= Logger.getLogger(TuneSwarmAgent.class
															.getName());
	private static final String	BASEURL				= "ws://192.168.1.122:8082/ws/";
	private static boolean      stopped				= false;
	private URI					cloud				= null;
	private static Context		ctx					= null;
	private static EveService	service 			= null;
	private boolean				playOnShake			= false;
	private boolean				lightOnly			= true;
	private boolean				learnLightPrelay	= true;
	private boolean				sendNoteEvents		= true;
	private long				lightPrelay			= 0;
	private long				maxReactionDelay	= 250;
	private SoundPlayer			player;

	private long				lightOnSince		= -1;
	private long				playingSince		= -1;

	public void configure(@Name("config") ObjectNode config) {
		if (config.has("playOnShake")) {
			playOnShake = config.get("playOnShake").asBoolean();
		}
		if (config.has("lightOnly")) {
			lightOnly = config.get("lightOnly").asBoolean();
		}
		if (config.has("lightPrelay")) {
			lightPrelay = config.get("lightPrelay").asLong();
		}
		if (config.has("maxReactionDelay")) {
			maxReactionDelay = config.get("maxReactionDelay").asLong();
		}
		if (config.has("learnLightPrelay")) {
			learnLightPrelay = config.get("learnLightPrelay").asBoolean();
		}
		if (config.has("frequency")) {
			player.setFrequency(config.get("frequency").asDouble());
		}
		if (config.has("shake_slop_ms")) {
			ShakeSensor.SHAKE_SLOP_TIME_MS = config.get("shake_slop_ms")
					.asInt();
		}
		if (config.has("shake_threshold_g")) {
			ShakeSensor.SHAKE_THRESHOLD_GRAVITY = config.get(
					"shake_threshold_g").floatValue();
		}
		if (config.has("stream")) {
			player.switchStream(config.get("stream").asText());
		}
		if (config.has("volume")) {
			player.setVolume(config.get("volume").asDouble());
		}
		if (config.has("playAngklung")) {
			player.setPlayAngklung(config.get("playAngklung").asBoolean());
		}
		if (config.has("sendNoteEvents")) {
			sendNoteEvents = config.get("sendNoteEvents").asBoolean();
		}
		EventBus.getDefault().post(new StateEvent(null, "updateInfo"));
	}

	public Long ping() {
		return getScheduler().now();
	}

	public void startLight(@Name("color") String color) {
		if (stopped){
			return;
		}
		
		LOG.severe("Starting light:" + color);
		if (color.equals("Green")) {
			EventBus.getDefault().post(
					new StateEvent(Color.GREEN + "", "changeColor"));
			lightOnSince = getScheduler().now();
		}

	}

	public void stopLight() {
		LOG.severe("Stop light!");
		EventBus.getDefault().post(
				new StateEvent(Color.BLUE + "", "changeColor"));
	}

	public void startTone() {
		if (stopped){
			return;
		}

		LOG.severe("Starting sound!");
		player.startSound();
		playingSince = getScheduler().now();
	}

	public void stopTone() {
		LOG.severe("Stop sound!");
		player.stopSound();
		if (sendNoteEvents) {
			final ToneEvent event = new ToneEvent();
			event.setDuration( getScheduler().now()-playingSince);
			event.setTimestamp(playingSince);
			event.setTone(player.getTone());
			
			final ObjectNode params = JOM.createObjectNode();
			params.set("toneEvent", JOM.getInstance().valueToTree(event));
			
			schedule("sendTone", params, DateTime.now());
		}
	}

	public void sendTone(@Name("toneEvent") ToneEvent event) {
		final ObjectNode params = JOM.createObjectNode();
		params.set("toneEvent", JOM.getInstance().valueToTree(event));
		try {
			caller.call(cloud, "onNote", params);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't send onNote event", e);
		}
	}

	public void startShake() {
		if (playOnShake) {
			startTone();
		}
		if (learnLightPrelay && lightOnSince > 0) {
			long delay = getScheduler().now() - lightOnSince;
			if (delay < maxReactionDelay) {
				lightPrelay = (lightPrelay + delay) / 2;
				EventBus.getDefault().post(new StateEvent(null, "updateInfo"));
			}
		}
	}

	public void stopShake() {
		if (playOnShake) {
			stopTone();
		}
	}

	public void scheduleTone(@Name("tone") ToneDescription tone) {
		if (player.getFrequency() != tone.getTone().getFrequency()) {
			player.setFrequency(tone.getTone().getFrequency());
		}
		startLight("Green");
		schedule("stopLight", null, (int) tone.getDuration());
		if (!lightOnly) {
			schedule("startTone", null, (int) lightPrelay);
			schedule("stopTone", null, (int) (tone.getDuration() + lightPrelay));
		}
	}

	public void setFrequency(@Name("frequency") final double frequency) {
		player.setFrequency(frequency);
	}

	public String storeTune(@Name("description") TuneDescription description) {
		if (description.getId() == null) {
			description.setId(new UUID().toString());
		}
		getState().put("tune:" + description.getId(),
				JOM.getInstance().valueToTree(description));
		return description.getId();
	}

	public void startTune(@Name("id") String tuneId,
			@Name("timestamp") Long duedate) {
		final TypeUtil<TuneDescription> tu = new TypeUtil<TuneDescription>() {};
		final TuneDescription description = getState()
				.get("tune:" + tuneId, tu);
		if (description == null) {
			LOG.warning("Couldn't find tune:" + tuneId);
			return;
		}
		if (duedate < 0) {
			duedate = description.getStartTimeStamp();
		}
		if (duedate < 0 || duedate - getScheduler().now() > 0) {
			if (duedate < 0) {
				duedate = getScheduler().now() + 10;
			}
			// Schedule the separate notes
			for (ToneDescription tone : description.getTones()) {
				final ObjectNode params = JOM.createObjectNode();
				params.set("tone", JOM.getInstance().valueToTree(tone));
				schedule("scheduleTone", params,
						new DateTime(duedate + tone.getStart() - lightPrelay));
			}
		} else {
			LOG.warning("Tune of the past, or no useful timestamp:" + duedate
					+ " (now:" + getScheduler().now() + ")");
		}
	}

	public void init(Context ctx, EveService srvs) {
		TuneSwarmAgent.ctx = ctx;
		TuneSwarmAgent.service = srvs;

		player = new SoundPlayer(ctx);

		EventBus.getDefault().unregister(this);
		EventBus.getDefault().register(this);

		final TelephonyManager tm = (TelephonyManager) ctx
				.getSystemService(Context.TELEPHONY_SERVICE);
		final AgentConfig config = new AgentConfig();
		config.setId(tm.getDeviceId());

		final FileStateConfig stateConfig = new FileStateConfig();
		stateConfig.setJson(true);
		stateConfig.setPath(ctx.getFilesDir().getAbsolutePath()
				+ "/agentStates/");
		stateConfig.setId("TuneSwarmAgent");
		config.setState(stateConfig);

		// TODO: make this a syncScheduler
		SyncSchedulerConfig schedulerConfig = new SyncSchedulerConfig();
		config.setScheduler(schedulerConfig);

		setConfig(config, true);
		EventBus.getDefault().post(new StateEvent(null, "updateInfo"));

		schedule("reconnect", JOM.createObjectNode(), DateTime.now());
	}

	private String getIpAddr() {
		WifiManager wifiManager = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		if (ip == 0) {
			return null;
		}
		String ipString = String.format(Locale.US, "%d.%d.%d.%d", (ip & 0xff),
				(ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));

		return ipString;
	}
	
	private String getAccountName(){
		   AccountManager manager = AccountManager.get(ctx);
		    Account[] accounts = manager.getAccountsByType("com.google");
		    List<String> possibleEmails = new LinkedList<String>();

		    for (Account account : accounts) {
		        possibleEmails.add(account.name);
		    }

		    if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
		        String email = possibleEmails.get(0);
		        String[] parts = email.split("@");
		        if (parts.length > 0 && parts[0] != null)
		            return parts[0];
		        else
		            return "<unknown>";
		    } else
		        return "<unknown>";
		}

	public void reconnect() {

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String baseUrl = prefs.getString(ctx.getString(R.string.wsServer_key),
				BASEURL);

		String ip = getIpAddr();
		if (ip != null) {
			if (ip.startsWith("10.10.1.")) {
				LOG.warning("Defaulting to office base url!");
				baseUrl = "ws://10.10.1.105:8082/ws/";
			}
			if (ip.startsWith("192.168.1.")) {
				LOG.warning("Defaulting to home base url!");
				baseUrl = "ws://192.168.1.122:8082/ws/";
			}
			if (ip.startsWith("192.168.43.")) {
				LOG.warning("Defaulting to mobile base url!");
				baseUrl = "ws://192.168.43.59:8082/ws/";
			}
			if (ip.startsWith("192.168.150.")) {
				LOG.warning("Defaulting to laptop base url!");
				baseUrl = "ws://192.168.150.1:8082/ws/";
			}
		}

		System.err.println("Reconnecting to server:" + baseUrl + "conductor");
		final WebsocketTransportConfig clientConfig = new WebsocketTransportConfig();
		clientConfig.setServerUrl(baseUrl + "conductor");
		clientConfig.setId(getId());
		this.loadTransports(clientConfig, true);

		cloud = URI.create(baseUrl + "conductor");

		ObjectNode params = JOM.createObjectNode();
		params.put("name", getAccountName());
		try {
			caller.call(cloud, "registerAgent", params,
					new AsyncCallback<Double>() {

						@Override
						public void onSuccess(Double result) {
							player.setFrequency(result);
							EventBus.getDefault().post(
									new StateEvent(null, "updateInfo"));

						}

						@Override
						public void onFailure(Exception exception) {
							LOG.log(Level.SEVERE, "Error registering agent",
									exception);
						}

					});
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't register", e);
		}

		SyncScheduler scheduler = (SyncScheduler) getScheduler();
		scheduler.setCaller(caller);
		try {
			scheduler.addPeer(cloud);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Couldn't add sync peer", e);
		}
		EventBus.getDefault().post(new StateEvent(null, "updateInfo"));

	}

	public void onEventAsync(final StateEvent event) {
		if (event.getValue().equals("settingsUpdated")) {
			reconnect();
		} else if (event.getValue().equals("stopApp") && service != null){
			stopped=true;
			player.stopSound();
			service.stop();
		} else if (event.getValue().equals("startApp") && service != null){
			stopped=false;
			service.start();
		}
	}

	public String getText() {
		String text = "";
		text += "Label:" + getAccountName() + "\n";
		text += "Tone:" + player.getTone() + "\n";
		text += "Light delay:" + lightPrelay + " ms\n";
		text += "SyncOffset:"
				+ (getScheduler().now() - System.currentTimeMillis()) + " ms\n";
		return text;
	}
}
