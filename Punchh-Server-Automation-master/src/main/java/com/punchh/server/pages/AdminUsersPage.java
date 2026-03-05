package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AdminUsersPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	PageObj pageObj;
	String testEmail = "tesEmail@punchh.com", firstName = "fName";

	public AdminUsersPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void verifyInviteAdminMandatoryFields() {
		utils.getLocator("adminUserPage.inviteAdminButton").isDisplayed();
		utils.getLocator("adminUserPage.inviteAdminButton").click();
		utils.getLocator("adminUserPage.inviteAdminHeading").isDisplayed();
		utils.getLocator("adminUserPage.createButton").click();
		utils.getLocator("adminUserPage.nameEmailErrorLabel").isDisplayed();
		TestListeners.extentTest.get().pass("Verfied mandatory email and first name error message");
		logger.info("Verfied mandatory email and first name error message");
		utils.getLocator("adminUserPage.firstNameTextbox").isDisplayed();
		utils.getLocator("adminUserPage.firstNameTextbox").sendKeys(firstName);
		utils.getLocator("adminUserPage.adminEmailTextbox").isDisplayed();
		utils.getLocator("adminUserPage.adminEmailTextbox").sendKeys(testEmail);
		utils.getLocator("adminUserPage.createButton").click();
		utils.getLocator("adminUserPage.roleErrorLabel").isDisplayed();
		TestListeners.extentTest.get().pass("Verfied mandatory role error message");
		logger.info("Verfied mandatory role error message");
	}

	public void clickOnAddNewAdmin() {
		utils.getLocator("adminUserPage.adminHeading").isDisplayed();
		utils.getLocator("adminUserPage.inviteAdminButton").isDisplayed();
		utils.getLocator("adminUserPage.inviteAdminButton").click();
		utils.getLocator("adminUserPage.inviteAdminHeading").isDisplayed();
	}

	public void clickCreateAdmin() {
		utils.clickByJSExecutor(driver, utils.getLocator("adminUserPage.createButton"));
//		utils.getLocator("adminUserPage.createButton").click();
		utils.getLocator("adminUserPage.adminCreationMsgLabel").isDisplayed();
		TestListeners.extentTest.get().info("Admin created message is displayed:"
				+ utils.getLocator("adminUserPage.adminCreationMsgLabel").getText());
		logger.info("Admin created message is displayed:"
				+ utils.getLocator("adminUserPage.adminCreationMsgLabel").getText());
	}

	public void configureNewAdmin(String email, String timestamp, String userRole) {
		String fName = "fname" + timestamp;
		String lName = "lname" + timestamp;
		utils.getLocator("adminUserPage.adminEmailTextbox").isDisplayed();
		utils.getLocator("adminUserPage.adminEmailTextbox").clear();
		utils.getLocator("adminUserPage.adminEmailTextbox").sendKeys(email);
		TestListeners.extentTest.get().info("Admin email is set as: " + email);
		logger.info("Admin email is set as: " + email);
		utils.getLocator("adminUserPage.firstNameTextbox").isDisplayed();
		utils.getLocator("adminUserPage.firstNameTextbox").clear();
		utils.getLocator("adminUserPage.firstNameTextbox").sendKeys(fName);
		TestListeners.extentTest.get().info("Admin first name is set as: " + fName);
		logger.info("Admin first name is set as: " + fName);
		utils.getLocator("adminUserPage.lastNameTextbox").isDisplayed();
		utils.getLocator("adminUserPage.lastNameTextbox").clear();
		utils.getLocator("adminUserPage.lastNameTextbox").sendKeys(lName);
		TestListeners.extentTest.get().info("Admin last name is set as: " + lName);
		logger.info("Admin last name is set as: " + lName);
		Select sel = new Select(utils.getLocator("adminUserPage.timezoneDropdown"));
		sel.selectByValue(Utilities.getConfigProperty("istTimezone"));
		Select selRole = new Select(utils.getLocator("adminUserPage.roleDropdown"));
		selRole.selectByVisibleText(userRole);
		TestListeners.extentTest.get().info("Admin role is set as: " + selRole.getFirstSelectedOption().getText());
		logger.info("Admin role is set as: " + selRole.getFirstSelectedOption().getText());
		List<WebElement> list = utils.getLocatorList("adminUserPage.notificationAndPermissionsCheckbox");
		if (list.size() == 12) {
			for (int i = 0; i < 10; i++) {
				selUtils.jsClick(list.get(i));
			}
		} else {
			// Assert.fail("Email notification, taublue count is not correct");
		}
	}

	public void setAdminFranchise() {
		Select selFranchise = new Select(utils.getLocator("adminUserPage.userFranchise"));
		selFranchise.selectByIndex(2);
		TestListeners.extentTest.get()
				.info("Admin franchise is set as: " + selFranchise.getFirstSelectedOption().getText());
		logger.info("Admin franchise is set as: " + selFranchise.getFirstSelectedOption().getText());
	}

	public int searchAdmin(String text) {
		utils.waitTillPagePaceDone();
		WebElement searchBox = utils.getLocator("adminUserPage.adminSearchInput");
		utils.scrollToElement(driver, searchBox);
		selUtils.clearTextUsingJS(searchBox);
		selUtils.SendKeysViaJS(searchBox, text);
		utils.getLocator("adminUserPage.searchButton").click();
		utils.waitTillPagePaceDone();
		int rowCount = utils.getLocatorList("adminUserPage.adminTableUserRows").size();
		TestListeners.extentTest.get()
				.info("Clicked on search button with text '" + text + "' and found " + rowCount + " results");
		logger.info("Clicked on search button with text '" + text + "' and found " + rowCount + " results");
		return rowCount;
	}

	public void verifyNewlyCreatedAdmin(String email, String userRole, String firstLastName) {
		utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(email);
		utils.getLocator("adminUserPage.searchButton").click();
		driver.findElement(
				By.xpath(utils.getLocatorValue("adminUserPage.searchEmailLabel").replace("temp", email.toLowerCase())))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.searchRoleLabel").replace("temp", userRole)))
				.isDisplayed();
		TestListeners.extentTest.get().pass("Successfully verfied newly created Admin" + email);
		logger.info("Successfully verfied newly created Admin" + email);
	}

	// Returns admin ID based on admin name
	public String getAdminID(String adminName) {
		String adminUserNameXpath = utils.getLocatorValue("adminUserPage.adminUserNameLink")
				.replace("temp", adminName);
		String href = driver.findElement(By.xpath(adminUserNameXpath)).getAttribute("href");
		String adminID = LocationPage.extractUsingRegex(href, "business_admins/(\\d+)");
		return adminID;
	}

	public void editAdmin(String userEmail, String timeStamp) {
		String firstLastName = "Fname" + timeStamp + " " + "Lname" + timeStamp;
		driver.findElement(
				By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName))).click();
		driver.findElement(By.xpath(
				utils.getLocatorValue("adminUserPage.editLabelHeading").replace("temp", userEmail.toLowerCase())))
				.isDisplayed();
	}

	public void updateAdminDetailsAndVerify(String userEmail, String timeStamp) {
		String userEmailUpdated = "updated" + userEmail;
		String fName = "updatedfname" + timeStamp;
		String lName = "updatedlname" + timeStamp;
		utils.getLocator("adminUserPage.adminEmailTextbox").isDisplayed();
		utils.getLocator("adminUserPage.adminEmailTextbox").clear();
		utils.getLocator("adminUserPage.adminEmailTextbox").sendKeys(userEmailUpdated);
		TestListeners.extentTest.get().info("Admin email is set as: " + userEmailUpdated);
		logger.info("Admin email is set as: " + userEmailUpdated);
		utils.getLocator("adminUserPage.firstNameTextbox").isDisplayed();
		utils.getLocator("adminUserPage.firstNameTextbox").clear();
		utils.getLocator("adminUserPage.firstNameTextbox").sendKeys(fName);
		TestListeners.extentTest.get().info("Admin first name is set as: " + fName);
		logger.info("Admin first name is set as: " + fName);
		utils.getLocator("adminUserPage.lastNameTextbox").isDisplayed();
		utils.getLocator("adminUserPage.lastNameTextbox").clear();
		utils.getLocator("adminUserPage.lastNameTextbox").sendKeys(lName);
		TestListeners.extentTest.get().info("Admin last name is set as: " + lName);
		logger.info("Admin last name is set as: " + lName);
		utils.getLocator("adminUserPage.updateButton").isDisplayed();
		utils.getLocator("adminUserPage.updateButton").click();
		utils.getLocator("adminUserPage.adminUpdateMsgLabel").isDisplayed();
	}

	public void verifyUpdatedAdmin(String userEmail, String userRole, String timeStamp) {
		String firstLastName = "Updatedfname" + timeStamp + " " + "Updatedlname" + timeStamp;
		String userEmailUpdated = "updated" + userEmail;
		utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(userEmailUpdated);
		utils.getLocator("adminUserPage.searchButton").click();
		driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.searchEmailLabel").replace("temp",
				userEmailUpdated.toLowerCase()))).isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName)))
				.isDisplayed();
		driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.searchRoleLabel").replace("temp", userRole)))
				.isDisplayed();
		TestListeners.extentTest.get().pass("Successfully verfied updated Admin" + userEmailUpdated);
		logger.info("Successfully verfied updated Admin" + userEmailUpdated);
	}

	public void verifyGenerateBusinessAdminkey(String userEmail, String timeStamp) {
		try {
			String firstLastName = "Updatedfname" + timeStamp + " " + "Updatedlname" + timeStamp;
			String userEmailUpdated = "updated" + userEmail;
			utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
			utils.getLocator("adminUserPage.searchboxTextbox").clear();
			utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(userEmailUpdated);
			utils.getLocator("adminUserPage.searchButton").click();
			driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.searchEmailLabel").replace("temp",
					userEmailUpdated.toLowerCase()))).isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName)))
					.isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName)))
					.click();
			utils.getLocator("adminUserPage.generateButtonLink").isDisplayed();
			utils.getLocator("adminUserPage.generateButtonLink").click();
			utils.getLocator("adminUserPage.apiKeyLabel").isDisplayed();
			TestListeners.extentTest.get().pass("Clicked on search");
			logger.info("Successfully verified generate business key");
			utils.getLocator("adminUserPage.updateButton").isDisplayed();
			utils.getLocator("adminUserPage.updateButton").click();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void verifyReGenerateBusinessAdminkey(String userEmail, String timeStamp) {
		String firstLastName = "Updatedfname" + timeStamp + " " + "Updatedlname" + timeStamp;
		String userEmailUpdated = "updated" + userEmail;
		utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
		utils.getLocator("adminUserPage.searchboxTextbox").clear();
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(userEmailUpdated);
		utils.getLocator("adminUserPage.searchButton").click();
		driver.findElement(
				By.xpath(utils.getLocatorValue("adminUserPage.adminNameLink").replace("temp", firstLastName))).click();
		utils.getLocator("adminUserPage.regenerateButtonLink").isDisplayed();
		utils.getLocator("adminUserPage.regenerateButtonLink").click();
		utils.getLocator("adminUserPage.apiKeyLabel").isDisplayed();
		TestListeners.extentTest.get().pass("Successfully verified regenerate business key");
		logger.info("Successfully verified regenerate business key");
	}

	public void selectLocations(String locationList) {
		selUtils.implicitWait(5);
		logger.info("Selecting locations");
		TestListeners.extentTest.get().info("Selecting locations");
		List<String> locList = Arrays.asList(locationList.split(","));
		utils.getLocator("adminUserPage.searchLocInput").isDisplayed();
		for (String str : locList) {
			utils.getLocator("adminUserPage.searchLocInput").click();
			String locStr = str.split("-")[0].trim();
//				selUtils.jsClick(driver.findElement(
//						By.xpath(utils.getLocatorValue("adminUserPage.locationOptionLabel").replace("temp", str))));
//			driver.findElement(
//					By.xpath(utils.getLocatorValue("adminUserPage.locationOptionLabel").replace("temp", str))).click();
			utils.getLocator("adminUserPage.searchLocInput").sendKeys(locStr);
			utils.waitTillVisibilityOfElement(utils.getLocator("adminUserPage.searchedLocationList"),"Location list");
			utils.getLocator("adminUserPage.searchLocInput").sendKeys(Keys.ENTER);
		}
		if (utils.getLocatorList("adminUserPage.selectedLocatorLabel").size() == locList.size()) {
			for (String str : locList) {
				// String locStr = str.split("-")[0].trim();
				driver.findElement(
						By.xpath(utils.getLocatorValue("adminUserPage.selectedLocTextLabel").replace("temp", str)));
				logger.info("Verfied Selected location: " + str);
				TestListeners.extentTest.get().pass("Verfied Selected location: " + str);
			}
		}
	}

	public void verifyLocationsInEdit(String locationList) {
		List<String> locList = Arrays.asList(locationList.split(","));
		if (utils.getLocatorList("adminUserPage.selectedLocatorLabel").size() == locList.size()) {
			for (String str : locList) {
				String locStr = str.split("-")[0].trim();
				driver.findElement(
						By.xpath(utils.getLocatorValue("adminUserPage.selectedLocTextLabel").replace("temp", locStr)));
				logger.info("Verfied Selected location: " + locStr);
				TestListeners.extentTest.get().pass("Verfied Selected location: " + locStr);
			}
		}
	}

	public void searchUser(String user) {
		utils.getLocator("adminUserPage.searchBox").sendKeys(user);
		utils.getLocator("adminUserPage.searchBox").sendKeys(Keys.ENTER);
		utils.getLocator("adminUserPage.userLink").click();

	}

	public void changeUserPermissionforCampaignBeta() {
		utils.getLocator("adminUserPage.campaignBetaAccess").click();
		utils.getLocator("adminUserPage.saveBtn").click();

	}

	public void verifyUserPermissionForAmitKumar1(String email, String userRole) {
		utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(email);
		utils.getLocator("adminUserPage.searchButton").click();
		driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.searchRoleLabel").replace("temp", userRole)))
				.click();
		String configManagement = utils.getLocator("adminUserPage.configurationManagement").getAttribute("class");
		if (configManagement.equalsIgnoreCase("alert toggle-check alert-success")) {
			utils.getLocator("adminUserPage.configurationManagement").click();
			logger.info(email + " don't have access to Configuration Management");
		} else {
			logger.info(email + " change in Configuration Management is not required");
		}
		String workflowManage = utils.getLocator("adminUserPage.workflowManagement").getAttribute("class");
		if (workflowManage.equalsIgnoreCase("alert toggle-check alert-success")) {
			utils.getLocator("adminUserPage.workflowManagement").click();
			logger.info(email + " don't have access to Workflow Management");
		} else {
			logger.info(email + " change in Workflow Management is not required");
		}
		String campBetaAccess = utils.getLocator("adminUserPage.campaignBetaAccess").getAttribute("class");
		if (campBetaAccess.equalsIgnoreCase("alert toggle-check alert-danger")) {
			utils.getLocator("adminUserPage.campaignBetaAccess").click();
			logger.info(email + "have access to Campaign Beta Access");
		} else {
			logger.info(email + " change in Campaign Beta Access is not required");
		}
		utils.getLocator("adminUserPage.saveBtn").click();
		logger.info("Verification for " + email + "have have been completed");
	}

	public void verifyUserPermissionForAmitKumar4(String email, String userRole) {
		utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(email);
		utils.getLocator("adminUserPage.searchButton").click();
		driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.searchRoleLabel").replace("temp", userRole)))
				.click();
		String configManagement = utils.getLocator("adminUserPage.configurationManagement").getAttribute("class");
		if (configManagement.equalsIgnoreCase("alert toggle-check alert-success")) {
			utils.getLocator("adminUserPage.configurationManagement").click();
			logger.info(email + " don't have access to Configuration Management");
		} else {
			logger.info(email + " change in Configuration Management is not required");
		}
		String workflowManage = utils.getLocator("adminUserPage.workflowManagement").getAttribute("class");
		if (workflowManage.equalsIgnoreCase("alert toggle-check alert-success")) {
			utils.getLocator("adminUserPage.workflowManagement").click();
			logger.info(email + " don't have access to Workflow Management");
		} else {
			logger.info(email + " change in Workflow Management is not required");
		}
		String campBetaAccess = utils.getLocator("adminUserPage.campaignBetaAccess").getAttribute("class");
		if (campBetaAccess.equalsIgnoreCase("alert toggle-check alert-danger")) {
			utils.getLocator("adminUserPage.campaignBetaAccess").click();
			logger.info(email + "have access to Campaign Beta Access");
		} else {
			logger.info(email + " change in Campaign Beta Access is not required");
		}
		utils.getLocator("adminUserPage.saveBtn").click();
		logger.info("Verification for " + email + "have have been completed");
	}

	public void selectBussinessManager(String adminEmail) {
		utils.getLocator("adminUserPage.searchboxTextbox").isDisplayed();
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(adminEmail);
		utils.getLocator("adminUserPage.searchboxTextbox").sendKeys(Keys.ENTER);
		utils.getLocator("adminUserPage.adminBusinessManagerLink").click();

	}

	public void turnPermissionoff(String element) {
		WebElement ele = driver.findElement(By.xpath("//div[@title='" + element + "']"));
		String eleStatus = ele.getAttribute("class");
		if (eleStatus.equals("alert toggle-check alert-success")) {
			utils.waitTillElementToBeClickable(ele);
			utils.StaleElementclick(driver, ele);
			logger.info("permission turned Off : " + element);
		} else {
			logger.info("permission is already Off");
		}
		utils.getLocator("adminUserPage.savebtn").click();

	}

	// shashank
	public String verifyBMUListIsExist(List<String> userNameList) {
		String userNotExist = "";
		for (String expUserName : userNameList) {
			utils.getLocator("adminUserPage.searchBox").clear();
			utils.getLocator("adminUserPage.searchBox").sendKeys(expUserName.trim());
			utils.getLocator("adminUserPage.searchBox").sendKeys(Keys.ENTER);
			List<WebElement> userWebelementList = utils.getLocatorList("adminUserPage.userSearchBoxResult");
			boolean flag = false;
			String actualEmail = "";
			for (WebElement ele : userWebelementList) {
				actualEmail = ele.getText();
				if (actualEmail.startsWith(expUserName)) {
					flag = true;
					break;
				} else {
					flag = false;
				}

			}
			if (!flag) {
				logger.info(expUserName + "user  is not uploaded in bulk upload.");
				TestListeners.extentTest.get().info(expUserName + " user is not uploaded in bulk upload.");
				userNotExist = userNotExist + expUserName;
			} else {

				logger.info(expUserName + " is uploaded successfully in bulk upload.");
				TestListeners.extentTest.get().pass(expUserName + " is uploaded successfully in bulk upload.");
			}
		}
		return userNotExist;

	}

	public void clickRole(String user, String userRole) {
		utils.getLocator("adminUserPage.searchBox").sendKeys(user);
		utils.getLocator("adminUserPage.searchBox").sendKeys(Keys.ENTER);
		utils.waitTillPagePaceDone();
		driver.findElement(By.xpath(utils.getLocatorValue("adminUserPage.clickRole").replace("$temp", userRole)))
				.click();

	}

	public void turnPermissionOn(String element) {
		WebElement ele = driver
				.findElement(By.xpath(utils.getLocatorValue("adminUserPage.permission").replace("$element", element)));
		String eleStatus = ele.getAttribute("class");
		if (eleStatus.equals("alert toggle-check alert-danger")) {
			utils.waitTillElementToBeClickable(ele);
			utils.StaleElementclick(driver, ele);
			logger.info("permission turned On : " + element);
		} else {
			logger.info("permission is already On");
		}
		utils.getLocator("adminUserPage.savebtn").click();

	}

	// Vansham :- this function will return the list of disabled permissions of the
	// role
	public List<String> verifyDisabledPermissions() {
		List<WebElement> disabledPermissions = utils.getLocatorList("adminUserPage.disabledPermissions");
		List<String> disabledPermissionsList = new ArrayList<>();
		for (WebElement disabledPermission : disabledPermissions) {
			disabledPermissionsList.add(disabledPermission.getText());
		}
		return disabledPermissionsList;
	}

	// Vansham :- this function will return the list of enabled permissions of the
	// role
	public List<String> verifyEnabledPermissions() {
		utils.waitTillPagePaceDone();
		List<WebElement> enabledPermissions = utils.getLocatorList("adminUserPage.enabledPermissions");
		List<String> enabledPermissionsList = new ArrayList<>();
		for (WebElement enabledPermission : enabledPermissions) {
			enabledPermissionsList.add(enabledPermission.getText());
		}
		return enabledPermissionsList;
	}
	public List<String> getAccessibleLocationGroupList() throws InterruptedException {
		utils.waitTillElementToBeClickable(utils.getLocator("adminUserPage.accessibleLocationGroupDrpDwn"));
		utils.getLocator("adminUserPage.accessibleLocationGroupDrpDwn").click();
		List<WebElement> elem = utils.getLocatorList("adminUserPage.accessibleLocationGroupList");
		List<String> locationList = new ArrayList<String>();
		int col = elem.size();
		for (int i = 0; i < col; i++) {
			String val = elem.get(i).getText();
			locationList.add(val);
		}
		logger.info("Accessible Location groups in dropdown are : " + locationList);
		TestListeners.extentTest.get().pass("Accessible Location groups in dropdown are : " + locationList);
		return locationList;
	}

}
