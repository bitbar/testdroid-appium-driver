package com.testdroid.appium;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;
import com.testdroid.api.*;
import com.testdroid.api.http.MultipartFormDataContent;
import com.testdroid.api.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Client for running Appium tests against Testdroid Cloud
 *
 * @author Henri Kivelä <henri.kivela@bitbar.com>
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
public class TestdroidAppiumClient {

    public static final String CLOUD_URL = "https://cloud.testdroid.com";
    public static final String CLOUD_APPIUM_URL = "http://appium.testdroid.com/wd/hub";
    public static final String APPIUM_UPLOAD_URL = "http://appium.testdroid.com/upload";

    // File for testdroid properties that will be used if no environment variables found
    private static final String TESTDROID_PROPERTIES = "testdroid.properties";

    // Environment variable names
    public static final String TESTDROID_CLOUD_URL = "testdroid.cloudUrl";
    public static final String TESTDROID_USERNAME = "testdroid.username";
    public static final String TESTDROID_PASSWORD = "testdroid.password";
    public static final String TESTDROID_PROJECT = "testdroid.project";
    public static final String TESTDROID_DEVICE = "testdroid.device";
    public static final String TESTDROID_GUI = "testdroid.gui";
    public static final String TESTDROID_APPIUM_URL = "testdroid.appiumUrl";
    public static final String TESTDROID_APPIUM_UPLOAD_URL = "testdroid.appiumUploadUrl";
    // Appium constants
    public static final String APPIUM_PLATFORM_IOS = "iOS";
    public static final String APPIUM_PLATFORM_ANDROID = "Android";
    public static final String APPIUM_AUTOMATION_NAME = "appium.automationName";
    public static final String APPIUM_APPFILE = "appium.appFile";
    // Testdroid constants
    public static final String TESTDROID_TARGET_IOS = "ios";
    public static final String TESTDROID_TARGET_ANDROID = "android";
    public static final String TESTDROID_TARGET_CHROME = "chrome";
    public static final String TESTDROID_TARGET_SAFARI = "safari";
    public static final String TESTDROID_TARGET_SELENDROID = "selendroid";
    public static final String TESTDROID_FILE_UUID = "testdroid.uuid";
    public static final String TESTDROID_UUID_SAMPLE_ANDROID = "sample/BitbarSampleApp.apk";
    public static final String TESTDROID_UUID_SAMPLE_IOS = "sample/BitbarIOSSample.ipa";

    // @TODO add rest of platforms

