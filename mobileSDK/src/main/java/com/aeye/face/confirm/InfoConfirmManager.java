package com.aeye.face.confirm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.aeye.face.AEFaceSdk;

/**
 * 实名信息预览对外门面：拉取预览数据并打开 {@link InfoConfirmActivity}。
 */
public final class InfoConfirmManager {

    private InfoConfirmManager() {
    }

    /** 使用 {@link AEFaceSdk#init(String)} 配置的 baseUrl，按 userId 拉取预览信息。 */
    public static void fetch(String userId, InfoConfirmRepository.FetchCallback callback) {
        AEFaceSdk.ensureInitialized();
        DefaultInfoConfirmLoader loader = new DefaultInfoConfirmLoader(
                AEFaceSdk.getApiBaseUrl(),
                userId,
                AEFaceSdk.isUseMockOnError());
        InfoConfirmRepository.fetch(loader, callback);
    }

    public static void fetch(InfoConfirmLoader loader, InfoConfirmRepository.FetchCallback callback) {
        InfoConfirmRepository.fetch(loader, callback);
    }

    public static InfoConfirmPayload getCached() {
        return InfoConfirmRepository.getCached();
    }

    public static Intent toIntent(Context ctx, InfoConfirmPayload payload) {
        InfoConfirmPayload p = payload != null ? payload : new InfoConfirmPayload();
        Intent i = new Intent(ctx, InfoConfirmActivity.class);
        if (!TextUtils.isEmpty(p.getMainTitle())) {
            i.putExtra(InfoConfirmExtras.EXTRA_MAIN_TITLE, p.getMainTitle());
        }
        if (!TextUtils.isEmpty(p.getSubTitle())) {
            i.putExtra(InfoConfirmExtras.EXTRA_SUB_TITLE, p.getSubTitle());
        }
        i.putExtra(InfoConfirmExtras.EXTRA_REAL_NAME, p.getRealName());
        i.putExtra(InfoConfirmExtras.EXTRA_REGION, p.getRegion());
        i.putExtra(InfoConfirmExtras.EXTRA_ID_TYPE, p.getIdType());
        i.putExtra(InfoConfirmExtras.EXTRA_ID_NUMBER, p.getIdNumber());
        if (!TextUtils.isEmpty(p.getFailDetail())) {
            i.putExtra(InfoConfirmExtras.EXTRA_FAIL_DETAIL, p.getFailDetail());
        }
        return i;
    }

    public static void open(Activity activity, InfoConfirmPayload payload) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        activity.startActivity(toIntent(activity, payload));
    }

    /** 按 userId 拉取预览并打开确认页。 */
    public static void fetchAndOpen(Activity activity, String userId,
                                    InfoConfirmRepository.FetchCallback callback) {
        fetch(userId, new InfoConfirmRepository.FetchCallback() {
            @Override
            public void onSuccess(InfoConfirmPayload payload, boolean fromRemote) {
                open(activity, payload);
                if (callback != null) {
                    callback.onSuccess(payload, fromRemote);
                }
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }
}
