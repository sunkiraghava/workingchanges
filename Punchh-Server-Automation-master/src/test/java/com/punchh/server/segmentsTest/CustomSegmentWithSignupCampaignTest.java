package com.punchh.server.segmentsTest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
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
public class CustomSegmentWithSignupCampaignTest {

	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String campaignName;
	private static final String invaliduseremail = "Invaliduser@example.com,";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
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
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
	}

	@Test(description = "SQ-T6417 Create custom segment add new user use this segment with signup cam and verify user get reward from signup cam || "
			+ "SQ-T5990 SignUp campaign with segment and user comes through WebEmail ||  "
			+ "SQ-T6897:- Verify Invalid email format to custom segments using API", groups = { "regression",
					"dailyrun", "dailyrun" }, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T6417_verifySignupCreateCustomSegmentAddNewUserUseThisSegmentWithSignupCamVerifyUserGetRewardFromSignupCam()
			throws Exception {
		// T5990_verifySignupCampaignWithSegmentAndUserComesThroughWebEmail is also
		// added
		// create custom segment
		String segmentName = "CustomSeg" + CreateDateTime.getTimeDateString();
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segmentName, dataSet.get("apiKey"));
		Assert.assertEquals(createSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched");
		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");
		pageObj.utils().logPass("custom segment creted :" + segmentName);

		// add Invalid email format to custom segments using API
		Response addInvalidUserSegmentResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId,
				invaliduseremail, dataSet.get("apiKey"));
		Assert.assertEquals(addInvalidUserSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 not matched for Add Invalid email format to custom segment");
		pageObj.utils().logPass("Invalid email format can't add to custom segment");
		Assert.assertEquals(addInvalidUserSegmentResponse.jsonPath().getString("error"), "Invalid email format");

		// add user to custom segments before user signup
		userEmail = pageObj.iframeSingUpPage().generateEmail();

		Response addUserSegmentResponse = pageObj.endpoints().addUserToCustomSegment(customSegmentId, userEmail,
				dataSet.get("apiKey"));
		Assert.assertEquals(addUserSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 not matched for Add user to custom segment");
		pageObj.utils().logPass("Added user to custom segment");

		String signUpCampaignName = CreateDateTime.getUniqueString("Automation SignUpCampaign");
		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Campaigns Link
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// Select ongoing dropdown value
		pageObj.campaignspage().selectOngoingdrpValue(dataSet.get("onGoingDrp"));
		pageObj.campaignspage().clickNewCampaignBtn();
		// create signup campaign with PN Email configured

		// set campaign name and gift type
		pageObj.signupcampaignpage().setCampaignName(signUpCampaignName);
		pageObj.signupcampaignpage().setGiftType(dataSet.get("giftType"));
		pageObj.signupcampaignpage().setGiftReason(signUpCampaignName);
		pageObj.signupcampaignpage().setRedeemable(dataSet.get("redeemable"));
		pageObj.signupcampaignpage().clickNextBtn();
		String campaignid = pageObj.signupcampaignpage().getCampaignid();
		pageObj.signupcampaignpage().setSegmentType(segmentName);
		pageObj.signupcampaignpage().createWhomDetailsSignupCampaign(
				signUpCampaignName + " " + dataSet.get("pushNotification"), dataSet.get("emailSubject"),
				dataSet.get("emailTemplate"));
		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "SignUp Campaign is not created...");

		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		pageObj.iframeSingUpPage().iframeSignUp(userEmail);

		// Timeline validation
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String campaignName = pageObj.guestTimelinePage().getcampaignNameMasscampaign(signUpCampaignName);
		String pushNotificationStatus = pageObj.guestTimelinePage().getPushNotificationText();
		String rewardedRedeemableStatus = pageObj.guestTimelinePage()
				.verifyrewardedRedeemablegiftedbyCampaign(dataSet.get("redeemable"));

		Assert.assertTrue(campaignName.equalsIgnoreCase(signUpCampaignName), "Campaign name did not matched");
		Assert.assertTrue(pushNotificationStatus.contains(signUpCampaignName),
				"Push notification did not displayed...");
		Assert.assertTrue(rewardedRedeemableStatus.contains(dataSet.get("redeemable")),
				"Rewarded Redeemable notification did not displayed...");
		pageObj.utils().logPass(
				"Signup campaign details: push notification, campaign name, reward notification validated successfully on timeline");
		// delete campaigns
		pageObj.utils().deleteCampaignFromDb(campaignName, env);

		// delete custom segments
		Response deleteSegmentResponse = pageObj.endpoints().deletingCustomSegment(dataSet.get("apiKey"),
				customSegmentId);
		Assert.assertEquals(deleteSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 not matched for delete custom segment");
		pageObj.utils().logPass("Verified delete custom segment");

	}

	@Test(description = "SQ-T6418 verify custom bulking add members API", groups = { "regression", "dailyrun",
			"dailyrun" }, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T6418_verifyCustomBulkingAddMembersAPI() throws Exception {

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);

		// create custom segment
		String segmentName = "CustomSeg" + CreateDateTime.getTimeDateString();
		Response createSegmentResponse = pageObj.endpoints().createCustomSegment(segmentName, dataSet.get("apiKey"));
		Assert.assertEquals(createSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched");
		int customSegmentId = createSegmentResponse.jsonPath().get("custom_segment_id");
		pageObj.utils().logPass("custom segment creted :" + segmentName);

		// add user to custom segment using bulking add members API
		// Path to file
		File destinationFile = null;
		String directoryPath = System.getProperty("user.dir") + "/resources";
		destinationFile = new File(directoryPath + "/Testdata/UserList.csv");
		String Segid = String.valueOf(customSegmentId);
		Response addBulkmembersResponse = pageObj.endpoints().bulkAddMemberesInSegment(segmentName, Segid,
				destinationFile, dataSet.get("apiKey"));

		Assert.assertEquals(addBulkmembersResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched");

		// get segment members count

		Response response = pageObj.endpoints().getSegmentCount(dataSet.get("apiKey"), Segid);
		Assert.assertEquals(response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 201 did not matched");
		String count = response.jsonPath().get("count");
		String cnt1 = count.replaceAll(",", "");
		int segmentCount = Integer.parseInt(cnt1);
		Assert.assertTrue(segmentCount >= 0, "Custom Segment count is not greater than 0");

		// delete custom segments
		Response deleteSegmentResponse = pageObj.endpoints().deletingCustomSegment(dataSet.get("apiKey"),
				customSegmentId);
		Assert.assertEquals(deleteSegmentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_ACCEPTED,
				"Status code 202 not matched for delete custom segment");
		pageObj.utils().logPass("Verified delete custom segment");

	}

	@Test(description = "SQ-T6403 Create segment with multiple attribute and run with Trigger based campaign( Signup & Post-checkin)", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T6403_createSegmentWithMultipleAttributesAndRunTriggerBasedCampaignSignupAndPostCheckin()
			throws InterruptedException {
		// if user is unable to receive the push notification for the signup campaign,
		// then enable Enable Bulking For Signup Campaign from cockpit>>campaigns
		// create User
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUp = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(signUp.statusCode(), ApiConstants.HTTP_STATUS_OK, "sign up api status code did not match");
		pageObj.utils().logPass("Api2 user signup is successful");

		// pos checkin
		// POS Checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Response response3 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Checkin API status code did not match");
		Assert.assertEquals(response3.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Checkin API status code did not match");

		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Timeline validation (move to user timeline)
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean campaignNameStatus = pageObj.guestTimelinePage()
				.verifyIsCampaignExistOnTimeLine("DND SQ-T6403 signup campaign");
		boolean campaignNotificationStatus = pageObj.guestTimelinePage()
				.verifyCampaginOrSystemNotificationIsDisplayed("DND SQ-T6403 signup campaign", campaignNameStatus);
		boolean pushNotificationStatus = pageObj.guestTimelinePage().verifyPushNotificationIsDisplayed(
				"DND SQ-T6403 signup campaign", "DND SQ-T6403 push notification for sign up campaign",
				campaignNameStatus);
		SoftAssert softassert = new SoftAssert();
		softassert.assertTrue(campaignNameStatus, "Campaign name did not matched");
		softassert.assertTrue(campaignNotificationStatus, "Campaign notification did not displayed...");
		softassert.assertTrue(pushNotificationStatus, "Push notification did not displayed...");
		softassert.assertAll();
		pageObj.utils().logPass(
				"Signup Campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");

		boolean campaignNameStatus2 = pageObj.guestTimelinePage()
				.verifyIsCampaignExistOnTimeLine("DND SQ-T6403 post-checkin campaign");
		boolean campaignNotificationStatus2 = pageObj.guestTimelinePage().verifyCampaginOrSystemNotificationIsDisplayed(
				"DND SQ-T6403 post-checkin campaign", campaignNameStatus2);
		boolean pushNotificationStatus2 = pageObj.guestTimelinePage().verifyPushNotificationIsDisplayed(
				"DND SQ-T6403 post-checkin campaign", "DND SQ-T6403 post-checkin campaign push notification",
				campaignNameStatus2);
		SoftAssert softAssert2 = new SoftAssert();
		softAssert2.assertTrue(campaignNameStatus2, "Campaign name did not matched");
		softAssert2.assertTrue(campaignNotificationStatus2, "Campaign notification did not displayed...");
		softAssert2.assertTrue(pushNotificationStatus2, "Push notification did not displayed...");
		softAssert2.assertAll();
		pageObj.utils().logPass(
				"Post checkin campaign detail: push notification, campaign name, pointsnotification validated successfully on timeline");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		pageObj.utils().logit("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}

}