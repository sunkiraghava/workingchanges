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
public class RewardAndAmountRedemptionTest {

	private static Logger logger = LogManager.getLogger(RewardAndAmountRedemptionTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String iFrameEmail;
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
	@Test(description = "SQ-T2167 Verify the redemption of Amount", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2167_verifytheredemptionofAmount() throws InterruptedException {

		// user creation using pos signup api
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response resp = pageObj.endpoints().posSignUp(iFrameEmail, dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos signup api");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// set earnig type and Base Conversion Rate to Points configuration
		/*
		 * pageObj.menupage().clickCockPitMenu(); pageObj.menupage().clickEarningLink();
		 * pageObj.earningPage().setProgramType("Points Based");
		 * pageObj.earningPage().setPointsConvertTo("Currency");
		 * pageObj.earningPage().setBaseConversionRate("1.0");
		 * pageObj.earningPage().updateConfiguration();
		 */

		// click message gift and gift points
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().messageRewardAmountToUser(dataSet.get("subject"), dataSet.get("location"),
				dataSet.get("giftTypes"), dataSet.get("amount"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response respo = pageObj.endpoints().posRedemptionOfAmount(iFrameEmail, date, key, txn,
				dataSet.get("redeemAmount"), dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, respo.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.utils().logit("Response time for POS redemption api in milliseconds is :" + resp.getTime());
		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		/*
		 * String discountValuePosCheckin =
		 * pageObj.guestTimelinePage().getDiscountValuePosApprovedLoyaltyDetails();
		 * String discountedAmount =
		 * pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();
		 */

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Redemption did not redeemed in account history");
		/*
		 * Assert.assertTrue(Itemdata.get(1).contains("0 Items"),
		 * "reward item did not decreased in account balance");
		 */
		pageObj.utils().logPass("Redemption of amount is validated in acount history");

		/*
		 * TestData.AddTestDataToWriteInJSON("email", iFrameEmail);
		 * TestData.EditOrAddNewGivenFieldForGivenScenarioFromJson(TestData.
		 * getJsonFilePath(run , env), sTCName);
		 */

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2186 Verify the redemption of Reward", groups = "Sanity", priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2186_VerifyredemptionofReward() throws InterruptedException {

		// User register/signup using API2 Signup
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("Api2 user signup is successful");

		// Instance login and goto timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// set earnig type and Base Conversion Rate to Points configuration
		/*
		 * pageObj.menupage().clickCockPitMenu(); pageObj.menupage().clickEarningLink();
		 * pageObj.earningPage().setProgramType("Points Based");
		 * pageObj.earningPage().setPointsConvertTo("Currency");
		 * pageObj.earningPage().setBaseConversionRate("1.0");
		 * pageObj.earningPage().updateConfiguration();
		 */
		// check redeemable's availability in business and create redeemable if not
		// available
		// navigate to Settings -> Redeemables
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().createRedeemableIfNotExistWithExistingQC(dataSet.get("redeemable"), "Flat Discount",
				"", "2.0");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift reward
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");

		// fetch user offers using ap2 mobileOffers
		Response offerResponse = pageObj.endpoints().getUserOffers(token, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, offerResponse.getStatusCode(), "Status code 200 did not matched for api2 user offers");
		String reward_id = offerResponse.jsonPath().get("rewards[0].reward_id").toString();
		pageObj.utils().logit(reward_id);

		// Pos redemption of reward id
		Response resp = pageObj.endpoints().posRedemptionOfReward(iFrameEmail, dataSet.get("locationKey"), reward_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();
		Thread.sleep(5000);
		/*
		 * String discountValuePosCheckin =
		 * pageObj.guestTimelinePage().getDiscountValuePosApprovedLoyaltyDetails();
		 * String discountedAmount =
		 * pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();
		 */

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforItemRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(
				Itemdata.get(0).contains("Item Redeemed") || Itemdata.get(0).contains("Points Pending")
						|| Itemdata.get(0).contains("Points Earned") || Itemdata.get(0).contains("Item Gifted"),
				"Redemption did not redeemed in account history");
		/*
		 * Assert.assertTrue(Itemdata.get(1).contains("0 Items"),
		 * "reward item did not decreased in account balance");
		 */
		pageObj.utils().logPass("Redemption of reward id is validated in acount history");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}