package com.aeye.face.view;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceview extends SurfaceView implements SurfaceHolder.Callback{

	private String TAG = "CameraSurfaceview";
	
	private SurfaceHolder mHolder = null;
	
	private Camera mCamera = null;

	public CameraSurfaceview(Context context, Camera camera) {
		super(context);
		this.mCamera = camera;
		
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
	}

	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		 if(mHolder.getSurface() == null){
			 return;
		 }
		 
		 try{
			 mCamera.stopPreview();
		 }catch(Exception e){
		 }
		 
		 try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
		} catch (IOException e) {
			 Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
		 
		 
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mCamera != null) {
			mCamera.stopPreview();
		}
	}


	 

}
