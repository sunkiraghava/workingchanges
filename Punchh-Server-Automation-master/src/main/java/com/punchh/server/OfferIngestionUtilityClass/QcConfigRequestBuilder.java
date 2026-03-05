package com.punchh.server.OfferIngestionUtilityClass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.util.*;

public class QcConfigRequestBuilder {

    // ---------- Top-Level Fields ----------
    private String name;
    private String externalId = "";
    private String amountCap = "1";
    private String percentageOfProcessedAmount = "10";
    private String qcProcessingFunction = "sum_amounts";
    private String roundingRule = "floor";
    private String effectiveLocation;
    private boolean stackDiscounting = false;
    private boolean reuseQualifyingItems = false;
    private boolean enableMenuItemAggregator = false;

    // ---------- Nested Lists ----------
    private List<LineItemFilter> lineItemFilters = new ArrayList<>();
    private AggregatorGroupingAttributes aggregatorGroupingAttributes;
    private List<ItemQualifier> itemQualifiers = new ArrayList<>();
    private List<ReceiptQualifier> receiptQualifiers = new ArrayList<>();

    // ---------- Chainable Builder Methods ----------
    public QcConfigRequestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public QcConfigRequestBuilder withEffectiveLocation(String effectiveLocation) {
        this.effectiveLocation = effectiveLocation;
        return this;
    }

    public QcConfigRequestBuilder withAmountCap(String amountCap) {
        this.amountCap = amountCap;
        return this;
    }

    public QcConfigRequestBuilder withPercentage(String percentage) {
        this.percentageOfProcessedAmount = percentage;
        return this;
    }

    public QcConfigRequestBuilder withRoundingRule(String roundingRule) {
        this.roundingRule = roundingRule;
        return this;
    }

    public QcConfigRequestBuilder withLineItemFilter(String selectorId, String processingMethod, String quantity) {
        this.lineItemFilters.add(LineItemFilter.builder()
                .lineItemSelectorId(selectorId)
                .processingMethod(processingMethod)
                .quantity(quantity)
                .build());
        return this;
    }

    public QcConfigRequestBuilder withAggregatorAttributes(boolean itemName, boolean itemId,
                                                           boolean itemMajorGroup, boolean itemFamily, boolean lineItemType) {
        this.aggregatorGroupingAttributes = AggregatorGroupingAttributes.builder()
                .itemName(itemName)
                .itemId(itemId)
                .itemMajorGroup(itemMajorGroup)
                .itemFamily(itemFamily)
                .lineItemType(lineItemType)
                .build();
        return this;
    }

    public QcConfigRequestBuilder withItemQualifier(String expressionType, String lineItemSelectorId, Object netValue) {
        this.itemQualifiers.add(ItemQualifier.builder()
                .expressionType(expressionType)
                .lineItemSelectorId(lineItemSelectorId)
                .netValue(netValue)
                .build());
        return this;
    }

    public QcConfigRequestBuilder withReceiptQualifier(String attribute, String operator, Object value) {
        this.receiptQualifiers.add(ReceiptQualifier.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value)
                .build());
        return this;
    }

    // ---------- Build JSON ----------
    public String buildJson() throws JsonProcessingException {
        if (name == null || effectiveLocation == null) {
            throw new IllegalArgumentException("Name and effectiveLocation are mandatory");
        }

        QcConfigData data = QcConfigData.builder()
                .name(name)
                .externalId(externalId)
                .amountCap(amountCap)
                .percentageOfProcessedAmount(percentageOfProcessedAmount)
                .qcProcessingFunction(qcProcessingFunction)
                .roundingRule(roundingRule)
                .effectiveLocation(effectiveLocation)
                .stackDiscounting(stackDiscounting)
                .reuseQualifyingItems(reuseQualifyingItems)
                .lineItemFilters(lineItemFilters)
                .enableMenuItemAggregator(enableMenuItemAggregator)
                .aggregatorGroupingAttributes(aggregatorGroupingAttributes)
                .itemQualifiers(itemQualifiers)
                .receiptQualifiers(receiptQualifiers)
                .build();

        Map<String, Object> root = new HashMap<>();
        root.put("data", Collections.singletonList(data));

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    // ---------- Inner Classes ----------
    @Data
    @Builder
    public static class QcConfigData {
        private String name;
        private String externalId;
        private String amountCap;
        private String percentageOfProcessedAmount;
        private String qcProcessingFunction;
        private String roundingRule;
        private String effectiveLocation;
        private boolean stackDiscounting;
        private boolean reuseQualifyingItems;
        private List<LineItemFilter> lineItemFilters;
        private boolean enableMenuItemAggregator;
        private AggregatorGroupingAttributes aggregatorGroupingAttributes;
        private List<ItemQualifier> itemQualifiers;
        private List<ReceiptQualifier> receiptQualifiers;
    }

    @Data
    @Builder
    public static class LineItemFilter {
        private String lineItemSelectorId;
        private String processingMethod;
        private String quantity;
    }

    @Data
    @Builder
    public static class AggregatorGroupingAttributes {
        private boolean itemName;
        private boolean itemId;
        private boolean itemMajorGroup;
        private boolean itemFamily;
        private boolean lineItemType;
    }

    @Data
    @Builder
    public static class ItemQualifier {
        private String expressionType;
        private String lineItemSelectorId;
        private Object netValue;
    }

    @Data
    @Builder
    public static class ReceiptQualifier {
        private String attribute;
        private String operator;
        private Object value;
    }

    // ---------- Example Usage ----------
    public static void main(String[] args) throws Exception {
        String json = new QcConfigRequestBuilder()
                .withName("AutomationQC_API_" + System.currentTimeMillis())
                .withEffectiveLocation("location:400707")
                .withLineItemFilter("f922175d-8808-427e-a55e-d0c7ef0c0e59", "", "1")
                .withAggregatorAttributes(true, false, false, false, false)
                .withItemQualifier("line_item_exists", "f922175d-8808-427e-a55e-d0c7ef0c0e59", null)
                .withReceiptQualifier("total_amount", ">=", 10)
                .withReceiptQualifier("revenue_code", "in", "Online")
                .withReceiptQualifier("employee_name", "in", "john")
                .buildJson();

        System.out.println(json);
    }
}
