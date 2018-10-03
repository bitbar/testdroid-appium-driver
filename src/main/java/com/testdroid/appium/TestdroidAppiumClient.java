package com.testdroid.appium;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.testdroid.api.APIException;
import com.testdroid.api.APIListResource;
import com.testdroid.api.DefaultAPIClient;
import com.testdroid.api.dto.Context;
import com.testdroid.api.filter.StringFilterEntry;
import com.testdroid.api.http.MultipartFormDataContent;
import com.testdroid.api.model.*;
import com.testdroid.appium.model.AppiumResponse;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static com.testdroid.api.dto.Operand.EQ;
import static com.testdroid.dao.repository.dto.MappingKey.NAME;

/**
 * Client for running Appium tests against Testdroid Cloud
 *
 * @author Henri Kivelä <henri.kivela@bitbar.com>
 * @author Jarno Tuovinen <jarno.tuovinen@bitbar.com>
 */
public class TestdroidAppiumClient {

    private static final String RESULTS_INFO =
            "{} #{} {}/api/v2/users/{}/projects/{}/runs/{}/device-sessions/{}/output-file-set/files";

    private static final String CLOUD_URL = "https://cloud.testdroid.com";
    private static final String CLOUD_APPIUM_URL = "http://appium.testdroid.com/wd/hub";
    private static final String APPIUM_UPLOAD_URL = "http://appium.testdroid.com/upload";

    // File for testdroid properties that will be used if no environment variables found
    private static final String TESTDROID_PROPERTIES = "testdroid.properties";

    // Environment variable names
    private static final String TESTDROID_CLOUD_URL = "testdroid.cloudUrl";
    private static final String TESTDROID_USERNAME = "testdroid.username";
    private static final String TESTDROID_PASSWORD = "testdroid.password";
    private static final String TESTDROID_PROJECT = "testdroid.project";
    private static final String TESTDROID_DEVICE = "testdroid.device";
    private static final String TESTDROID_GUI = "testdroid.gui";
    private static final String TESTDROID_APPIUM_URL = "testdroid.appiumUrl";
    private static final String TESTDROID_APPIUM_UPLOAD_URL = "testdroid.appiumUploadUrl";
    // Appium constants
    public static final String APPIUM_PLATFORM_IOS = "iOS";
    public static final String APPIUM_PLATFORM_ANDROID = "Android";
    private static final String APPIUM_AUTOMATION_NAME = "appium.automationName";
    private static final String APPIUM_APPFILE = "appium.appFile";
    // Testdroid constants
    private static final String TESTDROID_TARGET_IOS = "ios";
    private static final String TESTDROID_TARGET_ANDROID = "android";
    public static final String TESTDROID_TARGET_CHROME = "chrome";
    public static final String TESTDROID_TARGET_SAFARI = "safari";
    public static final String TESTDROID_TARGET_SELENDROID = "selendroid";
    private static final String TESTDROID_FILE_UUID = "testdroid.uuid";
    public static final String TESTDROID_UUID_SAMPLE_ANDROID = "sample/BitbarSampleApp.apk";
    public static final String TESTDROID_UUID_SAMPLE_IOS = "sample/BitbarIOSSample.ipa";

