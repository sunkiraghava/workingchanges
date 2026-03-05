package com.punchh.server.LisCreationUtilityClasses;

//with modifires
public class DataItem {
	public String name;
	public String external_id;
	public String filter_item_set;
	public boolean exclude_non_payable;
	public BaseItems base_items;
	public ModifireItems modifiers;

	public DataItem(String name, String external_id, String filter_item_set, boolean exclude_non_payable,
			BaseItems base_items, ModifireItems modifiers) {
		this.name = name;
		this.external_id = external_id;
		this.filter_item_set = filter_item_set;
		this.exclude_non_payable = exclude_non_payable;
		this.base_items = base_items;
		this.modifiers = modifiers;
	}

}