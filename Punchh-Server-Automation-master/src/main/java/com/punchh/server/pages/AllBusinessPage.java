package com.punchh.server.pages;
// author - vansham

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AllBusinessPage {
	static Logger logger = LogManager.getLogger(AllBusinessPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String allBusinessDescription = "Test all business description";

	public AllBusinessPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void verifyHeader() {
		utils.waitTillPagePaceDone();
		utils.getLocator("allBusinessPage.punchhHeader").isDisplayed();
		logger.info("Header menu is displayed");
		TestListeners.extentTest.get().info("Header menu is displayed");
		utils.getLocator("allBusinessPage.punchhLogo").isDisplayed();
		logger.info("Puchh logo is present in the header");
		TestListeners.extentTest.get().info("Puchh logo is present in the header");
		utils.getLocator("allBusinessPage.avatarIcon").isDisplayed();
		logger.info("Avatar logo of the login user is present on the header of the All business page");
		TestListeners.extentTest.get()
				.pass("Avatar logo of the login user is present on the header of the All business page");
	}

	public boolean searchBusiness(String BusinessName, String businessId) throws InterruptedException, IOException {
		boolean flag1 = false;
		boolean businessListDisplayed = false;
		String url = "";
		utils.getLocator("allBusinessPage.searchSelectBusiness").isDisplayed();
		utils.getLocator("allBusinessPage.searchSelectBusiness").click();
		utils.getLocator("allBusinessPage.businessSearchBox").isDisplayed();
		utils.getLocator("allBusinessPage.businessSearchBox").click();
		utils.getLocator("allBusinessPage.businessSearchBox").sendKeys(BusinessName);
		utils.longWaitInSeconds(2);
		List<WebElement> count = utils.getLocatorList("allBusinessPage.searchBusinessList");
		Integer searchBusinessCount = count.size();

		if (searchBusinessCount == 1) {
			logger.info(searchBusinessCount + " : is the count of search business");
			TestListeners.extentTest.get().info(searchBusinessCount + " : is the count of search business");
			String SearchedBusinessName = utils.getLocator("allBusinessPage.searchBusinessList").getText();
			Assert.assertEquals(SearchedBusinessName, BusinessName, "Searched business is not apprearing");
			logger.info(SearchedBusinessName + " : is the searched business");
			TestListeners.extentTest.get().info(SearchedBusinessName + " : is the searched business");
			businessListDisplayed = true;
			// click on business here
			url = utils.getLocator("allBusinessPage.searchedBusinessUrl").getAttribute("href");
			logger.info("Searched business URL is:- " + url);
			TestListeners.extentTest.get().info("Searched business URL is:- " + url);

		} else if (searchBusinessCount >= 1) {
			logger.info(searchBusinessCount + " : is the count of search business");
			TestListeners.extentTest.get().info(searchBusinessCount + " : is the count of search business");
			List<WebElement> searchedBusinessNames = utils.getLocatorList("allBusinessPage.searchBusinessList");
			for (int i = 0; i < searchedBusinessNames.size(); i++) {
				WebElement ele = searchedBusinessNames.get(i);
				String businessName = ele.getText();
				if (businessName.equalsIgnoreCase(BusinessName)) {
					businessListDisplayed = true;
					logger.info(businessName + " :is the searched business names ");
					TestListeners.extentTest.get().info(businessName + " :is the searched business names ");
					// click on business here
					url = utils.getLocator("allBusinessPage.searchBusinessList").getAttribute("href");
					logger.info("Searched business URL is:- " + url);
					TestListeners.extentTest.get().info("Searched business URL is:- " + url);
				}
			}

		} else if (!businessListDisplayed) {
			logger.info("Business is not found");
			TestListeners.extentTest.get().fail("Business is not found");
			Assert.assertTrue(businessListDisplayed, "Business is not found");
		}
		flag1 = verifySegmentActiveLink(url);
		Assert.assertTrue(flag1, "The provided url is broken");
		logger.info("URl of the searched business is working as expected");
		TestListeners.extentTest.get().info("URl of the searched business is working as expected");
		utils.getLocator("allBusinessPage.searchedBusinessUrl").click();
		utils.waitTillPagePaceDone();
		boolean selectedBusinessFlag = utils.verifyPartOfURL(businessId);
		Assert.assertTrue(selectedBusinessFlag, "Searched Business name and the selected Business name doesn't match");
		logger.info("Searched Business is appeared");
		TestListeners.extentTest.get().pass("Searched Business is appeared");
		driver.navigate().back();
		return selectedBusinessFlag;

	}

	public List<String> verifyFooterLinks() throws IOException {
		boolean flag1 = false;
		boolean flag = true;
		int counter = 0;
		List<WebElement> allLink = utils.getLocatorList("allBusinessPage.footerLinks");
		List<WebElement> actualLinkTexts = utils.getLocatorList("allBusinessPage.actualLinkText");
		logger.info("Total links are " + allLink.size());
		TestListeners.extentTest.get().info("Total links are " + allLink.size());
		List<String> str = new ArrayList<>();
		for (int i = 0; i < allLink.size(); i++) {
			String actualLinkText = actualLinkTexts.get(i).getText().trim();
			str.add(actualLinkText);
			WebElement ele = allLink.get(i);
			String url = ele.getAttribute("href");
			if ((url.contains("javascript:void(0)"))) {
				continue;
			} else {
				flag1 = verifySegmentActiveLink(url);
				if (flag1 == false) {
					counter++;
				}

			}
		}
		if (flag1 == true) {
			logger.info("All " + allLink.size() + " links are working");
			TestListeners.extentTest.get().pass("All " + allLink.size() + " links are working");
		}
		if (counter != 0) {
			flag = false;
		}
		return str;
	}

	public boolean VerifySubmenus() throws IOException {
		utils.waitTillPagePaceDone();
		boolean flag1 = false;
		boolean flag = true;
		int counter = 0;
		List<WebElement> menuItems = utils.getLocatorList("allBusinessPage.navItemName");
		for (int i = 0; i < menuItems.size(); i++) {
			WebElement ele = menuItems.get(i);
			logger.info(ele.getText() + ": nav menu item");
			TestListeners.extentTest.get().info(ele.getText() + ": nav menu item");
		}

		List<WebElement> subMenuItems = utils.getLocatorList("allBusinessPage.submenuItem");
		logger.info("Total submenu links are " + subMenuItems.size());
		TestListeners.extentTest.get().info("Total submenu links are " + subMenuItems.size());
		for (int i = 0; i < subMenuItems.size(); i++) {
			WebElement ele = subMenuItems.get(i);
			String url = ele.getAttribute("href");
			if ((url.contains("javascript:void(0)"))) {
				continue;
			} else {
				flag1 = verifySegmentActiveLink(url);
				if (flag1 == false) {
					counter++;
				}
			}
		}
		if (flag1 == true) {
			logger.info("All " + subMenuItems.size() + " links are working");
			TestListeners.extentTest.get().info("All " + subMenuItems.size() + " links are working");
		}
		if (counter != 0) {
			flag = false;
		}
		return flag;
	}

	public boolean VerifySubMenuSearch(String MenuName) throws IOException {
		boolean displayed = false;
		utils.getLocator("allBusinessPage.menuSearchbar").isDisplayed();
		utils.getLocator("allBusinessPage.menuSearchbar").click();
		utils.getLocator("allBusinessPage.menuSearchbar").sendKeys(MenuName);
		String searchedSubmenuResult = utils.getLocator("allBusinessPage.searchedSubmenuItem").getText();
		Assert.assertTrue(searchedSubmenuResult.equals(MenuName), "Searched menu is not present");
		logger.info(searchedSubmenuResult + ": is the displayed searched sub-menu item");
		TestListeners.extentTest.get().pass(searchedSubmenuResult + ": is the displayed searched sub-menu item");
		displayed = true;
		return displayed;
	}

	public boolean verifySegmentActiveLink(String linkurl) throws IOException {
		boolean flag = false;
		URL url = new URL(linkurl);
		HttpURLConnection httpUrlConnect = (HttpURLConnection) url.openConnection();
		utils.longWaitInSeconds(2);
		httpUrlConnect.setConnectTimeout(3000);
		try {
			httpUrlConnect.connect();
			if (httpUrlConnect.getResponseCode() == 200) {
				flag = true;
				logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage());
				TestListeners.extentTest.get().pass(linkurl + " - " + httpUrlConnect.getResponseMessage());
			}
		} catch (Exception e) {
			logger.info(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - " + HttpURLConnection.HTTP_NOT_FOUND
					+ "-" + httpUrlConnect.getResponseCode());
			TestListeners.extentTest.get().fail(linkurl + " - " + httpUrlConnect.getResponseMessage() + " - "
					+ HttpURLConnection.HTTP_NOT_FOUND + "-" + httpUrlConnect.getResponseCode());
		}

		return flag;
	}
}
