package com.punchh.server.pages;

import java.util.Properties;

import com.punchh.server.utilities.TestListeners;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.punchh.server.utilities.Utilities;
import org.testng.annotations.Listeners;

@Listeners(TestListeners.class)
public class UnifiedLoginPage {
    static Logger logger = LogManager.getLogger(UnifiedLoginPage.class);
    private WebDriver driver;
    Properties prop;
    Utilities utils;

    public UnifiedLoginPage(WebDriver driver) {
        this.driver = driver;
        prop = Utilities.loadPropertiesFile("config.properties");
        utils = new Utilities(driver);
    }
    public void clickContinueButton() {
        utils.getLocator("unifiedLoginPage.continueButton").click();
        logger.info("Continue Button is clicked");
        TestListeners.extentTest.get().pass("Continue Button is clicked");
        utils.waitTillPagePaceDone();
    }
    public void enterUsername(String username) {

        WebElement usernameField = utils.getLocator("unifiedLoginPage.usernameField");
        utils.waitTillElementToBeClickable(usernameField);
        usernameField.clear();
        usernameField.sendKeys(username);
        logger.info("User entered username");
        TestListeners.extentTest.get().pass("username is added");
        utils.waitTillPagePaceDone();
    }
    public void enterPassword(String password) {

        WebElement passwordField = utils.getLocator("unifiedLoginPage.passwordField");
        utils.waitTillElementToBeClickable(passwordField);
        passwordField.clear();
        passwordField.sendKeys(utils.decrypt(password));
        logger.info("User entered password");
        TestListeners.extentTest.get().pass("password is added");
        utils.waitTillPagePaceDone();
    }
    public void loginToUnifiedDashboard(String email,String password){
        enterUsername(email);
        clickContinueButton();
        enterPassword(password);
        clickContinueButton();
    }
    public boolean isTextAndLogoPresent(String textLocatorKey, String logoLocatorKey) {
        try {
            return utils.getLocator(textLocatorKey).isDisplayed()
                    && utils.getLocator(logoLocatorKey).isDisplayed();
        } catch (Exception e) {
            logger.info("Exception occurred while verifying text and logo: " + e.getMessage());
            return false;
        }
    }
    public String getErrorMessageOnInvalidCredentials() {
        WebElement errorMessage = utils.getLocator("unifiedLoginPage.errorMessageOnInvalidCredentials");
        logger.info("error message is displayed on entering invalid credentials");
        return errorMessage.getText().trim();
    }
    public void clickOnSignOutOnUnifiedDashboardHomeScreen() {
        WebElement logout = utils.getLocator("unifiedLoginPage.logoutIcon");
        logout.click();
        utils.waitTillPagePaceDone();
        logger.info("Sign Out is clicked.");
    }
}
