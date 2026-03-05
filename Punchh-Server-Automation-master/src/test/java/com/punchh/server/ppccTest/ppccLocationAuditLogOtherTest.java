/*
 * @author Kalpana Singh (kalpana.singh@partech.com)
 * @brief This class contains UI test cases for the POS Control Center > Location Audit Log > miscellaneous test.
 * @fileName ppccLocationTest.java
 */
package com.punchh.server.ppccTest;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class ppccLocationAuditLogOtherTest {
    static Logger logger = LogManager.getLogger(ppccLocationAuditLogOtherTest.class);
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

    @Test(description = "SQ-T6459 Verify the sorting functionality on Location Audit Log using Location Id"
            + "SQ-T6460 Verify the sorting functionality on Location Audit Log using Status"
            + "SQ-T6469 Verify the sorting functionality on Location Audit Log using event Type"
            + "SQ-T6470 Verify the sorting functionality on Location Audit Log using username"
            + "SQ-T6458 Verify the sorting functionality on Location Audit Log using Store Name")
     @Owner(name = "Kalpana")
     public void T6459_verifyTheSortingFunctionalityOnLocationAuditLogUsingLocationId() throws InterruptedException{
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccLocationAuditLogPage().navigateToAuditLogs();
        pageObj.ppccLocationAuditLogPage().changeEntriesToHundred(100);

        // apply sort and verify data for store name column in audit log in ascending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Store Name");
        List<String> storeNameValueList = pageObj.ppccLocationAuditLogPage().getColumnData("1");
        List<String> sortedStoreNameList = new ArrayList<>(storeNameValueList);
        Collections.sort(sortedStoreNameList);
        Assert.assertEquals(sortedStoreNameList, storeNameValueList, "StoreName column data is not in ascending order.");
        pageObj.utils().logPass("StoreName column data is in ascending order.");

        // apply sort and verify data for store name column in audit log in descending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Store Name");
        storeNameValueList = pageObj.ppccLocationAuditLogPage().getColumnData("1");
        List<String> sortedstoreNameValueListDesc = new ArrayList<>(storeNameValueList);
        sortedstoreNameValueListDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(sortedstoreNameValueListDesc, storeNameValueList, "StoreName column data is not in descending order.");
        pageObj.utils().logPass("StoreName column data is in descending order.");

        // apply sort and verify data for location id column in audit log in ascending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Location id");
        List<String> columnValueLocationIdList = pageObj.ppccLocationAuditLogPage().getColumnData("2");
        List<String> sortedColumnValueLocationIdList = new ArrayList<>(columnValueLocationIdList);
        Collections.sort(sortedColumnValueLocationIdList);
        Assert.assertEquals(sortedColumnValueLocationIdList, columnValueLocationIdList, "Location Id column data is not in ascending order.");
        pageObj.utils().logPass("Location Id column data is in ascending order.");

        // apply sort and verify data for location id column in audit log in descending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Location id");
        columnValueLocationIdList = pageObj.ppccLocationAuditLogPage().getColumnData("2");
        List<String> sortedColumnValueLocationIdListDesc = new ArrayList<>(columnValueLocationIdList);
        sortedColumnValueLocationIdListDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(sortedColumnValueLocationIdListDesc, columnValueLocationIdList, "Location Id column data is not in descending order.");
        pageObj.utils().logPass("Location Id column data is in descending order.");

        // apply sort and verify data for Status column in audit log in ascending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Status");
        List<String> columnValueStatusList = pageObj.ppccLocationAuditLogPage().getColumnData("3");
        List<String> sortedColumnValueStatusList = new ArrayList<>(columnValueStatusList);
        Collections.sort(sortedColumnValueStatusList);
        Assert.assertEquals(sortedColumnValueStatusList, columnValueStatusList, "Status column data is not in ascending order.");
        pageObj.utils().logPass("Status column data is in ascending order.");

        // apply sort and verify data for Status column in audit log in descending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Status");
        columnValueStatusList = pageObj.ppccLocationAuditLogPage().getColumnData("3");
        List<String> sortedcolumnValueStatusListDesc = new ArrayList<>(columnValueStatusList);
        sortedcolumnValueStatusListDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(sortedcolumnValueStatusListDesc, columnValueStatusList, "Status column data is not in descending order.");
        pageObj.utils().logPass("Status column data is in descending order.");

        // apply sort and verify data for Event type column in audit log in ascending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Event Type");
        List<String> columnValueEventTypeList = pageObj.ppccLocationAuditLogPage().getColumnData("6");
        List<String> sortedColumnValueEventTypeList = new ArrayList<>(columnValueEventTypeList);
        Collections.sort(sortedColumnValueEventTypeList);
        Assert.assertEquals(sortedColumnValueEventTypeList, columnValueEventTypeList, "EventType column data is not in ascending order.");
        pageObj.utils().logPass("EventType column data is in ascending order.");

        // apply sort and verify data for Event type column in audit log in descending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Event Type");
        columnValueEventTypeList = pageObj.ppccLocationAuditLogPage().getColumnData("6");
        List<String> sortedcolumnValueEventTypeListDesc = new ArrayList<>(columnValueEventTypeList);
        sortedcolumnValueEventTypeListDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(sortedcolumnValueEventTypeListDesc, columnValueEventTypeList, "EventType column data is not in descending order.");
        pageObj.utils().logPass("EventType column data is in descending order.");

        // apply sort and verify data for username column in audit log in ascending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Username");
        List<String> columnValueUserNameList = pageObj.ppccLocationAuditLogPage().getColumnData("5");
        List<String> sortedColumnValueUserNameList = new ArrayList<>(columnValueUserNameList);
        Collections.sort(sortedColumnValueUserNameList);
        Assert.assertEquals(sortedColumnValueUserNameList, columnValueUserNameList, "Username column data is not in ascending order.");
        pageObj.utils().logPass("Username column data is in ascending order.");

        // apply sort and verify data for username column in audit log in descending order.
        pageObj.ppccLocationAuditLogPage().sortColumn("Username");
        columnValueUserNameList = pageObj.ppccLocationAuditLogPage().getColumnData("5");
        List<String> sortedcolumnValueUserNameListDesc = new ArrayList<>(columnValueUserNameList);
        sortedcolumnValueUserNameListDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(sortedcolumnValueUserNameListDesc, columnValueUserNameList, "Username column data is not in descending order.");
        pageObj.utils().logPass("Username column data is in descending order.");
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
