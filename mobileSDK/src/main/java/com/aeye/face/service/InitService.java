package com.aeye.face.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.aeye.android.config.ConfigData;
import com.aeye.sdk.AEFaceAlive;
import com.aeye.sdk.AEFaceDetect;
import com.aeye.sdk.AEFaceQuality;
import com.aeye.sdk.AEFaceUnhack;

public class InitService extends Service {
    private String TAG = InitService.class.getSimpleName();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initModel();
        return super.onStartCommand(intent, flags, startId);
    }

	@Override
	public void onDestroy() {
		release();
		super.onDestroy();
	}

    private void release() {
        Log.e(TAG, "release");
        AEFaceDetect.getInstance().AEYE_FaceDetect_Destory();
        AEFaceQuality.getInstance().AEYE_FaceQuality_Destory();
        AEFaceAlive.getInstance().AEYE_Alive_DestoryVIS();
//		AEFaceRecog.getInstance().AEYE_FaceExtract_Destory();
		AEFaceUnhack.getInstance().AEYE_AliveUnhack_Destory();
	}

    private void initModel() {
        Log.e(TAG, "initModel");
        ConfigData.makeDestDir(this);
		ConfigData.unlockInit(this);

		AEFaceDetect.getInstance().AEYE_FaceDetect_Init(this, null);
		AEFaceQuality.getInstance().AEYE_FaceQuality_Init(this, null);
		AEFaceAlive.getInstance().AEYE_Alive_InitVIS(this, null);
//		AEFaceRecog.getInstance().AEYE_FaceExtract_Init(this, null);
		AEFaceUnhack.getInstance().AEYE_AliveUnhack_Init(this, null);
		ConfigData.lockInit(this);
	}
}
