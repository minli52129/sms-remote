const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');
const config = require('./config');
const db = require('./db');
const apiRouter = require('./routes/api');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

// 中间件
app.use(express.json());
app.use(express.static(path.join(__dirname, '../web')));

// API 路由
app.use('/api', apiRouter);

// 存储连接的设备
const androidDevices = new Map();
const webClients = new Map();

// Socket.IO 连接处理
io.on('connection', (socket) => {
  console.log(`客户端连接: ${socket.id}`);

  // Android 设备认证
  socket.on('auth', (data) => {
    if (data.token !== config.authToken) {
      socket.emit('auth_failed');
      socket.disconnect();
      return;
    }

    socket.deviceType = 'android';
    socket.deviceId = data.deviceId || socket.id;
    androidDevices.set(socket.deviceId, socket);
    db.updateDevice(socket.deviceId, 1);

    socket.emit('auth_success', { deviceId: socket.deviceId });
    console.log(`Android 设备已认证: ${socket.deviceId}`);

    // 通知 Web 客户端设备上线
    broadcastToWeb('device_online', { deviceId: socket.deviceId });
  });

  // Web 客户端认证
  socket.on('web_auth', (data) => {
    if (data.password !== config.webPassword) {
      socket.emit('auth_failed');
      socket.disconnect();
      return;
    }

    socket.deviceType = 'web';
    webClients.set(socket.id, socket);
    socket.emit('auth_success');
    console.log(`Web 客户端已认证: ${socket.id}`);

    // 发送当前设备列表
    const devices = Array.from(androidDevices.keys()).map(id => ({
      deviceId: id,
      isOnline: true
    }));
    socket.emit('devices_list', devices);
  });

  // Web 发送短信指令
  socket.on('send_sms', (data) => {
    const { to, content, msgId, simSlot } = data;
    const targetDevice = Array.from(androidDevices.values())[0];

    if (!targetDevice) {
      socket.emit('sms_result', {
        msgId,
        success: false,
        error: '无在线设备'
      });
      return;
    }

    // 保存到数据库
    db.insertMessage({
      msgId,
      direction: 'outbound',
      phoneNumber: to,
      content,
      status: 'sending',
      simSlot
    });

    // 转发到 Android 设备
    targetDevice.emit('send_sms', {
      to,
      content,
      msgId,
      simSlot: simSlot || 0
    });
  });

  // Android 上报发送结果
  socket.on('sms_result', (data) => {
    const { msgId, success, error } = data;
    db.updateMessageStatus(msgId, success ? 'sent' : 'failed');
    broadcastToWeb('sms_result', data);
  });

  // Android 上报收到短信
  socket.on('sms_received', (data) => {
    const { from, content, timestamp, simSlot, carrierName } = data;

    db.insertMessage({
      msgId: `in_${timestamp}`,
      direction: 'inbound',
      phoneNumber: from,
      content,
      status: 'received',
      simSlot,
      carrierName
    });

    broadcastToWeb('sms_received', data);
  });

  // Android 上报 SIM 卡信息
  socket.on('sim_info', (data) => {
    broadcastToWeb('sim_info', data);
  });

  // 心跳
  socket.on('ping', () => {
    socket.emit('pong');
  });

  // 断开连接
  socket.on('disconnect', () => {
    if (socket.deviceType === 'android') {
      androidDevices.delete(socket.deviceId);
      db.updateDevice(socket.deviceId, 0);
      broadcastToWeb('device_offline', { deviceId: socket.deviceId });
      console.log(`Android 设备断开: ${socket.deviceId}`);
    } else if (socket.deviceType === 'web') {
      webClients.delete(socket.id);
      console.log(`Web 客户端断开: ${socket.id}`);
    }
  });
});

// 广播给所有 Web 客户端
function broadcastToWeb(event, data) {
  webClients.forEach(client => {
    client.emit(event, data);
  });
}

// 启动服务器
server.listen(config.port, '0.0.0.0', () => {
  console.log(`短信远程服务已启动: http://0.0.0.0:${config.port}`);
  console.log(`Web 界面: http://120.48.127.198:${config.port}`);
});
