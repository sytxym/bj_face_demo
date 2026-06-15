/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aeye.face.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.aeye.face.AEFacePack;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one
 * talking to it. The implementation encapsulates the steps needed to take
 * preview-sized images, which are used for both preview and decoding.
 * 
 */
public final class CameraManagerLight {

	private static final String TAG = CameraManager.class.getSimpleName();

	private static final int MIN_FRAME_WIDTH = 358;// 240;
	private static final int MIN_FRAME_HEIGHT = 441;// 240;
	private static final int MAX_FRAME_WIDTH = 800;// 480;
	private static final int MAX_FRAME_HEIGHT = 600;// 360;

	private static CameraManagerLight cameraManager;

	static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
	static {
		int sdkInt;
		try {
			sdkInt = Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException nfe) {
			// Just to be safe
			sdkInt = 10000;
		}
		SDK_INT = sdkInt;
	}

	private final Context context;

	public CameraConfigurationManagerLight getConfigManager() {
		return configManager;
	}

	private final CameraConfigurationManagerLight configManager;

	public Camera getCamera() {
		return camera;
	}

	private Camera camera;
	private Rect framingRect;
	private Rect framingRectInPreview;
	private boolean initialized;
	private boolean previewing;
	private final boolean useOneShotPreviewCallback;
	/**
	 * Preview frames are delivered here, which we pass on to the registered
	 * handler. Make sure to clear the handler so it will only receive one
	 * message.
	 */
	private final PreviewCallbackLight previewCallback;
	/**
	 * Autofocus callbacks arrive here, and are dispatched to the Handler which
	 * requested them.
	 */
	private final AutoFocusCallbackLight autoFocusCallback;

	/**
	 * Initializes this static object with the Context of the calling Activity.
	 *
	 * @param context
	 *            The Activity which wants to use the camera.
	 */
	public static void init(Context context) {
		if (cameraManager == null) {
			cameraManager = new CameraManagerLight(context);
		}
	}

	public static void unInit() {
		cameraManager = null;
	}

	/**
	 * Gets the CameraManager singleton instance.
	 *
	 * @return A reference to the CameraManager singleton.
	 */
	public static CameraManagerLight get(Context context) {
		init(context);
		return cameraManager;
	}

	private CameraManagerLight(Context context) {

		this.context = context;
		this.configManager = new CameraConfigurationManagerLight(context);

		// Camera.setOneShotPreviewCallback() has a race condition in Cupcake,
		// so we use the older
		// Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later,
		// we need to use
		// the more efficient one shot callback, as the older one can swamp the
		// system and cause it
		// to run out of memory. We can't use SDK_INT because it was introduced
		// in the Donut SDK.
		// useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) >
		// Build.VERSION_CODES.CUPCAKE;
		useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3
																				// =
																				// Cupcake

		previewCallback = new PreviewCallbackLight(configManager,
				useOneShotPreviewCallback);
		autoFocusCallback = new AutoFocusCallbackLight();
	}

	private int getDisplayOrientation(int carmeraId) {
		CameraInfo info= new CameraInfo();
		Camera.getCameraInfo(carmeraId, info);
		WindowManager wm=(WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display=wm.getDefaultDisplay();
		int rotation=display.getRotation();
		int degrees=0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees=0;
			break;
		case Surface.ROTATION_90:
			degrees=90;
			break;
		case Surface.ROTATION_180:
			degrees=180;
			break;
		case Surface.ROTATION_270:
			degrees=270;
			break;
		default:
			break;
		}
		int result;
		if(info.facing== CameraInfo.CAMERA_FACING_FRONT){
			result=(info.orientation+degrees)%360;
			result=(360-result)%360;
		}else{
			result=(info.orientation-degrees+360)%360;
		}
		return result;
	}
	
	public int getOrientation(int cameraId) {
		CameraInfo info= new CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int degrees=0;
		WindowManager wm=(WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display=wm.getDefaultDisplay();
		int rotation=display.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees=0;
			break;
		case Surface.ROTATION_90:
			degrees=90;
			break;
		case Surface.ROTATION_180:
			degrees=180;
			break;
		case Surface.ROTATION_270:
			degrees=270;
			break;
		default:
			break;
		}
		rotation = (info.orientation + degrees) % 360;
		if (rotation > 180)
			return rotation - 360;
		return rotation;
	}
	
	/**
	 * Opens the camera driver and initializes the hardware parameters.
	 * 
	 * @param holder
	 *            The surface object which the camera will draw preview frames
	 *            into.
	 * @throws IOException
	 *             Indicates the camera driver failed to open.
	 */
	public void openDriver(SurfaceHolder holder, int defaultId)
			throws IOException {
		if (camera == null) {
			camera = Camera.open(defaultId);
			if (camera == null) {
				throw new IOException();
			}
			camera.setPreviewDisplay(holder);

			if (!initialized) {
				initialized = true;
				configManager.initFromCameraParameters(camera);
			}
			// configManager.setDesiredCameraParameters(camera);

			if (AEFacePack.getInstance().isSetDisplayOrientation()) {
				int rotate = AEFacePack.getInstance().getDisplayOrientation();
				configManager.setDesiredCameraParameters(camera, rotate);
			} else {
				configManager.setDesiredCameraParameters(camera,
						getDisplayOrientation(defaultId));
				/*if (defaultId == CameraInfo.CAMERA_FACING_BACK) {
					configManager.setDesiredCameraParameters(camera, 90);
				} else {
					configManager.setDesiredCameraParameters(camera, 90);
					// pad
					// configManager.setDesiredCameraParameters(camera,270);
				}*/
			}
		}
	}



