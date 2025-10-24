package me.steamworkp2p.service;

import com.codedisaster.steamworks.*;
import me.steamworkp2p.event.P2PPacketProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * P2P网络服务类
 * 负责处理P2P连接、数据传输等功能
 */
@Service
public class P2PNetworkService {
    
    private static final Logger logger = LoggerFactory.getLogger(P2PNetworkService.class);
    
    @Autowired
    private SteamService steamService;
    
    // 连接状态监听器
    private final List<ConnectionStateListener> connectionListeners = new CopyOnWriteArrayList<>();
    
    // 消息监听器
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    
    // 当前连接
    private final Map<SteamID, SteamNetworking.P2PSessionState> connections = new ConcurrentHashMap<>();
    
    // 是否正在监听
    private boolean isListening = false;
    
    /**
     * 连接状态监听器接口
     */
    public interface ConnectionStateListener {
        void onConnectionEstablished(SteamID steamID);
        void onConnectionLost(SteamID steamID);
        void onConnectionFailed(SteamID steamID);
    }
    
    /**
     * 消息监听器接口
     */
    public interface MessageListener {
        void onMessageReceived(SteamID steamID, String message);
    }
    
    /**
     * 开始监听P2P连接
     */
    public boolean startListening() {
        if (!steamService.isInitialized()) {
            logger.error("❌ Steam API未初始化，无法开始监听");
            return false;
        }
        
        try {
            // 使用Steam P2P API开始监听
            isListening = true;
            logger.info("🎧 P2P监听已启动，等待连接...");
            logger.debug("🔍 开始处理P2P数据包...");
            
            // 启动数据包处理线程
            startPacketProcessor();
            
            return true;
            
        } catch (Exception e) {
            logger.error("💥 启动P2P监听时发生错误", e);
            return false;
        }
    }
    
    /**
     * 停止监听P2P连接
     */
    public void stopListening() {
        if (isListening) {
            isListening = false;
            logger.info("🛑 P2P监听已停止");
            
            // 关闭所有现有连接
            for (SteamID steamID : connections.keySet()) {
                disconnectFromUser(steamID.toString());
            }
            connections.clear();
        }
    }
    
    /**
     * 连接到指定的Steam用户
     */
    public boolean connectToUser(String steamIDString) {
        if (!steamService.isInitialized()) {
            logger.error("❌ Steam API未初始化，无法连接");
            return false;
        }
        
        try {
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamIDString));
            logger.info("🔗 正在连接到用户: {} ({})", steamIDString, steamID);
            
            // 使用Steam P2P API发送连接请求
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                // 发送P2P连接请求
                ByteBuffer messageBuffer = ByteBuffer.allocateDirect("CONNECT_REQUEST".getBytes().length);
                messageBuffer.put("CONNECT_REQUEST".getBytes());
                messageBuffer.flip();
                
                boolean result = steamNetworking.sendP2PPacket(steamID, 
                    messageBuffer, 
                    SteamNetworking.P2PSend.Reliable, 
                    0);
                
