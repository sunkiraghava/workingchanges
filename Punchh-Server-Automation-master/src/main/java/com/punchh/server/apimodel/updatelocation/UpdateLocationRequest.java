package com.punchh.server.apimodel.updatelocation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateLocationRequest {

	@JsonProperty("location_id")
	private String locationId;

	@JsonProperty("store_number")
	private String storeNumber;

	private UpdateLocationDetails location;
}