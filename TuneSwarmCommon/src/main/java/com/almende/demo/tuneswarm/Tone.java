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
	C4(261.63),
	D4(293.66),
	E4(329.63),
	F4(349.23),
	G4(392.00),
	A4(440.00),
	B4(493.88),
	C5(523.25),
	D5(587.33);
	
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
