package com.punchh.server.pages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

//package com.punchh.server.pages;
@Listeners(TestListeners.class)
public class DataExportPage {
	static Logger logger = LogManager.getLogger(DataExportPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public DataExportPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void goToDataExport() throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Reports", "Data Export");
	}

	public void createNewDataExportTemplate(String exportName) {
		List<WebElement> checkBoxList = null;
		setDataExportNameAndDate(exportName);
		checkBoxList = utils.getLocatorList("dataExportPage.fieldsCheckbox");
		logger.info("Selecting all fields checkbox for data export");
		TestListeners.extentTest.get().info("Selecting all fields checkbox for data export");
		for (WebElement ele : checkBoxList) {
			if (ele.getAttribute("class").contains("boolean optional"))
				logger.info(ele.getAttribute("class"));
			else
				ele.click();
		}
		utils.getLocator("dataExportPage.saveExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveExportButton").click();
		utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		TestListeners.extentTest.get()
				.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
	}

	public void setDataExportNameAndDate(String exportName) {
		utils.getLocator("dataExportPage.dataExportNameTextbox").isDisplayed();
		utils.getLocator("dataExportPage.dataExportNameTextbox").clear();
		utils.getLocator("dataExportPage.dataExportNameTextbox").sendKeys(exportName);
		logger.info("Export Name is set as:" + exportName);
		TestListeners.extentTest.get().info("Export Name is set as:" + exportName);

		logger.info("Selecting all fields checkbox for data export");
		TestListeners.extentTest.get().info("Selecting all fields checkbox for data export");
		// checkBoxList = utils.getLocatorList("dataExportPage.fieldsCheckbox");
		utils.getLocator("dataExportPage.dateTextbox").isDisplayed();
		utils.getLocator("dataExportPage.dateTextbox").click();
//		utils.getLocator("dataExportPage.last90DaysLabel").isDisplayed();
//		utils.getLocator("dataExportPage.last90DaysLabel").click();

		utils.getLocator("dataExportPage.currentWeekLabel").isDisplayed();
		utils.getLocator("dataExportPage.currentWeekLabel").click();
	}

	public void setDataExportName(String exportName) {
		utils.getLocator("dataExportPage.dataExportNameTextbox").isDisplayed();
		utils.getLocator("dataExportPage.dataExportNameTextbox").clear();
		utils.getLocator("dataExportPage.dataExportNameTextbox").sendKeys(exportName);
		logger.info("Export Name is set as:" + exportName);
		TestListeners.extentTest.get().info("Export Name is set as:" + exportName);
	}

	public void setDataExportDateRange(String dateRannge) {
		utils.getLocator("dataExportPage.clickRateRange").isDisplayed();
		utils.getLocator("dataExportPage.clickRateRange").click();
		List<WebElement> ele = utils.getLocatorList("dataExportPage.listOfDateRange");
		utils.selectListDrpDwnValue(ele, dateRannge);
		logger.info("Clicked on Date Range " + dateRannge);
		TestListeners.extentTest.get().info("Clicked on Date Range " + dateRannge);
	}

	public void setDataExportLocation(String locationChoice, String locationName) {
		// select location
		switch (locationChoice) {
		case "location":
			utils.getLocator("dataExportPage.clickOnLocation").click();
			utils.getLocator("dataExportPage.enterlocation").click();
			utils.getLocator("dataExportPage.enterlocation").sendKeys(locationName);
			utils.getLocator("dataExportPage.enterlocation").sendKeys(Keys.ENTER);
			logger.info("Selected location is " + locationName);
			TestListeners.extentTest.get().info("Selected location is " + locationName);
			break;

		default:
			logger.info("Export is running for all Locations");
			TestListeners.extentTest.get().info("Export is running for all Locations");
			break;
		}

	}

	public void createNewAllDataExportTemplate(String exportName) {
		List<WebElement> checkBoxList = null;
		setDataExportNameAndDate(exportName);
		checkBoxList = utils.getLocatorList("dataExportPage.fieldsCheckbox");
		if (checkBoxList.size() == 13) {
			selectFields();
			for (WebElement ele : checkBoxList)
				selUtils.jsClick(ele);
		} else {
			Assert.fail("Data export fields count is not 14, count mismatch, please check");
		}
		utils.getLocator("dataExportPage.saveExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveExportButton").click();
		utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		TestListeners.extentTest.get()
				.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
	}

	private void selectFields() {
		List<WebElement> filedTitleList = utils.getLocatorList("dataExportPage.fieldCardTitle");
		for (WebElement fieldTitle : filedTitleList) {
			logger.info("Expanding field: " + fieldTitle.getText());
			TestListeners.extentTest.get().info("Expanding field: " + fieldTitle.getText());
			selUtils.scrollToElement(fieldTitle);
			selUtils.jsClick(fieldTitle);

		}
		List<WebElement> selectAllList = utils.getLocatorList("dataExportPage.selectAllButton");
		if (selectAllList.size() == 13) {
			for (WebElement selectButton : selectAllList) {
				selUtils.scrollToElement(selectButton);
				selUtils.jsClick(selectButton);
			}
		} else {
			Assert.fail("Data export fields are not expanded, please check");
		}
		logger.info("Selected all available fiedls for all exports type");
	}

	public void goToDataExportBeta() {
		utils.getLocator("instanceDashboardPage.reportLabel").isDisplayed();
		utils.getLocator("instanceDashboardPage.reportLabel").click();
		utils.getLocator("instanceDashboardPage.exportBetaLink").isDisplayed();
		utils.getLocator("instanceDashboardPage.exportBetaLink").click();
		utils.getLocator("instanceDashboardPage.dataExportHeading").isDisplayed();
		logger.info("Navigated to dataexport page");
		TestListeners.extentTest.get().info("Navigated to dataexport page");
	}

