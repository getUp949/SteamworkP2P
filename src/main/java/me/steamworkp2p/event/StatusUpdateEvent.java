package me.steamworkp2p.event;

import org.springframework.context.ApplicationEvent;

/**
 * 状态更新事件
 */
public class StatusUpdateEvent extends ApplicationEvent {
    
    public StatusUpdateEvent(Object source) {
        super(source);
    }
}
