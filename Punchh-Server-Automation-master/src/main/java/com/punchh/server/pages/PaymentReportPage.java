package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PaymentReportPage {

	static Logger logger = LogManager.getLogger(PaymentReportPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public PaymentReportPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void searchbyCardorTxn(String cardNumber) {
		utils.getLocator("paymentreportPage.searchBox").clear();
		utils.getLocator("paymentreportPage.searchBox").sendKeys(cardNumber);
		utils.getLocator("paymentreportPage.searchBox").sendKeys(Keys.ENTER);
	}

	public List<String> getCardDetailsPaymentReportSection() {
		List<String> accountData = new ArrayList<String>();
		List<WebElement> tableRowOne = utils.getLocatorList("paymentreportPage.paymentReporttableRow1");
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			accountData.add(val);
		}
		return accountData;

	}

}
