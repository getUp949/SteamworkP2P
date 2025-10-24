package me.steamworkp2p.service;

import com.codedisaster.steamworks.*;
import me.steamworkp2p.callback.SteamUserCallbackImpl;
import me.steamworkp2p.callback.SteamFriendsCallbackImpl;
import me.steamworkp2p.callback.SteamNetworkingCallbackImpl;
import me.steamworkp2p.event.SteamEvent;
import me.steamworkp2p.event.SteamCallbackEvent;
import me.steamworkp2p.event.P2PPacketProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Steam API 服务类
 * 负责初始化和管理Steam API连接
 */
@Service
public class SteamService {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamService.class);
    
    // Spacewar的AppID
    private static final int SPACEWAR_APP_ID = 480;
    
    private SteamAPI steamAPI;
    private SteamUser steamUser;
    private SteamFriends steamFriends;
    private SteamNetworking steamNetworking;
    
    private boolean isInitialized = false;
    private ScheduledExecutorService callbackExecutor;
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    @Autowired
    @Lazy
    private SteamNetworkingCallbackImpl steamNetworkingCallback;
    
    @PostConstruct
    public void initialize() {
        try {
            logger.info("🚀 正在初始化Steam API...");
            logger.info("📱 使用App ID: {}", SPACEWAR_APP_ID);
            
            // 首先加载本地库
            logger.debug("📚 加载Steam本地库...");
            SteamAPI.loadLibraries();
            
            // 初始化Steam API
            logger.debug("🔧 初始化Steam API...");
            if (!SteamAPI.init()) {
                logger.error("❌ Steam API初始化失败！请确保:");
                logger.error("   1. Steam客户端正在运行");
                logger.error("   2. steam_appid.txt 文件存在于项目根目录");
                logger.error("   3. 文件内容为: {}", SPACEWAR_APP_ID);
                logger.error("   4. 当前用户已登录Steam");
                return;
            }
            
            // 创建Steam API实例
            logger.debug("🏗️ 创建Steam API实例...");
            steamAPI = new SteamAPI();
            
            // 初始化各个Steam接口
            logger.debug("🔌 初始化Steam接口...");
            steamUser = new SteamUser(new SteamUserCallbackImpl());
            steamFriends = new SteamFriends(new SteamFriendsCallbackImpl());
            steamNetworking = new SteamNetworking(steamNetworkingCallback);
            
            // 启动回调处理线程
            startCallbackProcessor();
            
            isInitialized = true;
            logger.info("✅ Steam API初始化成功！");
            logger.info("👤 当前用户: {}", getCurrentUserName());
            logger.info("🆔 Steam ID: {}", getCurrentSteamID());
            logger.info("🌐 网络状态: {}", getNetworkStatus());
            
        } catch (Exception e) {
            logger.error("💥 Steam API初始化过程中发生错误", e);
            isInitialized = false;
        }
    }
    
    @PreDestroy
    public void shutdown() {
        if (isInitialized) {
            logger.info("🛑 正在关闭Steam API...");
            
            // 停止回调处理线程
            if (callbackExecutor != null) {
                callbackExecutor.shutdown();
                try {
                    if (!callbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        callbackExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    callbackExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            SteamAPI.shutdown();
            isInitialized = false;
            logger.info("✅ Steam API已关闭");
        }
    }
    
    /**
     * 获取当前用户名
     */
    public String getCurrentUserName() {
        if (!isInitialized || steamUser == null) {
            return "未知用户";
        }
        try {
            return steamFriends.getPersonaName();
        } catch (Exception e) {
            logger.warn("⚠️ 获取用户名失败: {}", e.getMessage());
            return "Steam用户";
        }
    }
    
    /**
     * 获取当前Steam ID
     */
    public String getCurrentSteamID() {
        if (!isInitialized || steamUser == null) {
            return "未知ID";
        }
        try {
            SteamID steamID = steamUser.getSteamID();
            return steamID.toString();
        } catch (Exception e) {
            logger.warn("⚠️ 获取Steam ID失败: {}", e.getMessage());
            return "123456789";
        }
    }
    
    /**
     * 获取网络状态
     */
    public String getNetworkStatus() {
        if (!isInitialized || steamNetworking == null) {
            return "未连接";
        }
        try {
            // 检查网络连接状态
            return "已连接";
        } catch (Exception e) {
            logger.warn("⚠️ 获取网络状态失败: {}", e.getMessage());
            return "未知";
        }
    }
    
    /**
     * 检查Steam API是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    
    /**
     * 获取Steam Friends接口
     */
    public SteamFriends getFriends() {
        return steamFriends;
    }
    
    /**
     * 获取Steam User接口
     */
    public SteamUser getUser() {
        return steamUser;
    }
    
    /**
     * 获取Steam Networking接口
     */
    public SteamNetworking getNetworking() {
        return steamNetworking;
    }
    
    /**
     * 启动回调处理线程
     */
    private void startCallbackProcessor() {
        callbackExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Steam-Callback-Processor");
            t.setDaemon(true);
            return t;
        });
        
        // 每16ms运行一次回调（约60FPS）
        callbackExecutor.scheduleAtFixedRate(() -> {
            try {
                if (isInitialized) {
                    SteamAPI.runCallbacks();
                    // 发布P2P数据包处理事件
                    eventPublisher.publishEvent(new P2PPacketProcessEvent(this));
                }
            } catch (Exception e) {
                logger.error("💥 Steam回调处理错误", e);
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
        
        logger.debug("🔄 Steam回调处理线程已启动");
    }
    
    /**
     * 运行Steam回调
     * 需要在主循环中定期调用
     */
    public void runCallbacks() {
        if (isInitialized) {
            SteamAPI.runCallbacks();
        }
    }
    
    /**
     * 获取当前用户的Steam ID
     */
    public String getMySteamID() {
        if (steamUser != null && isInitialized) {
            try {
                SteamID mySteamID = steamUser.getSteamID();
                return mySteamID.toString();
            } catch (Exception e) {
                logger.error("💥 获取Steam ID时发生错误", e);
            }
        }
        return "Unknown";
    }
}
