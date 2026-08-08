package com.aeye.face.uitls;

import com.aeye.face.api.model.ColorResponseBean;

import java.io.Serializable;

public class ColorInfo implements Serializable {
    private int r;
    private int g;
    private int b;
    private int light;
    private String name;

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
   
     * 替代原 DemoApplication.getColorInfo() 方法
     *
     * @param bean 服务端返回的颜色数据 (BGR格式)
     * @param name 颜色显示名称 (如"第二屏")
     * @return ColorInfo对象 (亮度默认255)
     * @since 2026-06-29
     */
    public static ColorInfo fromColor1Bean(ColorResponseBean.ColorsBean.Color1Bean bean, String name) {
        ColorInfo color = new ColorInfo();
        color.setB(bean.getC1());  // c1 → Blue
        color.setG(bean.getC2());  // c2 → Green
        color.setR(bean.getC3());  // c3 → Red
        color.setName("默认颜色" + name);
        color.setLight(255);
        return color;
    }

}
