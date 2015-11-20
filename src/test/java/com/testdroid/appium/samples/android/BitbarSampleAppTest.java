package com.testdroid.appium.samples.android;

import com.testdroid.appium.TestdroidAppiumClient;
import com.testdroid.appium.TestdroidAppiumDriverAndroid;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Example Appium Android application test.
 *
 * @author Henri Kivelä <henri.kivela@bitbar.com>
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BitbarSampleAppTest {
    private static final String ANDROID_PACKAGE = "com.bitbar.testdroid";
    private static final String ANDROID_ACTIVITY = ".BitbarSampleApplicationActivity";
    private static final String SAMPLE_APP_PATH = "src/test/resources/BitbarSampleApp.apk";

    private static final Logger logger = LoggerFactory.getLogger(BitbarSampleAppTest.class);

    private static TestdroidAppiumClient client;
    private static TestdroidAppiumDriverAndroid wd;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new TestdroidAppiumClient();
        // You can override the the in testdroid.properties or give file UUID
        client.setAppFile(new File(SAMPLE_APP_PATH));
        client.setAndroidPackage(ANDROID_PACKAGE);
        client.setAndroidActivity(ANDROID_ACTIVITY);
        // Wait one hour for free device
        client.setDeviceWaitTime(3600);

        wd = client.getAndroidDriver();
        wd.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void tearDown() {
        client.quit();
    }

    @Test
    public void mainPageTest() throws IOException, InterruptedException {
        screenshot("1.png");
        if (client.getTestdroidTarget().equals(TestdroidAppiumClient.TESTDROID_TARGET_SELENDROID)) {
            wd.findElement(By.xpath("//RadioButton[2]")).click();
            screenshot("2.png");
            wd.findElement(By.xpath("//EditText[1]")).sendKeys("John Doe");
            screenshot("3.png");
            wd.navigate().back();
            wd.findElement(By.xpath("//Button[1]")).click();
        }
        else {
            wd.findElement(By.xpath("//android.widget.RadioButton[2]")).click();
            screenshot("2.png");
            wd.findElement(By.xpath("//android.widget.EditText[1]")).sendKeys("John Doe");
            screenshot("3.png");
            wd.navigate().back();
            wd.findElement(By.xpath("//android.widget.Button[1]")).click();
        }
        screenshot("4.png");
    }

    private static File screenshot(String name) {
        return client.screenshot(name);
    }

}
