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
    
    @Autowired
    private P2PNetworkUtils networkUtils;
    
    // 连接状态监听器
    private final List<ConnectionStateListener> connectionListeners = new CopyOnWriteArrayList<>();
    
    // 消息监听器
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    
    // 当前连接
    private final Map<SteamID, SteamNetworking.P2PSessionState> connections = new ConcurrentHashMap<>();
    
    // 连接尝试跟踪 - 用于超时检测
    private final Map<SteamID, Long> connectionAttempts = new ConcurrentHashMap<>();
    
    // 连接超时时间（毫秒）
    private static final long CONNECTION_TIMEOUT = 30000; // 30秒
    
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
            
            // 启动网络监控
            networkUtils.startNetworkMonitoring();
            
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
            
            // 停止网络监控
            networkUtils.stopNetworkMonitoring();
            
            // 关闭所有现有连接
            for (SteamID steamID : connections.keySet()) {
                disconnectFromUser(steamID.toString());
            }
            connections.clear();
        }
    }
    
    /**
     * 连接到指定的Steam用户
     * 模仿C++示例的InitiateServerConnection逻辑
     */
    public boolean connectToUser(String steamIDString) {
        logger.info("🔍 [P2P连接] 开始连接用户: {}", steamIDString);
        
        if (!steamService.isInitialized()) {
            logger.error("❌ [P2P连接] Steam API未初始化，无法连接");
            return false;
        }
        
        try {
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamIDString));
            logger.info("🔗 [P2P连接] 正在连接到用户: {} ({})", steamIDString, steamID);
            logger.info("🔍 [P2P连接] 当前Steam用户: {}", steamService.getCurrentUserName());
            
            // 检查是否已经连接
            if (connections.containsKey(steamID)) {
                logger.warn("⚠️ [P2P连接] 与用户 {} 的连接已存在", steamIDString);
                return true;
            }
            
            // 使用Steam P2P API发送连接请求
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                logger.info("🔍 [P2P连接] Steam Networking接口可用");
                
                // 发送P2P连接请求 - 模仿C++示例的BSendServerData
                String connectMessage = "P2P_CONNECT_REQUEST";
                ByteBuffer messageBuffer = ByteBuffer.allocateDirect(connectMessage.getBytes().length);
                messageBuffer.put(connectMessage.getBytes());
                messageBuffer.flip();
                
                logger.info("🔍 [P2P连接] 准备发送P2P数据包到: {}, 数据大小: {}", steamIDString, messageBuffer.remaining());
                
                // 使用可靠传输发送连接请求
                boolean result = steamNetworking.sendP2PPacket(steamID, 
                    messageBuffer, 
                    SteamNetworking.P2PSend.Reliable, 
                    0);
                
                if (result) {
                    logger.info("✅ [P2P连接] 连接请求已发送给用户: {}", steamIDString);
                    logger.info("🔍 [P2P连接] 等待对方接受连接...");
                    
                    // 记录连接尝试时间，用于超时检测
                    connectionAttempts.put(steamID, System.currentTimeMillis());
                    
                    return true;
                } else {
                    logger.error("❌ [P2P连接] 发送连接请求失败 - Steam API返回false");
                    logger.error("🔍 [P2P连接] 可能原因：1) 目标用户不在线 2) 网络问题 3) Steam P2P服务问题");
                    return false;
                }
            } else {
                logger.error("❌ [P2P连接] Steam Networking接口不可用");
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
     * 模仿C++示例的DisconnectFromServer逻辑
     */
    public void disconnectFromUser(String steamIDString) {
        try {
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamIDString));
            logger.info("🔌 [P2P断开] 正在断开与用户 {} 的连接", steamIDString);
            
            // 使用Steam P2P API关闭连接
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                // 发送断开连接消息
                String disconnectMessage = "P2P_DISCONNECT";
                ByteBuffer messageBuffer = ByteBuffer.allocateDirect(disconnectMessage.getBytes().length);
                messageBuffer.put(disconnectMessage.getBytes());
                messageBuffer.flip();
                
                // 发送断开连接通知
                steamNetworking.sendP2PPacket(steamID, 
                    messageBuffer, 
                    SteamNetworking.P2PSend.Reliable, 
                    0);
                
                // 关闭P2P会话
                steamNetworking.closeP2PSessionWithUser(steamID);
                
                // 从连接列表中移除
                connections.remove(steamID);
                connectionAttempts.remove(steamID);
                
                // 从活跃连接中移除
                steamService.removeActiveConnection(steamID);
                
                logger.info("✅ [P2P断开] 已断开与用户 {} 的连接", steamIDString);
                
                // 通知连接状态监听器
                for (ConnectionStateListener listener : connectionListeners) {
                    try {
                        listener.onConnectionLost(steamID);
                    } catch (Exception e) {
                        logger.error("💥 连接状态监听器处理错误", e);
                    }
                }
            }
            
        } catch (NumberFormatException e) {
            logger.error("❌ 无效的Steam ID格式: {}", steamIDString);
        } catch (Exception e) {
            logger.error("💥 断开连接时发生错误", e);
        }
    }
    
    /**
     * 发送消息给指定用户
     * 模仿C++示例的BSendServerData方法
     */
    public boolean sendMessage(String steamIDString, String message) {
        if (!steamService.isInitialized()) {
            logger.error("❌ Steam API未初始化，无法发送消息");
            return false;
        }
        
        try {
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamIDString));
            logger.debug("📤 [P2P发送] 发送消息给用户 {}: {}", steamIDString, message);
            
            // 检查连接是否存在
            if (!connections.containsKey(steamID) && !steamService.hasActiveConnection(steamID)) {
                logger.warn("⚠️ [P2P发送] 与用户 {} 的连接不存在，尝试建立连接", steamIDString);
                if (!connectToUser(steamIDString)) {
                    logger.error("❌ [P2P发送] 无法建立与用户 {} 的连接", steamIDString);
                    return false;
                }
            }
            
            // 使用Steam P2P API发送消息
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                // 将消息转换为字节数组
                byte[] messageBytes = message.getBytes("UTF-8");
                
                // 发送P2P数据包 - 模仿C++示例的发送逻辑
                ByteBuffer messageBuffer = ByteBuffer.allocateDirect(messageBytes.length);
                messageBuffer.put(messageBytes);
                messageBuffer.flip();
                
                // 根据消息类型选择发送方式
                SteamNetworking.P2PSend sendType = determineSendType(message);
                
                boolean result = steamNetworking.sendP2PPacket(steamID, 
                    messageBuffer, 
                    sendType, 
                    0);
                
                if (result) {
                    logger.info("✅ [P2P发送] 消息已发送给用户 {}: {}", steamIDString, message);
                    return true;
                } else {
                    logger.error("❌ [P2P发送] 发送消息失败 - Steam API返回false");
                    logger.error("🔍 [P2P发送] 可能原因：1) 连接已断开 2) 网络问题 3) 消息过大");
                    return false;
                }
            } else {
                logger.error("❌ [P2P发送] Steam Networking接口不可用");
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
     * 根据消息类型确定发送方式
     * 模仿C++示例的发送标志选择逻辑
     */
    private SteamNetworking.P2PSend determineSendType(String message) {
        // 连接相关消息使用可靠传输
        if (message.startsWith("P2P_CONNECT_") || message.startsWith("P2P_DISCONNECT")) {
            return SteamNetworking.P2PSend.Reliable;
        }
        
        // 普通消息使用可靠传输（确保消息送达）
        return SteamNetworking.P2PSend.Reliable;
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
     * 模仿C++示例的ReceiveNetworkData方法
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
                    
                    // 处理不同类型的消息 - 模仿C++示例的消息类型处理
                    handleReceivedMessage(senderID, message);
                    
                    // 更新连接状态
                    connections.put(senderID, sessionState);
                    
                    // 清除连接尝试记录（连接成功）
                    connectionAttempts.remove(senderID);
                }
                
                // 检查是否还有更多数据包
                packetSize = steamNetworking.isP2PPacketAvailable(0);
            }
            
            // 检查连接超时
            checkConnectionTimeouts();
            
        } catch (Exception e) {
            logger.error("💥 处理接收数据包时发生错误", e);
        }
    }
    
    /**
     * 处理接收到的消息
     * 模仿C++示例的消息类型处理逻辑
     */
    private void handleReceivedMessage(SteamID senderID, String message) {
        try {
            // 处理连接请求
            if ("P2P_CONNECT_REQUEST".equals(message)) {
                logger.info("📨 [P2P接收] 收到来自 {} 的连接请求", senderID);
                handleConnectionRequest(senderID);
                return;
            }
            
            // 处理连接确认
            if ("P2P_CONNECT_ACCEPT".equals(message)) {
                logger.info("✅ [P2P接收] 收到来自 {} 的连接确认", senderID);
                handleConnectionAccept(senderID);
                return;
            }
            
            // 处理连接拒绝
            if ("P2P_CONNECT_REJECT".equals(message)) {
                logger.warn("❌ [P2P接收] 收到来自 {} 的连接拒绝", senderID);
                handleConnectionReject(senderID);
                return;
            }
            
            // 处理断开连接消息
            if ("P2P_DISCONNECT".equals(message)) {
                logger.info("🔌 [P2P接收] 收到来自 {} 的断开连接消息", senderID);
                handleDisconnectMessage(senderID);
                return;
            }
            
            // 处理ping消息
            if (message.startsWith("P2P_PING:")) {
                logger.debug("📡 [P2P接收] 收到来自 {} 的ping消息", senderID);
                networkUtils.handlePingRequest(senderID, message);
                return;
            }
            
            // 处理pong消息
            if (message.startsWith("P2P_PONG:")) {
                logger.debug("📡 [P2P接收] 收到来自 {} 的pong消息", senderID);
                networkUtils.handlePingResponse(senderID, message);
                return;
            }
            
            // 处理普通消息
            logger.debug("📨 [P2P接收] 收到来自 {} 的普通消息: {}", senderID, message);
            
            // 通知消息监听器
            for (MessageListener listener : messageListeners) {
                try {
                    listener.onMessageReceived(senderID, message);
                } catch (Exception e) {
                    logger.error("💥 消息监听器处理错误", e);
                }
            }
            
        } catch (Exception e) {
            logger.error("💥 处理接收消息时发生错误", e);
        }
    }
    
    /**
     * 处理连接请求
     */
    private void handleConnectionRequest(SteamID senderID) {
        try {
            logger.info("🔗 [P2P接收] 处理来自 {} 的连接请求", senderID);
            
            // 自动接受连接请求 - 模仿C++示例的自动接受逻辑
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                // 发送连接确认
                String acceptMessage = "P2P_CONNECT_ACCEPT";
                ByteBuffer messageBuffer = ByteBuffer.allocateDirect(acceptMessage.getBytes().length);
                messageBuffer.put(acceptMessage.getBytes());
                messageBuffer.flip();
                
                boolean result = steamNetworking.sendP2PPacket(senderID, 
                    messageBuffer, 
                    SteamNetworking.P2PSend.Reliable, 
                    0);
                
                if (result) {
                    logger.info("✅ [P2P接收] 已发送连接确认给 {}", senderID);
                    // 添加到活跃连接
                    steamService.addActiveConnection(senderID);
                    
                    // 通知连接状态监听器
                    for (ConnectionStateListener listener : connectionListeners) {
                        try {
                            listener.onConnectionEstablished(senderID);
                        } catch (Exception e) {
                            logger.error("💥 连接状态监听器处理错误", e);
                        }
                    }
                } else {
                    logger.error("❌ [P2P接收] 发送连接确认失败");
                }
            }
            
        } catch (Exception e) {
            logger.error("💥 处理连接请求时发生错误", e);
        }
    }
    
    /**
     * 处理连接确认
     */
    private void handleConnectionAccept(SteamID senderID) {
        logger.info("✅ [P2P连接] 连接已建立: {}", senderID);
        
        // 添加到活跃连接
        steamService.addActiveConnection(senderID);
        
        // 通知连接状态监听器
        for (ConnectionStateListener listener : connectionListeners) {
            try {
                listener.onConnectionEstablished(senderID);
            } catch (Exception e) {
                logger.error("💥 连接状态监听器处理错误", e);
            }
        }
    }
    
    /**
     * 处理连接拒绝
     */
    private void handleConnectionReject(SteamID senderID) {
        logger.warn("❌ [P2P连接] 连接被拒绝: {}", senderID);
        
        // 通知连接状态监听器
        for (ConnectionStateListener listener : connectionListeners) {
            try {
                listener.onConnectionFailed(senderID);
            } catch (Exception e) {
                logger.error("💥 连接状态监听器处理错误", e);
            }
        }
    }
    
    /**
     * 处理断开连接消息
     */
    private void handleDisconnectMessage(SteamID senderID) {
        logger.info("🔌 [P2P接收] 处理来自 {} 的断开连接", senderID);
        
        // 从连接列表中移除
        connections.remove(senderID);
        connectionAttempts.remove(senderID);
        
        // 从活跃连接中移除
        steamService.removeActiveConnection(senderID);
        
        // 通知连接状态监听器
        for (ConnectionStateListener listener : connectionListeners) {
            try {
                listener.onConnectionLost(senderID);
            } catch (Exception e) {
                logger.error("💥 连接状态监听器处理错误", e);
            }
        }
    }
    
    /**
     * 检查连接超时
     */
    private void checkConnectionTimeouts() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<SteamID, Long> entry : connectionAttempts.entrySet()) {
            SteamID steamID = entry.getKey();
            long attemptTime = entry.getValue();
            
            if (currentTime - attemptTime > CONNECTION_TIMEOUT) {
                logger.warn("⏰ [P2P连接] 连接超时: {}", steamID);
                
                // 移除超时的连接尝试
                connectionAttempts.remove(steamID);
                
                // 通知连接状态监听器
                for (ConnectionStateListener listener : connectionListeners) {
                    try {
                        listener.onConnectionFailed(steamID);
                    } catch (Exception e) {
                        logger.error("💥 连接状态监听器处理错误", e);
                    }
                }
            }
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
    
    /**
     * 获取网络状态信息
     */
    public String getNetworkStatus() {
        return networkUtils.getConnectionStats();
    }
    
    /**
     * 获取网络连接质量
     */
    public String getNetworkQuality() {
        return networkUtils.getNetworkQuality();
    }
}
