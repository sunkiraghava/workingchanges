package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

@Listeners(TestListeners.class)
public class DeactivateRecurringMassCampaignTest {
	static Logger logger = LogManager.getLogger(DeactivateRecurringMassCampaignTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private String sTCName;
	private static Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	@Test(groups = { "regression", "dailyrun" }, description = "SQ-T2603 Validate Deactivate of Recurring Mass Gifting Campaign.")
	@Owner(name = "Ashwini Shetty")
	public void T2603_deactivateRecurringMassCampaign() throws InterruptedException {
		String recurringMassGiftingName = "Recurring Mass Gifting Campaign" + CreateDateTime.getTimeDateString();
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select offer dropdown value
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsMassCampaign(recurringMassGiftingName, dataSet.get("giftType"),
				dataSet.get("redemable"));
		pageObj.signupcampaignpage().createWhomDetailsMassCampaign(dataSet.get("audiance"), dataSet.get("segment"),
				dataSet.get("pushNotification"), dataSet.get("emailSubject"), dataSet.get("emailTemplate"));
		// pageObj.signupcampaignpage().createWhenDetailsMassCampaign("Once", dateTime);
		pageObj.signupcampaignpage().setFrequency("Daily");
		String startdateTime = CreateDateTime.getTomorrowDate() + " 11:00 PM";
		String enddateTime = CreateDateTime.getFutureDate(2) + " 11:00 PM";
		pageObj.signupcampaignpage().setStartEndDateTime(startdateTime, enddateTime);
		pageObj.campaignspage().searchAndSelectCamapign(recurringMassGiftingName);
		// deactivate
		pageObj.campaignspage().deactivateCampaign(recurringMassGiftingName);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
