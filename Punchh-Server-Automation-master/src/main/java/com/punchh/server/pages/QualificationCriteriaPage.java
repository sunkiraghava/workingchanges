package com.punchh.server.pages;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

public class QualificationCriteriaPage {

	static Logger logger = LogManager.getLogger(QualificationCriteriaPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public QualificationCriteriaPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void enterDetailsNewQcSelectorPage(String input_name, String rectag, String QCname, String amtcap) {
		enterQcName(QCname, rectag, amtcap);
		utils.getLocator("QualificationCriteriaPage.ProcessingFunctiondropdown").click();
		utils.getLocator("QualificationCriteriaPage.SumOfAmounts").click();
		// utils.getLocator("QualificationCriteriaPage.LineItemFiltersdrpdwn").click();
		selUtils.longWait(100);
		addlineItemfilters(input_name);
		addQualiicationCriteria(input_name);
		utils.clickByJSExecutor(driver, utils.getLocator("QualificationCriteriaPage.UpdateButton"));
		// utils.getLocator("QualificationCriteriaPage.UpdateButton").click();
		selUtils.longWait(100);
		logger.info("Qualification Criteria created successfully with tag:-" + rectag);
		TestListeners.extentTest.get().pass("Qualification Criteria created successfully with tag:-" + rectag);
	}

	public void enterQcName(String QCname, String rectag, String amtcap) {
		utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn").click();
		utils.getLocator("QualificationCriteriaPage.QualificationCriteriaName").sendKeys(QCname);
		utils.getLocator("QualificationCriteriaPage.ReceiptTagName").sendKeys(rectag);
		utils.getLocator("QualificationCriteriaPage.AmountCap").sendKeys(amtcap);
	}

	// used to add single line item filter ( first LIS only)
	public void addlineItemfilters(String input_name) {
		utils.longWaitInSeconds(2);
		utils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.LineItemFiltersdrpdwn"));
		WebElement lineItemFiltersDrpDwn = utils.getLocator("QualificationCriteriaPage.LineItemFiltersdrpdwn");
		utils.scrollToElement(driver, lineItemFiltersDrpDwn);
		utils.selectDrpDwnValue(lineItemFiltersDrpDwn, input_name);
		utils.longWaitInSeconds(2);
		logger.info("Line Item Filter is added successfully : " + input_name);
	}

	public void addQualiicationCriteria(String input_name) {
		utils.longWaitInSeconds(2);
		utils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.QualificationCriteriadrpdwn"));
		WebElement lineItemFiltersDrpDwn = utils.getLocator("QualificationCriteriaPage.QualificationCriteriadrpdwn");
		utils.scrollToElement(driver, lineItemFiltersDrpDwn);
		utils.selectDrpDwnValue(lineItemFiltersDrpDwn, input_name);
		utils.longWaitInSeconds(2);
		logger.info("Line Item Filter is added successfully : " + input_name);
	}

	public void activateTag(String QCname) {
		utils.getLocator("QualificationCriteriaPage.ActivateTagbtn").isDisplayed();
		utils.getLocator("QualificationCriteriaPage.ActivateTagbtn").click();
		yesOrNo("Yes");
		utils.getLocator("QualificationCriteriaPage.RecieptTagActivationmsg").getText().contains(QCname);
		logger.info("Reciept Tag activation is completed successfully for Qualification Criteria:-" + QCname);
		TestListeners.extentTest.get().pass("Reciept Tag activation is  success for Qualification Criteria:-" + QCname);
	}

	public void createQualificationCriteria(String QCname, String rectag, String amtcap, String functionName,
			String unitDiscount, boolean flagQC, String lineItemSelectorName) {

		selUtils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn"));
		// String rectag = QCname;
		enterQcName(QCname, rectag, amtcap);

		selectProcessingFunction(functionName.trim());
		selUtils.longWait(100);
		switch (functionName.trim()) {
		case "Rate Rollback":
			utils.getLocator("QualificationCriteriaPage.unitDiscount").sendKeys(unitDiscount);
			logger.info("Rate Rollback is selected successfully ");
			TestListeners.extentTest.get().pass("Rate Rollback is selected successfully ");
			break;
		default:
			break;
		}
		// Selecting Line item filter and qualifire based on flag
		if (flagQC) {
			addlineItemfilters(lineItemSelectorName);
			addQualiicationCriteria(lineItemSelectorName);
		}
		utils.getLocator("QualificationCriteriaPage.UpdateButton").click();
		selUtils.longWait(100);
		verifySuccessMessage();
		logger.info(QCname + " is successfully created with amount:-" + amtcap);
		TestListeners.extentTest.get().pass(QCname + " is successfully created with amount:-" + amtcap);
		selUtils.implicitWait(2);
	}

	public void createQualificationCriteria(String QCname, String amtcap, String functionName, String unitDiscount,
			boolean flagQC, String lineItemSelectorName) {

		selUtils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn"));
		String rectag = QCname;
		enterQcName(QCname, rectag, amtcap);

		selectProcessingFunction(functionName.trim());
		selUtils.longWait(100);
		switch (functionName.trim()) {
		case "Rate Rollback":
			utils.getLocator("QualificationCriteriaPage.unitDiscount").sendKeys(unitDiscount);
			logger.info("Rate Rollback is selected successfully ");
			TestListeners.extentTest.get().pass("Rate Rollback is selected successfully ");
			break;
		default:
			break;
		}
		// Selecting Line item filter and qualifire based on flag
		if (flagQC) {
			addlineItemfilters(lineItemSelectorName);
			addQualiicationCriteria(lineItemSelectorName);
		}
		utils.scrollToElement(driver, utils.getLocator("QualificationCriteriaPage.UpdateButton"));
		utils.getLocator("QualificationCriteriaPage.UpdateButton").click();
		selUtils.longWait(100);
		// verifySuccessMessage();
		logger.info(QCname + " is successfully created with amount:-" + amtcap);
		TestListeners.extentTest.get().pass(QCname + " is successfully created with amount:-" + amtcap);
		selUtils.implicitWait(2);
	}

	public void selectProcessingFunction(String functionName) {
		utils.getLocator("QualificationCriteriaPage.ProcessingFunctiondropdown").click();
		String prcessingFunction_UpdatedXpath = utils
				.getLocatorValue("QualificationCriteriaPage.processingFunctiondropdownValue")
				.replace("$FunctionName", functionName);
		driver.findElement(By.xpath(prcessingFunction_UpdatedXpath)).click();
	}

	public void verifySuccessMessage() {
		String expectedSuccessMessage = "Qualification Criterion created";
		String actualSuccessMessage = utils.getLocator("QualificationCriteriaPage.successMessage").getText();
		Assert.assertEquals(expectedSuccessMessage, actualSuccessMessage.trim());
		logger.info("Success Message is verified ");
		TestListeners.extentTest.get().pass("Success Message is verified ");
	}

	public void setQCName(String qcName) {
		utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn").click();
		utils.getLocator("QualificationCriteriaPage.QualificationCriteriaName").clear();
		utils.getLocator("QualificationCriteriaPage.QualificationCriteriaName").sendKeys(qcName);
		logger.info("QC name is set as :" + qcName);
		TestListeners.extentTest.get().pass("Qc name is set as :" + qcName);
	}

	public void setProcessingFunction(String option) {
		utils.getLocator("QualificationCriteriaPage.ProcessingFunctiondropdown").click();
		utils.getLocator("QualificationCriteriaPage.processingFuncSeachBox").clear();
		utils.getLocator("QualificationCriteriaPage.processingFuncSeachBox").sendKeys(option);
		utils.getLocator("QualificationCriteriaPage.searchedValue").click();
		logger.info("Processing function is set as :" + option);
		TestListeners.extentTest.get().pass("Processing Function is set as :" + option);
	}

	public void setPercentageOfProcessedAmount(String option) {
		utils.getLocator("QualificationCriteriaPage.percentageAmount").clear();
		utils.getLocator("QualificationCriteriaPage.percentageAmount").sendKeys(option);
		logger.info("Percentage amount is set as :" + option);
		TestListeners.extentTest.get().pass("Percentage amount is set as :" + option);
	}

	public void clearPercentageOfProcessedAmount() {
		utils.getLocator("QualificationCriteriaPage.percentageAmount").clear();
		logger.info("Percentage amount is Clear now ");
		TestListeners.extentTest.get().pass("Percentage amount is Clear now ");
	}

	public void setLineItemFilters(int i, String item, String unitPrice, String qty) {
		try {
			String eleItemXpath = utils.getLocatorValue("QualificationCriteriaPage.lineItemFiltersDrp")
					.replace("${IndexNum}", i + "");
			WebElement eleItemsWEle = driver.findElement(By.xpath(eleItemXpath));
			utils.selectDrpDwnValue(eleItemsWEle, item);
			logger.info("item is set as :" + item);
			TestListeners.extentTest.get().pass("item is set as :" + item);

			String eleUnitpriceXpath = utils.getLocatorValue("QualificationCriteriaPage.lineItemFiltersUnitPriceDrp")
					.replace("${IndexNum}", i + "");
			WebElement eleUnitpriceWEle = driver.findElement(By.xpath(eleUnitpriceXpath));
			utils.selectDrpDwnValue(eleUnitpriceWEle, unitPrice);
			logger.info("unitPrice is set as :" + unitPrice);
			TestListeners.extentTest.get().pass("unitPrice is set as :" + unitPrice);

		} catch (Exception e) {
			logger.info("Failed to set Item / unit price in line item selector");
			TestListeners.extentTest.get().fail("unitPrice is set as :" + unitPrice);
			Assert.fail();
		}
	}

	public void setQty(int i, String qty) {
		try {
			selUtils.longWait(3000);
			int indexNum = i + 1;
			String xpath = utils.getLocatorValue("QualificationCriteriaPage.lineItemFiltersQtyDrp")
					.replace("${RowNum}", indexNum + "").replace("${IdNum}", i + "");

			// String xpath = "//div[@id='line-item-selectors']//table/tbody/tr[" + intexed
			// +
			// "]/td[4]//span[@id='select2-qualification_criterion_discounting_expressions_attributes_"
			// + i
			// + "_quantity-container']";
			System.out.println("XPATH --- " + xpath);
			WebElement qtyWEele = driver.findElement(By.xpath(xpath));
			// utils.scrollToElement(driver, qtyWEele);
			qtyWEele.isEnabled();
			qtyWEele.click();
			utils.getLocator("QualificationCriteriaPage.qtySearchBox").sendKeys(qty);
			String qtyValXpath = utils.getLocatorValue("QualificationCriteriaPage.selectQTYVal").replace("${qtyVal}",
					qty);
			driver.findElement(By.xpath(qtyValXpath)).click();
		} catch (Exception e) {
			System.out.println("QualificationCriteriaPage.setQty()  :: QTY element not found");
			logger.info("QualificationCriteriaPage.setQty()  :: QTY element not found");
			TestListeners.extentTest.get().fail("QualificationCriteriaPage.setQty()  :: QTY element not found");
			Assert.fail();

		}

	}

	public void setItemQualifiers(int i, String qualifier, String item) {

		String eleineItemExistXpath = utils.getLocatorValue("QualificationCriteriaPage.lineItemExistDrp")
				.replace("${IndexNum}", i + "");
		WebElement eleineItemExistWele = driver.findElement(By.xpath(eleineItemExistXpath));
		utils.selectDrpDwnValue(eleineItemExistWele, qualifier);
		logger.info("Item Qualifiers line item exist is set as :" + qualifier);
		TestListeners.extentTest.get().pass("Item Qualifiers line item exist is set as :" + qualifier);

		String eleUlineItemExistIteXpath = utils.getLocatorValue("QualificationCriteriaPage.lineItemExistItemDrp")
				.replace("${IndexNum}", i + "");
		WebElement eleUlineItemExistIteWele = driver.findElement(By.xpath(eleUlineItemExistIteXpath));
		utils.selectDrpDwnValue(eleUlineItemExistIteWele, item);
		logger.info("Item Qualifiers  item  is set as :" + item);
		TestListeners.extentTest.get().pass("Item Qualifiers  item  is set as :" + item);
	}

	public String createQC() {
		utils.waitTillPagePaceDone();
		utils.scrollToElement(driver, utils.getLocator("QualificationCriteriaPage.UpdateButton"));
		// utils.getLocator("QualificationCriteriaPage.UpdateButton").click();
		utils.StaleElementclick(driver, utils.getLocator("QualificationCriteriaPage.UpdateButton"));
		String msg = utils.getLocator("QualificationCriteriaPage.qcSuccessmsg").getText().replace("x", "").trim();
		return msg;
	}

	public String testQualificationwithReceipt(String reciept) throws InterruptedException {
		utils.getLocator("QualificationCriteriaPage.testQualificationBtn").click();
		utils.getLocator("QualificationCriteriaPage.menuItemStringbox").clear();
		utils.getLocator("QualificationCriteriaPage.menuItemStringbox").sendKeys(reciept);
		utils.getLocator("QualificationCriteriaPage.evaluateBtn").click();
		Thread.sleep(3000);
		String qualifyingMsg = utils.getLocator("QualificationCriteriaPage.qualifiesTab").getText();
		return qualifyingMsg;
	}

	public void deleteQC(String qcname) throws InterruptedException {
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").clear();
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(qcname);
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(Keys.ENTER);
		utils.getLocator("QualificationCriteriaPage.deleteQc").click();
		utils.acceptAlert(driver);
		Thread.sleep(2000);
		// String
		// msg=utils.getLocator("QualificationCriteriaPage.qcSuccessmsg").getText().replace("x",
		// "").trim();
		logger.info("QC Deleted");
		// return msg;
	}

	public void SearchQC(String qcname) {
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").clear();
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(qcname);
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(Keys.ENTER);
		utils.getLocator("QualificationCriteriaPage.qcNameLink").click();
	}

	public void EditQCEnableMenuItemAggregator(String option) throws InterruptedException {

		utils.getLocator("QualificationCriteriaPage.enableAggregator").click();
		Thread.sleep(2000);
		List<WebElement> eles = utils.getLocatorList("QualificationCriteriaPage.aggregatorOptions");
		for (int i = 0; i < eles.size(); i++) {
			String value = eles.get(i).getText();
			System.out.println(value);
			if (value.equals(option)) {
				logger.info("checkbox selected is :" + value);
			} else {
				eles.get(i).click();
			}
		}
		utils.longWaitInSeconds(3);
	}

	public void setMaximumDiscountedUnits(String value) {
		utils.getLocator("QualificationCriteriaPage.maxDiscountUnits").clear();
		utils.getLocator("QualificationCriteriaPage.maxDiscountUnits").sendKeys(value);
		logger.info("Max Discounted units set as :" + value);
	}

	public boolean checkQualificationCriteriaIsExist(String qcName) throws InterruptedException {
		boolean result = false;
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").clear();
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(qcName);
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(Keys.ENTER);
		Thread.sleep(2000);

		boolean isTextDisplayedInSource = driver.getPageSource().contains("No Matches Found");

		if (isTextDisplayedInSource) {
			String matchResultText = utils.getLocator("QualificationCriteriaPage.qcSearchResultList").getText();
			if (matchResultText.trim().equalsIgnoreCase("No Matches Found")) {
				result = false;
				return result;
			}
		} else {

			List<WebElement> wEleList = utils.getLocatorList("QualificationCriteriaPage.qcSearchResultListIfexist");
			for (WebElement eleW : wEleList) {
				String actualTextFromList = eleW.getText();
				if (actualTextFromList.equalsIgnoreCase(qcName)) {
					result = true;
					return result;
				}

			}

		}

		return result;

	}

	public void createQualificationCriteria(String qcName, String amtcap, String functionName, String unitDiscount,
			boolean flagQC, String lineItemSelectorName, String lineItemExpression, String percentageOfProcessedAmount,
			boolean turnOnDiscountStacking, boolean enableReuseOfQualifyingItemsFlag,
			boolean enableMenuItemAggregatorFlag, String aggregateFunctionName, String effectiveLocation) {

		selUtils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn"));
		String rectag = qcName;
		if (amtcap != "") {
			enterQcName(qcName, rectag, amtcap);
		} else {
			setQCName(qcName);
		}

		if (percentageOfProcessedAmount != null) {
			setPercentageOfProcessedAmount(percentageOfProcessedAmount);
		}
		selectProcessingFunction(functionName.trim());
		selUtils.longWait(100);

		if (effectiveLocation != "") {
			WebElement wEle = driver.findElement(
					By.xpath("//span[@id='select2-qualification_criterion_effective_location-container']/span"));
			wEle.click();
			driver.findElement(By.xpath("//input[@class='select2-search__field']")).sendKeys(effectiveLocation);
			driver.findElement(By.xpath("//ul/li[text()='" + effectiveLocation + "']")).click();
		}
		switch (functionName.trim()) {
		case "Rate Rollback":
			utils.getLocator("QualificationCriteriaPage.unitDiscount").sendKeys(unitDiscount);
			logger.info("Rate Rollback is selected successfully ");
			TestListeners.extentTest.get().pass("Rate Rollback is selected successfully ");
			break;
		default:
			break;
		}
		// Selecting Line item filter and qualifire based on flag
		if (flagQC) {
			addlineItemfilters(lineItemSelectorName);

			if (lineItemExpression != "") {
				addlineItemfiltersExpressions(lineItemExpression);
			}
			addQualiicationCriteria(lineItemSelectorName);
		}

		if (turnOnDiscountStacking) {
			utils.clickByJSExecutor(driver,
					utils.getLocator("QualificationCriteriaPage.turnOnDiscountStackingCheckBox"));
		}
		if (enableReuseOfQualifyingItemsFlag) {
			utils.clickByJSExecutor(driver,
					utils.getLocator("QualificationCriteriaPage.enableReuseOfQualifyingItemsCheckBox"));
		}
		if (enableMenuItemAggregatorFlag) {

			selectAggregator(enableMenuItemAggregatorFlag, aggregateFunctionName);

			// unselect other aggregate
		}

		utils.getLocator("QualificationCriteriaPage.UpdateButton").click();
		selUtils.longWait(100);
		// verifySuccessMessage();
		logger.info(qcName + " is successfully created with amount:-" + amtcap);
		TestListeners.extentTest.get().pass(qcName + " is successfully created with amount:-" + amtcap);
		selUtils.implicitWait(2);
	}

	// Sum of amount / max Price unit price
	public void addlineItemfiltersExpressions(String input_name) {

		utils.getLocator("QualificationCriteriaPage.lisFilterExpressionDropDown").click();
		selUtils.longWait(100);
		List<WebElement> lineItemfilters = utils
				.getLocatorList("QualificationCriteriaPage.lisSelectFilterExpressionDropDwonList");
		for (int i = 0; i < lineItemfilters.size(); i++) {
			if (lineItemfilters.get(i).getText().equals(input_name)) {
				lineItemfilters.get(i).click();
				break;
			}
		}
	}

	public void createQCIfNotExist(String QCname, String amtcap, String functionName, String unitDiscount,
			boolean flagQC, String lineItemSelectorName, String lineItemExpressionString,
			String percentageOfProcessedAmount, boolean TurnOnDiscountStacking,
			boolean enableReuseOfQualifyingItemsFlag, boolean enableMenuItemAggregatorFlag,
			String aggregateFunctionName, String effectiveLocation) throws InterruptedException {
		selUtils.implicitWait(10);
		boolean isLFExist = checkQualificationCriteriaIsExist(QCname);

		if (!isLFExist) {
			logger.info(QCname + " LineItem Selector is not exist and Create new LineItem Selector");
			TestListeners.extentTest.get().pass(QCname + " QC Selector is not exist and Create new LineItem Selector");

			createQualificationCriteria(QCname, amtcap, functionName, unitDiscount, true, lineItemSelectorName,
					lineItemExpressionString, percentageOfProcessedAmount, TurnOnDiscountStacking,
					enableReuseOfQualifyingItemsFlag, enableMenuItemAggregatorFlag, aggregateFunctionName,
					effectiveLocation);
			logger.info(QCname + " QC Selector is created successfuly");
			TestListeners.extentTest.get().pass(QCname + " QC Selector is created successfuly");

		} else {
			logger.info(QCname + " QC Selector is exist ");
			TestListeners.extentTest.get().pass(QCname + " QC Selector is exist ");

		}
		selUtils.implicitWait(50);
		selUtils.longWait(5000);

	}

	public void selectAggregator(boolean enableMenuItemAggregatorFlag, String aggregateFunctionName) {
		utils.clickByJSExecutor(driver, utils.getLocator("QualificationCriteriaPage.enableMenuItemAggregatorCheckBox"));

		List<WebElement> list = utils.getLocatorList("QualificationCriteriaPage.enableMenuItemAggregatorItemNameList");
		for (WebElement ele : list) {
			String str = ele.getText().trim();
			if (!str.equalsIgnoreCase(aggregateFunctionName)) {
				ele.click();

			}
		}

	}

	public void setTargetUnitPrice(String price) {
		utils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.setTargetUnitPrice"));
		utils.getLocator("QualificationCriteriaPage.setTargetUnitPrice").click();
		utils.getLocator("QualificationCriteriaPage.setTargetUnitPrice").clear();
		utils.getLocator("QualificationCriteriaPage.setTargetUnitPrice").sendKeys(price);
		logger.info("Target Unit Price is set as :" + price);
		TestListeners.extentTest.get().pass("Target Unit Price is set as :" + price);
	}

	public void setLineItemFilter(int i, String item) {
		List<WebElement> eleItems = utils.getLocatorList("QualificationCriteriaPage.lineItemFiltersDrp");

		eleItems.get(i).click();
		utils.getLocator("QualificationCriteriaPage.searchBoxDrp").clear();
		utils.getLocator("QualificationCriteriaPage.searchBoxDrp").sendKeys(item);
		utils.getLocator("QualificationCriteriaPage.searchedValue").click();
		TestListeners.extentTest.get().pass("Line Item Filters item is set as :" + item);
	}

	public void yesOrNo(String choice) {
		switch (choice) {
		case "Yes":
			utils.getLocator("QualificationCriteriaPage.receiptTagYes").click();
			break;

		case "No":
			utils.getLocator("QualificationCriteriaPage.receiptTagNo").click();
			break;
		}
	}

	public void setUnitDiscount(String unitDiscount) {

		utils.getLocator("QualificationCriteriaPage.unitDiscount").clear();
		utils.getLocator("QualificationCriteriaPage.unitDiscount").sendKeys(unitDiscount);
		logger.info("Unit discount is set as :" + unitDiscount);
		TestListeners.extentTest.get().pass("Unit discount is set as :" + unitDiscount);
	}

	public void setReceiptTagName(String rectag) {
		utils.getLocator("QualificationCriteriaPage.ReceiptTagName").clear();
		utils.getLocator("QualificationCriteriaPage.ReceiptTagName").sendKeys(rectag);
		logger.info("Receipt Tag name is set as :" + rectag);
		TestListeners.extentTest.get().info("Receipt Tag name is set as :" + rectag);
	}

	public void updateButton() {
		utils.scrollToElement(driver, utils.getLocator("QualificationCriteriaPage.UpdateButton"));
		utils.clickByJSExecutor(driver, utils.getLocator("QualificationCriteriaPage.UpdateButton"));
		logger.info("Qualification Criteria created successfully");
		TestListeners.extentTest.get().info("Qualification Criteria created successfully");
		utils.longWaitInSeconds(3);

	}

	public String VerifyErrorMsgOnActivatingTag(String QCname, String choice) {
		utils.getLocator("QualificationCriteriaPage.ActivateTagbtn").isDisplayed();
		utils.getLocator("QualificationCriteriaPage.ActivateTagbtn").click();
		// VerifyConfirmationPopup();
		yesOrNo(choice);
		String text = utils.getLocator("QualificationCriteriaPage.errorMsg").getText();
		return text;
	}

	public String VerifyConfirmationPopup() {
		utils.getLocator("QualificationCriteriaPage.ActivateTagbtn").isDisplayed();
		utils.getLocator("QualificationCriteriaPage.ActivateTagbtn").click();
		logger.info("Clicked Activate Tag Button");
		TestListeners.extentTest.get().info("Clicked Activate Tag Button");

		utils.getLocator("QualificationCriteriaPage.confirmationPopup").isDisplayed();
		String text = utils.getLocator("QualificationCriteriaPage.confirmationPopup").getText();

		utils.getLocator("QualificationCriteriaPage.closeConfirmationPopUp").click();
		if (utils.getLocator("QualificationCriteriaPage.confirmationPopup").isDisplayed()) {
			logger.info("Confirmation Popup does not disappear on tapping the x icon");
			TestListeners.extentTest.get().info("Confirmation Popup does not disappear on tapping the x icon");
		} else {
			logger.info("Confirmation Popup disappeared on tapping the x icon");
			TestListeners.extentTest.get().info("Confirmation Popup disappeared on tapping the x icon");
		}
		return text;

	}

	public String verifyReceiptTagVisibleOnGuestTimeline(String rctTag, String key) {
		// boolean flag=false;
		String val = "";
		// String xpath=
		// utils.getLocatorValue("guestTimeLine.receiptTag").replace("$receiptTag",rctTag);
		for (int i = 1; i <= 20; i++) {
			utils.longWaitInSeconds(2);
			utils.refreshPageWithCurrentUrl();
			utils.waitTillPagePaceDone();
			try {
				WebElement ele = driver.findElement(By.xpath("//div[contains(text(),'(" + key + ")')]"));
				val = ele.getText();
				if (val.contains(rctTag)) {
					// flag=true;
					break;
				}
			} catch (Exception e) {

			}
		}
		return val;

	}

	public void deactivateTagFromQc(String qcname) {
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").clear();
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(qcname);
		utils.getLocator("QualificationCriteriaPage.qcSearchbox").sendKeys(Keys.ENTER);
		utils.getLocator("QualificationCriteriaPage.qcNameLink").click();
		utils.getLocator("QualificationCriteriaPage.deactivateTag").click();
		logger.info("Tag has been deactivated successfully.");
		TestListeners.extentTest.get().info("Tag has been deactivated successfully.");
	}

	public void removeLisFromQc(String lisName) {
		String xpath = utils.getLocatorValue("QualificationCriteriaPage.removeLISfromQc").replace("$lisName", lisName)
				+ "[1]";
		WebElement el = driver.findElement(By.xpath(xpath));
		el.click();
		logger.info("Line item selector " + lisName + " has been removed succsessfully");
		TestListeners.extentTest.get().info("Line item selector " + lisName + " has been removed succsessfully");
	}

	public void setLis(boolean flagQC, String lineItemSelectorName) {
		// utils.getLocator("QualificationCriteriaPage.LineItemFiltersdrpdwn").click();
		selUtils.longWait(100);
		utils.getLocator("QualificationCriteriaPage.addlineItemfiltersSearchBox").sendKeys(lineItemSelectorName);
		List<WebElement> lineItemfilters = utils.getLocatorList("QualificationCriteriaPage.LineItemList");
		for (int i = 0; i < lineItemfilters.size(); i++) {
			if (lineItemfilters.get(i).getText().equals(lineItemSelectorName)) {
				lineItemfilters.get(i).click();
				break;
			}
		}
		logger.info("Line Item selector added successfully : " + lineItemSelectorName);
		TestListeners.extentTest.get().info("Line Item selector added successfully : " + lineItemSelectorName);
	}

	public String getFirstSelectedLineItemFilterName() {
		String defaultItem = "";
		utils.implicitWait(1);
		utils.scrollToElement(driver, utils.getLocator("QualificationCriteriaPage.selectedLineItemFilter_Xpath"));
		utils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.selectedLineItemFilter_Xpath"));
		Select select = new Select(utils.getLocator("QualificationCriteriaPage.selectedLineItemFilter_Xpath"));
		WebElement option = select.getFirstSelectedOption();
		defaultItem = option.getText();
		logger.info("Default Line Item Filter is : " + defaultItem);
		TestListeners.extentTest.get().info("Default Line Item Filter is : " + defaultItem);
		utils.implicitWait(50);
		return defaultItem;

	}

	public String getFirstSelectedLineItemQualifierName() {
		utils.implicitWait(1);
		utils.scrollToElement(driver,
				utils.getLocator("QualificationCriteriaPage.selectedLineItemFilterItemQualifier_Xpath"));
//utils.waitTillElementToBeClickable(
//    utils.getLocator("QualificationCriteriaPage.selectedLineItemFilterItemQualifier_Xpath"));
//utils.scrollToElement(driver,
//    utils.getLocator("QualificationCriteriaPage.selectedLineItemFilterItemQualifier_Xpath"));
		Select select = new Select(
				utils.getLocator("QualificationCriteriaPage.selectedLineItemFilterItemQualifier_Xpath"));
		WebElement option = select.getFirstSelectedOption();
		String defaultItem = option.getText();
		logger.info("Item Qualifier >  Line Item Filter is : " + defaultItem);
		TestListeners.extentTest.get().info("Item Qualifier >  Line Item Filter is : " + defaultItem);
		utils.implicitWait(50);
		return defaultItem;

	}

	public String getSelectedProcessingFunctionsValue() {
		utils.implicitWait(1);
		utils.scrollToElement(driver, utils.getLocator("QualificationCriteriaPage.selectProcessingFunctionXpath"));
		utils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.selectProcessingFunctionXpath"));
		utils.scrollToElement(driver, utils.getLocator("QualificationCriteriaPage.selectProcessingFunctionXpath"));
		Select select = new Select(utils.getLocator("QualificationCriteriaPage.selectProcessingFunctionXpath"));
		WebElement option = select.getFirstSelectedOption();
		String defaultItem = option.getText();
		logger.info(defaultItem + " Processing Function is selected ");
		TestListeners.extentTest.get().info(defaultItem + " Processing Function is selected ");
		utils.implicitWait(50);
		return defaultItem;

	}

	public String verifyErrorMessageForNegativeValue(String labelName) {
		utils.longWaitInSeconds(3);
		String xpath = "//label[text()='" + labelName + "']/following-sibling::div//span[@class='error']";
		String errorMessage = driver.findElement(By.xpath(xpath)).getText();
		return errorMessage;

	}

	public void enterValueInInputBox(String labelName, String value) {
		utils.longWaitInSeconds(3);
		String xpath = "//label[text()='" + labelName + "']/following-sibling::div//input";
		driver.findElement(By.xpath(xpath)).sendKeys(value);
	}

	public void clickOnAddNewQualificationCriteria() {
		WebElement addQCBtn = utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn");
		addQCBtn.click();
		logger.info("clicked add qc button");
		TestListeners.extentTest.get().info("clicked add qc button");
	}

	public List<String> getEffectivelocationDrpList() {
		utils.getLocator("QualificationCriteriaPage.effectiveLocationDrpDwn").click();
		List<WebElement> ele = utils.getLocatorList("QualificationCriteriaPage.effectiveLocationDrpList");
		List<String> options = ele.stream().map(s -> s.getText()).distinct().sorted().collect(Collectors.toList());
		utils.getLocator("QualificationCriteriaPage.effectiveLocationDrpDwn").click();
		return options;
	}

	public String getAlreadyAttachedEffectiveLocationValue(String lgName) {
		String xpath = utils.getLocatorValue("QualificationCriteriaPage.existingEffectiveLocation").replace("$temp",
				lgName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		String val = ele.getDomAttribute("title").trim();
		return val;
	}

	public void setEfectivelocationQC(String locationGrpName) {
		utils.getLocator("QualificationCriteriaPage.effectiveLocationDrpDwn").click();
		utils.getLocator("QualificationCriteriaPage.efectiveLocationSeachBox").clear();
		utils.getLocator("QualificationCriteriaPage.efectiveLocationSeachBox").sendKeys(locationGrpName);
		String xpath = utils.getLocatorValue("QualificationCriteriaPage.efectiveLocationSeachedVal").replace("$temp",
				locationGrpName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		ele.click();
		logger.info("Effective location is set as :" + locationGrpName);
		TestListeners.extentTest.get().pass("Effective location is set as :" + locationGrpName);
	}

	public void selectItemQualifierOperatorStrategy(String itemQualifierOperator) {
		WebElement itemQualifierOperatorStrategy = utils
				.getLocator("QualificationCriteriaPage.itemQualifierOperatorStrategy");
		Select dropdown = new Select(itemQualifierOperatorStrategy);
		dropdown.selectByVisibleText(itemQualifierOperator);

		// Validation after selection
		String selected = dropdown.getFirstSelectedOption().getText().trim();
		Assert.assertEquals(selected, itemQualifierOperator,
				"Logical Operator Strategy not correctly selected. Expected: " + itemQualifierOperator + ", Found: "
						+ selected);
	}

	public void selectSelect2Option(WebDriver driver, String fieldId, String optionText) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		// Step 1: Click the visible Select2 box (this opens the dropdown)
		WebElement select2Container = driver.findElement(By.xpath("//span[@id='select2-" + fieldId
				+ "-container']/ancestor::span[contains(@class,'select2-container')]"));
		select2Container.click();

		// Step 2: Wait for dropdown options to appear
		WebElement option = wait.until(ExpectedConditions.elementToBeClickable(By
				.xpath("//li[contains(@class,'select2-results__option') and normalize-space()='" + optionText + "']")));

		// Step 3: Click the desired option
		option.click();
	}

	public void fillQualificationCriterionForm(String qualificationCriterion, String receiptTagName, String amtCap,
			String percentageOfProcessedAmount, String processingFunction, String roundingRule,
			String maxDiscountedUnits, String unitDiscount, String minimumUnitRate, String effectiveLocation,
			boolean turnOnDiscountStacking, boolean enableReuseOfQualifyingItemsFlag,
			List<Map<String, String>> lineItemFilters, boolean enableMenuItemAggregatorFlag,
			String aggregateFunctionName, String itemQualifierOperator, List<Map<String, String>> itemQualifiers)
			throws InterruptedException {

		selUtils.implicitWait(10);

		if (!checkQualificationCriteriaIsExist(qualificationCriterion)) {

			utils.waitTillElementToBeClickable(utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn"));
			utils.getLocator("QualificationCriteriaPage.QualificationCriteriaBtn").click();

			fillMandatoryFields(qualificationCriterion, processingFunction);

			// Optional text fields
			fillOptionalTextField("QualificationCriteriaPage.ReceiptTagName", receiptTagName);
			fillOptionalTextField("QualificationCriteriaPage.AmountCap", amtCap);
			fillOptionalTextField("QualificationCriteriaPage.percentageAmount", percentageOfProcessedAmount);
			fillOptionalTextField("QualificationCriteriaPage.minimumUnitRate", minimumUnitRate);

			// Optional dropdowns
			if (roundingRule != null) {
				utils.selectDrpDwnValue(utils.getLocator("QualificationCriteriaPage.roundingRuleDropdown"),
						roundingRule);
			}

			fillOptionalTextField("QualificationCriteriaPage.maxDiscountUnits", maxDiscountedUnits);
			fillOptionalTextField("QualificationCriteriaPage.unitDiscount", unitDiscount);

			if (effectiveLocation != null)
				setEfectivelocationQC(effectiveLocation);

			setCheckboxField("QualificationCriteriaPage.turnOnDiscountStackingCheckBox", turnOnDiscountStacking);
			setCheckboxField("QualificationCriteriaPage.enableReuseOfQualifyingItemsCheckBox",
					enableReuseOfQualifyingItemsFlag);

			if (lineItemFilters != null)
				fillLineItemFilters(lineItemFilters);

			// Menu item aggregator & aggregate function
			setCheckboxField("QualificationCriteriaPage.enableAggregator", enableMenuItemAggregatorFlag);
			selectAggregateFunction(aggregateFunctionName);

			if (itemQualifierOperator != null)
				selectItemQualifierOperatorStrategy(itemQualifierOperator);
			fillItemQualifiers(itemQualifiers);

			utils.getLocator("QualificationCriteriaPage.UpdateButton").click();
		} else {
			logger.info(qualificationCriterion + " QC Selector already exists ");
			TestListeners.extentTest.get().pass(qualificationCriterion + " QC Selector already exists ");
		}
	}

	private void fillLineItemFilters(List<Map<String, String>> filters) {
		for (int i = 0; i < Math.min(filters.size(), 5); i++) {
			Map<String, String> filter = filters.get(i);

			if (filter.containsKey("selector")) {
				WebElement selectorDropdown = driver.findElement(By.xpath(String.format(
						"//select[@id='qualification_criterion_discounting_expressions_attributes_%d_line_item_selector_id']",
						i)));
				utils.selectDrpDwnValue(selectorDropdown, filter.get("selector"));
			}

			if (filter.containsKey("processing_method")) {
				WebElement methodDropdown = driver.findElement(By.xpath(String.format(
						"//select[@id='qualification_criterion_discounting_expressions_attributes_%d_processing_method']",
						i)));
				utils.selectDrpDwnValue(methodDropdown, filter.get("processing_method"));
			}

			if (filter.containsKey("quantity")) {
				String fieldId = String.format("qualification_criterion_discounting_expressions_attributes_%d_quantity",
						i);
				selectSelect2Option(driver, fieldId, "Qty: " + filter.get("quantity"));
			}
		}
	}

	private void fillMandatoryFields(String qualificationCriterion, String processingFunction) {
		WebElement criterionField = utils.getLocator("QualificationCriteriaPage.QualificationCriteriaName");
		selUtils.clearAndSendKeys(criterionField, qualificationCriterion);

		selectProcessingFunction(processingFunction);
	}

	private void fillOptionalTextField(String locatorKey, String value) {
		if (value != null) {
			WebElement field = utils.getLocator(locatorKey);
			selUtils.clearAndSendKeys(field, value);
		}
	}

	private void setCheckboxField(String locatorKey, boolean value) {
		WebElement checkbox = utils.getLocator(locatorKey);
		selUtils.setCheckbox(checkbox, value, driver);
	}

	private void fillItemQualifiers(List<Map<String, String>> itemQualifiers) {
		if (itemQualifiers == null || itemQualifiers.isEmpty()) {
			return;
		}

		for (int i = 0; i < Math.min(itemQualifiers.size(), 5); i++) {
			Map<String, String> qualifier = itemQualifiers.get(i);

			// --- Expression Type ---
			if (qualifier.containsKey("type")) {
				String xpath = String.format(
						"//*[@id='qualification_criterion_qualifying_expressions_attributes_%d_expression_type']", i);
				WebElement dropdown = driver.findElement(By.xpath(xpath));
				utils.selectDrpDwnValue(dropdown, qualifier.get("type"));
				logger.info("Selected Expression Type for index " + i + ": " + qualifier.get("type"));
			}

			// --- Line Item Selector ---
			if (qualifier.containsKey("selector")) {
				String xpath = String.format(
						"//*[@id='qualification_criterion_qualifying_expressions_attributes_%d_line_item_selector_id']",
						i);
				WebElement selectorDropdown = driver.findElement(By.xpath(xpath));
				utils.selectDrpDwnValue(selectorDropdown, qualifier.get("selector"));
				logger.info("Selected line item selector for index " + i + ": " + qualifier.get("selector"));
			}

			// --- Net Value ---
			if (qualifier.containsKey("netValue")) {
				String xpath = String
						.format("//*[@id='qualification_criterion_qualifying_expressions_attributes_%d_net_value']", i);
				WebElement netValueField = driver.findElement(By.xpath(xpath));
				selUtils.clearAndSendKeys(netValueField, qualifier.get("netValue"));
				logger.info("Entered Net Value for index " + i + ": " + qualifier.get("netValue"));
			}
		}
	}

	private void selectAggregateFunction(String aggregateFunctionName) {
		if (aggregateFunctionName == null || aggregateFunctionName.isEmpty()) {
			return; // Nothing to select
		}

		List<WebElement> aggregatorOptions = utils.getLocatorList("QualificationCriteriaPage.aggregatorOptions");

		for (WebElement option : aggregatorOptions) {
			String value = option.getText().trim();
			if (value.equalsIgnoreCase(aggregateFunctionName)) {
				logger.info("Selected aggregate function: " + value);
				option.click();
				break; // Stop after selecting the correct option
			}
		}

		// Optional: wait to ensure selection is applied
		utils.longWaitInSeconds(2);
	}

	public String getLogicalOperatorHintText() {
		WebElement hintText = utils.getLocator("QualificationCriteriaPage.logicalOperatorHint");
		return hintText.getText().trim();
	}

}
