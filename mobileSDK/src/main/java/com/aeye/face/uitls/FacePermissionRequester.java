package com.aeye.face.uitls;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用无 UI 的保留型 Fragment 请求 SDK 所需权限（相机等）。
 * <p>
 * 目的：宿主 Activity 无需重写 {@code onRequestPermissionsResult}；SDK 内部拉起系统权限对话框，
 * 用户授权后自动回调 {@link Callback#onResult(boolean)}，从而在 {@link com.aeye.face.AEFaceVerifyFlow}
 * 中实现「授权后自动继续核验流程，无需用户再次点击按钮」。
 * </p>
 * <p>
 * 要求宿主 Activity 是 {@link FragmentActivity}（AppCompatActivity 天然满足）。
 * </p>
 */
public final class FacePermissionRequester {

    /** SDK 运行所需的权限（WAKE_LOCK 为普通权限，Manifest 声明后自动授予；相机是唯一需要运行时授权的项）。 */
    public static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WAKE_LOCK
    };

    private static final String TAG = "AEFace-PermReq";
    private static final int REQUEST_CODE = 0x7f01;

    public interface Callback {
        /** @param granted true 全部权限已授予；false 存在被拒 */
        void onResult(boolean granted);
    }

    private FacePermissionRequester() {
    }

    /** 判定 SDK 所需权限是否全部授予。 */
    public static boolean hasAllPermissions(Activity activity) {
        if (activity == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        for (String p : REQUIRED_PERMISSIONS) {
            if (activity.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 若权限已全部授予立即回调 {@code onResult(true)}；否则拉起系统权限对话框，
     * 授权/拒绝后再回调（回调发生在主线程）。
     */
    public static void requestIfNeeded(final Activity activity, final Callback callback) {
        if (callback == null) {
            return;
        }
        if (hasAllPermissions(activity)) {
            post(() -> callback.onResult(true));
            return;
        }
        if (!(activity instanceof FragmentActivity) || activity.isFinishing()) {
            // 无法拉起对话框：直接判为未授予，由调用方按失败处理
            post(() -> callback.onResult(false));
            return;
        }
        FragmentActivity host = (FragmentActivity) activity;
        FragmentManager fm = host.getSupportFragmentManager();
        Holder holder = (Holder) fm.findFragmentByTag(TAG);
        if (holder == null) {
            holder = new Holder();
            fm.beginTransaction().add(holder, TAG).commitNowAllowingStateLoss();
        }
        holder.request(callback);
    }

    private static List<String> missing(Activity activity) {
        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT < 23) {
            return list;
        }
        for (String p : REQUIRED_PERMISSIONS) {
            if (activity.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                list.add(p);
            }
        }
        return list;
    }

    private static void post(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }

    /**
     * 隐式承载权限申请的 Fragment：随 Activity 生命周期存续，
     * {@link #onRequestPermissionsResult} 回到主线程后转发给业务回调。
     */
    public static class Holder extends Fragment {

        private Callback pending;

        public Holder() {
            setRetainInstance(true);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        void request(Callback cb) {
            this.pending = cb;
            Activity host = getActivity();
            if (host == null || host.isFinishing()) {
                deliver(false);
                return;
            }
            List<String> need = missing(host);
            if (need.isEmpty()) {
                deliver(true);
                return;
            }
            requestPermissions(need.toArray(new String[0]), REQUEST_CODE);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode != REQUEST_CODE) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
            }
            boolean granted = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            deliver(granted);
        }

        private void deliver(boolean granted) {
            Callback cb = pending;
            pending = null;
            if (cb != null) {
                cb.onResult(granted);
            }
        }
    }
}
