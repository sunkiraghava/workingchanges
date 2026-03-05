package com.punchh.server.LisCreationUtilityClasses;

public class BaseItemClauses {
	public String attribute;
	public String operator;
	public String value;

	public BaseItemClauses(String attribute, String operator, String value) {
		this.attribute = attribute;
		this.operator = operator;
		this.value = value;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub

		String str = "{\"attribute\":\"" + this.attribute + "\",\"value\":\"" + this.value + "\",\"operator\":\""
				+ this.operator + "\"}";

		return str.toString();
	}
}
