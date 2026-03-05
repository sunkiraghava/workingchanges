package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CockpitRedemptionsPage {

	// Author:shashank sharma

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public CockpitRedemptionsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void navigateToRedemptionsTabs(String tabToNavigate) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("window.scrollTo(0, 0);");

		String xpath = utils.getLocatorValue("cockpitPage.redemptionsTabsXpath").replace("$TabName", tabToNavigate);
		WebElement weleTab = driver.findElement(By.xpath(xpath));
		// utils.scrollToElement(driver, weleTab);
		weleTab.click();
		utils.longWait(4);
		logger.info("User navigate to post redemption tab ");
		TestListeners.extentTest.get().pass("User navigate to post redemption tab ");

	}

	public void clickedOnAutoCheckinRedemptionCheckBox(String checkBoxFlag) {
		String checkBoxValue = utils.getLocator("cockpitPage.autoCheckingCheckBox").getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			logger.info("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
			TestListeners.extentTest.get()
					.pass("Auto checking box Unchecked and do not cliked it as checkBoxFlag= " + checkBoxFlag);
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, utils.getLocator("cockpitPage.autoCheckingCheckBox"));
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Autocheckin box is unchecked and user want to check the chekedbox");
			TestListeners.extentTest.get().pass("Autocheckin box is unchecked and user want to check the chekedbox");

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, utils.getLocator("cockpitPage.autoCheckingCheckBox"));
			utils.getLocator("dashboardPage.updateBtn").click();

			logger.info("Autocheckin box is already cheked and user want to uncheck ");
			TestListeners.extentTest.get().pass("Autocheckin box is already cheked and user want to uncheck ");
		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

			logger.info("Autocheckin box is already checked and user want to check the chekedbox, so do not click");
			TestListeners.extentTest.get()
					.pass("Autocheckin box is already checked and user want to check the chekedbox, so do not click");

		}

	}

	public void updateTotalNumberOfRedemptionsAllowed(String number) {

		utils.getLocator("cockpitPage.totalNumberOfRedemptionsAllowedInput").clear();
		utils.getLocator("cockpitPage.totalNumberOfRedemptionsAllowedInput").sendKeys(number);

		logger.info("Updated the Total number of redemptions allowed input box in Multiple redemption tab");
		TestListeners.extentTest.get()
				.pass("Updated the Total number of redemptions allowed input box in Multiple redemption tab");

	}

	public void clickOnUpdateButton() {
		utils.getLocator("dashboardPage.updateBtn").click();
		selUtils.implicitWait(10);
		logger.info("Clicked on update button in multiple redemption page ");
		TestListeners.extentTest.get().pass("Clicked on update button in multiple redemption page ");
	}

	public void updateMaxRedemptionAmount(String amount) {
		utils.getLocator("cockpitPage.maxRedemptionInputBox").clear();
		utils.getLocator("cockpitPage.maxRedemptionInputBox").sendKeys(amount);

		logger.info("Updated the Total number of redemptions allowed input box in Multiple redemption tab");
		TestListeners.extentTest.get()
				.pass("Updated the Total number of redemptions allowed input box in Multiple redemption tab");

	}

	public void setAcquisitionType(String acquisitionType, String acquisitionValue) {// , String earnedRewardsValue,
		// String
		// prePurchaseDiscountValue,String couponPromoValue) {
		int indexNumber = 0;
		switch (acquisitionType) {
		case "Offers":
			indexNumber = 0;
			break;
		case "Earned Rewards":
			indexNumber = 1;
			break;
		case "Pre-Purchased Discount":
			indexNumber = 2;
			break;
		case "Coupons & Promos":
			indexNumber = 3;
			break;

		default:
			indexNumber = 0;
			break;
		}
		selUtils.implicitWait(30);
		if (!acquisitionValue.equalsIgnoreCase("") && !acquisitionType.equalsIgnoreCase("")) {
			List<WebElement> weleList = driver
					.findElements(By.xpath("//span[@id='select2-business_redemption_acquisition_rules_attributes_"
							+ indexNumber + "_code-container']/span[text()='Select']"));

			if (weleList.size() != 0) {
				weleList.get(0).click();
				// set offer value
				driver.findElement(By.xpath("//ul[@id='select2-business_redemption_acquisition_rules_attributes_"
						+ indexNumber + "_code-results']/li[text()='" + acquisitionType + "']")).click();
				driver.findElement(By.xpath("//input[@id='business_redemption_acquisition_rules_attributes_"
						+ indexNumber + "_multiplication_factor']")).clear();
				driver.findElement(By.xpath("//input[@id='business_redemption_acquisition_rules_attributes_"
						+ indexNumber + "_multiplication_factor']")).sendKeys(acquisitionValue);
			} else {
				driver.findElement(By.xpath("//input[@id='business_redemption_acquisition_rules_attributes_"
						+ indexNumber + "_multiplication_factor']")).clear();
				driver.findElement(By.xpath("//input[@id='business_redemption_acquisition_rules_attributes_"
						+ indexNumber + "_multiplication_factor']")).sendKeys(acquisitionValue);
			}
			selUtils.implicitWait(50);
		} else {
			selUtils.implicitWait(50);
			clearAcquisitionTypeData(acquisitionType);
		}

	}
	
	public void clearAcquisitionTypeData(String acquisitionType) {
	    List<WebElement> weleList = utils.getLocatorList("dashboardPage.acquisitionTypeClearIcon");
	    for (WebElement element : weleList) {
	        String selectedValue = element.getAttribute("title");
	        if (selectedValue != null && selectedValue.equalsIgnoreCase(acquisitionType)) {
	            element.click();
	            utils.logit("Successfully cleared the  Acquisition Type value:: "+element.getText());
	            utils.pressKey(Keys.ESCAPE);
	            break; 
	        }
	    }
	}

	public void onEnableRewardLocking() {
		WebElement enableRewardLocking = driver
				.findElement(By.xpath("//input[@id='business_enable_discount_locking']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableRewardLocking);
		String val = enableRewardLocking.getAttribute("checked");
		if (val == null) {
			// utils.getLocator("cockpitPage.enableRewardLocking").click();
			utils.clickByJSExecutor(driver, enableRewardLocking);
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Enable Reward Locking checkbox ");
			TestListeners.extentTest.get().info("Enable Reward Locking checkbox ");
		}
	}

	public void offEnableRewardLocking() {
		WebElement enableRewardLocking = driver
				.findElement(By.xpath("//input[@id='business_enable_discount_locking']"));
		String js = "arguments[0].style.visibility='visible';";
		((JavascriptExecutor) driver).executeScript(js, enableRewardLocking);
		String val = enableRewardLocking.getAttribute("checked");
		if (val != null) {
			// utils.getLocator("cockpitPage.enableRewardLocking").click();
			utils.clickByJSExecutor(driver, utils.getLocator("cockpitPage.enableRewardLocking"));
			utils.getLocator("dashboardPage.updateBtn").click();
			logger.info("Disable Reward Locking checkbox ");
			TestListeners.extentTest.get().info("Disable Reward Locking checkbox ");
		}
	}

	public boolean resultOfUserLookUpPosAPI1(String externalUidResponse, String locked, String externalUidResponseflag,
			String lockedFlag) {
		try {
			if (externalUidResponse == externalUidResponseflag && locked == lockedFlag)
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean resultOfUserLookUpPosAPI(String externalUidResponse, String locked, String externalUidResponseflag,
			String lockedFlag) {
		try {
			if (externalUidResponse.equalsIgnoreCase(externalUidResponseflag) && (locked.equalsIgnoreCase(lockedFlag)))
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void cockpitAllCheckBoxElements(String checkBoxFlag) throws InterruptedException {
		boolean result = false;
		do {
			List<WebElement> wList = utils.getLocatorList("cockpitPage.checkBoxList_Xpath");
			for (WebElement wEle : wList) {
				clickedOnAllCheckBox(checkBoxFlag, wEle);
			}
			clickOnUpdateButton();
			result = clickOnAllCheckboxValue(checkBoxFlag);
		} while (result == false);

	}

	public void clickedOnAllCheckBox(String checkBoxFlag, WebElement wEle) {
		String checkBoxValue = wEle.getAttribute("checked");

		if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
		} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			utils.clickByJSExecutor(driver, wEle);

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
			utils.clickByJSExecutor(driver, wEle);

		} else if (checkBoxValue.equalsIgnoreCase("true") && (checkBoxFlag.equalsIgnoreCase("check"))) {

		}

	}

	public int verifyCheckboxValue(String checkBoxFlag) {
		boolean flag = true;
		List<WebElement> wList = utils.getLocatorList("cockpitPage.checkBoxList_Xpath");
		List<WebElement> wListName = utils.getLocatorList("cockpitPage.checkBoxLabelList_Xpath");
		int counter = 0;
		for (WebElement wEle : wList) {
			String checkBoxValue = wEle.getAttribute("checked");

			if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
				String labelName = wListName.get(counter).getAttribute("for");
				labelName = labelName.replaceAll("business_", "").replace("_", " ").toUpperCase();
				logger.info(counter + "--- " + labelName + " is not checked -- Failed ");
				TestListeners.extentTest.get().warning(counter + "--- " + labelName + " is not checked -- Failed ");
				flag = false;

			} else if ((checkBoxValue != null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
				String labelName = wListName.get(counter).getAttribute("for");
				labelName = labelName.replaceAll("business_", "").replace("_", " ").toUpperCase();
				logger.info(counter + "--- " + labelName + " is checked -- PASS ");
				TestListeners.extentTest.get().pass(counter + "--- " + labelName + " is checked -- PASS ");

			}

			if ((checkBoxValue != null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
				String labelName = wListName.get(counter).getAttribute("for");
				labelName = labelName.replaceAll("business_", "").replace("_", " ").toUpperCase();
				logger.info(counter + "--- " + labelName + " is  checked -- Failed ");
				TestListeners.extentTest.get().warning(counter + "--- " + labelName + " is  checked -- Failed ");
				flag = false;

			} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
				String labelName = wListName.get(counter).getAttribute("for");
				labelName = labelName.replaceAll("business_", "").replace("_", " ").toUpperCase();
				logger.info(counter + "--- " + labelName + " is unchecked -- PASS ");
				TestListeners.extentTest.get().pass(counter + "--- " + labelName + " is unchecked -- PASS ");

			}
			counter++;
		}
		return counter;
	}

	public boolean clickOnAllCheckboxValue(String checkBoxFlag) {
		boolean flag = true;
		List<WebElement> wList = utils.getLocatorList("cockpitPage.checkBoxList_Xpath");
		int counter = 0;
		for (WebElement wEle : wList) {
			String checkBoxValue = wEle.getAttribute("checked");
			if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
				utils.clickByJSExecutor(driver, wEle);
				flag = false;

			} else if ((checkBoxValue != null) && (checkBoxFlag.equalsIgnoreCase("check"))) {
			}
			if ((checkBoxValue != null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {
				utils.clickByJSExecutor(driver, wEle);
				flag = false;

			} else if ((checkBoxValue == null) && (checkBoxFlag.equalsIgnoreCase("uncheck"))) {

			}
			counter++;
		}

		return flag;
	}

	public void setAutoUnlockPeriod(String queryParam, String time) {
		switch (queryParam) {
		case "present":
			utils.getLocator("cockpitPage.checkAutoUnlockPeriod").isDisplayed();
			utils.getLocator("cockpitPage.enterAutoUnlockPeriod").clear();
			utils.getLocator("cockpitPage.enterAutoUnlockPeriod").sendKeys(time);
			logger.info("Auto Unlock Period is displayed and time entered in Auto Unlock Period tab is " + time);
			TestListeners.extentTest.get()
					.pass("Auto Unlock Period is displayed and time entered in Auto Unlock Period tab is " + time);
			break;
		case "not present":
			logger.info("Auto Unlock Period is not visible");
			TestListeners.extentTest.get().pass("Auto Unlock Period is not visible");
		}
	}

	public void runSidekiqJob(String url, String job) throws InterruptedException {
		driver.navigate().to(url + "/sidekiq/scheduled");
		// Thread.sleep(4000);
		selUtils.longWait(4000);
		driver.navigate().refresh();
		utils.getLocator("cockpitPage.sidekiqSearchBar").sendKeys(job);
		utils.getLocator("cockpitPage.sidekiqSearchBar").sendKeys(Keys.ENTER);
		try {
			selUtils.implicitWait(10);
			utils.getLocator("cockpitPage.sidekiqCheckAllCheckBox").click();
			utils.getLocator("cockpitPage.sidekiqAddToQueue").click();
			logger.info("Worker jobs " + job + "  found in sidekiq and triggered");
			TestListeners.extentTest.get().pass(" Worker jobs " + job + " found in sidekiq and triggered");
			selUtils.implicitWait(50);
		} catch (Exception e) {
			selUtils.implicitWait(50);
			logger.info("Worker jobs " + job + "  did not found in sidekiq");
			TestListeners.extentTest.get().pass(" Worker jobs " + job + "  did not found in sidekiq");
		}
	}

	public void checkAssert(String queryParam, boolean status1) {
		utils.waitTillPagePaceDone();
		switch (queryParam) {
		case "false":
			selUtils.implicitWait(30);
			Assert.assertFalse(status1, "Error in searching email in locked account");
			logger.info("Email search in locked account is successful and user is not found");
			TestListeners.extentTest.get().pass("Email search in locked account is successful and user is not found");
			selUtils.implicitWait(50);
			break;
		case "true":
			selUtils.implicitWait(30);
			Assert.assertTrue(status1, "Error in searching email in locked account");
			logger.info("Email search in locked account is successful");
			TestListeners.extentTest.get().pass("Email search in locked account is successful");
			selUtils.implicitWait(50);
			break;
		}
	}

	public void deleteSidekiqJob(String url, String job) throws InterruptedException {
		driver.navigate().to(url + "/sidekiq/scheduled");
		selUtils.longWait(4000);
		driver.navigate().refresh();
		utils.getLocator("cockpitPage.sidekiqSearchBar").sendKeys(job);
		utils.getLocator("cockpitPage.sidekiqSearchBar").sendKeys(Keys.ENTER);
		try {
			selUtils.implicitWait(10);
			utils.getLocator("cockpitPage.sidekiqCheckAllCheckBox").click();
			utils.getLocator("cockpitPage.sidekiqDeleteQueue").click();
			logger.info(
					"Discount Basket Unlock Worker jobs DiscountBasketAutoUnlockWorker found in sidekiq and DELETED");
			TestListeners.extentTest.get().pass(
					"Discount Basket Unlock Worker jobs DiscountBasketAutoUnlockWorker found in sidekiq and DELETED");
			selUtils.implicitWait(50);
		} catch (Exception e) {
			selUtils.implicitWait(50);
			logger.info("Discount Basket Unlock Worker jobs DiscountBasketAutoUnlockWorker did not found in sidekiq");
			TestListeners.extentTest.get()
					.pass("Discount Basket Unlock Worker jobs DiscountBasketAutoUnlockWorker did not found in sidekiq");
		}
	}

	public String checkSidekiqJob(String url, String job) throws InterruptedException {
		driver.navigate().to(url + "/sidekiq/scheduled");
		driver.navigate().refresh();
		utils.getLocator("cockpitPage.sidekiqSearchBar").sendKeys(job);
		utils.getLocator("cockpitPage.sidekiqSearchBar").sendKeys(Keys.ENTER);
		List<WebElement> tableRowOne = utils.getLocatorList("cockpitPage.listOfDiscountBasketAutoUnlockWorker");
		int col = tableRowOne.size();
		String column = Integer.toString(col);
		return column;
	}

	public boolean getExternalUidFromPOSUserLookUpApi(String lookUpField, String userEmail, String locationkey,
			String externalUid, String lockedFlag) throws InterruptedException {
		String externalUidResponse1 = "";
		String locked1 = "";
		boolean flag = false;
		int attempts = 0;
		while (attempts < 20) {
			Thread.sleep(1000);
			try {
				// POS user lookUp
				Response userLookupResponse1 = pageObj.endpoints().userLookupPosApi("email", userEmail, locationkey,
						externalUid);
				Assert.assertEquals(userLookupResponse1.getStatusCode(), 200,
						"Status code 200 did not match with POS user lookUp ");
				externalUidResponse1 = userLookupResponse1.jsonPath().getString("external_uid");
				locked1 = userLookupResponse1.jsonPath().getString("locked");
				if (externalUidResponse1.equalsIgnoreCase(externalUid) && locked1.equalsIgnoreCase(lockedFlag)) {
					flag = true;
					break;
				}

			} catch (Exception e) {
				logger.info("external_uid and locked status is not found ");
			}
			attempts++;
		}
		Assert.assertEquals(externalUidResponse1, externalUid, "exteral uid doesn't match");
		Assert.assertEquals(locked1, lockedFlag, "locked value is not true");
		return flag;
	}

	public void setAutoRedemptionDiscounts(String value, String choice) throws InterruptedException {
		choice = choice.toLowerCase();
		List<WebElement> listOfDropDownSelected = driver.findElements(
				By.xpath("//select[@id='business_auto_redemption_discounts']/following-sibling::span//ul/li"));
		boolean flag = false;
		for (WebElement wEle : listOfDropDownSelected) {
			String drpValue = wEle.getText();
			if (drpValue.contains(value)) {
				logger.info(value + " is already selected .");
				TestListeners.extentTest.get().info(value + " is already selected .");
				flag = true;
				break;
			}
		}

		switch (choice) {
		case "select":
			if (flag == false) {
				utils.waitTillElementToBeClickable(utils.getLocator("cockpitPage.enterAutoRedemptionDiscounts"));
				utils.getLocator("cockpitPage.enterAutoRedemptionDiscounts").sendKeys(value);
				utils.getLocator("cockpitPage.enterAutoRedemptionDiscounts").sendKeys(Keys.ENTER);
				logger.info(value + " is set into Auto redemption discounts dropdown");
				TestListeners.extentTest.get().pass(value + " is set into Auto redemption discounts dropdown");
			}
			break;

		case "unselect": {
			if (flag == true) {
				listOfDropDownSelected.get(listOfDropDownSelected.size() - 1).click();

				driver.findElement(By.xpath("//li[@role='treeitem'][text()='" + value + "']")).click();
				logger.info(value + " is set into Auto redemption discounts dropdown");
				TestListeners.extentTest.get().pass(value + " is set into Auto redemption discounts dropdown");
			}

			break;
		}

		default:
			break;
		}

	}

	public void setRedemptionCodeStrategy(String strategy) throws InterruptedException {
		utils.waitTillElementToBeClickable(utils.getLocator("cockpitPage.clickRedemptionCodeStrategy"));
		utils.getLocator("cockpitPage.clickRedemptionCodeStrategy").click();
		selUtils.longWait(3000);
		WebElement elem = utils.getLocator("cockpitPage.redemptionCodeStrategyList");
		utils.selectDrpDwnValue(elem, strategy);
		utils.getLocator("cockpitPage.clickRedemptionCodeStrategy").click();
		logger.info("Selected Redemption Code Strategy is " + strategy);
		TestListeners.extentTest.get().pass("Selected Redemption Code Strategy is " + strategy);
	}

	public String RedemptionCodeStrategySelectBoxIsEditable() {

		String isHiddenValue = utils.getLocator("cockpitPage.redemptionCodeStrategySelectBox")
				.getAttribute("aria-hidden");
		return isHiddenValue;
	}

	public int getRedemptionCodeStrategySelectBoxValue() {

		String selectedRedemptionCodeStrategyValue = utils
				.getLocator("cockpitPage.redemptionCodeStrategySelectBoxValue").getAttribute("title");
		selectedRedemptionCodeStrategyValue = selectedRedemptionCodeStrategyValue.replace(" Digits", "");

		int expRedemptionCodeStrategySize = Integer.parseInt(selectedRedemptionCodeStrategyValue);

		return expRedemptionCodeStrategySize;
	}

	public void enterRedeemableAttributes(String choice, String value) {
		switch (choice) {
		case "clear all":
			utils.getLocator("cockpitPage.redeemableAttributes").click();
			utils.getLocator("cockpitPage.redeemableAttributes").clear();
			break;

		default:
			utils.getLocator("cockpitPage.redeemableAttributes").click();
			break;
		}
		utils.getLocator("cockpitPage.redeemableAttributes").sendKeys(value);
		clickOnUpdateButton();
		logger.info("Entered Redeemable Attributes " + value);
		TestListeners.extentTest.get().pass("Entered Redeemable Attributes " + value);
	}

	public void setProcessingOrder(String choice, String strategy) {
		boolean flag = false;
		switch (choice) {
		case "update":
			utils.getLocator("cockpitPage.setProcessingOrderDrpDown").click();
			List<WebElement> ele = utils.getLocatorList("cockpitPage.setProcessingOrderList");
			utils.selectListDrpDwnValue(ele, strategy);
			logger.info("Set processing order is selected as :- " + strategy);
			TestListeners.extentTest.get().pass("Set processing order is selected as :- " + strategy);
			break;
		case "remove":
			try {
				utils.getLocator("cockpitPage.clearSetProcessingOrder").click();
                utils.getLocator("cockpitPage.totalNumberOfRedemptionsAllowedInput").click();
				logger.info("Set processing order is cleared");
				TestListeners.extentTest.get().pass("Set processing order is cleared");
				break;
			} catch (Exception e) {
				logger.info("Set processing order don't have any value");
				TestListeners.extentTest.get().pass("Set processing order don't have any value");
			}
		}

	}

	public void updateRedemptionMark(String value) {
		WebElement redemptionMark = utils.getLocator("cockpitPage.redemptionMark");
		utils.waitTillElementToBeClickable(redemptionMark);
		redemptionMark.click();
		redemptionMark.clear();
		redemptionMark.sendKeys(value);
		logger.info("entered the value -- " + value + " in the redemption mark");
		TestListeners.extentTest.get().info("entered the value -- " + value + " in the redemption mark");
	}

	public List<String> divideRange(int start, int end, int partitionSize) {
		if (start > end || partitionSize <= 0) {
			logger.info("Invalid input ");
			TestListeners.extentTest.get().info("Invalid input");
			return null;
		}
		int numParts = (int) Math.ceil((double) (end - start + 1) / partitionSize);
		List<String> lst = new ArrayList<String>();
		int currentStart = start;
		for (int i = 0; i < numParts; i++) {
			int currentEnd = Math.min(currentStart + partitionSize - 1, end);
			String text = currentStart + "-" + currentEnd;
			lst.add(text);
			currentStart = currentEnd + 1;
		}
		return lst;
	}
    public void updateMinimumPayablePrice(String value) {
      utils.getLocator("cockpitPage.minimumPayablePrice").click();
      utils.getLocator("cockpitPage.minimumPayablePrice").clear();
      utils.getLocator("cockpitPage.minimumPayablePrice").sendKeys(value);
      logger.info("entered the value " + value + " in the Minimum Payable Price");
      TestListeners.extentTest.get()
          .info("entered the value " + value + " in the Minimum Payable Price");
    }

	public void setProcessingPriorityByDiscountType(String discountType) {

		String text = "";
		switch (discountType) {
		case "Fuel discount":
			text = "fuel_reward";
			break;

		case "Rewards":
			text = "reward";
			break;

		case "Subscription":
			text = "subscription";
			break;

		case "Discount Amount":
			text = "discount_amount";
			break;

		case "Coupon/Promos":
			text = "redemption_code";
			break;

		default:
			text = "reward";
			break;
		}
		utils.getLocator("cockpitPage.totalNumberOfRedemptionsAllowedInput").click();
		utils.getLocator("forceRedemptionPage.processingPriority").click();

		List<WebElement> weleList = driver
				.findElements(By.xpath("//ul[@id='select2-business_redemption_discount-results']//li"));

		if (weleList.size() != 0) {
			driver.findElement(By.xpath("//ul[@id='select2-business_redemption_discount-results']//li[contains(@id,'"
					+ text + "') and @aria-selected='false']")).click();
			selUtils.implicitWait(50);
		}
		utils.longWaitInSeconds(1);
	}

	public void clearProcessingPriorityByDiscountType() {

		List<WebElement> weleList = driver.findElements(By.xpath(
				"//div[contains(@class,'business_redemption_discount')]//span[contains(@class,'select2-selection--multiple')]//li[contains(@class,'selection__choice')]"));
		for (int i = 0; i < weleList.size(); i++) {
			WebElement e = driver.findElement(By.xpath(
					"//div[contains(@class,'business_redemption_discount')]//span[contains(@class,'select2-selection--multiple')]//li[contains(@class,'selection__choice')]/span"));
			selUtils.waitTillElementToBeClickable(e);
			e.click();
		}

	}

	public void setInteroperability(String first_acquisition_type, String second_acquisition_type) {
		String text1, text2 = "";

		switch (first_acquisition_type) {
		case "Offers":
			text1 = "offer";
			break;

		case "Earned Rewards":
			text1 = "loyalty";
			break;

		case "Pre-Purchased Discount":
			text1 = "pre_purchased";
			break;

		case "Coupons & Promos":
			text1 = "promo_coupon";
			break;

		default:
			text1 = "reward";
			break;
		}

		switch (second_acquisition_type) {
		case "Offers":
			text2 = "offer";
			break;

		case "Earned Rewards":
			text2 = "loyalty";
			break;

		case "Pre-Purchased Discount":
			text2 = "pre_purchased";
			break;

		case "Coupons & Promos":
			text2 = "promo_coupon";
			break;

		default:
			text2 = "reward";
			break;
		}

		// Click on Add Interoperability link
		utils.getLocator("cockpitPage.addInteroperability").click();

		// Select first AcquisitionType under Interoperability
		utils.getLocator("cockpitPage.selectFirstAcquisitionUnderInteroperability").click();
		WebElement firstAcquisitionType = driver.findElement(
				By.xpath("//*[@id='business_interoperability_strategy_1_first_acquisition_type']//option[@value='"
						+ text1 + "']"));
		firstAcquisitionType.click();

		// Select Second AcquisitionType under Interoperability
		utils.getLocator("cockpitPage.selectSecondAcquisitionUnderInteroperability").click();
		WebElement secondAcquisitionType = driver.findElement(
				By.xpath("//*[@id='business_interoperability_strategy_1_second_acquisition_type']//option[@value='"
						+ text2 + "']"));
		secondAcquisitionType.click();

	}

	public void clearInteroperability() {
		try {
			if (utils.getLocator("cockpitPage.clearInteroperability").isDisplayed()) {
				utils.getLocator("cockpitPage.clearInteroperability").click();
				TestListeners.extentTest.get().pass("Existing Interoperability has been deleted");
			}

		} catch (Exception e) {
			logger.info("Interoperability does not exist");
		}
	}

	public void cleartAutoRedemptionDiscounts() {

		List<WebElement> weleList = utils.getLocatorList("cockpitPage.autoRedemptionDiscountsList");
		for (int i = 0; i < weleList.size(); i++) {
			utils.longWaitInSeconds(1);
			WebElement webEle = utils.getLocator("cockpitPage.removeAutoRedemptionDiscounts");
			utils.waitTillElementToBeClickable(webEle);
			utils.clickByJSExecutor(driver, webEle);
		}
		utils.getLocator("cockpitPage.clickONAutoRedemptionDiscounts").click();
	}

	public void setInteroperabilityNew(String first_acquisition_type, String second_acquisition_type) throws InterruptedException {
		// Click on Add Interoperability link
		utils.getLocator("cockpitPage.addInteroperability").click();
		Thread.sleep(2000);
		// Select first AcquisitionType under Interoperability

		WebElement firstAcquisitionType = utils.getLocator("cockpitPage.firstInteroperabilityAcquisitionTypeDropdownXpath") ;
		utils.selectDrpDwnValue(firstAcquisitionType, first_acquisition_type);

		WebElement secondAcquisitionType = utils.getLocator("cockpitPage.secondInteroperabilityAcquisitionTypeDropdownXpath") ;
		utils.selectDrpDwnValue(secondAcquisitionType, second_acquisition_type);

	}
	
	public void selectInteroperability(String first_acquisition_type, String second_acquisition_type) {
		// Click on Add Interoperability link
		utils.scrollToElement(driver, utils.getLocator("cockpitPage.addInteroperability"));
		utils.getLocator("cockpitPage.addInteroperability").click();

		// Select first AcquisitionType under Interoperability
		utils.getLocator("cockpitPage.selectFirstAcquisitionUnderInteroperability").click();
		List<WebElement> listOfDropDownSelected = driver
				.findElements(By.xpath("//ul[contains(@id,'interoperability_strategy_1_first_acquisition_type')]/li"));
		utils.selectListDrpDwnValue(listOfDropDownSelected, first_acquisition_type);
		// Select Second AcquisitionType under Interoperability
		utils.getLocator("cockpitPage.selectSecondAcquisitionUnderInteroperability").click();
		List<WebElement> secondAcquisitionType = driver
				.findElements(By.xpath("//ul[contains(@id,'interoperability_strategy_1_second_acquisition_type')]/li"));
		utils.selectListDrpDwnValue(secondAcquisitionType, second_acquisition_type);
	}

	// this method to clear and update the Max Redemption based upon the user choice
	public void SetMaxRedemption(String choice, String value) {
		WebElement maxRedemptionInputBox = utils.getLocator("cockpitPage.maxRedemptionInputBox");
		maxRedemptionInputBox.click();
		switch (choice.toLowerCase()) {
			case "clear":
				maxRedemptionInputBox.clear();
				logger.info("Cleared the Max Redemption input box.");
				TestListeners.extentTest.get().pass("Cleared the Max Redemption input box.");
				break;

			case "update":
				maxRedemptionInputBox.clear();
				maxRedemptionInputBox.sendKeys(value);
				logger.info("Updated the Max Redemption input box with value: " + value);
				TestListeners.extentTest.get().pass("Updated the Max Redemption input box with value: " + value);
				break;

			default:
				logger.warn("Invalid choice provided. Please use 'clear' or 'update'.");
				TestListeners.extentTest.get().warning("Invalid choice provided. Please use 'clear' or 'update'.");
				break;
		}
	}

}// end of class
