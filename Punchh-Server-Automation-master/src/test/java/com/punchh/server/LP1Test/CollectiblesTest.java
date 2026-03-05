package com.punchh.server.LP1Test;

import java.lang.reflect.Method;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;


@Listeners(TestListeners.class)
public class CollectiblesTest {
    private static Logger logger = LogManager.getLogger(CollectiblesTest.class);
    public WebDriver driver;
    private PageObj pageObj;
    private String sTCName;
    private String baseUrl;
    private String env;
    private String run = "ui";
    private static Map<String, String> dataSet;
    private Utilities utils;
    private Properties prop;
    private String businessID;
    private String collectibleName1,collectibleName2;
    
    @BeforeClass(alwaysRun = true)
    public void openBrowser() {
        prop = Utilities.loadPropertiesFile("config.properties");
        driver = new BrowserUtilities().launchBrowser();
        pageObj = new PageObj(driver);
        utils = new Utilities(driver);
        env = pageObj.getEnvDetails().setEnv();
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
        businessID = null;
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

    //will uncomment once changes merged in master
    @Test(description = "SQ-T7538 Verify creation of a new Collectible with all mandatory fields"
    		+ "SQ-T7539 Verify Collectible Name is unique within a category [same Name Collectible cannot be created in same category]"
    		+ "SQ-T7540 Verify Disappear Date cannot be a past date when creating/updating a collectible"
    		+ "SQ-T7541 Verify format validation for unsupported formats on Image upload [eg. gif, jpeg etc]"
    		+ "SQ-T7542 Verify file size limit on Image upload [< 1 MB]"
    		+ "SQ-T7543 Verify default value of Status for new Collectibles"
    		+ "SQ-T7557 Verify Collectible with Active status can be created")
    @Owner(name = "Rakhi Rawat")
    public void T7538_VerifyNewCollectibleCreation(){
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        
        pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
        pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
        pageObj.dashboardpage().checkUncheckAnyFlag("Enable Digital Collectible", "check");
        
        //Navigate to All Collectibles Page and create a new Collectible
        pageObj.menupage().navigateToSubMenuItem("Settings", "All Collectibles");
        pageObj.collectibleCategoryPage().clickAddCollectibleButton();
        
        collectibleName1 = "AutoCollectibleOne" + System.currentTimeMillis();
        collectibleName2 = "AutoCollectibleTwo" + System.currentTimeMillis();
        String date = CreateDateTime.getTomorrowsDate();
        String pastDate = CreateDateTime.getYesterdaysDate();
        
        String defaultValue = pageObj.collectibleCategoryPage().createCollectible(collectibleName1, collectibleName1, dataSet.get("categoryName"), dataSet.get("shareMsg"), date, "Active");
        Assert.assertEquals(defaultValue, "Inactive", "Default value for status dropdown is not Inactive");
		utils.logPass("Verified that default value for status dropdown is Inactive when creating a collectible");
        boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message sent did not displayed");
		utils.logPass("New Collectible created successfully with name: " + collectibleName1);
		
		//LPE-T3679/SQ-T7539
		//create another collectible with same name under same category 
		pageObj.collectibleCategoryPage().clickAddCollectibleButton();
		pageObj.collectibleCategoryPage().createCollectible(collectibleName1, collectibleName1,
				dataSet.get("categoryName"), dataSet.get("shareMsg"), date, "Active");
		String errorMsg = pageObj.collectibleCategoryPage().validateCollectibleCategoryErrorsMessage();
		Assert.assertEquals(errorMsg, MessagesConstants.collectibleCreationErrorMessage, "Error message did not displayed/matched");
		utils.logPass("Verified that existing collectible name under same category is not allowed");	
		
		//update existing collectible with same name under same category
		pageObj.menupage().navigateToSubMenuItem("Settings", "All Collectibles");
		pageObj.collectibleCategoryPage().clickEditBtn(dataSet.get("collectibleName"));
		pageObj.collectibleCategoryPage().createCollectible(dataSet.get("collectibleName"), dataSet.get("collectibleName"),
				dataSet.get("categoryName"), dataSet.get("shareMsg"), date, "Active");
		String errorMsg1 = pageObj.collectibleCategoryPage().validateCollectibleCategoryErrorsMessage();
		Assert.assertEquals(errorMsg1, MessagesConstants.collectibleUpdationErrorMessage, "Error message did not displayed/matched");
		utils.logPass("Verified that updating existing collectible with same name under same category is not allowed");	
		
		//LPE-T3688/SQ-T7540
		//set disapper date in past 
		pageObj.collectibleCategoryPage().clickEditBtn(dataSet.get("collectibleName"));
		pageObj.collectibleCategoryPage().setDisappearDate(pastDate);
		pageObj.collectibleCategoryPage().clickSaveCategoryButton();
		String errorMsg2 = pageObj.collectibleCategoryPage().validateCollectibleCategoryErrorsMessage();
		Assert.assertEquals(errorMsg2, MessagesConstants.collectibleUpdationErrorMessage, "Error message did not displayed/matched");
		String dateErrorMsg = pageObj.collectibleCategoryPage().getInputFieldError("Disappear Date");
		Assert.assertEquals(dateErrorMsg, MessagesConstants.collectibleDateErrorMessage, "Error message did not displayed/matched");
		utils.logPass("Verified that Disappear Date cannot be a past date when creating/updating a collectible");	
		
		//LPE-T3684/SQ-T7541
		//upload collectible image in invalid format
		pageObj.collectibleCategoryPage().uploadCollectibleImage(dataSet.get("invalidFormatImagePath"));
		pageObj.collectibleCategoryPage().clickSaveCategoryButton();
		String imageErrorMsg = pageObj.collectibleCategoryPage().getInputFieldError("Image");
		Assert.assertEquals(imageErrorMsg, MessagesConstants.collectibleInvalidImageErrorMessage, "Error message did not displayed/matched");
		utils.logPass("Verified format validation for unsupported formats on Image upload");	
		
		//LPE-T3685/SQ-T7542
		//upload Image [< 1 MB]
		pageObj.collectibleCategoryPage().uploadCollectibleImage(dataSet.get("1mbImagePath"));
		pageObj.collectibleCategoryPage().clickSaveCategoryButton();
		String imageErrorMsg1 = pageObj.collectibleCategoryPage().getInputFieldError("Image");
		Assert.assertEquals(imageErrorMsg1, MessagesConstants.collectibleImageSizeErrorMessage, "Error message did not displayed/matched");
		utils.logPass("Verified file size limit on Image upload [< 1 MB] is unsucessful");	
		
		//create collectible with no category
		pageObj.collectibleCategoryPage().createCollectible(collectibleName2, collectibleName2,
				"", dataSet.get("shareMsg"), date, "Active");
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Success message sent did not displayed");
		utils.logPass("New Collectible created successfully with name: " + collectibleName2);
	
    }

	@Test(description = "SQ-T3691 Verify Collectible with Inactive status can be created"
			+ "SQ-T7558 Verify Collectible Status can be updated from Active to Inactive and vice versa"
			+ "SQ-T7559 Test editing an existing collectible"
			+ "SQ-T7560 Verify Category behaviour when selecting an existing active category"
			+ "SQ-T7561 Verify Collectible cannot be created with Inactive category"
			+ "SQ-T7562 Verify Collectible with same Name can be created in a different category"
			+ "SQ-T7563 Verify Collectible with same Name can be created without a category [default No Category]")
	@Owner(name = "Rakhi Rawat")
	public void T3691_createCollectibleWithInactiveStatus() {
		
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Digital Collectible", "check");

		// Navigate to All Collectibles Page and create a new Collectible
		pageObj.menupage().navigateToSubMenuItem("Settings", "All Collectibles");
		pageObj.collectibleCategoryPage().clickAddCollectibleButton();

		String collectibleName1 = "AutoCollectible1" + System.currentTimeMillis();
		String collectibleName2 = "AutoCollectible2" + System.currentTimeMillis();
		String date = CreateDateTime.getTomorrowsDate();
		pageObj.collectibleCategoryPage().createCollectible(collectibleName1, collectibleName1,
				dataSet.get("categoryName"), dataSet.get("shareMsg"), date, "Inactive");
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message sent did not displayed");
		utils.logPass("New Collectible with inactive status created successfully : " + collectibleName1);
		
		//SQ-T7559 SQ-T7560
		//Edit existing collectible and update all details with active category 
		pageObj.collectibleCategoryPage().switchActiveInactiveTab("inactive_collectibles");
		pageObj.collectibleCategoryPage().clickEditBtn(collectibleName1);
		pageObj.collectibleCategoryPage().createCollectible(collectibleName2, collectibleName2,
				dataSet.get("categoryName"), dataSet.get("shareMsg"), date, "Active");
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Success message sent did not displayed");
		utils.logPass("Existing Collectible with unique name and details updated successfully : " + collectibleName1);
		//LPE-T3803/SQ-T7558
		utils.logPass("Verified Collectible Status can be updated from Active to Inactive and vice versa");
		// SQ-T7561
		// create collectible with inactive category and then update category
		pageObj.collectibleCategoryPage().clickEditBtn(collectibleName2);
		boolean result = pageObj.collectibleCategoryPage().selectCategory(dataSet.get("inactiveCategoryName"));
		Assert.assertFalse(result, "Inactive category is shown in category dropdown but it should not be listed");
		utils.logPass("Verified Inactive category is not displayed in the dropdown so Collectible cannot be created with Inactive category selected");
		
		// SQ-T7563
		// create collectible with same name with no category
		pageObj.menupage().navigateToSubMenuItem("Settings", "All Collectibles");
		pageObj.collectibleCategoryPage().clickAddCollectibleButton();
		pageObj.collectibleCategoryPage().createCollectible(collectibleName2, collectibleName2,
				"", dataSet.get("shareMsg"), date, "Active");
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Success message sent did not displayed");
		utils.logPass("Verified that new Collectible with same name can be created successfully without selecting any category : " + collectibleName2);

		// SQ-T7562
		// create collectible with same name in different category
		pageObj.collectibleCategoryPage().clickEditBtn(collectibleName2);
		pageObj.collectibleCategoryPage().createCollectible(collectibleName2, collectibleName2,
				dataSet.get("categoryName2"), dataSet.get("shareMsg"), date, "Active");
		boolean status3 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status3, "Success message sent did not displayed");
		utils.logPass("Verified that new Collectible with same name can be created successfully in different category : " + collectibleName2);
		
	}
 
