package com.punchh.server.pages;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PpccSettingsPage {

    static Logger logger = LogManager.getLogger(PpccSettingsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	WebDriverWait wait;

	public PpccSettingsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(10));
	}

    public WebElement getPpccSettingsPage() {
        logger.info("Navigating to PPCC Settings Page");
        WebElement ppccSettingsPage = utils.getLocator("ppccSettingsPage.settingsTab");
        return ppccSettingsPage;
    }

    public void navigateToSettingTab()
    {
        utils.waitTillPagePaceDone();
        getPpccSettingsPage().click();
    }

    public WebElement getSelectedPosSearchField()
    {
        logger.info("Getting the selected POS search field");
        WebElement selectedPosSearchField = utils.getLocator("ppccSettingsPage.selectedPosSearchField");
        return selectedPosSearchField;
    }

    public WebElement getSelectPosSearchField()
    {
        logger.info("Getting the selected POS search button");
        WebElement selectPosSearchField = utils.getLocator( "ppccSettingsPage.selectPosSearchField");
        return selectPosSearchField;
    }

    public WebElement getAssignPosToBusinessButton()
    {
        logger.info("Getting the selected POS search result");
        WebElement assignPosToBusiness = utils.getLocator("ppccSettingsPage.assignPosToBusiness");
        return assignPosToBusiness;
    }

    public WebElement getUnassignPosToBusinessButton()
    {
        logger.info("Getting the selected POS search result text");
        WebElement unassignPosToBusiness = utils.getLocator("ppccSettingsPage.unassignPosToBusiness");
        return unassignPosToBusiness;
    }

    public boolean validateSearchFunctionalityForSelectPos(String searchItem)
    {
        utils.getLocator("ppccSettingsPage.selectPosSearchField").clear();
        utils.getLocator("ppccSettingsPage.selectPosSearchField").sendKeys(searchItem);
        String items = utils.getLocatorValue("ppccSettingsPage.itemEntryInSelectPosColumn");
        boolean result = checkSearchFunctionality(searchItem, items);
        utils.getLocator("ppccSettingsPage.selectPosSearchField").clear();
        return result;
    }

    public boolean validateSearchFunctionalityForSelectedPos(String searchItem)
    {
        utils.getLocator("ppccSettingsPage.selectedPosSearchField").clear();
        utils.getLocator("ppccSettingsPage.selectedPosSearchField").sendKeys(searchItem);
        String items = utils.getLocatorValue("ppccSettingsPage.itemEntryInSelectedPosColumn");
        boolean result = checkSearchFunctionality(searchItem, items);
        utils.getLocator("ppccSettingsPage.selectedPosSearchField").clear();
        return result;
    }

    private boolean checkSearchFunctionality(String searchItem, String items)
    {    
        utils.longWaitInSeconds(1);
        List<WebElement> rows = driver.findElements(By.xpath(items));
        boolean isSearchedItemPresent = true;
        if (rows.isEmpty()) { isSearchedItemPresent = false;}
        else
        {
            for (WebElement row : rows)
            {
                String rowText = row.getText();
                if (!rowText.contains(searchItem))
                {
                    isSearchedItemPresent = false;
                }
            }
        }
        return isSearchedItemPresent;
    }

    public void assignPolicyToBusiness(String posType)
    {
        utils.getLocator("ppccSettingsPage.selectPosSearchField").sendKeys(posType);
        selectPosFromUnassignedPos();
        utils.getLocator("ppccSettingsPage.assignPosToBusiness").click();
    }

    public void unassignPolicyFromBusiness(String posType)
    {
        utils.getLocator("ppccSettingsPage.selectedPosSearchField").clear();
        utils.getLocator("ppccSettingsPage.selectedPosSearchField").sendKeys(posType);
        utils.waitTillPagePaceDone();
        selectPosFromAssignedPos();
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccSettingsPage.unassignPosToBusiness").click();
        utils.waitTillPagePaceDone();
    }

    public void selectPosFromUnassignedPos()
    {
        utils.getLocator("ppccSettingsPage.itemEntryInSelectPosColumn").click();
    }
    
    public void selectPosFromAssignedPos()
    {
        utils.getLocator("ppccSettingsPage.itemEntryInSelectedPosColumn").click();
    }

    public void closePopUp()
    {
        utils.getLocator("ppccViewOnly.closeButton").click();
    }
}
