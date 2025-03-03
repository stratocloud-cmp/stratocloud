package com.stratocloud.order;

import com.stratocloud.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class OrderSummaryBuilder {
    private final List<OrderSummaryLine> lines = new ArrayList<>();

    public void addLine(String name, List<String> details){
        lines.add(new OrderSummaryLine(name, details));
    }

    public String build(){
        StringBuilder stringBuilder = new StringBuilder();

        for (OrderSummaryLine line : lines) {
            stringBuilder.append(line.toString());
        }

        return stringBuilder.toString();
    }

    private record OrderSummaryLine(String name, List<String> details) {
        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();

            if(Utils.isEmpty(details))
                return stringBuilder.toString();


            for (String detail : details) {
                stringBuilder.append(detail).append("\n");
            }

            return stringBuilder.toString();
        }
    }
}
