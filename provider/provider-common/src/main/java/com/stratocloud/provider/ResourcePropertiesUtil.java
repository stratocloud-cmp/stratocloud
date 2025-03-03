package com.stratocloud.provider;

import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ResourcePropertiesUtil {
    public static Map<String, Object> getPropertiesAtIndex(Map<String, Object> properties,
                                                           int index,
                                                           List<String> splittingKeys){
        Map<String, Object> result = JSON.clone(properties);

        if(Utils.isEmpty(properties))
            return result;

        if(Utils.isEmpty(splittingKeys))
            return result;

        for (String splittingKey : splittingKeys) {
            Object value = properties.get(splittingKey);

            if(value == null)
                continue;

            if(value instanceof List<?> list){
                if(Utils.isEmpty(list))
                    continue;

                if(list.size() <= index){
                    log.warn("The size of property {} is {}, cannot get by index {}, setting it to empty list. " +
                            "PropertyValue={}.", splittingKey, list.size(), index, list);
                    result.put(splittingKey, new ArrayList<>());
                    continue;
                }

                Object element = list.get(index);
                result.put(splittingKey, new ArrayList<>(List.of(element)));
            }else {
                log.warn("Property {} is not a list, cannot get by index {}. PropertyValue={}.",
                        splittingKey, index, value);
            }
        }

        return result;
    }
}
