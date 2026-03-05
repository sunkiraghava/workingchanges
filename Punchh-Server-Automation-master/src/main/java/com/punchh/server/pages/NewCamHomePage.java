package com.punchh.server.pages;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.Color;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class NewCamHomePage {

	static Logger logger = LogManager.getLogger(MenuPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public NewCamHomePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	// clickNewCamHomePageBtn - click to switch to New campaign home page
	// clickSwitchToClassicBtn - click to switch to classic campaign page
	// getCampaignStatus - return the campaign status i.e. draft, active, inactive

	// click to switch to New campaign home page
	public void clickNewCamHomePageBtn() {
		try {
			utils.implicitWait(1);
			WebElement ele = utils.getLocator("newCamHomePage.newCamPageBtn");
			if (ele.isDisplayed()) {
				utils.getLocator("newCamHomePage.newCamPageBtn").click();
				utils.waitTillPagePaceDone();
				campaignAdvertiseBlock();
			}
		} catch (Exception e) {

		}
		utils.implicitWait(60);
	}

	// click to switch to classic campaign page
	public void clickSwitchToClassicBtn() {
		try {
			utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.switchToClassicBtn"));
//		utils.getLocator("newCamHomePage.switchToClassicBtn").click();
//			utils.waitTillPagePaceDone();
		} catch (Exception e) {

		}
	}

	public void selectCampaignsTab(String tabName) {
		WebElement campaignsTab = driver.findElement(By.xpath("//span[text()='" + tabName + "']"));
		campaignsTab.click();
		TestListeners.extentTest.get().pass("campaigns tab selected :" + tabName);
	}

	public String moveToBalckoutdates() {
		utils.getLocator("newCamHomePage.massTabOverflowBtn").click();
		utils.getLocator("newCamHomePage.blackOutDateBtn").click();
		TestListeners.extentTest.get().pass("clicked on blackout dates button in overflow icon");
		utils.switchToNewOpenedWindow();
		String val = utils.getLocator("campaignsBetaPage.blackoutDates").getText();
		return val;
	}

	public void searchCampaign(String camName) {
		utils.waitTillNewCamsTableAppear();
		utils.getLocator("newCamHomePage.searchCamBox").clear();
		utils.getLocator("newCamHomePage.searchCamBox").sendKeys(camName);
		logger.info("campaign searched on new cam home page");
		TestListeners.extentTest.get().pass("campaign searched on new cam home page");
		utils.longWaitInSeconds(1);
	}

	// return the campaign status i.e. draft, active, inactive
	public String getCampaignStatus() {
		utils.waitTillNewCamsTableAppear();
		String campTableHeading = utils.getLocatorValue("newCamHomePage.campTableHeading");
		List<WebElement> headingCloumn = driver.findElements(By.xpath(campTableHeading));
		String statusTabIndex = Integer.toString(headingCloumn.size() + 1);
		String statusTabIndexValue = utils.getLocatorValue("newCamHomePage.statusTabIndexValue")
				.replace("$statusTabIndex", statusTabIndex);
		WebElement status = driver.findElement(By.xpath(statusTabIndexValue));
		String val = status.getText();
		return val;
	}

	public String getAnyCampaignStatusWithPoolingNCHP(String camName, String status) {
		String statusVal = "";
		int attempts = 0;
		while (attempts <= 25) {
			try {
				utils.waitTillNewCamsTableAppear();
				utils.getLocator("newCamHomePage.searchCamBox").clear();
				utils.getLocator("newCamHomePage.searchCamBox").sendKeys(camName);
				utils.longWaitInMiliSeconds(500);
				WebElement camStatus = driver.findElement(
						By.xpath("//td//div//a[text()='" + camName + "']/../../following-sibling::td[5]//span"));
				statusVal = camStatus.getText();
				if (statusVal.equalsIgnoreCase(status)) {
					logger.info("Campaign status is :" + statusVal);
					TestListeners.extentTest.get().pass("Campaign status is :" + statusVal);
					break;
				} else {
					TestListeners.extentTest.get().info("Campaign status is not processed : " + attempts);
					utils.refreshPage();
				}
			} catch (Exception e) {
				TestListeners.extentTest.get().info("Campaign status is not processed : " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		return statusVal;
	}

	public String getCampaignAnyStatusNoPooling(String camName) {
		String statusVal = null;
		try {
			utils.waitTillNewCamsTableAppear();
			utils.getLocator("newCamHomePage.searchCamBox").clear();
			utils.getLocator("newCamHomePage.searchCamBox").sendKeys(camName);
			utils.longWaitInSeconds(1);
			WebElement status = driver
					.findElement(By.xpath("//td[div[p[text()='" + camName + "']]]/following-sibling::td[5]//span"));
			statusVal = status.getText();
		} catch (Exception e) {
			TestListeners.extentTest.get().info("Campaign status is not appeared or Campaign not found");
		}
		logger.info("Campaign status is :" + statusVal);
		TestListeners.extentTest.get().pass("Campaign status is :" + statusVal);
		return statusVal;
	}

	public String deactivateCampaign() {

		WebElement dotsIcon = utils.getLocator("newCamHomePage.dotsIcon");
		utils.waitTillElementToBeClickable(dotsIcon);
		dotsIcon.click();
		logger.info("clicked dots option icon");
		TestListeners.extentTest.get().pass("clicked dots option icon");
		utils.longWaitInSeconds(2);
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.dotsIconDeactivateOption"));
		utils.getLocator("newCamHomePage.dotsIconDeactivateOption").click();
		logger.info("clicked deactivate option");
		TestListeners.extentTest.get().pass("clicked deactivate option");
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.yesDeactivateBtn"));
		utils.getLocator("newCamHomePage.yesDeactivateBtn").click();
		// utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(2);
		utils.waitTillNewCamsTableAppear();
		logger.info("successfully deactivate the campaign");
		TestListeners.extentTest.get().info("successfully deactivate the campaign");
		String status = utils.getLocator("newCamHomePage.statusInactive").getText();
		return status;
	}

	public String activateCampaign() {
		WebElement dotsIcon = utils.getLocator("newCamHomePage.dotsIcon");
		utils.waitTillElementToBeClickable(dotsIcon);
		dotsIcon.click();

		utils.longWaitInSeconds(1);
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.dotsIconActivateOption"));
		utils.getLocator("newCamHomePage.dotsIconActivateOption").click();
		utils.longWaitInSeconds(2);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.yesActivateBtn"), "yes activate button");
		utils.getLocator("newCamHomePage.yesActivateBtn").click();
		// utils.waitTillPagePaceDone();
		utils.waitTillNewCamsTableAppear();
		logger.info("successfully activate the campaign");
		TestListeners.extentTest.get().info("successfully activate the campaign");

		String status = utils.getLocator("newCamHomePage.statusActive").getText();
		return status;
	}

	public String archiveCampaign() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon"), "dotsIcon");
		utils.getLocator("newCamHomePage.dotsIcon").click();
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.dotsIconArchiveOption"));
		utils.longWaitInSeconds(2);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIconArchiveOption"), "archive option");
		utils.getLocator("newCamHomePage.dotsIconArchiveOption").click();

		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.campaignArchiveMsg"), "archive status");
		String status = utils.getLocator("newCamHomePage.campaignArchiveMsg").getText();

		return status;
	}

	public boolean checkAchivalpresence(boolean expectedValue) {
		boolean status = false;
		int attempts = 0;

		while (attempts < 5) {
			try {
				utils.refreshPage();
				utils.waitTillNewCamsTableAppear();
				utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon"), "dotsIcon");
				utils.getLocator("newCamHomePage.dotsIcon").click();
				utils.implicitWait(2);
				List<WebElement> elements = utils.getLocatorList("newCamHomePage.dotsIconArchiveOption");
				status = !elements.isEmpty();
				// status = utils.checkElementPresent(ele);

				if (status == expectedValue) {
					break; // Exit loop immediately if found
				}
			} catch (Exception e) {
				logger.info("Archive option is not present in the dots icon attempt " + attempts);
				TestListeners.extentTest.get()
						.info("Archive option is not present in the dots icon attempt " + attempts);
			}
			attempts++;
		}
		return status;
	}

	/*
	 * public boolean checkAchivalpresence() { boolean status = false;
	 * utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon")
	 * , "dotsIcon"); utils.getLocator("newCamHomePage.dotsIcon").click(); try {
	 * utils.implicitWait(2); WebElement ele =
	 * utils.getLocator("newCamHomePage.dotsIconArchiveOption"); status =
	 * utils.checkElementPresent(ele); } catch (Exception e) {
	 * 
	 * } return status;
	 * 
	 * }
	 */

	public String stopProcessingCampaign() {
		// utils.refreshPage();
		// utils.waitTillPagePaceDone();
		WebElement dotsIcon = utils.getLocator("newCamHomePage.dotsIcon");
		utils.waitTillElementToBeClickable(dotsIcon);
		dotsIcon.click();
		utils.longWaitInSeconds(1);
		utils.getLocator("newCamHomePage.dotsIconStopProcessingOption").click();
		utils.getLocator("newCamHomePage.yesStopProcessingBtn").click();
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(5);
		String status = utils.getLocator("newCamHomePage.statusStopped").getText();
		return status;
	}

	public void deleteCampaign(String campaignName) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon"), "dotsIcon");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.dotsIcon"));
		// utils.getLocator("newCamHomePage.dotsIcon").click();

		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIconDeleteOption"), "delete option");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.dotsIconDeleteOption"));
		// utils.getLocator("newCamHomePage.dotsIconDeleteOption").click();

		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.yesDeleteBtn"), "yes delete button");
		utils.getLocator("newCamHomePage.yesDeleteBtn").click();
		utils.waitTillPagePaceDone();
		// utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.campaignDeleteMsg"),
		// "delete status");
		int size = utils.getLocatorList("newCamHomePage.campaignDeleteMsg").size();
		if (size == 1) {
			Assert.assertEquals(size, 1, "error while deleting the campaign");
			logger.info("Searched campaign deleted successfuly");
			TestListeners.extentTest.get().pass("Searched campaign deleted successfuly");
		} else {
			logger.info("Error while deleting the campaign");
			TestListeners.extentTest.get().pass("Error while deleting the campaign");
		}
	}

	public void selectCampaignOption(String optionName) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon"), "dotsIcon");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.dotsIcon"));

		WebElement ele = driver.findElement(By.xpath("//*[@title='" + optionName + "']"));
		utils.waitTillElementToBeClickable(ele);
		utils.clickByJSExecutor(driver, ele);
		// utils.waitTillPagePaceDone();
		logger.info("clicked campaign option :" + optionName);
		TestListeners.extentTest.get().pass("clicked campaign option :" + optionName);
	}

	public void selectStickyBarOptions(String optionName) {
		WebElement ele = driver.findElement(By.xpath("//span[text()='" + optionName + "']/parent::button"));
		utils.waitTillElementToBeClickable(ele);
		utils.clickByJSExecutor(driver, ele);
		// utils.waitTillPagePaceDone();
		logger.info("clicked campaign option :" + optionName);
		TestListeners.extentTest.get().pass("clicked campaign option :" + optionName);
	}

	public void closeOptionsDailog() {
		utils.longWaitInSeconds(5);
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.camOptionsClosebtn"));
		utils.getLocator("newCamHomePage.camOptionsClosebtn").click();
		logger.info("campaign option dailog box closed successfully");
	}

	public String getExportCodeSuccessMsg() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.exportCodeMsg"),
				"Export code success message");
		String val = utils.getLocator("newCamHomePage.exportCodeMsg").getText();
		logger.info("campaign option dailog box closed successfully");
		return val;
	}

	public String getNewPageTitleClassic() {
		utils.waitTillCompletePageLoad();
		String val = utils.getLocator("newCamHomePage.anyPageTitle").getText();
		logger.info("new page title is :" + val);
		return val;
	}

	public String getNewPageTitleCPP() {
		utils.waitTillCompletePageLoad();
		String val = utils.getLocator("newCamHomePage.cppSummaryPagetitle").getText();
		logger.info("new page title is :" + val);
		return val;
	}

	public void navigateToBackPage() {
		driver.navigate().back();
		logger.info("Navigating back page...");
		utils.waitTillCompletePageLoad();
	}

	public void createMassCampaignCHP(String camType, String includeAnOffer) {
		// utils.waitTillPagePaceDone();
		utils.getLocator("newCamHomePage.createCampaignbtn").click();
		WebElement CampaignType = driver.findElement(By.xpath("//span[normalize-space()='" + camType + "']"));
		CampaignType.click();
		utils.getLocator("newCamHomePage.nextBtn").click();
		utils.longWaitInSeconds(2);
		// Include offer
//		WebElement IncludeOfferInCampaign = driver
//				.findElement(By.xpath("//span[contains(text(),'" + includeAnOffer + "')]/.."));
		String includeOfferYesNoXpath = utils.getLocatorValue("newCamHomePage.includeOfferYesNo").replace("$choice",
				includeAnOffer);

		WebElement IncludeOfferInCampaign = driver.findElement(By.xpath(includeOfferYesNoXpath));
		IncludeOfferInCampaign.click();
		utils.getLocator("newCamHomePage.createBtn").click();
		TestListeners.extentTest.get().pass("Mass Offer Campaign from NCHP Name and Type Entered");
		utils.waitTillPagePaceDone();

	}

	public void createAutomationsCampaignCHP(String camCategory, String camType, String includeAnOffer) {
		// clickNewCamHomePageBtn();
		utils.getLocator("newCamHomePage.createCampaignbtn").click();
		WebElement campaignCategory = driver.findElement(
				By.xpath("//div[contains(@class,'radio-button')]//span[normalize-space()='" + camCategory + "']"));
		campaignCategory.click();
		WebElement nextBtn = utils.getLocator("newCamHomePage.nextBtn");
		nextBtn.click();

		// set campaign type
		WebElement campaignType = driver.findElement(By.xpath("//span[normalize-space()='" + camType + "']"));
		campaignType.click();
		WebElement CamTypeNextBtn = utils.getLocator("newCamHomePage.CamTypeNextBtn");
		CamTypeNextBtn.click();

		// set offer Yes or No
		WebElement IncludeOfferInCampaign = driver
				.findElement(By.xpath("//span[contains(text(),'" + includeAnOffer + "')]/.."));
		IncludeOfferInCampaign.click();
		utils.getLocator("newCamHomePage.createBtn").click();
		TestListeners.extentTest.get().pass("Automations Post Checkin Campaign from NCHP Name and Type Entered");
		utils.waitTillPagePaceDone();

	}

	public void createOtherCampaignCHP(String camCategory, String camType) {
		// clickNewCamHomePageBtn();
		utils.getLocator("newCamHomePage.createCampaignbtn").click();
		WebElement campaignCategory = driver.findElement(
				By.xpath("//div[contains(@class,'radio-button')]//span[normalize-space()='" + camCategory + "']"));
		campaignCategory.click();
		WebElement nextBtn = utils.getLocator("newCamHomePage.nextBtn");
		nextBtn.click();
		// set campaign type
		WebElement campaignType = driver.findElement(By.xpath("//span[normalize-space()='" + camType + "']"));
		// utils.scrollToElement(driver, campaignType);
		// campaignType.click();
		utils.StaleElementclick(driver, campaignType);
		utils.getLocator("newCamHomePage.createBtn").click();
		utils.waitTillPagePaceDone();

	}

	public String createTags(String tagName) {
		utils.getLocator("newCamHomePage.manageTagsBtn").click();
		utils.longWaitInSeconds(2);
		utils.getLocator("newCamHomePage.createTagsBtn").click();
		utils.getLocator("newCamHomePage.tagNameBox").clear();
		utils.getLocator("newCamHomePage.tagNameBox")
				.sendKeys("TaggNameeMoreeThannFiftyyCharacterssLongg_" + CreateDateTime.getTimeDateString());
		utils.longWaitInSeconds(1);
		utils.getLocator("newCamHomePage.createBtn").click();
		String val = utils.getLocator("newCamHomePage.tagNameErrorMsg").getText();
		utils.getLocator("newCamHomePage.tagNameBox").clear();
		utils.getLocator("newCamHomePage.tagNameBox").sendKeys(tagName);
		utils.longWaitInSeconds(3);
		utils.waitTillPagePaceDone();
		utils.getLocator("newCamHomePage.createBtn").click();
		return val;

	}

	public String deleteTag(String tagName) {
		// search and delete created tag
		utils.longWaitInSeconds(2);
		utils.waitTillPagePaceDone();
		utils.getLocator("newCamHomePage.searchTagsBox").clear();
		utils.getLocator("newCamHomePage.searchTagsBox").sendKeys(tagName);
		utils.getLocator("newCamHomePage.manageTagsdotsIcon").click();
		utils.getLocator("newCamHomePage.dotsIconDeleteOption").click();
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.yesDeleteBtn"));
		utils.getLocator("newCamHomePage.yesDeleteBtn").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.deleteTagMsg"), "Toast msg");
		String val = utils.getLocator("newCamHomePage.deleteTagMsg").getText();
		// close dailog box
		WebElement closeBtn = utils.getLocator("newCamHomePage.deleteTagDialogueBox");
		utils.clickWithRetry(closeBtn, 20, 500);
		return val;

	}

	public void clickManageTagsBtn() {
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.manageTagsBtn"));
		logger.info("clicked on the manage tags button");
		TestListeners.extentTest.get().info("clicked on the manage tags button");

	}

	public void closeMangeTagFrame() {
		try {
			utils.waitTillElementDisappear(utils.getLocator("newCamHomePage.tagSuccessMsg"));
		} catch (Exception e) {
			logger.info("success msg not appeared");
		}
		utils.longWaitInSeconds(1);
		utils.getLocator("newCamHomePage.manageTagsCloseBtn").click();
		utils.longWaitInSeconds(1);
	}

	public void clickCreateTagBtn() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.createTagsBtn"), "create tag");
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.createTagsBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.createTagsBtn"));
		logger.info("clicked on the create tags button");
		TestListeners.extentTest.get().info("clicked on the create tags button");
	}

	public String createNewTag(String tagName) {
		String message = "";
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.tagName"), "tag name field");
		utils.getLocator("newCamHomePage.tagName").sendKeys(tagName);
		utils.longWaitInSeconds(1);
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.newCreateButton"));
		utils.clickUsingActionsClass(utils.getLocator("newCamHomePage.newCreateButton"));
		utils.implicitWait(1);

		String attrValue = null;
		try {
			WebElement element = utils.getLocator("newCamHomePage.tagName");
			if (element != null) {
				attrValue = element.getAttribute("class");
			}
		} catch (Exception e) {
			// element not found or detached from DOM
			attrValue = "";
		}

		if (attrValue != null && attrValue.contains("error")) {
			message = utils.getLocator("newCamHomePage.tagNameError").getText();
		} else {
			utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.tagSuccessMsg"), "success msg for tag");
			message = utils.getLocator("newCamHomePage.tagSuccessMsg").getText();
		}

		return message;
	}

	public void renameTag(String tagName, String newName) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.tagSearch"), "");
		utils.getLocator("newCamHomePage.tagSearch").sendKeys(tagName);
		String xpath = utils.getLocatorValue("newCamHomePage.threeDotsOfTags").replace("${tagName}", tagName);
		utils.longwait(1500);
		utils.getLocator("newCamHomePage.manageTagsdotsIcon").click();
		// utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));

		// utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
