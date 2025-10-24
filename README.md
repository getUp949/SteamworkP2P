# Steam P2P 网络程序

这是一个基于Steam Spacewar (AppID: 480) 实现的P2P网络程序，支持任意手动添加到Steam库中的游戏进行P2P连接。

## 功能特性

- ✅ 基于Steam Spacewar (AppID: 480) 实现P2P连接
- ✅ 支持任意手动添加到Steam库中的游戏
- ✅ Web界面支持
- ✅ 实时消息传输
- ✅ 连接状态监控
- ✅ 多用户连接管理

## 系统要求

- Java 17 或更高版本
- Steam客户端 (必须正在运行)
- Windows操作系统 (Steamworks4j依赖)
- 已安装的游戏需要手动添加到Steam库中

## 安装和运行

### 1. 环境准备

确保您的系统已安装：
- Java 17+
- Maven 3.6+
- Steam客户端 (完整版本需要)

### 2. 快速开始 - Web版本 (推荐)

Web版本提供了现代化的浏览器界面，支持实时通信：

```bash
# Windows用户
run-web.bat

# 或者直接使用Maven
mvn spring-boot:run
```

启动后，在浏览器中访问: http://localhost:8080

Web版本特性：
- 现代化的响应式界面
- 实时WebSocket通信
- 支持多用户同时访问
- 现代化浏览器界面

### 3. 完整版本运行

**重要**: 在运行完整版本之前，请确保Steam客户端正在运行并已登录。

#### 3.1 启动Steam客户端

确保Steam客户端正在运行并已登录您的账户。

#### 3.2 添加游戏到Steam库

1. 打开Steam客户端
2. 点击"游戏" -> "添加非Steam游戏到我的库"
3. 选择您想要通过P2P连接的游戏
4. 点击"添加所选程序"

#### 3.3 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行完整版本
mvn spring-boot:run
```


## 使用说明

### 1. 启动程序

程序启动后会自动：
- 初始化Steam API
- 显示当前Steam用户信息
- 准备P2P网络功能

### 2. 开始监听

点击"开始监听"按钮启动P2P监听模式，等待其他用户连接。

### 3. 连接到其他用户

1. 在"Steam ID"输入框中输入目标用户的Steam ID
2. 点击"连接"按钮发起连接请求

### 4. 发送消息

1. 在"目标Steam ID"输入框中输入接收方的Steam ID
2. 在消息输入框中输入要发送的消息
3. 点击"发送"按钮发送消息

### 5. 查看连接状态

连接表格会显示当前所有活跃的P2P连接及其状态。

## 技术架构

- **后端**: Spring Boot + Steamworks4j
- **前端**: Thymeleaf + WebSocket
- **网络**: Steam P2P Networking
- **依赖管理**: Maven

## 项目结构

```
src/main/java/me/steamworkp2p/
├── WebApplication.java            # Web应用程序主类
├── controller/
│   └── WebController.java         # Web控制器
├── service/
│   ├── SteamService.java          # Steam API服务
│   └── P2PNetworkService.java    # P2P网络服务
└── model/
    └── ConnectionInfo.java        # 连接信息模型

src/main/resources/
├── templates/
│   └── index.html                 # 主页面模板
└── application.properties         # 应用配置
```

## 故障排除

### 常见问题

1. **Steam API初始化失败**
   - 确保Steam客户端正在运行
   - 确保已登录Steam账户
   - 检查防火墙设置

2. **无法连接到其他用户**
   - 确保目标用户也在运行此程序
   - 检查网络连接
   - 验证Steam ID是否正确

3. **消息发送失败**
   - 确保目标用户已连接
   - 检查连接状态
   - 重试发送

### 日志查看

程序运行时会输出详细的日志信息，包括：
- Steam API初始化状态
- P2P连接状态
- 消息传输状态
- 错误信息

## 开发说明

### 添加新功能

1. 在相应的服务类中添加业务逻辑
2. 在Web控制器中添加API接口
3. 更新HTML模板以添加新的UI组件

### 自定义配置

可以通过修改 `application.properties` 文件来自定义：
- Steam App ID
- 日志级别
- 其他应用配置

## 许可证

本项目仅供学习和研究使用。使用Steam API需要遵守Steam服务条款。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 联系方式

如有问题或建议，请通过GitHub Issues联系。
