package com.punchh.server.campaignsTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.Integration2.IntUtils;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
@SuppressWarnings("static-access")
public class AndroidPassesTest {

	private static Logger logger = LogManager.getLogger(AndroidPassesTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;
	SeleniumUtilities selUtils;
	private Properties dbConfig;
	private String punchhDBName;
	TestListeners testListObj;
	private IntUtils intUtils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		dbConfig = Utilities.loadPropertiesFile("dbConfig.properties");
		punchhDBName = dbConfig.getProperty(env.toLowerCase() + ".DBName");
		selUtils = new SeleniumUtilities(driver);
		utils = new Utilities(driver);
		intUtils = new IntUtils(driver);
	}

	@Test(description = "SQ-T5718 : Verify Google Loyalty pass, Add and remove from wallet functionality.")
	public void T5718_verifyAndroidPassForUser() throws Exception {

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		pageObj.utils().logPass("Verified user signup via API v1");
		String userID = signUpResponse.jsonPath().get("id").toString();

		// open guestTimeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		// check entry not present in android_passes table
		String query1 = "select count(*) as count from android_passes where owner_id = '" + userID + "'";
		boolean expValue1 = DBUtils.verifyValueFromDBUsingPolling(env, query1, "count", "0");
		Assert.assertTrue(expValue1, "Entry is already present in android_passes table");
		logger.info("Verified that initially no entry is present in android_passes table");
		pageObj.utils().logPass("Verified that initially no entry is present in android_passes table");

		// Navigate to android pass
		pageObj.guestTimelinePage().navigateToAndroidPassOfUser();
		// check entry in android_passes table
		String query2 = "select status from android_passes where owner_id = '" + userID + "'";
		boolean expValue2 = DBUtils.verifyValueFromDBUsingPolling(env, query2, "status", "0");
		Assert.assertTrue(expValue2, "Entry not present/False entry in android_passes table");
		logger.info("Verified that entry is created in android_passes table and initial status is 0 ");
		pageObj.utils().logPass("Verified that entry is created in android_passes table and initial status is 0 ");

		// add pass to wallet
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		// check status in android_passes table
		String query3 = "select status from android_passes where owner_id = '" + userID + "'";
		boolean expValue3 = DBUtils.verifyValueFromDBUsingPolling(env, query3, "status", "1");
		Assert.assertTrue(expValue3, "Status not updated in android_passes table");
		logger.info("Verified that status is updated to 1 in android_passes table ");
		pageObj.utils().logPass("Verified that status is updated to 1 in android_passes table ");

		// remove pass from wallet
		pageObj.guestTimelinePage().clickOnRemoveFromWalletButton();
		// check status in android_passes table
		String query4 = "select status from android_passes where owner_id = '" + userID + "'";
		boolean expValue4 = DBUtils.verifyValueFromDBUsingPolling(env, query4, "status", "2");
		Assert.assertTrue(expValue4, "Status not updated in android_passes table");
		logger.info("Verified that status is updated to 2 in android_passes table ");
		pageObj.utils().logPass("Verified that status is updated to 2 in android_passes table ");

		// view full loyalty pass of user
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		String query5 = "select count(*) as count, status from android_passes where owner_id = '" + userID + "'";
		boolean expValue5 = DBUtils.verifyValueFromDBUsingPolling(env, query5, "count", "1");
		boolean expValue5a = DBUtils.verifyValueFromDBUsingPolling(env, query5, "status", "1");
		Assert.assertTrue(expValue5, "Duplicate entry is created in android_passes table");
		Assert.assertTrue(expValue5a, "Status not updated in android_passes table");
		logger.info("Verified that status is updated to 1 in android_passes table and no duplicate entry made");
		pageObj.utils().logPass("Verified that status is updated to 1 in android_passes table and no duplicate entry made");

		// remove pass from wallet
		pageObj.newCamHomePage().navigateToBackPage();
		pageObj.guestTimelinePage().clickOnRemoveFromWalletButton();
		String query6 = "select count(*) as count, status from android_passes where owner_id = '" + userID + "'";
		boolean expValue6 = DBUtils.verifyValueFromDBUsingPolling(env, query6, "count", "1");
		boolean expValue6a = DBUtils.verifyValueFromDBUsingPolling(env, query6, "status", "2");
		Assert.assertTrue(expValue6, "Duplicate entry is created in android_passes table");
		Assert.assertTrue(expValue6a, "Status not updated in android_passes table");
		logger.info("Verified that status is updated to 2 in android_passes table and no duplicate entry made ");
		pageObj.utils().logPass("Verified that status is updated to 2 in android_passes table and no duplicate entry made");

	}

