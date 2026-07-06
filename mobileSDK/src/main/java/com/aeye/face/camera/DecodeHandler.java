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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

//import com.aeye.aeyelib.AEyeAlive;
//import com.aeye.aeyelib.RGBUtil;
import com.aeye.android.config.ConfigData;
import com.aeye.android.data.AEFaceInfo;
import com.aeye.android.libutils.ComplexUtil;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFacePack;
import com.aeye.face.camera.PreviewFrameCache;
import com.aeye.face.config.FaceSdkHostParamBuilder;
import com.aeye.face.config.IDConstants;
import com.aeye.face.uitls.MBitmapUtil;
import com.aeye.face.view.RecognizeActivity;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceDetect;
import com.aeye.sdk.AEFaceQuality;

import java.io.File;

/**
 * 解码、停止<BR/>
 */
public class DecodeHandler extends Handler {
    private String TAG = DecodeHandler.class.getSimpleName();
    public static final int MSG_RESET = 100;
    public static final int MSG_DECODE = 111;
    public static final int MSG_QUIT = 122;
    public static final int MSG_SIDE = 133;

    public static final int FACE_MAX = 5;
    /**
     * 记录 摄像头的方向
     **/
    private SharedPreferences sp;
    /**
     *
     **/
    private final RecognizeActivity activity;
    /**
     * 检测 次数
     **/
    private int loseCount = 0;
    private int faceCount = 0;
    private int aliveCount = 1;
    private int captureNum = 0;
    private boolean isFisrt = true;
    private boolean requestSide = false;
    //	private static boolean delayAlive = false;
//	private int[] location = new int[36 * FACE_MAX];
    private boolean changeOri = true;
    private boolean shakeStatus = false;

    private boolean cfgShowRect = false;
    private int CfgLoseFace = 2;
    private int CfgPicNum = 1;
    private int CfgCapFace = 2;
    private int CfgAliveLevel = FaceSdkHostParamBuilder.DEFAULT_ALIVE_LEVEL;
    private int CfgMotionPicNum = 1;

    AEFaceInfo faceInfo = new AEFaceInfo();

    private int skipFrame = 5;

    //	int[] envCount = new int[]{0,0,0,0,0};
    private int envPreFrm;
    private int envCount;
    int ENV_COUNT_MAX = 3;
    private int envLast = AEFaceQuality.QUALITY_OK;

    /**
     * 构造
     */
    public DecodeHandler(RecognizeActivity activity) {
        this.activity = activity;
        sp = activity.getSharedPreferences(ConfigData.SP_CAMERA_INFO,
                Context.MODE_PRIVATE);
        faceInfo.cameraId = sp.getInt(ConfigData.SP_CAMERA_DIRECTION, 0);
        faceInfo.direction = activity.getOrientation();
        changeOri = (faceInfo.direction + 360) % 180 == 0 ? false : true;
        resetData();
    }

    private void resetData() {
        loseCount = 0;
        faceCount = 0;
        aliveCount = 1;
        captureNum = 0;
//		delayAlive = false;
        requestSide = false;
        faceInfo.isAlive = false;
        shakeStatus = false;
        isFisrt = true;

        envLast = AEFaceQuality.QUALITY_OK;
        envCount = 0;
        skipFrame = 5;

        CfgLoseFace = AEFacePack.getInstance().getLoseFaceNum();
        CfgPicNum = AEFacePack.getInstance().getPictureNumber();
        CfgCapFace = AEFacePack.getInstance().getCaptureFace();
        CfgAliveLevel = AEFacePack.getInstance().getAliveLevel();
        cfgShowRect = AEFacePack.getInstance().isShowFaceRect();
        CfgMotionPicNum = AEFacePack.getInstance().getMotionPicNum();

//		captured = 0;
//		captured1 = 0;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case IDConstants.id_decode:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                break;

            case MSG_RESET:
                resetData();
                break;

            case IDConstants.id_request_side:
                requestSide = true;
                break;

            case IDConstants.id_shake:
                shakeStatus = (Boolean) message.obj;
                Log.d("ZDX", "SHAKE : " + shakeStatus);
                break;

            case IDConstants.id_quit:
                Looper.myLooper().quit();
                break;
        }
    }

    //	static int captured = 0;
