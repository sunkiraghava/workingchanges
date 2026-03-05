package com.punchh.server.utilities;

import com.aventstack.extentreports.ExtentReports;
import com.jayway.jsonpath.JsonPath;
import de.taimos.totp.TOTP;
import io.appium.java_client.android.AndroidDriver;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.*;
import javax.mail.search.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.*;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * �* The Class Utils. �
 */
@Listeners(TestListeners.class)
public class AndroidUtilities {

  static Logger logger = LogManager.getLogger(AndroidUtilities.class);
  private AndroidDriver driver;
  static Properties prop = new Properties();
  ExtentReports extentReport;
  public static File jsonFile;
  String regex = "\\((.*?)\\)";
  private static final byte[] keyValue =
      new byte[] {'m', 'Y', 'p', 'U', 'b', 'l', 'I', 'c', 'k', 'E', 'y', 'n', 'A', 'e', 'E', 'M'};
  private static final String ALGO = "AES";


  public AndroidUtilities(AndroidDriver driver1) {
    this.driver = driver1;
    if (jsonFile == null) {
      jsonFile =
          new File(System.getProperty("user.dir") + "//resources//Locators//objRepository.json");
    }

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

  public static String getConfigProperty(String key) {
    // String propFilePath = System.getProperty("user.dir") +
    // "/src/test/resources/Properties/config.properties";
    String propFilePath =
        System.getProperty("user.dir") + "/resources/Properties/config.properties";
    FileInputStream fis;
    try {
      fis = new FileInputStream(propFilePath);
      prop.load(fis);
    } catch (Exception e) {
      e.printStackTrace();
    }
    String value = prop.get(key).toString();
    if (StringUtils.isEmpty(value)) {
      try {
        throw new Exception("Unspecified for key: " + key + " in properties file.");
      } catch (Exception e) {
      }
    }
    return value;
  }

  public static String getApiConfigProperty(String key) {
    // String propFilePath = System.getProperty("user.dir") +
    // "/src/test/resources/Properties/apiConfig.properties";
    String propFilePath =
        System.getProperty("user.dir") + "/resources/Properties/apiConfig.properties";
    FileInputStream fis;
    try {
      fis = new FileInputStream(propFilePath);
      prop.load(fis);
    } catch (Exception e) {
      e.printStackTrace();
    }
    String value = prop.get(key).toString();
    if (StringUtils.isEmpty(value)) {
      try {
        throw new Exception("Unspecified for key: " + key + " in properties file.");
      } catch (Exception e) {
      }
    }
    return value;
  }

  public static String getDBConfigProperty(String key) {
    String propFilePath =
        System.getProperty("user.dir") + "/resources/Properties/dbConfig.properties";
    FileInputStream fis;
    try {
      fis = new FileInputStream(propFilePath);
      prop.load(fis);
    } catch (Exception e) {
      e.printStackTrace();
    }
    String value = prop.get(key).toString();
    if (StringUtils.isBlank(value)) {
      try {
        throw new Exception("Unspecified for key: " + key + " in DB config properties file.");
      } catch (Exception e) {
      }
    }
    return value;
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

  public WebElement getLocator(String locator) {
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
     // TestListeners.extentTest.get().fail("Error while getting locator: " + e);
      logger.error("Error while getting locator: " + e);
    }
    return null;
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

  public static String getCurrentDate(String format) {
    DateFormat dateFormat = new SimpleDateFormat(format);
    Date currentDate = new Date();
    return dateFormat.format(currentDate);
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

}
