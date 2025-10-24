package me.steamworkp2p.callback;

import com.codedisaster.steamworks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steam好友回调实现类
 * 处理Steam好友相关的事件回调
 */
public class SteamFriendsCallbackImpl implements SteamFriendsCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamFriendsCallbackImpl.class);
    
    @Override
    public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result) {
        if (success && localSuccess) {
            logger.debug("👤 设置个人名称成功");
        } else {
            logger.warn("⚠️ 设置个人名称失败: {}", result);
        }
    }
    
    @Override
    public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange change) {
        logger.debug("👥 好友状态变化: SteamID={}, Change={}", steamID, change);
    }
    
    @Override
    public void onGameOverlayActivated(boolean active) {
        logger.debug("🎮 游戏覆盖层状态: {}", active ? "激活" : "停用");
    }
    
    @Override
    public void onGameServerChangeRequested(String server, String password) {
        logger.debug("🔄 游戏服务器变更请求: Server={}", server);
    }
    
    @Override
    public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
        logger.debug("🏠 游戏大厅加入请求: Lobby={}, Friend={}", steamIDLobby, steamIDFriend);
    }
    
    @Override
    public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
        logger.debug("🖼️ 头像图片加载完成: SteamID={}, Image={}, Size={}x{}", 
                    steamID, image, width, height);
    }
    
    @Override
    public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
        logger.debug("📊 好友丰富状态更新: Friend={}, AppID={}", steamIDFriend, appID);
    }
    
    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
        logger.debug("🎯 游戏丰富状态加入请求: Friend={}, Connect={}", steamIDFriend, connect);
    }
}
