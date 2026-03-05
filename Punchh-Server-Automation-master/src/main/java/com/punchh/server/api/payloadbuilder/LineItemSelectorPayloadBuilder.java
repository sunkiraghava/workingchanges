package com.punchh.server.api.payloadbuilder;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apimodel.lineitemselector.BaseItemCriteria;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorCondition;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorRequest;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorRule;
import com.punchh.server.apimodel.lineitemselector.ModifierCriteria;

public class LineItemSelectorPayloadBuilder {

	private LineItemSelectorRule.LineItemSelectorRuleBuilder ruleBuilder;
	private final List<LineItemSelectorCondition> baseConditions;
	private final List<LineItemSelectorCondition> modifierConditions;
	private final List<LineItemSelectorRule> rulesList;
	private Integer maxDiscountUnits;
	private String processingMethod;
	private ObjectMapper mapper;

	public LineItemSelectorPayloadBuilder() {
		this.ruleBuilder = LineItemSelectorRule.builder();
		this.baseConditions = new ArrayList<>();
		this.modifierConditions = new ArrayList<>();
		this.rulesList = new ArrayList<>();
		this.mapper = new ObjectMapper();
	}

	/** Start a new LIS rule */
	public LineItemSelectorPayloadBuilder startNewRule() {
		this.ruleBuilder = LineItemSelectorRule.builder();
		this.baseConditions.clear();
		this.modifierConditions.clear();
		this.maxDiscountUnits = null;
		this.processingMethod = null;
		return this;
	}

	// Set LIS name
	public LineItemSelectorPayloadBuilder setName(String name) {
		this.ruleBuilder.name(name);
		return this;
	}

	// Set filter_item_set
	public LineItemSelectorPayloadBuilder setFilterItemSet(LineItemSelectorFilterItemSet filter) {
		this.ruleBuilder.filterItemSet(filter);
		return this;
	}

	// Add base item condition
	public LineItemSelectorPayloadBuilder addBaseItemClause(String attribute, String operator, String value) {
		baseConditions
				.add(LineItemSelectorCondition.builder().attribute(attribute).operator(operator).value(value).build());
		return this;
	}

	// Add modifier condition
	public LineItemSelectorPayloadBuilder addModifierClause(String attribute, String operator, String value) {
		modifierConditions
				.add(LineItemSelectorCondition.builder().attribute(attribute).operator(operator).value(value).build());
		return this;
	}

	// Set max_discount_units for modifiers
	public LineItemSelectorPayloadBuilder setMaxDiscountUnits(int units) {
		this.maxDiscountUnits = units;
		return this;
	}

	// Set processing_method for modifiers
	public LineItemSelectorPayloadBuilder setProcessingMethod(String method) {
		this.processingMethod = method;
		return this;
	}

	// Set exclude_non_payable flag
	public LineItemSelectorPayloadBuilder setExcludeNonPayable(boolean flag) {
		this.ruleBuilder.excludeNonPayable(flag);
		return this;
	}

	/** Add current rule to the payload list */
	public LineItemSelectorPayloadBuilder addCurrentRule() {
		if (ruleBuilder == null)
			throw new IllegalStateException("Call startNewRule() before adding a rule.");

		// populate base items
		if (!baseConditions.isEmpty()) {
			BaseItemCriteria baseItems = BaseItemCriteria.builder().clauses(new ArrayList<>(baseConditions)).build();
			ruleBuilder.baseItems(baseItems);
		}

		// populate modifiers
		if (!modifierConditions.isEmpty() || maxDiscountUnits != null || processingMethod != null) {
			ModifierCriteria modifiers = ModifierCriteria.builder().maxDiscountUnits(maxDiscountUnits)
					.processingMethod(processingMethod)
					.clauses(modifierConditions.isEmpty() ? null : new ArrayList<>(modifierConditions)).build();
			ruleBuilder.modifiers(modifiers);
		}

		LineItemSelectorRule rule = ruleBuilder.build();
		rule.validate();
		rulesList.add(rule);

		// reset for next rule
		startNewRule();
		return this;
	}

	/** Build final payload (supports single or multiple LIS) */
	public String build() throws JsonProcessingException {
		if (ruleBuilder != null && (ruleBuilder.build().getName() != null)) {
			// Add last rule if not explicitly added
			addCurrentRule();
		}

		if (rulesList.isEmpty())
			throw new IllegalArgumentException("At least one LIS rule must be added.");

		LineItemSelectorRequest request = LineItemSelectorRequest.builder().data(new ArrayList<>(rulesList)).build();

		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
		System.out.println("Generated JSON:\n" + json);
		return json;
	}
}
