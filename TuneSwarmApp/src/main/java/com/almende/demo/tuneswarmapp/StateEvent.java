/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarmapp;

/**
 * The Class StateEvent.
 */
public class StateEvent {
	private String	value	= "";
	private String	id	= "";
	
	/**
	 * Instantiates a new state event.
	 * 
	 * @param agentId
	 *            the agent id
	 * @param value
	 *            the value
	 */
	public StateEvent(final String id, final String value) {
		setValue(value);
		setId(id);
	}
	
	/**
	 * Instantiates a new state event.
	 * 
	 * @param agentId
	 *            the agent id
	 */
	public StateEvent(final String id) {
		setId(id);
	}
	
	/**
	 * Gets the value.
	 * 
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setValue(final String value) {
		this.value = value;
	}
	
	/**
	 * Gets the agent id.
	 * 
	 * @return the agent id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the agent id.
	 * 
	 * @param agentId
	 *            the new agent id
	 */
	public void setId(final String id) {
		this.id = id;
	}
}
