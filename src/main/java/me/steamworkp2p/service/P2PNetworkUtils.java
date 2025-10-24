package me.steamworkp2p.service;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * P2P网络工具类
 * 模仿C++示例的网络状态检查、连接监控等功能
 */
@Component
public class P2PNetworkUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(P2PNetworkUtils.class);
    
    @Autowired
    private SteamService steamService;
    
    // 连接状态监控
    private final Map<SteamID, ConnectionMonitor> connectionMonitors = new ConcurrentHashMap<>();
    
    // 网络状态检查调度器
    private ScheduledExecutorService networkMonitor;
    
    // 网络状态检查间隔（毫秒）
    private static final long NETWORK_CHECK_INTERVAL = 5000; // 5秒
    
    // 连接超时时间（毫秒）
    private static final long CONNECTION_TIMEOUT = 30000; // 30秒
    
    /**
     * 连接监控器
     */
    private static class ConnectionMonitor {
        @SuppressWarnings("unused")
        private final SteamID steamID;
        private long lastActivityTime;
        private int pingCount;
        private long averagePing;
        
        public ConnectionMonitor(SteamID steamID) {
            this.steamID = steamID;
            this.lastActivityTime = System.currentTimeMillis();
            this.pingCount = 0;
            this.averagePing = 0;
        }
        
        public void updateActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        public boolean isTimeout() {
            return System.currentTimeMillis() - lastActivityTime > CONNECTION_TIMEOUT;
        }
        
        public void updatePing(long ping) {
            pingCount++;
            averagePing = (averagePing * (pingCount - 1) + ping) / pingCount;
        }
        
        public long getAveragePing() {
            return averagePing;
        }
    }
    
    /**
     * 启动网络监控
     */
    public void startNetworkMonitoring() {
        if (networkMonitor != null && !networkMonitor.isShutdown()) {
            logger.warn("⚠️ [P2P监控] 网络监控已在运行");
            return;
        }
        
        networkMonitor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "P2P-Network-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        // 定期检查网络状态
        networkMonitor.scheduleAtFixedRate(this::checkNetworkStatus, 
            0, NETWORK_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        
        logger.info("🔍 [P2P监控] 网络监控已启动，检查间隔: {}ms", NETWORK_CHECK_INTERVAL);
    }
    
    /**
     * 停止网络监控
     */
    public void stopNetworkMonitoring() {
        if (networkMonitor != null && !networkMonitor.isShutdown()) {
            networkMonitor.shutdown();
            try {
                if (!networkMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                    networkMonitor.shutdownNow();
                }
            } catch (InterruptedException e) {
                networkMonitor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("🛑 [P2P监控] 网络监控已停止");
        }
    }
    
    /**
     * 检查网络状态
     * 模仿C++示例的网络状态检查逻辑
     */
    private void checkNetworkStatus() {
        if (!steamService.isInitialized()) {
            return;
        }
        
        try {
            // 检查所有活跃连接
            for (SteamID steamID : steamService.getActiveConnections()) {
                ConnectionMonitor monitor = connectionMonitors.get(steamID);
                if (monitor == null) {
                    monitor = new ConnectionMonitor(steamID);
                    connectionMonitors.put(steamID, monitor);
                }
                
                // 检查连接是否超时
                if (monitor.isTimeout()) {
                    logger.warn("⏰ [P2P监控] 连接超时: {}", steamID);
                    handleConnectionTimeout(steamID);
                } else {
                    // 更新活动时间
                    monitor.updateActivity();
                    
                    // 执行ping测试
                    performPingTest(steamID, monitor);
                }
            }
            
            // 清理已断开的连接监控
            connectionMonitors.entrySet().removeIf(entry -> 
                !steamService.hasActiveConnection(entry.getKey()));
            
        } catch (Exception e) {
            logger.error("💥 [P2P监控] 网络状态检查时发生错误", e);
        }
    }
    
    /**
     * 执行ping测试
     * 模仿C++示例的ping测试逻辑
     */
    private void performPingTest(SteamID steamID, ConnectionMonitor monitor) {
        try {
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                // 发送ping消息
                String pingMessage = "P2P_PING:" + System.currentTimeMillis();
                byte[] messageBytes = pingMessage.getBytes();
                
                java.nio.ByteBuffer messageBuffer = java.nio.ByteBuffer.allocateDirect(messageBytes.length);
                messageBuffer.put(messageBytes);
                messageBuffer.flip();
                
                boolean sent = steamNetworking.sendP2PPacket(steamID, 
                    messageBuffer, 
                    SteamNetworking.P2PSend.Unreliable, 
                    0);
                
                if (sent) {
                    logger.debug("📡 [P2P监控] 已发送ping到: {}", steamID);
                }
            }
        } catch (Exception e) {
            logger.error("💥 [P2P监控] 执行ping测试时发生错误", e);
        }
    }
    
    /**
     * 处理连接超时
     */
    private void handleConnectionTimeout(SteamID steamID) {
        logger.warn("🔌 [P2P监控] 处理连接超时: {}", steamID);
        
        // 从活跃连接中移除
        steamService.removeActiveConnection(steamID);
        
        // 从监控列表中移除
        connectionMonitors.remove(steamID);
        
        // 记录超时事件
        logger.error("⏰ [P2P监控] 连接超时，已断开: {}", steamID);
    }
    
    /**
     * 处理ping响应
     * 当收到ping响应时调用
     */
    public void handlePingResponse(SteamID steamID, String pingMessage) {
        try {
            // 解析ping消息
            if (pingMessage.startsWith("P2P_PING:")) {
                long sentTime = Long.parseLong(pingMessage.substring(9));
                long currentTime = System.currentTimeMillis();
                long ping = currentTime - sentTime;
                
                // 更新连接监控
                ConnectionMonitor monitor = connectionMonitors.get(steamID);
                if (monitor != null) {
                    monitor.updatePing(ping);
                    monitor.updateActivity();
                    
                    logger.debug("📡 [P2P监控] 收到ping响应: {}ms (平均: {}ms)", 
                        ping, monitor.getAveragePing());
                }
            }
        } catch (Exception e) {
            logger.error("💥 [P2P监控] 处理ping响应时发生错误", e);
        }
    }
    
    /**
     * 处理ping请求
     * 当收到ping请求时调用
     */
    public void handlePingRequest(SteamID steamID, String pingMessage) {
        try {
            // 解析ping消息并发送响应
            if (pingMessage.startsWith("P2P_PING:")) {
                String pongMessage = "P2P_PONG:" + pingMessage.substring(9);
                byte[] messageBytes = pongMessage.getBytes();
                
                java.nio.ByteBuffer messageBuffer = java.nio.ByteBuffer.allocateDirect(messageBytes.length);
                messageBuffer.put(messageBytes);
                messageBuffer.flip();
                
                SteamNetworking steamNetworking = steamService.getNetworking();
                if (steamNetworking != null) {
                    steamNetworking.sendP2PPacket(steamID, 
                        messageBuffer, 
                        SteamNetworking.P2PSend.Unreliable, 
                        0);
                    
                    logger.debug("📡 [P2P监控] 已发送pong响应到: {}", steamID);
                }
            }
        } catch (Exception e) {
            logger.error("💥 [P2P监控] 处理ping请求时发生错误", e);
        }
    }
    
    /**
     * 获取连接统计信息
     */
    public String getConnectionStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("🔍 [P2P统计] 连接状态:\n");
        stats.append("  - 活跃连接数: ").append(steamService.getActiveConnections().size()).append("\n");
        stats.append("  - 监控连接数: ").append(connectionMonitors.size()).append("\n");
        
        for (Map.Entry<SteamID, ConnectionMonitor> entry : connectionMonitors.entrySet()) {
            SteamID steamID = entry.getKey();
            ConnectionMonitor monitor = entry.getValue();
            stats.append("  - ").append(steamID).append(": 平均ping=").append(monitor.getAveragePing()).append("ms\n");
        }
        
        return stats.toString();
    }
    
    /**
     * 检查网络连接质量
     */
    public String getNetworkQuality() {
        if (connectionMonitors.isEmpty()) {
            return "无活跃连接";
        }
        
        long totalPing = 0;
        int connectionCount = 0;
        
        for (ConnectionMonitor monitor : connectionMonitors.values()) {
            if (monitor.getAveragePing() > 0) {
                totalPing += monitor.getAveragePing();
                connectionCount++;
            }
        }
        
        if (connectionCount == 0) {
            return "连接质量未知";
        }
        
        long averagePing = totalPing / connectionCount;
        
        if (averagePing < 50) {
            return "优秀 (" + averagePing + "ms)";
        } else if (averagePing < 100) {
            return "良好 (" + averagePing + "ms)";
        } else if (averagePing < 200) {
            return "一般 (" + averagePing + "ms)";
        } else {
            return "较差 (" + averagePing + "ms)";
        }
    }
}
