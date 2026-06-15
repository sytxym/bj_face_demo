package com.aeye.face.camera;

import java.util.ArrayList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccShakeDetect implements SensorEventListener{
	 
	// 速度阈值，当摇晃速度达到这值后产生作用
	private static final int SPEED_SHRESHOLD = 250;
	// 两次检测的时间间隔
	private static final int UPTATE_INTERVAL_TIME = 50;
	private SensorManager sensorManager;
	private OnShakeListener onShakeListener;
	private float lastX;
	private float lastY;
	private float lastZ;
	// 上次检测时间
	private long lastUpdateTime;
	private ArrayList<Boolean> shakeList = new ArrayList<Boolean>();
	private static final int MAX_BUFFER_NUM = 5;
	private static final int CHG_STATUS_NUM = 3;
	private static boolean shakeStatus = false;
	
	void addData(boolean data) {
		if (shakeList.size() >= MAX_BUFFER_NUM) {
			shakeList.remove(0);
		}
		shakeList.add(data);
	}
	
	boolean statusFresh(boolean data) {
		addData(data);
		int shake = 0;
		for(Boolean item : shakeList) {
			if (item) 
				shake++;
		}
		if (shakeStatus) {
			if ((MAX_BUFFER_NUM - shake) >= MAX_BUFFER_NUM) {
				shakeStatus = false;
				return true;
			}
		} else {
			if (shake >= CHG_STATUS_NUM) {
				shakeStatus = true;
				return true;
			}
		}
		return false;
	}
	
	public interface OnShakeListener {
		public void onShake(boolean status);
	}
	
	public void registerSensor(Context context, OnShakeListener listener) {
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if (sensor != null) {
				sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
			}
		}
		onShakeListener = listener;
	}

	public void unregisterSensor() {
		sensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// 现在检测时间
		long currentUpdateTime = System.currentTimeMillis();
		// 两次检测的时间间隔
		long timeInterval = currentUpdateTime - lastUpdateTime;
		// 判断是否达到了检测时间间隔
		if (timeInterval < UPTATE_INTERVAL_TIME) return;
		// 现在的时间变成last时间
		lastUpdateTime = currentUpdateTime;
		// 获得x,y,z坐标
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		
		// 获得x,y,z的变化值
		float deltaX = x - lastX;
		float deltaY = y - lastY;
		float deltaZ = z - lastZ;

		// 将现在的坐标变成last坐标
		lastX = x;
		lastY = y;
		lastZ = z;
		double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
				* deltaZ) / timeInterval * 10000;
		// 达到速度阀值，发出提示
		if (statusFresh(speed >= SPEED_SHRESHOLD)) {
			onShakeListener.onShake(shakeStatus);
		}
	}
}
