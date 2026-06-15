package com.aeye.face.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FaceView extends AppCompatImageView{
	private final String TAG = "AEYE_PREVIEW";
	private Bitmap m_Bitmap = null;
	private Bitmap m_BitmapTemp = null;
	private boolean mSuppressFaceRect;

	public void setSuppressFaceRect(boolean suppress) {
		this.mSuppressFaceRect = suppress;
		if (suppress) {
			setImageDrawable(null);
		}
	}

	public FaceView(Context context) {
		super(context);
	}

	public FaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void drawFaceRect(Rect faceRect, int width, int height,
			boolean bMirror) {
		if (mSuppressFaceRect) {
			return;
		}
		Rect curfaceRect = new Rect();
		
//		Log.e("xiaomin", "width = " + width + " height = " + height);
		if (m_BitmapTemp == null) {
			m_BitmapTemp = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
		}

		if (faceRect == null || faceRect.left >= faceRect.right
				|| faceRect.top >= faceRect.bottom || faceRect.left < 0
				|| faceRect.right > width || faceRect.top < 0
				|| faceRect.bottom > height) {
			setImageBitmap(m_BitmapTemp);
//			Log.e("xiaomin", "faceRect == null");
			return;
		}

//		Log.e("xiaomin", "top = " + faceRect.top + " left = " + faceRect.left
//				+ " bottom = " + faceRect.bottom + " right = " + faceRect.right);

		curfaceRect.left = faceRect.left;
		curfaceRect.right = faceRect.right;
		curfaceRect.top = faceRect.top;
		curfaceRect.bottom = faceRect.bottom;
		
		if (m_Bitmap != null) {
			m_Bitmap.recycle();
			m_Bitmap = null;
		}

		m_Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(m_Bitmap);

		if (bMirror) {
			int left = width - curfaceRect.right;
			int right = width - curfaceRect.left;
			curfaceRect.left = left;
			curfaceRect.right = right;
			// curfaceRect.bottom = height - curfaceRect.top;
			// curfaceRect.top = height - curfaceRect.bottom;
		}

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(3.0f);
		paint.setColor(Color.parseColor("#00fc45"));
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawRect(curfaceRect, paint);
		int bitmapWidth = m_Bitmap.getWidth();
		int bitmapHeight = m_Bitmap.getHeight();
		setImageBitmap(m_Bitmap);
	}
}