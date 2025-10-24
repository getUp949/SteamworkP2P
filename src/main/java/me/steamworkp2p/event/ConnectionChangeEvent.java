package me.steamworkp2p.event;

import org.springframework.context.ApplicationEvent;

/**
 * 连接状态变化事件
 */
public class ConnectionChangeEvent extends ApplicationEvent {
    
    private final String steamId;
    private final boolean connected;
    
    public ConnectionChangeEvent(Object source, String steamId, boolean connected) {
        super(source);
        this.steamId = steamId;
        this.connected = connected;
    }
    
    public String getSteamId() {
        return steamId;
    }
    
    public boolean isConnected() {
        return connected;
    }
}
