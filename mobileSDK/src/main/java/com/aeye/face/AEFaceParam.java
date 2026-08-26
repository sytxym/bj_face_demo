package com.aeye.face;

import android.app.Application;
import android.content.res.Resources;

import com.aeye.sdk.AEFaceAlive;
import com.sdk.core.R;

public class AEFaceParam {
    /**
     * 模板更新阈值
     */
    public static final String ModelUpdateThresh = "ModelUpdateThresh";
    /**
     * 模板更新开关  0关闭  1启用
     */
    public static final String ModelUpdateSwitch = "ModelUpdateSwitch";
    /**
     * 建模超时时间  （单位秒）
     */
    public static final String ModelOverTime = "ModelOverTime";
    /**
     * 认证超时时间  （单位秒）
     */
    public static final String RecogOverTime = "RecogOverTime";
    /**
     * 质量评估开关  0关闭1启用
     */
    public static final String QualitySwitch = "QualitySwitch";
    /**
     * 质量评估置信度
     */
    public static final String QualityConfidenceLevel = "QualityConfidenceLevel";
    /**
     * 多人脸检测多人脸检测开关
     */
    public static final String MutifaceSwitch = "MutifaceSwitch";
    /**
     * 连续未检测人脸数
     */
    public static final String ContinueFailDetectNum = "ContinueFailDetectNum";
    /**
     * 连续检测人脸数
     */
    public static final String ContinueSuccessDetectNum = "ContinueSuccessDetectNum";
    /**
     * 活体开关
     */
    public static final String AliveSwitch = "AliveSwitch";
    /**
     * 首个活体动作
     */
    public static final String AliveFirstMotion = "AliveFirstMotion";
    /**
     * 活体动作集合
     */
    public static final String AliveMotion = "AliveMotion";
    /**
     * 活体动作个数
     */
    public static final String AliveMotionNum = "AliveMotionNum";
    /**
     * 活体每个动作取图个数
     */
    public static final String AliveMotionPicNum = "AliveMotionPicNum";
    /**
     * 固定2个活体动作个数
     */
    public static final String AliveFixMotionSwitch = "AliveFixMotionSwitch";
    /**
     * 单个活体超时时间
     */
    public static final String SingleAliveMotionTime = "SingleAliveMotionTime";
    /**
     * 活体等级,等级越高越难
     */
    public static final String AliveLevel = "AliveLevel";

    /**
     * 遮挡判断
     */
    public static final String AliveMask = "AliveMask";
    public static final int ALIVE_MASK_DEFAULT = 0;
    /**
     * 数据加密类型
     */
    public static final String EnCryptType = "EnCryptType";

    public static final int ENCRYPT_TYPE_NULL = 0;
    public static final int ENCRYPT_TYPE_SM4 = 1;
    public static final int ENCRYPT_TYPE_AES = 2;

    /**
     * 超时原因提示开关
     */
    public static final String TimeoutNotice = "TimeoutNotice";
    /**
     * 人脸出现触发活体计时
     */
    public static final String FaceStartTimer = "FaceStartTimer";
    /**
     * 准备倒计时显示
     */
    public static final String ShowPrepare = "ShowPrepare";
    /**
     * 显示引导界面
     */
    public static final String ShowIntroduce = "ShowIntroduce";
    /**
     * 严格模式
     */
    public static final String StrictMode = "StrictMode";
    /**
     * 最大亮度
     */
    public static final String MaxBrightness = "MaxBrightness";
    /**
     * 白色背景
     */
    public static final String WhiteBackgroud = "WhiteBackgroud";
    /**
     * 获取正脸图
     */
    public static final String CaptureStraight = "CaptureStraight";
    /**
     * 简单动画效果
     */
    public static final String SimpleAnim = "SimpleAnim";
    /**
     * 单个活体的阈值
     */
    public static final String SingleAliveMotionThresh = "SingleAliveMotionThresh";
    /**
     * 建模多角度开关
     */
    public static final String ModelMutiAngleSwitch = "ModelMutiAngleSwitch";
    /**
     * 认证多角度开关
     */
    public static final String RecogMutiAngleSwitch = "RecogMutiAngleSwitch";
    /**
     * 采集照片数量
     */
    public static final String FetchImageNum = "FetchImageNum";
    /**
     * 压缩开关  0关闭1启用
     */
    public static final String ZipSwitch = "ZipSwitch";
    /**
     * 语音开关  0关闭1启用
     */
    public static final String VoiceSwitch = "VoiceSwitch";
    /**
     * 摄像头开关  0后置 1前置
     */
    public static final String CameraId = "CameraId";
    /**
     * 显示旋转角度
     */
    public static final String DisplayRotate = "DisplayRotate";
    /**
     * 算法旋转
     */
    public static final String DecodeRotate = "DecodeRotate";
    /**
     * 女性皮肤
     */
    public static final String LadyAnimIcon = "LadyAnimIcon";
    /**
     * 人脸检测区域显示
     */
    public static final String ShowFaceRect = "ShowFaceRect";
    /**
     * 是否显示返回按钮
     */
    public static final String ShowBackButton = "ShowBackButton";
    /**
     * 标题背景颜色
     */
    public static final String ColorTopBarBg = "ColorTopBarBg";
    /**
     * 底部背景颜色
     */
    public static final String ColorBottomBarBg = "ColorBottomBarBg";
    /**
     * 主题风格 0 人脸  1方框
     */
    public static final String Theme = "Theme";
    /**
     * 标题字体大小
     */
    public static final String SizeTopTitle = "SizeTopTitle";
    /**
     * 标题字符串
     */
    public static final String TitleTopBar = "toptitle";

