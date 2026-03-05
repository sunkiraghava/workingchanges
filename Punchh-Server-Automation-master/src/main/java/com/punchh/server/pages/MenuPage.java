package com.punchh.server.pages;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.ReadData;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class MenuPage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Properties menuItems_prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	public static Map<?, ?> jsonDetailsMap = null;
	public static int counter = 0;
	private PageObj pageObj;

	private Map<String, By> locators;

	public MenuPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		if (jsonDetailsMap == null) {
			jsonDetailsMap = ReadData.getMenuAndSubMenuDetails(
					System.getProperty("user.dir") + "/resources/Locators/navigation_menu_submenu_items.json");
			counter++;
		}
		pageObj = new PageObj(driver);

		locators = utils.getAllByMap();
	}

	// Click on Menu Options
//	public void clickCampaignsMenu() throws InterruptedException {
//		// utils.getPageLoadTime(utils.getLocator("menuPage.dashboardMenu"),"Business
//		// Selection Page");
//		selUtils.waitTillElementToBeClickable(utils.getLocator("menuPage.dashboardMenu"));
//		Thread.sleep(1000);
//		utils.clickByJSExecutor(driver, utils.getLocator("menuPage.dashboardMenu"));
////		clickDashboardMenu();
////		utils.clickByJSExecutor(driver, utils.getLocator("menuPage.campaignMenu"));
////		selUtils.waitTillElementToBeClickable(utils.getLocator("menuPage.campaignMenu"));
//		utils.getLocator("menuPage.campaignMenu").click();
//		// utils.StaleElementclick(driver, utils.getLocator("menuPage.campaignMenu"));
//		logger.info("Clicked Campaigns Menu");
//	}

//	public void clickCockPitMenu() {
//		clickDashboardMenu();
//		utils.getLocator("menuPage.cockpitMenu").click();
//		logger.info("Clicked Cockpit Menu");
//	}

//	public void clickGuestMenu() {
//		clickDashboardMenu();
//		utils.getLocator("menuPage.guestMenu").click();
//		logger.info("Clicked Guest Menu");
//	}

//	public void clickReportsMenu() throws InterruptedException {
//		clickDashboardMenu();
//		utils.getLocator("menuPage.reportsMenu").click();
//		logger.info("Clicked reports Menu");
//	}

	public void clickDashboardMenu() {
		try {
			utils.implicitWait(3);
			WebElement dashboardMenu = driver.findElement(locators.get("menuPage.dashboardMenu"));
			dashboardMenu.click();
		} catch (Exception e) {
			WebElement dashboardMenuFallback = driver.findElement(By.xpath("//summary[@data-shortcut='d']"));
			dashboardMenuFallback.click();
		}
		utils.implicitWait(50);
		utils.waitTillPagePaceDone();
		TestListeners.extentTest.get().info("Clicked Dashboard Menu");
		logger.info("Clicked dashboard Menu");
	}

//	public void clickSupportMenu() {
//		clickDashboardMenu();
//		utils.getLocator("menuPage.supportMenu").click();
//		logger.info("Clicked Support Menu");
//	}

//	public void clickSettingsMenu() {
//		clickDashboardMenu();
//		utils.getLocator("menuPage.settingMenu").click();
//		logger.info("Clicked Setting Menu");
//	}

