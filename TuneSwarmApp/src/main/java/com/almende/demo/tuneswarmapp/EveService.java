/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.demo.tuneswarmapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import com.almende.demo.tuneswarmapp.util.ShakeSensor;
import com.almende.demo.tuneswarmapp.util.ShakeSensor.OnShakeListener;

/**
 * The Class EveService.
 */
public class EveService extends Service {
	
	/**
	 * The Constant myThread.
	 */
	public static final HandlerThread	myThread	= new HandlerThread(
															EveService.class
																	.getCanonicalName());
	static {
		myThread.setPriority(Thread.MAX_PRIORITY);
	}
	/**
	 * The Constant NEWTASKID.
	 */
	public static final int				NEWTASKID	= 0;

	public static final TuneSwarmAgent myAgent = new TuneSwarmAgent();
	private SensorManager			mSensorManager;
	private Sensor					mAccelerometer;
	private ShakeSensor				shakeSensor;

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(final Intent intent) {
		return null;
	}
	
	/**
	 * Setup base notification.
	 */
	public void setupBaseNotification() {
		final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		final Intent intent = new Intent(this, PlayScreen.class);
		final PendingIntent pIntent = PendingIntent.getActivity(this, 0,
				intent, 0);
		
		// Build notification
		final Notification noti = new Notification.Builder(this)
				.setContentTitle("TuneSwarm App running!")
				.setSmallIcon(R.drawable.ic_launcher).setContentIntent(pIntent)
				.build();
		
		noti.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		
		notificationManager.notify(NEWTASKID, noti);
	}
	

	/**
	 * Inits the eve.
	 * 
	 * @param ctx
	 *            the ctx
	 */
	public void initEve(final Context ctx, final EveService srvs) {
		final Handler myHandler = new Handler(myThread.getLooper());
		myHandler.post(new Runnable() {
			@Override
			public void run() {
				myAgent.init(ctx,srvs);
				setupShakeListener();
				setupBaseNotification();
			}
		});
	}
	
	private void setupShakeListener() {
		// ShakeDetector initialization
				mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
				mAccelerometer = mSensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
				shakeSensor = new ShakeSensor();
				shakeSensor.setOnShakeListener(new OnShakeListener() {

					@Override
					public void onStartShake() {
						myAgent.startShake();
					}
					@Override
					public void onStopShake() {
						myAgent.stopShake();
					}

				});
				mSensorManager.registerListener(shakeSensor, mAccelerometer,
						SensorManager.SENSOR_DELAY_UI);
	}
	
	public void stop(){
		final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(NEWTASKID);
		notificationManager.cancelAll();
	}
	public void start(){
		setupBaseNotification();
	}
	
	/**
	 * Starts the service.
	 * 
	 * @param intent
	 *            the intent
	 * @param flags
	 *            the flags
	 * @param startId
	 *            the start id
	 * @return the int
	 * @see super#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (!myThread.isAlive()) {
			myThread.start();
		}
		initEve(getApplication(),this);
		
		return START_STICKY;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		mSensorManager.unregisterListener(shakeSensor);
		
		super.onDestroy();
	}
	
}
