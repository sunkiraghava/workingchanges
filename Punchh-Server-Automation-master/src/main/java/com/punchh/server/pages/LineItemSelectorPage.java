package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class LineItemSelectorPage {
	static Logger logger = LogManager.getLogger(LineItemSelectorPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public LineItemSelectorPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void enterDetailsNewLineItemSelectorPage(String input_name, String item_id, String filterSetName) {
		try {

			utils.getLocator("LineItemSelectorsPage.addLineItemSelectorButton").isDisplayed();
			utils.getLocator("LineItemSelectorsPage.addLineItemSelectorButton").click();
			utils.getLocator("LineItemSelectorsPage.nameField").sendKeys(input_name);

			utils.getLocator("LineItemSelectorsPage.baseItemIDBtn").click();
			utils.getLocator("LineItemSelectorsPage.itemIdTextBox").sendKeys(item_id);

			WebElement element = driver.findElement(By.id("select2-line_item_selector_filter_type-container"));
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);

			utils.getLocator("LineItemSelectorsPage.filterItemSetdropdown").click();
			String filterSetXpath = utils.getLocatorValue("LineItemSelectorsPage.selectFilterItemSet")
					.replace("$FilterSetName", filterSetName);

			driver.findElement(By.xpath(filterSetXpath)).click();

			utils.getLocator("LineItemSelectorsPage.updateButton").click();
			utils.getLocator("LineItemSelectorsPage.lineItemSuccessmsg").isDisplayed();
			logger.info("LineItem selector is created successfully:-" + input_name);
			TestListeners.extentTest.get().pass("LineItem selector is created successfully:-" + input_name);
		} catch (Exception e) {
			logger.error("LineItem selector is not created");
			TestListeners.extentTest.get().fail("LineItem selector is not created");
		}
	}

	public void setLineItemName(String name) {
		utils.getLocator("LineItemSelectorsPage.addLineItemSelectorButton").isDisplayed();
		utils.getLocator("LineItemSelectorsPage.addLineItemSelectorButton").click();
		utils.getLocator("LineItemSelectorsPage.nameField").sendKeys(name);
		logger.info("Entered item name as :" + name);
		TestListeners.extentTest.get().pass("Line item name is set as :" + name);
	}

	public void setBaseItemsAsItemid(String itemId) {
		utils.getLocator("LineItemSelectorsPage.baseItemIDBtn").click();
		utils.getLocator("LineItemSelectorsPage.itemIdTextBox").sendKeys(itemId);
		logger.info("Entered item ids as :" + itemId);
		TestListeners.extentTest.get().pass("Item id  is set as :" + itemId);
	}

	public void setModifiersAsItemid(String itemId) {
		utils.getLocator("LineItemSelectorsPage.modifiersItemid").click();
		utils.getLocator("LineItemSelectorsPage.modifiersItemTxtBox").sendKeys(itemId);
		logger.info("Entered item ids as :" + itemId);
		TestListeners.extentTest.get().pass("Modifier Item id  is set as :" + itemId);
	}

	public void setFilterItemSet(String option) throws InterruptedException {
		utils.getLocator("LineItemSelectorsPage.filterItemSetdropdown").click();
		List<WebElement> elements = utils.getLocatorList("LineItemSelectorsPage.filterItemList");
		utils.selectListDrpDwnValue(elements, option);
		logger.info("Selected filter item set as :" + option);
		TestListeners.extentTest.get().pass("Filter Item Set  is set as :" + option);

	}

	public void createLIS() {
		utils.getLocator("LineItemSelectorsPage.updateButton").click();
		// String
		// msg=utils.getLocator("LineItemSelectorsPage.lineItemSuccessmsg").getText().replace("x",
		// "").trim();
		// return msg;
	}

	public void setModifierSelectionRule(String val, String option) throws InterruptedException {
		utils.getLocator("LineItemSelectorsPage.maxDiscountedModifiers").clear();
		utils.getLocator("LineItemSelectorsPage.maxDiscountedModifiers").sendKeys(val);
		logger.info("Maximum discount modifiers is s set as :" + val);
		utils.getLocator("LineItemSelectorsPage.modifierSelectionRuleDrp").click();
		List<WebElement> elements = utils.getLocatorList("LineItemSelectorsPage.modifierSelectionRuleList");
		utils.selectListDrpDwnValue(elements, option);
		logger.info("Modifier selection rule is set as :" + option);
		TestListeners.extentTest.get().pass("Modifier selection rule is set as :" + option);

	}

	public void deleteLineItemSelectors(String lisname) throws InterruptedException {
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").clear();
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").sendKeys(lisname);
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").sendKeys(Keys.ENTER);
		utils.getLocator("LineItemSelectorsPage.deleteLis").click();
		utils.acceptAlert(driver);
		Thread.sleep(3000);
		// String
		// msg=utils.getLocator("LineItemSelectorsPage.lineItemSuccessmsg").getText().replace("x",
		// "").trim();
		logger.info("LIS is Deleted :" + lisname);
		// return msg;
	}

	public boolean checkLineItemIsExist(String lineItemFilterName) throws InterruptedException {
		boolean result = false;
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").clear();
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").sendKeys(lineItemFilterName);
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").sendKeys(Keys.ENTER);
		Thread.sleep(2000);

		boolean isTextDisplayedInSource = driver.getPageSource().contains("No Matches Found");

		if (isTextDisplayedInSource) {
			String matchResultText = utils.getLocator("LineItemSelectorsPage.lisSearchResultList").getText();
			if (matchResultText.trim().equalsIgnoreCase("No Matches Found")) {
				result = false;
				return result;
			}
		} else {

			List<WebElement> wEleList = utils.getLocatorList("LineItemSelectorsPage.lisSearchResultListIfexist");
			for (WebElement eleW : wEleList) {
				String actualTextFromList = eleW.getText();
				if (actualTextFromList.equalsIgnoreCase(lineItemFilterName)) {
					result = true;
					return result;
				}

			}

		}

		return result;

	}

	public void createLineItemFilterIfNotExist(String lineItemFilterName, String itemID, String filterSetName)
			throws InterruptedException {
		boolean isLFExist = checkLineItemIsExist(lineItemFilterName);

		if (!isLFExist) {
			logger.info(lineItemFilterName + " LineItem Selector is not exist and Create new LineItem Selector");
			TestListeners.extentTest.get()
					.pass(lineItemFilterName + " LineItem Selector is not exist and Create new LineItem Selector");

			enterDetailsNewLineItemSelectorPage(lineItemFilterName, itemID, filterSetName);
			logger.info(lineItemFilterName + " LineItem Selector is created successfuly");
			TestListeners.extentTest.get().pass(lineItemFilterName + " LineItem Selector is created successfuly");

		} else {
			logger.info(lineItemFilterName + " LineItem Selector is exist ");
			TestListeners.extentTest.get().pass(lineItemFilterName + " LineItem Selector is exist ");
		}
		utils.refreshPage();
	}

	public void searchAndSelectLineItemSelector(String lisname) {
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").clear();
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").sendKeys(lisname);
		utils.getLocator("LineItemSelectorsPage.lisSearchbox").sendKeys(Keys.ENTER);
		utils.getLocator("LineItemSelectorsPage.lisName").click();
		utils.waitTillPagePaceDone();
	}

	public void setLineItemQuantity(String operator, String itemQuantity) {
		utils.getLocator("LineItemSelectorsPage.clickQuantityButton").click();
		utils.getLocator("LineItemSelectorsPage.qtySelectorTypeDrp").click();
		List<WebElement> elem = utils.getLocatorList("LineItemSelectorsPage.qtySelectorTypeList");
		utils.selectListDrpDwnValue(elem, operator);
		logger.info("Entered Quantity operator as :" + operator);
		TestListeners.extentTest.get().pass("Entered Quantity operator as :" + operator);
		utils.getLocator("LineItemSelectorsPage.enterQuantity").click();
		utils.getLocator("LineItemSelectorsPage.enterQuantity").sendKeys(itemQuantity);
		utils.getLocator("LineItemSelectorsPage.updateButton").click();
		logger.info("Entered Quantity as :" + itemQuantity);
		TestListeners.extentTest.get().pass("Entered Quantity is set as :" + itemQuantity);
	}

	public void auditLogsOfSelectedLIS() {
		boolean flag;
		utils.getLocator("LineItemSelectorsPage.clickAuditLogButtonOfSelectedLIS").isDisplayed();
		utils.getLocator("LineItemSelectorsPage.clickAuditLogButtonOfSelectedLIS").click();
		flag = utils.getLocator("LineItemSelectorsPage.auditLogsVisibility").isDisplayed();
		Assert.assertEquals(true, flag, "Audit Logs are not Visible of Selected Line item Selector");
		logger.info("Audit Logs are Visible of Selected Line item Selector");
		TestListeners.extentTest.get().pass("Audit Logs are Visible of Selected Line item Selector");
	}

	public void verifedUpdateValuesInLis(String filterType, String itemFieldName, String updatedValue) {
		utils.waitTillElementToBeClickable(utils.getLocator("LineItemSelectorsPage.updateButton"));
		String xpath = utils.getLocatorValue("LineItemSelectorsPage.getValueOfGivenField")
				.replace("$filterType", filterType).replace("$itemFieldName", itemFieldName);
		String updatedValueFromUI = driver.findElement(By.xpath(xpath)).getAttribute("value");
		Assert.assertEquals(updatedValueFromUI, updatedValue, "Updated value is not matched with expected value");
		logger.info("Updated value is matched with expected value");
		TestListeners.extentTest.get().pass("Updated value is matched with expected value");
	}

	public void verifiedFilterItemSelectedValue(String expValue) {
		WebElement wele = utils.getLocator("LineItemSelectorsPage.filterItemSelectBox_Xpath");
		Select selObj = new Select(wele);
		WebElement selValueWele = selObj.getFirstSelectedOption();
		String actualSelectedValue = selValueWele.getText();
		Assert.assertEquals(actualSelectedValue, expValue,
				actualSelectedValue + " Filter Item Set value is not matched with expected value " + expValue);
		logger.info(expValue + " expected dropdown value is not matched with actual " + actualSelectedValue);
		TestListeners.extentTest.get()
				.pass(expValue + " expected dropdown value is not matched with actual " + actualSelectedValue);

	}
	//method to clear the value in Maximum Discounted Modifiers field
	public void clearMaxDiscountedModifiers() {
		utils.getLocator("LineItemSelectorsPage.maxDiscountedModifiers").clear();
		logger.info("Cleared Maximum Discounted Modifiers field");
		TestListeners.extentTest.get().pass("Cleared Maximum Discounted Modifiers field");
		utils.getLocator("LineItemSelectorsPage.updateButton").click();
	}

}