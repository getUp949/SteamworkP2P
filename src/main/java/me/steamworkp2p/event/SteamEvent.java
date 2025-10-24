package me.steamworkp2p.event;

import org.springframework.context.ApplicationEvent;

/**
 * Steam事件基类
 */
public abstract class SteamEvent extends ApplicationEvent {
    
    public SteamEvent(Object source) {
        super(source);
    }
}
