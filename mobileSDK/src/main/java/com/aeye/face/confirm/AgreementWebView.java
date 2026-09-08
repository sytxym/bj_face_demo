package com.aeye.face.confirm;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * 暴露纵向滚动信息，用于判断协议页是否滑到底。
 */
public class AgreementWebView extends WebView {

    public AgreementWebView(Context context) {
        super(context);
    }

    public AgreementWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AgreementWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isScrolledToBottom() {
        int range = computeVerticalScrollRange();
        int extent = computeVerticalScrollExtent();
        int offset = computeVerticalScrollOffset();
        return range <= extent + 8 || offset + extent >= range - 24;
    }
}
