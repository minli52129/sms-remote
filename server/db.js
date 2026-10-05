const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');
const config = require('./config');

// 确保数据目录存在
const dataDir = path.dirname(config.dbPath);
if (!fs.existsSync(dataDir)) {
  fs.mkdirSync(dataDir, { recursive: true });
}

const db = new Database(config.dbPath);

// 初始化数据库表
db.exec(`
  CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    msg_id TEXT UNIQUE,
    direction TEXT CHECK(direction IN ('inbound', 'outbound')),
    phone_number TEXT NOT NULL,
    content TEXT NOT NULL,
    status TEXT DEFAULT 'pending',
    sim_slot INTEGER,
    carrier_name TEXT,
    created_at INTEGER NOT NULL,
    completed_at INTEGER
  );

  CREATE TABLE IF NOT EXISTS devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT UNIQUE,
    name TEXT,
    last_seen INTEGER,
    is_online INTEGER DEFAULT 0
  );

  CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
  CREATE INDEX IF NOT EXISTS idx_messages_phone ON messages(phone_number);
`);

module.exports = {
  // 插入消息
  insertMessage: (msg) => {
    const stmt = db.prepare(`
      INSERT INTO messages (msg_id, direction, phone_number, content, status, sim_slot, carrier_name, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `);
    return stmt.run(
      msg.msgId,
      msg.direction,
      msg.phoneNumber,
      msg.content,
      msg.status || 'pending',
      msg.simSlot || null,
      msg.carrierName || null,
      Date.now()
    );
  },

  // 更新消息状态
  updateMessageStatus: (msgId, status) => {
    const stmt = db.prepare(`
      UPDATE messages SET status = ?, completed_at = ? WHERE msg_id = ?
    `);
    return stmt.run(status, Date.now(), msgId);
  },

  // 获取消息列表
  getMessages: (limit = 100, offset = 0) => {
    const stmt = db.prepare(`
      SELECT * FROM messages ORDER BY created_at DESC LIMIT ? OFFSET ?
    `);
    return stmt.all(limit, offset);
  },

  // 获取设备列表
  getDevices: () => {
    const stmt = db.prepare(`SELECT * FROM devices`);
    return stmt.all();
  },

  // 更新设备状态
  updateDevice: (deviceId, isOnline) => {
    const stmt = db.prepare(`
      INSERT INTO devices (device_id, last_seen, is_online)
      VALUES (?, ?, ?)
      ON CONFLICT(device_id) DO UPDATE SET last_seen = ?, is_online = ?
    `);
    return stmt.run(deviceId, Date.now(), isOnline, Date.now(), isOnline);
  }
};
