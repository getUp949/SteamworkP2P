package me.steamworkp2p.model;

/**
 * 连接信息模型类
 * 用于存储和传输连接信息
 */
public class ConnectionInfo {
    
    private String steamID;
    private String status;
    private String displayName;
    private boolean connected;
    
    public ConnectionInfo(String steamID, String status) {
        this.steamID = steamID;
        this.status = status;
        this.displayName = steamID;
        this.connected = "已连接".equals(status);
    }
    
    public ConnectionInfo(String steamID, String displayName, boolean connected) {
        this.steamID = steamID;
        this.status = connected ? "已连接" : "未连接";
        this.displayName = displayName;
        this.connected = connected;
    }
    
    public String getSteamID() {
        return steamID;
    }
    
    public void setSteamID(String steamID) {
        this.steamID = steamID;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
        this.status = connected ? "已连接" : "未连接";
    }
    
    @Override
    public String toString() {
        return "ConnectionInfo{" +
                "steamID='" + steamID + '\'' +
                ", status='" + status + '\'' +
                ", displayName='" + displayName + '\'' +
                ", connected=" + connected +
                '}';
    }
}
