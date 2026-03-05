package com.punchh.server.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Message;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import net.minidev.json.JSONObject;
import org.json.JSONTokener;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.Listeners;

import com.aventstack.extentreports.ExtentReports;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import de.taimos.totp.TOTP;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * �* The Class Utils. �
 */
@Listeners(TestListeners.class)
public class Utilities {

	static Logger logger = LogManager.getLogger(Utilities.class);
	private WebDriver driver;
	static Properties prop = new Properties();
	ExtentReports extentReport;
	public static File jsonFile;
	String regex = "\\((.*?)\\)";
	private static final byte[] keyValue = new byte[] { 'm', 'Y', 'p', 'U', 'b', 'l', 'I', 'c', 'k', 'E', 'y', 'n', 'A',
			'e', 'E', 'M' };
	private static final String ALGO = "AES";
	private static final ObjectMapper objectMapper = new ObjectMapper();

	public Utilities(WebDriver driver) {
		this.driver = driver;
		if (jsonFile == null) {
			jsonFile = new File(System.getProperty("user.dir") + "//resources//Locators//objRepository.json");
		}
	}

	public Utilities() {
		if (jsonFile == null) {
			jsonFile = new File(System.getProperty("user.dir") + "//resources//Locators//objRepository.json");
		}
	}

	public static Logger logUtil(String className) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
		System.setProperty("current.date.time", dateFormat.format(new Date()));
		Logger logger = LogManager.getLogger(className + ".class");
		return logger;
	}

	/**
	 * Load properties file.
	 * 
	 * @param fileName the file name
	 * @return the properties
	 */

	public static Properties loadPropertiesFile(String fileName) {
		String user_dir = System.getProperty("user.dir");
		Properties prop = new Properties();
		try {
			// File path when executing from batch file
			String propertyFilePath = System.getProperty("PropertyFile");
			if (propertyFilePath != null) {
				user_dir = user_dir.replace("bin", "");
			}
			// prop.load(new FileInputStream(user_dir +
			// "//src//test//resources//Properties//" + fileName));
			prop.load(new FileInputStream(user_dir + "//resources//Properties//" + fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return prop;
	}

	public static String getInstance(String env) {
		// Split and get local instance name from env variable if required
		String instance = env;
		if (env.contains("_")) {
			instance = env.split("_")[0];
			logger.info("Instance used for retrieving config is : " + instance);
		}
		return instance.toLowerCase();
	}

	private static String getPropertyValue(String fileName, String key) {
		String propFilePath = System.getProperty("user.dir") + "/resources/Properties/" + fileName;
		String value = "";

		FileInputStream fis;
		try {
			fis = new FileInputStream(propFilePath);
			prop.load(fis);
		} catch (Exception e) {
			e.printStackTrace();
		}

		value = prop.get(key).toString();
		if (StringUtils.isEmpty(value)) {
			try {
				throw new Exception("Unspecified for key: " + key + " in properties file: " + fileName);
			} catch (Exception e) {
			}
		}
		return value;
	}

	public static String getConfigProperty(String key) {
		return getPropertyValue("config.properties", key);
	}

	public static String getApiConfigProperty(String key) {
		return getPropertyValue("apiConfig.properties", key);
	}

	public static String getDBConfigProperty(String key) {
		return getPropertyValue("dbConfig.properties", key);
	}

	public static String getConfigProperty(String env, String key) {
		String instance = getInstance(env);
		return getPropertyValue("config.properties", instance + "." + key);
	}

	public static String getApiConfigProperty(String env, String key) {
		String instance = getInstance(env);
		return getPropertyValue("apiConfig.properties", instance + "." + key);
	}

	public static String getDBConfigProperty(String env, String key) {
		String instance = getInstance(env);
		return getPropertyValue("dbConfig.properties", instance + "." + key);
	}

	// Implicit wait method
	public void implicitWait(long waitTime) {
		try {
			// configProperties =
			// FileLoader.loadPropertiesFile("ia.properties");
			// String wtype = configProperties.getProperty(waitType);
			// long waitDuration = Long.parseLong(wtype);
			driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(waitTime));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	public void implicitWaitMilliSeconeds(long waitTime) {
		try {
			driver.manage().timeouts().implicitlyWait(Duration.ofMillis(waitTime));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
	}

	// Method to generate random numbers
	public static int getRandomNo(int bound) {
		int ri;
		Random randomInt = new Random();
		do {
			ri = randomInt.nextInt(bound);
		} while (ri < 0);
		return ri;
	}

	public static int getRandomNoFromRange(int from, int to) {
		Random random = new Random();
		int n1 = from;
		int n2 = to;
		int randomNumber;
		if (n2 > n1) {
			randomNumber = random.nextInt(n2 - n1) + n1;
		} else {
			randomNumber = n2;
		}
		return randomNumber;
	}

	public static int[] getRandomNoList(int selectCount, int totalCount) {
		int orderOfSelection[] = new int[selectCount];
		Random randomGenerator = new Random();
		for (int i = 0; i < selectCount; i++) {
			boolean flag = true;
			int tempInt;
			do {
				tempInt = randomGenerator.nextInt(totalCount);
			} while (tempInt < 0);
			if (i == 0) {
				orderOfSelection[i] = tempInt;
				logger.debug("Generated : " + orderOfSelection[i]);
			} else {
				for (int cnt = 0; cnt < i; cnt++) {
					if (orderOfSelection[cnt] == tempInt) {
						i--;
						flag = false;
						logger.debug("Already generated, hence changing : " + tempInt);
					}
				}
				if (flag) {
					orderOfSelection[i] = tempInt;
					logger.debug("Generated : " + orderOfSelection[i]);
				}
			}
		}
		return orderOfSelection;
	}

	// Method to generate random numbers with out zero
	public static int getRandomNoWithoutZero(int bound) {
		int ri;
		Random randomInt = new Random();
		do {
			ri = randomInt.nextInt(bound + 1);
		} while (ri <= 0);
		return ri;
	}

	public WebElement getLocator(String locator) {
		String tempLocator, locatorType = null;
		try {
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locator));
			if (temploc.contains(";")) {
				String loc[] = temploc.split(";");
				locatorType = loc[0];
				tempLocator = loc[1];
			} else {
				tempLocator = temploc;
				locatorType = "";
			}
			if (locatorType.equalsIgnoreCase("xpath") || tempLocator.contains("//")) {
				return driver.findElement(By.xpath(tempLocator));
			} else if (locatorType.equalsIgnoreCase("id")) {
				return driver.findElement(By.id(tempLocator));
			} else if (locatorType.equalsIgnoreCase("name")) {
				return driver.findElement(By.name(tempLocator));
			} else if (locatorType.equalsIgnoreCase("css")) {
				return driver.findElement(By.cssSelector(tempLocator));
			} else if (locatorType.equalsIgnoreCase("text")) {
				return driver.findElement(By.linkText(tempLocator));
			} else if (locatorType.equalsIgnoreCase("class")) {
				return driver.findElement(By.className(tempLocator));
			} else if (locatorType.equalsIgnoreCase("tag")) {
				return driver.findElement(By.tagName(tempLocator));
			} else if (locatorType.equalsIgnoreCase("partialText")) {
				return driver.findElement(By.partialLinkText(tempLocator));
			}
		} catch (IOException e) {
			TestListeners.extentTest.get().fail("Error while getting locator: " + e);
			logger.error("Error while getting locator: " + e);
		}
		return null;
	}

	// To return by locator based on the locator type mentioned in json file
	public By getBy(String locatorKey) {
		String tempLocator, locatorType = null;

		try {
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locatorKey));

			if (temploc.contains(";")) {
				String loc[] = temploc.split(";", 2);
				String possibleType = loc[0].trim().toLowerCase();
				if (possibleType.equals("xpath") || possibleType.equals("id") || possibleType.equals("name")
						|| possibleType.equals("css") || possibleType.equals("class") || possibleType.equals("text")
						|| possibleType.equals("tag") || possibleType.equals("partialtext")) {
					locatorType = loc[0].trim();
					tempLocator = loc[1];
				} else {
					tempLocator = temploc;
					locatorType = "";
				}
			} else {
				tempLocator = temploc;
				locatorType = "";
			}

			if (locatorType.equalsIgnoreCase("xpath") || tempLocator.contains("//")) {
				return By.xpath(tempLocator);
			} else if (locatorType.equalsIgnoreCase("id")) {
				return By.id(tempLocator);
			} else if (locatorType.equalsIgnoreCase("name")) {
				return By.name(tempLocator);
			} else if (locatorType.equalsIgnoreCase("css")) {
				return By.cssSelector(tempLocator);
			} else if (locatorType.equalsIgnoreCase("text")) {
				return By.linkText(tempLocator);
			} else if (locatorType.equalsIgnoreCase("class")) {
				if (tempLocator.contains(" ")) {
					return By.cssSelector("." + tempLocator.trim().replaceAll("\\s+", "."));
				}
				return By.className(tempLocator);
			} else if (locatorType.equalsIgnoreCase("tag")) {
				return By.tagName(tempLocator);
			} else if (locatorType.equalsIgnoreCase("partialText")) {
				return By.partialLinkText(tempLocator);
			}

		} catch (Exception e) {
			logger.error("Error while getting locator: " + e);
		}

		throw new RuntimeException("Invalid locator key: " + locatorKey);
	}

	public Map<String, By> getByMap(String pageKey) {
		Map<String, By> locatorMap = new LinkedHashMap<>();
		try {
			JsonNode root = objectMapper.readTree(jsonFile);
			JsonNode pageNode = root.get(pageKey);
			if (pageNode == null || !pageNode.isObject()) {
				throw new RuntimeException("Page key not found in locator JSON: " + pageKey);
			}
			Iterator<String> fieldNames = pageNode.fieldNames();
			while (fieldNames.hasNext()) {
				String key = fieldNames.next();
				locatorMap.put(key, getBy(pageKey + "." + key));
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Error loading locators for page: " + pageKey + " - " + e);
			throw new RuntimeException("Failed to load locators for: " + pageKey, e);
		}
		return locatorMap;
	}

	private static Map<String, By> allLocatorsCache = null;

	public Map<String, By> getAllByMap() {
		if (allLocatorsCache != null) {
			return allLocatorsCache;
		}
		Map<String, By> locatorMap = new LinkedHashMap<>();
		try {
			JsonNode root = objectMapper.readTree(jsonFile);
			Iterator<String> pageKeys = root.fieldNames();
			while (pageKeys.hasNext()) {
				String pageKey = pageKeys.next();
				JsonNode pageNode = root.get(pageKey);
				if (pageNode != null && pageNode.isObject()) {
					Iterator<String> elementKeys = pageNode.fieldNames();
					while (elementKeys.hasNext()) {
						String elementKey = elementKeys.next();
						String fullKey = pageKey + "." + elementKey;
						try {
							locatorMap.put(fullKey, getBy(fullKey));
						} catch (Exception e) {
							logger.warn("Skipping invalid locator: " + fullKey + " - " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error loading all locators: " + e);
			throw new RuntimeException("Failed to load all locators from JSON", e);
		}
		allLocatorsCache = locatorMap;
		logger.info("Loaded " + locatorMap.size() + " locators into global map");
		return allLocatorsCache;
	}

	public List<WebElement> getLocatorList(String locator) {
		String tempLocator, locatorType = null;
		try {
			// jsonFile = new File("src//test//resources//Locators//objRepository.json");
			// jsonFile = new File(System.getProperty("user.dir") +
			// "//resources//Locators//objRepository.json");
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locator));
			if (temploc.contains(";")) {
				String loc[] = temploc.split(";");
				locatorType = loc[0];
				tempLocator = loc[1];
			} else {
				tempLocator = temploc;
				locatorType = "";
			}
			if (locatorType.equalsIgnoreCase("xpath") || tempLocator.contains("//")) {
				return driver.findElements(By.xpath(tempLocator));
			} else if (locatorType.equalsIgnoreCase("id")) {
				return driver.findElements(By.id(tempLocator));
			} else if (locatorType.equalsIgnoreCase("name")) {
				return driver.findElements(By.name(tempLocator));
			} else if (locatorType.equalsIgnoreCase("css")) {
				return driver.findElements(By.cssSelector(tempLocator));
			} else if (locatorType.equalsIgnoreCase("text")) {
				return driver.findElements(By.linkText(tempLocator));
			} else if (locatorType.equalsIgnoreCase("class")) {
				return driver.findElements(By.className(tempLocator));
			} else if (locatorType.equalsIgnoreCase("tag")) {
				return driver.findElements(By.tagName(tempLocator));
			} else if (locatorType.equalsIgnoreCase("partialText")) {
				return driver.findElements(By.partialLinkText(tempLocator));
			}
		} catch (IOException e) {
			TestListeners.extentTest.get().fail("Error while getting locator: " + e);
			logger.error("Error while getting locator: " + e);
		}
		return null;
	}

	public String getLocatorValue(String locator) {
		@SuppressWarnings("unused")
		String tempLocator, locatorType = null;
		try {
			// jsonFile = new File("src//test//resources//Locators//objRepository.json");
			// jsonFile = new File(System.getProperty("user.dir") +
			// "//resources//Locators//objRepository.json");
			String temploc = ((String) JsonPath.read(jsonFile, "$." + locator));
			if (temploc.contains(";")) {
				String loc[] = temploc.split(";");
				locatorType = loc[0];
				tempLocator = loc[1];
			} else {
				tempLocator = temploc;
				locatorType = "";
			}
			return tempLocator;
		} catch (IOException e) {
			TestListeners.extentTest.get().fail("Error while getting locator: " + e);
			logger.error("Error while getting locator: " + e);
		}
		return null;
	}

	// Converts a string to By using the appropriate locator type
	public By getByLocator(String locatorType, String locatorValue) {
		switch (locatorType.toLowerCase()) {
		case "id":
			return By.id(locatorValue);
		case "name":
			return By.name(locatorValue);
		case "xpath":
			return By.xpath(locatorValue);
		case "css":
			return By.cssSelector(locatorValue);
		case "class":
			return By.className(locatorValue);
		case "tag":
			return By.tagName(locatorValue);
		case "linktext":
			return By.linkText(locatorValue);
		case "partiallinktext":
			return By.partialLinkText(locatorValue);
		default:
			throw new IllegalArgumentException("Invalid locator type: " + locatorType);
		}
	}

	public static int[] getOrderOfSelection(int columnCount, int selectColumn) {
		if (selectColumn > columnCount) {

			logger.info("selectColumn ");
		}
		int orderOfSelection[] = new int[selectColumn];
		Random randomGenerator = new Random();
		for (int idx = 0; idx < selectColumn; idx++) {
			boolean ins = true;
			int tempInt;
			do {
				tempInt = randomGenerator.nextInt(columnCount);
			} while (tempInt < 0);

			if (idx == 0) {
				orderOfSelection[idx] = tempInt;
				logger.debug("Generated : " + orderOfSelection[idx]);
			} else {
				for (int cnt = 0; cnt < idx; cnt++) {
					if (orderOfSelection[cnt] == tempInt) {
						idx--;
						ins = false;
						logger.debug("Already generated, hence changing : " + tempInt);
					}
				}
				if (ins) {
					orderOfSelection[idx] = tempInt;
					logger.debug("Generated : " + orderOfSelection[idx]);
				}
			}
		}
		for (int idx = 0; idx < selectColumn; idx++) {
		}
		return orderOfSelection;
	}

	public static int[] getOrderOfSelectionWithoutZero(int columnCount, int selectColumn) {
		int orderOfSelection[] = new int[selectColumn];

		Random randomGenerator = new Random();

		if (columnCount < selectColumn) {
			return getOrderOfSelectionWithoutZero(columnCount, columnCount);
		}

		if (columnCount == 1 && selectColumn == 1) {
			orderOfSelection[0] = 1;
			return orderOfSelection;
		}

		for (int idx = 0; idx < selectColumn; idx++) {
			boolean ins = true;
			int tempInt;
			do {
				tempInt = randomGenerator.nextInt(columnCount + 1);
			} while (tempInt < 0 || tempInt == 0);

			if (idx == 0) {
				orderOfSelection[idx] = tempInt;
				logger.debug("Generated : " + orderOfSelection[idx]);
			} else {
				for (int cnt = 0; cnt < idx; cnt++) {
					if (orderOfSelection[cnt] == tempInt) {
						idx--;
						ins = false;
						logger.debug("Already generated, hence changing : " + tempInt);
					}
				}
				if (ins) {
					orderOfSelection[idx] = tempInt;
					logger.debug("Generated : " + orderOfSelection[idx]);
				}
			}
		}

		for (int idx = 0; idx < selectColumn; idx++) {
		}
		return orderOfSelection;
	}

	/*
	 * public static String screenShotCaptureOld(WebDriver driver, String
	 * screeshotDescription) { String destFile = null; /* String tm =
	 * getTimestamp(); destFile = ExtentReportManager.reportPathNValue + "/" + tm +
	 * "_" + screeshotDescription + ".png"; String path = "./" + tm + "_" +
	 * screeshotDescription + ".png";
	 */
	/*
	 * destFile = ExtentReportManager.reportPathNValue + "/" + screeshotDescription
	 * + ".png"; String path = "./" + screeshotDescription + ".png"; try { File
	 * screenShotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
	 * FileUtils.copyFile(screenShotFile, new File(destFile));
	 * TestListeners.extentTest.get().addScreenCaptureFromPath(path);
	 * TestListeners.extentTest.get().info("Screenshot saved for report: " +
	 * destFile); logger.info("Screenshot saved for report: " + destFile); } catch
	 * (Exception e) { TestListeners.extentTest.get().
	 * fail("Error while saving screenshot for report: " + e);
	 * logger.error("Error while saving screenshot for report: " + e); } return
	 * destFile; }
	 */

	/*
	 * public static String screenShotCaptureBase64Old(WebDriver driver, String
	 * screenshotDescription) { String destFile = null; try { // Capture screenshot
	 * as Base64 TakesScreenshot ts = (TakesScreenshot) driver; String
	 * base64Screenshot = ts.getScreenshotAs(OutputType.BASE64);
	 * 
	 * // Decode Base64 to bytes byte[] decodedBytes =
	 * Base64.getDecoder().decode(base64Screenshot);
	 * 
	 * // Define destination file path destFile =
	 * ExtentReportManager.reportPathNValue + "/" + screenshotDescription + ".png";
	 * Path destination = Paths.get(destFile); Files.write(destination,
	 * decodedBytes);
	 * 
	 * // For relative path in report String relativePath = "./" +
	 * screenshotDescription + ".png";
	 * 
	 * // Add screenshot to Extent report
	 * TestListeners.extentTest.get().addScreenCaptureFromPath(relativePath);
	 * TestListeners.extentTest.get().info("Screenshot saved for report: " +
	 * destFile); logger.info("Screenshot saved for report: " + destFile);
	 * 
	 * } catch (Exception e) { TestListeners.extentTest.get().
	 * fail("Error while saving screenshot for report: " + e);
	 * logger.error("Error while saving screenshot for report: " + e); } return
	 * destFile; }
	 */

	public static String screenShotCapture(WebDriver driver, String screenshotDescription) {
		try {
			TakesScreenshot ts = (TakesScreenshot) driver;

			// Capture screenshot as Base64
			String base64Screenshot = ts.getScreenshotAs(OutputType.BASE64);

			// Embed directly into Extent report (no file saving)
			TestListeners.extentTest.get().addScreenCaptureFromBase64String(base64Screenshot, screenshotDescription);
			TestListeners.extentTest.get().info("Screenshot embedded for: " + screenshotDescription);
			logger.info("Screenshot embedded for: " + screenshotDescription);

			return "Screenshot embedded for: " + screenshotDescription;

		} catch (Exception e) {
			TestListeners.extentTest.get().fail("Error while capturing screenshot: " + e);
			logger.error("Error while capturing screenshot: ", e);
			return "Screenshot capture failed: " + e.getMessage();
		}
	}

	public static String getTimestamp() {
		String tm = CreateDateTime.getCurrentSystemDateAndYear()
				+ Integer.toString(CreateDateTime.getHourAndMinuteForCurrentSystemTime(0))
				+ Integer.toString(CreateDateTime.getHourAndMinuteForCurrentSystemTime(1))
				+ Integer.toString(CreateDateTime.getHourAndMinuteForCurrentSystemTime(2));
		return tm;

	}

	public static String getDateTime() {
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		String dateStr = dateFormat.format(date);
		return dateStr;
	}

	public static String getDate() {
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		String dateStr = dateFormat.format(date);
		return dateStr;
	}

	public String nextdaysDate() {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, +1);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(cal.getTime());
	}

	public static String getTimeCalculation(String startTime, String endTime) throws ParseException {
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		Date startDate = dateFormat.parse(startTime);
		Date endDate = dateFormat.parse(endTime);
		long difference = endDate.getTime() - startDate.getTime();
		String diffSeconds = String.valueOf(difference / 1000 % 60);
		String diffMinutes = String.valueOf(difference / (60 * 1000) % 60);
		String diffHours = String.valueOf(difference / (60 * 60 * 1000) % 24);
		String diffDays = String.valueOf(difference / (24 * 60 * 60 * 1000));
		String str = diffDays + " " + diffHours + ":" + diffMinutes + ":" + diffSeconds;
		return str;
	}

	public static String formatNumber(String StrVal, int decimalPrecision) {
		String rowVal = null;
		String str = "";
		while (decimalPrecision > 0) {
			str += "#";
			decimalPrecision--;
		}
		NumberFormat formatter = new DecimalFormat("#0." + str);
		double doub = Double.parseDouble(StrVal);
		rowVal = formatter.format(doub);
		return rowVal;
	}

	public static String formateDate(String strVal) throws ParseException {
		String rowVal = null;
		try {
			SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy");
			Date date = inputFormat.parse(strVal);
			rowVal = outputFormat.format(date);
		} catch (Exception e) {
			logger.error("Error in formatting date " + e);
			// TestListeners.extentTest.get().fail("Error in formatting date " + e);
		}
		return rowVal;
	}

	public static String getNumberFormat(int decimalPrecision) {
		String str = "";
		while (decimalPrecision > 0) {
			str += "#";
			decimalPrecision--;
		}
		// NumberFormat formatter = new DecimalFormat("#0."+ str);
		return str;
	}

	public static String dateFormatInMMDDYYYY() {
		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
		String s = formatter.format(date);
		return s;
	}

	public static String getLaggedTime() throws ParseException {
		SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
		Date now = new Date();
		String time = sdfTime.format(now);
		Date d = sdfTime.parse(time);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.add(Calendar.MINUTE, 10);
		time = sdfTime.format(cal.getTime());
		return time;
	}

	public static void dragAndMoveOffset(WebDriver driver, WebElement source, WebElement destination, int xOffSet,
			int yOffSet) {
		try {
			Actions action = new Actions(driver);
			action.moveToElement(source).pause(5000).clickAndHold(source).pause(3000).moveByOffset(0, 0)
					.moveToElement(destination).moveByOffset(xOffSet, yOffSet).pause(3000).release().perform();
		} catch (Exception e) {
			logger.error("Error in drag and move offset: " + e);
			TestListeners.extentTest.get().fail("Error in drag and move offset: " + e);
		}
	}

	public boolean fileDownloadCheck(String reportName, String fileType) {
		boolean flag = false;
		try {
			prop = Utilities.loadPropertiesFile("config.properties");
			File file = new File(prop.getProperty("downloadPath") + reportName + fileType);
			long sizeInKB = file.length() / 1024 + 1;
			if (file.exists()) {
				logger.info("File: " + reportName + fileType + " is downloaded successfully");
				TestListeners.extentTest.get().pass("File: " + reportName + fileType + " is downloaded successfully");
				fileSizeCheck(reportName, sizeInKB);
				flag = true;
			} else {
				logger.error("File: " + reportName + fileType + " is not downloaded");
				TestListeners.extentTest.get().fail("File: " + reportName + fileType + " is not downloaded");
			}
		} catch (Exception e) {
			logger.error("Error in downloading or verifying report: " + e);
			TestListeners.extentTest.get().fail("Error in downloading or verifying report: " + e);
		}
		return flag;
	}

	public static boolean fileSizeCheck(String fileName, long sizeinBytes) {
		try {
			if (sizeinBytes > 0) {
				logger.info("File: " + fileName + " size is: " + sizeinBytes + " bytes");
				TestListeners.extentTest.get().pass("File: " + fileName + "size is: " + sizeinBytes + " bytes");
				return true;
			} else {
				logger.error("File: " + fileName + " size is: " + sizeinBytes);
				TestListeners.extentTest.get().fail("File: " + fileName + " size is: " + sizeinBytes);
			}
		} catch (Exception e) {
			logger.error("Something went wrong while performing file size check, Exception: " + e);
			TestListeners.extentTest.get()
					.fail("Something went wrong while performing file size check, Exception: " + e);
		}
		return false;
	}

	public static void deleteExistingDownload(String itemName) {
		try {
			System.out.println("--");
			prop = Utilities.loadPropertiesFile("config.properties");
			File file = new File(System.getProperty("user.dir") + prop.getProperty("downloadPath") + itemName);
			if (file.exists()) {
				file.delete();
				logger.info("Deleted existing item: " + itemName);
				TestListeners.extentTest.get().info("Deleted existing item: " + itemName);
			}
		} catch (Exception e) {
			logger.error("Something went wrong while deleting existing download, Exception: " + e);
			TestListeners.extentTest.get()
					.fail("Something went wrong while deleting existing download, Exception: " + e);
		}
	}

	public static String getFutureDate(int num) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, num);
		String output = sdf.format(c.getTime());
		return output;
	}

	public static String yesterdaysDate() {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		return dateFormat.format(cal.getTime());
	}

	public static String getUpcomingDate(int num, String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		Date currentDate = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(currentDate);
		c.add(Calendar.YEAR, 0);
		c.add(Calendar.MONTH, 0);
		c.add(Calendar.DATE, num);
		c.add(Calendar.HOUR, 0);
		c.add(Calendar.MINUTE, 0);
		c.add(Calendar.SECOND, 0);
		Date upcomingDate = c.getTime();
		return dateFormat.format(upcomingDate);
	}

	public static String getCurrentDate(String format) {
		DateFormat dateFormat = new SimpleDateFormat(format);
		Date currentDate = new Date();
		return dateFormat.format(currentDate);
	}

	public void fileDownloadCheck(String reportName, String fileType, String path) {
		try {
			File file = new File(path);
			long sizeInKB = file.length() / 1024 + 1;
			if (sizeInKB > 0) {
				logger.info("File: " + reportName + " size is: " + sizeInKB + " KB");
				TestListeners.extentTest.get().pass("File: " + reportName + "size is: " + sizeInKB + " KB");
			} else {
				logger.info("File: " + reportName + " size is: " + sizeInKB + " KB which is less than 1 KB");
				TestListeners.extentTest.get()
						.fail("File: " + reportName + " size is: " + sizeInKB + " KB which is less than 1 KB");
			}
		} catch (Exception e) {
			logger.error("Error in downloading or verifying report: " + e);
			TestListeners.extentTest.get().fail("Error in downloading or verifying report: " + e);
		}
	}

	public static void deleteIfFileExists(String fileName, String ext) {
		try {
			File file = new File(prop.getProperty("downloadPath") + fileName + ext);
			if (file.exists()) {
				file.delete();
				logger.info("Existing file " + fileName + ext + " is deleted successfully");
				TestListeners.extentTest.get().info("Existing file " + fileName + ext + " is deleted successfully");
			}
		} catch (Exception e) {
			logger.error("Error in deleting existing file, Exception: " + e);
			TestListeners.extentTest.get().fail("Error in deleting existing file, Exception: " + e);
		}
	}

	public String filetoString(String path) throws IOException {
		return new String(Files.readAllBytes(Paths.get(path)));
	}

	/*
	 * public void setRobotText(String text) throws InterruptedException { try {
	 * StringSelection stringSelection = new StringSelection(text); Clipboard
	 * clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	 * clipboard.setContents(stringSelection, stringSelection); Robot robot = new
	 * Robot(); robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_V);
	 * robot.keyRelease(KeyEvent.VK_V); robot.keyRelease(KeyEvent.VK_CONTROL);
	 * Thread.sleep(3000); robot.keyPress(KeyEvent.VK_TAB);
	 * robot.keyRelease(KeyEvent.VK_TAB); } catch (HeadlessException e) {
	 * logger.error("Error:: " + e); TestListeners.extentTest.get().fail("Error:: "
	 * + e); } catch (AWTException e) { logger.error("AWT exception:: " + e);
	 * TestListeners.extentTest.get().fail("AWT exception:: " + e); } }
	 */

	public static void clearLogFile() {
		FileReader fr = null;
		FileWriter fw = null;
		String user_dir = System.getProperty("user.dir");
		try {
			fr = new FileReader(user_dir + "//logs//AutomationLog.log");
			fw = new FileWriter(user_dir + "//logs//AutomationOldLogFile.log", true);
			int c = fr.read();
			fw.write("\r\n");
			while (c != -1) {
				fw.write(c);
				c = fr.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fr.close();
				fw.close();
				FileOutputStream writer = new FileOutputStream(user_dir + "//logs//AutomationLog.log");
				writer.write(("").getBytes());
				writer.close();
			} catch (IOException e) {
				logger.info(e);
			}
		}
	}

	public String getexportedFileName(String str) {
		String exportedFileName = null;
		Matcher m = Pattern.compile(regex).matcher(str);
		while (m.find()) {
			exportedFileName = (m.group(1));
		}
		return exportedFileName;
	}

	public List<String> getexportedFileNameList(String string) {
		List<String> matchList = new ArrayList<String>();
		Pattern regex = Pattern.compile("\\((.*?)\\)");
		Matcher regexMatcher = regex.matcher(string);
		while (regexMatcher.find()) {// Finds Matching Pattern in String
			matchList.add(regexMatcher.group(1));// Fetching Group from String
		}
		return matchList;
	}

	public boolean downloadFTPFile(String fileName, String path) throws InterruptedException {
		FTPClient client = new FTPClient();
		try {
			prop = Utilities.loadPropertiesFile("config.properties");
			client.connect(prop.getProperty("ftpIp"), Integer.parseInt(prop.getProperty("ftpPort")));
			boolean login = client.login(prop.getProperty("ftpUsername"), prop.getProperty("ftpPassword"));
			client.enterLocalPassiveMode();
			client.setFileType(FTP.BINARY_FILE_TYPE);
			client.setAutodetectUTF8(true);
			client.enterLocalPassiveMode();
			if (login) {
				String fileNameTemp = path + fileName;
				InputStream inputStream = client.retrieveFileStream(fileNameTemp);
				FileOutputStream out = new FileOutputStream(
						System.getProperty("user.dir") + "/resources/ExportData/" + fileName);
				org.apache.commons.io.IOUtils.copy(inputStream, out);
				out.close();
				if (verifyFileExists(fileName)) {
					logger.info("Verified segment export file download: " + fileName);
					TestListeners.extentTest.get().pass("Verified segment export file download: " + fileName);
				} else {
					logger.error("Failed to verify segment export file download");
					TestListeners.extentTest.get().fail("Failed to verify segment export file download");
				}
			} else {
				logger.error("Failed to connect to FTP");
				TestListeners.extentTest.get().fail("Failed to connect to FTP");
			}
		} catch (IOException e) {
			logger.error("Error in downloading file via FTP " + e);
			TestListeners.extentTest.get().fail("Error in downloading file via FTP " + e);
			Assert.fail("Error in downloading file via FTP");
		} finally {
			try {
				client.disconnect();
			} catch (IOException e) {
				logger.info(e);
				e.printStackTrace();
			}
		}
		return false;
	}

	public boolean verifyFileExists(String fileName) {
		boolean flag = false;
		try {
			// File file = new File(System.getProperty("user.dir") +
			// "/src/test/resources/ExportData/" + fileName);
			File file = new File(System.getProperty("user.dir") + "/resources/ExportData/" + fileName);
			// file = new File(prop.getProperty("exportFileLoc") + fileName);
			if (file.exists()) {
				long sizeInKB = file.length();
				flag = fileSizeCheck(fileName, sizeInKB);
				logger.info("Verified exported file exists: " + fileName);
				TestListeners.extentTest.get().pass("Verified exported file exists: " + fileName);
				return flag;
			} else {
				logger.error("File doesn't exists: " + fileName);
				TestListeners.extentTest.get().fail("File doesn't exists: " + fileName);
			}
		} catch (Exception e) {
			logger.error("Error in deleting existing file, Exception: " + e);
			TestListeners.extentTest.get().fail("Error in deleting existing file, Exception: " + e);
		}
		return flag;
	}

	public boolean checkElementPresent(WebElement element) {
		boolean ElementPresent = false;
		try {
			implicitWait(3);
			if (element.isDisplayed()) {
				ElementPresent = true;
				logger.info("Element:" + element + " is present");
			}
		} catch (Exception e) {
			logger.info("Element:" + element + " is not present " + e);
		}
		implicitWait(60);
		return ElementPresent;
	}

	public boolean isElementPresent(By locator) {
		try {
			WebElement element = driver.findElement(locator); // Try to find the element
			return element != null; // Return true if element is found
		} catch (NoSuchElementException e) {
			return false; // Return false if element is not found
		}
	}

	public boolean isElementVisible(By locator) {
		try {
			WebElement element = driver.findElement(locator);
			return element != null && element.isDisplayed(); // Check if element is displayed
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public boolean isTextPresent(WebDriver driver, String text) {
		try {
			logger.info("Text to find : " + text);
			logger.info("Using Xpath to search text : " + "//*[contains(text(), '" + text + "')]");
			WebElement element = driver.findElement(By.xpath("//*[contains(text(), '" + text + "')]"));
			if (element.isDisplayed()) {
				logger.info("Text found");
				return true;
			}
		} catch (org.openqa.selenium.NoSuchElementException | org.openqa.selenium.StaleElementReferenceException e) {
			return false;
		}
		return false;
	}

	public boolean StaleElementclick(WebDriver driver, WebElement element) {
		boolean result = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				scrollToElement(driver, element);
				longWaitInSeconds(1);
				clickByJSExecutor(driver, element);
				// element.click();
				result = true;
				break;
			} catch (StaleElementReferenceException e) {
			} catch (Exception e) {
			}
			attempts++;
		}
		return result;
	}

	// to handle ElementClickInterceptedException and StaleElementReferenceException
	public void clickWithRetry(WebElement element, int timeoutInSeconds, int pollingInMillis) {
		FluentWait<WebDriver> wait = new FluentWait<>(driver).withTimeout(Duration.ofSeconds(timeoutInSeconds))
				.pollingEvery(Duration.ofMillis(pollingInMillis)).ignoring(ElementClickInterceptedException.class)
				.ignoring(StaleElementReferenceException.class);
		wait.until(driver -> {
			try {
				element.click();
				logger.info("Clicked on element: " + element);
				TestListeners.extentTest.get().info("Clicked on element: " + element);
				return true; // success
			} catch (ElementClickInterceptedException e) {
				logger.warn("ElementClickInterceptedException: Retrying click on element: " + element);
				return false; // retry
			}
		});
	}

	public void scrollToElement(WebDriver driver, WebElement element) {
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		jse.executeScript("arguments[0].scrollIntoView(true);", element);
	}

	public void clickByJSExecutor(WebDriver driver, WebElement element) {
		JavascriptExecutor jse = (JavascriptExecutor) driver;
		jse.executeScript("arguments[0].click();", element);
	}

	public void selectDrpDwnValue(WebElement ele, String value) {
		Select option = new Select(ele);
		// option.selectByValue("");
		option.selectByVisibleText(value);
	}

	public boolean selectListDrpDwnValue(List<WebElement> elements, String value) {
		boolean valueFound = false;
		for (int i = 0; i < elements.size(); i++) {
			// logger.info(elements.get(i).getText());
			if (elements.get(i).getText().equalsIgnoreCase(value)) {
				WebElement ele = elements.get(i);
				ele.click();
				// ((JavascriptExecutor)
				// driver).executeScript("arguments[0].click();",ele);
				/*
				 * Actions action = new Actions(driver);
				 * action.moveToElement(elements.get(i)).click().build().perform();
				 */
				logger.info("Selected dropdown value as: " + value);
				valueFound = true;
				break;
			}
		}
		if (!valueFound) {
			logger.warn("Dropdown value not found: " + value);
		}
		return valueFound;
	}

	public void selectValueFromSpanExpandedDropdown(String value) {
		// Make sure dropdown is already clicked and expanded
		try {
			// No risk of selecting same value in other dropdowns as once value is selected
			// it changes to span from li
			String optionXpath = String.format("//li[text()=\"%s\"]", value);
			// Directly find the option with the matching text
			WebElement matchingOption = driver.findElement(By.xpath(optionXpath));
			if (matchingOption != null) {
				matchingOption.click();
				logger.info("Successfully selected the option: '" + value + "'");
				TestListeners.extentTest.get().info("Successfully selected the option: '" + value + "'");
			} else {
				logger.warn("No matching option found for value: '" + value + "'");
				TestListeners.extentTest.get().warning("No matching option found for value: '" + value + "'");
			}
		} catch (Exception e) {
			logger.error("Error occurred while interacting with the dropdown: '" + value + "'", e);
			TestListeners.extentTest.get().fail("Error occurred while interacting with the dropdown: '" + value + "'");
		}
	}

	public void selecDrpDwnValue(List<WebElement> elements, String value) {
		for (int i = 0; i < elements.size(); i++) {
			// System.out.println(elements.get(i).getText());
			if (elements.get(i).getText().equalsIgnoreCase(value)) {
				// WebElement ele = elements.get(i);
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", elements.get(i));
				logger.info("Slected dropdown value as: " + value);
				break;
			}
		}
	}

	public static boolean isAlertpresent(WebDriver driver) {
		try {
			driver.switchTo().alert();
			return true;

		} catch (NoAlertPresentException e) {
			logger.info("Alert is not present " + e);
			return false;
		}
	}

	public void acceptAlert(WebDriver driver) {
		if (isAlertpresent(driver)) {
			logger.info("Element: Alert is present");
			Alert alert = driver.switchTo().alert();
			alert.accept();
		}
	}

	public void clearDataSet(Map<?, ?> dataSet) {
		if (dataSet != null && !dataSet.isEmpty()) {
			dataSet.clear();
			logger.info("Test dataSet cleared...");
			TestListeners.extentTest.get().info("Test dataSet cleared...");
		}
	}

	public void deleteCampaignFromDb(String campaignName, String env) throws Exception {
		if (campaignName == null) {
			logger.info("Campaign name is null. Skipping deletion.");
			return;
		}

		String camTypeLower = campaignName.toLowerCase();
		if (camTypeLower.contains("postcheckin") || camTypeLower.contains("postmessage")
				|| camTypeLower.contains("dashboardcheckin")) {
			deleteCampaignByNameFromFreePunchhCampaignsTable(campaignName, env);
		} else if (camTypeLower.contains("mass")) {
			deleteCampaignByNameFromMassGiftingTable(campaignName, env);
		} else {
			deleteCampaignByNameFromCampaignsTable(campaignName, env);
		}
	}

	// use only for signup,posredemption,postpayment,anniversary type campaigns
	public void deleteCampaignByNameFromCampaignsTable(String campaignName, String env) throws Exception {
		if (campaignName != null && !campaignName.isEmpty()) {
			String query = "DELETE FROM campaigns WHERE name = '" + campaignName + "';";
			DBUtils.executeQuery(env, query);
		}
	}

	// use for postcheckins post message campaigns
	public void deleteCampaignByNameFromFreePunchhCampaignsTable(String campaignName, String env) throws Exception {
		if (campaignName != null && !campaignName.isEmpty()) {
			String query = "DELETE FROM free_punchh_campaigns WHERE name = '" + campaignName + "';";
			DBUtils.executeQuery(env, query);
		}
	}

	// use for mass offer/notification campaigns deletion with schedule deleteion
	public void deleteCampaignByNameFromMassGiftingTable(String campaignName, String env) throws Exception {

		// delete schedule associated with mass campaign
		String massCampaignId = getCamiDByNameFromMassGiftingTable(campaignName, env);
		if (massCampaignId != null && !massCampaignId.isEmpty()) {
			String scheduleDeleteQuery = "DELETE FROM schedules WHERE source_id = " + massCampaignId + ";";
			DBUtils.executeQuery(env, scheduleDeleteQuery);
		}
		// delete mass offer/notification campaign
		if (campaignName != null && !campaignName.isEmpty()) {
			String query = "DELETE FROM mass_giftings WHERE name = '" + campaignName + "';";
			DBUtils.executeQuery(env, query);
		}

	}

	public String getCamiDByNameFromMassGiftingTable(String campaignName, String env) throws Exception {
		String camId = null;
		if (campaignName != null && !campaignName.isEmpty()) {
			String query = "SELECT id FROM mass_giftings WHERE name = '" + campaignName + "';";
			camId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		}
		return camId;
	}

	// use for only for signup,posredemption,postpayment type campaigns
	public void deleteCampaignByIdCampaignsTable(String campaignId, String env) throws Exception {
		if (campaignId != null && !campaignId.isEmpty()) {
			String query = "DELETE FROM campaigns WHERE id= " + campaignId + ";";
			DBUtils.executeQuery(env, query);
		}
	}

	// to delete redeemable by name
	public void deleteRedeemableByName(String redeemableName, String env) throws Exception {
		if (redeemableName != null && !redeemableName.isEmpty()) {
			String query = "DELETE FROM redeemables WHERE name= '" + redeemableName + "';";
			DBUtils.executeQuery(env, query);
		}
	}

	// to delete location by locationname
	public void deleteLocationByName(String locationName, String businessId, String env) throws Exception {
		if (locationName != null && !locationName.isEmpty()) {
			String query = "DELETE FROM locations WHERE name = '" + locationName + "' and business_id = " + businessId
					+ " ;";
			DBUtils.executeQuery(env, query);
		}
	}

	public String getNumbersFromString(String str) {
		Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?)");
		Matcher m = p.matcher(str);
		String temp = null;
		while (m.find()) {
			temp = m.group();
		}
		return temp;
	}

	public static void longWait(long waitDuration) {
		try {
			Thread.sleep(waitDuration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void longwait(long waitDuration) {
		try {
			Thread.sleep(waitDuration);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void longWaitInSeconds(Integer seconds) {
		try {
			logger.info("Initiating a wait period of {} seconds.", seconds);
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void longWaitInMiliSeconds(Integer milliseconds) {
		try {
			logger.info("Initiating a wait period of {} milliseconds.", milliseconds);
			TimeUnit.MILLISECONDS.sleep(milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public String removeLeadingZero(String str) {
		String strPattern = "^0+(?!$)";
		str = str.replaceAll(strPattern, "");
		return str;
	}

	public boolean existsElement(String loc) {
		try {
			getLocator(loc);
		} catch (NoSuchElementException e) {
			return false;
		}
		return true;
	}

	public static String getAlphaNumericString(int n) {

		String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";

		// create StringBuffer size of AlphaNumericString
		StringBuilder sb = new StringBuilder(n);

		for (int i = 0; i < n; i++) {

			int index = (int) (AlphaNumericString.length() * Math.random());

			// add Character one by one in end of sb
			sb.append(AlphaNumericString.charAt(index));
		}

		return sb.toString();
	}

	public static Response updateJiraScale(String tcNumber, String cycleKey, String status) {
		Response response = null;
		JSONObject requestParams = new JSONObject();
		try {
			RestAssured.baseURI = "https://api.zephyrscale.smartbear.com";
			RequestSpecification request = RestAssured.given().log().all();
			requestParams.put("projectKey", "SQ");
			requestParams.put("testCaseKey", tcNumber);
			requestParams.put("testCycleKey", cycleKey);
			requestParams.put("statusName", status);
			request.headers("Authorization",
					"Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NjJiNDQ4MC1iZjE0LTMzZGMtYmQ4Yy1mNjBmYmM2MWE3ZjUiLCJjb250ZXh0Ijp7ImJhc2VVcmwiOiJodHRwczpcL1wvcHVuY2hoZGV2LmF0bGFzc2lhbi5uZXQiLCJ1c2VyIjp7ImFjY291bnRJZCI6IjYwY2M3Yjc5YjIxNTYxMDA2OWJkMmY3ZSJ9fSwiaXNzIjoiY29tLmthbm9haC50ZXN0LW1hbmFnZXIiLCJleHAiOjE2Njk5MTU0OTQsImlhdCI6MTYzODM3OTQ5NH0.Cis4rmtM8WBEfGku4B0nxlybCTuZN3SnjQ0q_BEhZk0");
			request.headers("Content-Type", "application/json");
			// request.body(requestParams.toJSONString());
			request.body(requestParams.toString());
			response = request.post("/v2/testexecutions");
			logger.info(response.asString());
			logger.info(response.statusCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public String getLatestCycleKeyZephyr(String nameContains) {
		try {
			RestAssured.baseURI = getConfigProperty("zephyr.baseUrl");
			Response response = RestAssured.given()
					// .log().all()
					.header("Authorization", "Bearer " + decrypt(getConfigProperty("zephyr.token")))
					.header("Accept", "application/json")
					.queryParam("projectKey", getConfigProperty("zephyr.projectKey")).queryParam("maxResults", 1000)
					.get("testcycles");

			if (response.getStatusCode() != 200) {
				logger.warn("Zephyr - Failed to fetch test cycles. Response code: " + response.getStatusCode()
						+ ". Response body: " + response.getBody().asString());
				return null;
			}

			JSONArray cycles = new JSONObject(response.getBody().asString()).getJSONArray("values");
			String latestCycleKey = null;
			String latestCycleName = null;
			long highestNumericId = -1;

			for (int i = 0; i < cycles.length(); i++) {
				JSONObject cycle = cycles.getJSONObject(i);
				String cycleName = cycle.optString("name", "");
				String cycleKey = cycle.optString("key", "");
				long cycleId = cycle.optLong("id", -1);

				if (cycleName.toLowerCase().contains(nameContains.toLowerCase())) {
					if (cycleId > highestNumericId) {
						highestNumericId = cycleId;
						latestCycleKey = cycleKey;
						latestCycleName = cycleName;
					}
				}
			}

			if (latestCycleKey != null) {
				logger.info("Zephyr - Latest cycle key found: " + latestCycleKey + " (Name: " + latestCycleName + ")");
			} else {
				logger.warn("Zephyr - No cycle found containing: " + nameContains);
			}

			return latestCycleKey;

		} catch (Exception e) {
			logger.warn("Zephyr - Exception occurred while fetching cycle key: ", e);
			return null;
		}
	}

	public void updateZephyrTCViaDescription(ITestResult result, String jiraCycleKey, String status) throws Exception {
		logger.info("Zephyr - Updating Jira for test case : " + result.getMethod().getMethodName());

		String testCaseDescription = result.getMethod().getDescription();
		if (testCaseDescription == null || testCaseDescription.isEmpty()) {
			logger.warn("Zephyr - No description found in the test method. Skipping update.");
			TestListeners.extentTest.get()
					.warning("Zephyr - No description found in the test method. Skipping update.");
			return;
		}

		if (jiraCycleKey == null || status == null || jiraCycleKey.isEmpty() || status.isEmpty()) {
			logger.warn("Invalid input parameters provided for updating Zephyr test case." + " jiraCycleKey: "
					+ jiraCycleKey + ", status: " + status);
			TestListeners.extentTest.get().warning("Invalid input parameters provided for updating Zephyr test case."
					+ " jiraCycleKey: " + jiraCycleKey + ", status: " + status);
			return;
		}

		// Regex pattern to extract SQ-Tx.. test case IDs
		Pattern pattern = Pattern.compile("SQ[-_]T\\d+");
		Matcher matcher = pattern.matcher(testCaseDescription);

		// Replace underscores with hyphens and collect all matches
		List<String> testCaseIds = new ArrayList<>();
		while (matcher.find()) {
			testCaseIds.add(matcher.group().replace('_', '-'));
		}

		if (testCaseIds.isEmpty()) {
			logger.warn("\"==== Zephyr - No test case IDs found in description:" + testCaseDescription + " ===== ");
			TestListeners.extentTest.get().warning(
					"\"==== Zephyr - No test case IDs found in description:" + testCaseDescription + " ===== ");
			// Assert.fail("Zephyr - No test case IDs found in description: " +
			// testCaseDescription);
			return;
		}

		logger.info("Zephyr - Extracted Test Case IDs for Zephyr update: " + testCaseIds);

		for (String tcId : testCaseIds) {
			updateZephyrTestExecutionStatus(tcId, jiraCycleKey, status);
		}
	}

	public void updateZephyrTestExecutionStatus(String tcId, String cycleKey, String status) throws Exception {
		String assignedId = null;

		assignedId = getLatestExecutionAssignedToId(cycleKey, tcId);
		if (assignedId == null || assignedId.isEmpty()) {
			logger.warn("Zephyr - No assignedToId found for test case " + tcId);
			assignedId = getLatestExecutionAssignedToId(cycleKey, tcId);
		}
		logger.info("Assigned ID: " + assignedId);

		JSONObject updateBody = new JSONObject().put("projectKey", getConfigProperty("zephyr.projectKey"))
				.put("testCaseKey", tcId).put("testCycleKey", cycleKey).put("statusName", status)
				.put("assignedToId", assignedId).put("executedById", prop.getProperty("executedById"));

		RestAssured.baseURI = getConfigProperty("zephyr.baseUrl");
		Response response = RestAssured.given()
				.header("Authorization", "Bearer " + decrypt(getConfigProperty("zephyr.token")))
				.header("Content-Type", "application/json").body(updateBody.toString()).post("testexecutions");

		if (response.getStatusCode() != 201 && response.getStatusCode() != 502) {
			logger.error("Zephyr - Failed to update status for test case: " + tcId + ". Request body: " + updateBody
					+ ". Response code: " + response.getStatusCode() + ". Response body: "
					+ response.getBody().asString());
			TestListeners.extentTest.get()
					.fail("Zephyr - Failed to update status for test case: " + tcId + ". Request body: " + updateBody
							+ ". Response code: " + response.getStatusCode() + ". Response body: "
							+ response.getBody().asString());
			Assert.fail("Zephyr - Failed to update status for test case: " + tcId + ". Request body: " + updateBody);
		}
		if (response.getStatusCode() == 502) {
			logger.error("Zephyr - Bad Gateway error while updating status for test case: " + tcId
					+ ". This may indicate a temporary issue with the Zephyr server.");
			TestListeners.extentTest.get().warning("Zephyr - Bad Gateway error while updating status for test case: "
					+ tcId + ". This may indicate a temporary issue with the Zephyr server.");
		} else {
			logger.info("Zephyr - Successfully updated status for test case: " + tcId);
			TestListeners.extentTest.get().info("Zephyr - Successfully updated status for test case: " + tcId);
		}
	}

	public String getLatestExecutionAssignedToId(String testCycle, String testCaseId) {
		try {
			RestAssured.baseURI = getConfigProperty("zephyr.baseUrl");
			Response response = RestAssured.given()
					.header("Authorization", "Bearer " + decrypt(getConfigProperty("zephyr.token")))
					.header("Content-Type", "application/json").queryParam("testCycle", testCycle)
					.queryParam("testCase", testCaseId).get("/testexecutions");

			if (response.getStatusCode() != 200) {
				logger.warn("Failed to fetch test executions. Response code: " + response.getStatusCode()
						+ ". Response body: " + response.getBody().asString());
				return null;
			}

			// Parse JSON response
			JsonNode rootNode = objectMapper.readTree(response.getBody().asString());
			JsonNode valuesNode = rootNode.get("values");

			if (valuesNode == null || !valuesNode.isArray() || valuesNode.size() == 0) {
				return null;
			}

			// Find the execution with the latest actualEndDate
			JsonNode latestExecution = null;
			Instant latestDate = null;

			for (JsonNode execution : valuesNode) {
				JsonNode endDateNode = execution.get("actualEndDate");
				if (endDateNode != null && !endDateNode.isNull()) {
					Instant endDate = Instant.parse(endDateNode.asText());

					if (latestDate == null || endDate.isAfter(latestDate)) {
						latestDate = endDate;
						latestExecution = execution;
					}
				}
			}

			// Extract assignedToId from the latest execution
			if (latestExecution != null) {
				JsonNode assignedToIdNode = latestExecution.get("assignedToId");
				if (assignedToIdNode != null && !assignedToIdNode.isNull()) {
					return assignedToIdNode.asText();
				}
			}

			return null;

		} catch (Exception e) {
			logger.error("Error fetching latest execution assignedToId: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public String decrypt(String encryptedData) {
		try {
			Key key = generateKey();
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] decordedValue = Base64.getDecoder().decode(encryptedData);
			byte[] decValue = c.doFinal(decordedValue);
			return new String(decValue);
		} catch (Exception e) {
			logger.error("Decryption failed", e);
			throw new RuntimeException("Decryption failed", e);
		}
	}

	private static Key generateKey() throws Exception {
		Key key = new SecretKeySpec(keyValue, ALGO);
		return key;
	}

	public String encrypt(String Data) throws Exception {
		Key key = generateKey();
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] encVal = c.doFinal(Data.getBytes());
		String encryptedValue = Base64.getEncoder().encodeToString(encVal);
		return encryptedValue;
	}

	public static void main(String[] args) throws Exception {
		Utilities myClass = new Utilities();
		String result = myClass.encrypt("Punchh@885");
		System.out.println("Result:" + result);
	}

	public String getSystemPublicIPv4() throws Exception {
		try {
			URL url = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String publicIPAddress = in.readLine();
			in.close();
			return publicIPAddress;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

//	// Gmail properties loader
//	public Properties getGmailProperties() {
//		logit("Loading configuration for Gmail.");
//		Properties properties = new Properties();
//		properties.put("mail.store.protocol", getConfigProperty("gmail.store.protocol"));
//		properties.put("mail.imaps.host", getConfigProperty("gmail.imaps.host"));
//		properties.put("mail.imaps.port", getConfigProperty("gmail.imaps.port"));
//		properties.put("mail.imaps.starttls.enable", getConfigProperty("gmail.imaps.starttls.enable"));
//		return properties;
//	}

//	// Gmail server connection
//	public Store connectToGmailServer() throws Exception {
//		logit("Connecting to the Gmail server.");
//		Properties properties = getGmailProperties();
//		Session emailSession = Session.getInstance(properties);
//		Store store = emailSession.getStore(properties.getProperty("mail.store.protocol"));
//		store.connect(getConfigProperty("gmail.username"), decrypt(getConfigProperty("gmail.password")));
//		logit("Successfully connected to the Gmail server.");
//		return store;
//	}

	/**
	 * Unified Gmail email retrieval method that handles all search types.
	 * 
	 * @param searchBy    The attribute to search by ("subject", "toEmail",
	 *                    "fromEmail", "toAndFromEmail"). - "subject": search by
	 *                    subject only - "toEmail": search by recipient only -
	 *                    "fromEmail": search by sender only - "toAndFromEmail":
	 *                    search by both recipient and sender (see searchValue
	 *                    format below)
	 * @param searchValue The value to filter the emails by. - For "subject",
	 *                    "toEmail", or "fromEmail": a single value (subject text or
	 *                    email). - For "toAndFromEmail": a comma-separated string
	 *                    "toEmail,fromEmail"
	 * @param withPolling Whether to poll for emails (if false, only one attempt is
	 *                    made).
	 * @throws Exception If an error occurs during email retrieval.
	 **/
//	public String getGmailEmailBody(String searchBy, String searchValue, boolean withPolling) throws Exception {
//		Store store = connectToGmailServer();
//		Folder emailFolder = openInboxFolder(store);
//
//		String body = "";
//		boolean emailFound = false;
//		int maxAttempts = withPolling ? 24 : 1;
//		int attempts = 0;
//
//		// Polling loop: retry searching for emails if not found
//		while (attempts < maxAttempts && !emailFound) {
//			// Refresh folder state (forces the folder to sync with the server)
//			emailFolder.getMessageCount();
//
//			Message[] messages = searchEmails(emailFolder, searchValue, searchBy);
//
//			if (messages.length > 0) {
//				logit("Matching emails found: " + messages.length);
//				// Extract the text content from the first matching email body
//				body = extractTextFromEmailMessage(messages[0]);
//				logit("First Matching Email Body: " + body);
//				// Exit loop after finding an email
//				emailFound = true;
//			} else {
//				logit("No matching emails found, retrying... (attempt " + (attempts + 1) + " of " + maxAttempts + ")");
//				attempts++;
//				if (attempts < maxAttempts) {
//					// Wait for 5 seconds before retrying
//					longWaitInSeconds(5);
//				}
//			}
//		}
//
//		if (!emailFound) {
//			logit("Error", "No matching emails found after " + maxAttempts + " attempt(s)");
//		}
//		closeEmailSession(emailFolder, store);
//		return body;
//	}

//	// Overloaded method without polling parameter (default: polling true)
//	public String getGmailEmailBody(String searchBy, String searchValue) throws Exception {
//		return getGmailEmailBody(searchBy, searchValue, true);
//	}

//	public Folder openInboxFolder(Store store) throws Exception {
//		logit("Opening the INBOX folder...");
//		Folder emailFolder = store.getFolder("INBOX");
//		emailFolder.open(Folder.READ_WRITE);
//		logit("Successfully connected to the INBOX folder.");
//		return emailFolder;
//	}

	/**
	 * Searches for unread emails in the specified email folder based on the given
	 * criteria.
	 * 
	 * @param emailFolder The email folder to search for emails (e.g., INBOX).
	 * @param searchValue The value to filter emails by. If searching by subject,
	 *                    this is the subject keyword. If searching by recipient,
	 *                    this is the recipient email address. For "toAndFromEmail",
	 *                    this should be "toEmail,fromEmail". Must not be null or
	 *                    empty.
	 * @param searchBy    The criteria to search by. Acceptable values are: -
	 *                    "subject": Searches for emails containing the specified
	 *                    subject keyword - "toEmail": Searches for emails sent to
	 *                    the specified recipient email address. - "fromEmail":
	 *                    Searches for emails from the specified sender email
	 *                    address - "toAndFromEmail": Searches for emails with both
	 *                    specific recipient and sender
	 * @return An array of {@link Message} objects matching the search criteria. If
	 *         no emails match, returns an empty array.
	 **/
//	public Message[] searchEmails(Folder emailFolder, String searchValue, String searchBy) throws Exception {
//		if (searchValue == null || searchValue.isEmpty()) {
//			throw new IllegalArgumentException("searchValue must not be null or empty.");
//		}
//
//		logit("Searching unread emails with search by: " + searchBy + " with value: " + searchValue);
//
//		// Define a search term for unread emails
//		SearchTerm statusTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
//		SearchTerm searchCondition;
//
//		if ("toAndFromEmail".equalsIgnoreCase(searchBy)) {
//			// Handle toAndFromEmail search
//			if (!searchValue.contains(",")) {
//				throw new IllegalArgumentException(
//						"searchValue must be in format 'toEmail,fromEmail' for toAndFromEmail search.");
//			}
//			String[] parts = searchValue.split(",", 2);
//			String toEmail = parts[0].trim();
//			String fromEmail = parts[1].trim();
//
//			SearchTerm toTerm = new RecipientStringTerm(Message.RecipientType.TO, toEmail);
//			SearchTerm fromTerm = new FromStringTerm(fromEmail);
//			searchCondition = new AndTerm(new SearchTerm[] { statusTerm, toTerm, fromTerm });
//		} else {
//			// Handle single criteria search
//			SearchTerm valueTerm = null;
//			if ("subject".equalsIgnoreCase(searchBy)) {
//				valueTerm = new SubjectTerm(searchValue);
//			} else if ("toEmail".equalsIgnoreCase(searchBy)) {
//				valueTerm = new RecipientStringTerm(Message.RecipientType.TO, searchValue);
//			} else if ("fromEmail".equalsIgnoreCase(searchBy)) {
//				valueTerm = new FromStringTerm(searchValue);
//			} else {
//				logit("Warn", "Invalid searchBy parameter: " + searchBy);
//				throw new IllegalArgumentException(
//						"Invalid searchBy parameter. Use 'subject', 'toEmail', 'fromEmail', or 'toAndFromEmail'.");
//			}
//			searchCondition = new AndTerm(statusTerm, valueTerm);
//		}
//
//		// Perform the search
//		Message[] messages = emailFolder.search(searchCondition);
//		logit("Found " + messages.length + " unread emails matching the criteria.");
//		return messages;
//	}

//	public void closeEmailSession(Folder emailFolder, Store store) throws Exception {
//		logit("Closing email session...");
//		emailFolder.close(false);
//		store.close();
//		logit("Email session closed successfully.");
//	}

	public String getTimestampInNanoseconds() {
		return String.valueOf(System.nanoTime());
	}

//	public String extractTextFromEmailMessage(Message message) throws Exception {
//		StringBuilder result = new StringBuilder();
//		if (message.isMimeType("text/plain")) {
//			result.append(message.getContent().toString());
//		} else if (message.isMimeType("text/html")) {
//			String html = message.getContent().toString();
//			result.append(Jsoup.parse(html).text());
//		} else if (message.isMimeType("multipart/*")) {
//			Multipart multipart = (Multipart) message.getContent();
//			for (int i = 0; i < multipart.getCount(); i++) {
//				BodyPart bodyPart = multipart.getBodyPart(i);
//				if (bodyPart.isMimeType("text/plain")) {
//					result.append(bodyPart.getContent().toString());
//				} else if (bodyPart.isMimeType("text/html")) {
//					String html = bodyPart.getContent().toString();
//					result.append(Jsoup.parse(html).text());
//				} else if (bodyPart.getContent() instanceof Multipart) {
//					Multipart nestedMultipart = (Multipart) bodyPart.getContent();
//					for (int j = 0; j < nestedMultipart.getCount(); j++) {
//						BodyPart nestedPart = nestedMultipart.getBodyPart(j);
//						if (nestedPart.isMimeType("text/plain")) {
//							result.append(nestedPart.getContent().toString());
//						} else if (nestedPart.isMimeType("text/html")) {
//							String nestedHtml = nestedPart.getContent().toString();
//							result.append(Jsoup.parse(nestedHtml).text());
//						} else {
//							logit("Warn", "Skipping unsupported nested MIME type: " + nestedPart.getContentType());
//						}
//					}
//				} else {
//					logit("Warn", "Skipping unsupported MIME type: " + bodyPart.getContentType());
//				}
//			}
//		} else {
//			logit("Warn", "Unsupported message type: " + message.getContentType());
//		}
//		logit("Extracted text: " + result.toString());
//		return result.toString();
//	}

//	// Extracts attachments from an email message and saves them
//	public List<String> extractAttachmentFromEmailMessage(Message message) throws Exception {
//		List<String> attachments = new ArrayList<>();
//		String downloadedFilePath = Paths.get(System.getProperty("user.dir"), "resources", "ExportData").toString();
//
//		if (message.isMimeType("multipart/*")) {
//			Multipart multipart = (Multipart) message.getContent();
//			for (int i = 0; i < multipart.getCount(); i++) {
//				BodyPart bodyPart = multipart.getBodyPart(i);
//				String disposition = bodyPart.getDisposition();
//				if (disposition != null && (disposition.equalsIgnoreCase(BodyPart.ATTACHMENT)
//						|| disposition.equalsIgnoreCase(BodyPart.INLINE))) {
//					String fileName = bodyPart.getFileName();
//					if (fileName != null) {
//						attachments.add(fileName);
//						// Save the attachment, overwrite if exists
//						InputStream is = bodyPart.getInputStream();
//						Files.copy(is, Paths.get(downloadedFilePath, fileName), StandardCopyOption.REPLACE_EXISTING);
//						is.close();
//						logit("Saved attachment: " + fileName + " to " + downloadedFilePath);
//					}
//				}
//			}
//		}
//		return attachments;
//	}

	/*
	 * Retrieves both the email body and attachment filenames from the first
	 * matching email
	 */
//	public Map<String, Object> getGmailEmailBodyAndAttachments(String searchBy, String searchValue) throws Exception {
//		Store store = connectToGmailServer();
//		Folder emailFolder = openInboxFolder(store);
//
//		String body = "";
//		List<String> attachments = new ArrayList<>();
//		boolean emailFound = false;
//		int maxAttempts = 10;
//		int attempts = 0;
//		// Retry searching for email until found
//		while (attempts < maxAttempts && !emailFound) {
//			emailFolder.getMessageCount();
//			Message[] messages = searchEmails(emailFolder, searchValue, searchBy);
//
//			if (messages.length > 0) {
//				logit("Matching emails found: " + messages.length);
//				body = extractTextFromEmailMessage(messages[0]);
//				attachments = extractAttachmentFromEmailMessage(messages[0]);
//				emailFound = true;
//			} else {
//				logit("No matching emails found, retrying... (attempt " + (attempts + 1) + " of " + maxAttempts + ")");
//				attempts++;
//				if (attempts < maxAttempts) {
//					longWaitInSeconds(5);
//				}
//			}
//		}
//
//		if (!emailFound) {
//			logit("Error", "No matching emails found after " + maxAttempts + " attempt(s)");
//		}
//		closeEmailSession(emailFolder, store);
//		// store the body and attachments in a map and return
//		Map<String, Object> result = new HashMap<>();
//		result.put("body", body);
//		result.put("attachments", attachments);
//		return result;
//	}

	/**
	 * Logs a message with the specified status and updates the Extent Test Listener
	 * based on the provided status.
	 *
	 * @param message The message to log and report.
	 * @param status  The status of the test (e.g., "PASS", "FAIL", "ERROR", "WARN",
	 *                "INFO", "WARNING" etc.).
	 */
	public void logit(String status, String message) {
		var extentTest = TestListeners.extentTest.get();
		switch (status.toUpperCase()) {
		case "FAIL":
		case "ERROR":
			logger.error(message);
			if (extentTest != null)
				extentTest.fail(message);
			break;
		case "WARN":
		case "WARNING":
			logger.warn(message);
			if (extentTest != null)
				extentTest.warning(message);
			break;
		case "PASS":
			logger.info(message);
			if (extentTest != null)
				extentTest.pass(message);
			break;
		case "SKIP":
			logger.info(message);
			if (extentTest != null)
				extentTest.skip(message);
			break;
		case "INFO":
		default:
			logger.info(message);
			if (extentTest != null)
				extentTest.info(message);
			break;
		}
	}

	public void logit(String message) {
		logit("INFO", message);
	}

	/** Log warning message */
	public void logWarn(String message) {
		logit("WARN", message);
	}

	/** Log skip message */
	public void logSkip(String message) {
		logit("SKIP", message);
	}

	/**
	 * Convenience method for logging pass messages. Equivalent to logit("Pass",
	 * message)
	 * 
	 * @param message The message to log as pass
	 */
	public void logPass(String message) {
		logit("Pass", message);
	}

	/**
	 * Convenience method for logging fail messages. Equivalent to logit("Fail",
	 * message)
	 * 
	 * @param message The message to log as fail
	 */
	public void logFail(String message) {
		logit("Fail", message);
	}

	/**
	 * Convenience method for logging info messages. Equivalent to logit("Info",
	 * message)
	 * 
	 * @param message The message to log as info
	 */
	public void logInfo(String message) {
		logit("Info", message);
	}

	// For API Response Logging
	public void logit(Response response) {
		logger.info(response.then().log().everything());
		var extentTest = TestListeners.extentTest.get();
		extentTest.info("API Status Code: " + response.getStatusCode());
		extentTest.info("API Response Time: " + response.getTime() + " ms");
		extentTest.info("API Headers:\n" + "<pre style='max-height: 250px; overflow-y: auto;'>" + response.getHeaders()
				+ "</pre>");
		String body = response.getBody().asPrettyString();
		if (body == null || body.isBlank()) {
			extentTest.info("API Body:\n" + "<pre style='max-height: 250px; overflow-y: auto;'>No body present</pre>");
		} else {
			extentTest.info("API Body:\n" + "<pre style='max-height: 250px; overflow-y: auto;'>" + body + "</pre>");
		}
	}

	public static String[] resolveParameterNames(Method method, int valueCount) {
		String[] names = new String[valueCount];
		Parameter[] parameters = method != null ? method.getParameters() : new Parameter[0];
		for (int i = 0; i < valueCount; i++) {
			String name = null;
			if (i < parameters.length && parameters[i] != null) {
				name = parameters[i].getName();
			}
			if (name == null || name.isBlank() || name.matches("arg\\d+")) {
				name = "param" + (i + 1);
			}
			names[i] = name;
		}
		return names;
	}

	public static String trimValue(String value, int maxLength) {
		if (value == null) {
			return "null";
		}
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength - 3) + "...";
	}

	public void switchToWindow() {
		Set<String> window_handles = driver.getWindowHandles();
		ArrayList<String> tabs = new ArrayList<String>(window_handles);
		int count = tabs.size();
		logger.info("Total open windows are: " + count);
		driver.switchTo().window(tabs.get(count - 1));
	}

	public void switchToParentWindow() {
		Set<String> window_handles = driver.getWindowHandles();
		ArrayList<String> tabs = new ArrayList<String>(window_handles);
		int count = tabs.size();
		logger.info("Total open windows are: " + count);
		driver.switchTo().window(tabs.get(count - count));
	}

	public void closeSeconedTabandSwitchToParentWindow() {
		Set<String> window_handles = driver.getWindowHandles();
		ArrayList<String> tabs = new ArrayList<String>(window_handles);
		int count = tabs.size();
		logger.info("Total open windows are: " + count);
		driver.switchTo().window(tabs.get(count - 1)).close();
		driver.switchTo().window(tabs.get(count - count));
	}

	public void mouseHover(WebDriver driver, WebElement element) {
		Actions actions = new Actions(driver);
		actions.moveToElement(element).build().perform();
	}

	public void geOtp() {
		String secretKey = "13pkjiovi6aaodpg6iy5er7uart7mk3m";
		String code = getTOTPCode(secretKey);
		System.out.println(code);
	}

	public void goBack() {
		driver.navigate().back();
	}

	public static String getTOTPCode(String secretKey) {
		Base32 base32 = new Base32();
		byte[] bytes = base32.decode(secretKey.toUpperCase());
		String hexKey = Hex.encodeHexString(bytes);
		return TOTP.getOTP(hexKey);
	}

	public void waitTillCompletePageLoad() {
		new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver -> ((JavascriptExecutor) webDriver)
				.executeScript("return document.readyState").equals("complete"));
	}

	public boolean isClicked(WebElement element) {
		try {
			// WebDriverWait wait = new WebDriverWait(driver, 5);
			// wait.until(ExpectedConditions.elementToBeClickable(element));
			element.click();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public WebElement getXpathWebElements(By by) {
		return driver.findElement(by);
	}

	public void refreshPage() {
		driver.navigate().refresh();
		longWaitInSeconds(3);
		logger.info("Refreshing Page...");
	}

	public void refreshPageWithCurrentUrl() {
		driver.get(driver.getCurrentUrl());
		driver.navigate().to(driver.getCurrentUrl());
		longWaitInSeconds(2);
		logger.info("Refreshing Page...");
	}

	public void waitTillPagePaceDone_old() {
		try {
			// wait till pace-done appeared
			String xpath = "//body[contains(@class,'pace-done')]";
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
			logger.info("Inside Page pace wait progress ...");
			wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
			logger.info("Page pace wait completed ...");
		} catch (Exception e) {
			logger.warn(e.getMessage());
			TestListeners.extentTest.get().warning(e.getMessage());
		}
	}
	/*
	 * public void waitTillPagePaceDone() { try { //WebElement ele =
	 * driver.findElement(By.xpath("//body")); //WebDriverWait wait = new
	 * WebDriverWait(driver, Duration.ofSeconds(120));
	 * logger.info("Inside Page pace wait progress ...");
	 * //wait.until(ExpectedConditions.att(ele, "class", "pace-done"));
	 * //wait.until(ExpectedConditions.attributeContains(By.
	 * xpath("//div[@class='pace  pace-active']"), "class", "pace  pace-inactive"));
	 * new WebDriverWait(driver,
	 * Duration.ofSeconds(80)).until(ExpectedConditions.attributeContains(By.
	 * xpath("//div[@class='pace  pace-active']"), "class", "pace  pace-inactive"));
	 * logger.info("Page pace wait completed ..."); } catch (Exception e) {
	 * logger.warn(e.getMessage());
	 * TestListeners.extentTest.get().warning(e.getMessage()); } }
	 */

	/*
	 * public void waitTillPagePaceDonee(String pageName) { StopWatch pageLoad = new
	 * StopWatch(); pageLoad.start(); WebElement ele =
	 * driver.findElement(By.xpath("//body")); WebDriverWait wait = new
	 * WebDriverWait(driver, Duration.ofSeconds(60));
	 * wait.until(ExpectedConditions.attributeContains(ele, "class", "pace-done"));
	 * pageLoad.stop(); long pageLoadTime_ms = pageLoad.getTime(); long
	 * pageLoadTime_Seconds = pageLoadTime_ms / 1000;
	 * logger.info("Total Page Load Time for Page " + pageName + ": " +
	 * pageLoadTime_ms + " milliseconds");
	 * logger.info("Total Page Load Time for Page " + pageName + ": " +
	 * pageLoadTime_Seconds + " seconds"); TestListeners.extentTest.get()
	 * .info("Total Page Load Time for Page " + pageName + " : " + pageLoadTime_ms +
	 * " milliseconds"); TestListeners.extentTest.get()
	 * .info("Total Page Load Time for Page " + pageName + ": " +
	 * pageLoadTime_Seconds + " seconds"); }
	 */

	public String twoDigitDecimalNumber() {
		Random rd = new Random();
		float num = rd.nextFloat() * 1000;
		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		String num1 = df.format(num);
		return num1;
	}

	public String twoDigitDecimalNumberUnderFiveHundred() {
		Random rd = new Random();
		double num = rd.nextDouble() * 500; // Generates a number between 0 (inclusive) and 500 (exclusive)
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(num);
	}

	public void getPageLoadTime(WebElement ele, String pageName) {
		StopWatch pageLoad = new StopWatch();
		pageLoad.start();
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		wait.until(ExpectedConditions.elementToBeClickable(ele));
		pageLoad.stop();
		// Get the time
		long pageLoadTime_ms = pageLoad.getTime();
		long pageLoadTime_Seconds = pageLoadTime_ms / 1000;
		logger.info("Total Page Load Time for Page " + pageName + ": " + pageLoadTime_ms + " milliseconds");
		logger.info("Total Page Load Time for Page " + pageName + ": " + pageLoadTime_Seconds + " seconds");
		TestListeners.extentTest.get()
				.info("Total Page Load Time for Page " + pageName + " : " + pageLoadTime_ms + " milliseconds");
		TestListeners.extentTest.get()
				.info("Total Page Load Time for Page " + pageName + ": " + pageLoadTime_Seconds + " seconds");
	}

	public void waitTillElementToBeClickable(WebElement WebElement) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
			wait.until(ExpectedConditions.elementToBeClickable(WebElement));
		} catch (Exception e) {
			logger.error("WebElement failed to load/appear: " + e);
			TestListeners.extentTest.get().info("WebElement failed to load/appear: " + e);
		}
	}

	public void waitTillVisibilityOfElementLocated(By locator, String elementName, long seconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
			wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		} catch (Exception e) {
			TestListeners.extentTest.get().info(elementName + " is not present due to exception: " + e.getMessage());
			logger.error(elementName + " is not present due to exception: " + e.getMessage());
		}
	}

	public void waitTillVisibilityOfElement(WebElement element, String elementName) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
			wait.until(ExpectedConditions.visibilityOf(element));
		} catch (Exception e) {
			TestListeners.extentTest.get().info(elementName + " not present and webelement is " + element + e);
			logger.error(elementName + " is not present and webelement is " + element);
		}
	}

	public void waitTillElementDisappear(String locator, long seconds) {
		String xpath = getLocatorValue(locator);
		try {
			implicitWait(3);
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xpath)));
			logit("Element disappeared: " + xpath);
		} catch (Exception e) {
			logit("Exception occurred: " + e.getMessage());
		} finally {
			implicitWait(50);
		}
	}

	public void waitTillElementDisappear(String xpath) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xpath)));
			logger.error("Element disappeared :" + xpath);
			TestListeners.extentTest.get().info("Element disappeared :" + xpath);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			TestListeners.extentTest.get().warning(e.getMessage());
		}
	}

	public void waitTillElementDisappear(WebElement ele) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(180));
			wait.until(ExpectedConditions.invisibilityOf(ele));
			logger.error("Element disappeared :" + ele);
			TestListeners.extentTest.get().info("Element disappeared :" + ele);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			TestListeners.extentTest.get().warning(e.getMessage());
		}
	}

	public void waitTillSpinnerDisappear() {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(180));
			wait.until(ExpectedConditions
					.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'spinnerBorder')]")));
			logger.error("Spinner disappeared");
			TestListeners.extentTest.get().info("spinner disappeared");
		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
		}
	}

	public void waitTillSpinnerDisappear(long time) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(time));
			wait.until(ExpectedConditions
					.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'spinnerBorder')]")));
			logger.info("Spinner disappeared");
			TestListeners.extentTest.get().info("spinner disappeared");
		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
		}
	}

	public void waitTillPPCCPageLoaderDisappear() {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
			wait.until(
					ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@class='punchh-loader-circle']")));
			logger.error("PPCC Page Loader/Spinner disappeared");
			TestListeners.extentTest.get().info("PPCC Page Loader/Spinner disappeared");
		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
		}
	}

	public void clickWithActions(WebElement ele) {
		Actions action = new Actions(driver);
		action.click(ele).build().perform();
		logger.info("Clicked  element " + ele);
		TestListeners.extentTest.get().info("Clicked element: " + ele);
	}

	public boolean isJwtToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			logger.error("Token is empty, expected JWT token");
			throw new IllegalArgumentException("Token is empty, expected JWT token");
		}
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			logger.error("The provided token is not a JWT token");
			return false;
		}
		try {
			Base64.getUrlDecoder().decode(parts[0]);
			Base64.getUrlDecoder().decode(parts[1]);
			Base64.getUrlDecoder().decode(parts[2]);
			logger.info("The provided token is a valid JWT token");
			return true;
		} catch (Exception ex) {
			logger.info("Error while verifying JWT token: " + ex.getMessage());
			return false;
		}
	}

	public void unzip(String zipFilePath, String destDir) {
		File dir = new File(destDir);
		// create output directory if it doesn't exist
		if (!dir.exists())
			dir.mkdirs();
		FileInputStream fis;
		// buffer for read and write data to file
		byte[] buffer = new byte[1024];
		try {
			fis = new FileInputStream(zipFilePath);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(destDir + File.separator + fileName);
				System.out.println("Unzipping to " + newFile.getAbsolutePath());
				// create directories for sub directories in zip
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				// close this ZipEntry
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			// close last ZipEntry
			zis.closeEntry();
			zis.close();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void sleep(long milliseconds) {
		try {
			logger.info("Sleeping for " + milliseconds + " milliseconds...");
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			logger.error("Sleep was interrupted: " + e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	public void switchToWindowN(int n) {
		((JavascriptExecutor) driver).executeScript("window.open()");
		ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(n));
	}

	public void switchToNewOpenedWindow() {
		ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
		driver.switchTo().window(tabs.get(tabs.size() - 1));
	}

	public void turnSliderOn(WebDriver driver, WebElement ele) {
		String color = ele.getCssValue("background-color");
		String hexcolor = Color.fromString(color).asHex();
		if (hexcolor != "#37c936") {
			waitTillElementToBeClickable(ele);
			StaleElementclick(driver, ele);
		}
	}

	public void turnSliderOff(WebDriver driver, WebElement ele) {
		String color = ele.getCssValue("background-color");
		String hexcolor = Color.fromString(color).asHex();
		if (hexcolor == "#37c936") {
			waitTillElementToBeClickable(ele);
			StaleElementclick(driver, ele);
		}
	}

	public List<Integer> getAllDrpValuse(WebElement ele) {
		Select select = new Select(ele);
		List<WebElement> options = select.getOptions();
		List<String> optionsVal = options.stream().map(WebElement::getText).distinct().sorted()
				.collect(Collectors.toList());
		List<Integer> optionsValues = optionsVal.stream().map(Integer::parseInt).collect(Collectors.toList());
		return optionsValues;
	}

	public String isOptionalField(WebElement ele) {
		String val = ele.getAttribute("class");
		return val;
	}

	public void navigateBackPage() {
		driver.navigate().back();
		logger.info("nagivate back Page");
	}

	public String readFileAsString(String file) throws Exception {
		return new String(Files.readAllBytes(Paths.get(file)));
	}

	public boolean textContains(String actualText, String expectedText) {
		boolean flag = false;
		logger.info("actual text : " + actualText);
		logger.info("expected text : " + expectedText);
		if (actualText.contains(expectedText)) {
			flag = true;
		}
		return flag;
	}

	public void clickUsingActionsClass(WebElement element) {
		Actions actions = new Actions(driver);
		actions.moveToElement(element).click().build().perform();
	}

	public void deselectDrpDwnValue(WebElement ele, String value) {
		Select option = new Select(ele);
		// option.selectByValue("");
		option.deselectByVisibleText(value);
		// option.selectByVisibleText(value);
	}

	public void getAPIResponseTime(Response response) {
		// gets the response time in milliseconds
		logger.info("response time in milliseconds : " + response.getTime());
		// gets the response time in the time unit passed as a parameter
		logger.info("response time in seconed : " + response.getTimeIn(TimeUnit.SECONDS));
		TestListeners.extentTest.get()
				.warning("Response time of api in seconed is  : " + response.getTimeIn(TimeUnit.SECONDS));

		if (response.getTimeIn(TimeUnit.SECONDS) > 5) {
			TestListeners.extentTest.get()
					.warning("Response time of api in seconed is  : " + response.getTimeIn(TimeUnit.SECONDS));
		}
	}

	public void checkUncheckFlag(String flagName, String checkBoxFlag) {

		WebElement enableSTOCheckbox = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logit(flagName + ": checkbox is already unchecked, therefore did nothing as user also want to "
					+ checkBoxFlag + " it");
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			clickByJSExecutor(driver, enableSTOCheckbox);
			logit(flagName + ": checkbox is unchecked, therefore clicked it as user want to " + checkBoxFlag + " it");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			clickByJSExecutor(driver, enableSTOCheckbox);
			logit(flagName + ": checkbox is checked, therefore clicked it as user want to " + checkBoxFlag + " it");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {
			logit(flagName + ": checkbox is already checked, therefore did nothing as user also want to " + checkBoxFlag
					+ " it");
		}
	}

	public void deselectAllDrpDownValue(WebElement ele) {
		Select option = new Select(ele);
		option.deselectAll();
	}

	public boolean comparingLists(List<String> expectedLst, List<String> actualLst) {
		boolean allValuesPresent = true;
		String notFound = " ";
		for (String value : expectedLst) {
			if (!actualLst.contains(value)) {
				allValuesPresent = false;
				notFound = notFound + "," + value;
			}
		}
		if (!allValuesPresent) {
			logger.info("this values are missing " + notFound);
			TestListeners.extentTest.get().info("this values are missing " + notFound);
		}
		return allValuesPresent;
	}

	public static String phonenumber() {
		long phone = (long) (Math.random() * Math.pow(10, 10));
		String phone1 = String.valueOf(phone);
		return phone1;
	}

	public long phoneNumber1() {
		long phone = (long) (Math.random() * Math.pow(10, 10));
		return phone;
	}

	public void selectDrpDwnValueNew(WebElement ele, String value) {
		Select option = new Select(ele);
		option.selectByValue(value);
	}

	public String getTimezone(String str) {
		String timezone = "";
		if (str.contains("PM")) {
			String[] parts = str.split("PM", 2);
			timezone = parts[1];
			return timezone;
		} else if (str.contains("AM")) {
			String[] parts = str.split("AM", 2);
			timezone = parts[1];
			return timezone;
		} else {
			return timezone;
		}
	}

	public String convertDateTimeZone(String date, String originalTimeZone) {
		String actualTimezone = getTimezone(date);
		System.out.println(actualTimezone);

		if (originalTimeZone.isEmpty()) {
			try {
				// Create a SimpleDateFormat object for parsing the current date
				DateFormat currentFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm aa z");
				currentFormat.setTimeZone(TimeZone.getTimeZone(actualTimezone));
				Date currentDateObj = currentFormat.parse(date);

				// Create a SimpleDateFormat object for formatting the UTC date
				DateFormat UTCFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z");
				UTCFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				String UtcDate = UTCFormat.format(currentDateObj);
				System.out.println(UtcDate);
				TestListeners.extentTest.get().info(UtcDate);
				return UtcDate;
			} catch (Exception e) {
				logger.info("Error converting date: " + e.getMessage());
				TestListeners.extentTest.get().fail("Error converting date: " + e.getMessage());
				return null;
			}
		}
		{
			try {
				// Create a SimpleDateFormat object for parsing the current date
				DateFormat currentFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm aa z");
				currentFormat.setTimeZone(TimeZone.getTimeZone(originalTimeZone));
				Date currentDateObj = currentFormat.parse(date);

				// Create a SimpleDateFormat object for formatting the UTC date
				DateFormat UTCFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z");
				UTCFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				String UtcDate = UTCFormat.format(currentDateObj);

				return UtcDate;
			} catch (Exception e) {
				logger.info("Error converting date: " + e.getMessage());
				TestListeners.extentTest.get().fail("Error converting date: " + e.getMessage());
				return null;
			}
		}
	}

	public String getSuccessMessage() {
		waitTillVisibilityOfElement(getLocator("locationPage.getSuccessErrorMessage"), "");
		String message = getLocator("locationPage.getSuccessErrorMessage").getText();
		TestListeners.extentTest.get().info("Message displayed: " + message);
		logger.info("Message displayed: " + message);
		return message;
	}

	public boolean valuePresentInList(List<WebElement> lst, String value) {
		for (WebElement wEle : lst) {
			String txt = wEle.getText();
			if (txt.equalsIgnoreCase(value)) {
				logger.info("Value is present in the list: " + txt);
				TestListeners.extentTest.get().info("Value is present in the list: " + txt);
				return true;
			}
		}
		return false;
	}

	public String getTodayDate() {
		LocalDate today = LocalDate.now();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);
		String formattedDate = today.format(formatter);
		return formattedDate;
	}

	public String getPopUpText() {
		driver.switchTo().alert();
		String text = driver.switchTo().alert().getText();
		logger.info("the text from the alert --" + text);
		TestListeners.extentTest.get().info("the text from the alert --" + text);
		return text;
	}

	public boolean valuePresentInStringList(List<String> lst, String value) {
		for (String ele : lst) {
			if (ele.equalsIgnoreCase(value)) {
				return true;
			}
		}
		return false;
	}

	public boolean verifyPartOfURL(String partOfURL) {
		boolean flag = false;
		waitTillPagePaceDone();
		String url = driver.getCurrentUrl();
		if (url.contains(partOfURL)) {
			logger.info(partOfURL + " part is verified from the current URL which is " + url);
			TestListeners.extentTest.get().info(partOfURL + " part is verified from the current URL which is " + url);
			flag = true;
		}
		return flag;
	}

	public void createNewWindowAndSwitch(String parentWindow) {
		((JavascriptExecutor) driver).executeScript("window.open('https://www.google.com','_blank');");
		Set<String> allWindowHandles = driver.getWindowHandles();
		for (String handle : allWindowHandles) {
			if (!handle.equals(parentWindow)) {
				driver.switchTo().window(handle);
				break;
			}
		}
	}

	public void switchToWindowByIndex(WebDriver driver, int index) {
		Set<String> windowHandles = driver.getWindowHandles();
		if (index < windowHandles.size()) {
			String[] handles = windowHandles.toArray(new String[0]);
			driver.switchTo().window(handles[index]);
			longWait(1000);
		} else {
			throw new IllegalArgumentException("Invalid window index: " + index);
		}
	}

	public String getRewardIdFromJsonArray(Response response, String arrayName, String Key, String actualKeyValue,
			String KeyToBeFetch) {
		List<Object> obj = new ArrayList<Object>();
		int j = 0;
		String expectedValue, expectedValueFlag;
		obj = response.jsonPath().getList(arrayName);
		for (int i = 0; i < obj.size(); i++) {
			expectedValue = response.jsonPath().getString(arrayName + "[" + i + "]." + Key);
			if (expectedValue.contains(actualKeyValue)) {
				j = i;
				break;
			}
		}
		expectedValueFlag = response.jsonPath().getString(arrayName + "[" + j + "]." + KeyToBeFetch);
		return expectedValueFlag;
	}

	public void waitTillInVisibilityOfElement(WebElement element, String elementName) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			wait.until(ExpectedConditions.invisibilityOf(element));
		} catch (Exception e) {
			TestListeners.extentTest.get().info(elementName + " is present and webelement is " + element + e);
			logger.info(elementName + " is present and webelement is " + element);
		}
	}

	public List<String> getAllVisibleTextFromDropdwon(WebElement wEle) {
		Select select = new Select(wEle);
		List<WebElement> options = select.getOptions();
		List<String> optionsVal = options.stream().filter(option -> !option.getAttribute("value").trim().isEmpty())
				.map(WebElement::getText).distinct().sorted().collect(Collectors.toList());
		List<String> optionsValues = optionsVal.stream().collect(Collectors.toList());
		TestListeners.extentTest.get().info("Dropdown options are: " + optionsValues);
		logger.info("Dropdown options are: " + optionsValues);
		return optionsValues;
	}

	public int returnAPIResponseArrayIndex(Response apiResponse, String apiResponseVar, String expectedText) {
		// boolean flag = false;
		int counter = 0;
		// String jsonObjectString = apiResponse.asString();
		// JSONArray finalResponse = new JSONArray(jsonObjectString);
		for (int i = 0; i < apiResponse.jsonPath().getString(apiResponseVar).length(); i++) {
			String id = apiResponse.jsonPath().getString("[" + i + "]." + apiResponseVar);
			if (textContains(id, expectedText)) {
				counter = i;
				break;
			}
		}
		// String jsonObjectString = apiResponse.asString();
		// JSONArray finalResponse = new JSONArray(jsonObjectString);
		for (int i = 0; i < apiResponse.jsonPath().getString(apiResponseVar).length(); i++) {
			String id = apiResponse.jsonPath().getString("[" + i + "]." + apiResponseVar);
			if (textContains(id, expectedText)) {
				counter = i;
				break;
			}
		}
		return counter;
	}

	public void waitTillPaceDataProgressComplete() {
		try {
			WebElement ele = driver.findElement(By.xpath("//body"));
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
			wait.until(ExpectedConditions.attributeContains(ele, "class", "pace-done pace-done"));
		} catch (Exception e) {

		}
	}

	public static ZonedDateTime parseDateTime(String dateTimeString) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a z")
				.withZone(ZoneId.of("Asia/Kolkata"));
		return ZonedDateTime.parse(dateTimeString, formatter);
	}

	public static String compareDateTimes(ZonedDateTime dateTime1, ZonedDateTime dateTime2) {
		String result = "";
		if (dateTime1.isBefore(dateTime2)) {
			result = "Before";
		} else if (dateTime1.isAfter(dateTime2)) {
			result = "After";
		} else {
			result = "Equals";
		}
		return result;
	}

	public static String getCurrentTimeWithZone() {
		// Get the current date and time in IST (Indian Standard Time)
		ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

		// Define the date-time formatter for the rest of the pattern
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a z");

		// Format the date-time
		String formattedDateTime = currentDateTime.format(formatter);
		return formattedDateTime.toString();

	}

	public static String getCurrentTimeWithZoneWithFullMonth() {
		// Get the current date and time in IST (Indian Standard Time)
		ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

		// Define the date-time formatter for the rest of the pattern
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a z");

		// Format the date-time
		String formattedDateTime = currentDateTime.format(formatter);
		return formattedDateTime.toString();

	}

	// to upload files on remote
	public void uplodFile(WebElement addFile, String path) {
		((RemoteWebElement) addFile).setFileDetector(new LocalFileDetector());
		addFile.sendKeys(path);
	}

	public void sendKeysUsingActionClass(WebElement element, String text) {
		Actions actions = new Actions(driver);
		actions.moveToElement(element);
		actions.click();
		actions.sendKeys(text).build().perform();
		logger.info("entered the text -- " + text);
		TestListeners.extentTest.get().info("entered the text -- " + text);
	}

	public String getJsonReponseKeyValueFromJsonArray(Response response, String arrayName, String key, String val) {
		// convert JSON string into JSON Object using JSONObject() method
		String jsonData = response.asString();
		JSONObject json = new JSONObject(jsonData);
		// get locations array from the JSON Object and store it into JSONArray
		JSONArray jsonArray = json.getJSONArray(arrayName);
		String value = "";
		int len = jsonArray.length();
		for (int i = 0; i < len; i++) {
			// store each object in JSONObject
			JSONObject explrObject = jsonArray.getJSONObject(i);
			value = explrObject.get(key).toString();
			if (value.equalsIgnoreCase(val)) {
				logger.info("json key value matched");
				break;
			}
		}
		return value;
	}

	/*
	 * Example JSON Response: { "arrayName": [ { "key": "val", "finalkey":
	 * "returned value" } ] }
	 */
	public static String getJsonReponseKeyValueFromJsonArrayForUnknownKeyValuePair(Response response, String arrayName,
			String key, String val, String finalkey) {
		// convert JSON string into JSON Object using JSONObject() method
		String jsonData = response.asString();
		JSONObject json = new JSONObject(jsonData);
		// get locations array from the JSON Object and store it into JSONArray
		JSONArray jsonArray = json.getJSONArray(arrayName);
		String value = "";
		String value1 = "";
		int len = jsonArray.length();
		for (int i = 0; i < len; i++) {
			// store each object in JSONObject
			JSONObject explrObject = jsonArray.getJSONObject(i);
			value = explrObject.get(key).toString();

			if (value.equalsIgnoreCase(val)) {
				logger.info("json key value matched");
				value1 = explrObject.get(finalkey).toString();
				break;
			}
		}
		return value1;
	}

	public List<String> getAllValuesFromJsonArrayByKey(Response response, String arrayName, String key) {
		List<String> values = new ArrayList<>();
		String jsonData = response.asString();
		JSONObject json = new JSONObject(jsonData);
		JSONArray jsonArray = json.getJSONArray(arrayName);
		String value = "";
		int len = jsonArray.length();
		for (int i = 0; i < len; i++) {
			JSONObject explrObject = jsonArray.getJSONObject(i);
			value = explrObject.get(key).toString();
			values.add(value);
			logger.info("Found " + key + " at index " + i + ": " + value);
		}
		logger.info("Total " + key + " values found: " + values.size());
		TestListeners.extentTest.get().info("Total " + key + " values found: " + values.size() + " - " + values);
		return values;
	}

	/*
	 * Extracts a value from a nested JSON array in the response based on a matching
	 * key-value pair.
	 */
	public String extractValueFromJsonArray(Response response, String arrayName, String matchKey, String matchValue,
			String targetKey, String nestedKey) {

		String jsonData = response.asString();
		JSONArray rootArray = new JSONArray(jsonData);
		JSONObject rootObject = rootArray.getJSONObject(0);
		JSONArray targetArray = rootObject.getJSONArray(arrayName);
		String resultValue = "";
		for (int i = 0; i < targetArray.length(); i++) {
			JSONObject currentObject = targetArray.getJSONObject(i);
			String currentMatchVal = currentObject.get(matchKey).toString();

			if (currentMatchVal.equalsIgnoreCase(matchValue)) {
				if (nestedKey.isEmpty()) {
					resultValue = currentObject.get(targetKey).toString();
				} else {
					resultValue = currentObject.getJSONObject(targetKey).get(nestedKey).toString();
				}
				logger.info("JSON value found as '" + resultValue + "' for " + targetKey + "." + nestedKey);
				TestListeners.extentTest.get()
						.info("JSON value found as '" + resultValue + "' for " + targetKey + "." + nestedKey);
				break;
			}
		}
		return resultValue;
	}

	// This method is to iterate on json response if json array has no name
	// present in response
	public String getJsonReponseKeyValueFromJsonArrayWithoutArrayName(Response response, String key, String val) {

		String jsonData = response.asString();
		// adding array name to json response
		JSONArray jsonArray = new JSONArray(jsonData);
		int count = jsonArray.length();
		String value = "";
		for (int i = 0; i < count; i++) {
			JSONObject jObject = jsonArray.getJSONObject(i);
			value = String.valueOf(jObject.get(key));
			if (value.equalsIgnoreCase(val) || value.contains(val)) {
				logger.info("json key value matched");
				TestListeners.extentTest.get().info("json key value matched :" + value);
				break;
			}
		}
		return value;
	}

	public String getJsonReponseKeyValueFromJsonArrayWithoutArrayNameContainsText(Response response, String key,
			String val) {

		String jsonData = response.asString();
		// adding array name to json response
		JSONArray jsonArray = new JSONArray(jsonData);
		int count = jsonArray.length();
		String value = "";
		for (int i = 0; i < count; i++) {
			JSONObject jObject = jsonArray.getJSONObject(i);
			value = String.valueOf(jObject.get(key));
			logger.info("counter " + i + " json key value fetched is ==> " + value);
			if (value.equalsIgnoreCase(val) || value.contains(val)) {
				logger.info("json key value matched");
				TestListeners.extentTest.get().info("json key value matched :" + value);
				break;
			}
		}
		return value;
	}

	public void typeTextCharacterByCharacter(WebElement element, String text) throws InterruptedException {
		for (char ch : text.toCharArray()) {
			element.sendKeys(Character.toString(ch));
			Thread.sleep(100);
		}
	}

	public static void clearFolder(String folderPath, String fileExtention) {
		File folder = new File(folderPath);
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			if (files != null) { // Check for null in case of an I/O error
				for (File file : files) {
					String filePath = file.toString();
					if (file.isFile() && (filePath.endsWith(fileExtention))) {
						file.delete();
					} else if (file.isDirectory()) {
						clearFolder(file.toString(), fileExtention); // Recursively clear subdirectories
						file.delete(); // Then delete the empty subdirectory
					}
				}
			}
		}
	}

	// Set the state of a checkbox based on the text of the checkbox label
	public boolean setCheckboxStateViaCheckBoxText(String checkboxText, boolean shouldCheck) {

		boolean isStateChanged = false;

		WebElement checkbox = driver
				.findElement(By.xpath("//label[text()=\"" + checkboxText + "\"]/preceding-sibling::input[1]"));
		boolean isChecked = checkbox.isSelected();
		if (isChecked != shouldCheck) {
			logger.info("Checkbox '" + checkboxText + "' state needs to be changed. Current state: "
					+ (isChecked ? "checked" : "unchecked") + ". Target state: "
					+ (shouldCheck ? "checked" : "unchecked"));
			if (shouldCheck) {
				logger.info("Checking the checkbox.");
				clickByJSExecutor(driver, checkbox);
			} else {
				logger.info("Unchecking the checkbox.");
				clickByJSExecutor(driver, checkbox);
			}

			// Verify the final state of the checkbox
			boolean newState = checkbox.isSelected();
			if (newState == shouldCheck) {
				logger.info("Checkbox '" + checkboxText + "' state successfully changed to: "
						+ (newState ? "checked" : "unchecked"));
				isStateChanged = true;
				TestListeners.extentTest.get().pass("Checkbox '" + checkboxText + "' state successfully changed to: "
						+ (newState ? "checked" : "unchecked"));
			} else {
				logger.error("Failed to change the checkbox '" + checkboxText + "' state.");
				TestListeners.extentTest.get().fail("Failed to change the checkbox '" + checkboxText + "' state.");
			}
		} else {
			logger.info("Checkbox '" + checkboxText + "' is already in the desired state: "
					+ (isChecked ? "checked" : "unchecked"));
			TestListeners.extentTest.get().pass("Checkbox '" + checkboxText + "' is already in the desired state: "
					+ (isChecked ? "checked" : "unchecked"));
		}
		return isStateChanged;
	}

	// Default true
	public void setCheckboxStateViaCheckBoxText(String checkboxText) {
		setCheckboxStateViaCheckBoxText(checkboxText, true);
	}

	// Accepts "check" or "uncheck" as state
	public void setCheckboxStateViaCheckBoxText(String checkboxText, String state) {
		boolean shouldCheck = state.equalsIgnoreCase("check");
		setCheckboxStateViaCheckBoxText(checkboxText, shouldCheck);
	}

	public static String generateRandomString(int length) {
		String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		SecureRandom RANDOM = new SecureRandom();

		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
		}
		return sb.toString();
	}

	public String generateCodeChallenge(String codeVerifier) throws NoSuchAlgorithmException {
		// Step 1: Generate SHA-256 hash
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
		// Step 2: Encode hash in Base64
		String base64Hash = Base64.getEncoder().encodeToString(hash);
		// Step 3: Convert Base64 to Base64URL
		String base64UrlHash = base64Hash.replace("+", "-").replace("/", "_").replaceAll("=+$", "");
		logger.info("Generated Code Challenge: " + base64UrlHash);
		return base64UrlHash;
	}

	public String generateCodeVerifier(int length) {
		String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
		int CODE_VERIFIER_LENGTH = length;
		SecureRandom secureRandom = new SecureRandom();
		StringBuilder codeVerifier = new StringBuilder(CODE_VERIFIER_LENGTH);
		for (int i = 0; i < CODE_VERIFIER_LENGTH; i++) {
			int randomIndex = secureRandom.nextInt(CHARACTERS.length());
			codeVerifier.append(CHARACTERS.charAt(randomIndex));
		}
		logger.info("Generated Code Verifier: " + codeVerifier.toString());
		return codeVerifier.toString();
	}

	public static boolean validateJsonAgainstSchema(String expectedSchema, String actualString) {
		boolean result = false;
		try {
			// Load the schema
			JSONObject rawSchema = new JSONObject(new JSONTokener(expectedSchema));
			Schema schema = SchemaLoader.load(rawSchema);

			// Parse the JSON payload
			JSONObject json = new JSONObject(new JSONTokener(actualString));

			// Validate the JSON against the schema
			schema.validate(json);
			logger.info("JSON is valid against the schema");
			TestListeners.extentTest.get().pass("JSON is valid against the schema");

			result = true;

		} catch (ValidationException e) {
			result = false;
			logger.error("JSON is not valid against the schema: " + e.getMessage() + ". Schema violations: "
					+ e.getCausingExceptions().toString());
			TestListeners.extentTest.get().fail("JSON is not valid against the schema: " + e.getMessage()
					+ ". Schema violations: " + e.getCausingExceptions().toString());
		}
		return result;

	}

	public static boolean validateJsonArrayAgainstSchema(String expectedSchema, String actualString) {
		boolean result = false;
		try {
			// Load the schema
			JSONObject rawSchema = new JSONObject(new JSONTokener(expectedSchema));
			Schema schema = SchemaLoader.load(rawSchema);

			// Parse the JSON payload as an array of strings
			JSONArray jsonArray = new JSONArray(new JSONTokener(actualString));

			// Validate the JSON Array against the schema
			schema.validate(jsonArray);

			logger.info("JSON array is valid against the schema");
			TestListeners.extentTest.get().pass("JSON array is valid against the schema");
			result = true;

		} catch (ValidationException e) {
			result = false;
			logger.error("JSON array is not valid against the schema: " + e.getMessage() + ". Schema violations: "
					+ e.getCausingExceptions().toString());
			TestListeners.extentTest.get().fail("JSON array is not valid against the schema: " + e.getMessage()
					+ ". Schema violations: " + e.getCausingExceptions().toString());
		}
		return result;
	}

	public void duplicateTab(String url) {
		// driver.switchTo().newWindow(WindowType.TAB);
		switchToWindowN(1);
		driver.navigate().to(url);
		waitTillPagePaceDone();
	}

	public void waitTillNewCamsTableAppear() {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(180));
			wait.until(ExpectedConditions
					.visibilityOfElementLocated(By.xpath("//div[contains(@class,'table-container')]")));
			logger.info("Table Appeared");
		} catch (Exception e) {
			logger.warn(e.getMessage());
			TestListeners.extentTest.get().warning(e.getMessage());
		}
	}

	public boolean verifyPartOfURLwithoutPagePaceDone(String partOfURL) {
		boolean flag = false;
		String url = driver.getCurrentUrl();
		if (url.contains(partOfURL)) {
			logger.info(partOfURL + " part is verified from the current URL which is " + url);
			TestListeners.extentTest.get().info(partOfURL + " part is verified from the current URL which is " + url);
			flag = true;
		}
		return flag;
	}

	// Compares two lists of strings, ignoring whitespace and brackets
	public boolean compareList(List<String> actualList, List<String> expectedList) {
		List<String> formattedActualList = actualList.stream().map(item -> item.replaceAll("[\\[\\]]", "").trim())
				.collect(Collectors.toList());

		Set<String> actualSet = new HashSet<>(formattedActualList);
		Set<String> expectedSet = new HashSet<>(expectedList);

		if (!actualSet.equals(expectedSet)) {
			logger.info("Mismatch. Actual: " + actualSet + ", Expected: " + expectedSet);
			TestListeners.extentTest.get().info("Mismatch. Actual: " + actualSet + ", Expected: " + expectedSet);
			return false;
		}
		return true;
	}

	public static boolean isBinaryData(String input) {
		// Convert the string to a byte array using UTF-8 encoding
		byte[] bytes = input.getBytes();

		// Check each byte in the byte array
		for (byte b : bytes) {
			// If the byte value is outside the printable ASCII range (0x20 to 0x7e), it's
			// likely binary
			if (b < 32 || b > 126) {
				return true;
			}
		}
		return false;
	}

	public void tryAllClick(WebDriver driver, WebElement element) {

		try {
			// Attempt JavaScript click
			clickByJSExecutor(driver, element);
			logger.info("JavaScript click successful");
		} catch (Exception e) {
			logger.info("JavaScript click failed: " + e.getMessage());
		}

		try {
			// Attempt Actions click
			clickWithActions(element);
			logger.info("click with actions successful");
		} catch (Exception e) {
			logger.info("Actions click failed: " + e.getMessage());
		}

		try {
			// Attempt normal click
			element.click();
			logger.info("normal click successful");
		} catch (Exception e) {
			logger.info("Standard click failed: " + e.getMessage());
		}
	}

	/*
	 * public void waitTillPagePaceDone() { // First, check if "pace-running" exists
	 * and wait for "pace-done" to appear boolean isPaceRunning = false; try {
	 * implicitWait(2); // Set a short implicit wait to check for pace-running
	 * 
	 * // Wait for the page pace to be "running" (if it's present) before waiting
	 * for // "pace-done" WebDriverWait wait = new WebDriverWait(driver,
	 * Duration.ofSeconds(1)); // Short wait for checking // pace-running
	 * isPaceRunning = wait.until(ExpectedConditions
	 * .presenceOfElementLocated(By.xpath("//body[contains(@class,'pace-running')]")
	 * )) != null; } catch (Exception e) {
	 * logger.info("looks like timeout, waiting for pace run to complete: " +
	 * e.getMessage()); TestListeners.extentTest.get()
	 * .info("looks like timeout, waiting for pace run to complete: " +
	 * e.getMessage()); }
	 * 
	 * try { if (isPaceRunning) {
	 * logger.info("Page pace is running. Waiting for completion...");
	 * 
	 * // Wait for the "pace-done" class to appear WebDriverWait wait = new
	 * WebDriverWait(driver, Duration.ofSeconds(120)); // Wait up to 120 seconds for
	 * // pace-done to appear wait.until(
	 * ExpectedConditions.presenceOfElementLocated(By.xpath(
	 * "//body[contains(@class,'pace-done')]")));
	 * 
	 * logger.info("Page pace wait completed ..."); } else {
	 * logger.info("Pace running is not visible. No need to wait for pace done."); }
	 * } catch (TimeoutException e) {
	 * logger.warn("Timeout waiting for pace to complete: " + e.getMessage());
	 * TestListeners.extentTest.get().
	 * warning("Timeout waiting for pace to complete: " + e.getMessage()); } catch
	 * (NoSuchElementException e) { // Handle case where element doesn't exist on
	 * the page at all logger.warn("Element not found: " + e.getMessage());
	 * TestListeners.extentTest.get().warning("Element not found: " +
	 * e.getMessage()); } catch (Exception e) { logger.warn("Unexpected error: " +
	 * e.getMessage()); TestListeners.extentTest.get().warning("Unexpected error: "
	 * + e.getMessage()); } implicitWait(50); // Reset implicit wait to a longer
	 * duration after pace done }
	 */

	public void waitTillPagePaceDone() {
		try {
			// Temporarily reduce implicit wait to speed up presence check
			implicitWait(2);
			List<WebElement> paceRunningElements = driver
					.findElements(By.xpath("//body[contains(@class,'pace-running')]"));

			if (!paceRunningElements.isEmpty()) {
				logger.info("Page pace is running. Waiting for completion...");

				// Wait up to 120 seconds for "pace-done" to appear
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
				wait.until(
						ExpectedConditions.presenceOfElementLocated(By.xpath("//body[contains(@class,'pace-done')]")));

				logger.info("Page pace wait completed ...");
			} else {
				logger.info("Page Pace is not running. Skipping pace wait...");
			}
		} catch (TimeoutException e) {
			logger.warn("Timeout waiting for page pace to complete: " + e.getMessage());
			TestListeners.extentTest.get().warning("Timeout waiting for page pace to complete: " + e.getMessage());
		} catch (Exception e) {
			logger.warn("Unexpected error: " + e.getMessage());
			TestListeners.extentTest.get().warning("Unexpected error: " + e.getMessage());
		} finally {
			implicitWait(60); // Restore default implicit wait
		}
	}

	public boolean verifyUiActiveLink(String linkurl) throws IOException {
		boolean flag = false;
		URL url = new URL(linkurl);
		HttpURLConnection httpUrlConnect = (HttpURLConnection) url.openConnection();
//		longWaitInSeconds(1);
		sleep(300);
		httpUrlConnect.setConnectTimeout(3000);
		logger.info("Verifying link: " + linkurl);
		try {
			httpUrlConnect.connect();
			if (httpUrlConnect.getResponseCode() == 200) {
				flag = true;
				logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage());
				TestListeners.extentTest.get().pass(linkurl + " - " + httpUrlConnect.getResponseMessage());
			}
		} catch (Exception e) {
			logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - " + HttpURLConnection.HTTP_NOT_FOUND
					+ "-" + httpUrlConnect.getResponseCode());
			TestListeners.extentTest.get().fail(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - "
					+ HttpURLConnection.HTTP_NOT_FOUND + "-" + httpUrlConnect.getResponseCode());
		}

		return flag;
	}

	public boolean brokenUiLinks() throws IOException {
		boolean flag1 = false;
		boolean flag = true;
		int counter = 0;
		List<WebElement> allLink = getLocatorList("segmentPage.brokenUrlOfSegment");
		logger.info("Total links are " + allLink.size());
		TestListeners.extentTest.get().info("Total links are " + allLink.size());
		try {
			for (int i = 0; i < allLink.size(); i++) {
				WebElement ele = allLink.get(i);
				// if (ele.getAttribute("href").contains("#")) {
				// continue;
				// }
				System.out.println(ele.getAttribute("href"));
				String url = ele.getAttribute("href");
				if ((url.contains("javascript:void(0)"))) {
					continue;
				} else {
					flag1 = verifyUiActiveLink(url);
					if (flag1 == false) {
						counter++;
					}
				}
			}
		} catch (StaleElementReferenceException e) {

		}
		if (counter != 0) {
			flag = false;
		}
		return flag;
	}

	public boolean verifyExactURLwithoutPagePaceDone(String giveURL) {
		boolean flag = false;
		String url = driver.getCurrentUrl();
		if (url.equalsIgnoreCase(giveURL)) {
			logger.info(giveURL + " given url is verified from the current URL which is " + url);
			TestListeners.extentTest.get()
					.info(giveURL + " given url  is verified from the current URL which is " + url);
			flag = true;
		}
		return flag;
	}

	public static <T> List<T> getItemsInAButNotInB(List<T> listA, List<T> listB) {
		List<T> result = new ArrayList<>(listA);
		result.removeAll(listB);
		return result;
	}

	public LocalDateTime parseDate(String dateText, DateTimeFormatter formatter) {
		try {
			return LocalDateTime.parse(dateText, formatter);
		} catch (Exception e) {
			logger.error("Error parsing date: " + dateText, e);
			return null;
		}
	}

	public boolean apiResponseStatusCode(Response response, int statusCode) {
		boolean flag = false;
		if (response.getStatusCode() == statusCode) {
			flag = true;
		}
		return flag;
	}

	public boolean isflagNameVisible(String flagName) {
		try {
			waitTillPagePaceDone();
			implicitWait(3);
			WebElement operatingFlagName = driver.findElement(
					By.xpath(getLocatorValue("earningPage.flagNameVisible").replace("$flagName", flagName)));
			implicitWait(50);
			return operatingFlagName != null && operatingFlagName.isDisplayed(); // Check if element is displayed
		} catch (NoSuchElementException e) {
			implicitWait(50);
			return false;
		}
	}

	public boolean isToggleNameVisible(String flagName) {
		try {
			waitTillPagePaceDone();
			implicitWait(3);
			WebElement operatingFlagName = driver.findElement(
					By.xpath(getLocatorValue("earningPage.toggleNameVisible").replace("$flagName", flagName)));
			implicitWait(50);
			return operatingFlagName != null && operatingFlagName.isDisplayed(); // Check if element is displayed
		} catch (org.openqa.selenium.NoSuchElementException e) {
			implicitWait(50);
			return false;
		}
	}

	// Helper method to get a single field value from a Document
	public <T> T getFieldValue(Document document, String fieldPath, Class<T> expectedType) {
		Object current = document;
		for (String key : fieldPath.split("\\.")) {
			if (!(current instanceof Document)) {
				logger.error("Invalid field path '{}' before '{}'", fieldPath, key);
				throw new IllegalArgumentException(
						String.format("Invalid field path '%s' before '%s'", fieldPath, key));
			}
			current = ((Document) current).get(key);
			if (current == null) {
				logger.error("Field '{}' is missing or null", fieldPath);
				throw new IllegalArgumentException(String.format("Field '%s' is missing or null", fieldPath));
			}
		}

		if (expectedType == Double.class || expectedType == Float.class) {
			if (current instanceof Number) {
				return expectedType.cast(((Number) current).doubleValue());
			} else {
				logger.error("Field '{}' type mismatch. Expected a numeric type, Found: '{}'", fieldPath,
						current.getClass().getSimpleName());
				throw new IllegalArgumentException(
						String.format("Field '%s' type mismatch. Expected a numeric type, Found: '%s'", fieldPath,
								current.getClass().getSimpleName()));
			}
		}

		if (!expectedType.isInstance(current)) {
			logger.error("Field '{}' type mismatch. Expected: '{}', Found: '{}'", fieldPath,
					expectedType.getSimpleName(), current.getClass().getSimpleName());
			throw new IllegalArgumentException(String.format("Field '%s' type mismatch. Expected: '%s', Found: '%s'",
					fieldPath, expectedType.getSimpleName(), current.getClass().getSimpleName()));
		}
		logger.info("Field '{}' retrieved successfully with value: '{}'", fieldPath, current);
		return expectedType.cast(current);
	}

	// this method is used to find the value of a key in a JSON string mainly it is
	// using in webhook manager page logs
	public String findValueByKeyFromJsonAsString(String input, String key) {
		// Simple pattern: look for "key":<value>
		String pattern = "\"" + key + "\"\\s*:\\s*(\".*?\"|\\d+|true|false|null|\\[.*?\\]|\\{.*?\\})";
		java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
		java.util.regex.Matcher matcher = regex.matcher(input);

		if (matcher.find()) {
			String rawValue = matcher.group(1);
			return rawValue.replaceAll("^\"|\"$", ""); // remove surrounding quotes if present
		}
		return null; // Key not found
	}

	private void flattenJson(JsonNode node, String prefix, Map<String, Object> flatMap, ObjectMapper mapper) {
		Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
			JsonNode value = entry.getValue();
			if (value.isObject()) {
				flattenJson(value, key, flatMap, mapper);
			} else if (value.isArray() && value.size() > 0) {
				flatMap.put(key, mapper.convertValue(value, Object.class));
			} else if (value.isNull() || value.asText().isEmpty() || value.asText().equals("[]")
					|| value.asText().equals("{}") || value.asText().equals("null")) {
				// flatMap.put(key, null);
			} else {
				flatMap.put(key, value.asText());
			}
		}
	}

	public Map<String, Object> getJsonKeysValueFromJsonPayload(String json, String path, String prefix)
			throws Exception {
		Map<String, Object> flatMap = new HashMap<>();
		Map<String, Object> resultMap = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(json);

		// Navigate to the given path
		JsonNode targetNode = root;
		for (String part : path.split("\\.")) {
			targetNode = targetNode.path(part);
		}

		if (targetNode.isMissingNode() || targetNode.isNull()) {
			logger.info("Invalid path or node is null.");
			return flatMap;
		}

		// Flatten the target node
		flattenJson(targetNode, prefix, flatMap, mapper);

		for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (!key.endsWith("previous_changes")) {

				String updatedKey = key.replace(".", "_");
				resultMap.put(updatedKey, value);
			}
		}

		return resultMap;
	}

	// Method to execute terminal commands and return the output as a list of
	// strings
	// This method is used in AppiumUtils.java to get the iOS version of the
	// simulator device
	// Use this for local execution only. Do not use this method in the cloud.
	public List<String> executeTerminalCommand(String command) {
		List<String> output = new ArrayList<>();
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);

			// Read the output of the command
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				output.add(line);
			}
			logger.info("Terminal command output: " + output);

			// Wait for the process to complete
			int exitCode = process.waitFor();
			logger.info("Terminal command executed successfully with exit code: " + exitCode);

		} catch (IOException | InterruptedException e) {
			logger.error("Error executing terminal command: " + command, e);

		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return output;
	}

	// Method to get the iOS version of the simulator device. This method is used in
	// AppiumUtils.java to set the platform version.
	public String get_iOSVersion() {
		String version = null;
		try {
			List<String> output = executeTerminalCommand("xcrun simctl list runtimes");
			for (String line : output) {
				if (line.contains("iOS")) {
					version = line.split("\\s+")[1];
//					logger.info("iOS version: " + version);
//					break;
				}
			}
			logger.info("iOS version: " + version);
		} catch (Exception e) {
			logger.error("Error getting iOS version: ", e);
			TestListeners.extentTest.get().fail("Error getting iOS version: " + e.getMessage());
		}
		return version;
	}

	public Map<String, Object> replaceMapKeysWithNewKeys(Map<String, Object> originalMap,
			Map<String, String> keyMapping) {
		Map<String, Object> newMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
			String newKey = keyMapping.getOrDefault(entry.getKey(), entry.getKey());
			newMap.put(newKey, entry.getValue());
		}
		return newMap;
	}

	// this function is used to get the previous changes keys list for adapter in
	// webhook manager page
	// this function is used to get the previous changes keys list for adapter in
	// webhook manager page
	public List<String> getPreviousChangesKeysListForAdapter(String jsonString, String path) {
		List<String> previousChangesKeys = new LinkedList<String>();
		try {
			JSONObject data = new JSONObject(jsonString);

			// Check if the key exists in the JSON object
			if (!data.getJSONObject("payload").has(path)) {
				logger.warn("Key '" + path + "' not found in the JSON payload.");
				TestListeners.extentTest.get().warning("Key '" + path + "' not found in the JSON payload.");
				return previousChangesKeys; // Return an empty list if the key is not found
			}

			JSONArray previousChanges = data.getJSONObject("payload").getJSONArray(path);

			for (int i = 0; i < previousChanges.length(); i++) {
				previousChangesKeys.add(previousChanges.getString(i));
			}
		} catch (JSONException e) {
			logger.error("Error parsing JSON: " + e.getMessage());
			TestListeners.extentTest.get().fail("Error parsing JSON: " + e.getMessage());
		}
		return previousChangesKeys;
	}

	/**
	 * Filters the original map by a list of keys and returns a new map containing
	 * only the key-value pairs where the key exists in the list.
	 */
	public <K, V> Map<K, V> filterMapByKeys(List<K> keysToCheck, Map<K, V> originalMap) {
		Map<K, V> filteredMap = new HashMap<>();

		for (K key : keysToCheck) {
			if (originalMap.containsKey(key)) {
				filteredMap.put(key, originalMap.get(key));
			}
		}

		return filteredMap;
	}

	// it used only for webhook manager page to get the previous changes data list
	public List<String> getPreviousChangesDataList(String input) {
		List<String> resultList = new ArrayList<>();
//		        String input = "updated_at,gender,secondary_email,phone/zip_code,referral_code";

		if (input == null || input.isEmpty()) {
			logger.info("Input string is null or empty.");
			return resultList; // Return empty list if input is null or empty
		} else {
			// Step 1: Replace / with ,
			String[] dataArr = input.split("/");

			for (String part : dataArr) {
				// Step 2: Split by ,
				String[] parts = part.split(",");

				// Step 3: Add double quotes
				StringBuilder result = new StringBuilder();
				for (int i = 0; i < parts.length; i++) {
					result.append("\"").append(parts[i].trim()).append("\"");
					if (i < parts.length - 1) {
						result.append(",");
					}
				}

				// Step 4: Add to result list
				resultList.add(result.toString());
			}
		}
		return resultList;

	}

	public String verifiedKeyValueDispatchedEventPayload(Map<String, Object> flatMap, String payloadFromUI,
			List<String> keysToSkip) {
		String finalVerifiedString = "";
		for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
			String key = entry.getKey();
			String expValue = entry.getValue().toString();

			// Skip comparison for keys in the keysToSkip list
			if (keysToSkip.contains(key)) {
				logger.info("Skipping comparison for key: " + key);
				TestListeners.extentTest.get().info("Skipping comparison for key: " + key);
				continue;
			}

			String actualKeyValue = findValueByKeyFromJsonAsString(payloadFromUI, key);
			try {
				if (!actualKeyValue.equalsIgnoreCase(expValue) || actualKeyValue == null) {
					finalVerifiedString = finalVerifiedString + key + "/";
					logger.info(
							"Key: " + key + " does not match. Expected: " + expValue + ", Actual: " + actualKeyValue);
					TestListeners.extentTest.get().info(
							"Key: " + key + " does not match. Expected: " + expValue + ", Actual: " + actualKeyValue);

				} else {
					logger.info("Key: " + key + " matches actual and expected value: " + actualKeyValue);
					TestListeners.extentTest.get()
							.info("Key: " + key + " matches actual and expected value: " + actualKeyValue);

				}
			} catch (Exception e) {
				finalVerifiedString = finalVerifiedString + key + "/";
				logger.info(key + " not found in the payload");
				TestListeners.extentTest.get().fail(key + " not found in the payload");
			}
		}
		return finalVerifiedString;
	}

	public boolean checkFlagVisiblityOnUi(String labelName) {
		longWaitInSeconds(3);
		String pageSource = driver.getPageSource();

		// boolean flagIsPresent = utils.isTextPresent(driver, labelName);
		boolean flagIsPresent = pageSource.contains(labelName);
		implicitWait(50);
		return flagIsPresent;
	}

	public static String getPaymentToken(String str) {
		Pattern p = Pattern.compile("(?<=One_Time_Token=).*?(?=&|$)");
		logger.info("String to be searched: " + str);
		final Matcher m = p.matcher(str);
		if (m.find()) {
			logger.info("Pattern found: " + m.group(0).trim());
			return m.group(0).trim();
		} else {
			logger.warn("Pattern not found in the input string: " + str);
			return null; // or an appropriate fallback value
		}
	}

	public void uploadFile(WebDriver driver, WebElement fileInput, String path) {
		// if (driver instanceof RemoteWebDriver) {
		// ((RemoteWebElement) fileInput).setFileDetector(new LocalFileDetector());
		fileInput.sendKeys(path);
	}

	public String getSuccessMessageText() {
		waitTillElementToBeClickable((getLocator("instanceCommonElements.successMessage")));
		return getLocator("instanceCommonElements.successMessage").getText();

	}

	public void updateBusinessTimezone(String business, String timezone, String env) throws Exception {
		if (timezone != null && !timezone.isEmpty()) {
			String query = "UPDATE businesses SET preferences = REGEXP_REPLACE(preferences,':timezone_for_expiry: [^\\\\s]+',':timezone_for_expiry: "
					+ timezone + "') WHERE preferences REGEXP ':timezone_for_expiry: [^\\\\s]+' and name like '%"
					+ business + "%'";

			DBUtils.executeQuery(env, query);
		}
	}

	public String getCurrentURL() {
		String currentUrl = driver.getCurrentUrl();
		logger.info("Current URL: " + currentUrl);
		return currentUrl;

	}

	/**
	 * Get a cookie by its name from the browser
	 * 
	 * @param cookieName The name of the cookie to retrieve
	 * @return The cookie in format "cookieName=value"
	 * @throws AssertionError if the cookie is not found
	 */
	public String getCookieByName(String cookieName) {
		String cookieValue = null;
		Set<Cookie> allCookies = driver.manage().getCookies();
		for (Cookie cookie : allCookies) {
			if (cookieName.equals(cookie.getName())) {
				cookieValue = cookie.getName() + "=" + cookie.getValue();
				logger.info("Found cookie: " + cookieValue);
				break;
			}
		}

		if (cookieValue == null) {
			logger.error("Cookie '" + cookieName + "' not found");
		}

		return cookieValue;
	}

	// This function is used to get the cuurent time stamp and is used in webhook
	public String getCurrentTimeStampAfterEventTriggered() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm:ss a");
		LocalDateTime now = LocalDateTime.now();
		String formatted = now.format(formatter);
		return formatted.toString();
	}

	// to delete maass camp by name
	public void deleteMassCampaignByName(String massCampaignName, String env) throws Exception {
		if (massCampaignName != null && !massCampaignName.isEmpty()) {
			String query = "DELETE FROM mass_giftings WHERE name= '" + massCampaignName + "';";
			DBUtils.executeQuery(env, query);
		}
	}

	// This function is used to get the future date in YYYY-MM-DD HH:MM:SS format
	public static String getFutureDateForDBUpdate(int num) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, num);
		String output = sdf.format(c.getTime());
		return output;
	}

	// Sort by Keys (Descending)
	public static Map<String, String> sortByKeyDescending(Map<String, String> inputMap) {
		return inputMap.entrySet().stream().sorted(Map.Entry.<String, String>comparingByKey().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	// Sort by Values (Descending)
	public static Map<String, String> sortByValueDescending(Map<String, String> inputMap) {
		return inputMap.entrySet().stream().sorted(Map.Entry.<String, String>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	public static int getKeyIndex(Map<String, String> map, String targetKey) {
		int index = 0;
		for (String key : map.keySet()) {
			if (key.equals(targetKey)) {
				logger.info(targetKey + " targetKey is stored at index :- " + index);
				TestListeners.extentTest.get().pass(targetKey + " targetKey is stored at index :- " + index);
				return index;
			}
			index++;
		}
		return -1; // key not found
	}

	public String getBGColor(WebElement ele) {
		String rgbFormat = ele.getCssValue("background-color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		System.out.println("hexcode is :" + hexcode);
		return hexcode;
	}

	public void updateBusinessRedemption1Dot0Flag(String business, boolean isTrue, String env) throws Exception {
		String query = "UPDATE businesses SET preferences = REGEXP_REPLACE(preferences,':return_qualifying_condition_to_v1: [^\\\\s]+',':return_qualifying_condition_to_v1: "
				+ isTrue
				+ "') WHERE preferences REGEXP ':return_qualifying_condition_to_v1: [^\\\\s]+' and name like '%"
				+ business + "%'";

		DBUtils.executeQuery(env, query);
	}

	// This function is used to remove the brackets and get the percentage value - 1
	// (80%) to 1
	public String removePercentageBracketsAndGetPercentage(String input) {
		// Remove everything from first space (i.e., remove the parentheses part)
		String output = input.replaceAll("\\s*\\(.*?\\)", "").trim();
		return output;
	}

	public void waitTillSegmentSidePanelClickable() {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(200));
			wait.until(ExpectedConditions
					.invisibilityOfElementLocated(By.xpath("//div[contains(@class,'_skeleton-multi')]")));
			logger.info("Segment Side Pannel Clickable");
			TestListeners.extentTest.get().info("Segment Side Pannel Clickable");
		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
		}
	}

	public boolean checkLsatDateOfMonth(int date) {
		boolean isLastDate = false;

		int day = date; // your input (dd)

		// Get current system date
		LocalDate today = LocalDate.now();
		int month = today.getMonthValue(); // current month (1–12)
		int year = today.getYear(); // current year

		// Get the last day of the current month
		YearMonth ym = YearMonth.of(year, month);
		int lastDay = ym.lengthOfMonth();

		if (day == lastDay) {
			isLastDate = true;
		}
		return isLastDate;
	}

	public void flushSidekiq(String env) {
		try {
			prop = loadPropertiesFile("config.properties");
			String url = prop.getProperty(env + ".sidekiqUrl");
			// String val = encrypt("");
			String sidekiqUrl = decrypt(url);
			try (Jedis jedis = new Jedis(sidekiqUrl, 6379)) {
				jedis.flushDB();
				logger.info("Redis DB flushed (all Sidekiq jobs + data cleared)");
				boolean sidekiqEmpty = verifySidekiqEmpty(jedis);
				if (sidekiqEmpty) {
					logger.info("Verification successful: All Sidekiq queues, retries, schedule, and dead jobs are 0");
				} else {
					logger.warn("Some Sidekiq keys still have jobs");
				}
			}
		} catch (Exception e) {
			logger.error("Failed to flush Sidekiq: " + e.getMessage(), e);
		}
	}

	private boolean verifySidekiqEmpty(Jedis jedis) {
		boolean allClear = true;
		// Check standard Sidekiq sets
		allClear &= checkSortedSet(jedis, "retry");
		allClear &= checkSortedSet(jedis, "schedule");
		allClear &= checkSortedSet(jedis, "dead");
		// Check all Sidekiq queues
		String cursor = "0";
		ScanParams scanParams = new ScanParams().match("queue:*").count(100);
		do {
			ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
			List<String> keys = scanResult.getResult();
			for (String queue : keys) {
				long count = jedis.llen(queue);
				if (count > 0) {
					logger.info("Queue not empty: " + queue + " has " + count + " jobs");
					allClear = false;
				} else {
					logger.info("Queue empty: " + queue);
				}
			}
			cursor = scanResult.getCursor();
		} while (!cursor.equals("0"));
		return allClear;
	}

	private boolean checkSortedSet(Jedis jedis, String key) {
		long count = jedis.zcard(key);
		if (count > 0) {
			logger.info("" + key + " still has " + count + " jobs");
			return false;
		} else {
			logger.info("" + key + " is empty");
			return true;
		}
	}

	public static String extractLeadingInteger(String input) {
		if (input == null || input.isEmpty()) {
			return null;
		}

		// Use regex to match a number at the start of the string
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\d+").matcher(input.trim());

		if (matcher.find()) {
			return matcher.group(); // Return the matched digits as a String
		}

		return null; // Return null if no integer is found
	}

	// Delete Subscription Plan from DB table
	public void deleteSubscriptionPlan(String env, String planId, String businessId) throws Exception {
		if (planId != null && !planId.isEmpty()) {
			String query = "DELETE FROM subscription_plans WHERE id = " + planId + " AND business_id = " + businessId
					+ ";";
			DBUtils.executeQuery(env, query);
			logit("Subscription Plan ID[" + planId + "] has been deleted successfully");
		}
	}

	// Delete LIS and QC and Redeemable from DB
	public void deleteLISQCRedeemable(String env, String actualExternalIdLIS, String actualExternalIdQC,
			String actualExternalIdRedeemable) throws Exception {
		if (actualExternalIdLIS != null && !actualExternalIdLIS.isEmpty()) {
			// // Delete LIS 1
			String deleteLISQuery1 = "Delete from line_item_selectors where external_id ='" + actualExternalIdLIS
					+ "';";
			DBUtils.executeQuery(env, deleteLISQuery1);

			logger.info("LIS External ID: " + actualExternalIdLIS);
			TestListeners.extentTest.get().info("LIS External ID: " + actualExternalIdLIS);
		}

		if (actualExternalIdQC != null && !actualExternalIdQC.isEmpty()) {
			// Delete Qualifying Expressions
			String getQCIDQuery = "select id from qualification_criteria where external_id ='" + actualExternalIdQC
					+ "';";
			String qcID = DBUtils.executeQueryAndGetColumnValue(env, getQCIDQuery, "id");
			String deleteQCFromQualifying_expressions = "delete from qualifying_expressions where qualification_criterion_id ='"
					+ qcID + "';";
			DBUtils.executeQuery(env, deleteQCFromQualifying_expressions);

			// Delete QC
			String deleteQCFromQualification_criteria = "delete from qualification_criteria where external_id = '"
					+ actualExternalIdQC + "';";
			DBUtils.executeQuery(env, deleteQCFromQualification_criteria);

		}

		if (actualExternalIdRedeemable != null && !actualExternalIdRedeemable.isEmpty()) {

			// Delete from redeemable tables
			String deleteRedeemableQuery = "delete from redeemables where uuid ='" + actualExternalIdRedeemable + "';";
			DBUtils.executeQuery(env, deleteRedeemableQuery);

			logger.info("LIS, QC and redeemable has been deleted successfully");
			TestListeners.extentTest.get().pass("LIS, QC and redeemable has been deleted successfully");
		}

	}

	public String getUserIdFromUrl() {
		String url = driver.getCurrentUrl();
		String id = null;
		Pattern pattern = Pattern.compile(".*/guests/(\\d+)");
		Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			id = matcher.group(1);
			logger.info("Extracted user ID: " + id);
			TestListeners.extentTest.get().info("Extracted user ID: " + id);
		} else {
			logger.info("user ID not found in URL.");
			TestListeners.extentTest.get().info("user ID not found in URL.");
		}
		return id;
	}

	public static Map<String, String> parseDetailsToMap(String details) {
		Map<String, String> detailsMap = new HashMap<>();
		for (String line : details.split("\n")) {
			String[] parts = line.split(":", 2);
			if (parts.length == 2) {
				String key = parts[0].trim();
				String value = parts[1].trim().replaceAll("^'+|'+$", "");
				detailsMap.put(key, value);
			}
		}
		return detailsMap;
	}

	public static List<String> getPreferencesKeyValue(String preferences, String keyToBeFetched) {
		// Regular expression to match the key and extract its values
		Pattern pattern = Pattern.compile(":" + keyToBeFetched + ":\\s*([^:\\n]+|[\\w,]+)");
		Matcher matcher = pattern.matcher(preferences);

		// List to store all values
		List<String> valuesList = new ArrayList<>();
		String keyValues = "";
		// Find all matches
		while (matcher.find()) {
			keyValues = matcher.group();
		}

		if (keyValues.isEmpty()) {
			logger.info(keyToBeFetched + " key values is empty");
			TestListeners.extentTest.get().info(keyToBeFetched + " key values is empty");
			return valuesList;
		}

		keyValues = keyValues.replace(" ", "");
		String[] valArr = keyValues.split(":" + keyToBeFetched + ":");

		String[] keyValArr = valArr[1].split(",");
		for (String strArrVal : keyValArr) {
			valuesList.add(strArrVal);
		}

		// Print the extracted values
		if (!valuesList.isEmpty()) {
			logger.info(keyToBeFetched + " values: " + valuesList);

		} else {
			logger.info(keyToBeFetched + " Key not found!");

		}
		return valuesList;
	}

	public String getCamIdFromUrl(String url) {

		String[] parts = url.split("/");
		String withParams = parts[parts.length - 1];
		String campaignID = withParams.split("\\?")[0];
		return campaignID;

	}

	public void deleteCsvFileFromBulkGuestActivitiesTable(String BMUfileName, String env) throws Exception {
		String query = "DELETE FROM bulk_guest_activities WHERE name = '" + BMUfileName + "';";
		DBUtils.executeQuery(env, query);
	}

	public void scrollToBottomOfMainContent(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("var el = document.querySelector('.new-layout-main-content');"
				+ "if (el) el.scrollTop = el.scrollHeight;");
	}

	// Updates flags in the preferences or similar column of any table
	public void updatePreferenceFlag(String env, String tableName, String colName, String whereCol, String whereValue,
			String preferenceKey, String flagValue) throws Exception {

		// Construct the SQL query to fetch current preferences
		String query = String.format("SELECT %s FROM %s WHERE %s = '%s'", colName, // column to fetch
				tableName, // table name
				whereCol, // column in WHERE clause
				whereValue // value in WHERE clause
		);

		// Fetch current preferences
		String currentPreferences = DBUtils.executeQueryAndGetColumnValue(env, query, colName);

		// Update the preference
		boolean isUpdated = DBUtils.updateBusinessesPreference(env, currentPreferences, flagValue, preferenceKey,
				whereValue);

		// Assert and log
		Assert.assertTrue(isUpdated, preferenceKey + " value is not updated to " + flagValue);
		logger.info(preferenceKey + " value is updated to " + flagValue);
	}

	public void pressKey(Keys key) {
		new Actions(driver).sendKeys(key).perform();
	}

	public static String convertToEscapedJsonString(String rawJson) throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		// Parse raw JSON → Object
		Object jsonObject = mapper.readTree(rawJson);

		// Convert Object → escaped JSON string
		return mapper.writeValueAsString(jsonObject);
	}

	public String convertToDoubleStringified(JsonNode rawJson) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode builderClauseNode = rawJson;
		String escapedBuilderClause = mapper.writeValueAsString(builderClauseNode);

		Object builderClausesObject = mapper.readTree(convertToEscapedJsonString(escapedBuilderClause));
		// First stringify
		String firstStringify = mapper.writeValueAsString(builderClausesObject);

		// Second stringify
		return mapper.writeValueAsString(firstStringify);
	}

	public void waitTillElementToBeVisible(WebElement WebElement) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
			wait.until(ExpectedConditions.elementToBeClickable(WebElement));

			wait.until(driver -> (Boolean) ((JavascriptExecutor) driver).executeScript("var e = arguments[0];"
					+ "var r = e.getBoundingClientRect();" + "return e && !e.disabled && "
					+ "r.width > 0 && r.height > 0 && " + "window.getComputedStyle(e).pointerEvents !== 'none';",
					WebElement));
		} catch (Exception e) {
			logger.error("WebElement failed to load/appear: " + e);
			TestListeners.extentTest.get().info("WebElement failed to load/appear: " + e);
		}
	}

	public String convertToDoubleStringifiedProperties(String rawJson) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		// Parse raw JSON → Object
		Object builderClausesObject = mapper.readTree(convertToEscapedJsonString(rawJson));
		// First stringify
		String firstStringify = mapper.writeValueAsString(builderClausesObject);

		// Second stringify (same as JSON.stringify(JSON.stringify(...)))
		return mapper.writeValueAsString(firstStringify);
	}

	public static String decodeJwtPayload(String jwt) throws Exception {
		if (jwt == null)
			throw new IllegalArgumentException("jwt is null");
		String[] parts = jwt.split("\\.");
		if (parts.length < 2)
			throw new IllegalArgumentException("invalid jwt");
		byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
		return new String(decoded, StandardCharsets.UTF_8);
	}

	public static String getPreferenceValue(String preferences, String key) {
		List<String> values = getPreferencesKeyValue(preferences, key);

		if (values == null || values.isEmpty()) {
			return null;
		}

		return values.get(0).replaceAll("[\\[\\]' ]", "");
	}
}
