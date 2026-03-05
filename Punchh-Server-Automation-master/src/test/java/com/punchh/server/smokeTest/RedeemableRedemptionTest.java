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
import org.testng.asserts.SoftAssert;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedeemableRedemptionTest {

	private static Logger logger = LogManager.getLogger(RedeemableRedemptionTest.class);
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
	@Test(description = "SQ-T2208 Verify the redemption of redemption code", groups = "Sanity", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2208_verifytheredemptionofredemptioncode() throws InterruptedException {

		String reward_Code = "";

		// User Signip using mobile api 2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

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

		// click message gift and gift reward
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		// pageObj.guestTimelinePage().verifyGuestTimeline(dataSet.get("joinedChannel"),
		// iFrameEmail);
		pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),
				dataSet.get("redeemable"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		String rewardName = pageObj.guestTimelinePage().getRewardName();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		Assert.assertEquals(rewardName, "Rewarded $2.0 OFF");
		// iFrame Login
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardOffer(dataSet.get("rewardName"));

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		// Pos redemption api
		Response resp = pageObj.endpoints().posRedemptionOfCode(iFrameEmail, date, reward_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		pageObj.utils().logit("Response time for POS redemption api in milliseconds is :" + resp.getTime());
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
		pageObj.guestTimelinePage().refreshTimeline();
		// Thread.sleep(5000);
		// Validate timeline for redemption and receipt
		String redeemedRedemption = pageObj.guestTimelinePage().redeemedRedemption(reward_Code);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		/*
		 * pageObj.guestTimelinePage().refreshTimeline(); Thread.sleep(5000); String
		 * discountValuePosCheckin =
		 * pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails(); String
		 * discountedAmount =
		 * pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();
		 */

		try {
			SoftAssert softassert = new SoftAssert();
			Assert.assertEquals(redeemedRedemption, reward_Code, "Redemption code did not displayed on time line");

			pageObj.utils().logPass("Web and Pos receipt validated successfully");
		} catch (Exception e) {
			logger.error("Error in validating web and pos receipt" + e);
			Assert.fail("Error in validating web and pos receipt" + e);
		}

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforGiftedItem();
		System.out.println(Itemdata);
		Assert.assertTrue(
				Itemdata.get(0).contains("Points Pending") || Itemdata.get(0).contains("Points Earned")
						|| Itemdata.get(0).contains("Item Redeemed") || Itemdata.get(0).contains("Item Gifted"),
				"Redemption did not redeemed in account history");
		/*
		 * Assert.assertTrue(Itemdata.get(1).contains("0 Items") ||
		 * Itemdata.get(1).contains("0 Points") ,
		 * "reward item did not decreased in account balance");
		 */
		pageObj.utils().logPass("Redemption of reward is validated in acount history");
		pageObj.guestTimelinePage().clickRewards();
		String honoredreward = pageObj.guestTimelinePage().getHonored();
		Assert.assertTrue(honoredreward.contains(dataSet.get("redeemable")),
				"Redeemed reward name did not matched in honored list");
		Assert.assertTrue(honoredreward.contains("Honored"), "Redeemed reward status did not matched in honored list");
		pageObj.utils().logPass("Redemption of redemption code is validated in honored list");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2206 Verify the redemption of Redeemable", groups = "Sanity", priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2206_verifytheredemptionofRedeemable() throws InterruptedException {
		// iFrame Signup
		/*
		 * pageObj.iframeSingUpPage().navigateToIframe(baseUrl +
		 * dataSet.get("whiteLabel") + dataSet.get("slug")); iFrameEmail =
		 * pageObj.iframeSingUpPage().iframeSignUp();
		 * pageObj.iframeSingUpPage().iframeSignOut();
		 */

		// User Signip using mobile api 2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
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
		String redeemable_id = pageObj.redeemablePage().getRedeemableID(dataSet.get("redeemable"));

		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");
		// Pos redemption api
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		Response resp = pageObj.endpoints().posRedemptionOfRedeemable(iFrameEmail, date, key, txn,
				dataSet.get("locationKey"), redeemable_id, "110011");
		Assert.assertEquals(resp.getStatusCode(), 200, "Status code 200 did not matched for pos redemption api");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");
		// validate guest timeline
		pageObj.guestTimelinePage().refreshTimeline();
		// Thread.sleep(5000);
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		/*
		 * pageObj.guestTimelinePage().refreshTimeline(); Thread.sleep(5000); String
		 * discountValuePosCheckin =
		 * pageObj.guestTimelinePage().getDiscountValuePosCheckinDetails(); String
		 * discountedAmount =
		 * pageObj.guestTimelinePage().getDiscountedAmountPosCheckinDetails();
		 */

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Redemption did not redeemed in account history");
		/*
		 * Assert.assertTrue(Itemdata.get(1).contains("0 Items") ||
		 * Itemdata.get(1).contains("1 Item"),
		 * "reward item did not decreased in account balance");
		 */
		pageObj.utils().logPass("Redemption of redeemable is validated in acount history");
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}