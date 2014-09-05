package com.almende.demo.tuneswarmapp;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.almende.demo.tuneswarmapp.util.SoundPlayer;
import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.scheduling.SimpleSchedulerConfig;
import com.almende.eve.state.file.FileStateConfig;
import com.almende.eve.transform.rpc.annotation.Name;
import com.almende.eve.transport.ws.WebsocketTransportConfig;
import com.almende.util.callback.AsyncCallback;

import de.greenrobot.event.EventBus;

public class TuneSwarmAgent extends Agent {
	private static final Logger	LOG			= Logger.getLogger(TuneSwarmAgent.class
													.getName());
	private static final String	BASEURL		= "ws://192.168.1.122:8082/ws/";
	private URI					cloud		= null;
	private static Context		ctx			= null;
	private boolean				playOnShake	= true;

	private SoundPlayer			player		= new SoundPlayer();

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
	public void setFrequency(@Name("frequency") final double frequency){
		player.setFrequency(frequency);
	}

	public void init(Context ctx) {
		TuneSwarmAgent.ctx = ctx;
		
		final AudioManager mAudiomgr = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
		mAudiomgr.setStreamVolume(AudioManager.STREAM_ALARM, mAudiomgr.getStreamMaxVolume(AudioManager.STREAM_ALARM),0);
		
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
		SimpleSchedulerConfig schedulerConfig = new SimpleSchedulerConfig();
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
			caller.call(cloud, "registerAgent", null,new AsyncCallback<Double>(){

				@Override
				public void onSuccess(Double result) {
					player.setFrequency(result);
				}

				@Override
				public void onFailure(Exception exception) {
					LOG.log(Level.SEVERE, "Error registering agent",exception);
				}
				
			});
		} catch (IOException e) {
			LOG.log(Level.WARNING,"Couldn't register",e);
		}
	}

	public void onEventAsync(final StateEvent event) {}
}
