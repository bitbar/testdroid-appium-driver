# Testdroid Appium Driver

Testdroid Appium driver for Java.

## Dependencies

Driver depends on [testdroid-api](https://github.com/bitbar/testdroid-api).  
Build testdroid-api and install it locally

```
mvn clean install -DskipTests
```

## Install Testdroid API client

```
git pull git@github.com:bitbar/testdroid-appium-driver.git
cd testdroid-appium-driver && mvn -DskipTests clean install
```

## Install Appium

### OS X

Install Node.js

```
brew install node
```

Install Appium
Safest version at the moment is 1.3.6.

```
npm install appium@1.3.6
```

### Ubuntu

TODO

## Run tests

### iOS application with Testdroid Cloud

Copy testdroid.properties.ios.example to testdroid.properties, edit the username etc.

Run test using device in properties

```
mvn -Dtest=BitbarIOSSampleTest test
```

Run test on particular device without editing testdroid.properties

```
mvn -Dtestdroid.device="iPad Air A1474 7.0.3" -Dtest=BitbarIOSSampleTest test
```

### Android application with Testdroid Cloud

Copy testdroid.properties.android.example to testdroid.properties, edit the username etc.

There is one example test for two different applications (BitbarSampleAppTest and TestdroidTest).

```
mvn -Dtest=BitbarSampleAppTest test
```

### Chrome test on Android with Testdroid Cloud

Copy testdroid.properties.android.chrome.example to testdroid.properties, edit the username etc.

```
mvn -Dtest=ChromeTest test
```

### Local iOS

Start appium

```
appium -U <DEVICE_UUID> --port 4723
```

Execute IOS sample test

```
mvn -Dtest=BitbarIOSSampleTest -Dtestdroid.appiumUrl=http://localhost:4723/wd/hub clean test
```

### Local Android

Start Appium

```
node ./appium.js --platform-name=Android --device-name=AndroidDevice --app=/path/to/apk/Testdroid.apk --app-activity=".MM_MainMenu"
```

Execute test

```
mvn -Dtest=TestdroidTest -Dtestdroid.appiumUrl=http://localhost:4723/wd/hub
```

## Notes

testdroid.properties is read from current directory. Global properties are not
supported at the moment.

