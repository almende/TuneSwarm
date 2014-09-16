/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;


/**
 * The Class ToneEvent.
 */
public class ToneEvent {
	private long duration;
	private String tone;
	private long timestamp;
	
	/**
	 * Instantiates a new tone event.
	 */
	public ToneEvent(){
	}
	
	/**
	 * Gets the duration.
	 *
	 * @return the duration
	 */
	public long getDuration() {
		return duration;
	}
	
	/**
	 * Sets the duration.
	 *
	 * @param duration
	 *            the new duration
	 */
	public void setDuration(long duration) {
		this.duration = duration;
	}
	
	/**
	 * Gets the tone.
	 *
	 * @return the tone
	 */
	public String getTone() {
		return tone;
	}
	
	/**
	 * Sets the tone.
	 *
	 * @param tone
	 *            the new tone
	 */
	public void setTone(String tone) {
		this.tone = tone;
	}
	
	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
	/**
	 * Sets the timestamp.
	 *
	 * @param timestamp
	 *            the new timestamp
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
