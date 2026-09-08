package com.aeye.face.confirm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.ViewTreeObserver;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.aeye.face.AEFacePack;
import com.aeye.face.ui.FaceImmersiveStatusBar;
import com.sdk.core.R;

/**
 * 扫脸认证服务协议 H5。滑到页底且倒计时结束后方可点「已阅读」返回勾选。
 */
public class AgreementWebActivity extends Activity {

    public static final String EXTRA_URL = "agree_url";

    private AgreementWebView webView;
    private Button btnRead;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int remainSec = AgreementConfig.READ_COUNTDOWN_SEC;
    private boolean countdownDone;
    private boolean reachedBottom;
    private boolean countdownStarted;
    private ViewTreeObserver.OnScrollChangedListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FaceImmersiveStatusBar.install(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aeye_activity_agreement);
        FaceImmersiveStatusBar.bindToolbar(this, findViewById(R.id.face_toolbar));
        FaceImmersiveStatusBar.bindBottomMargin(this, findViewById(R.id.btn_agree_read), 20);
        AEFacePack.getInstance().registerFaceFlowActivity(this);

        ImageView back = findViewById(R.id.btn_back);
        back.setOnClickListener(v -> finish());
        btnRead = findViewById(R.id.btn_agree_read);
        progressBar = findViewById(R.id.pb_agree);
        webView = findViewById(R.id.web_agree);
        setupWebView();

        btnRead.setEnabled(false);
        bindReadButtonText();
        btnRead.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        String url = getIntent().getStringExtra(EXTRA_URL);
        if (url == null || url.trim().isEmpty()) {
            url = AgreementConfig.getAgreementUrl();
        }
        webView.loadUrl(url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    view.loadUrl(request.getUrl().toString());
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                startCountdownIfNeeded();
                view.postDelayed(() -> checkReachedBottom(), 300);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar == null) {
                    return;
                }
                if (newProgress >= 100) {
                    progressBar.setVisibility(android.view.View.GONE);
                } else {
                    progressBar.setVisibility(android.view.View.VISIBLE);
                }
            }
        });
        scrollListener = this::checkReachedBottom;
        webView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
    }

    private void startCountdownIfNeeded() {
        if (countdownStarted) {
            return;
        }
        countdownStarted = true;
        remainSec = AgreementConfig.READ_COUNTDOWN_SEC;
        countdownDone = false;
        bindReadButtonText();
        handler.postDelayed(countdownTick, 1000);
    }

    private final Runnable countdownTick = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            remainSec--;
            if (remainSec <= 0) {
                countdownDone = true;
                remainSec = 0;
                bindReadButtonText();
                refreshReadEnabled();
                return;
            }
            bindReadButtonText();
            handler.postDelayed(this, 1000);
        }
    };

    private void checkReachedBottom() {
        if (webView == null || reachedBottom) {
            return;
        }
        if (webView.isScrolledToBottom()) {
            reachedBottom = true;
            refreshReadEnabled();
        }
    }

    private void bindReadButtonText() {
        if (btnRead == null) {
            return;
        }
        if (!countdownDone && remainSec > 0) {
            btnRead.setText(getString(R.string.info_agree_read_countdown, remainSec));
        } else {
            btnRead.setText(R.string.info_agree_read);
        }
    }

    private void refreshReadEnabled() {
        if (btnRead != null) {
            btnRead.setEnabled(countdownDone && reachedBottom);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView != null && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            if (scrollListener != null && webView.getViewTreeObserver().isAlive()) {
                webView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
            }
            webView.stopLoading();
            webView.setWebViewClient(null);
            webView.setWebChromeClient(null);
            webView.destroy();
            webView = null;
        }
        AEFacePack.getInstance().unregisterFaceFlowActivity(this);
        super.onDestroy();
    }
}
