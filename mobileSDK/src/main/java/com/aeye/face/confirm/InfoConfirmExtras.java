package com.aeye.face.confirm;

/**
 * {@link InfoConfirmActivity} 通过 Intent 传入的字段名；宿主在拉取甲方接口后填入。
 */
public final class InfoConfirmExtras {
    public static final String EXTRA_REAL_NAME = "aeye.confirm.real_name";
    public static final String EXTRA_REGION = "aeye.confirm.region";
    public static final String EXTRA_ID_TYPE = "aeye.confirm.id_type";
    public static final String EXTRA_ID_NUMBER = "aeye.confirm.id_number";
    /** 可选：主标题，不传则用默认文案 */
    public static final String EXTRA_MAIN_TITLE = "aeye.confirm.main_title";
    /** 可选：副标题 */
    public static final String EXTRA_SUB_TITLE = "aeye.confirm.sub_title";
    /** 可选：失败页展示文案（活体失败时 RecognizeActivity 可读，当前由 SDK 默认） */
    public static final String EXTRA_FAIL_DETAIL = "aeye.confirm.fail_detail";

    private InfoConfirmExtras() {
    }
}
