package com.stratocloud.resource;

public record ResourceReadActionResult(String name, String value, boolean isUrl, ResultType resultType) {

    public ResourceReadActionResult(String name, String value, boolean isUrl){
        this(name, value, isUrl, ResultType.PLAIN_TEXT);
    }

    public enum ResultType{
        PLAIN_TEXT, YAML
    }
}
