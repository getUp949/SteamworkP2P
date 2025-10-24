package me.steamworkp2p;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Web版本启动类
 * 启动Spring Boot Web应用
 */
@SpringBootApplication
public class WebApplication {
    
    public static void main(String[] args) {
        System.out.println("启动Steam P2P Web应用...");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("按Ctrl+C停止应用");
        
        SpringApplication.run(WebApplication.class, args);
    }
}
