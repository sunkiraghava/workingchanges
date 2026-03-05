package com.punchh.server.pages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SchedulePage {
	static Logger logger = LogManager.getLogger(SchedulePage.class);
	private WebDriver driver;
	Properties prop, propDb;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String scheduleName;
	private PageObj pageObj;
	String regx = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";

	public SchedulePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		propDb = Utilities.loadPropertiesFile("dbConfig.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public int verifyDataExportSchedule(String scheduleType, String segmentScheduleName, String ftpPath) {
		int fileCount = 0;
		try {
			selectDataExportSchedule(scheduleType, segmentScheduleName);
			utils.longWaitInSeconds(3);
			if (utils.getLocator("schedulePage.logTextLabel").getText().contains("Completed")) {
				String logText = utils.getLocator("schedulePage.logTextLabel").getText();
				List<String> fileNameList = utils.getexportedFileNameList(logText);
				fileCount = fileNameList.size();
				if (fileNameList.size() == 0)
					Assert.fail("Segment export file count appears to be zero, please check");
				for (String fileName : fileNameList) {
					System.out.println(fileName);
					utils.downloadFTPFile(fileName, ftpPath);
				}
			} else {
				TestListeners.extentTest.get().fail("Segment export file is not complete");
				Assert.fail("Segment export file is not complete");
			}
		} catch (Exception e) {
			selUtils.AddScreenshot("Segment export");
			logger.error("Error in running schedule " + e);
			TestListeners.extentTest.get().fail("Error in running schedule " + e);
			Assert.fail("Error in running schedule " + e);
		}
		return fileCount;
	}

	public boolean getlogsTextLabel() {
		boolean flag = false;
		utils.waitTillPagePaceDone();
		int attempts = 0;
		utils.implicitWait(5);
		while (attempts <= 50) {
			try {
				utils.longWaitInSeconds(4);
				WebElement logsBox = utils.getLocator("schedulePage.logTextLabel");
				utils.waitTillVisibilityOfElement(logsBox, "Logs box");
				if (logsBox.isDisplayed()) {
					logger.info("Logs box is displayed");
					TestListeners.extentTest.get().pass("Logs box is displayed");
					flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Logs box is displayed : " + attempts);
				TestListeners.extentTest.get().info("Element is not present or Logs box is displayed : " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		utils.implicitWait(60);
		return flag;
	}

	public boolean selectDataExportSchedule(String scheduleType, String scheduleName) {
		logger.info("Exporting Data");
		utils.waitTillPagePaceDone();
		boolean resultFlag = false;
		utils.getLocator("schedulePage.scheduleTypeDropdown").isDisplayed();
		Select sel = new Select(utils.getLocator("schedulePage.scheduleTypeDropdown"));
		sel.selectByValue(scheduleType);
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.editScheduleLink").replace("temp", scheduleName)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.editScheduleLink").replace("temp", scheduleName))).click();
		/*
		 * utils.longWaitInSeconds(7); utils.refreshPage();
		 * utils.waitTillPagePaceDone();
		 * utils.waitTillVisibilityOfElement(utils.getLocator(
		 * "schedulePage.logTextLabel"), "logs text"); boolean flag =
		 * utils.getLocator("schedulePage.logTextLabel").isDisplayed();
		 */
		boolean flag = getlogsTextLabel();
		Assert.assertTrue(flag, "Log text on Data Export Schedule is not displayed");
		selUtils.scrollToElement(utils.getLocator("schedulePage.logTextLabel"));
		// selUtils.AddScreenshot("Segment export");
		if (scheduleName.contains("DataExport")) {
			for (int i = 0; i <= 400; i++) {
				driver.navigate().refresh();
				utils.waitTillPagePaceDone();
				if ((utils.getLocator("schedulePage.logTextLabel").getText().contains("failed")
						|| utils.getLocator("schedulePage.logTextLabel").getText().contains("Error"))) {
					logger.info("Error is - " + utils.getLocator("schedulePage.logTextLabel").getText());
					TestListeners.extentTest.get()
							.fail("Error is - " + utils.getLocator("schedulePage.logTextLabel").getText());
					resultFlag = false;
					break;
				}
				if (utils.getLocator("schedulePage.logTextLabel").getText().contains("Completed")
						&& utils.getLocator("schedulePage.logTextLabel").getText().contains("Sending email to")) {
					resultFlag = true;
					break;
				} else {
					utils.longWaitInSeconds(3);
				}
				logger.info(i);
			}
		} else if (scheduleName.contains("Segment")) {
			for (int i = 0; i <= 300; i++) {
				driver.navigate().refresh();
				utils.waitTillPagePaceDone();
				if (utils.getLocator("schedulePage.logTextLabel").getText().contains("Completed")) {
					resultFlag = true;
					break;
				} else {
					selUtils.longWait(3000);
				}
				logger.info(i);
			}
		}
		return resultFlag;
	}

	public String scheduleNewDataExport(String ftp) {
		String scheduleName = null;
		try {
//			utils.getLocator("schedulePage.dataExportScheduleNameTextbox").isDisplayed();
//			utils.getLocator("schedulePage.dataExportScheduleNameTextbox").clear();
//			scheduleName = CreateDateTime.getUniqueString("AutoSchedule");
//			utils.getLocator("schedulePage.scheduleNameTextbox").sendKeys(scheduleName);
//			logger.info("Segment schedule name is set as: " + scheduleName);
//			TestListeners.extentTest.get().info("Segment schedule name is set as: " + scheduleName);
			utils.getLocator("schedulePage.scheduleFrequencyDropdown").isDisplayed();
			Select sel = new Select(utils.getLocator("schedulePage.scheduleFrequencyDropdown"));
			sel.selectByValue(prop.getProperty("frequencyImmediate"));
			logger.info("Segment schedule namefrequency is set as: " + sel.getFirstSelectedOption().getText());
			TestListeners.extentTest.get()
					.info("Segment schedule namefrequency is set as: " + sel.getFirstSelectedOption().getText());
			utils.getLocator("schedulePage.emailTextField").isDisplayed();
			utils.getLocator("schedulePage.emailTextField").sendKeys(prop.getProperty("exportEmail"));
			utils.getLocator("schedulePage.ftpEndPointDropdown").isDisplayed();
			Select sel1 = new Select(utils.getLocator("schedulePage.ftpEndPointDropdown"));
			sel1.selectByVisibleText(ftp);
			logger.info("FTP end point is set as: " + sel.getFirstSelectedOption().getText());
			TestListeners.extentTest.get().info("FTP end point is set as: " + sel.getFirstSelectedOption().getText());
			utils.getLocator("schedulePage.filePrefixTextbox").isDisplayed();
			utils.getLocator("schedulePage.filePrefixTextbox").sendKeys("AutoExport");
			utils.getLocator("schedulePage.saveScheduleButton").isDisplayed();
			utils.getLocator("schedulePage.saveScheduleButton").click();
			selUtils.longWait(8000);
			utils.getLocator("schedulePage.scheduleSuccessMsgLabel").isDisplayed();
			logger.info(utils.getLocator("schedulePage.scheduleSuccessMsgLabel").getText());
			TestListeners.extentTest.get().pass(utils.getLocator("schedulePage.scheduleSuccessMsgLabel").getText());
		} catch (Exception e) {
			logger.error("Error in scheduling new segment export " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return scheduleName;
	}

	public String scheduleNewDataExportPerf() {
		String scheduleName = null;
		try {
			utils.getLocator("schedulePage.scheduleFrequencyDropdown").isDisplayed();
			Select sel = new Select(utils.getLocator("schedulePage.scheduleFrequencyDropdown"));
			sel.selectByValue("weekly");
			logger.info("Segment schedule namefrequency is set as: " + sel.getFirstSelectedOption().getText());
			TestListeners.extentTest.get()
					.info("Segment schedule namefrequency is set as: " + sel.getFirstSelectedOption().getText());
			utils.getLocator("schedulePage.emailTextField").isDisplayed();
			utils.getLocator("schedulePage.emailTextField").sendKeys("vignesh.nadar@punchh.com");
//			utils.getLocator("schedulePage.ftpEndPointDropdown").isDisplayed();
//			Select sel1 = new Select(utils.getLocator("schedulePage.ftpEndPointDropdown"));
//			sel1.selectByVisibleText(prop.getProperty("ftpEndPoint"));
//			logger.info("FTP end point is set as: " + sel.getFirstSelectedOption().getText());
//			TestListeners.extentTest.get().info("FTP end point is set as: " + sel.getFirstSelectedOption().getText());

			Select sel2 = new Select(driver.findElement(By.id("schedule_day_of_week")));
			sel2.selectByVisibleText("Friday");
			driver.findElement(By.id("schedule_start_date")).sendKeys("2021-09-17");
			Select sel1 = new Select(driver.findElement(By.id("schedule_start_time")));
			sel1.selectByVisibleText("04:00 AM");
			utils.getLocator("schedulePage.filePrefixTextbox").isDisplayed();
			utils.getLocator("schedulePage.filePrefixTextbox").sendKeys("AutoExport");
			utils.getLocator("schedulePage.saveScheduleButton").isDisplayed();
			utils.getLocator("schedulePage.saveScheduleButton").click();
			selUtils.implicitWait(50);
			utils.getLocator("schedulePage.scheduleSuccessMsgLabel").isDisplayed();
			logger.info(utils.getLocator("schedulePage.scheduleSuccessMsgLabel").getText());
			TestListeners.extentTest.get().pass(utils.getLocator("schedulePage.scheduleSuccessMsgLabel").getText());
		} catch (Exception e) {
			logger.error("Error in scheduling new segment export " + e);
			TestListeners.extentTest.get().fail("Error in verifying guest time line " + e);
		}
		return scheduleName;
	}

	public void editSchedule(String segmentScheduleName) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.editScheduleLink").replace("temp", segmentScheduleName)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.editScheduleLink").replace("temp", segmentScheduleName)))
				.click();
		////

//		driver.findElement(
//		By.xpath(utils.getLocatorValue("schedulePage.runButton").replace("temp", segmentScheduleName)))
//		.isDisplayed();
//driver.findElement(
//		By.xpath(utils.getLocatorValue("schedulePage.runButton").replace("temp", segmentScheduleName)))
//		.click();
//logger.info("Schedule type is selected as: " + sel.getFirstSelectedOption());
//TestListeners.extentTest.get().pass("Schedule type is selected as: " + sel.getFirstSelectedOption());
//driver.switchTo().alert().accept();
//utils.getLocator("schedulePage.segmentExportExecutedLabel").isDisplayed();
//	driver.navigate().back();
//driver.findElement(
//		By.xpath(utils.getLocatorValue("schedulePage.runButton").replace("temp", segmentScheduleName)))
//		.isDisplayed();

	}

	public String scheduleNewEmailExport(String ExportFilePrefix) {
		utils.getLocator("schedulePage.scheduleFrequencyDropdown").isDisplayed();
		Select sel = new Select(utils.getLocator("schedulePage.scheduleFrequencyDropdown"));
		sel.selectByValue(prop.getProperty("frequencyImmediate"));
		logger.info("Segment schedule namefrequency is set as: " + sel.getFirstSelectedOption().getText());
		TestListeners.extentTest.get()
				.info("Segment schedule namefrequency is set as: " + sel.getFirstSelectedOption().getText());
		utils.getLocator("schedulePage.emailTextField").isDisplayed();
		utils.getLocator("schedulePage.emailTextField").sendKeys(prop.getProperty("exportEmail"));
		utils.getLocator("schedulePage.ftpEndPointDropdown").isDisplayed();
		if (ExportFilePrefix != "") {
			utils.getLocator("schedulePage.filePrefixTextbox").isDisplayed();
			utils.getLocator("schedulePage.filePrefixTextbox").sendKeys(ExportFilePrefix);
		}
//		utils.getLocator("schedulePage.updateScheduleButton").click();
		utils.getLocator("schedulePage.saveScheduleButton").isDisplayed();
		utils.getLocator("schedulePage.saveScheduleButton").click();
		utils.waitTillPagePaceDone();
		boolean flag = utils.getLocator("schedulePage.scheduleSuccessMsgLabel").isDisplayed();
		Assert.assertTrue(flag, "Schedule Success Message is not displayed");
		String successMsg = utils.getLocator("schedulePage.scheduleSuccessMsgLabel").getText();
		logger.info("Schedule Success Message is displayed: " + successMsg);
		TestListeners.extentTest.get().pass("Schedule Success Message is displayed: " + successMsg);
		return successMsg;
	}

	@SuppressWarnings("static-access")
	public String verifyExportSchedule(String env, String scheduleType, String segmentScheduleName, String ftpPath) {
		String fileName = null;
		String scheduleId = null;
		Assert.assertTrue(selectDataExportSchedule(scheduleType, segmentScheduleName), "Error in Schedule page Logs");
		Pattern pattern = Pattern.compile("/(\\d{4,6})/");
		Matcher matcher = pattern.matcher(driver.getCurrentUrl());
		if (matcher.find()) {
			scheduleId = matcher.group(1);
		}
		utils.longWaitInSeconds(7);
		utils.waitTillVisibilityOfElement(utils.getLocator("schedulePage.logTextLabel"), "Logs");
		try {
			if (utils.getLocator("schedulePage.logTextLabel").getText().contains("Completed")) {
				String logText = utils.getLocator("schedulePage.logTextLabel").getText();
				fileName = utils.getexportedFileNameList(logText).get(0);
				String query = "Select schedule_data FROM schedule_tasks st WHERE schedule_id =" + scheduleId;
				String scheduleData = DBUtils.executeQueryAndGetColumnValue(env, query,
						"schedule_data");

				String link = null;
				Pattern p = Pattern.compile(regx, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(scheduleData);
				while (m.find()) {
					// links.add(m.group());
					link = m.group();
				}
				logger.info("URL" + link);
				driver.navigate().to(link);
				Utilities.longWait(8000);
				logger.info("====File path ====");
				TestListeners.extentTest.get().info("====File path ====");
				logger.info(new File(fileName).getAbsolutePath());
				TestListeners.extentTest.get().info(new File(fileName).getAbsolutePath());
				if (utils.verifyFileExists(fileName)) {
					logger.info("Verified segment export file download: " + fileName);
					TestListeners.extentTest.get().pass("Verified segment export file download: " + fileName);
				} else {
					logger.error("Failed to verify segment export file download");
					TestListeners.extentTest.get().fail("Failed to verify segment export file download");
					Assert.fail("Failed to download file:" + fileName);
				}
			} else {
				TestListeners.extentTest.get().fail("Segment export file is not complete");
				Assert.fail("Segment export file is not complete");
			}
		} catch (Exception e) {
			selUtils.AddScreenshot("Segment export");
			logger.error("Error in running schedule " + e.getMessage());
			TestListeners.extentTest.get().fail("Error in running schedule " + e);
			Assert.fail("Error in running schedule " + e);
		}
		return fileName;
	}

	public boolean verifyColumns(String fileName, List<String> fieldList) {
		boolean flag = true;
		boolean flag1 = false;
		List<String> list = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(System.getProperty("user.dir") + prop.getProperty("downloadPath") + fileName));
			String line = null;
			while ((line = reader.readLine()) != null) {
				list.add(line);
			}
			reader.close();
			String arr[] = list.get(1).split(",");
			for (String str : arr)
				logger.info(str + " ");
			list = Arrays.asList(arr);
			utils.longWaitInSeconds(2);
			for (String value : list) {
				value = value.trim();
				flag1 = fieldList.contains(value);
				if (flag1 == false) {
					flag = false;
				}
			}
			logger.info("Comparison of rows in csv file is: " + flag1);
			TestListeners.extentTest.get().info("Comparison of rows in csv file is: " + flag1);
		} catch (Exception e) {
			logger.info("Error verifying column" + e);
			TestListeners.extentTest.get().fail("Error verifying column " + fileName + " error " + e);
			Assert.fail("Error verifying column");
		}
		return flag;
	}

	public List<String> extractUrls(String text) {
		List<String> containedUrls = new ArrayList<String>();
		String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
		Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
		Matcher urlMatcher = pattern.matcher(text);

		while (urlMatcher.find()) {
			containedUrls.add(text.substring(urlMatcher.start(0), urlMatcher.end(0)));
		}

		return containedUrls;
	}

	public boolean schedulePageLogs(String logText, List<String> pageLogsList) {
		boolean logFlag = false;
		boolean logFlag1 = false;
		int counter = 0;
		for (String pLogs : pageLogsList) {
			logFlag1 = logText.contains(pLogs);
			if (logFlag1) {
				counter++;
			}
		}
		if (pageLogsList.size() == counter) {
			logFlag = true;
		}
		return logFlag;
	}

	@SuppressWarnings("static-access")
	public ArrayList<String> verifyExportScheduleForAll(String scheduleType, String segmentScheduleName, String env,
			List<String> pageLogsList, HashMap<String, ArrayList<String>> parentMap) {
		boolean logFlag = false;
		String logText = "";
		ArrayList<String> fileNameList = new ArrayList<String>();
		ArrayList<String> nameOfFiles = new ArrayList<String>();
		List<String> linkList = new ArrayList<String>();
		String fileName = null;
		String scheduleId = null;
		int i = 0;
		boolean flag1 = false;
		Assert.assertTrue(selectDataExportSchedule(scheduleType, segmentScheduleName), "Error on Schedule page Logs");
		Pattern pattern = Pattern.compile("/(\\d{4,6})/");
		Matcher matcher = pattern.matcher(driver.getCurrentUrl());
		if (matcher.find()) {
			scheduleId = matcher.group(1);
		}
		utils.longWaitInSeconds(3);
		if (utils.getLocator("schedulePage.logTextLabel").getText().contains("Completed")) {
			logText = utils.getLocator("schedulePage.logTextLabel").getText();
			logFlag = schedulePageLogs(logText, pageLogsList);
			Assert.assertTrue(logFlag, "logs for Data export on schedule page is not present");
			logger.info("logs for Data export on schedule page is present");
			TestListeners.extentTest.get().pass("logs for Data export on schedule page is present");
		} else {
			selUtils.AddScreenshot("Data export");
			TestListeners.extentTest.get().fail("Data export file is not complete");
			Assert.fail("Data export file is not complete");
		}
		try {
			List<String> logList = utils.getexportedFileNameList(logText);
			for (String logL : logList) {
				fileName = logL;
				fileNameList.add(fileName);
			}
			String query = "Select schedule_data FROM schedule_tasks st WHERE schedule_id =" + scheduleId;
			String scheduleData = DBUtils.executeQueryAndGetColumnValue(env, query,
					"schedule_data");
			linkList = extractUrls(scheduleData);
			for (String link : linkList) {
				String fName = fileNameList.get(i);
				logger.info("URL--  " + link);
				driver.navigate().to(link);
//				Utilities.longWait(8000);  // not appropriate as it uses static wait , we need dynamic wait here
				String fileNamePart = (fName.substring(0, fName.length() - 4)).trim();
				boolean downloadStatus = waitForFileToBeDownloaded(fileNamePart, 100);
				Assert.assertTrue(downloadStatus, "File is not downloaded : " + fileNamePart);
				logger.info("File is downloaded successfully: " + fileNamePart);
				TestListeners.extentTest.get().pass("File is downloaded successfully: " + fileNamePart);
				logger.info("====File path ====");
				TestListeners.extentTest.get().info("====File path ====");
				logger.info(new File(fName).getAbsolutePath());
				TestListeners.extentTest.get().info(new File(fName).getAbsolutePath());
				String str = fName;
				String subStr1 = str.substring(20, str.length());
				String subStr2 = (subStr1.substring(0, subStr1.length() - 11)).trim();
				String subStr3 = subStr2.replace("_points_to_currency", "");
				List<String> fileMapToList = parentMap.get(subStr3);
				flag1 = verifyColumns(fName, fileMapToList);
				if (flag1 == true) {
					logger.info("CSV file column is matching with selected fields for " + subStr2);
					TestListeners.extentTest.get()
							.pass("CSV file column is matching with selected fields for " + subStr2);
				} else {
					logger.info("CSV file column is not matching with selected fields for " + subStr2);
					TestListeners.extentTest.get()
							.fail("CSV file column is not matching with selected fields for " + subStr2);
				}
				nameOfFiles.add(fName);
				i++;
			}
		} catch (Exception e) {
			logger.error("Error in running schedule " + e.getMessage());
			TestListeners.extentTest.get().fail("Error in running schedule " + e);
			Assert.fail("Error in running schedule " + e);
		}
		return nameOfFiles;
	}

	public boolean verifySchedulePresent(String schedule) {
		boolean flag = false;
		logger.info("Exporting Data");
		utils.waitTillPagePaceDone();
		utils.getLocator("schedulePage.clickOnScheduleList").isDisplayed();
		utils.getLocator("schedulePage.clickOnScheduleList").click();
		List<WebElement> ele = utils.getLocatorList("schedulePage.availableScheduleList");
		for (WebElement Wele : ele) {
			String actualValues = Wele.getText();
			if (actualValues.contains(schedule)) {
				flag = true;
			}
		}
		return flag;
	}

	public String segmentExportSchedule(String toggleOption, String segmentExportName, String frequency)
			throws ParseException {
		logger.info("Segment Exporting Data");
		utils.waitTillPagePaceDone();
		utils.getLocator("schedulePage.verifySegmentExportPage").isDisplayed();
		switch (toggleOption) {
		case "ON":
			pageObj.dashboardpage().checkUncheckToggle("Publish to Salesforce", toggleOption);
			break;

		case "OFF":
			pageObj.dashboardpage().checkUncheckToggle("Publish to Salesforce", toggleOption);
			utils.getLocator("schedulePage.segmentExportName").clear();
			utils.getLocator("schedulePage.segmentExportName").sendKeys(segmentExportName);
			logger.info("Segment Export Schedule Name : " + segmentExportName);
			TestListeners.extentTest.get().info("Segment Export Schedule Name : " + segmentExportName);
			break;
		case "N/A":
			utils.getLocator("schedulePage.segmentExportName").clear();
			utils.getLocator("schedulePage.segmentExportName").sendKeys(segmentExportName);
			logger.info("Segment Export Schedule Name : " + segmentExportName);
			TestListeners.extentTest.get().info("Segment Export Schedule Name : " + segmentExportName);
			break;
		}
		String successMsg = "";
		if (frequency.equalsIgnoreCase("Immediately")) {
			successMsg = scheduleNewEmailExport("");
		}
		return successMsg;
	}

	public void openSchedule(String scheduleType, String scheduleName) {
		logger.info("Exporting Data");
		utils.waitTillPagePaceDone();
		boolean resultFlag = false;
		utils.getLocator("schedulePage.scheduleTypeDropdown").isDisplayed();
		Select sel = new Select(utils.getLocator("schedulePage.scheduleTypeDropdown"));
		sel.selectByValue(scheduleType);
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.editScheduleLink").replace("temp", scheduleName)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.editScheduleLink").replace("temp", scheduleName))).click();
		utils.waitTillPagePaceDone();
		boolean flag = utils.getLocator("schedulePage.updateScheduleButton").isDisplayed();
		Assert.assertTrue(flag, "Schedule Page " + scheduleName + " is Not Displayed");
		logger.info("Schedule Page " + scheduleName + " is Displayed");
		TestListeners.extentTest.get().info("Schedule Page " + scheduleName + " is Displayed");

	}

	public void selectDeleteOrDeactivateOption(String scheduleName, String option) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("schedulePage.deleteOrDeactivate").replace("$option", option))).click();
		logger.info("Click on " + option + " for " + scheduleName);
		TestListeners.extentTest.get().pass("Click on " + option + " for " + scheduleName);
	}

	public boolean scheduleJobBackgroundColor(String jobName, String hexcode) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("schedulePage.scheduleJobBackgroundColor").replace("$jobName", jobName);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), jobName + " Background Color");
		String hexColorCode = pageObj.segmentsPage().getTextColour(driver.findElement(By.xpath(xpath)));
		boolean result = utils.textContains(hexcode, hexColorCode);
		return result;
	}

	public void selectDeleteOrDeactivateOptionDataExport(String scheduleName, String option) {
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		utils.getLocator("schedulePage.emailTextField").click();
		utils.getLocator("schedulePage.deleteDataExport").click();
		logger.info("Click on " + option + " for " + scheduleName);
		TestListeners.extentTest.get().pass("Click on " + option + " for " + scheduleName);
	}

	@SuppressWarnings("static-access")
	public String verifyExportScheduleForRemovedHeaderDataExport(String env, String scheduleType,
			String segmentScheduleName, String ftpPath) {
		String fileName = null;
		String scheduleId = null;
		try {
			selectDataExportSchedule(scheduleType, segmentScheduleName);
			Pattern pattern = Pattern.compile("/(\\d{4,6})/");
			Matcher matcher = pattern.matcher(driver.getCurrentUrl());
			if (matcher.find()) {
				scheduleId = matcher.group(1);
			}
			utils.waitTillPagePaceDone();
			utils.longWaitInSeconds(5);
			utils.waitTillVisibilityOfElement(utils.getLocator("schedulePage.logTextLabel"), "Logs");
			if (utils.getLocator("schedulePage.logTextLabel").getText().contains("Completed")) {
				String logText = utils.getLocator("schedulePage.logTextLabel").getText();
				fileName = utils.getexportedFileNameList(logText).get(0);
				String query = "Select schedule_data FROM schedule_tasks st WHERE schedule_id =" + scheduleId;
				String scheduleData = DBUtils.executeQueryAndGetColumnValue(env, query,
						"schedule_data");

				String link = null;
				Pattern p = Pattern.compile(regx, Pattern.CASE_INSENSITIVE);
				Matcher m = p.matcher(scheduleData);
				while (m.find()) {
					// links.add(m.group());
					link = m.group();
				}
				logger.info("URL" + link);
				driver.navigate().to(link);
				Utilities.longWait(8000);
				logger.info("====File path ====");
				TestListeners.extentTest.get().info("====File path ====");
				logger.info(new File(fileName).getAbsolutePath());
				TestListeners.extentTest.get().info(new File(fileName).getAbsolutePath());
				if (pageObj.dataExportPage().verifyFileExistsForRemovedHeaderDataExport(fileName)) {
					logger.info("Verified segment export file download: " + fileName);
					TestListeners.extentTest.get().pass("Verified segment export file download: " + fileName);
				} else {
					logger.error("Failed to verify segment export file download");
					TestListeners.extentTest.get().fail("Failed to verify segment export file download");
					Assert.fail("Failed to download file:" + fileName);
				}
			} else {
				TestListeners.extentTest.get().fail("Segment export file is not complete");
				Assert.fail("Segment export file is not complete");
			}
		} catch (Exception e) {
			selUtils.AddScreenshot("Segment export");
			logger.error("Error in running schedule " + e.getMessage());
			TestListeners.extentTest.get().fail("Error in running schedule " + e);
			Assert.fail("Error in running schedule " + e);
		}
		return fileName;
	}

	public boolean scheduleNewMultipleEmailExport() {
		boolean isErrorDisplayed = false;
		utils.getLocator("schedulePage.scheduleFrequencyDropdown").isDisplayed();
		Select sel = new Select(utils.getLocator("schedulePage.scheduleFrequencyDropdown"));
		sel.selectByValue(prop.getProperty("frequencyImmediate"));
		logger.info("Segment schedule frequency is set as: " + sel.getFirstSelectedOption().getText());
		TestListeners.extentTest.get()
				.info("Segment schedule frequency is set as: " + sel.getFirstSelectedOption().getText());
		utils.getLocator("schedulePage.emailTextField").isDisplayed();
		String baseEmail = "ashwini.shetty";
		for (int i = 0; i <= 20; i++) {
			String email = baseEmail + (i == 0 ? "" : "+" + i) + "@partech.com";
			utils.getLocator("schedulePage.emailTextField").sendKeys(email);
			utils.getLocator("schedulePage.emailTextField").sendKeys(Keys.RETURN);
		}

		utils.getLocator("schedulePage.ftpEndPointDropdown").isDisplayed();
		utils.getLocator("schedulePage.filePrefixTextbox").isDisplayed();
		utils.getLocator("schedulePage.filePrefixTextbox").sendKeys("AutoExport");

		utils.getLocator("schedulePage.saveScheduleButton").isDisplayed();
		utils.getLocator("schedulePage.saveScheduleButton").click();

		if (utils.getLocator("schedulePage.scheduleErrorMsgLabel").isDisplayed()) {
			String errorMessage = utils.getLocator("schedulePage.scheduleErrorMsgLabel").getText();
			logger.info("Error Message: " + errorMessage);
			TestListeners.extentTest.get().pass("Error Message: " + errorMessage);

			isErrorDisplayed = true;
		}

		return isErrorDisplayed;
	}

	// Method to wait for a file to be downloaded
	public boolean waitForFileToBeDownloaded(String expectedFileNamePart, int timeoutInSeconds) {
		File downloadDir = new File(System.getProperty("user.dir") + prop.getProperty("downloadPath"));
		int wait = 0;
		while (wait < timeoutInSeconds) {
			// An array to hold file objects
			File[] files = downloadDir
					.listFiles((dir, name) -> name.contains(expectedFileNamePart) && !name.endsWith(".crdownload"));
			if (files != null && files.length > 0) {
				return true;
			}
			utils.longWait(1000);
			wait++;
		}
		return false;
	}

	public void runSchedules() {
		utils.getLocator("schedulePage.runBtn").isDisplayed();
		utils.getLocator("schedulePage.runBtn").click();
		utils.acceptAlert(driver);
	}

	public boolean verifyDPScheduleLogs() {
		boolean logFlag = false;
		utils.getLocator("schedulePage.editSchedule").isDisplayed();
		utils.getLocator("schedulePage.editSchedule").click();
		utils.waitTillPagePaceDone();
		String logs_Xpath = utils.getLocatorValue("schedulePage.logTextLabel");

		for (int i = 0; i <= 25; i++) {
			utils.longWaitInSeconds(5);
			driver.navigate().refresh();
			utils.waitTillPagePaceDone();
			try {
				WebElement logsElement = driver.findElement(By.xpath(logs_Xpath));
				utils.waitTillVisibilityOfElement(logsElement, "Logs");
				String logText = logsElement.getText();
				if (logText.contains("Completed")) {
					logFlag = true;
					logger.info("Schedule ran successfully and 'Completed' log found.");
					TestListeners.extentTest.get().pass("Schedule ran successfully and 'Completed' log found.");
					break;
				}
			} catch (Exception e) {
				logger.info("Some Error occured while verifying logs" + e);
			}
		}
		return logFlag;
	}

	public void deleteSchedule() {
		utils.longWaitInSeconds(3);
		WebElement deleteBtn = utils.getLocator("schedulePage.deleteScheduleButton");
		deleteBtn.click();
		utils.acceptAlert(driver);
		logger.info("Schedule deleted successfully");
		TestListeners.extentTest.get().info("Schedule deleted successfully");
		utils.waitTillPagePaceDone();
	}

}