//	public void clickWhiteLabelMenu() {
//		utils.getLocator("menuPage.whiteLabelMenu").click();
//		logger.info("Clicked White Label Menu");
//	}

	// Click on SubMenu Options
	public void clickCampaignsLink() {
		WebElement campaignLink = driver.findElement(locators.get("menuPage.campaignLink"));
		campaignLink.click();
		logger.info("Clicked Campaigns Link");
	}

	public void clickCampaignSets() {
		WebElement campaignSetsLink = driver.findElement(locators.get("menuPage.campaignSetsLink"));
		campaignSetsLink.click();
		logger.info("Clicked Campaign Sets Link");

	}

	public void clickLapsedGustTrend() {
		WebElement gustTrendLink = driver.findElement(locators.get("menuPage.gustTrendLink"));
		gustTrendLink.click();
		logger.info("Clicked lapsed guets trend Link");

	}

	public void clickCampaignsBetaLink() {
		WebElement campaignBetaLink = driver.findElement(locators.get("menuPage.campaignBetaLink"));
		campaignBetaLink.click();
		logger.info("Clicked Campaigns Beta Link");

	}

	public void clickMobileConfigurationLink() {
		WebElement mobileConfigurationLink = driver.findElement(locators.get("menuPage.mobileConfigurationLink"));
		mobileConfigurationLink.click();
		logger.info("Clicked Mobile Configuration Link");

	}

	public void clickCouponlookupLink() {
		WebElement couponLookupLink = driver.findElement(locators.get("menuPage.couponLookupLink"));
		couponLookupLink.click();
		logger.info("Clicked Coupon Lookup Link");

	}

	public void clickSchedulesLink() {
		WebElement schedulesLink = driver.findElement(locators.get("menuPage.schedulesLink"));
		schedulesLink.click();
		logger.info("Clicked Schedules Link");

	}

	public void clickEarningLink() {
		// utils.getLocator("menuPage.earningLink").click();
		WebElement earningLink = driver.findElement(locators.get("menuPage.earningLink"));
		utils.clickByJSExecutor(driver, earningLink);
		logger.info("Clicked earning Link");

	}

	public void clickJourneysLink() {
		WebElement journeysLink = driver.findElement(locators.get("menuPage.journeysLink"));
		journeysLink.click();
		logger.info("Clicked Journeys Link");

	}

	public void clickGiftcardsLink() {
		WebElement giftcardsLink = driver.findElement(locators.get("menuPage.giftcardsLink"));
		giftcardsLink.click();
		logger.info("Clicked gift cards link");

	}

	public void clickPaymentReportLink() {
		WebElement paymentreportLink = driver.findElement(locators.get("menuPage.paymentreportLink"));
		paymentreportLink.click();
		logger.info("Clicked payment report link");

	}

	public void clickOnBarcodeLookup() {
		WebElement supportLabel = driver.findElement(locators.get("instanceDashboardPage.supportLabel"));
		supportLabel.isDisplayed();
		selUtils.mouseHoverOverElement(supportLabel);
		WebElement barcodeLookLink = driver.findElement(locators.get("menuPage.barcodeLookLink"));
		barcodeLookLink.click();
		logger.info("Clicked barcode lookup Link");
	}

	public void settingsLink() {
		WebElement settingsMenuLabel = driver.findElement(locators.get("menuPage.settingsMenuLabel"));
		settingsMenuLabel.click();
		logger.info("Clicked Menu label");

	}

	public void adminUsersLink() {
		WebElement adminUsersLink = driver.findElement(locators.get("menuPage.adminUsersLink"));
		adminUsersLink.click();
		logger.info("Clicked admin user link");
		TestListeners.extentTest.get().info("Clicked admin user link");
	}

	public void eclubUsersLink() {
		WebElement eclubGuestsLink = driver.findElement(locators.get("menuPage.eclubGuestsLink"));
		eclubGuestsLink.click();
		logger.info("Clicked eclub user link ");
		TestListeners.extentTest.get().info("Clicked eclub user link");

	}

	public void redemptionLink() {
		WebElement redemptionLogLink = driver.findElement(locators.get("menuPage.redemptionLogLink"));
		redemptionLogLink.click();
		logger.info("Clicked eclub user link ");
		TestListeners.extentTest.get().info("Clicked redemption logs user link");

	}

	public void awaitingMigrationLink() {
		WebElement awaitingMigrationLabel = driver.findElement(locators.get("menuPage.awaitingMigrationLabel"));
		awaitingMigrationLabel.click();
		logger.info("Clicked Awaiting Migration link ");
		TestListeners.extentTest.get().info("Clicked Awaiting Migration link ");

	}

	public void migratedLink() {
		WebElement migratedLabel = driver.findElement(locators.get("menuPage.migratedLabel"));
		migratedLabel.click();
		logger.info("Clicked Migrated link");
		TestListeners.extentTest.get().info("Clicked Migrated link");

	}

	public void segmentBetaLink() {
		WebElement segmentBetaLink = driver.findElement(locators.get("menuPage.segmentBetaLink"));
		segmentBetaLink.click();
		logger.info("Clicked Segment beta link");
		TestListeners.extentTest.get().info("Clicked Segment beta link");

	}

	public void clickLocationsLink() {
		WebElement locationsLink = driver.findElement(locators.get("menuPage.locationsLink"));
		utils.waitTillElementToBeClickable(locationsLink);
		locationsLink.click();
		logger.info("Clicked Locations menu");

	}

	public void clickCockpitDashboardLink() {
		WebElement cockpitDashboardLink = driver.findElement(locators.get("menuPage.cockpitDashboardLink"));
		cockpitDashboardLink.click();
		logger.info("Clicked dashboard Menu");

	}

	public void clickredeemablesLink() {
		WebElement redeemablesLink = driver.findElement(locators.get("menuPage.redeemablesLink"));
		utils.clickByJSExecutor(driver, redeemablesLink);
		logger.info("Clicked redeemables link");

	}

	public void clickLineItemSelectorsLink() {
		WebElement lineItemSelectorLink = driver.findElement(locators.get("menuPage.lineItemSelectorLink"));
		lineItemSelectorLink.click();
		logger.info("Clicked Line item selector link");

	}

	public void clickQualificationCriteriaLink() {
		// utils.getLocator("menuPage.qualificationCriteriaLink").click();
		WebElement qualificationCriteriaLink = driver.findElement(locators.get("menuPage.qualificationCriteriaLink"));
		utils.clickByJSExecutor(driver, qualificationCriteriaLink);
		logger.info("Clicked LQualification criteria link");

	}

	public void hoverOnLeftNavMenue() {
		WebElement dashboardLink = driver.findElement(locators.get("menuPage.dashboardLink"));
		dashboardLink.click();
	}

	public void clickFtpEndPoint() {
		WebElement ftpLink = driver.findElement(locators.get("menuPage.ftpLink"));
		ftpLink.click();
	}

	public void hoverOnMenu() {
		WebElement dashboardLink = driver.findElement(locators.get("menuPage.dashboardLink"));
		selUtils.mouseHoverOverElement(dashboardLink);
	}

