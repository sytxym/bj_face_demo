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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.aeye.android.data.AEFaceInfo;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.android.uitls.ImageUtils;
import com.aeye.face.AEFacePack;
import com.aeye.face.config.IDConstants;
import com.aeye.face.uitls.PictureManagerUtilsLight;
import com.aeye.face.lightView.RecognizeLightActivity;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceAliveListener;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.<BR/>
 * 自动对焦、解码成功、解码不成功、网络超时
 */
public final class CaptureActivityHandlerLight extends Handler implements AEFaceAliveListener {
	private static final String TAG = "TAG_"+CaptureActivityHandlerLight.class.getSimpleName();
	public static final int MSG_CAPTURE_SIDE = 200;
	private final double POSVALUE_DEFAULT = 1000;

	/** 获取 人脸 和 眼睛成功的次数 **/
	public int succeedNum = 0;
	/** 成功检测活体次数 */
	public int succeedAN = 0;
	private int poseIndex = 0;
	private int[] poseArray = null;
	private int[] poseTotal = null;
	private RecognizeLightActivity activity;
	private AEFaceInfo mBuffer;
//	private AccShakeDetect shakeDet;
	private double compare = POSVALUE_DEFAULT;
	private int facing = 1;  // 1 正脸     0 非正脸
	private int mCurPos = 0;
	private boolean mSideSucc = true;
	private int cameraDirection = 0;
	/** 处理繁重任务的线程 **/
	private final DecodeThreadLight decodeThread;
	private State state;

	private enum State {
		PREVIEW, PAUSE
	}

	private boolean poseChange = false;

	/**
	 * 构造
	 * @param activity
	 */
	public CaptureActivityHandlerLight(RecognizeLightActivity activity) {
		this.activity = activity;
//		shakeDet = new AccShakeDetect();
//		shakeDet.registerSensor(activity, new OnShakeListener() {
//
//			@Override
//			public void onShake(boolean status) {
//				if (decodeThread != null) {
//					decodeThread.getHandler().sendMessage(
//							Message.obtain(decodeThread.getHandler(), IDConstants.id_shake, status));
//				}
//			}
//
//		});
		
		decodeThread = new DecodeThreadLight(activity);
		decodeThread.start();
		
		mBuffer = new AEFaceInfo();
		AEFaceAlive.getInstance().AEYE_Alive_setAliveListenerVIS(this);
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case IDConstants.id_auto_focus:
			if (state == State.PREVIEW) {
				CameraManager.get(activity).requestAutoFocus(this,
						IDConstants.id_auto_focus);
			}
			break;
			
		case IDConstants.id_success_side:
			mSideSucc = true;
		case IDConstants.id_decode_succeeded: {
			Log.e("CaptureHandler","id decode _succeeded");
			AEFaceInfo bitRect = (AEFaceInfo) message.obj;

//			if (succeedNum < AEFacePack.getInstance().getPictureNumber()) {
//				PictureManagerUtils.getPictureManager().addOnePictureInfo(
//						bitRect, succeedNum);
//			}
//			succeedNum++;

			if (bitRect.isAlive) {
//				if (succeedNum >= AEFacePack.getInstance()
//						.getPictureNumber()) {
					// 返回数据
					activity.finishActivityBySuccessful();// onrecog
//				}
			}
			break;
		}

		case IDConstants.id_decode_failed:
			// We're decoding as fast as possible, so when one decode fails,
			// start another.
			// resetData();
			if (state == State.PREVIEW) {
				CameraManagerLight.get(activity).requestPreviewFrame(decodeThread.getHandler(),
						IDConstants.id_decode);
			}
			break;
			
		case IDConstants.id_fail_finish:
			activity.finishActivityByFail();
			break;
			
		case IDConstants.id_draw_faceRect:
			Rect rect = (Rect)message.obj;
			int width = message.arg1;
			int height = message.arg2;
			activity.showFaceRect(rect, width, height, 
					(cameraDirection == 180)?false:true);
		}
	}

	public void resetData() {
		removeCallbacksAndMessages(null);
		decodeThread.getHandler().removeCallbacksAndMessages(null);
		
		activity.resetData();
		state = State.PAUSE;
		succeedNum = 0;
		mSideSucc = true;
		compare = POSVALUE_DEFAULT;
		facing = 1;
		poseChange = false;
		poseIndex = 0;
		poseArray = null;
		cameraDirection = activity.getOrientation();
		
		PictureManagerUtilsLight.getPictureManager().resetPictureManager();
		decodeThread.getHandler().sendEmptyMessage(DecodeHandler.MSG_RESET);
	}

	public void startPreview() {
		resetData();
		Log.e(TAG,"=================================startPreview");
		CameraManagerLight.get(activity).startPreview();
	}

	/** 退出预览、退出decodehandler的looper */
	public void quitSynchronously() {
		CameraManagerLight.get(activity).stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(),
				IDConstants.id_quit);
		quit.sendToTarget();
		decodeThread.end();

		// Be absolutely sure we don't send any queued up messages
		removeMessages(IDConstants.id_decode_succeeded);
		removeMessages(IDConstants.id_decode_failed);
		
		activity = null;
		mBuffer = null;
		AEFaceAlive.getInstance().AEYE_Alive_setAliveListenerVIS(null);
