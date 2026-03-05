/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Field Dependency Test
 * @fileName ppccDependentFieldsTest.java
 */
package com.punchh.server.ppccTest;
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
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ppccDependentFieldsTest {
    static Logger logger = LogManager.getLogger(ppccDependentFieldsTest.class);
    public WebDriver driver;
    private Properties prop;
    private PageObj pageObj;
    private String sTCName;
    private String env, run = "ui";
    private String baseUrl;
    private static Map<String, String> dataSet;

    @BeforeClass(alwaysRun = true)
    public void openBrowser() {
        prop = Utilities.loadPropertiesFile("config.properties");
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
        logger.info(sTCName + " ==>" + dataSet);
        pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
    }

    @Test(description = "SQ-T6725 Verify the Show Hide Required dependency for fields.")
    @Owner(name = "Kalpana")
    public void T6725_verifyTheShowHideRequiredDependencyForFields()throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().clickOnCreatePolicyButton();
        pageObj.ppccPolicyPage().defineGeneralSettings("Aloha");
        pageObj.ppccPolicyPage().clickNextButton();

        // this is to verify required dependency when "PayPal Tender ID" is filled and "PayPal Item ID" is set to required and publish should be disabled
        pageObj.ppccPolicyPage().enterPaypalTenderId(Integer.parseInt(dataSet.get("PaypaltenderId")));
        boolean isRequiredDependencyVisible=pageObj.ppccPolicyPage().isFieldLabelMarkedAsRequired("PayPal Item ID ");
        Assert.assertTrue(isRequiredDependencyVisible, "Required dependency is not visible for PayPal Item ID field");
        pageObj.utils().logPass("Required dependency is visible for PayPal Item ID field");
        pageObj.ppccPolicyPage().enterCompId(Integer.parseInt(dataSet.get("CompId")));
        pageObj.ppccPolicyPage().enterPunchhItemId(Integer.parseInt(dataSet.get("PunchhItemId")));
        pageObj.ppccPolicyPage().enterRedeemItemId(Integer.parseInt(dataSet.get("RedeemItemId")));
        pageObj.ppccPolicyPage().enterVoidReason(Integer.parseInt(dataSet.get("VoidReason")));
        boolean isPublishButtonEnabled=pageObj.ppccPolicyPage().isPublishButtonEnabled();
        Assert.assertFalse(isPublishButtonEnabled, "Publish button is enabled even when required field is not filled");
        pageObj.utils().logPass("Publish button is disabled when required field is not filled");

        // this is to verify Show Dependency if Gift card is set to true.
        pageObj.ppccPolicyPage().enterHasGiftCardIntegrationCheckBox();
        boolean isGiftCardVendorFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Vendor ");
        Assert.assertTrue(isGiftCardVendorFieldVisible, "Show dependency is not working for Gift Card Vendor field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Vendor field");

        boolean isGiftCardUserFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card User ");
        Assert.assertTrue(isGiftCardUserFieldVisible, "Show dependency is not working for Gift Card User field");
        pageObj.utils().logPass("Show dependency is working for Gift Card User field");

        boolean isGiftCardUPasswordFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Password ");
        Assert.assertTrue(isGiftCardUPasswordFieldVisible, "Show dependency is not working for Gift Card Password field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Password field");

        boolean isGiftCardMerchantIdFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Merchant ID ");
        Assert.assertTrue(isGiftCardMerchantIdFieldVisible, "Show dependency is not working for Gift Card Merchant Id field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Merchant Id field");

        boolean isGiftCardMerchantNameFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Merchant Name ");
        Assert.assertTrue(isGiftCardMerchantNameFieldVisible, "Show dependency is not working for Gift Card Merchant Name field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Merchant Name field");

        boolean isGiftCardStoreIdFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Store ID ");
        Assert.assertTrue(isGiftCardStoreIdFieldVisible, "Show dependency is not working for Gift Card Store Id field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Store Id field");

        boolean isGiftCardRoutingIdFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Routing ID ");
        Assert.assertTrue(isGiftCardRoutingIdFieldVisible, "Show dependency is not working for Gift Card Routing Id field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Routing Id field");

        boolean isGiftCardItemIdFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Item ID ");
        Assert.assertTrue(isGiftCardItemIdFieldVisible, "Show dependency is not working for Gift Card Item Id field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Item Id field");

        boolean isGiftCardTenderIdFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Tender ID ");
        Assert.assertTrue(isGiftCardTenderIdFieldVisible, "Show dependency is not working for Gift Card Tender ID field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Tender ID field");

        boolean isGiftCardProxyPortFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Proxy Port ");
        Assert.assertTrue(isGiftCardProxyPortFieldVisible, "Show dependency is not working for Gift Card Proxy Port field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Proxy Port field");

        boolean isGiftCardUrlFieldVisible=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Url ");
        Assert.assertTrue(isGiftCardUrlFieldVisible, "Show dependency is not working for Gift Card Url field");
        pageObj.utils().logPass("Show dependency is working for Gift Card Url field");

        // this is to verify Hide Dependency if Gift card is set to False.
        pageObj.ppccPolicyPage().enterHasGiftCardIntegrationCheckBox();
        boolean isGiftCardVendorFieldHidden=pageObj.ppccPolicyPage().isFieldVisible("Gift Card Vendor ");
        Assert.assertFalse(isGiftCardVendorFieldHidden, "Hide dependency is not working for Gift Card Vendor field");
        pageObj.utils().logPass("Hide dependency is working for Gift Card Vendor field");

        boolean isGiftCardUserFieldHidden=pageObj.ppccPolicyPage().isFieldVisible("Gift Card User ");
        Assert.assertFalse(isGiftCardUserFieldHidden, "Hide dependency is not working for Gift Card User field");
        pageObj.utils().logPass("Hide dependency is working for Gift Card User Field");

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
