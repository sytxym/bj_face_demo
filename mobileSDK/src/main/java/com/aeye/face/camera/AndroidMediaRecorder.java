package com.aeye.face.camera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.SyncStateContract.Constants;
import android.util.Log;
import android.view.Surface;

import com.aeye.android.config.ConfigData;
import com.aeye.android.uitls.CommonUtils;

public class AndroidMediaRecorder {
	private static final String TAG = "AndroidMediaRecorder";
	private MediaRecorder m_recorder = null;
	private Camera m_camera = null;
	private String m_fileName = "";
	private Surface m_surface = null;
	private FileOutputStream m_fileOutput = null;

	public AndroidMediaRecorder(Camera camera, Surface surface) {
		m_recorder = new MediaRecorder();

		m_camera = camera;
		m_surface = surface;
	}

	public boolean StartRecord(String fileName, int cameraId) {
		if (m_recorder == null)
			m_recorder = new MediaRecorder();

		m_recorder.reset();
		m_fileName = fileName;

		if (m_recorder == null || m_camera == null || m_fileName.length() < 5 || m_surface == null) {
			Log.d(TAG, "Invalid parameters");
			return false;
		}

		if (m_fileOutput != null) {
			try {
				m_fileOutput.close();
				m_fileOutput = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d(TAG, "Output file close failed!");
				return false;
			}
		}

		try {
			m_fileOutput = new FileOutputStream(m_fileName);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			Log.d(TAG, "Output file create failed!");
			m_fileOutput = null;
			return false;
		}

		m_camera.unlock();
		Log.d(TAG, "unlock");

		m_recorder.setCamera(m_camera);

		m_recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		m_recorder.setAudioSamplingRate(44100);
		m_recorder.setAudioChannels(2);
		m_recorder.setAudioEncodingBitRate(64000);

		m_recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		m_recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

		m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		m_recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

		m_recorder.setVideoEncodingBitRate(800000);
		m_recorder.setVideoSize(640, 480);
		// m_recorder.setVideoFrameRate(25);

		// m_recorder.setProfile(CamcorderProfile.get(CamcorderProfile));
		m_recorder.setOutputFile(m_fileName);
		if (cameraId == CameraInfo.CAMERA_FACING_BACK) {// 后置摄像头
			m_recorder.setOrientationHint(90);
		} else if (cameraId == CameraInfo.CAMERA_FACING_FRONT) {// 前置摄像头
			m_recorder.setOrientationHint(270);
		}
		m_recorder.setPreviewDisplay(m_surface);

		try {
			m_recorder.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, "MediaRecorder Prepared error!");
			// ReleaseMediaRecorder();
			m_recorder.reset();
			try {
				m_camera.reconnect();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.d(TAG, "MediaRecorder Prepared error!");
			// ReleaseMediaRecorder();
			m_recorder.reset();
			try {
				m_camera.reconnect();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			return false;
		}
		m_recorder.start(); // 开始刻录
		return true;
	}

	public void StopRecorder() {
		if (m_recorder == null) {
			Log.d(TAG, "Invalid parameter: m_recorder in StopRecorder function");
			return;
		}

		m_recorder.stop();
		m_recorder.reset();

		if (m_camera != null)
			m_camera.lock();

		if (m_fileOutput != null) {
			try {
				m_fileOutput.close();
				m_fileOutput = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d(TAG, "Output file close failed!");
			}
		}
	}

	public void ReleaseMediaRecorder() {
		if (m_recorder != null) {
			m_recorder.reset();
			m_recorder.release();
			m_recorder = null;

			if (m_camera != null)
				m_camera.lock();
		}
	}

	public static String GenerateAudioFilePath(String flag, String idfId) {
		String videoFilePath = ConfigData.getVideoDir();
		StringBuffer tempFileName = new StringBuffer();
		tempFileName.append(videoFilePath);
		File file = new File(tempFileName.toString());
		if (!file.exists() && !file.isDirectory()) {
			file.mkdirs();
		}
		tempFileName.append("/");
		tempFileName.append("video"+System.currentTimeMillis()+".mp4");
		
		return tempFileName.toString();
	}
}
