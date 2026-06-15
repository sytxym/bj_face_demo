/*
 * Copyright (C) 2010 ZXing authors
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

import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aeye.face.lightView.DecodeData;
import com.aeye.face.lightView.RecognizeLightActivity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class PreviewCallbackLight implements Camera.PreviewCallback {

 private static final String TAG = PreviewCallbackLight.class.getSimpleName();

 private final CameraConfigurationManagerLight configManager;
 private final boolean useOneShotPreviewCallback;
 private Handler previewHandler;
 private int previewMessage;
 private DecodeData decodeData;


 PreviewCallbackLight(CameraConfigurationManagerLight configManager, boolean useOneShotPreviewCallback) {
   this.configManager = configManager;
   this.useOneShotPreviewCallback = useOneShotPreviewCallback;
 }

 void setHandler(Handler previewHandler, int previewMessage) {
   this.previewHandler = previewHandler;
   this.previewMessage = previewMessage;
   if(this.decodeData == null){
       this.decodeData = new DecodeData();
   }
 }

 public void onPreviewFrame(byte[] data, Camera camera) {
   Point cameraResolution = configManager.getCameraResolution();
//    Log.e(TAG," onPreview Frame");
   final int width = cameraResolution.x, height = cameraResolution.y;
//    final byte[] data1 = data;
//    new Thread(new Runnable() {
//      @Override
//      public void run() {
//        if(RecognizeActivity.isRecord) {
//          Log.e(TAG," onPreview Frame  isRecord  bitmap ");
////          Bitmap bitmapFull = BitmapUtils.rawByteArray2RGBABitmap2(data1,
////                  width, height, 0);
////          saveBitmap(bitmapFull);
//          putYUVData(data1,data1.length);
//        }
//      }
//    }).start();
     this.decodeData.setData(data);
     this.decodeData.setCurrentColor(RecognizeLightActivity.getColorForSo());
     this.decodeData.setCurrentColorIndex(RecognizeLightActivity.getCurrentIndex());
   if (!useOneShotPreviewCallback) {
     camera.setPreviewCallback(null);
   }
   if (previewHandler != null) {
     Message message = previewHandler.obtainMessage(previewMessage, cameraResolution.x,
         cameraResolution.y, this.decodeData);
     message.sendToTarget();
     previewHandler = null;
   } else {
     Log.d(TAG, "Got preview callback, but no handler for it");
   }
 }



}
