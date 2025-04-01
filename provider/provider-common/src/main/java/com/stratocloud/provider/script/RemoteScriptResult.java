package com.stratocloud.provider.script;

import com.stratocloud.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public record RemoteScriptResult(Status status, String output, String error) {

    public enum Status{
        SUCCESS,
        FAILED
    }


    public static RemoteScriptResult failed(String output, String error){
        return new RemoteScriptResult(Status.FAILED, output, error);
    }

    public static RemoteScriptResult succeed(String output, String error) {
        return new RemoteScriptResult(Status.SUCCESS, output, error);
    }

    private Map<String, String> getOutputArguments(String functionName){
        Map<String, String> result = new HashMap<>();

        if(Utils.isBlank(output))
            return result;

        String[] lines = output.split("\n");

        for (String line : lines) {
            if(Utils.isBlank(line))
                continue;

            if(line.startsWith(functionName+"(") && line.endsWith(")")){
                String keyValue = line.substring(functionName.length()+1, line.length()-1).trim();

                int index = keyValue.indexOf("=");

                if(index == -1 || index >= keyValue.length()-1)
                    continue;

                String key = keyValue.substring(0, index).trim();
                String value = keyValue.substring(index+1).trim();
                result.put(key, value);
            }
        }

        return result;
    }


    public Map<String, String> getOutputArguments(){
        return getOutputArguments("output");
    }

    public Map<String, String> getHiddenOutputArguments(){
        return getOutputArguments("output_hidden");
    }
}
