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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.aeye.aeyelib.AEyeLightAlive;
import com.aeye.aeyelib.RGBUtil;
import com.aeye.android.config.ConfigData;
import com.aeye.android.data.AEFaceInfo;
import com.aeye.android.libutils.ComplexUtil;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.AEFacePack;
import com.aeye.face.AEFaceParam;
import com.aeye.face.config.IDConstants;
import com.aeye.face.lightView.DecodeData;
import com.aeye.face.lightView.LightCacheBean;
import com.aeye.face.lightView.RecognizeLightActivity;
import com.aeye.face.uitls.FLogUtil;
import com.aeye.face.uitls.PictureManagerUtilsLight;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceDetect;
import com.aeye.sdk.AEFaceQuality;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * 解码、停止<BR/>
 *
 */
public class DecodeHandlerLight extends Handler {
	private static final String TAG = DecodeHandlerLight.class.getSimpleName();

	public static final int MSG_RESET = 100;
	public static final int MSG_DECODE = 111;
	public static final int MSG_QUIT = 122;
	public static final int MSG_SIDE = 133;

	public static final int FACE_MAX = 5;
	/** 记录 摄像头的方向 **/
	private SharedPreferences sp;
	/**  **/
	private final RecognizeLightActivity activity;
	/** 检测 次数 **/
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
	private int CfgAliveLevel = 2;
	private int CfgMotionPicNum = 1;

	AEFaceInfo faceInfo = new AEFaceInfo();

	private int skipFrame = 5;

	//	int[] envCount = new int[]{0,0,0,0,0};
	private int envPreFrm;
	private int envCount;
	int ENV_COUNT_MAX = 3;
	private int envLast = AEFaceQuality.QUALITY_OK;
	private int currentColorIndex=-1;
	/**
	 * 记录每次闪光第一帧、 整体最后一帧的人脸位置 闪光过程中不再检测人脸，防止某些手机人脸检测耗时
	 */
	private Rect[]  takeRect;

	private Bitmap bitmapFull;

	private int currentColorCount = 0;
	private int lastColor = -1;
	private int bestPos = 0;
	private ArrayList<LightCacheBean> cacheBeanArrayList = new ArrayList<>();

//	private ArrayList<float[]> pointArrayList = new ArrayList<>();
	int[] qua = new int[]{0};
	int mInsertframId = -1;
	boolean isMotionAliveSuc = false;

	/** 构造 */
	public DecodeHandlerLight(RecognizeLightActivity activity) {
		this.activity = activity;
		sp = activity.getSharedPreferences(ConfigData.SP_CAMERA_INFO,
				Context.MODE_PRIVATE);
		faceInfo.cameraId = sp.getInt(ConfigData.SP_CAMERA_DIRECTION, 0);
		faceInfo.direction = activity.getOrientation();
		changeOri = (faceInfo.direction + 360) % 180 == 0 ? false: true;
		resetData();
	}

