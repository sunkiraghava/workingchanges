package com.punchh.server.pages;

import java.text.Collator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PpccPackagePage {

	static Logger logger = LogManager.getLogger(PpccPackagePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	WebDriverWait wait;

	public PpccPackagePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(5));
	}

	public void navigateToPackagesTab() {
		utils.waitTillPagePaceDone();
		waitTillPCCLoaderDisappears();
		utils.getLocator("ppccPackagePage.packagesTab").click();
		logger.info("Navigated to Pos Control Center packages tab");
		utils.waitTillPagePaceDone();
	}

	public List<String> getColumnsHeaders() {
		String xpath = utils.getLocatorValue("ppccPackagePage.headers");
		List<WebElement> headers = driver.findElements(By.xpath(xpath));

		List<String> actualHeaders = new ArrayList<>();
		for (WebElement header : headers) {
			actualHeaders.add(header.getText().trim());
		}
		return actualHeaders;
	}

	public WebElement getSearchFieldOnUI() {
		WebElement searchField = utils.getLocator("ppccPackagePage.searchField");
		return searchField;
	}

	public WebElement getFilterIconOnUI() {
		WebElement filterIcon = utils.getLocator("ppccPackagePage.filterButton");
		return filterIcon;
	}

	public WebElement getOpenedCopyPackageLink() {
		WebElement openedCopyPackageLink = utils.getLocator("ppccPackagePage.openedCopyPackageLink");
		return openedCopyPackageLink;
	}

	public WebElement getOpenedDownloadPackage() {
		WebElement openedDownloadPackage = utils.getLocator("ppccPackagePage.openedDownloadPackage");
		return openedDownloadPackage;
	}

	public WebElement getPinIcon() {
		WebElement pinIcon = utils.getLocator("ppccPackagePage.pinIcon");
		return pinIcon;
	}

	public WebElement getCancelButton() {
		WebElement cancelButton = utils.getLocator("ppccPackagePage.closeReleaseNotesPopUp");
		return cancelButton;
	}

	public WebElement getCopyReleaseNotes() {
		WebElement copyReleaseNotesPopUp = utils.getLocator("ppccPackagePage.copyReleaseNotesPopUp");
		return copyReleaseNotesPopUp;
	}

	public WebElement getCloseReleaseNotesPopUp() {
		WebElement closeReleaseNotesPopUp = utils.getLocator("ppccPackagePage.closeReleaseNotesPopUp");
		return closeReleaseNotesPopUp;
	}

	public WebElement getCopyPackageLink(String index) {
		String copyPackageLink = utils.getLocatorValue("ppccPackagePage.copyPackageLink");
		WebElement copyPackageLinkXpath = driver.findElement(By.xpath(copyPackageLink + index));
		return copyPackageLinkXpath;
	}

	public WebElement getDownloadPackage(String index) {
		String downloadPackage = utils.getLocatorValue("ppccPackagePage.downloadPackage");
		WebElement downloadPackageXpath = driver.findElement(By.xpath(downloadPackage + index));
		return downloadPackageXpath;
	}

	public WebElement getViewReleaseNotes(String index) {
		String viewReleaseNotes = utils.getLocatorValue("ppccPackagePage.viewReleaseNotes");
		WebElement viewReleaseNotesXpath = driver.findElement(By.xpath(viewReleaseNotes + index));
		return viewReleaseNotesXpath;
	}

	public void removeFilters() {
		String removeFilter = utils.getLocatorValue("ppccPackagePage.removeFilters");
		WebElement removeFilterButton = driver.findElement(By.xpath(removeFilter));
		removeFilterButton.click();
		utils.waitTillPagePaceDone();
	}

	public WebElement getSortIcon() {
		WebElement sortIcon = utils.getLocator("ppccPackagePage.sortIcon");
		return sortIcon;
	}

	public List<String> getMissingSortableHeaders() {
		List<WebElement> headers = driver.findElements(By.xpath("ppccPackagePage.headers"));
		List<String> missingHeaders = new ArrayList<>();
		for (WebElement header : headers) {
			String sortIcon = utils.getLocatorValue(getSortIcon().toString());
			List<WebElement> svgElements = header.findElements(By.xpath(sortIcon));
			if (svgElements.isEmpty()) {
				missingHeaders.add(header.getText().trim());
			}
		}
		return missingHeaders;
	}

	public boolean checkSearchFunctionality(String searchItem) {
		utils.getLocator("ppccPackagePage.searchField").sendKeys(searchItem);
		utils.getLocator("ppccPackagePage.searchField").sendKeys(Keys.ENTER);
		String items = utils.getLocatorValue("ppccPackagePage.itemEntry");

		utils.waitTillPagePaceDone();

		List<WebElement> rows = driver.findElements(By.xpath(items));
		boolean isSearchedItemPresent = true;
		if (rows.isEmpty()) {
			isSearchedItemPresent = false;
		} else {
			for (WebElement row : rows) {
				String rowText = row.getText();
				if (!rowText.contains(searchItem)) {
					isSearchedItemPresent = false;
				}
			}
		}
		return isSearchedItemPresent;
	}

	public boolean isDataFiltered(String filterOption, String filterValue, int columnIndex, String expectedValue) {
		applyFilter(filterOption, filterValue);
		boolean isDataFiltered = isFilteredResultValid(columnIndex, expectedValue);
		return isDataFiltered;
	}

	private void applyFilter(String filterOption, String filterValue) {
		utils.getLocator("ppccPackagePage.filterButton").click();
		utils.getLocator("ppccPackagePage.selectFilterOption").click();
		List<WebElement> elements = utils.getLocatorList("ppccPackagePage.filterListOption");
		utils.selectListDrpDwnValue(elements, filterOption);
		utils.getLocator("ppccPackagePage.selectFilterOption").click();
		List<WebElement> filterValueElements = utils.getLocatorList("ppccPackagePage.filterListOption");
		utils.selectListDrpDwnValue(filterValueElements, filterValue);
		utils.getLocator("ppccPackagePage.applyButton").click();
		utils.waitTillPagePaceDone();
	}

	private boolean isFilteredResultValid(int columnIndex, String expectedValue) {
		String items = utils.getLocatorValue("ppccPackagePage.itemEntry");
		List<WebElement> rows = driver.findElements(By.xpath(items));

		if (rows.isEmpty()) {
			return false;
		}
		String columnData = utils.getLocatorValue("ppccPackagePage.columnData").replace("{columnIndex}",
				String.valueOf(columnIndex));
		for (WebElement row : rows) {
			WebElement column = row.findElement(By.xpath(columnData));
			String value = column.getText();

			if (!value.contains(expectedValue)) {
				logger.info("Found a row where data is not '" + expectedValue + "': " + value);
				return false;
			}
		}
		return true;
	}

	public void sortData(String columnLocator) {
		utils.waitTillPagePaceDone();
		waitTillPCCLoaderDisappears();
		utils.getLocator(columnLocator).click();
		utils.waitTillPagePaceDone();
	}

	public List<LocalDateTime> getDateColumnData(String column, DateTimeFormatter formatter) {
		List<String> rawData = getColumnData(column);
		List<LocalDateTime> dateList = new ArrayList<>();

		for (String dateText : rawData) {
			LocalDateTime parsedDate = utils.parseDate(dateText, formatter);
			if (parsedDate != null) {
				dateList.add(parsedDate);
			}
		}
		return dateList;
	}

	public List<String> getColumnData(String columnIndex) {
		List<String> dataList = new ArrayList<>();
		String column = utils.getLocatorValue("ppccPackagePage.columnData").replace("{columnIndex}", columnIndex);
		List<WebElement> columnElements = driver.findElements(By.xpath(column));
		for (WebElement row : columnElements) {
			String data = row.getText().trim();
			logger.info("Data in column " + columnIndex + ": " + data);
			dataList.add(data);
		}
		return dataList;
	}

	public void changeEntriesToHundred(int numberOfRows) {
		utils.getLocator("ppccPackagePage.defaultNumberOfRows").click();
		String locator = utils.getLocatorValue("ppccPackagePage.changeToNumberOfRows").replace("{numberOfRows}",
				String.valueOf(numberOfRows));
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

	public List<String> getCleanedVersionData(List<String> versionData) {
		return versionData.stream().map(s -> s.replaceAll("\\n[0-9a-fA-F\\-]{36}$", "")).collect(Collectors.toList());
	}

	public void waitTillPCCLoaderDisappears() {
		String loaderXpath = utils.getLocatorValue("ppccPackagePage.pccLoaderDisappears");
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(180));
			wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(loaderXpath)));
			logger.info("Spinner disappeared");
			TestListeners.extentTest.get().info("spinner disappeared");
		} catch (Exception e) {
			logger.info(e.getMessage());
			TestListeners.extentTest.get().info(e.getMessage());
		}
	}

}
