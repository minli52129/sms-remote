# 短信远程收发系统

基于 WebSocket 的安卓短信远程收发系统，支持双卡。

## 架构

```
Web界面 ◄──HTTP/WS──► 中间服务 ◄──WebSocket──► Android应用
```

## 目录结构

```
sms-remote/
├── android/          # Android 项目
├── server/           # Node.js 中间服务
└── web/              # Web 界面
```

## 快速开始

### 1. 启动中间服务

```bash
cd server
npm install
npm start
```

### 2. 访问 Web 界面

浏览器打开: http://ip:3000

默认密码: `自己设置`

### 3. 配置 Android

1. 修改 `WebSocketManager.kt` 中的 `SERVER_URL` 和 `AUTH_TOKEN`
2. 编译安装到 Android 设备
3. 授予短信权限
4. 设为默认短信应用
5. 启动服务

## 配置说明

### 服务器配置 (server/config.js)

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| port | 服务端口 | 3000 |
| authToken | 设备认证 Token | change-me-to-a-secure-token |
| webPassword | Web 界面密码 | admin123 |

### Android 配置 (WebSocketManager.kt)

| 配置项 | 说明 |
|--------|------|
| SERVER_URL | 服务器地址 |
| AUTH_TOKEN | 认证 Token（与服务器一致） |

## 功能特性

- [x] 双卡双待支持
- [x] 实时短信收发
- [x] 短信历史记录
- [x] 设备状态监控
- [x] 前台服务保活

## 注意事项

1. Android 需要将应用设为**默认短信应用**才能接收短信
2. 建议关闭电池优化以保持服务运行
3. 生产环境请使用 WSS (WebSocket Secure)
4. 请修改默认的 authToken 和 webPassword
