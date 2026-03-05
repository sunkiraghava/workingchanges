package com.punchh.server.pages;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class GamingLevelPage {

	static Logger logger = LogManager.getLogger(GamingLevelPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public GamingLevelPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void clickOnGamingLevel(String gamingLevel) {
		String xpath = utils.getLocatorValue("gamingLevelPage.gamingLevelLink").replace("$temp", gamingLevel);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("Clicked on Gaming Level dropdown");
		TestListeners.extentTest.get().info("Clicked on Gaming Level dropdown");
	}

	public void createGame(String name, String gameType) {
		utils.getLocator("gamingLevelPage.addNewGameLink").click();
		utils.getLocator("gamingLevelPage.addGameName").sendKeys(name);
		utils.getLocator("gamingLevelPage.gameTypeDropdown").click();
		// utils.getLocator("dashboardPage.searchBox").sendKeys(gameType);
		// List<WebElement> ele = utils.getLocatorList("gamingLevelPage.gameTypeList");
		List<WebElement> ele = utils.getLocatorList("gamingLevelPage.gameTypeList");
		utils.selectListDrpDwnValue(ele, gameType);
		utils.getLocator("gamingLevelPage.noOfImagesOnCard").sendKeys("8");
		utils.getLocator("gamingLevelPage.saveBtn").click();

	}

	public void createGamingLevel(String game, String kind, String level) {
		utils.getLocator("gamingLevelPage.addNewGamingLevelLink").click();
		utils.getLocator("gamingLevelPage.selectGameDropdown").click();
		List<WebElement> ele = utils.getLocatorList("gamingLevelPage.selectGameDropdownList");
		utils.selectListDrpDwnValue(ele, game);
		utils.getLocator("gamingLevelPage.kindDrpdown").click();
		List<WebElement> ele1 = utils.getLocatorList("gamingLevelPage.kindDropdownList");
		utils.selectListDrpDwnValue(ele1, kind);
		utils.getLocator("gamingLevelPage.levelInputBox").sendKeys(level);

	}

	public String getGamingLevelId() {
		String url = driver.getCurrentUrl();
		String gamingLevelId = url.split("/gaming_levels/")[1].split("/")[0];
		logger.info("Gaming Level Id is : " + gamingLevelId);
		TestListeners.extentTest.get().info("Gaming Level Id is : " + gamingLevelId);
		return gamingLevelId;
	}

	public void deleteGaminglevel(String gamingLevel) {
		String xpath = utils.getLocatorValue("gamingLevelPage.deleteGamingLevel").replace("$temp", gamingLevel);
		driver.findElement(By.xpath(xpath)).click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
		logger.info("Gaming level has been deleted");
		TestListeners.extentTest.get().info("Gaming level has been deleted");

	}

}