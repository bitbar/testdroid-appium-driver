package com.testdroid.appium.model;

/**
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
public class AppiumResponse {

    private Integer status;

    private String sessionId;

    private UploadStatus value;

    public AppiumResponse() {
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public UploadStatus getValue() {
        return value;
    }

    public void setValue(UploadStatus value) {
        this.value = value;
    }
}
