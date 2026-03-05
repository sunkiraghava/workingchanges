package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GuestTimeLineValidationTest {
	static Logger logger = LogManager.getLogger(GuestTimeLineValidationTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private static Map<String, String> dataSet;
	private String env;
	private String baseUrl;
	String sTCName;
	String run = "ui";
	String redemptionCode;
	String test1Slug, test1Url, test1redeemAmount;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single login to instance
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
		logger.info(sTCName + " ==> " + dataSet);
		// move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2170 Verify Guest timeline and Account history in point to currency business || "
			+ "SQ-T2201 Verify POS Checkin performed on test Location", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2170_verifyGuestTimelineInPointToCurrency() throws InterruptedException {
		test1redeemAmount = dataSet.get("redeemAmount");
		test1Slug = dataSet.get("slug");
		test1Url = baseUrl;
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Guest signup
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), 200);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Pos api checkin
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");
		Assert.assertEquals(resp.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Verify checkin in guest timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String approvadLoyalty = pageObj.guestTimelinePage().getApprovedLoyalty();
		String discountValuePosCheckin = pageObj.guestTimelinePage().getRecieptApprovedLoyaltyDetails();
		Assert.assertTrue(approvadLoyalty.contains(key),
				"loyalty checkin approved barcode did not matched or displayed on timeline");

		Assert.assertTrue(pageObj.guestTimelinePage().verifyPosCheckinInTimeLine(key, dataSet.get("amount"),
				dataSet.get("baseConversionRate")), "Error in verifying POS checkin in guest timeline ");

		// verify gift reward in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		String Itemdata = pageObj.accounthistoryPage().getAccountHistorydetailsForAnyevent("100 points converted into");
		Assert.assertTrue(Itemdata.contains("100 points converted"),
				"Gifted currency by campaign did not appeared in account history");
		/*
		 * List<String> pointsdata =
		 * pageObj.accounthistoryPage().getAccountDetailsforRewardEarned();
		 * System.out.print(pointsdata);
		 * Assert.assertTrue(pointsdata.get(0).contains("Rewards Earned"
		 * ),"Rewards earned by user did not appeared in account history");
		 * Assert.assertTrue(pointsdata.get(0).
		 * contains("100 points converted into $10.00 of banked rewards") ||
		 * pointsdata.get(0).
		 * contains("100 points converted into $18.00 of banked rewards")
		 * ,"Rewards earned by user did not appeared in account history");
		 * Assert.assertTrue(pointsdata.get(2).contains("+$10.00") ||
		 * pointsdata.get(2).contains("+$18.00"),"Reward added to account balance");
		 */
		TestListeners.extentTest.get().pass("Verified rewards earned in account history");
		// Pos redemption api
		Response respo = pageObj.endpoints().posRedemptionOfAmount(userEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		redemptionCode = respo.jsonPath().get("redemption_code").toString();
		// validate guest timeline and account history
		pageObj.guestTimelinePage().clickTimeLine();

		String discountValue = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		boolean val = pageObj.guestTimelinePage().verifyRedemptionOnTimeline(dataSet.get("redeemAmount"));
		Thread.sleep(8000);
		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> redeemedData = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.print(redeemedData);
		Assert.assertTrue(val, "Reward redeemed on time line did not matched");
		Assert.assertTrue(redeemedData.get(0).contains("Rewards Redeemed"),
				"Rewards redeemed by user did not appeared in account history");
		Assert.assertTrue(redeemedData.get(2).contains("($5.00)"),
				"Reward redeemed  points and balance did not appeared in account history");
		TestListeners.extentTest.get().pass("Verified rewards redeemed in account history");

		// Test case: SQ-T2569 T2569_verifyRedemptionLog
		// pageObj.menupage().clickSupportMenu();
		// pageObj.menupage().redemptionLink();
		pageObj.menupage().navigateToSubMenuItem("Support", "Redemption Log");
		boolean status = pageObj.RedemptionLogPage().verifyRedemptionLog(redemptionCode, test1redeemAmount);
		Assert.assertTrue(status, "redemption logs did nit displayed in Redemption Log page");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2209, Verify Guest timeline and Account history in visit based business", groups = {
			"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2209_verifyGuestTimelineVisitBasedBusiness() throws InterruptedException {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Guest signup
		Response response = pageObj.endpoints().posSignUp(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response.getStatusCode(), 200);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// POS checkin
		Response response2 = null;
		for (int i = 0; i < Integer.parseInt(dataSet.get("numberOfVisit")); i++) {
			Thread.sleep(3000);
			// Pos api checkin
			String key = CreateDateTime.getTimeDateString();
			String txn = "123456" + CreateDateTime.getTimeDateString();
			String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
			Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationKey"));
			Assert.assertEquals(200, resp.getStatusCode(), "Status userEmail 200 did not matched for post chekin api");
		}
		// Verify checkin in guest timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// verify redeemable and checkin's in guest timeline
		String val = pageObj.guestTimelinePage().verifyRedeemableCard();
		Assert.assertEquals(val, "1", "Redeemable card value 1 did not matched");
		Assert.assertEquals(Integer.parseInt(dataSet.get("numberOfVisit")),
				pageObj.guestTimelinePage().getCheckinCount());
		// verifying checkin's in account history
		pageObj.guestTimelinePage().clickAccountHistory();
		Assert.assertEquals(Integer.parseInt(dataSet.get("numberOfVisit")),
				pageObj.accounthistoryPage().getCheckinCount());

		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfCard(userEmail, date, key, txn, dataSet.get("locationKey"));
		Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		// validate guest timeline
		Thread.sleep(5000);
		pageObj.guestTimelinePage().clickTimeLine();
		pageObj.guestTimelinePage().refreshTimeline();
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		String totalRedeemed = pageObj.guestTimelinePage().getTotalRedeemed();
		String totalRedeemable = pageObj.guestTimelinePage().getTotalRedeemable();
		pageObj.guestTimelinePage().clickAccountHistory();
		boolean cardRedeemedTag = pageObj.guestTimelinePage().accountHistoryForCardRedeemed();
		Assert.assertTrue(cardRedeemedTag, "Card Redeemed tag did not displayed in account history");

	}

	// Merged woth T6226_rollingCheckinSumPTR
	@SuppressWarnings("unused")
//	@Test(description = "SQ-T2267, Verify Guest timeline and Account history in point to reward business", groups = {
//			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2267_verifyGuestTimelinePointToRewardBusiness() throws InterruptedException {

		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Guest signup
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client").trim(),
				dataSet.get("secret").trim());
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token");
		// POS checkin
		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		Assert.assertEquals(checkinResponse.jsonPath().get("email").toString(), userEmail.toLowerCase());

		// Verify checkin in guest account history using account history api
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(200, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 Account History");
		// When json array has no name in response
		String jsonData = accountHistoryResponse.asString();
		JSONArray jsonArray = new JSONArray(jsonData);
		int count = jsonArray.length();
		for (int i = 0; i < count; i++) {
			JSONObject jObject = jsonArray.getJSONObject(i);
			String val = String.valueOf(jObject.get("description"));
			System.out.println(val);
			if (val.contains("210 points earned for your $210.00 purchase")) {
				String eventVal = jObject.get("event_value").toString();
				System.out.println(eventVal);
				Assert.assertEquals(eventVal, "+210 points", "points earned after checkin did not matched");
				TestListeners.extentTest.get().pass("checkin validated with points earned :" + eventVal);
				break;
			}
		}
		// Verify checkin in guest timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify redeemable point in guest timeline
		/*
		 * int amountCheckin = 210;
		 * Assert.assertTrue(pageObj.guestTimelinePage().getRedeemablePointCount() ==
		 * (amountCheckin * 1));
		 */
		// Pos redemption api
		Response rewardResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"),
				dataSet.get("secret"));
		JSONArray jsonArry = new JSONObject(rewardResponse.getBody().asString()).getJSONArray("rewards");
		String rewardId = jsonArry.getJSONObject(1).get("reward_id").toString();
		Response respo = pageObj.endpoints().posRedemptionOfReward(userEmail, dataSet.get("locationKey"), rewardId);
		Assert.assertEquals(200, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		// validate guest timeline
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
//		String totalRedeemed = pageObj.guestTimelinePage().getTotalRedeemed();
//		String totalRedeemable = pageObj.guestTimelinePage().getTotalRedeemable();
		pageObj.guestTimelinePage().clickAccountHistory();
		boolean itemRedeemLabel = pageObj.guestTimelinePage().accountHistoryForItemRedeemed1();
		Assert.assertTrue(itemRedeemLabel, "Item redeem label is not appearing in account history");

	}

	// Merged with T6221_rollingCheckinSumPUR
//	@Test(description = "SQ-T2182, Verify Guest timeline and Account history in point unlock business", groups = {
//			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Amit Kumar")
	@SuppressWarnings("unused")
	public void T2182_verifyGuestTimelinePointUnlockBusiness() throws InterruptedException {
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		// Guest signup
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client").trim(),
				dataSet.get("secret").trim());
		Assert.assertEquals(signUpResponse.getStatusCode(), 200);
		Assert.assertEquals(signUpResponse.jsonPath().get("user.communicable_email").toString(),
				userEmail.toLowerCase());
		String token = signUpResponse.jsonPath().get("access_token.token");
		// POS checkin
		Response response2 = null;
		response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response2.getStatusCode(), 200);
		Assert.assertEquals(response2.jsonPath().get("email").toString(), userEmail.toLowerCase());
		// Verify checkin in guest timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// verify redeemable point in guest timeline
		int amountCheckin = 210;
		Assert.assertTrue(pageObj.guestTimelinePage().getRedeemablePointCount() == (amountCheckin * 3),
				"Failed to verify redemable point count");
		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfRedeemable(userEmail, date, key, txn,
				dataSet.get("locationKey"), dataSet.get("redeemable_id"), "110011");
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		Assert.assertTrue(resp.jsonPath().get("status").toString().contains("Please HONOR it."));
		// validate guest timeline
		Thread.sleep(5000);
		pageObj.guestTimelinePage().refreshTimeline();
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
//				String totalRedeemed = pageObj.guestTimelinePage().getTotalRedeemed();
//				String totalRedeemable = pageObj.guestTimelinePage().getTotalRedeemable();
		pageObj.guestTimelinePage().clickAccountHistory();
		boolean itemRedeemLabel = pageObj.guestTimelinePage().accountHistoryForItemRedeemed();
		Assert.assertTrue(itemRedeemLabel, "Item redeem label is not appearing in account history");

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
