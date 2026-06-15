//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import java.io.IOException;

public class VoicePlayer {
    private static MediaPlayer media = null;
    private static Context mContext;

    public VoicePlayer(Context context) {
        media = new MediaPlayer();
        mContext = context.getApplicationContext();
    }

    public static void playVoice(Context context, int id) {
        if (media == null) {
            media = new MediaPlayer();
        }

        if (media != null) {
            try {
                media.reset();
                AssetFileDescriptor afd = null;
                afd = context.getResources().openRawResourceFd(id);
                if (afd == null) {
                    return;
                }

                media.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                media.setOnPreparedListener(new OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                media.prepareAsync();
            } catch (IllegalArgumentException var3) {
                var3.printStackTrace();
            } catch (IllegalStateException var4) {
                var4.printStackTrace();
            } catch (IOException var5) {
                var5.printStackTrace();
            }
        } else {
            Log.d("ZDX", "VoicePlayer object is null");
        }

    }

    public static void playVoice(int id) {
        if (media == null) {
            media = new MediaPlayer();
        }

        if (media != null) {
            try {
                media.reset();
                AssetFileDescriptor afd = null;
                afd = mContext.getResources().openRawResourceFd(id);
                if (afd == null) {
                    return;
                }

                media.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                media.setOnPreparedListener(new OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                media.prepareAsync();
            } catch (IllegalArgumentException var2) {
                var2.printStackTrace();
            } catch (IllegalStateException var3) {
                var3.printStackTrace();
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        } else {
            Log.d("ZDX", "VoicePlayer object is null");
        }

    }

    public static void destroyPlayer() {
        if (media != null) {
            if (media.isPlaying()) {
                media.stop();
            }

            media.release();
        }

        media = null;
    }
}
