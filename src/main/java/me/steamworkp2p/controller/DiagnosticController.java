package me.steamworkp2p.controller;

import me.steamworkp2p.service.SteamService;
import me.steamworkp2p.service.P2PNetworkService;
import me.steamworkp2p.service.SteamP2PService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.codedisaster.steamworks.SteamID;

/**
 * 诊断控制器
 * 用于检查P2P连接状态和排查问题
 */
@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {
    
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticController.class);
    
    @Autowired
    private SteamService steamService;
    
    @Autowired
    private P2PNetworkService p2pNetworkService;
    
    @Autowired
    private SteamP2PService steamP2PService;
    
    /**
     * 获取系统状态诊断信息
     */
    @GetMapping("/status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Steam API状态
            boolean steamInitialized = steamService.isInitialized();
            status.put("steamInitialized", steamInitialized);
            
            if (steamInitialized) {
                // Steam用户信息
                String currentUser = steamService.getCurrentUserName();
                status.put("currentSteamUser", currentUser);
                
                // Steam Networking状态
                boolean networkingAvailable = steamService.getNetworking() != null;
                status.put("steamNetworkingAvailable", networkingAvailable);
            }
            
            // P2P监听状态
            boolean isListening = p2pNetworkService.isListening();
            status.put("p2pListening", isListening);
            
            // 活跃连接
            Set<SteamID> activeConnections = steamService.getActiveConnections();
            status.put("activeConnectionsCount", activeConnections.size());
            status.put("activeConnections", activeConnections.toString());
            
            // 系统时间
            status.put("timestamp", System.currentTimeMillis());
            status.put("status", "OK");
            
        } catch (Exception e) {
            logger.error("💥 获取系统状态时发生错误", e);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 测试P2P连接请求发送
     */
    @PostMapping("/test-connection")
    public Map<String, Object> testConnection(@RequestParam String steamId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            logger.info("🔍 开始诊断P2P连接请求发送...");
            
            // 检查前置条件
            if (!steamService.isInitialized()) {
                result.put("success", false);
                result.put("error", "Steam API未初始化");
                return result;
            }
            
            if (steamService.getNetworking() == null) {
                result.put("success", false);
                result.put("error", "Steam Networking不可用");
                return result;
            }
            
            // 验证Steam ID格式
            try {
                SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamId));
                result.put("steamIdValid", true);
                result.put("steamIdParsed", steamID.toString());
            } catch (NumberFormatException e) {
                result.put("success", false);
                result.put("error", "无效的Steam ID格式: " + steamId);
                return result;
            }
            
            // 尝试发送连接请求
            boolean connectionResult = steamP2PService.connectToUser(steamId);
            
            result.put("success", connectionResult);
            result.put("message", connectionResult ? "连接请求发送成功" : "连接请求发送失败");
            result.put("targetSteamId", steamId);
            result.put("timestamp", System.currentTimeMillis());
            
            if (connectionResult) {
                logger.info("✅ 诊断测试：连接请求发送成功到 {}", steamId);
            } else {
                logger.warn("❌ 诊断测试：连接请求发送失败到 {}", steamId);
            }
            
        } catch (Exception e) {
            logger.error("💥 诊断测试时发生错误", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 检查Steam用户是否在线
     */
    @GetMapping("/check-user")
    public Map<String, Object> checkUser(@RequestParam String steamId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证Steam ID
            SteamID steamID = SteamID.createFromNativeHandle(Long.parseLong(steamId));
            
            result.put("steamId", steamId);
            result.put("steamIdParsed", steamID.toString());
            result.put("timestamp", System.currentTimeMillis());
            
            // 注意：Steam API可能无法直接检查其他用户是否在线
            // 这需要Steam好友系统或游戏特定的实现
            result.put("note", "无法直接检查其他Steam用户是否在线，需要对方也运行此应用");
            
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("error", "无效的Steam ID格式: " + steamId);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取详细的连接日志
     */
    @GetMapping("/connection-logs")
    public Map<String, Object> getConnectionLogs() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取当前连接状态
            Set<SteamID> activeConnections = steamService.getActiveConnections();
            
            result.put("activeConnections", activeConnections.size());
            result.put("connectionList", activeConnections.toString());
            result.put("isListening", p2pNetworkService.isListening());
            result.put("steamInitialized", steamService.isInitialized());
            result.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
