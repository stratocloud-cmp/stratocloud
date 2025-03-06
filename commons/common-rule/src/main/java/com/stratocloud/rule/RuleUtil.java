package com.stratocloud.rule;

import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;

public class RuleUtil {

    private static final NashornScriptEngineFactory engineFactory = new NashornScriptEngineFactory();

    private static final ClassFilter classFilter = className -> false;

    public static ScriptEngine getJavaScriptEngine(){
        return engineFactory.getScriptEngine(classFilter);
    }


}
