package com.stratocloud.provider.aliyun.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * {
 *   "eventId": "92b33345-0cef-47be-821f-fb9914d3****",
 *   "eventAttributes": {
 *     "SensitiveAction": "true"
 *   },
 *   "eventVersion": 1,
 *   "sourceIpAddress": "ecs.aliyuncs.com",
 *   "userAgent": "ecs.aliyuncs.com",
 *   "eventRW": "Write",
 *   "eventType": "ApiCall",
 *   "referencedResources": {
 *     "ACS::ECS::Instance": [
 *       "i-8vb0smn1lf6g77md****"
 *     ],
 *     "ACS::ECS::Disk": [
 *       "d-8vbf8rpv2nn0l1zm****"
 *     ]
 *   },
 *   "userIdentity": {
 *     "type": "system",
 *     "userName": "ecs.aliyuncs.com"
 *   },
 *   "serviceName": "Ecs",
 *   "requestId": "32B7EB75-62EE-511E-9449-E19EBF67C2ED",
 *   "eventTime": "2022-10-22T21:52:00Z",
 *   "isGlobal": false,
 *   "acsRegion": "cn-hangzhou",
 *   "eventName": "DeleteDisk"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AliyunEvent {
    private String eventId;
    private Map<String, Object> eventAttributes;
    private long eventVersion;
    private String sourceIpAddress;
    private String userAgent;
    private String eventRW;
    private String eventType;
    private Map<String, List<String>> referencedResources;
    private Map<String, Object> userIdentity;
    private String serviceName;
    private String requestId;
    private String eventTime;
    private Boolean isGlobal;
    private String acsRegion;
    private String eventName;
}
