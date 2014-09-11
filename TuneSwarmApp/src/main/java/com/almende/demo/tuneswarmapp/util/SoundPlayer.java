package com.almende.demo.tuneswarmapp.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.almende.demo.tuneswarm.Tone;

public class SoundPlayer {
	private static final Logger	LOG				= Logger.getLogger(SoundPlayer.class
														.getName());
	private final Object		sleepLock		= new Object();
	private final boolean[]		isRunning		= new boolean[1];
	private final int			sr				= 44100;
	private final int			buffsize		= AudioTrack
														.getMinBufferSize(
																sr,
																AudioFormat.CHANNEL_OUT_MONO,
																AudioFormat.ENCODING_PCM_16BIT);
	private double				fr				= 440.f;
	private double				volume			= 0.95;
	private final Context		ctx;

	// create an audiotrack object
	private AudioManager		mAudiomgr;
	private AudioTrack			audioTrack;
	private final Thread		synthesisThread;

	private boolean				playAngklung	= true;
	private InputStream			fileStream		= null;
	private String				filename		= "";

	public SoundPlayer(final Context ctx) {
		this.ctx = ctx;
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
			try {
				final short samples[] = new short[buffsize / 2];
				final double twopi = 2 * Math.PI;
				double ph = 0.0;
				
				while (true) {
					while (isRunning[0]) {
						if (playAngklung) {
							if (!filename.equals("raw/" + getTone() + ".s16")) {
								// Open file and read sample
								filename = "raw/" + getTone() + ".s16";
								try {
									fileStream = new BufferedInputStream(ctx
											.getAssets().open(filename));
								} catch (IOException e) {
									LOG.log(Level.WARNING,
											"couldn't open soundfile", e);
								}
							}
							final byte[] buffer = new byte[buffsize];
							try {
								int count = fileStream.read(buffer);
								for (int i = 0; i < count - 1; i++) {
									samples[i / 2] = (short) (((buffer[i++]) + (buffer[i] << 8)) * volume);
								}
								if (count < buffsize) {
									// Reached end of the file
									fileStream = new BufferedInputStream(ctx
											.getAssets().open(filename));
									int newcount = fileStream.read(buffer, 0,
											buffsize - count);
									for (int i = 0; i < newcount - 1; i++) {
										samples[(count + i) / 2] = (short) (((buffer[i++]) + (buffer[i] << 8)) * volume);
									}
								}
							} catch (IOException e) {
								LOG.log(Level.WARNING, "couldn't play sound", e);
							}
						} else {

							final int amp = (int) (Short.MAX_VALUE * volume);
							for (int i = 0; i < buffsize / 2; i++) {
								final short sample = (short) Math.max(Math.min(
										(amp * Math.sin(ph)), Short.MAX_VALUE),
										Short.MIN_VALUE);
								samples[i] = sample;

								ph += twopi * fr / sr;
							}
						}
						audioTrack.write(samples, 0, buffsize / 2);
					}
					synchronized (sleepLock) {
						try {
							sleepLock.wait();
						} catch (InterruptedException e) {}
					}
				}
			} catch (Exception e1) {
				LOG.log(Level.WARNING, "Exception in synthesis thread", e1);
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
		filename = "";
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

	public String getTone() {
		for (Tone tone : Tone.values()) {
			if (tone.getFrequency() == fr) {
				return tone.name();
			}
		}
		return "??";
	}

	public void setVolume(final double volume) {
		this.volume = volume;
	}

	public void setPlayAngklung(boolean playAngklung) {
		this.playAngklung = playAngklung;
	}
}
