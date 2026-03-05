package com.punchh.server.mobileUtilities;

import com.punchh.server.utilities.Utilities;

import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;

public class AppiumUtils {
	String PLATFORM_VERSION = System.getProperty("platform.version", new Utilities().get_iOSVersion());
	String DEVICE_NAME = System.getProperty("device.name", "iPhone 16e");
	String AUTOMATION_NAME = System.getProperty("automation.name", "XCUITest");
	Duration WDA_LAUNCH_TIMEOUT = Duration.ofSeconds(600);
	Duration IMPLICIT_WAIT = Duration.ofSeconds(60);
	String APPIUM_SERVER_URL = System.getProperty("appium.server.url", "http://127.0.0.1:4723");
	Duration COMMAND_TIMEOUT = Duration.ofSeconds(0);
	String APPIUM_DEBUG_LEVEL = "error";

	private static final Logger logger = LogManager.getLogger(AppiumUtils.class);
	private AppiumDriverLocalService appiumService;

	public IOSDriver getIOSDriver(String appPath) {
		try {
			logger.info("Initializing XCUITestOptions for the IOSDriver.");

			XCUITestOptions options = new XCUITestOptions();
			options.setPlatformVersion(PLATFORM_VERSION);
			options.setDeviceName(DEVICE_NAME);
			options.setAutomationName(AUTOMATION_NAME);
			options.setWdaLaunchTimeout(WDA_LAUNCH_TIMEOUT);
			options.setFullReset(true);
			options.setAutoAcceptAlerts(true);
			options.setNewCommandTimeout(COMMAND_TIMEOUT);
			options.showXcodeLog();

			IOSDriver driver = new IOSDriver(new URL(APPIUM_SERVER_URL), options);
			driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);

			logger.info("Using XCUITestOptions as ==> {}", options.asMap().toString());
			logger.info("IOSDriver initialized successfully with session id ==> " + driver.getSessionId().toString());

			return driver;
		} catch (MalformedURLException e) {
			logger.error("Invalid URL for the Appium server: {}", APPIUM_SERVER_URL, e);
			throw new RuntimeException("Failed to create IOSDriver due to malformed URL.", e);
		} catch (Exception e) {
			logger.error("Error initializing the IOSDriver: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to create IOSDriver.", e);
		}
	}

	// Method to start the Appium server
	public void startAppiumServer() {
		logger.info("Starting Appium server...");
		try {
			AppiumServiceBuilder serviceBuilder = new AppiumServiceBuilder()
//					.usingDriverExecutable(new File("/opt/homebrew/bin/node"))                      
//					.withAppiumJS(new File("/usr/local/lib/node_modules/appium/build/lib/main.js"))
					.withArgument(() -> "--session-override")
					.withArgument(() -> "--log-level", APPIUM_DEBUG_LEVEL);

			HashMap<String, String> environment = new HashMap<>();
			environment.put("PATH", System.getenv("PATH"));
			serviceBuilder.withEnvironment(environment);

			appiumService = AppiumDriverLocalService.buildService(serviceBuilder);
			appiumService.start();

			if (appiumService.isRunning()) {
				logger.info("Appium server started successfully");
			} else {
				throw new RuntimeException("Failed to start the Appium server.");
			}
		} catch (Exception e) {
			logger.error("Error starting the Appium server: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to start the Appium server.", e);
		}
	}

	// Method to stop the Appium server
	public void stopAppiumServer() {
		logger.info("Stopping Appium server...");
		if (appiumService != null) {
			if (appiumService.isRunning()) {
				appiumService.stop();
				logger.info("Appium server stopped successfully.");
			} else {
				logger.warn("Appium server is not running.");
			}
		} else {
			logger.warn("No Appium service instance found to stop.");
		}
	}

}