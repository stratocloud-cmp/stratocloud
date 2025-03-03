package com.stratocloud.rule;


import com.stratocloud.jpa.entities.Tenanted;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.NumberFormat;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class NamingSequence extends Tenanted {
    @Column(nullable = false)
    private String ruleType;
    @Column(nullable = false)
    private String prefix;
    @Column(nullable = false)
    private Integer count;

    public NamingSequence(String ruleType, String prefix, int count) {
        this.ruleType = ruleType;
        this.prefix = prefix;
        this.count = count;
    }

    public String next(int minLength) {
        NumberFormat instance = NumberFormat.getNumberInstance();
        instance.setMinimumIntegerDigits(minLength);
        instance.setGroupingUsed(false);
        String result = instance.format(count);
        count++;
        return result;
    }
}
