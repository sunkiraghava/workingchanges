package com.punchh.server.pages;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ContentLibraryPage {
	static Logger logger = LogManager.getLogger(ContentLibraryPage.class);
	private WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public ContentLibraryPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	public boolean createFolderAndVerifySuccessMessage() {
		WebElement newCategory = utils.getLocator("contentLibraryPage.createNewFolder");
		utils.waitTillInVisibilityOfElement(newCategory, "New Category");
		logger.info("Yes Folder displayed ,Folder created and Success message displayed");
		newCategory.click();
		return utils.getLocator("contentLibraryPage.successMessageAfterFolderCreate").isDisplayed();
	}

	public void editTheNewCategoryFolder(String enterTheFolderName) {
		WebElement createNewCategory = utils.getLocator("contentLibraryPage.createdNewCategory");
		createNewCategory.click();
		WebElement editButton = utils.getLocator("contentLibraryPage.folderEditButton");
		utils.clickByJSExecutor(driver, editButton);
		// editButton.click();
		WebElement folderRenameEle = utils.getLocator("contentLibraryPage.folderRenameField");
		folderRenameEle.clear();
		folderRenameEle.sendKeys(enterTheFolderName);
		WebElement saveButton = utils.getLocator("contentLibraryPage.folderSaveButton");
		saveButton.click();
	}

	public String verifyTheTextPresentOnPopUp() {
		// driver.navigate().refresh();
		String message = "";
		utils.getLocator("contentLibraryPage.newSavedRowButton").isEnabled();
		logger.info("Yest button is enable");
		utils.longwait(3000);
		WebElement wb = utils.getLocator("contentLibraryPage.newSavedRowButton");
		utils.clickByJSExecutor(driver, wb);
		utils.longwait(3000);
		logger.info("Clicked on new saved row button");
		// message=utils.getLocator("contentLibraryPage.textOnPopUp").getText();
		// logger.info("Text message found");
		return message;
	}

	public void clickOnPopUpOkayButton() {
		utils.clickByJSExecutor(driver, utils.getLocator("contentLibraryPage.popUpOkayButton"));
		logger.info("Confirmation pop up displayed and confirmed");
	}

	public int verifyLink(String enterTheLinkToVerify) throws IOException {
		String linkXpath = utils.getLocatorValue("contentLibraryPage.links");
		linkXpath = linkXpath.replace("${links}", enterTheLinkToVerify);
		WebElement links = driver.findElement(By.xpath(linkXpath));
		String urlLink = links.getAttribute("href");
		logger.info("Link found =>" + urlLink);
		URL url = new URL(urlLink);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setConnectTimeout(3000);
		httpConn.connect();
		int responseCode;
		responseCode = httpConn.getResponseCode();
		logger.info("Status link verified");
		TestListeners.extentTest.get().info("Status link verified");
		return responseCode;
	}

	public void createEditRenameSaveFolderForRow(String enterTheFolderName) {
		try {
			WebElement renamedFolder = null;
			try {
				renamedFolder = utils.getLocator("contentLibraryPage.renamedFolder");
			} catch (Exception e) {
				logger.warn("Renamed folder not found immediately: " + e.getMessage());
			}
			if (renamedFolder != null && renamedFolder.isDisplayed()) {
				logger.info("Renamed folder found — proceeding to edit");
				renamedFolder.click();
			} else {
				logger.info("Renamed folder not found — attempting to create new folder");
				WebElement newCategory = null;
				try {
					newCategory = utils.getLocator("contentLibraryPage.createNewFolder");
				} catch (Exception e) {
					logger.error("Unable to locate 'Create New Folder' button: " + e.getMessage());
				}
				if (newCategory != null && newCategory.isDisplayed()) {
					newCategory.click();
					logger.info("Clicked on 'Create New Folder'");

					try {
						renamedFolder = utils.getLocator("contentLibraryPage.renamedFolder");
						if (renamedFolder != null && renamedFolder.isDisplayed()) {
							utils.clickByJSExecutor(driver, renamedFolder);
							logger.info("Clicked on newly created folder");
						} else {
							logger.error("Newly created folder not found after creation");
							return;
						}
					} catch (Exception e) {
						logger.error("Exception when clicking newly created folder: " + e.getMessage());
						return;
					}
				} else {
					logger.error("'Create New Folder' button is not available");
					return;
				}
			}
			// Proceed with edit, rename, save
			WebElement editButton = utils.getLocator("contentLibraryPage.editButton");
			utils.waitTillVisibilityOfElement(editButton, "Rename Folder");
			utils.clickByJSExecutor(driver, editButton);
			logger.info("Clicked on edit button");
			WebElement folderRenameTextField = utils.getLocator("contentLibraryPage.folderRenameField");
			folderRenameTextField.clear();
			folderRenameTextField.sendKeys(enterTheFolderName);
			logger.info("Entered folder name: " + enterTheFolderName);
			WebElement saveButton = utils.getLocator("contentLibraryPage.folderSaveButton");
			utils.waitTillVisibilityOfElement(saveButton, "Save Button");
			utils.clickByJSExecutor(driver, saveButton);
			logger.info("Clicked on save button");
		} catch (Exception e) {
			logger.error("Unhandled exception in createEditRenameSaveFolderForRow: " + e.getMessage(), e);
		}
	}

	public void clickOnNewSavedRowButton() {
		WebElement newSavedRowButton = utils.getLocator("contentLibraryPage.newSavedRowButton");
		utils.waitTillVisibilityOfElement(newSavedRowButton, "New Saved Row Button");
		newSavedRowButton.click();
		logger.info("Clicked on new row saved button");
	}

	public void dragAndDropBlockOnRow(String enterBlockLabel) throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on tabLabel");
		// utils.implicitWait(5);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath");
		templateIconXpath = templateIconXpath.replace("${templateIconName}", enterBlockLabel);
		WebElement buttonBlock = driver.findElement(By.xpath(templateIconXpath));
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.targetPathOnCanvas");
		Actions action = new Actions(driver);
		action.clickAndHold(buttonBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added " + enterBlockLabel + " to Row");
		TestListeners.extentTest.get().info("Added " + enterBlockLabel + " to row ");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropBlockOnSplit1(String enterBlockLabel) throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on tabLabel");
		// utils.implicitWait(5);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath");
		templateIconXpath = templateIconXpath.replace("${templateIconName}", enterBlockLabel);
		WebElement buttonBlock = driver.findElement(By.xpath(templateIconXpath));
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.rowSlit1Of6");
		Actions action = new Actions(driver);
		action.clickAndHold(buttonBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added " + enterBlockLabel + " to Row");
		TestListeners.extentTest.get().info("Added " + enterBlockLabel + " to row ");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropBlockOnSplit2(String enterBlockLabel) throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on tabLabel");
		// utils.implicitWait(5);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath");
		templateIconXpath = templateIconXpath.replace("${templateIconName}", enterBlockLabel);
		WebElement buttonBlock = driver.findElement(By.xpath(templateIconXpath));
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.rowSlit2Of6");
		Actions action = new Actions(driver);
		action.clickAndHold(buttonBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added " + enterBlockLabel + " to Row");
		TestListeners.extentTest.get().info("Added " + enterBlockLabel + " to row ");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropBlockOnSplit3(String enterBlockLabel) throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on tabLabel");
		// utils.implicitWait(5);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath");
		templateIconXpath = templateIconXpath.replace("${templateIconName}", enterBlockLabel);
		WebElement buttonBlock = driver.findElement(By.xpath(templateIconXpath));
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.rowSlit3Of6");
		Actions action = new Actions(driver);
		action.clickAndHold(buttonBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added " + enterBlockLabel + " to Row");
		TestListeners.extentTest.get().info("Added " + enterBlockLabel + " to row ");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropBlockOnSplit4(String enterBlockLabel) throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on tabLabel");
		// utils.implicitWait(5);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath");
		templateIconXpath = templateIconXpath.replace("${templateIconName}", enterBlockLabel);
		WebElement buttonBlock = driver.findElement(By.xpath(templateIconXpath));
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.rowSlit4Of6");
		Actions action = new Actions(driver);
		action.clickAndHold(buttonBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added " + enterBlockLabel + " to Row");
		TestListeners.extentTest.get().info("Added " + enterBlockLabel + " to row ");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropBlockOnSplit5(String enterBlockLabel) throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on tabLabel");
		// utils.implicitWait(5);
		String templateIconXpath = utils.getLocatorValue("emailTemplatePage.templateIconXpath");
		templateIconXpath = templateIconXpath.replace("${templateIconName}", enterBlockLabel);
		WebElement socialBlock = driver.findElement(By.xpath(templateIconXpath));
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.rowSlit5Of6");
		Actions action = new Actions(driver);
		action.clickAndHold(socialBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added " + enterBlockLabel + " to Row");
		TestListeners.extentTest.get().info("Added " + enterBlockLabel + " to row ");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropBlockOnSplit6() throws InterruptedException {
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row tab label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on content tab Label");
		WebElement savedRowBlock = utils.getLocator("contentLibraryPage.savedRows");
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.rowSlit6Of6");
		Actions action = new Actions(driver);
		action.clickAndHold(savedRowBlock).moveToElement(targetDestinationPathWeleList).pause(Duration.ofSeconds(2))
				.release().build().perform();
		logger.info("Added saved Row to splitted row");
		TestListeners.extentTest.get().info("Added saved Row to splitted row");
		// driver.switchTo().defaultContent();
	}

	public void dragAndDropSavedRowBlockOnRow() throws InterruptedException {
		logger.info("Action started to drag and drop saved row block for splited row");
		driver.switchTo().defaultContent();
		utils.implicitWait(50);
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		utils.getLocator("emailTemplatePage.rowTabLabel").isDisplayed();
		utils.clickByJSExecutor(driver, utils.getLocator("emailTemplatePage.rowTabLabel"));
		logger.info("Clicked on row tab label");
		utils.getLocator("emailTemplatePage.contentTabLabel").isDisplayed();
		WebElement tabLabel = utils.getLocator("emailTemplatePage.contentTabLabel");
		utils.clickByJSExecutor(driver, tabLabel);
		logger.info("Clicked on content tabLabel");
		// utils.implicitWait(5);
		WebElement templateIconXpathWele = utils.getLocator("contentLibraryPage.savedRows");
		utils.waitTillVisibilityOfElement(templateIconXpathWele, "Saved Row Block");
		WebElement targetDestinationPathWeleList = utils.getLocator("emailTemplatePage.targetPathOnCanvas");
		utils.waitTillVisibilityOfElement(targetDestinationPathWeleList, "Target Path");
		Actions action = new Actions(driver);
		action.clickAndHold(templateIconXpathWele).moveToElement(targetDestinationPathWeleList)
				.pause(Duration.ofSeconds(2)).release().build().perform();
		logger.info("Added savedRow to Email template ");
		TestListeners.extentTest.get().info("Added savedRow to Email template ");
		// driver.switchTo().defaultContent();
	}

	public void addButtonToRow() throws InterruptedException {
		dragAndDropBlockOnRow("Button");
		utils.longWaitInSeconds(3);
		selUtils.waitTillVisibilityOfElement(utils.getLocator("emailTemplatePage.createdButtonLabel"), "button block");
		driver.switchTo().parentFrame();
		logger.info("Added button to row");
		TestListeners.extentTest.get().info("Added button to row");
		driver.switchTo().defaultContent();
	}

	public void addSavedRowBlock() throws InterruptedException {
		utils.implicitWait(50);
		dragAndDropSavedRowBlockOnRow();
		driver.switchTo().parentFrame();
		logger.info("Added saved row block added to email template");
		TestListeners.extentTest.get().info("Added saved row block added to email template");
		driver.switchTo().defaultContent();
	}

	public void clickOutSideOfRowElement() {
		driver.switchTo().frame(0);
		WebElement element = utils.getLocator("contentLibraryPage.outSideOFRow");
		int x = utils.getLocator("contentLibraryPage.outSideOFRow").getLocation().getX();
		logger.info("X co-ordinate = " + x);
		int y = utils.getLocator("contentLibraryPage.outSideOFRow").getLocation().getY();
		logger.info("Y co-ordinate = " + y);
		selUtils.clickByOffset(element, x + 10, y + 10);
		logger.info("Clicked out side of row web element");
	}

	public void clickOutSideORowAfterEdit() {
		// driver.switchTo().frame(0);
		WebElement element = utils.getLocator("contentLibraryPage.outSideOFRow");
		int x = utils.getLocator("contentLibraryPage.outSideOFRow").getLocation().getX();
		logger.info("X co-ordinate after edit=" + x);
		int y = utils.getLocator("contentLibraryPage.outSideOFRow").getLocation().getY();
		logger.info("Y co-ordinate after edit=" + y);
		selUtils.clickByOffset(element, x + 10, y + 10);
		logger.info("Clicked out side of row element after edit");
	}

	public void clickOnSaveRow() {
		utils.getLocator("contentLibraryPage.rowSaveButtonOnRow").click();
		logger.info("Clicked on save button on row");
		driver.switchTo().defaultContent();
	}

	public void fillUpTheRowsaveFormAndsave(String enterTheLabelName, String enterTheLabelTag, String testDataRowName,
			String testDataTagName) {
		utils.longWaitInSeconds(3);
		String labelXathName = utils.getLocatorValue("contentLibraryPage.fillupTheRowForm");
		labelXathName = labelXathName.replace("${xyz}", enterTheLabelName);
		WebElement label = driver.findElement(By.xpath(labelXathName));
		label.sendKeys(Keys.CLEAR);
		label.sendKeys(testDataRowName);
		logger.info("Row name entered on row fill up form");
		String labelXathTab = utils.getLocatorValue("contentLibraryPage.fillupTheRowForm");
		labelXathTab = labelXathTab.replace("${xyz}", enterTheLabelTag);
		WebElement label1 = driver.findElement(By.xpath(labelXathTab));
		label1.sendKeys(Keys.CLEAR);
		label1.sendKeys(testDataTagName);
		label1.sendKeys(Keys.TAB);
		logger.info("Tag name entered into row form fill up");
		WebElement savebutton = utils.getLocator("contentLibraryPage.saveRowForm");
		savebutton.isDisplayed();
		logger.info("Yes save button displayed on row fill up form");
		utils.clickByJSExecutor(driver, savebutton);
		logger.info("Clicked on Save button on row fill up form and form fill up done");
		TestListeners.extentTest.get().info("Clicked on Save button on row fill up form and form fill up done");
		utils.longWaitInSeconds(3);
	}

	public void backToContentLibrary() {
		WebElement element = utils.getLocator("contentLibraryPage.backToContentLibraryButton");
		element.isDisplayed();
		logger.info("yes back to content library");
		utils.clickByJSExecutor(driver, element);
		logger.info("Back to content library button clicked");
		WebElement yesback = utils.getLocator("contentLibraryPage.yesBackToContentLibrary");
		yesback.isDisplayed();
		logger.info("Yes back to contetlibrary confirmatation button displayed");
		utils.clickByJSExecutor(driver, yesback);
		logger.info("Clicked on Yes back to contetlibrary confirmatation button");
		utils.longWaitInSeconds(3);
		utils.waitTillPagePaceDone();
	}

	public void attachTheSavedRowToEmailTemplate(String EnterRowNameToSearch, String clickOnTheSeachedRow) {
		utils.implicitWait(50);
		WebElement iframe = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(iframe);
		String browseButtonXpath = utils.getLocatorValue("emailTemplatePage.savedRowBlockBrowseButton");
		WebElement browseButton = utils.getXpathWebElements(By.xpath(browseButtonXpath));
		utils.waitTillVisibilityOfElementLocated(By.xpath(browseButtonXpath), "Browse Button", 60);
		browseButton.click();
		logger.info("saved row browse button clicked");
		driver.switchTo().defaultContent();
		driver.switchTo().frame(iframe);
		driver.switchTo().parentFrame();
		utils.longwait(3000);
		WebElement searchField = utils.getLocator("contentLibraryPage.savedRowSearchField");
		searchField.sendKeys(EnterRowNameToSearch);
		logger.info("Row name entered into row search field under email template");
		String searchedRowXpath = utils.getLocatorValue("contentLibraryPage.searchedRow");
		searchedRowXpath = searchedRowXpath.replace("${rowName}", clickOnTheSeachedRow);
		// driver.switchTo().frame("//iframe");
		WebElement searchedRow = driver.findElement(By.xpath(searchedRowXpath));
		utils.waitTillVisibilityOfElement(searchedRow, "Saved Rows under content Library");
		selUtils.mouseHoverAndClickBYMouseAction(driver, searchedRow, searchedRow);
		utils.longWaitInSeconds(2);
		logger.info("Folder selected and ready for attachment with email template");
		// driver.switchTo().defaultContent();
		WebElement saveAsDraft = utils.getLocator("contentLibraryPage.saveAsDraft");
		utils.scrollToElement(driver, saveAsDraft);
		utils.clickByJSExecutor(driver, saveAsDraft);
		logger.info("saved draft button clicked and saved as draft so it has been attached to emailTemplate");
		TestListeners.extentTest.get().info("Saved rows attached to email template");
	}

	public void deleteRow(String hintRowName, String enterExactRowName) throws InterruptedException {
		WebElement searchField = utils.getLocator("contentLibraryPage.savedRowsSearchField");
		searchField.clear();
		searchField.sendKeys(hintRowName);
		String savedRowXpath = utils.getLocatorValue("contentLibraryPage.savedRow");
		savedRowXpath = savedRowXpath.replace("${rowName}", enterExactRowName);
		WebElement savedRow = driver.findElement(By.xpath(savedRowXpath));
		// WebElement rowCard = utils.getLocator("contentLibraryPage.rowOverLay");
		WebElement rowDeletbutton = utils.getLocator("contentLibraryPage.deleteButtonOnRow");
		selUtils.mouseHoverAndClickBYMouseAction(driver, savedRow, rowDeletbutton);
		WebElement deleteButton = utils.getLocator("contentLibraryPage.deleteButtonPresentOnPopUp");
		deleteButton.click();
	}

	public void AddSixBlocksToSplitedRow(String enterTheURL) throws InterruptedException {
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		WebElement rowTab = utils.getLocator("emailTemplatePage.rowTabLabel");
		rowTab.isDisplayed();
		utils.clickByJSExecutor(driver, rowTab);
		logger.info("Clicked on content row tab label");
		WebElement splittedRow = utils.getLocator("emailTemplatePage.splitSixSectionRow");
		WebElement dropContenBlockHere = utils.getLocator("emailTemplatePage.dropAndDragHere");
		selUtils.dragAndDropActions(splittedRow, dropContenBlockHere);
		logger.info("Splitted row draged and drop");
		TestListeners.extentTest.get().info("Splitted row draged and drop");
		WebElement contentTab = utils.getLocator("emailTemplatePage.contentTabLabel");
		contentTab.isDisplayed();
		utils.clickByJSExecutor(driver, contentTab);
		logger.info("Clicked on content tab label");
		utils.longWaitInSeconds(2);
		dragAndDropBlockOnSplit1("Button");
		dragAndDropBlockOnSplit2("Text");
		dragAndDropBlockOnSplit3("Social");
		dragAndDropBlockOnSplit4("HTML");
		dragAndDropBlockOnSplit5("Video");
		dragAndDropBlockOnSplit6();
		WebElement videoBlock = utils.getLocator("emailTemplatePage.videoOnRow");
		utils.waitTillVisibilityOfElement(videoBlock, "Video Block");
		utils.clickByJSExecutor(driver, videoBlock);
		logger.info("Clicked on video block");
		TestListeners.extentTest.get().info("Clicked on video block");
		WebElement urltextFiled = utils.getLocator("emailTemplatePage.videoUrlTextbox");
		urltextFiled.clear();
		urltextFiled.sendKeys(enterTheURL);
		urltextFiled.sendKeys(Keys.ENTER);
		logger.info("URL entered into the video block");
		driver.switchTo().defaultContent();
	}

	public void deleteTheFolder() {
		WebElement deleteFolder = utils.getLocator("contentLibraryPage.deleteFolderButton");
		//utils.waitTillInVisibilityOfElement(deleteFolder, "Folder");
		//utils.clickByJSExecutor(driver, deleteFolder);
		utils.StaleElementclick(driver, deleteFolder);
		logger.info("Clicked on folder delete button pop displayed");
		utils.longWaitInSeconds(1);
		WebElement deleteButtonPopUp = utils.getLocator("contentLibraryPage.deleteButtonPresentOnPopUp");
		utils.waitTillVisibilityOfElement(deleteButtonPopUp, "yes delete button");
		utils.clickByJSExecutor(driver, deleteButtonPopUp);
		logger.info("Folder deleted");
	}

	public void clickOnShowStructureButton() {
		WebElement frame = utils.getLocator("contentLibraryPage.iframe");
		driver.switchTo().frame(frame);
		WebElement showStructureButton = utils.getLocator("contentLibraryPage.showStructureButton");
		utils.clickByJSExecutor(driver, showStructureButton);
		logger.info("Clicked on show structure button");
		driver.switchTo().defaultContent();
	}

	public void editAndUpdateTheSavedRow(String enterRowName) throws InterruptedException {
		String savedRowXpath = utils.getLocatorValue("contentLibraryPage.savedRow");
		savedRowXpath = savedRowXpath.replace("${rowName}", enterRowName);
		WebElement savedRow = driver.findElement(By.xpath(savedRowXpath));
		// WebElement rowCard = utils.getLocator("contentLibraryPage.rowOverLay");
		String rowEditButtonXpath = utils.getLocatorValue("contentLibraryPage.editButtonOnRow");
		rowEditButtonXpath = rowEditButtonXpath.replace("${rowName}", enterRowName);
		WebElement editButtonOnRow = driver.findElement(By.xpath("rowEditButtonXpath"));
		selUtils.mouseHoverAndClickBYMouseAction(driver, savedRow, editButtonOnRow);
		WebElement editButton = utils.getLocator("contentLibraryPage.yesEditButton");
		editButton.click();
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//iframe")));
		dragAndDropBlockOnSplit2("Button");
		driver.switchTo().defaultContent();
	}
}