                if (result) {
                    logger.info("✅ 连接请求已发送给用户: {}", steamIDString);
                    return true;
                } else {
                    logger.error("❌ 发送连接请求失败");
                    return false;
                }
            } else {
                logger.error("❌ Steam Networking接口不可用");
                return false;
            }
            
        } catch (NumberFormatException e) {
            logger.error("❌ 无效的Steam ID格式: {}", steamIDString);
            return false;
        } catch (Exception e) {
            logger.error("💥 连接用户时发生错误", e);
            return false;
        }
    }
    
    /**
     * 断开与指定用户的连接
     */
    public void disconnectFromUser(String steamIDString) {
        try {
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamIDString));
            logger.info("🔌 正在断开与用户 {} 的连接", steamIDString);
            
            // 使用Steam P2P API关闭连接
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                steamNetworking.closeP2PSessionWithUser(steamID);
                connections.remove(steamID);
                logger.info("✅ 已断开与用户 {} 的连接", steamIDString);
            }
            
        } catch (NumberFormatException e) {
            logger.error("❌ 无效的Steam ID格式: {}", steamIDString);
        } catch (Exception e) {
            logger.error("💥 断开连接时发生错误", e);
        }
    }
    
    /**
     * 发送消息给指定用户
     */
    public boolean sendMessage(String steamIDString, String message) {
        if (!steamService.isInitialized()) {
            logger.error("❌ Steam API未初始化，无法发送消息");
            return false;
        }
        
        try {
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamIDString));
            logger.debug("📤 发送消息给用户 {}: {}", steamIDString, message);
            
            // 使用Steam P2P API发送消息
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                // 将消息转换为字节数组
                byte[] messageBytes = message.getBytes("UTF-8");
                
                // 发送P2P数据包
                ByteBuffer messageBuffer = ByteBuffer.allocateDirect(messageBytes.length);
                messageBuffer.put(messageBytes);
                messageBuffer.flip();
                
                boolean result = steamNetworking.sendP2PPacket(steamID, 
                    messageBuffer, 
                    SteamNetworking.P2PSend.Reliable, 
                    0);
                
                if (result) {
                    logger.info("✅ 消息已发送给用户 {}: {}", steamIDString, message);
                    return true;
                } else {
                    logger.error("❌ 发送消息失败");
                    return false;
                }
            } else {
                logger.error("❌ Steam Networking接口不可用");
                return false;
            }
            
        } catch (NumberFormatException e) {
            logger.error("❌ 无效的Steam ID格式: {}", steamIDString);
            return false;
        } catch (Exception e) {
            logger.error("💥 发送消息时发生错误", e);
            return false;
        }
    }
    
    /**
     * 监听P2P数据包处理事件
     */
    @EventListener
    public void handleP2PPacketProcessEvent(P2PPacketProcessEvent event) {
        processReceivedPackets();
    }
    
    /**
     * 处理接收到的P2P数据包
     */
    private void processReceivedPackets() {
        if (!steamService.isInitialized()) {
            return;
        }
        
        try {
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking == null) {
                return;
            }
            
            // 读取P2P数据包
            SteamNetworking.P2PSessionState sessionState = new SteamNetworking.P2PSessionState();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            
            int packetSize = steamNetworking.isP2PPacketAvailable(0);
            while (packetSize > 0) {
                SteamID senderID = new SteamID();
                int bytesRead = steamNetworking.readP2PPacket(senderID, buffer, packetSize);
                if (bytesRead > 0) {
                    // 处理接收到的数据包
                    byte[] data = new byte[packetSize];
                    buffer.rewind();
                    buffer.get(data);
                    
                    String message = new String(data, "UTF-8");
                    
                    logger.debug("📥 收到来自用户 {} 的消息: {}", senderID, message);
                    
                    // 通知消息监听器
                    for (MessageListener listener : messageListeners) {
                        try {
                            listener.onMessageReceived(senderID, message);
                        } catch (Exception e) {
                            logger.error("💥 消息监听器处理错误", e);
                        }
                    }
                    
                    // 更新连接状态
                    connections.put(senderID, sessionState);
                }
                
                // 检查是否还有更多数据包
                packetSize = steamNetworking.isP2PPacketAvailable(0);
            }
            
        } catch (Exception e) {
            logger.error("💥 处理接收数据包时发生错误", e);
        }
    }
    
    /**
     * 启动数据包处理线程
     */
    private void startPacketProcessor() {
        // 数据包处理将在Steam回调中自动进行
        logger.debug("🔄 P2P数据包处理已启动");
    }
    
    /**
     * 添加连接状态监听器
     */
    public void addConnectionStateListener(ConnectionStateListener listener) {
        connectionListeners.add(listener);
    }
    
    /**
     * 移除连接状态监听器
     */
    public void removeConnectionStateListener(ConnectionStateListener listener) {
        connectionListeners.remove(listener);
    }
    
    /**
     * 添加消息监听器
     */
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }
    
    /**
     * 移除消息监听器
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
    
    /**
     * 获取当前连接列表
     */
    public Map<SteamID, SteamNetworking.P2PSessionState> getConnections() {
        return new HashMap<>(connections);
    }
    
    /**
     * 检查是否正在监听
     */
    public boolean isListening() {
        return isListening;
    }
}