	private void resetData() {
		takeRect = null;
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
		currentColorIndex=-1;
		bitmapFull = null;
//		captured = 0;
//		captured1 = 0;
		cacheBeanArrayList.clear();
		bestPos = 0;
//		pointArrayList.clear();
		mInsertframId = -1;
		if(activity.getAliveMode()== AEFaceParam.ALIVEMODE_MOTION_LIGHT){
			isMotionAliveSuc  = false;
		}else {
			isMotionAliveSuc = true;
		}
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
			case IDConstants.id_decode:
				DecodeData decodeData = (DecodeData) message.obj;
				if (decodeData != null) {
					decode(decodeData, message.arg1, message.arg2);
				}
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
				FLogUtil.printLog("============================ quite looper ========================");
				if(bitmapFull !=null){
					bitmapFull.recycle();
				}
				Looper.myLooper().quit();
				break;
			case 1111:
				final long time = System.currentTimeMillis();
				final Bitmap bitmapSave = (Bitmap) message.obj;
				final int pos = message.arg1;
				new Thread(new Runnable() {
					@Override
					public void run() {
//						Bitmap bitmap = scaleBitmap(bitmapSave);
						saveBitmap(bitmapSave, pos + "");
						FLogUtil.printLog("===================== save bitmap cost ====================="+(System.currentTimeMillis()-time));
					}
				}).start();
				break;
			case IDConstants.id_inset_point:
				final int mFramId = message.arg1;
				final Rect rect = (Rect) message.obj;
				new Thread(new Runnable() {
					@Override
					public void run() {
						//插入关键点
						insertPoint(mFramId,rect);

					}
				}).start();

				break;
		}
	}

	private void insertPoint(int mFramId,Rect rect) {
		FLogUtil.printLog("===================== get best bitmap ===================== start");
		Bitmap bitmap = AEyeLightAlive.getInstance().AEYE_CurrentSetImageData(mFramId);
		FLogUtil.printLog("===================== get best bitmap ====================="+bitmap);

		if(bitmap !=null) {
			Rect[] rects =  AEFaceDetect.getInstance().AEYE_FaceDetect(bitmap);

			if(rects !=null) {
				FLogUtil.printLog("===================== get best bitmap rects  ===================" + rects[0]+" , center X,Y : "+rects[0].centerX()+","+rects[0].centerY());
				float[] mLandMark = getBestBitLocation(rects[0], bitmap);
				if(mLandMark !=null)
				AEyeLightAlive.getInstance().insetKeyPoints(mFramId, mLandMark);
			}
			else{
				FLogUtil.printLog("===================== get best bitmap rect null ===================");
				int x = rect.centerX()*2;
				int y  = rect.centerY()*2;
				int width = rect.width();
				int left = x -width, top = y - width, right = x +width,bottom = y +width;
				Rect scaleRect = new Rect(left,top,right,bottom);
				float[] mLandMark = getBestBitLocation(scaleRect, bitmap);
				AEyeLightAlive.getInstance().insetKeyPoints(mFramId, mLandMark);
			}
		}else{
			activity.finishActivityByOther(-15,"闪光颜色获取最佳图失败！");
		}
	}

	private void insertPoint2(int mFramId) {
		FLogUtil.printLog("===================== get best bitmap ===================== start");
		int[] data = AEyeLightAlive.getInstance().AEYE_CurrentPix(mFramId);
		FLogUtil.printLog("===================== get best pix ====================="+data);

		if(data !=null) {
			Rect[] rects = AEFaceDetect.getInstance().AEYE_FaceDetect(data, RGBUtil.width, RGBUtil.height);
			if(rects !=null) {
				FLogUtil.printLog("===================== get best bitmap rect1  *==================" + rects[0]);
				float[] mLandMark = getBestBitLocation2(rects, data);
				AEyeLightAlive.getInstance().insetKeyPoints(mFramId, mLandMark);
			}else{
				FLogUtil.printLog("===================== get best bitmap rect null ===================");
				activity.finishActivityByOther(-16,"获取最佳图人脸失败！");
			}
		}else{
			activity.finishActivityByOther(-15,"闪光颜色获取最佳图失败！");
		}
	}

	private float[] getBestBitLocation2(Rect[] rect,int[] data) {
		FLogUtil.printLog("===================== getBestBitLocation =====================rect: "+rect[0]);
		float[] qualit = AEFaceAlive.getInstance().AEYE_Alive_Quality(data, RGBUtil.width, RGBUtil.height,rect[0]);
		FLogUtil.printLog("===================== getBestBitLocation =====================qualit: "+qualit);
		float[] mlandMark = AEFaceAlive.getInstance().AEYE_Alive_Quality_Landmark(qualit);
		return mlandMark;
	}

	private Bitmap scaleBitmap(Bitmap image){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		image.compress(Bitmap.CompressFormat.JPEG, 65, out);
		int size = 5;
		float zoom = (float)Math.sqrt(size * 1024 / (float)out.toByteArray().length);

		Matrix matrix = new Matrix();
		matrix.setScale(zoom, zoom);

		Bitmap result = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

		out.reset();
		result.compress(Bitmap.CompressFormat.JPEG, 65, out);
		while(out.toByteArray().length > size * 1024){
			System.out.println(out.toByteArray().length);
			matrix.setScale(0.8f, 0.8f);
			result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);
			out.reset();
			result.compress(Bitmap.CompressFormat.JPEG, 50, out);
		}
		return  result;
	}
	//	static int captured = 0;