	@Test(description = "SQ-T5717 : Verify different user identifers for android pass")
	public void T5717_verifyUserIdentifierForAndroidPass() throws Exception {
		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		pageObj.utils().logPass("Verified user signup via API v1");
		String userCode = signUpResponse.jsonPath().get("user_code").toString();
		String access_token = signUpResponse.jsonPath().get("auth_token.token").toString();

		// CASE-0 : user identifer_1 = phone, but user does not have phone.
		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
		pageObj.settingsPage().setUserIdentifierForPass(dataSet.get("userIdentifier_1"));
		pageObj.settingsPage().clickOnApplePassSaveButton();
		// navigate to android pass of user
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToAndroidPassOfUser();
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().checkUserIdentifierOnPass(userCode);

		// CASE-1 : user identifer_1 = phone, user has phone.
		// update phone number of user
		long newPhone = (long) (Math.random() * Math.pow(10, 10));
		String newPhone1 = Long.toString(newPhone);
		Response updateUser_Response = pageObj.endpoints().Api1MobileUpdateGuestDetailsWithoutEmail("Password@123",
				newPhone1, dataSet.get("client"), dataSet.get("secret"), access_token);
		Assert.assertEquals(updateUser_Response.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched ");
		logger.info("Update user profile is successful");
		pageObj.utils().logPass("Update user profile is successful");

		ArrayList<String> chromeTabs = new ArrayList<>(driver.getWindowHandles());
		driver.close();
		driver.switchTo().window(chromeTabs.get(0));
		pageObj.guestTimelinePage().refreshTimeline();
		pageObj.guestTimelinePage().clickOnDownloadAndroidPassButton();
		ArrayList<String> chromeTabs2 = new ArrayList<>(driver.getWindowHandles());
		driver.switchTo().window(chromeTabs2.get(1));
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().checkUserIdentifierOnPass(newPhone1);

		// CASE-2 : user identifer_2 = email
		// CASE-3 : user identifer_3 = user_code
		for (int i = 2; i <= 3; i++) {
			ArrayList<String> chromeTabs3 = new ArrayList<>(driver.getWindowHandles());
			driver.close();
			driver.switchTo().window(chromeTabs3.get(0));
			pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
			pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
			pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
			pageObj.settingsPage().setUserIdentifierForPass(dataSet.get("userIdentifier_" + i));
			pageObj.settingsPage().clickOnApplePassSaveButton();
			// navigate to android pass of user
			pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
			pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
			String userIdentifierValue = (i == 2) ? userEmail : userCode;
			pageObj.guestTimelinePage().clickOnDownloadAndroidPassButton();
			ArrayList<String> chromeTabs4 = new ArrayList<>(driver.getWindowHandles());
			driver.switchTo().window(chromeTabs4.get(1));
			pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
			pageObj.guestTimelinePage().checkUserIdentifierOnPass(userIdentifierValue);
		}
		pageObj.newCamHomePage().navigateToBackPage();
		pageObj.guestTimelinePage().clickOnRemoveFromWalletButton();
		logger.info("Successfully verified different user identifiers for android pass ");
		pageObj.utils().logit("Successfully verified different user identifiers for android pass ");

	}

	@Test(description = "SQ-T5720 : Verify android loyalty pass is updated if the user update profile, account balance")
	public void T5720_verifyAndroidPassUpdate() throws Exception {
		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a user
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		pageObj.utils().logPass("Verified user signup via API v1");
		String userID = signUpResponse.jsonPath().get("id").toString();
		String auth_token = signUpResponse.jsonPath().get("authentication_token").toString();

		// navigate to android pass of user
		pageObj.instanceDashboardPage().refreshPage();
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().verifyGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToAndroidPassOfUser();
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
	//	pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		String updatedEmail = userEmail.replace("autoiframe", "autoiframe_n");

		// update user profile details
		Response profileUpdateResponse = pageObj.endpoints().authApiUpdateUserInfo(dataSet.get("client"),
				dataSet.get("secret"), auth_token, updatedEmail, dataSet.get("newFN"), dataSet.get("newLN"));
		Assert.assertEquals(profileUpdateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched ");
		logger.info("Successfully updated profile details of user ");
		pageObj.utils().logPass("Successfully updated profile details of user ");
		// update user account balance
		Response sendGiftResponse = pageObj.endpoints().sendPointsToUser(userID, dataSet.get("points"),
				dataSet.get("businessAdminKey"));
		Assert.assertEquals(sendGiftResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Status code 201 did not matched ");
		logger.info("Successfully gifted points to user ");
		pageObj.utils().logPass("Successfully gifted points to user ");

		// check updated details on android pass
		int i = 1;
		do {
			driver.navigate().refresh();
			Thread.sleep(4000);
			String newName = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel(dataSet.get("nameLabel"));
			String newEmail = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel(dataSet.get("emailLabel"));
			String newBalance = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel(dataSet.get("pointsLabel"));
			if (newName.contains(dataSet.get("newFN")) && newName.contains(dataSet.get("newLN"))
					&& newEmail.equals(updatedEmail) && newBalance.equals(dataSet.get("points"))) {
				logger.info("Successfully updated details on androidPass ");
				pageObj.utils().logit("Successfully updated details on androidPass ");
				break;
			} else {
				logger.info("Details not updated on androidPass,attempt: " + i);
				pageObj.utils().logit("Details not updated on androidPass,attempt: " + i);
				i++;
			}
		} while (i <= 4);
		Assert.assertNotEquals(i, 5, "Details not updated on androidPass : ");
		logger.info(
				"Successfully verified that android loyalty pass is updated if user update profile details, account balance ");
		pageObj.utils().logPass(
				"Successfully verified that android loyalty pass is updated if user update profile details, account balance ");
		pageObj.newCamHomePage().navigateToBackPage();
		pageObj.guestTimelinePage().clickOnRemoveFromWalletButton();

	}

	@Test(description = " SQ-T5783 : INT2-1540 | INT2-1349 | Verify that the Android Personalize Pass option should be there while setting up the Android Pass on the Setting screen. "
			+ " SQ-T5785 : INT2-1538 | Verify the Google Pass onboarding workflow( Design and Backend validations) for the below cases."
			+ " SQ-T5787 : INT2-1611 | AndroidPersonalizePass : Handle the android_pass_url not use by user again and again by clicking"
			+ " SQ-T5786 : INT2-1969 | INT2-1976| Verify Android Personalize PASS flow for the New User ( New Email ) when any of the Guest Validation is enabled."
			+ " SQ-T5721 : INT2-1111 : Verify tag : {{{save_to_android_pay_url}}} , in Mass campaign "
			+ " SQ-T5784 : INT2-1824 | Verify that if Android Personalize Pass is enabled then GooglePass should be visible while creating any segment ")
	public void T5783_T5785_verifyAndroidPersonalizePassFlow() throws Exception {

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable passes in cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_apple_passes", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check allow personalize pass option in android loyalty pass settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
		pageObj.settingsPage().clickOnApplePassCheckBox("Allow personalize pass", "ON");
		pageObj.settingsPage().clickOnApplePassSaveButton();

		// check 'android_personalize_pass' value under preferences in the
		// “android_pay_classes” table.
		String query1 = "select preferences from android_pay_classes where business_id = '" + dataSet.get("business_ID")
				+ "'";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "preferences", 2);
		String flag = DBUtils.businessesPreferenceFlag(expColValue1, "allow_personalize_pass");
		Assert.assertEquals(flag, "true", "android_personalize_pass value is not true in android_pay_classes table");
		logger.info("Verified that android_personalize_pass value is true in android_pay_classes table");
		pageObj.utils().logPass("Verified that android_personalize_pass value is true in android_pay_classes table");

		List<String> expectedList = Arrays.asList(dataSet.get("tag1"), dataSet.get("msg1"), dataSet.get("tag2"),
				dataSet.get("msg2"));

		// Navigate to segments
		pageObj.menupage().navigateToSubMenuItem("Guests", "Segments");
		pageObj.newSegmentHomePage().switchToNewSegmentManagementToolBtn();
		pageObj.newSegmentHomePage().verifyNewSegmentHomePage("All Segments");
		pageObj.newSegmentHomePage().clickOnCreateSegmentButton();
		utils.switchToWindow();

		String segmentName = CreateDateTime.getUniqueString("Profile_Details_Channel_GooglePass_");
		pageObj.newSegmentHomePage().setSegmentBetaName(segmentName);
		pageObj.segmentsBetaPage().selectSegmentType("Profile Details");
		pageObj.segmentsBetaPage().setAttribute("Onboarding Channel");
		pageObj.segmentsBetaPage().setOperatorText("Onboarding channel", "GooglePass");

		// navigate to campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("DND_campaign"));
		pageObj.newCamHomePage().selectCampaignOption("Edit");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.campaignspage().openOrCloseTagsButton("open");
		List<String> tagsAndDescriptionList = pageObj.campaignspage().getTagsAndDescriptionList();

		boolean verify = tagsAndDescriptionList.containsAll(expectedList);
		Assert.assertTrue(verify, "Tags and Description list does not contain expected values");
		logger.info("Verified that Tags and Description list contains expected values");
		pageObj.utils().logPass("Verified that Tags and Description list contains expected values");
		pageObj.campaignspage().openOrCloseTagsButton("close");
		String subject = "Test_Subject_Personalize_PASS_" + CreateDateTime.getTimeDateString();
		pageObj.signupcampaignpage().setEmailSubject(subject);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickPreviousButton();

		pageObj.campaignspage().sendTestNotification(dataSet.get("DND_email"));

		String email = GmailConnection.getGmailEmailBody("subject", subject, true);
		List<String> personalizePassLinks = pageObj.guestTimelinePage().extractLinksFromGmailBody(email);

		// Verify Android Personalize PASS flow for the New User ( New Email ).
		String firstName1 = CreateDateTime.getUniqueString("testFN_");
		String lastName1 = CreateDateTime.getUniqueString("testLN_");
		String email1 = CreateDateTime.getUniqueString("testUserAP_") + "@partech.com"; // AP : Android Pass

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(0));
		pageObj.guestTimelinePage().signInWithGoogleTestAccount(prop.getProperty("testAccountEmail"),
				utils.decrypt(prop.getProperty("testAccountPwd")));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		// pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName1, lastName1, email1);
		// pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();

		// verify entry in users table with signup_channel = GooglePass
		String query2 = "select COUNT(*) as count,signup_channel,id,user_details from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email1 + "'";
		boolean expValue2 = DBUtils.verifyValueFromDBUsingPolling(env, query2, "count", "1");
		boolean expValue2a = DBUtils.verifyValueFromDBUsingPolling(env, query2, "signup_channel", "GooglePass");
		String userId1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "id", 2);
		String userDetails = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query2, "user_details", 2);
		Assert.assertTrue(expValue2, "Entry not present in users table with signup_channel = GooglePass");
		Assert.assertTrue(expValue2a, "signup_channel is not GooglePass in users table");
		Assert.assertTrue(userDetails.contains("privacy_policy: true"),
				"user_details does not contain :privacy_policy: true");
		logger.info("Verified that entry is present in users table with signup_channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = GooglePass");

