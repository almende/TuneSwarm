/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarmapp;

import android.app.Application;
import android.content.Intent;

/**
 * The Class ConferenceApp.
 */
public class TuneSwarmApp extends Application {
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		startService(new Intent(this, EveService.class));
	}
}
