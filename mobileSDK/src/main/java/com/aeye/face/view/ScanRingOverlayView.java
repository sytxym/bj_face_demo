package com.aeye.face.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.sdk.core.R;

/**
 * 活体取景：圆外白底 + 粗灰轨 + 细蓝弧（同圆心，蓝弧在灰轨内穿行）；成功/失败细色环。
 */
public class ScanRingOverlayView extends View {

    /** 灰/色环中心线半径占取景面板宽度比例（与 onDraw 一致） */
    private static final float RING_CENTER_RATIO = 0.42f;

    public static final int MODE_SCANNING = 0;
    public static final int MODE_SUCCESS = 1;
    public static final int MODE_FAIL = 2;

    private int mode = MODE_SCANNING;
    /** 0~1：成功过渡时蓝弧扫满整圈；检测中为 0 */
    private float progress;
    /** 检测态蓝弧起始角（度，0=3 点钟方向顺时针），用于绕圆旋转 */
    private float arcStartAngle = 150f;
    /** 检测态单段弧长（度），沿圆周旋转一整圈 */
    private static final float SCAN_ARC_SWEEP_DEG = 90f;

    /** 白底竖屏：圆外遮罩，仅预览圆内透明露出 SurfaceView */
    private boolean holeMaskEnabled;

    private final Paint ringThin = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringArc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringSolid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outsideWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outerPath = new Path();

    public ScanRingOverlayView(Context context) {
        super(context);
        init();
    }