//	static int captured1 = 0;
	private void decode(DecodeData decodeData, int width, int height) {
		byte[] data = decodeData.getData();
		if (faceInfo.isAlive || !activity.getDecodeStatus() || data==null) {
			return;
		}
		FLogUtil.printLog( "TIME  decode  begin  "+ RecognizeLightActivity.isRecord+" , isGetLastBitmap :"+ RecognizeLightActivity.isGetLastBitmap);
		if(!RecognizeLightActivity.isRecord && RecognizeLightActivity.isGetLastBitmap){
			return;
		}
		long time = System.currentTimeMillis();
		faceInfo.imgByteA = data.clone();
		faceInfo.imgWidth = width;
		faceInfo.imgHeight = height;
		if (changeOri) {
			faceInfo.width = (width > 960 || height > 960) ? (height + 1) / 2
					: height;
			faceInfo.height = (width > 960 || height > 960) ? (width + 1) / 2
					: width;
		} else {
			faceInfo.width = (width > 960 || height > 960) ? (width + 1) / 2
					: width;
			faceInfo.height = (width > 960 || height > 960) ? (height + 1) / 2
					: height;
		}

		faceInfo.grayByteA = BitmapUtils.rawByteArray2Y(faceInfo.imgByteA, width, height,
				faceInfo.direction);
		//获取rgba的数据
		faceInfo.faceArr = cvtSpace(faceInfo.imgByteA, faceInfo.imgWidth,
				faceInfo.imgHeight, faceInfo.direction);
		Rect[] rect = null;
		if(!isMotionAliveSuc) {
			rect = AEFaceDetect.getInstance().AEYE_FaceDetect(faceInfo.faceArr,
					faceInfo.width, faceInfo.height);
			if(rect !=null && rect.length>0) {
				faceInfo.imgRect = rect[0];
				AEFaceAlive.getInstance().AEYE_Alive_SetPose(activity.getPose());
				long ret = AEFaceAlive.getInstance().AEYE_Alive_DetectVIS_Single(
						faceInfo.faceArr,
						faceInfo.width,
						faceInfo.height,
						faceInfo.imgRect,
						faceInfo);
				Log.e(TAG, "AEYE_AliveDetect : " + ret);
				isMotionAliveSuc = (ret==0)?true:false;
				if(isMotionAliveSuc){
					activity.setMotionAliveSuc();
				}
			}
		}


		if(aliveCount ==1){
			mInsertframId =-1;
		}
		boolean detectFace = false;
		if (lastColor != decodeData.getCurrentColorIndex()) {
			currentColorCount = 0;
			qua[0] = -1;
		}
		if(currentColorCount<=0)
		{
			detectFace = true;
		}
		if(detectFace) {
			rect = AEFaceDetect.getInstance().AEYE_FaceDetect(faceInfo.faceArr,
					faceInfo.width, faceInfo.height);
			takeRect = rect;
		}else{
			rect = takeRect;
		}
		boolean faceFar = false;

			if (rect != null) {
				faceInfo.faceNumber = rect.length;
				if(isMotionAliveSuc) {
					int faceWidth = rect[0].width();

					faceFar = (faceWidth < 340);
//				faceFar = false;
					if (faceFar) {
						activity.showFaceOut(false);
						aliveCount = 1;
						currentColorIndex = -1;
						removeCurrentMessage();
					} else {
						activity.showFaceOut(true);
						faceInfo.imgRect = rect[0];

						int quality = AEFaceQuality.QUALITY_OK;
						if (quality == AEFaceQuality.QUALITY_OK || quality == RecognizeLightActivity.QUALITY_SIDE) {
							loseCount = 0;
							faceCount++;
							lastColor = decodeData.getCurrentColorIndex();
							currentColorCount++;
							if (currentColorCount >= 3) {
								int state = getState(decodeData);
								activity.addPicNumber(decodeData.getCurrentColorIndex(), currentColorCount);

								final int pos = aliveCount - 1;
//						Message message = new Message();
//						message.obj = bitmapFull;
//						message.what = 1111;
//						message.arg1 = pos;
//							sendMessage(message);//保存插入的图片，用于分析
								aliveCount++;

								int ret = 0;
								if (!RecognizeLightActivity.isRecord && !RecognizeLightActivity.isGetLastBitmap) {
									activity.isGetLastBitmap = true;
									LightCacheBean insetBean = cacheBeanArrayList.get(0);
									Log.e("LIULU", "insert last two pic : " + insetBean.getCurrentColor() + " , state : " + insetBean.getState() + " ,framId : " + mInsertframId);
									ret = AEyeLightAlive.getInstance().AEYE_SetImageData(insetBean.getRgbImg(), insetBean.getCurrentColor(), insetBean.getState(), true, qua, mInsertframId, faceInfo.direction);
//								ret = AEyeLightAlive.getInstance().AEYE_SetImageData(bgrImg, decodeData.getCurrentColor(), state, true, qua,mInsertframId);
									FLogUtil.printLog(" ******* last Bitmap, currentIndex : " + decodeData.getCurrentColorIndex());
									//判断进入转换下个颜色序列
									activity.getHandler().addAliveImage(faceInfo);
									setPicReturn();
									if (ret == 0 || ret == 8) {
										activity.finishActivityBySuccessful();
									}
								} else {
									//不是最后一帧
									if (currentColorCount >= 4 && cacheBeanArrayList.size() > 1) {
										//第3帧开始做插入
										LightCacheBean insetBean = cacheBeanArrayList.get(0);
										boolean isNotBoundary = true;
										if( currentColorCount >=5){
											//取中间颜色的图片保存
											if(PictureManagerUtilsLight.getPictureManager().getCurNum()<=currentColorIndex) {
												FLogUtil.printLog(" add pic : "+currentColorIndex);
												activity.getHandler().addAliveImage(faceInfo);
												setPicReturn();
											}
										}
										if (isChangeNextColor(decodeData, insetBean)) {
										} else {
											ret = AEyeLightAlive.getInstance().AEYE_SetImageData(insetBean.getRgbImg(), insetBean.getCurrentColor(), insetBean.getState(), true, qua, mInsertframId, faceInfo.direction);
										}
										boolean isBestPic = (qua[0] == 0);
										if (isBestPic) {
											//获取最佳的这张图的关键点位
											bestPos = insetBean.getPos();
										}
										if (isChangeNextColor(decodeData, insetBean)) {
											//判断进入转换下个颜色序列
											int frame = mInsertframId - 1;
											Log.e("LIULU", "转换下一个颜色值" + decodeData.getCurrentColorIndex() + " , 前一个颜色序列frame : " + frame);

											insetPointMessage(frame, rect[0]);
										}

									}
									LightCacheBean mBean = new LightCacheBean(faceInfo.imgByteA, decodeData.getCurrentColor(), state, pos);
									//缓存前面的两帧
									if (cacheBeanArrayList.size() < 2) {
//									Log.e("LIULU", "添加缓存帧  ******************************* currentColorCount :"+currentColorCount);
										cacheBeanArrayList.add(mBean);
									} else {
										cacheBeanArrayList.remove(0);
										cacheBeanArrayList.add(mBean);
									}

								}
								if (ret != 0) {
//									*  0：成功
//											*  1：插入太多图片
//											*  2：插入的图片不够
//											*  3：插入的颜色不对
//											*  4：光照条件太差，屏幕光线没有对照片产生影响
									if (ret == 4) {
										activity.finishActivityByOther(-11, "光照条件太差");
									} else if (ret == 2) {
										activity.finishActivityByOther(-12, "插入的图片不够");
									} else if (ret == 3) {
										activity.finishActivityByOther(-13, "插入的颜色不对");
									} else {
										activity.finishActivityByOther(-14, "算法返回ret" + ret);
									}
								}
							}
						} else {
//						aliveCount = 1;
						}
					}
//			}
				}else{
					//有人脸，活体没过，界面增加提示
					activity.showTipAfterHasFace();
				}
			} else { // 如果没找到 人脸 的 具体位置 就继续寻找
				aliveCount = 1;
				currentColorIndex = -1;
				if(activity.getAliveMode()== AEFaceParam.ALIVEMODE_MOTION_LIGHT) {
					isMotionAliveSuc = false;
				}
				activity.showNoFace();
				activity.clearPicNumber();
				removeCurrentMessage();
			}
//		}
		activity.getHandler().restartDecode();
		checkAgain();
	}

	/**
	 * 发送插入关键点的耗时操作
	 * @param sertid
	 */
	private void insetPointMessage(int sertid,Rect rect) {
		Message messagePoint = new Message();
		messagePoint.what = IDConstants.id_inset_point;
		messagePoint.arg1 = sertid;
		messagePoint.obj = rect;
		sendMessage(messagePoint);//插入关键点，因耗时
	}

