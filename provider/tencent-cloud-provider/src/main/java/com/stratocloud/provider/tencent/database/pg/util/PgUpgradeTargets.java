package com.stratocloud.provider.tencent.database.pg.util;

import java.util.List;

public record PgUpgradeTargets(String currentVersion,
                               List<String> minorUpgradeTargets,
                               List<String> majorUpgradeTargets) {
}
