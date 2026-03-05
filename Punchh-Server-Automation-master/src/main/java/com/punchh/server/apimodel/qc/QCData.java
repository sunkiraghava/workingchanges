package com.punchh.server.apimodel.qc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class QCData {
	private String name;
	private String external_id;
	private Integer amount_cap;
	private Integer percentage_of_processed_amount;
	private String qc_processing_function;
	private String rounding_rule;
	private Integer max_discount_units;
	private Double unit_discount;
	private Double minimum_unit_rate;
	private Double target_price;
	private List<String> effective_location;
	private String effective_locationStr;
	private Boolean stack_discounting;
	private Boolean reuse_qualifying_items;
	private List<QCLineItemFilter> line_item_filters;
	private Boolean enable_menu_item_aggregator;
	private QCAggregatorGroupingAttributes aggregator_grouping_attributes;
	private List<QCItemQualifier> item_qualifiers;
	private List<QCReceiptQualifier> receipt_qualifiers;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String qualifying_expressions_operator;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String item_filter_expressions_operator;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String discount_evaluation_strategy;

}
