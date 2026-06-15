package com.xym.testface.bean;

public interface HttpInterface {

    public int onResponse(LiveResponseBean liveResponseBean);


    public int onPostFailed(int code, String message);

}
