package com.aeye.face.api.model;

import com.alibaba.fastjson.JSON;

/**
 * 炫彩颜色序列接口响应。
 */
public class ColorResponseBean {

    private String seq;
    private int result;
    private String info;
    private DebugInfoBean debugInfo;
    private ColorsBean colors;

    public static ColorResponseBean parse(String str) {
        return JSON.parseObject(str, ColorResponseBean.class);
    }

    public static class DebugInfoBean {
        private String elapse;
        private String path;

        public String getElapse() {
            return elapse;
        }

        public void setElapse(String elapse) {
            this.elapse = elapse;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class ColorsBean {
        private Color1Bean color1;
        private Color1Bean color2;
        private Color1Bean color3;
        /**
         * 颜色序列号。服务端可能返回字符串或数字（如 {@code 2088601214}），
         * 用 Object 承接后再统一转成字符串。
         */
        private Object sequnce;

        public String getSequnce() {
            return sequnce == null ? null : String.valueOf(sequnce);
        }

        public void setSequnce(Object sequnce) {
            this.sequnce = sequnce;
        }

        /** 单个颜色值 (BGR格式: c1=B, c2=G, c3=R) */
        public static class Color1Bean {
            private int c1;
            private int c2;
            private int c3;

            public int getC1() {
                return c1;
            }

            public void setC1(int c1) {
                this.c1 = c1;
            }

            public int getC2() {
                return c2;
            }

            public void setC2(int c2) {
                this.c2 = c2;
            }

            public int getC3() {
                return c3;
            }

            public void setC3(int c3) {
                this.c3 = c3;
            }
        }

        public Color1Bean getColor1() {
            return color1;
        }

        public void setColor1(Color1Bean color1) {
            this.color1 = color1;
        }

        public Color1Bean getColor2() {
            return color2;
        }

        public void setColor2(Color1Bean color2) {
            this.color2 = color2;
        }

        public Color1Bean getColor3() {
            return color3;
        }

        public void setColor3(Color1Bean color3) {
            this.color3 = color3;
        }
    }

    public String getSeq() {
        return seq;
    }

    public void setSeq(String seq) {
        this.seq = seq;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public DebugInfoBean getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(DebugInfoBean debugInfo) {
        this.debugInfo = debugInfo;
    }

    public ColorsBean getColors() {
        return colors;
    }

    public void setColors(ColorsBean colors) {
        this.colors = colors;
    }

    /** 成功回调: 返回颜色序列和流水号 */
    public interface Response {
        int onResponse(ColorsBean colors, String seq);
    }

    /** 失败回调: 返回错误码和错误消息 */
    public interface WrongDeal {
        int onPostFailed(int code, String message);
    }
}
