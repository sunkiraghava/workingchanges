package com.punchh.server.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.punchh.server.utilities.Utilities;

public class GamesPage {

	static Logger logger = LogManager.getLogger(GamesPage.class);
	private WebDriver driver;
	Utilities utils;
	private PageObj pageObj;

	public GamesPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		pageObj = new PageObj(driver);
	}

	// Clicks on any button
	public void clickButton(String locator) {
		WebElement button = utils.getLocator(locator);
		String buttonName = button.getText();
		utils.waitTillElementToBeClickable(button);
		utils.StaleElementclick(driver, button);
		utils.waitTillPagePaceDone();
		utils.logit("Clicked on " + buttonName);
	}

	// Checks if an element is present or not using xpath
	public boolean isPresent(String locator) {
		boolean status = false;
		try {
			utils.implicitWait(1);
			driver.findElement(By.xpath(locator));
			status = true;
		} catch (NoSuchElementException e) {
			status = false;
		}
		utils.implicitWait(50);
		return status;
	}
	
	// Perform edit or copy action on the secret key input field
	public String performSecretKeyAction(String action, String value) throws Exception {
		String btnXpath, copiedValue = "";
		WebElement inputField = utils.getLocator("gamesPage.cataboomJwtSecretKey");
		if (action.equalsIgnoreCase("edit")) {
			btnXpath = utils.getLocatorValue("gamesPage.jwtKeyInputBtn").replace("temp", "1");
			driver.findElement(By.xpath(btnXpath)).click();
			inputField.sendKeys(value);
			utils.logit("Entered secret key: " + value);
		} else if (action.equalsIgnoreCase("copy")) {
			btnXpath = utils.getLocatorValue("gamesPage.jwtKeyInputBtn").replace("temp", "2");
			WebElement copyBtn = driver.findElement(By.xpath(btnXpath));
			copyBtn.click();
			copiedValue = copyBtn.getAttribute("data-key");
			utils.logit("Clicked on copy button and copied value: " + copiedValue);
		}
		return copiedValue;
	}

	// Verifies text on the page
	public boolean verifyTextOnPage(String text) {
		utils.waitTillPagePaceDone();
		String data = driver.getPageSource();
		if (data.contains(text)) {
			utils.logit("Text '" + text + "' is present on the page");
			return true;
		} else {
			utils.logit("Text '" + text + "' is not present on the page");
			return false;
		}
	}
	
	// Navigates to given URL
	public void navigateToURL(String url) {
		driver.navigate().to(url);
		utils.waitTillPagePaceDone();
		utils.logit("Navigated to URL: " + url);
	}

}