//		((JavascriptExecutor) driver).executeScript("arguments[0].focus();", driver.findElement(By.xpath(xpath)));
//		((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('click'));",
//				driver.findElement(By.xpath(xpath)));

		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.tagRename"), "tag rename button");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.tagRename"));
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.renameTagInputBox"), "rename Tag input box");
		utils.getLocator("newCamHomePage.renameTagInputBox").clear();

		utils.getLocator("newCamHomePage.renameTagInputBox").sendKeys(newName);
		utils.getLocator("newCamHomePage.tagSearch").sendKeys(Keys.TAB);
		// utils.getLocator("newCamHomePage.tagSearch").sendKeys(Keys.TAB);

		utils.longwait(1000);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.renameSaveBtn"), "save btn");
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.renameSaveBtn"));
		utils.getLocator("newCamHomePage.renameSaveBtn").click();
	}

	public String tagRenameErrorMsg() {
		String errorText = "";
		utils.longwait(1000);
		errorText = utils.getLocator("newCamHomePage.tagNameErrorMsg").getText();
		logger.info("error msg is --" + errorText);
		TestListeners.extentTest.get().info("error msg is --" + errorText);
		return errorText;
	}

	public void tagRenameSuccessMsg(String tagName, String newName) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.tagSearch"), "tag search field");
		utils.getLocator("newCamHomePage.tagSearch").clear();
		utils.getLocator("newCamHomePage.tagSearch").sendKeys(newName);
		utils.getLocator("newCamHomePage.tagSearch").sendKeys(Keys.TAB);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.verifyRenameTag"), "verify renaming tag");
		String displayTagName = utils.getLocator("newCamHomePage.verifyRenameTag").getText();
		Assert.assertEquals(displayTagName, newName, "tag name didnot change when trying to rename");
		logger.info("verified tag name - " + tagName + " rename to -" + newName);
		TestListeners.extentTest.get().pass("verified tag name - " + tagName + " rename to -" + newName);
	}

	public void selectAllCampaignCheckBox() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.checkBoxForSingleCampaign"), "check box");
		utils.getLocator("newCamHomePage.checkBoxForSingleCampaign").click();
		logger.info("selected all the campaigns present in the page");
		TestListeners.extentTest.get().info("selected all the campaigns present in the page");
	}

	public void unSelectAllCampaignCheckBox() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.checkBoxAllUnselect"), "check box");
		utils.getLocator("newCamHomePage.checkBoxAllUnselect").click();
		logger.info("selected all the campaigns present in the page");
		TestListeners.extentTest.get().info("selected all the campaigns present in the page");
	}

	public void clickTagButton(String campaignNo) {
		String xpath = utils.getLocatorValue("newCamHomePage.tagButtton");
//		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(5);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		logger.info("click on the tag button after selecting the campaigns");
		TestListeners.extentTest.get().info("click on the tag button after selecting the campaigns");
	}

	public void clickBottomTagButton() {
		WebElement bottomTagButton = utils.getLocator("newCamHomePage.bottomTagButton");
		bottomTagButton.click();
		logger.info("click on the bottom tag button");
		TestListeners.extentTest.get().info("click on the bottom tag button");
		utils.longWaitInMiliSeconds(500);
	}

	public void selectTagForCampaign(String tagName) {
		searchTagInTagBox(tagName);
		String xpath = utils.getLocatorValue("newCamHomePage.selectTagForCampaign").replace("${tagName}", tagName);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "check box");
		String text = driver.findElement(By.xpath(xpath)).getText();
		if (!text.equalsIgnoreCase("check_box")) {
			utils.longWaitInSeconds(1);
//		utils.clickWithActions(driver.findElement(By.xpath(xpath)));
			driver.findElement(By.xpath(xpath)).click();
			utils.longWaitInSeconds(3);
			text = "";
			text = driver.findElement(By.xpath(xpath)).getText();
			Assert.assertEquals(text, "check_box", "failed to select the tag -- " + tagName);
			logger.info("selected the tag " + tagName);
			TestListeners.extentTest.get().info("selected the tag " + tagName);
		}
	}

	public List<String> clickApplyBtn() {
		List<String> lst = new ArrayList<>();
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.applyBtn"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.applyBtn"));
		logger.info("clicked on the apply button");
		TestListeners.extentTest.get().info("clicked on the apply button");
		utils.longwait(2000);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.updateCampaignTag"), "success msg for tag");
		String msg = utils.getLocator("newCamHomePage.updateCampaignTag").getText();
		String msg2 = utils.getLocator("newCamHomePage.campaignTagAdded").getText();
		lst.add(msg);
		lst.add(msg2);
		utils.longWaitInSeconds(10);
		return lst;
	}

	public void deSelectTagForCampaign(String tagName) {
		String xpath = utils.getLocatorValue("newCamHomePage.selectTagForCampaign").replace("${tagName}", tagName);
		String text = driver.findElement(By.xpath(xpath)).getText();
		if (text.equalsIgnoreCase("check_box")) {
			driver.findElement(By.xpath(xpath)).click();
			logger.info("deselecting the tag +" + tagName);
			TestListeners.extentTest.get().info("deselecting the tag +" + tagName);
		} else {
			logger.info("already the tag--" + tagName + " is not selected");
			TestListeners.extentTest.get().info("already the tag--" + tagName + " is not selected");
		}
	}

	public void selectOrDeselectAllTag(String selectOrDeselect) {
		utils.longwait(1000);
		List<WebElement> eleLst = utils.getLocatorList("newCamHomePage.selectOrDeselectAllTag");
		if (eleLst.size() == 0) {
			logger.info("no tags are present to select and deselect");
			TestListeners.extentTest.get().info("no tags are present to select and deselect");
		} else {
			switch (selectOrDeselect) {
			case "select":
				for (int i = 0; i < eleLst.size(); i++) {
					String text = eleLst.get(i).getText();
					if (text.equalsIgnoreCase("check_box")) {
						logger.info("tag is already selected");
						TestListeners.extentTest.get().info("tag is already selected");
					} else {
						eleLst.get(i).click();
						logger.info("tag is selected");
						TestListeners.extentTest.get().info("tag is selected");
					}
				}
				break;
			case "deselect":
				for (int j = 0; j < eleLst.size(); j++) {
					String text = eleLst.get(j).getText();
					if (text.equalsIgnoreCase("check_box")) {
						eleLst.get(j).click();
						logger.info("deselected the tag");
						TestListeners.extentTest.get().info("deselected the tag");
					} else {
						logger.info("tag is already not selected");
						TestListeners.extentTest.get().info("tag is already not selected");
					}
				}
				break;
			}

		}
	}

	public List<String> checkSingleTagAddedInCampaigns() {
		utils.longwait(2000);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.tagsVisible"), "");
		List<WebElement> eleLst = utils.getLocatorList("newCamHomePage.tagsVisible");
		List<String> tagLst = new ArrayList<>();
		if (eleLst.size() == 0) {
			logger.info("tags are not added to the campaigns");
			// TestListeners.extentTest.get().fail("tags are not added to the campaigns");
			return tagLst;
		} else {

			for (int i = 0; i < eleLst.size(); i++) {
				String text = eleLst.get(i).getText();
				tagLst.add(text);
			}
			return tagLst;
		}
	}

	public List<String> checkMultipleTagAddedInCampaigns() {
		utils.longwait(2000);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.multipleTagVisible"), "");
		List<WebElement> eleLst = utils.getLocatorList("newCamHomePage.multipleTagVisible");
		List<String> tagLst = new ArrayList<>();
		if (eleLst.size() == 0) {
			logger.info("tags are not added to the campaigns");
			TestListeners.extentTest.get().fail("tags are not added to the campaigns");
			return tagLst;
		} else {

			for (int i = 0; i < eleLst.size(); i++) {
				String text = eleLst.get(i).getText().replace("+", "");
				tagLst.add(text);
			}
			return tagLst;
		}
	}

