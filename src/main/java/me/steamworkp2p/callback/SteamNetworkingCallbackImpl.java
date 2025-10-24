package me.steamworkp2p.callback;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import me.steamworkp2p.service.SteamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Steam Networking 回调实现类
 * 处理P2P连接状态变化
 */
@Component
public class SteamNetworkingCallbackImpl implements SteamNetworkingCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamNetworkingCallbackImpl.class);
    
    @Autowired
    @Lazy
    private SteamService steamService;
    
    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, SteamNetworking.P2PSessionError sessionError) {
        logger.warn("❌ P2P连接失败: RemoteID={}, Error={}", steamIDRemote, sessionError);
        // 从活跃连接中移除
        steamService.removeActiveConnection(steamIDRemote);
    }
    
    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote) {
        System.out.println("🔧 测试控制台输出 - 收到P2P连接请求: " + steamIDRemote);
        logger.info("📨 收到P2P连接请求: RemoteID={}", steamIDRemote);
        
        try {
            // 自动接受连接请求
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                boolean accepted = steamNetworking.acceptP2PSessionWithUser(steamIDRemote);
                if (accepted) {
                    logger.info("✅ 已自动接受来自 {} 的连接请求", steamIDRemote);
                    // 添加到活跃连接
                    steamService.addActiveConnection(steamIDRemote);
                } else {
                    logger.warn("❌ 接受连接请求失败: RemoteID={}", steamIDRemote);
                }
            } else {
                logger.error("❌ SteamNetworking未初始化，无法处理连接请求");
            }
        } catch (Exception e) {
            logger.error("💥 处理P2P连接请求时发生错误", e);
        }
    }
}
