package com.stratocloud.resource;

import com.stratocloud.utils.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.temporal.ChronoUnit;

public record ResourceCost(double cost, double timeAmount, ChronoUnit timeUnit) {
    public static final ResourceCost ZERO = new ResourceCost(0, 1, ChronoUnit.MINUTES);

    public ResourceCost add(ResourceCost that){
        if(this.hasLargerTimeUnitThan(that))
            return this.addSmallerTimeUnitCost(that);
        else
            return that.addSmallerTimeUnitCost(this);
    }

    public ResourceCost absoluteAdd(ResourceCost that){
        ResourceCost scale = this.hasLargerTimeUnitThan(that) ? this : that;
        return new ResourceCost(this.cost+ that.cost, scale.timeAmount, scale.timeUnit);
    }

    private ResourceCost addSmallerTimeUnitCost(ResourceCost that){
        Assert.isTrue(this.hasLargerTimeUnitThan(that),
                "Failed to add cost because of time unit. This: %s. That: %s.".formatted(this, that)
        );

        long unitRatio = this.timeUnit.getDuration().dividedBy(that.timeUnit.getDuration());

        double amountRatio = this.timeAmount / that.timeAmount;

        double newCost = this.cost + (that.cost * unitRatio * amountRatio);

        return new ResourceCost(newCost, this.timeAmount, this.timeUnit);
    }


    private boolean hasLargerTimeUnitThan(ResourceCost that){
        return this.timeUnit.compareTo(that.timeUnit) >= 0;
    }



    public ResourceCost convertTimeUnitTo(ChronoUnit targetUnit){
        if(targetUnit.compareTo(timeUnit) >= 0){
            long ratio = targetUnit.getDuration().dividedBy(timeUnit.getDuration());
            return new ResourceCost(cost * ratio, timeAmount, targetUnit);
        }else {
            long ratio = timeUnit.getDuration().dividedBy(targetUnit.getDuration());
            return new ResourceCost(cost / ratio, timeAmount, targetUnit);
        }
    }

    public String toDescription() {
        if(cost == 0)
            return "";

        String costStr = BigDecimal.valueOf(cost).setScale(2, RoundingMode.FLOOR).toString();

        if(cost < 0)
            return "退费%s元".formatted(costStr);

        String amountStr = timeAmount==1.0?"": new DecimalFormat().format(timeAmount);
        return "%s元/%s%s".formatted(costStr, amountStr, getTimeUnitName());
    }

    public String toMonthlyCostDescription(){
        return convertTimeUnitTo(ChronoUnit.MONTHS).toDescription();
    }

    private String getTimeUnitName() {
        return switch (timeUnit){
            case SECONDS -> "秒";
            case MINUTES -> "分钟";
            case HOURS -> "小时";
            case DAYS -> "天";
            case WEEKS -> "周";
            case MONTHS -> "月";
            case YEARS -> "年";
            default -> timeUnit.name();
        };
    }

    public ResourceCost multiply(Integer multiplier) {
        return new ResourceCost(cost * multiplier, timeAmount, timeUnit);
    }
}
