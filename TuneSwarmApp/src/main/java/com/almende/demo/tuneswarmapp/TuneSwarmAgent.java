package com.almende.demo.tuneswarmapp;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.almende.demo.tuneswarm.ToneDescription;
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
	private static final Logger	LOG			= Logger.getLogger(TuneSwarmAgent.class
													.getName());
	private static final String	BASEURL		= "ws://192.168.1.122:8082/ws/";
	private URI					cloud		= null;
	private static Context		ctx			= null;
	private boolean				playOnShake	= true;
	private boolean				lightOnly	= true;
	private long				lightPrelay	= 0;
	private SoundPlayer			player		= new SoundPlayer();

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
	}

	public Long ping() {
		return getScheduler().now();
	}

	public void startLight(@Name("color") String color) {
		LOG.severe("Starting light:" + color);
		if (color.equals("Green")) {
			EventBus.getDefault().post(
					new StateEvent(Color.GREEN + "", "changeColor"));
		}

	}

	public void stopLight() {
		LOG.severe("Stop light!");
		EventBus.getDefault().post(
				new StateEvent(Color.BLUE + "", "changeColor"));
	}

	public void startTone() {
		LOG.severe("Starting sound!");
		player.startSound();
	}

	public void stopTone() {
		LOG.severe("Stop sound!");
		player.stopSound();
	}

	public void startShake() {
		if (playOnShake) {
			startTone();
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
		schedule("stopLight", null, (int) (tone.getDuration()+lightPrelay));
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
						new DateTime(duedate + tone.getStart()));
			}
		} else {
			LOG.warning("Tune of the past, or no useful timestamp:" + duedate
					+ " (now:" + getScheduler().now() + ")");
		}
	}

	public void init(Context ctx) {
		TuneSwarmAgent.ctx = ctx;

		final AudioManager mAudiomgr = (AudioManager) ctx
				.getSystemService(Context.AUDIO_SERVICE);
		mAudiomgr.setStreamVolume(AudioManager.STREAM_RING,
				mAudiomgr.getStreamMaxVolume(AudioManager.STREAM_RING), 0);

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

		reconnect();
	}

	public void reconnect() {

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		final String baseUrl = prefs.getString(
				ctx.getString(R.string.wsServer_key), BASEURL);

		System.err.println("Reconnecting to server:" + baseUrl + "conductor");
		final WebsocketTransportConfig clientConfig = new WebsocketTransportConfig();
		clientConfig.setServerUrl(baseUrl + "conductor");
		clientConfig.setId(getId());
		this.loadTransports(clientConfig, true);

		cloud = URI.create(baseUrl + "conductor");

		try {
			caller.call(cloud, "registerAgent", null,
					new AsyncCallback<Double>() {

						@Override
						public void onSuccess(Double result) {
							player.setFrequency(result);
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
	}

	public void onEventAsync(final StateEvent event) {
		if (event.getValue().equals("settingsUpdated")) {
			reconnect();
		}
	}
}
