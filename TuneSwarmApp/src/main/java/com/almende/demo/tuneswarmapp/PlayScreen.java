package com.almende.demo.tuneswarmapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.almende.demo.tuneswarmapp.util.SystemUiHider;

import de.greenrobot.event.EventBus;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PlayScreen extends Activity {

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean	AUTO_HIDE				= true;

	private static final boolean[]	stopped					= new boolean[1];

	static {
		stopped[0] = false;
	}
	private long mLastClickTime = 0;
	
	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int		AUTO_HIDE_DELAY_MILLIS	= 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean	TOGGLE_ON_CLICK			= true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int		HIDER_FLAGS				= SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider			mSystemUiHider;

	public void onEventMainThread(final StateEvent event) {
		if (event.getValue().equals("changeColor")) {
			final FrameLayout contentView = (FrameLayout) findViewById(R.id.fullscreen_container);
			if (contentView != null) {
				contentView.setBackgroundColor(Integer.valueOf(event.getId()));
			} else {
				Log.w("ColorChanger", "Couldn't find fullscreen_container");
			}
		}
		if (event.getValue().equals("updateInfo")) {
			// TODO: get agent, update info on screen
			final TextView text = (TextView) findViewById(R.id.fullscreen_content);
			text.setText(EveService.myAgent.getText());
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_play_screen);

		EventBus.getDefault().unregister(this);
		EventBus.getDefault().register(this);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int	mControlsHeight;
					int	mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final TextView text = (TextView) findViewById(R.id.fullscreen_content);
				text.setText(EveService.myAgent.getText());
				
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		Button uninstallBtn = (Button) findViewById(R.id.unInstallButton);
		uninstallBtn.setOnTouchListener(mDelayHideTouchListener);
		uninstallBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Uri packageUri = Uri
						.parse("package:com.almende.demo.tuneswarmapp");
				Intent uninstallIntent = new Intent(
						Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
				startActivity(uninstallIntent);
				uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
				startActivity(uninstallIntent);
			}
		});

		final Button stopBtn = (Button) findViewById(R.id.stopButton);
		stopBtn.setOnTouchListener(mDelayHideTouchListener);

		if (stopped[0]) {
			stopBtn.setText(R.string.startButton);
		} else {
			stopBtn.setText(R.string.stopButton);
		}
		stopBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		        if (SystemClock.elapsedRealtime() - mLastClickTime < 500){
		            return;
		        }
		        mLastClickTime = SystemClock.elapsedRealtime();
				synchronized (stopped) {
					if (stopped[0]) {
						stopped[0] = false;
						stopBtn.setText(R.string.stopButton);
						EventBus.getDefault().post(
								new StateEvent(null, "startApp"));
					} else {
						stopped[0] = true;
						stopBtn.setText(R.string.startButton);
						EventBus.getDefault().post(
								new StateEvent(null, "stopApp"));
					}
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tuneswarm_app, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.action_settings:
				try {
					startActivity(new Intent(this, SettingsActivity.class));
				} catch (Throwable t) {
					t.printStackTrace();
				}
				return true;
			case android.R.id.home:
				getFragmentManager().popBackStack();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(500);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener	mDelayHideTouchListener	= new View.OnTouchListener() {
														@Override
														public boolean onTouch(
																View view,
																MotionEvent motionEvent) {
															if (AUTO_HIDE) {
																delayedHide(AUTO_HIDE_DELAY_MILLIS);
															}
															return view
																	.performClick();
														}
													};

	Handler					mHideHandler			= new Handler();
	Runnable				mHideRunnable			= new Runnable() {
														@Override
														public void run() {
															mSystemUiHider
																	.hide();
														}
													};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
