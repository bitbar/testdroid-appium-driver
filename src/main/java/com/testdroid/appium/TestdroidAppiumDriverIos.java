package com.testdroid.appium;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.ios.IOSDriver;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.interactions.HasTouchScreen;
import org.openqa.selenium.interactions.TouchScreen;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteTouchScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

/**
 * Testdroid Appium driver
 *
 * https://git@github.com/bitbar/testdroid-appium-driver
 *
 * Usage:
 *
 * @TODO
 *
 * @author Henri Kivelä <henri.kivela@bitbar.com>
 */

public class TestdroidAppiumDriverIos extends IOSDriver implements HasTouchScreen {

    public static final String CAPABILITY_TESTDROID_USERNAME = "testdroid_username";
    public static final String CAPABILITY_TESTDROID_PASSWORD = "testdroid_password";
    private static final Logger logger = LoggerFactory.getLogger(TestdroidAppiumDriverIos.class);

    public RemoteTouchScreen touch;

    public TestdroidAppiumDriverIos(URL url, DesiredCapabilities capabilities) {
        super(url, capabilities);
        touch = new RemoteTouchScreen(getExecuteMethod());
    }

    public TouchScreen getTouch() {
        return touch;
    }

    public void takeScreenshot(String filePath) throws Exception {
        File f = getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(f, new File(filePath));
        logger.info("Screenshot captured: {}", filePath);
    }
}