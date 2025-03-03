package com.stratocloud.rule;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.entities.Tenanted;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.Setter;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.Map;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED)
public class Rule extends Tenanted {
    @Column(nullable = false, updatable = false)
    private String type;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String script;


    public Object execute(Map<String, Object> parameters){
        ScriptEngine scriptEngine = RuleUtil.getJavaScriptEngine();
        try {
            SimpleBindings bindings = new SimpleBindings();
            bindings.put("args", parameters);
            return scriptEngine.eval(script, bindings);
        } catch (ScriptException e) {
            throw new StratoException(e.getMessage(), e);
        }
    }

    public void update(String name, String script) {
        this.name = name;
        this.script = script;
    }
}
