package com.stratocloud.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;

@Slf4j
public class ClosableUtil {
    public static void tryClose(Closeable... closeables){
        if(closeables == null)
            return;

        for (Closeable closeable : closeables) {
            tryCloseOne(closeable);
        }
    }

    private static void tryCloseOne(Closeable closeable){
        try {
            if(closeable != null)
                closeable.close();
        } catch (Exception e){
            log.warn(e.toString());
        }
    }
}
