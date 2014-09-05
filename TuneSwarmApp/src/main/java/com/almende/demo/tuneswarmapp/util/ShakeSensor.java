package com.almende.demo.tuneswarmapp.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeSensor implements SensorEventListener {

	/*
	 * The gForce that is necessary to register as shake.
	 * Must be greater than 1G (one earth gravity unit).
	 * You can install "G-Force", by Blake La Pierre
	 * from the Google Play Store and run it to see how
	 *  many G's it takes to register a shake
	 */
	private static final float SHAKE_THRESHOLD_GRAVITY = 2.0F;
	private static final int SHAKE_SLOP_TIME_MS = 250;

	private OnShakeListener mListener;
	private static long mShakeTimestamp = Long.MAX_VALUE;
	private static boolean shaking = false;

	public void setOnShakeListener(OnShakeListener listener) {
		this.mListener = listener;
	}

	public interface OnShakeListener {
		public void onStartShake();
		public void onStopShake();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// ignore
	}

	
	
	@Override
	public void onSensorChanged(SensorEvent event) {

		if (mListener != null) {
			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];

			float gX = x / SensorManager.GRAVITY_EARTH;
			float gY = y / SensorManager.GRAVITY_EARTH;
			float gZ = z / SensorManager.GRAVITY_EARTH;

			// gForce will be close to 1 when there is no movement.
			double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

			if (gForce > SHAKE_THRESHOLD_GRAVITY) {
				if (!shaking){
					mListener.onStartShake();
					shaking=true;
				}
				mShakeTimestamp = System.currentTimeMillis();
			} else {
				if (shaking && mShakeTimestamp + SHAKE_SLOP_TIME_MS < System.currentTimeMillis()){
					shaking=false;
					mListener.onStopShake();
				}
			}
		}
	}
}
