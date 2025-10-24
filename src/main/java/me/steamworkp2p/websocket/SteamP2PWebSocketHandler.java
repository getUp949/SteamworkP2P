package me.steamworkp2p.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.steamworkp2p.event.ConnectionChangeEvent;
import me.steamworkp2p.event.NewMessageEvent;
import me.steamworkp2p.event.StatusUpdateEvent;
import me.steamworkp2p.service.SteamP2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket处理器 - 处理实时通信
 */
@Component
public class SteamP2PWebSocketHandler implements WebSocketHandler {
    
    @Autowired
    private SteamP2PService steamP2PService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket连接已建立: " + session.getId());
        
        // 发送当前状态
        sendStatusUpdate(session);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            System.out.println("收到WebSocket消息: " + payload);
            
            // 这里可以处理客户端发送的实时消息
            // 例如：发送P2P消息、连接请求等
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
        sessions.remove(session.getId());
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("WebSocket连接已关闭: " + session.getId());
        sessions.remove(session.getId());
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * 监听状态更新事件
     */
    @EventListener
    public void handleStatusUpdateEvent(StatusUpdateEvent event) {
        Map<String, Object> status = Map.of(
            "isListening", steamP2PService.isListening(),
            "connections", steamP2PService.getActiveConnections(),
            "steamRunning", steamP2PService.isSteamRunning(),
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage("status_update", status);
    }
    
    /**
     * 监听新消息事件
     */
    @EventListener
    public void handleNewMessageEvent(NewMessageEvent event) {
        Map<String, Object> messageData = Map.of(
            "from", event.getFromSteamId(),
            "to", event.getToSteamId(),
            "message", event.getMessage(),
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage("new_message", messageData);
    }
    
    /**
     * 监听连接状态变化事件
     */
    @EventListener
    public void handleConnectionChangeEvent(ConnectionChangeEvent event) {
        Map<String, Object> connectionData = Map.of(
            "steamId", event.getSteamId(),
            "connected", event.isConnected(),
            "timestamp", System.currentTimeMillis()
        );
        
        broadcastMessage("connection_change", connectionData);
    }
    
    /**
     * 发送状态更新给指定会话
     */
    private void sendStatusUpdate(WebSocketSession session) {
        try {
            Map<String, Object> status = Map.of(
                "isListening", steamP2PService.isListening(),
                "connections", steamP2PService.getActiveConnections(),
                "steamRunning", steamP2PService.isSteamRunning(),
                "timestamp", System.currentTimeMillis()
            );
            
            sendMessage(session, "status_update", status);
        } catch (Exception e) {
            System.err.println("发送状态更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 广播消息给所有连接的客户端
     */
    private void broadcastMessage(String type, Object data) {
        Map<String, Object> message = Map.of(
            "type", type,
            "data", data
        );
        
        sessions.values().forEach(session -> {
            try {
                sendMessage(session, type, data);
            } catch (Exception e) {
                System.err.println("广播消息失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, String type, Object data) throws IOException {
        if (session.isOpen()) {
            Map<String, Object> message = Map.of(
                "type", type,
                "data", data
            );
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        }
    }
}
