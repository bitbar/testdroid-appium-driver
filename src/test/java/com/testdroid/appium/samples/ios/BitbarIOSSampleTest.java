package com.testdroid.appium.samples.ios;

import com.testdroid.appium.TestdroidAppiumClient;
import com.testdroid.appium.TestdroidAppiumDriver;
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
 * Example Appium iOS application test.
 *
 * @author Henri Kivelä <henri.kivela@bitbar.com>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BitbarIOSSampleTest {
    private static final String BUNDLE_ID = "com.bitbar.testdroid.BitbarIOSSample";
    private static final String SAMPLE_APP_PATH = "src/test/resources/BitbarIOSSample.ipa";

    private static final Logger logger = LoggerFactory.getLogger(BitbarIOSSampleTest.class);

    private static TestdroidAppiumClient client;
    private static TestdroidAppiumDriver wd;

    @BeforeClass
    public static void setUp() throws Exception {
        client = new TestdroidAppiumClient();
        // You can override the the in testdroid.properties or give file UUID
        client.setAppFile(new File(SAMPLE_APP_PATH));
        client.setBundleId(BUNDLE_ID);
        wd = client.getDriver();
    }

    @AfterClass
    public static void tearDown() {
        client.quit();
    }

    @Test
    public void mainPageTest() throws IOException, InterruptedException {
        wd.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        wd.findElement(By.xpath("//UIAApplication[1]/UIAWindow[1]/UIAButton[1]")).click();
        screenshot("1.png");
        wd.findElement(By.name("userName")).sendKeys("John Doe");
        screenshot("2.png");
        wd.findElement(By.name("return")).click();
        wd.findElement(By.name("sendAnswer")).click();
        screenshot("3.png");
    }

    private static File screenshot(String name) {
        return client.screenshot(name);
    }

}
