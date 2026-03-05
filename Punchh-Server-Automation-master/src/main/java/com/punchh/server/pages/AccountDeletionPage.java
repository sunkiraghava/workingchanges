package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AccountDeletionPage {
	static Logger logger = LogManager.getLogger(AccountDeletionPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public AccountDeletionPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

//	public void clickAllowDeactivation()
//	{
//			WebElement ioscheckbox=driver.findElement(By.xpath("//label[text()='iOS']"));
//			System.out.println(driver.findElement(By.xpath("//label[text()='iOS']/../input[2]")).getAttribute("checked"));
//			if(driver.findElement(By.xpath("//label[text()='iOS']/../input[2]")).getAttribute("checked")==null)
//			{
//				driver.findElement(By.xpath("//label[text()='iOS']")).click();
//			}
//		
//		//utils.getLocator("accountDeletionPage.androidDeactivationCheckbox").isSelected();
//	}
	public void clickIosDeactivation() {
		utils.getLocator("accountDeletionPage.iosDeactivationCheckbox").isDisplayed();
		System.out.println(utils.getLocator("accountDeletionPage.iosDeactivationBoxChecked").getAttribute("checked"));
		if (utils.getLocator("accountDeletionPage.iosDeactivationBoxChecked").getAttribute("checked") == null) {
			utils.getLocator("accountDeletionPage.iosDeactivationCheckbox").click();
		}
	}

	public void clickAndroidDeactivation() {
		utils.getLocator("accountDeletionPage.androidDeactivationCheckbox").isDisplayed();
		System.out
				.println(utils.getLocator("accountDeletionPage.androidDeactivationBoxChecked").getAttribute("checked"));
		if (utils.getLocator("accountDeletionPage.androidDeactivationBoxChecked").getAttribute("checked") == null) {
			utils.getLocator("accountDeletionPage.androidDeactivationCheckbox").click();
		}
	}

	public void appDeletionTypeAndReason(String iosDeletionType, String androidDeletionType, String iosDeletionReason) {
		utils.getLocator("accountDeletionPage.iosDeletionTypeDrpdn").click();
		List<WebElement> ele = utils.getLocatorList("accountDeletionPage.iosDeletionTypeList");
		utils.selectListDrpDwnValue(ele, iosDeletionType);
		utils.getLocator("accountDeletionPage.androidDeletionTypeDrpdn").click();
		List<WebElement> elem = utils.getLocatorList("accountDeletionPage.androidDeletionTypeList");
		utils.selectListDrpDwnValue(elem, androidDeletionType);
//		selUtils.jsClick(utils.getLocator("accountDeletionPage.iosDeletionReasonDrpdn"));
		utils.getLocator("accountDeletionPage.iosDeletionReasonDrpdn").click();
		List<WebElement> elements = utils.getLocatorList("accountDeletionPage.iosDeletionReasonList");
		utils.selectListDrpDwnValue(elements, iosDeletionReason);
		logger.info("Mobile configurations updated");
	}

	public void setDeletionRequestEmail() {
		utils.getLocator("accountDeletionPage.deletionRequestEmail").clear();
		utils.getLocator("accountDeletionPage.deletionRequestEmail").sendKeys("abc@gmail.com");
	}

	public String Update() {
		WebElement ele = utils.getLocator("accountDeletionPage.updateMobileConfig");
		utils.StaleElementclick(driver, ele);
		utils.waitTillPagePaceDone();
		utils.waitTillVisibilityOfElement(utils.getLocator("locationPage.getSuccessErrorMessage"), "");
		String messge = "";
		messge = utils.getLocator("locationPage.getSuccessErrorMessage").getText();
		return messge;
	}

}
