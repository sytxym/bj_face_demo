//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.aeye.android.uitls;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AudioUtils {
    static MediaPlayer mediaPlayer = null;

    public AudioUtils() {
    }

    public static Map<String, Object> playMusic(AssetFileDescriptor afd) {
        Map<String, Object> map = new HashMap();
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.reset();
        }

        try {
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    AudioUtils.mediaPlayer.stop();
                    AudioUtils.mediaPlayer.release();
                    AudioUtils.mediaPlayer = null;
                }
            });
            map.put("result", true);
        } catch (Exception var3) {
            map.put("result", false);
            map.put("errMsg", var3.getMessage());
        }

        return map;
    }

    public Map<String, Object> playMusic(String fileName) {
        Map<String, Object> map = new HashMap();
        MediaPlayer mediaPlayer = new MediaPlayer();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.reset();
        }

        try {
            String pathName = (new File(Environment.getExternalStorageDirectory(), fileName)).getAbsolutePath();
            mediaPlayer.setDataSource(pathName);
            mediaPlayer.prepare();
            mediaPlayer.start();
            map.put("result", true);
        } catch (Exception var5) {
            map.put("result", false);
            map.put("errMsg", var5.getMessage());
        }

        return map;
    }
}
