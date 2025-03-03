package com.stratocloud.provider.tencent.cert.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentCertBuildInput implements ResourceActionInput {

    @BooleanField(label = "使用免费证书", defaultValue = true)
    private Boolean useFreeCert;

    @SelectField(
            label = "证书类型",
            options = {
                    "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                    "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
                    "30", "31", "32", "33", "34", "35", "37", "38", "39", "40", "41", "42"
            },
            optionNames = {
                    "SecureSite 增强型企业版（EV Pro）",
                    "SecureSite 增强型（EV）",
                    "SecureSite 企业型专业版（OV Pro）",
                    "SecureSite 企业型（OV）",
                    "SecureSite 企业型（OV）通配符",
                    "Geotrust 增强型（EV）",
                    "Geotrust 企业型（OV）",
                    "Geotrust 企业型（OV）通配符",
                    "TrustAsia 域名型多域名 SSL 证书",
                    "TrustAsia 域名型（DV）通配符",
                    "TrustAsia 企业型通配符（OV）SSL 证书（D3）",
                    "TrustAsia 企业型（OV）SSL 证书（D3）",
                    "TrustAsia 企业型多域名 （OV）SSL 证书（D3）",
                    "TrustAsia 增强型 （EV）SSL 证书（D3）",
                    "TrustAsia 增强型多域名（EV）SSL 证书（D3）",
                    "GlobalSign 企业型（OV）SSL 证书",
                    "GlobalSign 企业型通配符 （OV）SSL 证书",
                    "GlobalSign 增强型 （EV）SSL 证书",
                    "TrustAsia 企业型通配符多域名（OV）SSL 证书（D3）",
                    "GlobalSign 企业型多域名（OV）SSL 证书",
                    "GlobalSign 企业型通配符多域名（OV）SSL 证书",
                    "GlobalSign 增强型多域名（EV）SSL 证书",
                    "Wotrus 域名型证书",
                    "Wotrus 域名型多域名证书",
                    "Wotrus 域名型通配符证书",
                    "Wotrus 企业型证书",
                    "Wotrus 企业型多域名证书",
                    "Wotrus 企业型通配符证书",
                    "Wotrus 增强型证书",
                    "Wotrus 增强型多域名证书",
                    "DNSPod 国密域名型证书",
                    "DNSPod 国密域名型多域名证书",
                    "DNSPod 国密域名型通配符证书",
                    "DNSPod 国密企业型证书",
                    "DNSPod 国密企业型多域名证书",
                    "DNSPod 国密企业型通配符证书",
                    "DNSPod 国密增强型证书",
                    "DNSPod 国密增强型多域名证书",
                    "TrustAsia 域名型通配符多域名证书"
            },
            conditions = "this.useFreeCert === false"
    )
    private Long productId;

    @NumberField(
            label = "域名数量",
            conditions = "this.useFreeCert === false",
            defaultValue = 1
    )
    private Long domainNum;


    /**
     * 验证方式：DNS_AUTO = 自动DNS验证，DNS = 手动DNS验证，FILE = 文件验证。
     */
    @SelectField(
            label = "验证方式",
            options = {"DNS_AUTO", "DNS", "FILE"},
            optionNames = {"自动DNS验证", "手动DNS验证", "文件验证"},
            conditions = "this.useFreeCert === true",
            defaultValues = "DNS_AUTO"
    )
    private String dvAuthMethod;

    @InputField(
            label = "域名",
            conditions = "this.useFreeCert === true"
    )
    private String domainName;

    @SelectField(
            label = "证书类型",
            options = {
                    "83"
            },
            optionNames = {
                    "TrustAsia C1 DV Free"
            },
            defaultValues = "83",
            conditions = "this.useFreeCert === true"
    )
    private String packageType;

    @InputField(label = "邮箱", conditions = "this.useFreeCert === true")
    private String contactEmail;

    @InputField(label = "手机", conditions = "this.useFreeCert === true", required = false)
    private String contactPhone;

    @SelectField(
            label = "加密算法",
            options = {"RSA", "ECC"},
            optionNames = {"RSA", "ECC"},
            conditions = "this.useFreeCert === true",
            defaultValues = "RSA"
    )
    private String csrEncryptAlgo;

    @InputField(label = "CSR 的加密密码", inputType = "password", conditions = "this.useFreeCert === true")
    private String csrKeyPassword;

}
