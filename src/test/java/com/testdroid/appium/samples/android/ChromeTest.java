package com.testdroid.appium.samples.android;

import com.testdroid.appium.TestdroidAppiumClient;
import com.testdroid.appium.TestdroidAppiumDriverAndroid;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Example test for Android Chrome.
 *
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ChromeTest {
    private static TestdroidAppiumClient client;
    private static TestdroidAppiumDriverAndroid wd;

    @BeforeClass
    public static void setUp() throws Exception {
        client = new TestdroidAppiumClient();
        // File UUID must be provided, because Testdroid Appium is not able to work without apk.
        // It is enough to provide example apk UUID.
        client.setFileUUID(TestdroidAppiumClient.TESTDROID_UUID_SAMPLE_ANDROID);
        client.setPlatformName(TestdroidAppiumClient.APPIUM_PLATFORM_ANDROID);
        client.setTestdroidTarget(TestdroidAppiumClient.TESTDROID_TARGET_CHROME);
        client.setBrowserName(TestdroidAppiumClient.TESTDROID_TARGET_CHROME);
        // Wait one hour for free device
        client.setDeviceWaitTime(3600);

        wd = client.getAndroidDriver();
        wd.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
    }

    @AfterClass
    public static void tearDown() {
        if (client != null) {
            client.quit();
        }
    }

    @Test
    public void mainPageTest() throws IOException, InterruptedException {
        wd.get("http://www.google.com");
        Thread.sleep(500);
        screenshot("google.png");
    }

    private static File screenshot(String name) {
        return client.screenshot(name);
    }

}
