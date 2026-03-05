package com.punchh.server.pages;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DashboardPage {

	static Logger logger = LogManager.getLogger(DashboardPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public DashboardPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void selectDataRange(String value) {
		utils.waitTillPagePaceDone();
		utils.getLocator("dashboardPage.dataRangeTextBox").click();
		logger.info("Clicked data range dropdown");
		List<WebElement> ele = utils.getLocatorList("dashboardPage.dateRangeList");
		utils.selectListDrpDwnValue(ele, value);
		utils.waitTillPagePaceDone();
		logger.info("selected date range " + value);
		TestListeners.extentTest.get().info("selected date range " + value);
	}

	public void selectLocation(String value) {
		// Existing : Not required logging for small check; so suggesting removing
		// "logger.info("Clicked
		// data range dropdown");
		List<WebElement> ele = utils.getLocatorList("dashboardPage.dateRangeList");
		utils.selectListDrpDwnValue(ele, value);
		// Existing : Suggesting removal of "utils.waitTillPagePaceDone()" ; Not
		// required as no page
		// pace for selecting dropDown. Also adding execution time.
		// Existing : Suggesting removal of "logger.info("selected date range " + value)
		// ; Test listener
		// already logging
		TestListeners.extentTest.get().info("selected date range " + value);
	}

	// This method enters a custom date range w/o using date range dropdown
	public void enterCustomDateRange(String date) {
		utils.waitTillPagePaceDone();
		utils.getLocator("dashboardPage.dataRangeTextBox").click();
		selUtils.clearTextUsingJS(utils.getLocator("dashboardPage.dataRangeTextBox"));
		selUtils.SendKeysViaJS(utils.getLocator("dashboardPage.dataRangeTextBox"), date);
		utils.getLocator("dashboardPage.dataRangeTextBox").sendKeys(Keys.ENTER);
		utils.waitTillPagePaceDone();
	}

	// This method selects a location by using search input field
	public void selectLocation(String partialName, String fullName) {
		utils.waitTillPagePaceDone();
		utils.waitTillElementToBeClickable(utils.getLocator("dashboardPage.locationDrp"));
		utils.getLocator("dashboardPage.locationDrp").click();
		utils.getLocator("dashboardPage.searchInputField").sendKeys(partialName);
		List<WebElement> ele = utils.getLocatorList("dashboardPage.locationDrpList");
		utils.waitTillVisibilityOfElement(ele.get(0), "Dropdown value");
		utils.selectListDrpDwnValue(ele, fullName);
		logger.info("Selected location as: " + fullName);
		TestListeners.extentTest.get().info("Selected location as: " + fullName);
	}

	public List<String> getTopStatsCards() {
		List<WebElement> ele = utils.getLocatorList("dashboardPage.topStatsList");
		List<String> StatsCardsname = ele.stream().map(s -> s.getText().replaceAll("[0-9 $%.]", "").trim()).distinct()
				.sorted().collect(Collectors.toList());
		return StatsCardsname;
	}

	public boolean signupsChart() {
		WebElement signupsChart = utils.getLocator("dashboardPage.signupsChart");
		return utils.checkElementPresent(signupsChart);
	}

	public boolean checkinSalesGraph() {
		WebElement checkinSalesGraph = utils.getLocator("dashboardPage.checkinSalesGraph");
		return utils.checkElementPresent(checkinSalesGraph);

	}

	public boolean loyaltyCheckinsGraph() {
		WebElement loyaltyCheckinsGraph = utils.getLocator("dashboardPage.loyaltyCheckinsGraph");
		return utils.checkElementPresent(loyaltyCheckinsGraph);

	}

	public boolean redemptionsSalesGraph() {
		// utils.waitTillPagePaceDone();
		WebElement redemptionsSalesGraph = utils.getLocator("dashboardPage.redemptionsSalesGraph");
		utils.waitTillInVisibilityOfElement(redemptionsSalesGraph, "redemptionsSalesGraph");
		return utils.checkElementPresent(redemptionsSalesGraph);

	}

	public boolean CampaignsTile() {
		WebElement CampaignsTile = utils.getLocator("dashboardPage.CampaignsTile");
		return utils.checkElementPresent(CampaignsTile);

	}

	public boolean appDownloadsGraph() {
		WebElement appDownloadsGraph = utils.getLocator("dashboardPage.appDownloadsGraph");
		return utils.checkElementPresent(appDownloadsGraph);

	}

	public boolean getDashBoardChartsHeaderstatus(String chartHeaderName) {
		boolean flag = false;

		// chart-header
		String headerXpath = utils.getLocatorValue("dashboardPage.dashBoardChartHeader").replace("$temp",
				chartHeaderName.trim());
		WebElement chartHeader = utils.getXpathWebElements(By.xpath(headerXpath));

		// Validate chart header is displayed
		if (chartHeader.isDisplayed()) {
			logger.info(chartHeaderName + " - chart header is displayed");
			TestListeners.extentTest.get().info(chartHeaderName + " - chart header is displayed");
			flag = true;
		}
		return flag;
	}

	public String getDashBoardChartsCount(String chartHeaderName, String specificStats) {
		// Get the Dashboard chart's count
		String countXpath = "";

		// Some stats have count using different xpath, so handling it
		if (specificStats.equals("Yes")) {
			countXpath = utils.getLocatorValue("dashboardPage.specificStatsValue").replace("$temp",
					chartHeaderName.trim());
		} else {
			countXpath = utils.getLocatorValue("dashboardPage.dashBoardChartcount").replace("$temp",
					chartHeaderName.trim());
		}
		WebElement chartCount = utils.getXpathWebElements(By.xpath(countXpath));
		String countFound = chartCount.getText().replaceAll("[^a-zA-Z0-9$%. ]", "");
		logger.info(chartHeaderName + " - chart count is displayed as: " + countFound);
		TestListeners.extentTest.get().info(chartHeaderName + " - chart count is displayed as: " + countFound);
		return countFound;
	}

	public boolean validateDashBoardChartsSVGAndClass(String chartHeaderName, String chartId, String countText,
			String checkSvg) {
		boolean flag = false;

		// Get the No data available message element
		String noDataXpath = utils.getLocatorValue("dashboardPage.noDataMsg").replace("$temp", chartId);
		WebElement noDataElement = null;

		// If count = '0', check no data message is displayed and don't check for svg
		if (countText.equals("0")) {
			noDataElement = utils.getXpathWebElements(By.xpath(noDataXpath));
			noDataElement.isDisplayed();
			flag = true;
			logger.info(chartHeaderName + " - no data message is displayed as: " + noDataElement.getText());
			TestListeners.extentTest.get()
					.info(chartHeaderName + " - no data message is displayed as: " + noDataElement.getText());
		}
		// If count = 'No data', then don't check for svg
		else if (countText.equalsIgnoreCase("No data")) {
			flag = true;
			logger.info(chartHeaderName + " - nothing is displayed (as expected)");
			TestListeners.extentTest.get().info(chartHeaderName + " - nothing is displayed (as expected)");
		}
		// If count is not present at all, then check for svg class only
		else if (countText.isEmpty()) {
			flag = isDashboardChartSvgAndClassDisplayed(chartHeaderName, chartId);
		}
		// If count is >0 and svg is not expected
		else if (Integer.parseInt(countText.replaceAll("[$%.]", "")) > 0 && checkSvg.equals("No")) {
			flag = true;
		}
		// Else check for svg class
		else {
			flag = isDashboardChartSvgAndClassDisplayed(chartHeaderName, chartId);
		}
		return flag;
	}

	// Verifies the dashboard stats where card content keeps on changing
	public boolean validateCarouselContents(String subHeader, String expectedString) {
		List<String> expectedHeaders = Arrays.asList(expectedString.split(","));
		List<String> foundHeaders = new ArrayList<>();

		String nextButtonXpath = utils.getLocatorValue("dashboardPage.carouselNextIndicator").replace("$temp",
				subHeader);
		WebElement nextButton = utils.getXpathWebElements(By.xpath(nextButtonXpath));

		// Looping until all expected headers are found
		while (foundHeaders.size() < expectedHeaders.size()) {
			// Check each expected header and adding unique headers to foundHeaders list
			expectedHeaders.stream()
					.filter(header -> !foundHeaders.contains(header) && getDashBoardChartsHeaderstatus(header))
					.forEach(header -> {
						foundHeaders.add(header);
						logger.info(header + " - sub header '" + subHeader + "' is displayed");
						TestListeners.extentTest.get().info(header + " - sub header '" + subHeader + "' is displayed");
						verifyCardBodyContainsText(header, subHeader);
					});
			// If all expected headers are found, then return true else click on next
			if (foundHeaders.size() == expectedHeaders.size()) {
				return true;
			}
			nextButton.click();
		}
		return false;
	}

	// Checks whether the dashboard chart's card body contains text
	public boolean verifyCardBodyContainsText(String header, String subHeader) {
		String carouselBodyXpath = utils.getLocatorValue("dashboardPage.carouselBodyContent").replace("$temp1", header)
				.replace("$temp2", subHeader);
		WebElement carouselBody = utils.getXpathWebElements(By.xpath(carouselBodyXpath));
		int attempts = 0;
		while (attempts < 2) {
			utils.waitTillVisibilityOfElement(carouselBody, "Carousel content");
			String dynamicCarouselText = carouselBody.getText();
			if (!dynamicCarouselText.isEmpty()) {
				logger.info("Card body contains the text");
				TestListeners.extentTest.get().info("Card body contains the text");
				return true;
			}
			attempts++;
			utils.longWaitInSeconds(2);
		}
		// If no text is found after all attempts, then fail the test
		Assert.fail("Can't find the text for header '" + header + "' and subheader '" + subHeader + "'");
		return false;
	}

	// Verifies whether system busy message is displayed or not
	public boolean isDashboardStatsNotLoadingMessageDisplayed(WebElement element, int maxRetries) throws Exception {
		boolean flag = false;
		int attempts = 0;
		String systemBusyBannerXpath = utils.getLocatorValue("dashboardPage.systemBusyMessageBanner");
		while (attempts < maxRetries) {
			try {
				String systemBusyBannerValue = utils.getXpathWebElements(By.xpath(systemBusyBannerXpath))
						.getAttribute("class");
				if (systemBusyBannerValue.equals("d-none")) {
					flag = false;
					logger.info("System busy message not found. Continuing with the script...");
					TestListeners.extentTest.get().info("System busy message not found. Continuing with the script...");
					break;
				} else {
					flag = true;
					logger.info("System busy message found in attempt: " + (attempts + 1) + " of " + maxRetries);
					TestListeners.extentTest.get()
							.info("System busy message found in attempt: " + (attempts + 1) + " of " + maxRetries);
				}
			} catch (Exception e) {
				logger.error("Error in checking system busy message: " + e);
				TestListeners.extentTest.get().info("Error in checking system busy message: " + e);
			}
			attempts++;
			logger.info("System busy message present after " + attempts + " attempts.");
			TestListeners.extentTest.get().info("System busy message present after " + attempts + " attempts.");
			// utils.refreshPage();
			// Navigate away from Dashboard landing page to Cockpit and coming back
			pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
			pageObj.menupage().clickDashboardMenu();
			utils.waitTillPagePaceDone();
			// utils.longWaitInSeconds(10);
		}
		// Skip test if System Busy Message didn't disappear after max attempts
		if (flag) {
			utils.logit("skip", "System busy message is still displayed. Hence, skipping the test.");
			throw new SkipException("System Busy Message didn't disappear after max attempts");
		}
		return flag;
	}

	/*
	 * Checks whether given sub header name is present below the dashboard graph
	 * (Ex: iOS under App Downloads)
	 */
	public boolean isSubHeaderBelowChartPresent(String chartId, String subHeader) {
		boolean flag = false;
		String subHeaderXpath = utils.getLocatorValue("dashboardPage.subHeaderBelowChart").replace("$temp1", chartId)
				.replace("$temp2", subHeader);
		WebElement subHeaderElement = utils.getXpathWebElements(By.xpath(subHeaderXpath));
		if (subHeaderElement.isDisplayed()) {
			logger.info("Sub header '" + subHeader + "' name below graph is displayed");
			TestListeners.extentTest.get().info("Sub header '" + subHeader + "' name below graph is displayed");
			flag = true;
		}
		return flag;
	}

	/*
	 * Verifies the dashboard chart's expected legends list matches with actual
	 * legends list
	 */
	public boolean isDashboardChartLegendsDisplayed(String chartId, String expectedString) {
		boolean flag = false;

		String legendXpath = utils.getLocatorValue("dashboardPage.chartLegendList").replace("$temp", chartId);
		List<WebElement> actualList = driver.findElements(By.xpath(legendXpath));
		List<String> expectedList = Arrays.asList(expectedString.split(","));

		int foundCount = 0;
		while (foundCount < expectedList.size()) {
			for (String expectedLegend : expectedList) {
				if (utils.valuePresentInList(actualList, expectedLegend)) {
					foundCount++;
				}
			}
			if (foundCount == expectedList.size()) {
				flag = true;
			}
		}
		return flag;
	}

	public boolean isDashboardChartSvgAndClassDisplayed(String chartHeaderName, String chartId) {
		boolean flag = false;
		// Get chart-svg element
		String svgXpath = utils.getLocatorValue("dashboardPage.chartSvg").replace("$temp", chartId.trim());
		WebElement chartSvg = utils.getXpathWebElements(By.xpath(svgXpath));

		// Validate chart's svg is displayed
		if (chartSvg.isDisplayed()) {
			logger.info(chartHeaderName + " - chart svg is displayed");
			TestListeners.extentTest.get().info(chartHeaderName + " - chart svg is displayed");
			flag = true;
		}

		// Validate c3-chart class is displayed
		String chartC3ClassXpath = utils.getLocatorValue("dashboardPage.chartClass").replace("$temp", chartId.trim());
		WebElement chartClass = utils.getXpathWebElements(By.xpath(chartC3ClassXpath));
		if (chartClass.isDisplayed()) {
			logger.info(chartHeaderName + " - c3-chart svg class is displayed");
			TestListeners.extentTest.get().info(chartHeaderName + " - c3-chart svg class is displayed");
			flag = true;
		}
		return flag;
	}

	public void verifyLoyaltyCheckinsandSalesgraphswithAlloption() {
		WebElement dropdownItem = utils.getLocator("dashboardPage.loyaltyCheckinSalesDrp");
		dropdownItem.click();
		logger.info("Clicked loyalty checkin and sales dropdown");
		List<WebElement> ele = utils.getLocatorList("dashboardPage.loyaltyCheckinSalesDrpList");
		List<String> options = ele.stream().map(s -> s.getText()).distinct().sorted().collect(Collectors.toList());
		dropdownItem.click();
		int size = options.size();
		try {
			for (int i = 0; i < size; i++) {

				String val = options.get(i);
				dropdownItem.click();
				utils.logit("Clicked Loyalty Checkins and Sales dropdown to select value: " + val);
				List<WebElement> elem = utils.getLocatorList("dashboardPage.loyaltyCheckinSalesDrpList");
				utils.selectListDrpDwnValue(elem, val);
				if (isDashboardChartSvgAndClassDisplayed("Loyalty Checkins and Sales", "dashboard-graph")) {
					TestListeners.extentTest.get()
							.pass("Verified Loyalty Check-ins and Sales graphs with option: " + val);
				}
			}

		} catch (Exception e) {
			logger.error("Error in verifying Loyalty Check-ins and Sales graphs with all option" + e);
			TestListeners.extentTest.get()
					.fail("Error in verifying Loyalty Check-ins and Sales graphs with all option" + e);

		}
	}

	public void verifyRedemptionsandSalesGraphWithAllOptions() {
		WebElement dropdownItem = utils.getLocator("dashboardPage.RedemptionSalesDrp");
		WebElement checkinText = utils.getLocator("dashboardPage.checkinsText");
		selUtils.scrollToElement(checkinText);
		dropdownItem.click();
		List<WebElement> ele = utils.getLocatorList("dashboardPage.RedemptionSalesDrpList");
		List<String> options = ele.stream().map(s -> s.getText()).distinct().sorted().collect(Collectors.toList());
		dropdownItem.click();
		int size = options.size();
		try {
			for (int i = 0; i < size; i++) {

				String val = options.get(i);
				dropdownItem.click();
				utils.logit("Clicked Redemptions & Sales dropdown to select value: " + val);
				List<WebElement> elem = utils.getLocatorList("dashboardPage.RedemptionSalesDrpList");
				utils.selectListDrpDwnValue(elem, val);
				if (isDashboardChartSvgAndClassDisplayed("Redemptions & Sales", "redemption-sales")) {
					TestListeners.extentTest.get().pass("Verified Redemptions & Sales graphs with option: " + val);
				}
			}

		} catch (Exception e) {
			logger.error("Error in verifying Redemptions & Sales graphs with all option" + e);
			TestListeners.extentTest.get().fail("Error in verifying Redemptions & Sales graphs with all option" + e);

		}
	}

	public void verifyBrowserLogs() {
		int counter = 0;
		List<String> Jserrors = new ArrayList<String>();
		LogEntries log = driver.manage().logs().get("browser");
		List<LogEntry> logs = log.getAll();
		for (LogEntry entry : logs) {
			if (entry.toString().contains("Failed to load resource: the server responded with a status of 400 ()")) {
				Jserrors.add(entry.toString());
				counter++;
			}
		}
		if (counter > 0) {
			TestListeners.extentTest.get()
					.info("JS errors found in browser console: " + counter + "errors: " + Jserrors);
			logger.warn("JS errors found in browser console: " + counter + "errors: " + Jserrors);
		}

	}

	public void clickPosConsoleBtn() {
		utils.getLocator("dashboardPage.posConsoleBtn").click();
		logger.info("Clicked pos console button");
	}

	public void searchUser(String email) {
		try {
			utils.getLocator("dashboardPage.location").click();
			utils.getLocator("dashboardPage.searchUsertextBox").clear();
			utils.getLocator("dashboardPage.searchUsertextBox").sendKeys(email);
			utils.getLocator("dashboardPage.searchUsertextBox").sendKeys(Keys.ENTER);
			utils.getLocator("dashboardPage.searchedUser").click();
			TestListeners.extentTest.get().pass("user searched in pos console");
			logger.info("User searched in pos console");
		} catch (Exception e) {
			logger.error("Error in searching user in pos console" + e);
			TestListeners.extentTest.get().fail("Error in searching user in pos console" + e);
		}

	}

	public void onEnableSTOCheckbox() {
		WebElement enableSTOCheckbox = driver.findElement(By.xpath("//input[@id='business_enable_sto']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableSTOCheckbox);
		String val = enableSTOCheckbox.getAttribute("checked");
		if (val == null) {
			// utils.getLocator("dashboardPage.enableSTO").click();
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
		} else {
			logger.info("Enable STO checkbox is already selected : " + val);
		}
	}

	public void offEnableSTOCheckbox() {
		WebElement enableSTOCheckbox = driver.findElement(By.xpath("//input[@id='business_enable_sto']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableSTOCheckbox);
		String val = enableSTOCheckbox.getAttribute("checked");
		if (val != null) {
			utils.getLocator("dashboardPage.enableSTO").click();
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable STO checkbox is checked : " + val);
			TestListeners.extentTest.get().info("Enable STO checkbox is checked : " + val);
		} else {
			logger.info("Enable STO checkbox is unchecked : " + val);
			TestListeners.extentTest.get().info("Enable STO checkbox is unchecked : " + val);
		}
	}

	public void onEnabledataExportBetabox() {
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		WebElement dataExportBetabox = driver
				.findElement(By.xpath(utils.getLocatorValue("dashboardPage.checkDataExportBeta")));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, dataExportBetabox);
		String val = dataExportBetabox.getAttribute("checked");
		if (val == null) {
			// utils.getLocator("dashboardPage.checkDataExportBeta").click();
			utils.getLocator("dashboardPage.enableDataExportBeta").isDisplayed();
			utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.enableDataExportBeta"));
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable Data Export Beta checkbox is checked sucessfully");
			TestListeners.extentTest.get().info("Enable Data Export Beta checkbox is checked sucessfully");
		} else {
			logger.info("Enable Data Export Beta checkbox is already selected : " + val);
			TestListeners.extentTest.get().info("Enable Data Export Beta checkbox is already selected : " + val);
		}
	}

	public void offEnabledataExportBetabox() {
		utils.getLocator("instanceDashboardPage.miscellaneous_config_tab").click();
		WebElement dataExportBetabox = driver
				.findElement(By.xpath(utils.getLocatorValue("dashboardPage.checkDataExportBeta")));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, dataExportBetabox);
		String val = dataExportBetabox.getAttribute("checked");
		if (val != null) {
			// utils.getLocator("dashboardPage.checkDataExportBeta").click();
			utils.clickByJSExecutor(driver, dataExportBetabox);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Disabled Data Export Beta checkbox");
			TestListeners.extentTest.get().info("Disabled Data Export Beta checkbox");
		} else {
			logger.info("already unchecked : " + val);
		}
	}

	public void offEnableBusinessPhoneUniqueness() {
		WebElement enableBusinessPhoneUniquenessCheckbox = driver
				.findElement(By.xpath("//input[@id='business_phone_uniqueness']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableBusinessPhoneUniquenessCheckbox);
		String val = enableBusinessPhoneUniquenessCheckbox.getAttribute("checked");
		if (val != null) {
			// utils.getLocator("dashboardPage.enableValidateUniquenessOfPhoneNumberAcrossGuests").click();
			utils.clickByJSExecutor(driver, enableBusinessPhoneUniquenessCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable Validate Uniqueness Of Phone Number Across Guests checkbox is checked : " + val);
			TestListeners.extentTest.get().info(
					"Enable enable Validate Uniqueness Of Phone Number Across Guests checkbox is checked : " + val);
		} else {
			logger.info("already unchecked: " + val);
		}
	}

	public void onEnableBusinessPhoneUniqueness() {
		WebElement enableBusinessPhoneUniquenessCheckbox = driver
				.findElement(By.xpath("//input[@id='business_phone_uniqueness']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableBusinessPhoneUniquenessCheckbox);
		String val = enableBusinessPhoneUniquenessCheckbox.getAttribute("checked");
		if (val == null) {
			// utils.getLocator("dashboardPage.enableValidateUniquenessOfPhoneNumberAcrossGuests").click();
			utils.clickByJSExecutor(driver, enableBusinessPhoneUniquenessCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
		} else {
			logger.info(
					"Enable Validate Uniqueness Of Phone Number Across Guests checkbox is already selected : " + val);
			TestListeners.extentTest.get().info(
					"Enable Validate Uniqueness Of Phone Number Across Guests checkbox is already selected : " + val);
		}
	}

	public void enableSMS() {
		utils.getLocator("dashboardPage.enableSMS").isDisplayed();
		System.out.println(utils.getLocator("dashboardPage.enableSMSChecked").getAttribute("checked"));
		if (utils.getLocator("dashboardPage.enableSMSChecked").getAttribute("checked") == null) {
			utils.getLocator("dashboardPage.enableSMS").click();
		}
		utils.getLocator("dashboardPage.updateBtn").click();
		logger.info("SMS is enabled");
		TestListeners.extentTest.get().info("SMS is Enabled");
	}

	public void smsAdapter(String adapter) throws InterruptedException {
		utils.getLocator("dashboardPage.smsAdapaterdrpdwn").click();
		utils.getLocator("dashboardPage.searchInputField").sendKeys(adapter);
		utils.getLocator("dashboardPage.searchInputField").sendKeys(Keys.ENTER);
		utils.getLocator("dashboardPage.cockpitCampaignUpdate").isDisplayed();
		utils.getLocator("dashboardPage.cockpitCampaignUpdate").click();
		Thread.sleep(5000);
		logger.info("SMS Adapter is updated");
		TestListeners.extentTest.get().info("SMS Adapter is updated");
	}

	public void integrationService() {
		utils.getLocator("dashboardPage.integrationServiceLink").click();
		utils.getLocator("dashboardPage.AttentiveTab").click();
		utils.getLocator("dashboardPage.editAccessToken").click();
		utils.getLocator("dashboardPage.enterAccessToken").sendKeys("invalid");
		logger.info("Attentive API Access Token entered");
		utils.getLocator("dashboardPage.updateAttentiveBtn").click();
		logger.info("Attentive API Access Token updated");
		TestListeners.extentTest.get().info("Attentive API Access Token updated");
	}

	public void integrationServiceLogs() {
		utils.getLocator("dashboardPage.logsServicetypeDrpdwm").click();
		// List<WebElement> elem =
		// utils.getLocatorList("dashboardPage.serviceTypeList");
		// utils.selectListDrpDwnValue(elem, logs);
		utils.getLocator("dashboardPage.serviceTypeList").isDisplayed();
		utils.getLocator("dashboardPage.smsTypeSelection").click();
		logger.info("Service type is selected");
	}

	public boolean failureSMSLogs(String phone1) throws InterruptedException {
		utils.getLocator("dashboardPage.enterPhone").sendKeys(phone1);
		boolean result = false;
		int attempts = 0;
		while (attempts < 20) {
			utils.getLocator("dashboardPage.searchPhone").click();
			Thread.sleep(10000);
			try {
				WebElement SMSLogs = utils.getLocator("dashboardPage.failureSMSLabel");
				if (SMSLogs.isDisplayed()) {
					result = true;
					logger.info("Failure SMS Logs Displayed");
					TestListeners.extentTest.get().pass("Failure SMS Logs Displayed");
					break;
				}
			} catch (Exception e) {
				logger.error("Failure SMS logs not displayed");
			}
			attempts++;
		}
		return result;
	}

	public void enableZipCode() {
		utils.getLocator("dashboardPage.enableZipCode").isDisplayed();
		System.out.println(utils.getLocator("dashboardPage.enableZipCodeChecked").getAttribute("checked"));
		if (utils.getLocator("dashboardPage.enableZipCodeChecked").getAttribute("checked") == null) {
			utils.getLocator("dashboardPage.enableZipCode").click();
		}
		utils.getLocator("dashboardPage.updateBtn").click();
		logger.info("SMS is enabled");
		TestListeners.extentTest.get().info("SMS is Enabled");
	}

	public void enableGuestMigrationMgmt() {
		utils.getLocator("dashboardPage.migrationTabLabel").isDisplayed();
		utils.getLocator("dashboardPage.migrationTabLabel").click();
		String val = utils.getLocator("dashboardPage.enableGuestMigrationMgmtCheckbox").getAttribute("checked");
		if (val == null) {
			// utils.getLocator("dashboardPage.checkDataExportBeta").click();
			utils.getLocator("dashboardPage.enableGuestMigrationMgmtCheckbox").isDisplayed();
			utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.enableGuestMigrationMgmtCheckbox"));
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable Data Export Beta checkbox is checked sucessfully");
			TestListeners.extentTest.get().info("Enable Data Export Beta checkbox is checked sucessfully");
		} else {
			logger.info("Enable Data Export Beta checkbox is already selected : " + val);
			TestListeners.extentTest.get().info("Enable Data Export Beta checkbox is already selected : " + val);
		}
	}

	public void checkUncheckFlagOnCockpitDasboard(String flagName, String checkBoxFlag) {
		String enableSTOCheckboxLoc = utils.getLocatorValue("dashboardPage.flagCheckedOrNot").replace("$flagName",
				flagName);
		WebElement enableSTOCheckbox = driver.findElement(By.xpath(enableSTOCheckboxLoc));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info(flagName + " " + " Unchecked and do not clicked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass(flagName + " " + " Unchecked and do not clicked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info(flagName + " " + " is unchecked and user want to check the checkbox");
			TestListeners.extentTest.get().pass(flagName + " " + " is unchecked and user want to check the checkbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();

			logger.info(flagName + " " + " is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass(flagName + " " + " is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info(flagName + " " + " is already checked and user want to check the checkbox, so do not click");
			TestListeners.extentTest.get()
					.pass(flagName + " " + " is already checked and user want to check the checkbox, so do not click");

		}

		utils.getLocator("dashboardPage.updateBtn").click();
		selUtils.longWait(5);

	}

	public void checkUncheckFlagCockpitDashboard(String flagName, String checkBoxFlag) {
		selUtils.longWait(3);
		WebElement enableSTOCheckbox = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}
	}

	public void checkEnableRichEmailEditorFlag() {
		selUtils.longWait(200);
		if ((!(utils.getLocator("dashboardPage.enableRichEmailEditor").isSelected()))) {
			utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.enableRichEmailEditor"));
			// utils.getLocator("dashboardPage.enableRichEmailEditor").click();
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable Rich Email Editor is checked");
			TestListeners.extentTest.get().info("Enable Rich Email Editor is checked");
		} else {
			logger.info("Enable Rich Email Editor was already checked");
			TestListeners.extentTest.get().info("Enable Rich Email Editor was already checked");
		}
	}

	public void checkEnableFranchiseesFlag() {
		selUtils.longWait(200);
		if ((!(utils.getLocator("dashboardPage.enableFranchisees").isSelected()))) {
			utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.enableFranchisees"));
			// utils.getLocator("dashboardPage.enableFranchisees").click();
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable Franchisees is checked");
			TestListeners.extentTest.get().info("Enable Franchisees is checked");
		} else {
			logger.info("Enable Franchisees was already checked");
			TestListeners.extentTest.get().info("Enable Franchisees was already checked");
		}
	}

	public void navigateToTabs(String tabName) {
		try {
			utils.implicitWait(2);
			String tabXpath = utils.getLocatorValue("dashboardPage.dashboardTab_Xpath").replace("$TabName",
					tabName.trim());
			WebElement pageTabName = utils.getXpathWebElements(By.xpath(tabXpath));
			selUtils.waitTillElementToBeClickable(pageTabName);
			pageTabName.click();

		} catch (Exception e) {
			utils.getLocator("dashboardPage.dashboardToggleIcon").click();
			// driver.findElement(By.xpath("//a[@class='nav-link
			// dropdown-toggle']/span/i")).click();
			driver.findElement(By.xpath("//ul[@class='dropdown-menu show']/li/a[text()='" + tabName + "']")).click();

		}
		utils.implicitWait(50);
		utils.waitTillCompletePageLoad();
	}

	public void checkUncheckFlagOnCockpitDasboardOLOPage(String flagName, String checkBoxFlag) {
		selUtils.longWait(3);
		WebElement enableSTOCheckbox = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, enableSTOCheckbox);

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}
		selUtils.longWait(5);

	}

	public void checkUncheckAnyFlag(String flagName, String checkBoxFlagValue) {

		WebElement checkBoxFlag = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxFlagCurrentStatus = checkBoxFlag.getAttribute("checked");

		switch (checkBoxFlagValue) {
		case "check":
			if (checkBoxFlagCurrentStatus == null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				utils.getLocator("dashboardPage.updateBtn").click();
				logger.info(flagName + " checked successfully");
				TestListeners.extentTest.get().info(flagName + " checked successfully");
			} else {
				logger.info(flagName + " is already checked...");
				TestListeners.extentTest.get().info(flagName + " is already checked...");
			}

			break;
		case "uncheck":
			if (checkBoxFlagCurrentStatus != null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				utils.getLocator("dashboardPage.updateBtn").click();
				logger.info(flagName + " unchecked successfully");
				TestListeners.extentTest.get().info(flagName + " unchecked successfully");
			} else {
				logger.info(flagName + " is already unchecked...");
				TestListeners.extentTest.get().info(flagName + " is already unchecked...");
			}

			break;
		}
	}

	public void checkUncheckAnyFlagWitoutUpdate(String flagName, String checkBoxFlagValue) {

		WebElement checkBoxFlag = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxFlagCurrentStatus = checkBoxFlag.getAttribute("checked");

		switch (checkBoxFlagValue) {
		case "check":
			if (checkBoxFlagCurrentStatus == null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				logger.info(flagName + " checked successfully");
				TestListeners.extentTest.get().info(flagName + " checked successfully");
			} else {
				logger.info(flagName + " is already checked...");
				TestListeners.extentTest.get().info(flagName + " is already checked...");
			}

			break;
		case "uncheck":
			if (checkBoxFlagCurrentStatus != null) {
				utils.clickByJSExecutor(driver, checkBoxFlag);
				logger.info(flagName + " unchecked successfully");
				TestListeners.extentTest.get().info(flagName + " unchecked successfully");
			} else {
				logger.info(flagName + " is already unchecked...");
				TestListeners.extentTest.get().info(flagName + " is already unchecked...");
			}

			break;
		}
	}

	public String checkBoxFlagOnOffAndClick(String FladId, String checkBoxFlag) {

		// explicitly call below updateCheckBox() for check/uncheck the flag

		utils.implicitWait(30);
		String xpath = utils.getLocatorValue("dashboardPage.checkBoxFlagOnOff").replace("$flag", FladId);
		WebElement checkBoxWebElement = driver.findElement(By.xpath(xpath));
		String checkBoxValue = driver.findElement(By.xpath(xpath)).getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, checkBoxWebElement);
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.StaleElementclick(driver, checkBoxWebElement);
//			utils.clickByJSExecutor(driver, checkBoxWebElement);
			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
		}
		return checkBoxValue;
	}

	public void checkBoxFlagOnOffAndUpdate(String FladId, String checkBoxFlag, boolean update) {

		utils.implicitWait(30);
		String xpath = utils.getLocatorValue("dashboardPage.checkBoxFlagOnOff").replace("$flag", FladId);
		WebElement checkBoxWebElement = driver.findElement(By.xpath(xpath));
		String checkBoxStatus = driver.findElement(By.xpath(xpath)).getAttribute("checked");

		// check if not checked
		if ((checkBoxStatus == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, checkBoxWebElement);
			logger.info("chekedbox " + FladId + " checked/enabled");
			TestListeners.extentTest.get().pass("chekedbox " + FladId + " checked/enabled");

		} else if ((checkBoxStatus != null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			logger.info("chekedbox " + FladId + " is already checked/enabled");
			TestListeners.extentTest.get().pass("chekedbox " + FladId + " checked/enabled");
		} // uncheck if not unchecked
		else if ((checkBoxStatus != null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, checkBoxWebElement);
			logger.info("chekedbox " + FladId + " unchecked/disabled");
			TestListeners.extentTest.get().pass("chekedbox " + FladId + " unchecked/disabled");

		} else if ((checkBoxStatus == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("chekedbox " + FladId + " already unchecked/disabled");
			TestListeners.extentTest.get().pass("chekedbox " + FladId + " unchecked/disabled");
		}

		if (update) {
			WebElement updateBtn = utils.getLocator("dashboardPage.updateBtn");
			utils.StaleElementclick(driver, updateBtn);
		}

	}

	public void clickOnUpdateOloButton() {
		utils.getLocator("dashboardPage.OLOUpdateButton_Xpath").click();

	}

	public void setPaymentAdapterInSubscriptionPage(String paymentMode) {
		utils.waitTillPagePaceDone();
		WebElement paymentAdapterDropdown = utils.getLocator("dashboardPage.subscriptionAdapter_Xpath");
		utils.selectDrpDwnValue(paymentAdapterDropdown, paymentMode);
		utils.longWaitInSeconds(2);
	}

	public void clickOnUpdateButton() {
		utils.getLocator("dashboardPage.updateBtn").click();
		utils.waitTillPagePaceDone();
	}

	public void clickOnSaveButton() {
		utils.getLocator("dashboardPage.saveButton").click();
		utils.waitTillPagePaceDone();
	}

	public String updateCheckBox() {
		utils.scrollToElement(driver, utils.getLocator("dashboardPage.updateBtn"));
		utils.getLocator("dashboardPage.updateBtn").click();
		logger.info("check box is updated");
		TestListeners.extentTest.get().pass("check box is updated");
		String msg = utils.getSuccessMessage();
		return msg;
	}

	public void clickPunchIcon() {
		utils.getLocator("dashboardPage.punchIcon").click();
		logger.info("Clicked pucnhh icon");
	}

	public boolean verifySelectBusinessOption() {
		WebElement ele = utils.getLocator("dashboardPage.searchBusiness");
		return utils.checkElementPresent(ele);
	}

	public boolean verifyFlagIsAvailableOrNot(String flagName) {
		boolean flag = false;
		try {
			WebElement enableSTOCheckbox = driver
					.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));

			enableSTOCheckbox.isDisplayed();
			flag = true;
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public void navigateToAllBusinessPage() throws InterruptedException {
		utils.getLocator("dashboardPage.businessDropIcon").click();
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.allBusinessLink"));
	}

	public void selectServiceOptionInWhitelabel(String tabName) {
		List<WebElement> ele = utils.getLocatorList("dashboardPage.selectServiceFromList");
		utils.selectListDrpDwnValue(ele, tabName);
		logger.info("Selected service in the whitelabel is " + tabName);
		TestListeners.extentTest.get().info("Selected service in the whitelabel is " + tabName);
	}

	public void enterProcessOrderClosedEventSlug(String slug) {
		utils.getLocator("dashboardPage.processOrderClosedEventSlug").clear();
		utils.getLocator("dashboardPage.processOrderClosedEventSlug").sendKeys(slug);
	}

	// Enters Last N days to tag old receipts field
	public void setLastNDaysToTagOldReceipts(String value) {
		utils.getLocator("dashboardPage.lastNDaysToTagOldReceipts").clear();
		utils.getLocator("dashboardPage.lastNDaysToTagOldReceipts").sendKeys(value);
	}

	public void updateWhitelabelOLO() {
		utils.getLocator("dashboardPage.updateWhitelabelOLO").click();
		logger.info("check box of OLO is updated");
		TestListeners.extentTest.get().pass("check box of OLO is updated");
	}

	public void editProcessableSubscriptionRedemptionCodesPerGuest(String str) {
		utils.getLocator("dashboardPage.processableSubscriptionRedemptionCodesPerGuest").clear();
		utils.getLocator("dashboardPage.processableSubscriptionRedemptionCodesPerGuest").sendKeys(str);
		clickOnUpdateButton();
	}

	public void checkUncheckToggle(String flagName, String toBeOnOff) {
		selUtils.longWait(3);

		String xpath = utils.getLocatorValue("dashboardPage.checkUncheckToggle").replace("$flagName", flagName);
		WebElement radioFlagWele = driver.findElement(By.xpath(xpath));

		String checkBoxValue = radioFlagWele.getAttribute("checked");

		if ((checkBoxValue == null) && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + toBeOnOff);
		} else if ((checkBoxValue == null) && (toBeOnOff.equalsIgnoreCase("ON"))) {
			utils.clickByJSExecutor(driver, radioFlagWele);
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (toBeOnOff.equalsIgnoreCase("OFF"))) {
			utils.clickByJSExecutor(driver, radioFlagWele);

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (toBeOnOff.equalsIgnoreCase("ON"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}
		selUtils.longWait(5);

	}

	public void clickOnResetDeleteBusinessButton() throws InterruptedException {
		utils.getLocator("dashboardPage.resetDeleteBusinessButton").click();
		selUtils.longWait(3000);
		utils.waitTillPagePaceDone();
		logger.info("Clicked on Reset/Delete Button On Dashboard page");
		TestListeners.extentTest.get().pass("Clicked on Reset/Delete Button On Dashboard page");
	}

	public void enterSlugNameAndClickOnResetButton(String slugName) {
		WebElement inputBoxSlugName = utils.getLocator("dashboardPage.slugNameInputBox_resetBusiness");
		inputBoxSlugName.click();
		inputBoxSlugName.clear();
		inputBoxSlugName.sendKeys(slugName);
		logger.info(slugName + " slug name is entered ");
		TestListeners.extentTest.get().pass(slugName + " slug name is entered ");

		utils.getLocator("dashboardPage.resetBusinessButton").click();
		utils.waitTillPagePaceDone();
		logger.info("Clicked on Reset Button On Dashboard page");
		TestListeners.extentTest.get().info("Clicked on Reset Button On Dashboard page");
	}

	public void locationScoreboards(String connection) {
		utils.getLocator("dashboardPage.clickOnLocationScoreboards").click();
		List<WebElement> ele = utils.getLocatorList("dashboardPage.selectLocationScoreboards");
		utils.selectListDrpDwnValue(ele, connection);
		logger.info("Selected Location Scoreboards service in the Stats Configuration is " + connection);
		TestListeners.extentTest.get()
				.info("Selected Location Scoreboards service in the Stats Configuration is " + connection);
	}

	public void dashboardsScoreboards(String connection) {
		utils.getLocator("dashboardPage.clickOnDashboardsScoreboards").click();
		List<WebElement> ele = utils.getLocatorList("dashboardPage.selectDashboardsScoreboards");
		utils.selectListDrpDwnValue(ele, connection);
		logger.info("Selected Dashboards service in the Stats Configuration is " + connection);
		TestListeners.extentTest.get().info("Selected Dashboards service in the Stats Configuration is " + connection);
	}

	public void dashboardsGraph(String connection) {
		utils.getLocator("dashboardPage.clickOnDashboardsGraphsScoreboards").click();
		List<WebElement> ele = utils.getLocatorList("dashboardPage.selectDashboardsGraphsScoreboards");
		utils.selectListDrpDwnValue(ele, connection);
		logger.info("Selected Dashboards Graph in the Stats Configuration is " + connection);
		TestListeners.extentTest.get()
				.info("Selected Dashboards Graph service in the Stats Configuration is " + connection);
	}

	public String dashboardStatsRedemptionsAndSales() {
		utils.waitTillPagePaceDone();
		boolean flag = utils.getLocator("dashboardPage.dashboardStatsRedemptionsAndSales").isDisplayed();
		Assert.assertTrue(flag, "Redemptions And Sales is not visible on Dashboard Stats");
		utils.getLocator("dashboardPage.clickOnRedemptionOnDashboardStats").click();
		utils.waitTillPagePaceDone();
		String heading = utils.getLocator("dashboardPage.redemptionReportHeading").getText();
		logger.info(
				"Report opens after clicking on Redemption under Redemptions And Sales section on dashboard stats page is "
						+ heading);
		TestListeners.extentTest.get().info(
				"Report opens after clicking on Redemption under Redemptions And Sales section on dashboard stats page is "
						+ heading);
		return heading;
	}

	public void StatsConfigToDatabrickes(String connection) {

		List<WebElement> selectWebEleList = driver.findElements(By.tagName("select"));
		for (WebElement eleW : selectWebEleList) {
			Select sel = new Select(eleW);
			sel.selectByVisibleText(connection);
		}
		logger.info("Stats Configuration is configured to Databricks");
	}

	public void verifyPunchhReportStatsConfig(String connection) {
		String config = utils.getLocator("dashboardPage.checkPunchhReportInStats").getText();
		Assert.assertEquals(config, connection,
				"Punchh Report connection is NOT set to DATABRICKS in Stats configuration");
		logger.info("Punchh Report connection is set to DATABRICKS in Stats configuration ");
		TestListeners.extentTest.get().pass("Punchh Report connection is set to DATABRICKS in Stats configuration ");
	}

	public boolean brokenLinksUi() throws IOException {
		boolean flag1 = false;
		boolean flag = true;
		int counter = 0;
		List<WebElement> allLink = utils.getLocatorList("dashboardPage.brokenUrlOfDashboard");
		logger.info("Total links are " + allLink.size());
		TestListeners.extentTest.get().info("Total links are " + allLink.size());
		for (int i = 0; i < allLink.size(); i++) {
			WebElement ele = allLink.get(i);
			String url = ele.getAttribute("href");
			flag1 = verifyDashboardActiveLink(url);
			if (flag1 == false) {
				counter++;
			}
		}

		if (counter != 0) {
			flag = false;
		}
		return flag;
	}

	public boolean verifyDashboardActiveLink(String linkurl) throws IOException {
		boolean flag = false;
		URL url = new URL(linkurl);
		HttpURLConnection httpUrlConnect = (HttpURLConnection) url.openConnection();
		httpUrlConnect.setConnectTimeout(3000);
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
			logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - " + HttpURLConnection.HTTP_NOT_FOUND
					+ "-" + httpUrlConnect.getResponseCode());
			TestListeners.extentTest.get().fail(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - "
					+ HttpURLConnection.HTTP_NOT_FOUND + "-" + httpUrlConnect.getResponseCode());
		}

		return flag;
	}

	public boolean flagPresentorNot(String flagName) {
		utils.implicitWait(20);
		boolean flag = true;
		String xpath = utils.getLocatorValue("dashboardPage.flagPresentOrNot").replace("$flagName", flagName);
		if (driver.findElements(By.xpath(xpath)).size() == 0) {
			flag = false;
		}
		utils.implicitWait(50);
		return flag;
	}

	public void clickOnUpdateGlobalConfigButton() {
		utils.getLocator("dashboardPage.updateGlobalConfig").click();
		logger.info("Clicked on Update Global Configuration Button");
		TestListeners.extentTest.get().info("Clicked on Update Global Configuration Button");
		utils.waitTillPagePaceDone();

	}

	// This method will return the selected value(s) from the dropdown
	public Object getSelectedValueFromDropdown(String dropdownName) {
		String xpath = utils.getLocatorValue("dashboardPage.labelDropnDownXpath").replace("${labelName}", dropdownName);
		WebElement dropdownElement = driver.findElement(By.xpath(xpath));
		utils.scrollToElement(driver, dropdownElement);
		Select select = new Select(dropdownElement);

		if (select.isMultiple()) {
			List<WebElement> selectedOptions = select.getAllSelectedOptions();
			List<String> selectedValues = selectedOptions.stream().map(WebElement::getText)
					.collect(Collectors.toList());
			logger.info(dropdownName + " is a multi-select dropdown with selected values: " + selectedValues);
			TestListeners.extentTest.get()
					.info(dropdownName + " is a multi-select dropdown with selected values: " + selectedValues);
			return selectedValues;
		} else {
			String selectedValue = "";
			try {
				utils.implicitWait(2);
				selectedValue = select.getFirstSelectedOption().getText();
				logger.info(dropdownName + " is a single-select dropdown with selected value: " + selectedValue);
				TestListeners.extentTest.get()
						.info(dropdownName + " is a single-select dropdown with selected value: " + selectedValue);
			} catch (NoSuchElementException e) {
				logger.info("No options are selected in the dropdown.");
				TestListeners.extentTest.get().info("No options are selected in the dropdown.");
			}
			utils.implicitWait(50);
			return selectedValue;
		}
	}

	public String checkCockpitFlagDescription(String flagName) {
		String descrXpath = utils.getLocatorValue("dashboardPage.cockpitFlagDescription").replace("$flagName",
				flagName);
		String desc = driver.findElement(By.xpath(descrXpath)).getText();
		return desc;
	}

	public void logoutApp() throws InterruptedException {
		utils.getLocator("dashboardPage.imgLogo").click();
		utils.getLocator("dashboardPage.logoutLink").click();
		utils.waitTillPagePaceDone();
		logger.info("logged out successfully");
		TestListeners.extentTest.get().info("logged out successfully");
	}

	public boolean verifyUserIsLoggedIn() {
		try {
			WebElement logoutLink = utils.getLocator("dashboardPage.imgLogo");
			return logoutLink.isDisplayed();
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public void businessHealthTabDropdown(String option) {

		utils.getLocator("dashboardPage.businessHealthDrpdwn").click();
		String xpath = utils.getLocatorValue("dashboardPage.businessHealthDrpdwnOptions").replace("$color", option);
		WebElement el = driver.findElement(By.xpath(xpath));
		el.click();
		utils.getLocator("dashboardPage.updateBtn").click();
		logger.info("color set from the Business Health dropdown : " + option);
		TestListeners.extentTest.get().info("color set from the Business Health dropdown : " + option);

	}

	public void navigateToTabsNew(String tabName) throws InterruptedException {

		utils.waitTillPagePaceDone();
		String tabXpath = utils.getLocatorValue("dashboardPage.dashboardTab_Xpath").replace("$TabName", tabName);
		driver.findElements(By.xpath(tabXpath)).get(0).click();

		selUtils.longWait(200);
	}

	public void createFeaturesRollouts(String businessFlagName, String datatypeOfFlagName, String value,
			String BusinessesToBeSelect) throws InterruptedException {

		utils.waitTillPagePaceDone();
		utils.getLocator("dashboardPage.featureRolloutsFlagNameTextBox").clear();
		utils.getLocator("dashboardPage.featureRolloutsFlagNameTextBox").sendKeys(businessFlagName);
		utils.selectDrpDwnValue(utils.getLocator("dashboardPage.featureRolloutsDatatypeDropDown"), datatypeOfFlagName);

		if ((datatypeOfFlagName.equalsIgnoreCase("Integer")) || (datatypeOfFlagName.equalsIgnoreCase("String"))) {

			String valueTextBoxXpath = utils.getLocatorValue("dashboardPage.featureRolloutsSetValueTextBox")
					.replace("$dataType", datatypeOfFlagName.toLowerCase());

			driver.findElement(By.xpath(valueTextBoxXpath)).clear();
			driver.findElement(By.xpath(valueTextBoxXpath)).sendKeys(value);
		} else {

			String valueDropdownXpath = utils.getLocatorValue("dashboardPage.featureRolloutsSetSelectDropDown")
					.replace("$dataType", datatypeOfFlagName.toLowerCase());
			utils.selectDrpDwnValue(driver.findElement(By.xpath(valueDropdownXpath)), value);
		}

		utils.selectDrpDwnValue(utils.getLocator("dashboardPage.listAllBusinessesDropDown"), BusinessesToBeSelect);

		utils.getLocator("dashboardPage.saveButtonFeatureRollouts").click();
		utils.longwait(2000);

	}

	public void createFeaturesRolloutsForTypeBoolean(String businessFlagName, String datatypeOfFlagName, String value,
			String BusinessesToBeSelect) throws InterruptedException {

		utils.waitTillPagePaceDone();
		utils.getLocator("dashboardPage.featureRolloutsFlagNameTextBox").clear();
		utils.getLocator("dashboardPage.featureRolloutsFlagNameTextBox").sendKeys(businessFlagName);
		utils.selectDrpDwnValue(utils.getLocator("dashboardPage.featureRolloutsDatatypeDropDown"), datatypeOfFlagName);

		utils.selectDrpDwnValue(utils.getLocator("dashboardPage.featureRolloutsSetValueTextBoxTypeBoolean"), "False");

		utils.selectDrpDwnValue(utils.getLocator("dashboardPage.listAllBusinessesDropDown"), BusinessesToBeSelect);

		utils.getLocator("dashboardPage.saveButtonFeatureRollouts").click();
		utils.longwait(2000);

	}

	public boolean buisnessHealthDrpDwnList(String text) {
		boolean flag = false;
		String xpath = utils.getLocatorValue("featureRolloutsPage.businessHealthDrpdwn");
		List<WebElement> weleList = driver.findElements(By.xpath(xpath));
		for (int i = 0; i < weleList.size(); i++) {
			String option = weleList.get(i).getText();
			if (option.contains(text)) {
				flag = true;
			}
		}
		return flag;
	}

	public String getAdminTimezone() throws InterruptedException {

		utils.waitTillElementToBeClickable(utils.getLocator("dashboardPage.imgLogo"));
		utils.StaleElementclick(driver, utils.getLocator("dashboardPage.imgLogo"));
		Thread.sleep(2000);
		utils.StaleElementclick(driver, utils.getLocator("dashboardPage.editProfileLink"));
		utils.getLocator("dashboardPage.adminTimezone").isDisplayed();
		logger.info("Admin timezone is visible");
		TestListeners.extentTest.get().info("Admin timezone is visible");
		String text = utils.getLocator("dashboardPage.adminTimezone").getText();
		String timezone = text.substring(2).trim();
		return timezone;

	}

	public void onEnableSavedPaymentCheckbox() {
		WebElement enableCheckbox = utils.getLocator("dashboardPage.enableCheckbox");
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableCheckbox);
		String val = enableCheckbox.getAttribute("checked");
		if (val == null) {
			// utils.getLocator("dashboardPage.enableSTO").click();
			utils.clickByJSExecutor(driver, enableCheckbox);
			utils.getLocator("dashboardPage.updateBtn").click();
		} else {
			logger.info("Enable saved payment checkbox is already selected : " + val);
		}
	}

	public void selectLocationFromConsolePage(String location) {
		String xpath = utils.getLocatorValue("dashboardPage.posConsoleLocations").replace("$temp", location);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		logger.info("Location is visible on pos console page :" + location);
		TestListeners.extentTest.get().info("Location is visible on pos console page :" + location);
		driver.findElement(By.xpath(xpath)).click();
	}

	public String attemptRedemptionFromPosConsole(String redemption) {
		utils.getLocator("dashboardPage.searchRedemption").isDisplayed();
		utils.getLocator("dashboardPage.searchRedemption").sendKeys(redemption);
		utils.getLocator("dashboardPage.searchRedemptionGoBtn").click();
		utils.getLocator("dashboardPage.honorButton").click();
		utils.longWaitInSeconds(2);
		String text = utils.getLocator("dashboardPage.MarkAsHonoredMsg").getText();
		return text;
	}

	public void selectPermissionLevel(String Option) {
		utils.getLocator("dashboardPage.clickPermissionLevel").click();
		logger.info("Permission level is clicked");
		TestListeners.extentTest.get().info("Permission level is clicked");
		List<WebElement> ele = utils.getLocatorList("dashboardPage.permissionLevelDrpDown");
		utils.selectListDrpDwnValue(ele, Option);
		logger.info("Selected Permission Level as " + Option);
		TestListeners.extentTest.get().pass("Selected Permission Level as " + Option);
	}

	public void dpTablue() {
		utils.waitTillPagePaceDone();
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe[@title='Data Visualization']")));
		int attempts = 0;
		boolean flag = false;
		while (attempts < 7) {
			try {
				utils.longWaitInSeconds(10);
				if (utils.getLocator("dashboardPage.locationScoreboardTablueVisible").isDisplayed()) {
					logger.info("Location Scoreboard Tablue Report is ready for view");
					TestListeners.extentTest.get().info("Location Scoreboard Tablue Report is ready for view");
					return;
				}

			} catch (Exception e) {
				logger.info("Location Scoreboard Tablue Report is not ready for view");
				TestListeners.extentTest.get().info("Location Scoreboard Tablue Report is not ready for view");
			}
			attempts++;
		}
		Assert.assertTrue(flag, "Location Scoreboard Tablue Report is failed to load");
	}

	public boolean verifyGlobalConfigFlagsPresence(String flagName) {
		boolean status = false;
		String xpath = utils.getLocatorValue("dashboardPage.globalConfligFlag").replace("temp", flagName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		status = utils.checkElementPresent(ele);
		return status;
	}

	public boolean submenuPresentorNot(String menueName, String subMenuItem) {
		utils.implicitWait(20);
		boolean flag = true;
		String xpath = utils.getLocatorValue("menuPage.childMenueName").replace("$ParentMenuName", menueName)
				.replace("$ChildMenuName", subMenuItem);
		if (driver.findElements(By.xpath(xpath)).size() == 0) {
			flag = false;
		}
		utils.implicitWait(50);
		return flag;
	}

	public String verifyBulkAccountRefresherHint() {
		utils.getLocator("dashboardPage.hintText").isDisplayed();
		String text = utils.getLocator("dashboardPage.hintText").getText();
		return text;
	}

	public void clickRefreshBtn() {
		utils.getLocator("cockpitPage.refreshBtn").click();
		logger.info("Clicked Refresh button");
	}

	public void enterIdsInBulkAccountRefresher(String id) {
		utils.getLocator("cockpitPage.bulkAccountRefreshInputBox").sendKeys(id);
		logger.info("User IDs entered susscessfully");
	}

	public void checkSegmentZipcodeSize(String inputField, String givenSize) {
		String xpath = utils.getLocatorValue("segmentPage.segmentZipcodeSize").replace("$suffix", inputField);
		WebElement ele = driver.findElement(By.xpath(xpath));
		String limit = ele.getAttribute("value");
		if (!limit.equalsIgnoreCase(givenSize)) {
			utils.getLocator("segmentPage.segmentZipcodeSize").click();
			utils.getLocator("segmentPage.segmentZipcodeSize").clear();
			utils.getLocator("segmentPage.segmentZipcodeSize").sendKeys(givenSize);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Segment zipcode size in Segment Global Configuration is set as " + givenSize);
			TestListeners.extentTest.get()
					.info("Segment zipcode size in Segment Global Configuration is set as " + givenSize);
		} else {
			logger.info("Segment zipcode size in Segment Global Configuration is already " + limit);
			TestListeners.extentTest.get()
					.info("Segment zipcode size in Segment Global Configuration is already " + limit);
		}
	}

	public void deleteResetBusinessBtnClick() {
		WebElement deleteResetBusinessBtn = utils.getLocator("dashboardPage.deleteResetBusinessBtn");
		// deleteResetBusinessBtn .click();
		utils.StaleElementclick(driver, deleteResetBusinessBtn);
		utils.waitTillPagePaceDone();
		logger.info("delete/ResetBusiness button clicked");
	}

	public void verifyGlobalConfigFlagCheckedUnchecked(String flagName) {
		String xpath = utils.getLocatorValue("dashboardPage.flagCheckedOrNot").replace("$flagName", flagName);
		WebElement checkbox = driver.findElement(By.xpath(xpath));
		String checkBoxValue = checkbox.getAttribute("checked");
		if (checkBoxValue == null || !checkBoxValue.equals("true")) {
			utils.clickByJSExecutor(driver, checkbox);
			clickOnUpdateGlobalConfigButton();
			logger.info("Skipping this Test Case because given flag: " + flagName
					+ " is not checked in Global Configuration");
			TestListeners.extentTest.get().warning("Skipping this Test Case because given flag: " + flagName
					+ " is not checked in Global Configuration");
			throw new SkipException("Given flag: " + flagName + " is  not checked in Global Configuration");
		}
		logger.info("Given flag is already checked in global configuration : " + flagName);
		TestListeners.extentTest.get().info("Given flag is already checked in global configuration : " + flagName);
	}

	public String checkBoxResponse(String checkBoxName) {
		String xpath = utils.getLocatorValue("dashboardPage.dashBoardCheckBox").replace("$flagName", checkBoxName);
		String checkBoxValue = driver.findElement(By.xpath(xpath)).getAttribute("checked");
		System.out.println();
		return checkBoxValue;
	}

	public void clickfooterLinkButton(String option) {
		String xpath = utils.getLocatorValue("dashboardPage.footerOption").replace("$flagName", option);
		WebElement optionFlag = driver.findElement(By.xpath(xpath));
		boolean flagDisplay = optionFlag.isDisplayed();
		Assert.assertTrue(flagDisplay, option + " Button is not displaped");
		optionFlag.click();
		logger.info("Clicked On " + option + " Button");
		TestListeners.extentTest.get().info("Clicked On " + option + " Button");
	}

	public void checkStatusOption() {
		utils.switchToWindow();
		// utils.waitTillPagePaceDone();
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://status.punchh.com/");
		Assert.assertTrue(flag, "Unable to open status page after clicking on status button");
		logger.info("Able to open status page after clicking on status button");
		TestListeners.extentTest.get().pass("Able to open status page after clicking on status buttonn");
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath("//h2/a")), "Status");
		Assert.assertEquals(driver.findElement(By.xpath("//h2/a")).getText(), "About This Site",
				"Status page is giving any error");
		logger.info("Status page is not giving any error");
		TestListeners.extentTest.get().pass("Status page is not giving any error");
		utils.switchToParentWindow();
	}

	public void checkDeveloperOption() {
		utils.switchToWindow();
		// utils.waitTillPagePaceDone();
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://developers.partech.com/");
		Assert.assertTrue(flag, "Unable to open developers page after clicking on developers button");
		logger.info("Able to open developers page after clicking on developers button");
		TestListeners.extentTest.get().pass("Able to open developers page after clicking on developers buttonn");
		Assert.assertTrue(
				driver.findElement(By.xpath("//div[normalize-space()='PAR Punchh Loyalty APIs']")).isDisplayed(),
				"developers page is giving any error");
		logger.info("developers page is not giving any error");
		TestListeners.extentTest.get().pass("developers page is not giving any error");
		utils.switchToParentWindow();
	}

	public void checkContactOption() {
		utils.switchToWindow();
		utils.longWaitInSeconds(5);
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://punchh.com/contact/");
		Assert.assertTrue(flag, "Unable to open contact page after clicking on contact button");
		logger.info("Able to open contact page after clicking on contact button");
		TestListeners.extentTest.get().pass("Able to open contact page after clicking on contact button");
		Assert.assertEquals(driver.findElement(By.xpath("//h1")).getText(), "Contact Us",
				"Contact page is giving any error");
		logger.info("Contact page is not giving any error");
		TestListeners.extentTest.get().pass("Contact page is not giving any error");
		utils.switchToParentWindow();
	}

	public void checkAboutOption() {
		utils.switchToWindow();
		// utils.waitTillPagePaceDone();
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://punchh.com/about/");
		Assert.assertTrue(flag, "Unable to open blog page after clicking on blog button");
		logger.info("Able to open blog page after clicking on blog button");
		TestListeners.extentTest.get().pass("Able to open blog page after clicking on blog button");
		Assert.assertEquals(driver.findElement(By.xpath("//h1")).getText(), "About PAR Technology and Punchh",
				"about page is giving any error");
		logger.info("About page is not giving any error");
		TestListeners.extentTest.get().pass("About page is not giving any error");
		utils.switchToParentWindow();
	}

	public void checkBlogOption() {
		utils.switchToWindow();
		// utils.waitTillPagePaceDone();
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://punchh.com/blog/");
		Assert.assertTrue(flag, "Unable to open blog page after clicking on blog button");
		logger.info("Able to open blog page after clicking on blog button");
		TestListeners.extentTest.get().pass("Able to open blog page after clicking on blog button");
		Assert.assertTrue(driver.findElement(By.xpath("//h2[normalize-space(text())='Blogs']")).isDisplayed(),
				"Blogs page is giving any error");
		logger.info("Blogs page is not giving any error");
		TestListeners.extentTest.get().pass("Blogs page is not giving any error");
		utils.switchToParentWindow();
	}

	public void checkSecurityOption() {
		utils.switchToWindow();
		utils.longWaitInSeconds(4);
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://punchh.com/security/");
		Assert.assertTrue(flag, "Unable to open security page after clicking on security button");
		logger.info("Able to open security page after clicking on security button");
		TestListeners.extentTest.get().pass("Able to open security page after clicking on security button");
		Assert.assertTrue(
				driver.findElement(By.xpath("//h2[normalize-space(text())='Punchh Security Overview']")).isDisplayed(),
				"Security page is giving any error");
		logger.info("Security page is not giving any error");
		TestListeners.extentTest.get().pass("Security page is not giving any error");
		utils.switchToParentWindow();
	}

	public void checkPrivacyOption() {
		utils.switchToWindow();
		// utils.waitTillPagePaceDone();
		boolean flag = utils.verifyExactURLwithoutPagePaceDone("https://punchh.com/punchh-software-privacy-policy/");
		Assert.assertTrue(flag, "Unable to open Privacy page after clicking on Privacy button");
		logger.info("Able to open Privacy page after clicking on Privacy button");
		TestListeners.extentTest.get().pass("Able to open Privacy page after clicking on Privacy button");
		Assert.assertTrue(
				driver.findElement(By.xpath("//h2[normalize-space(text())='Punchh Privacy Policy']")).isDisplayed(),
				"Privacy page is giving any error");
		logger.info("Privacy page is not giving any error");
		TestListeners.extentTest.get().pass("Privacy page is not giving any error");
		utils.switchToParentWindow();
	}

	public void clickOnHelpButton() {
		boolean flagDisplay = utils.getLocator("dashboardPage.dashboardBulbButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "Help Button is not displaped");
		utils.clickByJSExecutor(driver, utils.getLocator("dashboardPage.dashboardBulbButton"));
		utils.getLocator("dashboardPage.dashboardBulbButton").click();
		logger.info("Clicked On Help Button");
		TestListeners.extentTest.get().info("Clicked On Help Button");
	}

	public void verifyHelpPage() {
		utils.switchToWindow();
		// utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(5);
		boolean flag = utils.verifyPartOfURLwithoutPagePaceDone("sptest.iamshowcase.com");
		Assert.assertTrue(flag, "Unable to open Help page after clicking on Help button");
		logger.info("Able to open Help page after clicking on Help button");
		TestListeners.extentTest.get().pass("Able to open Help page after clicking on Help buttonn");
		Assert.assertTrue(driver.findElement(By.xpath("//h1[normalize-space()='Subject Information']")).isDisplayed(),
				"Help page is giving any error");
		logger.info("Help page is not giving any error");
		TestListeners.extentTest.get().pass("Help page is not giving any error");
		utils.switchToParentWindow();
	}

	public void clickOnSupportPortal() {
		boolean flagDisplay = utils.getLocator("dashboardPage.dashboardBulbButton").isDisplayed();
		Assert.assertTrue(flagDisplay, "Support Portal Button is not displaped");
		utils.getLocator("dashboardPage.dashboardBulbButton").click();
		logger.info("Clicked On Support Portal Button");
		TestListeners.extentTest.get().info("Clicked On Support Portal Button");
	}

	// DP Reports common method for all the the report headings.

	public boolean verifyReportHeading(String reportType) {
		utils.waitTillPagePaceDone();
		WebElement reportHeading = utils.getLocator("dashboardPage.reportHeading");
		boolean reportPresent = utils.checkElementPresent(reportHeading);
		logger.info("{} Report heading presence: {}", reportType, reportPresent);
		return reportPresent;
	}

	public boolean verifyPaymentMode() {
		WebElement paymentMode = utils.getLocator("dashboardPage.payment_mode");
		return utils.checkElementPresent(paymentMode);
	}

	public boolean verifyPaymentTransactionType() {
		WebElement paymentTransactionTypeVal = utils.getLocator("dashboardPage.payment_transaction_type");
		return utils.checkElementPresent(paymentTransactionTypeVal);
	}

	public boolean verifyPaymentStatus() {
		WebElement paymentStatusVal = utils.getLocator("dashboardPage.payment_status");
		return utils.checkElementPresent(paymentStatusVal);
	}

	public boolean verifyPaymentFilterBy() {
		WebElement paymentFilterByVal = utils.getLocator("dashboardPage.payments_filter_by");
		return utils.checkElementPresent(paymentFilterByVal);
	}

	public boolean verifyPaymentSearchButton() {
		WebElement paymentSearchButton = utils.getLocator("dashboardPage.payments_searchButton");
		paymentSearchButton.click();
		return utils.checkElementPresent(paymentSearchButton);
	}

	public boolean verifyLocationSelected(String expectedValue) {
		WebElement locationElement = utils.getLocator("dashboardPage.locationDrp");
		String selectedValue = locationElement.getText();
		logger.info("Location selected: {}", selectedValue);
		return selectedValue.equals(expectedValue);
	}

	public void selectLiabilityAsOnDate() {
		utils.waitTillPagePaceDone();
		utils.getLocator("dashboardPage.liabilityAsOnDate").click();
		logger.info("Clicked 'Liability as on date:' dropdown");
		List<WebElement> elements = utils.getLocatorList("dashboardPage.liabilityAsOnDateInput");
		TestListeners.extentTest.get().info("Selected Liability As On Date: ");
	}

	public boolean isLocationSelected(String expectedValue) {
		WebElement locationElement = utils.getLocator("dashboardPage.locationDrp");
		String selectedValue = locationElement.getText();
		logger.info("Location selected: {}", selectedValue);
		return selectedValue.equals(expectedValue);
	}

	public void updateButton() {
		utils.scrollToElement(driver, utils.getLocator("dashboardPage.updateBtn"));
		utils.getLocator("dashboardPage.updateBtn").click();
		logger.info("check box is updated");
		TestListeners.extentTest.get().pass("check box is updated");
	}

	public void globalConfigFlagCheckedUnchecked(String flagName, String checkBoxFlag) {
		String xpath = utils.getLocatorValue("dashboardPage.flagCheckedOrNot").replace("$flagName", flagName);
		WebElement checkBoxWebElement = driver.findElement(By.xpath(xpath));
		String checkBoxValue = checkBoxWebElement.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not clicked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not clicked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, checkBoxWebElement);
			clickOnUpdateGlobalConfigButton();
			logger.info("Skipping this Test Case because given flag: " + flagName
					+ " is not checked in Global Configuration and user want to check it");
			TestListeners.extentTest.get().warning("Skipping this Test Case because given flag: " + flagName
					+ " is not checked in Global Configuration and user want to check it");
			throw new SkipException("Given flag: " + flagName + " is not checked in Global Configuration");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, checkBoxWebElement);
			clickOnUpdateGlobalConfigButton();
			logger.info("Skipping this Test Case because given flag: " + flagName
					+ " is checked in Global Configuration and user want to uncheck it");
			TestListeners.extentTest.get().warning("Skipping this Test Case because given flag: " + flagName
					+ " is checked in Global Configuration and user want to uncheck it");
			throw new SkipException("Given flag: " + flagName + " is checked in Global Configuration");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {
			logger.info("Autocheckin box is already checked and user want to check the checkbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the checkbox, so do not click");
		}
	}

	public boolean isElementDisplayed(String flagName) {
		utils.implicitWait(7);
		try {
			String xpath = utils.getLocatorValue("dashboardPage.flagPresentOrNot").replace("$flagName", flagName);
			WebElement element = driver.findElement(By.xpath(xpath));
			return element.isDisplayed();
		} catch (Exception e) {
			return false;
		} finally {
			utils.implicitWait(50);
		}
	}

	public void clickDeleteButton() {
		utils.getLocator("advanceAuthConfigurationPage.deleteButton").click();
		logger.info("Clicked on Delete Button");
		TestListeners.extentTest.get().info("Clicked on Delete Button");
	}

	public boolean isCheckboxEnable(String flagName) {
		try {
			WebElement checkBox = driver.findElement(
					By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]/parent::div/parent::div"));
			String checkBoxValue = checkBox.getAttribute("class");
			if (checkBoxValue.contains("display-taxation-support")) {
				return false;
			} else {
				return true;
			}

		} catch (Exception e) {
			return false;
		}
	}

	public boolean isCheckboxChecked(String flagName) {
		try {
			WebElement checkBox = driver
					.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
			String checkBoxValue = checkBox.getAttribute("checked");
			return checkBoxValue != null;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean checkIfGivenTextIsPresentInAuditLog(String text) {
		boolean isTextFound = false;
		utils.getLocator("guestTimeLine.auditLogsBtn").click();
		utils.waitTillPagePaceDone();
		List<WebElement> logEntries = utils.getLocatorList("dashboardPage.auditLogContents");
		for (WebElement entry : logEntries) {
			String logText = entry.getText();
			if (logText.contains(text)) {
				logger.info("Found the text in audit log: " + text);
				TestListeners.extentTest.get().info("Found the text in audit log: " + text);
				return true;
			}
		}
		logger.info("Text not found in audit log: " + text);
		TestListeners.extentTest.get().info("Text not found in audit log: " + text);
		return isTextFound;

	}

	public boolean refreshPageOn502or504() throws InterruptedException {
		boolean result = false;
		int attempts = 0;
		while (attempts < 20) {
			try {
				String pageSource = driver.getPageSource();
				if (pageSource.contains("502") || pageSource.contains("504")) {
					logger.info("502/504 displayed. Refreshing page...");
					TestListeners.extentTest.get().info("502/504 displayed. Refreshing page...");
					driver.navigate().refresh();
					utils.longWaitInSeconds(5);
				} else {
					result = true;
					logger.info("Page loaded successfully without 502/504 error");
					TestListeners.extentTest.get().pass("Page loaded successfully without 502/504 error");
					break;
				}
			} catch (Exception e) {
				logger.error("Exception while checking 502/504 error", e);
			}
			attempts++;
		}
		return result;
	}

	public void enterValueinWebhookConfigDashboard(String labelName, String value) throws InterruptedException {
		String xpath = utils.getLocatorValue("dashboardPage.webhookConfigDashboardInputBox").replace("$boxName",
				labelName);
		WebElement inputBox = driver.findElement(By.xpath(xpath));
		inputBox.clear();
		inputBox.sendKeys(value);
		utils.getLocator("dashboardPage.saveBehavioralConfig").click();
		int attempts = 0;
		while (attempts < 5) {
			if (!utils.getLocator("dashboardPage.saveBehavioralConfig").isEnabled()) {
				System.out.println("Button is disabled now.");
				break;
			}
			Thread.sleep(1000);
			attempts++;
		}
		logger.info("Entered value in " + labelName + " input box: " + value);
		TestListeners.extentTest.get().info("Entered value in " + labelName + " input box: " + value);
		utils.longWaitInSeconds(10);
	}

	public void navigateToGlobalWebhookConfigDashboardTabs(String tabName) throws InterruptedException {
		utils.waitTillPagePaceDone();
		String tabXpath = utils.getLocatorValue("dashboardPage.globalWebhookConfigDashboardTabs").replace("$tab",
				tabName);
		WebElement webEle = driver.findElement(By.xpath(tabXpath));
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", webEle);
		selUtils.longWait(200);
	}

	public void verifyAndUpdateRateLimitTier(String tier, String expectedRPM, String expectedBatchSize)
			throws InterruptedException {
		String rateLimitTierXpath = utils.getLocatorValue("dashboardPage.rateLimitTier");
		String rateLimitTierRPMXpath = rateLimitTierXpath.replace("$tier", tier).replace("$value", "rpm");
		String rateLimitTierBatchSizeXpath = rateLimitTierXpath.replace("$tier", tier).replace("$value", "batch_size");
		WebElement rateLimitTierRPMWebElement = driver.findElement(By.xpath(rateLimitTierRPMXpath));
		WebElement rateLimitTierBatchSizeWebElement = driver.findElement(By.xpath(rateLimitTierBatchSizeXpath));
		String actualRPM = rateLimitTierRPMWebElement.getAttribute("value");
		System.out.println("==== actualRPM === " + actualRPM);
		String actualBatchSize = rateLimitTierBatchSizeWebElement.getAttribute("value");
		System.out.println("==== actualBatchSize === " + actualBatchSize);

		if (actualRPM.equals(expectedRPM) && actualBatchSize.equals(expectedBatchSize)) {
			logger.info("Rate Limit Tier '" + tier + "' has expected RPM: " + expectedRPM + " and Batch Size: "
					+ expectedBatchSize + " so, no update needed.");
			TestListeners.extentTest.get().info("Rate Limit Tier '" + tier + "' has expected RPM: " + expectedRPM
					+ " and Batch Size: " + expectedBatchSize + " so, no update needed.");
		} else {
			rateLimitTierRPMWebElement.clear();
			rateLimitTierRPMWebElement.sendKeys(expectedRPM);
			rateLimitTierBatchSizeWebElement.clear();
			rateLimitTierBatchSizeWebElement.sendKeys(expectedBatchSize);
			utils.getLocator("dashboardPage.saveRateLimitConfig").click();
			int attempts = 0;
			while (attempts < 5) {
				if (!utils.getLocator("dashboardPage.saveRateLimitConfig").isEnabled()) {
					System.out.println("Button is disabled now.");
					break;
				}
				Thread.sleep(1000);
				attempts++;
			}
			logger.info("Rate Limit Tier '" + tier + "' updated to RPM: " + expectedRPM + " and Batch Size: "
					+ expectedBatchSize + ".");
			TestListeners.extentTest.get().info("Rate Limit Tier '" + tier + "' updated to RPM: " + expectedRPM
					+ " and Batch Size: " + expectedBatchSize + ".");
		}
	}

	public void clickOnRefreshCacheButtonOnWebhookConfigDashboard() throws InterruptedException {
		utils.getLocator("dashboardPage.refreshCacheButton").click();
		Thread.sleep(2000);
		logger.info("Clicked on Refresh Cache Button on Webhook Configuration Dashboard");
		TestListeners.extentTest.get().info("Clicked on Refresh Cache Button on Webhook Configuration Dashboard");
	}

	public boolean tabPresentOrNot(String tabName) {
		utils.waitTillPagePaceDone();
		boolean flag = true;
		String xpath = utils.getLocatorValue("dashboardPage.dashboardTab_Xpath").replace("$TabName", tabName);
		if (driver.findElements(By.xpath(xpath)).size() == 0) {
			flag = false;
		}
		return flag;
	}

	public String getValueFromSmartPassRewardLimitFieldInGlobalConfig() {
		utils.waitTillPagePaceDone();
		WebElement element = utils.getLocator("dashboardPage.smartPassRewardLimitInGlobalConfig");
		String value = element.getAttribute("value");
		logger.info("Current value in Smart Pass Reward Limit field: " + value);
		TestListeners.extentTest.get().info("Current value in Smart Pass Reward Limit field: " + value);
		return value;
	}
}