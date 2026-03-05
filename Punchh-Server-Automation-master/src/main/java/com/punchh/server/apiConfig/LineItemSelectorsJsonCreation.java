package com.punchh.server.apiConfig;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.BaseItems;
import com.punchh.server.LisCreationUtilityClasses.DataContainer;
import com.punchh.server.LisCreationUtilityClasses.DataItem;
import com.punchh.server.LisCreationUtilityClasses.DataItem2;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifireItems;

//Author : Shashank	
public class LineItemSelectorsJsonCreation {

	public static String createLisJson(String lisName, String external_id, String filter_item_set,
			List<BaseItemClauses> baseItemClausesList, List<ModifiersItemsClauses> modifireItemClausesList,
			int modifire_max_discount_units, String modifire_processing_method) {
		String lisPayload = "";
		DataItem dataItem = null;
		DataItem2 dataItem2 = null;
		List dataList = new ArrayList();
		BaseItems baseItems = new BaseItems(baseItemClausesList);

		if (!modifireItemClausesList.isEmpty() && (!filter_item_set.equalsIgnoreCase("base_only"))) {
			// Create base items
			ModifireItems modifireItemsObj = new ModifireItems(modifireItemClausesList, modifire_max_discount_units,
					modifire_processing_method);
			dataItem = new DataItem(lisName, external_id, filter_item_set, true, baseItems, modifireItemsObj);
			dataList.add(dataItem);
		} else if (!modifireItemClausesList.isEmpty() && filter_item_set.equalsIgnoreCase("base_only")) {
			dataItem2 = new DataItem2(lisName, external_id, filter_item_set, true, baseItems);
			dataList.add(dataItem2);
		} else if (modifireItemClausesList.isEmpty()) {
			dataItem2 = new DataItem2(lisName, external_id, filter_item_set, true, baseItems);
			dataList.add(dataItem2);
		}

		// Create the data container
		DataContainer dataContainer = new DataContainer(dataList);

		// Convert to JSON
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataContainer);
			lisPayload = jsonString.toString();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return lisPayload;
	}

	public List<BaseItemClauses> getListOfBaseItemClauses(String clausesItemStr) {
		String baseItemClStr = "";
		List<BaseItemClauses> listOfObjects = new LinkedList<BaseItemClauses>();
		String[] baseItemClausesArray1 = clausesItemStr.split("/");
		for (int i = 0; i < baseItemClausesArray1.length; i++) {
			String[] baseItemClausesArray2 = baseItemClausesArray1[i].split(",");
			String valArray = baseItemClausesArray2[2];
			String val = "";
				if (valArray.contains("$")) {
					val = valArray.replace("$", ",");
				} else {
					val = valArray;
				}

			listOfObjects.add(
					new BaseItemClauses(baseItemClausesArray2[0], baseItemClausesArray2[1], val));
		}

		return listOfObjects;

	}

	public List<ModifiersItemsClauses> getListOfModifiersItemClauses(String clausesItemStr) {
		List<ModifiersItemsClauses> listOfObjects = new LinkedList<ModifiersItemsClauses>();
		String[] modifiersItemClausesArray1 = clausesItemStr.split("/");
		for (int i = 0; i < modifiersItemClausesArray1.length; i++) {
			String[] baseItemClausesArray2 = modifiersItemClausesArray1[i].split(",");
			String valArray = baseItemClausesArray2[2];
			String val = "";
			if (valArray.contains("$")) {
				val = valArray.replace("$", ",");
			} else {
				val = valArray;
			}
			listOfObjects.add(new ModifiersItemsClauses(baseItemClausesArray2[0], baseItemClausesArray2[1],
					val));
		}

		return listOfObjects;

	}

	public static String createLisJsonNew(String lisName, String external_id, String filter_item_set,
			List<BaseItemClauses> baseItemClausesList, List<ModifiersItemsClauses> modifireItemClausesList,
			int modifire_max_discount_units, String modifire_processing_method) {
		String lisPayload = "";
		DataItem dataItem = null;
		DataItem2 dataItem2 = null;
		List dataList = new ArrayList();
		BaseItems baseItems = new BaseItems(baseItemClausesList);

		JSONObject item = new JSONObject();
		item.put("name", lisName);
		item.put("external_id", external_id);
		item.put("filter_item_set", filter_item_set);
		item.put("exclude_non_payable", true);
		item.put("base_items", baseItems);

		System.out.println("item.toString()-- " + item.toString());
		return item.toString();
	}

}// end of class
