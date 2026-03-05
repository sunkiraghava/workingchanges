package com.punchh.server.mobilePages;

import io.appium.java_client.ios.IOSDriver;

public class mobilePageObj {

	public IOSDriver mobileDriver;
	ApplePassesPage applePassesPage;

	public mobilePageObj(IOSDriver mobileDriver) {
		this.mobileDriver = mobileDriver;
	}

	public ApplePassesPage applePassesPage() {
		if (applePassesPage == null) {
			applePassesPage = new ApplePassesPage(mobileDriver);
		}
		return applePassesPage;
	}
}
