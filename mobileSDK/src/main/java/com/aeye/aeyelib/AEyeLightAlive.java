package com.aeye.aeyelib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.aeye.face.uitls.FLogUtil;
import com.aeye.sdk.AEFaceAlive;


public class AEyeLightAlive {
    private String TAG = "AEyeLightAlive";
    private byte[] alignData = new byte[256 * 5 * 256 * 3];
    private byte[] lastData = new byte[256 * 6 * 256 * 3];
    private int[] imgInfo = new int[]{640, 480};
    private volatile boolean isInit = false;
    private static AEyeLightAlive instance;


    public static AEyeLightAlive getInstance() {
        if (instance == null) {
            synchronized (AEyeLightAlive.class) {
                if (instance == null) {
                    instance = new AEyeLightAlive();
                }
            }
        }
        return instance;
    }

//    public int AEYE_AliveInit(Context context) {
//        int ret;
//        if (!isInit) {
//            Bitmap bitmapMask = BitmapFactory.decodeFile(FileUtil.getFilePath(context, "ovalMask.jpg"));
//            byte[] rgbImgMask = RGBUtil.bitmap2BGR(bitmapMask);
//            imgInfo[0] = bitmapMask.getWidth();
//            imgInfo[1] = bitmapMask.getHeight();
//            Log.e(TAG, "init t1=" + System.currentTimeMillis());
//            ret = ALightNative.getInstance().Init(imgInfo, rgbImgMask);
//            if (ret == 0) {
//                isInit = true;
//            }
//        } else {
//            Log.e(TAG, "already init");
//            ret = 0;
//        }
//        Log.e(TAG, "AEYE_AliveInit init end isInit=" + isInit);
//        return ret;
//    }

    public int AEYE_AliveInit(Context context,Bitmap bitmapMask) {
        int ret;
        if (!isInit) {
//            Bitmap bitmapMask = BitmapFactory.decodeFile(FileUtil.getFilePath(context, "ovalMask.jpg"));
            byte[] rgbImgMask = RGBUtil.bitmap2BGR(bitmapMask);
            imgInfo[0] = bitmapMask.getWidth();
            imgInfo[1] = bitmapMask.getHeight();
            Log.e(TAG, "init t1=" + System.currentTimeMillis());
            ret = ALightNative.getInstance().Init(imgInfo, rgbImgMask,256,5,0.95f,new int[]{1080,1920},5
            ,480,260,256,4,83,24,70,-1.f,1);
            if (ret == 0) {
                isInit = true;
            }
        } else {
            Log.e(TAG, "already init");
            ret = 0;
        }
        Log.e(TAG, "AEYE_AliveInit init end isInit=" + isInit);
        return ret;
    }
    /*********************************************
     * unInit alg and device
     * *******************************************/
    public int AEYE_AliveDestroy() {
        Log.e(TAG, "AEYE_AliveDestroy");
        int ret = -1;
        if(isInit) {
            ret = ALightNative.getInstance().Destroy();
            if (ret == 0) {
                isInit = false;
            }
        }
        return ret;
    }

    /*********************************************
     * get version string
     * *******************************************/
    public String AEYE_GetVersion() {
        String version = ALightNative.getInstance().GetVersion();
        return version;
    }


    public int rgb2intColor(int[] rgb) {
        int color = (rgb[0] << 16) + (rgb[1] << 8) + rgb[2];
        return color;
    }

    /**
     * 插入闪光中的图片
     * @param rgbImg        i->image
     * @param color         i-2->color
     * @param state         i-2->state
     * @param isNotBoundary
     * @param quality
     *
     * @return
     */
    public int AEYE_SetImageData(byte[] rgbImg, int color, int state, boolean isNotBoundary, int[] quality,int frameId,int mRotate) {
        Log.e(TAG, "AEYE_SetImageData isInit=" + isInit);
        if (!isInit) {
            throw new RuntimeException("AEyeLightAlive not init");
        }
        int ret,rotate = 0;
        //算法要求， 小于0的乘以-1， 大于0的用360减
        if(mRotate<0){
//            if(mRotate/90==-1){
//                rotate = 270;
//            }else if(mRotate /90 == 2){
//                rotate = 180;
//            }else if(mRotate/90 == 3){
//                rotate = 90;
//            }
            rotate = mRotate*(-1);
        }else{
            rotate = 360 -mRotate;
        }
        //rotate=[0, 90, 180, 270]
//        Log.e("LIULU"," rotate : "+rotate);
        ret = ALightNative.getInstance().InsertImage(rgbImg, color, state, isNotBoundary, quality,frameId,1,rotate);
        return ret;

    }

