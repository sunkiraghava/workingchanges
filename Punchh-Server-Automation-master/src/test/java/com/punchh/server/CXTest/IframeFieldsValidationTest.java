package com.punchh.server.CXTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.SeleniumUtilities;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class IframeFieldsValidationTest {
	static Logger logger = LogManager.getLogger(IframeFieldsValidationTest.class);
	public WebDriver driver;
	Utilities utils;
	SeleniumUtilities selUtils;
	Properties prop;
	String iframeemail;
	String iframemptydata;
	String iframeinvalid;
	String iframevalid;
	String iframevalid1;
	private String sTCName;
	private String env, run = "ui";
	private static Map<String, String> dataSet;

	PageObj pageObj;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		// String browserName = prop.getProperty("browserName");
		env = prop.getProperty("environment");
		driver = new BrowserUtilities().launchBrowser();
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);

	}

	// validate iframe feilds with empty data
	@Test(priority = 1)
	public void iframePageValidation() throws InterruptedException {
		logger.info("== Iframe fields validation test ==");
		// SoftAssert softAssertion = new SoftAssert();
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Assertion Dashboard page loaded successfully
		// pageObj.instanceDashboardPage().assertDashboard();
		// iFrame configuration-Enable all the fields and update
		pageObj.instanceDashboardPage().iframeconfiguration();
		// Validate Iframe page options
		pageObj.iframeSingUpPage().navigateToIframe(
				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + dataSet.get("slug"));
		pageObj.instanceDashboardPage().iframepagevalidation();
		// iframe signup with empty data
		iframemptydata = pageObj.iframeSingUpValidationPageClass().iframeSignUpField();
	}

	@Test(priority = 2)
	public void invalidDataValidation() throws InterruptedException {
		logger.info("== Iframe fields validation test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Assertion Dashboard page loaded successfully
		// pageObj.instanceDashboardPage().assertDashboard();
		// iFrame configuration-Enable all the fields and update
		pageObj.instanceDashboardPage().iframeconfiguration();
		// Validate Iframe page options
		pageObj.iframeSingUpPage().navigateToIframe(
				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + dataSet.get("slug"));
		pageObj.instanceDashboardPage().iframepagevalidation();
		// Validate iframe fields with invalid data
		iframeinvalid = pageObj.iframeSingUpValidationPageClass().iframeSignUpFieldinvaliddata();
	}

	@Test(priority = 3)
	public void validDataValidation() throws InterruptedException {
		logger.info("== Iframe fields validation test ==");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("instanceUrl"));
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Assertion Dashboard page loaded successfully
		// pageObj.instanceDashboardPage().assertDashboard();
		// iFrame configuration-Enable all the fields and update
		pageObj.instanceDashboardPage().iframeconfiguration();
		// Validate Iframe page options
		pageObj.iframeSingUpPage().navigateToIframe(
				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + dataSet.get("slug"));
		pageObj.instanceDashboardPage().iframepagevalidation();
		// Validate iframe fields with valid data
		iframevalid = pageObj.iframeSingUpValidationPageClass().iframeSignUpFieldvaliddata();
		// iframe signuot
		pageObj.iframeSingUpValidationPageClass().iframeSignOut();
		// login and directed to edit profile page
		pageObj.iframeSingUpValidationPageClass().navigateToIframe(
				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + dataSet.get("slug"));
		pageObj.iframeSingUpValidationPageClass().iframeLoginvalid(iframevalid);
	}

