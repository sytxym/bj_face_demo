package com.aeye.face.uitls;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;

public class AudioUtils {

	static MediaPlayer mediaPlayer = null;

	public static Map<String, Object> playMusic(AssetFileDescriptor afd) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
		}

		if (mediaPlayer.isPlaying()) {
			mediaPlayer.reset();
		}

		try {
			mediaPlayer.setDataSource(afd.getFileDescriptor(),
					afd.getStartOffset(), afd.getLength());

			mediaPlayer.prepare();
			mediaPlayer.start();

			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer mp) {
					mediaPlayer.stop();
					mediaPlayer.release();
					mediaPlayer = null;
				}
			});
			map.put("result", true);
		} catch (Exception e) {
			map.put("result", false);
			map.put("errMsg", e.getMessage());
		}
		return map;
	}

	public static void playVoice(Context context, int id) {
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
		}

		if (mediaPlayer != null) {
	        try {
	        	mediaPlayer.reset();
				AssetFileDescriptor afd = null;
				afd = context.getResources().openRawResourceFd(id);
	            if (afd == null) return;
	            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
				mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	
					@Override
					public void onPrepared(MediaPlayer mp) {
						mp.start();
					}
					
				});
				mediaPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.d("ZDX", "VoicePlayer object is null");
		}
	}
	
	public static void playVoice(Context context, int id, final MediaPlayer.OnCompletionListener listener) {
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
		}

		if (mediaPlayer != null) {
	        try {
	        	mediaPlayer.reset();
				AssetFileDescriptor afd = null;
				afd = context.getResources().openRawResourceFd(id);
	            if (afd == null) return;
	            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
				mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	
					@Override
					public void onPrepared(MediaPlayer mp) {
						mp.start();
					}
					
				});
				mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					
					@Override
					public void onCompletion(MediaPlayer mp) {
						listener.onCompletion(mp);
						mp.setOnCompletionListener(null);
					}
				});
				mediaPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.d("ZDX", "VoicePlayer object is null");
		}
	}
	
	public static boolean playVoiceIdle(Context context, int id) {
		if (mediaPlayer == null) {
			mediaPlayer = new MediaPlayer();
		}
		if (mediaPlayer != null) {
	        try {
	        	if (mediaPlayer.isPlaying()) {
	        		return false;
	        	}
	        	mediaPlayer.reset();
				AssetFileDescriptor afd = null;
				afd = context.getResources().openRawResourceFd(id);
	            if (afd == null) {
	            	return false;
	            }
	            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
				mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	
					@Override
					public void onPrepared(MediaPlayer mp) {
						mp.start();
					}
					
				});
				mediaPlayer.prepareAsync();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Log.d("ZDX", "VoicePlayer object is null");
			return false;
		}
		return true;
	}

	public static void destroyPlayer() {
		if(mediaPlayer != null) {
			if(mediaPlayer.isPlaying()) {
				mediaPlayer.stop();
			}

			mediaPlayer.release();
		}

		mediaPlayer = null;
	}
}
