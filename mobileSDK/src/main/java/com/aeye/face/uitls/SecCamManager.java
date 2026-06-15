//package com.aeye.face.uitls;
//
//import android.content.Context;
//
///**
// * create by liulu at 2023/4/23
// **/
//public interface SecCamManager {
//    /**
//     * 此接口为能力判断接口，返回本机是否持安全摄像头能。 *
//     * @return boolean true表示支持；false表示不支持。
//     */
//    boolean isSupportSecCam();
//
//
//
//    /**
//     * 此接⼝为初始化接口，包含和系统的连接，及调用。需传入context，
//     * 初始化信息(由服务器端组装)，回调实例，初始化及打开照相机后ionfd均通过该接口回调。 *
//     * @param c 应用Activity
//     * @param s 回调函数，结果处理都在监听函数中
//     */
//    void initSecCam(Context c, SecCamCallback s);
//
//
//
//    /**
//     * 此接口为打开指定安全相机接口。
//     * 打开摄像头后，会通过SecCamResponseListener接口进行返回文件描述符。
//     *
//     * @param s 回调函数，结果处理都在监听函数中
//     */
//    void openSecCam(SecCamCallback s);
//
//
//    /**
//     * 此接口为获取安全图片接口。
//     * 需传入打开相机后返回的ionfb。以及服务器生成challenge。 *
//     * @param msg openSecCam函数中的SecCamCallback回调⽅法中返回结果的msg
//     */
//    String getSecImage(String msg);
//
//
//    /**
//     * 此接⼝为关闭安全相机接⼝。 *
//     */
//    void closeSecCam();
//
//
//
//    /*SecCamCallback与SecCamResult定义*/
//    public interface SecCamCallback {
//        void onResult(SecCamResult result);
//    }
//    public class SecCamResult {
//        private int code;
//        private String msg,version;
//        private SecCamResult(){}
//        public SecCamResult(EticErrorCodeEnum eticErrorCodeEnum){
//            this.code = eticErrorCodeEnum.getCode();
//            this.msg = eticErrorCodeEnum.getMsg();
//            this.version = "1.0";
//        }
//        public int getCode() {
//            return code;
//        }
//        public void setCode(int code) {
//            this.code = code;
//        }
//        public String getMsg() {
//            return msg;
//        }
//        public void setMsg(String msg) {
//            this.msg = msg;
//        }
//        public String getVersion() {
//            return version;
//        }
//        public void setVersion(String version) {
//            this.version = version;
//        }
//        @Override
//        public String toString() {
//            return "SecCamResult{" +
//                    "code=" + code +
//                    ", msg='" + msg + '\'' +
//                    ", version='" + version + '\'' +
//                    '}';
//        }
//    }
//
//    /*返回错误码*/
//    public class SecCamResponseMessage
//    {
//        public static final int RESULT_INIT_SUCCESS = 100;//初始化成功code
//        public static final int RESULT_INIT_AIDL_FAILURE = 101;
//        public static final int RESULT_INIT_ERROR = 102;
//        public static final int RESULT_OPEN_CAMERA_SUCCESS = 200;//开启摄像头成 功code
//        public static final int RESULT_OPEN_CAMERA_FAILURE = 201;
//        public static final int RESULT_OPEN_CAMERA_ERROR = 202;
//        public static final int RESULT_GET_IMAGE_SUCCESS = 300;//获取安全图⽚成 功code
//        public static final int RESULT_GET_IMAGE_FAILURE = 301;
//        public static final int RESULT_GET_IMAGE_ERROR = 302;
//    }
//}