//	private void insetKeyPoints(int frame, float[] mLandMark) {
//		int[] pointX = new int[83];//所有x坐标
//		int[] pointY = new int[83];//所有Y坐标
//		for (int i = 0; i < mLandMark.length; i=i+2) {
//			pointX[i/2] = (int) mLandMark[i];
//			pointY[i/2] = (int) mLandMark[i+1];
//		}
//		AEyeLightAlive.getInstance().AEYE_InsertKeyPoints(pointX,pointY,frame);
//	}

	private boolean isChangeNextColor(DecodeData decodeData, LightCacheBean insetBean) {
		return insetBean.getCurrentColor() != decodeData.getCurrentColor();
	}

	/**
	 * 获取人脸关键点
	 * @param rect
	 */
	private float[] getFaceLocation(Rect[] rect) {
		float[] qualit = AEFaceAlive.getInstance().AEYE_Alive_Quality(bitmapFull,rect[0]);
		float[] mlandMark = AEFaceAlive.getInstance().AEYE_Alive_Quality_Landmark(qualit);

		//人脸框左上角  人脸框右下角， 左眼 24， 右眼 70， 鼻子 64 ，左嘴角 37 ，右嘴角 46
		int leftEyePos = 24*2,rightEyePos = 70*2,norsePos = 64*2,lefMouthPos = 37*2, rightMouthPos = 46*2, markLength = mlandMark.length;
		if(aliveCount ==1) {
			float[] leftEye = {mlandMark[leftEyePos], mlandMark[leftEyePos +1]};
			float[] rightEye = {mlandMark[rightEyePos ], mlandMark[rightEyePos +1]};

			float[] norse = {mlandMark[norsePos  ], mlandMark[norsePos +1]};

			float[] leftMouth = {mlandMark[lefMouthPos ], mlandMark[lefMouthPos +1]};
			float[] rightMouth = {mlandMark[rightMouthPos ], mlandMark[rightMouthPos +1]};

			float[] faceLeftTop = {rect[0].left,rect[0].top};
			float[] faceRightBottom = {rect[0].right,rect[0].bottom};
//						Bitmap bitmap = createBitmapPoint(bitmapFull, leftEye, rightEye, norse, leftMouth, rightMouth,faceLeftTop,faceRightBottom);
//						Bitmap bitmap = createBitmapMask(bitmapFull,mlandMark);
//						Message message = new Message();
//						message.obj = bitmap;
//						message.what = 1111;
//						message.arg1 = aliveCount;
//							sendMessage(message);//保存 图片，用于分析
		}
		return mlandMark;
	}


	private float[] getBestBitLocation(Rect rect,Bitmap bitmap) {
		FLogUtil.printLog("===================== getBestBitLocation =====================rect: "+rect+" , center X,Y : "+rect .centerX()+","+rect .centerY());
		float[] qualit = AEFaceAlive.getInstance().AEYE_Alive_Quality(bitmap,rect);
		FLogUtil.printLog("===================== getBestBitLocation =====================qualit: "+qualit);
		float[] mlandMark= null;
		if(qualit !=null)
		 mlandMark = AEFaceAlive.getInstance().AEYE_Alive_Quality_Landmark(qualit);

		//人脸框左上角  人脸框右下角， 左眼 24， 右眼 70， 鼻子 64 ，左嘴角 37 ，右嘴角 46
//		int leftEyePos = 24*2,rightEyePos = 70*2,norsePos = 64*2,lefMouthPos = 37*2, rightMouthPos = 46*2, markLength = mlandMark.length;
//			float[] leftEye = {mlandMark[leftEyePos], mlandMark[leftEyePos +1]};
//			float[] rightEye = {mlandMark[rightEyePos ], mlandMark[rightEyePos +1]};
//
//			float[] norse = {mlandMark[norsePos  ], mlandMark[norsePos +1]};
//
//			float[] leftMouth = {mlandMark[lefMouthPos ], mlandMark[lefMouthPos +1]};
//			float[] rightMouth = {mlandMark[rightMouthPos ], mlandMark[rightMouthPos +1]};
//
//			float[] faceLeftTop = {rect[0].left,rect[0].top};
//			float[] faceRightBottom = {rect[0].right,rect[0].bottom};
//						Bitmap bitmap1 = createBitmapPoint(bitmapFull, leftEye, rightEye, norse, leftMouth, rightMouth,faceLeftTop,faceRightBottom);
//						Bitmap bitmap = createBitmapMask(bitmapFull,mlandMark);
//						Bitmap bitmap1 = createBitmapPoint(bitmap,leftEye,rightEye);
//						Message message = new Message();
//						message.obj = bitmap1;
//						message.what = 1111;
//						message.arg1 = aliveCount;
//							sendMessage(message);//保存 图片，用于分析
		return mlandMark;
	}

	private Bitmap createBitmapPoint(Bitmap bitmap, float[] faceleft, float[] faceRight, float[] leftEye, float[] rightEye, float[] norse, float[] leftMouth, float[] rightMouth) {
		Bitmap pointbmp = bitmap;
		final Canvas canvas = new Canvas(pointbmp);
		 Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.RED);
		float radius = 3f;
		canvas.drawCircle(leftEye[0],leftEye[1],radius,paint);
		canvas.drawCircle(rightEye[0],rightEye[1],radius,paint);
		canvas.drawCircle(norse[0],norse[1],radius,paint);
		canvas.drawCircle(leftMouth[0],leftMouth[1],radius,paint);
		canvas.drawCircle(rightMouth[0],rightMouth[1],radius,paint);

		canvas.drawCircle(faceleft[0],faceleft[1],radius,paint);
		canvas.drawCircle(faceRight[0],faceRight[1],radius,paint);
		canvas.save();
		canvas.restore();
		return pointbmp;
	}

	private Bitmap createBitmapPoint(Bitmap bitmap, float[] leftEye, float[] rightEye) {
		Bitmap pointbmp = bitmap;
		final Canvas canvas = new Canvas(pointbmp);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.RED);
		float radius = 3f;
		canvas.drawCircle(leftEye[0],leftEye[1],radius,paint);
		canvas.drawCircle(rightEye[0],rightEye[1],radius,paint);
		canvas.save();
		canvas.restore();
		return pointbmp;
	}

	/**
	 * 所有关键点画出来
	 * @param bitmap
	 * @param landmark
	 * @return
	 */
	private Bitmap createBitmapMask(Bitmap bitmap, float[] landmark) {
		Bitmap pointbmp = bitmap;
		final Canvas canvas = new Canvas(pointbmp);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.RED);

		Paint paintText = new Paint();
		paintText.setAntiAlias(true);
		paintText.setColor(Color.YELLOW);
		float radius = 3f;
		for (int i = 0; i < landmark.length; i=i+2) {
			canvas.drawCircle(landmark[i],landmark[i+1],radius,paint);
			canvas.drawText(i/2+"",landmark[i],landmark[i+1],paintText);
		}

		canvas.save();
		canvas.restore();
		return pointbmp;
	}
	/**
	 * 从第一屏白色开始移除当前颜色值的发送
	 */
	private void removeCurrentMessage() {
		//从第一屏白色开始移除当前颜色值的发送
		removeMessages(IDConstants.id_decode_failed);
	}

	private int getState( DecodeData decodeData) {
		int state = 0;
		if (currentColorIndex == -1) {
			//白屏第一帧
			state = 0;
			FLogUtil.printLog("*************第一个白屏第一帧图片，需要将state设置成0 ," + state);
			decodeData.setCurrentColor(activity.getColorForSoList().get(0));
			mInsertframId =0;
		} else if (currentColorIndex >= 0) {
			if (currentColorIndex == 0) {
				state = 2;
				FLogUtil.printLog("**************第一个白屏时的非第一帧，需要将state设置成2 ," + state);
				decodeData.setCurrentColor(activity.getColorForSoList().get(0));
				mInsertframId =0;
			}
			if (currentColorIndex == 1 && decodeData.getCurrentColorIndex() == 1) {
				//第一个白屏时的非第一帧，需要将state设置成2
				state = 2;
				FLogUtil.printLog("**************第一个白屏时的非第一帧，需要将state设置成2 ," + state);
//				setPicReturn();
				decodeData.setCurrentColor(activity.getColorForSoList().get(0));
				mInsertframId =0;
			}
			if (currentColorIndex == 4 && decodeData.getCurrentColorIndex() == 5) {
				//黑屏第一帧
				state = 0;
				FLogUtil.printLog("**************第五个黑屏时的第一帧图片，需要将state设置成0 ," + state);
				decodeData.setCurrentColor(activity.getColorForSoList().get(4));
				mInsertframId =4;
			}
			if (currentColorIndex >= 5 && decodeData.getCurrentColorIndex() >= 5) {
				//第五个黑屏时的非第一帧，需要将state设置成3
				state = 3;
				FLogUtil.printLog("**************第五个黑屏时的非第一帧，需要将state设置成3 ," + state);
//				setPicReturn();
				decodeData.setCurrentColor(activity.getColorForSoList().get(4));
				mInsertframId =4;
			}
			if (decodeData.getCurrentColorIndex() == 2 || decodeData.getCurrentColorIndex() == 3 || decodeData.getCurrentColorIndex() == 4) {
				//第二、三、四个闪屏时的第一帧图片，需要将state设置成1
				if (currentColorIndex != decodeData.getCurrentColorIndex()) {
					state = 1;
					FLogUtil.printLog("**************二三四第一帧,需要将state设置成1 ," + state);
				} else {
					state = 4;
					FLogUtil.printLog("**************第二、三、四个闪屏时的非第一帧，需要将state设置成4, " + state);
//					setPicReturn();
				}
				if(decodeData.getCurrentColorIndex() ==2){
					decodeData.setCurrentColor(activity.getColorForSoList().get(1));
					mInsertframId =1;
				}
				if(decodeData.getCurrentColorIndex() ==3){
					decodeData.setCurrentColor(activity.getColorForSoList().get(2));
					mInsertframId =2;
				}
				if(decodeData.getCurrentColorIndex() ==4){
					decodeData.setCurrentColor(activity.getColorForSoList().get(3));
					mInsertframId =3;
				}
			}
		}
		currentColorIndex = decodeData.getCurrentColorIndex();
		return state;
	}

	/**
	 * 添加返回的图片
	 */
	private void setPicReturn() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int index = currentColorIndex - 1;
					FLogUtil.printLog("add pic curNum : " + PictureManagerUtilsLight.getPictureManager().getCurNum());
						PictureManagerUtilsLight.getPictureManager().addOnePictureInfo(faceInfo, index);
			}
		}).start();
	}

	public static Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height, int direction) {
		int imageSize = 1;
		int frameSize = width * height;
		int[] rgba = null;
//		if (width <= 960 && height <= 960) {
			rgba = new int[frameSize];
			imageSize = 0;
//		} else {
//			rgba = new int[frameSize / 4];
//			imageSize = 1;
//		}

		ComplexUtil.getInstance().YUVToBitmapR(data, rgba, width, height, imageSize, direction);
		Bitmap bmp = null;
		if (imageSize == 0) {
			if (direction != 90 && direction != -90) {
				bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				bmp.setPixels(rgba, 0, width, 0, 0, width, height);
			} else {
				bmp = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
				bmp.setPixels(rgba, 0, height, 0, 0, height, width);
			}
		} else if (imageSize == 1) {
			if (direction != 90 && direction != -90) {
				bmp = Bitmap.createBitmap(width / 2, height / 2, Bitmap.Config.ARGB_8888);
				bmp.setPixels(rgba, 0, width / 2, 0, 0, width / 2, height / 2);
			} else {
				bmp = Bitmap.createBitmap(height / 2, width / 2, Bitmap.Config.ARGB_8888);
				bmp.setPixels(rgba, 0, height / 2, 0, 0, height / 2, width / 2);
			}
		}

		return bmp;
	}
	private void elseProcess(AEFaceInfo faceInfo, boolean haveFace) {
		faceCount = 0;
		faceInfo.imgRect = null;
		if (cfgShowRect) {
			activity.getHandler().sendMessage(activity.getHandler().
					obtainMessage(IDConstants.id_draw_faceRect, faceInfo.width, faceInfo.height, null));
		}
		if (haveFace) {
			activity.showHint("aeye_quality_out", Color.WHITE);
			activity.showFaceOut(true);
		} else if ((loseCount++) == CfgLoseFace && !AEFacePack.getInstance().isAliveOff()
				&& activity.getDecodeStatus()) {
			activity.showHint("aeye_quality_out", Color.WHITE);
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

	private int facePosition(Rect rect, int width, int height) {
		ENV_COUNT_MAX = 3;
		int result = envLast;
		Rect face = new Rect(rect);
		face.top = rect.top - rect.height()/6;
		float minX = width/20;
		float maxX = width - minX;
		float minY = height/5;
		float maxY = height - minY;

		int min = (height > width)? width: height;
		float rate = (float)min / (float)rect.width();
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
		} else */if (face.left < minX || face.right > maxX ||
				face.top < minY || face.bottom > maxY || rate < 1.1) {
			if (activity.getHandler().getCurPos() == AEFaceAlive.POSE_MOUTH_OPEN ||
					face.top < (minY / 2) || face.bottom > maxY + (minY / 2) || rate < 1.1) {
				ENV_COUNT_MAX = 1;
			} else {
				ENV_COUNT_MAX = 5;
			}
			if (envDelay(RecognizeLightActivity.QUALITY_OUT)) {
				result = RecognizeLightActivity.QUALITY_OUT;
				envLast = RecognizeLightActivity.QUALITY_OUT;
			}
		} else {
			if (activity.getHandler().isSideFaceing()) {
				if (envDelay(RecognizeLightActivity.QUALITY_SIDE)) {
					result = RecognizeLightActivity.QUALITY_SIDE;
					envLast = RecognizeLightActivity.QUALITY_SIDE;
				}
			} else {
				if (envDelay(AEFaceQuality.QUALITY_OK)) {
					result = AEFaceQuality.QUALITY_OK;
					envLast = AEFaceQuality.QUALITY_OK;
				}
			}
		}
		return result;
	}

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
		Message message = Message.obtain(activity.getHandler(),
				IDConstants.id_decode_failed);
		message.arg1 = RecognizeLightActivity.getCurrentIndex();
		message.arg2 = RecognizeLightActivity.getColorForSo();
//		FLogUtil.printLog(" start check again ************" + RecognizeLightActivity.getColorForSo());
		message.sendToTarget();
//		FLogUtil.printLog(" start check send to target  ************" + RecognizeLightActivity.getColorForSo());
	}


	public static void saveBitmap(Bitmap bitmap,String name) {
		FileOutputStream fileOutput = null;
		BufferedOutputStream bOutputStream = null;

		try {
			String dir = Environment.getExternalStorageDirectory().getPath() + "/FaceCollect";
			File dirFile = new File(dir);
			if (!dirFile.exists()) {
				dirFile.mkdir();
			}

			if (bitmap != null) {
				File file = new File(dir + "/" + name + ".jpg");
				if (!file.exists()) {
					file.createNewFile();
				}

				fileOutput = new FileOutputStream(file);
				bOutputStream = new BufferedOutputStream(fileOutput);
				bitmap.compress(Bitmap.CompressFormat.PNG, 30, bOutputStream);
				bOutputStream.flush();
			}
		} catch (Exception var15) {
		} finally {
			try {
				if (bOutputStream != null) {
					bOutputStream.close();
					if (fileOutput != null) {
						fileOutput.close();
					}
				}
			} catch (Exception var14) {
			}

		}

	}
}
