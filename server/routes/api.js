const express = require('express');
const db = require('../db');

const router = express.Router();

// 获取消息列表
router.get('/messages', (req, res) => {
  const { limit = 100, offset = 0 } = req.query;
  const messages = db.getMessages(parseInt(limit), parseInt(offset));
  res.json({ success: true, data: messages });
});

// 获取设备列表
router.get('/devices', (req, res) => {
  const devices = db.getDevices();
  res.json({ success: true, data: devices });
});

module.exports = router;
