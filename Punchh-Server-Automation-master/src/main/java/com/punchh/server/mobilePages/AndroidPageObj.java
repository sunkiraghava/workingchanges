package com.punchh.server.mobilePages;

import com.punchh.server.pages.IframeLoginSignUpPage;
import com.punchh.server.utilities.Utilities;
import io.appium.java_client.android.AndroidDriver;

public class AndroidPageObj {

    public AndroidDriver driver;
    private Utilities utils;
	private AndroidAppPages androidAppPages;

    public AndroidPageObj(AndroidDriver driver) {
        this.driver = driver;
    }

    // For Ui classes
    public AndroidAppPages androidAppPages() {
        if (androidAppPages == null) {
            androidAppPages = new AndroidAppPages(this.driver);
        }
        return androidAppPages;
    }


}//End of class