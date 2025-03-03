package com.stratocloud.identifier;


import com.stratocloud.utils.NetworkUtil;

public class SnowflakeId {
    private static final Sequence sequence = new Sequence(NetworkUtil.getLocalInetAddress());

    public static synchronized long nextId(){
        return sequence.nextId();
    }
}
