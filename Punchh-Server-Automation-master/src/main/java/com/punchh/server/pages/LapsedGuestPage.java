package com.punchh.server.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
public class LapsedGuestPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;

	public LapsedGuestPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public String getLapsedGuestsDetails(String NumberOfDays) throws InterruptedException {
		String days = "";
		utils.getLocator("lapsedGuestPage.dateRangeBox").click();
		utils.getLocator("lapsedGuestPage.numberDays").click(); // 180 days
		Thread.sleep(1000);
		utils.getLocator("lapsedGuestPage.listBtn").click();
		String xpath = utils.getLocatorValue("lapsedGuestPage.guestTableRow").replace("temp", NumberOfDays);
		List<WebElement> tableRow = driver.findElements(By.xpath(xpath));
		List<String> guetsData = new ArrayList<String>();
		int col = tableRow.size();
		for (int i = 0; i < col; i++) {
			String val = tableRow.get(i).getText();
			guetsData.add(val);
		}
		String a = guetsData.get(2);
		String guests = a.replaceAll("[a-z A-Z]", "");
		logger.info("Total guests are :" + guests);
		if (Integer.parseInt(guests) >= 1) {
			String s = guetsData.get(0);
			days = s.replaceAll("[a-z A-Z #]", "");
			logger.info("Number of days :" + days);
		}

		return days;
	}

	public void clickLapsedUser() {
		utils.waitTillPagePaceDone();
		utils.getLocator("lapsedGuestPage.dateRangeBox").click();
		utils.getLocator("lapsedGuestPage.numberDays").click();
		utils.longWaitInSeconds(1);
		utils.waitTillPagePaceDone();
		utils.getLocator("lapsedGuestPage.listBtn").click();
		utils.longWaitInSeconds(2);
		utils.getLocator("lapsedGuestPage.threeDaylapsedUsers").click();
		utils.getLocator("lapsedGuestPage.oneDayLapsedUserEmail").click();
		logger.info("clicked on lapsed user email");
		TestListeners.extentTest.get().pass("clicked on lapsed user email");

	}

	public void getUser(String NumberOfDays) {
		String xpath = utils.getLocatorValue("lapsedGuestPage.gustLik").replace("temp", NumberOfDays);
		WebElement LapsedGuestLink = driver.findElement(By.xpath(xpath));
		LapsedGuestLink.click();
		utils.getLocator("lapsedGuestPage.userLink").click();
		utils.getUserIdFromUrl();
	}

	public boolean verifyUserIsExistOnReactivationRequestedPage(String userEmail) {

		boolean flag = false;
		do {
			selUtils.implicitWait(2);
			try {
				List<WebElement> mainList = driver.findElements(By.xpath("//ul[@class='pagination']/li"));
				if (mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
						&& mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
					flag = false;
					String pageNumber = mainList.get(mainList.size() - 3).getText();
					driver.findElement(By.linkText(pageNumber)).click();

				} else if (mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
						&& !mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
					flag = true;
					String pageNumber = mainList.get(mainList.size() - 2).getText();
					driver.findElement(By.linkText(pageNumber)).click();
					break;

				} else if (!mainList.get(mainList.size() - 1).getAttribute("class").equalsIgnoreCase("next")
						&& !mainList.get(mainList.size() - 2).getAttribute("class").equalsIgnoreCase("disabled")) {
					flag = true;
					String pageNumber = mainList.get(mainList.size() - 1).getText();
					driver.findElement(By.linkText(pageNumber)).click();
					break;
				}
			} catch (Exception e) {
				logger.info("Next Button is not available now for clicking . ");
				TestListeners.extentTest.get().pass("Next Button is not available now for clicking . ");
				flag = false;
				break;
			}

		} while (flag == false);

		String userXpath = utils.getLocatorValue("lapsedGuestPage.userIsExist").replace("${userEmail}", userEmail);
		List<WebElement> weleList = driver.findElements(By.xpath(userXpath));
		if (weleList.size() > 0) {
			return true;
		} else {
			return false;
		}

	}

	public void reactivateTheUser(String userEmail) {
		String xpath = utils.getLocatorValue("lapsedGuestPage.reactivateUserButton").replace("${userEmail}", userEmail);
		driver.findElement(By.xpath(xpath)).click();
		driver.switchTo().alert().accept();
		logger.info(userEmail + " user is reactivate on reactivatation requested page ");
		TestListeners.extentTest.get().pass(userEmail + " user is reactivate on reactivatation requested page ");

	}

}
