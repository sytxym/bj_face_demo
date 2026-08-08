/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aeye.face.camera;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aeye.face.view.RecognizeActivity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


final class DecodeThreadLight extends Thread {

    private static final String TAG = "DecodeThreadLight";
    /** 等待解码线程 Looper 就绪的上限；超时返回 null，调用方需判空 */
    private static final long HANDLER_INIT_TIMEOUT_MS = 3000L;
    /** 退出时等待解码线程结束的上限；解码线程若卡死在 native 调用，不能拖死主线程（ANR） */
    private static final long QUIT_JOIN_TIMEOUT_MS = 2000L;

    private RecognizeActivity activity;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    DecodeThreadLight(RecognizeActivity activity) {

        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);
    }

    /** 可能返回 null（初始化超时/异常），调用方必须判空 */
    Handler getHandler() {
        try {
            if (!handlerInitLatch.await(HANDLER_INIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Log.w(TAG, "getHandler: init latch timeout, decode thread not ready");
            }
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandlerLight(activity);
        handlerInitLatch.countDown();
        Looper.loop();
    }

    public void end() {
        try {
            join(QUIT_JOIN_TIMEOUT_MS);
            if (isAlive()) {
                // 解码线程可能卡死在耗时/挂起的 native 调用中，放弃等待并中断，避免主线程 ANR
                Log.w(TAG, "end: decode thread still alive after " + QUIT_JOIN_TIMEOUT_MS + "ms, abandon");
                interrupt();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        activity = null;
        handler = null;
    }
}
