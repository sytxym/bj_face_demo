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
	 * @param resultCode 异常/结果编码（字符串，含前导零），业务 App 以本字段为准。
	 *                   核验通过 {@code "0414000"}；相机无权限 {@code "0414013"}；Root/越狱 {@code "0414001"}；
	 *                   无人脸超时 {@code "0412002"}；多人脸 {@code "0414003"}；过暗 {@code "0414004"}；
	 *                   过亮 {@code "0414005"}；单动作超时 {@code "0414008"}；出框超时 {@code "0414009"}；
	 *                   活体算法失败 {@code "04014010"}；网络超时 {@code "0114011"}；服务异常 {@code "0419001"}。
	 *                   查询核验 {@code /faceRecord/queryVerifyResult} 业务信封 {@code code}/{@code message}
	 *                   （如 {@code 0415001}～{@code 0415004}、{@code 0412006}），不用网关最外层字段、不覆盖。
	 *                   用户取消 {@code "0414011"}（用户已取消，请稍后重试）、
	 *                   相机设备异常 {@code "0414012"}（相机暂时无法使用，请稍后重试）、
	 *                   其他核验方式 {@code "0414014"}。
	 *                   {@code resultMsg} 为对应提示（失败页倒数第二栏文案）；{@code data} 顶层同时带
	 *                   {@code resultCode}/{@code resultMsg}/{@code code}/{@code msg}。
	 */
	public void onFinish(int value, String data, String resultCode, String resultMsg);
};