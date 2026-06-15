/*
 * Copyright (C) 2010 ZXing authors
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
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.aeye.face.lightView.RecognizeLightActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class CameraConfigurationManagerLight {

	private static final String TAG = CameraConfigurationManager.class
			.getSimpleName();

	private static final int TEN_DESIRED_ZOOM = 27;
	private static final int DESIRED_SHARPNESS = 30;

	private static final Pattern COMMA_PATTERN = Pattern.compile(",");

	private final Context context;
	private Point screenResolution;
	public Point cameraResolution;
	private int previewFormat;
	private String previewFormatString;

	CameraConfigurationManagerLight(Context context) {
		this.context = context;
	}

	/**
	 * Reads, one time, values from the camera that are needed by the app.
	 */
	void initFromCameraParameters(Camera camera) {
		Camera.Parameters parameters = camera.getParameters();
		previewFormat = parameters.getPreviewFormat();
		previewFormatString = parameters.get("preview-format");
		Log.d(TAG, "Default preview format: " + previewFormat + '/'
				+ previewFormatString);
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		screenResolution = new Point(display.getWidth(), display.getHeight());
		Log.i(TAG, "Screen resolution: " + screenResolution);
		/** modify here **/
		Point screenResolutionForCamera = new Point();
		screenResolutionForCamera.x = screenResolution.x;
		screenResolutionForCamera.y = screenResolution.y;
		// preview size is always something like 480*320, other 320*480
		if (screenResolution.x < screenResolution.y) {
			screenResolutionForCamera.x = screenResolution.y;
			screenResolutionForCamera.y = screenResolution.x;
		}
		/** end **/
		cameraResolution = getCameraResolution(parameters,
				screenResolutionForCamera);
		Log.d(TAG, "Camera resolution: " + screenResolutionForCamera);
		/*
		 * cameraResolution = getCameraResolution(parameters, screenResolution);
		 * Log.d(TAG, "Camera resolution: " + screenResolution);
		 */
		/** end **/
	}
	/**
	 * Sets the camera up to take preview images which are used for both preview
	 * and decoding. We detect the preview format here so that
	 * buildLuminanceSource() can build an appropriate LuminanceSource subclass.
	 * In the future we may want to force YUV420SP as it's the smallest, and the
	 * planar Y can be used for barcode scanning without a copy in some cases.
	 */
	void setDesiredCameraParameters(Camera camera, int rotate) {
		Camera.Parameters parameters = camera.getParameters();
		Log.i(TAG, "Setting preview size: " + cameraResolution);
		parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);

		List<String> focusModesList = parameters.getSupportedFocusModes();
		if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		} else if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}else if(focusModesList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}
//		parameters.setAutoExposureLock(true);
//		parameters.setAutoWhiteBalanceLock(true);
//		parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//		parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
//		parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
//		parameters.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);

//		parameters.setPreviewFpsRange(15,20);
//		parameters.setPreviewFrameRate(30);
		String supportedIsoValues = parameters.get("iso-values");
		if(supportedIsoValues !=null && supportedIsoValues.length()>0) {
			Log.e(TAG, " iso : " + supportedIsoValues);
			String flat = parameters.flatten();
			String[] isoValues = null;
			String values_keyword=null;
			String iso_keyword=null;
			if(flat.contains("iso-values")) {
				// most used keywords
				values_keyword="iso-values";
				iso_keyword="iso";
			} else if(flat.contains("iso-mode-values")) {
				// google galaxy nexus keywords
				values_keyword="iso-mode-values";
				iso_keyword="iso";
			} else if(flat.contains("iso-speed-values")) {
				// micromax a101 keywords
				values_keyword="iso-speed-values";
				iso_keyword="iso-speed";
			} else if(flat.contains("nv-picture-iso-values")) {
				// LG dual p990 keywords
				values_keyword="nv-picture-iso-values";
				iso_keyword="nv-picture-iso";
			}
			if(iso_keyword!=null) {
				// flatten contains the iso key!!
				String iso = flat.substring(flat.indexOf(values_keyword));
				iso = iso.substring(iso.indexOf("=")+1);

				if(iso.contains(";")) iso = iso.substring(0, iso.indexOf(";"));

				isoValues = iso.split(",");
				for (int i = 0; i <isoValues.length ; i++) {
					Log.e(TAG, " isoValues  : " + isoValues[i]);
				}
				if(isoValues.length>0){
//					parameters.set("iso",isoValues[isoValues.length-1]);
				}

			} else {
				// iso not supported in a known way
			}
		}
