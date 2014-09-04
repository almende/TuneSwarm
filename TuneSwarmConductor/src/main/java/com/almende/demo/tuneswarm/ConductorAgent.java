/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.transport.ws.WebsocketTransportConfig;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class ConductorAgent.
 */
public class ConductorAgent extends Agent {
	private static final ConductorAgent SINGLETON = new ConductorAgent();
	
	/**
	 * Inits the Conference Cloud Agent.
	 */
	public void init() {
		final String id = "conductor";
		final AgentConfig config = new AgentConfig(id);
		
		final WebsocketTransportConfig serverConfig = new WebsocketTransportConfig();
		serverConfig.setServer(true);
		serverConfig.setAddress("ws://10.10.1.105:8082/ws/" + id);
		
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

	
}
