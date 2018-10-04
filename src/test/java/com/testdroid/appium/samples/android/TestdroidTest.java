package com.testdroid.appium.samples.android;

import com.testdroid.appium.TestdroidAppiumClient;
import com.testdroid.appium.TestdroidAppiumDriverAndroid;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Example Appium Android application test.
 * <p>
 * Sources for the application can be found at
 * https://github.com/bitbar/testdroid-samples/tree/master/apps/android/TestdroidSample
 *
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestdroidTest {
    private static final String ANDROID_PACKAGE = "com.testdroid.sample.android";
    private static final String ANDROID_ACTIVITY = ".MM_MainMenu";
    private static final String SAMPLE_APP_PATH = "src/test/resources/Testdroid.apk";

    private static TestdroidAppiumClient client;
    private static TestdroidAppiumDriverAndroid wd;

    @BeforeClass
    public static void setUp() throws Exception {

        client = new TestdroidAppiumClient();
        // You can override the the in testdroid.properties or give file UUID
        client.setAppFile(new File(SAMPLE_APP_PATH));
        client.setAndroidPackage(ANDROID_PACKAGE);
        client.setAndroidActivity(ANDROID_ACTIVITY);
        client.setPlatformName(TestdroidAppiumClient.APPIUM_PLATFORM_ANDROID);
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
    public void mainPageTest() throws InterruptedException {
        int i = 1;
        Thread.sleep(200); // Wait a bit to make sure the main activity is ready

        // Make this test work with older API levels. If you know you don't run your tests on older devices, this
        // is not needed.
        String xpathPrefix = "";
        if (!client.getTestdroidTarget().equals(TestdroidAppiumClient.TESTDROID_TARGET_SELENDROID)) {
            xpathPrefix = "android.widget.ScrollView[1]//android.widget.";
        }

        screenshot("screenshot-" + i++ + "-MainMenu1.png");
        // Native Activity
        wd.findElement(By.xpath("//" + xpathPrefix + "Button[1]")).click();
        Thread.sleep(200); // Wait a bit to for animation
        screenshot("screenshot-" + i++ + "-NativeActivity.png");
        wd.navigate().back();
        // Hybrid Activity
        wd.findElement(By.xpath("//" + xpathPrefix + "Button[1]")).click();
        Thread.sleep(1000); // Wait a bit to for animation and webpage
        screenshot("screenshot-" + i++ + "-HybridActivity.png");
        wd.navigate().back();
        // Function
        wd.findElement(By.xpath("//" + xpathPrefix + "Button[1]")).click();
        Thread.sleep(200); // Wait a bit to for animation
        screenshot("screenshot-" + i++ + "-Functions.png");
        wd.navigate().back();
        // Device Info
        wd.findElement(By.xpath("//" + xpathPrefix + "Button[1]")).click();
        Thread.sleep(200); // Wait a bit to for animation
        screenshot("screenshot-" + i++ + "-DeviceInfo.png");
        wd.navigate().back();
        screenshot("screenshot-" + i + "-MainMenu2.png");
    }

    private void screenshot(String name) {
        client.screenshot(name);
    }

}
