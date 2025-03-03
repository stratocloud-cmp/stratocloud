package com.stratocloud.script;

import com.stratocloud.utils.Utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record RemoteScript(RemoteScriptType type, String content, String programPath) {
    public RemoteScript acceptEnvironment(Map<String,String> environment){
        if(Utils.isBlank(content) || Utils.isEmpty(environment))
            return this;

        String newContent = content;
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            Pattern pattern = Pattern.compile("\\{\\{ *" +entry.getKey()+ " *}}");

            Matcher matcher = pattern.matcher(newContent);
            newContent = matcher.replaceAll(entry.getValue());
        }
        return new RemoteScript(type, newContent, programPath);
    }
}
