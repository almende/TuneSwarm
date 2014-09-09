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

	// create an audiotrack object
	private AudioManager	mAudiomgr;
	private AudioTrack		audioTrack;
	private final Thread	synthesisThread;

	public SoundPlayer(final Context ctx) {
		mAudiomgr = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
		mAudiomgr.setStreamVolume(AudioManager.STREAM_RING,
				mAudiomgr.getStreamMaxVolume(AudioManager.STREAM_RING), 0);

		audioTrack = new AudioTrack(AudioManager.STREAM_RING, sr,
				AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				buffsize, AudioTrack.MODE_STREAM);
		isRunning[0] = false;
		synthesisThread = new Synthesis();
		synthesisThread.start();
	}

	public void switchStream(final String stream) {
		stopSound();
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

	}

	final class Synthesis extends Thread {
		@Override
		public void run() {
			setPriority(Thread.MAX_PRIORITY);

			final short samples[] = new short[buffsize];
			final double twopi = 8. * Math.atan(1.);
			double ph = 0.0;

			while (true) {
				final int amp = (int) (65000 * volume);
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
		synchronized (sleepLock) {
			sleepLock.notifyAll();
		}
		audioTrack.pause();
		audioTrack.flush();
	}

	public double getFrequency() {
		return fr;
	}

	public void setVolume(final double volume) {
		this.volume = volume;
	}
}
