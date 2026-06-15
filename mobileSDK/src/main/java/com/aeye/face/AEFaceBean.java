package com.aeye.face;


import android.graphics.Bitmap;
import android.graphics.Rect;

import com.aeye.android.uitls.BitmapUtils;
import com.aeye.face.uitls.AEFaceDataUtil;
import com.aeye.face.uitls.SMUtil;

public class AEFaceBean {
    public String[] images;
    public String[] imageSign;
    public String[] compress_pic;
    public String[] compress_picSign;
    public String[] rect;
    public String[] alive;
    public String[] aliveSign;
    public int picnum;
    public String bioType;
    public String channel;
    public boolean isCompress;
    public boolean isCrypt;
    public int enCryptType;

    public  String lightData;
    public  String sequnce;

    public  String alignData;

    public Rect getRect(int index) {
        if (rect != null) {
            if (index < picnum) {
                String cur = rect[index];
                return Rect.unflattenFromString(cur);
            } else {
                return null;
            }
        }
        return null;
    }

    public Bitmap getCompressBitmap(int index) {
        if (compress_pic != null && index >= 0 && index < compress_pic.length) {
            if (picnum <= 0 || index < picnum) {
                String cur = compress_pic[index];
                if (cur != null && !cur.isEmpty()) {
                    return String2Bitmap(cur);
                }
            }
        }
        return null;
    }

    /** images[index]，index=0 为活体通过后人脸比对用正脸照 */
    public Bitmap getFaceImage(int index) {
        if (images == null || index < 0 || index >= images.length) {
            return null;
        }
        if (picnum > 0 && index >= picnum) {
            return null;
        }
        String cur = images[index];
        if (cur != null && !cur.isEmpty()) {
            return String2Bitmap(cur);
        }
        return null;
    }

    /** alive[index]，动作活体后台判活用全景图 */
    public Bitmap getAliveBitmap(int index) {
        if (alive == null || index < 0 || index >= alive.length) {
            return null;
        }
        String cur = alive[index];
        if (cur != null && !cur.isEmpty()) {
            return String2Bitmap(cur);
        }
        return null;
    }

    /** image0 正脸照，优先 images[0]，其次 compress_pic[0] */
    public Bitmap getFrontFaceBitmap() {
        Bitmap face = getFaceImage(0);
        if (face != null) {
            return face;
        }
        return getCompressBitmap(0);
    }

    public Bitmap getBitmap(int index) {
        Bitmap face = getFaceImage(index);
        if (face != null) {
            return face;
        }
        return getAliveBitmap(index);
    }

    public Bitmap getLightBitmap(int index) {
        if (images != null) {
            if (index < picnum) {
                String cur = images[index];
                if (cur != null) {
                    return SMUtil.DataSM4Decode("E3A03D4A1586F6952F0E699344D0F4E2", cur);
//                    return BitmapUtils.convertStringToBitmap(cur);
                }
            }
        }
        return null;
    }
    public String getStrImage(int index) {
        if (images != null) {
            if (index < picnum) {
                String cur = images[index];
                if (cur != null) {
                    return BitmapUtils.convertIconToString(String2Bitmap(cur));
                }
            }
        }
        return null;
    }

    public String getAlive(int index) {
        if (alive != null && index >= 0 && index < alive.length) {
            String cur = alive[index];
            if (cur != null) {
                return BitmapUtils.convertIconToString(String2Bitmap(cur));
            }
        }
        return null;
    }

    public String getLightAlive(int index) {
        if (images != null) {
            if (index < alive.length) {
                String cur = alive[index];
                if (cur != null) {
                    return cur;
                }
            }
        }
        return null;
    }

    public Bitmap String2Bitmap(String data) {
        if (enCryptType == AEFaceParam.ENCRYPT_TYPE_SM4) {
            return AEFaceDataUtil.SM42Bitmap(data);
        } else if (enCryptType == AEFaceParam.ENCRYPT_TYPE_AES) {
            return AEFaceDataUtil.AES2Bitmap(data);
        } else {
            return BitmapUtils.convertStringToBitmap(data);
        }
    }

}
