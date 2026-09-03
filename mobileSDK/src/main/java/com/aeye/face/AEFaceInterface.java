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
	 * 活体/核验结束（原生宿主使用）。
	 *
	 * @param value      SDK 内部结果码，见 {@link AEFacePack#SUCCESS} 等（向后兼容，保持不变）
	 * @param data       采集数据 JSON（含 images 等，可上传后台核验）
	 * @param resultCode 三端统一结果码（字符串，含前导零）：核验通过 {@code "0"}、
	 *                   未通过 {@code "0414009"}（{@code resultMsg}/{@code msg} 为 failtype）、核验超时 {@code "0414010"}、
	 *                   用户取消 {@code "0414011"}、摄像头异常 {@code "0414012"}、
	 *                   设备不安全 {@code "0414013"}、选择其他核验方式 {@code "0414014"}。
	 *                   {@code data} 顶层同时带 {@code code}/{@code msg}（与 resultCode/resultMsg 同值）。
	 */
	public void onFinish(int value, String data, String resultCode);
};