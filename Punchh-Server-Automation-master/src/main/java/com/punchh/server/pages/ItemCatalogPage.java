package com.punchh.server.pages;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.asserts.SoftAssert;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

@Listeners(TestListeners.class)
public class ItemCatalogPage {
	static Logger logger = LogManager.getLogger(ItemCatalogPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public ItemCatalogPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void setValueInSearchBox1(String drpValue, String value) throws InterruptedException {
//		utils.clickByJSExecutor(driver, utils.getLocator("ItemCatalogPage.itemCatalogSearchDrp1"));
		Thread.sleep(2000);
		utils.getLocator("ItemCatalogPage.itemCatalogSearchDrp1").click();
//		Thread.sleep(2000);
		List<WebElement> elem = utils.getLocatorList("ItemCatalogPage.itemCatalogSearchList1");
		utils.selectListDrpDwnValue(elem, drpValue);
		logger.info("Entered Drop down valuse is :" + drpValue);
		TestListeners.extentTest.get().pass("Entered Drop down valuse is :" + drpValue);
		utils.clickByJSExecutor(driver, utils.getLocator("ItemCatalogPage.searchBox1"));
//		utils.getLocator("ItemCatalogPage.searchBox1").click();
		utils.getLocator("ItemCatalogPage.searchBox1").sendKeys(value);
		logger.info("Entered first textbox value is :" + value);
		TestListeners.extentTest.get().pass("Entered first textbox value is :" + value);
	}

	public void setValueInSearchBox2(String drpValue, String value) {
		utils.waitTillElementToBeClickable(utils.getLocator("ItemCatalogPage.itemCatalogSearchDrp2"));
		utils.getLocator("ItemCatalogPage.itemCatalogSearchDrp2").click();
		List<WebElement> elem = utils.getLocatorList("ItemCatalogPage.itemCatalogSearchList2");
		utils.selectListDrpDwnValue(elem, drpValue);
		logger.info("Entered Drop down valuse is :" + drpValue);
		TestListeners.extentTest.get().pass("Entered Drop down valuse is :" + drpValue);
		utils.getLocator("ItemCatalogPage.searchBox2").click();
		utils.getLocator("ItemCatalogPage.searchBox2").sendKeys(value);
		logger.info("Entered first textbox value is :" + value);
		TestListeners.extentTest.get().pass("Entered first textbox value is :" + value);
	}

	public void setValueInSearchBox3(String drpValue, String value) {
		utils.getLocator("ItemCatalogPage.itemCatalogSearchDrp3").click();
		List<WebElement> elem = utils.getLocatorList("ItemCatalogPage.itemCatalogSearchList3");
		utils.selectListDrpDwnValue(elem, drpValue);
		logger.info("Entered Drop down valuse is :" + drpValue);
		TestListeners.extentTest.get().pass("Entered Drop down valuse is :" + drpValue);
		utils.getLocator("ItemCatalogPage.searchBox3").click();
		utils.getLocator("ItemCatalogPage.searchBox3").sendKeys(value);
		logger.info("Entered first textbox value is :" + value);
		TestListeners.extentTest.get().pass("Entered first textbox value is :" + value);
	}

	public void setNumberOfEntriesPerPage(String entries) {
		String xpath = utils.getLocatorValue("ItemCatalogPage.numberOfEntriesPerPage").replace("$page", entries);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("Number Of Entries Per Page is selected as :" + entries);
		TestListeners.extentTest.get().pass("Number Of Entries Per Page is selected as :" + entries);
	}

	public void clickOnResetButton() {
		utils.getLocator("ItemCatalogPage.clickOnResetButton").click();
		logger.info("Clicked On Reset Button");
		TestListeners.extentTest.get().pass("Clicked On Reset Button");
	}

	public void clickOnSearchButton() throws InterruptedException {
		utils.getLocator("ItemCatalogPage.clickOnSearchButton").click();
		logger.info("Clicked On Searched Button");
		TestListeners.extentTest.get().pass("Clicked On Searched Button");
		Thread.sleep(2000);
	}

	public boolean clickOnCopyButton()
			throws HeadlessException, UnsupportedFlavorException, IOException, InterruptedException {
		utils.getLocator("ItemCatalogPage.clickOnCopyButton").click();
		Thread.sleep(2000);
		driver.switchTo().alert().accept();
		logger.info("Clicked On Copy Button");
		TestListeners.extentTest.get().pass("Clicked On Copy Button");
		String myText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		if (myText != null) {
			return true;
		} else {
			return false;
		}
	}

	public void verifyItemCatalogList(String col1, String col2, String col3, String axtualText1, String axtualText2,
			String axtualText3) throws InterruptedException {

		for (int i = 1; i <= 5; i++) {
			Thread.sleep(2000);
			String j = Integer.toString(i);
			String xpath1 = utils.getLocatorValue("ItemCatalogPage.verifyItemCatalogList").replace("$row", j)
					.replace("$col", col1);
			String text1 = driver.findElement(By.xpath(xpath1)).getText();
			Assert.assertTrue(axtualText1.equalsIgnoreCase(text1),
					"Actual text is not eqaul to the text found at row " + j + " and colunm " + col1);

			String xpath2 = utils.getLocatorValue("ItemCatalogPage.verifyItemCatalogList").replace("$row", j)
					.replace("$col", col2);
			String text2 = driver.findElement(By.xpath(xpath2)).getText();
			Assert.assertTrue(axtualText2.equalsIgnoreCase(text2),
					"Actual text is not eqaul to the text found at row " + j + " and colunm " + col2);

			String xpath3 = utils.getLocatorValue("ItemCatalogPage.verifyItemCatalogList").replace("$row", j)
					.replace("$col", col3);
			String text3 = driver.findElement(By.xpath(xpath3)).getText();
			Assert.assertTrue(axtualText3.equalsIgnoreCase(text3),
					"Actual text is not eqaul to the text found at row " + j + " and colunm " + col3);

			logger.info("Item catalog table is verified after filtering it by Brand, Department and Category name");
			TestListeners.extentTest.get()
					.pass("Item catalog table is verified after filtering it by Brand, Department and Category name");
		}
	}

	public void clickOnNextPage() {
		utils.waitTillElementToBeClickable(utils.getLocator("ItemCatalogPage.clickOnNextPage"));
		utils.StaleElementclick(driver, utils.getLocator("ItemCatalogPage.clickOnNextPage"));
		// utils.getLocator("ItemCatalogPage.clickOnNextPage").click();
		logger.info("Clicked on Next Page");
		TestListeners.extentTest.get().pass("Clicked on Next Page");
	}

}