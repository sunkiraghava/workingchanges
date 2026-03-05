package com.punchh.server.pages;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PpccLocationPage {
    static Logger logger = LogManager.getLogger(PpccLocationPage.class);
    private WebDriver driver;
    Properties prop;
    Utilities utils;
    SeleniumUtilities selUtils;

    String publishedPolicy="";

    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    public PpccLocationPage(WebDriver driver) {
        this.driver = driver;
        prop = Utilities.loadPropertiesFile("config.properties");
        utils = new Utilities(driver);
        selUtils = new SeleniumUtilities(driver);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
    public void navigateToLocationsTab() {
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.LocationsTab").click();
        logger.info("User navigated to Locations tab");
        TestListeners.extentTest.get().pass("User navigated to Locations tab");
        utils.waitTillPagePaceDone();
    }

    public void searchLocationsInDeprovisionedList(String LocationName) {
		List<WebElement> searchInputDeprovisionedGrid = utils.getLocatorList("ppccLocationPage.searchField");
		searchInputDeprovisionedGrid.get(1).click();
		searchInputDeprovisionedGrid.get(1).sendKeys(LocationName);
        utils.waitTillPagePaceDone();
        logger.info("Locations is searched in Deprovision List");
        TestListeners.extentTest.get().pass("Locations is searched in Deprovision List");
    }

    public void searchLocationsInProvisionedList(String LocationName) {
        utils.waitTillPagePaceDone();
		List<WebElement> searchInputDeprovisionedGrid = utils.getLocatorList("ppccLocationPage.searchField");
        utils.waitTillPagePaceDone();
		searchInputDeprovisionedGrid.get(0).click();
		searchInputDeprovisionedGrid.get(0).clear();
		searchInputDeprovisionedGrid.get(0).sendKeys(LocationName);
        utils.waitTillPagePaceDone();
        logger.info("Locations is searched in Deprovision List");
        TestListeners.extentTest.get().pass("Locations is searched in Provisioned List");
    }

    public void provisionALocation(String publishedPolicy, String packageVersion) {
        utils.longWaitInSeconds(2);
//		utils.clickByJSExecutor(driver, utils.getLocator("ppccLocationPage.selectDeprovisionCheckbox"));
		utils.getLocator("ppccLocationPage.selectDeprovisionCheckbox").click();
        logger.info("Locations are selected");
        utils.waitTillPagePaceDone();
//		utils.clickByJSExecutor(driver, utils.getLocator("ppccLocationPage.provisionButton"));
		utils.getLocator("ppccLocationPage.provisionButton").click();
        utils.waitTillPagePaceDone();
        logger.info("provision button is clicked");
        TestListeners.extentTest.get().pass("Provision Button is clicked");
        utils.waitTillPagePaceDone();
        selectPosTypeInPopUp("Aloha");
        utils.longWaitInSeconds(2);
        selectPolicyInPopUp(publishedPolicy);
        utils.longWaitInSeconds(2);
        selectPackageInPopUp(packageVersion);
        WebElement confirmProvisionfield = wait.until(ExpectedConditions.elementToBeClickable(utils.getLocator("ppccLocationPage.confirmTextFieldProvisionPopUp")));
        confirmProvisionfield.sendKeys("Confirm");
        logger.info("Confirm is entered");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmButtonProvisionPopUp").click();
        utils.waitTillPagePaceDone();
        logger.info("Location is provisioned");
        utils.waitTillPagePaceDone();
    }

    public void selectPosTypeInPopUp(String posType) {

		utils.longWaitInSeconds(2);
        WebElement selectPosTypeDropdown = utils.getLocator("ppccLocationPage.posDropdownProvisionPopUp");
        selectPosTypeDropdown.click();
        List<WebElement> selectPosInProvision = utils.getLocatorList("ppccLocationPage.posDropdownListProvisionPopUp");
		utils.waitTillPagePaceDone();
        utils.selecDrpDwnValue(selectPosInProvision, posType);
        logger.info("POS Type is selected");
        TestListeners.extentTest.get().pass("POS type is selected");
        utils.waitTillPagePaceDone();
    }

    public void selectPolicyInPopUp(String policyName) {

        WebElement selectPolicyTypeDropdown = wait.until(ExpectedConditions.elementToBeClickable(utils.getLocator("ppccLocationPage.policyDropdownProvisionPopUp")));
        selectPolicyTypeDropdown.click();
        List<WebElement> selectPolicy = utils.getLocatorList("ppccLocationPage.policyListProvisionPopUp");
		utils.waitTillPagePaceDone();
        utils.selecDrpDwnValue(selectPolicy, policyName);
        logger.info("Policy is selected");
        TestListeners.extentTest.get().pass("Policy is selected");
		utils.waitTillPagePaceDone();
    }

    public void selectPackageInPopUp(String packageName) {

        WebElement selectPackageDropdown = wait.until(ExpectedConditions.elementToBeClickable(utils.getLocator("ppccLocationPage.packageDropdownProvisionPopUp")));
        selectPackageDropdown.click();
        utils.waitTillPagePaceDone();
        List<WebElement> selectPackage = utils.getLocatorList("ppccLocationPage.packageSelectClass");
        for (WebElement webElement : selectPackage) {
            if (webElement.getAttribute("title").equals(packageName)) {
                webElement.click();
                break;
            }
        }
        logger.info("Package is selected");
        TestListeners.extentTest.get().pass("Package is selected");
        utils.waitTillPagePaceDone();
    }

    public String getPolicyNameofLocation() {

        List<WebElement> policyColumnProvisionedList = utils.getLocatorList("ppccLocationPage.policyColumnProvisionedList");
        WebElement firstrowProvisionedLocation = policyColumnProvisionedList.get(0);
        String provisionedActualPolicyName = firstrowProvisionedLocation.getText();
        logger.info("Policy Name is Correct" + provisionedActualPolicyName);
        TestListeners.extentTest.get().pass("Policy Name is Correct");
        return provisionedActualPolicyName;
    }

    public String getPolicyStatusOfLocation() {
        List<WebElement> policyStatusProvisionedList = utils.getLocatorList("ppccLocationPage.policyStatusProvisionedList");
        WebElement statusProvisionedLocation = policyStatusProvisionedList.get(0);
        String statusActualProvisionedLocation = statusProvisionedLocation.getText();
        logger.info("Policy Status is Correct" + statusActualProvisionedLocation);
        TestListeners.extentTest.get().pass("Policy Status is Correct");
        return statusActualProvisionedLocation;
    }

    public String getPackageNameofLocation() {

        List<WebElement> packageNameProvisionedList = utils.getLocatorList("ppccLocationPage.packageNameProvisionedList");
        WebElement firstrowProvisionedLocation = packageNameProvisionedList.get(0);
        String provisionedActualPackageName = firstrowProvisionedLocation.getText();
        logger.info("Package Name is Correct" + provisionedActualPackageName );
        TestListeners.extentTest.get().pass("Package Name is Correct");
        return provisionedActualPackageName;
    }

    public String getPackageStatusofLocation() {

        List<WebElement> packageStatusProvisionedList = utils.getLocatorList("ppccLocationPage.packageStatusProvisionedList");
        WebElement firstrowProvisionedLocation = packageStatusProvisionedList.get(0);
        String provisionedActualPackageStatus = firstrowProvisionedLocation.getText();
        logger.info("Package Status is Correct" + provisionedActualPackageStatus);
        TestListeners.extentTest.get().pass("Package Status is Correct");
        return provisionedActualPackageStatus;
    }

    public void deProvisionALocation(){
        clickSelectAllCheckbox();
        utils.getLocator("ppccLocationPage.deprovisionButton").click();
        utils.waitTillPagePaceDone();
        logger.info("De-Provision Pop up is opened");
        utils.getLocator("ppccLocationPage.confirmTextFieldDeprovisionAction").sendKeys("Confirm");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmButtonProvisionPopUp").click();
        utils.waitTillPagePaceDone();
        logger.info("Location is de-provisioned");
        TestListeners.extentTest.get().pass("Location is de-provisioned");
    }

    public void changePolicyALocation(String changePolicy){
        clickSelectAllCheckbox();
        utils.getLocator("ppccLocationPage.changePolicyButton").click();
        utils.waitTillPagePaceDone();
        logger.info("Change policy Pop up is opened");
        TestListeners.extentTest.get().pass("Change policy Pop up is opened");

        WebElement selectChangePolicyTypeDropdown = wait.until(ExpectedConditions.elementToBeClickable(utils.getLocator("ppccLocationPage.policyDropdownProvisionPopUp")));
        selectChangePolicyTypeDropdown.click();
        utils.waitTillPagePaceDone();
        List<WebElement> policyList = utils.getLocatorList("ppccLocationPage.policyListProvisionPopUp");
        utils.waitTillPagePaceDone();
        utils.selecDrpDwnValue(policyList, changePolicy);
        logger.info("Policy is selected");
        TestListeners.extentTest.get().pass("Policy is selected in Change Policy Pop up");
        utils.waitTillPagePaceDone();

        utils.getLocator("ppccLocationPage.confirmTextFieldChangePolicyAction").sendKeys("Confirm");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmButtonProvisionPopUp").click();
        utils.waitTillPagePaceDone();
        logger.info("Location is changed with policy successfully.");
        TestListeners.extentTest.get().pass("Change Policy Action is performed on provisioned location");
    }

    public void remoteUpgradeALocation(String packageName){
        clickSelectAllCheckbox();
        utils.getLocator("ppccLocationPage.remoteUpgradeButton").click();
        utils.waitTillPagePaceDone();
        logger.info("Remote Upgrade Pop up is opened");
        TestListeners.extentTest.get().pass("Remote Upgrade Pop up is opened");
        utils.waitTillPagePaceDone();
        selectPackageInPopUp(packageName);
        logger.info("Package is selected");
        TestListeners.extentTest.get().pass("Package is selected in Remote Upgrade up");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmTextFieldRemoteUpgradeAction").sendKeys("Confirm");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmButtonProvisionPopUp").click();
        utils.waitTillPagePaceDone();
        logger.info("Location is remote upgraded successfully.");
        TestListeners.extentTest.get().pass("Location is remote upgraded successfully.");
    }

    public void cancelUpdateALocation(){
        clickSelectAllCheckbox();
        utils.getLocator("ppccLocationPage.moreActionsButton").click();
        logger.info("More Actions is clicked");
        TestListeners.extentTest.get().pass("More Actions is clicked");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.cancelUpdateButton").click();
        logger.info("Cancel Update is clicked");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmTextFieldCancelUpdateAction").sendKeys("Confirm");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmButtonProvisionPopUp").click();
        utils.waitTillPagePaceDone();
        logger.info("Location is Updates Cancelled");
        TestListeners.extentTest.get().pass("Location is Cancelled updated successfully.");
        utils.waitTillPagePaceDone();
    }
    public void InitiateUpdateALocation(){
        clickSelectAllCheckbox();
        utils.getLocator("ppccLocationPage.moreActionsButton").click();
        logger.info("More Actions is clicked");
        TestListeners.extentTest.get().pass("More Actions is clicked");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.initiateUpdateButton").click();
        logger.info("Initiate Update is clicked");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmTextFieldInitiateUpdateAction").sendKeys("Confirm");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.confirmButtonProvisionPopUp").click();
        logger.info("Location is Initiated for update");
        TestListeners.extentTest.get().pass("Location is Initiated for update successfully.");
        utils.waitTillPagePaceDone();
    }

    public boolean getStoreNameofLocation(String expectedStoreName) {

        List<WebElement> storeColumnProvisionedList = utils.getLocatorList("ppccLocationPage.storeName");
        boolean isStoreNamePresent = false;
        for (WebElement element : storeColumnProvisionedList) {
            if (element.getText().equals(expectedStoreName)) {
                isStoreNamePresent = true;
                break;
            }
        }
        logger.info("Store Name is returned in provisioned location list." + expectedStoreName);
        TestListeners.extentTest.get().pass("Store Name is returned in provisioned location list.");
        utils.waitTillPagePaceDone();
        return isStoreNamePresent;
    }

    public void applyFilterOnLocations(String filterOption, String filterValue)
    {
        utils.waitTillPagePaceDone();
        String filterValueXpath = utils.getLocatorValue("ppccLocationPage.filterValue").replace("{filterValue}",
                filterValue);
        String filterOptionXpath = utils.getLocatorValue("ppccLocationPage.filterValue").replace("{filterValue}",
                filterOption);
        utils.getLocator("ppccLocationPage.filterIcon").click();
        logger.info("FilterIcon is clicked");
        TestListeners.extentTest.get().pass("FilterIcon is clicked");
        utils.getLocator("ppccLocationPage.selectTextOnFilter").click();
        utils.longWaitInSeconds(2);
        driver.findElement(By.xpath(filterOptionXpath)).click();
        logger.info("Selecting the filter Key in filters");
        TestListeners.extentTest.get().pass("Selecting the filter Key in filters");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.selectTextOnFilter").click();
        driver.findElement(By.xpath(filterValueXpath)).click();
        logger.info("Selecting the filter Value in filters");
        TestListeners.extentTest.get().pass("Selecting the filter Value in filters");
        utils.getLocator("ppccLocationPage.applyButtonOnFilter").click();
        logger.info("Apply button is clicked in filters, filters are applied.");
        TestListeners.extentTest.get().pass("Apply button is clicked in filters, filters are applied.");
        utils.waitTillPagePaceDone();
    }

    public void removeAllFilters()
    {
        driver.findElement(By.xpath(utils.getLocatorValue("ppccLocationPage.removeAllFilters"))).click();
        utils.waitTillPagePaceDone();
    }

    public void removeFiltersPopup()
    {
        driver.findElement(By.xpath(utils.getLocatorValue("ppccLocationPage.removeFiltersPopup"))).click();
    }

    public boolean verifyFilteredLocationList(String expectedValue,String filterOption)
    {
        String Locations = utils.getLocatorValue("ppccLocationPage.locationsRow");
        List<WebElement> rows = driver.findElements(By.xpath(Locations));

        if (rows.isEmpty()) {
            return false;
        }

        String columnValue="";
        switch (filterOption) {
            case "POS Type":
                columnValue = utils.getLocatorValue("ppccLocationPage.posTypeColumn");
                break;
            case "Policy Status":
                columnValue = utils.getLocatorValue("ppccLocationPage.policyStatusColumn");
                break;
            case "Policy Name":
                columnValue = utils.getLocatorValue("ppccLocationPage.policyNameColumn");
                break;
            case "Package Status":
                columnValue = utils.getLocatorValue("ppccLocationPage.locationStatus");
                break;
            case "Health Status":
                columnValue = utils.getLocatorValue("ppccLocationPage.healthStatusColumn");
                break;
            case "Has Overrides":
                columnValue = utils.getLocatorValue("ppccLocationPage.configOverrideColumn");
                break;
            default:
                logger.warn("Invalid filter option: " + filterOption);
                return false;
        }

        for (WebElement row : rows)
        {
            WebElement column = row.findElement(By.xpath(columnValue));
            String value = column.getText();

            if (!value.contains(expectedValue))
            {
                logger.info("Column Value is not same:  '" + expectedValue + "': " + value);
                return false;
            }
        }
        return true;
    }

    public boolean isLocationsFiltered(String expectedValue, String filterOption)
    {
        boolean isLocationsFiltered = verifyFilteredLocationList(expectedValue,filterOption);
        return isLocationsFiltered;
    }


    public boolean isFilterPresent(String filterOption)
    {
        utils.getLocator("ppccLocationPage.filterIcon").click();
        utils.waitTillPagePaceDone();
        logger.info("FilterIcon is clicked");
        TestListeners.extentTest.get().pass("FilterIcon is clicked");
        utils.getLocator("ppccLocationPage.selectTextOnFilter").click();

        boolean isFilterPresent = false;
        List<WebElement> filterOptions = utils.getLocatorList("ppccLocationPage.filterPresent");

        for (WebElement filter : filterOptions) {
            if (filter.getText().equalsIgnoreCase(filterOption)) {
                logger.info("Filter present is: " + filter.getText());
                isFilterPresent = true;
                break;
            }
        }
        if (!isFilterPresent) {
            logger.info("Filter is not present: " + filterOption + "': " + filterOption);
        }
        return isFilterPresent;
    }
    public static Map<String, List<String>> saveListOfLocationGroupWithStoreNumber(Response response) {
        Map<String, List<String>> locationGroupData = new HashMap<>();
        JsonPath jsonPath = response.jsonPath();
        List<Map<String, Object>> groups = jsonPath.getList("");
        for (Map<String, Object> group : groups) {
            String groupName = (String) group.get("name");

            List<Map<String, Object>> locations = (List<Map<String, Object>>) group.get("locations");
            List<String> storeNumbers = new ArrayList<>();

            for (Map<String, Object> location : locations) {
                storeNumbers.add((String) location.get("store_number"));
            }

            locationGroupData.put(groupName, storeNumbers);
        }
        logger.info("Location Group Data: " + locationGroupData);
        return locationGroupData;
    }

    public List<String> getAllLocationGroupFromFilter(String filterOption) {
        String filterOptionXpath = utils.getLocatorValue("ppccLocationPage.filterValue").replace("{filterValue}",
                filterOption);
        utils.getLocator("ppccLocationPage.filterIcon").click();
        logger.info("FilterIcon is clicked");
        TestListeners.extentTest.get().pass("FilterIcon is clicked");
        utils.getLocator("ppccLocationPage.selectTextOnFilter").click();
        driver.findElement(By.xpath(filterOptionXpath)).click();
        logger.info("Selecting the filter Key in filters");
        TestListeners.extentTest.get().pass("Selecting the filter Key in filters");
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.selectTextOnFilter").click();
        List<WebElement> filterOptions = utils.getLocatorList("ppccLocationPage.filterPresent");
        List<String> optionsText = new ArrayList<>();
        for (WebElement option : filterOptions) {
            optionsText.add(option.getText());
        }
        logger.info("All Location Group are present in Filter");
        TestListeners.extentTest.get().pass("All Location Group are present in Filter");
        return optionsText;
    }
    public List<String> getDisplayedStoreNumbers() {
        List<WebElement> storeNumberColumn = utils.getLocatorList("ppccLocationPage.storeNumberColumn");
        List<String> storeNumbers = new ArrayList<>();
        TestListeners.extentTest.get().pass("All Location Group are present in Filter");
        for (WebElement store_number : storeNumberColumn) {
            storeNumbers.add(store_number.getText().trim());
        }
        logger.info("Store Number are displayed on UI");
        return storeNumbers;
    }
    public void selectLocationGroupNameInFilter(String groupName) {
        String filterValueXpath = utils.getLocatorValue("ppccLocationPage.filterValue").replace("{filterValue}",
                groupName);
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.selectTextOnFilter").click();
        driver.findElement(By.xpath(filterValueXpath)).click();
        logger.info("Selecting the filter Value in filters");
        TestListeners.extentTest.get().pass("Selecting the filter Value in filters");
        utils.getLocator("ppccLocationPage.applyButtonOnFilter").click();
        logger.info("Apply button is clicked in filters, filters are applied.");
        TestListeners.extentTest.get().pass("Apply button is clicked in filters, filters are applied.");
        utils.waitTillPagePaceDone();
    }

    public void clickOnCancelOnFilters(){
        utils.getLocator("ppccLocationPage.cancelButtonOnFilter").click();
        logger.info("Cancel button is clicked in filters, filters pop up is closed.");
        TestListeners.extentTest.get().pass("Cancel button is clicked in filters, filters pop up is closed.");
        utils.waitTillPagePaceDone();
    }

    public void selectFilterKey(String filterOption)
    {
        String filterOptionXpath = utils.getLocatorValue("ppccLocationPage.filterValue").replace("{filterValue}",
                filterOption);
        utils.getLocator("ppccLocationPage.filterIcon").click();
        logger.info("FilterIcon is clicked");
        TestListeners.extentTest.get().pass("FilterIcon is clicked");
        utils.getLocator("ppccLocationPage.filterOption").click();
        driver.findElement(By.xpath(filterOptionXpath)).click();
        logger.info("Selecting the filter Key in filters");
        TestListeners.extentTest.get().pass("Selecting the filter Key in filters");
        utils.waitTillPagePaceDone();
    }

    public void clickOnFilterIcon(){
        utils.getLocator("ppccLocationPage.filterIcon").click();
        utils.waitTillPagePaceDone();
        logger.info("FilterIcon is clicked");
        TestListeners.extentTest.get().pass("FilterIcon is clicked");
    }
    public boolean validateLocationGroupsInFilter(Map<String, List<String>> locationGroupDataFromApi, List<String> locationGroupPresentInFilter, String filterOption) {
        for (Map.Entry<String, List<String>> entry : locationGroupDataFromApi.entrySet()) {
            String locationGroupName = entry.getKey();
            List<String> expectedStoreNumbers = entry.getValue();
            if (!locationGroupPresentInFilter.contains(locationGroupName)) {
                logger.info("Location Group name not in dropdown: " + locationGroupName);
                TestListeners.extentTest.get().pass("Location Group Name is present in filter");
                return false;
            }
            selectFilterKey(filterOption);
            logger.info("Location Group Filter is selected in filter");
            TestListeners.extentTest.get().pass("Location Group Filter is selected in filter");
            selectLocationGroupNameInFilter(locationGroupName);
            logger.info("Location Group Value is selected in filter");
            TestListeners.extentTest.get().pass("Location Group Value is selected in filter");
            clickOnFilterIcon();
            logger.info("Filter Icon is clicked");
            TestListeners.extentTest.get().pass("Filter Icon is clicked");
            List<String> actualStoreNumbers = getDisplayedStoreNumbers();
            logger.info("Store Numbers are returned as on UI");
            Set<String> expectedSet = new HashSet<>(expectedStoreNumbers);
            Set<String> actualSet = new HashSet<>(actualStoreNumbers);

            if (!expectedSet.equals(actualSet)) {
                logger.info("Store numbers mismatch for group: " + locationGroupName);
                return false;
            }
        }
        return true;
    }

    public void clickOnSetConfigButtonOnLocation(){
        utils.getLocator("ppccLocationPage.setConfigLocationList").click();
        String waitTillItem = utils.getLocatorValue("ppccLocationPage.headerConfigOverridePopup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(waitTillItem)));
        logger.info("Set Config is clicked");
        TestListeners.extentTest.get().pass("Set Config is clicked");
    }

    public void clickOnViewConfigButtonOnLocation(){
        utils.getLocator("ppccLocationPage.viewConfigLocationList").click();
        String waitTillItem = utils.getLocatorValue("ppccLocationPage.headerConfigOverridePopup");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(waitTillItem)));
        logger.info("View Config is clicked");
        TestListeners.extentTest.get().pass("View Config is clicked");
    }

    public void clickOnCancelInConfigOverridePopUp(){
        utils.getLocator("ppccLocationPage.cancelButtonOverridePopup").click();
        logger.info("Cancel button is clicked in Override Pop Up");
        TestListeners.extentTest.get().pass("Cancel button is clicked in Override Pop Up");
        utils.waitTillPagePaceDone();
    }

    public void clickOnXIconInConfigOverridePopUp(){
        utils.getLocator("ppccLocationPage.xIconConfigOverridePopup").click();
        logger.info("X button is clicked in Override Pop Up");
        TestListeners.extentTest.get().pass("X button is clicked in Override Pop Up");
        utils.waitTillPagePaceDone();
    }

    public void addFieldAndValuesInConfigOverride(String fieldName, String fieldValue){
        clickOnSetConfigButtonOnLocation();
        utils.getLocator("ppccLocationPage.selectButton").click();
        String fieldNameXpath = utils.getLocatorValue("ppccLocationPage.fieldValue").replace("{fieldValue}",
                fieldName);
        driver.findElement(By.xpath(fieldNameXpath)).click();
        logger.info("Selecting the Field In Override Field Dropdown");
        TestListeners.extentTest.get().pass("Selecting the Field In Override Field Dropdown");
        utils.getLocator("ppccLocationPage.textInputField").sendKeys(fieldValue);
        utils.waitTillPagePaceDone();
        logger.info("Value of the Field is entered In Override Field Dropdown");
        TestListeners.extentTest.get().pass("Value of the Field is entered In Override Field Dropdown");
        utils.longWaitInSeconds(1);
        clickOnConfirmButtonInConfigOverridePopUp();
        utils.waitTillPagePaceDone();
    }

    public void clickOnConfirmButtonInConfigOverridePopUp(){
        String waitTillItem = utils.getLocatorValue("ppccLocationPage.confirmButtonOverridePopup");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(waitTillItem)));
        utils.getLocator("ppccLocationPage.confirmButtonOverridePopup").click();
        logger.info("Confirm Button in Config Override is clicked.");
        TestListeners.extentTest.get().pass("Confirm Button in Config Override is clicked.");
    }

    public void clickOnClearOverridesInConfigOverridePopUp(){
        clickOnViewConfigButtonOnLocation();
        String waitTillItem = utils.getLocatorValue("ppccLocationPage.clearOverridesButton");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(waitTillItem)));
        utils.getLocator("ppccLocationPage.clearOverridesButton").click();
        logger.info("ClearOverride Button in Config Override is clicked.");
        TestListeners.extentTest.get().pass("ClearOverride Button in Config Override is clicked.");
        utils.getLocator("ppccLocationPage.confirmTextFieldClearOverrideAction").sendKeys("Confirm");
        logger.info("Confirm Text is entered in Clear Override Pop Up");
        String waitTillItem1 = utils.getLocatorValue("ppccLocationPage.confirmButtonClearOverridePopUp");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(waitTillItem1)));
        utils.getLocator("ppccLocationPage.confirmButtonClearOverridePopUp").click();
        logger.info("Confirm is clicked in Clear Override Pop Up");
        TestListeners.extentTest.get().pass("ClearOverride Action is complete.");
        utils.waitTillPagePaceDone();
    }

    public boolean verifyValuesSavedInConfigOverridePopUp(String fieldName, String fieldValue) {
        clickOnViewConfigButtonOnLocation();
        utils.waitTillPagePaceDone();
        String fieldNamePresentInConfigOverride= utils.getLocator("ppccLocationPage.selectButton").getText();
        String fieldValuePresentInConfigOverride=utils.getLocator("ppccLocationPage.textInputField").getAttribute("value");
        logger.info("Field Name and Value are saved in Config Override Pop Up");
        boolean isValuesSavedInConfigOverride=false;
        if (fieldNamePresentInConfigOverride != null && fieldValuePresentInConfigOverride != null &&
                fieldNamePresentInConfigOverride.equals(fieldName) &&
                fieldValuePresentInConfigOverride.equals(fieldValue))
        {
            isValuesSavedInConfigOverride=true;
            logger.info("Field Name and Value are saved in Config Override Pop Up");
            TestListeners.extentTest.get().pass("Field Name and Value are saved in Config Override Pop Up");
        }
        else {
            logger.info("Field Name and Value are not saved in Config Override Pop Up");
            TestListeners.extentTest.get().fail("Field Name and Value are not saved in Config Override Pop Up");
        }
        logger.info("fields and values are correct in Config Override Pop up");
        TestListeners.extentTest.get().pass("fields and values are correct in Config Override Pop up");
        clickOnXIconInConfigOverridePopUp();
        logger.info("x icon is clicked Config Override Pop up");
        TestListeners.extentTest.get().pass("Config Override Pop up is closed");
        return isValuesSavedInConfigOverride;
    }

    public boolean verifyConfigOverrideIsCleared(){
        boolean isSetConfigButtonPresent=false;
        isSetConfigButtonPresent =  utils.getLocator("ppccLocationPage.setConfigLocationList").isDisplayed();
        logger.info("Checking the presence of Set Config after Clear Overrides");
        return isSetConfigButtonPresent;
    }

    public String checkStaticTextOnConfigOverridePopUp()
    {
        String textPresentOnConfigOverridePopUp= utils.getLocator("ppccLocationPage.textOnOverridePopup").getText();
        return textPresentOnConfigOverridePopUp;
    }

    public WebElement getAddButtonOnConfigOverridePopUp()
    {
        WebElement getAddButtonOnConfigOverridePopUp = utils.getLocator("ppccLocationPage.addButtonOverridePopup");
        return getAddButtonOnConfigOverridePopUp;
    }

    public String checkHeaderTextOnConfigOverridePopUp()
    {
        String headertextPresentOnConfigOverridePopUp= utils.getLocator("ppccLocationPage.headerConfigOverridePopup").getText();
        return headertextPresentOnConfigOverridePopUp;
    }

    public String createPolicy() {
        String policyName = "AUT " + new java.sql.Timestamp(System.currentTimeMillis()).getTime();
        logger.info("specified policy name");
        TestListeners.extentTest.get().pass("Policy is created successfully : "+ policyName);
        return policyName;
    }

    public void cancelUpdateAllLocations(){
        clickSelectAllCheckbox();
		utils.getLocator("ppccLocationPage.moreActionsButton").click();
        logger.info("More Actions is clicked");
        TestListeners.extentTest.get().pass("More Actions is clicked");
		utils.getLocator("ppccLocationPage.cancelUpdateButton").click();
		// utils.tryAllClick(driver, utils.getLocator("ppccLocationPage.cancelUpdateButton"));
        logger.info("Cancel Update is clicked");
		utils.longWaitInSeconds(2);
    }

    public void initiateUpdateAllLocations(){
        clickSelectAllCheckbox();
        utils.getLocator("ppccLocationPage.moreActionsButton").click();
        logger.info("More Actions is clicked");
        TestListeners.extentTest.get().pass("More Actions is clicked");
        utils.getLocator("ppccLocationPage.initiateUpdateButton").click();
        logger.info("Initiate Update is clicked");
        utils.waitTillPagePaceDone();
    }

    public String getTitleText()
	{
        String titleText = utils.getLocator("ppccLocationPage.messageBar").getText();
        logger.info("The title text is: " + titleText);
        return titleText;
    }

    public void overrideConfigs(String fieldName, String fieldValue)
    {
        utils.getLocator("ppccLocationPage.moreActions").click();
        utils.getLocator("ppccLocationPage.configOverride").click();
        utils.waitTillPagePaceDone();
        utils.getLocator("ppccLocationPage.selectButton").click();
        String fieldNameXpath = utils.getLocatorValue("ppccLocationPage.fieldValue").replace("{fieldValue}",
                fieldName);
        driver.findElement(By.xpath(fieldNameXpath)).click();
        logger.info("Selecting the Field In Override Field Dropdown");
        TestListeners.extentTest.get().pass("Selecting the Field In Override Field Dropdown");
        utils.getLocator("ppccLocationPage.textInputField").sendKeys(fieldValue);
        utils.waitTillPagePaceDone();
        logger.info("Value of the Field is entered In Override Field Dropdown");
        TestListeners.extentTest.get().pass("Value of the Field is entered In Override Field Dropdown");
        clickOnConfirmButtonInConfigOverridePopUp();
        utils.waitTillPagePaceDone();
        utils.longWaitInMiliSeconds(300);
    }
    public void clickSelectAllCheckbox()
    {
        utils.getLocator("ppccLocationPage.selectallCheckboxProvisionedList").click();
        logger.info("Select All checkbox is clicked to select all locations");
        utils.waitTillPagePaceDone();
    }
    public void selectAllCheckbox() {
        utils.getLocator("ppccLocationPage.selectallCheckBox").click();
        logger.info("Select All checkbox is clicked");
        TestListeners.extentTest.get().pass("Select All checkbox is clicked");
        utils.waitTillPagePaceDone();
    }
    public void clickOnDownloadIcon() {
        utils.getLocator("ppccLocationPage.downloadButton").click();
        logger.info("Download button is clicked");
        TestListeners.extentTest.get().pass("Download button is clicked");
        utils.longWaitInSeconds(5);
    }
    public void selectAllCheckBoxAndClickOnDownloadIcon() {
        selectAllCheckbox();
        utils.waitTillPagePaceDone();
        clickOnDownloadIcon();
        utils.longWaitInSeconds(5);
    }
    public String getFileNameOnMultipleLocationDownloadAction(String businessName) {
        String expectedFileName = businessName.trim().replaceAll("\\s+", "_");
        return expectedFileName;
    }
    public void clickOnFirstRowCheckBox() {
        utils.getLocator("ppccLocationPage.firstRowCheckbox").click();
        logger.info("First Location is selected");
        TestListeners.extentTest.get().pass("First Location is selected");
        utils.waitTillPagePaceDone();
    }
    public String getFileNameOnSingleLocationDownloadAction() {
        String storeNumber = utils.getLocator("ppccLocationPage.storeNumberColumn").getText();
        String locationName = utils.getLocator("ppccLocationPage.storeColumn").getText().replace(" ", "_");
        logger.info("Store Number: " + storeNumber + ", Location Name: " + locationName);
        String expectedFileName = (storeNumber + "_" + locationName);
        expectedFileName = expectedFileName.length() > 17 ? expectedFileName.substring(0, 17) : expectedFileName;
        return expectedFileName;
    }
    public boolean verifyLocationDownloadedFileAndDelete(String expectedFileName,String expectedFileExtension){
        File downloadDir = new File(System.getProperty("user.dir"), "resources/ExportData");
        File[] matchingFiles = downloadDir.listFiles((dir, name) -> name.startsWith(expectedFileName) && name.endsWith(expectedFileExtension));
        logger.info("File is downloaded"+expectedFileName );
        TestListeners.extentTest.get().pass("File is downloaded");
        if (matchingFiles == null || matchingFiles.length == 0) {
            logger.warn("No matching file found for expectedFileName " + expectedFileName);
            return false;
        }
        boolean allDeleted = Arrays.stream(matchingFiles).allMatch(file -> {
            boolean deleted = file.delete();
            if (!deleted) {
                logger.error("Failed to delete file: " + file.getName());
            } else {
                logger.info("Deleted file: " + file.getName());
            }
            return deleted;
        });
        if (allDeleted) {
            logger.info("All matching files deleted successfully.");
        } else {
            logger.warn("Some files could not be deleted.");
        }
        return allDeleted;
    }
}