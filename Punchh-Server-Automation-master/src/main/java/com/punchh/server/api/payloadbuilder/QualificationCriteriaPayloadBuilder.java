package com.punchh.server.api.payloadbuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punchh.server.apimodel.qc.QCAggregatorGroupingAttributes;
import com.punchh.server.apimodel.qc.QCData;
import com.punchh.server.apimodel.qc.QCItemQualifier;
import com.punchh.server.apimodel.qc.QCLineItemFilter;
import com.punchh.server.apimodel.qc.QCReceiptQualifier;

public class QualificationCriteriaPayloadBuilder {
	private final QCData.QCDataBuilder dataBuilder;
	private final List<QCLineItemFilter> lineItemFilters = new ArrayList<>();
	private final List<QCItemQualifier> itemQualifiers = new ArrayList<>();
	private final List<QCReceiptQualifier> receiptQualifiers = new ArrayList<>();

	private boolean qualifyingExpressionsOperatorProvided = false;
	private String qualifyingExpressionsOperatorValue = null;
	private boolean itemFilterExpressionsOperatorProvided = false;
	private String itemFilterExpressionsOperatorValue = null;
	private boolean discountEvaluationStrategyProvided = false;
	private String discountEvaluationStrategyValue = null;

	public QualificationCriteriaPayloadBuilder() {
		this.dataBuilder = QCData.builder();
	}