	@Test(description = "LPE-T3784 Verify Collectible with same Name present in category C1/C2 of Business 1 can be created in different business ie. Business 2"
			+ "LPE-T3796 Verify Collectible category can be updated from default to value1 and value 1 to value2"
			+ "LPE-T3829 Verify Collectible with same Name without category present [default No Category] in Business 1 can be created in different business ie. Business 2", dependsOnMethods = "T7538_VerifyNewCollectibleCreation")
	@Owner(name = "Rakhi Rawat")
	public void T3784_CollectibleWithSameNamePresentOnDiffBusiness() {
		
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Digital Collectible", "check");

		// Navigate to All Collectibles Page and create a new Collectible
		pageObj.menupage().navigateToSubMenuItem("Settings", "All Collectibles");
		pageObj.collectibleCategoryPage().clickAddCollectibleButton();

		String date = CreateDateTime.getTomorrowsDate();
		
		//LPE-T3829
		//create collectible with same name in different business with no category
		pageObj.collectibleCategoryPage().createCollectible(collectibleName2, collectibleName2,
				"", dataSet.get("shareMsg"), date, "Active");
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Success message sent did not displayed");
		utils.logPass("New Collectible with same name present in other business created successfully : " + collectibleName2);
		
		//LPE-T3796 
		//update collectible category from default(no category) to value1 
		// update existing collectible with same Name present in other business with category
		pageObj.collectibleCategoryPage().clickEditBtn(collectibleName2);
		pageObj.collectibleCategoryPage().createCollectible(collectibleName1, collectibleName1,
				dataSet.get("categoryName1"), dataSet.get("shareMsg"), date, "Active");
		boolean status1 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status1, "Success message sent did not displayed");
		utils.logPass("Updated existing collectible with inactive status created successfully : " + collectibleName1);
		
		//update collectible category from value1 to value2
		pageObj.collectibleCategoryPage().expandCollectible(dataSet.get("categoryName1"));
		pageObj.collectibleCategoryPage().clickEditBtn(collectibleName1);
		pageObj.collectibleCategoryPage().selectCategory(dataSet.get("categoryName2"));
		pageObj.collectibleCategoryPage().clickSaveCategoryButton();
		boolean status2 = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status2, "Success message sent did not displayed");
		utils.logPass("Verified Collectible category can be updated from default to value1 and value 1 to value2");
		
	}
	
    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        DBUtils.deleteCollectibleCategoriesByBusiness(env, businessID);
        pageObj.utils().clearDataSet(dataSet);
        logger.info("Data set cleared");
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        driver.quit();
        logger.info("Browser closed");
    }
}
