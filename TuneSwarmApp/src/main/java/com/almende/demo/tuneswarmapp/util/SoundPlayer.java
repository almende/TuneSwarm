package com.almende.demo.tuneswarmapp.util;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundPlayer {
	private static final Object		sleepLock	= new Object();
	private static final boolean[]	isRunning	= new boolean[1];
	private static final int		sr			= 44100;
	private static final int		buffsize	= AudioTrack
														.getMinBufferSize(
																sr,
																AudioFormat.CHANNEL_OUT_MONO,
																AudioFormat.ENCODING_PCM_16BIT);
	private static double			fr			= 440.f;

	// create an audiotrack object
	private static final AudioTrack			audioTrack	= new AudioTrack(
														AudioManager.STREAM_ALARM,
														sr,
														AudioFormat.CHANNEL_OUT_MONO,
														AudioFormat.ENCODING_PCM_16BIT,
														buffsize,
														AudioTrack.MODE_STREAM);
	private final static Thread				t			= new SoundPlayer().new Synthesis();
	static {
		isRunning[0] = false;
		t.start();
	}

	final class Synthesis extends Thread {
		@Override
		public void run() {
			setPriority(Thread.MAX_PRIORITY);

			final short samples[] = new short[buffsize];
			final int amp = 20000;
			final double twopi = 8. * Math.atan(1.);
			double ph = 0.0;

			while (true) {
				while (isRunning[0]) {
					for (int i = 0; i < buffsize; i++) {
						samples[i] = (short) (amp * Math.sin(ph));
						ph += twopi * fr / sr;
					}
					audioTrack.write(samples, 0, buffsize);
				}
				synchronized (sleepLock) {
					try {
						sleepLock.wait();
					} catch (InterruptedException e) {}
				}

			}
		}
	};

	public void setFrequency(double frequency) {
		fr = frequency;
	}

	public void startSound() {
		// start audio
		isRunning[0] = true;
		audioTrack.play();
		synchronized (sleepLock) {
			sleepLock.notifyAll();
		}
	}

	public void stopSound() {
		isRunning[0] = false;
		audioTrack.pause();
		audioTrack.flush();
	}
}