		// verify entry in user_transitions table with channel = GooglePass
		String query3 = "select COUNT(*) as count,channel from user_transitions where business_id = '"
				+ dataSet.get("business_ID") + "' and user_id = '" + userId1 + "'";
		boolean expValue3 = DBUtils.verifyValueFromDBUsingPolling(env, query3, "count", "1");
		boolean expValue3a = DBUtils.verifyValueFromDBUsingPolling(env, query3, "channel", "GooglePass");
		Assert.assertTrue(expValue3, "Entry not present in user_transitions table with channel = GooglePass");
		Assert.assertTrue(expValue3a, "channel is not GooglePass in user_transitions table");
		logger.info("Verified that entry is present in user_transitions table with channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in user_transitions table with channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query4 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userId1 + "'";
		boolean expValue4 = DBUtils.verifyValueFromDBUsingPolling(env, query4, "count", "1");
		boolean expValue4a = DBUtils.verifyValueFromDBUsingPolling(env, query4, "source", "email");
		Assert.assertTrue(expValue4, "Entry not present in android_passes table with source = email");
		Assert.assertTrue(expValue4a, "source is not email in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = email");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = email");

		// Verify Android Personalize PASS flow for Loyalty user who has no Android
		// Pass.
		// create a user
		String firstName2 = CreateDateTime.getUniqueString("testFN_");
		String lastName2 = CreateDateTime.getUniqueString("testLN_");
		String email2 = CreateDateTime.getUniqueString("testUser_") + "@partech.com";
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(email2, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		logger.info("Verified user signup via API v1");
		pageObj.utils().logPass("Verified user signup via API v1");
		String userID = signUpResponse.jsonPath().get("id").toString();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(1));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		// pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName2, lastName2, email2);
		// pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();

		// verify entry in users table but signup_channel != GooglePass
		String query5 = "select COUNT(*) as count,signup_channel from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email2 + "'";
		boolean expValue5 = DBUtils.verifyValueFromDBUsingPolling(env, query5, "count", "1");
		boolean expValue5a = DBUtils.verifyValueFromDBUsingPolling(env, query5, "signup_channel", "GooglePass");
		Assert.assertTrue(expValue5, "Entry is not present in users table");
		Assert.assertFalse(expValue5a, "signup_channel is GooglePass in users table");
		logger.info("Verified that entry is present in users table but signup_channel != GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table but signup_channel != GooglePass");

		// verify entry in user_transitions table with channel = GooglePass
		String query6 = "select COUNT(*) as count,channel from user_transitions where business_id = '"
				+ dataSet.get("business_ID") + "' and user_id = '" + userID + "'";
		boolean expValue6 = DBUtils.verifyValueFromDBUsingPolling(env, query6, "count", "1");
		boolean expValue6a = DBUtils.verifyValueFromDBUsingPolling(env, query6, "channel", "GooglePass");
		Assert.assertTrue(expValue6, "Entry is not present in user_transitions table");
		Assert.assertTrue(expValue6a, "Channel is not GooglePass in user_transitions table");
		logger.info("Verified that entry is present in user_transitions table with channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in user_transitions table with channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query7 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userID + "'";
		boolean expValue7 = DBUtils.verifyValueFromDBUsingPolling(env, query7, "count", "1");
		boolean expValue7a = DBUtils.verifyValueFromDBUsingPolling(env, query7, "source", "sms");
		Assert.assertTrue(expValue7, "Entry is not present in android_passes table");
		Assert.assertTrue(expValue7a, "source is not sms in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = sms");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = sms");

		// Verify Android Personalize PASS flow for Loyalty user who has Android Pass.
		String expectedFirstName = dataSet.get("existingUserFN");
		String userId3 = dataSet.get("existingUserId");
		String firstName3 = CreateDateTime.getUniqueString("testFN_");
		String lastName3 = CreateDateTime.getUniqueString("testLN_");
		String email3 = dataSet.get("existingUserEmail");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(2));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName3, lastName3, email3);
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();

