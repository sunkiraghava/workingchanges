package com.punchh.server.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class DynamicRewardsPage {

	static Logger logger = LogManager.getLogger(DynamicRewardsPage.class);
	private WebDriver driver;
	Utilities utils;

	public DynamicRewardsPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
	}

	public void createDynamicRewardSetName(String rewardName) {
		utils.getLocator("dynamicRewardsPage.newDynamicRewardBtn").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("dynamicRewardsPage.nameTextBox").sendKeys(rewardName);

	}

	public void enterDynamicRewardSurpriseSetWithPoints(String weight, String rewardType, String points) {
		utils.getLocator("dynamicRewardsPage.addNextBtn").click();
		utils.getLocator("dynamicRewardsPage.weightBoxOne").sendKeys(weight);
		WebElement rewardTypeDrp = utils.getLocator("dynamicRewardsPage.rewardTypeDropdownOne");
		utils.selectDrpDwnValue(rewardTypeDrp, rewardType);
		utils.waitTillElementToBeClickable(utils.getLocator("dynamicRewardsPage.ValueBoxOne"));
		utils.getLocator("dynamicRewardsPage.ValueBoxOne").click();
		utils.getLocator("dynamicRewardsPage.ValueBoxOne").sendKeys(points);
		logger.info("Dynamic reward surprise set details with points entered");
		TestListeners.extentTest.get().info("Dynamic reward surprise set details with points entered");
	}

	public void enterDynamicRewardSurpriseSetWithRedeemable(String weight, String rewardType, String redeemable) {
		utils.getLocator("dynamicRewardsPage.addNextBtn").click();
		utils.getLocator("dynamicRewardsPage.weightBoxTwo").sendKeys(weight);
		WebElement rewardTypeDrp = utils.getLocator("dynamicRewardsPage.rewardTypeDropdownTwo");
		utils.selectDrpDwnValue(rewardTypeDrp, rewardType);
		utils.waitTillElementToBeClickable(utils.getLocator("dynamicRewardsPage.ValueDrpBoxTwo"));
		WebElement redeemableValueDrp = utils.getLocator("dynamicRewardsPage.ValueDrpBoxTwo");
		utils.selectDrpDwnValue(redeemableValueDrp, redeemable);
		logger.info("Dynamic reward surprise set details with redeemable entered");
		TestListeners.extentTest.get().info("Dynamic reward surprise set details with redeemable entered");
	}

	public String saveDynamicRewardSet() {
		utils.getLocator("dynamicRewardsPage.saveBtn").click();
		utils.waitTillPagePaceDone();
		WebElement msg = utils.getLocator("dynamicRewardsPage.successMsg");
		String val = msg.getText();
		logger.info("Dynamic reward surprise set saved");
		TestListeners.extentTest.get().info("Dynamic reward surprise set saved");
		return val;
	}

	public String deleteDynamicRewardSet(String rewardSetName) {
		String dynamicRewardXpath = utils.getLocatorValue("dynamicRewardsPage.deleteDynamicRewardBtn").replace("$temp",
				rewardSetName);
		WebElement deleteBtn = driver.findElement(By.xpath(dynamicRewardXpath));
		deleteBtn.click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		WebElement msg = utils.getLocator("dynamicRewardsPage.successMsg");
		String val = msg.getText();
		logger.info("Dynamic reward surprise set deleted");
		TestListeners.extentTest.get().info("Dynamic reward surprise set deleted");
		return val;
	}

	public void setConditionalGifingOn() {
		WebElement toggleBtn = utils.getLocator("dynamicRewardsPage.conditonalGiftingToggle");
		toggleBtn.click();
		utils.waitTillPagePaceDone();
		logger.info("Conditional Gifting toggle set to ON");
	}

	public void enterDynamicRewardGiftingRuletWithPoints(String segment, String qc, String rewardType, String points) {
		utils.getLocator("dynamicRewardsPage.addNextBtn").click();
		// set segment
		utils.getLocator("dynamicRewardsPage.segmentDrpOne").click();
		utils.getLocator("dynamicRewardsPage.searchBox").sendKeys(segment);
		String searchedSegmentXpath = utils.getLocatorValue("dynamicRewardsPage.searchedVal").replace("$temp", segment);
		WebElement searchedSegment = driver.findElement(By.xpath(searchedSegmentXpath));
		utils.clickByJSExecutor(driver, searchedSegment);
//		searchedSegment.click();

		// set qc
		utils.getLocator("dynamicRewardsPage.qcDrpOne").click();
		utils.getLocator("dynamicRewardsPage.searchBox").sendKeys(qc);
		String searchedqcXpath = utils.getLocatorValue("dynamicRewardsPage.searchedVal").replace("$temp", qc);
		WebElement searchedQc = driver.findElement(By.xpath(searchedqcXpath));
		utils.clickByJSExecutor(driver, searchedQc);
//		searchedQc.click();

		WebElement rewardTypeDrp = utils.getLocator("dynamicRewardsPage.rewardTypeDropdownOne");
		utils.selectDrpDwnValue(rewardTypeDrp, rewardType);
		utils.getLocator("dynamicRewardsPage.ValueBoxOne").sendKeys(points);
		logger.info("Dynamic reward gifting rule details with points entered");
		TestListeners.extentTest.get().info("Dynamic reward gifting rule details with points entered");
	}

	public void enterDynamicRewardGiftingWithRedeemable(String segment, String qc, String rewardType,
			String redeemable) {
		utils.getLocator("dynamicRewardsPage.addNextBtn").click();
		// set segment
		utils.getLocator("dynamicRewardsPage.segmentDrpTwo").click();
		utils.getLocator("dynamicRewardsPage.searchBox").sendKeys(segment);
		String searchedSegmentXpath = utils.getLocatorValue("dynamicRewardsPage.searchedVal").replace("$temp", segment);
		WebElement searchedSegment = driver.findElement(By.xpath(searchedSegmentXpath));
		searchedSegment.click();

		// set qc
		utils.getLocator("dynamicRewardsPage.qcDrpTwo").click();
		utils.getLocator("dynamicRewardsPage.searchBox").sendKeys(qc);
		String searchedqcXpath = utils.getLocatorValue("dynamicRewardsPage.searchedVal").replace("$temp", qc);
		WebElement searchedQc = driver.findElement(By.xpath(searchedqcXpath));
		searchedQc.click();

		WebElement rewardTypeDrp = utils.getLocator("dynamicRewardsPage.rewardTypeDropdownTwo");
		utils.selectDrpDwnValue(rewardTypeDrp, rewardType);
		WebElement redeemableValueDrp = utils.getLocator("dynamicRewardsPage.ValueDrpBoxTwo");
		utils.selectDrpDwnValue(redeemableValueDrp, redeemable);
		logger.info("Dynamic reward gifting rule details with redeemable entered");
		TestListeners.extentTest.get().info("Dynamic reward gifting rule details with redeemable entered");
	}

	public void setDefaultReward(String value) {
		WebElement defaultRewardDrp = utils.getLocator("dynamicRewardsPage.defaultReward");
		utils.selectDrpDwnValue(defaultRewardDrp, value);
		utils.waitTillPagePaceDone();
		logger.info("Default Reward entered");
	}

}