package com.punchh.server.mobilePages;

import com.punchh.server.utilities.AndroidUtilities;
import com.punchh.server.utilities.TestListeners;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.*;

import static com.punchh.server.utilities.SingletonDBUtils.utils;

public class AndroidAppPages {
    private AndroidDriver driver;
    private AndroidUtilities androidUtils;
    static Logger logger = LogManager.getLogger(AndroidAppPages.class);

    public AndroidAppPages(AndroidDriver driver){
        this.driver = driver;
        androidUtils = new AndroidUtilities(driver);
    }



    //used to setup the app options
    public UiAutomator2Options getUiAutomator2OptionsSettings(String deviceName){
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setPlatformVersion("15.0");
        options.setDeviceName("AndroidTest_Pixel9");
        options.setAutomationName("UiAutomator2");
        options.setApp("/Users/punchh_shashank/eclipse_navupgrade_workspace/app-masterapp-release.apk");
        return new UiAutomator2Options();
    }


    public void switchToPreProdEnvironment(String userName, String password) throws InterruptedException {
        Thread.sleep(5000);
        androidUtils.getLocator("mobileAndroidAppPage.letsGoStartedXpath").click();
        logger.info("Clicked on let's get started button ");
        TestListeners.extentTest.get().info("Clicked on let's get started button ");

        Thread.sleep(2000);
        androidUtils.getLocator("mobileAndroidAppPage.skipLinkXpath").click();
        logger.info("Clicked on Skip icon from top");
        TestListeners.extentTest.get().info("Clicked on Skip icon from top");

        Thread.sleep(2000);
        androidUtils.getLocator("mobileAndroidAppPage.skippedButtonXpath").click();
        logger.info("Clicked on Skip button");
        TestListeners.extentTest.get().info("Clicked on Skip button");

        Thread.sleep(2000);
        androidUtils.getLocator("mobileAndroidAppPage.termsAndConditionCheckBoxXpath").click();
        logger.info("Clicked on Terms and Condition checkbox ");
        TestListeners.extentTest.get().info("Clicked on Terms and Condition checkbox ");

        androidUtils.getLocator("mobileAndroidAppPage.loginInButtonXpath").click();
        logger.info("Clicked on Log In button ");
        TestListeners.extentTest.get().info("Clicked on Log In button ");
        Thread.sleep(2000);

        androidUtils.getLocator("mobileAndroidAppPage.allowButtonAlert").click();
        logger.info("Clicked on Allow button in alert window");
        TestListeners.extentTest.get().info("Clicked on Allow button in alert window");

        Thread.sleep(2000);
        androidUtils.getLocator("mobileAndroidAppPage.loginPageUserNameInputBoxXpath").sendKeys(userName);
        logger.info(userName + " username is entered ");
        TestListeners.extentTest.get().info(userName + " username is entered ");

        androidUtils.getLocator("mobileAndroidAppPage.loginPagePasswordInputBoxXpath").sendKeys(password);
        logger.info("Password is entered ");
        TestListeners.extentTest.get().info("Password is entered ");

        androidUtils.getLocator("mobileAndroidAppPage.loginInButtonXpath").click();
        logger.info("Clicked om Login user name button");
        TestListeners.extentTest.get().info("Clicked om Login user name button");

        Thread.sleep(6000);
        androidUtils.getLocator("mobileAndroidAppPage.moreIconXpath").click();
        logger.info("Clicked on More Icon");
        TestListeners.extentTest.get().info("Clicked on More Icon");

        Thread.sleep(2000);
        scrollDown(driver);
        Thread.sleep(1000);

        androidUtils.getLocator("mobileAndroidAppPage.switchEnvLinkXpath").click();
        logger.info("Clicked on Switch environment link");
        TestListeners.extentTest.get().info("Clicked on Switch environment link");

        Thread.sleep(3000);
        // select pre prod env
        androidUtils.getLocator("mobileAndroidAppPage.preProdRadioButtonXpath")
                .click();
        logger.info("Pre prod is selected");
        TestListeners.extentTest.get().info("Pre prod is selected");

        Thread.sleep(1000);
        androidUtils.getLocator("mobileAndroidAppPage.continueAlertButton").click();
        logger.info("Clicked on Continue button");
        TestListeners.extentTest.get().info("Clicked on Continue button");

        Thread.sleep(7000);
        androidUtils.getLocator("mobileAndroidAppPage.confirmationOkAlertButtonXpath").click();
        logger.info("Clicked on confirmation Ok button");
        TestListeners.extentTest.get().info("Clicked on confirmation Ok button");

    }