	public void removeHeaderFromDataExport(String removeHeader) {
		// check/uncheck removed header option in data export
		switch (removeHeader) {
		case "Remove header info":
			utils.getLocator("dataExportPage.removeHeaderInfoCheckbox").click();
			logger.info("Clicked on Remove header info from csv");
			TestListeners.extentTest.get().pass("Clicked on Remove header info from csv ");
			break;

		case "Remove column headers":
			utils.getLocator("dataExportPage.removeColunmHeaderCheckbox").click();
			logger.info("Clicked on Remove column headers from csv");
			TestListeners.extentTest.get().pass("Clicked on Remove column headers from csv ");
			break;

		case "Remove Both headers":
			utils.getLocator("dataExportPage.removeHeaderInfoCheckbox").click();
			logger.info("Clicked on Remove header info from csv");
			TestListeners.extentTest.get().pass("Clicked on Remove header info from csv ");
			utils.longWaitInSeconds(1);
			utils.getLocator("dataExportPage.removeColunmHeaderCheckbox").click();
			logger.info("Clicked on Remove column headers from csv");
			TestListeners.extentTest.get().pass("Clicked on Remove column headers from csv ");
			break;

		default:
			logger.info("Remove header option is not Selected");
			TestListeners.extentTest.get().info("Remove header option is not Selected");
			break;
		}
	}

