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
public class MembershipsPage {
	static Logger logger = LogManager.getLogger(MembershipsPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public MembershipsPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);

	}

	public void selectMembership(String enterTheMembershipLevelName) {
		String memershiplabelXpath = utils.getLocatorValue("membershipsPage.membershipLevel");
		memershiplabelXpath = memershiplabelXpath.replace("${membershipLeveLName}", enterTheMembershipLevelName);
		WebElement membershipLevel = driver.findElement(By.xpath(memershiplabelXpath));
		membershipLevel.click();
		logger.info("Membership lavel clicked selected");
		TestListeners.extentTest.get().info("Membership lavel clicked selected");
	}

	public void clickOnMembershipLevelButton() {
		WebElement membershipLevelButton = utils.getLocator("membershipsPage.updateMembershipLebelButton");
		//utils.waitTillInVisibilityOfElement(membershipLevelButton, "Membership Level Button");
		membershipLevelButton.click();
	}

	public void scrollToEnableEmailEditor() {
		utils.scrollToElement(driver, utils.getLocator("membershipsPage.scrollToEnableEmailEditor"));
		utils.logit("Scrolled to Enable Email Editor");
	}
}