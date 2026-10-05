const socket = io();
let isAuthenticated = false;
let simCards = [];

// 连接状态
socket.on('connect', () => {
  updateConnectionStatus(true, '已连接');
});

socket.on('disconnect', () => {
  updateConnectionStatus(false, '未连接');
});

// 认证
socket.on('auth_success', () => {
  isAuthenticated = true;
  document.getElementById('loginPanel').classList.add('hidden');
  document.getElementById('mainPanel').classList.remove('hidden');
  loadMessages();
});

socket.on('auth_failed', () => {
  alert('认证失败，请检查密码');
});

// 设备列表
socket.on('devices_list', (devices) => {
  renderDevices(devices);
});

socket.on('device_online', (data) => {
  addDevice(data.deviceId);
});

socket.on('device_offline', (data) => {
  updateDeviceStatus(data.deviceId, false);
});

// SIM 卡信息
socket.on('sim_info', (data) => {
  simCards = data.sims;
  renderSimCards();
  updateSimSelect();
});

// 短信结果
socket.on('sms_result', (data) => {
  if (data.success) {
    showNotification('短信发送成功');
  } else {
    showNotification('发送失败: ' + (data.error || '未知错误'), 'error');
  }
  loadMessages();
});

// 收到短信
socket.on('sms_received', (data) => {
  showNotification('收到新短信');
  loadMessages();
});

// 登录
function login() {
  const password = document.getElementById('passwordInput').value;
  socket.emit('web_auth', { password });
}

// 发送短信
function sendSms() {
  const phone = document.getElementById('phoneInput').value.trim();
  const content = document.getElementById('contentInput').value.trim();
  const simSlot = parseInt(document.getElementById('simSelect').value);

  if (!phone || !content) {
    alert('请填写完整信息');
    return;
  }

  const msgId = 'web_' + Date.now();
  socket.emit('send_sms', { to: phone, content, msgId, simSlot });

  document.getElementById('contentInput').value = '';
}

// 加载消息列表
async function loadMessages() {
  try {
    const res = await fetch('/api/messages?limit=50');
    const data = await res.json();
    if (data.success) {
      renderMessages(data.data);
    }
  } catch (e) {
    console.error('加载消息失败', e);
  }
}

// 渲染设备列表
function renderDevices(devices) {
  const container = document.getElementById('deviceList');
  container.innerHTML = devices.map(d => `
    <div class="device-item">
      <span>设备 ${d.deviceId}</span>
      <span class="badge ${d.isOnline ? 'badge-online' : 'badge-offline'}">
        ${d.isOnline ? '在线' : '离线'}
      </span>
    </div>
  `).join('');
}

function addDevice(deviceId) {
  const container = document.getElementById('deviceList');
  if (!container.innerHTML.includes(deviceId)) {
    container.innerHTML += `
      <div class="device-item">
        <span>设备 ${deviceId}</span>
        <span class="badge badge-online">在线</span>
      </div>
    `;
  }
}

function updateDeviceStatus(deviceId, isOnline) {
  renderDevices([{ deviceId, isOnline }]);
}

// 渲染 SIM 卡信息
function renderSimCards() {
  const container = document.getElementById('simList');
  container.innerHTML = simCards.map(sim => `
    <div class="sim-item">
      <span>SIM ${sim.slot + 1} - ${sim.carrierName}</span>
      <span>${sim.number || '未知号码'}</span>
    </div>
  `).join('');
}

// 更新 SIM 卡选择框
function updateSimSelect() {
  const select = document.getElementById('simSelect');
  select.innerHTML = simCards.map(sim => `
    <option value="${sim.slot}">SIM ${sim.slot + 1} - ${sim.carrierName}</option>
  `).join('');
}

// 渲染消息列表
function renderMessages(messages) {
  const container = document.getElementById('messageList');
  container.innerHTML = messages.map(msg => `
    <div class="message-item ${msg.direction === 'inbound' ? 'message-inbound' : 'message-outbound'}">
      <div class="message-header">
        <span>${msg.direction === 'inbound' ? '来自' : '发送至'}: ${msg.phone_number}</span>
        <span>${new Date(msg.created_at).toLocaleString()}</span>
      </div>
      <div class="message-content">${escapeHtml(msg.content)}</div>
    </div>
  `).join('');
}

// 更新连接状态
function updateConnectionStatus(connected, text) {
  const dot = document.getElementById('connectionStatus');
  const statusText = document.getElementById('connectionText');
  dot.className = `status-dot ${connected ? 'connected' : 'disconnected'}`;
  statusText.textContent = text;
}

// 显示通知
function showNotification(message, type = 'success') {
  const div = document.createElement('div');
  div.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 12px 20px;
    background: ${type === 'success' ? '#4caf50' : '#f44336'};
    color: #fff;
    border-radius: 4px;
    z-index: 1000;
  `;
  div.textContent = message;
  document.body.appendChild(div);
  setTimeout(() => div.remove(), 3000);
}

// HTML 转义
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// 回车登录
document.getElementById('passwordInput').addEventListener('keypress', (e) => {
  if (e.key === 'Enter') login();
});
