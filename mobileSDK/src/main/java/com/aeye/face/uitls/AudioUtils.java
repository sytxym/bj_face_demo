package com.aeye.face.uitls;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

/**
 * 语音提示播放。
 * <p>MediaPlayer 的 reset/isPlaying/stop/release 均为到 mediaserver 的同步 binder 调用，
 * mediaserver 异常时可能长时间不返回（曾致主线程卡死 ANR，见 "attachNewPlayer called in state 4"），
 * 因此全部操作统一投递到专用音频线程执行，任何情况下不阻塞调用方（主线程）。</p>
 */
public class AudioUtils {

	private static final String TAG = "AudioUtils";

	/** 仅在音频线程访问 */
	static MediaPlayer mediaPlayer = null;

	private static volatile Handler sAudioHandler;

	private static Handler audioHandler() {
		if (sAudioHandler == null) {
			synchronized (AudioUtils.class) {
				if (sAudioHandler == null) {
					HandlerThread thread = new HandlerThread("AEFace-Audio");
					thread.setDaemon(true);
					thread.start();
					sAudioHandler = new Handler(thread.getLooper());
				}
			}
		}
		return sAudioHandler;
	}

	private static void runOnAudioThread(final Runnable task) {
		Handler handler = audioHandler();
		Runnable safeTask = () -> {
			try {
				task.run();
			} catch (Throwable t) {
				Log.w(TAG, "audio task error: " + t.getMessage());
				resetPlayerQuietly();
			}
		};
		if (Looper.myLooper() == handler.getLooper()) {
			safeTask.run();
		} else {
			handler.post(safeTask);
		}
	}

	/** 播放器状态异常时释放重建，避免坏状态实例卡住后续调用（仅音频线程调用） */
	private static void resetPlayerQuietly() {
		MediaPlayer player = mediaPlayer;
		mediaPlayer = null;
		if (player != null) {
			try {
				player.release();
			} catch (Throwable ignored) {
			}
		}
	}

	public static Map<String, Object> playMusic(final AssetFileDescriptor afd) {
		Map<String, Object> map = new HashMap<String, Object>();
		runOnAudioThread(() -> {
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
						runOnAudioThread(AudioUtils::resetPlayerQuietly);
					}
				});
			} catch (Exception e) {
				Log.w(TAG, "playMusic: " + e.getMessage());
				resetPlayerQuietly();
			}
		});
		map.put("result", true);
		return map;
	}

	public static void playVoice(Context context, final int id) {
		playVoice(context, id, null);
	}

	public static void playVoice(Context context, final int id, final MediaPlayer.OnCompletionListener listener) {
		final Context app = context.getApplicationContext();
		runOnAudioThread(() -> {
			try {
				if (mediaPlayer == null) {
					mediaPlayer = new MediaPlayer();
				}
				mediaPlayer.reset();
				AssetFileDescriptor afd = app.getResources().openRawResourceFd(id);
				if (afd == null) {
					return;
				}
				mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
				afd.close();
				mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

					@Override
					public void onPrepared(MediaPlayer mp) {
						mp.start();
					}

				});
				if (listener != null) {
					mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

						@Override
						public void onCompletion(MediaPlayer mp) {
							listener.onCompletion(mp);
							mp.setOnCompletionListener(null);
						}
					});
				}
				mediaPlayer.prepareAsync();
			} catch (IllegalArgumentException | IllegalStateException | IOException e) {
				Log.w(TAG, "playVoice: " + e.getMessage());
				resetPlayerQuietly();
			}
		});
	}

	/** 空闲时播放：当前有语音在播则跳过本次（异步执行，始终立即返回） */
	public static boolean playVoiceIdle(Context context, final int id) {
		final Context app = context.getApplicationContext();
		runOnAudioThread(() -> {
			try {
				if (mediaPlayer == null) {
					mediaPlayer = new MediaPlayer();
				}
				if (mediaPlayer.isPlaying()) {
					return;
				}
				mediaPlayer.reset();
				AssetFileDescriptor afd = app.getResources().openRawResourceFd(id);
				if (afd == null) {
					return;
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
			} catch (IllegalArgumentException | IllegalStateException | IOException e) {
				Log.w(TAG, "playVoiceIdle: " + e.getMessage());
				resetPlayerQuietly();
			}
		});
		return true;
	}

	public static void destroyPlayer() {
		runOnAudioThread(() -> {
			MediaPlayer player = mediaPlayer;
			mediaPlayer = null;
			if (player == null) {
				return;
			}
			try {
				if (player.isPlaying()) {
					player.stop();
				}
			} catch (Throwable ignored) {
			}
			try {
				player.release();
			} catch (Throwable ignored) {
			}
		});
	}
}
