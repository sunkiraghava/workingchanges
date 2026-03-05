package com.punchh.server.apimodel.qc;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QCAggregatorGroupingAttributes {
	private Boolean item_name;
	private Boolean item_id;
	private Boolean item_major_group;
	private Boolean item_family;
	private Boolean line_item_type;
}
