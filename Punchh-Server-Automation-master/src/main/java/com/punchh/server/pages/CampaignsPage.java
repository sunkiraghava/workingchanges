package com.punchh.server.pages;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;

@Listeners(TestListeners.class)
public class CampaignsPage {

	static Logger logger = LogManager.getLogger(CampaignsPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	SignupCampaignPage signupcampaignpage;
	private PageObj pageObj;

	private Map<String, By> locators;

	public CampaignsPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		signupcampaignpage = new SignupCampaignPage(driver);
		pageObj = new PageObj(driver);
		locators = utils.getAllByMap();
	}

	public void selectOngoingdrpValue(String value) throws InterruptedException {
		utils.longWaitInSeconds(2);
		try {
			utils.implicitWait(3);
			WebElement switchToClassicBtn = driver.findElement(locators.get("newCamHomePage.switchToClassicBtn"));
			switchToClassicBtn.click();
			utils.waitTillPagePaceDone();
		} catch (Exception e) {

		}
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		WebElement onGoingDrp = driver.findElement(locators.get("campaignsPage.onGoingDrp"));
		onGoingDrp.click();
		utils.longWaitInSeconds(1);
		logger.info("Clicked OnGoing Dropdown");
		List<WebElement> onGoingList = driver.findElements(locators.get("campaignsPage.onGoingList"));

		for (int i = 0; i < onGoingList.size(); i++) {
			if (onGoingList.get(i).getText().equalsIgnoreCase(value)) {
				onGoingList.get(i).click();
				logger.info("Slected " + value + " Option");
				break;
			}
		}
	}

	public void selectOfferdrpValue(String value) {
		// pageObj.newCamHomePage().campaignAdvertiseBlock();
		try {
			utils.implicitWait(1);
			WebElement switchToClassicBtn = driver.findElement(locators.get("newCamHomePage.switchToClassicBtn"));
			switchToClassicBtn.click();
			logger.info("Clicked on " + value + " from dropdown");
			TestListeners.extentTest.get().info("Clicked on " + value + " from dropdown");
			utils.waitTillPagePaceDone();
		} catch (Exception e) {

		}
		// pageObj.newCamHomePage().campaignAdvertiseBlock();
		WebElement offerDrp = driver.findElement(locators.get("campaignsPage.offerDrp"));
		utils.waitTillElementToBeClickable(offerDrp);
		offerDrp.click();
		logger.info("Clicked on Dropdown");
		TestListeners.extentTest.get().info("Clicked on dropdown");
		utils.longWaitInSeconds(1);
		List<WebElement> offerList = driver.findElements(locators.get("campaignsPage.offerList"));

		for (int i = 0; i < offerList.size(); i++) {
			if (offerList.get(i).getText().equalsIgnoreCase(value)) {
				// ele.get(i).click();
				utils.longWaitInSeconds(1);
				offerList.get(i).click();
				// utils.clickWithActions(ele.get(i));
				logger.info("Slected " + value + " offer option");
				TestListeners.extentTest.get().info("Slected " + value + " offer option");
				break;
			}
		}
	}

	public void selectMessagedrpValue(String value) {
		try {
			utils.implicitWait(4);
			WebElement switchToClassicBtn = driver.findElement(locators.get("newCamHomePage.switchToClassicBtn"));
			switchToClassicBtn.click();
			utils.waitTillPagePaceDone();
		} catch (Exception e) {

		}
		pageObj.newCamHomePage().campaignAdvertiseBlock();
		WebElement messageDrp = driver.findElement(locators.get("campaignsPage.messageDrp"));
		messageDrp.click();
		logger.info("Clicked message Dropdown");
		List<WebElement> messageList = driver.findElements(locators.get("campaignsPage.messageList"));

		for (int i = 0; i < messageList.size(); i++) {
			if (messageList.get(i).getText().equalsIgnoreCase(value)) {
				// ele.get(i).click();
				utils.clickWithActions(messageList.get(i));
				logger.info("Slected " + value + " offer option");
				break;
			}
		}
	}

	public void clickNewCampaignBtn() {
		utils.waitTillPagePaceDone();
		WebElement newCampaignBtn = driver.findElement(locators.get("campaignsPage.newCampaignBtn"));
		newCampaignBtn.click();
		logger.info("Clicked new campaign button");
		TestListeners.extentTest.get().pass("Clicked new campaign button");
	}

