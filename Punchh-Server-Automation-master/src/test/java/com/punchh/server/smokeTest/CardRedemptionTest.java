package com.punchh.server.smokeTest;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class CardRedemptionTest {

	private static Logger logger = LogManager.getLogger(CardRedemptionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private Map<String, String> dataSet;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2245 Verify the redemption of Card", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2245_VerifyCardRedemptionInVisitBasedBusiness() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// user creation using pos signup api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		// click message gift and gift orders visits
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().messageOrdersToUser(dataSet.get("subject"), dataSet.get("giftTypes"),
				dataSet.get("giftOrders"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// click message gift and gift points
		/*
		 * pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"),
		 * dataSet.get("option"),dataSet.get("giftTypes"), dataSet.get("giftReason"));
		 * boolean status = pageObj.campaignspage().validateSuccessMessage();
		 * Assert.assertTrue(status, "Message sent did not displayed on timeline");
		 */

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfCard(userEmail, date, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		String totalRedeemed = pageObj.guestTimelinePage().getTotalRedeemed();
		String totalRedeemable = pageObj.guestTimelinePage().getTotalRedeemable();
		pageObj.guestTimelinePage().clickAccountHistory();
		boolean cardRedeemedTag = pageObj.guestTimelinePage().accountHistoryForCardRedeemed();
		Assert.assertTrue(cardRedeemedTag, "Card Redeemed tag did not displayed in account history");
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforCardRedeemed();
		logger.info(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Card Redeemed"), "Redemption did not redeemed in account history");
		/*
		 * Assert.assertTrue(Itemdata.get(1).contains("0 Items") ||
		 * Itemdata.get(1).contains("1 Item"),
		 * "reward item did not decreased in account balance");
		 */
		TestListeners.extentTest.get().pass("Redemption of card is validated in acount history");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}