//	public void deleteTag(String tagName) {
//		utils.getLocator("newCamHomePage.tagSearch").sendKeys(tagName);
//		utils.getLocator("newCamHomePage.tagSearch").sendKeys(Keys.TAB);
//
//		String xpath = utils.getLocatorValue("newCamHomePage.threeDotsOfTags").replace("${tagName}", tagName);
//		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "three dots");
//		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
//
//		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.deleteTag"), "");
//		utils.getLocator("newCamHomePage.deleteTag").click();
//
//		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.deleteTagConfirmation"), "");
//		utils.getLocator("newCamHomePage.deleteTagConfirmation").click();
//
//		logger.info(tagName+" is been deleted");
//		TestListeners.extentTest.get().info(tagName+" is been deleted");
//	}

	public void clickMoreFilterBtn() throws InterruptedException {

		utils.waitTillNewCamsTableAppear();
		// waitTillCampCountIsDisplaying();

		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.moreFilterButton"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.moreFilterButton"));
		// utils.getLocator("newCamHomePage.moreFilterButton").click();
		logger.info("clicked on the more filter button");
		TestListeners.extentTest.get().info("clicked on the more filter button");
	}

	public void checkGiftType() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.moreFilterBody"), "Side Panel More Filter");
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.giftTypeClick"), "gift type field");
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.giftTypeClick"));
		utils.longWaitInSeconds(2);
