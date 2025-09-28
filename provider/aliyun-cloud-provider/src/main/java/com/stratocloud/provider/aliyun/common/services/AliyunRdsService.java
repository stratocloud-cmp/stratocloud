package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.rds20140815.models.*;
import com.stratocloud.provider.aliyun.rds.model.RdsAccount;
import com.stratocloud.provider.aliyun.rds.model.RdsInstance;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceClass;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceDetail;

import java.util.List;
import java.util.Optional;

public interface AliyunRdsService {

    List<RdsInstanceClass> describeInstanceClasses();

    Optional<RdsInstanceClass> describeInstanceClass(String classCode);

    List<RdsInstance> describeInstances(DescribeDBInstancesRequest request);

    Optional<RdsInstance> describeInstance(String instanceId);

    Optional<RdsInstanceDetail> describeInstanceDetail(String instanceId);

    DescribePriceResponseBody describePrice(DescribePriceRequest request);

    String createInstance(CreateDBInstanceRequest request);

    String createInstanceForRebuild(CreateDBInstanceForRebuildRequest request);

    void startInstance(String instanceId);

    void stopInstance(String instanceId);

    void deleteInstance(String instanceId, String backupKeepPolicy);

    void destroyInstance(String instanceId);

    void modifyInstanceName(ModifyDBInstanceDescriptionRequest request);

    List<RdsAccount> describeAccounts(String instanceId);

    void resetAccountPassword(ResetAccountPasswordRequest request);

    void modifyMaintainTime(ModifyDBInstanceMaintainTimeRequest request);

    void renewInstance(RenewInstanceRequest request);

    DescribeRenewalPriceResponseBody describeRenewalPrice(DescribeRenewalPriceRequest request);

    DescribeAvailableClassesResponseBody describeAvailableClasses(DescribeAvailableClassesRequest request);

    void modifyInstanceSpec(ModifyDBInstanceSpecRequest request);

    void upgradeKernelVersion(UpgradeDBInstanceKernelVersionRequest request);
}
