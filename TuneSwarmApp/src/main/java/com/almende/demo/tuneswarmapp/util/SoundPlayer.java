package com.almende.demo.tuneswarmapp.util;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class SoundPlayer {
	private final Object	sleepLock	= new Object();
	private final boolean[]	isRunning	= new boolean[1];
	private final int		sr			= 44100;
	private final int		buffsize	= AudioTrack.getMinBufferSize(sr,
												AudioFormat.CHANNEL_OUT_MONO,
												AudioFormat.ENCODING_PCM_16BIT);
	private double			fr			= 440.f;
	private double			volume		= 0.85;
	private int				ramp		= 1;
	private int				rampFactor	= 200;

	// create an audiotrack object
	private AudioManager	mAudiomgr;
	private AudioTrack		audioTrack;
	private final Thread	synthesisThread;

	public SoundPlayer(final Context ctx) {
		mAudiomgr = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
		mAudiomgr.setStreamVolume(AudioManager.STREAM_RING,
				mAudiomgr.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
		mAudiomgr.setSpeakerphoneOn(true);

		audioTrack = new AudioTrack(AudioManager.STREAM_RING, sr,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				buffsize, AudioTrack.MODE_STREAM);
		audioTrack.play();

		isRunning[0] = false;
		synthesisThread = new Synthesis();
		synthesisThread.start();
	}

	public void switchStream(final String stream) {
		stopSound();
		audioTrack.stop();
		if ("ring".equals(stream)) {
			mAudiomgr.setStreamVolume(AudioManager.STREAM_RING,
					mAudiomgr.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
			audioTrack = new AudioTrack(AudioManager.STREAM_RING, sr,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, buffsize,
					AudioTrack.MODE_STREAM);
		}
		if ("music".equals(stream)) {
			mAudiomgr.setStreamVolume(AudioManager.STREAM_MUSIC,
					mAudiomgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
			audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sr,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, buffsize,
					AudioTrack.MODE_STREAM);
		}
		audioTrack.play();

	}

	final class Synthesis extends Thread {
		@Override
		public void run() {
			setPriority(Thread.MAX_PRIORITY);

			final short samples[] = new short[buffsize];
			final double twopi = 8. * Math.atan(1.);
			double ph = 0.0;

			while (true) {
				final int amp = (int) (Short.MAX_VALUE  * volume);
				while (isRunning[0]) {
					for (int i = 0; i < buffsize; i++) {
						samples[i] = (short) Math.max(Math.min((amp * Math.sin(ph)),Short.MAX_VALUE),Short.MIN_VALUE);
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
		audioTrack.flush();
		ramp = (int) (rampFactor * 65000 * volume);
		synchronized (sleepLock) {
			sleepLock.notifyAll();
		}
	}

	public void stopSound() {
		isRunning[0] = false;
	}

	public double getFrequency() {
		return fr;
	}

	public void setVolume(final double volume) {
		this.volume = volume;
	}

	public void setRampFactor(int rampFactor) {
		this.rampFactor = rampFactor;
	}
}
