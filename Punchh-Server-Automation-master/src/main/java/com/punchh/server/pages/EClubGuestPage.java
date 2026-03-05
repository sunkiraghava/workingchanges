package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class EClubGuestPage {
	static Logger logger = LogManager.getLogger(EClubGuestPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";

	public EClubGuestPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public boolean verifyGuestuploadInEclub(String location, String email) {
		try {
			selUtils.implicitWait(10);
			utils.getLocator("eclubGuestPage.searchTextbox").isDisplayed();
			utils.getLocator("eclubGuestPage.searchTextbox").clear();
			utils.getLocator("eclubGuestPage.searchTextbox").sendKeys(location);
			utils.getLocator("eclubGuestPage.searchButton").click();
			selUtils.implicitWait(3);
			driver.findElement(By.xpath(utils.getLocatorValue("eclubGuestPage.locationLink").replace("temp", location)))
					.isDisplayed();
			driver.findElement(By.xpath(utils.getLocatorValue("eclubGuestPage.locationLink").replace("temp", location)))
					.click();
			if (driver.findElements(By.xpath(utils.getLocatorValue("eclubGuestPage.guestLabel").replace("temp", email)))
					.size() < 1) {
				System.out.println(utils.getLocatorList("eclubGuestPage.nextLink").size());
				while (utils.getLocatorList("eclubGuestPage.nextLink").size() > 0) {
					if (driver
							.findElements(
									By.xpath(utils.getLocatorValue("eclubGuestPage.guestLabel").replace("temp", email)))
							.size() < 1) {
						utils.getLocator("eclubGuestPage.nextLink").click();
						if (driver
								.findElements(By.xpath(
										utils.getLocatorValue("eclubGuestPage.guestLabel").replace("temp", email)))
								.size() > 0)
							break;
					}

				}
			}
			driver.findElement(By.xpath(utils.getLocatorValue("eclubGuestPage.guestLabel").replace("temp", email)))
					.isDisplayed();
			driver.findElement(
					By.xpath(utils.getLocatorValue("eclubGuestPage.guestSuccessLabel").replace("temp", email)))
					.isDisplayed();
			logger.info("Sucessfully verified eclub upload via Dashboard API");
			TestListeners.extentTest.get().pass("Sucessfully verified eclub upload via Dashboard API");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void uploadEclubGuest(String uploadName, String location) {
		utils.getLocator("eclubGuestPage.eclubHeading").isDisplayed();
		utils.getLocator("eclubGuestPage.importLink").isDisplayed();
		utils.getLocator("eclubGuestPage.importLink").click();
		utils.getLocator("eclubGuestPage.uploadEclubGuestHeading").isDisplayed();
		utils.getLocator("eclubGuestPage.bulkUploadNameTextbox").isDisplayed();
		utils.getLocator("eclubGuestPage.bulkUploadNameTextbox").clear();
		utils.getLocator("eclubGuestPage.bulkUploadNameTextbox").sendKeys(uploadName);
		Select sel = new Select(driver.findElement(By.id("bulk_guest_upload_effective_location")));
		sel.selectByVisibleText(location);
	}
	public List<String> getEffectiveLocationGroupList() throws InterruptedException {
		utils.waitTillElementToBeClickable(utils.getLocator("eclubGuestPage.effectiveLocationGroupDrpDwn"));
		utils.getLocator("eclubGuestPage.effectiveLocationGroupDrpDwn").click();
		List<WebElement> elem = utils.getLocatorList("eclubGuestPage.effectiveLocationGroupList");
		List<String> locationList = new ArrayList<String>();
		int col = elem.size();
		for (int i = 0; i < col; i++) {
			String val = elem.get(i).getText();
			locationList.add(val);
		}
		logger.info("Effective Location groups in Upload eClub Guest dropdown are : " + locationList);
		TestListeners.extentTest.get().pass("Effective Location groups in Upload eClub Guest dropdown are : " + locationList);
		return locationList;
	}

	public void clickOnImportLink() {
		utils.getLocator("eclubGuestPage.eclubHeading").isDisplayed();
		utils.getLocator("eclubGuestPage.importLink").isDisplayed();
		utils.getLocator("eclubGuestPage.importLink").click();
		logger.info("Clicked on import link in Eclub Guests Page");
		TestListeners.extentTest.get().info("Clicked on import link in Eclub Guests Page");

	}
	public void clickOnCSVuploadsOnEclubGuestsPage(String csvName) {
		String xpath = utils.getLocatorValue("eclubGuestPage.uploadCSVLink").replace("$temp", csvName);
		WebElement el = driver.findElement(By.xpath(xpath));
		el.isDisplayed();
		el.click();
		logger.info("Clicked on uploded CSV link on Eclub Guests Page : " + csvName);
		TestListeners.extentTest.get().info("Clicked on uploded CSV link on Eclub Guests Page : " + csvName);
	}

	public void clickOnAddListBtn() {
		utils.getLocator("eclubGuestPage.addListButton").isDisplayed();
		utils.getLocator("eclubGuestPage.addListButton").click();
		logger.info("Clicked on Add List button on CSV view page");
		TestListeners.extentTest.get().info("Clicked on Add List button on CSV view page");
	}
    public boolean verifyEffectiveLocationDrpDwn(){
		WebElement ele = utils.getLocator("eclubGuestPage.guestUploadEffectiveLocationDrpDwn");
		return ele.isEnabled();
    }
    public String verifyEffectiveLocationDrpDwnValue(){
		String text = utils.getLocator("eclubGuestPage.effectiveLocationGroupDrpDwn").getText();
		return text;
    }
}
