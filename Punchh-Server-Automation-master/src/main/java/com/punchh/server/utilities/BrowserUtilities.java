package com.punchh.server.utilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import Support.ConfigurationClass;

/*
 * Desc: This utility method for browser handling, getting locators and other common utilities
 */

// The Class BrowserUtilities.
@Listeners(TestListeners.class)
public class BrowserUtilities {

	WebDriver driver;
	static Logger logger = LogManager.getLogger(BrowserUtilities.class);

	public WebDriver invokeLocalDriver(String browserName) {

		if (browserName.toUpperCase().trim().equalsIgnoreCase("FIREFOX")) {

			FirefoxProfile profile = new FirefoxProfile();
			FirefoxOptions options = new FirefoxOptions();

			profile.setPreference("privacy.resistFingerprinting", false);// Disable fingerprinting (like Chrome)
			profile.setPreference("dom.webdriver.enabled", true); // Default webdriver flag (Chrome exposes this by
																	// default)
			profile.setPreference("media.peerconnection.enabled", true); // Disable WebRTC protection (Chrome exposes
																			// IPs by default)
			profile.setPreference("privacy.trackingprotection.enabled", false); // Disable anti-tracking (Chrome allows
																				// trackers by default)
			profile.setPreference("intl.accept_languages", "en-US, en"); // Disable language spoofing
			profile.setPreference("webgl.disabled", false); // Disable WebGL blocking
			profile.setPreference("network.http.sendRefererHeader", 2); // Allow Referer headers
			profile.setPreference("network.http.referer.spoofSource", false);
			profile.setPreference("permissions.default.desktop-notification", 1);
			profile.setPreference("signon.rememberSignons", false);// Disable password manager and notifications
			profile.setPreference("dom.webnotifications.enabled", false); // Disable web notifications
			profile.setPreference("permissions.default.desktop-notification", 2); // Block notifications

			options.addArguments("-private"); // incognito mode
			options.setAcceptInsecureCerts(true);
			options.addArguments("--disable-extensions"); // Disable extensions
			options.addArguments("--start-maximized"); // Maximize window
			options.addArguments("--width=1920");
			options.addArguments("--height=1080");
			options.addArguments("--disable-gpu"); // for performance issues
			options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

			options.setProfile(profile);
			driver = new FirefoxDriver(options);

		} else if (browserName.toUpperCase().trim().equalsIgnoreCase("CHROME")) {
			ChromeOptions options = new ChromeOptions();
			String downloadFilePath = System.getProperty("user.dir") + "/resources/ExportData";

			options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
			options.setExperimentalOption("useAutomationExtension", false);
			options.addArguments("disable-infobars");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-blink-features=AutomationControlled");
			options.addArguments("--disable-extensions");
			options.addArguments("--disable-notifications");
			options.addArguments("--disable-gpu");
			options.addArguments("--window-size=1920,1080");
			options.addArguments("--disable-browser-side-navigation");
			options.addArguments("--disable-dev-shm-usage");
			options.setAcceptInsecureCerts(true);
			options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
			Map<String, Object> prefs = new HashMap<String, Object>();
			prefs.put("credentials_enable_service", false);
			prefs.put("profile.password_manager_enabled", false);
			prefs.put("profile.default_content_setting_values.notifications", 2);
			prefs.put("profile.default_content_settings.popups", 0);
			prefs.put("download.default_directory", downloadFilePath);
			options.setExperimentalOption("prefs", prefs);

			driver = new ChromeDriver(options);

		} else if (browserName.toUpperCase().trim().equalsIgnoreCase("HeadlessChrome")) {
			ChromeOptions options = new ChromeOptions();
			String downloadFilePath = System.getProperty("user.dir") + "/resources/ExportData";

			options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
			options.setExperimentalOption("useAutomationExtension", false);
			options.addArguments("disable-infobars");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-blink-features=AutomationControlled");
			options.addArguments("--disable-extensions");
			options.addArguments("--disable-notifications");
			options.addArguments("--disable-gpu");
			options.addArguments("--window-size=1920,1080");
			options.addArguments("--disable-browser-side-navigation");
			options.addArguments("--disable-dev-shm-usage");
			// for headless
			options.addArguments("--headless=new");
			options.addArguments("--disable-software-rasterizer");
			options.addArguments("--disable-features=VizDisplayCompositor");
			options.addArguments("--renderer-process-limit=1");
			options.addArguments("--disable-features=RendererCodeIntegrity");
			options.setAcceptInsecureCerts(true);
			options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
			Map<String, Object> prefs = new HashMap<String, Object>();
			prefs.put("credentials_enable_service", false);
			prefs.put("profile.password_manager_enabled", false);
			prefs.put("profile.default_content_setting_values.notifications", 2);
			prefs.put("profile.default_content_settings.popups", 0);
			prefs.put("download.default_directory", downloadFilePath);
			options.setExperimentalOption("prefs", prefs);

			driver = new ChromeDriver(options);

		} else {
			Assert.fail("Driver does not exist");
			driver = null;
		}
		return driver;
	}

