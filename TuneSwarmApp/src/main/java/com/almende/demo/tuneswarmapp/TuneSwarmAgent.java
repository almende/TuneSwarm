package com.almende.demo.tuneswarmapp;

import java.net.URI;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.scheduling.SimpleSchedulerConfig;
import com.almende.eve.state.file.FileStateConfig;
import com.almende.eve.transport.ws.WebsocketTransportConfig;

import de.greenrobot.event.EventBus;

public class TuneSwarmAgent extends Agent {
	private static final String	BASEURL	= "ws://10.10.1.105:8082/ws/";
	private URI					cloud	= null;
	private static Context		ctx		= null;

	public void init(Context ctx) {
		TuneSwarmAgent.ctx = ctx;
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

		System.err.println("Reconnecting to server:" + baseUrl + getId());
		final WebsocketTransportConfig clientConfig = new WebsocketTransportConfig();
		clientConfig.setServerUrl(baseUrl + getId());
		clientConfig.setId(getId());
		this.loadTransports(clientConfig, true);

		cloud = URI.create(baseUrl + getId());
	}

	public void onEventAsync(final StateEvent event) {}
}
