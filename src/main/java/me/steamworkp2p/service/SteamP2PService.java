package me.steamworkp2p.service;

import me.steamworkp2p.event.ConnectionChangeEvent;
import me.steamworkp2p.event.NewMessageEvent;
import me.steamworkp2p.event.StatusUpdateEvent;
import me.steamworkp2p.model.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Steam P2P服务适配器
 * 为Web控制器提供统一的接口
 */
@Service
public class SteamP2PService {
    
    @Autowired
    private P2PNetworkService p2pNetworkService;
    
    @Autowired
    private SteamService steamService;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * 检查Steam是否正在运行
     */
    public boolean isSteamRunning() {
        return steamService.isInitialized();
    }
    
    /**
     * 检查是否正在监听P2P连接
     */
    public boolean isListening() {
        return p2pNetworkService.isListening();
    }
    
    /**
     * 开始监听P2P连接
     */
    public boolean startListening() {
        boolean success = p2pNetworkService.startListening();
        if (success) {
            // 发布状态更新事件
            eventPublisher.publishEvent(new StatusUpdateEvent(this));
        }
        return success;
    }
    
    /**
     * 停止监听P2P连接
     */
    public void stopListening() {
        p2pNetworkService.stopListening();
        // 发布状态更新事件
        eventPublisher.publishEvent(new StatusUpdateEvent(this));
    }
    
    /**
     * 连接到指定Steam用户
     */
    public boolean connectToUser(String steamId) {
        boolean success = p2pNetworkService.connectToUser(steamId);
        if (success) {
            // 发布连接变化事件
            eventPublisher.publishEvent(new ConnectionChangeEvent(this, steamId, true));
        }
        return success;
    }
    
    /**
     * 断开与指定用户的连接
     */
    public void disconnectUser(String steamId) {
        p2pNetworkService.disconnectFromUser(steamId);
        // 发布连接变化事件
        eventPublisher.publishEvent(new ConnectionChangeEvent(this, steamId, false));
    }
    
    /**
     * 发送消息给指定用户
     */
    public boolean sendMessage(String steamId, String message) {
        boolean success = p2pNetworkService.sendMessage(steamId, message);
        if (success) {
            // 发布新消息事件
            eventPublisher.publishEvent(new NewMessageEvent(this, "我", steamId, message));
        }
        return success;
    }
    
    /**
     * 获取当前活跃连接列表
     */
    public List<ConnectionInfo> getActiveConnections() {
        // 从P2P服务获取连接信息并转换为ConnectionInfo列表
        List<ConnectionInfo> connections = new ArrayList<>();
        
        // 这里应该从实际的P2P连接中获取信息
        // 目前返回模拟数据
        if (isListening()) {
            // 添加一些模拟连接用于演示
            connections.add(new ConnectionInfo("76561198000000001", "用户1", true));
            connections.add(new ConnectionInfo("76561198000000002", "用户2", true));
        }
        
        return connections;
    }
}
