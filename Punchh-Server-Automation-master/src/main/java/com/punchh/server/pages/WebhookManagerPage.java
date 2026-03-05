package com.punchh.server.pages;

import java.awt.HeadlessException;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class WebhookManagerPage {
	static Logger logger = LogManager.getLogger(WebhookManagerPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;
	private static String cookiesVar;

	public WebhookManagerPage(WebDriver driver) {
		this.driver = driver;
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj =  new PageObj(driver);
	}

	public void navigateToWebhookManagerTab(String tabName) {
		utils.longWaitInSeconds(2);
		selUtils.switchToIframe("iframe");
		driver.switchTo().frame(driver.findElement(By.tagName("iframe")));		
		String xpath = utils.getLocatorValue("webhookManagerPage.tabNameXpath").replace("$TabName", tabName);
		driver.findElement(By.xpath(xpath)).click();
		utils.longWaitInSeconds(2);
		driver.switchTo().defaultContent();
	}
	
	public void navigateToInboundWebhookManagerTab(String tabName) {
		utils.waitTillPagePaceDone();
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
	    clickTab("webhookManagerPage.inboundTabNameXpath", tabName);
		utils.longWaitInSeconds(1);
		logger.info(tabName +" tab is clicked ");
		TestListeners.extentTest.get().info(tabName +" tab is clicked ");
		driver.switchTo().defaultContent();
	}
	
	private void clickTab(String locatorKey, String tabName) {
	    String tabXpath = utils.getLocatorValue(locatorKey).replace("$TabName", tabName);
	    driver.findElement(By.xpath(tabXpath)).click();
	}
	
	// Fetch Inbound Service Logs List
	public List<WebElement> fetchInboundLogsList() {
		driver.switchTo().frame(driver.findElement(By.tagName("iframe")));
		List<WebElement> listOfSuccess = utils.getLocatorList("webhookManagerPage.successTabXpath");
		return listOfSuccess;
	}
	
	// Fetch Inbound Service Logs Data
	public JSONObject fetchInboundLogData(List<WebElement> listOfSuccess, int index) {
	    WebElement wEle = listOfSuccess.get(index);
	    logger.info("Clicking on element at index {}.", index);
	    wEle.click();
	    utils.longWaitInSeconds(1);
	    JSONObject actBodyData = new JSONObject(getLogsBodyData("webhookManagerPage.inboundBodyDataXpath"));
	    logger.info("Successfully retrieved JSON data: {}", actBodyData);
	    closeJsonView();
	    return actBodyData;
	}
	
	public String getLogsBodyData(String locator) {
	    WebElement bodyData = driver.findElement(By.xpath(utils.getLocatorValue(locator)));
	    return bodyData.getText();
		
	}
	
	public boolean verifyTheSrtatusLogs(String tagName, String expectedValue) {
		driver.switchTo().frame(driver.findElement(By.tagName("iframe")));
		List<WebElement> listOfSuccess = utils.getLocatorList("webhookManagerPage.successTabXpath");
		boolean flagResult = false;
		if (listOfSuccess.size() != 0) {
			for (WebElement wEle : listOfSuccess) {

				wEle.click();
				utils.longWaitInSeconds(2);
				String jsonKeyXpath = utils.getLocatorValue("webhookManagerPage.jsonResponseTagsXpath")
						.replace("$tagName", tagName);
				String actualKeyValue = driver.findElement(By.xpath(jsonKeyXpath)).getText().replace("\"", "");

				if (expectedValue.equalsIgnoreCase(actualKeyValue)) {
					flagResult = true;
					logger.info(tagName + " value  " + actualKeyValue + " is matched with the expected tagvalue i.e "
							+ expectedValue);
					TestListeners.extentTest.get().info(tagName + " value  " + actualKeyValue
							+ " is matched with the expected tagvalue i.e " + expectedValue);

					break;
				} else {
					utils.getLocator("webhookManagerPage.jsonCloseButton").click();
				}

			}

		}
		return flagResult;

	}
	
	public void closeJsonView() {
	    utils.getLocator("webhookManagerPage.jsonCloseButton").click();
		logger.info("Closing JSON view.");
	}
	
	
	public void clickOnCreateWebhookButton() {
		utils.waitTillPagePaceDone();
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		utils.getLocator("webhookManagerPage.createWebhookBtn").click();
		//utils.longWaitInSeconds(1);
		logger.info("Clicked on create webhook button");
		TestListeners.extentTest.get().info("Clicked on create webhook button");
		driver.switchTo().defaultContent();
	}

	// used to select value from dropdown on Webhook Create Page
	public void selectValueFromDropDownOnWebhookCreatePage(String dropDownNameLabel, String value) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String xpath = utils.getLocatorValue("webhookManagerPage.dropdownFieldsXpath").replace("${dropDownNameLabel}", dropDownNameLabel);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		WebElement dropDown = driver.findElement(By.xpath(xpath));
		utils.selectDrpDwnValue(dropDown, value);
		//utils.longWaitInSeconds(1);
		logger.info(dropDownNameLabel +" dropdown label is clicked ");
		TestListeners.extentTest.get().info(dropDownNameLabel +" dropdown label is clicked ");
		driver.switchTo().defaultContent();
	}

	// used to enter value in text box on Webhook Create Page
	public void enterValueInInputBoxOnWebhookCreatePage(String inputboxLabelName, String value) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String xpath = utils.getLocatorValue("webhookManagerPage.inputFieldsXpath").replace("${inputboxLabelName}", inputboxLabelName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		WebElement inputBox = driver.findElement(By.xpath(xpath));
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].value = '';", inputBox);
		inputBox.sendKeys(value);
		utils.longWaitInSeconds(1);
		logger.info(value +" value is entered in  "+ inputboxLabelName +" inputbox");
		TestListeners.extentTest.get().info(value +" value is entered in  "+ inputboxLabelName +" inputbox");
		driver.switchTo().defaultContent();
	}

	// used to select event
	public void selectEvent(List<String> eventSelectionNameList) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		utils.getLocator("webhookManagerPage.eventsSelectionInputBoxXpath").click();
		utils.implicitWait(2);
		try {
			for (String eventName : eventSelectionNameList) {
				WebElement searchBoxWEle = utils.getLocator("webhookManagerPage.eventSelectSearchBoxXpath");
				searchBoxWEle.clear();
				searchBoxWEle.sendKeys(eventName);
				utils.longWaitInSeconds(1);
				String xpath1 = utils.getLocatorValue("webhookManagerPage.searchedEventCheckBoxXpath")
						.replace("${eventName}", eventName);

				try {
					
					WebElement eventCheckBox = driver.findElement(By.xpath(xpath1));
					eventCheckBox.click();
					logger.info(eventName + " event is selected ");
					TestListeners.extentTest.get().info(eventName + " event is selected ");
				} catch (Exception e) {
					logger.info(eventName + " event is not visible may be it is in subcategory ");
					TestListeners.extentTest.get()
							.info(eventName + " event is not visible may be it is in subcategory ");

					utils.getLocator("webhookManagerPage.expandCollapseButtonXpath").click();
					utils.longWaitInSeconds(1);
					String subCategorEventCheckBoxXpath = utils
							.getLocatorValue("webhookManagerPage.clickOnSubCategoryEventXpath")
							.replace("${eventName}", eventName);
					utils.scrollToElement(driver, driver.findElement(By.xpath(subCategorEventCheckBoxXpath)));
					driver.findElement(By.xpath(subCategorEventCheckBoxXpath)).click();

				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.info("WebhookManagerPage.selectEvent() " + e.getMessage());
		}
		utils.longWaitInSeconds(1);
		utils.getLocator("webhookManagerPage.eventsSelectionInputBoxXpath").click();
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
	}

	// used to select event
	public void clickOnSubmitButton() {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		utils.scrollToElement(driver, utils.getLocator("webhookManagerPage.submitButtonXpath"));
		WebElement submitButtonWele = utils.getLocator("webhookManagerPage.submitButtonXpath");
		utils.scrollToElement(driver, submitButtonWele);
		utils.waitTillElementToBeClickable(submitButtonWele);
		submitButtonWele.click();
		logger.info("Submit button is clicked");
		TestListeners.extentTest.get().info("Submit button is clicked");
		utils.longWaitInSeconds(2);
		driver.switchTo().defaultContent();
	}


	public void clickOnActiveCheckBox(boolean toBeClicked){
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		WebElement activeCheckBoxWele = utils.getLocator("webhookManagerPage.activeCheckBoxXpath");
		utils.waitTillElementToBeClickable(activeCheckBoxWele);
		utils.scrollToElement(driver, activeCheckBoxWele);
		activeCheckBoxWele.click();
		activeCheckBoxWele.click();
		WebElement submitButtonWele = utils.getLocator("webhookManagerPage.submitButtonXpath");
		String submitButtonText = submitButtonWele.getText() ;

		if(submitButtonText.equalsIgnoreCase("Submit") &&  toBeClicked ){
			activeCheckBoxWele.click();
			logger.info("Active button is active now");
			TestListeners.extentTest.get().info("Active button is active now");

		} else if (submitButtonText.equalsIgnoreCase("Verify & Submit") &&  !toBeClicked ) {
			activeCheckBoxWele.click();
			logger.info("Active button is not active");
			TestListeners.extentTest.get().info("Active button is not active");
		}
		driver.switchTo().defaultContent();
	}

	public String openLogsForWebhook(String userEmail, String eventName, String webhookName ,String jsonCondition, String currentTimeStamp)
			throws InterruptedException, HeadlessException, UnsupportedFlavorException, IOException {
		String eventStatusJsonBody = "";
		String finalEventStatusJsonBody = "";
		String logsStatusCode="";
		String xpathText ="" ;
		boolean flag = false;

		String fetchActionCondition =null ; 
		if(eventName.equalsIgnoreCase("User Subscription")) {
			fetchActionCondition = "\"action\":\"update\"";
		} else {
			fetchActionCondition = "\"action\":\"create\"" ; 
		}
	
		selectDropDownValueInLogsTabPage("Events", eventName) ; 
		selectDropDownValueInLogsTabPage("Webhooks", webhookName) ; 

		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe"); 
		String additionalFilterXpath =utils.getLocatorValue("webhookManagerPage.AdditionalFilterButtonXpath") ;
		xpathText = driver.findElement(By.xpath(additionalFilterXpath)).getAttribute("title") ; 
		
		if(xpathText.equalsIgnoreCase("Show Additional Filters")) {
			utils.getLocator("webhookManagerPage.showAdditionalFilterButtonXpath").click();
			utils.longWaitInSeconds(2);
		}
		utils.getLocator("webhookManagerPage.additionalInfoEmailInputBoxXpath").clear();
		utils.getLocator("webhookManagerPage.additionalInfoEmailInputBoxXpath").sendKeys(userEmail);
		utils.getLocator("webhookManagerPage.additionalSearchedButtonXpath").click();
		utils.longWaitInSeconds(2);
		int counter = 0;
		utils.implicitWait(3);
		List<WebElement> listOfSuccessButtonFinal = null;
			while (!flag && counter < 25) {
				String listOfResultSuccessXpath = utils.getLocatorValue("webhookManagerPage.openWebhookLogsXpath")
						.replace("${eventName}", eventName).replace("${webhookName}", webhookName);

				String xpathTimeStamp = utils.getLocatorValue("webhookManagerPage.listOfTimeStampXpath")
						.replace("${eventName}", eventName).replace("${webhookName}", webhookName);
				List<WebElement> timeStampList = driver.findElements(By.xpath(xpathTimeStamp));

				List<WebElement> listOfSuccessBtnWele = driver.findElements(By.xpath(listOfResultSuccessXpath));
				listOfSuccessButtonFinal = new ArrayList<>();
				int indexCounter = 0;
				for (WebElement timeStampWEle : timeStampList) {
					String timeStampText = timeStampWEle.getText().trim();
					String isTimeAfterCurrentTimeStamp = checkTimeFromUIIsAfterCurrentTimeStamp(currentTimeStamp,
							timeStampText);
					logger.info("Time stamp from UI - "+ timeStampText);
					TestListeners.extentTest.get().info("Time stamp from UI - "+ timeStampText);
					if (isTimeAfterCurrentTimeStamp.equalsIgnoreCase("After") || isTimeAfterCurrentTimeStamp.equalsIgnoreCase("Equal")) {
						listOfSuccessButtonFinal.add(listOfSuccessBtnWele.get(indexCounter));
					}
					indexCounter++;
				}
				if (listOfSuccessButtonFinal.size() != 0) {
					flag = true;
					break;
				}
				WebElement wEleTimeSlotSelect = utils.getLocator("webhookManagerPage.timeSlotDropdownXpath");
				Select sel = new Select(wEleTimeSlotSelect);
				sel.selectByVisibleText("Today");
				utils.longWaitInSeconds(3);
				sel.selectByVisibleText("Last 30 Min.");
				counter++;
			}
		String xpath = utils.getLocatorValue("webhookManagerPage.openWebhookLogsXpath")
				.replace("${eventName}", eventName).replace("${webhookName}", webhookName);
		List<WebElement> list1 = driver.findElements(By.xpath(xpath));

		for (WebElement ele : listOfSuccessButtonFinal) {
			ele.click();
			utils.longWaitInSeconds(2);
			int copyDivIndexNumber = 0;
			List<WebElement> copyDivList = utils.getLocatorList("webhookManagerPage.jsonBodyIndexXpath");
			copyDivIndexNumber = copyDivList.size();
			String copyDivXpath = utils.getLocatorValue("webhookManagerPage.jsonBodyLogsXpath").replace("${indexNum}",
					String.valueOf(copyDivIndexNumber));

			String xpathOfCopyButton = utils.getLocatorValue("webhookManagerPage.copyButtonXpath")
					.replace("${copyDivIndexNumber}", copyDivXpath);
			eventStatusJsonBody = driver.findElement(By.xpath(copyDivXpath)).getText();
			if (!jsonCondition.isEmpty() && eventStatusJsonBody.contains(fetchActionCondition)
					&& eventStatusJsonBody.contains(jsonCondition)) {
				finalEventStatusJsonBody = eventStatusJsonBody;
				logsStatusCode = utils.getLocator("webhookManagerPage.logsStatusCodeXpath").getText().replaceAll("^(\\d+).*", "$1");;
				finalEventStatusJsonBody = finalEventStatusJsonBody + "\n\"WebhookStatusCode\":"+logsStatusCode;
				utils.getLocator("webhookManagerPage.closeLogsWindowXpath").click();
				logger.info("WebhookManagerPage.openLogsForWebhook() Inside ifcondition");
				break;
			} else if (jsonCondition.isEmpty() && eventStatusJsonBody.contains(fetchActionCondition)) {
				finalEventStatusJsonBody = eventStatusJsonBody;
				logsStatusCode = utils.getLocator("webhookManagerPage.logsStatusCodeXpath").getText().replaceAll("^(\\d+).*", "$1");;
				finalEventStatusJsonBody = finalEventStatusJsonBody + "\n\"WebhookStatusCode\":"+logsStatusCode;
				utils.getLocator("webhookManagerPage.closeLogsWindowXpath").click();
				break;
			} else {
				utils.getLocator("webhookManagerPage.closeLogsWindowXpath").click();

			}

		}
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		return finalEventStatusJsonBody;
	}
	public String checkTimeFromUIIsAfterCurrentTimeStamp(String currentTimeStamp, String timeStampFromUIVar) {
		String returnTimeStamp = null;
		String timeStr1FromUI =timeStampFromUIVar.trim().toLowerCase();
		currentTimeStamp = currentTimeStamp.trim().toLowerCase();
		DateTimeFormatter formatter = new DateTimeFormatterBuilder()
		        .parseCaseInsensitive() // ← Accepts "pm", "PM", etc.
		        .appendPattern("dd/MM/yy hh:mm:ss a")
		        .toFormatter(Locale.ENGLISH);
        LocalDateTime time1 = LocalDateTime.parse(currentTimeStamp, formatter);
        LocalDateTime time2 = LocalDateTime.parse(timeStr1FromUI, formatter);
        
        if (time2.isAfter(time1)) {
            returnTimeStamp = "After";
        } else if (time2.isBefore(time1)) {
        	 returnTimeStamp = "Before";
        } else {
            returnTimeStamp = "Equal";
        }
        return returnTimeStamp;
        
	}

	// it click on 3 dots and perform action on webhook eg. edit, delete / evFrameworkType= "adapter" or "webhook"
	public void deleteWebhook(String evFrameworkType ,  String webhookName, String actionOption) {
		navigateToInboundWebhookManagerTabNew(evFrameworkType);
		utils.longWaitInSeconds(5);
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String xpath = utils.getLocatorValue("webhookManagerPage.webhookActionThreeDotsIconXpath")
				.replace("${webhookName}", webhookName);
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
		driver.findElement(By.xpath(xpath)).click();
		utils.longWaitInSeconds(4);
		String actionOptionButtonXpath = utils.getLocatorValue("webhookManagerPage.actionOptionsXpath")
				.replace("${actionOption}", actionOption);
		WebElement deleteButton = driver.findElement(By.xpath(actionOptionButtonXpath));
		utils.waitTillElementToBeClickable(deleteButton);
		utils.clickByJSExecutor(driver, deleteButton);
		//deleteButton.click();
		utils.longWaitInSeconds(3);
		String xpathofDeleteConfirmationButton = utils.getLocatorValue("webhookManagerPage.actionIconForWebhookXpth").replace("${webHookName}" , webhookName) ;
		utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpathofDeleteConfirmationButton)));
		driver.findElement(By.xpath(xpathofDeleteConfirmationButton)).click();
		logger.info("Clicked on " + actionOption + " button for webhook : " + webhookName);
		TestListeners.extentTest.get().info("Clicked on " + actionOption + " button for webhook : " + webhookName);
		driver.switchTo().defaultContent();
	}
	
	
	
	public void navigateToInboundWebhookManagerTabNew(String tabName) {
		utils.implicitWait(15);
		utils.waitTillPagePaceDone();
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String tabXpath = utils.getLocatorValue("webhookManagerPage.tabNameXpath").replace("$TabName", tabName);
		try {
			// wait for the navigation bar to be present
			new WebDriverWait(driver, Duration.ofSeconds(10))
					.until(ExpectedConditions.presenceOfElementLocated(By.id("event-framework-nav")));
		} catch (Exception e) {
			logger.info(
					"Navigation bar with id event-framework-nav not found within 10 seconds, proceeding without wait.");
			TestListeners.extentTest.get().info(
					"Navigation bar with id event-framework-nav not found within 10 seconds, proceeding without wait.");
		}
		try {
			// Now wait for the Adapters tab to be clickable
			new WebDriverWait(driver, Duration.ofSeconds(30))
					.until(ExpectedConditions.elementToBeClickable(By.xpath(tabXpath)));
		} catch (Exception e) {
			logger.info(tabName + " tab is not found within 30 seconds, proceeding without wait.");
			TestListeners.extentTest.get()
					.info(tabName + " tab is not found within 30 seconds, proceeding without wait.");
		}

//		utils.clickByJSExecutor(driver, driver.findElement(By.xpath(tabXpath)));
//		utils.clickUsingActionsClass(driver.findElement(By.xpath(tabXpath)));
//		driver.findElement(By.xpath(tabXpath)).click();
		utils.longWaitInSeconds(5);
		utils.tryAllClick(driver, driver.findElement(By.xpath(tabXpath)));
		utils.longWaitInSeconds(5);
		logger.info(tabName + " tab is clicked ");
		TestListeners.extentTest.get().info(tabName + " tab is clicked ");
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
	}
	
	
	// get webhook status from UI
	public String getWebhookStatus(String webhookName) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String xpath = utils.getLocatorValue("webhookManagerPage.webhookStatusXpath")
				.replace("${webhookName}", webhookName);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), webhookName);
		WebElement statusElement = driver.findElement(By.xpath(xpath));
		String status = statusElement.getText();
		logger.info("Webhook Status: " + status);
		TestListeners.extentTest.get().info("Webhook Status: " + status);
		driver.switchTo().defaultContent();
		return status;
	}
	
	
	public void clickOnWebhookName(String webhookName) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String xpath = utils.getLocatorValue("webhookManagerPage.webhookNameXpath")
				.replace("${webhookName}", webhookName);
		utils.waitTillVisibilityOfElement(driver.findElement(By.xpath(xpath)), webhookName);
		WebElement statusElement = driver.findElement(By.xpath(xpath));
		statusElement.click();
		logger.info("Clicked on Webhook Name: " + webhookName);
		TestListeners.extentTest.get().info("Clicked on Webhook Name: " + webhookName);
		driver.switchTo().defaultContent();
	}
	
	public void clickOnConfigurationFlag(String flagName, boolean toBeChecked) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		utils.implicitWait(5);
		WebElement checkBox = driver.findElement(By.xpath("//label[contains(text(),'" + flagName + "')]"));
		utils.scrollToElement(driver, checkBox);
		// Locate the checkbox or radio button
		logger.info("Before uncheckedCheckboxes");
		List<WebElement> uncheckedCheckboxes = driver
				.findElements(By.cssSelector(".custom-checkbox .custom-control-input:not(:checked)"));
		logger.info("After uncheckedCheckboxes");
		logger.info("uncheckedCheckboxes size: " + uncheckedCheckboxes.size());
		TestListeners.extentTest.get().info("uncheckedCheckboxes size: " + uncheckedCheckboxes.size());

		utils.implicitWait(5);
		for (WebElement checkbox1 : uncheckedCheckboxes) {
			try {

				if (checkbox1.isDisplayed()) {
					checkbox1.findElement(By.xpath("following-sibling::label")).click();
				}
			} catch (Exception e) {
			}
		}
		if (!toBeChecked) {
			
			// Click the checkbox to change its state
			checkBox.click();

			// Wait until the element gets the expected state
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
			wait.until(ExpectedConditions.elementToBeClickable(checkBox));
		}
		logger.info("Clicked on configuration flag: " + flagName);
		TestListeners.extentTest.get().info("Clicked on configuration flag: " + flagName);
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		utils.longWaitInSeconds(2);
	}
	
	
	public void activeOrInactiveEventsFromConfiguration(String eventName, String sourceEvent, String targetEvent) {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");

		// get all Source Event list name
		String sourceEventXpath = utils.getLocatorValue("webhookManagerPage.sourceEventListXpath").replace("${sourceEvent}", sourceEvent);

		List<WebElement> sourceEventList = driver.findElements(By.xpath(sourceEventXpath));
		for (WebElement sourceEvent1 : sourceEventList) {
			String sourceEventName = sourceEvent1.getText();
			if (sourceEventName.equalsIgnoreCase(eventName)) {
				sourceEvent1.click();
				break;
			}
		}

		// verified that event is move to target event side
		// get all Target Event list name
		String targetEventXpath = utils.getLocatorValue("webhookManagerPage.targetEventListXpath").replace("${targetEvent}", targetEvent);
		List<WebElement> targetEventList = driver.findElements(By.xpath(targetEventXpath));
		for (WebElement targetEvent1 : targetEventList) {
			String targetEventName = targetEvent1.getText();
			if (targetEventName.equalsIgnoreCase(eventName)) {
				//targetEvent1.click();
				logger.info(eventName + " event is moved to " + targetEvent);
				TestListeners.extentTest.get().info(eventName + " event is moved to " + targetEvent);
			
				break;
			}
		}

		driver.switchTo().defaultContent();
		utils.longWaitInSeconds(2);
	}
	public void clickOnCreateAdapterButton() {
		utils.implicitWait(10);
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		utils.waitTillElementToBeClickable(utils.getLocator("webhookManagerPage.createAdapterBtnXpath"));
		utils.getLocator("webhookManagerPage.createAdapterBtnXpath").click();
		utils.longWaitInSeconds(1);
		logger.info("Clicked on create adapter button");
		TestListeners.extentTest.get().info("Clicked on create adapter button");
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
	}
	
	public String getWebhookCookie() {
		String cookies =  getCookiesFromIframeURL();
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		return cookies;
		
	}
	
	
	//"show_webhooks": true,
