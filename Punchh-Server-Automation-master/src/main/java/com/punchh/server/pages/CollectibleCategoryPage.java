package com.punchh.server.pages;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;

import com.punchh.server.utilities.SeleniumUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class CollectibleCategoryPage {

	static Logger logger = LogManager.getLogger(CollectibleCategoryPage.class);
	private WebDriver driver;
	Properties prop;
	Utilities utils;
	SeleniumUtilities selUtils;
	private PageObj pageObj;

	public CollectibleCategoryPage(WebDriver driver) {
		this.driver = driver;
		prop = Utilities.loadPropertiesFile("config.properties");
		utils = new Utilities(driver);
		selUtils = new SeleniumUtilities(driver);
		pageObj = new PageObj(driver);
	}

	
   public void navigateToCollectibleCategories() {
       pageObj.menupage().navigateToSubMenuItem("Settings", "Collectible Categories");
       utils.logit("Navigated to Settings > Collectible Categories");
   }


   public void clickAddCategoryButton() {
       WebElement addButton = utils.getLocator("collectibleCategory.addButton");
       utils.waitTillElementToBeClickable(addButton);
       addButton.click();
       utils.logit("Clicked on Add Category button");
   }

   public void enterCategoryName(String name) {
       WebElement nameInput = utils.getLocator("collectibleCategory.nameInput");
       utils.waitTillElementToBeClickable(nameInput);
       nameInput.clear();
       nameInput.sendKeys(name);
       utils.logit("Entered Category Name: " + name);
   }

   public void enterCategoryDescription(String description) {
       WebElement descInput = utils.getLocator("collectibleCategory.descriptionInput");
       utils.waitTillElementToBeClickable(descInput);
       descInput.clear();
       descInput.sendKeys(description);
       utils.logit("Entered Category Description: " + description);
   }
   public void clickSaveCategoryButton() {
	   WebElement saveButton = utils.getLocator("collectibleCategory.saveButton");
	   utils.waitTillElementToBeClickable(saveButton);
	   saveButton.click();
	   utils.logit("Clicked on Save Category button");
   }
   
   public List<String> getCategoryNames() {
       return getColumnValues("Name");
   }

   public String getCategoryName(int rowIndex) {
	    return getCellValue("Name", rowIndex);
	}

   // shared reusable methods
   private List<String> getColumnValues(String headerName) {
       String xpath = String.format(
               utils.getLocatorValue("collectibleCategory.tableColumnCellsByHeader"),
               headerName
       );

       return driver.findElements(By.xpath(xpath))
               .stream()
               .map(e -> e.getText().trim())
               .collect(Collectors.toList());
   }

   private String getCellValue(String headerName, int rowIndex) {
       String xpath = String.format(
               utils.getLocatorValue("collectibleCategory.tableCellByHeaderAndRow"),
               rowIndex,
               headerName
       );

       return driver.findElement(By.xpath(xpath)).getText().trim();
   }
   
   public void clickAddCollectibleButton() {
       WebElement addButton = utils.getLocator("collectibleCategory.addNewCollectibleBtn");
       utils.waitTillElementToBeClickable(addButton);
       addButton.click();
       utils.logit("Clicked on Add New Collectible button");
   }
   public void uploadCollectibleImage(String path) {
		WebElement el = utils.getLocator("collectibleCategory.collectibleImageUpload");
		el.sendKeys(System.getProperty("user.dir") + path);
		utils.logInfo("uploaded the collectible image");
	}

	public String createCollectible(String name, String description, String category, String shareMSg, String date, String status) {
		utils.waitTillPagePaceDone();
		utils.getLocator("collectibleCategory.collectibleNameInput").clear();
		utils.getLocator("collectibleCategory.collectibleNameInput").sendKeys(name);
		utils.getLocator("collectibleCategory.CollectibleDescriptionInput").clear();
		utils.getLocator("collectibleCategory.CollectibleDescriptionInput").sendKeys(description);
		utils.getLocator("collectibleCategory.categoryDropdown").click();
		List<WebElement> ele = utils.getLocatorList("collectibleCategory.categoryDrpDwnList");
		utils.selectListDrpDwnValue(ele, category);
		utils.getLocator("collectibleCategory.shareMsgInput").clear();
		utils.getLocator("collectibleCategory.shareMsgInput").sendKeys(shareMSg);
		utils.getLocator("collectibleCategory.disappearDate").sendKeys(date);
		utils.getLocator("campaignsBetaPage.applyBtn").click();
		WebElement statusDropdown = utils.getLocator("collectibleCategory.statusDropdown");
		String defaultValue = statusDropdown.getText();
		statusDropdown.click();
		List<WebElement> ele1 = utils.getLocatorList("collectibleCategory.statusDrpDwnList");
		utils.selectListDrpDwnValue(ele1, status);
		uploadCollectibleImage("/resources/images.png");
		utils.getLocator("collectibleCategory.createBtn").click();
		utils.logit("Collectible created/updated with name: " + name);
		return defaultValue;
	}
	public String validateCollectibleCategoryErrorsMessage() {
		String msg = utils.getLocator("collectibleCategory.errorMsg").getText();
		return msg;
	}

	public void clickEditBtn(String collectibleName) {
		String xpath = String.format(utils.getLocatorValue("collectibleCategory.collectibleEditBtn").replace("$temp", collectibleName));
		WebElement editBtn = driver.findElement(By.xpath(xpath));
		utils.waitTillElementToBeClickable(editBtn);
		editBtn.click();
		utils.logit("Clicked on Edit button for collectible: " + collectibleName);
	}
	public void setDisappearDate(String date) {
		utils.getLocator("collectibleCategory.disappearDate").clear();
		utils.getLocator("collectibleCategory.disappearDate").sendKeys(date);
		utils.logInfo("Disappear Date set as: " + date);
		utils.getLocator("signupCampaignsPage.cancelbtn").click();
	}
	public String getInputFieldError(String field) {
		String xpath = utils.getLocatorValue("collectibleCategory.fieldErrorMsg").replace("$field", field);
		WebElement ele = driver.findElement(By.xpath(xpath));
		String msg = ele.getText();
		utils.logit("Disappear date error message: " + msg);
		return msg;
	}
	public void switchActiveInactiveTab(String tab) {
		String xpath = utils.getLocatorValue("collectibleCategory.collectibleCategoryActiveInactiveTab").replace("temp", tab);
		driver.findElement(By.xpath(xpath)).click();
		utils.logInfo("Switched to " + tab + " tab");
	}

	public boolean selectCategory(String category) {
		utils.getLocator("collectibleCategory.categoryDropdown").click();
		List<WebElement> ele = utils.getLocatorList("collectibleCategory.categoryDrpDwnList");
		boolean isSelected = utils.selectListDrpDwnValue(ele, category);
		if (isSelected) {
	        utils.logit("Selected category from dropdown is: " + category);
	    } else {
	        utils.logit("Dropdown value not found: " + category);
	    }

	    return isSelected;
	}

	public void expandCollectible(String collectibleName) {
		String xpath = utils.getLocatorValue("collectibleCategory.expandCollectible").replace("$temp", collectibleName);
		WebElement expandBtn = driver.findElement(By.xpath(xpath));
		utils.waitTillElementToBeClickable(expandBtn);
		expandBtn.click();
		utils.logit("Expanded category: " + collectibleName);
	}
}
