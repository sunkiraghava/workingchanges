package com.punchh.server.LisCreationUtilityClasses;

public class DataItem2 {
	public String name;
	public String external_id;
	public String filter_item_set;
	public boolean exclude_non_payable;
	public BaseItems base_items;

	public DataItem2(String name, String external_id, String filter_item_set, boolean exclude_non_payable,
			BaseItems base_items) {
		this.name = name;
		this.external_id = external_id;
		this.filter_item_set = filter_item_set;
		this.exclude_non_payable = exclude_non_payable;
		this.base_items = base_items;
	}
}
