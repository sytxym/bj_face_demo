package com.aeye.face;

public interface AEFaceInterface {
	/** 暂未使用
	 * @param value
	 * @param data
	 */
	public void onStart(int value, String data);

	/** 暂未使用
	 * @param value
	 * @param data
	 */
	public void onPrompt(int value, String data);
	
	/** 暂未使用
	 * @param value
	 * @param data
	 */
	public void onProcess(int value, String data);
	
	/**
	 * @param value  返回原因编码
	 * @param data  数据json结构
	 */
	public void onFinish(int value, String data);
};