    public void signupAndroidUser(String emailID, String fullName , String pwd ,  boolean isBirthdayRequired , String phone1)
            throws InterruptedException {

        // click on privacy checkbox
        androidUtils.getLocator("mobileAndroidAppPage.termsAndConditionCheckBoxXpath").click();
        // click on signup button
        androidUtils.getLocator("mobileAndroidAppPage.signupButtonXpath").click();
        logger.info("Clicked on Sign up button");
        TestListeners.extentTest.get().info("Clicked on Sign up button");

        Thread.sleep(5000);
        androidUtils.waitTillElementToBeClickable(androidUtils.getLocator("mobileAndroidAppPage.signupButtonXpath"));
        androidUtils.getLocator("mobileAndroidAppPage.fullNameInputBoxXpath").sendKeys(fullName);
        System.out.println(fullName + " full name entered");
        androidUtils.getLocator("mobileAndroidAppPage.userSignUpEmailInputBoxXpath")
                .sendKeys(emailID);
        System.out.println(emailID + " email is entered");
        androidUtils.getLocator("mobileAndroidAppPage.userSignUpPasswordInputBoxXpath")
                .sendKeys(pwd);
        System.out.println("Password is entered");
        androidUtils.getLocator("mobileAndroidAppPage.userSignUpPhoneInputBoxXpath").sendKeys(phone1);
        System.out.println(phone1 + " phone entered");

        if (isBirthdayRequired) {
            String birthdayDate = androidUtils.getCurrentDate("MMM dd ") + "2000";
          // utils.getLocator("mobileAndroidAppPage.birthdayInputBoxXpath").sendKeys(birthdayDate);
          driver.findElement(By.xpath("//android.widget.EditText[@resource-id=\"birthday\"]")).sendKeys(birthdayDate);
            Thread.sleep(1000);
            driver.findElement(By.xpath("//android.view.View[@text=\"Confirm\"]")).click();
        //   utils.getLocator("mobileAndroidAppPage.birthdayConfirmationButtonXpath").click();
            Thread.sleep(1000);
        }

        scrollDown(driver);
        Thread.sleep(1000);
        androidUtils.getLocator("mobileAndroidAppPage.signupButtonXpath").click();
        System.out.println("Clicked on signup button ");
        Thread.sleep(4000);
        androidUtils.waitTillElementToBeClickable( androidUtils.getLocator("mobileAndroidAppPage.userDashboardLetsDoItButtonXpath"));
        androidUtils.getLocator("mobileAndroidAppPage.userDashboardLetsDoItButtonXpath").click();
        System.out.println("Clicked on lets do it button");
        Thread.sleep(2000);
        androidUtils.getLocator("mobileAndroidAppPage.userDashboardPermissionAllowAlertButton").click();
        Thread.sleep(3000);
        androidUtils.getLocator("mobileAndroidAppPage.userDashboardSelectFirstLocationRadioButtonXpath").click();
        Thread.sleep(2000);
        androidUtils.getLocator("mobileAndroidAppPage.userDashboardLocationDoneButtonXpath").click();
        System.out.println("First location is selected ");
        Thread.sleep(3000);
    }


    public Set<String> verifiedPushNotification(Set<String> expPN_NameList)
            throws InterruptedException {
        Set<String> list = new LinkedHashSet<String>();
        boolean flag = false;
        int counter = 0;
        do {
            driver.openNotifications();
            Thread.sleep(5000);
            List<WebElement> notifications = driver.findElements(By.xpath("//android.widget.TextView"));
            List<String> actualPN_List = new ArrayList<String>();
            for (WebElement notificationWele : notifications) {
                String notificationText = notificationWele.getText();
                actualPN_List.add(notificationWele.getText());
                logger.info(notificationWele.getText());
                TestListeners.extentTest.get().pass(notificationWele.getText());
            }
            for (String str : expPN_NameList) {
                if (actualPN_List.contains(str)) {
                    if (list.contains(str)) {
                        list.remove(str);
                    }
                    flag = true;
                    logger.info(str + " push notification is FOUND - ");
                    TestListeners.extentTest.get().pass(str + " push notification is FOUND - ");

                } else {
                    list.add(str);

                }

            }
            driver.navigate().back();
            counter++;
            Thread.sleep(1000);
        } while (!flag && counter <= 3);

        return list;
    }


    //verifying rich messages from inbox
    public Set<String> getAllRichMessagesFromInboxForPN() throws InterruptedException {
        boolean flag = false;
        int counter = 0;
        Set<String> listOfRichMessages = new LinkedHashSet<String>();
        androidUtils.longWaitInSeconds(3);
        driver.findElement(By.xpath("//android.widget.TextView[@text=\"Inbox\"]")).click();
        //   androidUtils.getLocator("mobileAndroidAppPage.userInboxIconXpath").click();
        androidUtils.longWaitInSeconds(3);

        do {
            List<WebElement> richMessagesWElementList = utils.getLocatorList("mobileAndroidAppPage.allRichMessagesXpath");
                //driver.findElements(By.xpath("//android.view.ViewGroup[@resource-id=\"rchmsgHeroImage\"]/following-sibling::android.view.ViewGroup/android.widget.TextView[1]"));
            //utils.getLocatorList("mobileAndroidAppPage.allRichMessagesXpath");
            for (WebElement wEle : richMessagesWElementList) {
                String richMessage = wEle.getText();
                listOfRichMessages.add(richMessage);
            }
            scrollDown(driver);
            androidUtils.longWaitInSeconds(2);
            counter++;
        } while (counter <= 10);

        for (String str : listOfRichMessages) {
            TestListeners.extentTest.get().info("Rich message is -- " + str);
        }
        return listOfRichMessages;
    }


    public  void scrollDown(AndroidDriver driver) {
        // Get screen size
        int screenHeight = driver.manage().window().getSize().getHeight();
        int screenWidth = driver.manage().window().getSize().getWidth();

        // Define start and end points for the scroll
        int startX = screenWidth / 2;
        int startY = (int) (screenHeight * 0.7); // Start near the bottom
        int endY = (int) (screenHeight * 0.3); // End near the top

        // Define the PointerInput for touch actions
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence scrollAction = new Sequence(finger, 1);

        // Move to the start point
        scrollAction.addAction(
                finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        scrollAction.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        // Move to the end point with some duration for a smooth scroll
        scrollAction.addAction(finger.createPointerMove(Duration.ofMillis(1000),
                PointerInput.Origin.viewport(), startX, endY));
        scrollAction.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        // Perform the action
        driver.perform(Collections.singletonList(scrollAction));
    }


}//End of class