	/*
	 * Browser ==> chrome, HUB_HOST==> localhost/ 10.20.30.40, url= "http://" + host
	 * + ":4444/wd/hub"
	 */
	public WebDriver invokeRemoteDriver(String browserName, String host) {

		if (browserName.toUpperCase().trim().equalsIgnoreCase("CHROME")) {
			// DesiredCapabilities dc = DesiredCapabilities.chrome();
			ChromeOptions options = new ChromeOptions();
			String downloadFilePath = System.getProperty("user.dir") + "/resources/Downloads";
			options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
			options.setExperimentalOption("useAutomationExtension", false);
			options.addArguments("disable-infobars");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-blink-features=AutomationControlled");
			options.addArguments("--disable-extensions");
			options.addArguments("--disable-notifications");
			options.addArguments("--disable-gpu");
			options.addArguments("--window-size=1920,1080");
			options.addArguments("--disable-browser-side-navigation");
			options.addArguments("--disable-dev-shm-usage");
			options.setAcceptInsecureCerts(true);
			options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
			Map<String, Object> prefs = new HashMap<String, Object>();
			prefs.put("credentials_enable_service", false);
			prefs.put("profile.password_manager_enabled", false);
			prefs.put("profile.default_content_setting_values.notifications", 2);
			prefs.put("profile.default_content_settings.popups", 0);
			prefs.put("download.default_directory", downloadFilePath);
			options.setExperimentalOption("prefs", prefs);

			options.setCapability(ChromeOptions.CAPABILITY, options);
			URL url = null;
			try {
				url = new URL("http://" + host + ":4444");
				// url = new URL("http://" + host + ":64310"); //sel version 4 url for hub
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			driver = new RemoteWebDriver(url, options);

		} else if (browserName.toUpperCase().trim().equalsIgnoreCase("HeadlessChrome")) {
			ChromeOptions options = new ChromeOptions();
			String downloadFilePath = System.getProperty("user.dir") + "/resources/ExportData";

			options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
			options.setExperimentalOption("useAutomationExtension", false);
			options.addArguments("disable-infobars");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-blink-features=AutomationControlled");
			options.addArguments("--disable-extensions");
			options.addArguments("--disable-notifications");
			options.addArguments("--disable-gpu");
			options.addArguments("--window-size=1920,1080");
			options.addArguments("--disable-browser-side-navigation");
			options.addArguments("--disable-dev-shm-usage");
			// for headless
			options.addArguments("--headless=new");
			options.addArguments("--disable-software-rasterizer");
			options.addArguments("--disable-features=VizDisplayCompositor");
			options.addArguments("--renderer-process-limit=1");
			options.addArguments("--disable-features=RendererCodeIntegrity");
			options.setAcceptInsecureCerts(true);
			options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
			Map<String, Object> prefs = new HashMap<String, Object>();
			prefs.put("credentials_enable_service", false);
			prefs.put("profile.password_manager_enabled", false);
			prefs.put("profile.default_content_setting_values.notifications", 2);
			prefs.put("profile.default_content_settings.popups", 0);
			prefs.put("download.default_directory", downloadFilePath);
			options.setExperimentalOption("prefs", prefs);

			options.setCapability(ChromeOptions.CAPABILITY, options);
			URL url = null;
			try {
				url = new URL("http://" + host + ":4444");
				// url = new URL("http://" + host + ":64310"); //sel version 4 url for hub
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			driver = new RemoteWebDriver(url, options);
		}

		else if (browserName.toUpperCase().trim().equalsIgnoreCase("FIREFOX")) {
			// DesiredCapabilities dc = DesiredCapabilities.firefox();
			FirefoxOptions options = new FirefoxOptions();
			URL url = null;
			try {
				url = new URL("http://" + host + ":4444/wd/hub");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

			driver = new RemoteWebDriver(url, options);
		}

		return driver;

	}

	public WebDriver launchBrowser() {

		String browserName = getBrowser();
		String runType = getRunType();
		String host = getHost();

		if (runType.equalsIgnoreCase("local")) {
			driver = invokeLocalDriver(browserName);

		} else if (runType.equalsIgnoreCase("remote")) {
			driver = invokeRemoteDriver(browserName, host);

		}

		if (driver != null) {

			driver.manage().deleteAllCookies();
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(60));
			Capabilities cap = ((RemoteWebDriver) driver).getCapabilities();
			String bv = cap.getBrowserVersion().toString();
			logger.info("Browser Version: " + bv);
			logger.info("OS Name: " + System.getProperty("os.name"));
			logger.info("OS Version: " + System.getProperty("os.version"));
			logger.info("Browser launched successfully.");

		}
		return driver;
	}

	public String getRunType() {
		String runType;
		if (ConfigurationClass.RUNTYPE == null) {
			runType = "local";
		} else
			runType = ConfigurationClass.RUNTYPE;
		return runType;
	}

	public String getBrowser() {
		String browserName;
		if (ConfigurationClass.BROWSER == null) {
			browserName = "chrome";
			// browserName = "headlesschrome";
		} else
			browserName = ConfigurationClass.BROWSER;
		return browserName;
	}

	public String getHost() {
		String host;
		if (ConfigurationClass.HUB_HOST == null) {
			host = "localhost";
		} else
			host = ConfigurationClass.HUB_HOST;
		return host;
	}
}