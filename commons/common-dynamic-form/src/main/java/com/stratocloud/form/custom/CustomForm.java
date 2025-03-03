package com.stratocloud.form.custom;

import com.stratocloud.form.info.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record CustomForm(List<CustomFormItem> items) {

    public DynamicFormMetaData toDynamicFormMetaData(){
        List<FieldInfo> fieldInfoList = new ArrayList<>();
        String formClass = Map.class.getSimpleName();

        if(Utils.isNotEmpty(items)){
            for (CustomFormItem item : items) {
                FieldDetail fieldDetail = getFieldDetail(item);

                FieldInfo fieldInfo = new FieldInfo(
                        item.type(), item.key(), item.label(), item.description(), fieldDetail
                );
                fieldInfoList.add(fieldInfo);
            }
        }

        return new DynamicFormMetaData(formClass, fieldInfoList);
    }

    private FieldDetail getFieldDetail(CustomFormItem item) {
        return switch (item.type()){
            case "BooleanField" -> JSON.convert(item.detail(), BooleanFieldDetail.class);
            case "InputField" -> JSON.convert(item.detail(), InputFieldDetail.class);
            case "NumberField" -> JSON.convert(item.detail(), NumberFieldDetail.class);
            case "SelectField" -> JSON.convert(item.detail(), SelectFieldDetail.class);
            default -> new FieldDetail.Dummy();
        };
    }
}
