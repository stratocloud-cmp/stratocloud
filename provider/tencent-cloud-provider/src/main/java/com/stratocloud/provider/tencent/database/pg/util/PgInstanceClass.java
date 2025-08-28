package com.stratocloud.provider.tencent.database.pg.util;

import com.tencentcloudapi.postgres.v20170312.models.ClassInfo;
import com.tencentcloudapi.postgres.v20170312.models.Version;

import java.util.Set;

public record PgInstanceClass(ClassInfo detail,
                              Set<String> supportedZones,
                              Set<Version> supportedVersions) {
}
