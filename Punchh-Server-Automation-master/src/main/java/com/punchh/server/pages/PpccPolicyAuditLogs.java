package com.punchh.server.pages;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class PpccPolicyAuditLogs {

    static Logger logger = LogManager.getLogger(PpccPolicyAuditLogs.class);
    private WebDriver driver;
    Properties prop;
    Utilities utils;

    public PpccPolicyAuditLogs(WebDriver driver)
    {
        this.driver = driver;
        prop = Utilities.loadPropertiesFile("config.properties");
        utils = new Utilities(driver);
    }

    public WebElement getCancelButton()
    {
        WebElement cancelButton = utils.getLocator("ppccPolicyAuditLog.cancelButton");
        return cancelButton;
    }

    public WebElement getGoToPolicyMgmtPageLocator()
    {
        return utils.getLocator("ppccPolicyAuditLog.goBackToPolicyMgmtPage");
    }

    public List<String> getColumnsHeaders()
    {
        String xpath = utils.getLocatorValue("ppccPolicyAuditLog.columnHeader");
        List<WebElement> headers = driver.findElements(By.xpath(xpath));

        List<String> actualHeaders = new ArrayList<>();
        for (WebElement header : headers)
        {
            actualHeaders.add(header.getText().trim());
        }
        return actualHeaders;
    }

    public WebElement getSortIcon()
    {
        WebElement sortIcon = utils.getLocator("ppccPolicyAuditLog.sortIcon");
        return sortIcon;
    }

    public List<String> getMissingSortableHeaders()
    {
        List<WebElement> headers = driver.findElements(By.xpath("ppccPolicyAuditLog.columnHeader"));
        List<String> missingHeaders = new ArrayList<>();
        for (WebElement header : headers)
        {
            String sortIcon = utils.getLocatorValue(getSortIcon().toString());
            List<WebElement> svgElements = header.findElements(By.xpath(sortIcon));
            if (svgElements.isEmpty()) {
                missingHeaders.add(header.getText().trim());
            }
        }
        return missingHeaders;
    }

    public WebElement getSearchFieldOnUI()
    {
        WebElement searchField = utils.getLocator("ppccPolicyAuditLog.searchField");
        return searchField;
    }

    public WebElement getFilterIconOnUI()
    {
        WebElement filterIcon = utils.getLocator("ppccPolicyAuditLog.filterButton");
        return filterIcon;
    }

    public void searchItem(String searchItem)
    {
        utils.getLocator("ppccPolicyAuditLog.searchField").sendKeys(searchItem);
        utils.getLocator("ppccPolicyAuditLog.searchField").sendKeys(Keys.ENTER);
        utils.waitTillPagePaceDone();
    }

    public String getRowColumnText(int rowIndex, int columnIndex)
    {
        String item = utils.getLocatorValue("ppccPolicyAuditLog.rowColumnData").replace("{rowIndex}", String.valueOf(rowIndex)).replace("{columnIndex}", String.valueOf(columnIndex));
        String data = driver.findElement(By.xpath(item)).getText();
        logger.info("Data is: " + data);
        return data;
    }

    public boolean isValuePresentOnUI(String searchItem, String locator)
    {
        utils.waitTillPagePaceDone();
        List<WebElement> rows = driver.findElements(By.xpath(locator));
        boolean isSearchedItemPresent = true;
        if (rows.isEmpty()) { isSearchedItemPresent = false;}
        else
        {
            for (WebElement row : rows)
            {
                String rowText = row.getText();
                logger.info("Row text: " + rowText);
                if (!rowText.contains(searchItem))
                {
                    isSearchedItemPresent = false;
                }
            }
        }
        return isSearchedItemPresent;
    }

    public boolean isDataFiltered(String filterOption, String filterValue, int columnIndex, String expectedValue)
    {
        applyFilter(filterOption, filterValue);
        boolean isDataFiltered = isFilteredResultValid(columnIndex, expectedValue);
        return isDataFiltered;
    }

    private void applyFilter(String filterOption, String filterValue)
    {
        utils.getLocator("ppccPolicyAuditLog.filterButton").click();
        utils.getLocator("ppccPolicyAuditLog.selectFilterOption").click();
        driver.findElement(By.xpath(filterOption)).click();
        utils.getLocator("ppccPolicyAuditLog.selectFilterOption").click();
        driver.findElement(By.xpath(filterValue)).click();
        utils.getLocator("ppccPolicyAuditLog.applyButton").click();
        utils.waitTillPagePaceDone();
        logger.info("Filter applied successfully");
    }

    private boolean isFilteredResultValid(int columnIndex, String expectedValue)
    {
        String items = utils.getLocatorValue("ppccPolicyAuditLog.itemEntry");
        List<WebElement> rows = driver.findElements(By.xpath(items));

        if (rows.isEmpty()) { return false;}
        String columnData = utils.getLocatorValue("ppccPolicyAuditLog.columnData").replace("{columnIndex}", String.valueOf(columnIndex));
        for (WebElement row : rows)
        {
            WebElement column = row.findElement(By.xpath(columnData));
            String value = column.getText();
            logger.info("Value is: " + value);
            if (!value.contains(expectedValue))
            {
                logger.info("Found a row where data is not '" + expectedValue + "': " + value);
                return false;
            }
        }
        return true;
    }

    public void sortData(String columnLocator) {
        utils.waitTillPagePaceDone();
        utils.getLocator(columnLocator).click();
        utils.waitTillPagePaceDone();
    }

    public List<String> getColumnData(String columnIndex)
    {
        List<String> dataList = new ArrayList<>();
        String column = utils.getLocatorValue("ppccPolicyAuditLog.columnData").replace("{columnIndex}", columnIndex);
        List<WebElement> columnElements = driver.findElements(By.xpath(column));
        for (WebElement row : columnElements) {
            String data = row.getText().trim();
            logger.info("Data in column " + columnIndex + ": " + data);
            dataList.add(data);
        }
        return dataList;
    }

    public void changeEntriesToHundred(int numberOfRows)
    {
        utils.getLocator("ppccPolicyAuditLog.defaultNumberOfRows").click();
        String locator = utils.getLocatorValue("ppccPolicyAuditLog.changeToNumberOfRows").replace("{numberOfRows}", String.valueOf(numberOfRows));
        driver.findElement(By.xpath(locator)).click();
        utils.waitTillPagePaceDone();
    }

    public WebElement getCalendarIcon()
    {
        return utils.getLocator("ppccPolicyAuditLog.calendarIcon");
    }

    public WebElement getTodayDate()
    {
        return utils.getLocator("ppccPolicyAuditLog.todayDatePicker");   
    }

    public void setTodayDateInCalendar()
    {
        getCalendarIcon().click();
		getTodayDate().click();
		getTodayDate().click();
        utils.waitTillPagePaceDone();
    }

    public List<LocalDateTime> getEventDateTimeColumnData(String columnIndex, DateTimeFormatter formatter) {
        List<String> rawData = getColumnData(columnIndex);
        List<LocalDateTime> dateList = new ArrayList<>();

        for (String dateText : rawData) {
            LocalDateTime parsedDate = utils.parseDate(dateText, formatter);
            if (parsedDate != null) {
                dateList.add(parsedDate);
            }
        }
        return dateList;
    }

    public List<String> sortList(List<String> list) {
        String prefix = "UT ";
        list.sort((a, b) -> {
            boolean aHasPrefix = a.startsWith(prefix);
            boolean bHasPrefix = b.startsWith(prefix);

            if (aHasPrefix && !bHasPrefix) {
                return -1;
            } else if (!aHasPrefix && bHasPrefix) {
                return 1;
            } else {
                return b.compareToIgnoreCase(a);
            }
        });
        return list;
    }

    public WebElement getCancelButtonInDetailedPage()
    {
        return utils.getLocator("ppccPolicyAuditLog.cancelButtonDetailedPage");
    }

}
