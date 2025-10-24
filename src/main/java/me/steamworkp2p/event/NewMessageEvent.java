package me.steamworkp2p.event;

import org.springframework.context.ApplicationEvent;

/**
 * 新消息事件
 */
public class NewMessageEvent extends ApplicationEvent {
    
    private final String fromSteamId;
    private final String toSteamId;
    private final String message;
    
    public NewMessageEvent(Object source, String fromSteamId, String toSteamId, String message) {
        super(source);
        this.fromSteamId = fromSteamId;
        this.toSteamId = toSteamId;
        this.message = message;
    }
    
    public String getFromSteamId() {
        return fromSteamId;
    }
    
    public String getToSteamId() {
        return toSteamId;
    }
    
    public String getMessage() {
        return message;
    }
}
