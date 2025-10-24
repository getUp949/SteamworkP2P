package me.steamworkp2p.event;

import org.springframework.context.ApplicationEvent;

/**
 * Steam回调事件
 */
public class SteamCallbackEvent extends ApplicationEvent {
    
    public SteamCallbackEvent(Object source) {
        super(source);
    }
}
