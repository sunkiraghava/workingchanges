package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RecieptTaggingTest {
	static Logger logger = LogManager.getLogger(RecieptTaggingTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String lineItemQCname;
	private String itemid;
	private String rectag;
	private String QCname;
	private String amountcap;
	String punchKey, amount, date, businessID;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		businessID = dataSet.get("business_id");
		lineItemQCname = "test" + CreateDateTime.getTimeDateString();
		date = CreateDateTime.getTimeDateString();
		itemid = CreateDateTime.getTimeDateString();
		amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
		QCname = "Qc" + CreateDateTime.getTimeDateString();
		rectag = "recieptTag" + CreateDateTime.getTimeDateString();
		// Move to All businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T2560 Verify the functionality of Receipt tagging; "
			+ "SQ-T7269: Verify that any positive value can set in \"Last N Days To Tag Old Receipts\" field successfully.", groups = { "regression", "unstable",
			"dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T2560_verifyReceiptTagging() throws Exception {
		// SQ-T7269 starts
		String query = OfferIngestionUtilities.businessPreferenceQuery.replace("$id", businessID);
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "preferences");
		DBUtils.updateBusinessFlag(env, expColValue, "true", "enable_receipt_taggings_limit_dark_release", businessID);
		utils.logit("enable_receipt_taggings_limit_dark_release is set as true");

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().setLastNDaysToTagOldReceipts("30");
		pageObj.dashboardpage().clickOnUpdateButton();
		String successMsg = pageObj.mobileconfigurationPage().getSuccessMessage();
		Assert.assertEquals(successMsg, MessagesConstants.successUpdate);
		utils.logPass("Last N Days To Tag Old Receipts is set with positive value successfully.");
		// SQ-T7269 ends

		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().enterDetailsNewLineItemSelectorPage(lineItemQCname, itemid, "Only Base Items");

		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().enterDetailsNewQcSelectorPage(lineItemQCname, rectag, QCname, amountcap);
		pageObj.qualificationcriteriapage().activateTag(QCname);

		// API1 user signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "user signup failed ");
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// POS Checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckinQC(date, userEmail, key, txn, dataSet.get("locationKey"), itemid);
		Assert.assertEquals(resp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "checkin failed");
		Assert.assertEquals(resp.jsonPath().get("email").toString().toLowerCase(), userEmail.toLowerCase());

		// Verify checkin in guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean approvedReciptTagResult = pageObj.guestTimelinePage()
				.verifyApprovedReciptTagIsDisplayedOnTimeLine(rectag);
		Assert.assertTrue(approvedReciptTagResult, "Error verifying ApprovedReciptTag channel");
		boolean recieptTagPanelResult = pageObj.guestTimelinePage()
				.verifyRecieptTagPanelTagIsDisplayedOnTimeLine(rectag);
		Assert.assertTrue(recieptTagPanelResult, "Error verifying recieptTagPanel channel");
		boolean val = pageObj.guestTimelinePage().verifyCheckinChannelAndLocation("POS", dataSet.get("posLocation"));
		Assert.assertTrue(val, "Error in verifying/capturing checkin channel");

		// delete QC
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deactivateTagFromQc(QCname);
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deleteQC(QCname);

		// Delete Lis
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");

		pageObj.lineItemSelectorPage().deleteLineItemSelectors(lineItemQCname);

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}