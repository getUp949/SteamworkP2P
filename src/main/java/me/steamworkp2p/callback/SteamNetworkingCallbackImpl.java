package me.steamworkp2p.callback;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import me.steamworkp2p.service.SteamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

/**
 * Steam Networking 回调实现类
 * 处理P2P连接状态变化
 */
@Component
public class SteamNetworkingCallbackImpl implements SteamNetworkingCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SteamNetworkingCallbackImpl.class);
    
    private SteamService steamService;
    
    /**
     * 设置SteamService依赖
     */
    public void setSteamService(SteamService steamService) {
        this.steamService = steamService;
    }
    
    @Override
    public void onP2PSessionConnectFail(SteamID steamIDRemote, SteamNetworking.P2PSessionError sessionError) {
        logger.warn("❌ [P2P回调] P2P连接失败: RemoteID={}, Error={}", steamIDRemote, sessionError);
        
        // 从活跃连接中移除
        steamService.removeActiveConnection(steamIDRemote);
        
        // 记录连接失败原因
        String errorMessage = getErrorMessage(sessionError);
        logger.error("🔍 [P2P回调] 连接失败原因: {}", errorMessage);
    }
    
    /**
     * 获取连接错误信息
     * 模仿C++示例的错误处理逻辑
     */
    private String getErrorMessage(SteamNetworking.P2PSessionError sessionError) {
        switch (sessionError) {
            case None:
                return "无错误";
            case NotRunningApp:
                return "目标用户未运行此应用";
            case NoRightsToApp:
                return "无权限访问此应用";
            case DestinationNotLoggedIn:
                return "目标用户未登录Steam";
            case Timeout:
                return "连接超时";
            default:
                return "未知错误: " + sessionError;
        }
    }
    
    @Override
    public void onP2PSessionRequest(SteamID steamIDRemote) {
        System.out.println("🔧 [P2P接收] 测试控制台输出 - 收到P2P连接请求: " + steamIDRemote);
        logger.info("📨 [P2P接收] 收到P2P连接请求: RemoteID={}", steamIDRemote);
        logger.info("🔍 [P2P接收] 当前时间: {}", System.currentTimeMillis());
        logger.info("🔍 [P2P接收] 当前Steam用户: {}", steamService.getCurrentUserName());
        
        try {
            // 检查是否已经连接 - 模仿C++示例的连接检查逻辑
            if (steamService.hasActiveConnection(steamIDRemote)) {
                logger.warn("⚠️ [P2P接收] 与用户 {} 的连接已存在，忽略重复请求", steamIDRemote);
                return;
            }
            
            // 自动接受连接请求
            SteamNetworking steamNetworking = steamService.getNetworking();
            if (steamNetworking != null) {
                logger.info("🔍 [P2P接收] Steam Networking接口可用，准备接受连接");
                
                boolean accepted = steamNetworking.acceptP2PSessionWithUser(steamIDRemote);
                if (accepted) {
                    logger.info("✅ [P2P接收] 已自动接受来自 {} 的连接请求", steamIDRemote);
                    logger.info("🔍 [P2P接收] 连接已建立，添加到活跃连接列表");
                    
                    // 添加到活跃连接
                    steamService.addActiveConnection(steamIDRemote);
                    logger.info("🔍 [P2P接收] 当前活跃连接数: {}", steamService.getActiveConnections().size());
                    
                    // 发送连接确认消息给发送者 - 模仿C++示例的确认消息
                    try {
                        String confirmMessage = "P2P_CONNECT_ACCEPT";
                        ByteBuffer messageBuffer = ByteBuffer.allocateDirect(confirmMessage.getBytes().length);
                        messageBuffer.put(confirmMessage.getBytes());
                        messageBuffer.flip();
                        
                        boolean sent = steamNetworking.sendP2PPacket(steamIDRemote, messageBuffer, 
                            SteamNetworking.P2PSend.Reliable, 0);
                        
                        if (sent) {
                            logger.info("📤 [P2P接收] 已发送连接确认消息给发送者: {}", steamIDRemote);
                        } else {
                            logger.warn("⚠️ [P2P接收] 发送连接确认消息失败");
                        }
                    } catch (Exception e) {
                        logger.warn("⚠️ [P2P接收] 发送连接确认消息时发生错误: {}", e.getMessage());
                    }
                } else {
                    logger.warn("❌ [P2P接收] 接受连接请求失败: RemoteID={}", steamIDRemote);
                    logger.warn("🔍 [P2P接收] 可能原因：1) 连接已存在 2) Steam API错误 3) 网络问题");
                    
                    // 发送连接拒绝消息
                    try {
                        String rejectMessage = "P2P_CONNECT_REJECT";
                        ByteBuffer messageBuffer = ByteBuffer.allocateDirect(rejectMessage.getBytes().length);
                        messageBuffer.put(rejectMessage.getBytes());
                        messageBuffer.flip();
                        
                        steamNetworking.sendP2PPacket(steamIDRemote, messageBuffer, 
                            SteamNetworking.P2PSend.Reliable, 0);
                        logger.info("📤 [P2P接收] 已发送连接拒绝消息给发送者: {}", steamIDRemote);
                    } catch (Exception e) {
                        logger.warn("⚠️ [P2P接收] 发送连接拒绝消息时发生错误: {}", e.getMessage());
                    }
                }
            } else {
                logger.error("❌ [P2P接收] SteamNetworking未初始化，无法处理连接请求");
            }
        } catch (Exception e) {
            logger.error("💥 [P2P接收] 处理P2P连接请求时发生错误", e);
        }
    }
}
