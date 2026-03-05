package com.punchh.server.mobilePages;

import java.util.List;
import java.util.Properties;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import com.punchh.server.mobileUtilities.AppiumUtils;
import com.punchh.server.utilities.Utilities;
import com.punchh.server.mobileUtilities.mobileUtilities;
import io.appium.java_client.ios.IOSDriver;

public class ApplePassesPage {
	private IOSDriver mobileDriver;
	Properties prop;
	mobileUtilities mobileUtils;
	AppiumUtils appUtils;
	Utilities utils;
	String ssoEmail, temp = "temp";

	public ApplePassesPage(IOSDriver mobileDriver) {
		this.mobileDriver = mobileDriver;
		prop = Utilities.loadPropertiesFile("mobileConfig.properties");
		mobileUtils = new mobileUtilities(this.mobileDriver);
		utils = new Utilities(this.mobileDriver);
	}

	public void clickNextButton() {
		WebElement btnNext = mobileUtils.getLocator("applePasses.nextButton");
		utils.waitTillVisibilityOfElement(btnNext, "Next Button");
		btnNext.click();
		utils.logit("Clicked on Next Button");
	}

	public void clickAddButton() {
		WebElement addBtn = mobileUtils.getLocator("applePasses.addButton");
		utils.waitTillVisibilityOfElement(addBtn, "Apple Pass Add Button");
		utils.logit("Apple Pass Add Button is visible");
		addBtn.click();
		utils.longWaitInSeconds(5);
		utils.logit("Clicked on Apple Pass Add Button");
	}


	public void clickAgreeButton() {
		WebElement agreeBtn = mobileUtils.getLocator("applePasses.agreeButton");
		utils.waitTillVisibilityOfElement(agreeBtn, "Agree Button");
		agreeBtn.click();
		utils.longWaitInSeconds(10);
		utils.logit("Clicked on Agree Button");
	}

	public void enterName(String name) {
		WebElement nameTxtBx = mobileUtils.getLocator("applePasses.nameTxtBox");
		utils.waitTillVisibilityOfElement(nameTxtBx, "Name Text Box");
		nameTxtBx.sendKeys(name);
		utils.logit("Entered name in Name Field");
	}

	public void enterEmail(String email) {
		WebElement emailTxtBx = mobileUtils.getLocator("applePasses.emailTxtBox");
		utils.waitTillVisibilityOfElement(emailTxtBx, "Email Text Box");
		emailTxtBx.sendKeys(email);
		utils.logit("Entered email in Email Field");
	}

	public void openAppleWallet() {
		mobileDriver.activateApp("com.apple.Passbook");
		utils.logit("Opened Apple Wallet successfully");
	}

	public boolean isPassPresentInWallet() {
		List<WebElement> passes = mobileUtils.getLocatorList("applePasses.pass");
		boolean isPresent = !passes.isEmpty();
		utils.logit("Pass present in Apple Wallet: " + isPresent);
		return isPresent;
	}

	public String getDetailsOfApplePassInWallet(String attribute) {
		String details = "";
		WebElement ele = mobileUtils.getLocator("applePasses.detailsOfPass");
		details =  ele.getAttribute(attribute) ;
		utils.logit("Details of Apple Pass ---- " + details);
		return details;
	}
}
