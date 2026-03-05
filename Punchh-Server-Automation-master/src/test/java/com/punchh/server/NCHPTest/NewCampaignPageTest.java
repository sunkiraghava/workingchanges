package com.punchh.server.NCHPTest;

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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class NewCampaignPageTest {

	private static Logger logger = LogManager.getLogger(NewCampaignPageTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env;
	private String baseUrl;
	private static Map<String, String> dataSet;
	String run = "ui";
	private Utilities utils;

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
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		// Instance select business
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Anant
	@Test(description = "SQ-T4391 Verify the warning message on rename of tags", groups = { "regression",
			"dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T4391_verifyWarningMsgRenameTags() throws InterruptedException {
		String tag = "Tag" + CreateDateTime.getTimeDateString();
		String renameTag = Utilities.getAlphaNumericString(55);
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// delete existing tags
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteExistingTag();
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().closeMangeTagFrame();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();

		String msg = pageObj.newCamHomePage().createNewTag(tag);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag);
		utils.logit("tag is created successfully " + tag);

		pageObj.newCamHomePage().renameTag(tag, renameTag);

		String errorMsg = pageObj.newCamHomePage().tagRenameErrorMsg();

		Assert.assertEquals(errorMsg, "Tag name is limited to 50 characters.");
		logger.info("verified getting error msg when the tag name exceed 50 characters");
		utils.logit("verified getting error msg when the tag name exceed 26 characters");
		pageObj.newCamHomePage().closeMangeTagFrame();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag);
		// pageObj.newCamHomePage().closeMangeTagFrame();
	}

	// Anant
	@Test(description = "SQ-T4431 Verify addition of tags in single campaign"
			+ "SQ-T4567 Verify manage tag modal dialog when business have 0 tags"
			+ "SQ-T4570 Verify URL when Tag Type filter is selected"
			+ "SQ-T4571 Verify tag modal dialog when business have 0 tags and open tag dialog from 3 dot and then tag"
			+ "SQ-T4759 Verify URL when Tag and Creator filter is selected", groups = { "regression",
					"dailyrun" }, priority = 4)
	@Owner(name = "Rakhi Rawat")
	public void T4431_verifyAdditionOfTagSingleCampaign() throws InterruptedException {
		String tag = "Tag" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// delete existing tags
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteExistingTag();
		pageObj.newCamHomePage().closeOptionsDailog();

		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// pageObj.newCamHomePage().clickNewCamHomePageBtn();

		// delete existing tags
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteExistingTag();
		pageObj.newCamHomePage().closeOptionsDailog();
		// create a new tag
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		String text = pageObj.newCamHomePage().noTagText();
		Assert.assertTrue(text.contains(dataSet.get("TextMsg1")), "text message is not equal");
		Assert.assertTrue(text.contains(dataSet.get("TextMsg2")), "text message is not equal");
		logger.info("Verfied when no tag is present in the business then no tag text msg is coming as expected");
		utils.logPass("Verfied when no tag is present in the business then no tag text msg is coming as expected");

		pageObj.newCamHomePage().clickCreateTagBtn();
		String msg = pageObj.newCamHomePage().createNewTag(tag);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		pageObj.newCamHomePage().closeOptionsDailog();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		// search campaign
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName"));
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().clickTagButton(dataSet.get("singleTag"));

		pageObj.newCamHomePage().selectTagForCampaign(tag);
		List<String> message = pageObj.newCamHomePage().clickApplyBtn();

		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tag " + tag);
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagAddedMsg")), "Added tag msg is not display");

		logger.info("Successfully added tag " + tag + "in the campaign " + dataSet.get("campaignName"));
		utils.logit("Successfully added tag " + tag + "in the campaign " + dataSet.get("campaignName"));

		// select creator and tag
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filterVal1"), dataSet.get("filter1"));
		pageObj.newCamHomePage().selectTagFilterInSidePanel(tag);
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String creatorVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("creatorRE"));
		creatorVal = creatorVal.replace("+", " ");
		String[] arr = creatorVal.split("%");
		String tagsVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("tagRE"));
		String[] arr1 = tagsVal.split("&");
		String[] arr2 = arr1[0].split("%3A");
		Assert.assertEquals(arr[0], dataSet.get("filterVal1"),
				"url value is not updated when creator filter is selected");
		Assert.assertEquals(arr2[0], tag, "url value is not updated when tag filter is selected");
		logger.info("Verified when creator and tag filter is applied then url value changed");
		utils.logPass("Verified when creator and tag filter is applied then url value changed");

		// de select the filter
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter1"));
		pageObj.newCamHomePage().removeFilter(dataSet.get("filter2"));

		// apply the tag filter
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectTagFilterInSidePanel(tag);
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String val = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("TagRE"));
		String[] arr3 = val.split("%3A");
		Assert.assertEquals(arr3[0], tag, "on selecting the tag url did not change");
		logger.info("verify on selecting the tag, url contains the selected tag");
		utils.logPass("verify on selecting the tag, url contains the selected tag");

		pageObj.newCamHomePage().removeFilter(dataSet.get("filter2"));
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignName"));
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().clickTagButton(dataSet.get("singleTag"));

		pageObj.newCamHomePage().deSelectTagForCampaign(tag);
		message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tag " + tag);
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagRemovedMsg")), "Removed tag msg is not display");

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag);
		// pageObj.newCamHomePage().closeOptionsDailog();

		// click on 3 dots
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaign(dataSet.get("campaignName"));
		pageObj.newCamHomePage().clickOptionFromDotsDropDown("Tag");
		String text2 = pageObj.newCamHomePage().noTagText();
		Assert.assertTrue(text2.contains(dataSet.get("TextMsg1")), "text message is not equal");
		Assert.assertTrue(text2.contains(dataSet.get("TextMsg2")), "text message is not equal");
		logger.info(
				"Verfied when no tag is present in the business then no tag text msg is coming as expected when we open the tag model dailog from dots icon");
		utils.logPass(
				"Verfied when no tag is present in the business then no tag text msg is coming as expected when we open the tag model dailog from dots icon");
	}

	// Anant
	@Test(description = "SQ-T4432 Verify addition of multiple tags in single campaign"
			+ "SQ-T4434 Verify addition of all tags in single campaign"
			+ "SQ-T4436 Verify removal of a tag in single campaign", groups = { "regression",
					"dailyrun" }, priority = 5)
	@Owner(name = "Rakhi Rawat")
	public void T4432_verifyAdditionOfMultipleTagSingleCampaign() throws InterruptedException {
		String tag1 = "Tag1" + CreateDateTime.getTimeDateString();
		String tag2 = "Tag2" + CreateDateTime.getTimeDateString();
		String tag3 = "Tag3" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// delete existing tags
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteExistingTag();
		pageObj.newCamHomePage().closeOptionsDailog();

		// create tag 1
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		String msg = pageObj.newCamHomePage().createNewTag(tag1);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag1);
		utils.logit("tag is created successfully " + tag1);
		pageObj.newCamHomePage().closeOptionsDailog();

		// create tag 2
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		msg = pageObj.newCamHomePage().createNewTag(tag2);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag2);
		utils.logit("tag is created successfully " + tag2);
		pageObj.newCamHomePage().closeOptionsDailog();
		// create tag 3
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		msg = pageObj.newCamHomePage().createNewTag(tag3);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag3);
		utils.logit("tag is created successfully " + tag3);
		pageObj.newCamHomePage().closeOptionsDailog();

		// select two tags for the campaigns
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName"));
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().clickTagButton(dataSet.get("singleTag"));
		pageObj.newCamHomePage().selectTagForCampaign(tag1);
		pageObj.newCamHomePage().selectTagForCampaign(tag2);
		List<String> message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagAddedMsg")), "Added tag msg is not display");
		logger.info("Successfully added tags in the campaign " + dataSet.get("campaignName"));
		utils.logPass("Successfully added tags in the campaign " + dataSet.get("campaignName"));

		// select all tags
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName"));
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().clickTagButton(dataSet.get("singleTag"));
		pageObj.newCamHomePage().selectOrDeselectAllTag("select");

		message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagAddedMsg")), "Added tag msg is not display");

		logger.info("Successfully added tags in the campaign " + dataSet.get("campaignName"));
		utils.logPass("Successfully added tags in the campaign " + dataSet.get("campaignName"));

		// removed the tags added to the campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().searchCampaignNCHP(dataSet.get("campaignName"));
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		pageObj.newCamHomePage().clickTagButton(dataSet.get("singleTag"));
		pageObj.newCamHomePage().selectOrDeselectAllTag("deselect");

		message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagRemovedMsg")), "Removed tag msg is not display");

		logger.info("Successfully removed tags from campaign " + dataSet.get("campaignName"));
		utils.logPass("Successfully added tags from campaign " + dataSet.get("campaignName"));

		// delete tag1
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag1);
		// pageObj.newCamHomePage().closeOptionsDailog();

		// delete tag2
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag2);
		// pageObj.newCamHomePage().closeOptionsDailog();

		// delete tag3
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag3);

		logger.info("successfully deleted both the tags");
		utils.logPass("successfully deleted both the tags");
	}

	// Anant
	@Test(description = "SQ-T4433 Verify addition of tags in multiple campaigns", priority = 6, groups = { "regression",
			"unstable", "dailyrun" })
	@Owner(name = "Rakhi Rawat")
	public void T4433_verifyAdditionOfTagMultipleCampaign() throws InterruptedException {
		String tag = "NewTag" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// delete existing tags
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().closeOptionsDailog();
		// create tag 1
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		String msg = pageObj.newCamHomePage().createNewTag(tag);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag);
		utils.logit("tag is created successfully " + tag);
		pageObj.newCamHomePage().closeMangeTagFrame();
		// select multiple campaigns
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		// pageObj.newCamHomePage().clickTagButton(dataSet.get("multipleCampaignTag"));
		pageObj.newCamHomePage().clickBottomTagButton();

		// select the tag for campaigns
		pageObj.newCamHomePage().selectTagForCampaign(tag);
		List<String> message = pageObj.newCamHomePage().clickApplyBtn();

		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tag " + tag);
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagAddedMsg")), "Added tag msg is not display");

		List<String> tagLst = pageObj.newCamHomePage().checkSingleTagAddedInCampaigns();
		for (int i = 0; i < tagLst.size(); i++) {
			Assert.assertEquals(tag, tagLst.get(i), "tag is not added in the campaign");
			logger.info("tag is added in the campaign");
			utils.logit("tag is added in the campaign");
		}

		logger.info("Successfully added tag " + tag + "in the campaign " + dataSet.get("campaignName"));
		utils.logPass("Successfully added tag " + tag + "in the campaign " + dataSet.get("campaignName"));

		// deselect the tag from campaigns
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		// pageObj.newCamHomePage().clickTagButton(dataSet.get("multipleCampaignTag"));
		pageObj.newCamHomePage().clickBottomTagButton();
		pageObj.newCamHomePage().deSelectTagForCampaign(tag);
		message = pageObj.newCamHomePage().clickApplyBtn();

		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagRemovedMsg")), "Removed tag msg is not display");

		logger.info("Successfully removed tags from campaign " + dataSet.get("campaignName"));
		utils.logPass("Successfully added tags from campaign " + dataSet.get("campaignName"));

		// delete the tag
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag);

		logger.info("successfully deleted both the tags");
		utils.logPass("successfully deleted both the tags");
	}

	// Anant
	@Test(description = "SQ-T4435 Verify addition of all tags in multiple campaign"
			+ "SQ-T4437 Verify addition of multiple tags in multiple campaign", groups = { "regression",
					"dailyrun" }, priority = 7)
	@Owner(name = "Rakhi Rawat")
	public void T4435_verifyAdditionOfTagsMultipleCampaign() throws InterruptedException {
		String tag1 = "NewTag1" + CreateDateTime.getTimeDateString();
		String tag2 = "NewTag2" + CreateDateTime.getTimeDateString();
		String tag3 = "NewTag3" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// delete existing tags
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteExistingTag();
		pageObj.newCamHomePage().closeOptionsDailog();

		// create tag 1
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		String msg = pageObj.newCamHomePage().createNewTag(tag1);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag1);
		utils.logit("tag is created successfully " + tag1);
		pageObj.newCamHomePage().closeOptionsDailog();

		// create tag 2
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		msg = pageObj.newCamHomePage().createNewTag(tag2);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag2);
		utils.logit("tag is created successfully " + tag2);
		pageObj.newCamHomePage().closeOptionsDailog();

		// create tag 3
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().clickCreateTagBtn();
		msg = pageObj.newCamHomePage().createNewTag(tag3);
		Assert.assertEquals(msg, "Tag created successfully", "tag is not created");
		logger.info("tag is created successfully " + tag3);
		utils.logit("tag is created successfully " + tag3);
		pageObj.newCamHomePage().closeOptionsDailog();

		// select two tags for the campaigns
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		// pageObj.newCamHomePage().clickTagButton(dataSet.get("multipleCampaignTag"));
		pageObj.newCamHomePage().clickBottomTagButton();
		pageObj.newCamHomePage().selectTagForCampaign(tag1);
		pageObj.newCamHomePage().selectTagForCampaign(tag2);
		List<String> message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagAddedMsg")), "Added tag msg is not display");

		List<String> tagLst = pageObj.newCamHomePage().checkMultipleTagAddedInCampaigns();
		for (int i = 0; i < tagLst.size(); i++) {
			int integer = Integer.parseInt(tagLst.get(i));
			if (integer > 1) {
				logger.info("tag is added in the campaign");
				utils.logit("tag is added in the campaign");
			}
		}
		logger.info("Successfully added 2 tags in the campaign ");
		utils.logPass("Successfully added 2 tags in the campaign ");

		// select all tags
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		// pageObj.newCamHomePage().clickTagButton(dataSet.get("multipleCampaignTag"));
		pageObj.newCamHomePage().clickBottomTagButton();
		pageObj.newCamHomePage().selectOrDeselectAllTag("select");

		message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagAddedMsg")), "Added tag msg is not display");

		List<String> tagLst2 = pageObj.newCamHomePage().checkMultipleTagAddedInCampaigns();
		for (int i = 0; i < tagLst2.size(); i++) {
			int integer = Integer.parseInt(tagLst2.get(i));
			if (integer > 1) {
				logger.info("tag is added in the campaign");
				utils.logit("tag is added in the campaign");
			}
		}
		logger.info("Successfully added all tags in the campaigns");
		utils.logPass("Successfully added all tags in the campaign");

		// removed the tags added to the campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectAllCampaignCheckBox();
		// pageObj.newCamHomePage().clickTagButton(dataSet.get("multipleCampaignTag"));
		pageObj.newCamHomePage().clickBottomTagButton();
		pageObj.newCamHomePage().selectOrDeselectAllTag("deselect");

		message = pageObj.newCamHomePage().clickApplyBtn();
		Assert.assertEquals(message.get(0), dataSet.get("tagUpdateMsg"), "campaign did not update with tags");
		Assert.assertTrue(message.get(1).contains(dataSet.get("tagRemovedMsg")), "Removed tag msg is not display");

		logger.info("Successfully removed tags from campaign " + dataSet.get("campaignName"));
		utils.logPass("Successfully added tags from campaign " + dataSet.get("campaignName"));

		// delete tag1
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag1);
		// pageObj.newCamHomePage().closeOptionsDailog();

		// delete tag2
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag2);
		// pageObj.newCamHomePage().closeOptionsDailog();

		// delete tag3
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickManageTagsBtn();
		pageObj.newCamHomePage().deleteTag(tag3);

		logger.info("successfully deleted both the tags");
		utils.logPass("successfully deleted both the tags");
	}

	// Anant
	@Test(description = "SQ-T4364 Post Redemption UI & Verbiage updates for GTM", groups = { "regression",
			"dailyrun" }, priority = 8)
	@Owner(name = "Rakhi Rawat")
	public void T4364_postRedemptionUI() throws InterruptedException {
		String campaignName1 = "campaign1" + CreateDateTime.getTimeDateString();
		String campaignName2 = "campaign2" + CreateDateTime.getTimeDateString();

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create pos redeemption message campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectMessagedrpValue(dataSet.get("msgDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().setCampaignName(campaignName1);
		pageObj.signupcampaignpage().clickNextBtn();

		List<String> lst1 = pageObj.signupcampaignpage().campaignTriggerLst();
		Assert.assertTrue(lst1.contains(dataSet.get("value")),
				dataSet.get("value") + " value is not present in the UI");
		Assert.assertTrue(lst1.contains(dataSet.get("value2")),
				dataSet.get("value2") + " value is not present in the UI");

		logger.info("Verified both values " + dataSet.get("value") + " and " + dataSet.get("value2")
				+ " are present in the pos redeemption msg campaign");
		utils.logit("Verified both values " + dataSet.get("value") + " and "
				+ dataSet.get("value2") + " are present in the pos redeemption msg campaign");

		// create pos redeemption offer campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("offerDrpDown"));
		pageObj.campaignspage().clickNewCampaignBtn();

		pageObj.signupcampaignpage().createWhatDetailsPostredemptionCampaign(campaignName2, dataSet.get("giftType"),
				dataSet.get("giftReason"), dataSet.get("redeemableName"));
		List<String> lst2 = pageObj.signupcampaignpage().campaignTriggerLst();
		Assert.assertTrue(lst2.contains(dataSet.get("value")),
				dataSet.get("value") + " value is not present in the UI");
		Assert.assertTrue(lst2.contains(dataSet.get("value2")),
				dataSet.get("value2") + " value is not present in the UI");

		logger.info("Verified both values " + dataSet.get("value") + " and " + dataSet.get("value2")
				+ " are present in the pos redeemption offer campaign");
		utils.logit("Verified both values " + dataSet.get("value") + " and "
				+ dataSet.get("value2") + " are present in the pos redeemption offer campaign");
		// Delete created campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().searchAndDeleteDraftCampaignClassic(campaignName1);
		pageObj.campaignspage().searchAndDeleteDraftCampaignClassic(campaignName2);

	}

	// Anant

	// @Test(description = "SQ-T4511 Verify the gift type filter in more filter
	// option on new campaign home page\n"
	// + "SQ-T4510 Verify the functionality of gift type filter in more filter
	// option on new campaign home page"
	// + "SQ-T4569 Verify URL when archive filter is selected",groups = {
	// "regression", "unstable" },priority = 14)
	public void T4510_GiftTypeFilterNewCampaignPageOld() throws InterruptedException {

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// click on filter button
		for (int i = 1; i < 10; i++) {
			pageObj.newCamHomePage().clickMoreFilterBtn();
			pageObj.newCamHomePage().checkGiftType();

			pageObj.newCamHomePage().searchFieldSidePanel("Gift type", dataSet.get("giftType" + i));
			pageObj.newCamHomePage().selectGiftTypeFromDropDown(dataSet.get("giftType" + i), 5);
			pageObj.newCamHomePage().clickSidePanelApplyBtn();
			boolean visible = pageObj.newCamHomePage().selectedFilterVisible(dataSet.get("filterType"),
					dataSet.get("giftType" + i));
			Assert.assertTrue(visible, "selected gift type " + dataSet.get("giftType" + i) + " not visible");
			logger.info("selected gift type " + dataSet.get("giftType" + i) + " visible");
			utils.logit("selected gift type " + dataSet.get("giftType" + i) + " visible");

			if (!dataSet.get("giftType" + i).equalsIgnoreCase(dataSet.get("giftType5"))) {
				pageObj.newCamHomePage().viewCampSidePanelNew();
				String val = pageObj.newCamHomePage().checkGiftTypeinCampaignSidePanel(dataSet.get("giftType" + i));
				Assert.assertEquals(val, dataSet.get("giftType" + i),
						dataSet.get("giftType" + i) + " selected filter is not working");

				logger.info(dataSet.get("giftType" + i) + " selected filter is working");
				utils.logit(dataSet.get("giftType" + i) + " selected filter is working");
				pageObj.newCamHomePage().closeCampaignSidePanel();
				pageObj.newCamHomePage().removeGiftTypeFilter();
			} else {
				pageObj.newCamHomePage().removeGiftTypeFilter();
			}
		}

		logger.info(
				"Verified able to select gift types and also verified all the campaigns are getting filter according to the filter type");
		utils.logit(
				"Verified able to select gift types and also verified all the campaigns are getting filter according to the filter type");

		// select multiple gift type
		List<String> giftTypeLst2 = new ArrayList<String>();
		giftTypeLst2.add(dataSet.get("giftType2"));
		giftTypeLst2.add(dataSet.get("giftType4"));
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(giftTypeLst2, dataSet.get("drpDownName"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String giftVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("giftTypeRE"));
		Assert.assertTrue(giftVal.contains(dataSet.get("giftType2")),
				"after selecting the gift type url did not change for the gift type " + dataSet.get("giftType2"));
		Assert.assertTrue(giftVal.contains(dataSet.get("giftType4")),
				"after selecting the gift type url did not change for the gift type " + dataSet.get("giftType4"));
		logger.info("Verfied after selecting the gift type - " + dataSet.get("giftType2") + " and "
				+ dataSet.get("giftType4") + " url change as expected");
		utils.logPass("Verfied after selecting the gift type - " + dataSet.get("giftType2")
				+ " and " + dataSet.get("giftType4") + " url change as expected");

		pageObj.newCamHomePage().removeGiftTypeFilter();
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectIncludeArchivedCampaigns();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filterVal"), dataSet.get("filter"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("IncludeArchivedCampaignRE"));
		Assert.assertEquals(itemVal, "true", "on selecting the include archvied campaign url did not change");
		logger.info("Verify on selecting the include archvied campaign url change");
		utils.logPass("Verify on selecting the include archvied campaign url change");

		String val = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("statusRE"));
		Assert.assertEquals(val, dataSet.get("filterVal").toLowerCase(),
				"when archived filter is selected url is not changed");
		logger.info("Verify when archived filter is selected url is changed");
		utils.logPass("Verify when archived filter is selected url is changed");
	}

	// Anant
	@Test(description = "SQ-T4549 Verify gifting from duplicate post checkin offer campaign", groups = {
			"unstable" }, priority = 9)
	@Owner(name = "Rakhi Rawat")
	public void T4549_verifyGiftingFromDuplicateExpiredPosCheckinCampaign() throws InterruptedException {
		String duplicateCampaign = "AutmationPosCheckin" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// search the campaign
		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("campaignName"));

		// now create a duplicate campaign
		pageObj.campaignspage().selectCPPOptions(dataSet.get("optionValue"));
		pageObj.campaignspage().selectOptionFromEllipsisee("Duplicate");

		// set the name for duplicate campaign
		pageObj.campaignspage().createDuplicatePosCampaign(duplicateCampaign);

		// create a user using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		logger.info("created user --" + userEmail);
		utils.logit("created user --" + userEmail);

		// do a checkin on the user
		String key = CreateDateTime.getTimeDateString();
		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		// check if the gifting happen
		boolean campaignNameStatus = pageObj.guestTimelinePage().verifyIsCampaignExistOnTimeLine(duplicateCampaign);
		Assert.assertTrue(campaignNameStatus, duplicateCampaign + " Campaign name did not matched");
		logger.info(duplicateCampaign + " campaign is visible on user timeline page");
		utils.logPass(duplicateCampaign + " campaign is visible on user timeline page");

		String pushNotificationStatus = pageObj.guestTimelinePage().getPushNotificationForCampaign(duplicateCampaign);
		Assert.assertTrue(pushNotificationStatus.contains(dataSet.get("pn")), "Push notification did not displayed...");
		logger.info("Push notification is visible on user timeline page");
		utils.logPass("Push notification is visible on user timeline page");

		Boolean redeemableStatus = pageObj.guestTimelinePage().redeemableVisible(duplicateCampaign,
				dataSet.get("redeemableName"));
		Assert.assertTrue(redeemableStatus, "redeemable is not visible on the timeline");
		logger.info("verfied redeemable is visible on the timeline");
		utils.logPass("verfied redeemable is visible on the timeline");

		// search and delete campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(duplicateCampaign);
		pageObj.newCamHomePage().deleteCampaign(duplicateCampaign);
	}

	// Anant
	@Test(description = "SQ-T4551 Verify gifting from duplicate post checkin message campaign", priority = 10)
	@Owner(name = "Rakhi Rawat")
	public void T4551_verifyGiftingFromDuplicateExpiredPosCheckinMsgCampaign() throws InterruptedException {
		String duplicateCampaign = "AutmationPosCheckinMsg" + CreateDateTime.getTimeDateString();
		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickSwitchToClassicBtn();

		// search the campaign
		pageObj.campaignspage().searchAndSelectCamapign(dataSet.get("campaignName"));

		// now create a duplicate campaign
		pageObj.campaignspage().selectCPPOptions(dataSet.get("optionValue"));
		pageObj.campaignspage().selectOptionFromEllipsisee("Duplicate");

		// set the name for duplicate campaign
		pageObj.campaignspage().createDuplicatePosCampaign(duplicateCampaign);

		// create a user using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		logger.info("created user --" + userEmail);
		utils.logit("created user --" + userEmail);

		// do a checkin on the user
		String key = "567324" + CreateDateTime.getTimeDateString();
		String txn = "345123" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T10:50:00+05:30";
		Response resp = pageObj.endpoints().posCheckin(date, userEmail, key, txn, dataSet.get("locationkey"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, resp.getStatusCode(), "Status code 200 did not matched for post chekin api");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		// check if the gifting happen
		Boolean val = pageObj.guestTimelinePage().checkPNForCampaign(duplicateCampaign, dataSet.get("pn"));
		Assert.assertTrue(val, "campaign did not happen to the user");
		logger.info("verfied duplicate pos checkin msg campaign trigerred for the user");
		utils.logPass("verfied duplicate pos checkin msg campaign trigerred for the user");

		// search and delete campaign
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();
		pageObj.newCamHomePage().searchCampaign(duplicateCampaign);
		pageObj.newCamHomePage().deleteCampaign(duplicateCampaign);

	}

	// Anant
	@Test(description = "SQ-T4566 Verify URL when switching tab from All to Other/ Mass to Automation"
			+ "SQ-T4574 Verify URL when Segment filter is selected", groups = { "regression", "unstable",
					"dailyrun" }, priority = 11)
	@Owner(name = "Rakhi Rawat")
	public void T4566_verifyURLWhenSwitchingTab() throws InterruptedException {
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickNewCamHomePageBtn();

		for (int i = 1; i < 4; i++) {
			pageObj.newCamHomePage().switchTab(dataSet.get("tab" + i));
			String urlValue = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("categoryRE"));
			Assert.assertEquals(urlValue, dataSet.get("tab" + i), "On switching the tab the url value does not change");
			logger.info("Verfied on going to the tab --" + dataSet.get("tab" + i) + " URL contains the tab value");
			utils.logPass("Verfied on going to the tab --" + dataSet.get("tab" + i) + " URL contains the tab value");
		}
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().changePage();
		String pageVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("pageRE"));
		Assert.assertEquals(pageVal, "2", "on changing the page url did not change");
		logger.info("verify on changing the page, url contains the page number");
		utils.logPass("verify on changing the page, url contains the page number");

		// no of item in a page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectNoOfItem(dataSet.get("noOfItem"));
		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("itemRE"));
		Assert.assertEquals(itemVal, dataSet.get("noOfItem"), "on changing the page url did not change");
		logger.info("verify on changing the page, url contains the page number");
		utils.logPass("verify on changing the page, url contains the page number");

		// segment filter
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filterVal"), dataSet.get("filter"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String segmentVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("segmentRE"));
		segmentVal = segmentVal.replace("+", " ");
		Assert.assertEquals(segmentVal, dataSet.get("filterVal").toLowerCase(),
				"on selecting the segment filter url did not change");
		logger.info("verify on selecting the segment filter url change");
		utils.logPass("verify on selecting the segment filter url change");

		// apply sort by filter
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().selectSortByFilterValue(dataSet.get("sortByFilterVal"));
		String sortVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("sortByRE"));
		Assert.assertEquals(sortVal, dataSet.get("expectedVal"), "on selecting the sort by filter url did not change");
		logger.info("verified on selecting the sort by filter url change");
		utils.logPass("verified on selecting sort by filter url change");

		// campaign type filter
		List<String> campaignTypeLst = new ArrayList<String>();
		campaignTypeLst.add(dataSet.get("campType1"));
		campaignTypeLst.add(dataSet.get("campType2"));
		campaignTypeLst.add(dataSet.get("campType3"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(campaignTypeLst, dataSet.get("campTypeFilter"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String campType = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("campTypeRE"));
		Assert.assertTrue(campType.contains(dataSet.get("campType1").toLowerCase()),
				"when selected the campaign type -- " + dataSet.get("campType1") + " url did not change");
		Assert.assertTrue(campType.contains(dataSet.get("campType2").toLowerCase()),
				"when selected the campaign type -- " + dataSet.get("campType2") + " url did not change");
		Assert.assertTrue(campType.contains(dataSet.get("campType3").toLowerCase()),
				"when selected the campaign type -- " + dataSet.get("campType3") + " url did not change");
		logger.info("Verified when the different campaigns types is selected url change");
		utils.logPass("Verified when the different campaigns types is selected url change");

		// status filter
		List<String> statusTypeLst = new ArrayList<String>();
		statusTypeLst.add(dataSet.get("statusType1"));
		statusTypeLst.add(dataSet.get("statusType2"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectMultipleDrpDownValFromSidePanel(statusTypeLst, dataSet.get("statusFilter"));
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		String statusVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("statusRE"));
		Assert.assertTrue(statusVal.contains(dataSet.get("statusType1").toLowerCase()),
				"when selected the status -- " + dataSet.get("statusType1") + " url didnot change");
		Assert.assertTrue(statusVal.contains(dataSet.get("statusType2").toLowerCase()),
				"when selected the status -- " + dataSet.get("statusTypew") + " url didnot change");
		logger.info("Verified when the different campaign status is selected url change");
		utils.logPass("Verified when the different campaign status is selected url change");
	}

	// shashank
	@Test(description = "SQ-T4511 Verify the gift type filter in more filter option on new campaign home page\n"
			+ "SQ-T4510 Verify the functionality of gift type filter in more filter option on new campaign home page"
			+ "SQ-T4569 Verify URL when archive filter is selected", groups = { "regression", "unstable",
					"dailyrun" }, priority = 12)
	@Owner(name = "Shashank Sharma")
	public void T4510_GiftTypeFilterNewCampaignPage() throws InterruptedException {
		List<String> listOfFilterValuesExpected = Arrays.asList("Currency", "Coupon", "$ off", "Redeemable", "Points",
				"% off");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// wait till all campaign loaded om new CHP page
		pageObj.newCamHomePage().waitTillCampCountIsDisplaying();

		// click on more filter button
		pageObj.newCamHomePage().clickMoreFilterBtn();

		// fetching all value from filter drop down
		List<String> listOfFilterValues = pageObj.newCamHomePage().getFilterDropDownvalues("Gift type");
		listOfFilterValues.remove("No gift");
		// closing the filter side panel window
		pageObj.newCamHomePage().closeFilterSidePanel();
		Assert.assertTrue(listOfFilterValues.containsAll(listOfFilterValuesExpected));
		logger.info("Verified that all expected gift type available in UI");
		utils.logPass("Verified that all expected gift type available in UI");
		for (String filterValueStr : listOfFilterValuesExpected) {
			// closing the filter side panel window
			pageObj.newCamHomePage().waitTillCampCountIsDisplaying();
			// click on more filter button
			pageObj.newCamHomePage().clickMoreFilterBtn();

			// select filter value from filter
			logger.info(filterValueStr + " value is selected for the filter :- GiftType");
			pageObj.newCamHomePage().selectFilterDropDownValue("Gift type", filterValueStr);
			// clicked ont apply button
			pageObj.newCamHomePage().clickSidePanelApplyBtn();
			// verifying that campaigns are appearing after apply filter
			boolean visible = pageObj.newCamHomePage().selectedFilterVisible("Gift type", filterValueStr);
			Assert.assertTrue(visible, "selected gift type " + filterValueStr + " not visible");
			logger.info("selected gift type " + filterValueStr + " visible");
			utils.logit("selected gift type " + filterValueStr + " visible");

			// verifying applied filter is appearing in after clicking the campaign
			String actualFilterVaue = pageObj.newCamHomePage().checkGiftTypeinCampaignSidePanel(filterValueStr);
			Assert.assertEquals(actualFilterVaue, filterValueStr,
					actualFilterVaue + " expected filter value not matched with expected - " + filterValueStr);

			// close campaign side panel
			// pageObj.newCamHomePage().closeCampaignSidePanel();
			// remove applied filter
			pageObj.newCamHomePage().removeGiftTypeFilter();
		}

	}

	// shashank
	@Test(description = "SQ-T4511 Verify the gift type filter in more filter option on new campaign home page\n"
			+ "SQ-T4510 Verify the functionality of gift type filter in more filter option on new campaign home page"
			+ "SQ-T4569 Verify URL when archive filter is selected", groups = { "regression", "unstable",
					"dailyrun" }, priority = 13)
	@Owner(name = "Shashank Sharma")
	public void T4510_GiftTypeFilterNewCampaignPagePartTwo() throws InterruptedException {
		List<String> listOfFilterValuesExpected = Arrays.asList("Currency", "Coupon");

		// Login to instance
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 */

		// Instance select business
		// pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// goto new campaign page
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		// wait till all campaign load
		pageObj.newCamHomePage().waitTillCampCountIsDisplaying();
		// clicked on more filter button
		pageObj.newCamHomePage().clickMoreFilterBtn();

		// fetching all value from gift type filter
		List<String> listOfFilterValues = pageObj.newCamHomePage().getFilterDropDownvalues("Gift type");
		listOfFilterValues.remove("No gift");
		// close filter side panel
		pageObj.newCamHomePage().closeFilterSidePanel();
		Assert.assertTrue(listOfFilterValues.containsAll(listOfFilterValuesExpected));
		logger.info("Verified that all expected gift type available in UI");
		utils.logPass("Verified that all expected gift type available in UI");
		// wait till all campaign load
		pageObj.newCamHomePage().waitTillCampCountIsDisplaying();
		// clicked on more filter button
		pageObj.newCamHomePage().clickMoreFilterBtn();
		// select value from filter
		pageObj.newCamHomePage().selectFilterDropDownValue("Gift type", listOfFilterValuesExpected.get(0));
		pageObj.newCamHomePage().selectFilterDropDownValue("Gift type", listOfFilterValuesExpected.get(1));
		logger.info(listOfFilterValuesExpected.get(0) + " value is selected for the filter :- GiftType");
		logger.info(listOfFilterValuesExpected.get(1) + " value is selected for the filter :- GiftType");

		// click on filter side panel apply button
		pageObj.newCamHomePage().clickSidePanelApplyBtn();

		// getting filter value from URL
		String giftVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("giftTypeRE"));
		Assert.assertTrue(giftVal.contains(listOfFilterValuesExpected.get(0)),
				"after selecting the gift type url did not change for the gift type " + dataSet.get("giftType2"));
		Assert.assertTrue(giftVal.contains(listOfFilterValuesExpected.get(1)),
				"after selecting the gift type url did not change for the gift type " + dataSet.get("giftType4"));
		logger.info("Verfied after selecting the gift type - " + dataSet.get("giftType2") + " and "
				+ dataSet.get("giftType4") + " url change as expected");
		utils.logPass("Verfied after selecting the gift type - " + dataSet.get("giftType2")
				+ " and " + dataSet.get("giftType4") + " url change as expected");

		// remove filter
		pageObj.newCamHomePage().removeGiftTypeFilter();
		// pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.newCamHomePage().clickMoreFilterBtn();
		pageObj.newCamHomePage().selectIncludeArchivedCampaigns();
		pageObj.newCamHomePage().selectDrpDownValFromSidePanel(dataSet.get("filterVal"), dataSet.get("filter"));
		// click on side panel apply button
		pageObj.newCamHomePage().clickSidePanelApplyBtn();
		// get value from URL
		String itemVal = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("IncludeArchivedCampaignRE"));
		Assert.assertEquals(itemVal, "true", "on selecting the include archvied campaign url did not change");
		logger.info("Verify on selecting the include archvied campaign url change");
		utils.logPass("Verify on selecting the include archvied campaign url change");

		String val = pageObj.newCamHomePage().getValueFromUrlUsingRE(dataSet.get("statusRE"));
		Assert.assertEquals(val, dataSet.get("filterVal").toLowerCase(),
				"when archived filter is selected url is not changed");
		logger.info("Verify when archived filter is selected url is changed");
		utils.logPass("Verify when archived filter is selected url is changed");

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
