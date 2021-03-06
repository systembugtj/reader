package com.ader;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.marvin.widget.TouchGestureControlOverlay;
import com.google.marvin.widget.TouchGestureControlOverlay.Gesture;
import com.google.marvin.widget.TouchGestureControlOverlay.GestureListener;

import java.io.IOException;

public class DaisyPlayer extends Activity implements OnCompletionListener {

	private static final String IS_THE_BOOK_PLAYING = "Playing";
	private static final String TAG = "DaisyPlayer";
	private DaisyBook book;
	private MediaPlayer player;
	private TouchGestureControlOverlay gestureOverlay;
	private FrameLayout frameLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		book = (DaisyBook) getIntent().getSerializableExtra(
				"com.ader.DaisyBook");
		activateGesture();
		player = new MediaPlayer();
		player.setOnCompletionListener(this);
		play();
	}

	//@Override
	//protected void onStop() {
//		super.onStop();
		//player.release();
// 	}

	@Override
	protected void onDestroy() {
		// Let's stop playing the book if the user presses back, etc.
		stop();
		player.release();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.playermenu, menu);
		return(super.onCreateOptionsMenu(menu));
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.player_instructions:
			AlertDialog.Builder builder=new AlertDialog.Builder(this);
			
			builder
				.setTitle(R.string.player_instructions_description)
				.setMessage(R.string.player_instructions)
				.setPositiveButton(R.string.close_instructions, null)
				.show();
			break;
		}
		return true;
	}
	
	public void onCompletion(MediaPlayer mp) {
		Util.logInfo(TAG, "onCompletion called.");
		// stop();
		if (book.nextSection(false)) {
			play();
		}
	}

	public void play() {
		Util.logInfo(TAG, "play");
		player.reset();
		book.openSmil();
		read();
	}

	/**
	 * Start reading the current section of the book
	 */
	private void read() {
		Bookmark bookmark = book.getBookmark();

		if (book.hasAudioSegments()) {
			try {
				Util.logInfo(TAG, "Start playing " + bookmark.getFilename() + " " + bookmark.getPosition());
				player.setDataSource(bookmark.getFilename());
				player.prepare();
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalStateException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			player.seekTo(bookmark.getPosition());
			player.start();
		} else if (book.hasTextSegments()) {
			// TODO(jharty): add TTS to speak the text section
			// Note: we need to decide how to handle things like \n
			// For now, perhaps we can simply display the text in a new view.
			Util.logInfo("We need to read the text from: ", bookmark.getFilename());
		}
	}
    
	public void stop() {
		Util.logInfo(TAG, "stop");
		player.pause();
		book.getBookmark().setPosition(player.getCurrentPosition());
		player.reset();
		book.getBookmark().save(book.getPath() + "auto.bmk");
	}

	public void togglePlay() {
		Util.logInfo(TAG, "togglePlay called.");
		if (player.isPlaying())
			// stop();
			player.pause();
		else
			// play();
			player.start();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean(IS_THE_BOOK_PLAYING, player.isPlaying());
		if (player.isPlaying()) {
			stop();
		}
		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Boolean isPlaying = savedInstanceState.getBoolean(IS_THE_BOOK_PLAYING, true);
		if (!isPlaying) {
			stop();
		}
	}
	
	private void activateGesture() {
		frameLayout = new FrameLayout(this);
		gestureOverlay = new TouchGestureControlOverlay(this, gestureListener);
		frameLayout.addView(gestureOverlay);
		setContentView(frameLayout);
	}

	private GestureListener gestureListener = new GestureListener() {

		public void onGestureStart(Gesture g) {

		}

		public void onGestureChange(Gesture g) {
		}

		public void onGestureFinish(Gesture g) {
			if (g == Gesture.CENTER) {
				togglePlay();
			} else if (g == Gesture.UP) {
				if (book.previousSection()) {
					play();
				}
			} else if (g == Gesture.DOWN) {
				if (book.nextSection(true)) {
					play();
				}
			} else if (g == Gesture.LEFT) {
				int levelSetTo = book.decrementSelectedLevel();
				Util.logInfo(TAG, "Decremented Level to: " + levelSetTo);
			} else if (g == Gesture.RIGHT) {
				int levelSetTo = book.incrementSelectedLevel();
				Util.logInfo(TAG, "Incremented Level to: " + levelSetTo);
			}
		}
	};
}