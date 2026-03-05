package com.punchh.server.LisCreationUtilityClasses;

import java.util.List;

public class ModifireItems {
	public int max_discount_units;
	public String processing_method;
	public List<ModifiersItemsClauses> clauses;

	public ModifireItems(List<ModifiersItemsClauses> modifiersClause, int max_discount_units,
			String processing_method) {
		this.max_discount_units = max_discount_units;
		this.processing_method = processing_method;
		this.clauses = modifiersClause;

	}
}