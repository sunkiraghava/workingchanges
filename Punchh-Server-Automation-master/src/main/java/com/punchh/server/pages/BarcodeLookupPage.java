package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;


@Listeners(TestListeners.class)
public class BarcodeLookupPage {
	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public BarcodeLookupPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void selectLocation(String locationName) {
		WebElement selectWele = utils.getLocator("barcodeLookupPage.barcodeLookupLocationDropdown");
		if (!locationName.equals("")) {
			utils.selectDrpDwnValue(selectWele, locationName);
			logger.info(locationName + " Location is selected in Barcode lookup page ");
			TestListeners.extentTest.get().pass(locationName + " Location is selected in Barcode lookup page ");

		} else {
			utils.selectDrpDwnValue(selectWele, "Any");
			logger.info("Any Location is selected in Barcode lookup page ");
			TestListeners.extentTest.get().pass("Any Location is selected in Barcode lookup page ");
		}

	}

	public void enterBarcode(String barcode) {
		utils.getLocator("barcodeLookupPage.barcodeLooupTextBox").clear();
		utils.getLocator("barcodeLookupPage.barcodeLooupTextBox").sendKeys(barcode);
		logger.info(barcode + " barcode is entered Successfully");
		TestListeners.extentTest.get().pass(barcode + " barcode is entered Successfully");

	}

	public void clickOnSubmitButton() {
		utils.getLocator("barcodeLookupPage.barcodeLookupButton").click();
		logger.info("Cliked on Barcode submit button");
		TestListeners.extentTest.get().pass("Cliked on Barcode submit button");

	}

	public String getLookupReceiptDetailsBarcode() {
		String actualReceiptBarcode = utils.getLocator("barcodeLookupPage.receiptBarcode").getAttribute("data-barcode");
		return actualReceiptBarcode;
	}

	public boolean verifyTheColour(String userId, String expectedBackgroundColour) {
		try {
			String bgColor = utils.getLocatorValue("barcodeLookupPage.receiptBarcodeColour").replace("$userId", userId);
			utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(bgColor)), "background color");
			String actualBackgroundColour = driver.findElement(By.xpath(bgColor)).getAttribute("class");
//			String actualBackgroundColour = utils.getLocator("barcodeLookupPage.receiptBarcodeColour")
//					.getAttribute("class");
			Assert.assertEquals(actualBackgroundColour, expectedBackgroundColour);
			logger.info("Verified the expected colour");
			TestListeners.extentTest.get().pass("Verified the expected colour");
			return true;
		} catch (AssertionError ae) {
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean verifyReceiptItems(String expectedItem) {
		boolean flag = false;
		List<WebElement> itemListWEle = utils.getLocatorList("barcodeLookupPage.receiptItemList");
		for (WebElement wEle : itemListWEle) {
			String receipt = wEle.getText();
			if (receipt.equalsIgnoreCase(expectedItem)) {
				logger.info(expectedItem + " item is matched in receipt.");
				TestListeners.extentTest.get().pass(expectedItem + " item is matched in receipt.");
				flag = true;
				break;
			}
		}
		return flag;

	}

}