//	public boolean checkCampaignBetaMenu() {
//		boolean status = false;
//		try {
//			clickCampaignsMenu();
//			utils.waitTillVisibilityOfElement(utils.getLocator("menuPage.campaignBetaLink"), "not visible");
//			WebElement ele = utils.getLocator("menuPage.campaignBetaLink");
//			status = utils.checkElementPresent(ele);
//		} catch (Exception e) {
//
//		}
//		return status;
//	}

//	public boolean checkdataExportBetaMenu() {
//		boolean status = false;
//		try {
//			clickReportsMenu();
//			utils.mouseHover(driver, utils.getLocator("menuPage.reportsMenu"));
//			selUtils.implicitWait(5);
//			WebElement ele = utils.getLocator("dashboardPage.dataExportBetaMenu");
//			status = utils.checkElementPresent(ele);
//		} catch (Exception e) {
//		}
//		selUtils.implicitWait(50);
//		return status;
//	}

//	public void redeemableLink() {
//		utils.getLocator("menuPage.redeemableLink").click();
//		logger.info("Clicked redemption logs user link");
//		TestListeners.extentTest.get().info("Clicked redemption logs user link");
//	}

	public void emailTemplateLink() {
		WebElement emailtemplateLink = driver.findElement(locators.get("menuPage.emailtemplateLink"));
		emailtemplateLink.click();
		logger.info("Clicked redemption logs user link");
		TestListeners.extentTest.get().info("Clicked redemption logs user link");
	}

	public void clickLink() {
		WebElement campaignLink = driver.findElement(locators.get("menuPage.campaignLink"));
		campaignLink.click();
		logger.info("Clicked Campaigns Link");

	}

	public void hoverOnSubscriptionsMenu() {
		WebElement subscriptionsMenu = driver.findElement(locators.get("menuPage.SubscriptionsMenu"));
		selUtils.mouseHoverOverElement(subscriptionsMenu);
	}

	public void clickSubscriptionPlansLink() {
		WebElement subscriptionPlansLink = driver.findElement(locators.get("menuPage.SubscriptionPlansLink"));
		subscriptionPlansLink.click();
	}

	public void clickSubscriptionsMenuIcon() {
		WebElement subscriptionsMenuIcon = driver.findElement(locators.get("menuPage.SubscriptionsMenuIcon"));
		subscriptionsMenuIcon.click();
	}

	public boolean navigateToSubMenuItem_old(String menueName, String subMenuItem) throws InterruptedException {
		boolean isSubmenueDisplayed = true;
		try {
			String childMenueNameXpath = utils.getLocatorValue("menuPage.childMenueName")
					.replace("$ParentMenuName", menueName).replace("$ChildMenuName", subMenuItem);
			Thread.sleep(3000);
			String submenuXpath = utils.getLocatorValue("menuPage.submenuActiveList").replace("$ParentMenuName",
					menueName);
			List<WebElement> valueList = driver.findElements(By.xpath(submenuXpath));
			boolean activeStatusFlag = false;
			String menuRightSideAngleIconXpath = utils.getLocatorValue("menuPage.menuRightAngleIcon")
					.replace("$ParentMenuName", menueName);
			for (WebElement wEle : valueList) {

				String activeStatus = wEle.getAttribute("class");
				if (activeStatus.equalsIgnoreCase("active")) {
					activeStatusFlag = true;
					break;
				}
			}

			if (!activeStatusFlag) {

				WebElement menuRightSideAngleIcon = driver.findElement(By.xpath(menuRightSideAngleIconXpath));
				menuRightSideAngleIcon.click();
				utils.clickByJSExecutor(driver, utils.getXpathWebElements(By.xpath(childMenueNameXpath)));

			} else if (activeStatusFlag) {
				// String parentMenuNameXpath =
				// utils.getLocatorValue("menuPage.parentMenuName").replace("$ParentMenuName",
				// menueName);
				WebElement menuRightSideAngleIcon = driver.findElement(By.xpath(menuRightSideAngleIconXpath));
				utils.clickByJSExecutor(driver, menuRightSideAngleIcon);
				utils.clickByJSExecutor(driver, menuRightSideAngleIcon);
				utils.clickByJSExecutor(driver, utils.getXpathWebElements(By.xpath(childMenueNameXpath)));
			}
			logger.info("Clicked on  " + menueName + " >>> " + subMenuItem);
			TestListeners.extentTest.get().pass("Clicked on  " + menueName + " >>> " + subMenuItem);
			selUtils.longWait(500);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("subMenuItem is not visible -- " + subMenuItem);
			isSubmenueDisplayed = false;

		}
		return isSubmenueDisplayed;

	}

	public void clickCockpitGuest() {
		WebElement cockpitGuest = driver.findElement(locators.get("menuPage.cockpitGuest"));
		utils.clickByJSExecutor(driver, cockpitGuest);
		// utils.getLocator("menuPage.cockpitGuest").click();
		logger.info("Clicked Cockpit->Guests");
		TestListeners.extentTest.get().info("Clicked Cockpit->Guests");
	}

	public void clickCockpitGiftCardLink() {
		WebElement cockpitGiftCard = driver.findElement(locators.get("menuPage.cockpitGiftCard"));
		utils.clickByJSExecutor(driver, cockpitGiftCard);
//		utils.getLocator("menuPage.cockpitGiftCard").click();
		logger.info("Clicked gift cards link");
		TestListeners.extentTest.get().info("Clicked gift cards link");

	}

	public void clickCockpitPosIntegrationLink() {
		WebElement posIntegrationLink = driver.findElement(locators.get("menuPage.pOSIntegrationLink"));
		utils.clickByJSExecutor(driver, posIntegrationLink);
		logger.info("Clicked gift cards link");
		TestListeners.extentTest.get().info("Clicked gift cards link");

	}

	public void clickWhitelabelServices() {
		WebElement whitelabelServices = driver.findElement(locators.get("menuPage.whitelabelServices"));
		whitelabelServices.click();
		logger.info("Clicked whitelabel services link");
		TestListeners.extentTest.get().info("Clicked whitelabel services link");

	}

	public void clickWhitelabelIntegrationServices() {
		WebElement whitelabelIntegrationServices = driver.findElement(locators.get("menuPage.whitelabelIntegrationServices"));
		utils.clickByJSExecutor(driver, whitelabelIntegrationServices);
//		utils.getLocator("menuPage.whitelabelIntegrationServices").click();
		logger.info("Clicked whitelabel integration services link");
		TestListeners.extentTest.get().info("Clicked whitelabel Integration services link");
		utils.waitTillPagePaceDone();
	}

	public void clickGuestValidation() {
		WebElement guestValidation = driver.findElement(locators.get("menuPage.guestValidation"));
		guestValidation.click();
		logger.info("Clicked Guest Validation");
		TestListeners.extentTest.get().info("Clicked Guest Validation");
	}

	public void pinMenuList() {
		WebElement pinLabel = driver.findElement(locators.get("instanceDashboardPage.pinlabel"));
		pinLabel.isDisplayed();
		pinLabel.click();
		logger.info("pin Menu");
	}

	public void mobileConfigurationslink() {
		WebElement mobileConfigurationLink = driver.findElement(locators.get("menuPage.mobileConfigurationLink"));
		mobileConfigurationLink.click();
		logger.info("Clicked Mobile Configurations link");
		TestListeners.extentTest.get().info("Clicked Mobile Configurations link");
	}

	public void deactivatedGuests() {
		WebElement deactivateGuestsLink = driver.findElement(locators.get("menuPage.deactivateGuestsLink"));
		deactivateGuestsLink.click();
		logger.info("Clicked Deactivated Guests link");
		TestListeners.extentTest.get().info("Clicked Deactivated Guests link");
	}

	public void MembershipLink() {
		WebElement membershipsLink = driver.findElement(locators.get("menuPage.membershipsLink"));
		membershipsLink.click();
		logger.info("Clicked memberships Link");
	}

	public void redeemptionLinkInCockpit() {
		WebElement redeemptionLinkInCockpit = driver.findElement(locators.get("menuPage.redeemptionLinkInCockpit"));
		redeemptionLinkInCockpit.click();
		logger.info("Clicked memberships Link");
	}

	public void integrationServiceLogsLink() {
		WebElement integrationServiveLogsLink = driver.findElement(locators.get("menuPage.integrationServiveLogsLink"));
		integrationServiveLogsLink.click();
		logger.info("Clicked Integration Service Logs");
	}

	public void cockpitCampaign() {
		WebElement cockpitCampaignLink = driver.findElement(locators.get("menuPage.cockpitCampaignLink"));
		cockpitCampaignLink.click();
		logger.info("Clicked campaigns link");

	}

	public void punchhpickuplink() {
		WebElement punchhpickuplink = driver.findElement(locators.get("menuPage.punchhpickuplink"));
		punchhpickuplink.click();
		logger.info("Clicked Punchh pickup link");
		TestListeners.extentTest.get().info("Clicked Punchh pickup link");
	}

	public void iframeConfigurationsLink() {
		WebElement iframeConfigurationsLink = driver.findElement(locators.get("menuPage.iframeConfigurationsLink"));
		iframeConfigurationsLink.click();
		logger.info("Clicked iframe configurations Link");
	}

	public void miscellaneousConfigInCockpit() {
		WebElement miscellaneousConfigLabel = driver.findElement(locators.get("menuPage.miscellaneousConfigLabel"));
		miscellaneousConfigLabel.click();
		logger.info("Clicked Miscellaneous Config Link");
	}

	public void scanCodeLength(String length) {
		WebElement scanCodeLengthInput = driver.findElement(locators.get("menuPage.scanCodelength"));
		scanCodeLengthInput.click();
		scanCodeLengthInput.clear();
		scanCodeLengthInput.sendKeys(length);
		WebElement updateInConfig = driver.findElement(locators.get("menuPage.updateInConfig"));
		updateInConfig.click();
	}

	public void guestIncockpit() {
		WebElement guestIncockpit = driver.findElement(locators.get("menuPage.guestIncockpit"));
		guestIncockpit.click();
		logger.info("Clicked guest in cockpit");
	}

