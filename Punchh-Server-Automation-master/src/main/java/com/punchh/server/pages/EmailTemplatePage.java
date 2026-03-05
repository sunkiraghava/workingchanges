package com.punchh.server.pages;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class EmailTemplatePage {
	static Logger logger = LogManager.getLogger(EmailTemplatePage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	String ssoEmail, temp = "temp";
	String scheduleName;
	String oneDollerDiscount = "1";

	public EmailTemplatePage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(this.driver);
		selUtils = new SeleniumUtilities(driver);
	}

	public void createNewEmailTemplate(String emailTemplateName) {
		selUtils.implicitWait(50);
		utils.waitTillPagePaceDone();
		selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.newtemplateButton"));

		utils.getLocator("emailTemplatePage.emailTemplateHeading").isDisplayed();
		utils.getLocator("emailTemplatePage.newtemplateButton").isDisplayed();
		utils.getLocator("emailTemplatePage.newtemplateButton").isEnabled();
		utils.getLocator("emailTemplatePage.newtemplateButton").click();

		utils.longWaitInSeconds(5);
		selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.templateNameTextbox"));

		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		utils.longWaitInSeconds(5);
		selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.emailTemplateSideBarContent"));
		// WebElement contentWele =
		// utils.getLocator("emailTemplatePage.emailTemplateSideBarContent");
//		utils.waitTillPagePaceDone();
		utils.getLocator("emailTemplatePage.emailTemplateSideBarContent").isDisplayed();
		driver.switchTo().defaultContent();

		utils.getLocator("emailTemplatePage.templateNameTextbox").isDisplayed();
		utils.getLocator("emailTemplatePage.templateNameTextbox").clear();
		utils.getLocator("emailTemplatePage.templateNameTextbox").sendKeys(emailTemplateName);
		utils.getLocator("emailTemplatePage.templateNameCheckbox").click();
		logger.info("Email template name is set as: " + emailTemplateName);
		TestListeners.extentTest.get().info("Email template name is set as: " + emailTemplateName);
	}

	public void addMediaToTemplate() throws InterruptedException {
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.rowTabLabel").click();
		selUtils.dragAndDropActions(utils.getLocator("emailTemplatePage.rowDropAndDrag"),
				utils.getLocator("emailTemplatePage.dropAndDragHere"));
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.contentTabLabel").click();
		utils.longWaitInSeconds(2);
		WebElement templateIconXpathWele = utils.getLocator("emailTemplatePage.mediaLabel");
		List<WebElement> targetDestinationPathWeleList = utils.getLocatorList("emailTemplatePage.targetPathOnCanvas");
		Actions action = new Actions(driver);
		action.clickAndHold(templateIconXpathWele).moveToElement(targetDestinationPathWeleList.get(0))
				.pause(Duration.ofSeconds(1)).release().build().perform();
		utils.longWaitInSeconds(2);
		// utils.getLocator("emailTemplatePage.browseButton").isDisplayed();
		WebElement el = utils.getLocator("emailTemplatePage.browseButton");
		utils.waitTillElementToBeClickable(el);
		el.click();

//		dragAndDropTemplateIconOnCanvas("Media");
		driver.switchTo().parentFrame();
		utils.longWaitInSeconds(2);
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.testImageLable"));
		driver.switchTo().frame(0);
		utils.getLocator("emailTemplatePage.imageSourceLabel").isDisplayed();
		logger.info("Added media to Email template ");
		TestListeners.extentTest.get().info("Added media to Email template ");

	}

	public void addContentToTemplate() throws InterruptedException {
		utils.longWaitInSeconds(2);
		dragAndDropTemplateIconOnCanvas("Text");
		utils.getLocator("emailTemplatePage.textTextbox").isDisplayed();
		// utils.getLocator("emailTemplatePage.textTextbox").clear();
		selUtils.jsClick(utils.getLocator("emailTemplatePage.textTextbox"));
		driver.switchTo().parentFrame();
		logger.info("Added content to Email template ");
		TestListeners.extentTest.get().info("Added content to Email template ");
	}

	public void addButtonToTemplate() throws InterruptedException {
		dragAndDropTemplateIconOnCanvas("Button");
		// Thread.sleep(2000);
		utils.longWaitInSeconds(3);
		selUtils.waitTillVisibilityOfElement(utils.getLocator("emailTemplatePage.createdButtonLabel"),
				"Email Template Button option");
		boolean flag = utils.getLocator("emailTemplatePage.createdButtonLabel").isDisplayed();
		Assert.assertTrue(flag);
		driver.switchTo().parentFrame();
		logger.info("Added button to Email template ");
		TestListeners.extentTest.get().info("Added button to Email template ");
	}

	public void addSocialToTemplate() throws InterruptedException {
		dragAndDropTemplateIconOnCanvas("Social");
		boolean flag = utils.getLocator("emailTemplatePage.createdButtonLabel").isDisplayed();
		Assert.assertTrue(flag);
		driver.switchTo().parentFrame();
		logger.info("Added social media to Email template ");
		TestListeners.extentTest.get().info("Added social media to Email template ");
	}

	public void addDividerToTemplate() throws InterruptedException {
		dragAndDropTemplateIconOnCanvas("Divider");
		utils.getLocator("emailTemplatePage.dividerDisplay").isDisplayed();
		driver.switchTo().parentFrame();
		logger.info("Added Divider to Email template ");
		TestListeners.extentTest.get().info("Added Divider to Email template ");
	}

	public void addHTMLToTemplate() throws InterruptedException {
		dragAndDropTemplateIconOnCanvas("HTML");
		driver.switchTo().parentFrame();
		logger.info("Added HTML to Email template ");
		TestListeners.extentTest.get().info("Added HTML to Email template ");
	}

	public void addSubjectToTemplate() throws InterruptedException {
		driver.switchTo().parentFrame();
//		selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.subjectTab"));
//		Thread.sleep(4000);
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.subjectTab"));
//		utils.getLocator("emailTemplatePage.subjectTab").click();
		logger.info("Click Subject tab of Email template ");
		TestListeners.extentTest.get().info("Click Subject tab of Email template ");
		utils.getLocator("emailTemplatePage.clickNewSubjectLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.clickNewSubjectLabel").click();
		utils.getLocator("emailTemplatePage.enterSubject").isDisplayed();
		utils.getLocator("emailTemplatePage.enterSubject").clear();
		utils.getLocator("emailTemplatePage.enterSubject").sendKeys("Hello, This is the Subject Tab");
		utils.getLocator("emailTemplatePage.saveSubject").click();
		logger.info("Added Subject to Email template ");
		TestListeners.extentTest.get().info("Added Subject to Email template ");
	}

	public void saveAndPublish(String emailTemplateName) {
		try {
			driver.switchTo().parentFrame();
//			driver.switchTo().frame(0);
			utils.getLocator("emailTemplatePage.savePublishButton").isDisplayed();
			utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.savePublishButton"));
//			selUtils.jsClick(utils.getLocator("emailTemplatePage.savePublishButton"));
			driver.findElement(By
					.xpath(utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName)))
					.isDisplayed();
			logger.info("Template is created successfully");
			TestListeners.extentTest.get().pass("Template is created successfully" + emailTemplateName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void saveAndPublish1(String emailTemplateName) {
		try {
//			Thread.sleep(4000);

			selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.savePublishButton"));
			utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.savePublishButton"));
//			selUtils.jsClick(utils.getLocator("emailTemplatePage.savePublishButton"));
			logger.info("Template is created successfully");
			TestListeners.extentTest.get().pass("Template is created successfully" + emailTemplateName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void editTemplate(String emailTemplateName) throws InterruptedException {
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(7);
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.longWaitInSeconds(6);
//		utils.waitTillVisibilityOfElement(utils.getLocator("emailTemplatePage.templateCardImage"), "Edit Email Template Page");
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		utils.getLocator("emailTemplatePage.editTemplateButton").isDisplayed();
		utils.getLocator("emailTemplatePage.editTemplateButton").click();
		utils.getLocator("emailTemplatePage.yesEditButton").isDisplayed();
		utils.getLocator("emailTemplatePage.yesEditButton").click();
		logger.info("Clicked on edit template");
		TestListeners.extentTest.get().pass("Clicked on edit template");
		utils.longWaitInSeconds(5);
	}

	public void editTemplateSaveAndDraft(String emailTemplateName) throws InterruptedException {
		utils.longWaitInSeconds(5);
		utils.implicitWait(25);
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		utils.longWaitInSeconds(5);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		utils.getLocator("emailTemplatePage.editTemplateButton").isDisplayed();
		utils.getLocator("emailTemplatePage.editTemplateButton").click();
		logger.info("Clicked on edit template");
		TestListeners.extentTest.get().pass("Clicked on edit template");
		utils.implicitWait(50);
	}

	public void addVideoToTemplate(String youtubeVideoLink) throws InterruptedException {
		selUtils.longWait(5000);
		driver.switchTo().parentFrame();
		driver.switchTo().frame(0);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.rowTabLabel").click();
		selUtils.dragAndDropActions(utils.getLocator("emailTemplatePage.rowDropAndDrag"),
				utils.getLocator("emailTemplatePage.dropAndDragHere"));
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.contentTabLabel").click();
		selUtils.longWait(2000);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath")
				.replace("${templateIconName}", "Video");
		WebElement templateIconXpathWele = driver.findElement(By.xpath(templateIconXpath));
		List<WebElement> targetDestinationPathWeleList = utils.getLocatorList("emailTemplatePage.targetPathOnCanvas");
		Actions action = new Actions(driver);
		action.clickAndHold(templateIconXpathWele).moveToElement(targetDestinationPathWeleList.get(0))
				.pause(Duration.ofSeconds(1)).release().build().perform();
		selUtils.longWait(2000);
		utils.getLocator("emailTemplatePage.videoPlaceholderLabel").isDisplayed();
		selUtils.jsClick(utils.getLocator("emailTemplatePage.videoPlaceholderLabel"));
		utils.getLocator("emailTemplatePage.videoUrlTextbox").isDisplayed();
		selUtils.longWait(3000);
		utils.getLocator("emailTemplatePage.videoUrlTextbox").clear();
		utils.getLocator("emailTemplatePage.videoUrlTextbox").sendKeys(youtubeVideoLink);
		utils.getLocator("emailTemplatePage.videoUrlTextbox").sendKeys(Keys.ENTER);
//		utils.getLocator("emailTemplatePage.videoImage").isDisplayed();
		driver.switchTo().parentFrame();
		TestListeners.extentTest.get().pass("Video is added to template");
		logger.info("Video is added to template");
	}

	public void deleteTemplate(String emailTemplateName) throws InterruptedException {
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.waitTillVisibilityOfElement(utils.getLocator("emailTemplatePage.templateCardImage"),
				"Delete Email Template");
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		clickonDeleteTemplate(emailTemplateName);
		utils.longWaitInSeconds(3);
	}

	public void deleteDraftTemplate(String emailTemplateName) {
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.getLocator("emailTemplatePage.draftTemplateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.draftTemplateCardImage"));
		clickonDeleteTemplate(emailTemplateName);
	}

	private void clickonDeleteTemplate(String emailTemplateName) {
		driver.findElement(
				By.xpath(utils.getLocatorValue("emailTemplatePage.trashLabel").replace("temp", emailTemplateName)))
				.isDisplayed();
		utils.longWaitInSeconds(2);
		String trashLabelXpath = utils.getLocatorValue("emailTemplatePage.trashLabel").replace("temp", emailTemplateName);
		WebElement trashLabelButton = utils.getXpathWebElements(By.xpath(trashLabelXpath));
		utils.StaleElementclick(driver, trashLabelButton);
		selUtils.waitTillVisibilityOfElement(utils.getLocator("emailTemplatePage.yesDeleteButton"),
				"yes delete is visible");
		utils.getLocator("emailTemplatePage.yesDeleteButton").isDisplayed();
		utils.getLocator("emailTemplatePage.yesDeleteButton").click();
		logger.error("Successful to delete templete " + emailTemplateName);
		TestListeners.extentTest.get().pass("Successful to deleted templete" + emailTemplateName);
	}

	public void createDuplicateTemplate(String emailTemplateName) {
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		// driver.findElement(By.xpath(xpath)).click();
		utils.waitTillVisibilityOfElement(utils.getLocator("emailTemplatePage.templateCardImage"), "Card Image");
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		driver.findElement(
				By.xpath(utils.getLocatorValue("emailTemplatePage.duplicateLabel").replace("temp", emailTemplateName)))
				.isDisplayed();
		driver.findElement(
				By.xpath(utils.getLocatorValue("emailTemplatePage.duplicateLabel").replace("temp", emailTemplateName)))
				.click();
		String emailTemplateCopy = "Copy of " + emailTemplateName;
		driver.findElement(
				By.xpath(utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateCopy)))
				.isDisplayed();
		driver.findElement(By.xpath(
				utils.getLocatorValue("emailTemplatePage.draftStatusRibbonLabel").replace("temp", emailTemplateCopy)))
				.isDisplayed();
		logger.info("Duplicate template is created successfully: " + emailTemplateName);
		TestListeners.extentTest.get().pass("Duplicate template is created successfully: " + emailTemplateName);
	}

	public void saveAsDraft(String emailTemplateName) throws InterruptedException {

		driver.switchTo().parentFrame();
//		driver.switchTo().frame(0);
		utils.getLocator("emailTemplatePage.saveDraftButton").isDisplayed();
//		Thread.sleep(2000);
		selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.saveDraftButton"));
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.saveDraftButton"));
//		utils.getLocator("emailTemplatePage.saveDraftButton").click();
		utils.getLocator("emailTemplatePage.saveDraftSave").click();
		logger.info(emailTemplateName + " template draft created successfully");
		TestListeners.extentTest.get().pass(emailTemplateName + " template draft created successfully");
	}

	public void saveAsDraft1(String emailTemplateName) throws InterruptedException {
		utils.getLocator("emailTemplatePage.saveDraftButton").isDisplayed();
//		Thread.sleep(3000);
		selUtils.waitTillElementToBeClickable(utils.getLocator("emailTemplatePage.saveDraftButton"));
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.saveDraftButton"));
//		utils.getLocator("emailTemplatePage.saveDraftButton").click();
		logger.info(emailTemplateName + " template draft created successfully");
		TestListeners.extentTest.get().pass(emailTemplateName + " template draft created successfully");
	}

	public void shareWithFranchisees(String emailTemplateName) throws InterruptedException {
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		utils.getLocator("emailTemplatePage.multiSelectButton").isDisplayed();
		utils.getLocator("emailTemplatePage.multiSelectButton").click();
		utils.getLocator("emailTemplatePage.moreOptionButton").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.moreOptionButton"));
//		utils.getLocator("emailTemplatePage.moreOptionButton").click();
		utils.getLocator("emailTemplatePage.shareWithFranchisees").isDisplayed();
		utils.getLocator("emailTemplatePage.shareWithFranchisees").click();
		utils.getLocator("emailTemplatePage.yesShareButton").isDisplayed();
		utils.getLocator("emailTemplatePage.yesShareButton").click();
		logger.info("Click to Share Franchisees ");
		TestListeners.extentTest.get().pass("Click to Share Franchisees");
	}

	public void clickOnNewFolder() {
		utils.getLocator("emailTemplatePage.clickNewFolder").isDisplayed();
		utils.getLocator("emailTemplatePage.clickNewFolder").click();
		logger.info("Click on new folder ");
		TestListeners.extentTest.get().pass("Click on new folder ");
	}

	public void availableForFranchise() {
		driver.switchTo().parentFrame();
		utils.getLocator("emailTemplatePage.availableForFranchise").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.availableForFranchise"));
		logger.info(" Email template is available for Franchise use");
		TestListeners.extentTest.get().pass("Email template is available for Franchise use");
	}

	public void validateFranchiseTemplate(String emailTemplateName) throws InterruptedException {
		utils.getLocator("emailTemplatePage.clickOnSharedWithFranchise").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.clickOnSharedWithFranchise"));
		utils.getLocator("emailTemplatePage.searchEmailTemplate").isDisplayed();
		utils.getLocator("emailTemplatePage.searchEmailTemplate").clear();
		utils.getLocator("emailTemplatePage.searchEmailTemplate").sendKeys(emailTemplateName);
		String xpath = utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName);
		driver.findElement(By.xpath(xpath)).isDisplayed();
		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		logger.info("Email Template Shared with Franchisees is Displayed");
		TestListeners.extentTest.get().pass("Email Template Shared with Franchisees is Displayed");
	}

	public void saveAndPublishFranchise(String emailTemplateName) {
		try {
			driver.switchTo().parentFrame();
//			driver.switchTo().frame(0);
			utils.getLocator("emailTemplatePage.saveAndPublishFranchise").isDisplayed();
			utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.saveAndPublishFranchise"));
//			selUtils.jsClick(utils.getLocator("emailTemplatePage.savePublishButton"));
			driver.findElement(By
					.xpath(utils.getLocatorValue("emailTemplatePage.templateLabel").replace("temp", emailTemplateName)))
					.isDisplayed();
			logger.info("Template is created successfully");
			TestListeners.extentTest.get().pass("Template is created successfully" + emailTemplateName);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void dragAndDropTemplateIconOnCanvas(String templateIconName) throws InterruptedException {
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.rowTabLabel").click();
		selUtils.dragAndDropActions(utils.getLocator("emailTemplatePage.rowDropAndDrag"),
				utils.getLocator("emailTemplatePage.dropAndDragHere"));
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		utils.getLocator("emailTemplatePage.contentTabLabel").click();
		selUtils.longWait(2000);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath")
				.replace("${templateIconName}", templateIconName);
		WebElement templateIconXpathWele = driver.findElement(By.xpath(templateIconXpath));
		List<WebElement> targetDestinationPathWeleList = utils.getLocatorList("emailTemplatePage.targetPathOnCanvas");
		Actions action = new Actions(driver);
		action.clickAndHold(templateIconXpathWele).moveToElement(targetDestinationPathWeleList.get(0))
				.pause(Duration.ofSeconds(1)).release().build().perform();
		selUtils.longWait(2000);
		logger.info("Added " + templateIconName + " to Email template ");
		TestListeners.extentTest.get().info("Added " + templateIconName + " to Email template ");
//		driver.switchTo().defaultContent();
	}

	public void searchAndSelectEmailTemplate(String emailSubject) {

		// utils.getLocator("emailTemplatePage.searchEmailTemplate").sendKeys("Dynamic");

		utils.getLocator("emailTemplatePage.templateCardImage").isDisplayed();
		selUtils.mouseHoverOverElement(utils.getLocator("emailTemplatePage.templateCardImage"));
		utils.getLocator("campaignsBetaPage.chooseTemplate").click();
		utils.longWaitInSeconds(15);
		selUtils.waitTillElementToBeClickable(utils.getLocator("signupCampaignsPage.saveEmailTemplate"));
		utils.clickByJSExecutor(driver, utils.getLocator("signupCampaignsPage.saveEmailTemplate"));
		utils.longWaitInSeconds(5);
		utils.waitTillSpinnerDisappear();

		// utils.checkElementPresent(utils.getLocator("campaignSplitPage.addedEmailTemplate"));
	}

}