//    "show_adapters": true,
//    "enable_checkin_menu_items": true,
//    "enable_redemption_menu_items": true,
//    "enable_coupon_menu_items": true,
//    "enable_rate_limit": false,
//    "enable_success_headers_logging": false,
	public void clickOnConfigurationFlagUsingAPI(String type, String configFlagName, boolean toBeChecked) {
		//get URL from iframe
		String urlCookies = driver.findElement(By.tagName("iframe")).getAttribute("src");
		logger.info("URL of the iframe: " + urlCookies);
		
		// navigate to URL
		String cookies =  getCookiesFromIframeURL();
		
		// get the cookie
		
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		String configFlagID = ""; // "//label[contains(text(),'" + configFlagName + "')]";
		switch (configFlagName) {
		case "Show Webhooks Tab":
			configFlagID = "show_webhooks";
			break;
		case "Show Adapters Tab":
			configFlagID = "show_adapters";
			break;
		case "Enable Checkin Menu Items":
			configFlagID = "enable_checkin_menu_items";
			break;
		case "Enable Redemption Menu Items":
			configFlagID = "enable_redemption_menu_items";
			break;
		case "Enable Coupon Menu Items":
			configFlagID = "enable_coupon_menu_items";
			break;
		case "Enable Success Headers Logging":
			configFlagID = "enable_success_headers_logging";
			break;
		default:
			break;

		}
		Response response = pageObj.endpoints().getStatusOfFlagFromConfigurationPageInWebhook(type,cookies);

		boolean currentStatusIsClicked = response.jsonPath().getBoolean("business_config." + configFlagID);

		WebElement checkBox = driver.findElement(By.xpath("//label[contains(text(),'" + configFlagName + "')]"));
		utils.waitTillElementToBeClickable(checkBox);
		utils.scrollToElement(driver, checkBox);
		// Locate the checkbox or radio button
		utils.implicitWait(5);

		// toBeChecked = true & currentStatusIsClicked = true
		if (toBeChecked && currentStatusIsClicked) {
			logger.info(configFlagName + " is already checked");
			TestListeners.extentTest.get().info(configFlagName + " is already checked");
		} else if (!toBeChecked && !currentStatusIsClicked) {
			logger.info(configFlagName + " is already unchecked");
			TestListeners.extentTest.get().info(configFlagName + " is already unchecked");
		} else {
			checkBox.click();
			utils.longWaitInSeconds(1);
			logger.info("Clicked on configuration flag: " + configFlagName);
			TestListeners.extentTest.get().info("Clicked on configuration flag: " + configFlagName);
		}
		logger.info("Clicked on configuration flag: " + configFlagName);
		TestListeners.extentTest.get().info("Clicked on configuration flag: " + configFlagName);
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		utils.longWaitInSeconds(2);

	}
	
	public void changeStatusActiveOrInActiveOfWebhookOrAdapter(String type, String webhookOrAdapterName,
			String configFlagName, boolean toBeChecked) {
		pageObj.webhookManagerPage().clickOnWebhookName(webhookOrAdapterName);
		String cookies =  getCookiesFromIframeURL();

		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		boolean isActive = false;
		Response response = pageObj.endpoints().getStatusOfFlagFromConfigurationPageInWebhook(type,cookies);

		List<String> listOfNames = response.jsonPath().getList("name");
		int counter = 0;
		for (String name : listOfNames) {
			if (name.toString().equalsIgnoreCase(webhookOrAdapterName)) {

				isActive = response.jsonPath().getBoolean("[" + counter + "].is_active");
				logger.info(webhookOrAdapterName + " Webhook or Adapter is " + isActive);

			} else {
				counter++;
			}
		}

		WebElement checkBox = driver.findElement(By.xpath("//label[contains(text(),'" + configFlagName + "')]"));
		utils.waitTillElementToBeClickable(checkBox);
		utils.scrollToElement(driver, checkBox);
		// Locate the checkbox or radio button
		utils.implicitWait(5);

		// toBeChecked = true & currentStatusIsClicked = true
		if (toBeChecked && isActive) {
			logger.info(configFlagName + " is already checked");
			TestListeners.extentTest.get().info(configFlagName + " is already checked");
		} else if (!toBeChecked && !isActive) {
			logger.info(configFlagName + " is already unchecked");
			TestListeners.extentTest.get().info(configFlagName + " is already unchecked");
		} else {
			checkBox.click();
			utils.longWaitInSeconds(1);
			logger.info("Clicked on configuration flag: " + configFlagName);
			TestListeners.extentTest.get().info("Clicked on configuration flag: " + configFlagName);

		}
		logger.info("Clicked on configuration flag: " + configFlagName);
		TestListeners.extentTest.get().info("Clicked on configuration flag: " + configFlagName);
		utils.implicitWait(50);
		driver.switchTo().defaultContent();
		utils.longWaitInSeconds(2);

	}
	
	public void clickOnCloseButtonForAdapter() {
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");
		utils.longWaitInSeconds(2);
		utils.waitTillElementToBeClickable(utils.getLocator("webhookManagerPage.adapterConfirmationPopupCloseBtnXpath"));
		utils.getLocator("webhookManagerPage.adapterConfirmationPopupCloseBtnXpath").click();
		utils.longWaitInSeconds(1);
		logger.info("Clicked on Close button");
		TestListeners.extentTest.get().info("Clicked on Close button");
		driver.switchTo().defaultContent();
	}
	
	public void activateDeactivateWebhookOrAdapter(String tabName, String webhookOrAdapterName, String toBeActive) {
		navigateToInboundWebhookManagerTabNew(tabName);
		searchWebhookOrAdapter(webhookOrAdapterName);
		utils.longWaitInSeconds(2);
		driver.switchTo().defaultContent();
		selUtils.switchToIframe("iframe");

		// get the status of webhook or adapter

		String statusXpath = utils.getLocatorValue("webhookManagerPage.getStatusOfWebhookOrAdapterXpath")
				.replace("${webhookName}", webhookOrAdapterName);
		String currentStatus = driver.findElement(By.xpath(statusXpath)).getText();
		if (currentStatus.equalsIgnoreCase("Active") && toBeActive.equalsIgnoreCase("Active")) {
			logger.info(webhookOrAdapterName + " webhook or adapter is already active do not need to click on 3 dots");
			TestListeners.extentTest.get().info(
					webhookOrAdapterName + " webhook or adapter is already active do not need to click on 3 dots");

		} else if (currentStatus.equalsIgnoreCase("Inactive") && toBeActive.equalsIgnoreCase("Inactive")) {
			logger.info(
					webhookOrAdapterName + " webhook or adapter is already InActive do not need to click on 3 dots");
			TestListeners.extentTest.get().info(
					webhookOrAdapterName + " webhook or adapter is already InActive do not need to click on 3 dots");

		} else {
			String xpath = utils.getLocatorValue("webhookManagerPage.webhookActionThreeDotsIconXpath")
					.replace("${webhookName}", webhookOrAdapterName);
			driver.findElement(By.xpath(xpath)).click();
			String actionOptionButtonXpath = utils.getLocatorValue("webhookManagerPage.actionOptionsXpath")
					.replace("${actionOption}", "Toggle Status");

			driver.findElement(By.xpath(actionOptionButtonXpath)).click();
			utils.longWaitInSeconds(1);
			utils.getLocator("webhookManagerPage.activeDeactiveConfirmationYesBtnXpath").click();
			utils.longWaitInSeconds(3);
			logger.info("Clicked on Confirmation button");
			TestListeners.extentTest.get().info("Clicked on Confirmation button");
			currentStatus = driver.findElement(By.xpath(statusXpath)).getText();
			
			logger.info(currentStatus +" current status after clicking on Confirmation button");
			TestListeners.extentTest.get().info(currentStatus +" current status after clicking on Confirmation button");
			
			Assert.assertEquals(currentStatus, toBeActive, "Status is not changed to " + toBeActive);
			logger.info(webhookOrAdapterName + " webhook or adapter is now " + currentStatus);
			TestListeners.extentTest.get().info(webhookOrAdapterName + " webhook or adapter is now " + currentStatus);

		}
		utils.longWaitInSeconds(4);
		driver.switchTo().defaultContent();

	}
	
	// used to select event
		public boolean verifyEventIsDisplayingInEventSelectionDropDown(String eventName) {
			boolean flag = false;
			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe");
			utils.scrollToElement(driver, utils.getLocator("webhookManagerPage.eventsSelectionInputBoxXpath"));
			utils.getLocator("webhookManagerPage.eventsSelectionInputBoxXpath").click();
			utils.longWaitInSeconds(1);
			//get all the event list
			List<WebElement> eventList = utils.getLocatorList("webhookManagerPage.eventListXpath");
			for (WebElement event : eventList) {
				String eventNameFromList = event.getText();
				if (eventNameFromList.equalsIgnoreCase(eventName)) {
					flag = true;
					logger.info(eventName + " event is displaying in the dropdown");
					TestListeners.extentTest.get().info(eventName + " event is displaying in the dropdown");
					break;
				}
			}
			utils.getLocator("webhookManagerPage.eventsSelectionInputBoxXpath").click();
			utils.implicitWait(50);
			driver.switchTo().defaultContent();
			return flag;
		}

		public boolean verifyWebhookOrAdapterTabIsDisplaying(String tabName) {
			utils.longWaitInSeconds(5);	
			utils.implicitWait(10);
			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe");
			boolean flag = false;
			JavascriptExecutor js = (JavascriptExecutor) driver;
			js.executeScript("window.scrollTo(0, 0);");
			String tabXpath = utils.getLocatorValue("webhookManagerPage.tabNameXpath").replace("$TabName", tabName);
			try {
				driver.findElement(By.xpath(tabXpath)).click();
				flag = true;
				logger.info(tabName + " tab is displaying");
				TestListeners.extentTest.get().info(tabName + " tab is displaying");
			} catch (Exception e) {
				logger.info(tabName + " tab is not displaying");
				TestListeners.extentTest.get().info(tabName + " tab is not displaying");
			}
			utils.implicitWait(50);
			driver.switchTo().defaultContent();
			return flag;
		}
		
		public void selectDropDownValueInLogsTabPage(String dropDownName, String value) {
			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe");
			String xpath = utils.getLocatorValue("webhookManagerPage.selectDropDownValueInLogsTabXpath").replace("${labelName}", dropDownName);
			WebElement dropDown = driver.findElement(By.xpath(xpath));
			utils.selectDrpDwnValue(dropDown, value);
			utils.longWaitInSeconds(2);
			logger.info(value +" value is selected in " + dropDownName +" label dropdown");
			TestListeners.extentTest.get().info(value +" value is selected in " + dropDownName +" label dropdown");
			driver.switchTo().defaultContent();
		}
		
		public String getDispatchedEventPayloadFromLogs(String userEmail, String eventName, String webhookName,
				String jsonCondition,String currentTimeStamp) throws InterruptedException {
			String eventStatusJsonBody = "";
			String finalEventStatusJsonBody = "";
			String xpathText ="" ;
			boolean flag = false;

			String fetchActionCondition =null ; 
			if(eventName.equalsIgnoreCase("User Subscription")) {
				fetchActionCondition = "\"action\":\"update\"";
			}else if(eventName.contains("_")){
				String[] eventNameSplit = eventName.split("_");
				fetchActionCondition = "\"action\":\""+eventNameSplit[1]+"\"";
				eventName = eventNameSplit[0];
			}
			else {
				fetchActionCondition = "\"action\":\"create\"" ; 
			}
		
			selectDropDownValueInLogsTabPage("Events", eventName) ; 
			selectDropDownValueInLogsTabPage("Webhooks", webhookName) ; 

			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe"); 
			String additionalFilterXpath =utils.getLocatorValue("webhookManagerPage.AdditionalFilterButtonXpath") ;
			xpathText = driver.findElement(By.xpath(additionalFilterXpath)).getAttribute("title") ; 
			
			if(xpathText.equalsIgnoreCase("Show Additional Filters")) {
				utils.getLocator("webhookManagerPage.showAdditionalFilterButtonXpath").click();
				utils.longWaitInSeconds(2);
			}
			utils.getLocator("webhookManagerPage.additionalInfoEmailInputBoxXpath").clear();
			utils.getLocator("webhookManagerPage.additionalInfoEmailInputBoxXpath").sendKeys(userEmail);
			utils.getLocator("webhookManagerPage.additionalSearchedButtonXpath").click();
			utils.longWaitInSeconds(2);
			int counter = 0;
			utils.implicitWait(3);
			List<WebElement> listOfSuccessButtonFinal = null;
				while (!flag && counter < 25) {
					String listOfResultSuccessXpath = utils.getLocatorValue("webhookManagerPage.openWebhookLogsXpath")
							.replace("${eventName}", eventName).replace("${webhookName}", webhookName);

					String xpathTimeStamp = utils.getLocatorValue("webhookManagerPage.listOfTimeStampXpath")
							.replace("${eventName}", eventName).replace("${webhookName}", webhookName);
					List<WebElement> timeStampList = driver.findElements(By.xpath(xpathTimeStamp));

					List<WebElement> listOfSuccessBtnWele = driver.findElements(By.xpath(listOfResultSuccessXpath));
					listOfSuccessButtonFinal = new ArrayList<>();
					int indexCounter = 0;
					for (WebElement timeStampWEle : timeStampList) {
						String timeStampText = timeStampWEle.getText().trim();
						String isTimeAfterCurrentTimeStamp = checkTimeFromUIIsAfterCurrentTimeStamp(currentTimeStamp,
								timeStampText);
						if (isTimeAfterCurrentTimeStamp.equalsIgnoreCase("After") || isTimeAfterCurrentTimeStamp.equalsIgnoreCase("Equal")) {
							listOfSuccessButtonFinal.add(listOfSuccessBtnWele.get(indexCounter));
						}
						indexCounter++;
					}
					if (listOfSuccessButtonFinal.size() != 0) {
						flag = true;
						break;
					}
					WebElement wEleTimeSlotSelect = utils.getLocator("webhookManagerPage.timeSlotDropdownXpath");
					Select sel = new Select(wEleTimeSlotSelect);
					sel.selectByVisibleText("Today");
					utils.longWaitInSeconds(2);
					sel.selectByVisibleText("Last 30 Min.");

					counter++;
				}
			String xpath = utils.getLocatorValue("webhookManagerPage.openWebhookLogsXpath")
					.replace("${eventName}", eventName).replace("${webhookName}", webhookName);
			List<WebElement> list1 = driver.findElements(By.xpath(xpath));

			for (WebElement ele : listOfSuccessButtonFinal) {
				ele.click();
				utils.longWaitInSeconds(4);
				int copyDivIndexNumber = 0;
				List<WebElement> copyDivList = utils.getLocatorList("webhookManagerPage.jsonBodyIndexXpath");
				copyDivIndexNumber = copyDivList.size();
				String copyDivXpath = utils.getLocatorValue("webhookManagerPage.jsonBodyLogsXpath").replace("${indexNum}",
						String.valueOf(copyDivIndexNumber));
				
				eventStatusJsonBody = driver.findElement(By.xpath(copyDivXpath)).getText();
				String xpathDispatchedEventPayload = utils.getLocatorValue("webhookManagerPage.xpathDispatchedEventPayload")
						.replace("${indexNum}", copyDivXpath);
				if (!jsonCondition.isEmpty() && eventStatusJsonBody.contains(fetchActionCondition)
						&& eventStatusJsonBody.contains(jsonCondition)) {
					
					finalEventStatusJsonBody = driver.findElement(By.xpath(xpathDispatchedEventPayload)).getText();
					utils.getLocator("webhookManagerPage.closeLogsWindowXpath").click();
					logger.info("WebhookManagerPage.openLogsForWebhook() Inside ifcondition");
					break;
				} else if (jsonCondition.isEmpty() && eventStatusJsonBody.contains(fetchActionCondition)) {
					finalEventStatusJsonBody = driver.findElement(By.xpath(xpathDispatchedEventPayload)).getText();
					utils.getLocator("webhookManagerPage.closeLogsWindowXpath").click();
					break;
				} else {
					utils.getLocator("webhookManagerPage.closeLogsWindowXpath").click();

				}

			}
			utils.implicitWait(50);
			driver.switchTo().defaultContent();
			return finalEventStatusJsonBody;
		}
		
		
		public void searchWebhookOrAdapter(String searchValue) {
			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe");
			utils.waitTillElementToBeClickable(utils.getLocator("webhookManagerPage.searchInputBoxXpath"));
			utils.longWaitInSeconds(2);
			utils.getLocator("webhookManagerPage.searchInputBoxXpath").click();
			utils.getLocator("webhookManagerPage.searchInputBoxXpath").clear();
			utils.getLocator("webhookManagerPage.searchInputBoxXpath").sendKeys(searchValue);
			utils.longWaitInSeconds(2);
			logger.info("Searched for: " + searchValue);
			TestListeners.extentTest.get().info("Searched for: " + searchValue);
			driver.switchTo().defaultContent();
		}
		
		//used to get cookies from iframe URL for webhook configuration page
		public String getCookiesFromIframeURL() {
			String cookiesALl = "";
			if (cookiesVar != null && !cookiesVar.isEmpty()) {
				return cookiesVar;
			} else {
				String originalWindow = driver.getWindowHandle();
				// driver.switchTo().window(originalWindow);
				String urlCookies = driver.findElement(By.tagName("iframe")).getAttribute("src");
				logger.info("URL of the iframe: " + urlCookies);
				// Open the URL in a new tab
				// Open a new tab using JavaScript
				((JavascriptExecutor) driver).executeScript("window.open('" + urlCookies + "','_blank');");
				ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
				driver.switchTo().window(tabs.get(1)); // Switch to the new tab
				// Get all cookies
				Set<Cookie> allCookies = driver.manage().getCookies();

				// Print cookies
				for (Cookie cookie : allCookies) {
					cookiesALl = cookiesALl + cookie.getName() + "=" + cookie.getValue() + "; ";
					logger.info("Cookie Name: " + cookie.getName() + ", Value: " + cookie.getValue());
				}

				cookiesVar = cookiesALl; // Store cookies in the variable
				driver.close(); // Close the new tab
				driver.switchTo().window(originalWindow); // index 1 = new tab}

			}

			return cookiesALl;

		}
		
		public void deleteAllWebhookOrAdapter(String type) {
			utils.longWaitInSeconds(2);
			navigateToInboundWebhookManagerTabNew(type);
			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe");
			utils.implicitWait(2);
			List<WebElement> webhookElements = driver
					.findElements(By.xpath("//div[@id='main-layout']//tbody/tr/td[1]"));

			if (webhookElements.isEmpty()) {
				driver.switchTo().defaultContent();
				logger.info("No webhooks or adapters found to delete.");
				TestListeners.extentTest.get().info("No webhooks or adapters found to delete.");
				return;
			}

			// Collect names of deletable webhooks first to avoid stale references
			List<String> deletableWebhookNames = new ArrayList<>();
			for (WebElement webhook : webhookElements) {
				String webhookText = webhook.getText().trim();
				if (!webhookText.contains("DoNotDelete")) {
					deletableWebhookNames.add(webhookText);
				}
			}

			if (deletableWebhookNames.isEmpty()) {
				logger.info("No deletable webhooks or adapters found.");
				TestListeners.extentTest.get().info("No deletable webhooks or adapters found.");
			} else {
				for (String webhookName : deletableWebhookNames) {
					logger.info("Deleting webhook or adapter: " + webhookName);
					TestListeners.extentTest.get().info("Deleting webhook or adapter: " + webhookName);

					deleteWebhookNew(webhookName, "Delete");
					utils.longWaitInSeconds(1);
				}

				logger.info("All eligible webhooks or adapters have been deleted.");
				TestListeners.extentTest.get().info("All eligible webhooks or adapters have been deleted.");
			}

			utils.implicitWait(50);
			driver.switchTo().defaultContent();
		}

		
		
		// it click on 3 dots and perform action on webhook eg. edit, delete / evFrameworkType= "adapter" or "webhook"
		public void deleteWebhookNew(String webhookName, String actionOption) {
			utils.longWaitInSeconds(5);
			driver.switchTo().defaultContent();
			selUtils.switchToIframe("iframe");
			String xpath = utils.getLocatorValue("webhookManagerPage.webhookActionThreeDotsIconXpath")
					.replace("${webhookName}", webhookName);
			utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpath)));
			driver.findElement(By.xpath(xpath)).click();
			utils.longWaitInSeconds(4);
			String actionOptionButtonXpath = utils.getLocatorValue("webhookManagerPage.actionOptionsXpath")
					.replace("${actionOption}", actionOption);
			WebElement deleteButton = driver.findElement(By.xpath(actionOptionButtonXpath));
			utils.waitTillElementToBeClickable(deleteButton);
			utils.clickByJSExecutor(driver, deleteButton);
			//deleteButton.click();
			utils.longWaitInSeconds(3);
			String xpathofDeleteConfirmationButton = utils.getLocatorValue("webhookManagerPage.actionIconForWebhookXpth").replace("${webHookName}" , webhookName) ;
			utils.waitTillElementToBeClickable(driver.findElement(By.xpath(xpathofDeleteConfirmationButton)));
			driver.findElement(By.xpath(xpathofDeleteConfirmationButton)).click();
			logger.info("Clicked on " + actionOption + " button for webhook : " + webhookName);
			TestListeners.extentTest.get().info("Clicked on " + actionOption + " button for webhook : " + webhookName);
			driver.switchTo().defaultContent();
		}

	}
