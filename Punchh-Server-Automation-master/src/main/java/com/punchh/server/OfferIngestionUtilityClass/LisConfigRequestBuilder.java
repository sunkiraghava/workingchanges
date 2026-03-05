package com.punchh.server.OfferIngestionUtilityClass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

import java.util.*;

public class LisConfigRequestBuilder {

    // ---------- Top-Level Fields ----------
    private String name;
    private String externalId = "";
    private String filterItemSet = "base_and_modifiers";
    private boolean excludeNonPayable = true;

    private List<Clause> baseItemClauses = new ArrayList<>();
    private Integer maxDiscountUnits;
    private String modifierProcessingMethod = "";
    private List<Clause> modifierClauses = new ArrayList<>();
    private final Map<String, Object> dynamicFields = new LinkedHashMap<>();


    // ---------- Chainable Builder Methods ----------
    public LisConfigRequestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public LisConfigRequestBuilder withFilterItemSet(String filterItemSet) {
        this.filterItemSet = filterItemSet;
        return this;
    }

    public LisConfigRequestBuilder excludeNonPayable(boolean excludeNonPayable) {
        this.excludeNonPayable = excludeNonPayable;
        return this;
    }

    public LisConfigRequestBuilder withBaseItemClause(String attribute, String operator, Object value) {
        this.baseItemClauses.add(Clause.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value)
                .build());
        return this;
    }

    public LisConfigRequestBuilder withModifierClause(String attribute, String operator, Object value) {
        this.modifierClauses.add(Clause.builder()
                .attribute(attribute)
                .operator(operator)
                .value(value)
                .build());
        return this;
    }

    public LisConfigRequestBuilder withModifierProcessingMethod(String method) {
        this.modifierProcessingMethod = method;
        return this;
    }

    public LisConfigRequestBuilder withMaxDiscountUnits(int maxUnits) {
        this.maxDiscountUnits = maxUnits;
        return this;
    }
    
    // ---------- Dynamic Field Handling ----------
    public LisConfigRequestBuilder addField(String key, Object value) {
        dynamicFields.put(key, value);
        return this;
    }

    public LisConfigRequestBuilder removeField(String key) {
        dynamicFields.remove(key);
        return this;
    }
    

    // ---------- Build JSON ----------
    public String buildJson() throws JsonProcessingException {
        if (name == null) {
            throw new IllegalArgumentException("Name is mandatory");
        }

        BaseItems baseItems = null;
        if (!baseItemClauses.isEmpty()) {
            baseItems = BaseItems.builder()
                    .clauses(baseItemClauses)
                    .build();
        }

        Modifiers modifiers = null;
        if (!modifierClauses.isEmpty() || maxDiscountUnits != null) {
            modifiers = Modifiers.builder()
                    .maxDiscountUnits(maxDiscountUnits)
                    .processingMethod(modifierProcessingMethod)
                    .clauses(modifierClauses)
                    .build();
        }

        LisConfigData data = LisConfigData.builder()
                .name(name)
                .externalId(externalId)
                .filterItemSet(filterItemSet)
                .excludeNonPayable(excludeNonPayable)
                .baseItems(baseItems)
                .modifiers(modifiers)
                .build();

        Map<String, Object> root = new HashMap<>();
        root.put("data", Collections.singletonList(data));

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    // ---------- Inner Classes ----------
    @Data
    @Builder
    public static class LisConfigData {
        private String name;
        private String externalId;
        private String filterItemSet;
        private boolean excludeNonPayable;
        private BaseItems baseItems;
        private Modifiers modifiers;
    }

    @Data
    @Builder
    public static class BaseItems {
        private List<Clause> clauses;
    }

    @Data
    @Builder
    public static class Modifiers {
        private Integer maxDiscountUnits;
        private String processingMethod;
        private List<Clause> clauses;
    }

    @Data
    @Builder
    public static class Clause {
        private String attribute;
        private String operator;
        private Object value;
    }

    // ---------- Example Usage ----------
    public static void main(String[] args) throws Exception {
    	
    	
    	LisConfigRequestBuilder lisOBJ     = new LisConfigRequestBuilder()
                 .withName("AutomationLIS_API_" + System.currentTimeMillis())
                 .withFilterItemSet("base_and_modifiers")
                 .excludeNonPayable(true)
                 .withBaseItemClause("item_id", "in", "101,102")
                 .withBaseItemClause("modifiers_quantity", "<=", 2)
                 .withBaseItemClause("modifiers_amount", "<=", 4)
                 .withMaxDiscountUnits(3)
                 .withModifierProcessingMethod("")
                 .withModifierClause("item_id", "in", "111,112,113,114")
                 .withModifierClause("quantity", "in", "2") ;
                 
    	 String json = lisOBJ.buildJson();
    	 System.out.println("JSON with quantity field:" + json);
    	
    	 
    	 String json1 = lisOBJ.removeField("quantity").buildJson();
    	 System.out.println("JSON without quantity field:" + json1);
    	 
    }
}
