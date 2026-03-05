package com.punchh.server.deprecatedTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class JourneysTest {
	static Logger logger = LogManager.getLogger(JourneysTest.class);
	public WebDriver driver;
	PageObj pageObj;
	Utilities utils;
	SeleniumUtilities selUtils;
	String journeysName;
	private static Map<String, String> dataSet;
	private String env;
	private String sTCName;
	String run = "ui";
	Properties prop;

	// NOTE:- Commenting as these are not part of regression
	// pageObj.menupage().clickCampaignsMenu();
	// pageObj.menupage().clickJourneysLink();

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		journeysName = CreateDateTime.getUniqueString("AutoJourney");
		pageObj = new PageObj(driver);
		env = Utilities.getConfigProperty("environment");
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
	}

	@Test(groups = { "regression" }, description = "JB-1082, Validate the Send Email as Start block and Exit")
	public void JourneysSendMailAndExitTest() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
		pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
				Utilities.getConfigProperty("journeysPassword"));
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickJourneysLink();
		pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
		pageObj.journeysPage().DragSendEmailToBlock();
		System.out.println(dataSet.get("subject"));
		pageObj.journeysPage().enterSendEmailDetails(dataSet.get("subject"), dataSet.get("preHeaderText"),
				dataSet.get("segmentOption"), dataSet.get("redeemableValue"), dataSet.get("startDate"),
				dataSet.get("endDate"), dataSet.get("startTime"), dataSet.get("endTime"), dataSet.get("timeZone"));
		pageObj.journeysPage().clickOnCreateSendEmail();
		pageObj.journeysPage().selectExitIcon();
		pageObj.journeysPage().connectSendEmailAndExitBlock();
		pageObj.journeysPage().activateJourney();
		pageObj.journeysPage().verifyJourneyRedirectedToListingPage(journeysName);
	}

	@Test(groups = { "regression" }, description = "JB-1761, To validate Purchase start block")
	public void JourneysRedeemOfferAsStartBlockTest() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
		pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
				Utilities.getConfigProperty("journeysPassword"));
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickJourneysLink();
		pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
		pageObj.journeysPage().DragRedeemOfferToBlock();
		pageObj.journeysPage().enterReddemOfferDetails(dataSet.get("redeemable"), dataSet.get("couponOffer"),
				dataSet.get("segmentOption"), dataSet.get("location"), dataSet.get("startDate"), dataSet.get("endDate"),
				dataSet.get("startTime"), dataSet.get("endTime"), dataSet.get("timeZone"),
				dataSet.get("purchaseMethod"), dataSet.get("timeOfDay"), dataSet.get("dayOfWeek"));
		pageObj.journeysPage().dragSendEmailToMiddle();
		pageObj.journeysPage().enterSendEmailMiddleBlockDetails(dataSet.get("subject"), dataSet.get("preHeaderText"));
		pageObj.journeysPage().createMiddleSendEmail();
		pageObj.journeysPage().selectExitIcon();
		pageObj.journeysPage().connectRedeemOfferToSendEmail();
		pageObj.journeysPage().connectSendEmailMiddleAndExitBlock();
		pageObj.journeysPage().activateJourney();
		pageObj.journeysPage().verifyJourneyRedirectedToListingPage(journeysName);

	}

	@Test(groups = { "regression" }, description = "JB-1085, Validate Redeem offer block as Start block")
	public void JourneysMadeAPurchaseAsStartBlockTest() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
		pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
				Utilities.getConfigProperty("journeysPassword"));
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
//		pageObj.menupage().clickCampaignsMenu();
//		pageObj.menupage().clickJourneysLink();
		pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
		pageObj.journeysPage().DragMadePurchaseToBlock();
		pageObj.journeysPage().enterMadePurchaseDetails(dataSet.get("segmentOption"), dataSet.get("location"),
				dataSet.get("startDate"), dataSet.get("endDate"), dataSet.get("startTime"), dataSet.get("endTime"),
				dataSet.get("timeZone"), dataSet.get("purchaseMethod"), dataSet.get("timeOfDay"),
				dataSet.get("dayOfWeek"));
		pageObj.journeysPage().dragSendEmailToMiddle();
		pageObj.journeysPage().enterSendEmailMiddleBlockDetails(dataSet.get("subject"), dataSet.get("preHeaderText"));
		pageObj.journeysPage().createMiddleSendEmail();
		pageObj.journeysPage().selectExitIcon();
		pageObj.journeysPage().connectMadeAPurchaseToSendEmail();
		pageObj.journeysPage().connectSendEmailMiddleAndExitBlock();
		pageObj.journeysPage().activateJourney();
		pageObj.journeysPage().verifyJourneyRedirectedToListingPage(journeysName);
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		driver.close();
	}

}