//	public boolean checkLockedAccountMenu() {
//		boolean status = false;
//		try {
//			clickGuestMenu();
//			utils.mouseHover(driver, utils.getLocator("menuPage.guestMenu"));
//			selUtils.implicitWait(9);
//			WebElement ele = utils.getLocator("dashboardPage.lockedAccountManu");
//			status = utils.checkElementPresent(ele);
//		} catch (Exception e) {
//		}
//		selUtils.implicitWait(50);
//		return status;
//	}

	public void clickFeedbackMenu() {
		WebElement feedbackMenu = driver.findElement(locators.get("menuPage.feedbackMenu"));
		feedbackMenu.click();
	}

	public void clickFeedbackMenuLink() {
		WebElement feedbackMenuLink = driver.findElement(locators.get("menuPage.feedbackMenuLink"));
		feedbackMenuLink.click();
		selUtils.longWait(5000);
	}

	public void clickWhitelabelAPIMsg() {
		WebElement whitelabelAPImsg = driver.findElement(locators.get("menuPage.whitelabelAPImsg"));
		whitelabelAPImsg.click();
	}
//	public boolean checkMembershipMenu() {
//		boolean status = false;
//		try {
//			clickSettingsMenu();
//			utils.mouseHover(driver, utils.getLocator("menuPage.settingsMenu"));
//			selUtils.implicitWait(5);
//			WebElement ele = utils.getLocator("dashboardPage.membershipMenu");
//			status = utils.checkElementPresent(ele);
//		} catch (Exception e) {
//		}
//		selUtils.implicitWait(50);
//		return status;
//	}

	public void guestStatsAndAnnualReport(String choice) {
		WebElement membershipPageThreeDots = driver.findElement(locators.get("settingsPage.membershipPageThreeDots"));
		membershipPageThreeDots.click();
		logger.info("Clicked membershipThreeDots");
		switch (choice) {
		case "Annual Report":
			WebElement annualreport = driver.findElement(locators.get("settingsPage.annualreport"));
			annualreport.click();
			logger.info("clicked on annual report");
			TestListeners.extentTest.get().info("clicked on annual report");

			break;
		case "Guest Stats":
			WebElement guestStats = driver.findElement(locators.get("settingsPage.guestStats"));
			guestStats.click();
			logger.info("clicked on guest stats");
			TestListeners.extentTest.get().info("clicked on guest stats");
			break;

		}
	}

	public void dashboardPageMiscellaneousConfig() {
		WebElement misConfig = driver.findElement(locators.get("cockpitDashboardPage.misConfig"));
		misConfig.click();
		logger.info("Miscellaneous Config clicked");
	}

	public void posScoreBoard() {
		WebElement posScoreboardBtn = driver.findElement(locators.get("posStatsPage.posScoreboardBtn"));
		posScoreboardBtn.click();
		logger.info("clicked on POS Scoreboard ");

	}

	public void exportPOS() {
		WebElement exportPOSscoreboard = driver.findElement(locators.get("posStatsPage.exportPOSscoreboard"));
		exportPOSscoreboard.click();
		logger.info("clicked on export POS Scoreboard button");
		TestListeners.extentTest.get().info("clicked on export POS Scoreboard button");
	}

	public boolean flagPresentorNot(String flagName) {
		utils.implicitWait(20);
		boolean flag = true;
		String xpath = utils.getLocatorValue("dashboardPage.flagPresentOrNot").replace("$flagName", flagName);
		List<WebElement> flagElements = driver.findElements(By.xpath(xpath));
		if (flagElements.size() == 0) {
			utils.implicitWait(50);
			flag = false;
		}
		return flag;
	}

	public void clickCockpitLocation() {
		WebElement cockpitLocationLink = driver.findElement(locators.get("menuPage.cockpitLocationLink"));
		utils.clickByJSExecutor(driver, cockpitLocationLink);
		logger.info("clicked on  Cockpit location link");
		TestListeners.extentTest.get().info("clicked on  Cockpit location section");

	}

	// vansham - this function will return the list of submenus of the provided menu
	// item
	public List<String> subMenuItems(String menuName) {
		pinSidenavMenu();
		String xpath = utils.getLocatorValue("menuPage.menuItemName").replace("$temp", menuName);
		WebElement menuItemElement = driver.findElement(By.xpath(xpath));
		menuItemElement.click();
		String xpath2 = utils.getLocatorValue("menuPage.subMenuItemList").replace("$temp", menuName);
		List<WebElement> subMenuItemList = driver.findElements(By.xpath(xpath2));
		List<String> subMenuItems = new ArrayList<>();
		for (WebElement subMenuItem : subMenuItemList) {
			utils.waitTillElementToBeClickable(subMenuItem);
			subMenuItems.add(subMenuItem.getText());
		}
		Collections.sort(subMenuItems);
		return subMenuItems;
	}

	public boolean checkNewNavIsActiveAndClickOnSubMenuItem() {
		boolean flag = false;

		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));
			// Wait for the presence of the navigation element and set flag accordingly
			flag = wait.until(
					ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='punchh_nav_app']"))) != null;
			// Log the result based on flag value
			if (flag) {
				logger.info("New navigation is Active");
				TestListeners.extentTest.get().info("New navigation is Active");
			} else {
				logger.info("New navigation is Not Active");
				TestListeners.extentTest.get().info("New navigation is Not Active");
			}
		} catch (Exception e) {
			// Catching any exceptions, logging, and setting flag to false
			TestListeners.extentTest.get().info("Exception occurred while checking new navigation status");
			flag = false;
		}

		return flag;
	}

	public void pinSidenavMenu() {
		WebElement menuSearchBox = driver.findElement(locators.get("menuPage.menuSearchBox"));
		// click on menu search box
		/*
		 * menuSearchBox.click(); logger.info("Clicked on menu search box");
		 * TestListeners.extentTest.get().info("Clicked on menu search box");
		 */
		utils.mouseHover(driver, menuSearchBox);
		WebElement pinMenu = driver.findElement(locators.get("menuPage.pinMenu"));
		pinMenu.click();
		logger.info("Clicked on pin side menu");
		TestListeners.extentTest.get().info("Clicked on pin side menu");

		// utils.scrolltillEndOfPage(driver);
		// utils.scrollToElement(driver,
		// driver.findElement(By.xpath("//thead//tr//th[text()='Admins']")));
	}

	public boolean searchAndClickONSubmenuItemNewNav(String mainMenuName, String submenuName) {
		boolean flag = false;
		WebElement searchedSubMenuWEle = null;
		// this need to enabled for firefox browser run
		// pinSidenavMenu();
		WebElement menuSearchBox = driver.findElement(locators.get("menuPage.menuSearchBox"));
		// click on menu search box
		menuSearchBox.click();
		logger.info("Clicked on menu search box");
		TestListeners.extentTest.get().info("Clicked on menu search box");
		// enter submenu name
		WebElement subMenuSearch = driver.findElement(locators.get("menuPage.subMenuSearch"));
		subMenuSearch.clear();
		subMenuSearch.sendKeys(submenuName);
		utils.longWaitInMiliSeconds(500); // 0.5 seconds wait
		try {
			if (mainMenuName.equalsIgnoreCase("Cockpit") && submenuName.equalsIgnoreCase("Gift Cards")) {
				searchedSubMenuWEle = driver.findElement(locators.get("menuPage.newNavCockpitGiftCardsSubmenu_Xpath"));
			} else if (mainMenuName.equalsIgnoreCase("Cockpit") && submenuName.equalsIgnoreCase("Payments")) {
				searchedSubMenuWEle = driver.findElement(locators.get("menuPage.newNavCockpitPaymentsSubmenu_Xpath"));
			} else {
				String submenuXpath = utils.getLocatorValue("menuPage.newNavSubmenuXpath").replace("${submenuName}",
						submenuName);
				searchedSubMenuWEle = driver.findElement(By.xpath(submenuXpath));
			}
			utils.waitTillElementToBeClickable(searchedSubMenuWEle);
			utils.clickByJSExecutor(driver, searchedSubMenuWEle);
			logger.info("Clicked on  " + mainMenuName + " >> " + submenuName);
			TestListeners.extentTest.get().pass("Clicked on  " + mainMenuName + " >> " + submenuName);
			flag = true;

		} catch (Exception e) {
			flag = false;
			logger.info("Exception while clicking" + mainMenuName + " >> " + submenuName + ": " + e.getMessage());
			// Assert.fail("Exception while clicking" + mainMenuName + " >> " + submenuName
			// +": " + e.getMessage());
		}
		return flag;
	}

	public boolean searchAndClickONSubmenuItemOldNav(String menueName, String subMenuold) {

		boolean isSubmenuDisplayed = false;
		try {
			String subMenu_old = subMenuold;
			String childMenueNameXpath = utils.getLocatorValue("menuPage.childMenueName")
					.replace("$ParentMenuName", menueName).replace("$ChildMenuName", subMenu_old);
			String submenuXpath = utils.getLocatorValue("menuPage.submenuActiveList").replace("$ParentMenuName",
					menueName);
			List<WebElement> valueList = driver.findElements(By.xpath(submenuXpath));
			boolean activeStatusFlag = false;
			String menuRightSideAngleIconXpath = utils.getLocatorValue("menuPage.menuRightAngleIcon")
					.replace("$ParentMenuName", menueName);
			for (WebElement wEle : valueList) {

				String activeStatus = wEle.getAttribute("class");
				if (activeStatus.equalsIgnoreCase("active")) {
					activeStatusFlag = true;
					break;
				}
			}

			if (!activeStatusFlag) {

				WebElement menuRightSideAngleIcon = driver.findElement(By.xpath(menuRightSideAngleIconXpath));
				menuRightSideAngleIcon.click();
				utils.longWaitInSeconds(1);
				utils.clickByJSExecutor(driver, utils.getXpathWebElements(By.xpath(childMenueNameXpath)));

			} else if (activeStatusFlag) {
				// String parentMenuNameXpath =
				// utils.getLocatorValue("menuPage.parentMenuName").replace("$ParentMenuName",
				// menueName);
				WebElement menuRightSideAngleIcon = driver.findElement(By.xpath(menuRightSideAngleIconXpath));
				utils.clickByJSExecutor(driver, menuRightSideAngleIcon);
				utils.clickByJSExecutor(driver, menuRightSideAngleIcon);
				utils.clickByJSExecutor(driver, utils.getXpathWebElements(By.xpath(childMenueNameXpath)));
			}
			logger.info("Clicked on  " + menueName + " >>> " + subMenuold);
			TestListeners.extentTest.get().pass("Clicked on  " + menueName + " >>> " + subMenuold);
			isSubmenuDisplayed = true;
			selUtils.longWait(500);
		} catch (Exception e) {
			isSubmenuDisplayed = false;
			logger.info("Exception while clicking menu >> submenu :" + e.getMessage());
			Assert.fail("Exception while clicking menu >> submenu :" + e.getMessage());
		}
		return isSubmenuDisplayed;
	}

	public boolean navigateToSubMenuItem(String menueName, String subMenuItem) {

		boolean isSubmenuDisplayed = false;
		Map<String, String> innerMap = (Map<String, String>) jsonDetailsMap.get(menueName);
		String childMenuValue = innerMap.get(subMenuItem);

		// boolean isNewNavActive = checkNewNavIsActiveAndClickOnSubMenuItem();
		boolean isNewNavActive = true;
		String childArr[] = childMenuValue.split("/");

		if (isNewNavActive) {
			String subMenu_new = childArr[1];
			// new nav is active and navigate to new submeu items
			isSubmenuDisplayed = searchAndClickONSubmenuItemNewNav(menueName, subMenu_new);

		} else {
			String subMenu_old = childArr[0];
			isSubmenuDisplayed = searchAndClickONSubmenuItemOldNav(menueName, subMenu_old);
		}
		logger.info("Navigated to " + menueName + " -> " + subMenuItem);
		TestListeners.extentTest.get().info("Navigated to " + menueName + " -> " + subMenuItem);
		return isSubmenuDisplayed;
	}

	public int getMassCampaignPageNo(String massCampaignName) {
		int counter = 0;
		int maxCount = 0;
		boolean isCampaignPresent = false;
		String body = "";
		while (!isCampaignPresent && maxCount < 30) {
			counter++;
			maxCount++;
			Response loginResponse = pageObj.endpoints().getCampaignsFromSuperAdminScheduledPage(counter);
			Assert.assertEquals(loginResponse.getStatusCode(), 200);
			body = loginResponse.body().asString();
			isCampaignPresent = body.contains(massCampaignName);

		}
		if (maxCount >= 30) {
			logger.error("Campaign " + massCampaignName + " is not present in Superadmin Mass Campaign Page");
			Assert.fail("Campaign " + massCampaignName + " is not present in Superadmin Mass Campaign Page");
		}
		logger.info("Campaign " + massCampaignName + " is present on page number: " + counter
				+ " in Superadmin Mass Campaign Page");
		return counter;
	}

	public boolean checkIfSubMenuPresentInMenuItems(List<String> menuItems, String subMenuItem) {

		boolean isSubmenuItemPresent = false;
		for (String menuItem : menuItems) {
			if (menuItem.equalsIgnoreCase(subMenuItem)) {
				logger.info("Submenu item: " + subMenuItem + " is present in the menu items list");
				TestListeners.extentTest.get()
						.info("Submenu item: " + subMenuItem + " is present in the menu items list");
				return true;
			}
		}
		logger.info("Submenu item: " + subMenuItem + " is not present in the menu items list");
		TestListeners.extentTest.get().info("Submenu item: " + subMenuItem + " is not present in the menu items list");
		return isSubmenuItemPresent;
	}

	public void pinSidenavMenu_OldNav() {
		WebElement lefPanelDashboardIcon = driver.findElement(locators.get("menuPage.lefPanelDashboardIcon"));
		WebElement pinButton = driver.findElement(locators.get("menuPage.pinButtonOldNav"));
		utils.mouseHover(driver, lefPanelDashboardIcon);
		pinButton.click();
		logger.info("Clicked on pin side menu");
		TestListeners.extentTest.get().info("Clicked on pin side menu");
	}

	public List<String> subMenuItems_OldNav(String menuName) {
		pinSidenavMenu_OldNav();
		String xpath = utils.getLocatorValue("segmentBetaPage.segmentEllipsisOption").replace("$temp", menuName);
		WebElement menuItemElement = driver.findElement(By.xpath(xpath));
		menuItemElement.click();
		String listXpath = utils.getLocatorValue("menuPage.listOfMenuItems").replace("$temp", menuName);
		List<WebElement> subMenuItemList = driver.findElements(By.xpath(listXpath));
		List<String> subMenuItems = new ArrayList<>();
		for (WebElement subMenuItem : subMenuItemList) {
			utils.waitTillElementToBeClickable(subMenuItem);
			subMenuItems.add(subMenuItem.getText());
		}
		Collections.sort(subMenuItems);
		return subMenuItems;
	}
}