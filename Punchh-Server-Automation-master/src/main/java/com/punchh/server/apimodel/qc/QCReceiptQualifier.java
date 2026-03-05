package com.punchh.server.apimodel.qc;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QCReceiptQualifier {
	private String attribute;
	private String operator;
	private String value;
}
