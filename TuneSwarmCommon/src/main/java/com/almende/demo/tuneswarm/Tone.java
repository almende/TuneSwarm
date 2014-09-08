/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;

/**
 * The Enum Tone.
 */
@SuppressWarnings("javadoc")
public enum Tone {
	C(261.63), D(293.66), E(329.63), F(349.23), G(392.00);
	private double	frequency;

	private Tone(final double frequency) {
		this.setFrequency(frequency);
	}

	/**
	 * Gets the frequency.
	 *
	 * @return the frequency
	 */
	public double getFrequency() {
		return frequency;
	}

	/**
	 * Sets the frequency.
	 *
	 * @param frequency
	 *            the new frequency
	 */
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
};
