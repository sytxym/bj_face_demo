package com.aeye.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

import com.aeye.android.constant.AEReturnCode;
import com.aeye.android.data.AEFaceInfo;
import com.aeye.android.uitls.BitmapUtils;
import com.aeye.android.uitls.ImageUtils;

public class AEFaceTools {
	private static AEFaceTools mInstance = null;

	public static AEFaceTools getInstance() {
		if (mInstance == null) {
			mInstance = new AEFaceTools();
		}
		return mInstance;
	}

	public long AEYE_Init(Context context, Bundle paras) {
		AEFaceDetect.getInstance().AEYE_FaceDetect_Init(context, paras);
		AEFaceQuality.getInstance().AEYE_FaceQuality_Init(context, paras);
		return 0;
	}

	public int AEYE_QualityDetect(Bitmap dest) {
		if (dest == null) {
			return AEReturnCode.FACEVIS_QLT_PARUNERROR;
		}
//		Bitmap dest = BitmapUtils.getOptimizeBitmap(img);
		int width = dest.getWidth();
		int height = dest.getHeight();
		byte[] imageYUV = BitmapUtils.getImageGrayData(dest);
		Rect[] face = AEFaceDetect.getInstance().AEYE_FaceDetect(dest);

		if (face != null) {
			Log.d("ZDX", "find face!    ---   " + width + " : " + height + " ------ " + face[0].toString());
			return AEFaceQuality.getInstance().AEYE_FaceQuality(imageYUV, width, height, face[0]);
		}

		Log.d("ZDX", "no face!    ---   " + width + " : " + height);
		return -1;
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
	
	public Bitmap AEYE_CutAlivePic(Bitmap image, Rect faceRect) {
		return cutAliveImage(faceRect, image);
	}
	
	public Bitmap AEYE_CutPic(Bitmap image, int width, int height) {
		if (image == null) {
			return null;
		}

		Rect[] face = AEFaceDetect.getInstance().AEYE_FaceDetect(image);
		
		if (face != null) {
			AEFaceInfo info = ImageUtils.cutoutImage(image, face[0]);
			return BitmapUtils.scaleBitmap(info.faceBitmap, width, height);
		}
		
		return null;
	}
	
	public Rect[] AEYE_FaceDetect(Bitmap image) {
		if (image == null) {
			return null;
		}
		long before = System.currentTimeMillis();
		Rect[] face = AEFaceDetect.getInstance().AEYE_FaceDetect(image);
		long after = System.currentTimeMillis();
		Log.d("ZDX", image.getWidth() + " X " + image.getHeight() + " cost time " + (after-before) + "ms");
		
		return face;
	}

	public int AEYE_Destory() {
		AEFaceQuality.getInstance().AEYE_FaceQuality_Destory();
		AEFaceDetect.getInstance().AEYE_FaceDetect_Destory();
		return 0;
	}
}