		// verify entry in users table but signup_channel != GooglePass
		String query8 = "select COUNT(*) as count,signup_channel from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email3 + "'";
		boolean expValue8 = DBUtils.verifyValueFromDBUsingPolling(env, query8, "count", "1");
		boolean expValue8a = DBUtils.verifyValueFromDBUsingPolling(env, query8, "signup_channel", "GooglePass");
		Assert.assertTrue(expValue8, "Entry is not present in users table");
		Assert.assertFalse(expValue8a, "signup_channel is GooglePass in users table");
		logger.info("Verified that entry is present in users table but signup_channel != GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table but signup_channel != GooglePass");

		// verify no entry created in user_transitions table
		String query9 = "select COUNT(*) as count from user_transitions where business_id = '"
				+ dataSet.get("business_ID") + "' and user_id = '" + userId3 + "'";
		boolean expValue9 = DBUtils.verifyValueFromDBUsingPolling(env, query9, "count", "0");
		Assert.assertTrue(expValue9, "Entry is present in user_transitions table");
		logger.info("Verified that no entry is present in user_transitions table");
		pageObj.utils().logPass("Verified that no entry is present in user_transitions table");

		// verify entry created in android_passes table
		String query10 = "select COUNT(*) as count from android_passes where owner_id = '" + userId3 + "'";
		boolean expValue10 = DBUtils.verifyValueFromDBUsingPolling(env, query10, "count", "1");
		Assert.assertTrue(expValue10, "Entry is not present in android_passes table");
		logger.info("Verified that entry is present in android_passes table");
		pageObj.utils().logPass("Verified that entry is present in android_passes table");

		// verify conversion to existing android pass
		String nameOnPass = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel("Name");
		Assert.assertEquals(nameOnPass, expectedFirstName, "Failed to open existing android pass of user");
		logger.info("Verified that existing android pass is opened");
		pageObj.utils().logPass("Verified that existing android pass is opened");