	public boolean validateSuccessMessage() {
		selUtils.implicitWait(40);
		boolean flag = false;
		try {
			WebElement ele = driver.findElement(locators.get("campaignsPage.successAlert"));
			flag = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return flag;
	}

	public boolean validateErrorsMessage() {
		selUtils.implicitWait(40);
		boolean flag = false;
		try {
			WebElement ele = driver.findElement(locators.get("campaignsPage.errorAlert"));
			flag = utils.checkElementPresent(ele);
		} catch (Exception e) {

		}
		return flag;
	}

	public String validateErrorsMessagee() {
		selUtils.implicitWait(40);
		WebElement errorAlert = driver.findElement(locators.get("campaignsPage.errorAlert"));
		String val = errorAlert.getText().trim();
		return val;
	}

	public void removeSearchedCampaign(String Campaignname) throws InterruptedException {
		try {
			driver.findElement(locators.get("campaignsPage.searchBox")).clear();
			WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
			searchBox.sendKeys(Campaignname);
			searchBox.sendKeys(Keys.ENTER);
			WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
			campaignName.click();
			// new cpp delete
			utils.waitTillPagePaceDone();
			utils.implicitWait(4);
			utils.waitTillPagePaceDone();
			WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
			if (utils.checkElementPresent(newOptionList)) {
				utils.longWaitInSeconds(2);
				newOptionList.click();
				WebElement deleteLink = driver.findElement(locators.get("campaignsPage.deleteLink"));
				deleteLink.click();
				WebElement deleteYesBtn = driver.findElement(locators.get("campaignsPage.deleteYesBtn"));
				deleteYesBtn.click();
				utils.waitTillPagePaceDone();
				utils.longwait(3000);

				boolean status = validateSuccessMessage();
				Assert.assertTrue(status, "Unable to delete Campaign..." + Campaignname);
				logger.info("Campaign deleted....");
				TestListeners.extentTest.get().info("Campaign deleted....");
				// driver.findElement(By.xpath("//div[@title='Classic Page']")).click();
			}

		} catch (Exception e) {
			// classic delete
			WebElement listIcon = driver.findElement(locators.get("campaignsPage.listIcon"));
			listIcon.click();
			WebElement deleteBtnLink = driver.findElement(locators.get("campaignsPage.deleteBtnLink"));
			deleteBtnLink.click();
			// utils.getLocator("campaignsPage.deleteLink").click();
			driver.switchTo().alert().accept();
			utils.waitTillPagePaceDone();
			TestListeners.extentTest.get().info("campaign deleted....");
		}
		utils.implicitWait(50);
	}

	public void searchAndDeleteDraftCampaignClassic(String Campaignname) throws InterruptedException {
		try {
			driver.findElement(locators.get("campaignsPage.searchBox")).clear();
			WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
			searchBox.sendKeys(Campaignname);
			searchBox.sendKeys(Keys.ENTER);
			WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
			campaignName.click();
			////////////////////////////////////////////////////////////
			utils.waitTillPagePaceDone();
			WebElement listIcon = driver.findElement(locators.get("campaignsPage.listIcon"));
			listIcon.click();
			WebElement deleteBtnLink = driver.findElement(locators.get("campaignsPage.deleteBtnLink"));
			deleteBtnLink.click();
			// utils.getLocator("campaignsPage.deleteLink").click();
			driver.switchTo().alert().accept();
			utils.waitTillPagePaceDone();
			TestListeners.extentTest.get().info("campaign deleted....");

		} catch (Exception e) {

		}
	}

	public void selectnewCPPOptions(String optionName) {
		utils.longWaitInSeconds(4);
		WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
		if (utils.checkElementPresent(newOptionList)) {
			newOptionList.click();
			WebElement optionval = driver.findElement(By.xpath("//span[text()='" + optionName + "']"));
			optionval.click();
			utils.waitTillPagePaceDone();
			logger.info("Clicked option :" + optionName);
			TestListeners.extentTest.get().info("Clicked option :" + optionName);
		}
	}

	public String newCPPCamStatus(String campaignName) {
		WebElement camName = driver.findElement(By.xpath("//span[normalize-space()='" + campaignName + "']"));
		camName.isDisplayed();
		WebElement status = driver.findElement(By.xpath("//span[normalize-space()='Processed']"));
		return status.getText();

	}

	public void searchAndSelectCamapign(String Campaignname) {
		utils.waitTillElementToBeClickable(utils.getLocator("campaignsPage.searchBox"));
		utils.getLocator("campaignsPage.searchBox").clear();
		utils.getLocator("campaignsPage.searchBox").sendKeys(Campaignname);
		utils.getLocator("campaignsPage.searchBox").sendKeys(Keys.ENTER);
		utils.getLocator("campaignsPage.campaignName").click();
		utils.waitTillPagePaceDone();
	}

	public void getGiftingStats() {
		try {
			WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
			if (utils.checkElementPresent(newOptionList)) {
				newOptionList.click();
				WebElement classicPageDiv = driver.findElement(By.xpath("//div[@title='Classic Page']"));
				classicPageDiv.click();
			}
		} catch (Exception e) {

		}

	}

	// This method clicks the already searched campaign
	public void selectSearchedCamapign(String campaignName) {
		WebElement campaignNameEl = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignNameEl.click();
		utils.waitTillPagePaceDone();
	}

	public void gotoClassiccampaignSummaryPage() {
		try {
			WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
			if (utils.checkElementPresent(newOptionList)) {
				newOptionList.click();
				WebElement classicPageDiv = driver.findElement(By.xpath("//div[@title='Classic Page']"));
				classicPageDiv.click();
			}
		} catch (Exception e) {

		}

	}

	public int campaignEngagementStats() {
		utils.waitTillPagePaceDone();
		WebElement guestsTargeted = driver.findElement(locators.get("campaignsPage.guestsTargeted"));
		String val = guestsTargeted.getText();
		int count = Integer.parseInt(val);
		return count;

	}

	public String checkABCamStatus(String Campaignname) throws InterruptedException {
		String val = "";
		driver.findElement(locators.get("campaignsPage.searchBox")).clear();
		WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		searchBox.sendKeys(Campaignname);
		searchBox.sendKeys(Keys.ENTER);
		WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignName.click();

		// boolean flag = false;
		int attempts = 0;
		while (attempts < 10) {
			utils.refreshPage();
			Utilities.longWait(4000);
			try {
				WebElement camLogs = driver.findElement(locators.get("campaignsPage.camLogs"));
				val = camLogs.getText();
				if (val.contains("Started")) {
					// flag = true;
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Campaign Name did not matched");
			}
			attempts++;
		}
		return val;
	}

	public boolean searchClassicCampaign(String Campaignname) throws InterruptedException {
		boolean status = false;
		WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		searchBox.clear();
		searchBox.sendKeys(Campaignname);
		searchBox.sendKeys(Keys.ENTER);
		try {
			WebElement ele = driver.findElement(locators.get("campaignsPage.campaignName"));
			status = utils.checkElementPresent(ele);

		} catch (Exception e) {

		}
		return status;
	}
	
	public void searchCampaign(String Campaignname) {
	    int attempts = 0;
	    while (attempts < 3) { // retry up to 3 times
	        try {
	            // Wait until search box is clickable
	            utils.waitTillElementToBeClickable(utils.getLocator("campaignsPage.searchBox"));
	            // Locate the element
	            utils.getLocator("campaignsPage.searchBox").clear();
	            utils.longWaitInSeconds(1); 
	            // Re-locate the element after clear to handle dynamic DOM
	            utils.getLocator("campaignsPage.searchBox");
	            utils.waitTillElementToBeClickable(utils.getLocator("campaignsPage.searchBox"));
	            utils.longWaitInSeconds(1);
	            // Send keys
	            utils.getLocator("campaignsPage.searchBox").sendKeys(Campaignname);
	            utils.getLocator("campaignsPage.searchBox").sendKeys(Keys.ENTER);
	            break; // success, exit loop
	        } catch (StaleElementReferenceException e) {
	            attempts++;
	            utils.longWaitInSeconds(1); // wait a bit before retry
	            if (attempts == 3) {
	                throw e; // rethrow if still failing after retries
	            }
	        }
	    }
	}

	public String getDynamicToken() {
		WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignName.click();
		utils.waitTillPagePaceDone();
		try {
			utils.longWaitInSeconds(2);
			utils.waitTillPagePaceDone();
			WebElement newOptionList = driver.findElement(locators.get("campaignsPage.clickOnOptionButton"));
			utils.waitTillElementToBeClickable(newOptionList);

			if (utils.checkElementPresent(newOptionList)) {
				newOptionList.click();
				WebElement clickOnClassicPageButton = driver.findElement(locators.get("campaignsPage.clickOnClassicPageButton"));
				clickOnClassicPageButton.click();
				utils.longwait(5000);
				WebElement classicPageDiv = driver.findElement(By.xpath("//div[@title='Classic Page']"));
				classicPageDiv.click();
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		// utils.getLocator("campaignsPage.editCampaignBtn").click();
		utils.longWaitInSeconds(7);
		WebElement ele = driver.findElement(locators.get("campaignsPage.editCampaignBtn"));
		utils.StaleElementclick(driver, ele);
		utils.waitTillPagePaceDone();
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		WebElement dynamicToken = driver.findElement(locators.get("campaignsPage.dynamicToken"));
		String value = dynamicToken.getAttribute("value");
		logger.info("Dynamic token is : " + value);
		return value;
	}

	public String getDynamicUrl() {
		WebElement dynamicUrl = driver.findElement(locators.get("campaignsPage.dynamicUrl"));
		String value = dynamicUrl.getAttribute("value");
		String url = value.replace("GENERATED_SIGNATURE", "");
		logger.info("Dynamic url is : " + value);
		logger.info("Dynamic url is : " + url);
		return url;
	}

	/*
	 * public String getJWTCode(String token) throws InterruptedException {
	 * driver.get("http://jwtbuilder.jamiekurtz.com/"); Thread.sleep(3000);
	 * driver.findElement(By.xpath("//tbody/tr[3]/td[1]/input[1]")).clear();
	 * driver.findElement(By.xpath("//tbody/tr[3]/td[1]/input[1]")).sendKeys("email"
	 * ); driver.findElement(By.xpath("//tbody/tr[3]/td[2]/input[1]")).clear();
	 * driver.findElement(By.xpath("//tbody/tr[3]/td[2]/input[1]")).sendKeys(
	 * "aayushi.singh@gmail.com");
	 * driver.findElement(By.xpath("//input[@id='key']")).clear();
	 * driver.findElement(By.xpath("//input[@id='key']")).sendKeys(token);
	 * driver.findElement(By.xpath("//button[contains(text(),'Create Signed JWT')]")
	 * ).click(); Thread.sleep(3000); String value =
	 * driver.findElement(By.xpath("//pre[@id='created-jwt']")).getText();
	 * logger.info("JWT is : " + value); return value; }
	 */

	public String getJWTCode(String token)
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException {
		@SuppressWarnings("deprecation")
		String jwt = Jwts.builder().claim("email", "aayushi.singh@gmail.com")
				.signWith(SignatureAlgorithm.HS256, token.getBytes("UTF-8")).compact();
		logger.info("jwt code is : " + jwt);
		return jwt;
	}

	public String getCouponCode(String url) {
		driver.get(url);
		WebElement couponDetails = driver.findElement(locators.get("campaignsPage.couponDetails"));
		String value = couponDetails.getText();
		String a[] = value.split(":");
		logger.info("Coupon code is : " + a[1]);
		return a[1].trim();
	}

	public List<String> CouponLookUp(String code) {
		int attempts = 0;
		while (attempts < 20) {
			try {
				driver.findElement(locators.get("campaignsPage.codeSearch")).clear();
				WebElement codeSearch = driver.findElement(locators.get("campaignsPage.codeSearch"));
				codeSearch.sendKeys(code);
				codeSearch.sendKeys(Keys.ENTER);
				utils.waitTillPagePaceDone();
				WebElement ele = driver.findElement(By.xpath("//table[@id='search-coupon-results']//tbody/tr/td[1]"));
				String codeVal = ele.getText();
				if (codeVal.equalsIgnoreCase(code)) {
					logger.info("Coupon Lookup code found : " + codeVal);
					TestListeners.extentTest.get().pass(("Coupon Lookup code found : " + codeVal));
					break;
				}
			} catch (Exception e) {
				logger.info("Element is not present or Campaign Name did not matched");
			}
			attempts++;
		}

		List<String> couponData = new ArrayList<String>();
		List<WebElement> tableRowOne = driver.findElements(locators.get("campaignsPage.couponResultTable"));
		int col = tableRowOne.size();
		for (int i = 0; i < col; i++) {
			String val = tableRowOne.get(i).getText();
			couponData.add(val);

		}
		return couponData;

	}

	public void findCouponCampaignandEdit(String campaignName) {
		try {
			List<WebElement> elements = driver.findElements(locators.get("campaignsPage.couponCampaignsList"));
			for (int i = 0; i < elements.size(); i++) {
				if (elements.get(i).getText().equals(campaignName)) {
					logger.info("Coupon campaign name is ==> " + elements.get(i).getText());
					elements.get(i).click();
					break;
				}
			}
			//////////////////////////////////////////////
			try {
				WebElement newOptionList = driver.findElement(By.xpath("//span[text()='Options']"));
				if (utils.checkElementPresent(newOptionList)) {
					newOptionList.click();
					WebElement classicPageDiv = driver.findElement(By.xpath("//div[@title='Classic Page']"));
					classicPageDiv.click();
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
			//////////////////////////////////////////////
			WebElement editBtn = driver.findElement(locators.get("campaignsPage.editBtn"));
			editBtn.click();
			TestListeners.extentTest.get().pass("New coupon campaign selected successfully");

		} catch (Exception e) {
			logger.error("Error in selecting new coupon campaign" + e);
			TestListeners.extentTest.get().fail("Error in selecting new coupon campaign" + e);
		}
	}

	public String getCouponCampaignCode() {
		String code = "";
		try {
			WebElement listIcon = driver.findElement(locators.get("campaignsPage.listIcon"));
			listIcon.click();
			WebElement couponCodesListLink = driver.findElement(locators.get("campaignsPage.couponCodesListLink"));
			couponCodesListLink.click();
			selUtils.longWait(5000);
			refreshPage();
			WebElement awaitingCouponCodesBtn = driver.findElement(locators.get("campaignsPage.awaitingCouponCodesBtn"));
			awaitingCouponCodesBtn.click();
			// selUtils.implicitWait(4);
			WebElement couponCode = driver.findElement(locators.get("campaignsPage.couponCode"));
			for (int i = 0; i < 10; i++) {
				try {
					couponCode.isDisplayed();
					code = couponCode.getText();
					break;
				} catch (Exception e) {
					refreshPage();
					couponCode = driver.findElement(locators.get("campaignsPage.couponCode"));
					couponCode.isDisplayed();
					code = couponCode.getText();
				}
			}
			logger.info("Coupon campaign code is :" + code);
			TestListeners.extentTest.get().pass("coupon campaign code fetched successfully " + code);

		} catch (Exception e) {
			logger.error("Error in fetching  coupon code" + e);
			TestListeners.extentTest.get().fail("Error in fetching  coupon code" + e);
		}

		return code;
	}

	public void refreshPage() {
		driver.navigate().refresh();
		logger.info("Refreshing Page");
	}

	public void removeReferralCampaign() {
		WebElement referralCampaignSubStringLabel = driver.findElement(locators.get("campaignsPage.referralCampaignSubStringLabel"));
		referralCampaignSubStringLabel.click();
		WebElement listIcon = driver.findElement(locators.get("campaignsPage.listIcon"));
		listIcon.click();
		WebElement deleteLink = driver.findElement(locators.get("campaignsPage.deleteLink"));
		deleteLink.click();
		driver.switchTo().alert().accept();
		WebElement referralCampaignDeleteLabel = driver.findElement(locators.get("campaignsPage.referralCampaignDeleteLabel"));
		referralCampaignDeleteLabel.isDisplayed();
		logger.info("Campaign deleted");
		TestListeners.extentTest.get().info("Campaign deleted");
	}

	public void clickPostPurchaseBtn() {
		WebElement postPurchaseBtn = driver.findElement(locators.get("campaignsPage.postPurchaseBtn"));
		postPurchaseBtn.click();
	}

	public void clickNewCampaignBetaBtn() throws InterruptedException {
		WebElement newCampaignBetaBtn = driver.findElement(locators.get("campaignsPage.newCampaignBetaBtn"));
		newCampaignBetaBtn.click();
		logger.info("Clicked new campaign beta button");
		utils.longWait(1);
	}

	public void createBlackoutDate() throws InterruptedException {
		WebElement blackoutdateBtn = driver.findElement(locators.get("campaignsPage.blackoutdateBtn"));
		blackoutdateBtn.click();
		String date = utils.nextdaysDate();
		String dateOnly = date.split("-")[2];
		String xpath = utils.getLocatorValue("newCamHomePage.deleteBalckoutDate").replace("$date", dateOnly);
		try {
			WebElement deleteBlackoutDateEl = driver.findElement(By.xpath(xpath));
			deleteBlackoutDateEl.isDisplayed();
			WebElement deleteBlackoutdateIcon = driver.findElement(locators.get("newCamHomePage.deleteBlackoutdateIcon"));
			deleteBlackoutdateIcon.click();
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
		WebElement blackoutdateReason = driver.findElement(locators.get("campaignsPage.blackoutdateReason"));
		blackoutdateReason.sendKeys("Test Blackout");
		WebElement submitBtn = driver.findElement(locators.get("campaignsPage.submitBtn"));
		submitBtn.click();
		Thread.sleep(4000);

	}

	// for Moes business
	public String getPreGeneratedCuponCode() throws InterruptedException {
		WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignName.click();
		utils.waitTillPagePaceDone();
		try {
			utils.longWaitInSeconds(5);
			WebElement optionsButton = driver.findElement(locators.get("campaignsPage.clickOnOptionButton"));
			utils.waitTillElementToBeClickable(optionsButton);
			optionsButton.click();
			WebElement clickOnEditButton = driver.findElement(locators.get("campaignsPage.clickOnEditButton"));
			selUtils.waitTillElementToBeClickable(clickOnEditButton);
			clickOnEditButton.click();
		} catch (Exception e) {
			WebElement editCampaignButton = driver.findElement(locators.get("campaignsPage.editCampaignButton"));
			editCampaignButton.click();
		}

		WebElement sortingOptions = driver.findElement(locators.get("campaignsPage.sortingOptions"));
		selUtils.waitTillElementToBeClickable(sortingOptions);
		sortingOptions.click();
		WebElement couponCodesListLink = driver.findElement(locators.get("campaignsPage.couponCodesListLink"));
		selUtils.waitTillElementToBeClickable(couponCodesListLink);
		couponCodesListLink.click();
		String codeName = "";
		// boolean flag = false;
		int attempts = 0;
		while (attempts < 30) {
			try {
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				WebElement couponCodeCell = driver.findElement(By.xpath("//table[@id='coupons']/tbody/tr[2]/td[1]"));
				codeName = couponCodeCell.getText(); /// utils.getLocator("campaignsPage.cuponCodeText").getText();
				if (codeName != "") {
					// flag = true;
					TestListeners.extentTest.get().pass("Coupon code generated successfully :" + codeName);
					break;
				}
			} catch (Exception e) {

			}
			attempts++;
		}
		return codeName;
	}

	// for Moes business
	public List<String> getPreGeneratedCuponCodeList() throws InterruptedException {
		WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignName.click();
		utils.waitTillPagePaceDone();
		WebElement clickOnOptionButton = driver.findElement(locators.get("campaignsPage.clickOnOptionButton"));
		selUtils.waitTillElementToBeClickable(clickOnOptionButton);
		utils.clickByJSExecutor(driver, clickOnOptionButton);
		// utils.getLocator("campaignsPage.clickOnEditButton").click();
		WebElement clickOnEditButton = driver.findElement(locators.get("campaignsPage.clickOnEditButton"));
		selUtils.waitTillElementToBeClickable(clickOnEditButton);
		utils.clickByJSExecutor(driver, clickOnEditButton);
		// utils.getLocator("campaignsPage.clickOnEditButton").click();
		WebElement sortingOptions = driver.findElement(locators.get("campaignsPage.sortingOptions"));
		selUtils.waitTillElementToBeClickable(sortingOptions);

		sortingOptions.click();
		WebElement couponCodesListLink = driver.findElement(locators.get("campaignsPage.couponCodesListLink"));
		selUtils.waitTillElementToBeClickable(couponCodesListLink);

		couponCodesListLink.click();
		// Thread.sleep(5000);
		utils.waitTillCompletePageLoad();
		int counter = 0;
		List<WebElement> codeNameListWebelement;
		List<String> codeNameList;
		do {
			codeNameListWebelement = driver.findElements(locators.get("campaignsPage.couponCodeListXpath"));
			codeNameList = new ArrayList<String>();

			for (WebElement wEle : codeNameListWebelement) {
				codeNameList.add(wEle.getText());
			}
			if (codeNameList.size() == 0) {
				selUtils.longWait(1000);
				utils.refreshPage();
				utils.waitTillPagePaceDone();
				counter++;
			}

		} while (codeNameList.size() == 0 && counter <= 10);

		return codeNameList;
	}

	// for tastea business
	public List<String> getPreGeneratedCuponList() throws InterruptedException {
		WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignName.click();
		Thread.sleep(5000);
		WebElement editCampaignBtn = driver.findElement(locators.get("campaignsPage.editCampaignBtn"));
		editCampaignBtn.click();
		Thread.sleep(1000);
		// driver.findElement(By.xpath("//div[@title='Edit']")).click();
		selUtils.implicitWait(5);
		WebElement sortingOptionsButton = driver.findElement(By.xpath("//div[@id='sorting-options']/button/i"));
		sortingOptionsButton.click();
		selUtils.implicitWait(2);
		WebElement couponCodesListLink = driver.findElement(locators.get("campaignsPage.couponCodesListLink"));
		couponCodesListLink.click();
		Thread.sleep(5000);
		List<WebElement> codeNameListWebelement = driver
				.findElements(By.xpath("//table[@id='coupons']/tbody/tr/td[1]")); /// utils.getLocator("campaignsPage.cuponCodeText").getText();
		List<String> codeNameList = new ArrayList<String>();
		for (WebElement wEle : codeNameListWebelement) {
			codeNameList.add(wEle.getText());

		}

		return codeNameList;
	}

	public void deactivateCampaign(String cname) {
		WebElement ellipsisBtn = driver.findElement(locators.get("campaignsPage.ellipsisBtn"));
		ellipsisBtn.click();
		WebElement deactivateRecurringCampaign = driver.findElement(locators.get("campaignsPage.deactivateRecurringCampaign"));
		deactivateRecurringCampaign.isDisplayed();
		deactivateRecurringCampaign.click();
		Alert alert = driver.switchTo().alert();
		if (alert.getText().equals("Are you sure you want to deactivate this Campaign?")) {
			alert.accept();
		} else {
			logger.error("Incorrect alert message");
			TestListeners.extentTest.get().fail("Incorrect alert message");
		}
		WebElement deactivateLabel = driver.findElement(locators.get("campaignsPage.deactivateLabel"));
		deactivateLabel.isDisplayed();
		logger.info("Successfully deactivated recurring mass campaign: " + cname);
		TestListeners.extentTest.get().pass("Successfully deactivated recurring mass campaign: " + cname);

	}

	// for Moes business
	public void deactivateOrDeleteTheCoupon(String button) throws InterruptedException {
		utils.waitTillPagePaceDone();
		WebElement campaignName = driver.findElement(locators.get("campaignsPage.campaignName"));
		campaignName.click();
		utils.waitTillPagePaceDone();
		WebElement optionsButton = driver.findElement(locators.get("campaignsPage.optionsButton"));
		selUtils.waitTillElementToBeClickable(optionsButton);
		utils.clickByJSExecutor(driver, optionsButton);
		// utils.getLocator("campaignsPage.optionsButton").click();
		WebElement optionEditButton = driver.findElement(locators.get("campaignsPage.optionEditButton"));
		selUtils.waitTillElementToBeClickable(optionEditButton);
		utils.clickByJSExecutor(driver, optionEditButton);
		// utils.getLocator("campaignsPage.optionEditButton").click();
		WebElement sortingOptions = driver.findElement(locators.get("campaignsPage.sortingOptions"));
		selUtils.waitTillElementToBeClickable(sortingOptions);

		sortingOptions.click();
		selUtils.implicitWait(1);
		// Thread.sleep(1000);
		switch (button) {
		case "delete":
			WebElement deleteCoupnLink = driver.findElement(locators.get("campaignsPage.deleteCoupnLink"));
			selUtils.waitTillElementToBeClickable(deleteCoupnLink);
			selUtils.implicitWait(5);
			// Thread.sleep(5000);
			utils.clickByJSExecutor(driver, deleteCoupnLink);
			utils.acceptAlert(driver);
			logger.info("Campaign is deleted");
			TestListeners.extentTest.get().pass("Campaign is deleted");
			break;

		case "deactivate":
			WebElement deactivateCoupnLink = driver.findElement(locators.get("campaignsPage.deactivateCoupnLink"));
			selUtils.waitTillElementToBeClickable(deactivateCoupnLink);
			selUtils.implicitWait(5);
			// Thread.sleep(5000);
			utils.clickByJSExecutor(driver, deactivateCoupnLink);
			utils.acceptAlert(driver);
			logger.info("Campaign is deactivated");
			TestListeners.extentTest.get().pass("Campaign is deactivated");
			break;

		case "activate":
			WebElement activateChallengeCampLink = driver.findElement(locators.get("campaignsPage.activateChallengeCampLink"));
			selUtils.waitTillElementToBeClickable(activateChallengeCampLink);
			selUtils.implicitWait(5);
			// Thread.sleep(5000);
			utils.clickByJSExecutor(driver, activateChallengeCampLink);
			utils.acceptAlert(driver);
			logger.info("Campaign is activated");
			TestListeners.extentTest.get().pass("Campaign is activated");
			break;
		}

		selUtils.implicitWait(5);
		// Thread.sleep(5000);

	}

	public boolean verifyTemplateIconForCampaignName(String cmpName) {
		clickOnSwitchToClassicCamp();
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(3);
		boolean flag = false;
		searchCampaign(cmpName);
		utils.longWaitInSeconds(2);
		String templateIconXpath = utils.getLocatorValue("campaignsPage.tempplateIcon").replace("$cmpName", cmpName);
		WebElement templateIcon = driver.findElement(By.xpath(templateIconXpath));
		String txt = templateIcon.getText();
		selUtils.implicitWait(50);

		if (txt.equalsIgnoreCase("TEMPLATE")) {
			logger.info("Template icon is displaying");
			TestListeners.extentTest.get().pass("Template icon is displaying");
			flag = true;
		} else {
			logger.error("Template icon is not displaying");
			TestListeners.extentTest.get().fail("Template icon is not displaying");
			flag = false;
		}
		return flag;

	}

	public void searchAndSelectCamapign1(String Campaignname) {
		WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		utils.waitTillElementToBeClickable(searchBox);
		searchBox.click();
		driver.findElement(locators.get("campaignsPage.searchBox")).clear();
		searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		searchBox.sendKeys(Campaignname);
		searchBox.sendKeys(Keys.ENTER);
		WebElement campaignName1 = driver.findElement(locators.get("campaignsPage.campaignName1"));
		utils.clickByJSExecutor(driver, campaignName1);
		utils.waitTillPagePaceDone();
	}

	public void clickTestQualificationWithReceipt() {
		WebElement testQualificationWithReceipt = driver.findElement(locators.get("campaignsPage.testQualificationWithReceipt"));
		testQualificationWithReceipt.click();
		logger.info("clicked Test Qualification With Receipt");
		selUtils.implicitWait(3);
	}

	public void editMenuItemString(String str) {
		WebElement menuItemString = driver.findElement(locators.get("campaignsPage.menuItemString"));
		menuItemString.clear();
		menuItemString.sendKeys(str);
	}

	public void clickEvaluateBtn() {
		WebElement evaluate = driver.findElement(locators.get("campaignsPage.evaluate"));
		evaluate.click();
		utils.waitTillCompletePageLoad();
		selUtils.longWait(2000);
	}

	public String getEvaluateResult() {
		WebElement evaluateResult = driver.findElement(locators.get("campaignsPage.evaluateResult"));
		return evaluateResult.getText();
	}

	public String pageState() {
		String val = driver.getPageSource();
		return val;

	}

	public boolean tryNewCampaignBtnVisible() {
		try {
			WebElement switchToClassicBtn = driver.findElement(locators.get("newCamHomePage.switchToClassicBtn"));
			switchToClassicBtn.click();
			utils.waitTillPagePaceDone();
		} catch (Exception e) {

		}
		utils.waitTillPagePaceDone();
		utils.implicitWait(10);
		logger.info("searching for the try new campaign button");
		try {
			WebElement newCamPageBtn = driver.findElement(locators.get("newCamHomePage.newCamPageBtn"));
			newCamPageBtn.isDisplayed();
			return true;
		} catch (Exception e) {

		}
		utils.implicitWait(50);
		return false;
	}

	public void clickStatusOption() {
		WebElement sortByRecencyBtn = driver.findElement(locators.get("campaignsPage.sortByRecencyBtn"));
		sortByRecencyBtn.click();
		WebElement statusOption = driver.findElement(locators.get("campaignsPage.statusOption"));
		statusOption.click();
		utils.waitTillCompletePageLoad();
	}

	public String getActiveCampaign() {
		clickStatusOption();
		WebElement checkActiveCampaign = driver.findElement(locators.get("campaignsPage.checkActiveCampaign"));
		String val = checkActiveCampaign.getText();
		return val;
	}

	public void deleteCampaign(String cname) {
		searchAndSelectCamapign(cname);
		WebElement ellipsisBtn = driver.findElement(locators.get("campaignsPage.ellipsisBtn"));
		ellipsisBtn.click();
		WebElement deleteCampaign = driver.findElement(locators.get("campaignsPage.deleteCampaign"));
		deleteCampaign.click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
	}

	public String checkDerivedRewardCampaignSummary(String str) {
		utils.longWaitInSeconds(1);
		campaignSummaryPageCollapseSymbol();
		String xpath = utils.getLocatorValue("campaignsPage.checkOfferOfCampaign").replace("$flag", str);
		WebElement checkOfferOfCampaign = driver.findElement(By.xpath(xpath));
		utils.waitTillVisibilityOfElement(checkOfferOfCampaign, str);
		String val = checkOfferOfCampaign.getText();
		return val;
	}

	public void classicCampaignPageDeleteCampaign() {
		utils.longWaitInSeconds(2);
		WebElement optionBtn = driver.findElement(locators.get("campaignsPage.optionBtn"));
		utils.waitTillVisibilityOfElement(optionBtn, "option button");
		utils.clickByJSExecutor(driver, optionBtn);
		// utils.getLocator("campaignsPage.optionBtn").click();
		WebElement classicPageBtn = driver.findElement(locators.get("campaignsPage.classicPageBtn"));
		utils.waitTillVisibilityOfElement(classicPageBtn,
				"classic page button");
		classicPageBtn.click();
		utils.waitTillCompletePageLoad();
		WebElement ellipsisBtn = driver.findElement(locators.get("campaignsPage.ellipsisBtn"));
		ellipsisBtn.click();
		WebElement deleteCampaign = driver.findElement(locators.get("campaignsPage.deleteCampaign"));
		deleteCampaign.click();
		utils.acceptAlert(driver);
		utils.waitTillPagePaceDone();
	}

	public String getCampaignID() {
		String url = driver.getCurrentUrl();
		// Tailored regular expression to match the specific URL pattern
		Pattern pattern = Pattern.compile("(?<=campaign_builders#\\/)\\d+(?=\\/summary)");
		Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			logger.info("campaign id-" + matcher.group());
			TestListeners.extentTest.get().pass("campaign id-" + matcher.group());
			return matcher.group(); // Returns the captured number
		} else {
			return null; // Or handle the case where no number is found
		}
	}

	public String getStartTimeHour() {
		WebElement getStartTimeHour = driver.findElement(locators.get("campaignsPage.getStartTimeHour"));
		String time = getStartTimeHour.getText();
		String[] part = time.split("-");
		String[] hour = part[1].split(":");
		return hour[0];
	}

	public void createWhatDetailsPosRedeemptionCampaign(String name, String giftType, String giftreason, String gift) {
		// Set Name
		WebElement posRedeemptionCampaignName = driver
				.findElement(locators.get("signupCampaignsPage.posRedeemptionCampaignName"));
		posRedeemptionCampaignName.clear();
		posRedeemptionCampaignName.sendKeys(name);
		logger.info("Entered Pos redeemption campaign name");
		TestListeners.extentTest.get().info("Entered Pos redeemption campaign name");

		// Set Gift type
		WebElement posRedeemptionGiftTypeDrp = driver.findElement(locators.get("signupCampaignsPage.posRedeemptionGiftTypeDrp"));
		utils.selectDrpDwnValue(posRedeemptionGiftTypeDrp, giftType);

		// Set gift reason
		WebElement posRedeemptionGiftReason = driver
				.findElement(locators.get("signupCampaignsPage.posRedeemptionGiftReason"));
		posRedeemptionGiftReason.clear();
		posRedeemptionGiftReason.sendKeys(giftreason);
		logger.info("Entered gift reason");
		TestListeners.extentTest.get().info("Entered gift reason");

		// select gift

		switch (giftType) {
		case "Gift Points":
			WebElement posRedeemptionGiftPoints = driver.findElement(locators.get("signupCampaignsPage.posRedeemptionGiftPoints"));
			posRedeemptionGiftPoints.sendKeys(gift);
			logger.info("Entered the gift points --" + gift);
			TestListeners.extentTest.get().info("Entered the gift points --" + gift);
			break;
		case "Gift Redeemable":
			WebElement postRedeemptionRedeemableDrpDown = driver.findElement(locators.get("signupCampaignsPage.postRedeemptionRedeemableDrpDown"));
			utils.selectDrpDwnValue(postRedeemptionRedeemableDrpDown,
					gift);
			logger.info("Selected the redeeemable --" + gift);
			TestListeners.extentTest.get().info("Selected the redeeemable --" + gift);
			break;
		default:
			break;
		}

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();

		logger.info(
				"New post checkin campaign what details name, giftType, giftreason and redemable entered successfully: "
						+ name);
		TestListeners.extentTest.get().info(
				"New post checkin campaign what details name, giftType, giftreason and redemable entered successfully: "
						+ name);
	}

	public void selectGuestFrequency(String str) {
		WebElement posRedeemptionGuestFrequencyDrpDown = driver.findElement(locators.get("campaignsPage.posRedeemptionGuestFrequencyDrpDown"));
		utils.waitTillVisibilityOfElement(posRedeemptionGuestFrequencyDrpDown, "");
		utils.selectDrpDwnValue(posRedeemptionGuestFrequencyDrpDown, str);
		logger.info("selected the value --" + str);
		TestListeners.extentTest.get().info("selected the value --" + str);
	}

	public void setFrequency(String time, String days) {
		WebElement posRedeemptionGuestTimeField = driver
				.findElement(locators.get("campaignsPage.posRedeemptionGuestTimeField"));
		utils.waitTillVisibilityOfElement(posRedeemptionGuestTimeField, "");
		posRedeemptionGuestTimeField.clear();
		posRedeemptionGuestTimeField.sendKeys(time);

		utils.implicitWait(2);
		List<WebElement> daysInputWEleList = driver.findElements(locators.get("campaignsPage.posRedeemptionGuestDaysField"));
		for (WebElement ele : daysInputWEleList) {
			try {
				ele.clear();
				ele.sendKeys(days);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		logger.info("Entered the time --" + time + " entered the days --" + days);
		TestListeners.extentTest.get().info("Entered the time --" + time + " entered the days --" + days);
		utils.implicitWait(50);
	}

	public void setRedeemableInWhomPagePosRedeemableCampaign(String redeembleName) {
		WebElement redeemableDrpDownPosRedeemable = driver.findElement(locators.get("campaignsPage.redeemableDrpDownPosRedeemable"));
		utils.waitTillVisibilityOfElement(redeemableDrpDownPosRedeemable,
				"");
		utils.selectDrpDwnValue(redeemableDrpDownPosRedeemable,
				redeembleName);
		logger.info("selected the redeemable --" + redeembleName);
		TestListeners.extentTest.get().info("selected the redeemable --" + redeembleName);
	}

	public void setPNEmail(String push, String email) {
		WebElement massPushNotificationTxtBox = driver
				.findElement(locators.get("signupCampaignsPage.massPushNotificationTxtBox"));
		massPushNotificationTxtBox.clear();
		massPushNotificationTxtBox.sendKeys("push");
		WebElement massEmailSubjectTxtBox = driver.findElement(locators.get("signupCampaignsPage.massEmailSubjectTxtBox"));
		massEmailSubjectTxtBox.clear();
		massEmailSubjectTxtBox.sendKeys("push");
		WebElement massTemplateTxtBox = driver.findElement(locators.get("signupCampaignsPage.massTemplateTxtBox"));
		massTemplateTxtBox.clear();
		massTemplateTxtBox.sendKeys("push");

		logger.info("set the pn,email and subject email");
		TestListeners.extentTest.get().info("set the pn,email and subject email");

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
	}

	public void posRedeemptionMsgCampaignSetName(String name) {
		WebElement setNamePosRedeemMsgCampaign = driver
				.findElement(locators.get("campaignsPage.setNamePosRedeemMsgCampaign"));
		setNamePosRedeemMsgCampaign.clear();
		setNamePosRedeemMsgCampaign.sendKeys(name);
		logger.info("enter the campaignn name--" + name);
		TestListeners.extentTest.get().info("enter the campaignn name--" + name);

		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
	}

	public void selectGuestFrequencyPosRedemMsg(String str) {
		WebElement posRedeemptionMsgGuestFrequencyDrpDown = driver.findElement(locators.get("campaignsPage.posRedeemptionMsgGuestFrequencyDrpDown"));
		utils.waitTillVisibilityOfElement(posRedeemptionMsgGuestFrequencyDrpDown, "");
		utils.selectDrpDwnValue(posRedeemptionMsgGuestFrequencyDrpDown,
				str);
		logger.info("selected the value --" + str);
		TestListeners.extentTest.get().info("selected the value --" + str);
	}

	public void setFrequencyPosRedeem(String time, String days) {
		WebElement posRedeemMsgGuestTimeField = driver.findElement(locators.get("campaignsPage.posRedeemMsgGuestTimeField"));
		utils.waitTillVisibilityOfElement(posRedeemMsgGuestTimeField, "");
		posRedeemMsgGuestTimeField.clear();
		posRedeemMsgGuestTimeField.sendKeys(time);

		WebElement posRedeemMsgGuestDaysField = driver.findElement(locators.get("campaignsPage.posRedeemMsgGuestDaysField"));
		utils.waitTillVisibilityOfElement(posRedeemMsgGuestDaysField, "");
		posRedeemMsgGuestDaysField.clear();
		posRedeemMsgGuestDaysField.sendKeys(days);

		logger.info("Entered the time --" + time + " entered the days --" + days);
		TestListeners.extentTest.get().info("Entered the time --" + time + " entered the days --" + days);
	}

	public void setRedeemableInWhomPagePosRedeemMsgCampaign(String redeembleName) {
		WebElement redeemableDrpDownPosRedeemableMsg = driver.findElement(locators.get("campaignsPage.redeemableDrpDownPosRedeemableMsg"));
		utils.waitTillVisibilityOfElement(redeemableDrpDownPosRedeemableMsg, "");
		utils.selectDrpDwnValue(redeemableDrpDownPosRedeemableMsg,
				redeembleName);
		logger.info("selected the redeemable --" + redeembleName);
		TestListeners.extentTest.get().info("selected the redeemable --" + redeembleName);
	}

	public void selectOptionFromEllipsisee(String optionName) {
		utils.waitTillPagePaceDone();
		WebElement newEllipsisBtn = driver.findElement(locators.get("campaignsPage.newEllipsisBtn"));
		newEllipsisBtn.click();
		String xpath = utils.getLocatorValue("campaignsPage.selectOptionFromEllipsise").replace("${val}", optionName);
		WebElement ele = driver.findElement(By.xpath(xpath));
		utils.waitTillVisibilityOfElement(ele, optionName);
		utils.clickByJSExecutor(driver, ele);
		logger.info("clicked on the option " + optionName);
		TestListeners.extentTest.get().info("clicked on the option " + optionName);
	}

	public String campaignStatusThroughStatusTracker(String campaignName) {
		utils.switchToWindowByIndex(driver, 1);
		utils.waitTillPagePaceDone();
		String xpath = utils.getLocatorValue("campaignsPage.campaignNameDisplayed").replace("${campaignName}",
				campaignName);
		WebElement camName = driver.findElement(By.xpath(xpath));
		camName.isDisplayed();
		WebElement campaignProcessedStatus = driver.findElement(locators.get("campaignsPage.campaignProcessedStatus"));
		String status = campaignProcessedStatus.getText();
		// Close the latest opened tab
		driver.close();
		utils.switchToWindowByIndex(driver, 0);
		return status;
	}

	public void selectCPPOptions(String optionName) {

		WebElement ellipsis = driver.findElement(locators.get("campaignsPage.Ellipsis"));
		utils.waitTillElementToBeClickable(ellipsis);
		utils.clickByJSExecutor(driver, ellipsis);
		String xpath = utils.getLocatorValue("campaignsPage.selectOptionFromEllipsiseNew").replace("${val}",
				optionName);
		WebElement optionFromEllipsise = driver.findElement(By.xpath(xpath));
		utils.waitTillVisibilityOfElement(optionFromEllipsise, optionName);
		utils.clickByJSExecutor(driver, optionFromEllipsise);
		logger.info("Clicked option :" + optionName);
		TestListeners.extentTest.get().info("Clicked option :" + optionName);
	}

	public void createDuplicatePosCampaign(String name) {
		WebElement setCampaignName = driver.findElement(locators.get("campaignsPage.setCampaignName"));
		setCampaignName.clear();
		setCampaignName.sendKeys(name);

		WebElement nextBtnNew = driver.findElement(locators.get("newCamHomePage.nextBtnNew"));
		nextBtnNew.click();
		utils.waitTillPagePaceDone();
		// scroll to next btn
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		utils.scrollToElement(driver, nextBtn);
		nextBtn.click();
		utils.waitTillPagePaceDone();
		// activate campaign
		WebElement activate = driver.findElement(locators.get("campaignsPage.activate"));
		activate.click();
		utils.waitTillPagePaceDone();
		logger.info("create a duplicate Campaign named --" + name);
		TestListeners.extentTest.get().info("create a duplicate Campaign named --" + name);
	}

	public String searchAndGetCampaignID(String campaignName) {
		utils.waitTillPagePaceDone();
		WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		searchBox.clear();
		searchBox.sendKeys(campaignName);
		searchBox.sendKeys(Keys.ENTER);
		utils.waitTillPagePaceDone();
		logger.info("searching the campaign ---" + campaignName);
		TestListeners.extentTest.get().info("searching the campaign ---" + campaignName);

		String xpath = utils.getLocatorValue("campaignsPage.getCampaignId").replace("${campaignName}", campaignName);
		String attrbuteVal = driver.findElement(By.xpath(xpath)).getAttribute("data-id");
		logger.info("Campaign id -- " + attrbuteVal);
		TestListeners.extentTest.get().info("Campaign id -- " + attrbuteVal);

		return attrbuteVal;
	}

	public boolean searchCampaignInFinishedTab(String campaignName, String campaignID) {
		utils.waitTillPagePaceDone();
		WebElement campaignFinishedTab = driver.findElement(locators.get("campaignsPage.campaignFinishedTab"));
		campaignFinishedTab.click();
		logger.info("clicked on the Finished tab");
		TestListeners.extentTest.get().info("clicked on the Finished tab");

		String xpath = utils.getLocatorValue("campaignsPage.searchCampaignInFinishedTab")
				.replace("${campaignName}", campaignName).replace("${campaignID}", campaignID);
		int counter = 0;
		boolean flag = false;
		utils.implicitWait(5);
		do {
			try {
				driver.findElement(By.xpath(xpath)).isDisplayed();
				flag = true;
				break;
			} catch (Exception e) {
				counter++;
				utils.longwait(1500);
				utils.refreshPage();
				utils.waitTillPagePaceDone();
			}
		} while (counter < 60);
		utils.implicitWait(50);
		return flag;
	}

	public void clickOnSwitchToClassicCamp() {
		List<WebElement> listWebElement = driver.findElements(locators.get("newCamHomePage.switchToClassicBtn"));
		if (listWebElement.size() != 0) {
			listWebElement.get(0).click();
		}
		utils.waitTillPagePaceDone();
	}

	public void createCouponCampaignIfNotExist(String campaignName, String usageType, String codeType,
			String noOfGuests, String usagePerGuest, String giftType, String amount, String locationName, String qcName,
			boolean globalRedemptionThrottlingToggle, int days, String hours, String minutes, String ampmselect) {

		boolean isCampaignExist = false;
		try {
			isCampaignExist = searchCouponCampaignWithStatus(campaignName);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (!isCampaignExist) {
			selectOfferdrpValue("Coupon");
			clickNewCampaignBtn();
			signupcampaignpage.createWhatDetailsCoupanCampaign(campaignName);
			signupcampaignpage.setCouponCampaignUsageType(usageType);
			signupcampaignpage.setCouponCampaignCodeGenerationType(codeType);
			signupcampaignpage.setCouponCampaignGuestUsagewithGiftAmount(noOfGuests, usagePerGuest, giftType, amount,
					locationName, qcName, globalRedemptionThrottlingToggle);

			if (days > 0) {
				try {
					signupcampaignpage.setEndDateTimeForCouponCampaign(days, hours, minutes, ampmselect);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			signupcampaignpage.createWhenDetailsCampaign();
			boolean status = validateSuccessMessage();
			Assert.assertTrue(status, "Campaign created success message did not displayed....");
			TestListeners.extentTest.get().pass("Coupon campaign created successfully");

		} else {
			logger.info(campaignName + " is available");
			TestListeners.extentTest.get().pass(campaignName + " is  available");

		}

		utils.refreshPage();
		selUtils.longWait(5000);
	}

	public boolean searchCouponCampaignWithStatus(String Campaignname) throws InterruptedException {
		boolean status = false;
		WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		searchBox.clear();
		searchBox.sendKeys(Campaignname);
		searchBox.sendKeys(Keys.ENTER);
		try {
			WebElement ele = driver.findElement(locators.get("campaignsPage.campaignName"));
			List<WebElement> statusList = driver.findElements(locators.get("campaignsPage.campgnStatus"));
			for (WebElement eleW : statusList) {
				String actualTextFromList = eleW.getText();
				if (actualTextFromList.equalsIgnoreCase("Active") && utils.checkElementPresent(ele)) {
					status = true;
					logger.info("Campaign Name already exists");
					break;
				}
			}
		} catch (Exception e) {
			logger.info("Element is not present or Campaign Name did not matched");
		}

		return status;
	}

	public void checkMassCampStatus() {
		int counter = 0;
		while (counter < 10) {
			List<WebElement> massCampstatus = driver.findElements(locators.get("campaignsPage.massCampstatus"));
			if (massCampstatus.size() > 0) {
				logger.info("campaign not get processed refreshing the page");
				TestListeners.extentTest.get().info("campaign not get processed refreshing the page");
				counter++;
				utils.refreshPage();
				utils.waitTillPagePaceDone();
			} else {
				logger.info("campaign get processed");
				TestListeners.extentTest.get().info("campaign get processed");
				break;
			}
		}
	}

	public void updateRewardExpiry(String date) {
		WebElement updateRewardExpiry = driver.findElement(locators.get("campaignsPage.updateRewardExpiry"));
		updateRewardExpiry.click();
		WebElement rewardExpiryDate = driver.findElement(locators.get("campaignsPage.rewardExpiryDate"));
		utils.waitTillInVisibilityOfElement(rewardExpiryDate, "date");
		rewardExpiryDate.sendKeys(date);
		WebElement expiryCalendarBtn = driver.findElement(locators.get("campaignsPage.expiryCalendarBtn"));
		expiryCalendarBtn.click();
		WebElement expiryTime = driver.findElement(locators.get("campaignsPage.expiryTime"));
		expiryTime.clear();
		expiryTime.sendKeys("11:00PM");
		WebElement processBtn = driver.findElement(locators.get("campaignsPage.processBtn"));
		processBtn.click();
		utils.waitTillPagePaceDone();
		utils.logInfo("Updated reward expiry, updated date is: " + date);
	}

	public void navigateToLastPageOfCamp() {
		utils.implicitWait(3);
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();
		nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		utils.waitTillPagePaceDone();

	}

	public void searchClassicCampaign1(String Campaignname) throws InterruptedException {
		// boolean status = false;
		driver.findElement(locators.get("campaignsPage.searchBox")).clear();
		WebElement searchBox = driver.findElement(locators.get("campaignsPage.searchBox"));
		searchBox.sendKeys(Campaignname);
		searchBox.sendKeys(Keys.ENTER);
		// try {
		// WebElement ele = utils.getLocator("campaignsPage.campaignName");
		// status = utils.checkElementPresent(ele);
		//
		// } catch (Exception e) {
		//
		// }
		// return status;
	}

	public void createDuplicateCampaign(String campaignName) {
		WebElement dotsIcon = driver.findElement(locators.get("newCamHomePage.dotsIcon"));
		utils.waitTillVisibilityOfElement(dotsIcon, "dotsIcon");
		dotsIcon.click();

		WebElement dotsIconDuplicateOption = driver.findElement(locators.get("newCamHomePage.dotsIconDuplicateOption"));
		utils.waitTillVisibilityOfElement(dotsIconDuplicateOption,
				"duplicate option");
		dotsIconDuplicateOption.click();

		logger.info("clicked duplicate option");

		WebElement duplicateCouponCampaignTitle = driver.findElement(locators.get("campaignsPage.duplicateCouponCampaignTitle"));
		String text = duplicateCouponCampaignTitle.getAttribute("value");
		Assert.assertEquals(text, campaignName + " - copy");
		logger.info("Duplicate Campaign Name is : " + text);
		TestListeners.extentTest.get().info("Duplicate Campaign Name is : " + text);
	}

	public String verifyCampaignLogs() {
		utils.waitTillPagePaceDone();
		WebElement campaignLogs = driver.findElement(locators.get("campaignsPage.campaignLogs"));
		String logParam = campaignLogs.getText();
		return logParam;

	}

	public String createDuplicateCampaignOnClassicPage(String campaignName, String option) {
		WebElement ellipsis = driver.findElement(locators.get("campaignsPage.Ellipsis"));
		ellipsis.click();
		utils.longWaitInSeconds(3);
		logger.info("clicked three dots");
		String xpath = utils.getLocatorValue("campaignsPage.selectOptionFromEllipsiseNew").replace("${val}", option);
		WebElement el = driver.findElement(By.xpath(xpath));
		utils.clickByJSExecutor(driver, el);
		// utils.longWaitInSeconds(3);
		WebElement duplicateBtnClassicPage = driver.findElement(locators.get("campaignsPage.duplicateBtnClassicPage"));
		utils.waitTillVisibilityOfElement(duplicateBtnClassicPage,
				"duplicateBtnClassicPage");
		duplicateBtnClassicPage.click();
		logger.info("clicked duplicate option");
		WebElement duplicateCouponCampaignTitle = driver.findElement(locators.get("campaignsPage.duplicateCouponCampaignTitle"));
		String text = duplicateCouponCampaignTitle.getAttribute("value");
		logger.info("Duplicate Campaign Name is : " + text);
		TestListeners.extentTest.get().info("Duplicate Campaign Name is : " + text);
		return text;
	}

	public String verifyModalPopupOptions(String option) {
		WebElement cancelbtn = driver.findElement(locators.get("signupCampaignsPage.cancelbtn"));
		cancelbtn.click();
		WebElement modalPopup = driver.findElement(locators.get("signupCampaignsPage.modalPopup"));
		modalPopup.isDisplayed();
		String text = modalPopup.getText();
		switch (option) {
		case "no":
			WebElement modalPopupNoOption = driver.findElement(locators.get("signupCampaignsPage.modalPopupNoOption"));
			selUtils.waitTillElementToBeClickable(modalPopupNoOption);
			selUtils.implicitWait(5);
			utils.clickByJSExecutor(driver, modalPopupNoOption);
			logger.info("Clicked on No option");
			TestListeners.extentTest.get().info("Clicked on No option");
			break;

		case "yes":
			WebElement modalPopupYesOption = driver.findElement(locators.get("signupCampaignsPage.modalPopupYesOption"));
			selUtils.waitTillElementToBeClickable(modalPopupYesOption);
			selUtils.implicitWait(5);
			utils.clickByJSExecutor(driver, modalPopupYesOption);
			logger.info("Clicked on Yes option");
			TestListeners.extentTest.get().info("Clicked on Yes option");
			break;
		}
		selUtils.implicitWait(5);
		return text;
	}

	public void clickOnSwitchToNCHPBtn() {
		List<WebElement> listWebElement = driver.findElements(locators.get("campaignsPage.switchToNCHPBtn"));
		if (listWebElement.size() != 0) {
			listWebElement.get(0).click();
		}
		utils.waitTillPagePaceDone();
	}

	public String getCouponCodeforCouponCamp() {
		WebElement couponCode = null;
		String code = "";
		try {
			couponCode = driver.findElement(locators.get("campaignsPage.couponCode"));
			code = couponCode.getText();
		} catch (NoSuchElementException e) {
			utils.logInfo("Coupon code is not available. Refreshing the page to check again...");
			utils.refreshPage();
			utils.waitTillPagePaceDone();
			couponCode = driver.findElement(locators.get("campaignsPage.couponCode"));
			code = couponCode.getText();
		}
		utils.logInfo("Coupon Code value received is : " + code);
		return code;
	}

	public boolean verifyIfModalPopupAppearsOrNot() {
		WebElement cancelBtn = driver.findElement(locators.get("campaignsPage.cancelBtn"));
		cancelBtn.click();
		logger.info("clicked cancel button");
		TestListeners.extentTest.get().info("clicked cancel button");
		boolean flag = false;
		try {
			WebElement modalPopup = driver.findElement(locators.get("signupCampaignsPage.modalPopup"));
			modalPopup.isDisplayed();
			logger.info("Modal Popup is visible");
			flag = true;
		} catch (Exception e) {
			logger.info("Modal Popup is not visible");
			flag = false;
		}
		return flag;
	}

	public String activateCampaign(String campaignName) {
		WebElement optionBtn = driver.findElement(locators.get("campaignsPage.optionBtn"));
		utils.waitTillVisibilityOfElement(optionBtn, "optionBtn");
		optionBtn.click();

		WebElement dotsIconActivateOption = driver.findElement(locators.get("campaignsPage.dotsIconActivateOption"));
		utils.waitTillVisibilityOfElement(dotsIconActivateOption,
				"activate option");
		dotsIconActivateOption.click();

		WebElement yesActivateBtn = driver.findElement(locators.get("campaignsPage.yesActivateBtn"));
		utils.waitTillVisibilityOfElement(yesActivateBtn,
				"yes activate button");
		yesActivateBtn.click();
		utils.waitTillPagePaceDone();
		utils.longWaitInSeconds(5);
		logger.info("successfully activate the campaign");
		TestListeners.extentTest.get().info("successfully activate the campaign");

		String xpath = utils.getLocatorValue("campaignsPage.statusActive").replace("$campName", campaignName);
		WebElement statusElement = driver.findElement(By.xpath(xpath));
		String status = statusElement.getText();
		return status;
	}

	public void campaignSummaryPageCollapseSymbol() {
		WebElement campaignSummaryPageCollapseSymbol = driver.findElement(locators.get("campaignsPage.campaignSummaryPageCollapseSymbol"));
		String value = campaignSummaryPageCollapseSymbol.getAttribute("aria-expanded");
		if (value.equalsIgnoreCase("false")) {
			WebElement clickCampaignSummaryPageCollapseSymbol = driver.findElement(locators.get("campaignsPage.clickCampaignSummaryPageCollapseSymbol"));
			clickCampaignSummaryPageCollapseSymbol.click();
			logger.info("Expanded Campaign Summary Page Collapse Symbol");
			TestListeners.extentTest.get().info("Expanded Campaign Summary Page Collapse Symbol");
		}
	}

	public String captureCampaignName() {
		WebElement duplicateCouponCampaignTitle = driver.findElement(locators.get("campaignsPage.duplicateCouponCampaignTitle"));
		String text = duplicateCouponCampaignTitle.getAttribute("value");
		logger.info("Duplicate Campaign Name is : " + text);
		TestListeners.extentTest.get().info("Duplicate Campaign Name is : " + text);
		return text;
	}

	public String checkMassCampStatusBeforeOpening(String camName, String status) {
		String statusVal = "";
		int attempts = 0;
		selUtils.implicitWait(2);
		while (attempts <= 25) {
			try {
				searchCampaign(camName);
				utils.longWaitInSeconds(4);
				WebElement classicCampaignStatus = driver.findElement(locators.get("campaignsPage.classicCampaignStatus"));
				statusVal = classicCampaignStatus.getText();
				if (statusVal.equalsIgnoreCase(status)) {
					logger.info("Campaign status is :" + statusVal);
					TestListeners.extentTest.get().pass("Campaign status is :" + statusVal);
					break;
				}
			} catch (Exception e) {
				logger.info("Campaign status is not matched " + attempts);
				TestListeners.extentTest.get().info("Campaign status is not matched " + attempts);
				utils.refreshPage();
			}
			attempts++;
		}
		selUtils.implicitWait(50);
		return statusVal;
	}

	public List<String> verifiedAndGetApplePassURlList(boolean isApplePassDisplayedChoice) {
		utils.implicitWait(5);
		List<String> applePassURl_List = new ArrayList<String>();
		List<WebElement> headerListWele = driver.findElements(locators.get("campaignsPage.couponCodeHeaderListXpath"));
		if (headerListWele.size() > 0) {
			System.out.println("Testing");
			for (WebElement wEle : headerListWele) {
				String headerText = wEle.getText();
				if (headerText.equalsIgnoreCase("Apple Pass URL")) {
					List<WebElement> urlListWele = driver
							.findElements(locators.get("campaignsPage.applePassURLLinksListXpath"));
					for (WebElement textWele : urlListWele) {
						System.out.println(textWele.getText() + " added into list");
						applePassURl_List.add(textWele.getText());
					}

				}

			}

		} else {
			logger.info("Apple Pass URL is not present in coupon code list page");
			TestListeners.extentTest.get().info("Apple Pass URL is not present in coupon code list page");

		}
		return applePassURl_List;

	}

	// This method is used to click on tags button and close tags button in
	// campaigns 'Whom' page
	public void openOrCloseTagsButton(String action) {
		if (action.equalsIgnoreCase("open")) {
			WebElement tagsButton = driver.findElement(locators.get("campaignsPage.tagsButton"));
			tagsButton.isDisplayed();
			tagsButton.click();
			logger.info("clicked on tags button");
			TestListeners.extentTest.get().info("clicked on tags button");
		} else if (action.equalsIgnoreCase("close")) {
			WebElement closeTagsButton = driver.findElement(locators.get("campaignsPage.closeTagsButton"));
			closeTagsButton.isDisplayed();
			closeTagsButton.click();
			logger.info("clicked on close tags button");
			TestListeners.extentTest.get().info("clicked on close tags button");
		}
	}

	// This method is used to get the tags and description list in campaigns 'Whom'
	// page
	public List<String> getTagsAndDescriptionList() throws InterruptedException {
		List<String> tagsDescList = new ArrayList<String>();
		List<WebElement> webEle = driver.findElements(locators.get("campaignsPage.tagsDescList"));
		for (WebElement ele : webEle) {
			utils.waitTillVisibilityOfElement(ele, "");
			tagsDescList.add(ele.getText());
		}
		logger.info("Tags and Description list is extracted ");
		TestListeners.extentTest.get().info("Tags and Description list is extracted ");
		return tagsDescList;
	}

	// This method is used to click on 'Send Test Notification' button and send test
	// notification in campaigns 'Whom' page
	public void sendTestNotification(String testEmail) {
		WebElement testUserEmail = driver.findElement(locators.get("signupCampaignsPage.testUserEmail"));
		testUserEmail.clear();
		testUserEmail.sendKeys(testEmail);
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement sendTestNotificationBtn = driver.findElement(locators.get("signupCampaignsPage.sendTestNotificationBtn"));
		js.executeScript("arguments[0].click();",
				sendTestNotificationBtn);
		WebElement testNotificationSuccessMsg = driver.findElement(locators.get("campaignsPage.testNotificationSuccessMsg"));
		utils.waitTillVisibilityOfElement(testNotificationSuccessMsg,
				"Test Notifications Sent");
		logger.info("Test Notification sent to: " + testEmail);
		TestListeners.extentTest.get().info("Test Notification sent to: " + testEmail);
	}

	public void sendTestNotificationWithGift(String testEmail) {
		WebElement testUserEmail = driver.findElement(locators.get("signupCampaignsPage.testUserEmail"));
		testUserEmail.clear();
		testUserEmail.sendKeys(testEmail);
		utils.longWaitInSeconds(5);
		WebElement IncludeTestNotification = driver.findElement(locators.get("signupCampaignsPage.IncludeTestNotification"));
		utils.waitTillElementToBeClickable(IncludeTestNotification);
		IncludeTestNotification.click();
		JavascriptExecutor js = (JavascriptExecutor) driver;
		WebElement sendTestNotificationBtn = driver.findElement(locators.get("signupCampaignsPage.sendTestNotificationBtn"));
		js.executeScript("arguments[0].click();",
				sendTestNotificationBtn);
		WebElement testNotificationSuccessMsg = driver.findElement(locators.get("campaignsPage.testNotificationSuccessMsg"));
		utils.waitTillVisibilityOfElement(testNotificationSuccessMsg,
				"Test Notifications Sent");
		logger.info("Test Notifications Sent");
		TestListeners.extentTest.get().info("Test Notifications Sent with Gift");

	}

	public String getJWTCodeWithEmailAndToken(String token, String email)
			throws InterruptedException, InvalidKeyException, UnsupportedEncodingException {
		@SuppressWarnings("deprecation")
		String jwt = Jwts.builder().claim("email", email).signWith(SignatureAlgorithm.HS256, token.getBytes("UTF-8"))
				.compact();
		logger.info("jwt code is : " + jwt);
		return jwt;
	}

	public void editOngoingSignupCampaigns(String enterTheNameofCampagin) {
		String campainNameXpath = utils.getLocatorValue("campaignsPage.campaignNameToEdit");
		campainNameXpath = campainNameXpath.replace("${campaignName}", enterTheNameofCampagin);
		WebElement campaign = driver.findElement(By.xpath(campainNameXpath));
		campaign.click();
		WebElement optionForEdit = driver.findElement(locators.get("campaignsPage.optionsDropDown"));
		optionForEdit.click();
		WebElement editButton = driver.findElement(locators.get("campaignsPage.editButton"));
		editButton.click();
		WebElement nextBtn = driver.findElement(locators.get("signupCampaignsPage.nextBtn"));
		nextBtn.click();
		// utils.longWaitInSeconds(50);
	}

	public void selectSegmentFromDropDown(String enterTheSegmentName) {
		WebElement segmentDropDown = driver.findElement(locators.get("signupCampaignsPage.segmentDrp"));
		segmentDropDown.click();
		WebElement searchField = driver.findElement(locators.get("signupCampaignsPage.segmentSearchField1"));
		searchField.clear();
		searchField.sendKeys(enterTheSegmentName);
		searchField.sendKeys(Keys.ENTER);
	}

	public int getABCampaignsTotalSegmentSize() {
		WebElement totalSizeSpan = driver.findElement(By.xpath("//span[@data-appends='total_size']"));
		String val = totalSizeSpan.getText();
		return Integer.parseInt(val);
	}

	public void createDuplicateCampaigns(String campaignName) {
		utils.waitTillPagePaceDone();
		WebElement searchCamBox = driver.findElement(locators.get("newCamHomePage.searchCamBox"));
		searchCamBox.clear();
		searchCamBox.sendKeys(campaignName);
		WebElement dotsIcon = driver.findElement(locators.get("newCamHomePage.dotsIcon"));
		utils.waitTillVisibilityOfElement(dotsIcon, "dotsIcon");
		utils.tryAllClick(driver, dotsIcon);
		utils.clickWithActions(dotsIcon);
		WebElement dotsIconDuplicateOption = driver.findElement(locators.get("newCamHomePage.dotsIconDuplicateOption"));
		utils.waitTillVisibilityOfElement(dotsIconDuplicateOption,
				"duplicate option");
		dotsIconDuplicateOption.click();
		logger.info("clicked duplicate option");
		WebElement duplicateCouponCampaignTitle = driver.findElement(locators.get("campaignsPage.duplicateCouponCampaignTitle"));
		String text = duplicateCouponCampaignTitle.getAttribute("value");
		logger.info("Duplicate Campaign Name is : " + text);
		TestListeners.extentTest.get().info("Duplicate Campaign Name is : " + text);
	}

	public String getCampaignStatus() {

		WebElement campaignStatus = driver.findElement(locators.get("campaignsPage.campaignStatus"));
		String statusVal = campaignStatus.getText();
		logger.info("Status of campaign is : " + statusVal);
		TestListeners.extentTest.get().info("Status of campaign is : " + statusVal);
		return statusVal;
	}

	public void clikOnMoreOptionAndUpdateRewardExpiryForPostRedemption(String date) {
		LocalDate today = LocalDate.now();
		utils.longWaitInSeconds(2);
		WebElement optionBtn = driver.findElement(locators.get("campaignsPage.optionBtn"));
		utils.waitTillElementToBeClickable(optionBtn);
		utils.clickByJSExecutor(driver, optionBtn);
//		optionBtn.click();
		// utils.clickByJSExecutor(driver, optionBtn);
		WebElement updateRewardExpiryOnNewCPP = driver.findElement(locators.get("campaignsPage.updateRewardExpiryOnNewCPP"));
		updateRewardExpiryOnNewCPP.click();

		WebElement datePicker = driver.findElement(locators.get("campaignsPage.rewardExpiryDatePickerNewCPP"));
		utils.waitTillElementToBeClickable(datePicker);
		utils.longWaitInSeconds(1);
		datePicker.click();
		String xpath = utils.getLocatorValue("campaignsPage.rewardExpiryDateNewCPP").replace("$temp", date);
		WebElement inputDate = driver.findElement(By.xpath(xpath));
		inputDate.click();
		utils.longWaitInMiliSeconds(300);
		WebElement submitBtn = driver.findElement(locators.get("campaignsBetaPage.submitBtn"));
		utils.clickUsingActionsClass(submitBtn);
		utils.waitTillPagePaceDone();
		logger.info("Clicked on update reward expiry, updated date is " + today.getYear() + "-" + today.getMonthValue()
				+ "-" + date);
		TestListeners.extentTest.get().info("Clicked on update reward expiry, updated date is " + today.getYear() + "-"
				+ today.getMonthValue() + "-" + date);
	}

	public boolean pollQueryForExpectedValues(String envName, String sql, String[] colNames, int n, String[] expVals)
			throws Exception {
		List<String> actualValues = new ArrayList<>();
		for (int attempt = 1; attempt <= n; attempt++) {
			List<Map<String, String>> rows = DBUtils.executeQueryAndGetMultipleColumns(envName, sql, colNames);
			for (Map<String, String> row : rows) {
				boolean allMatch = true;
				for (int i = 0; i < colNames.length; i++) {
					String actual = row.get(colNames[i]);
					actualValues.add(actual);
					String expected = expVals[i];
					if (actual == null || !actual.contains(expected)) {
						allMatch = false;
						break;
					}
				}
				if (allMatch) {
					logger.info("Match found on attempt " + attempt + ": " + row);
					return true;
				}
			}
			if (attempt < n) {
				logger.info("No match found on attempt " + attempt + ", retrying...");
				TestListeners.extentTest.get().info("No match found on attempt " + attempt + ", retrying...");
				Utilities.longWait(1000); // wait 1 second before next attempt
			}
		}
		logger.info("No match found after " + n + " attempts.");
		return false;
	}

	public boolean isCamLogContainsMsg(String message) throws InterruptedException {
		String log = "";
		boolean isPresent = false;
		int attempts = 0;
		while (attempts < 5) {
			utils.refreshPage();
			Utilities.longWait(2000);
			try {
				WebElement massCampiagnLogMsg = driver.findElement(locators.get("campaignsPage.massCampiagnLogMsg"));
				log = massCampiagnLogMsg.getText();
				if (log.contains(message)) {
					isPresent = true;
					break;
				}
			} catch (Exception e) {
				logger.info("Exception in fetching campaign logs, retrying..." + attempts);
			}
			attempts++;
		}
		return isPresent;
	}

}
