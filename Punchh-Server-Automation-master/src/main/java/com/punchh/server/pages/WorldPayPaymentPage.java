package com.punchh.server.pages;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class WorldPayPaymentPage {
    static Logger logger = LogManager.getLogger(WorldPayPaymentPage.class);
    private WebDriver driver;
    Properties prop;
    Utilities utils;
    SeleniumUtilities selUtils;

    public WorldPayPaymentPage(WebDriver driver) {
        this.driver = driver;
        prop = Utilities.loadPropertiesFile("config.properties");
        utils = new Utilities(driver);
        selUtils = new SeleniumUtilities(driver);
    }
    
    public void navigatePaymentPage(String url) {
        try {
            driver.get(url);
            Utilities.longWait(3000);
            logger.info("Navigated to instance login page");
            TestListeners.extentTest.get().info("Navigated to Instance login page with url :" + url);
        } catch (Exception e) {
            logger.error("Error in navigating to instance login page: " + e);
            TestListeners.extentTest.get().fail("Error in navigating to instance login page: " + e);
            Assert.fail("== Failed to login to instance ==");
        }
    }
    
    public void submitCardDetails(String url, String cardNum, String month, String year, String cvv) throws InterruptedException {
        navigatePaymentPage(url);
		logger.info("Payment URL is: " + url);
        utils.getLocator("WorldpayPaymentPage.cardnumberTxtBx").sendKeys(cardNum);
        logger.info("Card number entered");
        WebElement expMonth = utils.getLocator("WorldpayPaymentPage.expMonthDrpDwn");
		utils.selectDrpDwnValue(expMonth, month);
		logger.info("Expiry month selected as " + month);
		WebElement expYear = utils.getLocator("WorldpayPaymentPage.expYearDrpDwn");
		utils.selectDrpDwnValue(expYear, year);
		logger.info("Expiry year selected as " + year);
        utils.getLocator("WorldpayPaymentPage.cvvTxtBx").sendKeys(cvv);
        logger.info("CVV entered");
        utils.getLocator("WorldpayPaymentPage.submitBtn").click();
        logger.info("Clicked on Submit button");
    }
    
	public boolean isCaptchaPresent(String url) {
		try {
			navigatePaymentPage(url);
			logger.info("Payment URL is: " + url);
			WebElement captcha = utils.getLocator("WorldpayPaymentPage.captchaTable");
			return captcha.isDisplayed();
		} catch (Exception e) {
			logger.info("Captcha not present");
			return false;
		}
	}
    
	public String getPaymentToken() {
		return utils.getLocator("WorldpayPaymentPage.transactionIdTxtField").getText();
	}
        

}