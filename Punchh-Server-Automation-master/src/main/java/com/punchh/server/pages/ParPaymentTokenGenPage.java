package com.punchh.server.pages;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import java.util.regex.PatternSyntaxException;

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
public class ParPaymentTokenGenPage {
    static Logger logger = LogManager.getLogger(MenuPage.class);
    private WebDriver driver;
    Properties prop;
    Utilities utils;
    SeleniumUtilities selUtils;

    public ParPaymentTokenGenPage(WebDriver driver) {
        this.driver = driver;
        prop = Utilities.loadPropertiesFile("config.properties");
        utils = new Utilities(driver);
        selUtils = new SeleniumUtilities(driver);
    }
    
    public void navigatePaymentPage(String url) {
        try {
            driver.get(url);
            Utilities.longWait(3000);
            // utils.getLocator("instanceLoginPage.punchLogoImg").isDisplayed();
            logger.info("Navigated to instance login page");
            TestListeners.extentTest.get().info("Navigated to Instance login page with url :" + url);
        } catch (Exception e) {
            logger.error("Error in navigating to instance login page: " + e);
            TestListeners.extentTest.get().fail("Error in navigating to instance login page: " + e);
            Assert.fail("== Failed to login to instance ==");
        }
    }
    
    public void clickOnCardDetailsButton() {
        utils.getLocator("ParPaymentTokenGenPage.creditDebitCardDetailsBtn").click();
    }

    public String submitCardDetails(String url, String cardNumber, String expDate, String cvv, String zipcode) throws InterruptedException {
        navigatePaymentPage(url);
        clickOnCardDetailsButton();
        utils.getLocator("ParPaymentTokenGenPage.cardnumberTxtBx").sendKeys(cardNumber);
        utils.getLocator("ParPaymentTokenGenPage.expdateTxtBx").sendKeys(expDate);
        utils.getLocator("ParPaymentTokenGenPage.ccvvTxtBx").sendKeys(cvv);
        utils.getLocator("ParPaymentTokenGenPage.zipTxtBx").sendKeys(zipcode);
        utils.getLocator("ParPaymentTokenGenPage.continueBtn").click();
        Utilities.longWait(2000);
        return Utilities.getPaymentToken(driver.getCurrentUrl());
    }
}