//		shakeDet.unregisterSensor();
	}

	/** 重新预览时请求自动焦点和预览帧 */
	public void restartPreviewAndDecode() {
		state = State.PREVIEW;
		CameraManagerLight.get(activity).requestPreviewFrame(decodeThread.getHandler(),
				IDConstants.id_decode);
		CameraManagerLight.get(activity).requestAutoFocus(this,
				IDConstants.id_auto_focus);
		activity.setDecodeStatus(true);
		poseTotal = AEFacePack.getInstance().getAlivePose();
		if (!AEFacePack.getInstance().isAliveOff()) {
			int motion = AEFacePack.getInstance().getAliveMotions();
			poseArray = computePoseArray(motion);
			mCurPos = 0;//poseArray[0];//
		} else if (AEFacePack.getInstance().isModelAllSide()) {
			poseIndex = IDConstants.SIDE_MIN;
		}
	}
	
	public boolean startOneSide() {
		if (mSideSucc) {
			decodeThread.getHandler().sendEmptyMessageDelayed(
					IDConstants.id_request_side, 800);
			mSideSucc = false;
			return true;
		} else {
			return false;
		}
	}
	
	public int getCurSide() {
		return poseIndex;
	}
	
	public int getNextSide() {
		return ++poseIndex;
	}

	public void cancelDecodeTask() {
		removeCallbacksAndMessages(null);
		if (decodeThread != null) {
			Handler handler = decodeThread.getHandler();
			if (handler != null) {
				activity.setDecodeStatus(false);
				handler.removeCallbacksAndMessages(null);
			}
		}
	}
	
	public void pauseDecode() {
		state = State.PAUSE;
	}

	public void restartDecode() {
		state = State.PREVIEW;
	}
	public void resumeDecode() {
		Log.e("capture handler","resumeDecode");
		state = State.PREVIEW;
		CameraManagerLight.get(activity).requestPreviewFrame(decodeThread.getHandler(),
				IDConstants.id_decode);
	}

	private int[] computePoseArray(int action) {
		if (AEFacePack.getInstance().isFixMotion()) {
			return computeFixedSequenceArray(action);
		}
		return computeRandomArray(action);
	}

	private int[] computeFixedSequenceArray(int action) {
		poseIndex = 0;
		int[] ret = new int[action];
		if (poseTotal == null || poseTotal.length == 0) {
			return ret;
		}
		for (int i = 0; i < action; i++) {
			ret[i] = poseTotal[i % poseTotal.length];
		}
		return ret;
	}

	private int[] computeRandomArray(int action) {
		poseIndex = 0;		
		int[] ret = new int[action];
		int first = AEFacePack.getInstance().getAliveFirstMotion();
		first = (first==1)?2:first; // 摇头
		int item = 0;
		SecureRandom rand = new SecureRandom();
		if (first >= 2 && first <= 6) {
			ret[0] = first;
			item = 1;
		}
		
		ArrayList<Integer> list = new ArrayList<Integer>();
		
		for (int i=0; i<poseTotal.length; i++) { //总共5个动作
			if (first != poseTotal[i]) {
				list.add(poseTotal[i]);
			}
		}
		
		for (; item<action; item++) {
			if (list.isEmpty() && action > item) {
				for (int i=0; i<poseTotal.length; i++) {
					if (ret[item - 1] != poseTotal[i]) {
						list.add(poseTotal[i]);
					}
				}
			}
			int index = rand.nextInt(list.size());
			ret[item] = list.get(index);
			list.remove(index);
		}
		return ret;
	}
	
	public int getCurPos() {
		return mCurPos;
	}
	
	public int updateToNextPose() {
		int size = poseIndex++;
		if(size<5)
		mCurPos = poseArray[size];
		return getCurPos();
	}
	
	public void flashDisplay(final boolean voicePlay, final boolean anim) {
		if (mCurPos >= 0 && mCurPos <= 6) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					activity.showAlivePose(mCurPos, voicePlay, anim);
				}
			});
		}
	}
	
	public boolean isInPoseChange() {
		return poseChange;
	}
	
	public void displayPoseChange(final boolean delay) {
//		poseChange = true;
//		if (activity.getHandler().getCurSide() > 1) {
////			activity.showPoseSuccessMsg(true);
//			activity.dismissHint();
//		}
//
//		pauseDecode();
//		activity.runOnUiThread(new Runnable() {
////		postDelayed(new Runnable() {
////
//			@Override
//			public void run() {
//
//				if (activity.getHandler().getCurSide() > 1) {
////					activity.showPoseSuccessMsg(false);
//				}
//
//				if (!AEFacePack.getInstance().isUseGlobalTime()) {
//					activity.restartTimer(AEFacePack.getInstance().getMotionTime());
//				}
//
//				flashDisplay(true, false/*true*/);
//
////				postDelayed(new Runnable() {
////
////					@Override
////					public void run() {
//						poseChange = false;
//						Log.e("resume","display decode : resumeDecode");
//						resumeDecode();
////					}
////				}, delay?50:0);
//			}});
	}

	
	boolean isSideFaceing() {
		return facing == 1? false: true;
	}
	
	private Bitmap cutAliveImage(Rect rect, Bitmap image) {
		int rectWidth = rect.width();
		int rectHeight = rect.height();
		int rectCenterX = rect.centerX();
		int rectCenterY = rect.centerY();
		
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		
		int min;
		
		int minLeft = rectCenterX > rectWidth? rectWidth: rectCenterX;
		min = minLeft;
		int minRight = (imageWidth - rectCenterX) > rectWidth? rectWidth: (imageWidth - rectCenterX);
		min = min > minRight? minRight: min;
		int minTop = rectCenterY > rectHeight? rectHeight: rectCenterY;
		min = min > minTop? minTop: min;
		int minBottom = (imageHeight - rectCenterY) > rectHeight? rectHeight: (imageHeight - rectCenterY);
		min = min > minBottom? minBottom: min;
		
		Rect resizeRect =  new Rect(rectCenterX - min, rectCenterY - min , rectCenterX + min, rectCenterY + min);
		Bitmap cutBmp = Bitmap.createBitmap(image, resizeRect.left, resizeRect.top, 
				resizeRect.width(), resizeRect.height());
		return Bitmap.createScaledBitmap(cutBmp, 240, 240, true);
	}
	
	private Bitmap cutFaceImageWithAlive(AEFaceInfo face) {
		Bitmap bitmap = BitmapUtils.rawByteArray2RGBABitmap2(face.imgByteA,
				face.imgWidth, face.imgHeight, face.direction);
		boolean[] bSuccess = new boolean[1];

		Rect rect = ImageUtils.resize(face.imgRect, bitmap.getWidth(),
				bitmap.getHeight(), bSuccess);

		Bitmap cutBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top,
				rect.width(), rect.height());

		face.faceRect = ImageUtils.getFaceRect(face.imgRect, rect);
		face.faceBitmap = ImageUtils.enlarge(cutBitmap);
		
		return cutAliveImage(face.imgRect, bitmap);
	}
	
	public void addAliveImage(AEFaceInfo info) {
		PictureManagerUtilsLight.getPictureManager().addAliveImage(
				cutFaceImageWithAlive(info));
	}
	
	private AEFaceInfo infoFilter(int pose, double rate, AEFaceInfo info) {
		if ((Math.abs(rate) < compare)) {
			compare = rate;
			mBuffer.imgByteA = null;
			mBuffer.imgByteA = info.imgByteA.clone();
			mBuffer.imgRect = info.imgRect;
			mBuffer.imgWidth = info.imgWidth;
			mBuffer.imgHeight = info.imgHeight;
			mBuffer.direction = info.direction;
		}
		//不做正脸检测
		facing = 1;
		
		return mBuffer;
	}

	@Override
	public int onAlivePose(int pose, double poseValue, Object reserve) {

			int next = updateToNextPose();
		Log.e("onAlivePose","onAlivePose: "+poseIndex);
				displayPoseChange(pose != AEFaceAlive.POSE_MOUTH_OPEN);
				if (!AEFacePack.getInstance().isUseGlobalTime()) {
					activity.stopTimer();
				}

			if (reserve != null) {
				if (!AEFacePack.getInstance().isCaptureStraight()) {
					poseValue = 0;
				}
				AEFaceInfo info = (AEFaceInfo)reserve;
				if (poseIndex > 1) {
					addAliveImage(info);
				} else {
					info.cutFaceImage();
				}
				Log.e("ZDX", "onAlivepose "+activity.currentIndex+" ,picnum: "+ PictureManagerUtilsLight.getPictureManager().getCurNum());

				if (activity.currentIndex < 6) {
					PictureManagerUtilsLight.getPictureManager().addOnePictureInfo(
							info, activity.currentIndex);
					Log.e("ZDX", "currentIndex "+activity.currentIndex+" ,picnum: "+ PictureManagerUtilsLight.getPictureManager().getCurNum());
				}
				if(poseIndex>=5 && activity.currentIndex>=5) {
					info.isAlive = true;
					Message message = Message.obtain(activity.getHandler(),
							IDConstants.id_decode_succeeded, info);
					message.sendToTarget();
					compare = POSVALUE_DEFAULT;
				}
			}
		activity.showAlivePose(pose,true,true);
		Log.d("ZDX", "onAlivePose POSE : " + pose/* + "  SUCCESS : " + success*/);
		return next;
	}

	@Override
	public void onAliveFrame(int pose, double poseValue, Object reserve) {
		// TODO Auto-generated method stub
//		Log.d("ZDX", "pose : " + pose + "  value : " + poseValue);
		if (AEFacePack.getInstance().isCaptureStraight()) {
			infoFilter(pose, poseValue, (AEFaceInfo) reserve);
		}
	}

	@Override
	public int onAliveFinish(int reason, Object reserve) {
		Log.d("ZDX", "onAliveFinish reason : " + reason);
		AEFaceInfo info = (AEFaceInfo)mBuffer;
		if (!AEFacePack.getInstance().isCaptureStraight()) {
			info = infoFilter(0, 0, (AEFaceInfo)reserve);
		}
		if (reason == 1 /*|| compare == POSVALUE_DEFAULT && null == AEFaceDetect.getInstance().
				AEYE_FaceDetect(info.faceArr, info.width, info.height)*/ ) {
			activity.finishActivityByFail();
		} else if (reason == 0) {
			addAliveImage(info);
			info.isAlive = true;
			Message message = Message.obtain(activity.getHandler(),
					IDConstants.id_decode_succeeded, info);
			message.sendToTarget();
		}
		return 0;
	}
}