	public List<String> createNewDataExportWithCustomData(String exportName, String exportType, String choice,
			String dateRannge, String locationChoice, String locationName, String removeHeader)
			throws InterruptedException {
		List<String> fieldList = new ArrayList<String>();
		setDataExportName(exportName);
		setDataExportDateRange(dateRannge);
		setDataExportLocation(locationChoice, locationName);
		removeHeaderFromDataExport(removeHeader);
		// exportType ="User Subscription Data";
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldCheckbox").replace("temp", exportType)))
				.click();
		logger.info("Selected export type: " + exportType);
		TestListeners.extentTest.get().info("Selected export type: " + exportType);
		switch (choice) {
		case "select all":
			fieldList = getReportOfAllFields(exportType);
			break;

		default:
			fieldList = getReportFields(exportType, "selected");
			break;
		}
		utils.getLocator("dataExportPage.saveExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveExportButton").click();
		utils.waitTillElementToBeClickable(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel"));
		utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		TestListeners.extentTest.get()
				.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		return fieldList;
	}

	public List<String> createNewDataExport(String exportName, String exportType, String choice)
			throws InterruptedException {
		List<String> fieldList = new ArrayList<String>();
		setDataExportNameAndDate(exportName);
		// exportType ="User Subscription Data";
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldCheckbox").replace("temp", exportType)))
				.click();
		logger.info("Selected export type: " + exportType);
		TestListeners.extentTest.get().info("Selected export type: " + exportType);
		switch (choice) {
		case "select all":
			fieldList = getReportOfAllFields(exportType);
			break;

		default:
			fieldList = getReportFields(exportType, "selected");
			break;
		}
		utils.getLocator("dataExportPage.saveExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveExportButton").click();
		utils.waitTillElementToBeClickable(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel"));
		utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		TestListeners.extentTest.get()
				.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		return fieldList;
	}

	public List<String> getReportFields(String exportType, String fieldName) throws InterruptedException {
		clickOnDataExportNameLink(exportType);
		List<String> fieldList = new ArrayList<String>();

		if (fieldName.equalsIgnoreCase("selected")) {
			String xpathSelectedFieldsText = utils.getLocatorValue("dataExportPage.selectedFieldsText").replace("$temp",
					exportType);
			utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpathSelectedFieldsText)), "Selected Fields");
			Thread.sleep(2000);

			String xpathSelectedFieldsListLabel = utils.getLocatorValue("dataExportPage.selectedFieldsListLabel")
					.replace("$temp", exportType);
			List<WebElement> list = driver.findElements(By.xpath(xpathSelectedFieldsListLabel));
			for (WebElement ele : list) {
				fieldList.add(ele.getText());
				logger.info(ele.getText());
				TestListeners.extentTest.get().info(ele.getText());
			}
			if (fieldList.isEmpty())
				Assert.fail("export fields appears to be zero");
		} else if (fieldName.equalsIgnoreCase("available")) {
			String xpathAvailableFieldsText = utils.getLocatorValue("dataExportPage.availableFieldsText")
					.replace("$temp", exportType);
			utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpathAvailableFieldsText)),
					"Available Fields");
			Thread.sleep(2000);

			String xpathAvailableFieldsListLabel = utils.getLocatorValue("dataExportPage.availableFieldsListLabel")
					.replace("$temp", exportType);
			List<WebElement> list = driver.findElements(By.xpath(xpathAvailableFieldsListLabel));
			for (WebElement ele : list) {
				fieldList.add(ele.getText());
				logger.info(ele.getText());
				TestListeners.extentTest.get().info(ele.getText());
			}
			if (fieldList.isEmpty())
				Assert.fail("export fields appears to be zero");
		}
		return fieldList;
	}

	private List<String> getReportOfAllFields(String exportType) {
		utils.longWaitInSeconds(1);
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldLink").replace("temp", exportType))).click();
		String xpath1 = utils.getLocatorValue("dataExportPage.selectAllFields").replace("$ExportType", exportType);
		driver.findElement(By.xpath(xpath1)).click();
		String xpath = utils.getLocatorValue("dataExportPage.selectedAllFieldsListLabel").replace("$ExportType",
				exportType);
		List<WebElement> list = driver.findElements(By.xpath(xpath));
		List<String> fieldList = new ArrayList<String>();
		for (WebElement ele : list) {
			fieldList.add(ele.getText());
			logger.info(ele.getText());
			TestListeners.extentTest.get().info(ele.getText());
		}
		if (fieldList.size() == 0)
			Assert.fail("export fields appears to be zero");
		return fieldList;
	}

	public boolean checkLabelPresent(String exportType, String label, String choice) {
		boolean flag = false;
		String xpath = "";
		List<WebElement> list = new ArrayList<WebElement>();
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldClick").replace("$ExportType", exportType)))
				.click();
		logger.info("Selected export type for label present is " + exportType);
		TestListeners.extentTest.get().info("Selected export type for label present is " + exportType);
		switch (choice) {
		case "Available Fields":
			utils.waitTillPagePaceDone();
			xpath = utils.getLocatorValue("dataExportPage.eleInAvailableList").replace("$ExportType", exportType);
			list = driver.findElements(By.xpath(xpath));
			for (WebElement ele : list) {
				String text = ele.getText();
				label = label.trim();
				flag = utils.textContains(text, label);
				if (flag == true)
					break;
			}
			break;

		case "Selected Fields":
			utils.waitTillPagePaceDone();
			xpath = utils.getLocatorValue("dataExportPage.eleInSelectedList").replace("$ExportType", exportType);
			list = driver.findElements(By.xpath(xpath));
			for (WebElement ele : list) {
				String text = ele.getText();
				flag = utils.textContains(text, label);
				if (flag == true)
					break;
			}
			break;
		}
		return flag;
	}

	public void verifyDateInDataExportReport(String fileName, List<String> fieldList, String pastDate,
			String futureDate) {
		List<String> list = new ArrayList<>();
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(System.getProperty("user.dir") + prop.getProperty("downloadPath") + fileName));
			String line = null;
			while ((line = reader.readLine()) != null) {
				list.add(line);
			}
			reader.close();
			String stry = list.get(0);
			boolean flag = stry.contains(pastDate);
			Assert.assertTrue(flag, "Start date is not matching in Data Export Report");
			logger.info("Start date is matching in Data Export Report");
			TestListeners.extentTest.get().pass("Start date is matching in Data Export Report");
			boolean flag1 = stry.contains(futureDate);
			Assert.assertTrue(flag1, "End date is not matching in Data Export Report");
			logger.info("End date is matching in Data Export Report");
			TestListeners.extentTest.get().pass("End date is matching in Data Export Report");

		} catch (Exception e) {
			logger.info("Error verifying column" + e);
			TestListeners.extentTest.get().fail("Error verifying column " + e);
			Assert.fail("Error verifying column");
		}
	}

	public boolean verifyColumnsValueWithDbValue(String fileName, List<String> dbList) {
		boolean flag = false;
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
			int counter = 0;
			for (String value : list) {
				String arr[] = value.split(",");
				flag1 = dbList.contains(arr[1]);
				if (flag1) {
					counter++;
				}
			}
			if ((list.size() - 2) == counter) {
				flag = true;
				logger.info("user id from downloaded file is equal to DB user id");
				TestListeners.extentTest.get().info("user id from downloaded file is equal to DB user id");
			}
		} catch (Exception e) {
			logger.info("Error verifying column" + e);
			TestListeners.extentTest.get().fail("Error verifying column " + e);
			Assert.fail("Error verifying column");
		}
		return flag;
	}

	public String verifyFlagsInDataExport(String exportType) {
		String value = driver.findElement(By.xpath(
				utils.getLocatorValue("dataExportPage.dataExportTextVerification").replace("$ExportType", exportType)))
				.getText();
		logger.info("verify Flags In Data Export " + exportType);
		TestListeners.extentTest.get().info("verify Flags In Data Export " + exportType);
		return value;
	}

	public void clickOnExport(String exportType) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldClick").replace("$ExportType", exportType)))
				.click();
		logger.info("click On Export " + exportType);
		TestListeners.extentTest.get().info("click On Export " + exportType);
	}

	public ArrayList<String> clickDataExportNameCheckBox(String exportType) {
		List<String> fieldList = new ArrayList<String>();
		fieldList = getReportOfAllFields(exportType);
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldCheckbox").replace("temp", exportType)))
				.click();
		logger.info("export name selected is " + exportType);
		TestListeners.extentTest.get().info("export name selected is " + exportType);
		ArrayList<String> fieldArrayList = new ArrayList<String>(fieldList);
		return fieldArrayList;
	}

	public void clickOnSaveExportButton() {
		utils.getLocator("dataExportPage.saveExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveExportButton").click();
		utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		TestListeners.extentTest.get()
				.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
	}

	// Need to check how to verify this
	public void verifyBusinessLiabilityReportValues(String heading) {
//		utils.waitTillPagePaceDone();
//		utils.longWaitInSeconds(14);
//		utils.waitTillVisibilityOfElement(utils.getLocator("dataExportPage.businessLiabilityReportHeading"), "Business Liability Report");
//		String headingValue = utils.getLocator("dataExportPage.businessLiabilityReportHeading").getText();
//		Assert.assertEquals(headingValue, heading,
//				"Heading for Business Liability Report is not Matching " + headingValue);
//		logger.info("Verified that Heading for Business Liability Report is  visible ");
//		TestListeners.extentTest.get().pass("Verified that Heading for Business Liability Report is  visible ");
	}

	public void verifySubHeadingOnBusinessLiabilityReport(String subHeading) {
//		utils.waitTillPagePaceDone();
//		utils.longWaitInSeconds(30);
//		WebElement iframe = driver.findElement(By.xpath("//iframe[@frameborder='0']"));
////		WebDriverWait wait = new WebDriverWait(driver, 30);
////		wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(iframe));
//		driver.switchTo().frame(iframe);
//		String subHead = utils.getLocator("dataExportPage.subHeadingBusinessLiabilityReport").getText();
//		Assert.assertEquals(subHead, subHeading, "Sub Heading for usiness Liability Report is not Matching");
//		logger.info("Verified that Sub Heading for usiness Liability Report is not Matching");
//		TestListeners.extentTest.get().pass("Verified that Sub Heading for usiness Liability Report is not Matching");
//		driver.switchTo().parentFrame();
	}

	public void createDataPipeline(String dpName, String destinationType, String destinationURL,
			String destinationFileType) throws InterruptedException {
		utils.waitTillPagePaceDone();
		selUtils.waitTillElementToBeClickable(utils.getLocator("dataExportPage.dataPipelineNameBox"));
		utils.longWaitInSeconds(2);
		clickOnTrialPipelineName();
		utils.scrollToElement(driver, utils.getLocator("dataExportPage.dataPipelineDestinationURL"));
		utils.getLocator("dataExportPage.dataPipelineDestinationURL").click();
		utils.getLocator("dataExportPage.dataPipelineDestinationURL").sendKeys(destinationURL);

		utils.getLocator("dataExportPage.dataPipelineNameBox").click();
		utils.getLocator("dataExportPage.dataPipelineNameBox").sendKeys(dpName);

		String destinationTypeXpath = utils.getLocatorValue("dataExportPage.dataPipelineDestinationType")
				.replace("${destinationType}", destinationType);
		driver.findElement(By.xpath(destinationTypeXpath)).click();

		String destinationFileTypeXpath = utils.getLocatorValue("dataExportPage.dataPipelineDestinationFileType")
				.replace("${destinationFileType}", destinationFileType);
		driver.findElement(By.xpath(destinationFileTypeXpath)).click();
		selUtils.longWait(2000);
		utils.getLocator("dataExportPage.dataPipelineCreateButton").click();
		utils.waitTillPagePaceDone();
		logger.info(dpName + " data pipeline is created successfully");
		TestListeners.extentTest.get().info(dpName + " data pipeline is created successfully");

	}

	public void clickOnSelectTablesActivateButton(String dataPipelineName) {
		String selectTablesActivateButtonXpath = utils
				.getLocatorValue("dataExportPage.dataPipelineSelectTablesActivateButton")
				.replace("${dataPipelineName}", dataPipelineName);
		utils.scrollToElement(driver, driver.findElement(By.xpath(selectTablesActivateButtonXpath)));
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(selectTablesActivateButtonXpath)));
		driver.findElement(By.xpath(selectTablesActivateButtonXpath)).click();

		logger.info(dataPipelineName + " data pipeline's select tables activate button is clicked ");
		TestListeners.extentTest.get()
				.info(dataPipelineName + " data pipeline's select tables activate button is clicked ");
	}

	public void deleteDataPipeline(String dataPipelineName) {
		String deleteButtonXpath = utils.getLocatorValue("dataExportPage.dataPipelineDeleteButton")
				.replace("${dataPipelineName}", dataPipelineName);
		driver.findElement(By.xpath(deleteButtonXpath)).click();
		selUtils.longWait(1000);
		utils.getLocator("dataExportPage.dataPipelinePopUpDeleteButton").click();
		logger.info("Deleted Data Pipeline with the name " + dataPipelineName + " from UI");
		TestListeners.extentTest.get().pass("Deleted Data Pipeline with the name " + dataPipelineName + " from UI");
		utils.waitTillPagePaceDone();
	}

	public boolean verifyDataTableIsPresentInDataPipeline(String expectedDataTableName) {

		List<WebElement> dataTableList = utils.getLocatorList("dataExportPage.dataPipelineIsExist");

		for (WebElement wEle : dataTableList) {
			String actualText = wEle.getText();
			if (expectedDataTableName.equalsIgnoreCase(actualText)) {
				return true;
			}

		}

		return false;

	}

	public ArrayList<String> listOfDataExport() {
		List<String> exportList = new ArrayList<String>();
		boolean flag = false;
		List<WebElement> weleList = driver.findElements(By.xpath("//div[@id='accordion']/div"));
		// int n = weleList.size();
		for (int i = 0; i < weleList.size(); i++) {
			String exportName = weleList.get(i).getText();
			exportList.add(exportName);
		}
		if (weleList.size() == exportList.size()) {
			flag = true;
		}
		Assert.assertTrue(flag, "All exports are not added in the Data Export List");
		logger.info("All exports are added in the Data Export List which were " + exportList);
		TestListeners.extentTest.get().info("All exports are added in the Data Export List which were " + exportList);
		ArrayList<String> exportNameList = new ArrayList<String>(exportList);
		return exportNameList;
	}

	public ArrayList<String> clickDataExportNameCheckBoxDefaultFields(String exportType) throws InterruptedException {
		List<String> fieldList = new ArrayList<String>();
		fieldList = getReportFields(exportType, "selected");
		driver.findElement(
				By.xpath(utils.getLocatorValue("dataExportPage.exportFieldCheckbox").replace("temp", exportType)))
				.click();
		logger.info("export name selected is " + exportType);
		TestListeners.extentTest.get().info("export name selected is " + exportType);
		ArrayList<String> fieldArrayList = new ArrayList<String>(fieldList);
		return fieldArrayList;
	}

	public boolean verifyFileExistsForRemovedHeaderDataExport(String fileName) {
		boolean flag = false;
		try {
			File file = new File(System.getProperty("user.dir") + "/resources/ExportData/" + fileName);
			if (file.exists()) {
				long sizeInKB = file.length();
				logger.info("Verified exported file exists: " + fileName + " and size of file is " + sizeInKB);
				TestListeners.extentTest.get()
						.pass("Verified exported file exists: " + fileName + " and size of file is " + sizeInKB);
				return true;
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

	public boolean checkTrialPipelineVisibility() {
		boolean visibilityFlag = false;
		utils.waitTillPagePaceDone();
		try {
			utils.implicitWait(10);
			visibilityFlag = utils.getLocator("dataExportPage.checkTrialPipelineVisibility").isDisplayed();
			logger.info("Trail Data Pipeline is available");
			TestListeners.extentTest.get().info("Trail Data Pipeline is available");
			utils.implicitWait(50);
		} catch (Exception e) {
			utils.implicitWait(50);
			logger.info("Trail Data Pipeline is not available");
			TestListeners.extentTest.get().info("Trail Data Pipeline is not available");
		}
		return visibilityFlag;
	}

	public void clickOnStartATrial() {
		utils.getLocator("dataExportPage.clickOnStartATrail").click();
		utils.waitTillPagePaceDone();
		String heading = utils.getLocator("dataExportPage.dataPipelinePageHeading").getText();
		Assert.assertEquals(heading, "Create Data Pipeline", "Create Data Pipeline is not visible");
		logger.info("Create Data Pipeline is visible");
		TestListeners.extentTest.get().info("Create Data Pipeline is visible");
	}

	public void validateTrialStartDate(String choice, String date, boolean flag) {
		boolean visibilityFlag;
		utils.scrollToElement(driver, utils.getLocator("dataExportPage.dataPipelineCreateButton"));
		// utils.scrollToElement(driver,
		// utils.getLocator("dataExportPage.statusfooter"));
		utils.longWaitInSeconds(2);
		utils.getLocator("dataExportPage.clickOnTrialStartDate").click();
		String classAttribute = null;
		String currentMonth = Utilities.getCurrentDate("MMMM");
		if (date.contains(", 0")) {
			date = date.replace(", 0", ", ");
		}
		utils.waitTillVisibilityOfElement(utils.getLocator("dataExportPage.calendarDayMonday"),
				"Data Pipeline Calendar");
		switch (choice) {
		case "Current":
			utils.longWaitInSeconds(2);
			String xpathCurrentDate = utils.getLocatorValue("dataExportPage.selectTrialDate").replace("$Date", date);
			driver.findElement(By.xpath(xpathCurrentDate)).click();
			logger.info("Current Date is Selected as Trail Pipeline start date " + date);
			TestListeners.extentTest.get().info("Current Date is Selected as Trail Pipeline start date " + date);
			classAttribute = driver.findElement(By.xpath(xpathCurrentDate)).getAttribute("class");
			break;

		case "Previous":
			if (!(date.contains(currentMonth))) {
				utils.getLocator("dataExportPage.leftNavButton").click();
			}
			utils.longWaitInSeconds(2);
			String xpathPreviousDate = utils.getLocatorValue("dataExportPage.selectTrialDate").replace("$Date", date);
			driver.findElement(By.xpath(xpathPreviousDate)).click();
			logger.info("Past Date is Selected as Trail Pipeline start date " + date);
			TestListeners.extentTest.get().info("Past Date is Selected as Trail Pipeline start date " + date);
			classAttribute = driver.findElement(By.xpath(xpathPreviousDate)).getAttribute("class");
			break;

		case "After 30 Days":
			if (!(date.contains(currentMonth))) {
				utils.getLocator("dataExportPage.rightNavButton").click();
			}
			utils.longWaitInSeconds(2);
			String xpathFutureDate = utils.getLocatorValue("dataExportPage.selectTrialDate").replace("$Date", date);
			driver.findElement(By.xpath(xpathFutureDate)).click();
			logger.info("Future Date is Selected as Trail Pipeline start date " + date);
			TestListeners.extentTest.get().info("Future Date is Selected as Trail Pipeline start date " + date);
			classAttribute = driver.findElement(By.xpath(xpathFutureDate)).getAttribute("class");
			break;
		}
		utils.implicitWait(10);
		if (classAttribute.contains("disabled")) {
			visibilityFlag = true;
		} else {
			visibilityFlag = false;
		}
		Assert.assertEquals(visibilityFlag, flag, "Start Trial Date calender is not working expected for " + choice);
		logger.info("Start Trial Date calender is working expected for " + choice);
		TestListeners.extentTest.get().pass("Start Trial Date calender is working expected for " + choice);
		utils.implicitWait(50);
	}

	public void clickOnTrialPipelineName() {
		utils.clickByJSExecutor(driver, utils.getLocator("dataExportPage.dataPipelineNameBox"));
//		utils.getLocator("dataExportPage.dataPipelineNameBox").click();
		logger.info("Clicked on Trial Pipeline Name");
		TestListeners.extentTest.get().info("Clicked on Trial Pipeline Name");
	}

	public String verifySuccessMessage(String index) {
		utils.waitTillPagePaceDone();
		// utils.longWaitInSeconds(3);
		String xpathMessage = "(" + utils.getLocatorValue("dataExportPage.trailPipelineMessage") + ")[" + index + "]";
		String message = driver.findElement(By.xpath(xpathMessage)).getText();
		logger.info(message);
		return message;
	}

	public void verifyActivationDaysMessage(String startDate, String endDate) {
//		One scenario need  to be test between 1st to 9th of the month

		// boolean flag = false;
		String xpathMessage = "(" + utils.getLocatorValue("dataExportPage.trailPipelineMessage") + ")[2]";
		String message = driver.findElement(By.xpath(xpathMessage)).getText();
//		if (message.contains(startDate) && message.contains(endDate)) {
//			flag = true;
//		}
		Assert.assertNotEquals(message, "", "Trial Data Pipeline Activation Range Message is not visible");
		logger.info(
				"Trial Data Pipeline Activation Range Message is visible and Activation message is " + xpathMessage);
		TestListeners.extentTest.get().pass(
				"Trial Data Pipeline Activation Range Message is visible and Activation message is " + xpathMessage);
	}

	public String verifyTestConnection() {
		utils.longWaitInSeconds(2);
		utils.getLocator("dataExportPage.testConnectionTrialPipeline").click();
		utils.waitTillPagePaceDone();
		String message = verifySuccessMessage("2");
		return message;
//		Assert.assertEquals(message, successMsg, "Test Connection is Failed");
//		logger.info("Test Connection message is successfully verified i.e. " + message);
//		TestListeners.extentTest.get().pass("Test Connection message is successfully verified i.e. " + message);
	}

	public String pipelineName() {
		utils.longWaitInSeconds(2);
		String name = utils.getLocator("dataExportPage.getTrialDataPipelineName").getText();
		return name;
	}

	public void clickEditPipeline() {
		utils.getLocator("dataExportPage.clickEditButton").click();
		logger.info("Clicked On Edit button");
		TestListeners.extentTest.get().info("Clicked On Edit button");
	}

	public void editNameTrialPipeline(String name) {
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(5);
		utils.getLocator("dataExportPage.dataPipelineNameBox").click();
		utils.getLocator("dataExportPage.dataPipelineNameBox").clear();
		utils.getLocator("dataExportPage.dataPipelineNameBox").sendKeys(name);
		logger.info("New name of trial data pipeline is " + name);
		TestListeners.extentTest.get().info("New name of trial data pipeline is " + name);
	}

	public void clickSaveButton() {
		utils.longWaitInSeconds(3);
		utils.scrollToElement(driver, utils.getLocator("dataExportPage.saveButton"));
		utils.getLocator("dataExportPage.saveButton").click();

		logger.info("Clicked On save button");
		TestListeners.extentTest.get().info("Clicked On save button");
		utils.waitTillPagePaceDone();
	}

	public void deleteTrailPipeline() {
		utils.waitTillPagePaceDone();
		if (utils.getLocator("dataExportPage.deleteTrialPipeline").isDisplayed()) {
			utils.getLocator("dataExportPage.deleteTrialPipeline").click();
			logger.info("Clicked On delete trial piepline button");
			TestListeners.extentTest.get().info("Clicked On delete trial piepline button");
		}
	}

	public void createCategorySalesReport(String categoryExportName, String bucketname) {
		utils.getLocator("dataExportPage.createCategoryScheduleButton").isDisplayed();
		utils.getLocator("dataExportPage.createCategoryScheduleButton").click();

		utils.getLocator("dataExportPage.categoryNameDropdown").click();
		List<WebElement> elem = utils.getLocatorList("dataExportPage.categoryTypeList");
		utils.selectListDrpDwnValue(elem, categoryExportName);
		TestListeners.extentTest.get().pass("Export Name is set as:" + categoryExportName);
		utils.getLocator("dataExportPage.bucketName").isDisplayed();
		utils.getLocator("dataExportPage.bucketName").click();
		utils.getLocator("dataExportPage.bucketName").sendKeys(bucketname);
		utils.getLocator("dataExportPage.saveCategoryExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveCategoryExportButton").click();
		utils.getLocator("dataExportPage.categoryScheduleCreated").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.categoryScheduleCreated").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.categoryScheduleCreated").getText());
	}

	public void createCategorySalesReportForP(String categoryExportName, String ftpName) {
		utils.getLocator("dataExportPage.createCategoryScheduleButton").isDisplayed();
		utils.getLocator("dataExportPage.createCategoryScheduleButton").click();

		utils.getLocator("dataExportPage.categoryNameDropdown").click();
		List<WebElement> elem = utils.getLocatorList("dataExportPage.categoryTypeList");
		utils.selectListDrpDwnValue(elem, categoryExportName);
		TestListeners.extentTest.get().pass("Export Name is set as:" + categoryExportName);
		utils.getLocator("dataExportPage.ftpDropdown").click();
		List<WebElement> elem1 = utils.getLocatorList("dataExportPage.ftpList");
		utils.selectListDrpDwnValue(elem1, ftpName);
		TestListeners.extentTest.get().pass("Export Name is set as:" + categoryExportName);
		utils.getLocator("dataExportPage.saveCategoryExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveCategoryExportButton").click();
		utils.getLocator("dataExportPage.categoryScheduleCreated").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.categoryScheduleCreated").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.categoryScheduleCreated").getText());
	}

	public void validateCategoryFieldsInCockpit() {
		utils.getLocator("cockpitPage.reportingInCockpit").click();
		utils.getLocator("dataExportPage.categoryReportingCheckbox").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.categoryReportingCheckbox").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.categoryReportingCheckbox").getText());
		utils.getLocator("dataExportPage.scanDataCheckbox").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.scanDataCheckbox").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.scanDataCheckbox").getText());
		utils.getLocator("dataExportPage.p+Checkbox").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.p+Checkbox").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.p+Checkbox").getText());
		utils.getLocator("dataExportPage.accountNumberfield").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.accountNumberfield").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.accountNumberfield").getText());
		utils.getLocator("dataExportPage.adIDField").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.adIDField").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.adIDField").getText());
		utils.getLocator("dataExportPage.menuItemField").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.menuItemField").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.menuItemField").getText());
	}

	public void validateUIOfCategorySchedulesAndDeactivate() {
		utils.getLocator("dataExportPage.ScanDataSubHeading").isDisplayed();
		logger.info("The Scan Data Category Report is displayed");
		TestListeners.extentTest.get().pass("The Scan Data Category Report is displayed");
		utils.getLocator("dataExportPage.BenchmarkSubHeading").isDisplayed();
		logger.info("The Benchmark Category Report is displayed");
		TestListeners.extentTest.get().pass("The Benchmark  Category Report is displayed");
		utils.getLocator("dataExportPage.ActivitySubHeading").isDisplayed();
		logger.info("The Activity Category Report is displayed");
		TestListeners.extentTest.get().pass("The Activity Category Report is displayed");
		utils.getLocator("dataExportPage.AgeVerificationSubHeading").isDisplayed();
		logger.info("The Age verification Category Report is displayed");
		TestListeners.extentTest.get().pass("The Age verification Category Report is displayed");
		utils.getLocator("dataExportPage.ScanDataSubHeading").click();
		utils.getLocator("dataExportPage.deactivateCategoryReport").isDisplayed();
		utils.getLocator("dataExportPage.deactivateCategoryReport").click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to deactivate this schedule?")) {
			alert.accept();
		} else {
			logger.error("Incorrect alert message");
			TestListeners.extentTest.get().fail("Incorrect alert message");
		}
		utils.getLocator("dataExportPage.deactivateLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.deactivateLabel").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.deactivateLabel").getText());
		logger.info("The Scan Data Category Report is deactivated");
		TestListeners.extentTest.get().pass("The Scan Data Category Report is deactivated");
	}

	public boolean validateDeletionOfScanData() {
		utils.getLocator("dataExportPage.ScanDataSubHeading").click();
		if (!utils.getLocator("dataExportPage.deleteCategoryReport").isDisplayed()) {
			logger.error("Delete Category Report not displayed for Scan Data");
			return false;
		}
		utils.getLocator("dataExportPage.deleteCategoryReport").click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to delete this schedule?")) {
			alert.accept();
		} else {
			logger.error("Incorrect alert message for Scan Data");
			TestListeners.extentTest.get().fail("Incorrect alert message for Scan Data");
			return false;
		}
		if (!utils.getLocator("dataExportPage.deleteLabel").isDisplayed()) {
			logger.error("Delete label not displayed for Scan Data");
			return false;
		}
		logger.info(utils.getLocator("dataExportPage.deleteLabel").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.deleteLabel").getText());
		logger.info("The Scan Data Category Report is deleted");
		TestListeners.extentTest.get().pass("The Scan Data Category Report is deleted");
		return true;
	}

	public boolean validateDeletionOfBenchmark() {
		utils.getLocator("dataExportPage.BenchmarkSubHeading").click();
		if (!utils.getLocator("dataExportPage.deleteCategoryReport").isDisplayed()) {
			logger.error("Delete Category Report not displayed for Benchmark");
			return false;
		}
		utils.getLocator("dataExportPage.deleteCategoryReport").click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to delete this schedule?")) {
			alert.accept();
		} else {
			logger.error("Incorrect alert message for Benchmark");
			TestListeners.extentTest.get().fail("Incorrect alert message for Benchmark");
			return false;
		}
		if (!utils.getLocator("dataExportPage.deleteLabel").isDisplayed()) {
			logger.error("Delete label not displayed for Benchmark");
			return false;
		}
		logger.info(utils.getLocator("dataExportPage.deleteLabel").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.deleteLabel").getText());
		logger.info("The Benchmark Category Report is deleted");
		TestListeners.extentTest.get().pass("The Benchmark Category Report is deleted");
		return true;
	}

	public boolean validateDeletionOfActivity() {
		utils.getLocator("dataExportPage.ActivitySubHeading").click();
		if (!utils.getLocator("dataExportPage.deleteCategoryReport").isDisplayed()) {
			logger.error("Delete Category Report not displayed for Activity");
			return false;
		}
		utils.getLocator("dataExportPage.deleteCategoryReport").click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to delete this schedule?")) {
			alert.accept();
		} else {
			logger.error("Incorrect alert message for Activity");
			TestListeners.extentTest.get().fail("Incorrect alert message for Activity");
			return false;
		}
		if (!utils.getLocator("dataExportPage.deleteLabel").isDisplayed()) {
			logger.error("Delete label not displayed for Activity");
			return false;
		}
		logger.info(utils.getLocator("dataExportPage.deleteLabel").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.deleteLabel").getText());
		logger.info("The Activity Category Report is deleted");
		TestListeners.extentTest.get().pass("The Activity Category Report is deleted");
		return true;
	}

	public boolean validateDeletionOfAgeVerification() {
		utils.getLocator("dataExportPage.AgeVerificationSubHeading").click();
		if (!utils.getLocator("dataExportPage.deleteCategoryReport").isDisplayed()) {
			logger.error("Delete Category Report not displayed for Age Verification");
			return false;
		}
		utils.getLocator("dataExportPage.deleteCategoryReport").click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to delete this schedule?")) {
			alert.accept();
		} else {
			logger.error("Incorrect alert message for Age Verification");
			TestListeners.extentTest.get().fail("Incorrect alert message for Age Verification");
			return false;
		}
		if (!utils.getLocator("dataExportPage.deleteLabel").isDisplayed()) {
			logger.error("Delete label not displayed for Age Verification");
			return false;
		}
		logger.info(utils.getLocator("dataExportPage.deleteLabel").getText());
		TestListeners.extentTest.get().pass(utils.getLocator("dataExportPage.deleteLabel").getText());
		logger.info("The Age Verification Category Report is deleted");
		TestListeners.extentTest.get().pass("The Age Verification Category Report is deleted");
		return true;
	}

	public void createLocationsDataExport(String exportName) {
		utils.getLocator("dataExportPage.dataExportNameTextbox").isDisplayed();
		utils.getLocator("dataExportPage.dataExportNameTextbox").clear();
		utils.getLocator("dataExportPage.dataExportNameTextbox").sendKeys(exportName);
		logger.info("Export Name is set as:" + exportName);
		utils.getLocator("dataExportPage.locationDataExport").isDisplayed();
		utils.getLocator("dataExportPage.locationDataExport").click();
		utils.getLocator("dataExportPage.saveExportButton").isDisplayed();
		utils.getLocator("dataExportPage.saveExportButton").click();
		utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").isDisplayed();
		logger.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());
		TestListeners.extentTest.get()
				.info(utils.getLocator("dataExportPage.dataExportTemplateCreationLabel").getText());

	}

	public void verifyDataPipelineList() {
		// Verify that the Data Pipeline page is displayed and all sections are present
		logger.info("Verifying that 'Global Tables' section is displayed");
		Assert.assertTrue(utils.getLocator("dataExportPage.globalTables").isDisplayed(),
				"'Global Tables' is not displayed.");

		logger.info("verifying the add new table button is displayed");
		Assert.assertTrue(utils.getLocator("dataExportPage.addNewTableButton").isDisplayed(),
				"'Add New Table' button is not displayed.");
		utils.getLocator("dataExportPage.addNewTableButton").click();
		logger.info("The pop-up for adding new table is displayed");
		WebElement cancelBtn = utils.getLocator("dataExportPage.cancelButton");
		utils.waitTillVisibilityOfElement(cancelBtn, "Cancel Button");
		utils.clickWithActions(cancelBtn);

		logger.info("Verify the search field is displayed");
		Assert.assertTrue(utils.getLocator("dataExportPage.searchField").isDisplayed(),
				"'Search' field is not displayed.");
		utils.getLocator("dataExportPage.searchField").sendKeys("feedback_categories");
		utils.waitTillPagePaceDone();
		logger.info("Verifying that search result is displayed");
		WebElement searchResult = utils.getLocator("dataExportPage.searchResult");
		Assert.assertTrue(searchResult.isDisplayed(), "'Search Result' is not displayed.");

		logger.info("Verifying that 'Categories' section is displayed");
		Assert.assertTrue(utils.getLocator("dataExportPage.categories").isDisplayed(),
				"'Categories' is not displayed.");

		logger.info("Verifying that 'Pipeline List' section is displayed");
		Assert.assertTrue(utils.getLocator("dataExportPage.pipelineList").isDisplayed(),
				"'Pipeline List' is not displayed.");

		logger.info("All sections on the Data Pipeline page are displayed successfully.");
	}

	public void verifyDataDictionaryLinkInDataPipeline() {
		// Locate and assert visibility of the Data Dictionary link
		Assert.assertTrue(utils.getLocator("dataExportPage.documentationLink").isDisplayed(),
				"Data Dictionary link is not displayed on the page");

		// Get the href value from the anchor tag
		String documentationLink = utils.getLocator("dataExportPage.documentationLink").getAttribute("href");
		logger.info("Data Dictionary link href is: " + documentationLink);
		TestListeners.extentTest.get().info("Data Dictionary link href is: " + documentationLink);

		// Verify the link starts with expected base URL
		Assert.assertTrue(documentationLink.startsWith("https://dashboard.staging.punchh.io/saml/auth"),
				"Data Dictionary link does not start with the expected base URL");

		logger.info("Verified the Data Dictionary link in Data Pipeline");
		TestListeners.extentTest.get().pass("Verified the Data Dictionary link in Data Pipeline");
	}

	public void verifyEmailSubscriptionInDataPipeline(String email) {
		utils.getLocator("dataExportPage.emailSubscriptionCheckbox").isDisplayed();
		utils.getLocator("dataExportPage.emailSubscriptionCheckbox").click();
		logger.info("Email Subscription checkbox is clicked");
		TestListeners.extentTest.get().info("Email Subscription checkbox is clicked");
		utils.getLocator("dataExportPage.addEmailIcon").isDisplayed();
		utils.getLocator("dataExportPage.addEmailIcon").click();
		utils.getLocator("dataExportPage.emailSubscriptionTextBox").sendKeys(email);
		logger.info("Email Subscription is set as: " + email);
		TestListeners.extentTest.get().info("Email Subscription is set as: " + email);
		utils.getLocator("dataExportPage.emailSubscriptionTextBox").sendKeys(Keys.RETURN);
		utils.getLocator("dataExportPage.saveButton").isDisplayed();
		utils.getLocator("dataExportPage.saveButton").click();
		logger.info("Clicked on Save button");
		TestListeners.extentTest.get().info("Clicked on Save button");
		utils.waitTillPagePaceDone();

	}

	public void deleteEmailSubscription() {
		utils.getLocator("dataExportPage.emailSubscriptionCheckbox").isDisplayed();
		utils.getLocator("dataExportPage.emailSubscriptionCheckbox").click();
		logger.info("Email Subscription checkbox is clicked");
		TestListeners.extentTest.get().info("Email Subscription checkbox is clicked");
		utils.getLocator("dataExportPage.deletEmailIcon").click();
		logger.info("Delete Email icon is clicked");
		TestListeners.extentTest.get().info("Delete Email icon is clicked");
		utils.getLocator("dataExportPage.saveButton").isDisplayed();
		utils.getLocator("dataExportPage.saveButton").click();
		logger.info("Clicked on Save button");
		TestListeners.extentTest.get().info("Clicked on Save button");
		utils.waitTillPagePaceDone();
		selUtils.longWait(2000);
	}

	public void clickOnDataExportNameLink(String exportType) throws InterruptedException {
		String xpathExportFieldLink = utils.getLocatorValue("dataExportPage.exportFieldLink").replace("temp",
				exportType);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpathExportFieldLink)));
	}

	public String getDisclaimerTextInGuestAwaitingMigration() {
		utils.getLocator("dataExportPage.guestAwaitingMigrationLink").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("dataExportPage.guestAwaitingMigrationDisclaimer"),
				"Guest Awaiting Migration Disclaimer");
		return utils.getLocator("dataExportPage.guestAwaitingMigrationDisclaimer").getText().trim();
	}

	public String verifyAdditionalTab() {

		utils.getLocator("dataExportPage.guestAwaitingMigrationLink").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("dataExportPage.guestAwaitingMigrationDisclaimer"),
				"Guest Awaiting Migration Disclaimer");
		return utils.getLocator("dataExportPage.guestAwaitingMigrationDisclaimer").getText().trim();
	}

	public void verifyAdditionalScheduledExportTabPresent() {
		utils.waitTillPagePaceDone();
		Assert.assertTrue(utils.getLocator("dataExportPage.additionalScheduledExportTab").isDisplayed(),
				"Additional Scheduled Export tab is not present in Data Export Page");
		logger.info("Additional Scheduled Export tab is present in Data Export Page");

	}

	public void validationsInAdditionalScheduledExportTab() {
		utils.getLocator("dataExportPage.additionalScheduledExportTab").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("dataExportPage.singleExportInfo").click();
		WebElement singleTooltip = utils.getLocator("dataExportPage.singleExportTooltipText");
		String infoText = singleTooltip.getText();
		Assert.assertEquals(infoText,
				"Only one export of this type can be created. Clicking again will open the existing export",
				"Info text for Single Export is not matching");
		logger.info("Verified Info text for Single Export is matching");
		TestListeners.extentTest.get().pass("Verified Info text for Single Export is matching");
		utils.getLocator("dataExportPage.multipleExportInfo").click();
		WebElement multipleTooltip = utils.getLocator("dataExportPage.multipleExportTooltipText");
		String infoText1 = multipleTooltip.getText();
		Assert.assertEquals(infoText1, "You can create multiple exports of this type, each with unique configurations",
				"Info text for Multiple Export is not matching");
		logger.info("Verified Info text for Multiple Export is matching");
		TestListeners.extentTest.get().pass("Verified Info text for Multiple Export is matching");
	}

}