///		utils.mouseHover(driver, utils.getLocator("newCamHomePage.giftTypeClick"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.giftTypeClick"));

		logger.info("clicked on the gift type drp down");
		TestListeners.extentTest.get().info("clicked on the gift type drp down");
	}

	public List<String> giftTypeDrpDownLst() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.giftTypeDrpDown"), "gift type lst");
		List<WebElement> ele = utils.getLocatorList("newCamHomePage.giftTypeDrpDown");
		List<String> lst = new ArrayList<>();
		for (int i = 0; i < ele.size(); i++) {
			String text = ele.get(i).getText();
			lst.add(text);
		}
		return lst;
	}

	public void selectGiftType(String giftName) {
		List<WebElement> ele = utils.getLocatorList("newCamHomePage.giftTypeDrpDown");
		for (int i = 0; i < ele.size(); i++) {
			if (ele.get(i).getText().equalsIgnoreCase(giftName)) {
//				utils.scrollToElement(driver, ele.get(i));
				utils.clickByJSExecutor(driver, ele.get(i));
				// ele.get(i).click();
				logger.info("clicked on the gift type " + giftName);
				TestListeners.extentTest.get().info("clicked on the gift type " + giftName);
				break;
			}
		}
	}

	public void clickSidePanelApplyBtn() {
		// utils.longWaitInSeconds(2);
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.sidePanelApplyBtn"));
		// utils.scrollToElement(driver,
		// utils.getLocator("newCamHomePage.sidePanelApplyBtn"));
//		((JavascriptExecutor) driver).executeScript("arguments[0].focus();",
//				utils.getLocator("newCamHomePage.sidePanelApplyBtn"));
//		((JavascriptExecutor) driver).executeScript("arguments[0].dispatchEvent(new Event('click'));",
//				utils.getLocator("newCamHomePage.sidePanelApplyBtn"));

		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.sidePanelApplyBtn"));
		utils.waitTillPagePaceDone();
		logger.info("clicked on the side panel apply button");
		TestListeners.extentTest.get().info("clicked on the side panel apply button");
	}

	public boolean selectedFilterVisible(String filterType, String filterValue) {
		try {
			utils.implicitWait(3);
			String xpath = utils.getLocatorValue("newCamHomePage.selectedFilter").replace("{filterType}", filterType)
					.replace("{filterValue}", filterValue);
			// utils.waitTillInVisibilityOfElement(driver.findElement(By.xpath(xpath)),
			// filterValue);
			driver.findElement(By.xpath(xpath)).isDisplayed();
			utils.implicitWait(50);
			return true;
		} catch (Exception e) {
			utils.implicitWait(50);
			return false;
		}
	}

	public void sidePanelDrpDownClick(String val) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.moreFilterBody"), "Side Panel More Filter");
		String xpath = utils.getLocatorValue("newCamHomePage.sidePanelDrpDownClick").replace("{drpDownValue}", val);
		WebElement drpDownele = driver.findElement(By.xpath(xpath));
		utils.waitTillVisibilityOfElement(drpDownele, val);
		utils.scrollToElement(driver, drpDownele);
		utils.longWaitInSeconds(1);
		utils.waitTillElementToBeClickable(drpDownele);
		utils.clickUsingActionsClass(drpDownele);
		logger.info("clicked on the " + val + " drp down");
		TestListeners.extentTest.get().info("clicked on the " + val + " drp down");
	}

	public void selectFilter(String filterType, String filterValue) {
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("newCamHomePage.sidePanelDrpDownLst").replace("{drpDownVal}", filterType);
		// xpath= xpath.replace("{filterValue}", filterValue);
		// WebElement ele = driver.findElement(By.xpath(xpath));
		// ele.click();
		List<WebElement> ele = driver.findElements(By.xpath(xpath));
		for (int i = 0; i < ele.size(); i++) {
			String val = ele.get(i).getText();
			if (val.equalsIgnoreCase(filterValue)) {
				utils.scrollToElement(driver, ele.get(i));
				utils.longWaitInSeconds(2);
				utils.waitTillElementToBeClickable(ele.get(i));
				// utils.clickByJSExecutor(driver, ele.get(i));
				utils.scrollToElement(driver, ele.get(i));
				utils.longWaitInSeconds(1);
				utils.clickUsingActionsClass(ele.get(i));
				// ele.get(i).click();
				logger.info("clicked on the " + filterValue);
				TestListeners.extentTest.get().info("clicked on the " + filterValue);
				break;
			}
		}
	}

	public String getFirstCampaignName() {
		String text = utils.getLocator("newCamHomePage.firstCampaignName").getText();
		logger.info("campaign name --" + text);
		TestListeners.extentTest.get().info("campaign name --" + text);
		return text;
	}

	public void clickOnFirstCampaign() {
		// String name=getFirstCampaignName();
		// utils.mouseHover(driver,
		// utils.getLocator("newCamHomePage.firstCampaignName"));
		utils.mouseHover(driver, utils.getLocator("newCamHomePage.openSidepannel"));
		utils.getLocator("newCamHomePage.firstCampaignName").click();
		logger.info("clicked on the first campaign");
		TestListeners.extentTest.get().info("clicked on the first campaign");
	}

	public void viewCampaignSummary() {

		// utils.mouseHover(driver,
		// utils.getLocator("newCamHomePage.firstCampaignName"));
		// utils.getLocator("newCamHomePage.firstCampaignName").click();
		WebElement camNameLink = utils.getLocator("newCamHomePage.openSidepannel");
		utils.clickUsingActionsClass(camNameLink);
		logger.info("clicked on the first campaign");
		TestListeners.extentTest.get().info("clicked on the first campaign");
		utils.getLocator("newCamHomePage.viewSummaryBtn").click();
		utils.longWaitInSeconds(2);
		utils.waitTillPagePaceDone();
	}

	public void viewCampaignSidePanel() {
		utils.longWaitInSeconds(2);
		// utils.mouseHover(driver,
		// utils.getLocator("newCamHomePage.firstCampaignName"));
		WebElement camName = utils.getLocator("newCamHomePage.openSidepannel");
		utils.clickUsingActionsClass(camName);
		// utils.getLocator("newCamHomePage.firstCampaignName").click();
		logger.info("clicked on the campaign");
	}

	public String getCamStartTimeFromTable() {
		String val = utils.getLocator("newCamHomePage.camStartTime").getText();
		return val;
	}

	public String getSidePanelCampaignDetails() {
		utils.longWaitInSeconds(5);
		WebElement camSidePanel = utils.getLocator("newCamHomePage.camSidePanel");
		String val = camSidePanel.getText();
		logger.info("clicked on the campaign name link");
//		utils.getLocator("newCamHomePage.closeBtn").click();
		utils.longWaitInSeconds(2);
		// utils.getLocator("newCamHomePage.closeSidePanelBtn").click();
		utils.clickWithActions(utils.getLocator("newCamHomePage.closeSidePanelBtn"));
		return val;

	}

	public String getSidePanelCampaignId() {
		utils.longWaitInSeconds(5);
		WebElement sidePanelCamId = utils.getLocator("newCamHomePage.campaignId");
		String val = sidePanelCamId.getText();
		logger.info("campaign id is : " + val);
		return val;

	}

	public void deleteCampaignFromStickyBar() {
		utils.getLocator("newCamHomePage.campaignCheckbox").click();
		utils.getLocator("newCamHomePage.stickyBarDeleteBtn").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.yesDeleteBtn"), "yes delete button");
		utils.getLocator("newCamHomePage.yesDeleteBtn").click();
		// utils.waitTillPagePaceDone();

		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.campaignDeleteMsg"), "delete status");
		Boolean status = utils.getLocator("newCamHomePage.campaignDeleteMsg").isDisplayed();
		Assert.assertTrue(status, "error while deleting the campaign");

		logger.info("Searched campaign deleted successfuly");
		TestListeners.extentTest.get().pass("Searched campaign deleted successfuly");
	}

	public String checklocationGrpInCampaignInSidePanel() {
		utils.longWaitInSeconds(2);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.locationGrpVisibleSidePanelCampaign"),
				"location grp side panel");
		String text = utils.getLocator("newCamHomePage.locationGrpVisibleSidePanelCampaign").getText();
		return text;
	}

	public void closeSidePanel() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.closeSidePanel"), "close side panel");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.closeSidePanel"));
		logger.info("close the side panel");
		TestListeners.extentTest.get().info("close the side panel");
	}

	public String checkGiftTypeinCampaignSidePanel(String giftType) {
		utils.longWaitInSeconds(4);
		String text = "";
		List<WebElement> wEleList = utils.getLocatorList("newCamHomePage.allCampaignListXpath");
		Assert.assertTrue(wEleList.size() > 0, giftType + " Campagins are not appearing for the filter ");

		for (WebElement wEle : wEleList) {

			wEle.click();
			try {
				utils.longWaitInSeconds(4);
				String xpath = utils.getLocatorValue("newCamHomePage.checkGiftTypeinCampaignSidePanel")
						.replace("{$giftType}", giftType);
				text = null;
				utils.implicitWait(10);
				if (driver.findElements(By.xpath(xpath)).size() != 0) {
					text = driver.findElement(By.xpath(xpath)).getText();
					utils.implicitWait(50);
					closeCampaignSidePanel();
					return text;
				} else {
					String xpath2 = utils.getLocatorValue("newCamHomePage.checkGiftTypeinCampaignSidePanelTwo")
							.replace("{$giftType}", giftType);
					utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath2)), "");
					text = driver.findElement(By.xpath(xpath2)).getText();
					utils.implicitWait(50);
					closeCampaignSidePanel();
					return text;
				}

			} catch (Exception e) {
				closeCampaignSidePanel();
				logger.info("No data present in the campaign side panel for the -- " + giftType
						+ " and campaign name is -- " + wEle.getText());
				TestListeners.extentTest.get().info("No data present in the campaign side panel for the -- " + giftType
						+ " and campaign name is -- " + wEle.getText());
			}
		}

		return text;

	}

	public void selectGiftTypeFromDropDown(String giftName, int counter) throws InterruptedException {
		utils.implicitWait(5);
		String xpath = utils.getLocatorValue("newCamHomePage.selectGiftType").replace("{$giftType}", giftName);
		String xpath2 = utils.getLocatorValue("newCamHomePage.selectGiftType2").replace("{$giftType}", giftName);
		List<WebElement> ele = driver.findElements(By.xpath(xpath));
		if (ele.size() == 1) {
			utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), giftName);
			utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		} else if (driver.findElements(By.xpath(xpath2)).size() > 0) {
			utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath2)), giftName);
			utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath2)));
		} else {
			if (utils.getLocatorList("newCamHomePage.notDisplayed").size() > 0) {
				counter = counter - 1;
				if (counter >= 0) {
					utils.refreshPage();
					utils.waitTillPagePaceDone();
					pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
					utils.waitTillPagePaceDone();
					utils.waitTillPaceDataProgressComplete();
					clickMoreFilterBtn();
					checkGiftType();
					searchFieldSidePanel("Gift type", giftName);
					selectGiftTypeFromDropDown(giftName, counter);
				}
			}
		}
		utils.implicitWait(50);
	}

	public void searchFieldSidePanel(String search, String content) {
		String xpath = utils.getLocatorValue("newCamHomePage.sidePanelSearchBox").replace("{$search}", search);
		driver.findElement(By.xpath(xpath)).clear();
		driver.findElement(By.xpath(xpath)).sendKeys(content);
		logger.info("entered the value" + content + " in the side panel " + search + " field search box");
		TestListeners.extentTest.get()
				.info("entered the value " + content + " in the side panel " + search + " field search box");
	}

	public void challengeCampaignCreation(String campName, String giftType, String redeemable, String segment) {
		// enter the name of the campaign
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.challengeCampaignName"), "campaign name");
		utils.getLocator("newCamHomePage.challengeCampaignName").sendKeys(campName);

		// select the gift type
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignGiftType"), giftType);

		// gift reason
		utils.getLocator("newCamHomePage.challengeCampaignGiftReason").sendKeys("Automation");

		// select redeemable
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignRedeemableSelect"), redeemable);

		// click the next
		utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		utils.waitTillPagePaceDone();

		// select the segment
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignSegmentDrpDown"), segment);

		uploadChallengeCampaignImage("icon_completed");
		utils.longWaitInSeconds(1);
		uploadChallengeCampaignImage("image");
		utils.longWaitInSeconds(1);
		uploadChallengeCampaignImage("icon");

		logger.info("uploaded all the three images");
		TestListeners.extentTest.get().info("uploaded all the three images");

		// click the next
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.challengeCampaignNext"));
//		utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		utils.waitTillPagePaceDone();

		// activate the campaign
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.challengeCampaignActivate"), "");
		utils.getLocator("newCamHomePage.challengeCampaignActivate").click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
	}

	public void uploadChallengeCampaignImage(String image) {
		String xpath = utils.getLocatorValue("newCamHomePage.challengeCampaignImageUpload").replace("${flag}", image);
		driver.findElement(By.xpath(xpath)).sendKeys(System.getProperty("user.dir") + "/resources/images.png");
		logger.info("uploaded the image --" + image);
		TestListeners.extentTest.get().info("uploaded the image --" + image);
	}

	public boolean checkCampaignStatus(String campaignName, String status) {
		String xpath = utils.getLocatorValue("newCamHomePage.campaignStatus").replace("${campaignName}", campaignName)
				.replace("${status}", status);
		utils.implicitWait(5);
		boolean val = driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.implicitWait(50);
		return val;
	}

	public boolean checkOptionPresent(String optionName) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon"), "dotsIcon");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.dotsIcon"));
		// utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIconDrpDown"),
		// "drp down");

		String xpath = utils.getLocatorValue("newCamHomePage.selectOptionFromDotsIcon").replace("${option}",
				optionName);
		utils.implicitWait(5);
		boolean flag = false;
		try {
			driver.findElement(By.xpath(xpath)).isDisplayed();
			flag = true;
		} catch (Exception e) {
		}
		utils.implicitWait(50);
		return flag;
	}

	public void switchTab(String tabName) {
		String xpath = utils.getLocatorValue("newCamHomePage.switchTab").replace("${tabName}", tabName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("switch to tab -- " + tabName);
		TestListeners.extentTest.get().info("switch to tab -- " + tabName);
	}

	public String getValueFromUrlUsingRE(String re) {
		String url = driver.getCurrentUrl();
		String regex = re;
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			String value = matcher.group(1);
			return value;
		} else {
			return null;
		}
	}

	public void moveToUrl(String val) {
		String removeUrl = "creators=" + val;
		String url = driver.getCurrentUrl();
		url = url.replace(removeUrl, "");
		driver.navigate().to(url);
	}

	public void changePage() {
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.nextPage"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.nextPage"));
		utils.waitTillPagePaceDone();
		logger.info("clicked on next page");
		TestListeners.extentTest.get().info("clicked on next page");
	}

	public void selectNoOfItem(String value) {
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.itemPerPage"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.itemPerPage"));
		String xpath = utils.getLocatorValue("newCamHomePage.itemPerPageDrpDown").replace("${value}", value);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("selected " + value + " items per page");
		TestListeners.extentTest.get().info("selected " + value + " items per page");
	}

	public String noTagText() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.noTagText"), "tag model dialog");
		String text = utils.getLocator("newCamHomePage.noTagText").getText();
		return text;
	}

	public void selectTagFilterInSidePanel(String tagValue) {
		utils.waitTillInVisibilityOfElement(utils.getLocator("newCamHomePage.tagFilterSidePanel"), "Tag Filter");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.tagFilterSidePanel"));
//		utils.longwait(500);
		String xpath = utils.getLocatorValue("newCamHomePage.selectTagFilterDrpDownValue").replace("${value}",
				tagValue);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		// utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), "tag
		// value");
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
	}

	public void clickOptionFromDotsDropDown(String option) {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIcon"), "dotsIcon");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.dotsIcon"));
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.dotsIconDrpDown"), "drp down");

		String xpath = utils.getLocatorValue("newCamHomePage.selectOptionFromDotsIcon").replace("${option}", option);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));

		logger.info("clicked on the option-- " + option);
		TestListeners.extentTest.get().info("clicked on the option -- " + option);
	}

	public String getToastMessage() {
		String toastmessage = utils.getLocator("newCamHomePage.toastmessage").getText();
		logger.info("toast message is -- " + toastmessage);
		TestListeners.extentTest.get().info("toast message is -- " + toastmessage);
		return toastmessage;
	}

	public void closeSelectedFilter(String filterName) {
		String xpath = utils.getLocatorValue("newCamHomePage.closeSelectedFilter").replace("${filterName}", filterName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();

		logger.info("deselect the selected filter -- " + filterName);
		TestListeners.extentTest.get().info("deselect the selected filter -- " + filterName);
	}

	public void selectIncludeArchivedCampaigns() {
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.selectIncludeArchivedCampaigns"));
		logger.info("checked the flag -- Include Archived Campaigns");
		TestListeners.extentTest.get().info("checked the flag -- Include Archived Campaigns");
	}

	public void sidePanelDrpDownExpand(String filterName) {
		utils.longwait(800);
		String xpath = utils.getLocatorValue("newCamHomePage.filterSidePanelDrpDown").replace("${filterName}",
				filterName);
		utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
//		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(2);
		logger.info("expand the drp down -- " + filterName);
		TestListeners.extentTest.get().info("expand the drp down -- " + filterName);
	}

	public void selectDrpDownValFromSidePanel(String optionName, String filterName) {
		sidePanelDrpDownExpand(filterName);
		utils.longWaitInSeconds(2);
		String xpath = utils.getLocatorValue("newCamHomePage.selectFilterVal").replace("${filterName}", filterName)
				.replace("${value}", optionName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		// utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
		logger.info("select the value -- " + optionName);
		TestListeners.extentTest.get().info("select the value -- " + optionName);
		sidePanelDrpDownExpand(filterName);
	}

	public void clickOnSwitchToClassicCamp() {
		List<WebElement> listWebElement = utils.getLocatorList("newCamHomePage.switchToClassicBtn");
		if (listWebElement.size() != 0) {
			listWebElement.get(0).click();
		}
	}

	public void clickSortByFilter() {
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.sortByFilterExpand"));
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.sortByFilterVisible"),
				"sort by filter open");
		logger.info("clicked on the sort by filer");
		TestListeners.extentTest.get().info("clicked on the sort by filer");
	}

	public void selectSortByFilterValue(String optionName) {
		clickSortByFilter();
		String xpath = utils.getLocatorValue("newCamHomePage.sortBySelectVal").replace("${optionVal}", optionName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("clicked the value -- " + optionName + " form the Sort By filter");
		TestListeners.extentTest.get().info("clicked the value -- " + optionName + " form the Sort By filter");
	}

	public void selectMultipleDrpDownValFromSidePanel(List<String> optionNameLst, String filterName) {
		sidePanelDrpDownExpand(filterName);
		utils.longwait(500);
		for (int i = 0; i < optionNameLst.size(); i++) {
			String xpath = utils.getLocatorValue("newCamHomePage.selectFilterVal").replace("${filterName}", filterName)
					.replace("${value}", optionNameLst.get(i));
			utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
			// utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
			utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
			logger.info("select the value -- " + optionNameLst.get(i));
			TestListeners.extentTest.get().info("select the value -- " + optionNameLst.get(i));
		}
	}

	public void flushAndSyncDataInNewCamPage() throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.flushData"));
		utils.waitTillPagePaceDone();

		logger.info("clicked on the flush button");
		TestListeners.extentTest.get().info("clicked on the flush button");

		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.sinkData"));
		utils.waitTillPagePaceDone();
		logger.info("clicked on the sync button");
		TestListeners.extentTest.get().info("clicked on the sync button");
	}

	public void removeGiftTypeFilter() {
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.giftTypeFilterRemoved"));
		// utils.waitTillPagePaceDone();
		logger.info("Removed the gift type filter");
		TestListeners.extentTest.get().info("Removed the gift type filter");
	}

	public void closeCampaignSidePanel() {
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.campaignSidePanelClose"));
		// utils.waitTillPagePaceDone();
		logger.info("closed the campaign side panel");
		TestListeners.extentTest.get().info("closed the campaign side panel");
	}

	public void deleteExistingTag() throws InterruptedException {
		utils.implicitWait(9);
		if (utils.getLocatorList("newCamHomePage.noTagVisible") != null) {
			List<WebElement> lst = utils.getLocatorList("newCamHomePage.tagLst");
			List<String> tagNames = lst.stream().map(s -> s.getText()).collect(Collectors.toList());
			String tagName = null;
			for (int i = 0; i < lst.size(); i++) {
//				utils.waitTillInVisibilityOfElement(lst.get(i), "");
				utils.waitTillElementToBeClickable(lst.get(i));
				utils.longWaitInSeconds(5);
				tagName = tagNames.get(i);
				utils.getLocator("newCamHomePage.searchTagsBox").click();
				utils.getLocator("newCamHomePage.searchTagsBox").clear();
				utils.getLocator("newCamHomePage.searchTagsBox").sendKeys(tagName);
				utils.getLocator("newCamHomePage.manageTagsdotsIcon").click();
				utils.getLocator("newCamHomePage.dotsIconDeleteOption").click();
				utils.getLocator("newCamHomePage.yesDeleteBtn").click();
				utils.getLocator("newCamHomePage.deleteTagMsg").getText();
				logger.info("deleted the tag -- " + tagName);
				TestListeners.extentTest.get().info("deleted the tag -- " + tagName);
				utils.longWaitInSeconds(5);
				if (i != lst.size() - 1) {
					utils.longWaitInSeconds(2);
					utils.getLocator("newCamHomePage.searchTagsBox").click();
					utils.getLocator("newCamHomePage.searchTagsBox").clear();
					utils.getLocator("newCamHomePage.searchTagsBox").sendKeys("a");
					utils.getLocator("newCamHomePage.searchTagsBox").sendKeys(Keys.BACK_SPACE);
					utils.longWaitInSeconds(3);
				}
			}
		}
		utils.implicitWait(50);
	}

	public void challengeWhatPage(String campName, String giftType, String redeemable) {
		// enter the name of the campaign
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.challengeCampaignName"), "campaign name");
		utils.getLocator("newCamHomePage.challengeCampaignName").sendKeys(campName);
		logger.info("entered the campaign name -- " + campName);
		TestListeners.extentTest.get().info("entered the campaign name -- " + campName);

		// select the gift type
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignGiftType"), giftType);

		// gift reason
		utils.getLocator("newCamHomePage.challengeCampaignGiftReason").sendKeys("Automation");

		// select redeemable
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignRedeemableSelect"), redeemable);

		// click the next
		utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		utils.waitTillPagePaceDone();

		logger.info("clicked on next button");
		TestListeners.extentTest.get().info("clicked on next button");
	}

	public void uploadAllImagesInChallengeCampaign() {
		uploadChallengeCampaignImage("icon_completed");
		utils.longWaitInSeconds(1);
		uploadChallengeCampaignImage("image");
		utils.longWaitInSeconds(1);
		uploadChallengeCampaignImage("icon");

		logger.info("upload all the required images for the challenge campaign");
		TestListeners.extentTest.get().info("upload all the required images for the challenge campaign");
	}

	public void challengeDrpDown(String value) {
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeDrpDown"), value);
		logger.info("selected the value -- " + value);
		TestListeners.extentTest.get().info("selected the value -- " + value);
	}

	public void noOfStepsPoints(String str) {
		utils.getLocator("newCamHomePage.noOfStepsPoints").clear();
		utils.getLocator("newCamHomePage.noOfStepsPoints").sendKeys(str);
		logger.info("entered the value -- " + str);
		TestListeners.extentTest.get().info("entered the value -- " + str);
	}

	public void pnForChallengeComplete(String pn) {
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.challengeCompletedPN"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.challengeCompletedPN"));

		logger.info("selected challenge completed PN");
		TestListeners.extentTest.get().info("selected challenge completed PN");

		List<WebElement> lst = utils.getLocatorList("newCamHomePage.pnLst");
		for (int i = 0; i < lst.size(); i++) {
			lst.get(i).clear();
			lst.get(i).sendKeys(pn);
		}

		utils.getLocator("newCamHomePage.pnTextarea").clear();
		utils.getLocator("newCamHomePage.pnTextarea").sendKeys(pn);

		// click the next
		utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		logger.info("clicked on the next button");
		TestListeners.extentTest.get().info("clicked on the next button");
		utils.waitTillPagePaceDone();
	}

	public void activateChallengeCampaign() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.challengeCampaignActivate"), "");
		utils.getLocator("newCamHomePage.challengeCampaignActivate").click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
	}

	public void removeFilter(String filterName) {
		String xpath = utils.getLocatorValue("newCamHomePage.removeFilter").replace("{$filter}", filterName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		utils.waitTillPagePaceDone();
		logger.info("Removed the filter -- " + filterName);
		TestListeners.extentTest.get().info("Removed the gift type filter -- " + filterName);
	}

	public boolean switchToClassicVisible() {
		utils.implicitWait(3);
		if (utils.getLocatorList("newCamHomePage.switchToClassicBtn").size() > 0) {
			logger.info("switch to classic button is visible");
			TestListeners.extentTest.get().info("switch to classic button is visible");
			utils.implicitWait(50);
			return true;
		}
		logger.info("switch to classic button is not visible");
		TestListeners.extentTest.get().info("switch to classic button is not visible");
		utils.implicitWait(50);
		return false;
	}

	public List<String> optionsVisibleInThreeDotsOfCampaign() {
		utils.longWaitInSeconds(2);
		// utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.dotsIcon"));
		WebElement dotsIcon = utils.getLocator("newCamHomePage.dotsIcon");
		utils.waitTillElementToBeClickable(dotsIcon);
		dotsIcon.click();
		utils.longWaitInSeconds(2);
		List<WebElement> lst = utils.getLocatorList("newCamHomePage.dotsIconDrpDownLst");
		List<String> optionsNames = lst.stream().map(s -> s.getText()).collect(Collectors.toList());
		/*
		 * List<String> str = new ArrayList<>(); for (int i = 0; i < lst.size(); i++) {
		 * String text = lst.get(i).getText(); str.add(text); }
		 */
		return optionsNames;
	}

	public String getTextColour(WebElement ele) {
		utils.waitTillPagePaceDone();
		String rgbFormat = ele.getCssValue("background-color");
		String hexcode = Color.fromString(rgbFormat).asHex();
		System.out.println("hexcode is :" + hexcode);
		return hexcode;
	}

	public String getSelectedDateBGColor() {
		// utils.scrollToElement(driver,
		// utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter"));
		utils.longWaitInSeconds(2);
		utils.clickUsingActionsClass(utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter"));
		String hexacode = getTextColour(utils.getLocator("newCamHomePage.selectedDate"));
		return hexacode;
	}

	public void applyDateFilter(String dateFrom, String dateTo) throws ParseException {
		String expectedFromDate = CreateDateTime.convertFormat(dateFrom);
		String expectedToDate = CreateDateTime.convertFormat(dateTo);

		String xpath1 = utils.getLocatorValue("newCamHomePage.dateDisplay").replace("${Date}", expectedFromDate);
		String xpath2 = utils.getLocatorValue("newCamHomePage.dateDisplay").replace("${Date}", expectedToDate);

		// select the from date
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter"));
		utils.sendKeysUsingActionClass(utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter"),
				dateFrom);

		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath1)), "From date");
		utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter").click();
		utils.longwait(500);

		// select the to date
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter"));
		// utils.sendKeysUsingActionClass(utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter"),
		// dateTo);
		utils.sendKeysUsingActionClass(utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter"), dateTo);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath2)), "To date");
		utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter").click();
		utils.longwait(500);
	}

	public List<String> getFilterLst(String filter) {
		String xpath = utils.getLocatorValue("newCamHomePage.filterLst").replace("{$filter}", filter);
		List<WebElement> ele = driver.findElements(By.xpath(xpath));
		List<String> lst = new ArrayList<>();
		for (int i = 0; i < ele.size(); i++) {
			String text = ele.get(i).getText();
			lst.add(text);
		}
		return lst;
	}

	public void searchValueInSidePanel(String filter, String value) {
		String xpath = utils.getLocatorValue("newCamHomePage.filterSearchBox").replace("{$filter}", filter);
		utils.waitTillInVisibilityOfElement(driver.findElement(By.xpath(xpath)), "search field");
		driver.findElement(By.xpath(xpath)).clear();
		driver.findElement(By.xpath(xpath)).sendKeys(value);
		utils.longWaitInSeconds(1);
		logger.info("in the " + filter + " search box searched the value -- " + value);
		TestListeners.extentTest.get().info("in the " + filter + " search box searched the value -- " + value);
	}

	public int searchBoxResultList(String filter) {
		String xpath1 = utils.getLocatorValue("newCamHomePage.searchBoxResultListVisible").replace("{$filter}", filter);
		String xpath2 = utils.getLocatorValue("newCamHomePage.searchBoxResultList").replace("{$filter}", filter);
		utils.waitTillInVisibilityOfElement(driver.findElement(By.xpath(xpath1)), "search list");
		int size = driver.findElements(By.xpath(xpath2)).size();
		return size;
	}

	public void selectCampaignFromNewCampaignPage(String campaignName) {
		String xpath = utils.getLocatorValue("newCamHomePage.selectCampaignFromNewCampaignPage")
				.replace("{$campaignName}", campaignName);
		String text = driver.findElement(By.xpath(xpath)).getText();
		if (text.contains("blank")) {
			utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
			logger.info("selecting the campaign -- " + campaignName);
			TestListeners.extentTest.get().info("selecting the campaign -- " + campaignName);
		} else {
			logger.info("campaign -- " + campaignName + " is already selected");
			TestListeners.extentTest.get().info("campaign -- " + campaignName + " is already selected");
		}
	}

	public String selectedCampaignCount() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.selectCampaignCount"), getCampaignStatus());
		String str = utils.getLocator("newCamHomePage.selectCampaignCount").getText();
		logger.info("number of campaigns seleted -- " + str);
		TestListeners.extentTest.get().info("number of campaigns seleted -- " + str);
		return str;
	}

	public void selectedBulkCampaignOptionSelect(String optionName) {
		String xpath = utils.getLocatorValue("newCamHomePage.selectedBulkCampaignOptionSelect").replace("{$optionName}",
				optionName);
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		logger.info("clicked on the option -- " + optionName);
		TestListeners.extentTest.get().info("clicked on the option -- " + optionName);
	}

	public String selectedBulkCampaignOptionVisible(String optionName) {
		String xpath = utils.getLocatorValue("newCamHomePage.selectedBulkCampaignOption").replace("$option",
				optionName);
		String button = driver.findElement(By.xpath(xpath)).getText();
		logger.info(" This option is visible -- " + optionName);
		TestListeners.extentTest.get().info("This option is visible -- " + optionName);
		return button;
	}

	public String archiveCampaignModalDialogueButtonvisible(String optionName) {
		String xpath = utils.getLocatorValue("newCamHomePage.archiveCampaignModalDialogueButton").replace("$option",
				optionName);
		String button = driver.findElement(By.xpath(xpath)).getText();
		logger.info(" This option is visible -- " + optionName);
		TestListeners.extentTest.get().info("This option is visible -- " + optionName);
		return button;
	}

	public String selectedBulkCampaignClearSelectionVisible() {
		String button = utils.getLocator("newCamHomePage.selectedBulkCampaignClearSelection").getText();
		logger.info(" This option is visible -- " + button);
		TestListeners.extentTest.get().info("This option is visible -- " + button);
		return button;
	}

	public String modalDialogueBoxContent() {
		String text = utils.getLocator("newCamHomePage.modalDialogueBoxContent").getText();
		logger.info("displayed text is -- " + text);
		TestListeners.extentTest.get().info("displayed text is -- " + text);
		return text;
	}

	public void deleteBulkCampaign() {
		utils.getLocator("newCamHomePage.yesDeleteBtn").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.deleteCampaignStatus"), "delete status");
		Boolean status = utils.getLocator("newCamHomePage.deleteCampaignStatus").isDisplayed();
		Assert.assertTrue(status, "error while deleting the campaign");
		logger.info("Successfully delete the selected campaigns ");
		TestListeners.extentTest.get().pass("Successfully delete the selected campaigns ");
	}

	public String deleteCampaignModelText() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.yesDeleteBtn"), "yes delete button");
		utils.waitTillInVisibilityOfElement(utils.getLocator("newCamHomePage.deleteModelText"), "display text");
		String str = utils.getLocator("newCamHomePage.deleteModelText").getText();
		return str;
	}

	public void selectChallengeCampaignTimeZone(String timeZone) {
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.selectChallengeCampaignTimeZone"), timeZone);
		logger.info("selected the timezone -- " + timeZone);
		TestListeners.extentTest.get().info("selected the timezone -- " + timeZone);
	}

	public boolean noResultFoundVisible() {
		utils.implicitWait(10);
		int size = utils.getLocatorList("newCamHomePage.noResultFound").size();
		if (size >= 1) {
			utils.implicitWait(50);
			return true;
		}
		utils.implicitWait(50);
		return false;
	}

	public String invalidSearchMsg() {
		String text = utils.getLocator("newCamHomePage.invalidSearchMsg").getText();
		logger.info("message displayed is -- " + text);
		TestListeners.extentTest.get().info("message displayed is -- " + text);
		return text;
	}

	public boolean clearSearchButtonVisible() {
		utils.implicitWait(3);
		int size = utils.getLocatorList("newCamHomePage.clearSearchBtn").size();
		if (size >= 1) {
			utils.implicitWait(50);
			return true;
		}
		utils.implicitWait(50);
		return false;
	}

	public boolean clearSearchAndFilterButtonVisible() {
		utils.implicitWait(3);
		int size = utils.getLocatorList("newCamHomePage.clearSearchAndFilterButton").size();
		if (size >= 1) {
			utils.implicitWait(50);
			return true;
		}
		utils.implicitWait(50);
		return false;
	}

	public String selectedFilterTextVisible(String filterName) {
		String value = null;
		String xpath = utils.getLocatorValue("newCamHomePage.selectedFilterTextNew").replace("{$filter}", filterName);
		int size = driver.findElements(By.xpath(xpath)).size();
		if (size != 0) {
			value = driver.findElement(By.xpath(xpath)).getText();
		} else {
			String xpath1 = utils.getLocatorValue("newCamHomePage.selectedFilterText").replace("{$filter}", filterName);
			value = driver.findElement(By.xpath(xpath1)).getText();
		}
		logger.info("selected value from the filter -- " + filterName + " is -- " + value);
		TestListeners.extentTest.get().info("selected value from the filter -- " + filterName + " is -- " + value);
		return value;
	}

	public List<String> getFilterValueList(String filter) {
		String xpath = utils.getLocatorValue("newCamHomePage.filterValueList").replace("{$filter}", filter);
		List<WebElement> ele = driver.findElements(By.xpath(xpath));
		List<String> str = new ArrayList<>();
		for (int i = 0; i < ele.size(); i++) {
			String text = ele.get(i).getText();
			str.add(text);
		}
		return str;
	}

	public List<String> getSidePanelFilterList() {
		utils.waitTillPagePaceDone();
		utils.waitTillInVisibilityOfElement(utils.getLocator("newCamHomePage.sidePanelFilterList"), "side panel list");
		List<WebElement> ele = utils.getLocatorList("newCamHomePage.sidePanelFilterList");
		List<String> str = new ArrayList<>();
		for (int i = 0; i < ele.size(); i++) {
			String text = ele.get(i).getText().trim();
			if (text.contains("info")) {
				text = text.replace("info", "").trim();
				str.add(text);
				continue;
			}
			str.add(text);
		}
		return str;
	}

	public boolean selectedFilterVisible(String filterName) {
		String xpath = utils.getLocatorValue("newCamHomePage.selectedFilterVisible").replace("{$filter}", filterName);
		if (driver.findElements(By.xpath(xpath)).size() == 1) {
			return true;
		}
		return false;
	}

	public int sidePanelDrpDownFilterOptionsCount(String filterName, int filterOptionSize) throws InterruptedException {
		utils.longwait(800);
		String xpath = utils.getLocatorValue("newCamHomePage.campaignFilterOptionsList").replace("$filter", filterName);
		List<WebElement> ele = driver.findElements(By.xpath(xpath));

		int size = ele.size();
		if (size == 0 || size == 1) {
			int newSize = sidePanelDrpDownFilterOptionsForZeroCount(filterName);
			size = newSize;
		}

		return size;
	}

	public void exportReport() {
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.newChpThreeDots"));
		utils.getLocator("newCamHomePage.newChpThreeDots").click();
		logger.info("clicked new CHP three dots");
		TestListeners.extentTest.get().info("clicked new CHP three dots");
		utils.getLocator("newCamHomePage.exportReport").click();
		logger.info("clicked export report option");
		TestListeners.extentTest.get().info("clicked export report option");
	}

	public List<String> getArchiveModalText() {
		List<String> str = new ArrayList<String>();

		List<WebElement> lst = utils.getLocatorList("newCamHomePage.archiveModalText");
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			str.add(text);
		}
		return str;
	}

	public List<String> getCampaignsInArchivalModalBox() {
		List<String> str = new ArrayList<String>();
		List<WebElement> list = utils.getLocatorList("newCamHomePage.campaignsInArchivalModalBox");
		logger.info(" Selected all campaigns in archive modal box ");
		TestListeners.extentTest.get().info(" Selected all campaigns in archive modal box ");
		for (int i = 0; i < list.size(); i++) {
			String text = list.get(i).getText();
			str.add(text);
		}
		return str;
	}

	public List<String> archiveModalCampaignDisplay() {
		List<String> str = new ArrayList<String>();
		List<WebElement> lst = utils.getLocatorList("newCamHomePage.archiveModalCampaignDisplay");
		for (int i = 0; i < lst.size(); i++) {
			String text = lst.get(i).getText();
			str.add(text);
		}
		return str;
	}

	public void applyDateFilterForInvalidDate(String dateFrom, String dateTo) throws ParseException {
		String expectedFromDate = CreateDateTime.convertFormat(dateFrom);
		String expectedToDate = CreateDateTime.convertFormat(dateTo);

		String xpath1 = utils.getLocatorValue("newCamHomePage.dateDisplay").replace("${Date}", expectedFromDate);
		String xpath2 = utils.getLocatorValue("newCamHomePage.dateDisplay").replace("${Date}", expectedToDate);

		// select the from date
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter"));
		utils.sendKeysUsingActionClass(utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter"),
				dateFrom);
		/*
		 * utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath1)),
		 * "From date"); String getAttr =
		 * driver.findElement(By.xpath(xpath1)).getAttribute("class"); if
		 * (getAttr.contains("content-solid")) {
		 * logger.info("from date is already selected ");
		 * TestListeners.extentTest.get().info("from date is already selected");
		 * utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter").click(
		 * ); } else { driver.findElement(By.xpath(xpath1)).click();
		 * logger.info("from date is selected");
		 * TestListeners.extentTest.get().info("from date is selected"); }
		 */
		utils.getLocator("newCamHomePage.selectStartDateOnCampHomePageFilter").click();
		utils.longWaitInSeconds(1);
		// select the to date
		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter"));
		utils.sendKeysUsingActionClass(utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter"), dateTo);
		utils.implicitWait(3);
		utils.getLocator("newCamHomePage.selectEndDateOnCampHomePageFilter").click();
		utils.longWaitInSeconds(1);
	}

	public boolean checkCampaignPresent(String campaignName) {
		searchCampaign(campaignName);
		boolean flag = noResultFoundVisible();
		return flag;
	}

	public List<String> getCampaignType(String option) {
		// capture all element in a list
		// capture text of all elements in new list(original)
		// compare original list with webelement text
		driver.findElement(By.xpath("//span[text()='" + option + "']")).click();
		List<WebElement> typeList = driver.findElements(By.xpath("//tbody//tr//td[5]//p"));
		// typeList.stream().forEach(s -> System.out.println(s.getText()));
		List<String> originalList = typeList.stream().map(s -> s.getText()).distinct().collect(Collectors.toList());
		return originalList;
	}

	public List<String> getCampaignTypeDrpValues(String option, String filterName) {
		driver.findElement(By.xpath("//span[text()='" + option + "']")).click();
		utils.getLocator("newCamHomePage.moreFilterButton").click();
		utils.longWaitInSeconds(2);
		sidePanelDrpDownExpand(filterName);
		utils.longwait(500);
		logger.info("Clicked loyalty checkin and sales dropdown");
		List<WebElement> ele = utils.getLocatorList("newCamHomePage.TypeDrpList");
		List<String> options = ele.stream().map(s -> s.getText()).distinct().sorted().collect(Collectors.toList());
		return options;
	}

	public String getCampSourceIDFromSidePanel() {
		utils.longWaitInSeconds(2);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.campSourceIDFromSidePanel"),
				"campaign source ID on side panel");
		String campID = utils.getLocator("newCamHomePage.campSourceIDFromSidePanel").getText();
		logger.info("Received source ID of campaign ");
		TestListeners.extentTest.get().info("Received source ID of campaign");
		return campID;
	}

	public void selectCampaignDate(String calendarNameLabel, String days, String monthAndYearToBeSelected)
			throws ParseException {
		String xpath = utils.getLocatorValue("newCamHomePage.openCalendar").replace("${calendar}", calendarNameLabel);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		// utils.longWaitInSeconds(3);
		// utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
		utils.scrollToElement(driver, driver.findElement(By.xpath(xpath)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));

		logger.info("clicked on the calendar of the field -- " + calendarNameLabel);
		TestListeners.extentTest.get().info("clicked on the calendar of the field -- " + calendarNameLabel);

		utils.longWaitInSeconds(1);

		int dateCpmareRst = 0;
		do {
			String currentCalValue = utils.getLocator("newCamHomePage.monthVisible").getText();
			dateCpmareRst = CreateDateTime.compareDates(monthAndYearToBeSelected, currentCalValue);
			if (dateCpmareRst == -1) {
				// utils.getLocator("newCamHomePage.previousMonth").click();
				utils.clickUsingActionsClass(utils.getLocator("newCamHomePage.previousMonth"));
				utils.longWaitInSeconds(1);
			} else if (dateCpmareRst == 1) {
				// utils.getLocator("newCamHomePage.nextMonth").click();
				utils.clickUsingActionsClass(utils.getLocator("newCamHomePage.nextMonth"));
				utils.longWaitInSeconds(1);
			}
		} while (dateCpmareRst != 0);

		String dateId = "";
		/*
		 * if (days.charAt(0) == '0') { days=days.replaceFirst("0", "").trim();
		 * 
		 * }
		 */
		String monYear[] = monthAndYearToBeSelected.split(" ");
		// String month=monYear[0].substring(0,3);
		String month = monYear[0];
		String monthNumber = CreateDateTime.convertMonthtoNumber(month);
		dateId = "id-" + monYear[1] + "-" + monthNumber + "-" + days;
		// dateId = "id-"+days+" "+month+" "+monYear[1];

		String xpath2 = utils.getLocatorValue("newCamHomePage.dayToSelect").replace("{days}", dateId);
		logger.info(xpath2);
		WebElement calDate = driver.findElement(By.xpath(xpath2));
		// utils.longWaitInSeconds(1);
		// utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath2)));
		utils.clickUsingActionsClass(calDate);

		logger.info("clicked on the date -- " + days + " " + monthAndYearToBeSelected);
		TestListeners.extentTest.get().info("clicked on the date -- " + days + " " + monthAndYearToBeSelected);

	}

	public int sidePanelDrpDownFilterOptionsForZeroCount(String filterName) throws InterruptedException {
		closeSidePanel();
		clickMoreFilterBtn();
		sidePanelDrpDownExpand(filterName);
		String xpath = utils.getLocatorValue("newCamHomePage.campaignFilterOptionsList").replace("$filter", filterName);
		List<WebElement> ele = driver.findElements(By.xpath(xpath));
		int size = ele.size();
		return size;
	}

	public void viewCampSidePanelNew() {
		boolean flag = false;
		for (int i = 1; i <= 4; i++) {
//			utils.mouseHover(driver, utils.getLocator("newCamHomePage.firstCampaignName"));
			utils.getLocator("newCamHomePage.openSidepannel").click();
			utils.longwait(1000);
			utils.waitTillPagePaceDone();
			try {
				utils.getLocator("newCamHomePage.campSourceIDFromSidePanel").isDisplayed();
				flag = true;
				break;
			} catch (Exception e) {
				utils.getLocator("newCamHomePage.closeSidePanel").click();
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				utils.waitTillPaceDataProgressComplete();
			}
		}
		if (flag) {
			logger.info("Campaign side-panel data displayed");
			TestListeners.extentTest.get().info("Campaign side-panel data displayed");
		} else {
			logger.info("Error in displaying Campaign side-panel data");
			TestListeners.extentTest.get().info("Error in displaying Campaign side-panel data");
		}
	}

	public String verifyErrorMsgOnDateNotWithinRange() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.errorMsgOnDateNotWithinRange"), "errorMsgOnDateNotWithinRange");
		String text = utils.getLocator("newCamHomePage.errorMsgOnDateNotWithinRange").getText();
		return text;
	}

	// shashank:-
	/*
	 * public void selectFilterBySearchingValue(String filterName, String
	 * valToBeSelect) throws InterruptedException { System.out.println("afgf"); int
	 * counter = 0; boolean flag = false; boolean isValSelected = false; do { String
	 * filterTypeInputBoxXpath =
	 * utils.getLocatorValue("newCamHomePage.filterTypeInputBoxXpath")
	 * .replace("$filterName", filterName); utils.scrollToElement(driver,
	 * driver.findElement(By.xpath(filterTypeInputBoxXpath)));
	 * utils.implicitWait(1); // get all the selected item String
	 * selectedFilterOptionsListXpath = utils
	 * .getLocatorValue("newCamHomePage.selectedFilterOptionsListXpath")
	 * .replace("$filterName", filterName);
	 * 
	 * List<WebElement> listOfSelectedFilterOptions = driver
	 * .findElements(By.xpath(selectedFilterOptionsListXpath)); if
	 * (listOfSelectedFilterOptions.size() != 0) { // check valToBeSelect is already
	 * selected or NOT for (WebElement Wele : listOfSelectedFilterOptions) { String
	 * actualSelectedVal = Wele.getText(); if
	 * (actualSelectedVal.equalsIgnoreCase(valToBeSelect)) { isValSelected = true;
	 * flag = true; break; }
	 * 
	 * } }
	 * 
	 * if (!isValSelected) {
	 * driver.findElement(By.xpath(filterTypeInputBoxXpath)).clear();
	 * driver.findElement(By.xpath(filterTypeInputBoxXpath)).sendKeys(valToBeSelect)
	 * ;
	 * 
	 * String filterSearchedResultTextXpath = "//div[label[normalize-space()='" +
	 * filterName + "']]/following-sibling::div/ul/div/li//span/strong";
	 * 
	 * List<WebElement> filterSearchedResultTextList = driver
	 * .findElements(By.xpath(filterSearchedResultTextXpath));
	 * 
	 * String filterSearchedResultXpath =
	 * utils.getLocatorValue("newCamHomePage.filterSearchedResultXpath")
	 * .replace("$filterName", filterName); List<WebElement>
	 * filterSearchedResultCheckkBoxList = driver
	 * .findElements(By.xpath(filterSearchedResultXpath)); int resultCounter = 0 ;
	 * if (filterSearchedResultTextList.size() != 0 &&
	 * (filterSearchedResultTextList.size() ==
	 * filterSearchedResultCheckkBoxList.size())) {
	 * 
	 * for (WebElement filterOptionsWele : filterSearchedResultTextList) { String
	 * actualTextSearchResult = filterOptionsWele.getText(); if
	 * (actualTextSearchResult.equalsIgnoreCase(valToBeSelect)) {
	 * filterSearchedResultTextList.get(resultCounter).click(); flag = true;
	 * logger.info(valToBeSelect + " filter value is selected in  filter " +
	 * filterName); TestListeners.extentTest.get() .info(valToBeSelect +
	 * " filter value is selected in  filter " + filterName);
	 * 
	 * break; } resultCounter++; }
	 * 
	 * 
	 * } else { logger.info(valToBeSelect +
	 * " filter value is not selected in  filter " + filterName + " as counter is "
	 * + counter); TestListeners.extentTest.get().info(valToBeSelect +
	 * " filter value is not selected in  filter " + filterName + " as counter is "
	 * + counter); utils.longWaitInSeconds(1); counter++;
	 * 
	 * }
	 * 
	 * } } while ((flag == false) && (counter <= 20)); Assert.assertTrue(flag,
	 * valToBeSelect + " filter value is not visible in the filter " + filterName);
	 * utils.implicitWait(50);
	 * 
	 * }
	 */

	public String verifySelectedCampaignTimezone() {
		String str = utils.getLocator("newCamHomePage.getSelectedCampaignTimezone").getText();
		String timezone = str.trim();
		logger.info("Selected timezone on campaign timezone dropdown is :" + timezone);
		TestListeners.extentTest.get().info("Selected timezone on campaign timezone dropdown is :" + timezone);
		return timezone;
	}

	public String verifyHintBelowTimezoneField() {
		utils.getLocator("newCamHomePage.hintBelowTimezoneField").isDisplayed();
		String hint = utils.getLocator("newCamHomePage.hintBelowTimezoneField").getText();
		logger.info("Hint below timezone field is visible");
		TestListeners.extentTest.get().info("Hint below timezone field is visible");
		return hint;
	}

	public void saveAsDraftButton() {
		utils.getLocator("newCamHomePage.saveAsDraftBtn").isDisplayed();
		utils.getLocator("newCamHomePage.saveAsDraftBtn").click();
		logger.info("clicked Save As Draft buttton");
		TestListeners.extentTest.get().info("clicked Save As Draft buttton");
	}

	public void createCampaignTemplate(String campaignType, String choice) {
		utils.getLocator("newCamHomePage.createTemplateBtn").isDisplayed();
		utils.getLocator("newCamHomePage.createTemplateBtn").click();
		utils.getLocator("newCamHomePage.templateTypePopup").isDisplayed();
		String xpath = utils.getLocatorValue("newCamHomePage.chooseTemplateType").replace("$campType", campaignType);
		driver.findElement(By.xpath(xpath)).click();
		utils.getLocator("newCamHomePage.nextBtn").click();
		utils.longWaitInSeconds(2);
		String xpath1 = utils.getLocatorValue("newCamHomePage.includeOfferYesNo").replace("$choice", choice);
		driver.findElement(By.xpath(xpath1)).click();
		utils.getLocator("newCamHomePage.createBtn").click();
		logger.info("Campaign template type selected");
		TestListeners.extentTest.get().pass("Campaign template type selected");
		utils.waitTillPagePaceDone();

	}

	public boolean getTemplateText() {
		boolean flag = false;
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(3);
		utils.getLocator("newCamHomePage.searchedTemplate").isDisplayed();
		flag = true;
		logger.info("Template Name is displayed on new cam home page");
		TestListeners.extentTest.get().info("Template Name is displayed on new cam home page");
		return flag;

	}

	public void searchTemplate(String tempName) {
		utils.waitTillNewCamsTableAppear();
		WebElement el = utils.getLocator("newCamHomePage.searchCamBox");
		el.clear();
		el.sendKeys(tempName);
		el.sendKeys(Keys.ENTER);
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.searchedTemplate"), "searchedTemplate");
		logger.info("template searched on new cam home page");
		TestListeners.extentTest.get().info("template searched on new cam home page");
		utils.longWaitInSeconds(1);
	}

	public boolean templateEllipsesOptions(String option, String heading) {
		// boolean flag=false;
		utils.longWaitInSeconds(1);
		clickEllipsesOptions();
		String xpath = utils.getLocatorValue("newCamHomePage.useTemplateOption").replace("$option", option);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("clicked template option : " + option);
		TestListeners.extentTest.get().info("clicked template option : " + option);
		String xpath1 = utils.getLocatorValue("newCamHomePage.verifyCampaignGetDuplicateOrNot").replace("$CampName",
				heading);
		boolean val = driver.findElement(By.xpath(xpath1)).isDisplayed();
		utils.implicitWait(50);
		return val;
	}

	public boolean useTemplateFromSidePanelCTA(String option, String heading) {
		utils.getLocator("newCamHomePage.openSidepannel").click();
		utils.waitTillPagePaceDone();
		String xpath1 = utils.getLocatorValue("newCamHomePage.useTemplateOption").replace("$option", option);
		driver.findElement(By.xpath(xpath1)).click();
		logger.info("clicked template option : " + option);
		TestListeners.extentTest.get().info("clicked template option : " + option);
		String xpath2 = utils.getLocatorValue("newCamHomePage.verifyCampaignGetDuplicateOrNot").replace("$CampName",
				heading);
		boolean val = driver.findElement(By.xpath(xpath2)).isDisplayed();
		utils.implicitWait(50);
		return val;
	}

	public void verifyBulkCheckBox() {
		utils.getLocator("newCamHomePage.bulkCheckbox").isDisplayed();
		utils.getLocator("newCamHomePage.bulkCheckbox").click();
		logger.info("Bulk checkbox is visible on new CHP");
		TestListeners.extentTest.get().info("Bulk checkbox is visible on new CHP");
	}

	public void campaignTabSelection(String campaignType) {
		String xpath = utils.getLocatorValue("newCamHomePage.templateTabsAllMassPost").replace("$campType",
				campaignType);
		driver.findElement(By.xpath(xpath)).click();
		logger.info("Campaign tab is selected as :" + campaignType);
		TestListeners.extentTest.get().info("Campaign tab is selected as :" + campaignType);
		utils.longWaitInSeconds(3);
	}

	public String getSelectedFilter(String filterName) {
		String xpath = utils.getLocatorValue("newCamHomePage.selectedFilterVisible").replace("{$filter}", filterName);
		if (driver.findElements(By.xpath(xpath)).size() == 1) {
			String text = driver.findElement(By.xpath(xpath)).getText();
			return text;
		}
		return null;
	}

	public List<String> getOptionsInCreateCampBtn() {
		utils.getLocator("newCamHomePage.createCampaignbtn").click();
		List<WebElement> eleList = utils.getLocatorList("newCamHomePage.campaignCategory");
		List<String> optionList = new ArrayList<>();
		for (int i = 0; i < eleList.size(); i++) {
			String text = eleList.get(i).getText();
			optionList.add(text);
			System.out.println(text);
		}
		return optionList;
	}

	public void createBlackoutDateFromNewCHP() throws InterruptedException {
		String date = utils.nextdaysDate();
		String dateOnly = date.split("-")[2];
		String xpath = utils.getLocatorValue("newCamHomePage.deleteBalckoutDate").replace("$date", dateOnly);
		try {
			driver.findElement(By.xpath(xpath)).isDisplayed();
			utils.getLocator("newCamHomePage.deleteBlackoutdateIcon").click();
			utils.acceptAlert(driver);
			utils.waitTillPagePaceDone();
			logger.info("Blackout date has been deleted");
			TestListeners.extentTest.get().info("Blackout date has been deleted");
			utils.longWaitInSeconds(1);
		} catch (Exception e) {
			logger.info("Blackout date is not available for next day");
			TestListeners.extentTest.get().info("Blackout date is not available for next day");
		}
		List<WebElement> dateBox = driver.findElements(By.xpath("//td[@data-date='" + date + "']"));
		for (WebElement ele : dateBox) {
			try {
				ele.click();
				break;
			} catch (Exception e) {

			}
		}
		utils.getLocator("campaignsPage.blackoutdateReason").sendKeys("Test Blackout");
		utils.getLocator("campaignsPage.submitBtn").click();
		Thread.sleep(4000);
	}

	public void checkSidepanelCTAoptions(String campName, String optionName) {
		String xpath = utils.getLocatorValue("newCamHomePage.openSidepannel").replace("$campName", campName);
		driver.findElement(By.xpath(xpath)).click();
		WebElement ele = driver.findElement(By.xpath("//span[text()='" + optionName + "']"));
		utils.waitTillElementToBeClickable(ele);
		utils.clickByJSExecutor(driver, ele);
		utils.waitTillPagePaceDone();
		logger.info("clicked campaign option :" + optionName);
		TestListeners.extentTest.get().info("clicked campaign option :" + optionName);

	}

	public void verifyTableContentPresence(String tab) {
		String xpath = utils.getLocatorValue("newCamHomePage.templateTableContent").replace("$tab", tab);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		logger.info("Table content tab " + tab + " visible on new cam home page");
		TestListeners.extentTest.get().pass("Table content tab " + tab + " visible on new cam home page");
	}

	public void approveDisapproveCampaign(String option, String reason) {
		String xpath = utils.getLocatorValue("newCamHomePage.approveDisapproveOption").replace("$option", option);
		driver.findElement(By.xpath(xpath)).click();
		utils.getLocator("newCamHomePage.approvalDisapprovalReason").isDisplayed();
		utils.getLocator("newCamHomePage.approvalDisapprovalReason").sendKeys(reason);
		utils.getLocator("newCamHomePage.approvalDisapprovalReason").sendKeys(Keys.ENTER);
		utils.longWaitInSeconds(2);
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.okBtn"));
		logger.info("Campaign has been disapproved");
		TestListeners.extentTest.get().info("Campaign has been disapproved");
	}

	public int getAllSelectedCampaignListSize() {
		List<WebElement> eleLst = utils.getLocatorList("newCamHomePage.bulkSelectedCampaignList");
		int count = eleLst.size();
		logger.info("Selected campaign list size is " + count);
		TestListeners.extentTest.get().info("Selected campaign list size is " + count);
		return count;
	}

	public int getBulkSelectedCampaignSCount() {
		String text = utils.getLocator("newCamHomePage.bulkSelectedCampaignCount").getText();
		int count = Integer.parseInt(text);
		logger.info("Selected campaign count is " + count);
		TestListeners.extentTest.get().info("Selected campaign count is " + count);
		return count;
	}

	public void editCampaignFromNewCHP() {
		utils.getLocator("newCamHomePage.nextBtn").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("newCamHomePage.nextBtn").click();
		utils.waitTillPagePaceDone();
		utils.getLocator("newCamHomePage.approveAndSchedule").click();
		utils.acceptAlert(driver);
		logger.info("Clicked approve and schedule button.");
		TestListeners.extentTest.get().info("Clicked approve and schedule button.");
		utils.waitTillPagePaceDone();
	}

	public void challengeQualificationDrpDown(String qcItem) {
		utils.getLocator("newCamHomePage.challengeQualificationDrpDwn").click();
		List<WebElement> elements = utils.getLocatorList("newCamHomePage.challengeQualificationDrpDwnList");
		utils.selectListDrpDwnValue(elements, qcItem);
		// utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeQualificationDrpDwn"),
		// value);
		logger.info("selected the value -- " + qcItem);
		TestListeners.extentTest.get().info("selected the value -- " + qcItem);
	}

	public void campaignAdvertiseBlock() {
		try {
			utils.implicitWait(2);
			WebElement heading = utils.getLocator("newCamHomePage.createCampaignbtn");
			if (heading.isDisplayed()) {
				utils.waitTillNewCamsTableAppear();
			}
		} catch (Exception e) {
			logger.info("This is classic campaign page");
			TestListeners.extentTest.get().info("This is classic campaign page");
		}
		try {
			boolean flag = false;
			do {
				utils.implicitWait(2);
				WebElement ele = utils.getLocator("newCamHomePage.removeCampaignAdvertisement");
				flag = ele.isDisplayed();
				if (flag) {
					utils.getLocator("newCamHomePage.removeCampaignAdvertisement").click();
					logger.info("Advertisement appeared on the page and it is removed");
					TestListeners.extentTest.get().info("Advertisement appeared on the page and it is removed");
				}
				ele = utils.getLocator("newCamHomePage.removeCampaignAdvertisement");
				flag = ele.isDisplayed();
			} while (flag);
		} catch (Exception e) {
			logger.info("Advertisement not appeared on the page");
			TestListeners.extentTest.get().info("Advertisement not appeared on the page");
		}
		utils.implicitWait(60);
	}

	public void pnForChallengeProgress(String pn) {

		List<WebElement> lst = utils.getLocatorList("newCamHomePage.pnLst");
		for (int i = 0; i < lst.size(); i++) {
			lst.get(i).clear();
			lst.get(i).sendKeys(pn);
		}
		utils.getLocator("newCamHomePage.challengeProgressPnTxtArea").clear();
		utils.getLocator("newCamHomePage.challengeProgressPnTxtArea").sendKeys(pn);

	}

	public void challengeReachDrpDown(String value) {
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeReachDrpDown"), value);
		logger.info("selected the value -- " + value);
		TestListeners.extentTest.get().info("selected the value -- " + value);
	}

	public void segmentDrpDown(String segment) {
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignSegmentDrpDown"), segment);
		logger.info("selected the value -- " + segment);
		TestListeners.extentTest.get().info("selected the value -- " + segment);
	}

	public void pnForChallengeAvailable(String pn) {

		utils.scrollToElement(driver, utils.getLocator("newCamHomePage.challengeAvailablePN"));
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.challengeAvailablePN"));

		logger.info("selected challenge available PN");
		TestListeners.extentTest.get().info("selected challenge available PN");

		List<WebElement> lst = utils.getLocatorList("newCamHomePage.pnLst");
		for (int i = 0; i < lst.size(); i++) {
			lst.get(i).clear();
			lst.get(i).sendKeys(pn);
		}
		utils.getLocator("newCamHomePage.challengeAvailablePnTxtArea").clear();
		utils.getLocator("newCamHomePage.challengeAvailablePnTxtArea").sendKeys(pn);

	}

	public void challengeCampaignPN(String pn) {
		utils.getLocator("newCamHomePage.challengePnTxtArea").clear();
		utils.getLocator("newCamHomePage.challengePnTxtArea").sendKeys(pn);
		logger.info("Entered Challenge Campaign Push Notification");
		TestListeners.extentTest.get().info("Entered Challenge Campaign Push Notification");
		// click the next
		utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		logger.info("clicked on the next button");
		TestListeners.extentTest.get().info("clicked on the next button");
		utils.waitTillPagePaceDone();
	}

	public void searchCampaignNew(String camName) {
		utils.waitTillNewCamsTableAppear();
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.searchCamBox"), "search box");
		utils.getLocator("newCamHomePage.searchCamBox").clear();
		utils.getLocator("newCamHomePage.searchCamBox").sendKeys(camName);
		utils.longWaitInSeconds(2);
		logger.info("campaign searched on new cam home page");
		TestListeners.extentTest.get().pass("campaign searched on new cam home page");
	}

	public void clearSearchValueInSidePanel(String filterName) {
		String xpath = utils.getLocatorValue("newCamHomePage.sidePanelSearchBox").replace("{$search}", filterName);
		driver.findElement(By.xpath(xpath)).clear();
	}

	public boolean createNewCampaignTags(String tagName) {
		clickManageTag();
		utils.longWaitInSeconds(2);
		utils.getLocator("newCamHomePage.createTagsBtn").click();
		utils.getLocator("newCamHomePage.tagNameBox").clear();
		utils.getLocator("newCamHomePage.tagNameBox").sendKeys(tagName);
		logger.info("Entered tag name : " + tagName);
		TestListeners.extentTest.get().info("Entered tag name : " + tagName);
		utils.longWaitInSeconds(1);
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.createBtn"));
		utils.getLocator("newCamHomePage.createBtn").click();
		boolean flag = utils.getLocator("newCamHomePage.tagValidation").isDisplayed();
//		Assert.assertTrue(flag, "Campaign Tag  is not Created");
		utils.logit("Campaign Tag  is Created");
		return flag;
	}

	public void searchCampaignNCHP(String campaignName) {
		utils.waitTillPagePaceDone();
		utils.waitTillNewCamsTableAppear();
		utils.getLocator("newCamHomePage.searchCampaignNCHP").click();
		utils.getLocator("newCamHomePage.searchCampaignNCHP").clear();
		utils.getLocator("newCamHomePage.searchCampaignNCHP").sendKeys(campaignName);
		utils.getLocator("newCamHomePage.searchCampaignNCHP").sendKeys(Keys.ENTER);
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(2);
		String camName = utils.getLocator("newCamHomePage.searedCampaign").getText();
		Assert.assertEquals(camName, campaignName, "Campaign Not Found");
		logger.info("Campaign is Found with Campaign Name " + camName);
		TestListeners.extentTest.get().info("Campaign is Found with Campaign Name " + camName);
	}

	public void clickEllipsesOptions() {
		utils.getLocator("newCamHomePage.templateEllipsis").click();
		logger.info("Clicked on Ellipses Option");
		TestListeners.extentTest.get().info("Clicked on Ellipses Option");
	}

	public void clickOptionsInEllipsesButton(String option) {
		// option -> Duplicate, Delete, Activate, Tag, Archive, View Summary, Edit,
		// Audit Log
		String xpath = utils.getLocatorValue("newCamHomePage.tagButton").replace("$option", option);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.scrollToElement(driver, driver.findElement(By.xpath(xpath)));
		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(xpath)));
		logger.info("click on the " + option + " button from Ellipses Option ");
		TestListeners.extentTest.get().info("click on the " + option + " button from Ellipses Option ");
	}

	public void anyOrAndTagOrMessageFilter(String filter, String option) {
		String xpath = utils.getLocatorValue("newCamHomePage.anyOrAndTagOrMessageFilter").replace("$filter", filter)
				.replace("$option", option);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		utils.clickUsingActionsClass(driver.findElement(By.xpath(xpath)));
		utils.longWaitInSeconds(2);
		logger.info("Clicked on " + option + " for " + filter + " option");
		TestListeners.extentTest.get().info("Clicked on " + option + " for " + filter + " option");
	}

	public List<String> campaignList() {
		utils.waitTillPagePaceDone();
		List<WebElement> eleList = utils.getLocatorList("newCamHomePage.getCampaignNameList");
		List<String> campList = new ArrayList<String>();
		if (eleList.size() == 0) {
			logger.info("Campaigns is not present");
			TestListeners.extentTest.get().fail("Campaigns is not present");
			return campList;
		} else {

			for (int i = 0; i < eleList.size(); i++) {
				String text = eleList.get(i).getText();
				campList.add(text);
			}
			return campList;
		}
	}

	public void clickOnTemplate() {
		utils.getLocator("newCamHomePage.clickTemplate").click();
		logger.info("Template Page is opened from New Campaign Home Page");
		TestListeners.extentTest.get().pass("Template Page is opened from New Campaign Home Page");
	}

	public String checkCurrentCampaignPage() {
		String text = utils.getLocator("newCamHomePage.checkCurrentCampaignPage").getText();
		logger.info("In New Campaign Home Page, current Page is " + text);
		TestListeners.extentTest.get().info("In New Campaign Home Page, current Page is " + text);
		return text;
	}

	public void clickManageTag() {
		utils.longWaitInSeconds(2);
		utils.getLocator("newCamHomePage.manageTagsBtn").click();
		logger.info("Click ok manage Tag Button on New Campaign Home Page");
		TestListeners.extentTest.get().info("Click ok manage Tag Button on New Campaign Home Page");
	}

	public void searchTagInTagBox(String tagName) {
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.searchTagsBox"));
		utils.getLocator("newCamHomePage.searchTagsBox").click();
		utils.getLocator("newCamHomePage.searchTagsBox").clear();
		utils.getLocator("newCamHomePage.searchTagsBox").sendKeys(tagName);
		logger.info("Searched tag in tag box on Create Tag Page");
		TestListeners.extentTest.get().info("Searched tag in tag box on Create Tag Page");
	}

	public void closeCreateTagPopup() {
		utils.getLocator("newCamHomePage.closeCreateTagPopupBoxXapth").click();
		logger.info("Create tag popup closed");
		TestListeners.extentTest.get().info("Create tag popup closed");
		utils.longWaitInSeconds(1);
	}

	// wait till all camp get loaded
	public void waitTillCampCountIsDisplaying() {
		logger.info("waitTillCampCountIsDisplaying STARTED ");
		int counter = 0;
		boolean flag = false;
		// utils.waitTillPagePaceDone();
		while ((!flag) && (counter <= 10)) {
			// get the camp count
			WebElement campCountWele = utils.getLocator("newCamHomePage.campCountHeaderXpath");
			String campCounterText = campCountWele.getText();
			String numStr = campCounterText.replaceAll("[^0-9]", ""); // Remove non-numeric characters
			int num = Integer.parseInt(numStr);
			if (num > 0) {
				flag = true;
				logger.info(" ALl Campagins loaded and the camp count is  " + campCounterText);
				break;
			} else {
				logger.info(counter + " Campaign count is equal to ZERO and count is " + campCounterText);
				utils.longWaitInSeconds(2);
			}
			counter++;
		}
		logger.info("waitTillCampCountIsDisplaying END ");
		utils.longWaitInSeconds(1);

	}

	// expand the filter dropdown
	public void expandMoreOrLessFilterDropDown(String filterName, String toBeExpand) {
		String xpath = utils.getLocatorValue("newCamHomePage.filterExpandMoreOrLessXpath").replace("${FilterName}",
				filterName);
		WebElement expandFilterOPtionWEle = driver.findElement(By.xpath(xpath));
		utils.scrollToElement(driver, expandFilterOPtionWEle);
		String isFilterExpandText = expandFilterOPtionWEle.getText();

		switch (toBeExpand) {
		case "YES": {
			if (isFilterExpandText.equalsIgnoreCase("expand_more")) {
				utils.longWaitInSeconds(1);
				expandFilterOPtionWEle.click();
//							logger.info(filterName + " filter is expanded more ");
			} else if (isFilterExpandText.equalsIgnoreCase("expand_less")) {
				logger.info(filterName + " filter is already expanded more no need to click");
			}
			break;
		}
		case "NO": {
			if (isFilterExpandText.equalsIgnoreCase("expand_less")) {
				utils.longWaitInSeconds(1);
				expandFilterOPtionWEle.click();
//							logger.info(filterName + " filter is expanded more ");
			} else if (isFilterExpandText.equalsIgnoreCase("expand_more")) {
				logger.info(filterName + " filter is not expanded more no need to click");
			}
			break;
		}
		default: {
			logger.info("Invalid value for switch case ");
		}
		}

	}

	// get filter drop down values by giving filter name:
	public List<String> getFilterDropDownvalues(String filterName) {
		List<String> filterValuesLsit = new ArrayList<String>();
		utils.longWaitInSeconds(2);
		expandMoreOrLessFilterDropDown(filterName, "YES");
		String xpathFilterValues = utils.getLocatorValue("newCamHomePage.filterDropdownValuesXpath")
				.replace("${FilterName}", filterName);
		List<WebElement> wEleList = driver.findElements(By.xpath(xpathFilterValues));
		for (WebElement wEle : wEleList) {
			logger.info(wEle.getText());
			filterValuesLsit.add(wEle.getText());
		}
		return filterValuesLsit;
	}

	// select the filter dropdown value
	public void selectFilterDropDownValue(String filterName, String valueToSelect) {
		utils.longWaitInSeconds(2);
		String filterTextBoxXpath = utils.getLocatorValue("newCamHomePage.filterInputBox_Xpath")
				.replace("${filterName}", filterName);
		WebElement inputBoxWele = driver.findElement(By.xpath(filterTextBoxXpath));
		// driver.findElement(By.xpath("//div[label[text()='${filterName}
		// ']]/following-sibling::div//input")) ;
		utils.scrollToElement(driver, inputBoxWele);
		inputBoxWele.click();
		//inputBoxWele.clear();
		String	inputSearchBoxXpath = utils.getLocatorValue("newCamHomePage.filterInputBox_SearchBoxXpath").replace("${filterName}", filterName);
		WebElement inputSearchBoxWele = driver.findElement(By.xpath(inputSearchBoxXpath));
		inputSearchBoxWele.sendKeys(valueToSelect);
		utils.longWaitInSeconds(1);
		String xpath = utils.getLocatorValue("newCamHomePage.filteredSearchedResultOptionClickXpath")
				.replace("${filterName}", filterName);
		// expandMoreOrLessFilterDropDown(filterName , "YES");
		driver.findElement(By.xpath(xpath)).click();
		utils.longWaitInSeconds(1);

		utils.longWaitInSeconds(1);
	}

	// close side filter panel
	public void closeFilterSidePanel() {
		utils.getLocator("newCamHomePage.closeFilterSidePannelXpath").click();
		utils.longWaitInSeconds(1);
	}

	public void challengeWhatPageGift(String campName, String giftType, String giftReason, String giftTypeValue) {
		// enter the name of the campaign
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.challengeCampaignName"), "campaign name");
		utils.getLocator("newCamHomePage.challengeCampaignName").sendKeys(campName);
		logger.info("entered the campaign name -- " + campName);
		TestListeners.extentTest.get().info("entered the campaign name -- " + campName);

		// select the gift type
		utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignGiftType"), giftType);

		// gift reason
		utils.getLocator("newCamHomePage.challengeCampaignGiftReason").sendKeys(giftReason);

		switch (giftType) {
		case "Gift Redeemable":
			// select redeemable -> giftTypeValue = redeemable
			utils.selectDrpDwnValue(utils.getLocator("newCamHomePage.challengeCampaignRedeemableSelect"),
					giftTypeValue);
			break;
		case "Gift Points":
			// giftTypeValue = Gift Points/Visits
			utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.challengeCampaignGiftPoint"));
			utils.getLocator("newCamHomePage.challengeCampaignGiftPoint").sendKeys(giftTypeValue);
			break;
		default:
			logger.info("No gift type selected");
			TestListeners.extentTest.get().info("No gift type selected");
		}

		// click the next
		utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		utils.waitTillPagePaceDone();

		logger.info("clicked on next button");
		TestListeners.extentTest.get().info("clicked on next button");
	}

	public void uploadChallengeCompletedCampaignImage(String image) {
		String xpath = utils.getLocatorValue("newCamHomePage.challengeCampaignImageUpload").replace("${flag}", image);
		driver.findElement(By.xpath(xpath))
				.sendKeys(System.getProperty("user.dir") + "/resources/challengeCompleted.png");
		logger.info("uploaded the image --" + image);
		TestListeners.extentTest.get().info("uploaded the image --" + image);
	}

	public void uploadAllImagesInChallengeCompletedCampaign() {
		uploadChallengeCompletedCampaignImage("icon_completed");
		utils.longWaitInSeconds(1);
		uploadChallengeCampaignImage("image");
		utils.longWaitInSeconds(1);
		uploadChallengeCampaignImage("icon");

		logger.info("upload all the required images for the challenge campaign");
		TestListeners.extentTest.get().info("upload all the required images for the challenge campaign");
	}

	public String getChallengeImageSrc(String imageIconHeading) {
		String src = "";
		utils.implicitWait(3);
		int attempts = 0;
		while (attempts < 10) {
			try {
				String xpath = utils.getLocatorValue("newCamHomePage.getChallengeIconSrc").replace("$imageIconHeading",
						imageIconHeading);
				src = driver.findElement(By.xpath(xpath)).getAttribute("src");
				if (src != "") {
					logger.info("Challenge campaign " + imageIconHeading + " icon src is : " + src);
					TestListeners.extentTest.get()
							.info("Challenge campaign " + imageIconHeading + " icon src is : " + src);
					break;
				}
			} catch (Exception e) {
				logger.info("Challenge campaign icon src element is not present or status did not matched...");
				utils.refreshPage();
				utils.longWaitInSeconds(5);
			}
			attempts++;
		}
		utils.implicitWait(50);
		return src;
	}

	public void clickOnNextButton() {
		// click the next
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.challengeCampaignNext"));
//				utils.getLocator("newCamHomePage.challengeCampaignNext").click();
		logger.info("Clicked on Next Button");
		TestListeners.extentTest.get().info("Clicked on Next Button");
		utils.waitTillPagePaceDone();
	}

	public void selectSplitTestOnly() {
		utils.clickByJSExecutor(driver, utils.getLocator("newCamHomePage.splitTestOnly"));
		logger.info("checked the flag -- Split tests only");
		TestListeners.extentTest.get().info("checked the flag -- Split tests only");
	}

	public String deleteCampaignTag(String tagName) {
		// search and delete created tag
		utils.longWaitInSeconds(2);
		utils.waitTillPagePaceDone();
		utils.getLocator("newCamHomePage.searchTagsBox").clear();
		utils.getLocator("newCamHomePage.searchTagsBox").sendKeys(tagName);
		utils.getLocator("newCamHomePage.manageTagsdotsIcon").click();
		utils.getLocator("newCamHomePage.dotsIconDeleteOption").click();
		utils.waitTillElementToBeClickable(utils.getLocator("newCamHomePage.yesDeleteBtn"));
		utils.getLocator("newCamHomePage.yesDeleteBtn").click();
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.deleteTagMsg"), "Toast msg");
		String val = utils.getLocator("newCamHomePage.deleteTagMsg").getText();
		return val;
	}

	public String getexportReportMsg() {
		utils.waitTillVisibilityOfElement(utils.getLocator("newCamHomePage.exportReportMsg"), "Export Report Msg");
		String val = utils.getLocator("newCamHomePage.exportReportMsg").getText();
		return val;
	}

	public String getTagSelected(String option) {
		WebElement el = null;
		switch (option) {
		case "All":
			el = utils.getLocator("newCamHomePage.tagSelected");
			break;

		case "Hover":
			el = utils.getLocator("newCamHomePage.selectedTags");
			break;

		default:
			logger.info("Invalid value for switch case: " + option);
			return null;
		}
		String val = el.getText();
		return val;
	}
}
