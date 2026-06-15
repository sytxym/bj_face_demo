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

import com.aeye.face.lightView.RecognizeLightActivity;

import java.util.concurrent.CountDownLatch;


final class DecodeThreadLight extends Thread {

    private RecognizeLightActivity activity;
    private Handler handler;
    private final CountDownLatch handlerInitLatch;

    DecodeThreadLight(RecognizeLightActivity activity) {

        this.activity = activity;
        handlerInitLatch = new CountDownLatch(1);
    }

    Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override/**����DecodeHandler�͸�Handler��Looper*/
    public void run() {
        Looper.prepare();
        handler = new DecodeHandlerLight(activity);
        handlerInitLatch.countDown();
        Looper.loop();
    }

    public void end() {
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        activity = null;
        handler = null;
    }
}
