/*
* @author Aman Jain (aman.jain@partech.com)
* @brief This class contains UI test cases for the POS Control Center Policy Audit Logs.
* @fileName ppccPolicyAuditLogsSortingFunctionalityTest.java
*/

package com.punchh.server.ppccTest;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
public class ppccPolicyAuditLogsSortingFunctionalityTest {
    static Logger logger = LogManager.getLogger(ppccPolicyAuditLogsSortingFunctionalityTest.class);
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

    @Test(description = "SQ-T6165 Validate the sorting for policy Id column in policy management audit log.")
    @Owner(name = "Aman Jain")
    public void SQ_T6165_verifySortingFunctionalityOfPolicyIdColumnOfPolicyMgmtAuditLog() throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().changeEntriesToHundred(100);

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.policyIdHeader");
        List<String> policyIdData = pageObj.ppccPolicyAuditLogs().getColumnData("2");
        List<String> sortedPolicyIdData = policyIdData.stream().map(Integer::parseInt).sorted().map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertEquals(policyIdData, sortedPolicyIdData, "Policy Id column data is not in ascending order.");
        pageObj.utils().logPass("Policy Id column data is in ascending order.");

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.policyIdHeader");
        policyIdData = pageObj.ppccPolicyAuditLogs().getColumnData("2");
        List<String> sortedPolicyIdDataDesc = policyIdData.stream().map(Integer::parseInt).sorted(Comparator.reverseOrder()).map(String::valueOf)
                .collect(Collectors.toList());
        Assert.assertEquals(policyIdData, sortedPolicyIdDataDesc, "Policy Id column data is not in descending order.");
        pageObj.utils().logPass("Policy Id column data is in descending order.");
    }

    @Test(description = "SQ-T6165 Validate the sorting for status column in policy management audit log.")
    @Owner(name = "Aman Jain")
    public void SQ_T6165_verifySortingFunctionalityOfStatusColumnOfPolicyMgmtAuditLog() throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().changeEntriesToHundred(100);

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.statusHeader");
        List<String> statusData = pageObj.ppccPolicyAuditLogs().getColumnData("3");
        List<String> sortedStatusData = new ArrayList<>(statusData);
        Collections.sort(sortedStatusData);
        Assert.assertEquals(statusData, sortedStatusData, "Status column data is not in ascending order.");
        pageObj.utils().logPass("Status column data is in ascending order.");

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.statusHeader");
        statusData = pageObj.ppccPolicyAuditLogs().getColumnData("3");
        List<String> sortedStatusDataDesc = new ArrayList<>(statusData);
        sortedStatusDataDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(statusData, sortedStatusDataDesc, "Status column data is not in descending order.");
        pageObj.utils().logPass("Status column data is in descending order.");
    }

    @Test(description = "SQ-T6165 Validate the sorting for username column in policy management audit log.")
    @Owner(name = "Aman Jain")
    public void SQ_T6165_verifySortingFunctionalityOfUsernameColumnOfPolicyMgmtAuditLog() throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().changeEntriesToHundred(100);

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.usernameHeader");
        List<String> usernameData = pageObj.ppccPolicyAuditLogs().getColumnData("4");
        List<String> sortedUsernameData = new ArrayList<>(usernameData);
        Collections.sort(sortedUsernameData);
        Assert.assertEquals(usernameData, sortedUsernameData, "Username column data is not in ascending order.");
        pageObj.utils().logPass("Username column data is in ascending order.");

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.usernameHeader");
        usernameData = pageObj.ppccPolicyAuditLogs().getColumnData("4");
        List<String> sortedUsernameDataDesc = new ArrayList<>(usernameData);
        sortedUsernameDataDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(usernameData, sortedUsernameDataDesc, "Username column data is not in descending order.");
        pageObj.utils().logPass("Username column data is in descending order.");
    }

    @Test(description = "SQ-T6165 Validate the sorting for event type column in policy management audit log.")
    @Owner(name = "Aman Jain")
    public void SQ_T6165_verifySortingFunctionalityOfEventTypeColumnOfPolicyMgmtAuditLog() throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().changeEntriesToHundred(100);

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.eventTypeHeader");
        List<String> eventTypeData = pageObj.ppccPolicyAuditLogs().getColumnData("5");
        List<String> sortedEventTypeData = new ArrayList<>(eventTypeData);
        Collections.sort(sortedEventTypeData);
        Assert.assertEquals(eventTypeData, sortedEventTypeData, "Event Type column data is not in ascending order.");
        pageObj.utils().logPass("Event Type column data is in ascending order.");

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.eventTypeHeader");
        eventTypeData = pageObj.ppccPolicyAuditLogs().getColumnData("5");
        List<String> sortedEventTypeDataDesc = new ArrayList<>(eventTypeData);
        sortedEventTypeDataDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(eventTypeData, sortedEventTypeDataDesc, "Event Type column data is not in descending order.");
        pageObj.utils().logPass("Event Type column data is in descending order.");
    }

    @Test(description = "SQ-T6165 Validate the sorting for policy name column in policy management audit log.")
    @Owner(name = "Aman Jain")
    public void SQ_T6165_verifySortingFunctionalityOfPolicyNameColumnOfPolicyMgmtAuditLog()
            throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().changeEntriesToHundred(100);

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.policyNameHeader");
        List<String> policyNameData = pageObj.ppccPolicyAuditLogs().getColumnData("1");
        List<String> sortedPolicyNameData = pageObj.ppccPolicyAuditLogs().sortList(policyNameData);
        Assert.assertEquals(policyNameData, sortedPolicyNameData, "Policy Name column data is not in ascending order.");
        pageObj.utils().logPass("Policy Name column data is in ascending order.");

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.policyNameHeader");
        policyNameData = pageObj.ppccPolicyAuditLogs().getColumnData("1");
        pageObj.utils().logit("Policy Name Data: " + policyNameData);
        List<String> sortedPolicyNameDataDesc = pageObj.ppccPolicyAuditLogs().sortList(policyNameData);
        Collections.reverse(sortedPolicyNameDataDesc);
        pageObj.utils().logit("Sorted Policy Name Data Descending: " + sortedPolicyNameDataDesc);
        Assert.assertEquals(policyNameData, sortedPolicyNameDataDesc, "Policy Name column data is not in descending order.");
        pageObj.utils().logPass("Policy Name column data is in descending order.");
    }

    @Test(description = "SQ-T6165 Validate the sorting for event date time column in policy management audit log.")
    @Owner(name = "Aman Jain")
    public void SQ_T6165_verifySortingFunctionalityOfEventDateTimeColumnOfPolicyMgmtAuditLog()
            throws InterruptedException {
        pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
        pageObj.menupage().navigateToSubMenuItem("Settings", "POS Control Center");
        pageObj.ppccPolicyPage().navigateToPolicyManagementTab();
        pageObj.ppccPolicyPage().navigateToAuditLogs();
        pageObj.ppccPolicyAuditLogs().changeEntriesToHundred(100);

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.eventDateTimeHeader");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a", Locale.ENGLISH);
        List<LocalDateTime> eventDateTimeData = pageObj.ppccPolicyAuditLogs().getEventDateTimeColumnData("6",
                formatter);
        List<LocalDateTime> sortedEventDateTimeData = new ArrayList<>(eventDateTimeData);
        Collections.sort(sortedEventDateTimeData);
        Assert.assertEquals(eventDateTimeData, sortedEventDateTimeData,
                "Event date time column data is not in ascending order.");
        pageObj.utils().logPass("Event date time column data is in ascending order.");

        pageObj.ppccPolicyAuditLogs().sortData("ppccPolicyAuditLog.eventDateTimeHeader");
        eventDateTimeData = pageObj.ppccPolicyAuditLogs().getEventDateTimeColumnData("6", formatter);
        List<LocalDateTime> sortedEventDateTimeDataDesc = new ArrayList<>(eventDateTimeData);
        sortedEventDateTimeDataDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(eventDateTimeData, sortedEventDateTimeDataDesc,
                "Event date time column data is not in descending order.");
        pageObj.utils().logPass("Event date time column data is in descending order.");
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
