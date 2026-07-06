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
	 * @param value SDK 内部结果码，见 {@link AEFacePack#SUCCESS} 等
	 * @param data  采集数据 JSON；自 vNext 起含 {@code uniResult} 字段供 UniApp 解析，原生可忽略
	 */
	public void onFinish(int value, String data);

	/**
	 * UniApp 统一结果回调，格式 {@code {"code":0,"message":"认证成功"}}。
	 * 默认空实现；uni 插件重写此方法，原生宿主无需处理。
	 */
	default void onUniFinish(String uniResultJson) {
	}
};