    static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private static final Logger logger = LoggerFactory.getLogger(TestdroidAppiumClient.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TestdroidAppiumDriver driver;

    private static DefaultAPIClient api;

    private static boolean guiEnabled = false;

    private static Thread deviceRunMonitorThread;

    private ScreenshotDisplay screenshotDisplay = null;

    private int deviceWaitTime = 120; //Optional, sets time to wait when device is in use, use 0 for no wait time
    private boolean signAppFile = true; //Optional, if set to false app file will not be resigned

    // Testdroid runtime properties

    private Properties testdroidProperties;

    private URL cloudUrl;
    private URL appiumUploadUrl;

    private String username; // Mandatory
    private String password; // Mandatory

    private String projectName; // Mandatory
    // Optional test run name, will be automatically set to device - timestamp if not found
    private String testRunName;

    private String testdroidDescription = ""; // Optional, default = ""
    private String testdroidTarget; // Mandatory
    private String testdroidLocale; // Optional, default = EN
    private String testdroidJUnitWaitTime; // Optional, default = 0, range [0,300]

    /**
     * Bundle ID for iOS - com.myapp.MyApp
     */
    private String bundleId;

    private String androidPackage;
    private String androidActivity;

    private String deviceName; // Mandatory

    // Provide either one of these
    private File appFile; // Path to local application file
    private String fileUUID; // UUID for existing application

    // Appium related

    private URL appiumUrl;
    private String platformName;
    private String automationName;
    private String browserName;

    /**
     * Constructor that configures the client using defaults and environment variables
     * <p/>
     * Set the following at least or use setters later:
     * <p/>
     * testdroid.username
     * testdroid.password
     * testdroid.projectName
     */
    public TestdroidAppiumClient() throws MalformedURLException {


        String sAppiumUrl = getProperty(TESTDROID_APPIUM_URL);
        if(sAppiumUrl != null) {
            appiumUrl = new URL(sAppiumUrl);
        } else {
            appiumUrl = new URL(CLOUD_APPIUM_URL);
        }

        String sCloudUrl = getProperty(TESTDROID_CLOUD_URL);
        if(sCloudUrl != null) {
            cloudUrl = new URL(sCloudUrl);
        } else {
            cloudUrl = new URL(CLOUD_URL);
        }

        String sAppiumUploadUrl = getProperty(TESTDROID_APPIUM_UPLOAD_URL);
        if(sAppiumUploadUrl != null) {
            appiumUploadUrl = new URL(sAppiumUrl);
        } else {
            appiumUploadUrl = new URL(APPIUM_UPLOAD_URL);
        }

        String appFilePath = getProperty(APPIUM_APPFILE);
        if (appFilePath != null) {
            appFile = new File(getProperty(APPIUM_APPFILE));
        }

        fileUUID = getProperty(TESTDROID_FILE_UUID);
        username = getProperty(TESTDROID_USERNAME);
        password = getProperty(TESTDROID_PASSWORD);
        projectName = getProperty(TESTDROID_PROJECT);
        deviceName = getProperty(TESTDROID_DEVICE);
        automationName = getProperty(APPIUM_AUTOMATION_NAME);

        String sGuiEnabled = getProperty(TESTDROID_GUI);
        if(sGuiEnabled != null && ("true".equals(sGuiEnabled.toLowerCase()) || "1".equals(sGuiEnabled))) {
            guiEnabled = true;
        }

        logger.info("TestdroidAppiumClient initialized");
        logger.info("Cloud URL: {}", cloudUrl);
        logger.info("Appium URL: {}", appiumUrl);
        logger.info("Appium upload URL: {}", appiumUploadUrl);
        logger.info("User: {}", username);
        logger.info("Project: {}", projectName);
        logger.info("Device: {}", deviceName);
        logger.info("Automation name: {}", automationName);
        logger.info("App file: {}", appFile);
        logger.info("File UUID: {}", fileUUID);
    }

    /**
     * Get property from environment or from testdroid.properties. Environment overrides.
     *
     * @param key
     * @return
     */
    public synchronized String getProperty(String key) {
        try {
            if(testdroidProperties == null) {
                testdroidProperties = new Properties();
                File file = new File(TESTDROID_PROPERTIES);
                if(file.exists()) {
                    logger.info("Loading default properties from {}", TESTDROID_PROPERTIES);
                    testdroidProperties.load(new FileInputStream(new File(TESTDROID_PROPERTIES)));
                }
            }
        } catch (IOException e) {
            logger.error("Failed loading {}", TESTDROID_PROPERTIES, e);
        }
        String value = System.getProperty(key);
        if(StringUtils.isEmpty(value)) {
            value = testdroidProperties.getProperty(key);
        }
        return value;
    }

    private synchronized static DefaultAPIClient getAPI(String cloudUrl, String username, String password) {
        if (api == null) {
            api = new DefaultAPIClient(cloudUrl, username, password);
        }
        return api;
    }


    public URL getCloudUrl() {
        return cloudUrl;
    }

    public void setCloudUrl(URL cloudUrl) {
        this.cloudUrl = cloudUrl;
    }

    public URL getAppiumUploadUrl() {
        return appiumUploadUrl;
    }

    public void setAppiumUploadUrl(URL appiumUploadUrl) {
        this.appiumUploadUrl = appiumUploadUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public URL getAppiumUrl() {
        return appiumUrl;
    }

    public void setAppiumUrl(URL appiumUrl) {
        this.appiumUrl = appiumUrl;
    }

    public void setDeviceWaitTime(int secs) { this.deviceWaitTime = secs; }

    public void setSignAppFile(boolean sign) {
        this.signAppFile = sign;
    }

    /**
     * Set Testdroid Cloud project name. Will be automatically created in cloud if does not exist.
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public File getAppFile() {
        return appFile;
    }

    /**
     * Set application file that will be uploaded to test
     */
    public void setAppFile(File appFile) {
        this.appFile = appFile;
    }

    public String getBundleId() {
        return bundleId;
    }

    /**
     * Set bundle ID for iOS tests
     *
     * @param bundleId Bundle ID. For example com.example.myapp.MyApp
     */
    public void setBundleId(String bundleId) {
        this.bundleId = bundleId;
    }

    public String getAndroidPackage() {
        return androidPackage;
    }

    public void setAndroidPackage(String androidPackage) {
        this.androidPackage = androidPackage;
    }

    public String getAndroidActivity() {
        return androidActivity;
    }

    public void setAndroidActivity(String androidActivity) {
        this.androidActivity = androidActivity;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean getSignAppFile() {
        return signAppFile;
    }

    /**
     * Set device name. Has to match device name in cloud if not running locally.
     *
     * @param deviceName
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getFileUUID() {
        return fileUUID;
    }

    public void setFileUUID(String fileUUID) {
        this.fileUUID = fileUUID;
    }

    public String getAutomationName() {
        return automationName;
    }

    public void setAutomationName(String automationName) {
        this.automationName = automationName;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getTestdroidDescription() {
        return testdroidDescription;
    }

    public void setTestdroidDescription(String testdroidDescription) {
        this.testdroidDescription = testdroidDescription;
    }

    public String getTestdroidTarget() {
        return testdroidTarget;
    }

    public void setTestdroidTarget(String testdroidTarget) {
        this.testdroidTarget = testdroidTarget;
    }

    public String getTestdroidLocale() {
        return testdroidLocale;
    }

    public void setTestdroidLocale(String testdroidLocale) {
        this.testdroidLocale = testdroidLocale;
    }

    public String getTestdroidJUnitWaitTime() {
        return testdroidJUnitWaitTime;
    }

    public void setTestdroidJUnitWaitTime(String testdroidJUnitWaitTime) {
        this.testdroidJUnitWaitTime = testdroidJUnitWaitTime;
    }

    public String getTestRunName() {
        return testRunName;
    }

    /**
     * Set test run name to use. Will be automatically set to deviceName - timestamp if not set.
     *
     * @param testRunName
     */
    public void setTestRunName(String testRunName) {
        this.testRunName = testRunName;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    /**
     * Upload application file to Testroid Appium broker
     * @throws IOException
     * @return File UUID. This can be used in future runs, so there is no need to upload the file every time.
     */
    public String uploadFile() throws Exception {
        if (appFile == null) {
            throw new Exception("appFile is null");
        }
        logger.info("Uploading application {}, {} bytes", appFile.getAbsolutePath(), appFile.length());

        final HttpHeaders headers = new HttpHeaders().setBasicAuthentication(username, password);

        HttpRequestFactory requestFactory =
                HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(JSON_FACTORY));
                        request.setHeaders(headers);
                    }
                });
        MultipartFormDataContent multipartContent = new MultipartFormDataContent();
        FileContent fileContent = new FileContent("application/octet-stream", appFile);

        MultipartFormDataContent.Part filePart = new MultipartFormDataContent.Part("file", fileContent);
        multipartContent.addPart(filePart);

        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(appiumUploadUrl), multipartContent);
        HttpResponse response = request.execute();