    public ScanRingOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScanRingOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        ringThin.setStyle(Paint.Style.STROKE);
        ringArc.setStyle(Paint.Style.STROKE);
        ringSolid.setStyle(Paint.Style.STROKE);
        outsideWhite.setStyle(Paint.Style.FILL);
        outsideWhite.setColor(Color.WHITE);
        outsideWhite.setAntiAlias(false);
    }

    /** 与 {@link #onDraw} 中灰环中心线半径一致 */
    public static float computeRingCenterRadius(int panelPx) {
        return panelPx * RING_CENTER_RATIO;
    }

    /**
     * 预览圆孔半径：与灰环内缘对齐，略外扩以消除遮罩抗锯齿造成的 1~2px 白缝。
     */
    public static float computePreviewHoleRadius(Context context, int panelPx) {
        float ringCenterR = computeRingCenterRadius(panelPx);
        float baseStroke = context.getResources().getDimension(R.dimen.face_scan_ring_base_stroke);
        float density = context.getResources().getDisplayMetrics().density;
        float ringInnerR = ringCenterR - baseStroke * 0.5f;
        float holeR = ringInnerR + Math.max(1f, 0.5f * density);
        float minHole = panelPx * 0.28f;
        return holeR > minHole ? holeR : minHole;
    }

    /**
     * 竖屏 3:4 预览尺寸（center-cover）：宽度对齐圆孔直径，高度按 4:3 放大，
     * 上下超出圆孔部分由 {@link #drawOutsideHoleMask} 遮住，圆内铺满且不变形（同支付宝类方案）。
     * @return [width, height]
     */
    public static int[] computePortraitPreviewCoverSize(Context context, int panelPx) {
        float holeR = computePreviewHoleRadius(context, panelPx);
        int surfaceW = Math.round(holeR * 2f);
        int surfaceH = Math.round(surfaceW * 4f / 3f);
        return new int[]{surfaceW, surfaceH};
    }

    public void setHoleMaskEnabled(boolean enabled) {
        if (holeMaskEnabled == enabled) {
            return;
        }
        holeMaskEnabled = enabled;
        invalidate();
    }

    public void setMode(int mode) {
        this.mode = mode;
        invalidate();
    }

    public void setProgress(float p) {
        if (p < 0f) {
            p = 0f;
        } else if (p > 1f) {
            p = 1f;
        }
        this.progress = p;
        if (mode == MODE_SCANNING) {
            invalidate();
        }
    }

    public float getProgress() {
        return progress;
    }

    /** 设置检测态进度弧旋转角度（0~360，顺时针） */
    public void setArcStartAngle(float degrees) {
        float a = degrees % 360f;
        if (a < 0f) {
            a += 360f;
        }
        if (arcStartAngle == a) {
            return;
        }
        arcStartAngle = a;
        if (mode == MODE_SCANNING) {
            invalidate();
        }
    }

    public float getArcStartAngle() {
        return arcStartAngle;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        int panelPx = Math.min(w, h);
        float radius = computeRingCenterRadius(panelPx);
        float baseStroke = getResources().getDimension(R.dimen.face_scan_ring_base_stroke);
        float progressStroke = getResources().getDimension(R.dimen.face_scan_ring_progress_stroke);
        float resultStroke = getResources().getDimension(R.dimen.face_scan_ring_result_stroke);
        float holeR = computePreviewHoleRadius(getContext(), panelPx);

        int cBase = ContextCompat.getColor(getContext(), R.color.face_scan_ring_base);
        int cOk = ContextCompat.getColor(getContext(), R.color.face_result_success);
        int cFailRing = ContextCompat.getColor(getContext(), R.color.face_scan_ring_fail);

        RectF baseOval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        if (mode == MODE_SUCCESS) {
            drawOutsideHoleMask(canvas, w, h, cx, cy, holeR);
            ringSolid.setStrokeWidth(resultStroke);
            ringSolid.setStrokeCap(Paint.Cap.ROUND);
            ringSolid.setColor(cOk);
            canvas.drawOval(baseOval, ringSolid);
            return;
        }
        if (mode == MODE_FAIL) {
            drawOutsideHoleMask(canvas, w, h, cx, cy, holeR);
            ringSolid.setStrokeWidth(resultStroke);
            ringSolid.setStrokeCap(Paint.Cap.ROUND);
            ringSolid.setColor(cFailRing);
            canvas.drawOval(baseOval, ringSolid);
            return;
        }

        drawOutsideHoleMask(canvas, w, h, cx, cy, holeR);

        ringThin.setStrokeWidth(baseStroke);
        ringThin.setColor(cBase);
        ringThin.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawOval(baseOval, ringThin);

        ringArc.setStrokeWidth(progressStroke);
        ringArc.setStrokeCap(Paint.Cap.ROUND);
        float startDeg = arcStartAngle;
        float sweep;
        if (progress > 0f) {
            sweep = 360f * progress;
        } else {
            sweep = SCAN_ARC_SWEEP_DEG;
        }
        drawGradientScanArc(canvas, baseOval, cx, cy, startDeg, sweep, ringArc);
    }

    /**
     * 检测态蓝弧：#094194 100% → 0% 沿弧长 sweep 渐变（弧头实色、弧尾透明）。
     */
    private void drawGradientScanArc(Canvas canvas, RectF oval, float cx, float cy,
                                     float startDeg, float sweepDeg, Paint paint) {
        if (sweepDeg <= 0.5f) {
            return;
        }
        int solid = ContextCompat.getColor(getContext(), R.color.face_scan_ring_progress);
        int transparent = Color.argb(0, Color.red(solid), Color.green(solid), Color.blue(solid));
        float sweepNorm = Math.min(sweepDeg / 360f, 1f);
        SweepGradient gradient = new SweepGradient(cx, cy,
                new int[]{solid, transparent},
                new float[]{0f, sweepNorm});
        Matrix matrix = new Matrix();
        matrix.setRotate(startDeg, cx, cy);
        gradient.setLocalMatrix(matrix);
        paint.setShader(gradient);
        canvas.drawArc(oval, startDeg, sweepDeg, false, paint);
        paint.setShader(null);
    }

    /** 圆外白底，仅圆内露出 SurfaceView */
    private void drawOutsideHoleMask(Canvas canvas, int w, int h, float cx, float cy, float holeR) {
        if (!holeMaskEnabled || w <= 0 || h <= 0) {
            return;
        }
        outerPath.reset();
        outerPath.setFillType(Path.FillType.EVEN_ODD);
        outerPath.addRect(-2f, -2f, w + 2f, h + 2f, Path.Direction.CW);
        outerPath.addCircle(cx, cy, holeR, Path.Direction.CW);
        canvas.drawPath(outerPath, outsideWhite);
    }
}