		// Verify Android Personalize PASS flow for Eclub user.
		// eclub upload via email
		String eClubUserEmail = CreateDateTime.getUniqueString("testEclubUser_") + "@partech.com";
		Response eclubUploadResponse = pageObj.endpoints().dashboardApiEClubUpload(eClubUserEmail,
				dataSet.get("adminToken"), dataSet.get("storeNumber"), false);
		pageObj.apiUtils().verifyResponse(eclubUploadResponse, "eclubUpload");
		String firstName4 = CreateDateTime.getUniqueString("testFN_");
		String lastName4 = CreateDateTime.getUniqueString("testLN_");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(3));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName4, lastName4, eClubUserEmail);
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();

		// verify entry in users table with signup_channel = eClub and joined_at != null
		String query11 = "select COUNT(*) as count,signup_channel,joined_at,id from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + eClubUserEmail + "'";
		boolean expValue11 = DBUtils.verifyValueFromDBUsingPolling(env, query11, "count", "1");
		boolean expValue11a = DBUtils.verifyValueFromDBUsingPolling(env, query11, "signup_channel", "eClub");
		String expValue11b = DBUtils.executeQueryAndGetColumnValue(env, query11, "joined_at");
		String eClubUserId = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query11, "id", 2);
		Assert.assertTrue(expValue11, "Entry not present in users table with signup_channel = eClub");
		Assert.assertTrue(expValue11a, "signup_channel is not eClub in users table");
		Assert.assertNotNull(expValue11b, "joined_at is null in users table");
		logger.info("Verified that entry is present in users table with signup_channel = eClub");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = eClub");

		// verify entry in user_transitions table with channel = GooglePass
		String query12 = "select COUNT(*) as count from user_transitions where business_id = '"
				+ dataSet.get("business_ID") + "' and user_id = '" + eClubUserId + "' and channel = 'GooglePass' ;";
		boolean expValue12 = DBUtils.verifyValueFromDBUsingPolling(env, query12, "count", "1");
		Assert.assertTrue(expValue12, "Entry not present in user_transitions table with channel = GooglePass");
		logger.info("Verified that entry is present in user_transitions table with channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in user_transitions table with channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query13 = "select COUNT(*) as count,source from android_passes where owner_id = '" + eClubUserId + "'";
		boolean expValue13 = DBUtils.verifyValueFromDBUsingPolling(env, query13, "count", "1");
		boolean expValue13a = DBUtils.verifyValueFromDBUsingPolling(env, query13, "source", "sms");
		Assert.assertTrue(expValue13, "Entry not present in android_passes table with source = sms");
		Assert.assertTrue(expValue13a, "source is not sms in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = sms");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = sms");

		// Verify Android Personalize PASS flow for User Who is Awaiting Migration user.
		// create Awaiting Migration user
		String userEmail_AM = CreateDateTime.getUniqueString("testAMUser_") + "@partech.com";
		Response createMigrationUserResponse = pageObj.endpoints().createBusinessMigrationUser(userEmail_AM,
				dataSet.get("adminToken"), "10", "20");
		Assert.assertEquals(createMigrationUserResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("Successfully created Awaiting Migration user");
		pageObj.utils().logPass("Successfully created Awaiting Migration user");

		String firstName5 = CreateDateTime.getUniqueString("testFN_");
		String lastName5 = CreateDateTime.getUniqueString("testLN_");

		// verify Awaiting Migration user is created and punchh user_id is null
		String query14 = "select COUNT(*) as count,user_id from business_migration_users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + userEmail_AM + "'";
		boolean expValue14 = DBUtils.verifyValueFromDBUsingPolling(env, query14, "count", "1");
		String expValue14a = DBUtils.executeQueryAndGetColumnValue(env, query14, "user_id");
		Assert.assertTrue(expValue14, "Entry not present in business_migration_users table");
		Assert.assertNull(expValue14a, "user_id is not null in business_migration_users table");
		logger.info("Verified that entry is present in business_migration_users table and user_id is null");
		pageObj.utils().logPass("Verified that entry is present in business_migration_users table and user_id is null");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(4));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName5, lastName5, userEmail_AM);
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();

		// verify entry created in users table with signup_channel = GooglePass
		String query15 = "select COUNT(*) as count,signup_channel,id from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + userEmail_AM + "'";
		boolean expValue15 = DBUtils.verifyValueFromDBUsingPolling(env, query15, "count", "1");
		boolean expValue15a = DBUtils.verifyValueFromDBUsingPolling(env, query15, "signup_channel", "GooglePass");
		String userId_AM = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query15, "id", 2);
		Assert.assertTrue(expValue15, "Entry not present in users table with signup_channel = GooglePass");
		Assert.assertTrue(expValue15a, "signup_channel is not GooglePass in users table");
		logger.info("Verified that entry is present in users table with signup_channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = GooglePass");

		// verify entry created in user_transitions table with channel = GooglePass
		String query16 = "select COUNT(*) as count,channel from user_transitions where business_id = '"
				+ dataSet.get("business_ID") + "' and user_id = '" + userId_AM + "'";
		boolean expValue16 = DBUtils.verifyValueFromDBUsingPolling(env, query16, "count", "1");
		boolean expValue16a = DBUtils.verifyValueFromDBUsingPolling(env, query16, "channel", "GooglePass");
		Assert.assertTrue(expValue16, "Entry not present in user_transitions table with channel = GooglePass");
		Assert.assertTrue(expValue16a, "channel is not GooglePass in user_transitions table");
		logger.info("Verified that entry is present in user_transitions table with channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in user_transitions table with channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query17 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userId_AM + "'";
		boolean expValue17 = DBUtils.verifyValueFromDBUsingPolling(env, query17, "count", "1");
		boolean expValue17a = DBUtils.verifyValueFromDBUsingPolling(env, query17, "source", "email");
		Assert.assertTrue(expValue17, "Entry not present in android_passes table with source = email");
		Assert.assertTrue(expValue17a, "source is not email in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = email");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = email");

		// verify user_id is updated in business_migration_users table
		String query18 = "select user_id from business_migration_users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + userEmail_AM + "'";
		String expValue18 = DBUtils.executeQueryAndGetColumnValue(env, query18, "user_id");
		Assert.assertEquals(expValue18, userId_AM, "user_id is not updated in business_migration_users table");
		logger.info("Verified that user_id is updated in business_migration_users table");
		pageObj.utils().logPass("Verified that user_id is updated in business_migration_users table");

		// Verify that personalize pass link On Google Pass is not used by user multiple
		// times
		String firstName6 = CreateDateTime.getUniqueString("testFN_");
		String lastName6 = CreateDateTime.getUniqueString("testLN_");
		String userEmail6 = CreateDateTime.getUniqueString("testUserAP_") + "@partech.com"; // AP : Android Pass

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(5));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		String linkOnGooglePass = pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName6, lastName6,
				userEmail6);
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();

		pageObj.instanceDashboardPage().navigateToPunchhInstance(linkOnGooglePass);
		utils.waitTillPagePaceDone();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName6 + "_new", lastName6 + "_new",
				userEmail6.replace("partech", "punchhhh"));
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		String actualName = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel("Name");
		String actualEmail = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel("Email");

		Assert.assertEquals(actualName, firstName6, "Firstname is updated to new value");
		Assert.assertEquals(actualEmail, userEmail6, "Email is updated to new value");
		logger.info("Verified that personalize pass link On Google Pass is not used by user multiple times");
		pageObj.utils().logPass("Verified that personalize pass link On Google Pass is not used by user multiple times");

		// Verify that personalize pass link in Email inbox is not used by user multiple
		// times
		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(5));
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		boolean flag2 = utils.getLocator("guestTimeLine.personalizePassBtn").isDisplayed();
		Assert.assertFalse(flag2, "Personalize pass button is visible on Google pass");
		logger.info("Verified that personalize pass link in Email inbox is not used by user multiple times");
		pageObj.utils().logPass("Verified that personalize pass link in Email inbox is not used by user multiple times");

		// verify download pass tag in email template
		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(6));
		pageObj.guestTimelinePage().viewFullAndroidLoyaltyPassOfUser();
		String emailOnPass = pageObj.guestTimelinePage().getDetailsOnAndroidPassByLabel("Email");
		Assert.assertEquals(emailOnPass, dataSet.get("DND_email"), "Email does not match with expected value");
		logger.info("Verified that email on pass is same as expected value");
		pageObj.utils().logPass("Verified that email on pass is same as expected value");
		// verify entry in android_passes table
		String query19 = "select COUNT(*) as count from android_passes where owner_id = '" + dataSet.get("userId_qa")
				+ "'";
		boolean expValue19 = DBUtils.verifyValueFromDBUsingPolling(env, query19, "count", "1");
		Assert.assertTrue(expValue19, "Entry is not present in android_passes table");
		logger.info("Verified that entry is present in android_passes table");

		TestListeners.extentTest.get().pass("Verified that entry is present in android_passes table");
	
	

		pageObj.utils().logPass("Verified that entry is present in android_passes table");


	}
	

	@Test(description = "SQ-T7273 : Verify if universal link generated through tags is allowing to create multiple users when hit multiple time")
	public void T7273_verifyPersonaliseUniversalURLForSignup() throws Exception {
		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// enable passes in cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_apple_passes", "check");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// check allow personalize pass option in android loyalty pass settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
		pageObj.settingsPage().clickOnApplePassCheckBox("Allow personalize pass", "ON");
		pageObj.settingsPage().clickOnApplePassSaveButton();

		List<String> expectedList = Arrays.asList(dataSet.get("tag1"), dataSet.get("msg1"), dataSet.get("tag2"),
				dataSet.get("msg2"));
		String personalizePassEmailLink = dataSet.get("tag1");
		String personalizePassSMSLink = dataSet.get("tag2");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("DND_campaign"));
		pageObj.newCamHomePage().selectCampaignOption("Edit");
		pageObj.signupcampaignpage().clickNextBtn();
		pageObj.campaignspage().openOrCloseTagsButton("open");
		List<String> tagsAndDescriptionList = pageObj.campaignspage().getTagsAndDescriptionList();

		boolean verify = tagsAndDescriptionList.containsAll(expectedList);
		Assert.assertTrue(verify, "Tags and Description list does not contain expected values");
		logger.info("Verified that Tags and Description list contains expected values");
		pageObj.utils().logPass("Verified that Tags and Description list contains expected values");
		pageObj.campaignspage().openOrCloseTagsButton("close");
		pageObj.signupcampaignpage().setPushNotification(personalizePassSMSLink);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickPreviousButton();

		pageObj.campaignspage().sendTestNotification(dataSet.get("DND_email"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("DND_email"));

		List<String> personalizePassLinks = pageObj.guestTimelinePage()
				.extractLinksFromPNBody(dataSet.get("DND_campaign"));

		// Verify Android Personalize PASS flow for the New User ( New Email ).
		String firstName1 = CreateDateTime.getUniqueString("testFN_");
		String lastName1 = CreateDateTime.getUniqueString("testLN_");
		String email1 = CreateDateTime.getUniqueString("testUserAP_") + "@partech.com"; // AP : Android Pass

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(0));
		pageObj.guestTimelinePage().signInWithGoogleTestAccount(prop.getProperty("testAccountEmail"),
				utils.decrypt(prop.getProperty("testAccountPwd")));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName1, lastName1, email1);

		// verify entry in users table with signup_channel = GooglePass
		String query1 = "select COUNT(*) as count,signup_channel,id,user_details from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email1 + "'";
		boolean expValue1 = DBUtils.verifyValueFromDBUsingPolling(env, query1, "count", "1");
		boolean expValue1a = DBUtils.verifyValueFromDBUsingPolling(env, query1, "signup_channel", "GooglePass");
		String userId1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "id", 2);
		String userDetails = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "user_details", 2);
		Assert.assertTrue(expValue1, "Entry not present in users table with signup_channel = GooglePass");
		Assert.assertTrue(expValue1a, "signup_channel is not GooglePass in users table");
		Assert.assertTrue(userDetails.contains("privacy_policy: true"),
				"user_details does not contain :privacy_policy: true");
		logger.info("Verified that entry is present in users table with signup_channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = GooglePass");

		// verify entry in user_transitions table with channel = GooglePass
		String query3 = "select COUNT(*) as count,channel from user_transitions where business_id = '"
				+ dataSet.get("business_ID") + "' and user_id = '" + userId1 + "'";
		boolean expValue3 = DBUtils.verifyValueFromDBUsingPolling(env, query3, "count", "1");
		boolean expValue3a = DBUtils.verifyValueFromDBUsingPolling(env, query3, "channel", "GooglePass");
		Assert.assertTrue(expValue3, "Entry not present in user_transitions table with channel = GooglePass");
		Assert.assertTrue(expValue3a, "channel is not GooglePass in user_transitions table");
		logger.info("Verified that entry is present in user_transitions table with channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in user_transitions table with channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query4 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userId1 + "'";
		boolean expValue4 = DBUtils.verifyValueFromDBUsingPolling(env, query4, "count", "1");
		boolean expValue4a = DBUtils.verifyValueFromDBUsingPolling(env, query4, "source", "sms");
		Assert.assertTrue(expValue4, "Entry not present in android_passes table with source = sms");
		Assert.assertTrue(expValue4a, "source is not email in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = sms");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = sms");

		String firstName2 = CreateDateTime.getUniqueString("testFN_");
		String lastName2 = CreateDateTime.getUniqueString("testLN_");
		String email2 = CreateDateTime.getUniqueString("testUserAP_") + "@partech.com"; // AP : Android Pass

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLinks.get(0));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName2, lastName2, email2);

		// verify entry in users table with signup_channel = GooglePass
		String query5 = "select COUNT(*) as count,signup_channel,id,user_details from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email1 + "'";
		boolean expValue5 = DBUtils.verifyValueFromDBUsingPolling(env, query5, "count", "1");
		boolean expValue5a = DBUtils.verifyValueFromDBUsingPolling(env, query5, "signup_channel", "GooglePass");
		String userId2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query1, "id", 2);
		String userDetails1 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query5, "user_details", 2);
		Assert.assertTrue(expValue5, "Entry not present in users table with signup_channel = GooglePass");
		Assert.assertTrue(expValue5a, "signup_channel is not GooglePass in users table");
		Assert.assertTrue(userDetails1.contains("privacy_policy: true"),
				"user_details does not contain :privacy_policy: true");
		logger.info("Verified that entry is present in users table with signup_channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query6 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userId2 + "'";
		boolean expValue6 = DBUtils.verifyValueFromDBUsingPolling(env, query6, "count", "1");
		boolean expValue6a = DBUtils.verifyValueFromDBUsingPolling(env, query6, "source", "sms");
		Assert.assertTrue(expValue6, "Entry not present in android_passes table with source = sms");
		Assert.assertTrue(expValue6a, "source is not email in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = sms");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = sms");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("DND_campaign"));
		pageObj.newCamHomePage().selectCampaignOption("Edit");
		pageObj.signupcampaignpage().clickNextBtn();

		pageObj.signupcampaignpage().setPushNotification(personalizePassEmailLink);
		pageObj.signupcampaignpage().clickNextButton();
		pageObj.signupcampaignpage().clickPreviousButton();

		pageObj.campaignspage().sendTestNotification(dataSet.get("DND_email"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(dataSet.get("DND_email"));

		List<String> personalizePassLink1 = pageObj.guestTimelinePage()
				.extractLinksFromPNBody(dataSet.get("DND_campaign"));

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLink1.get(0));

		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName1, lastName1, email1);

		String firstName3 = CreateDateTime.getUniqueString("testFN_");
		String lastName3 = CreateDateTime.getUniqueString("testLN_");
		String email3 = CreateDateTime.getUniqueString("testUserAP_") + "@partech.com"; // AP : Android Pass

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLink1.get(0));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName3, lastName3, email3);

		// verify entry in users table with signup_channel = GooglePass
		String query7 = "select COUNT(*) as count,signup_channel,id,user_details from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email3 + "'";
		boolean expValue7 = DBUtils.verifyValueFromDBUsingPolling(env, query7, "count", "1");
		boolean expValue7a = DBUtils.verifyValueFromDBUsingPolling(env, query7, "signup_channel", "GooglePass");
		String userId3 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "id", 2);
		String userDetails2 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query7, "user_details", 2);
		Assert.assertTrue(expValue7, "Entry not present in users table with signup_channel = GooglePass");
		Assert.assertTrue(expValue7a, "signup_channel is not GooglePass in users table");
		Assert.assertTrue(userDetails2.contains("privacy_policy: true"),
				"user_details does not contain :privacy_policy: true");
		logger.info("Verified that entry is present in users table with signup_channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query8 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userId3 + "'";
		boolean expValue8 = DBUtils.verifyValueFromDBUsingPolling(env, query8, "count", "1");
		boolean expValue8a = DBUtils.verifyValueFromDBUsingPolling(env, query8, "source", "email");
		Assert.assertTrue(expValue8, "Entry not present in android_passes table with source = email");
		Assert.assertTrue(expValue8a, "source is not email in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = email");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = email");

		String firstName4 = CreateDateTime.getUniqueString("testFN_");
		String lastName4 = CreateDateTime.getUniqueString("testLN_");
		String email4 = CreateDateTime.getUniqueString("testUserAP_") + "@partech.com"; // AP : Android Pass

		pageObj.instanceDashboardPage().navigateToPunchhInstance(personalizePassLink1.get(0));
		pageObj.guestTimelinePage().clickOnAddToWalletButton();
		pageObj.guestTimelinePage().clickOnPersonalizePassBtn();
		pageObj.guestTimelinePage().enterDetailsOnPersonalizePassPage(firstName4, lastName4, email4);

		// verify entry in users table with signup_channel = GooglePass
		String query9 = "select COUNT(*) as count,signup_channel,id,user_details from users where business_id = '"
				+ dataSet.get("business_ID") + "' and email = '" + email4 + "'";
		boolean expValue9 = DBUtils.verifyValueFromDBUsingPolling(env, query9, "count", "1");
		boolean expValue9a = DBUtils.verifyValueFromDBUsingPolling(env, query9, "signup_channel", "GooglePass");
		String userId4 = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query9, "id", 2);
		Assert.assertTrue(expValue9, "Entry not present in users table with signup_channel = GooglePass");
		Assert.assertTrue(expValue9a, "signup_channel is not GooglePass in users table");
		Assert.assertTrue(userDetails1.contains("privacy_policy: true"),
				"user_details does not contain :privacy_policy: true");
		logger.info("Verified that entry is present in users table with signup_channel = GooglePass");
		pageObj.utils().logPass("Verified that entry is present in users table with signup_channel = GooglePass");

		// verify entry created in android_passes table with correct source
		String query10 = "select COUNT(*) as count,source from android_passes where owner_id = '" + userId4 + "'";
		boolean expValue10 = DBUtils.verifyValueFromDBUsingPolling(env, query10, "count", "1");
		boolean expValue10a = DBUtils.verifyValueFromDBUsingPolling(env, query10, "source", "email");
		Assert.assertTrue(expValue10, "Entry not present in android_passes table with source = email");
		Assert.assertTrue(expValue10a, "source is not email in android_passes table");
		logger.info("Verified that entry is present in android_passes table with source = email");
		pageObj.utils().logPass("Verified that entry is present in android_passes table with source = email");

		logger.info(
				"Verified that personalize pass universal link is allowing to create multiple users when hit multiple time");
		pageObj.utils().logPass(
				"Verified that personalize pass universal link is allowing to create multiple users when hit multiple time");

	}

