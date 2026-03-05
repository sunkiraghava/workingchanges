package com.punchh.server.pages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

/*
 * @Author : Rakhi Rawat
 */

@Listeners(TestListeners.class)
public class CampaignPerformancePage {

	static Logger logger = LogManager.getLogger(CampaignPerformancePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	CreateDateTime createDateTime;

	public CampaignPerformancePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		createDateTime = new CreateDateTime();
	}

	public List<String> verifySplitTestCPPOptions() {
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsPage.Ellipsis"));
		utils.getLocator("campaignsPage.Ellipsis").click();
		List<WebElement> eleLst = utils.getLocatorList("campaignPerformancePage.cppEllipsis");
		List<String> optionList = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			optionList.add(text);
		}
		return optionList;
	}

	public String getMessageEngagementValue(String channel, String variant, String status) {
		String xpath = utils.getLocatorValue("campaignPerformancePage.messageEngagementValue")
				.replace("$channel", channel).replace("$variant", variant).replace("$temp", status);
		String text = driver.findElement(By.xpath(xpath)).getText();
		String value = text.replace(",", ""); //
		return value;
	}

	public void expandCampaignSummary() {
		utils.waitTillPagePaceDone();
		WebElement el = utils.getLocator("campaignPerformancePage.expandCampaignSummary");
		el.isDisplayed();
		el.click();
		logger.info("Expand Campaign Summary section");
		TestListeners.extentTest.get().info("Expand Campaign Summary section");
	}

	public List<String> getCampaignSummaryDetails() {
		List<String> campaignSummaryDetails = new ArrayList<String>();
		List<WebElement> campaignSummarySection = utils
				.getLocatorList("campaignPerformancePage.campaignSummarySection");
		int col = campaignSummarySection.size();
		for (int i = 0; i < col; i++) {
			String val = campaignSummarySection.get(i).getText();
			campaignSummaryDetails.add(val);
		}
		logger.info("Campaign Summary Details: " + campaignSummaryDetails);
		TestListeners.extentTest.get().info("Campaign Summary Details: " + campaignSummaryDetails);
		return campaignSummaryDetails;
	}

	public String getSegmentSizeFromCampaignSummary(String variant) {
		String segmentSize = "";
		String xpath = utils.getLocatorValue("campaignPerformancePage.segmentSize").replace("$temp", variant);
		String text = driver.findElement(By.xpath(xpath)).getText();
		if (text.contains("(") && text.contains(")")) {
			segmentSize = text.substring(text.indexOf('(') + 1, text.indexOf(')'));
		} else {
			segmentSize = text;
		}
		return segmentSize;
	}

	public String getMessageEngagementPercentage(String channel, String variant, String status) {
		String xpath = utils.getLocatorValue("campaignPerformancePage.messageEngagementValue")
				.replace("$channel", channel).replace("$variant", variant).replace("$temp", status);
		String text = driver.findElement(By.xpath(xpath)).getText();
		return text;
	}

	public List<String> getSplitCppKeyMetrics(String variant) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("campaignPerformancePage.cppSplitKeyMetricsLabel").replace("$variant",
				variant);
		List<WebElement> eleLst = driver.findElements(By.xpath(xpath));
		List<String> keyMetrics = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			keyMetrics.add(text);
		}
		return keyMetrics;
	}

	public String getCppKeyMetricsValues(String variant, String metrics, String choice) {
		String xpathKey;
		choice = choice.toUpperCase();
		switch (choice) {
		case "TOP":
			xpathKey = "campaignPerformancePage.cppKeyMetricsValuesTop";
			break;
		case "BOTTOM":
			xpathKey = "campaignPerformancePage.cppKeyMetricsValuesBottom";
			break;
		default:
			logger.info("Invalid choice for CPP Key Metrics: " + choice);
			throw new IllegalArgumentException("Invalid choice for CPP Key Metrics: " + choice);
		}
		String xpath = utils.getLocatorValue(xpathKey).replace("$variant", variant).replace("$metrics", metrics);
		String text = driver.findElement(By.xpath(xpath)).getText();
		String value = text.replaceAll("[^\\d.]", ""); //
		return value;
	}

	public List<String> getCampaignAudienceVsControlGroup() {
		utils.waitTillPagePaceDone();
		List<String> columnElelemts = new ArrayList<String>();
		List<WebElement> list = utils.getLocatorList("campaignPerformancePage.campaignAudienceControlGroupColumn");
		int col = list.size();
		for (int i = 0; i < col; i++) {
			String val = list.get(i).getText();
			if (!val.trim().isEmpty()) {
				columnElelemts.add(val);
			}
		}
		logger.info("Campaign Audienece Vs Control Group column are : " + columnElelemts);
		TestListeners.extentTest.get().info("Campaign Audienece Vs Control Group column are : " + columnElelemts);
		return columnElelemts;
	}

	public String getPopulationSizeAndPercentage(String columnName, String index, String choice) {
		String populationSize = "";
		String xpath = utils.getLocatorValue("campaignPerformancePage.populationSize").replace("$column", columnName)
				.replace("$index", index);
		String text = driver.findElement(By.xpath(xpath)).getText();
		switch (choice) {
		case "value":
			if (text.contains("(") && text.contains(")")) {
				populationSize = text.substring(0, text.indexOf('(')).replaceAll(",", "").trim();
				;
			} else {
				populationSize = text;
			}
			break;
		case "percentage":
			if (text.contains("(") && text.contains(")")) {
				populationSize = text.replaceAll(".*\\((\\d+)%\\).*", "$1");
			} else {
				populationSize = text;
			}
			break;
		default:
			logger.info("Invalid choice for CPP Key Metrics: " + choice);
			throw new IllegalArgumentException("Invalid choice for CPP Key Metrics: " + choice);
		}
		return populationSize;
	}

	public void openCampaignLinkFromAssociation() {
		utils.waitTillPagePaceDone();
		utils.getLocator("campaignPerformancePage.associationHeading").isDisplayed();
		utils.getLocator("campaignPerformancePage.campaignLink").click();
		logger.info("Clicked on Campaign link from Association section");
		TestListeners.extentTest.get().info("Clicked on Campaign link from Association section");
	}

	public boolean verifyInactiveTagVisibility() {
		boolean isVisible = false;
		utils.waitTillPagePaceDone();
		// WebElement inactiveTag =
		// utils.getLocator("campaignPerformancePage.inactiveTag");
		try {
			isVisible = utils.getLocator("campaignPerformancePage.inactiveTag").isDisplayed();
		} catch (Exception e) {
			logger.info("Inactive tag is not displayed on the Campaign Performance Page.");
			TestListeners.extentTest.get().info("Inactive tag is not displayed on the Campaign Performance Page.");
		}
		return isVisible;
	}

	public List<String> getNonSplitCppKeyMetrics() {
		utils.waitTillPagePaceDone();
		List<WebElement> eleLst = utils.getLocatorList("campaignPerformancePage.cppKeyMetricsLabel");
		List<String> keyMetrics = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			keyMetrics.add(text);
		}
		return keyMetrics;
	}

	public void navigateToCampaignUrl(String url) {
		driver.get(url);
		logger.info("Navigated to Campaign URL: " + url);
		TestListeners.extentTest.get().info("Navigated to Campaign URL: " + url);
	}

	public void clickAggregatePerformanceLink() {
		utils.waitTillPagePaceDone();
		utils.getLocator("campaignPerformancePage.aggregatePerformanceLink").click();
		utils.switchToWindow();
		logger.info("Clicked on Aggregate Performance link");
		TestListeners.extentTest.get().info("Clicked on Aggregate Performance link");
	}

	public String getLastAndNextRefreshDate(String nextOrLast) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue(("campaignPerformancePage.dataNextAndLastRefreshDates")).replace("$temp",
				nextOrLast);
		String text = driver.findElement(By.xpath(xpath)).getText();
		String result = text.replace("Data next refresh: ", "").replace("Data last refreshed: ", "");
		logger.info(nextOrLast + "date is : " + result);
		TestListeners.extentTest.get().info(nextOrLast + "date is : " + result);
		return result;
	}

	public boolean verifyAggregatePerformancePageMsg() {
		utils.waitTillPagePaceDone();
		boolean flag = false;
		try {
			flag = utils.getLocator("campaignPerformancePage.noResultHeading").isDisplayed();
		} catch (Exception e) {
			logger.info(
					"'No results found' is not displayed on Aggregate Performance Page when data is not available.");
			TestListeners.extentTest.get().info(
					"'No results found' is not displayed on Aggregate Performance Page when data is not available.");
		}
		return flag;
	}

	public void expandCampaignPerformanceSection() {
		utils.waitTillPagePaceDone();
		WebElement el = utils.getLocator("campaignPerformancePage.expandCampaignPerformance");
		el.isDisplayed();
		el.click();
		logger.info("Expanded Campaign Performance section");
		TestListeners.extentTest.get().info("Expanded Campaign Performance section");
	}

	public boolean verifyCampaignPerformanceSection() {
		try {
			WebElement el = utils.getLocator("campaignPerformancePage.campaignPerformanceTabsBlock");
			utils.waitTillVisibilityOfElement(el, "Campaign Performance Tabs");
			return el.isDisplayed();
		} catch (Exception e) {
			logger.info("Campaign Performance section is not loaded.");
			TestListeners.extentTest.get().info("Campaign Performance section is not loaded.");
			return false;
		}
	}

	public List<String> getAggregatePageKeyMetrics() {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("campaignPerformancePage.AggregateKeyMetrics");
		List<WebElement> eleLst = driver.findElements(By.xpath(xpath));
		List<String> keyMetrics = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			keyMetrics.add(text);
		}
		return keyMetrics;
	}

	public String verifyCampaignLogsOnNewCpp() {
		String logParam = utils.getLocator("campaignPerformancePage.campaignLogsNewCpp").getText();
		return logParam;

	}

	public boolean validateDateTimeFormat(String nextOrLast) {
		utils.waitTillPagePaceDone();
		utils.waitTillVisibilityOfElement(utils.getLocator("campaignPerformancePage.cppKeyMetricsLabel"), "");
		boolean isValid = false;
		// fetch dateTime from CPP
		String xpath = utils.getLocatorValue(("campaignPerformancePage.dataNextAndLastRefreshDates")).replace("$temp",
				nextOrLast);
		String text = driver.findElement(By.xpath(xpath)).getText();
		String result = text.replace("Data next refresh: ", "").replace("Data last refreshed: ", "");
		logger.info(nextOrLast + " date is : " + result);
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a z", Locale.ENGLISH);
		try {
			dateFormat.parse(result);
			isValid = true;
		} catch (Exception e) {
			logger.error("Date format is invalid for " + nextOrLast + ": " + e.getMessage());
			logger.error("Date format is invalid for " + nextOrLast + ": " + e.getMessage()
					+ ". Expected format: 'MMMM dd, yyyy hh:mm a z'");
			TestListeners.extentTest.get().info("Date format is invalid for " + nextOrLast + ": " + e.getMessage()
					+ ". Expected format: 'MMMM dd, yyyy hh:mm a z'");
		}
		return isValid;
	}

	public String getCampaignSummaryTitle() {
		utils.waitTillPagePaceDone();
		WebElement el = utils.getLocator("campaignPerformancePage.campaignSummaryTitle");
		el.isDisplayed();
		String title = el.getText();
		logger.info("Campaign Summary Title: " + title);
		TestListeners.extentTest.get().info("Campaign Summary Title: " + title);
		return title;
	}

	public void switchSplitVariantsTab(String title, String variant) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("campaignPerformancePage.splitVariantsTabSwitch").replace("$title", title)
				.replace("$variant", variant);
		WebElement el = driver.findElement(By.xpath(xpath));
		el.isDisplayed();
		el.click();
		logger.info("Switched to " + variant + " tab" + " under " + title);
		TestListeners.extentTest.get().info("Switched to " + variant + " tab" + " under " + title);
	}

	public List<String> getSplitCampaignPerformanceKeyMetrics() {
		utils.waitTillPagePaceDone();
		List<WebElement> eleLst = utils.getLocatorList("campaignPerformancePage.campaignPerformanceKeyMetricsLabel");
		List<String> keyMetrics = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			if (text != null && !text.trim().isEmpty()) {
				keyMetrics.add(text);
			}
		}
		return keyMetrics;
	}

	public List<String> getSplitTestControlGroupMetrics() {
		utils.waitTillPagePaceDone();
		List<WebElement> eleLst = utils.getLocatorList("campaignPerformancePage.controlGroupKeyMetrics");
		List<String> keyMetrics = new ArrayList<>();
		for (int i = 0; i < eleLst.size(); i++) {
			String text = eleLst.get(i).getText();
			if (text != null && !text.trim().isEmpty()) {
				keyMetrics.add(text);
			}
		}
		return keyMetrics;
	}

	public String getSplitTestControlGroupMetricsValue(String metrics) {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("campaignPerformancePage.controlGroupKeyMetricsValue").replace("$temp",
				metrics);
		WebElement el = driver.findElement(By.xpath(xpath));
		String value = el.getText();
		logger.info(metrics + " key metrics value is " + value);
		return value;
	}

	public String getNonSplitKeyMetricsValues(String metrics, String choice) {
		String xpathKey;
		choice = choice.toUpperCase();
		switch (choice) {
		case "TOP":
			xpathKey = "campaignPerformancePage.cppNonSplitKeyMetricsValuesTop";
			break;
		case "BOTTOM":
			xpathKey = "campaignPerformancePage.cppNonSplitKeyMetricsValuesBottom";
			break;
		default:
			logger.info("Invalid choice for CPP Key Metrics: " + choice);
			TestListeners.extentTest.get().info("Invalid choice for CPP Key Metrics: " + choice);	
			throw new IllegalArgumentException("Invalid choice for CPP Key Metrics: " + choice);
		}
		String xpath = utils.getLocatorValue(xpathKey).replace("$metrics", metrics);
		String text = driver.findElement(By.xpath(xpath)).getText();
		String value = text.replaceAll("[^\\d.]", "");
		return value;
	}
}
