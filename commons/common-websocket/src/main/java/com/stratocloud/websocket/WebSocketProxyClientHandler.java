package com.stratocloud.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Copies data from the client to the server session.
 */
@Slf4j
public class WebSocketProxyClientHandler extends AbstractWebSocketHandler {

    private final WebSocketSession webSocketServerSession;
    private final ClientOfflineListener listener;

    public WebSocketProxyClientHandler(WebSocketSession webSocketServerSession, ClientOfflineListener listener) {
        this.webSocketServerSession = webSocketServerSession;
        this.listener = listener;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
        webSocketServerSession.sendMessage(webSocketMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // notify client has been offline, upstream client should exit.
        log.info("Client offline. CloseStatus={}.", status);
        listener.clientOffline();
    }

    public interface ClientOfflineListener{
        void clientOffline();
    }
}