//	@Test(description = "SQ-T7533 : Verify available reward limit and error message in available reward limit in case of android")
//	public void T7533_verifyAvailableRewardLimitAndroid() throws Exception {
//		// open business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//
//		// enable passes in cockpit
//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
//		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_apple_passes", "check");
//		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
//
//		// check allow personalize pass option in android loyalty pass settings
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
//		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
//		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
//		pageObj.settingsPage().enterSmartPassRewardLimit(dataSet.get("rewardLimit"));
//		pageObj.settingsPage().clickOnApplePassSaveButton();
//
//		String query = "SELECT \n" + "TRIM(\n" + "    SUBSTRING_INDEX(\n"
//				+ "        SUBSTRING_INDEX(preferences, ':available_rewards_limit: ', -1),\n" + "        '\\n',\n"
//				+ "        1\n" + "    )\n" + ") AS smart_pass_rewards_limit\n" + "FROM android_pay_classes\n"
//				+ "WHERE preferences LIKE '%:available_rewards_limit:%' and business_id = " + dataSet.get("businessId")
//				+ " and type = \"AndroidPayLoyaltyClass\";\n" + "";
//		boolean expValue = DBUtils.verifyValueFromDBUsingPolling(env, query, "smart_pass_rewards_limit",
//				dataSet.get("rewardLimit"));
//		Assert.assertTrue(expValue, "Available reward limit is not updated in android_pay_classes table");
//		logger.info("Verified that available reward limit is updated in android_pay_classes table");
//		TestListeners.extentTest.get()
//				.pass("Verified that available reward limit is updated in android_pay_classes table");
//
//		pageObj.dashboardpage().navigateToAllBusinessPage();
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
//		String actualRewardLimit = pageObj.dashboardpage().getValueFromSmartPassRewardLimitFieldInGlobalConfig();
//		int num = Integer.parseInt(actualRewardLimit);
//		// verify that reward limit field is accepting only number less than the value
//		// set
//		// in global configuration
//		int num1 = num + 20;
//		pageObj.dashboardpage().navigateToAllBusinessPage();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
//		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
//		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
//		pageObj.settingsPage().enterSmartPassRewardLimit(Integer.toString(num1));
//		pageObj.settingsPage().clickOnApplePassSaveButton();
//		boolean isErrorMsgDisplayed = pageObj.settingsPage().isErrorMsgDisplayedForRewardLimit();
//		Assert.assertTrue(isErrorMsgDisplayed,
//				"Error message is not displayed when reward limit value is more than the value set in global configuration");
//		String actualErrorMsg = pageObj.settingsPage().getErrorMsg2DisplayedForRewardLimit();
//		String expectedErrorMsg = dataSet.get("errorMsg") + actualRewardLimit + ".";
//		Assert.assertEquals(actualErrorMsg, expectedErrorMsg,
//				"Error message text is not correct when reward limit value is more than the value set in global configuration");
//		logger.info(
//				"Verified that error message is displayed with correct text when reward limit value is more than the value set in global configuration");
//		TestListeners.extentTest.get().pass(
//				"Verified that error message is displayed with correct text when reward limit value is more than the value set in global configuration");
//
//	}
//
//	@Test(description = "SQ-T7534 : Verify available reward limit and error message in available reward limit in case of apple")
//	public void T7534_verifyAvailableRewardLimitApple() throws Exception {
//		// open business
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//
//		// enable passes in cockpit
//		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
//		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_apple_passes", "check");
//		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
//
//		// check allow personalize pass option in android loyalty pass settings
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
//		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
//		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
//		pageObj.settingsPage().enterSmartPassRewardLimit(dataSet.get("rewardLimit"));
//		pageObj.settingsPage().clickOnApplePassSaveButton();
//
//		String query = "SELECT \n" + "TRIM(\n" + "    SUBSTRING_INDEX(\n"
//				+ "        SUBSTRING_INDEX(preferences, ':available_rewards_limit: ', -1),\n" + "        '\\n',\n"
//				+ "        1\n" + "    )\n" + ") AS smart_pass_rewards_limit\n" + "FROM apple_pass_designs\n"
//				+ "WHERE preferences LIKE '%:available_rewards_limit:%' and business_id = " + dataSet.get("businessId")
//				+ " and type = \"AppleLoyaltyPassDesign\";\n" + "";
//		boolean expValue = DBUtils.verifyValueFromDBUsingPolling(env, query, "smart_pass_rewards_limit",
//				dataSet.get("rewardLimit"));
//		Assert.assertTrue(expValue, "Available reward limit is not updated in android_pay_classes table");
//		logger.info("Verified that available reward limit is updated in android_pay_classes table");
//		TestListeners.extentTest.get()
//				.pass("Verified that available reward limit is updated in android_pay_classes table");
//
//		pageObj.dashboardpage().navigateToAllBusinessPage();
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Global Configuration");
//		String actualRewardLimit = pageObj.dashboardpage().getValueFromSmartPassRewardLimitFieldInGlobalConfig();
//		int num = Integer.parseInt(actualRewardLimit);
//		// verify that reward limit field is accepting only number less than the value
//		// set
//		// in global configuration
//		int num1 = num + 20;
//		pageObj.dashboardpage().navigateToAllBusinessPage();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//		pageObj.menupage().navigateToSubMenuItem("Settings", "Passes");
//		pageObj.settingsPage().clickOnDesiredPassButton(dataSet.get("passType"), dataSet.get("buttonType"));
//		pageObj.settingsPage().clickOnApplePassTab(dataSet.get("tab"));
//		pageObj.settingsPage().enterSmartPassRewardLimit(Integer.toString(num1));
//		pageObj.settingsPage().clickOnApplePassSaveButton();
//		boolean isErrorMsgDisplayed = pageObj.settingsPage().isErrorMsgDisplayedForRewardLimit();
//		Assert.assertTrue(isErrorMsgDisplayed,
//				"Error message is not displayed when reward limit value is more than the value set in global configuration");
//		String actualErrorMsg = pageObj.settingsPage().getErrorMsg2DisplayedForRewardLimit();
//		String expectedErrorMsg = dataSet.get("errorMsg") + actualRewardLimit + ".";
//		Assert.assertEquals(actualErrorMsg, expectedErrorMsg,
//				"Error message text is not correct when reward limit value is more than the value set in global configuration");
//		logger.info(
//				"Verified that error message is displayed with correct text when reward limit value is more than the value set in global configuration");
//		TestListeners.extentTest.get().pass(
//				"Verified that error message is displayed with correct text when reward limit value is more than the value set in global configuration");
//
//	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		dataSet.clear();
		driver.quit();
		logger.info("Browser closed");
	}

}