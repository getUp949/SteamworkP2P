package me.steamworkp2p.callback;

import com.codedisaster.steamworks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Steam用户回调实现类
 * 处理Steam用户相关的事件回调
 */
public class SteamUserCallbackImpl implements SteamUserCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamUserCallbackImpl.class);
    
    @Override
    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
        logger.debug("🔐 验证认证票据: SteamID={}, Response={}, Owner={}", 
                    steamID, authSessionResponse, ownerSteamID);
    }
    
    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
        logger.debug("💳 微交易授权: AppID={}, OrderID={}, Authorized={}", 
                    appID, orderID, authorized);
    }
    
    @Override
    public void onEncryptedAppTicket(SteamResult result) {
        if (result == SteamResult.OK) {
            logger.debug("🔐 加密应用票据成功");
        } else {
            logger.warn("⚠️ 加密应用票据失败: {}", result);
        }
    }
}
