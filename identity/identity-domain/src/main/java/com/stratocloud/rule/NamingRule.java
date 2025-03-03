package com.stratocloud.rule;

import com.stratocloud.exceptions.StratoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Entity
@Getter
@Setter
public class NamingRule extends Rule{
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuffixType suffixType;
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Integer suffixLength;
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private Integer suffixStartNumber;

    public String getPrefix(Map<String, Object> parameters){
        Object result = execute(parameters);

        if(result == null)
            throw new StratoException("命名规则[%s]脚本有误，返回值为null".formatted(getName()));

        if(result instanceof String prefix)
            return prefix;
        else
            throw new StratoException(
                    "命名规则[%s]脚本有误，返回值必须为字符串，实际为%s".formatted(
                            getName(),
                            result.getClass().getSimpleName()
                    )
            );
    }

    public void updateSuffixPolicy(SuffixPolicy suffixPolicy) {
        suffixType = suffixPolicy.type();
        suffixLength = suffixPolicy.suffixLength();
        suffixStartNumber = suffixPolicy.suffixStartNumber();
    }

    public void setSuffixLength(Integer suffixLength) {
        this.suffixLength = suffixLength != null ? suffixLength : 0;
    }

    public void setSuffixStartNumber(Integer suffixStartNumber) {
        this.suffixStartNumber = suffixStartNumber != null ? suffixStartNumber : 0;
    }
}
