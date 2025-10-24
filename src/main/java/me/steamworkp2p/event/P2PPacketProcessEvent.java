package me.steamworkp2p.event;

import org.springframework.context.ApplicationEvent;

/**
 * P2P数据包处理事件
 */
public class P2PPacketProcessEvent extends ApplicationEvent {
    
    public P2PPacketProcessEvent(Object source) {
        super(source);
    }
}
