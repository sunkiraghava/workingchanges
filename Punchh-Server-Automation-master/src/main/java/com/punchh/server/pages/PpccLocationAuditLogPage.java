package com.punchh.server.pages;

import java.text.Collator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class PpccLocationAuditLogPage {

	static Logger logger = LogManager.getLogger(PpccLocationAuditLogPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	WebDriverWait wait;
	public PpccLocationAuditLogPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(5));
	}

	public void navigateToAuditLogs() {
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccLocationAuditLogPage.auditLogButton").click();
		logger.info("User navigated to Audit Logs Tab");
		TestListeners.extentTest.get().pass("User navigated to Audit Logs Tab");
		utils.waitTillPagePaceDone();
	}

	public String getAuditLogRowColumnText(int rowIndex, int columnIndex) {
		String auditRowEntry = utils.getLocatorValue("ppccLocationAuditLogPage.auditLogRowColumnData")
				.replace("{rowIndex}", String.valueOf(rowIndex)).replace("{columnIndex}", String.valueOf(columnIndex));
		String value = driver.findElement(By.xpath(auditRowEntry)).getText();
		logger.info("Value is: " + value);
		return value;
	}

	public String getAuditLogValueOfAppVersionColumn() {
		WebElement appVersionColumn = utils.getLocator("ppccLocationAuditLogPage.auditLogFirstRowAppVersionColumnData");
		String value = appVersionColumn.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public void searchInAuditLog(String searchValue) {
		utils.waitTillPagePaceDone();
		List<WebElement> searchInputAuditLog = utils.getLocatorList("ppccLocationAuditLogPage.searchField");
		utils.waitTillPagePaceDone();
		searchInputAuditLog.get(0).click();
		searchInputAuditLog.get(0).clear();
		searchInputAuditLog.get(0).sendKeys(searchValue);
		utils.waitTillPagePaceDone();
		logger.info("Value is searched in Audit Log");
		TestListeners.extentTest.get().pass("Audit log is returned as per searched value");
	}

	public void clickOnAuditLogRowToOpenLocationAuditLogDisplayDetails() {
		utils.getLocator("ppccLocationAuditLogPage.auditLogRow").click();
		utils.waitTillPagePaceDone();
		logger.info("Audit Log Display details is opened");
		TestListeners.extentTest.get().pass("Audit Log Display details is opened");
	}

	public String getStoreNameInLocationAuditLogDisplayDetails() {
		WebElement storeName = utils.getLocator("ppccLocationAuditLogPage.storeNameAuditLogDisplayDetails");
		String value = storeName.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public String getStatusInLocationAuditLogDisplayDetails() {
		WebElement status = utils.getLocator("ppccLocationAuditLogPage.statusAuditLogDisplayDetails");
		String value = status.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public String getEventDateTimeInLocationAuditLogDisplayDetails() {
		WebElement eventDateTime = utils.getLocator("ppccLocationAuditLogPage.eventDateTimeAuditLogDisplayDetails");
		String value = eventDateTime.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public String getEventTypeInLocationAuditLogDisplayDetails() {
		WebElement eventType = utils.getLocator("ppccLocationAuditLogPage.eventTypeAuditLogDisplayDetails");
		String value = eventType.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public String getUsernameInLocationAuditLogDisplayDetails() {
		WebElement userName = utils.getLocator("ppccLocationAuditLogPage.userNameAuditLogDisplayDetails");
		String value = userName.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public void clickCancelButtonOnLocationAuditLogDisplayDetails() {
		utils.getLocator("ppccLocationAuditLogPage.cancelButtonAuditLogDisplayDetails").click();
		utils.waitTillPagePaceDone();
		logger.info("Cancel button is clicked");
	}

	public void clickBackButtonOnLocationAuditLog() {
		utils.getLocator("ppccLocationAuditLogPage.backButtonAuditLogPage").click();
		utils.waitTillPagePaceDone();
		logger.info("Back button is clicked");
	}

	public String getPolicyIdInLocationAuditLogDisplayDetails() {
		utils.longWaitInSeconds(2);
		WebElement policyId = utils.getLocator("ppccLocationAuditLogPage.policyIdAuditLogDisplayDetails");
		String value = policyId.getText();
		logger.info("Value is: " + value);
		return value;
	}

	public Map<String, String> getFieldValuesInAuditDetailsChangeLog(String fieldName) {
		String fieldKey;
		String valueRowInChangeLog = "ppccLocationAuditLogPage.changeLogRowValueForField";

		switch (fieldName) {
			case "Gift Card Merchant ID":
				fieldKey = "ppccLocationAuditLogPage.giftCardMerchantIDChangeLoginAuditDisplayDetails";
				break;

			case "POS Type":
				fieldKey = "ppccLocationAuditLogPage.posTypeChangeLoginAuditDisplayDetails";
				break;

			case "Policy ID":
				fieldKey = "ppccLocationAuditLogPage.policyIdChangeLoginAuditDisplayDetails";
				break;

			case "Policy Name":
				fieldKey = "ppccLocationAuditLogPage.policyNameChangeLoginAuditDisplayDetails";
				break;

			case "Config Status":
				fieldKey = "ppccLocationAuditLogPage.configStatusChangeLoginAuditDisplayDetails";
				break;

			case "Package Status":
				fieldKey = "ppccLocationAuditLogPage.packageStatusChangeLoginAuditDisplayDetails";
				break;

			case "Remote Upgrade":
				fieldKey = "ppccLocationAuditLogPage.remoteUpgradeChangeLoginAuditDisplayDetails";
				break;

			case "Package Version":
				fieldKey = "ppccLocationAuditLogPage.packageVersionChangeLoginAuditDisplayDetails";
				break;

			case "Package Version ID":
				fieldKey = "ppccLocationAuditLogPage.packageVersionIdChangeLoginAuditDisplayDetails";
				break;

			default:
				throw new IllegalArgumentException("Field Type Not Present " + fieldName);
		}

		WebElement fieldValues = utils.getLocator(fieldKey);
		String valueRowXPath = utils.getLocatorValue(valueRowInChangeLog);
		List<WebElement> fieldChangeLogRows = fieldValues.findElements(By.xpath(valueRowXPath));

		if (fieldChangeLogRows.size() < 2) {
			throw new IllegalStateException(
					"Expected at least 2 rows (old and new value) but found " + fieldChangeLogRows.size());
		}

		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("oldValue", fieldChangeLogRows.get(0).getText().trim());
		valuesMap.put("newValue", fieldChangeLogRows.get(1).getText().trim());
		logger.info(fieldName + " values in Change Log: " + valuesMap);
		return valuesMap;
	}

	public void applyFilterOnLocationsAuditLog(String filterOption, String filterValue) {
		String filterValueXpath = utils.getLocatorValue("ppccLocationAuditLogPage.filterValue").replace("{filterValue}",
				filterValue);
		String filterOptionXpath = utils.getLocatorValue("ppccLocationAuditLogPage.filterValue")
				.replace("{filterValue}", filterOption);
		utils.longWaitInSeconds(2);
		utils.getLocator("ppccLocationAuditLogPage.filterIcon").click();
		logger.info("FilterIcon is clicked");
		TestListeners.extentTest.get().pass("FilterIcon is clicked");
		utils.getLocator("ppccLocationAuditLogPage.filterOption").click();
		driver.findElement(By.xpath(filterOptionXpath)).click();
		logger.info("Selecting the filter Key in filters");
		TestListeners.extentTest.get().pass("Selecting the filter Key in filters");
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccLocationAuditLogPage.selectTextOnFilter").click();
		driver.findElement(By.xpath(filterValueXpath)).click();
		logger.info("Selecting the filter Value in filters");
		TestListeners.extentTest.get().pass("Selecting the filter Value in filters");
		utils.getLocator("ppccLocationAuditLogPage.applyButtonOnFilter").click();
		logger.info("Apply button is clicked in filters, filters are applied.");
		TestListeners.extentTest.get().pass("Apply button is clicked in filters, filters are applied.");
		utils.waitTillPagePaceDone();
	}

	public boolean isLocationsFilteredOnAuditLog(String expectedValue, String filterOption) {
		boolean isLocationsFiltered = verifyFilteredLocationListInAuditLog(expectedValue, filterOption);
		return isLocationsFiltered;
	}

	public boolean verifyFilteredLocationListInAuditLog(String expectedValue, String filterOption) {
		String Locations = utils.getLocatorValue("ppccLocationAuditLogPage.locationsRow");
		List<WebElement> rows = driver.findElements(By.xpath(Locations));

		if (rows.isEmpty()) {
			return false;
		}

		String columnValue = "";
		switch (filterOption) {
			case "User Name":
				columnValue = utils.getLocatorValue("ppccLocationAuditLogPage.userNameColumn");
				break;
			case "Location Name(Store Name)":
				columnValue = utils.getLocatorValue("ppccLocationAuditLogPage.storeNameColumn");
				break;
			case "Package Status":
				columnValue = utils.getLocatorValue("ppccLocationAuditLogPage.packageStatusColumn");
				break;
			case "Event Type":
				columnValue = utils.getLocatorValue("ppccLocationAuditLogPage.eventTypeColumn");
				break;
			case "Policy Status":
				columnValue = utils.getLocatorValue("ppccLocationAuditLogPage.policyStatusColumn");
				break;
			default:
				logger.warn("Invalid filter option: " + filterOption);
				return false;
		}

		for (WebElement row : rows) {
			WebElement column = row.findElement(By.xpath(columnValue));
			String value = column.getText();

			if (!value.contains(expectedValue)) {
				logger.info("Column Value is not same:  '" + expectedValue + "': " + value);
				return false;
			}
		}
		return true;
	}

	public void sortColumn(String columnName) {
		String columnLocator="";
		switch (columnName.trim()) {
			case "Store Name":
				columnLocator = "ppccLocationAuditLogPage.storeNameHeader";
				break;
			case "Location id":
				columnLocator = "ppccLocationAuditLogPage.locationIdHeader";
				break;
			case "Status":
				columnLocator = "ppccLocationAuditLogPage.statusHeader";
				break;
			case "Event Type":
				columnLocator = "ppccLocationAuditLogPage.eventTypeHeader";
				break;
			case "Username":
				columnLocator = "ppccLocationAuditLogPage.userNameHeader";
				break;
			case "App Version":
				columnLocator = "ppccLocationAuditLogPage.appVersionHeader";
				break;
			case "Event DateTime":
				columnLocator = "ppccLocationAuditLogPage.eventDateTimeHeader";
				break;
			default:
				throw new IllegalArgumentException("Column not found " + columnName);
		}
		utils.waitTillPagePaceDone();
		utils.getLocator(columnLocator).click();
		utils.waitTillPagePaceDone();
	}

	public List<String> getColumnData(String columnIndex) {
		List<String> dataList = new ArrayList<>();
		String column;
		if ("4".equals(columnIndex)) {
			// this is to handle app version
			column = utils.getLocatorValue("ppccLocationAuditLogPage.auditLogAppVersionColumnData");
		} else {
			// Default behavior
			column = utils.getLocatorValue("ppccLocationAuditLogPage.auditLogColumnData").replace("{columnIndex}",
					columnIndex);
		}

		List<WebElement> columnElements = driver.findElements(By.xpath(column));
		for (WebElement row : columnElements) {
			String data = row.getText().trim();
			logger.info("Data in column " + columnIndex + ": " + data);
			dataList.add(data);
		}
		return dataList;
	}
	public void changeEntriesToHundred(int numberOfRows)
	{
		utils.waitTillPagePaceDone();
		utils.getLocator("ppccLocationAuditLogPage.defaultNumberOfRows").click();
		String locator = utils.getLocatorValue("ppccLocationAuditLogPage.changeToNumberOfRows").replace("{numberOfRows}", String.valueOf(numberOfRows));
		driver.findElement(By.xpath(locator)).click();
		utils.waitTillPagePaceDone();
	}

	public List<String> sortList(List<String> list) {
		List<String> sorted = new ArrayList<>(list);
		Collator coll = Collator.getInstance();
		coll.setStrength(Collator.PRIMARY);
		coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);

		sorted.sort((s1, s2) -> {
			int minLength = Math.min(s1.length(), s2.length());
			for (int i = 0; i < minLength; i++) {
				char c1 = s1.charAt(i);
				char c2 = s2.charAt(i);

				if (c1 == c2)
					continue;

				if (c1 == '-' && c2 == '.')
					return -1;
				if (c1 == '.' && c2 == '-')
					return 1;

				return coll.compare(s1, s2);
			}
			return Integer.compare(s1.length(), s2.length());
		});

		return sorted;
	}

	public List<LocalDateTime> getEventDateTimeColumnValues(String columnIndex, DateTimeFormatter formatter) {
		List<String> rawData = getColumnData(columnIndex);
		List<LocalDateTime> dateList = new ArrayList<>();

		for (String dateText : rawData) {
			LocalDateTime parsedDate = utils.parseDate(dateText, formatter);
			if (parsedDate != null) {
				dateList.add(parsedDate);
			}
		}
		return dateList;
	}

	public WebElement getCalendarIcon() {
		utils.waitTillPagePaceDone();
		return utils.getLocator("ppccLocationAuditLogPage.calendarIcon");
	}

	public WebElement getTodayDate() {
		utils.waitTillPagePaceDone();
		return utils.getLocator("ppccLocationAuditLogPage.todayDatePicker");
	}

	public void setTodayDateInCalendar() {
		getCalendarIcon().click();
		getTodayDate().click();
		getTodayDate().click();
		utils.waitTillPagePaceDone();
	}

	public boolean isValuePresentOnUI(String searchItem, String locator) {
		utils.waitTillPagePaceDone();
		List<WebElement> rows = driver.findElements(By.xpath(locator));
		boolean isSearchedItemPresent = true;
		if (rows.isEmpty()) {
			isSearchedItemPresent = false;
		} else {
			for (WebElement row : rows) {
				String rowText = row.getText();
				logger.info("Row text: " + rowText);
				if (!rowText.contains(searchItem)) {
					isSearchedItemPresent = false;
				}
			}
		}
		return isSearchedItemPresent;
	}
}