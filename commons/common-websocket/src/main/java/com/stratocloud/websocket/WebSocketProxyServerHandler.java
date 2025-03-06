package com.stratocloud.websocket;

import com.stratocloud.exceptions.StratoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Handles establishment and tracking of next 'hop', and
 * copies data from the current session to the next hop.
 */

@Slf4j
public class WebSocketProxyServerHandler extends AbstractWebSocketHandler implements SubProtocolCapable {

    private final Map<String, NextHop> nextHops = new ConcurrentHashMap<>();

    private final Function<URI, URI> proxyTargetGetter;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        NextHop nextHop = getNextHop(session);
        log.info("Next hop is: {}", nextHop.getRemoteAddress());
    }

    public WebSocketProxyServerHandler(Function<URI, URI> proxyTargetGetter) {
        this.proxyTargetGetter = proxyTargetGetter;
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession,
                              WebSocketMessage<?> webSocketMessage) throws Exception {
        NextHop nextHop = getNextHop(webSocketSession);
        nextHop.sendMessageToNextHop(webSocketMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus status) throws Exception {
        super.afterConnectionClosed(webSocketSession, status);
        NextHop nextHop = nextHops.get(webSocketSession.getId());
        if (nextHop != null) {
            nextHop.close();
        }
    }

    private NextHop getNextHop(WebSocketSession webSocketSession) {
        NextHop nextHop = nextHops.get(webSocketSession.getId());
        if (nextHop == null) {
            // registering offline listener to avoid nextHop leaks.
            nextHop = new NextHop(
                    webSocketSession,
                    () -> {
                        nextHops.remove(webSocketSession.getId());
                        try {
                            webSocketSession.close();
                        } catch (IOException e) {
                            throw new StratoException(e.getMessage(), e);
                        }
                    },
                    proxyTargetGetter.apply(webSocketSession.getUri())
            );
            nextHops.put(webSocketSession.getId(), nextHop);
        }
        return nextHop;
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of("binary");
    }
}
