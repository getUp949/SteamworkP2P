package me.steamworkp2p.controller;

import me.steamworkp2p.service.P2PNetworkService;
import me.steamworkp2p.service.SteamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * P2P连接控制器
 * 提供连接状态查询和管理的REST API
 */
@RestController
@RequestMapping("/api/connection")
public class ConnectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);
    
    @Autowired
    private P2PNetworkService p2pNetworkService;
    
    @Autowired
    private SteamService steamService;
    
    /**
     * 获取当前连接状态
     */
    @GetMapping("/status")
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            status.put("steamInitialized", steamService.isInitialized());
            status.put("listening", p2pNetworkService.isListening());
            status.put("connectedUsers", p2pNetworkService.getConnections().size());
            status.put("mySteamID", steamService.getMySteamID());
            status.put("timestamp", System.currentTimeMillis());
            
            logger.debug("📊 连接状态查询: {}", status);
            
        } catch (Exception e) {
            logger.error("💥 获取连接状态时发生错误", e);
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 开始监听P2P连接
     */
    @PostMapping("/start-listening")
    public Map<String, Object> startListening() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean success = p2pNetworkService.startListening();
            result.put("success", success);
            result.put("message", success ? "P2P监听已启动" : "启动P2P监听失败");
            
            if (success) {
                logger.info("🎧 P2P监听已通过API启动");
            } else {
                logger.warn("❌ 启动P2P监听失败");
            }
            
        } catch (Exception e) {
            logger.error("💥 启动P2P监听时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 停止监听P2P连接
     */
    @PostMapping("/stop-listening")
    public Map<String, Object> stopListening() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            p2pNetworkService.stopListening();
            result.put("success", true);
            result.put("message", "P2P监听已停止");
            
            logger.info("🛑 P2P监听已通过API停止");
            
        } catch (Exception e) {
            logger.error("💥 停止P2P监听时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 连接到指定用户
     */
    @PostMapping("/connect")
    public Map<String, Object> connectToUser(@RequestParam String steamID) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean success = p2pNetworkService.connectToUser(steamID);
            result.put("success", success);
            result.put("message", success ? "连接请求已发送" : "发送连接请求失败");
            result.put("targetSteamID", steamID);
            
            if (success) {
                logger.info("📤 已发送连接请求到用户: {}", steamID);
            } else {
                logger.warn("❌ 发送连接请求失败: {}", steamID);
            }
            
        } catch (Exception e) {
            logger.error("💥 连接用户时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 断开与指定用户的连接
     */
    @PostMapping("/disconnect")
    public Map<String, Object> disconnectFromUser(@RequestParam String steamID) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            p2pNetworkService.disconnectFromUser(steamID);
            result.put("success", true);
            result.put("message", "已断开连接");
            result.put("targetSteamID", steamID);
            
            logger.info("📤 已断开与用户的连接: {}", steamID);
            
        } catch (Exception e) {
            logger.error("💥 断开连接时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 发送消息给指定用户
     */
    @PostMapping("/send-message")
    public Map<String, Object> sendMessage(@RequestParam String steamID, @RequestParam String message) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean success = p2pNetworkService.sendMessage(steamID, message);
            result.put("success", success);
            result.put("message", success ? "消息已发送" : "发送消息失败");
            result.put("targetSteamID", steamID);
            result.put("content", message);
            
            if (success) {
                logger.info("📤 已发送消息给用户 {}: {}", steamID, message);
            } else {
                logger.warn("❌ 发送消息失败: {} -> {}", steamID, message);
            }
            
        } catch (Exception e) {
            logger.error("💥 发送消息时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
