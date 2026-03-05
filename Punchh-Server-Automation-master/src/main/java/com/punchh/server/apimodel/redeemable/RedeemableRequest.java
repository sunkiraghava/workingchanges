package com.punchh.server.apimodel.redeemable;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RedeemableRequest {
	private List<RedeemableData> data;
}
