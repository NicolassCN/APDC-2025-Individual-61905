package pt.unl.fct.di.apdc.indiv.util.data;

import java.util.Map;

public class UpdateUserAttributesData {
    private String identifier;
    private Map<String, String> attributes;

    public UpdateUserAttributesData() {}

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}