    // @TODO add rest of platforms

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Logger LOGGER = LoggerFactory.getLogger(TestdroidAppiumClient.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TestdroidAppiumDriverAndroid androidDriver;

    private TestdroidAppiumDriverIos iOSdriver;

    private static DefaultAPIClient api;

    private static boolean guiEnabled = false;

    private static Thread deviceRunMonitorThread;

    private ScreenshotDisplay screenshotDisplay = null;

    private int deviceWaitTime = 120; // Optional, sets time to wait when device is in use, use 0 for no wait time
    private boolean signAppFile = true; // Optional, if set to false app file will not be resigned

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
        if (sAppiumUrl != null) {
            appiumUrl = new URL(sAppiumUrl);
        } else {
            appiumUrl = new URL(CLOUD_APPIUM_URL);
        }

        String sCloudUrl = getProperty(TESTDROID_CLOUD_URL);
        if (sCloudUrl != null) {
            cloudUrl = new URL(sCloudUrl);
        } else {
            cloudUrl = new URL(CLOUD_URL);
        }

        String sAppiumUploadUrl = getProperty(TESTDROID_APPIUM_UPLOAD_URL);
        if (sAppiumUploadUrl != null) {
            appiumUploadUrl = new URL(sAppiumUploadUrl);
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
        if (sGuiEnabled != null && ("true".equals(sGuiEnabled.toLowerCase()) || "1".equals(sGuiEnabled))) {
            guiEnabled = true;
        }

        LOGGER.info("TestdroidAppiumClient initialized");
        LOGGER.info("Cloud URL: {}", cloudUrl);
        LOGGER.info("Appium URL: {}", appiumUrl);
        LOGGER.info("Appium upload URL: {}", appiumUploadUrl);
        LOGGER.info("User: {}", username);
        LOGGER.info("Project: {}", projectName);
        LOGGER.info("Device: {}", deviceName);
        LOGGER.info("Automation name: {}", automationName);
        LOGGER.info("App file: {}", appFile);
        LOGGER.info("File UUID: {}", fileUUID);
    }

    /**
     * Get property from environment or from testdroid.properties. Environment overrides.
     */
    private synchronized String getProperty(String key) {
        try {
            if (testdroidProperties == null) {
                testdroidProperties = new Properties();
                File file = new File(TESTDROID_PROPERTIES);
                if (file.exists()) {
                    LOGGER.info("Loading default properties from {}", TESTDROID_PROPERTIES);
                    testdroidProperties.load(new FileInputStream(new File(TESTDROID_PROPERTIES)));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed loading {}", TESTDROID_PROPERTIES, e);
        }
        String value = System.getProperty(key);
        if (StringUtils.isEmpty(value)) {
            value = testdroidProperties.getProperty(key);
        }
        return value;
    }

    private synchronized static void initAPI(String cloudUrl, String username, String password) {
        if (api == null) {
            api = new DefaultAPIClient(cloudUrl, username, password);
        }
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

    public void setDeviceWaitTime(int secs) {
        this.deviceWaitTime = secs;
    }

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
     *
     * @return File UUID. This can be used in future runs, so there is no need to upload the file every time.
     */
    private String uploadFile() throws Exception {
        if (appFile == null) {
            throw new Exception("appFile is null");
        }
        LOGGER.info("Uploading application {}, {} bytes", appFile.getAbsolutePath(), appFile.length());

        final HttpHeaders headers = new HttpHeaders().setBasicAuthentication(username, password);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(request -> request.setHeaders(headers));
        MultipartFormDataContent multipartContent = new MultipartFormDataContent();
        FileContent fileContent = new FileContent("application/octet-stream", appFile);

        MultipartFormDataContent.Part filePart = new MultipartFormDataContent.Part("file", fileContent);
        multipartContent.addPart(filePart);

        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(appiumUploadUrl), multipartContent);
        // Extract file UUID
        HttpResponse response = request.execute();

        AppiumResponse appiumResponse = OBJECT_MAPPER.readValue(response.getContent(), AppiumResponse.class);
        String fileUUID = appiumResponse.getValue().getUploads().getFile();
        LOGGER.info("File UUID: '{}'", fileUUID);

        return fileUUID;
    }

    private DesiredCapabilities setCommonCapabilities() throws Exception {
        // Common desired capabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();

        capabilities.setCapability("platformName", getPlatformName());

        // iOS
        if (StringUtils.isNotEmpty(bundleId)) {
            capabilities.setCapability("bundleId", bundleId);
        }
        // Android
        if (StringUtils.isNotEmpty(androidPackage)) {
            capabilities.setCapability("appPackage", androidPackage);
        }
        if (StringUtils.isNotEmpty(androidActivity)) {
            capabilities.setCapability("appActivity", androidActivity);
        }
        // Browser
        if (StringUtils.isNotEmpty(browserName)) {
            capabilities.setCapability("browserName", browserName);
        }

        capabilities.setCapability("automationName", automationName);
        capabilities.setCapability("noSign", !signAppFile);

        if (appFile == null && fileUUID == null) {
            throw new Exception("Provide either appFile or fileUUID");
        }

        if (appFile != null) {
            LOGGER.info("{} {}", appFile.getAbsoluteFile(), appFile.length());
            capabilities.setCapability("app", appFile.getAbsolutePath());
        }

        // @TODO is this needed?? only needed locally?
        capabilities.setCapability("deviceName", deviceName);

        // Local vs cloud
        if (appiumUrl.getHost().equals("localhost")) {
            LOGGER.info("Initializing Appium, server URL {}", appiumUrl);
            capabilities.setCapability("platformName", getPlatformName());
            capabilities.setCapability("automationName", automationName);
        } else {
            LOGGER.info("Cloud URL {}, username {}", cloudUrl.toString(), username);
            LOGGER.info("Looking for device '{}'", deviceName);
            initAPI(cloudUrl.toString(), username, password);
            APIDevice device = getDevice(deviceName);
            APIDevice.OsType osType = device.getOsType();
            int APILevel = device.getSoftwareVersion().getApiLevel();
            setPlatformName(osType.getDisplayName());
            LOGGER.info("Device OS Version {}, Device API Level: {}", osType, APILevel);

            if (getTestdroidTarget() == null) {
                if (APILevel == 0) {
                    setTestdroidTarget(TESTDROID_TARGET_IOS);
                } else if (APILevel >= 17) {
                    setTestdroidTarget(TESTDROID_TARGET_ANDROID);
                } else {
                    setTestdroidTarget(TESTDROID_TARGET_SELENDROID);
                }
                LOGGER.info("Testdroid Target: {}", getTestdroidTarget());
            }

            capabilities.setCapability("platformName", getPlatformName());
            capabilities.setCapability("testdroid_target", testdroidTarget);

            if (fileUUID == null) {
                fileUUID = uploadFile();
            } else {
                LOGGER.info("File UUID '{}' given, no need to upload application", fileUUID);
            }

            final String finalTestRunName = testRunName != null
                    ? testRunName : String.format("%s %s", deviceName, DATE_FORMAT.format(new Date()));

            LOGGER.info("Project: {}", projectName);
            LOGGER.info("Test run: {}", finalTestRunName);
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
                LOGGER.info("Setting testdroid_junitWaitTime to {}", testdroidJUnitWaitTime);
                capabilities.setCapability("testdroid_junitWaitTime", testdroidJUnitWaitTime);
            }
            capabilities.setCapability(TestdroidAppiumDriver.CAPABILITY_TESTDROID_USERNAME, username);
            capabilities.setCapability(TestdroidAppiumDriver.CAPABILITY_TESTDROID_PASSWORD, password);

            deviceRunMonitorThread = new Thread(() -> {
                Thread.currentThread().setName("DeviceRunMonitor");
                Logger logger = LoggerFactory.getLogger(Thread.currentThread().getName());
                boolean running = true;
                try {
                    APIUser me = api.me();
                    APIProject project = null;
                    while (running) {
                        if (project == null) {
                            List<APIProject> projects = me.getProjectsResource(new Context<>(APIProject.class)
                                    .addFilter(new StringFilterEntry(NAME, EQ, projectName))).getEntity().getData();
                            if (projects.size() > 0) {
                                project = projects.get(0);
                                logger.info("Found project: #{} {}", project.getId(), project.getName());
                            }
                        }
                        if (project != null) {
                            APIListResource<APITestRun> testRunResource = project
                                    .getTestRunsResource(new Context<>(APITestRun.class).setLimit(1)
                                            .setSearch((finalTestRunName)));
                            List<APITestRun> testRuns = testRunResource.getEntity().getData();
                            if (testRuns.size() > 0) {
                                APITestRun testRun = testRuns.get(0);
                                logger.info("{}: {}", testRun.getDisplayName(), testRun.getState().toString());
                                List<APIDeviceSession> sessions = testRun.getDeviceRunsResource().getEntity().getData();
                                for (APIDeviceSession deviceSession : sessions) {
                                    logger.info(RESULTS_INFO, deviceSession.getDevice().getDisplayName(),
                                            deviceSession.getId(), cloudUrl.toString(), me.getId(), project.getId(),
                                            testRun.getId(), deviceSession.getId());
                                }
                                if (APITestRun.State.FINISHED == testRun.getState()) {
                                    running = false;
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
            });
            deviceRunMonitorThread.start();

            LOGGER.info("Initializing Appium, server URL {}, user {}", appiumUrl, username);
        }
        return capabilities;
    }

    /**
     * Initialize Testdroid Cloud Appium session
     * <p>
     * Sets capabilities, uploads file to cloud, returns Appium driver when device ready for Appium commands.
     */
    // @TODO Refactor to use proper exceptions not generic one
    public TestdroidAppiumDriverIos getIOSDriver() throws Exception {
        DesiredCapabilities capabilities = setCommonCapabilities();
        iOSdriver = new TestdroidAppiumDriverIos(appiumUrl, capabilities);
        LOGGER.info("Appium connected at {}", appiumUrl);
        return iOSdriver;
    }

    public TestdroidAppiumDriverAndroid getAndroidDriver() throws Exception {
        DesiredCapabilities capabilities = setCommonCapabilities();
        androidDriver = new TestdroidAppiumDriverAndroid(appiumUrl, capabilities);
        LOGGER.info("Appium connected at {}", appiumUrl);
        return androidDriver;
    }

    private APIDevice getDevice(String deviceName) throws Exception {
        try {
            APIUser me = api.me();
            LOGGER.info("Connected to Testdroid Cloud with account {} {}", me.getName(), me.getEmail());
            Context<APIDevice> ctx = new Context<>(APIDevice.class);
            ctx.setSearch(deviceName);
            APIListResource<APIDevice> devicesResource = api.getDevices(ctx);
            List<APIDevice> devices = devicesResource.getEntity().getData();
            if (devices.size() == 0) {
                LOGGER.error("Unable to find device '{}'", deviceName);
                throw new Exception("No device found");
            }
            APIDevice device = devices.get(0);
            int sleepTime = 10;
            while (device.isLocked() && deviceWaitTime > 0) {
                LOGGER.info("All devices are in use right now, waiting for {} seconds...", deviceWaitTime);
                Thread.sleep(sleepTime * 1000);
                setDeviceWaitTime(deviceWaitTime - sleepTime);
                devicesResource = api.getDevices(new Context<>(APIDevice.class).setSearch(deviceName));
                device = devicesResource.getEntity().getData().get(0);
            }
            if (device.isLocked()) {
                String errorMsg = String.format("Every '%s' is busy at the moment", deviceName);
                LOGGER.error(errorMsg);
                throw new Exception(errorMsg);
            }

            LOGGER.info("Found device! ID {}", device.getId());
            return device;

        } catch (Exception ex) {
            LOGGER.error("Failed to query API for device '{}'", deviceName, ex);
            throw new Exception(String.format("Unable to use device '%s'", deviceName));
        }
    }

    public void quit() {
        LOGGER.info("Quitting Appium driver");
        if (deviceRunMonitorThread != null) {
            deviceRunMonitorThread.interrupt();
        }
        getCurrentDriver().quit();
    }

    public File screenshot(String name) {
        LOGGER.info("Taking screenshot...");
        File scrFile = getCurrentDriver().getScreenshotAs(OutputType.FILE);
        try {
            File testScreenshot = new File(name);
            FileUtils.copyFile(scrFile, testScreenshot);
            LOGGER.info("Screenshot stored to {}", testScreenshot.getAbsolutePath());
            if (guiEnabled) {
                showScreenshot(testScreenshot);
            }
            return testScreenshot;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showScreenshot(File screenshot) {
        try { // lets catch everything so that test goes trough even if problem with GUI
            if (screenshotDisplay != null) {
                //screenshotDisplay.dispatchEvent(new WindowEvent(screenshotDisplay, WindowEvent.WINDOW_CLOSING));
                screenshotDisplay.dispose();
            }
            screenshotDisplay = new ScreenshotDisplay();
            screenshotDisplay.show(screenshot);
        } catch (Exception ex) {
            LOGGER.error("Failed displaying screenshot - test run will still continue", ex);
        }
    }

    private AppiumDriver<MobileElement> getCurrentDriver() {
        return iOSdriver != null ? iOSdriver : androidDriver;
    }
}
