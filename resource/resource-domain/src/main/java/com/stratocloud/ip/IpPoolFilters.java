package com.stratocloud.ip;

import java.util.List;

public record IpPoolFilters(List<Long> ipPoolIds,
                            List<Long> networkResourceIds,
                            String search,
                            InternetProtocol protocol) {

}