//		parameters.set("iso","ISO100");
//		setFlash(parameters);
		//setZoom(parameters);
		// setSharpness(parameters);
		// modify here
		// camera.setDisplayOrientation(0);
		// Pad
		// camera.setDisplayOrientation(270);
		camera.setDisplayOrientation(rotate);
//		clearCameraFocus(camera,parameters);


		camera.setParameters(parameters);
	}

	/**
	 * 清除自动对焦
	 */
	private static void clearCameraFocus(Camera camera,Camera.Parameters parameters) {
		camera.cancelAutoFocus();
		parameters = camera.getParameters();
		parameters.setFocusAreas(null);
		parameters.setMeteringAreas(null);
		try {
			camera.setParameters(parameters);
		} catch (Exception e) {
			Log.e(TAG, "failed to set parameters.\n" + e);
		}
	}
	Point getCameraResolution() {
		return cameraResolution;
	}

	Point getScreenResolution() {
		return screenResolution;
	}

	int getPreviewFormat() {
		return previewFormat;
	}

	String getPreviewFormatString() {
		return previewFormatString;
	}

	private Point getCameraResolution(Camera.Parameters parameters,
									  Point screenResolution) {

		String previewSizeValueString = parameters.get("preview-size-values");
		// saw this on Xperia
		if (previewSizeValueString == null) {
			previewSizeValueString = parameters.get("preview-size-value");
		}

		Point cameraResolution = null;

		if (previewSizeValueString != null) {
			Log.i(TAG, "preview-size-values parameter: "
					+ previewSizeValueString);
			cameraResolution = findBestPreviewSizeValue(previewSizeValueString,
					screenResolution);
		}

		if (cameraResolution == null) {
			// Ensure that the camera resolution is a multiple of 8, as the
			// screen may not be.
			cameraResolution = new Point((screenResolution.x >> 3) << 3,
					(screenResolution.y >> 3) << 3);
		}
		cameraResolution = new Point(RecognizeLightActivity.mPreviewHeight, RecognizeLightActivity.mPreviewWidth);
		return cameraResolution;
	}

	class PreviewSize{
		Point point;
		float rate;

		public PreviewSize(int x, int y) {
			point = new Point(x, y);
			rate = (float)x / (float)y;
		}
	}

	private Point findBestPreviewSizeValue(
			CharSequence previewSizeValueString, final Point screenResolution) {
		final float screenRate = (float)screenResolution.x/(float)screenResolution.y;
		List<PreviewSize> list = new ArrayList<PreviewSize>();
		for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {

			previewSize = previewSize.trim();
			int dimPosition = previewSize.indexOf('x');
			if (dimPosition < 0) {
				Log.w(TAG, "Bad preview-size: " + previewSize);
				continue;
			}
			try {
				int x = Integer.parseInt(previewSize.substring(0, dimPosition));
				int y = Integer.parseInt(previewSize.substring(dimPosition + 1));
				list.add(new PreviewSize(x, y));
			} catch (NumberFormatException nfe) {
				Log.w(TAG, "Bad preview-size: " + previewSize);
				continue;
			}
		}
		if (list.size() > 0) {
			Collections.sort(list, new Comparator<PreviewSize>() {
				private float TARGET_EQUAL = 0.09f;
				private float EQUAL = 0.01f;

				@Override
				public int compare(PreviewSize lhs, PreviewSize rhs) {
					float lRate = Math.abs(lhs.rate - screenRate);
					float rRate = Math.abs(rhs.rate - screenRate);
					if ((lRate < TARGET_EQUAL && rRate < TARGET_EQUAL) || Math.abs(lRate - rRate) < EQUAL) {
						int lDiff = Math.abs(lhs.point.x - screenResolution.x) +
								Math.abs(lhs.point.y - screenResolution.y);
						int rDiff = Math.abs(rhs.point.x - screenResolution.x) +
								Math.abs(rhs.point.y - screenResolution.y);
						return lDiff - rDiff;
					}
					return (lRate > rRate) ? 1 : -1;
				}
			});
			/*
			for(int i=0; i<list.size(); i++) {
				PreviewSize item = list.get(i);
				Log.d(TAG, "item " + i + " " + item.point + " rate " + Math.abs(item.rate - screenRate));
			}
			*/
			return list.get(0).point;
		}

		return null;
	}

	private static int findBestMotZoomValue(CharSequence stringValues,
											int tenDesiredZoom) {
		int tenBestValue = 0;
		for (String stringValue : COMMA_PATTERN.split(stringValues)) {
			stringValue = stringValue.trim();
			double value;
			try {
				value = Double.parseDouble(stringValue);
			} catch (NumberFormatException nfe) {
				return tenDesiredZoom;
			}
			int tenValue = (int) (10.0 * value);
			if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom
					- tenBestValue)) {
				tenBestValue = tenValue;
			}
		}
		return tenBestValue;
	}

	private void setFlash(Camera.Parameters parameters) {
		// FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
		// And this is a hack-hack to work around a different value on the
		// Behold II
		// Restrict Behold II check to Cupcake, per Samsung's advice
		// if (Build.MODEL.contains("Behold II") &&
		// CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
		if (Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3) { // 3
			// //
			// =
			// //
			// Cupcake
			parameters.set("flash-value", 1);
		} else {
			parameters.set("flash-value", 2);
		}
		// This is the standard setting to turn the flash off that all devices
		// should honor.
		parameters.set("flash-mode", "off");
	}

	private void setZoom(Camera.Parameters parameters) {

		String zoomSupportedString = parameters.get("zoom-supported");
		if (zoomSupportedString != null
				&& !Boolean.parseBoolean(zoomSupportedString)) {
			return;
		}

		int tenDesiredZoom = TEN_DESIRED_ZOOM;

		String maxZoomString = parameters.get("max-zoom");
		if (maxZoomString != null) {
			try {
				int tenMaxZoom = (int) (10.0 * Double
						.parseDouble(maxZoomString));
				if (tenDesiredZoom > tenMaxZoom) {
					tenDesiredZoom = tenMaxZoom;
				}
			} catch (NumberFormatException nfe) {
				Log.w(TAG, "Bad max-zoom: " + maxZoomString);
			}
		}

		String takingPictureZoomMaxString = parameters
				.get("taking-picture-zoom-max");
		if (takingPictureZoomMaxString != null) {
			try {
				int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
				if (tenDesiredZoom > tenMaxZoom) {
					tenDesiredZoom = tenMaxZoom;
				}
			} catch (NumberFormatException nfe) {
				Log.w(TAG, "Bad taking-picture-zoom-max: "
						+ takingPictureZoomMaxString);
			}
		}

		String motZoomValuesString = parameters.get("mot-zoom-values");
		if (motZoomValuesString != null) {
			tenDesiredZoom = findBestMotZoomValue(motZoomValuesString,
					tenDesiredZoom);
		}

		String motZoomStepString = parameters.get("mot-zoom-step");
		if (motZoomStepString != null) {
			try {
				double motZoomStep = Double.parseDouble(motZoomStepString
						.trim());
				int tenZoomStep = (int) (10.0 * motZoomStep);
				if (tenZoomStep > 1) {
					tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
				}
			} catch (NumberFormatException nfe) {
				// continue
			}
		}

		// Set zoom. This helps encourage the user to pull back.
		// Some devices like the Behold have a zoom parameter
		if (maxZoomString != null || motZoomValuesString != null) {
			parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
		}

		// Most devices, like the Hero, appear to expose this zoom parameter.
		// It takes on values like "27" which appears to mean 2.7x zoom
		if (takingPictureZoomMaxString != null) {
			parameters.set("taking-picture-zoom", tenDesiredZoom);
		}
	}

	public static int getDesiredSharpness() {
		return DESIRED_SHARPNESS;
	}

}
