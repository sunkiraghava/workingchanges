package com.punchh.server.pages;

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
public class AwaitingMigrationPage {
	static Logger logger = LogManager.getLogger(AwaitingMigrationPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	PageObj pageObj;

	public AwaitingMigrationPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void createNewMigrationGuest(String email, String timeStamp,String location, String gender) {
		logger.info("Creating new migration user");
		TestListeners.extentTest.get().info("Creating new migration user");
		addMigrationUserDetails(email,timeStamp,location,gender);
		utils.getLocator("awaitingMigration.emailCheckbox").click();
		utils.getLocator("awaitingMigration.pnCheckbox").click();
		utils.getLocator("awaitingMigration.saveButton").click();
		utils.getLocator("awaitingMigration.successMessage").isDisplayed();
	}

	public boolean verifyMigrationUser(String email, String timeStamp) {
		boolean result = false;
		int attempts = 0;
		while (attempts < 5) {
			try {
				utils.refreshPage();
				utils.getLocator("awaitingMigration.searchTextbox").clear();
				utils.getLocator("awaitingMigration.searchTextbox").sendKeys(email);
				utils.getLocator("awaitingMigration.searchButton").click();
				String loc = utils.getLocatorValue("awaitingMigration.emailPhoneLabel").replace("temp", email)
						.replace("index", timeStamp);
				String phoneLoc = utils.getLocatorValue("awaitingMigration.emailPhoneLabel").replace("temp / index", timeStamp);
				String nameLoc = utils.getLocatorValue("awaitingMigration.emailPhoneLabel")
						.replace("temp / index", "fName" + timeStamp + " lName" + timeStamp);

				driver.findElement(By.xpath(loc)).isDisplayed();
				driver.findElement(By.xpath(phoneLoc)).isDisplayed();
				driver.findElement(By.xpath(nameLoc)).isDisplayed();
				utils.logInfo("Migration guest is successfully created: " + email);
				result = true;
				break;

			} catch (Exception e) {
				utils.logInfo("searched Migration Guest is not present polling count " + attempts);
			}
			attempts++;
		}
		return result;
	}

	public boolean verifyMigratedGuest(String email, String timeStamp) {
		try {
			utils.getLocator("awaitingMigration.migratedSearchTextbox").isDisplayed();
			utils.getLocator("awaitingMigration.migratedSearchTextbox").clear();
			utils.getLocator("awaitingMigration.migratedSearchTextbox").sendKeys(email);
			utils.getLocator("awaitingMigration.migratedSearchButton").click();
			String loc = utils.getLocatorValue("awaitingMigration.migratedEmailPhoneLabel").replace("temp", email)
					.replace("index", timeStamp);
			driver.findElement(By.xpath(loc)).isDisplayed();
			logger.info("Guest is successfully migrated: " + email);
			TestListeners.extentTest.get().pass("Guest is successfully migrated: " + email);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void nevigateToMigrationAndSearchUser(String userEmail) {
		// pageObj.menupage().clickGuestMenu();
		utils.getLocator("awaitingMigration.clickMigratedGuest").click();
		utils.getLocator("awaitingMigration.searchMigratedUser").clear();
		utils.getLocator("awaitingMigration.searchMigratedUser").sendKeys(userEmail);
		utils.getLocator("awaitingMigration.searchMigratedUser").sendKeys(Keys.ENTER);
	}

	public boolean checkAccountHistory() {
		utils.getLocator("guestTimeLine.accountHistoryBtn").click();
		boolean flag = false;
		String var = utils.getLocator("awaitingMigration.originalPoints").getText();
		if (var.contains("30")) {
			flag = true;
			logger.info("Gift card amount  matched");
		}
		return flag;
	}

	public boolean checkPointsAfterCheckIn() {
		utils.getLocator("guestTimeLine.accountHistoryBtn").click();
		boolean flag = false;
		String var = utils.getLocator("awaitingMigration.checkInPoints").getText();
		if (var.contains("20")) {
			flag = true;
			logger.info("CheckIn points matched");
		}
		return flag;
	}

	public boolean checkPointsAfterForceRedemption(String points) {
      // utils.getLocator("guestTimeLine.accountHistoryBtn").click();
        pageObj.guestTimelinePage().navigateInsideGuestTimeline("Account History");
		boolean flag = false;
		String var = utils.getLocator("awaitingMigration.forceRedemptionPoints").getText();
		if (var.contains(points)) {
			flag = true;
			logger.info("Force Redemption points matched");
		}
		return flag;
	}

	public boolean verifyRedemption() {
		utils.getLocator("guestTimeLine.accountHistoryBtn").click();
		boolean flag = false;
		String var = utils.getLocator("awaitingMigration.verifyRedemption").getText();
		if (var.contains("+Item")) {
			flag = true;
			logger.info("Redemption successful");
		}
		return flag;
	}

	public boolean clickawaitingMigrationGuestNameLink(String userEmail) {
		boolean result = false;
		int attempts = 0;
		while (attempts < 7) {
			utils.longwait(3000);
			try {
				nevigateToMigrationAndSearchUser(userEmail);
				driver.navigate().to(driver.getCurrentUrl());
				utils.waitTillCompletePageLoad();
				utils.waitTillVisibilityOfElement(utils.getLocator("awaitingMigration.awaitingMigrationGuestNameLink"),
						"awaiting Migration Guest Name is not visible");
				result = utils.getLocator("awaitingMigration.awaitingMigrationGuestNameLink").isDisplayed();
				utils.getLocator("awaitingMigration.awaitingMigrationGuestNameLink").click();
				logger.info("Clicked searched awaiting Migration Guest Name");
				TestListeners.extentTest.get().pass("Clicked searched awaiting Migration Guest Name");
				break;
			} catch (Exception e) {
				logger.error("searched awaiting Migration Guest Name is not present " + e);
				TestListeners.extentTest.get().info("searched awaiting Migration Guest Name is not present " + e);
			}
			attempts++;
		}
		return result;
	}

	public boolean searchMigratedGuest(String userEmail) {
		utils.implicitWait(3);
		utils.getLocator("awaitingMigration.searchMigratedUserNew").sendKeys(userEmail);
		utils.getLocator("awaitingMigration.searchMigratedUserNew").sendKeys(Keys.ENTER);

		utils.waitTillPagePaceDone();

		String xpath = utils.getLocatorValue("awaitingMigration.displayUser").replace("{$useremail}", userEmail);
		if (driver.findElements(By.xpath(xpath)).size() > 0) {
			utils.implicitWait(50);
			logger.info("veriffied awaiting user is visible");
			TestListeners.extentTest.get().pass("verified awaiting user is visible");
			return true;
		}
		utils.implicitWait(50);
		logger.info("awaiting user is not visible");
		TestListeners.extentTest.get().fail("awaiting user is not visible");
		return false;
	}

	public boolean cardNumberOnMigratedGuest(String email, String cardNumber) {
		String xpath = utils.getLocatorValue("awaitingMigration.cardNumberOnMigratedGuest").replace("{email}", email)
				.replace("{cardNumber}", cardNumber);
		if (driver.findElements(By.xpath(xpath)).size() > 0) {
			logger.info("card number is visible on the migrated user");
			TestListeners.extentTest.get().info("card number is visible on the migrated user");
			return true;
		}
		logger.info("card number is not visible on the migrated user");
		TestListeners.extentTest.get().info("card number is not visible on the migrated user");
		return false;
	}
	public void addMigrationUserDetails(String email, String timeStamp,String location, String gender) {
		utils.getLocator("awaitingMigration.newMigrationGuestButton").isDisplayed();
		utils.getLocator("awaitingMigration.newMigrationGuestButton").click();
		utils.getLocator("awaitingMigration.newMigrationGuestHeading").isDisplayed();
		utils.getLocator("awaitingMigration.orignalMembershipTextbox").isDisplayed();
		utils.getLocator("awaitingMigration.orignalMembershipTextbox").clear();
		utils.getLocator("awaitingMigration.orignalMembershipTextbox").sendKeys(timeStamp);
		utils.getLocator("awaitingMigration.firstNameTextbox").isDisplayed();
		utils.getLocator("awaitingMigration.firstNameTextbox").clear();
		utils.getLocator("awaitingMigration.firstNameTextbox").sendKeys("fName" + timeStamp);
		utils.getLocator("awaitingMigration.lastNameTextbox").isDisplayed();
		utils.getLocator("awaitingMigration.lastNameTextbox").clear();
		utils.getLocator("awaitingMigration.lastNameTextbox").sendKeys("lName" + timeStamp);
		utils.getLocator("awaitingMigration.emailTextbox").clear();
		utils.getLocator("awaitingMigration.emailTextbox").sendKeys(email);
		logger.info("Migration guest email is set as: " + email);
		TestListeners.extentTest.get().info("Migration guest email is set as: " + email);
		utils.getLocator("awaitingMigration.birthdayTextbox").clear();
		utils.getLocator("awaitingMigration.birthdayTextbox").sendKeys(Utilities.getConfigProperty("dob"));
		utils.getLocator("awaitingMigration.phoneTextbox").clear();
		utils.getLocator("awaitingMigration.phoneTextbox").sendKeys(timeStamp);
		utils.getLocator("awaitingMigration.initialPointTextbox").clear();
		utils.getLocator("awaitingMigration.initialPointTextbox").sendKeys(Utilities.getConfigProperty("initialPoint"));
		utils.getLocator("awaitingMigration.orignalPointTextbox").clear();
		utils.getLocator("awaitingMigration.orignalPointTextbox").sendKeys(Utilities.getConfigProperty("orignalPoint"));
		utils.getLocator("awaitingMigration.conversionTextbox").clear();
		utils.getLocator("awaitingMigration.conversionTextbox")
				.sendKeys(Utilities.getConfigProperty("rateOfConversion"));
		utils.getLocator("awaitingMigration.zipcodeTextbox").clear();
		utils.getLocator("awaitingMigration.zipcodeTextbox").sendKeys(Integer.toString(Utilities.getRandomNo(20000)));
		utils.getLocator("awaitingMigration.addressTextbox").clear();
		utils.getLocator("awaitingMigration.addressTextbox").sendKeys(Utilities.getConfigProperty("address"));
		utils.getLocator("awaitingMigration.cityTextbox").clear();
		utils.getLocator("awaitingMigration.cityTextbox").sendKeys(Utilities.getConfigProperty("address"));
		utils.getLocator("awaitingMigration.stateTextbox").clear();
		utils.getLocator("awaitingMigration.stateTextbox").sendKeys(Utilities.getConfigProperty("stateProvinces"));
		Select sel = new Select(utils.getLocator("awaitingMigration.locationDropdown"));
		sel.selectByVisibleText(location);
		Select sel1 = new Select(utils.getLocator("awaitingMigration.genderDropdown"));
		sel1.selectByVisibleText(gender);
		logger.info("Migration user details have been added");
		TestListeners.extentTest.get().info("Migration user details have been added");
	}
	
	public void createNewMigrationGuestWithEmailAndPnSubscription(String email, String timeStamp,String location, String gender,String choiceEmail, String choicePn) {
		logger.info("Creating new migration user with email and pn subscription");
		TestListeners.extentTest.get().info("Creating new migration user with email and pn subscription");
		addMigrationUserDetails(email,timeStamp,location,gender);
		Select sel2 = new Select(utils.getLocator("awaitingMigration.marketingEmailSubscriptionDrpDwn"));
		sel2.selectByVisibleText(choiceEmail);
		Select sel3 = new Select(utils.getLocator("awaitingMigration.marketingPnSubscriptionDrpDwn"));
		sel3.selectByVisibleText(choicePn);
		utils.getLocator("awaitingMigration.saveButton").click();
		utils.getLocator("awaitingMigration.successMessage").isDisplayed();
	}

	public void migrationUserEditProfileBtn() {
		utils.getLocator("awaitingMigration.editButton").isDisplayed();
		utils.getLocator("awaitingMigration.editButton").click();
	}

	public void uploadBMUFile(String uploadName, String uploadType, String filePath) {
		utils.getLocator("awaitingMigration.importBtn").isDisplayed();
		utils.getLocator("awaitingMigration.importBtn").click();
		utils.getLocator("awaitingMigration.uploadBmuName").sendKeys(uploadName);
		logger.info("BMU Upload name set as: " + uploadName);
		TestListeners.extentTest.get().info("BMU Upload name set as: " + uploadName);
		switch (uploadType) {
			case "Browse":
			utils.getLocator("awaitingMigration.bmuUploadCsv").sendKeys(filePath);
            logger.info("Uploading BMU file: " + filePath);
            TestListeners.extentTest.get().info("Uploading BMU file: " + filePath);
            break;
            case "Filepath":
            	utils.getLocator("awaitingMigration.bmuUploadFileUrl").sendKeys(filePath);
            	logger.info("Uploading BMU file using filepath: " + filePath);
            	TestListeners.extentTest.get().info("Uploading BMU file using filepath: " + filePath);
                break;
            default:
            	logger.info("Invalid upload type provided: " + uploadType);
				TestListeners.extentTest.get().info("Invalid upload type provided: " + uploadType);
		}
        utils.getLocator("locationPage.submitButton").click();	
        utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
        }
	
	public String validateBMUSuccessMessage() throws InterruptedException {
		String val = "";
		WebElement successMsg = utils.getLocator("redeemablesPage.successMsg");
		if (successMsg.isDisplayed()) {
			val = successMsg.getText();
		}
		return val;
	}

}