	/**
	 * Closes the camera driver if still in use.
	 */
	public void closeDriver() {
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}

	/**
	 * Asks the camera hardware to begin drawing preview frames to the screen.
	 */
	public void startPreview() {
		if (camera != null && !previewing) {
			camera.startPreview();
			previewing = true;
		}
	}

	/**
	 * Tells the camera to stop drawing preview frames.
	 */
	public void stopPreview() {
		if (camera != null && previewing) {
			if (!useOneShotPreviewCallback) {
				camera.setPreviewCallback(null);
			}
			camera.stopPreview();
			previewCallback.setHandler(null, 0);
			autoFocusCallback.setHandler(null, 0);
			previewing = false;
		}
	}

	/**
	 * A single preview frame will be returned to the handler supplied. The data
	 * will arrive as byte[] in the message.obj field, with width and height
	 * encoded as message.arg1 and message.arg2, respectively.
	 * 
	 * @param handler
	 *            The handler to send the message to.
	 * @param message
	 *            The what field of the message to be sent.
	 */
	public void requestPreviewFrame(Handler handler, int message) {
		if (camera != null && previewing) {
			previewCallback.setHandler(handler, message);
			if (useOneShotPreviewCallback) {
				camera.setOneShotPreviewCallback(previewCallback);
			} else {
				camera.setPreviewCallback(previewCallback);
			}
		}
	}

	/**
	 * Asks the camera hardware to perform an autofocus.
	 * 
	 * @param handler
	 *            The Handler to notify when the autofocus completes.
	 * @param message
	 *            The message to deliver.
	 */
	public void requestAutoFocus(Handler handler, int message) {
		if (camera != null && previewing) {
			autoFocusCallback.setHandler(handler, message);
			// Log.d(TAG, "Requesting auto-focus callback");
			try {
				camera.autoFocus(autoFocusCallback);
			} catch (Exception e) {
			    e.printStackTrace();
            }
		}
	}

	/**
	 * Calculates the framing rect which the UI should draw to show the user
	 * where to place the barcode. This target helps with alignment as well as
	 * forces the user to hold the device far enough away to ensure the image
	 * will be in focus.
	 * 
	 * @return The rectangle to draw on screen in window coordinates.
	 */
	public Rect getFramingRect() {
		Point screenResolution = configManager.getScreenResolution();
		if (framingRect == null) {
			if (camera == null) {
				return null;
			}
			int width = screenResolution.x * 3 / 4;
			if (width < MIN_FRAME_WIDTH) {
				width = MIN_FRAME_WIDTH;
			} else if (width > MAX_FRAME_WIDTH) {
				width = MAX_FRAME_WIDTH;
			}
			int height = screenResolution.y * 3 / 4;
			if (height < MIN_FRAME_HEIGHT) {
				height = MIN_FRAME_HEIGHT;
			} else if (height > MAX_FRAME_HEIGHT) {
				height = MAX_FRAME_HEIGHT;
			}
			int leftOffset = (screenResolution.x - width) / 2;
			int topOffset = (screenResolution.y - height) / 2;
			framingRect = new Rect(leftOffset, topOffset, leftOffset + width,
					topOffset + height);
			Log.d(TAG, "Calculated framing rect: " + framingRect);
		}
		return framingRect;
	}

	/**
	 * Like {@link #getFramingRect} but coordinates are in terms of the preview
	 * frame, not UI / screen.
	 */
	public Rect getFramingRectInPreview() {
		if (framingRectInPreview == null) {
			Rect rect = new Rect(getFramingRect());
			Point cameraResolution = configManager.getCameraResolution();
			Point screenResolution = configManager.getScreenResolution();
			// modify here
			// rect.left = rect.left * cameraResolution.x / screenResolution.x;
			// rect.right = rect.right * cameraResolution.x /
			// screenResolution.x;
			// rect.top = rect.top * cameraResolution.y / screenResolution.y;
			// rect.bottom = rect.bottom * cameraResolution.y /
			// screenResolution.y;
			rect.left = rect.left * cameraResolution.y / screenResolution.x;
			rect.right = rect.right * cameraResolution.y / screenResolution.x;
			rect.top = rect.top * cameraResolution.x / screenResolution.y;
			rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y;
			framingRectInPreview = rect;
		}
		return framingRectInPreview;
	}

	public Context getContext() {
		return context;
	}

	public int setCameraDefaultId() {
		int defaultCameraId = 0;
		int noCamera = 0x123;
		int numberOfCameras = Camera.getNumberOfCameras();
		if (numberOfCameras > 0) {
			CameraInfo cameraInfo = new CameraInfo();

			for (int i = 0; i < numberOfCameras; i++) {
				Camera.getCameraInfo(i, cameraInfo);
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
					defaultCameraId = i;
				} else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
					defaultCameraId = i;
				}
			}

			return defaultCameraId;
		} else {
			return noCamera;
		}
	}

}
