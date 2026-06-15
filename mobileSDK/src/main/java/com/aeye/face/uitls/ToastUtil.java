package com.aeye.face.uitls;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by admin on 2019/6/3.
 */

public class ToastUtil {
    public static Toast toast;

    public static void showToast(Context context, String string) {
        if (toast == null) {
            // 如果Toast对象为空了，那么需要创建一个新的Toast对象
            toast = Toast.makeText(context.getApplicationContext(), string, Toast.LENGTH_LONG);
        } else {
            // 如果toast对象还存在，那么就不再创建新的Toast对象
            toast.setText(string);
        }
        // 最后调用show方法吐丝
        toast.show();
    }

}