//	// Validate Account balance field
//		@Test(priority=4)
//	      public void verifyAccountBalancevalidation() throws InterruptedException
//	      {
//	  		logger.info("== Iframe fields validation test ==");
//	  		SoftAssert softAssertion = new SoftAssert();
//	  		pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("instanceUrl"));
//	  		pageObj.instanceDashboardPage().loginToInstance();
//	  		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//	  		String actualname=driver.findElement(By.xpath("//h2[text()='Welcome John']")).getText();
//	  		String expectedname="Welcome John";
//	  		Assert.assertEquals(actualname, expectedname);//To validate if directed to expected page
//	  		System.out.println("Assertion for selecting business passed");
//	  		 //iFrame configuration-Enable all the fields and update
//	  		Actions action = new Actions(driver);
//	  		WebElement menuOption = driver.findElement(By.xpath("//a[@href='/dashboard']"));
//	  		action.moveToElement(menuOption).perform();
//	  		driver.findElement(By.xpath("//i[@class='fa fa-thumb-tack']")).click();
//	  		driver.findElement(By.xpath("//span[text()='Whitelabel']")).click();
//	  		List<WebElement> values = driver.findElements(By.xpath("//li[@class='nav-item  dropdown open']//ul//li"));
//	  	    for(int i=1;i<=values.size();i++)
//	  		{
//	  			if(values.get(i).getText().contains("iFrame Configuration"))
//	  			{
//	  				values.get(i).click();
//	  				break;
//	  		}
//	  		}
//	  	    List<WebElement> element=driver.findElements(By.xpath("//input[contains(@id,'iframe_configuration')]"));
//	  	    for(int i=1;i<20; i++)
//	  	    {
//	  	    	
//	  	    	String js = "arguments[0].style.visibility='visible';";
//	  			((JavascriptExecutor) driver).executeScript(js, element.get(i));
//	  			String val=element.get(i).getAttribute("checked");
//	  			if(val == null) 
//	  			{
//	  				JavascriptExecutor jse = (JavascriptExecutor) driver;
//	  				jse.executeScript("arguments[0].click();", element.get(i));
//	  				String value=element.get(i).getAttribute("name");
//	  				System.out.println(value);
//	  			
//	  			}
//
//
//	  	    }
//	  	  driver.findElement(By.xpath("//button[text()='Update']")).click();
//	  	  String actualiframemessage="Iframe configuration updated";
//	  	  String expectediframemsg=driver.findElement(By.xpath("//strong[text()='Iframe configuration updated']")).getText();
//	  	  Assert.assertEquals(actualiframemessage, expectediframemsg);
//	  	  System.out.println("iframe fields updated successfully");	
//	  	//Validate Iframe page options 
//	  	  pageObj.iframeSingUpPage().navigateToIframe(
//	  				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + dataSet.get("slug"));
//	  	  String businessnm="My Biz";
//	  	  String expectedbString=driver.findElement(By.xpath("//h1[text()='My Biz']")).getText();
//	  	  Assert.assertEquals(businessnm, expectedbString);
//	  	  String expectedsign="SIGN UP";
//	  	  String actualsign=driver.findElement(By.xpath("//a[text()='Sign Up']")).getText();
//	  	  Assert.assertEquals(expectedsign, actualsign);
//	  	  String expectedlogin="LOGIN";
//	  	  String actuallogin=driver.findElement(By.xpath("//a[text()='Login']")).getText();
//	  	  Assert.assertEquals(expectedlogin, actuallogin);
//	  	  String expectedtxt="PHYSICAL LOYALTY CARD REGISTRATION";
//	  	  String actualtxt=driver.findElement(By.xpath("//a[text()='Physical Loyalty Card Registration']")).getText();
//	  	  Assert.assertEquals(expectedtxt, actualtxt);
//	      System.out.println("Iframe page loaded successfully");
//	  //    pageObj.iframeSingUpValidationPageClass().navigateToIframe(dataSet.get("instanceUrl") + dataSet.get("whiteLabel") + dataSet.get("slug"));
//	      iframevalid1= pageObj.iframeSingUpValidationPageClass().iframeSignUpFieldvaliddata();
//	      pageObj.iframeSingUpValidationPageClass().iframeSignOut();
//	// Instance login and goto timeline
//	        pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get("instanceUrl"));
//			pageObj.instanceDashboardPage().loginToInstance();
//			pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//			pageObj.instanceDashboardPage().navigateToGuestTimeline(iframevalid1);
//			pageObj.guestTimelinePage().verifyGuestTimeline(dataSet.get("joinedChannel"), iframevalid1);
////	      pageObj.instanceDashboardPage().navigateToPunchhInstance(Utilities.getConfigProperty("instanceUrl"));
////	  		pageObj.instanceDashboardPage().loginToInstance();
////	  		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
////	      pageObj.instanceDashboardPage().navigateToGuestTimeline(iframevalid1);
////	      pageObj.guestTimelinePage().verifyGuestTimeline(dataSet.get("joinedChannel"), iframevalid1);
//		 //validate Account balance after gifting
//////	      driver.findElement(By.xpath("//a[text()='Message/Gift']")).isDisplayed();
//////	      driver.findElement(By.xpath("//a[text()='Message/Gift']")).click();
//////	      driver.findelelement(By.xpath)
//	 pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),dataSet.get("redeemable"), dataSet.get("giftReason"));
//			boolean status = pageObj.campaignspage().validateSuccessMessage();
//			String rewardName = pageObj.guestTimelinePage().getRewardName();
//			Assert.assertTrue(status, "Message sent did not displayed on timeline");
//			Assert.assertEquals(rewardName, "American Dream");
//			pageObj.iframeSingUpValidationPageClass().iframeLoginvalid1(iframevalid1);
//			 String account1 = driver.findElement(By.xpath("//div[@class='account']")).getText();
//			 System.out.println(account1);
//	      }
////	 
////			@Test(priority=5)
////		      public void T2661_verifyAccountBalance() throws InterruptedException
////		      {
////		  		logger.info("== Iframe fields validation test ==");
////		  		SoftAssert softAssertion = new SoftAssert();
////		  		
////		  		pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get("instanceUrl"));
////				pageObj.instanceDashboardPage().loginToInstance();
////				pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
////				//pageObj.instanceDashboardPage().navigateToGuestTimeline(iFrameEmail);
////		  		String actualname=driver.findElement(By.xpath("//h2[text()='Welcome John']")).getText();
////		  		String expectedname="Welcome John";
////		  		Assert.assertEquals(actualname, expectedname);//To validate if directed to expected page
////		  		System.out.println("Assertion for selecting business passed");
////		  		 //iFrame configuration-Enable all the fields and update
////		  		Actions action = new Actions(driver);
////		  		WebElement menuOption = driver.findElement(By.xpath("//a[@href='/dashboard']"));
////		  		action.moveToElement(menuOption).perform();
////		  		driver.findElement(By.xpath("//i[@class='fa fa-thumb-tack']")).click();
////		  		driver.findElement(By.xpath("//span[text()='Whitelabel']")).click();
////		  		List<WebElement> values = driver.findElements(By.xpath("//li[@class='nav-item  dropdown open']//ul//li"));
////		  	    for(int i=1;i<=values.size();i++)
////		  		{
////		  			if(values.get(i).getText().contains("iFrame Configuration"))
////		  			{
////		  				values.get(i).click();
////		  				break;
////		  		}
////		  		}
////		  	    List<WebElement> element=driver.findElements(By.xpath("//input[contains(@id,'iframe_configuration')]"));
////		  	    for(int i=1;i<20; i++)
////		  	    {
////		  	    	
////		  	    	String js = "arguments[0].style.visibility='visible';";
////		  			((JavascriptExecutor) driver).executeScript(js, element.get(i));
////		  			String val=element.get(i).getAttribute("checked");
////		  			if(val == null) 
////		  			{
////		  				JavascriptExecutor jse = (JavascriptExecutor) driver;
////		  				jse.executeScript("arguments[0].click();", element.get(i));
////		  				String value=element.get(i).getAttribute("name");
////		  				System.out.println(value);
////		  			
////		  			}
////
////
////		  	    }
////		  	  driver.findElement(By.xpath("//button[text()='Update']")).click();
////		  	  String actualiframemessage="Iframe configuration updated";
////		  	  String expectediframemsg=driver.findElement(By.xpath("//strong[text()='Iframe configuration updated']")).getText();
////		  	  Assert.assertEquals(actualiframemessage, expectediframemsg);
////		  	  System.out.println("iframe fields updated successfully");	
////		  	//Validate Iframe page options 
////		  	  pageObj.iframeSingUpPage().navigateToIframe(
////		  				prop.getProperty("instanceUrl") + prop.getProperty("iframeWhitelabel") + dataSet.get("slug"));
////		  	  String businessnm="My Biz";
////		  	  String expectedbString=driver.findElement(By.xpath("//h1[text()='My Biz']")).getText();
////		  	  Assert.assertEquals(businessnm, expectedbString);
////		  	  String expectedsign="SIGN UP";
////		  	  String actualsign=driver.findElement(By.xpath("//a[text()='Sign Up']")).getText();
////		  	  Assert.assertEquals(expectedsign, actualsign);
////		  	  String expectedlogin="LOGIN";
////		  	  String actuallogin=driver.findElement(By.xpath("//a[text()='Login']")).getText();
////		  	  Assert.assertEquals(expectedlogin, actuallogin);
////		  	  String expectedtxt="PHYSICAL LOYALTY CARD REGISTRATION";
////		  	  String actualtxt=driver.findElement(By.xpath("//a[text()='Physical Loyalty Card Registration']")).getText();
////		  	  Assert.assertEquals(expectedtxt, actualtxt);
////		      System.out.println("Iframe page loaded successfully");
////		  //    pageObj.iframeSingUpValidationPageClass().navigateToIframe(dataSet.get("instanceUrl") + dataSet.get("whiteLabel") + dataSet.get("slug"));
////		      iframevalid1= pageObj.iframeSingUpValidationPageClass().iframeSignUpFieldvaliddata();
////		      pageObj.iframeSingUpValidationPageClass().iframeSignOut();
////		// Instance login and goto timeline
////		        pageObj.instanceDashboardPage().navigateToPunchhInstance(dataSet.get("instanceUrl"));
////				pageObj.instanceDashboardPage().loginToInstance();
////				pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
////				pageObj.instanceDashboardPage().navigateToGuestTimeline(iframevalid1);
////				pageObj.guestTimelinePage().verifyGuestTimeline(dataSet.get("joinedChannel"), iframevalid1);
////				
////				
////				//messafe/ gift user
////                pageObj.guestTimelinePage().messageGiftToUser(dataSet.get("subject"), dataSet.get("reward"),dataSet.get("redeemable"), dataSet.get("giftReason"));
////				boolean status = pageObj.campaignspage().validateSuccessMessage();
////				String rewardName = pageObj.guestTimelinePage().getRewardNameA();
////				Assert.assertTrue(status, "Message sent did not displayed on timeline");
////				Assert.assertEquals(rewardName, "Rewarded pasta");
////			    pageObj.iframeSingUpValidationPageClass().navigateToIframe(dataSet.get("instanceUrl") + dataSet.get("whiteLabel") + dataSet.get("slug"));
////				pageObj.iframeSingUpValidationPageClass().iframeLoginvalid1(iframevalid1);
////			 
////      } 

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		driver.close();
		driver.quit();
	}
}