//	static int captured1 = 0;
    private void decode(byte[] data, int width, int height) {
//		Log.d("TIME", "decode  begin  "+System.currentTimeMillis());
        if (faceInfo.isAlive || !activity.getDecodeStatus()) {
            return;
        }
        faceInfo.imgByteA = data.clone();
        faceInfo.imgWidth = width;
        faceInfo.imgHeight = height;
        PreviewFrameCache.update(data, width, height, faceInfo.direction);
        // TODO: 2022/8/19
//        if (changeOri) {
//            faceInfo.width = (width > 960 || height > 960) ? (height + 1) / 2
//                    : height;
//            faceInfo.height = (width > 960 || height > 960) ? (width + 1) / 2
//                    : width;
//        } else {
//            faceInfo.width = (width > 960 || height > 960) ? (width + 1) / 2
//                    : width;
//            faceInfo.height = (width > 960 || height > 960) ? (height + 1) / 2
//                    : height;
//        }
        if (changeOri) {
            faceInfo.width = height;
            faceInfo.height = width;
        } else {
            faceInfo.width = width;
            faceInfo.height = height;
        }
//        faceInfo.grayByteA = BitmapUtils.rawByteArray2Y(faceInfo.imgByteA, width, height,
//                faceInfo.direction);
//
//        faceInfo.faceArr = cvtSpace(faceInfo.imgByteA, faceInfo.imgWidth,
//                faceInfo.imgHeight, faceInfo.direction);
        // TODO: 2022/8/19
        faceInfo.grayByteA = MBitmapUtil.rawByteArray2Y(faceInfo.imgByteA, width, height,
                faceInfo.direction);

        faceInfo.faceArr = MBitmapUtil.cvtSpace(faceInfo.imgByteA, faceInfo.imgWidth,
                faceInfo.imgHeight, faceInfo.direction);
        if (isFisrt) {
            if (--skipFrame <= 0) {
//				activity.showHint("aeye_camera_notice", Color.WHITE);
                if (!AEFacePack.getInstance().isAliveOff()) {
                    int motion = AEFacePack.getInstance().getAliveMotions();
                    AEFaceAlive.getInstance().AEYE_Alive_setAliveParamVIS(motion, CfgAliveLevel);
                }
                isFisrt = false;
            }
        } else {
            long time = System.currentTimeMillis();
            Rect[] rect = AEFaceDetect.getInstance().AEYE_FaceDetect(faceInfo.faceArr,
                    faceInfo.width, faceInfo.height);
            Log.i("TIME", "Detect cost " + (System.currentTimeMillis() - time));
            if (rect != null) {
                faceInfo.faceNumber = rect.length;
                if(faceInfo.faceNumber==1) {

                    int faceWidth = rect[0].width();
//                    boolean faceFar = (faceWidth < 340);
                    boolean faceFar = false;
                    faceInfo.imgRect = rect[0];
                    if (cfgShowRect) {
                        activity.getHandler().sendMessage(activity.getHandler().
                                obtainMessage(IDConstants.id_draw_faceRect, faceInfo.width, faceInfo.height, rect[0]));
                    }

                    int quality = AEFaceQuality.QUALITY_OK;
                    boolean qualityOff = AEFacePack.getInstance().isQualityOff();
//                    qualityOff = false;
                    if (!qualityOff) {
                        time = System.currentTimeMillis();
                        quality = AEFaceQuality.getInstance().AEYE_FaceQuality(
                                faceInfo.grayByteA, faceInfo.width, faceInfo.height, rect[0]);
                        Log.e(TAG, "  quality=" + quality);
                        if (quality == AEFaceQuality.QUALITY_OK
                                || quality == AEFaceQuality.QUALITY_FAR
                                || quality == AEFaceQuality.QUALITY_NEAR) {

                            quality = facePosition(rect[0], faceInfo.width, faceInfo.height);
                            if (quality == AEFaceQuality.QUALITY_OK || quality == RecognizeActivity.QUALITY_SIDE) {
                                if (faceFar && quality == AEFaceQuality.QUALITY_OK) {
                                    quality = AEFaceQuality.QUALITY_FAR;
                                }
                            } else {
                                Log.e(TAG, "facePosition quality=" + quality);
                            }
                        }
                        Log.i("TIME", "Quality cost " + (System.currentTimeMillis() - time)+" quality result : "+quality);
                    }
                    if (!activity.getHandler().isInPoseChange() /*&& envLast != quality*/) {
                        activity.showQualityHint(quality, true);
                    }


                    if (quality == AEFaceQuality.QUALITY_OK || quality == RecognizeActivity.QUALITY_SIDE) {
                        loseCount = 0;
                        faceCount++;
                        activity.showFaceOut(true);
                        if (!AEFacePack.getInstance().isAliveOff()) { // 活体优先
                            {
                                if (!activity.getHandler().isInPoseChange() && !shakeStatus) {
                                    if (aliveCount < CfgMotionPicNum) {
                                        activity.getHandler().addAliveImage(faceInfo);
                                        aliveCount++;
                                    } else {
                                        if (AEFacePack.getInstance().isAliveMask()) {
                                            Bitmap vis1 = MBitmapUtil.cutFaceImageWithAlive(faceInfo);
                                            Bitmap vis = MBitmapUtil.cutFaceImage(faceInfo);
//                                    Bitmap vis = BitmapFactory.decodeFile("/sdcard/aeye/test_3.png");
                                            boolean hasFaceMask = checkFace(vis, faceInfo.imgRect, vis1);
                                            Log.e(TAG, "是否有遮挡:" + (hasFaceMask ? "是" : "否"));
                                            Message message = Message.obtain(activity.getHandler(),
                                                    IDConstants.id_mask, hasFaceMask);
                                            message.sendToTarget();
                                            if (!hasFaceMask) {
                                                if (AEFacePack.getInstance().isAlivePose()) {
                                                    long ret = AEFaceAlive.getInstance().AEYE_Alive_DetectVIS(
                                                            faceInfo.faceArr,
                                                            faceInfo.width,
                                                            faceInfo.height,
                                                            faceInfo.imgRect,
                                                            faceInfo);
                                                    Log.i("TIME", "Alive cost " + (System.currentTimeMillis() - time));
                                                    Log.e(TAG, "AEYE_AliveDetect : " + ret);
                                                } else {
                                                    Log.e(TAG, "other alive check ");
                                                    faceInfo.cutFaceImage();
                                                    Message message2 = Message.obtain(activity.getHandler(),
                                                            IDConstants.id_single_alive, faceInfo);
                                                    message2.sendToTarget();
                                                }

                                            }
                                        } else {
                                            if (AEFacePack.getInstance().isAlivePose()) {
                                                long ret = AEFaceAlive.getInstance().AEYE_Alive_DetectVIS(
                                                        faceInfo.faceArr,
                                                        faceInfo.width,
                                                        faceInfo.height,
                                                        faceInfo.imgRect,
                                                        faceInfo);
//                                                Log.i("TIME", "Alive cost " + (System.currentTimeMillis() - time));
                                                Log.d(TAG, "AEYE_AliveDetect : " + ret);
                                            } else {
                                                Log.e(TAG, "other alive check ");
                                                faceInfo.cutFaceImage();
                                                Message message2 = Message.obtain(activity.getHandler(),
                                                        IDConstants.id_single_alive, faceInfo);
                                                message2.sendToTarget();
                                            }
                                        }

                                    }
                                } else {
                                    aliveCount = 1;
                                    Log.e(TAG, "aliveCount rest");
                                }
                            }
                        } else if (AEFacePack.getInstance().isModelAllSide()) { //多角度
//						Log.d("ZDX", "faceCount: " + faceCount + "  requestSide: " + requestSide );
                            if (requestSide) {
                                if (faceCount >= CfgCapFace) {
                                    faceInfo.cutFaceImage();
                                    Message message = Message.obtain(activity.getHandler(),
                                            IDConstants.id_success_side, faceInfo);
                                    message.sendToTarget();

                                    captureNum++;
                                    faceCount = 0;
                                    requestSide = false;
                                }
                            }
                        } else {  //普通采集
                            if (captureNum < CfgPicNum) {
                                if (faceCount >= CfgCapFace) {
                                    if (captureNum == CfgPicNum - 1) {
                                        faceInfo.isAlive = true;
                                    } else {
                                        faceInfo.isAlive = false;
                                    }
                                    Log.e(TAG, "aliveCount add pic ");
                                    faceInfo.cutFaceImage();
                                    Message message = Message.obtain(activity.getHandler(),
                                            IDConstants.id_decode_succeeded, faceInfo);
                                    message.sendToTarget();

                                    captureNum++;
                                    faceCount = 0;
                                }
                            }
                        }
                    } else {
                        if (faceFar && quality == AEFaceQuality.QUALITY_OK) {
                            quality = AEFaceQuality.QUALITY_FAR;
                        }
                        Log.w(TAG, "AEYE_QualityDetect : " + quality);
                        if (quality == AEFaceQuality.QUALITY_FAR) {
                            elseProcess(faceInfo, true, AEFaceQuality.QUALITY_FAR);
                        } else
                            elseProcess(faceInfo, true, -1);
                    }
                }else {
                    //有多个人脸
                    elseProcess(faceInfo, true, -2);
                }
            } else { // 如果没找到 人脸 的 具体位置 就继续寻找
                elseProcess(faceInfo, false,-1);
            }
        }
        checkAgain();
//		Log.d("TIME", "decode  end  "+System.currentTimeMillis());
    }

    volatile int count;

    private boolean checkFace(Bitmap bitmap, Rect rect, Bitmap src) {
        Log.e(TAG, "checkFace count=" + count);
//        count++;
//        AEyeAlive.LiveResult liveResult = new AEyeAlive.LiveResult();
////        int ret = AEyeAlive.getInstance().Quality(bitmap, rect, liveResult);
//        byte[] rgb = RGBUtil.bitmap2BGR(bitmap);
//        if (rgb == null || rgb.length == 0) {
//            Log.e(TAG, "change error");
//        }
//        Log.e(TAG, "rgb size=" + rgb.length);
//        byte[] temp = rgb.clone();
//        int ret = AEyeAlive.getInstance().Quality(temp, bitmap.getWidth(), bitmap.getHeight(), liveResult);
//        Log.e(TAG, "ret==-1=" + (ret == -1) + ",liveResult.score" + liveResult.score);
////        String name = "sdk_out_" + count + "_" + ret + "_" + liveResult.score;
////        String name2 = "sdk_out_" + count + "_rgb_" + "_" + ret + "_" + liveResult.score;
////        if (!(ret == -1)) {
////            BitmapUtils.saveBitmap(bitmap, name);
////            BitmapUtils.saveBitmap(src, name2);
////        }
        return false;
    }

    private void elseProcess(AEFaceInfo faceInfo, boolean haveFace,int quality) {
        faceCount = 0;
        faceInfo.imgRect = null;
        if (cfgShowRect) {
            activity.getHandler().sendMessage(activity.getHandler().
                    obtainMessage(IDConstants.id_draw_faceRect, faceInfo.width, faceInfo.height, null));
        }
        if (haveFace) {
            if(quality == AEFaceQuality.QUALITY_FAR){
                activity.showHint("face_far", RecognizeActivity.HINT_COLOR_ERROR);
            }else if(quality ==-2){
                activity.showManyPersonMessageBox();
            }else
            activity.showHint("aeye_quality_out", RecognizeActivity.HINT_COLOR_ERROR);
            activity.showFaceOut(false);
        } else if ((loseCount++) == CfgLoseFace && !AEFacePack.getInstance().isAliveOff()
                && activity.getDecodeStatus()) {
            activity.showHint("aeye_quality_out", RecognizeActivity.HINT_COLOR_ERROR);
            activity.showNoFace();
        }
    }

    private int[] cvtSpace(byte[] data, int width, int height,
                           int direction) {
        int imageSize = -1;

        int frameSize = width * height;
        int[] rgba = null;

        if (width > 960 || height > 960) {
            rgba = new int[frameSize / 4];
            imageSize = 1;
        } else {
            rgba = new int[frameSize];
            imageSize = 0;
        }

        ComplexUtil.getInstance().YUVToBitmapR(data, rgba, width, height,
                imageSize, direction);
        return rgba;
    }

    boolean envDelay(int quality) {
        if (quality != envPreFrm) {
            envCount = 0;
            envPreFrm = quality;
        }

        if (++envCount >= ENV_COUNT_MAX) {
            envCount = ENV_COUNT_MAX;
            return true;
        }
        return false;
    }

    /**
     * 人脸位置判断
     *
     * @param rect
     * @param width
     * @param height
     * @return
     */
    private int facePosition(Rect rect, int width, int height) {

        int limitXrigntdiff = 32;
        int xChushu = 20;
        int nearFaceWidth = 250;
        if(AEFacePack.getInstance().ismRoiCenterSwitch()){
            //增加人脸区域为中间位置检测
            limitXrigntdiff = 24;
            xChushu = 10;
        }
        if(AEFacePack.getInstance().isLand()){
              limitXrigntdiff = 32;
              xChushu = 20;
            nearFaceWidth=160;
        }
        Log.e(TAG, "facePosition  limitXrigntdiff=" + limitXrigntdiff
                + ",xChushu=" + xChushu
                + ",left=" + rect.left
                + ",right=" + rect.right
                + ",rectW=" + rect.width()
                + ",rectH=" + rect.height()
                + ",width=" + width
                + ",height=" + height);
        ENV_COUNT_MAX = 3;
        int result = envLast;
        Rect face = new Rect(rect);
        float minX = width / xChushu;
        float maxX = width - minX;
        float minY = height / 8;
        float maxY = height - minY;

        int min = (height > width) ? width : height;
        float rate = (float) min / (float) rect.width();


		/*if (rate > 2.5) {
			if (envDelay(AEFaceQuality.QUALITY_FAR)) {
				result[0] = AEFaceQuality.QUALITY_FAR;
				envLast = AEFaceQuality.QUALITY_FAR;
			} else {
				result[0] = envLast;
			}
			return true;
		} else if (rate < 1.6) {
			if (envDelay(AEFaceQuality.QUALITY_NEAR)) {
				result = AEFaceQuality.QUALITY_NEAR;
				envLast = AEFaceQuality.QUALITY_NEAR;
			}
		} else */
        float topLimit = face.top - minY;
        float leftLimit = face.left - minX;
        float rightLimit = face.right - maxX;
        float bottomLimit = face.bottom - maxY;
        Log.e(TAG, "facePosition0 topLimit=" + topLimit
                + ",leftLimit=" + leftLimit
                + ",rightLimit=" + rightLimit
                + ",bottomLimit=" + bottomLimit
                + ",rate=" + rate
        );

        int limitXDiff = -limitXrigntdiff;
        if (leftLimit < limitXDiff || rightLimit >= limitXrigntdiff ||
                topLimit < limitXDiff || bottomLimit >= limitXrigntdiff || rate < 1.1) {
            Log.e(TAG, "facePosition1 minX=" + minX
                    + ",maxX=" + maxX
                    + ",minY=" + minY
                    + ",maxY=" + maxY
                    + ",rate=" + rate
            );
            Log.e(TAG, "facePosition  face out" + face
                    + "," + (leftLimit < -20 ? "minX=" + minX : "")
                    + "," + (rightLimit >= 20 ? "maxX=" + maxX : "")
                    + "," + (topLimit < -20 ? "minY=" + minY : "")
                    + "," + (bottomLimit >= 20 ? "maxY=" + maxY : "")
                    + "," + (rate < 1.1 ? "rate=" + rate : "")
            );
            if (activity.getHandler().getCurPos() == AEFaceAlive.POSE_MOUTH_OPEN ||
                    face.top < (minY / 2) || face.bottom > maxY + (minY / 2) || rate < 1.1) {
                ENV_COUNT_MAX = 1;
                Log.e(TAG, "mouth ");
            } else {
                ENV_COUNT_MAX = 5;
            }
            if (envDelay(RecognizeActivity.QUALITY_OUT)) {
                result = RecognizeActivity.QUALITY_OUT;
                envLast = RecognizeActivity.QUALITY_OUT;
            }
        } else {
            if (activity.getHandler().isSideFaceing()) {
                if (envDelay(RecognizeActivity.QUALITY_SIDE)) {
                    result = RecognizeActivity.QUALITY_SIDE;
                    envLast = RecognizeActivity.QUALITY_SIDE;
                }
            } else {
                if (envDelay(AEFaceQuality.QUALITY_OK)) {
                    result = AEFaceQuality.QUALITY_OK;
                    envLast = AEFaceQuality.QUALITY_OK;
                }
            }
        }
        if(result == AEFaceQuality.QUALITY_OK){
            if(rect.width()< nearFaceWidth){
                result = AEFaceQuality.QUALITY_FAR;
                envLast = AEFaceQuality.QUALITY_FAR;
            }
        }
        return result;
    }
