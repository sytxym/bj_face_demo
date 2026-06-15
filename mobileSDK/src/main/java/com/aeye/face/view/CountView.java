package com.aeye.face.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class CountView extends View {
	private Integer m_Count = 0;
	private int m_Max = 8;

	public CountView(Context context) {
		this(context, null);
	}

	public CountView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CountView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int centreX = getWidth() / 2; // ��ȡԲ�ĵ�x���
		int centreY = getHeight() / 2;
		int radius = (int) (centreX > centreY ? centreY / 2 : centreX / 2); // Բ���İ뾶
		Paint paint = new Paint();
		paint.setColor(0xffffffff); // ����Բ������ɫ
		paint.setStyle(Paint.Style.STROKE); // ���ÿ���
		paint.setStrokeWidth(radius / 8); // ����Բ���Ŀ��
		paint.setAntiAlias(true); // �����
		canvas.drawCircle(centreX, centreY, radius, paint); // ����Բ��

		synchronized (this) {
//			if (m_Count % m_Max != 0) {
				paint.setColor(0xffa6a6a6);
				paint.setStrokeWidth(radius / 8);
				paint.setStyle(Paint.Style.STROKE);
//				RectF oval = new RectF(centreX - radius, centreY - radius,
//						centreX + radius, centreY + radius);
//				canvas.drawArc(oval, 270, 360/* * m_Count / m_Max*/, false, paint);
				canvas.drawCircle(centreX, centreY, radius, paint);
//			}

			paint.setStyle(Paint.Style.FILL);
			paint.setColor(0xff4cb229);
			paint.setTextSize(getFontSize(60));
			paint.setTypeface(Typeface.DEFAULT_BOLD);
			float textWidth = paint.measureText("" + m_Count);
			float textHeight = paint.measureText("8"); 
			canvas.drawText("" + m_Count, centreX - textWidth / 2, centreY
					+ textHeight / 2, paint);
		}

		/*
		 * paint.setStrokeWidth(radius / 8); //����Բ���Ŀ��
		 * paint.setColor(0xff0f0f00); //���ý�ȵ���ɫ
		 * 
		 * 
		 * synchronized(m_Count) { canvas.drawCircle(centreX, centreY, radius,
		 * paint); // if (true) { //canvas.drawArc(oval, 270, 359, false,
		 * paint); //} else { //canvas.drawArc(oval, 270, 360 * (m_Max -
		 * m_Count) / m_Max, false, paint); //}
		 * 
		 * //canvas.drawArc(oval, 0, 360 * m_Count / m_Max - 90, false, paint);
		 * }
		 */
	}

	public int getFontSize(int size) {
		int width = getWidth(); // ��ȡԲ�ĵ�x���
		int height = getHeight();
		float ratioWidth = (float) width / 180;
		float ratioHeight = (float) height / 342;

		float radio = Math.min(ratioWidth, ratioHeight);
		return (int) (size * radio);
	}

	public void setCount(int max, int num) {
		synchronized (this) {
			if (max > 0)
				m_Max = max;
			m_Count = num;
		}
		postInvalidate();
	}
}