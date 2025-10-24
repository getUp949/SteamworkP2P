package me.steamworkp2p.callback;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steam Networking 回调实现类
 * 处理P2P连接状态变化
 */
public class SteamNetworkingCallbackImpl implements SteamNetworkingCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamNetworkingCallbackImpl.class);
    
    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, SteamNetworking.P2PSessionError sessionError) {
        logger.warn("❌ P2P连接失败: RemoteID={}, Error={}", steamIDRemote, sessionError);
    }
    
    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote) {
        logger.info("📨 收到P2P连接请求: RemoteID={}", steamIDRemote);
        // 这里可以自动接受连接请求，或者需要用户确认
        // 目前先记录日志，实际处理在P2PNetworkService中
    }
}