//    private int facePosition(Rect rect, int width, int height) {
//        ENV_COUNT_MAX = 3;
//        int result = envLast;
//        Rect face = new Rect(rect);
//        face.top = rect.top - rect.height() / 6;
//        float minX = width / 20;
//        float maxX = width - minX;
//        float minY = height / 5;
//        float maxY = height - minY;
//
//        int min = (height > width) ? width : height;
//        float rate = (float) min / (float) rect.width();
//		/*if (rate > 2.5) {
//			if (envDelay(AEFaceQuality.QUALITY_FAR)) {
//				result[0] = AEFaceQuality.QUALITY_FAR;
//				envLast = AEFaceQuality.QUALITY_FAR;
//			} else {
//				result[0] = envLast;
//			}
//			return true;
//		} else if (rate < 1.6) {
//			if (envDelay(AEFaceQuality.QUALITY_NEAR)) {
//				result = AEFaceQuality.QUALITY_NEAR;
//				envLast = AEFaceQuality.QUALITY_NEAR;
//			}
//		} else */
//        if (face.left < minX || face.right > maxX ||
//                face.top < minY || face.bottom > maxY || rate < 1.1) {
//            if (activity.getHandler().getCurPos() == AEFaceAlive.POSE_MOUTH_OPEN ||
//                    face.top < (minY / 2) || face.bottom > maxY + (minY / 2) || rate < 1.1) {
//                ENV_COUNT_MAX = 1;
//            } else {
//                ENV_COUNT_MAX = 5;
//            }
//            if (envDelay(RecognizeActivity.QUALITY_OUT)) {
//                result = RecognizeActivity.QUALITY_OUT;
//                envLast = RecognizeActivity.QUALITY_OUT;
//            }
//        } else {
//            if (activity.getHandler().isSideFaceing()) {
//                if (envDelay(RecognizeActivity.QUALITY_SIDE)) {
//                    result = RecognizeActivity.QUALITY_SIDE;
//                    envLast = RecognizeActivity.QUALITY_SIDE;
//                }
//            } else {
//                if (envDelay(AEFaceQuality.QUALITY_OK)) {
//                    result = AEFaceQuality.QUALITY_OK;
//                    envLast = AEFaceQuality.QUALITY_OK;
//                }
//            }
//        }
//        return result;
//    }
//	private void startDelay(long time) {
//		delayAlive = true;
//		if (activity.getHandler().getCurSide() > 1) {
//			activity.showPoseSuccessMsg(true);
//		}
//		postDelayed(new Runnable() {
//			
//			@Override
//			public void run() {
//				delayAlive = false;
//				if (activity.getHandler().getCurSide() > 1) {
//					activity.showPoseSuccessMsg(false);
//				}
//			}
//		}, time);
//	}

    private void checkAgain() {
        Log.e(TAG, "checkAgain");
        Message message = Message.obtain(activity.getHandler(),
                IDConstants.id_decode_failed);
        message.sendToTarget();
    }
}
