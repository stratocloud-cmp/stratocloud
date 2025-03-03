package com.stratocloud.provider.aliyun.cert;

import com.aliyun.slb20140515.models.DescribeServerCertificatesResponseBody;

public record AliyunServerCert(
        DescribeServerCertificatesResponseBody.DescribeServerCertificatesResponseBodyServerCertificatesServerCertificate detail
) {
}
