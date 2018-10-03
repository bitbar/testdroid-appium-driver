package com.testdroid.appium.model;

/**
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
public class UploadStatus {

    private String message;

    private Integer uploadCount;

    private Integer expiresIn;

    private UploadedFile uploads;

    public UploadStatus() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getUploadCount() {
        return uploadCount;
    }

    public void setUploadCount(Integer uploadCount) {
        this.uploadCount = uploadCount;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UploadedFile getUploads() {
        return uploads;
    }

    public void setUploads(UploadedFile uploads) {
        this.uploads = uploads;
    }
}
