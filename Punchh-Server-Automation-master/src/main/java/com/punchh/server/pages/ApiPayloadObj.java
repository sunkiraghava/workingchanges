package com.punchh.server.pages;

import com.punchh.server.api.payloadbuilder.LineItemSelectorPayloadBuilder;
import com.punchh.server.api.payloadbuilder.QualificationCriteriaPayloadBuilder;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.api.payloadbuilder.UpdateLocationPayloadBuilder;

public class ApiPayloadObj {

	public ApiPayloadObj() {

	}

	public LineItemSelectorPayloadBuilder lineItemSelectorBuilder() {
		return new LineItemSelectorPayloadBuilder();
	}

	public UpdateLocationPayloadBuilder updateLocationBuilder() {
		return new UpdateLocationPayloadBuilder();
	}

	public QualificationCriteriaPayloadBuilder qualificationCriteriaBuilder() {
		return new QualificationCriteriaPayloadBuilder();
	}

	public RedeemablePayloadBuilder redeemableBuilder() {
		return new RedeemablePayloadBuilder();
	}

}