    /**
     * 颜色序列1
     */
    public static final String ColorInfo1 = "colorinfo1";

    /**
     * 颜色序列2
     */
    public static final String ColorInfo2 = "colorinfo2";
    /**
     * 颜色序列3
     */
    public static final String ColorInfo3 = "colorinfo3";
    /**
     * 闪光活体序列号
     */
    public static final String Colorseq= "colorseq";
    /**
     * 活体模式
     * 0 ： 动作活体
     * 1：炫彩活体
     * 2： 动作+炫彩活体
     * 3：静默活体（仅检测人脸采集，无动作指令；Demo/宿主通过 ALIVEMODE 区分）
     */
    public static final String ALIVEMODE= "ALIVEMode";

    public static final int ALIVEMODE_MOTION = 0;
    public static final int ALIVEMODE_LIGHT = 1;
    public static final int ALIVEMODE_MOTION_LIGHT = 2;
    public static final int ALIVEMODE_SILENT = 3;

    public static final String AliveType = "AliveType";
    public static final int ALIVE_TYPE_POSE = 0;
    public static final int ALIVE_TYPE_OTHER = 1;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_ERROR_CANCEL = 1;
    public static final int CODE_ERROR_TIME_OUT = 2;
    public static final int CODE_ERROR_ALIVE_FAILED = 3;

    /**
     * 2026.2.9 增加人脸检测区域居中参数roi_center，只人脸在检测区域才采集
     */
    public static final String ROI_CenterSwitch = "ROI_CenterSwitch";

    /**2026.3.31 是否横屏，默认是竖屏0, 横屏是1**/
    public static final String IS_LAND_Switch = "isLandSwitch";

    /**
     * 宿主「选择认证方式」首页 Activity 全类名（如 com.xxx.MainActivity）。
     * 用户点击活体失败页「其他核验方式」时，将 CLEAR_TOP 回到该页并结束 SDK 人脸流程内所有 Activity。
     */
    public static final String HostHomeActivity = "HostHomeActivity";


    public static String getCodeStr(int code) {
        if (code == CODE_SUCCESS) {
            return "成功";
        } else {
            return "失败";
        }
    }

    public static String getReasonStr(int code) {
        String result = "";
        switch (code) {
            case CODE_ERROR_CANCEL:
                result = "用户取消";
                break;
            case CODE_ERROR_TIME_OUT:
                result = "超时";
                break;
            case CODE_ERROR_ALIVE_FAILED:
                result = "活体检测失败";
                break;
            case  AEFacePack.ERROR_DANGER_DEVICE:
                result ="本机设备存有隐患，请更换安全手机！";
                break;
            case AEFacePack.ERROR_CAMERA:
                result = "摄像头打开失败";
                break;
            case CODE_SUCCESS:
            default:
                break;
        }
        return result;
    }

    public static String getPoseStr(int id) {
        String result = "";
        if (id == AEFaceAlive.POSE_FACE_UP) {
            result = "抬头";
        } else if (id == AEFaceAlive.POSE_FACE_DOWN) {
            result = "低头";
        } else if (id == AEFaceAlive.POSE_FACE_SHAKE) {
            result = "摇头";
        } else if (id == AEFaceAlive.POSE_MOUTH_OPEN) {
            result = "张嘴";
        } else if (id == AEFaceAlive.POSE_EYE_BLINK) {
            result = "眨眼";
        }
        return result;
    }
}

