package com.aeye.face.camera;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class AndroidCameraManager extends SurfaceView implements
		SurfaceHolder.Callback {

	static final String TAG = "AndroidCameraPreview";
	private SurfaceHolder m_surfaceHolder = null;
	private CameraConfigurationManager param;
	private Camera m_camera = null;
	private int m_cameraIndex = 0;

	private AndroidMediaRecorder m_recorder = null;
	private String m_fileName = "";

	public AndroidCameraManager(Context context) {  
		super(context, null);  
		init(context, CameraInfo.CAMERA_FACING_FRONT);
	}  
	public AndroidCameraManager(Context context, AttributeSet attrs) {  
		super(context, attrs, 0);
		init(context, CameraInfo.CAMERA_FACING_FRONT);
	}  
	
	public AndroidCameraManager(Context context, AttributeSet attrs, int defStyle) {  
		super(context, attrs, defStyle);  
		init(context, CameraInfo.CAMERA_FACING_FRONT);
	}  
	
	// index: 0.back facing camera 1.front facing camera
	public void init(Context context, int index) {
		m_surfaceHolder = getHolder();
		m_surfaceHolder.addCallback(this);

		if (index >= Camera.getNumberOfCameras()) {
			Log.d(TAG, "Invalid camera index!");
			return;
		}

		m_cameraIndex = index;
	}

	public void ReleaseDevices() {
		if (m_recorder != null) {
			m_recorder.ReleaseMediaRecorder();
			m_recorder = null;
		}
		if (m_camera != null) {
			m_camera.stopPreview();
			m_camera.release();
			m_camera = null;
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview
		try {
			Log.d(TAG, "surface created1");

			if (m_camera == null) {
				m_camera = Camera.open(m_cameraIndex);
//				if (CheckPreviewSize() != null) {
//					Camera.Parameters pare = m_camera.getParameters();
//					pare.setPreviewSize(640, 480);
//					m_camera.setParameters(pare);
//				}
				CameraConfigurationManager param = new CameraConfigurationManager(this.getContext());
				param.initFromCameraParameters(m_camera);
				param.setDesiredCameraParameters(m_camera, 90);

				m_camera.setPreviewDisplay(m_surfaceHolder);
				m_camera.startPreview();
			}
		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview:" + e.getMessage());
		} catch (RuntimeException e) {
			Log.d(TAG, "Error open camera:" + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Take care of releasing the Camera preview in your activity
		if (m_camera != null)
			m_camera.stopPreview();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it
		if (m_surfaceHolder.getSurface() == null) {
			return;
		}

		try {
			m_camera.stopPreview();
			m_camera.setPreviewDisplay(m_surfaceHolder);
			m_camera.startPreview();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		SetCameraDisplayOrientation();
	}

	private void SetCameraDisplayOrientation() {
		try {
			m_camera.stopPreview();
		} catch (Exception e) {
			Log.d(TAG, "Error stopping camera preview:" + e.getMessage());
		}

		try {
			if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				// 横屏
				m_camera.setDisplayOrientation(0);
			} else {
				// 竖屏
				m_camera.setDisplayOrientation(90);
			}

			m_camera.setPreviewDisplay(m_surfaceHolder);
			m_camera.startPreview();

		} catch (Exception e) {
			Log.d(TAG, "Error start camera preview:" + e.getMessage());
		}
	}

	public void SwapCamera() {
		if (m_camera != null) {
			m_camera.stopPreview();
			m_camera.release();
			m_camera = null;
		}

		m_cameraIndex = (m_cameraIndex + 1) % Camera.getNumberOfCameras();
		m_camera = Camera.open(m_cameraIndex);

		if (CheckPreviewSize() != null) {
			Camera.Parameters pare = m_camera.getParameters();
			pare.setPreviewSize(640, 480);
			m_camera.setParameters(pare);
		}

		try {
			m_camera.setPreviewDisplay(m_surfaceHolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		m_camera.startPreview();

		SetCameraDisplayOrientation();
	}

	public boolean RecordMedia(int cameraId, String flag, String idfId) {

		m_fileName = AndroidMediaRecorder.GenerateAudioFilePath(flag, idfId);
		if (m_recorder == null)
			m_recorder = new AndroidMediaRecorder(m_camera,
					m_surfaceHolder.getSurface());

		return m_recorder.StartRecord(m_fileName, cameraId);
	}

	public void StopRecord() {
		if (m_recorder == null)
			return;

		m_recorder.StopRecorder();
	}

	private Size CheckPreviewSize() {
		if (m_camera == null)
			return null;

		Camera.Parameters pare = m_camera.getParameters();
		List<Size> supportSize = pare.getSupportedPreviewSizes();
		if (supportSize.size() < 1)
			return null;

		for (int i = 0; i < supportSize.size(); i++) {
			Size size = supportSize.get(i);
			if (size.width == 640 && size.height == 480)
				return size;
		}

		return null;
	}
}