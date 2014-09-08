/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;

/**
 * The Class Tone.
 */
public 	class ToneDescription {
	private long	start;
	private long	duration;
	private Tone	tone;

	/**
	 * Instantiates a new tone.
	 */
	public ToneDescription() {}

	/**
	 * Gets the start.
	 *
	 * @return the start
	 */
	public long getStart() {
		return start;
	}

	/**
	 * Sets the start.
	 *
	 * @param start
	 *            the new start
	 */
	public void setStart(long start) {
		this.start = start;
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
	public Tone getTone() {
		return tone;
	}

	/**
	 * Sets the tone.
	 *
	 * @param tone
	 *            the new tone
	 */
	public void setTone(Tone tone) {
		this.tone = tone;
	};

}