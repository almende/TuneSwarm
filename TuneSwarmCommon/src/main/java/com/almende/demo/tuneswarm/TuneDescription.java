/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The Class TuneDescription.
 */
public class TuneDescription {
	private static final Logger LOG = Logger.getLogger(TuneDescription.class
			.getName());
	private String		id;
	private long		startTimeStamp=-1;
	private List<ToneDescription>	tones;

	/**
	 * Instantiates a new tune description.
	 */
	public TuneDescription() {}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            the new id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the start time stamp.
	 *
	 * @return the start time stamp
	 */
	public long getStartTimeStamp() {
		return startTimeStamp;
	}

	/**
	 * Sets the start time stamp.
	 *
	 * @param startTimeStamp
	 *            the new start time stamp
	 */
	public void setStartTimeStamp(long startTimeStamp) {
		this.startTimeStamp = startTimeStamp;
	}

	/**
	 * Gets the tones.
	 *
	 * @return the tones
	 */
	public List<ToneDescription> getTones() {
		return tones;
	}

	/**
	 * Sets the tones.
	 *
	 * @param tones
	 *            the new tones
	 */
	public void setTones(List<ToneDescription> tones) {
		this.tones = tones;
	};
	
	/**
	 * Adds the tone.
	 *
	 * @param tone
	 *            the tone
	 */
	public void addTone(ToneDescription tone){
		this.tones.add(tone);
	}
	
	/**
	 * Split to tones.
	 *
	 * @return the map of TuneDescriptions
	 */
	public Map<Tone,TuneDescription> splitToTones(){
		final Map<Tone,TuneDescription> result = new HashMap<Tone,TuneDescription>();
		for (ToneDescription tone: this.tones){
			if (result.containsKey(tone.getTone())){
				result.get(tone.getTone()).addTone(tone);
			} else {
				final TuneDescription td = new TuneDescription();
				td.id = this.id;
				td.startTimeStamp=this.startTimeStamp;
				td.tones = new ArrayList<ToneDescription>();
				td.addTone(tone);
				result.put(tone.getTone(),td);
			}
		}
		return result;
	}
	
	/**
	 * Merge.
	 *
	 * @param other
	 *            the other
	 * @return Returns true if merged successful, false if notes would overlap.
	 */
	public boolean tryMerge(final TuneDescription other){
		//TODO: make this fail if notes overlap too much.
		LOG.warning("Merging: "+this+" with:"+other);
		this.tones.addAll(other.tones);
		return true;
	}

}
