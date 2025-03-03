package com.stratocloud.websocket;

import com.stratocloud.utils.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Represents a 'hop' in the proxying chain, establishes a 'client' to
 * communicate with the next server, with a {@link WebSocketProxyClientHandler}
 * to copy data from the 'client' to the supplied 'server' session.
 */
@Slf4j
public class NextHop {

    private final WebSocketSession clientSession;

    public NextHop(WebSocketSession webSocketServerSession,
                   WebSocketProxyClientHandler.ClientOfflineListener listener,
                   URI proxyTarget) {
        clientSession = createWebSocketClientSession(webSocketServerSession, listener, proxyTarget);
        log.info("Proxy target handshake established: {}", clientSession.getAcceptedProtocol());
    }

    private WebSocketHttpHeaders getWebSocketHttpHeaders(final WebSocketSession userAgentSession) {
        return new WebSocketHttpHeaders(userAgentSession.getHandshakeHeaders());
    }

    private WebSocketSession createWebSocketClientSession(WebSocketSession webSocketServerSession,
                                                          WebSocketProxyClientHandler.ClientOfflineListener listener,
                                                          URI proxyTarget) {
        try {
            WebSocketHttpHeaders headers = getWebSocketHttpHeaders(webSocketServerSession);
            StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
            webSocketClient.setSslContext(SecurityUtil.createIgnoreVerifySSL());
            WebSocketProxyClientHandler socketHandler = new WebSocketProxyClientHandler(
                    webSocketServerSession,
                    listener
            );
            return webSocketClient.execute(
                    socketHandler, headers, proxyTarget
            ).get(2000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Proxy target offline: " + proxyTarget);
            listener.clientOffline();
            throw new RuntimeException(e);
        }
    }

    public void sendMessageToNextHop(WebSocketMessage<?> webSocketMessage) throws IOException {
        clientSession.sendMessage(webSocketMessage);
    }

    /**
     * Triggering client offline.
     */
    public void close() throws IOException {
        clientSession.close();
    }

    public InetSocketAddress getRemoteAddress(){
        return clientSession.getRemoteAddress();
    }
}
