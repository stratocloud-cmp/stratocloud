package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.*;
import com.stratocloud.form.info.*;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Data
@Slf4j
public class CdbParamList implements DynamicForm {
    @SelectField(label = "字符集")
    private String character_set_server;
    @SelectField(label = "排序规则")
    private String collation_server;
    @SelectField(label = "表名大小写敏感")
    private String lower_case_table_names;


    public static ParamInfo[] getParamInfoList(Map<String, Object> params){
        if(Utils.isEmpty(params))
            return new ParamInfo[0];

        List<ParamInfo> result = new ArrayList<>();

        for (String key : params.keySet()) {
            Object value = params.get(key);

            if (value == null)
                continue;

            String s = String.valueOf(value);

            if (Utils.isBlank(s))
                continue;

            ParamInfo paramInfo = new ParamInfo();
            paramInfo.setName(key);
            paramInfo.setValue(s);

            result.add(paramInfo);
        }

        return result.toArray(ParamInfo[]::new);
    }

    public static DynamicFormMetaData getFormMetaData(TencentCloudProvider provider,
                                                      ExternalAccount account,
                                                      String templateType,
                                                      String engineType,
                                                      String engineVersion) {
        List<FieldInfo> fieldInfoList = new ArrayList<>();

        DescribeDefaultParamsRequest request = new DescribeDefaultParamsRequest();
        request.setTemplateType(templateType);
        request.setEngineType(engineType);
        request.setEngineVersion(engineVersion);

        DescribeDefaultParamsResponse response = provider.buildClient(account).describeCdbDefaultParams(request);

        if(response.getItems() == null)
            return new DynamicFormMetaData(CdbParamList.class.getSimpleName(), fieldInfoList);

        List<ParameterDetail> editableParams = Arrays.stream(response.getItems()).filter(
                i -> !(i.getIsNotSupportEdit() != null && i.getIsNotSupportEdit())
        ).toList();

        for (ParameterDetail param : editableParams) {
            try {
                handleParam(param, fieldInfoList);
            }catch (Exception e){
                log.warn(e.toString());
            }
        }

        return new DynamicFormMetaData(CdbParamList.class.getSimpleName(), fieldInfoList);
    }

    private static void handleParam(ParameterDetail param, List<FieldInfo> fieldInfoList) {
        if(fieldInfoList.stream().anyMatch(i -> Objects.equals(i.key(), param.getName())))
            return;

        String paramType = param.getParamType();

        Optional<String> paramLabel = translateParamName(param.getName());
        if(paramLabel.isEmpty())
            return;

        FieldInfo fieldInfo;

        if(Objects.equals(paramType, "enum")){
            List<String> options = param.getEnumValue() != null ? List.of(param.getEnumValue()) : List.of();

            fieldInfo = new FieldInfo(
                    SelectField.class.getSimpleName(),
                    param.getName(),
                    paramLabel.get(),
                    param.getDescription(),
                    new SelectFieldDetail(
                            false,
                            false,
                            Utils.isNotBlank(param.getDefault()) ? List.of(param.getDefault()) : List.of(),
                            options,
                            options,
                            Source.STATIC,
                            SelectEntityType.NONE,
                            List.of(),
                            false,
                            List.of(),
                            SelectType.NORMAL,
                            param.getName()
                    )
            );
        } else if(Objects.equals(paramType, "integer")){
            int min = getInt(param.getMin(), 0);
            int max = getInt(param.getMax(), Integer.MAX_VALUE);

            Integer defaultValue = param.getDefault() != null ? Integer.parseInt(param.getDefault()) : null;

            if(min == 0 && max == 1){
                fieldInfo = new FieldInfo(
                        SelectField.class.getSimpleName(),
                        param.getName(),
                        paramLabel.get(),
                        param.getDescription(),
                        new SelectFieldDetail(
                                false,
                                false,
                                Utils.isNotBlank(param.getDefault()) ? List.of(param.getDefault()) : List.of(),
                                List.of("0", "1"),
                                List.of("关闭", "开启"),
                                Source.STATIC,
                                SelectEntityType.NONE,
                                List.of(),
                                false,
                                List.of(),
                                SelectType.NORMAL,
                                param.getName()
                        )
                );
            }else {
                fieldInfo = new FieldInfo(
                        NumberField.class.getSimpleName(),
                        param.getName(),
                        paramLabel.get(),
                        param.getDescription(),
                        new NumberFieldDetail(
                                defaultValue,
                                min,
                                max,
                                false,
                                List.of(),
                                param.getName()
                        )
                );
            }

        } else if(Objects.equals(paramType, "float") || Objects.equals(paramType, "string")){
            fieldInfo = new FieldInfo(
                    InputField.class.getSimpleName(),
                    param.getName(),
                    paramLabel.get(),
                    param.getDescription(),
                    new InputFieldDetail(
                            param.getDefault(),
                            false,
                            List.of(),
                            null,
                            null,
                            "text",
                            false
                    )
            );
        } else {
            log.warn("Unsupported cdb param type: {}. ParamName={}.", param.getParamType(), param.getName());
            return;
        }

        fieldInfoList.add(fieldInfo);
    }

    private static Optional<String> translateParamName(String name) {
        return switch (name){
            case "character_set_server" -> Optional.of("字符集");
            case "collation_server" -> Optional.of("排序规则");
            case "lower_case_table_names" -> Optional.of("表名大小写敏感");
            default -> Optional.empty();
        };
    }

    private static int getInt(Long value, int defaultValue) {
        return value != null ? value.intValue() : defaultValue;
    }
}
