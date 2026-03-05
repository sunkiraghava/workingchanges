package com.punchh.server.OfferModularizationUtilityClass;

import java.util.*;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderPayloadBuilder {

    private final Map<String, Object> orderMap = new HashMap<>();
    private final List<Map<String, Object>> menuItems = new ArrayList<>();
    private final Map<String, Consumer<Boolean>> booleanFieldSetters = new HashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    // Explicit control flag
    private boolean includeMenuItems = true;

    // ------------------------------------------------------
    //               START NEW ORDER
    // ------------------------------------------------------
    public OrderPayloadBuilder startNewOrder() {
        orderMap.clear();
        menuItems.clear();
        booleanFieldSetters.clear();
        includeMenuItems = true;

        // Register boolean fields here
        booleanFieldSetters.put("is_taxable", val -> orderMap.put("is_taxable", val));
        booleanFieldSetters.put("round_off_bill", val -> orderMap.put("round_off_bill", val));

        return this;
    }

    // ------------------------------------------------------
    //               SIMPLE STRING/NUMBER SETTERS
    // ------------------------------------------------------

    public OrderPayloadBuilder setAuthToken(String authenticationToken) {
        orderMap.put("authentication_token", authenticationToken);
        return this;
    }

    public OrderPayloadBuilder setDiscountType(String type) {
        orderMap.put("discount_type", type);
        return this;
    }

    public OrderPayloadBuilder setRedemptionCode(String code) {
        orderMap.put("redemption_code", code);
        return this;
    }

    public OrderPayloadBuilder setReceiptAmount(double amount) {
        orderMap.put("receipt_amount", amount);
        return this;
    }

    public OrderPayloadBuilder setSubtotalAmount(double amount) {
        orderMap.put("subtotal_amount", amount);
        return this;
    }

    public OrderPayloadBuilder setReceiptDatetime(String dateTime) {
        orderMap.put("receipt_datetime", dateTime);
        return this;
    }

    public OrderPayloadBuilder setTransactionNo(long txnNo) {
        orderMap.put("transaction_no", txnNo);
        return this;
    }
    public OrderPayloadBuilder setTransactionNo(String txnNo) {
        orderMap.put("transaction_no", txnNo);
        return this;
    }

    public OrderPayloadBuilder setClient(String client) {
        orderMap.put("client", client);
        return this;
    }
    
    public OrderPayloadBuilder setStoreNumber(String storeNumber) {
        orderMap.put("store_number", storeNumber);
        return this;
    }

    // ------------------------------------------------------
    //               BOOLEAN FIELD HANDLING
    // ------------------------------------------------------

    public OrderPayloadBuilder setBooleanField(String fieldName, Boolean value) {
        if (value != null && booleanFieldSetters.containsKey(fieldName)) {
            booleanFieldSetters.get(fieldName).accept(value);
        }
        return this;
    }

    // ------------------------------------------------------
    //               MENU ITEM CONTROL
    // ------------------------------------------------------

    public OrderPayloadBuilder excludeMenuItems() {
        this.includeMenuItems = false;
        return this;
    }

    public OrderPayloadBuilder includeMenuItems() {
        this.includeMenuItems = true;
        return this;
    }

    // ------------------------------------------------------
    //               SAFE MENU ITEM ADDITION
    // ------------------------------------------------------

    public OrderPayloadBuilder addMenuItem(
            String itemName,
            int qty,
            double amount,
            String type,
            int id,
            String family,
            String majorGroup,
            String serial
    ) {

        Map<String, Object> item = new HashMap<>();
        item.put("item_name", itemName);
        item.put("item_qty", qty);
        item.put("item_amount", amount);
        item.put("menu_item_type", type);
        item.put("menu_item_id", id);
        item.put("menu_family", family);
        item.put("menu_major_group", majorGroup);
        item.put("serial_number", serial);

        menuItems.add(item);
        return this;
    }

    // Defensive copy for external map
    public OrderPayloadBuilder addMenuItem(Map<String, Object> itemMap) {
        if (itemMap != null) {
            menuItems.add(new HashMap<>(itemMap));
        }
        return this;
    }

    // ------------------------------------------------------
    //               FINAL JSON BUILD
    // ------------------------------------------------------

    public String build() {

        if (includeMenuItems && !menuItems.isEmpty()) {
            orderMap.put("menu_items", new ArrayList<>(menuItems));
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(orderMap);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build Order JSON", e);
        }
    }

    // ------------------------------------------------------
    //               TEST / SAMPLE USAGE
    // ------------------------------------------------------

    // public static void main(String[] args) {

    //     // Case 1: WITH menu_items
    //     String jsonWithMenuItems = new OrderPayloadBuilder()
    //             .startNewOrder()
    //             .setDiscountType("redemption_code")
    //             .setRedemptionCode("JNE7N48")
    //             .setReceiptAmount(10)
    //             .addMenuItem("White rice", 1, 2.86, "M", 290, "800", "152", "1.0")
    //             .setSubtotalAmount(1)
    //             .setReceiptDatetime("2015-04-03T18:05:01+05:30")
    //             .setTransactionNo(123213413)
    //             .setClient("NkWCTycw9yoQCzS_QF83pCbgnPTW6pjvd0EsW2J17oI")
    //             .setBooleanField("is_taxable", true)
    //             .setBooleanField("round_off_bill", false)
    //             .build();

    //     System.out.println("WITH MENU ITEMS:\n" + jsonWithMenuItems);

    //     // Case 2: WITHOUT menu_items
    //     String jsonWithoutMenuItems = new OrderPayloadBuilder()
    //             .startNewOrder()
    //             .setClient("NkWCTycw9yoQCzS_QF83pCbgnPTW6pjvd0EsW2J17oI")
    //             .excludeMenuItems()
    //             .setRedemptionCode("JNE7N48")
    //             .setTransactionNo(23542342)
    //             .build();

    //     System.out.println("\nWITHOUT MENU ITEMS:\n" + jsonWithoutMenuItems);
    // }
}