        // Extract file UUID
        AppiumResponse appiumResponse = request.execute().parseAs(AppiumResponse.class);
        String fileUUID = appiumResponse.uploadStatus.fileInfo.file;
        logger.info("File UUID: '{}'", fileUUID);

        return fileUUID;
    }

    /**
     * Initialize Testdroid Cloud Appium session
     *
     * Sets capabilities, uploads file to cloud, returns Appium driver when device ready for Appium commands.
     *
     * @return
     */
    // @TODO Refactor to use proper exceptions not generic one
    public TestdroidAppiumDriver getDriver() throws Exception {
        // Common desired capabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability("platformName", getPlatformName());

        // iOS
        if(StringUtils.isNotEmpty(bundleId)) {
            capabilities.setCapability("bundleId", bundleId);
        }
        // Android
        if(StringUtils.isNotEmpty(androidPackage)) {
            capabilities.setCapability("app-package", androidPackage);
        }
        if(StringUtils.isNotEmpty(androidActivity)) {
            capabilities.setCapability("app-activity", androidActivity);
        }
        // Browser
        if(StringUtils.isNotEmpty(browserName)) {
            capabilities.setCapability("browserName", browserName);
        }

        capabilities.setCapability("automationName", automationName);
        capabilities.setCapability("noSign", !signAppFile);

        if (appFile == null && fileUUID == null) {
            throw new Exception("Provide either appFile or fileUUID");
        }

        if (appFile != null) {
            logger.info("{} {}", appFile.getAbsoluteFile(), appFile.length());
            capabilities.setCapability("app", appFile.getAbsolutePath());
        }

        // @TODO is this needed?? only needed locally?
        capabilities.setCapability("deviceName", deviceName);

        // Local vs cloud
        if (appiumUrl.getHost().equals("localhost")) {
            logger.info("Initializing Appium, server URL {}", appiumUrl);
            capabilities.setCapability("platformName", getPlatformName());
            capabilities.setCapability("automationName", automationName);

        } else {
            logger.info("Cloud URL {}, username {}", cloudUrl.toString(), username);
            logger.info("Looking for device '{}'", deviceName);
            getAPI(cloudUrl.toString(), username, password);
            APIDevice device = getDevice(deviceName);
            APIDevice.OsType osType = device.getOsType();
            int APILevel = device.getSoftwareVersion().getApiLevel();
            setPlatformName(osType.getDisplayName());
            logger.info("Device OS Version {}, Device API Level: {}", osType, APILevel);

            if (getTestdroidTarget() == null) {
                if (APILevel == 0) {
                    setTestdroidTarget(TESTDROID_TARGET_IOS);
                }
                else if (APILevel >= 17) {
                    setTestdroidTarget(TESTDROID_TARGET_ANDROID);
                }
                else {
                    setTestdroidTarget(TESTDROID_TARGET_SELENDROID);
                }
                logger.info("Testdroid Target: {}", getTestdroidTarget());
            }

            capabilities.setCapability("platformName", getPlatformName());
            capabilities.setCapability("testdroid_target", testdroidTarget);

            if (fileUUID == null) {
                fileUUID = uploadFile();
            } else {
                logger.info("File UUID '{}' given, no need to upload application", fileUUID);
            }

            final String finalTestRunName = testRunName != null
                    ? testRunName : String.format("%s %s", deviceName, DATE_FORMAT.format(new Date()));

            logger.info("Project: {}", projectName);
            logger.info("Test run: {}", finalTestRunName);
            capabilities.setCapability("testdroid_project", projectName);
            capabilities.setCapability("testdroid_description", testdroidDescription);
            capabilities.setCapability("testdroid_testrun", finalTestRunName);
            capabilities.setCapability("testdroid_app", fileUUID);
            capabilities.setCapability("testdroid_device", deviceName);
            capabilities.setCapability("testdroid_target", testdroidTarget);
            if (StringUtils.isNotEmpty(testdroidLocale)) {
                capabilities.setCapability("testdroid_locale", testdroidLocale);
            }
            if (StringUtils.isNotEmpty(testdroidJUnitWaitTime)) {
                logger.info("Setting testdroid_junitWaitTime to {}", testdroidJUnitWaitTime);
                capabilities.setCapability("testdroid_junitWaitTime", testdroidJUnitWaitTime);
            }
            capabilities.setCapability(TestdroidAppiumDriver.CAPABILITY_TESTDROID_USERNAME, username);
            capabilities.setCapability(TestdroidAppiumDriver.CAPABILITY_TESTDROID_PASSWORD, password);

            deviceRunMonitorThread = new Thread(new Runnable() {
                APIUser me = null;

                public void run() {

                        Thread.currentThread().setName("DeviceRunMonitor");
                        Logger logger = LoggerFactory.getLogger(Thread.currentThread().getName());
                        boolean running = true;
                        try {
                            me = api.me();
                            APIProject project = null;
                            while (running) {
                                if (project == null) {
                                    project = getProject();
                                    if (project != null) {
                                        logger.info("Found project: #{} {}", project.getId(), project.getName());
                                    }
                                }
                                if (project != null) {
                                    APIListResource<APITestRun> testRunResource = project.getTestRunsResource(new APIQueryBuilder().offset(0).limit(10).search(finalTestRunName));
                                    java.util.List<APITestRun> testRuns = testRunResource.getEntity().getData();
                                    if (testRuns.size() > 0) {
                                        APITestRun testRun = testRuns.get(0);
                                        logger.info("{}: {}", testRun.getDisplayName(), testRun.getState().toString());
                                        APIListResource<APIDeviceRun> deviceRunsResource = testRun.getDeviceRunsResource();
                                        List<APIDeviceRun> deviceRunList = deviceRunsResource.getEntity().getData();
                                        for(APIDeviceRun deviceRun: deviceRunList) {
                                            logger.info("{} #{} {}/api/v2/users/{}/projects/{}/runs/{}/device-runs/{}/result-data.zip", deviceRun.getDeviceName(), deviceRun.getId(), cloudUrl.toString(), me.getId(), project.getId(), testRun.getId(), deviceRun.getId());
                                        }
                                    }
                                    Thread.sleep(30000);
                                }
                            }
                        } catch (APIException apiex) {
                            logger.error("Failed API query, aborting", apiex);
                        } catch (InterruptedException ex) {
                            logger.info("Interrupted - stopping");
                        }
                }

                private APIProject getProject() throws APIException {
                    APIListResource<APIProject> projectsResource = me.getProjectsResource(new APIQueryBuilder().offset(0).limit(10).search(projectName));
                    java.util.List<APIProject> projects = projectsResource.getEntity().getData();
                    if (projects.size() > 0) {
                        return projects.get(0);
                    } else {
                        return null;
                    }
                }

            });
            deviceRunMonitorThread.start();

            logger.info("Initializing Appium, server URL {}, user {}", appiumUrl, username);
        }

        driver = new TestdroidAppiumDriver(appiumUrl, capabilities);

        logger.info("Appium connected at {}", appiumUrl);

        return driver;

    }

    public APIDevice getDevice(String deviceName) throws Exception {
        APIUser me = null;
        APIDevice device;
        try {
            me = api.me();
            logger.info("Connected to Testdroid Cloud with account {} {}", me.getName(), me.getEmail());
            APIListResource<APIDevice> devicesResource = api.getDevices(
                    new APIDeviceQueryBuilder().search(deviceName));
            java.util.List<APIDevice> devices = devicesResource.getEntity().getData();
            if (devices.size() == 0) {
                logger.error("Unable to find device '{}'", deviceName);
                throw new Exception("No device found");
            }
            device = devices.get(0);
            int sleepTime = 10;
            while (device.isLocked() && deviceWaitTime > 0) {
                logger.info("All devices are in use right now, waiting for {} seconds...", deviceWaitTime);
                Thread.sleep(sleepTime * 1000);
                setDeviceWaitTime(deviceWaitTime - sleepTime);
                devicesResource = api.getDevices(new APIDeviceQueryBuilder().search(deviceName));
                device = devicesResource.getEntity().getData().get(0);
            }
            if (device.isLocked()) {
                String errorMsg = String.format("Every '%s' is busy at the moment", deviceName);
                logger.error(errorMsg);
                throw new Exception(errorMsg);
            }

            logger.info("Found device! ID {}", device.getId());
            return device;

        } catch (Exception ex) {
            logger.error("Failed to query API for device '{}'", deviceName, ex);
            throw new Exception(String.format("Unable to use device '%s'", deviceName));
        }
    }


    public void quit() {
        logger.info("Quitting Appium driver");
        if (deviceRunMonitorThread != null) {
            deviceRunMonitorThread.interrupt();
        }
        driver.quit();
    }

    public File screenshot(String name) {
        logger.info("Taking screenshot...");
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

        try {

            File testScreenshot = new File(name);
            FileUtils.copyFile(scrFile, testScreenshot);
            logger.info("Screenshot stored to {}", testScreenshot.getAbsolutePath());

            if(guiEnabled) {
                showScreenshot(testScreenshot);
            }

            return testScreenshot;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void showScreenshot(File screenshot) {
        try { // lets catch everything so that test goes trough even if problem with GUI
            if (screenshotDisplay != null) {
                //screenshotDisplay.dispatchEvent(new WindowEvent(screenshotDisplay, WindowEvent.WINDOW_CLOSING));
                screenshotDisplay.dispose();
            }
            screenshotDisplay = new ScreenshotDisplay();
            screenshotDisplay.show(screenshot);
        } catch (Exception ex) {
            logger.error("Failed displaying screenshot - test run will still continue", ex);

        }
    }
    // Appium server upload response classes

    public static class AppiumResponse {
        Integer status;
        @Key("sessionId")
        String sessionId;

        @Key("value")
        UploadStatus uploadStatus;

    }

    public static class UploadedFile {
        @Key("file")
        String file;
    }

    public static class UploadStatus {
        @Key("message")
        String message;
        @Key("uploadCount")
        Integer uploadCount;
        @Key("expiresIn")
        Integer expiresIn;
        @Key("uploads")
        UploadedFile fileInfo;
    }

}


// @TODO make screenshot displaying to fit to screen, reuse the frame

class ImagePanel extends JPanel {
    private Image img;

    public ImagePanel(Image image) {
        this.img = img;
        Dimension size = new Dimension(image.getWidth(null), image.getHeight(null));
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setSize(size);
        setLayout(null);
    }

    public void paintComponent(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }
}

class ScreenshotDisplay extends JFrame {
    public void show(File screenshotFile) {
        ImagePanel panel = null;
        Image image = null;
        try {
            image = ImageIO.read(screenshotFile);
            panel = new ImagePanel(image);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
        }
        add(panel);
        setVisible(true);
        setSize(image.getWidth(null), image.getHeight(null));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
}