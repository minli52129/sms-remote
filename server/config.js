module.exports = {
  port: 3000,
  // 认证 Token，请修改为自己的安全 token
  authToken: 'change-me-to-a-secure-token',
  // 数据库文件路径
  dbPath: './data/sms.db',
  // 心跳超时时间（毫秒）
  heartbeatTimeout: 60000,
  // Web 界面访问密码（可选）
  webPassword: 'admin123'
};
