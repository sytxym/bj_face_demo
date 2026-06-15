package com.aeye.face.uitls;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataUtil {


    public static List<ColorInfo> getRandomList(List<ColorInfo> src) {
        List<ColorInfo> resultList = new ArrayList<>();
        if (src != null) {
            int size = src.size();
            int[] sequence = getSequence(size);
            for (int i = 0; i < size; i++) {
                int index = sequence[i];
                resultList.add(src.get(index));
            }
        }
        return resultList;
    }

    public static int[] getSequence(int no) {
        int[] sequence = new int[no];
        for (int i = 0; i < no; i++) {
            sequence[i] = i;
        }
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < no; i++) {
            int p = random.nextInt(no);
            int tmp = sequence[i];
            sequence[i] = sequence[p];
            sequence[p] = tmp;
        }
        random = null;
        return sequence;
    }

    public static ColorInfo getColorInfo(int r, int g, int b, String colorName) {
        ColorInfo colorInfo = new ColorInfo();
        colorInfo.setR(r);
        colorInfo.setG(g);
        colorInfo.setB(b);
        colorInfo.setName(colorName);
        colorInfo.setLight(255);
        return colorInfo;
    }


}
