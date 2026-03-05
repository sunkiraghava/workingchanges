package com.punchh.server.pages;

import java.time.Duration;
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class SubscriptionPlansPage {
	static Logger logger = LogManager.getLogger(SubscriptionPlansPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String segmentDescription = "Test segment description";
	public String emailCount, segmentCount, pnCount, smsCount;
	private String timeZone = "(GMT+05:30) New Delhi ( IST )";

	public SubscriptionPlansPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void clickOnNewPlanButton() {
		utils.getLocator("subscriptionPlansPage.newPlanButton").click();
	}

	public void clickOnNext() {
		utils.getLocator("subscriptionPlansPage.nextButton").click();

	}

	public void uploadSubscriptionPlanImage() {
		driver.findElement(By.id("subscription_plan_subscription_image"))
				.sendKeys(System.getProperty("user.dir") + "/resources/Images/images.png");

		/*
		 * WebElement addFile =
		 * driver.findElement(By.id("subscription_plan_subscription_image")); String
		 * filePath = System.getProperty("user.dir") + "/resources/Images/images.png";
		 * logger.info(filePath); utils.uplodFile(addFile,filePath);
		 */

		/*
		 * WebElement addFile =
		 * driver.findElement(By.id("subscription_plan_subscription_image"));
		 * ((RemoteWebElement)addFile).setFileDetector(new LocalFileDetector());
		 * addFile.sendKeys(System.getProperty("user.dir") +
		 * "/resources/Images/images.png");
		 */

		/*
		 * WebElement fileInput =
		 * driver.findElement(By.id("subscription_plan_subscription_image"));
		 * ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
		 * fileInput.sendKeys(System.getProperty("user.dir") +
		 * "/resources/Images/images.png");
		 */

		/*
		 * WebElement upload =
		 * driver.findElement(By.id("subscription_plan_subscription_image"));
		 * RemoteWebElement webElement = ((RemoteWebElement) upload); LocalFileDetector
		 * detector = new LocalFileDetector(); webElement.setFileDetector(detector);
		 * File f = detector.getLocalFile(System.getProperty("user.dir") +
		 * "/resources/Images/images.png"); upload.sendKeys(f.getAbsolutePath());
		 */

		/*
		 * File uploadFile = new File(System.getProperty("user.dir") +
		 * "/resources/Images/images.png"); ((RemoteWebDriver)
		 * driver).setFileDetector(new LocalFileDetector()); WebElement fileInput =
		 * driver.findElement(By.id("subscription_plan_subscription_image"));
		 * fileInput.sendKeys(uploadFile.getAbsolutePath());
		 */

	}

	// verifying Subscription Plans text & newplan button on Subscription Plans
	// landing page
	public void verifySubscriptionLandingPage() {

		// verifying Subscription Plans text
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.newPlanButton"));

		utils.getLocator("subscriptionPlansPage.textLabelSubscriptionPlans").isDisplayed();
		System.out.println("Verified Subscription Plans is displayed ");
		TestListeners.extentTest.get().pass("Verified Subscription Plans is displayed ");

		// verifying newPlan button

		utils.getLocator("subscriptionPlansPage.newPlanButton").isDisplayed();
		System.out.println("Verified New Plans Button is displayed ");
		TestListeners.extentTest.get().pass("Verified New Plans Button is displayed ");

	}

	// This function is used to create Sbuscription plan with QC
	public void createSubscriptionPlan(String spName, String spPrice, String qcName, String discountType,
			boolean globalRedemptionThrottlingToggle, String dateTime, boolean singleUseFlag)
			throws InterruptedException {
		selUtils.implicitWait(5);
		clickOnNewPlanButton();
		selUtils.implicitWait(5);
		uploadSubscriptionPlanImage();

		List<WebElement> subscriptionInputBoxListWEle = utils
				.getLocatorList("subscriptionPlansPage.subscription_plan_name");
		for (WebElement wEle : subscriptionInputBoxListWEle) {
			try {
				wEle.click();
				wEle.clear();
				wEle.sendKeys(spName);
				break;
			} catch (Exception e) {

			}
		}
		// utils.getLocator("subscriptionPlansPage.subscription_plan_name").sendKeys(spName);
		// utils.getLocator("subscriptionPlansPage.subscriptionDescriptionInput").sendKeys("Subscription
		// Plan");
		utils.getLocator("subscriptionPlansPage.subscription_plan_price").sendKeys(spPrice);

		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").sendKeys("5");
		if (singleUseFlag) {
			utils.clickByJSExecutor(driver, utils.getLocator("subscriptionPlansPage.makeSingleUseFlag"));
			utils.acceptAlert(driver);
			logger.info("checked the single use flag");
			TestListeners.extentTest.get().pass("checked the single use flag");
		}

		clickOnNext();

		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.firstDiscountingRuleDiscounttype"),
				discountType);

		utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown").isDisplayed();
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown"), qcName);
		selUtils.longWait(100);
		utils.getLocator("subscriptionPlansPage.discountLimit").isDisplayed();
		utils.getLocator("subscriptionPlansPage.discountLimit").sendKeys("2");

		if (globalRedemptionThrottlingToggle) {
			utils.getLocator("subscriptionPlansPage.globalBenefitRedemptionThrottlingToggle").click();
			utils.getLocator("subscriptionPlansPage.subscriptionPlanGlobalLimitTimes").sendKeys("1");
			utils.getLocator("subscriptionPlansPage.subscriptionPlanGlobalLimitDays").sendKeys("1");
		}

		clickOnNext();
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.submitButton"));

		// select Timezone
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.timeZone"), timeZone);
		try {

			WebElement startTimeDropdownWele = utils.getLocator("subscriptionPlansPage.startTimeDropdown");
			utils.selectDrpDwnValue(startTimeDropdownWele, "12:00 AM");
			logger.info("Start time dropdown is avaiable for heartland adaptor and value is select");
			TestListeners.extentTest.get()
					.pass("Start time dropdown is avaiable for heartland adaptor and value is select");

		} catch (Exception e) {
			logger.info("Start time dropdown is NOT  avaiable for NO adaptor ");
			TestListeners.extentTest.get().pass("Start time dropdown is NOT  avaiable for NO adaptor ");

		}
		// select current date and time
//		utils.getLocator("subscriptionPlansPage.endDateTimeDropDown").clear();
//		utils.getLocator("subscriptionPlansPage.endDateTimeDropDown").sendKeys(dateTime);

		if (singleUseFlag) {
			boolean value = true;
			try {
				utils.getLocator("subscriptionPlansPage.renewPaymentScheduleLst").isDisplayed();
				Assert.assertFalse(value, "When flag is ON, then also the Renewal Payment Schedule is displayed");
			} catch (Exception e) {
				value = false;
				Assert.assertFalse(value, "When flag is ON, then also the Renewal Payment Schedule is displayed");
				logger.info("When flag is ON, then also the Renewal Payment Schedule is not displayed");
				TestListeners.extentTest.get()
						.pass("When flag is ON, then also the Renewal Payment Schedule is not displayed");
			}
		}

		utils.getLocator("subscriptionPlansPage.submitButton").click();
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.newPlanButton"));
		logger.info(spName + " is created successfully ");
		TestListeners.extentTest.get().pass(spName + " is created successfully ");
	}

	public void createNewSubscriptionPlanInFrench(String spNameInFr, String spNameInEn, String qcName,
			String discountType, String spPrice, String descriptionName, String miscellName)
			throws InterruptedException {
		utils.waitTillPagePaceDone();
		clickOnNewPlanButton();
		uploadSubscriptionPlanImage();
		utils.getLocator("subscriptionPlansPage.frSelection1").click();
		utils.getLocator("subscriptionPlansPage.subscriptionPlanNameInputInFr").sendKeys(spNameInFr);
		utils.getLocator("subscriptionPlansPage.enSelection1").click();
		utils.getLocator("subscriptionPlansPage.subscriptionPlanNameInputInEn").sendKeys(spNameInEn);
		utils.getLocator("subscriptionPlansPage.frSelection2").click();
		utils.getLocator("subscriptionPlansPage.subscriptionPlanDescriptionInputInFr").sendKeys(descriptionName);
		utils.getLocator("subscriptionPlansPage.frSelection3").click();
		utils.getLocator("subscriptionPlansPage.subscriptionPlanMiscellaneousInputInFr").sendKeys(miscellName);
		utils.getLocator("subscriptionPlansPage.subscription_plan_price").sendKeys(spPrice);
		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").sendKeys("20");
		clickOnNext();
		utils.waitTillPagePaceDone();
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.firstDiscountingRuleDiscounttype"),
				discountType);
		utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown").isDisplayed();
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown"), qcName);
		selUtils.longWait(100);

		utils.getLocator("subscriptionPlansPage.discountLimit").sendKeys("5");
		// utils.getLocator("subscriptionPlansPage.discountAmount").sendKeys("5");
		clickOnNext();

//		WebElement days = utils.getLocator("subscriptionPlansPage.advanceRenewalDays");
//		days.clear();
//		days.sendKeys("2");
		utils.getLocator("subscriptionPlansPage.startDateTime").click();
		utils.getLocator("subscriptionPlansPage.startDateTime")
				.sendKeys(CreateDateTime.getDesiredMinAheadCurrentDateAATime(2));
		selUtils.longWait(1000);
		utils.getLocator("subscriptionPlansPage.applyBtn").click();

//		WebElement startTime = utils.getLocator("subscriptionPlansPage.startTimeDropdown");
//		utils.selectDrpDwnValue(startTime, "12:00 AM");

		utils.getLocator("subscriptionPlansPage.submitButton").click();
		logger.info(spNameInFr + " is created successfully ");
		TestListeners.extentTest.get().pass(spNameInFr + " is created successfully ");

		utils.longWaitInSeconds(10);
	}

	public String getSbuscriptionPlanID(String spName) throws InterruptedException {
		String PlanID = "";
		// boolean flag = false;
		String xpath = utils.getLocatorValue("subscriptionPlansPage.subscriptionPlanNameSearch")
				.replace("$SubcriptionPlanName", spName);

		/*
		 * do { selUtils.implicitWait(2); try { List<WebElement> mainList =
		 * driver.findElements(By.xpath("//ul[@class='pagination']/li")); if
		 * (mainList.get(mainList.size() -
		 * 1).getAttribute("class").equalsIgnoreCase("next") &&
		 * mainList.get(mainList.size() -
		 * 2).getAttribute("class").equalsIgnoreCase("disabled")) { flag = false; String
		 * pageNumber = mainList.get(mainList.size() - 3).getText();
		 * driver.findElement(By.linkText(pageNumber)).click();
		 * 
		 * } else if (mainList.get(mainList.size() -
		 * 1).getAttribute("class").equalsIgnoreCase("next") &&
		 * !mainList.get(mainList.size() -
		 * 2).getAttribute("class").equalsIgnoreCase("disabled")) { flag = true; String
		 * pageNumber = mainList.get(mainList.size() - 2).getText();
		 * driver.findElement(By.linkText(pageNumber)).click(); break;
		 * 
		 * } else if (!mainList.get(mainList.size() -
		 * 1).getAttribute("class").equalsIgnoreCase("next") &&
		 * !mainList.get(mainList.size() -
		 * 2).getAttribute("class").equalsIgnoreCase("disabled")) { flag = true; String
		 * pageNumber = mainList.get(mainList.size() - 1).getText();
		 * driver.findElement(By.linkText(pageNumber)).click(); break; } } catch
		 * (Exception e) {
		 * logger.info("Next Button is not available now for clicking . ");
		 * TestListeners.extentTest.get().
		 * pass("Next Button is not available now for clicking . "); flag = false;
		 * break; }
		 * 
		 * } while (flag == false);
		 */
		// driver.findElement(By.xpath(xpath)).isDisplayed();

		// utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		new WebDriverWait(driver, Duration.ofSeconds(30))
				.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
		String spPlanURL = driver.findElement(By.xpath(xpath)).getAttribute("href");
		Pattern p = Pattern.compile("\\d+");
		Matcher m = p.matcher(spPlanURL);
		while (m.find()) {
			PlanID = m.group();
		}

		logger.info("Cerated subscription " + spName + " is found and plan id is " + PlanID);
		TestListeners.extentTest.get().pass("Cerated subscription " + spName + " is found and plan id is " + PlanID);

		selUtils.implicitWait(50);
		return PlanID;
	}

	public boolean subscriptionPlanAvailable(String spName) {
		boolean flag = false;
		List<WebElement> list = utils.getLocatorList("subscriptionPlansPage.subscriptionPlanNameAvailable");
		for (WebElement wele : list) {
			String var = wele.getText();
			if (var.contains(spName)) {
				flag = true;
				logger.info("Subscription Available");
				break;
			}
		}
		return flag;
	}

	public void updateSubscriptionPlan(String spName, String validity) {
		String xpath = utils.getLocatorValue("subscriptionPlansPage.subscriptionPlanNameSearch")
				.replace("$SubcriptionPlanName", spName);
		driver.findElement(By.xpath(xpath)).click();

		List<WebElement> subscriptionInputBoxListWEle = utils
				.getLocatorList("subscriptionPlansPage.subscriptionDescriptionList");
		for (WebElement wEle : subscriptionInputBoxListWEle) {
			try {
//				wEle.click();
				wEle.clear();
				wEle.sendKeys("Description of " + spName);
				break;
			} catch (Exception e) {

			}
		}
//		utils.getLocator("subscriptionPlansPage.subscriptionDescription").clear();
//		utils.getLocator("subscriptionPlansPage.subscriptionDescription").sendKeys("Description of " + spName);
		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").clear();
		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").sendKeys(validity);
		clickOnNext();
		clickOnNext();
		utils.getLocator("subscriptionPlansPage.submitButton").click();
		logger.info(spName + " is updateded successfully ");
		TestListeners.extentTest.get().pass(spName + " is updated successfully ");
	}

	public String verifyupdateSubscriptionPlan(String spName, String validity) {
		String xpath = utils.getLocatorValue("subscriptionPlansPage.subscriptionPlanNameSearch")
				.replace("$SubcriptionPlanName", spName);
		driver.findElement(By.xpath(xpath)).click();
		String result = utils.getLocator("subscriptionPlansPage.subscription_plan_validity").getAttribute("value");
		return result;
	}

	public void deleteSubscriptionPlan(String spName) {
		utils.getLocator("subscriptionPlansPage.subscriptionThreeDotDrp").click();
		utils.getLocator("subscriptionPlansPage.deleteSubcription").click();
		driver.switchTo().alert().accept();
		logger.info(spName + " is deleted successfully ");
		TestListeners.extentTest.get().pass(spName + " is deleted successfully ");
	}

	// This function is used to create subscription plan with QC
	public void createSubscriptionPlanWithSecondDiscountingRule(String spName, String spPrice, String qcName,
			String discountType, String discountType2, String qcName2, boolean globalRedemptionThrottlingToggle)
			throws InterruptedException {
		selUtils.implicitWait(5);
		clickOnNewPlanButton();
		selUtils.implicitWait(5);
		uploadSubscriptionPlanImage();
		List<WebElement> subscriptionInputBoxListWEle = utils
				.getLocatorList("subscriptionPlansPage.subscription_plan_name");
		for (WebElement wEle : subscriptionInputBoxListWEle) {
			try {
				wEle.click();
				wEle.clear();
				wEle.sendKeys(spName);
				break;
			} catch (Exception e) {

			}
		}
		// utils.getLocator("subscriptionPlansPage.subscription_plan_name").sendKeys(spName);
		utils.getLocator("subscriptionPlansPage.subscription_plan_price").sendKeys(spPrice);

		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").sendKeys("5");
		utils.getLocator("subscriptionPlansPage.subscriberCappingInput").sendKeys("5");
		clickOnNext();

		// Set for discounting rule1

		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.firstDiscountingRuleDiscounttype"),
				discountType);

		utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown").isDisplayed();
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown"), qcName);
		selUtils.longWait(100);
		utils.getLocator("subscriptionPlansPage.discountLimit").isDisplayed();
		utils.getLocator("subscriptionPlansPage.discountLimit").sendKeys("1");

		// Set Discounting rule2
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.secondDiscountingRuleDiscounttype"),
				discountType2);

		utils.getLocator("subscriptionPlansPage.selectSecondDiscountingRuleDropDown").isDisplayed();
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.selectSecondDiscountingRuleDropDown"), qcName2);

		selUtils.longWait(100);
		utils.getLocator("subscriptionPlansPage.secondDiscountingLimitInput").isDisplayed();
		utils.getLocator("subscriptionPlansPage.secondDiscountingLimitInput").sendKeys("1");

		if (globalRedemptionThrottlingToggle) {
			utils.getLocator("subscriptionPlansPage.globalBenefitRedemptionThrottlingToggle").click();
			utils.getLocator("subscriptionPlansPage.subscriptionPlanGlobalLimitTimes").sendKeys("1");
			utils.getLocator("subscriptionPlansPage.subscriptionPlanGlobalLimitDays").sendKeys("1");
		}

		clickOnNext();
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.submitButton"));

		// select Timezone
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.timeZone"), timeZone);
		// select start time if visible for heartland adaptor
		try {

			WebElement startTimeDropdownWele = utils.getLocator("subscriptionPlansPage.startTimeDropdown");
			utils.selectDrpDwnValue(startTimeDropdownWele, "12:00 AM");
			logger.info("Start time dropdown is avaiable for heartlan adaptor and value is select");
			TestListeners.extentTest.get()
					.pass("Start time dropdown is avaiable for heartlan adaptor and value is select");

		} catch (Exception e) {
			logger.info("Start time dropdown is NOT  avaiable for NO adaptor ");
			TestListeners.extentTest.get().pass("Start time dropdown is NOT  avaiable for NO adaptor ");

		}
		// button[@type='submit'
		utils.getLocator("subscriptionPlansPage.submitButton").click();
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.newPlanButton"));

		logger.info(spName + " is created successfully ");
		TestListeners.extentTest.get().pass(spName + " is created successfully ");
	}

	// This function is used to create Sbuscription plan with QC
	public void createSubscriptionPlanWithFutureDate(String spName, String spPrice, String qcName, String discountType,
			boolean globalRedemptionThrottlingToggle) throws InterruptedException {
		// timeZone = "(GMT+05:30) New Delhi ( IST )";
		selUtils.implicitWait(5);
		clickOnNewPlanButton();
		selUtils.implicitWait(5);
		uploadSubscriptionPlanImage();
		utils.getLocator("subscriptionPlansPage.subscription_plan_name").sendKeys(spName);
		utils.getLocator("subscriptionPlansPage.subscriptionDescriptionInput").sendKeys("Automated Subscription Plan");
		utils.getLocator("subscriptionPlansPage.subscription_plan_price").sendKeys(spPrice);

		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").sendKeys("5");
		clickOnNext();

		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.firstDiscountingRuleDiscounttype"),
				discountType);

		utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown").isDisplayed();
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.selectDiscountingRuleDropDown"), qcName);
		selUtils.longWait(100);
		utils.getLocator("subscriptionPlansPage.discountLimit").isDisplayed();
		utils.getLocator("subscriptionPlansPage.discountLimit").sendKeys("2");

		if (globalRedemptionThrottlingToggle) {
			utils.getLocator("subscriptionPlansPage.globalBenefitRedemptionThrottlingToggle").click();
			utils.getLocator("subscriptionPlansPage.subscriptionPlanGlobalLimitTimes").sendKeys("1");
			utils.getLocator("subscriptionPlansPage.subscriptionPlanGlobalLimitDays").sendKeys("1");
		}

		clickOnNext();
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.submitButton"));

		// select date
		utils.getLocator("subscriptionPlansPage.startDateTime").click();
		utils.getLocator("subscriptionPlansPage.startDateTime").sendKeys(CreateDateTime.getTomorrowDateTime());
		selUtils.longWait(1000);
		utils.getLocator("subscriptionPlansPage.applyBtn").click();

		// select Timezone
		utils.selectDrpDwnValue(utils.getLocator("subscriptionPlansPage.timeZone"), timeZone);

		// select start time if visible for heartland adaptor
		try {

			WebElement startTimeDropdownWele = utils.getLocator("subscriptionPlansPage.startTimeDropdown");
			utils.selectDrpDwnValue(startTimeDropdownWele, "12:00 AM");
			logger.info("Start time dropdown is avaiable for heartlan adaptor and value is select");
			TestListeners.extentTest.get()
					.pass("Start time dropdown is avaiable for heartlan adaptor and value is select");

		} catch (Exception e) {
			logger.info("Start time dropdown is NOT  avaiable for NO adaptor ");
			TestListeners.extentTest.get().pass("Start time dropdown is NOT  avaiable for NO adaptor ");

		}

		utils.getLocator("subscriptionPlansPage.submitButton").click();
		selUtils.waitTillElementToBeClickable(utils.getLocator("subscriptionPlansPage.newPlanButton"));
		logger.info(spName + " is created successfully ");
		TestListeners.extentTest.get().pass(spName + " is created successfully ");
	}

	public void inactiveActiveSubscription(String spName) throws InterruptedException {
		String xpath = utils.getLocatorValue("subscriptionPlansPage.subscriptionPlanNameSearch")
				.replace("$SubcriptionPlanName", spName);
		driver.findElement(By.xpath(xpath)).click();
		selUtils.implicitWait(50);
		utils.getLocator("subscriptionPlansPage.subscriptionThreeDotDrp").click();
		utils.clickByJSExecutor(driver, utils.getLocator("subscriptionPlansPage.deactivateSubscription"));
		Thread.sleep(1000);
		utils.acceptAlert(driver);
		selUtils.implicitWait(50);
	}

	public void searchSubscriptionLogs(String user) {
		utils.getLocator("subscriptionPlansPage.subscriptionLogSearch").sendKeys(user);
		utils.getLocator("subscriptionPlansPage.subscriptionLogSearch").sendKeys(Keys.ENTER);
	}

	public String userLogsStatus(String spname) {
		String xpath = utils.getLocatorValue("subscriptionPlansPage.userLogsStatus").replace("$flag", spname);
		String val = driver.findElement(By.xpath(xpath)).getText();
		return val;
	}

	public void deactivateSubscriptionReason() {
		utils.waitTillVisibilityOfElement(utils.getLocator("subscriptionPlansPage.deactivateSubscriptionToggle"), "");
		utils.getLocator("subscriptionPlansPage.deactivateSubscriptionToggle").click();
		utils.acceptAlert(driver);
	}

	public void searchSubscriptionPlan(String plan) {
		utils.getLocator("subscriptionPlansPage.search").clear();
		utils.getLocator("subscriptionPlansPage.search").sendKeys(plan);
		utils.getLocator("subscriptionPlansPage.search").sendKeys(Keys.ENTER);
		logger.info("search the subscription plan " + plan);
		TestListeners.extentTest.get().info("search the subscription plan " + plan);
	}

	public boolean getTopPlan(String plan) {
		utils.waitTillPagePaceDone();
		boolean present = false;
		try {
			String xpath = utils.getLocatorValue("subscriptionPlansPage.topPlan").replace("${planName}", plan);
			present = driver.findElement(By.xpath(xpath)).isDisplayed();
			return present;
		} catch (Exception e) {
			return present;
		}
	}

	public String getNoPlanHeading() {
		utils.waitTillPagePaceDone();
		utils.waitTillVisibilityOfElement(utils.getLocator("subscriptionPlansPage.noPlanHeading"), "");
		String text = utils.getLocator("subscriptionPlansPage.noPlanHeading").getText();
		return text;
	}

	public void hintTextsingleUseFlag(String hint) {
		clickOnNewPlanButton();

		utils.scrollToElement(driver, utils.getLocator("subscriptionPlansPage.hintTextSingleFlagUse"));
		String text = utils.getLocator("subscriptionPlansPage.hintTextSingleFlagUse").getText();
		Assert.assertTrue(text.contains(hint), "hint text is not equal");
		logger.info("verified when hint text for the single use flag");
		TestListeners.extentTest.get().pass("verified when hint text for the single use flag");
	}

	public void checkSingleUseFlagAlertMsg(String msg, String flag) {
		utils.clickByJSExecutor(driver, utils.getLocator("subscriptionPlansPage.makeSingleUseFlag"));
		String text = utils.getPopUpText();
		Assert.assertEquals(text, msg, "when the flag is " + flag + "correct alert msg is showing");
		logger.info("verified when the flag is +" + flag + "correct alert msg is showing");
		TestListeners.extentTest.get().pass("verified when the flag is +" + flag + "correct alert msg is showing");
		utils.acceptAlert(driver);
	}

	public void searchAndClickSubscriptionPlan(String planName) {
		searchSubscriptionPlan(planName);
		String xpath = utils.getLocatorValue("subscriptionPlansPage.topPlan").replace("${planName}", planName);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "");
		driver.findElement(By.xpath(xpath)).click();
		utils.waitTillPagePaceDone();
		logger.info("on the search subscription plan");
		TestListeners.extentTest.get().info("on the search subscription plan");
	}

	public boolean getSubscriptionAutoRenewalStatus(String spName) {
		String autoRenewalStatusXpath = utils.getLocatorValue("subscriptionPlansPage.autoRenewalStatus")
				.replace("$subscriptionName", spName);
		List<WebElement> listWEle = driver.findElements(By.xpath(autoRenewalStatusXpath));
		if (listWEle.size() != 0) {
			return true;
		} else {
			return false;
		}

	}

	public void updateSubscriptionPlanWithMakeAvailableForPurchaseToggle() {
		utils.getLocator("subscriptionPlansPage.makeAvailableForPurchaseToggle").isDisplayed();
		utils.getLocator("subscriptionPlansPage.makeAvailableForPurchaseToggle").click();
		clickOnNext();
		clickOnNext();
		utils.getLocator("subscriptionPlansPage.submitButton").click();
		logger.info("Subscription Plan updated successfully");
		TestListeners.extentTest.get().info("Subscription Plan updated successfully");
	}

	public boolean checkSubscriptionPlanStatus(String plan, String status) {
		String xpath = utils.getLocatorValue("subscriptionPlansPage.subscriptionPlanStatus").replace("$planName", plan)
				.replace("$status", status);
		utils.implicitWait(5);
		boolean val = driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.implicitWait(50);
		return val;
	}

	public void addPosMetaText(String text) {
		utils.waitTillPagePaceDone();
		utils.getLocator("subscriptionPlansPage.posMetaTxtArea").clear();
		utils.getLocator("subscriptionPlansPage.posMetaTxtArea").sendKeys(text);
		clickOnNext();
		logger.info("POS meta text is added for subscription plan");
		TestListeners.extentTest.get().info("POS meta text is added for subscription plan");
	}

	public boolean verifyErrorMsgForPosMeta(String message) throws InterruptedException {
		boolean flag = false;
		try {
			String result = utils.getLocator("iframePage.errorMsg").getText();
			if (message.contains(result)) {
				flag = true;
			}
		} catch (Exception e) {
			flag = false;
		}
		return flag;
	}

	public void uploadSubscriptionPlanImage(String path) {
		driver.findElement(By.id("subscription_plan_subscription_image")).sendKeys(path);
	}

	public void createSubscriptionPlanFirstPage(String spName, String path, String spPrice, boolean singleUseFlag)
			throws InterruptedException {
		selUtils.implicitWait(5);
		uploadSubscriptionPlanImage(path);
		List<WebElement> subscriptionInputBoxListWEle = utils
				.getLocatorList("subscriptionPlansPage.subscription_plan_name");
		for (WebElement wEle : subscriptionInputBoxListWEle) {
			try {
				utils.clickByJSExecutor(driver, wEle);
				// wEle.click();
				wEle.clear();
				// utils.longWaitInSeconds(2);
				wEle.sendKeys(spName);
				break;
			} catch (Exception e) {

			}
		}
		utils.getLocator("subscriptionPlansPage.subscription_plan_price").sendKeys(spPrice);

		utils.getLocator("subscriptionPlansPage.subscription_plan_validity").sendKeys("5");
		if (singleUseFlag) {
			utils.clickByJSExecutor(driver, utils.getLocator("subscriptionPlansPage.makeSingleUseFlag"));
			utils.acceptAlert(driver);
			logger.info("checked the single use flag");
			TestListeners.extentTest.get().pass("checked the single use flag");
		}
	}

	public void clickSubmitButton() {
		utils.getLocator("subscriptionPlansPage.submitButton").click();
		;
		utils.waitTillPagePaceDone();
		logger.info(" clicked on submit button ");
		TestListeners.extentTest.get().pass(" clicked on submit button");
	}

}
