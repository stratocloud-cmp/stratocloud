package com.stratocloud.resource.cmd.relationship;

import com.stratocloud.request.BatchJobParameters;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchConnectResourcesCmd implements BatchJobParameters {
    private List<ConnectResourcesCmd> connections;

    @Override
    public void merge(BatchJobParameters other) {
        if(!(other instanceof BatchConnectResourcesCmd otherCmd))
            return;

        if(Utils.isEmpty(otherCmd.getConnections()))
            return;

        if(connections == null)
            connections = new ArrayList<>();

        connections.addAll(otherCmd.getConnections());
    }
}
