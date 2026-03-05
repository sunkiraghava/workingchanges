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
public class JourneysStartAndMidBlockSendEmailTest {
	static Logger logger = LogManager.getLogger(JourneysStartAndMidBlockSendEmailTest.class);
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

	@Test(groups = { "regression" }, description = "Sent email as start block(never repeats, unlimited entry limit), "
			+ "Sent email as mid block(Send this email immediately)")
	public void JourneysSendMailImmidiately() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
		pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
				Utilities.getConfigProperty("journeysPassword"));
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickJourneysLink();
		pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
		pageObj.journeysPage().DragSendEmailToBlock();
		System.out.println(dataSet.get("subject"));
		pageObj.journeysPage().enterSendEmailDetails(dataSet.get("subject"), dataSet.get("preHeaderText"),
				dataSet.get("segmentOption"), dataSet.get("redeemableValue"), dataSet.get("startDate"),
				dataSet.get("endDate"), dataSet.get("startTime"), dataSet.get("endTime"), dataSet.get("timeZone"));
		pageObj.journeysPage().setRepertBehaviourCount(dataSet.get("repeatBehavioutCount"));
		pageObj.journeysPage().setAnyNumberOfIndividualEntryLimit();
		pageObj.journeysPage().clickOnCreateSendEmail();
		pageObj.journeysPage().dragSendEmailToMiddle();
		pageObj.journeysPage().enterSendEmailMiddleBlockDetails(dataSet.get("subject"), dataSet.get("preHeaderText"));
		pageObj.journeysPage().createMiddleSendEmail();
		pageObj.journeysPage().selectExitIcon();
		pageObj.journeysPage().connectSendEmailToSendEmail();
		pageObj.journeysPage().connectSendEmailMiddleAndExitBlock();
		// pageObj.journeysPage().activateJourney();

	}

	@Test(groups = { "regression" }, description = "Sent email as start block(never repeats, Set entry limit), "
			+ "Sent email as mid block(Send this email on date)")
	public void JourneysMidBlockSendMailOnDate() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
		pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
				Utilities.getConfigProperty("journeysPassword"));
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickJourneysLink();
		pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
		pageObj.journeysPage().DragSendEmailToBlock();
		System.out.println(dataSet.get("subject"));
		pageObj.journeysPage().enterSendEmailDetails(dataSet.get("subject"), dataSet.get("preHeaderText"),
				dataSet.get("segmentOption"), dataSet.get("redeemableValue"), dataSet.get("startDate"),
				dataSet.get("endDate"), dataSet.get("startTime"), dataSet.get("endTime"), dataSet.get("timeZone"));
		// Setting never repeat behaviour
		pageObj.journeysPage().setRepeatBehaviourAsNever();
		// setting individual entry limit
		pageObj.journeysPage().setIndividualEntryLimit(dataSet.get("individualEntryTimes"),
				dataSet.get("entryEveryCount"), dataSet.get("entryEveryDuration"));
		pageObj.journeysPage().clickOnCreateSendEmail();
		pageObj.journeysPage().dragSendEmailToMiddle();
		pageObj.journeysPage().enterSendEmailMiddleBlockDetails(dataSet.get("subject"), dataSet.get("preHeaderText"));
		pageObj.journeysPage().setSendThisEmailOnDate(dataSet.get("SentEmailOnDate"));
		pageObj.journeysPage().createMiddleSendEmail();
		pageObj.journeysPage().selectExitIcon();
		pageObj.journeysPage().connectSendEmailToSendEmail();
		pageObj.journeysPage().connectSendEmailMiddleAndExitBlock();
		// pageObj.journeysPage().activateJourney();

	}

	@Test(groups = {
			"regression" }, description = "Sent email as start block(set repeat behaviour, unlimited entry limit), "
					+ "Sent email as mid block(Send this email after)")
	public void JourneysMidBlockSendMailAfter() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
		pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
				Utilities.getConfigProperty("journeysPassword"));
		pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
		// pageObj.menupage().clickCampaignsMenu();
		// pageObj.menupage().clickJourneysLink();
		pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
		pageObj.journeysPage().DragSendEmailToBlock();
		pageObj.journeysPage().enterSendEmailDetails(dataSet.get("subject"), dataSet.get("preHeaderText"),
				dataSet.get("segmentOption"), dataSet.get("redeemableValue"), dataSet.get("startDate"),
				dataSet.get("endDate"), dataSet.get("startTime"), dataSet.get("endTime"), dataSet.get("timeZone"));
		// Set report behavior count
		pageObj.journeysPage().setRepertBehaviourCount(dataSet.get("repeatBehavioutCount"));
		// Set any number of Individual entry limit
		pageObj.journeysPage().setAnyNumberOfIndividualEntryLimit();
		pageObj.journeysPage().clickOnCreateSendEmail();
		pageObj.journeysPage().dragSendEmailToMiddle();
		pageObj.journeysPage().enterSendEmailMiddleBlockDetails(dataSet.get("subject"), dataSet.get("preHeaderText"));
		pageObj.journeysPage().setSendThisEmailAfter(dataSet.get("sendEmailAfterCount"),
				dataSet.get("sendEmailAfterDuration"), dataSet.get("anyTimeValue"));
		pageObj.journeysPage().createMiddleSendEmail();
		pageObj.journeysPage().selectExitIcon();
		pageObj.journeysPage().connectSendEmailToSendEmail();
		pageObj.journeysPage().connectSendEmailMiddleAndExitBlock();
		// pageObj.journeysPage().activateJourney();

	}

	@Test(groups = { "regression" }, description = "Sent email as start block(never repeats, Set entry limit), "
			+ "Sent email as mid block(Send this email on next)")
	public void JourneysMidBlockSendMailOnNext() {
		try {
			pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("journeysUrl"));
			pageObj.instanceDashboardPage().loginToInstance(Utilities.getConfigProperty("journeysUserName"),
					Utilities.getConfigProperty("journeysPassword"));
			pageObj.instanceDashboardPage().selectBusiness(Utilities.getConfigProperty("journeysSlug"));
			// pageObj.menupage().clickCampaignsMenu();
			// pageObj.menupage().clickJourneysLink();
			pageObj.journeysPage().clickOnCreateJourneyAndSetName(journeysName);
			pageObj.journeysPage().DragSendEmailToBlock();
			System.out.println(dataSet.get("subject"));
			pageObj.journeysPage().enterSendEmailDetails(dataSet.get("subject"), dataSet.get("preHeaderText"),
					dataSet.get("segmentOption"), dataSet.get("redeemableValue"), dataSet.get("startDate"),
					dataSet.get("endDate"), dataSet.get("startTime"), dataSet.get("endTime"), dataSet.get("timeZone"));
			// Setting never repeat behavior
			pageObj.journeysPage().setRepeatBehaviourAsNever();
			// setting individual entry limit
			System.out.println(dataSet.get("individualEntryTimes"));
			pageObj.journeysPage().setIndividualEntryLimit(dataSet.get("individualEntryTimes"),
					dataSet.get("entryEveryCount"), dataSet.get("entryEveryDuration"));
			pageObj.journeysPage().clickOnCreateSendEmail();
			pageObj.journeysPage().dragSendEmailToMiddle();
			pageObj.journeysPage().enterSendEmailMiddleBlockDetails(dataSet.get("subject"),
					dataSet.get("preHeaderText"));
			pageObj.journeysPage().setSendThisEmailOnNext(dataSet.get("sendEmailOnNextValue"),
					dataSet.get("anyTimeValue"));
			pageObj.journeysPage().createMiddleSendEmail();
			pageObj.journeysPage().selectExitIcon();
			pageObj.journeysPage().connectSendEmailToSendEmail();
			pageObj.journeysPage().connectSendEmailMiddleAndExitBlock();
			// pageObj.journeysPage().activateJourney();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		driver.close();
		driver.quit();
	}
}
