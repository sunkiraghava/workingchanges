package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RedemptionIHOPTest {

	private static Logger logger = LogManager.getLogger(RedemptionIHOPTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String iFrameEmail;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;

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
	@Test(description = "SQ-T2905 Verify redemption for Points Unlock Redeemable No Conversion Business Type", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2905_verifyredemptionforPointsUnlockRedeemableNoConversionBusinessType() throws InterruptedException {

		// iFrame Signup
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
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		// click message gift and gift points
		pageObj.guestTimelinePage().messagePointsToUser(dataSet.get("subject"), dataSet.get("option"),
				dataSet.get("giftTypes"), dataSet.get("giftReason"));
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Message sent did not displayed on timeline");

		// iFrame Login and generate code from redeem rewards
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeLogin(iFrameEmail);
		reward_Code = pageObj.iframeSingUpPage().redeemRewardFromRedeemrewardsWithNewUI(dataSet.get("rewardName"));
		Assert.assertTrue(!reward_Code.isEmpty(), "reward_Code is empty");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata);
		Assert.assertTrue(Itemdata.get(0).contains("Rewards Redeemed"),
				"Rewards Redeemed did not appeared in account history");
		Assert.assertTrue(Itemdata.get(0).contains("$2.0 OFF redeemed using redemption code " + reward_Code),
				"Rewards Redeemed with reward code did not appeared inin account history");
		Assert.assertTrue(Itemdata.get(1).contains("0 Points"), "Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata.get(2).contains("(100)") || Itemdata.get(2).contains("(0)"),
				"Redemption did not redeemed in account history");
		Assert.assertEquals(Itemdata.size(), 3, "Itemdata list size is more then  3 Double entry observed");

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();
		// Pos redemption api
		Response resp = pageObj.endpoints().posRedemptionOfCode(iFrameEmail, date, reward_Code, key, txn,
				dataSet.get("locationKey"));
		Assert.assertEquals(200, resp.getStatusCode(), "Status code 200 did not matched for pos redemption api");

		// validate guest timeline
		pageObj.guestTimelinePage().clickTimeLine();
		String discountValueWebCheckin = pageObj.guestTimelinePage().getDiscountValueWebCheckinDetails();
		String totalamoutnWebCheckin = pageObj.guestTimelinePage().getTotalAmountWebCheckin();
		pageObj.guestTimelinePage().refreshTimeline();

		pageObj.guestTimelinePage().clickAccountHistory();
		List<String> Itemdata1 = pageObj.accounthistoryPage().getAccountDetailsforRewardRedeemed();
		System.out.println(Itemdata1);
		Assert.assertTrue(Itemdata1.get(0).contains("Rewards Redeemed"),
				"Rewards Redeemed did not appeared in account history");
		Assert.assertTrue(Itemdata1.get(0).contains("$2.0 OFF redeemed using redemption code " + reward_Code),
				"Rewards Redeemed with reward code did not appeared inin account history");
		Assert.assertTrue(Itemdata1.get(1).contains("0 Points"), "Redemption did not redeemed in account history");
		Assert.assertTrue(Itemdata1.get(2).contains("(100)"), "Redemption did not redeemed in account history");
		Assert.assertEquals(Itemdata.size(), 3, "Itemdata list size is more then  3 Double entry observed");
		pageObj.utils().logPass("redemption for Points Unlock Redeemable No Conversion Business Type is validated");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