    public Bitmap AEYE_CurrentSetImageData(int frameId) {
        byte[] lastInsertImg = new byte[1920*1080*3];
        ALightNative.getInstance().GetCurrentInsertImage(lastInsertImg,frameId);

       Bitmap bitmap =  getInsertBestBitmap(lastInsertImg);
//        Bitmap bitmap = BGRtoBitmap(lastInsertImg,1080,1920);
        return  bitmap;
    }

    public int[] AEYE_CurrentPix(int frameId) {
        byte[] lastInsertImg = new byte[1920*1080*3];
        ALightNative.getInstance().GetCurrentInsertImage(lastInsertImg,frameId);

        int[] pix =  getInsertBestBGR(lastInsertImg);
        return  pix;
    }

    /*
     * byte[] data保存的是纯RGB的数据，而非完整的图片文件数据
     */
    static public Bitmap BGRtoBitmap(byte[] data, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int row = height - 1, col = width-1;
        for (int i = data.length-1; i >= 3; i -= 3) {
            int color = data[i-2] & 0xFF;
            color += (data[i-1] << 8) & 0xFF00;
            color += ((data[i]) << 16) & 0xFF0000;
            bmp.setPixel(col--, row, color);
            if (col < 0) {
                col = width-1;
                row--;
            }
        }
        return bmp;
    }
    /**
     *
     * @param keyPointX
     * @param keyPointY
     * @param frameId
     * return  *  0：成功
     *  *  1：frameId不对
     */
    public  int  AEYE_InsertKeyPoints(int[] keyPointX,int[]keyPointY,int frameId){
       int ret = ALightNative.getInstance().InsertKeyPoints(keyPointX,keyPointY,frameId);
        FLogUtil.printLog("ALightNative insert keypoint : "+ret);
        return  ret;
    }


    public int AEYE_GetImageData() {
        Log.e(TAG, "AEYE_SetImageData isInit=" + isInit);
        if (!isInit) {
            throw new RuntimeException("AEyeLightAlive not init");
        }
        int ret;
        ret = ALightNative.getInstance().getImage(alignData, lastData);
        return ret;

    }


    /**
     * AEYE_SetLastImageData之后调用
     * 获取对齐原始数据  bgr
     *
     * @return
     */
    public byte[] getAlignData() {
        return alignData;
    }

    /**
     * AEYE_SetLastImageData之后调用
     * 获取加密后对齐原始数据  bgr
     *
     * @return
     */
    public byte[] getNormalCuesData() {
        return lastData;
    }

    /**
     * 对齐数据转图片
     *
     * @return
     */
    public Bitmap getAlignBitmap() {
        return RGBUtil.RGBToBitmap(alignData, 256, 256 * 5);
    }

    /**
     * 加密后数据转图片
     *
     * @return
     */
    public Bitmap getNormalCuesBitmap() {
        return RGBUtil.RGBToBitmap(lastData, 256, 256 * 6);
    }


    /**
     * 获取到插入的最好的图
     * @param bgrImg
     * @return
     */
    public Bitmap getInsertBestBitmap(byte[] bgrImg) {
        return RGBUtil.bestBGRToBitmap(bgrImg, 1080, 1920);
    }

    public int[] getInsertBestBGR(byte[] bgrImg) {
        return RGBUtil.BGR2Pixel(bgrImg );
    }

    /** 获取最佳图的关键点 create by liulu at 2022/8/29 **/
    public float[] getBestBitLocation(Rect[] rect,Bitmap bitmap) {
        float[] qualit = AEFaceAlive.getInstance().AEYE_Alive_QualityLight(bitmap,rect[0]);
        float[] mlandMark = AEFaceAlive.getInstance().AEYE_Alive_Quality_Landmark(qualit);
        return mlandMark;
    }

    /** 插入关键点 create by liulu at 2022/8/29 **/
    public void insetKeyPoints(int frame, float[] mLandMark) {
        int[] pointX = new int[83];//所有x坐标
        int[] pointY = new int[83];//所有Y坐标
        for (int i = 0; i < mLandMark.length; i=i+2) {
            pointX[i/2] = (int) mLandMark[i];
            pointY[i/2] = (int) mLandMark[i+1];
        }
       int ret =  AEyeLightAlive.getInstance().AEYE_InsertKeyPoints(pointX,pointY,frame);
        FLogUtil.printLog(" insert point  ret : "+ret);
    }
}
