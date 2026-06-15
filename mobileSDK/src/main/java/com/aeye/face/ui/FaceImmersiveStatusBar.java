package com.aeye.face.ui;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.sdk.core.R;

/**
 * 实名核身 / 活体流程统一沉浸式状态栏：状态栏与顶栏同色（{@link R.color#face_theme_primary}），浅色状态栏图标。
 */
public final class FaceImmersiveStatusBar {

    private static final int TOOLBAR_CONTENT_DP = 56;

    private FaceImmersiveStatusBar() {
    }

    /** 在 {@code setContentView} 之前调用 */
    public static void install(Activity activity) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        int primary = ContextCompat.getColor(activity, R.color.face_theme_primary);
        int white = ContextCompat.getColor(activity, R.color.white);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(primary);
            window.setNavigationBarColor(white);
        }
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                controller.setAppearanceLightNavigationBars(true);
            }
        }
    }

    /**
     * 顶栏延伸至状态栏区域，并同步下方占位高度（与顶栏总高一致）。
     *
     * @param toolbar                 顶栏容器（需 {@code android:id="@+id/face_toolbar"}）
     * @param contentGapBelowToolbar  顶栏下方的占位 View，可为 null
     */
    public static void bindToolbar(Activity activity, View toolbar, View contentGapBelowToolbar) {
        if (toolbar == null) {
            return;
        }
        final int toolbarContentPx = dp(activity, TOOLBAR_CONTENT_DP);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets status = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            int top = status.top;
            int totalHeight = toolbarContentPx + top;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp != null) {
                lp.height = totalHeight;
                v.setLayoutParams(lp);
            }
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            if (contentGapBelowToolbar != null) {
                ViewGroup.LayoutParams gapLp = contentGapBelowToolbar.getLayoutParams();
                if (gapLp != null) {
                    gapLp.height = totalHeight;
                    contentGapBelowToolbar.setLayoutParams(gapLp);
                }
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    /** 底部按钮等增加导航栏安全区边距 */
    public static void bindBottomMargin(Activity activity, View bottomView, int extraBottomDp) {
        if (bottomView == null) {
            return;
        }
        final int extraPx = dp(activity, extraBottomDp);
        ViewCompat.setOnApplyWindowInsetsListener(bottomView, (v, windowInsets) -> {
            int nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (mlp != null) {
                mlp.bottomMargin = extraPx + nav;
                v.setLayoutParams(mlp);
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(bottomView);
    }

    private static int dp(Activity activity, int dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