	// --- Core fields ---
	public QualificationCriteriaPayloadBuilder setName(String name) {
		dataBuilder.name(name);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setExternalId(String externalId) {
		dataBuilder.external_id(externalId);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setAmountCap(Integer amountCap) {
		dataBuilder.amount_cap(amountCap);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setPercentageOfProcessedAmount(Integer percentage) {
		dataBuilder.percentage_of_processed_amount(percentage);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setQCProcessingFunction(String function) {
		dataBuilder.qc_processing_function(function);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setRoundingRule(String rule) {
		dataBuilder.rounding_rule(rule);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setMaxDiscountUnits(Integer units) {
		dataBuilder.max_discount_units(units);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setUnitDiscount(Double discount) {
		dataBuilder.unit_discount(discount);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setMinimumUnitRate(Double rate) {
		dataBuilder.minimum_unit_rate(rate);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setTargetPrice(Double price) {
		dataBuilder.target_price(price);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setEffectiveLocations(List<String> locations) {
		dataBuilder.effective_location(locations);
		return this;
	}
	public QualificationCriteriaPayloadBuilder setEffectiveLocationsID(String locationsID) {
		dataBuilder.effective_locationStr(locationsID);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setStackDiscounting(Boolean value) {
		dataBuilder.stack_discounting(value);
		return this;
	}

	public QualificationCriteriaPayloadBuilder setReuseQualifyingItems(Boolean value) {
		dataBuilder.reuse_qualifying_items(value);
		return this;
	}

	// --- Line Item Filters ---
	public QualificationCriteriaPayloadBuilder addLineItemFilter(String selectorId, String method, Integer quantity) {
		QCLineItemFilter filter = QCLineItemFilter.builder().line_item_selector_id(selectorId).processing_method(method)
				.quantity(quantity).build();
		this.lineItemFilters.add(filter);
		return this;
	}

	// --- Item Qualifiers ---
	public QualificationCriteriaPayloadBuilder addItemQualifier(String expressionType, String selectorId,
			Double netValue, Integer quantity) {
		QCItemQualifier qualifier = QCItemQualifier.builder().expression_type(expressionType)
				.line_item_selector_id(selectorId).net_value(netValue).quantity(quantity).build();
		this.itemQualifiers.add(qualifier);
		return this;
	}

	public QualificationCriteriaPayloadBuilder addItemQualifier(String expressionType, String selectorId,
			Double netValue) {
		return addItemQualifier(expressionType, selectorId, netValue, null);
	}

	// --- Receipt Qualifiers ---
	public QualificationCriteriaPayloadBuilder addReceiptQualifier(String attribute, String operator, String value) {
		QCReceiptQualifier qualifier = QCReceiptQualifier.builder().attribute(attribute).operator(operator).value(value)
				.build();
		this.receiptQualifiers.add(qualifier);
		return this;
	}

	// --- Aggregator Grouping Attributes ---
	public QualificationCriteriaPayloadBuilder setAggregatorGroupingAttributes(boolean itemName, boolean itemId,
			boolean itemMajorGroup, boolean itemFamily, boolean lineItemType) {
		QCAggregatorGroupingAttributes attrs = QCAggregatorGroupingAttributes.builder().item_name(itemName)
				.item_id(itemId).item_major_group(itemMajorGroup).item_family(itemFamily).line_item_type(lineItemType)
				.build();
		dataBuilder.aggregator_grouping_attributes(attrs);
		return this;
	}

	// --- Special Optional Fields ---
	public QualificationCriteriaPayloadBuilder setQualifyingExpressionsOperator(String operator) {
		this.qualifyingExpressionsOperatorProvided = true;
		this.qualifyingExpressionsOperatorValue = operator;
		return this;
	}

	public QualificationCriteriaPayloadBuilder setItemFilterExpressionsOperator(String operator) {
		this.itemFilterExpressionsOperatorProvided = true;
		this.itemFilterExpressionsOperatorValue = operator;
		return this;
	}

	public QualificationCriteriaPayloadBuilder setDiscountEvaluationStrategy(String strategy) {
		this.discountEvaluationStrategyProvided = true;
		this.discountEvaluationStrategyValue = strategy;
		return this;
	}

	// --- Build Final Request ---
	public String build() throws JsonProcessingException {
		dataBuilder.line_item_filters(lineItemFilters);
		dataBuilder.item_qualifiers(itemQualifiers);
		dataBuilder.receipt_qualifiers(receiptQualifiers);

		QCData qcData = dataBuilder.build();
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> qcMap = mapper.convertValue(qcData, Map.class);

		// Handle special keys
		handleSpecialKeys(qcMap);

		Map<String, Object> wrapper = new HashMap<>();
		wrapper.put("data", Arrays.asList(qcMap));
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);
		System.out.println(json);
		return json;
	}

	// --- Build for Inline QCData ---
	public QCData buildQCData() {
		dataBuilder.line_item_filters(lineItemFilters);
		dataBuilder.item_qualifiers(itemQualifiers);
		dataBuilder.receipt_qualifiers(receiptQualifiers);

		// Handle special optional fields
		dataBuilder.qualifying_expressions_operator(
				qualifyingExpressionsOperatorProvided ? qualifyingExpressionsOperatorValue : null);
		dataBuilder.item_filter_expressions_operator(
				itemFilterExpressionsOperatorProvided ? itemFilterExpressionsOperatorValue : null);
		dataBuilder.discount_evaluation_strategy(
				discountEvaluationStrategyProvided ? discountEvaluationStrategyValue : null);

		QCData qcData = dataBuilder.build();

		// Mandatory validations
		if (qcData.getQc_processing_function() == null || qcData.getQc_processing_function().isEmpty()) {
			throw new IllegalArgumentException("'qc_processing_function' is required");
		}
		return qcData;
	}

	public QCData buildObject() {
		return dataBuilder.build();
	}

	// --- Private Helper ---
	private void handleSpecialKeys(Map<String, Object> qcMap) {
		if (qualifyingExpressionsOperatorProvided) {
			qcMap.put("qualifying_expressions_operator", qualifyingExpressionsOperatorValue);
		} else {
			qcMap.remove("qualifying_expressions_operator");
		}
		if (itemFilterExpressionsOperatorProvided) {
			qcMap.put("item_filter_expressions_operator", itemFilterExpressionsOperatorValue);
		} else {
			qcMap.remove("item_filter_expressions_operator");
		}
		if (discountEvaluationStrategyProvided) {
			qcMap.put("discount_evaluation_strategy", discountEvaluationStrategyValue);
		} else {
			qcMap.remove("discount_evaluation_strategy");
		}
	}
}