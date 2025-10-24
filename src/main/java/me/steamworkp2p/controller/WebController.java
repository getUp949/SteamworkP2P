package me.steamworkp2p.controller;

import me.steamworkp2p.service.SteamP2PService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Web控制器 - 处理HTTP请求
 */
@Controller
public class WebController {
    
    @Autowired
    private SteamP2PService steamP2PService;
    
    /**
     * 主页
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Steam P2P 网络程序");
        model.addAttribute("isListening", steamP2PService.isListening());
        model.addAttribute("connections", steamP2PService.getActiveConnections());
        return "index";
    }
    
    /**
     * 获取连接状态
     */
    @GetMapping("/api/status")
    @ResponseBody
    public Map<String, Object> getStatus() {
        return Map.of(
            "isListening", steamP2PService.isListening(),
            "connections", steamP2PService.getActiveConnections(),
            "steamRunning", steamP2PService.isSteamRunning()
        );
    }
    
    /**
     * 开始监听P2P连接
     */
    @PostMapping("/api/start-listening")
    @ResponseBody
    public Map<String, Object> startListening() {
        try {
            boolean success = steamP2PService.startListening();
            return Map.of(
                "success", success,
                "message", success ? "P2P监听已启动" : "启动失败，请检查Steam连接"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "启动失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 停止监听P2P连接
     */
    @PostMapping("/api/stop-listening")
    @ResponseBody
    public Map<String, Object> stopListening() {
        try {
            steamP2PService.stopListening();
            return Map.of(
                "success", true,
                "message", "P2P监听已停止"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "停止失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 连接到指定Steam用户
     */
    @PostMapping("/api/connect")
    @ResponseBody
    public Map<String, Object> connect(@RequestParam String steamId) {
        try {
            boolean success = steamP2PService.connectToUser(steamId);
            return Map.of(
                "success", success,
                "message", success ? "连接请求已发送" : "连接失败"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "连接失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 发送消息
     */
    @PostMapping("/api/send-message")
    @ResponseBody
    public Map<String, Object> sendMessage(
            @RequestParam String steamId,
            @RequestParam String message) {
        try {
            boolean success = steamP2PService.sendMessage(steamId, message);
            return Map.of(
                "success", success,
                "message", success ? "消息已发送" : "发送失败"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 断开连接
     */
    @PostMapping("/api/disconnect")
    @ResponseBody
    public Map<String, Object> disconnect(@RequestParam String steamId) {
        try {
            steamP2PService.disconnectUser(steamId);
            return Map.of(
                "success", true,
                "message", "连接已断开"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "断开失败: " + e.getMessage()
            );
        }
    }
}
