package com.punchh.server.pages;

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
public class SurveysPage {
	static Logger logger = LogManager.getLogger(SurveysPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public SurveysPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public void selectSurveys(String EnterTheNameOfSurveys) {
		String surveyNameXpath = utils.getLocatorValue("surveysPage.surveysName");
		surveyNameXpath = surveyNameXpath.replace("${nameOfSurveys}", EnterTheNameOfSurveys);
		WebElement surveyName = driver.findElement(By.xpath(surveyNameXpath));
		surveyName.click();
		logger.info("Clicked on survery to edit");
		TestListeners.extentTest.get().info("Clicked on survery to edit");
	}

	public void clickOnUpdateButton() {
		utils.waitTillPagePaceDone();
		WebElement updateButton = utils.getLocator("surveysPage.updateButton");
		updateButton.click();
	}
	
}