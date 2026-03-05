package com.punchh.server.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LocationPage {

	static Logger logger = LogManager.getLogger(LocationPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	Properties prop;
	String consoleName, temp = "temp";
	String locationName, temp1 = "temp";
	String storeName, temp2 = "temp";
	String api_key;
	String cust_status;
	public String s;
	WebElement deactivate;
	private PageObj pageObj;

	public LocationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public String newLocation(String storeNumber) {
		utils.getLocator("locationPage.newLoctionButton").click();
		logger.info("Clicked on New location");
		locationName = prop.getProperty("locationNamePrefix").replace(temp, CreateDateTime.getTimeDateString());
		utils.getLocator("locationPage.nameTextBox").sendKeys(locationName);
		utils.getLocator("locationPage.storeNumberTextBox").sendKeys(storeNumber);
		utils.getLocator("locationPage.addressTextBox").sendKeys("Test Address");
		utils.getLocator("locationPage.cityTextBox").sendKeys("Test City");
		utils.getLocator("locationPage.stataTextBox").sendKeys("Test state");
		utils.getLocator("locationPage.postcodeTextBox").sendKeys("10012");
		selUtils.longWait(3000);
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Mobile App");
		if (utils.getLocatorList("locationPage.locationWorkingHrInputbox").size() != 0) {
			utils.getLocator("locationPage.locationWorkingHrInputbox").sendKeys("1-8");
		}
		utils.getLocator("locationPage.addLoctionButton").click();
		logger.info("Clicked on add location");
		return locationName;
	}

	public void selectLocationSearch(String locationName) {
		utils.longWaitInMiliSeconds(1);
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		utils.getLocator("locationPage.searchBar").sendKeys(Keys.ENTER);
		logger.info("Clicked on search bar and entered location name");
		utils.waitTillPagePaceDone();
		utils.longWaitInMiliSeconds(500);
		WebElement ele = driver.findElement(By.xpath("//a[normalize-space()='" + locationName + "']"));
		utils.waitTillElementToBeClickable(ele);
		utils.tryAllClick(driver, ele);
		utils.waitTillPagePaceDone();
		utils.getLocator("locationPage.pickupTabLink").click();
		utils.getLocator("locationPage.pickupCheckbox").click();
		utils.getLocator("locationPage.pickuptabUpdateButton").click();
	}

	public void selectLocationSearch2(String locationName) {
		String LName = "";
		if (locationName.equalsIgnoreCase("")) {
			LName = "Daphne";
		} else {
			LName = locationName;
		}
		utils.getLocator("locationPage.searchBar").sendKeys(LName);
		String xpath = utils.getLocatorValue("locationPage.locationConsole").replace(temp, LName);
		WebElement element = driver.findElement(By.xpath(xpath));
		utils.waitTillElementToBeClickable(element);
		element.click();
		logger.info("Clicked on search bar and entered location name: " + LName);
		TestListeners.extentTest.get().info("Clicked on search bar and entered location name: " + LName);
		utils.waitTillPagePaceDone();
	}

	public void verifyAlternativeStoreID(String altstorenum) {
		String LName = "Daphne";
		utils.getLocator("locationPage.searchBar").sendKeys(LName);
		logger.info("Clicked on search bar and entered location name");
		driver.findElement(By.xpath("//a[normalize-space()='temp']".replace(temp, LName))).click();
		utils.getLocator("locationPage.AlternativeStoreID").isDisplayed();
		logger.info("Alternative store id field is available");
		utils.getLocator("locationPage.AlternativeStoreID").clear();
		utils.getLocator("locationPage.AlternativeStoreID").sendKeys(altstorenum);
		utils.getLocator("locationPage.LocationtabUpdate").click();

	}

	public String verifyAlternativeStoreIDKeyValuesInCardsAPI(Response apiResponse, String variable, String variable2,
			String actualValue, String verifyingVar) {

		List<Object> obj = new ArrayList<Object>();
		int j = 0;
		String expectedValue, expectedValueFlag;

		obj = apiResponse.jsonPath().getList(variable);
		System.out.println(obj);
		for (int i = 0; i < obj.size(); i++) {

			expectedValue = apiResponse.jsonPath().getString("[0]." + variable + "[" + i + "]." + variable2);
			if (expectedValue.contains(actualValue)) {
				j = i;
				break;
			}
		}
		expectedValueFlag = apiResponse.jsonPath().getString(variable + "[" + j + "]." + verifyingVar);
		return expectedValueFlag;
	}

	public void SelectLocationConsole() {
		utils.waitTillPagePaceDone();
		utils.clickByJSExecutor(driver, utils.getLocator("locationPage.locationConsoleDrp"));
		// utils.getLocator("locationPage.locationConsoleDrp").click();
		logger.info("Clicked on LocationConsole dropdown");
		utils.getLocator("locationPage.locationConsoleDrpList").click();
		logger.info("Clicked on Add console in the dropdown");
	}

	public void selectLocationConsoleList() {
		utils.waitTillPagePaceDone();
		utils.getLocator("locationPage.locationConsoleDrp").click();
		logger.info("Clicked on LocationConsole dropdown");
		TestListeners.extentTest.get().info("Clicked on LocationConsole dropdown");
		utils.getLocator("locationPage.locationConsoleDrpList1").click();
		logger.info("Clicked on Console list in the dropdown");
		TestListeners.extentTest.get().info("Clicked on Console list in the dropdown");
		utils.waitTillPagePaceDone();
	}

	public void addConsole(String consoleName) {

		utils.getLocator("locationPage.nameText1Box").sendKeys(consoleName); // Enters the console name in the add
																				// console page
		utils.getLocator("locationPage.createButton").click();
		logger.info("Entered console name");
		TestListeners.extentTest.get().info("Entered console name");
		api_key = driver
				.findElement(By.xpath("//td[contains(text(),'temp')]/../td[3]/span[@class]".replace(temp, consoleName)))
				.getText();
		logger.info(api_key);
		TestListeners.extentTest.get().info(api_key);
	}

	public void navigateTopickupConsole(String url) {
		// selUtils.implicitWait(40);
		driver.get(url);
		// utils.getLocator("instanceLoginPage.punchLogoImg").isDisplayed();
		logger.info("Navigated to pickup Console login page ");
		TestListeners.extentTest.get().info("Navigated to pickup Console login page with url :" + url);

	}

	public void pickupConsole() {
		// driver.switchTo().newWindow(wind)
		// driver.get("https://qa.punchh.io/activate/moes/");
		// logger.info("Pickup console opened in new tab");
		utils.getLocator("locationPage.authorizationTextBox").sendKeys(api_key.substring(0, 10));
		logger.info("Entered api_key");
		selUtils.longWait(3000);
		utils.getLocator("locationPage.verifyButton").click();
		logger.info(" Clicked on verify and Pickup console login successfull");
		utils.waitTillSpinnerDisappear(5);
		// Accept sound Alert
		utils.getLocator("pickupConsolePage.soundAlertpopupYesButton").click();
		logger.info(" Accept sound alert popup ");

		driver.navigate().back();
		driver.navigate().back();
		logger.info("Back to location console page");
		driver.navigate().refresh();
		selUtils.longWait(5000);
		logger.info(api_key.substring(10));
		// Assert.assertEquals(api_key.substring(10),"Key in use");
	}

	public void pickupConsoleLogin() {
		WebElement authKeyField = utils.getLocator("locationPage.authorizationTextBox");
		utils.waitTillVisibilityOfElement(authKeyField, "Device Authorization Key");
		authKeyField.sendKeys(api_key.substring(0, 10));
		utils.logit("Entered Device Authorization key");
		utils.longWaitInSeconds(3);
		utils.getLocator("locationPage.verifyButton").click();
		utils.logit("Clicked on verify and Pickup console login is successful");
		utils.longWaitInSeconds(2);
		// utils.waitTillSpinnerDisappear();
		boolean isSoundAlertPopupPresent = pageObj.gamesPage()
				.isPresent(utils.getLocatorValue("pickupConsolePage.soundAlertpopupYesButton"));
		if (isSoundAlertPopupPresent) {
			// Accept sound alert popup
			utils.getLocator("pickupConsolePage.soundAlertpopupYesButton").click();
			utils.logit("Accepted sound alert popup");
		}
	}

	public String orderSearch(String orderId) {
		try {
			utils.longWaitInSeconds(5);
			utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderId);
			cust_status = utils.getLocator("pickupConsolePage.customerstatusTextBox").getText();
			utils.longWaitInSeconds(2); // wait for given order details to display
			return cust_status;
		} catch (Exception e) {
			logger.error("Error in finding the order id " + e);
			TestListeners.extentTest.get().fail("Error in finding the order id " + e);
		}
		return cust_status;
	}

	public void deleteConsole(String consoleName) {
		deactivate = driver
				.findElement(By.xpath("//td[contains(text(),'temp')]/../td[4]/a[1]".replace(temp, consoleName)));
		deactivate.click();
		// deactivate=utils.getLocator("locationPage.deactivateconsole.replace(temp)",
		// consoleName);
		// deactivate.click();
		driver.switchTo().alert().accept();
		logger.info("Console is deactivated");
		utils.waitTillPagePaceDone();
		WebElement delete = driver.findElement(By.xpath("//a[@data-method='delete']"));
		// utils.StaleElementclick(driver, delete);
		delete.click();
		driver.switchTo().alert().accept();
		utils.logit("Console is deleted");
	}

	public void deletenewLocation() {
		utils.getLocator("locationPage.locationOperationsDrp").click();
		utils.getLocator("locationPage.locationOperationsDrpList").click();
		driver.switchTo().alert().accept();
		logger.info("location deleted");
	}

	public void OrderstatustoDelayfromConsole() {
		utils.getLocator("pickupConsolePage.actionMenu").isDisplayed();
		utils.getLocator("pickupConsolePage.actionMenu").click();
		utils.getLocator("pickupConsolePage.actionMenu-DelayStatus").click();
		utils.getLocator("pickupConsolePage.confirmDelayStatus").click();
		logger.info("Order status changed to Delayed");

	}

	public void OrderstatustoReadyfromConsole() {
		utils.getLocator("pickupConsolePage.actionMenu").isDisplayed();
		utils.getLocator("pickupConsolePage.actionMenu").click();
		utils.getLocator("pickupConsolePage.actionMenu-ReadyStatus").click();
		utils.getLocator("pickupConsolePage.confirmReadyStatus").click();
		logger.info("Order status changed to Ready");

	}

	// Clicks on elipsis button on All Store Locations page
	public void clickElipsisButton() {
		utils.getLocator("locationPage.ellipsisButton").isDisplayed();
		utils.getLocator("locationPage.ellipsisButton").click();
	}

	/*
	 * Checks whether the List contains at least one location that is containing
	 * given name and is an approved and disapproved location
	 */
	public boolean verifyApprovedAndDisapprovedLocations(List<Map<String, String>> outputFileContent,
			String locNamePart) {
		boolean isVerified = false;
		String approvedLocationId = null;
		String disapprovedLocationId = null;

		for (Map<String, String> entry : outputFileContent) {
			String locationId = entry.get("punchh_location_id");
			String status = entry.get("status");
			String name = entry.get("location_name");

			if (status.equals("approved") && name.contains(locNamePart) && approvedLocationId == null) {
				approvedLocationId = locationId;
			} else if (status.equals("disapproved") && name.contains(locNamePart) && disapprovedLocationId == null) {
				disapprovedLocationId = locationId;
			}
			// Break when at least one location of each condition is found
			if (approvedLocationId != null && disapprovedLocationId != null) {
				isVerified = true;
				break;
			}
		}
		utils.logit("Approved Location ID: " + approvedLocationId);
		utils.logit("Disapproved Location ID: " + disapprovedLocationId);
		return isVerified;
	}

	public void orderStatusToPickedUpFromConsole(String orderId) {
		utils.getLocator("pickupConsolePage.actionMenu").click();
		WebElement pickedUpAction = utils.getLocator("pickupConsolePage.actionMenu-PickedupStatus");
		pickedUpAction.isDisplayed();
		utils.longWaitInMiliSeconds(500);
		pickedUpAction.click();
		WebElement pickedUpConfirmBtn = utils.getLocator("pickupConsolePage.confirmPickedupStatus");
		pickedUpConfirmBtn.isDisplayed();
		utils.waitTillVisibilityOfElement(pickedUpConfirmBtn, "Confirm Button");
		utils.waitTillElementToBeClickable(pickedUpConfirmBtn);
		utils.StaleElementclick(driver, pickedUpConfirmBtn);
		// utils.clickWithActions(pickedUpConfirmBtn);
		utils.logit("Order Id " + orderId + " status changed to Picked Up");
		selUtils.longWait(3000);
		utils.getLocator("pickupConsolePage.pastOrdersTab").click();
		utils.logit("Clicked on Past Orders tab");
	}

	public void VerifyHowToUseTutorial() {
		utils.getLocator("pickupConsolePage.actionMenu").isDisplayed();
		utils.getLocator("pickupConsolePage.actionMenu").click();
		logger.info("Tutorial video page is opened");
	}

	public void verifyToAuditLogNavigation(String expheadersText) {
		utils.getLocator("locationPage.locationHeading").isDisplayed();
		utils.getLocator("locationPage.ellipsisButton").isDisplayed();
		utils.getLocator("locationPage.ellipsisButton").click();
		utils.getLocator("locationPage.auditLogLink").isDisplayed();
		utils.getLocator("locationPage.auditLogLink").click();
		String actualText = utils.getLocator("locationPage.auditLogHeading").getText();
		Assert.assertEquals(actualText, expheadersText);
		logger.info(
				"Sucessfully verfied that clicking on the Audit log within a location page shows audit log for that location.");
		TestListeners.extentTest.get().pass(
				"Sucessfully verfied that clicking on the Audit log within a location page shows audit log for that location.");
	}

	public String clickOnSelectedLocation(String locationName) {
		// selUtils.implicitWait(50);
		utils.getLocator("locationPage.searchBar").click();
		utils.getLocator("locationPage.searchBar").clear();
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		utils.longWaitInSeconds(4); // using hard wait because location search functionality has auto search feature
//		utils.getLocator("locationPage.searchBar").sendKeys(Keys.ENTER);
		logger.info("Clicked on search bar and entered location name");
		String location = utils.getLocator("locationPage.clickSearchedLocation").getText();
		Assert.assertEquals(locationName, location, "Location Name didn't match");
		utils.getLocator("locationPage.clickSearchedLocation").click();
//		driver.findElement(By.xpath("//a[normalize-space()='temp']".replace(temp, locationName))).click();
		String result = utils.getLocator("locationPage.allowLocationForMultipleRedemptionFlag").getAttribute("checked");
		return result;
	}

	public void clickOnLocationName(String locationName) {
		utils.getLocator("locationPage.searchBar").clear();
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		utils.getLocator("locationPage.searchBar").sendKeys(Keys.ENTER);
		logger.info("Clicked on search bar and entered location name: " + locationName);
		TestListeners.extentTest.get().info("Clicked on search bar and entered location name: " + locationName);
		String xpath = utils.getLocatorValue("locationPage.locationName").replace("temp", locationName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		utils.clickByJSExecutor(driver, ele);
		// driver.findElement(By.xpath("//a[normalize-space()='temp']".replace(temp,
		// locationName))).click();
	}

	public void createLocationGroup(String locationName, String locationGroupName) throws InterruptedException {

		// click on add new location group button
		utils.getLocator("locationPage.addNewLocationGroupButton").click();
		utils.getLocator("locationPage.locationSearchBarBox").sendKeys(locationName);
		selUtils.longWait(2000);

		String locationNameCheckBoxXpath = utils.getLocatorValue("locationPage.locationNameCheckBox")
				.replace("${locationName}", locationName);
		driver.findElement(By.xpath(locationNameCheckBoxXpath)).click();
		utils.getLocator("locationPage.createLocationGroupButton").click();
		utils.getLocator("locationPage.locationGroupInputBox").clear();
		utils.getLocator("locationPage.locationGroupInputBox").sendKeys(locationGroupName);
		utils.getLocator("locationPage.locationGroupConfirmButton").click();
		utils.waitTillPagePaceDone();
	}

	public void searchAndClickOnLocation(String locationName) {

		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		selUtils.longWait(2000);
		logger.info("Clicked on search bar and entered location name");
		driver.findElement(By.xpath(utils.getLocatorValue("locationPage.locationName").replace(temp, locationName)))
				.click();
	}

	public String getErrorSuccessMessage() {
		utils.waitTillPagePaceDone();
		String errorMessge = "";
		errorMessge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return errorMessge;
	}

	public void deleteLocationGroup(String locGroupName) throws InterruptedException {
		utils.getLocator("locationPage.locationSearchBox").sendKeys(locGroupName);
		Thread.sleep(2000);
		WebElement wEle = driver.findElement(
				By.xpath("//h3[text()='" + locGroupName + "']/following-sibling::div/ul/li/a[@title='Edit']/i"));
		utils.clickByJSExecutor(this.driver, wEle);
		utils.getLocator("locationPage.locationGroupDeleteButton").click();
		driver.switchTo().alert().accept();
	}

	public boolean isDisplayed(String str) {
		return utils.getLocator(str).isDisplayed();
	}

	public List<String> getTableHeadings() {
		List<WebElement> lst = utils.getLocatorList("pickupConsolePage.tableHeadings");
		List<String> valueList = new ArrayList<>();
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			valueList.add(text);
			// System.out.print(text);
		}
		return valueList;
	}

	public void orderCheckBox() {
		utils.getLocator("pickupConsolePage.checkBox").click();

		selUtils.longWait(1000);
		utils.getLocator("pickupConsolePage.readyForCustomerButton").click();

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
		wait.until(ExpectedConditions
				.presenceOfElementLocated(By.xpath(utils.getLocatorValue("pickupConsolePage.confirmButton"))));
		utils.getLocator("pickupConsolePage.confirmButton").click();
	}

	public boolean selectReadyUsingOrderId(String orderID) {
		selUtils.longWait(350000);
		utils.refreshPage();
		utils.implicitWait(5);
		utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderID);
		selUtils.longWait(2000);
		utils.getLocator("pickupConsolePage.checkBox").click();
		selUtils.longWait(1000);

		String oldStatus = utils.getLocator("pickupConsolePage.orderstatusTextBox").getText();

		utils.getLocator("pickupConsolePage.readyForCustomerButton").click();
		selUtils.longWait(1000);
		utils.getLocator("pickupConsolePage.confirmButton").click();
		selUtils.longWait(2000);
		String newStatus = utils.getLocator("pickupConsolePage.orderstatusTextBox").getText();

		return oldStatus.equals(newStatus);
	}

	public List<String> singleScanTypeTypeDrpDownList() {
		List<WebElement> lst = utils.getLocatorList("locationPage.singleScanType1");
		List<String> value = new ArrayList<String>();
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			if (text == "") {
				continue;
			}
			if (text == "Long token") {
				utils.selectDrpDwnValue(lst.get(i), text);
			}
			value.add(text);
		}
		return value;
	}

	public void clickTopLocation() {
		List<WebElement> ele = utils.getLocatorList("locationPage.topLocation");
		ele.get(0).click();
		utils.waitTillPagePaceDone();
		logger.info("clicked the top location on the location Page");
		TestListeners.extentTest.get().info("clicked the top location on the location Page");
	}

	public void newLocationWithBusiness(String locationName, String storeNumber, String hours) {

		utils.getLocator("locationPage.newLoctionButton").click();
		logger.info("Clicked on New location");

		utils.getLocator("locationPage.nameTextBox").sendKeys(locationName);
		utils.getLocator("locationPage.storeNumberTextBox").sendKeys(storeNumber);
		utils.getLocator("locationPage.addressTextBox").sendKeys("Test Address");
		utils.getLocator("locationPage.cityTextBox").sendKeys("Test City");
		utils.getLocator("locationPage.stataTextBox").sendKeys("Test state");
		utils.getLocator("locationPage.postcodeTextBox").sendKeys("10012");
		utils.getLocator("locationPage.addLoctionButton").click();

		utils.waitTillElementToBeClickable(utils.getLocator("locationPage.mobileAppTab"));
		utils.getLocator("locationPage.mobileAppTab").click();

		utils.getLocator("locationPage.workingHoursField").clear();
		utils.getLocator("locationPage.workingHoursField").sendKeys(hours);
		utils.getLocator("locationPage.addLoctionButton").click();
		logger.info("Clicked on add location");
		TestListeners.extentTest.get().info("Clicked on add location");
		// return locationName;
	}

	public void searchAndDeleteLocation(String locationName) {
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		logger.info("Clicked on search bar and entered location name");
		utils.waitTillPagePaceDone();
		clickTopLocation();
		deletenewLocation();
	}

	public void locationOperation(String operation) {
		utils.getLocator("locationPage.clickOnLocationOperation").click();
		List<WebElement> ele = utils.getLocatorList("locationPage.selectLocationOperation");
		utils.selectListDrpDwnValue(ele, operation);
		utils.acceptAlert(driver);
		logger.info("Selected service in the Location Operation is " + operation);
		TestListeners.extentTest.get().info("Selected service in the Location Operation is " + operation);
	}

	public void locationScoreboardReport(String dateRannge) {
		utils.longWaitInSeconds(10);
//		utils.getLocator("locationPage.dateRangeLocationScoreboard").isDisplayed();
//		utils.getLocator("locationPage.dateRangeLocationScoreboard").click();
//		List<WebElement> ele = utils.getLocatorList("locationPage.listOfDateRangeLocationScoreboard");
//		utils.selectListDrpDwnValue(ele, dateRannge);
//		logger.info("Clicked on Date Range " + dateRannge);
//		TestListeners.extentTest.get().info("Clicked on Date Range " + dateRannge);
		utils.getLocator("locationPage.exportLocationScoreboard").click();
		logger.info("Clicked on Export Location Scoreboard");
		TestListeners.extentTest.get().info("Clicked on Export Location Scoreboard");
	}

	public boolean selectReadyOrderId(String orderID) {
		int i = 0;
		boolean flag = true;

		do {
			try {
				utils.waitTillVisibilityOfElement(utils.getLocator("pickupConsolePage.ordersearchBox"), "");
				utils.getLocator("pickupConsolePage.ordersearchBox").clear();
				utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderID);
				utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(Keys.ENTER);
				utils.longWaitInSeconds(1);
				String xpath = utils.getLocatorValue("pickupConsolePage.orderIdVisible").replace("{orderID}", orderID);
				if (driver.findElement(By.xpath(xpath)).isDisplayed()) {
					logger.info("order ID is  visible");
					TestListeners.extentTest.get().info("order ID is visible ");
					flag = false;
					break;
				}
			} catch (Exception e) {
				utils.longWaitInSeconds(2);
				i++;
				utils.refreshPage();
				utils.waitTillPaceDataProgressComplete();
				logger.info("order is not visible again searching");
				TestListeners.extentTest.get().info("order is not visible again searching");
			}
		} while (i < 30 && flag);

		utils.waitTillVisibilityOfElement(utils.getLocator("pickupConsolePage.checkBox"), "");
		utils.getLocator("pickupConsolePage.checkBox").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("pickupConsolePage.orderstatusTextBox"), "");
		String oldStatus = utils.getLocator("pickupConsolePage.orderstatusTextBox").getText();
		utils.getLocator("pickupConsolePage.readyForCustomerButton").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("pickupConsolePage.confirmButton"), "confirm button");
		utils.getLocator("pickupConsolePage.confirmButton").click();
		utils.longwait(1000);
		utils.waitTillVisibilityOfElement(utils.getLocator("pickupConsolePage.orderstatusTextBox"), "");
		String newStatus = utils.getLocator("pickupConsolePage.orderstatusTextBox").getText();
		boolean value = oldStatus.equals(newStatus);

		logger.info("Verfied able to change the order status");
		TestListeners.extentTest.get().pass("Verfied able to change the order status");
		return value;
	}

	public void searchAndSelectLocation(String locationName) throws InterruptedException {
		utils.waitTillPagePaceDone();
		utils.getLocator("locationPage.searchBar").clear();
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		utils.getLocator("locationPage.searchBar").sendKeys(Keys.ENTER);
		utils.waitTillPagePaceDone();
		Thread.sleep(1000);
		logger.info("Clicked on search bar and entered location name");
		TestListeners.extentTest.get().info("Clicked on search bar and entered location name");
		clickTopLocation();
	}

	// Search given Location and return the value in given column
	public String getLocationStatus(String locationName, String columnName) {
		String columnValue, index = "";
		utils.waitTillPagePaceDone();
		WebElement searchBar = utils.getLocator("locationPage.searchBar");
		searchBar.clear();
		searchBar.sendKeys(locationName);
		searchBar.sendKeys(Keys.ENTER);
		utils.longWaitInSeconds(3);
		logger.info("Clicked on search bar and entered location name");
		TestListeners.extentTest.get().info("Clicked on search bar and entered location name");
		if (columnName.equals("City")) {
			index = "2";
		} else if (columnName.equals("Status")) {
			index = "6";
		} else if (columnName.equals("Tax Rate (%)")) {
			index = "7";
		} else {
			throw new IllegalArgumentException("Unsupported column name, please check: " + columnName);
		}
		String xpath = utils.getLocatorValue("locationPage.locationStatus").replace("$locationName", locationName)
				.replace("$index", index);
		boolean isLocationPresent = pageObj.gamesPage().isPresent(xpath);
		if (!isLocationPresent) {
			utils.logit("Location '" + locationName + "' is not found");
			return "Location Not Found";
		} else {
			columnValue = driver.findElement(By.xpath(xpath)).getText();
			utils.logit("Location '" + locationName + "' has " + columnName + ": " + columnValue);
			return columnValue;
		}
	}

	public void VerifyCustomerStatus(String cus_status) throws InterruptedException {
		String customerstatus = utils.getLocator("pickupConsolePage.customerstatusTextBox").getText();
		Assert.assertEquals(customerstatus, cus_status);
		logger.info("Customer status for this order was changed to " + customerstatus);
		TestListeners.extentTest.get().pass("Customer status for this order was changed to " + customerstatus);

	}

	public void SelectPOSDisplayGuestIdentityTypeAsQRCode() throws InterruptedException {
		utils.getLocator("locationPage.posTabLink").click();
		utils.getLocator("locationPage.DisplayGuestIdentityasDrpdown").click();
		utils.getLocator("locationPage.DisplayGuestIdentityasDrpdownlist1").click();
		utils.getLocator("locationPage.LocationtabUpdate").click();
		logger.info("Selected QR code as Display Guest Identity type");
		TestListeners.extentTest.get().pass("Selected QR code as Display Guest Identity type");

	}

	public void SelectPOSDisplayGuestIdentityTypeAsBarCode() throws InterruptedException {
		utils.getLocator("locationPage.posTabLink").click();
		utils.getLocator("locationPage.DisplayGuestIdentityasDrpdown").click();
		utils.getLocator("locationPage.DisplayGuestIdentityasDrpdownlist2").click();
		utils.getLocator("locationPage.LocationtabUpdate").click();
		logger.info("Selected Bar code as Display Guest Identity type");
		TestListeners.extentTest.get().pass("Selected Bar code as Display Guest Identity type");

	}

	public void setLocationStatus(String locationName, String statusToBeSet) throws InterruptedException {
		utils.waitTillPagePaceDone();
		String presentStatus = getLocationStatus(locationName, "Status");

		if (presentStatus.equalsIgnoreCase(statusToBeSet)) {
			logger.info("Location Status is already set as : " + statusToBeSet + " , so do not change");
			TestListeners.extentTest.get()
					.pass("Location Status is already set as : " + statusToBeSet + " , so do not change");
		} else if (presentStatus.equalsIgnoreCase("Approved") && statusToBeSet.equalsIgnoreCase("Disapproved")) {
			clickTopLocation();
			locationOperation("Disable for all further loyalty checkins");
			utils.waitTillPagePaceDone();
			logger.info("Location status is approved and user want to disapprove it , is successful");
			TestListeners.extentTest.get()
					.pass("Location status is approved and user want to disapprove it, is successful");
		} else if (presentStatus.equalsIgnoreCase("Disapproved") && statusToBeSet.equalsIgnoreCase("Approved")) {
			clickTopLocation();
			locationOperation("Enable for loyalty checkins");
			utils.acceptAlert(driver);
			utils.waitTillPagePaceDone();
			logger.info("Location status is disapproved and user want to approve it, is successful");
			TestListeners.extentTest.get()
					.pass("Location status is disapproved and user want to approve it, is successful");
		}
	}

	public String getCustomerStatus(String orderId, String status) {
		String val = "";
		int attempts = 0;
		while (attempts <= 10) {
			utils.getLocator("pickupConsolePage.ordersearchBox").clear();
			utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderId);
			utils.longWaitInSeconds(1);
			val = utils.getLocator("pickupConsolePage.customerstatusTextBox").getText();
			try {
				if (val.equalsIgnoreCase(status)) {
					logger.info("customer status " + status + " matched");
					TestListeners.extentTest.get().pass("customer status " + status + " matched");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present customer status did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present customer status did not matched... polling count is : " + attempts);
			}
			utils.longWaitInSeconds(1);
			attempts++;
		}

		return val;
	}

	public String getOrderType(String orderId, String status) {
		String val = "";
		int attempts = 0;
		while (attempts <= 10) {
			utils.getLocator("pickupConsolePage.ordersearchBox").clear();
			utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderId);
			utils.longWaitInSeconds(1);
			val = utils.getLocator("pickupConsolePage.ordertypeTextBox").getText().toLowerCase();
			try {
				if (val.equalsIgnoreCase(status)) {
					logger.info("customer status " + status + " matched");
					TestListeners.extentTest.get().pass("customer status " + status + " matched");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present customer status did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present customer status did not matched... polling count is : " + attempts);
			}
			utils.longWaitInSeconds(1);
			attempts++;
		}

		return val;
	}

	public String getOrderStatus(String orderId, String status) {
		String val = "";
		int attempts = 0;
		while (attempts <= 10) {
			utils.getLocator("pickupConsolePage.ordersearchBox").clear();
			utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderId);
			utils.longWaitInSeconds(1);
			val = utils.getLocator("pickupConsolePage.orderstatusTextBox").getText();
			try {
				if (val.equalsIgnoreCase(status)) {
					logger.info("customer status " + status + " matched");
					TestListeners.extentTest.get().pass("customer status " + status + " matched");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present customer status did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present customer status did not matched... polling count is : " + attempts);
			}
			utils.longWaitInSeconds(1);
			attempts++;
		}

		return val;
	}

	public String getOrderStatusPastOrders(String orderId, String status) {
		String val = "";
		int attempts = 0;
		while (attempts <= 10) {
			utils.getLocator("pickupConsolePage.ordersearchBox").clear();
			utils.getLocator("pickupConsolePage.ordersearchBox").sendKeys(orderId);
			utils.longWaitInSeconds(1);
			val = utils.getLocator("pickupConsolePage.orderstatusTextBoxPastOrders").getText();
			try {
				if (val.equalsIgnoreCase(status)) {
					logger.info("customer status " + status + " matched");
					TestListeners.extentTest.get().pass("customer status " + status + " matched");
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present customer status did not matched... polling count is : " + attempts);
				TestListeners.extentTest.get().info(
						"Element is not present customer status did not matched... polling count is : " + attempts);
			}
			utils.longWaitInSeconds(1);
			attempts++;
		}

		return val;
	}

	public void clickImportPARCommerceConfigurations() {
		utils.longWaitInSeconds(2);
		utils.getLocator("locationPage.importParCommerceConfigBtn").click();
		utils.longWaitInSeconds(1);

	}

	public void bulkImportLocationParWalletConfig(String name, String recipientEmail, String csvFilePath) {
		pageObj.utils().waitTillElementToBeClickable(
				pageObj.utils().getLocator("locationPage.bulkImportLocationParWalletNameField"));
		utils.getLocator("locationPage.bulkImportLocationParWalletNameField").sendKeys(name);
		utils.getLocator("locationPage.bulkImportLocationParWalletEmailField").sendKeys(recipientEmail);
		utils.longWaitInSeconds(1);
		utils.uploadFile(driver, utils.getLocator("locationPage.bulkImportParWalletBrowseBtn"), csvFilePath);
		utils.longWaitInSeconds(2);
		utils.getLocator("locationPage.bulkImportParWalletbrowseBtnUploadBtn").click();
		utils.longWaitInSeconds(2);
	}

	public Boolean getBulkImportStatus(String bulkImportName) {
		try {
			WebElement bulkImportXpathByName = driver.findElement(By.xpath(
					utils.getLocatorValue("locationPage.bulkImportStatus").replace("$bulkImportName", bulkImportName)));
			return bulkImportXpathByName.isDisplayed();
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	public void selectTabOnEditLocation(String tabName) {

		WebElement tabXpath = driver.findElement(
				By.xpath(utils.getLocatorValue("locationPage.tabOnEditLocation").replace("$tabName", tabName)));
		tabXpath.click();
		utils.longWaitInSeconds(2);

	}

	public Map<String, String> getLocationParCommerceGiftCardConfig() {
		Map<String, String> config = new HashMap<>();
		config.put("vendorLocationId", utils.getLocator("locationPage.vendorLocationIdField").getAttribute("value"));
		config.put("vendorLocationStatus", utils.getLocator("locationPage.vendorLocationStatus").getAttribute("value"));
		return config;
	}

	public void archiveLocationGroup(String locGroupName) {
		utils.getLocator("locationPage.locationSearchBox").sendKeys(locGroupName);
		String xpath = utils.getLocatorValue("locationPage.locationGroupName").replace("$locationGroupName",
				locGroupName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		ele.click();
		utils.getLocator("locationPage.ellipsisButton").isDisplayed();
		utils.getLocator("locationPage.ellipsisButton").click();
		utils.getLocator("locationPage.archiveLocationGroupLink").click();
		driver.switchTo().alert().accept();
		logger.info(locGroupName + " location group is archived successfully");
		TestListeners.extentTest.get().info(locGroupName + " location group is archived successfully");
	}

	public boolean verifyLocationGroupArchivedOrNot(String locationGroupName) {
		boolean flag = false;
		try {
			String xpath = utils.getLocatorValue("locationPage.archivedLocationGroup").replace("$locationGroupName",
					locationGroupName);
			WebElement archivedLocationGroup = driver.findElement(By.xpath(xpath));
			archivedLocationGroup.isDisplayed();
			flag = true;
		} catch (Exception e) {
			logger.info("Location group is not archived");
			TestListeners.extentTest.get().info("Location group is not archived");
		}
		return flag;
	}

	public int getLocationGroupCount() {
		List<WebElement> listOfLocationGroup = utils.getLocatorList("locationPage.listOfLocationGroupXpath");
		int count = listOfLocationGroup.size();
		logger.info("listOfLocationGroup size -- " + count);
		return count;
	}

	public void selectAddnewLocationGruoup() {
		WebElement AddNewLocationGroupBtn = utils.getLocator("locationPage.addNewLocationGroupButton");
		AddNewLocationGroupBtn.click();

	}

	public void searchLocationOnLocationGroupPage(String locationName) {
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		utils.getLocator("locationPage.searchButton").click();
		logger.info("Clicked on search bar and entered location name");
	}

	// Fills the import location group form and submits
	public String[] importLocationGroup(String filePath) {
		String requestName = CreateDateTime.getUniqueString("LocationImport_");
		String locationGroupName = CreateDateTime.getUniqueString("LocationGroup_");
		utils.getLocator("locationPage.requestName").clear();
		utils.getLocator("locationPage.requestName").sendKeys(requestName);
		utils.getLocator("locationPage.groupName").clear();
		utils.getLocator("locationPage.groupName").sendKeys(locationGroupName);
		utils.getLocator("locationPage.browseButton").sendKeys(filePath);
		utils.getLocator("locationPage.submitButton").click();
		utils.waitTillPagePaceDone();
		utils.logit("Submit button is clicked with request name: " + requestName + " and location group name: "
				+ locationGroupName);
		return new String[] { requestName, locationGroupName };
	}

	// Clicks on Bulk Action button on location group page
	public void clickBulkActionButton() {
		utils.getLocator("locationPage.bulkActionButton").click();
	}

	// Clicks on import button on location group page
	public void clickImportButton() {
		utils.getLocator("locationPage.importButton").click();
	}

	// Clicks on 'here' link to download sample file
	public void clickHereLink() {
		utils.getLocator("locationPage.locUploadSampleFileLink").click();
		utils.longWaitInSeconds(2); // Wait for download to complete
	}

	// Fills the import locations form and submits
	public String importLocation(String filePath) {
		String requestName = CreateDateTime.getUniqueString("Location Import ");
		utils.getLocator("locationPage.bulkLocUploadName").clear();
		utils.getLocator("locationPage.bulkLocUploadName").sendKeys(requestName);
		utils.getLocator("locationPage.browseButton").sendKeys(filePath);
		utils.getLocator("locationPage.submitButton").click();
		utils.waitTillPagePaceDone();
		utils.logit("Submit button is clicked");
		return requestName;
	}

	// Overloaded method of importLocation when requestName is given
	public void importLocation(String filePath, String requestName) {
		utils.getLocator("locationPage.bulkLocUploadName").clear();
		utils.getLocator("locationPage.bulkLocUploadName").sendKeys(requestName);
		utils.getLocator("locationPage.browseButton").sendKeys(filePath);
		utils.getLocator("locationPage.submitButton").click();
		utils.waitTillPagePaceDone();
		utils.logit("Submit button is clicked");
	}

	// Extract any string based on regex pattern
	public static String extractUsingRegex(String text, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new IllegalArgumentException("Could not find a match for regex '" + regex + "' in text: " + text);
		}
	}

	// Gets the value of a particular column in CSV uploads table
	public String getCSVUploadsColumnValue(String locationGroupName, String locationIndex) {
		// Index example: 1=Status, 2=Error
		String valueXpath = utils.getLocatorValue("locationPage.csvUploadsColumnValue")
				.replace("name", locationGroupName).replace("index", locationIndex);
		int attempts = 0;
		int max_attempts = 10;
		String text = "";
		while (attempts < max_attempts) {
			utils.logit("Attempt number: " + (attempts + 1));
			WebElement valueElement = driver.findElement(By.xpath(valueXpath));
			text = valueElement.getText();
			if (!text.equals("Processing")) {
				break;
			}
			utils.refreshPage();
			attempts++;
		}
		utils.logit("CSV Uploads table row value: " + text);
		return text;
	}

	// Searches for a location group and returns its ID
	public String searchLocationGroupAndGetId(String locationGroupName) {
		WebElement searchBox = utils.getLocator("locationPage.locationSearchBox");
		searchBox.clear();
		searchBox.sendKeys(locationGroupName);
		String locGroupXpath = utils.getLocatorValue("locationPage.locGroupName").replace("temp", locationGroupName);
		WebElement locGroupElement = driver.findElement(By.xpath(locGroupXpath));
		locGroupElement.isDisplayed();
		String locGroupCardEditButtonXpath = utils.getLocatorValue("locationPage.locGroupCardEditButton")
				.replace("temp", locationGroupName);
		String href = driver.findElement(By.xpath(locGroupCardEditButtonXpath)).getAttribute("href");
		String locGroupId = href.replaceAll("\\D+", "");
		TestListeners.extentTest.get().info("Location group ID of " + locationGroupName + ": " + locGroupId);
		logger.info("Location group id of " + locationGroupName + ": " + locGroupId);
		return locGroupId;
	}

	// Downloads the CSV and returns its file name
	public String downloadCSVFile(String linkName) {
		String linkXpath = utils.getLocatorValue("locationPage.csvDownloadLink").replace("temp", linkName);
		WebElement linkElement = driver.findElement(By.xpath(linkXpath));
		String href = linkElement.getAttribute("href");
		String csvName = href.substring(href.lastIndexOf('/') + 1);
		linkElement.click();
		TestListeners.extentTest.get().info("Clicked on '" + linkName + "' link to download: " + csvName);
		logger.info("Clicked on '" + linkName + "' link to download: " + csvName);
		utils.longWaitInSeconds(2);
		return csvName;
	}

	public String CheckNoresultFoundFOrInvalidLocation() {
		WebElement ele = utils.getLocator("locationPage.noResultFound");
		String result = ele.getText();
		return result;
	}

	public void seletctAllLocationsgroupsCheckbox() {
		WebElement selectAllLocationsGroups = utils.getLocator("locationPage.selectAllLocationsGroupsCHeckBox");
		selectAllLocationsGroups.click();
	}

	public boolean checkCretateLocationGroupBtnDisabled() {
		utils.longWaitInMiliSeconds(500);
		WebElement createLocationGroupBtn = utils.getLocator("locationPage.createLocationGroupButton");
		// Check if disabled attribute exists
		boolean isEnabled = createLocationGroupBtn.isEnabled();
		return isEnabled;
	}

	public String createLocationGroup(String locationGroupName) {
		WebElement createLocationGroupBtn = utils.getLocator("locationPage.createLocationGroupButton");
		createLocationGroupBtn.click();
		utils.getLocator("locationPage.locationGroupInputBox").clear();
		utils.getLocator("locationPage.locationGroupInputBox").sendKeys(locationGroupName);
		utils.getLocator("locationPage.locationGroupConfirmButton").click();
		utils.waitTillPagePaceDone();
		String successMessage = utils.getLocator("instanceCommonElements.successMessage").getText();
		return successMessage;
	}

	public void searchLocationGroup(String locationGroupName) {
		WebElement searchBox = utils.getLocator("locationPage.searchLocationGroupBox");
		searchBox.clear();
		searchBox.sendKeys(locationGroupName);
		utils.longWaitInMiliSeconds(500);
		searchBox.sendKeys(Keys.ENTER);
		// utils.getLocator("locationPage.searchButton").click();
		utils.logit("Clicked on search bar and entered Location Group name");
	}

	public void archiveLocationsGroup() {
		WebElement ele = utils.getLocator("locationPage.archiveLGBtn");
		ele.click();
		utils.acceptAlert(driver);
		logger.info("Location group is archived successfully");
	}

	public String createNewLocation(String storeNumber, String State) {

		utils.getLocator("locationPage.newLoctionButton").click();
		logger.info("Clicked on New location");

		locationName = " " + "Automation Location" + CreateDateTime.getTimeDateString();
		utils.getLocator("locationPage.nameTextBox").sendKeys(locationName);
		utils.getLocator("locationPage.storeNumberTextBox").sendKeys(storeNumber);
		utils.getLocator("locationPage.addressTextBox").sendKeys("Test Address");
		utils.getLocator("locationPage.cityTextBox").sendKeys("Test City");
		utils.getLocator("locationPage.stataTextBox").sendKeys(State);
		utils.getLocator("locationPage.postcodeTextBox").sendKeys("10012");
		utils.getLocator("locationPage.addLoctionButton").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("locationPage.createLocationSuccessMessage").isDisplayed();
		return locationName;
	}

	public boolean flagValue(String flagName) {
		Boolean flag;
		selUtils.longWait(3);
		WebElement enableSTOCheckbox = driver
				.findElement(By.xpath("//label[text()='" + flagName + "']/preceding-sibling::input[1]"));
		String checkBoxValue = enableSTOCheckbox.getAttribute("checked");

		if (checkBoxValue == null) {
			return false;
		}

		else {
			return true;
		}
	}

	public void clickOnSelectedLocationAndEnableMultipleRedemption(String locationName) {
		// selUtils.implicitWait(50);
		utils.getLocator("locationPage.searchBar").click();
		utils.getLocator("locationPage.searchBar").clear();
		utils.getLocator("locationPage.searchBar").sendKeys(locationName);
		utils.longWaitInSeconds(4); // using hard wait because location search functionality has auto search feature
//		utils.getLocator("locationPage.searchBar").sendKeys(Keys.ENTER);
		logger.info("Clicked on search bar and entered location name");
		String location = utils.getLocator("locationPage.clickSearchedLocation").getText();
		Assert.assertEquals(locationName, location, "Location Name didn't match");
		utils.getLocator("locationPage.clickSearchedLocation").click();
//		driver.findElement(By.xpath("//a[normalize-space()='temp']".replace(temp, locationName))).click();
		pageObj.dashboardpage()
				.checkBoxFlagOnOffAndClick("location_location_extra_attributes_enable_multiple_redemptions", "check");
		pageObj.dashboardpage().updateButton();
	}
}
