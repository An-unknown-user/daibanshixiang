const http = require('http');

const PORT = process.env.PORT || 3000;

const htmlContent = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>智能待办 - 基于时间与位置的智能提醒工具</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      background: #0f172a;
      color: #f8fafc;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 16px;
    }
    .header {
      text-align: center;
      margin-bottom: 24px;
    }
    .header h1 {
      font-size: 26px;
      font-weight: 800;
      color: #60a5fa;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
    }
    .header p {
      font-size: 14px;
      color: #94a3b8;
      margin-top: 6px;
    }
    .badge-bar {
      display: flex;
      gap: 8px;
      margin-top: 12px;
      justify-content: center;
      flex-wrap: wrap;
    }
    .tag {
      background: #1e293b;
      border: 1px solid #334155;
      padding: 4px 12px;
      border-radius: 999px;
      font-size: 12px;
      color: #38bdf8;
    }
    .tag.green { color: #34d399; border-color: #065f46; background: #064e3b33; }
    
    .phone-container {
      width: 100%;
      max-width: 420px;
      background: #1e293b;
      border: 2px solid #334155;
      border-radius: 36px;
      overflow: hidden;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7);
      display: flex;
      flex-direction: column;
      height: 780px;
      position: relative;
    }
    .phone-header {
      background: #0f172a;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #334155;
    }
    .phone-title {
      font-size: 18px;
      font-weight: 700;
      color: #ffffff;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .phone-title span {
      font-size: 11px;
      background: #2563eb;
      padding: 2px 8px;
      border-radius: 6px;
      font-weight: 600;
    }
    .phone-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .group-header {
      font-size: 13px;
      font-weight: 700;
      color: #60a5fa;
      margin-top: 6px;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .todo-card {
      background: #0f172a;
      border: 1px solid #334155;
      border-radius: 16px;
      padding: 14px;
      display: flex;
      gap: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .todo-card:hover {
      border-color: #3b82f6;
      transform: translateY(-1px);
    }
    .check-btn {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 2px solid #64748b;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 2px;
      flex-shrink: 0;
      cursor: pointer;
    }
    .check-btn.checked {
      background: #10b981;
      border-color: #10b981;
      color: white;
      font-size: 13px;
    }
    .card-body {
      flex: 1;
    }
    .card-title {
      font-size: 15px;
      font-weight: 600;
      color: #f8fafc;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .flag-icon {
      color: #ef4444;
      font-size: 14px;
    }
    .card-desc {
      font-size: 12px;
      color: #94a3b8;
      margin-top: 4px;
      line-height: 1.4;
    }
    .chips-row {
      display: flex;
      gap: 6px;
      margin-top: 8px;
      flex-wrap: wrap;
    }
    .chip {
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 6px;
      font-weight: 500;
    }
    .chip.blue { background: #1e3a8a; color: #bfdbfe; }
    .chip.cyan { background: #083344; color: #67e8f9; }
    .chip.amber { background: #451a03; color: #fde68a; }
    .chip.green { background: #064e3b; color: #a7f3d0; }
    .chip.gray { background: #1e293b; color: #94a3b8; }
    
    .bottom-nav {
      background: #0f172a;
      border-top: 1px solid #334155;
      display: flex;
      padding: 10px 0 16px 0;
    }
    .nav-tab {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      color: #64748b;
      cursor: pointer;
      font-size: 11px;
      font-weight: 500;
      transition: color 0.15s;
    }
    .nav-tab.active {
      color: #60a5fa;
      font-weight: 700;
    }
    .nav-icon {
      font-size: 18px;
    }
    
    .fab {
      position: absolute;
      right: 20px;
      bottom: 80px;
      width: 52px;
      height: 52px;
      border-radius: 50%;
      background: #ef4444;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      font-weight: 300;
      box-shadow: 0 10px 25px -5px rgba(239, 68, 68, 0.5);
      cursor: pointer;
      transition: transform 0.2s;
    }
    .fab:hover { transform: scale(1.05); }

    .smart-banner {
      background: linear-gradient(135deg, #1e3a8a, #0f172a);
      border: 1px solid #3b82f6;
      border-radius: 14px;
      padding: 12px;
      margin-bottom: 6px;
    }
    .smart-banner-title {
      font-size: 13px;
      font-weight: 700;
      color: #93c5fd;
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .smart-banner-desc {
      font-size: 11px;
      color: #cbd5e1;
      margin-top: 4px;
      line-height: 1.4;
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>🔔 智能待办 · Smart Todo App</h1>
    <p>基于时间 + 地理位置的智能待办提醒工具（Room 本地持久化 + WorkManager 路径计算）</p>
    <div class="badge-bar">
      <span class="tag green">● 服务运行正常</span>
      <span class="tag">Android Jetpack Compose</span>
      <span class="tag">Material 3</span>
      <span class="tag">Room 数据库</span>
      <span class="tag">WorkManager 周期巡检</span>
      <span class="tag">AlarmManager 精确闹钟</span>
    </div>
  </div>

  <div class="phone-container">
    <div class="phone-header">
      <div class="phone-title">
        <span>智能待办</span>
        <span id="current-tab-title" style="background:#ef4444">重要</span>
      </div>
      <div style="font-size: 12px; color: #94a3b8;">⚙ 设置</div>
    </div>

    <div class="phone-content" id="list-content">
      <!-- Banner -->
      <div class="smart-banner">
        <div class="smart-banner-title">🚗 智能最晚出发规划就绪</div>
        <div class="smart-banner-desc">系统基于实时 GPS 坐标与城市路况，当余量不足 2 小时时自动推送建议出发时间。</div>
      </div>

      <!-- Group 1 -->
      <div class="group-header">📅 今天</div>
      
      <div class="todo-card" onclick="alert('重要客户方案路演\\n时间：今日 14:30\\n地点：市中心国际会议中心 A座302\\n预估驾车：35分钟 | 公交：48分钟\\n前置准备：4项待办已完成2项')">
        <div class="check-btn"></div>
        <div class="card-body">
          <div class="card-title">
            <span class="flag-icon">🚩</span> 重要客户项目方案路演
          </div>
          <div class="card-desc">带上演示电脑、激光笔与纸质合同副本三份，现场提前15分钟调试投屏。</div>
          <div class="chips-row">
            <span class="chip blue">14:30</span>
            <span class="chip gray">📍 市中心国际会议中心</span>
            <span class="chip cyan">🚗 智能提醒 (建议出发 13:42)</span>
            <span class="chip green">📋 2/4 准备清单</span>
          </div>
        </div>
      </div>

      <!-- Group 2 -->
      <div class="group-header">📅 明天</div>

      <div class="todo-card" onclick="alert('年度健康体检\\n时间：明日 09:00\\n地点：瑞金医院\\n提醒：提前60分钟固定闹钟\\n前置准备清单：3项')">
        <div class="check-btn"></div>
        <div class="card-body">
          <div class="card-title">
            <span class="flag-icon">🚩</span> 年度健康体检预约
          </div>
          <div class="card-desc">前一天晚上8点后禁食水，带好身份证和医保卡。</div>
          <div class="chips-row">
            <span class="chip blue">09:00</span>
            <span class="chip gray">📍 瑞金医院 体检中心</span>
            <span class="chip amber">⏰ 提前1小时</span>
            <span class="chip green">📋 0/3 准备清单</span>
          </div>
        </div>
      </div>
    </div>

    <div class="fab" onclick="alert('点击了新建待办：在 Android 原生客户端上将唤起 Compose 新建面板，支持时间滚轮、高德地点选择与动态清单配置。')">+</div>

    <div class="bottom-nav">
      <div class="nav-tab active" id="tab-1" onclick="switchTab('important')">
        <div class="nav-icon">🚩</div>
        <div>重要</div>
      </div>
      <div class="nav-tab" id="tab-2" onclick="switchTab('progress')">
        <div class="nav-icon">⏱</div>
        <div>进行中</div>
      </div>
      <div class="nav-tab" id="tab-3" onclick="switchTab('completed')">
        <div class="nav-icon">✔</div>
        <div>已完成</div>
      </div>
    </div>
  </div>

  <script>
    function switchTab(type) {
      document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
      const badge = document.getElementById('current-tab-title');
      const content = document.getElementById('list-content');
      const fab = document.querySelector('.fab');

      if (type === 'important') {
        document.getElementById('tab-1').classList.add('active');
        badge.innerText = '重要';
        badge.style.background = '#ef4444';
        fab.style.display = 'flex';
        fab.style.background = '#ef4444';
        content.innerHTML = \`
          <div class="smart-banner">
            <div class="smart-banner-title">🚗 智能最晚出发规划就绪</div>
            <div class="smart-banner-desc">系统基于实时 GPS 坐标与城市路况，当余量不足 2 小时时自动推送建议出发时间。</div>
          </div>
          <div class="group-header">📅 今天</div>
          <div class="todo-card">
            <div class="check-btn"></div>
            <div class="card-body">
              <div class="card-title"><span class="flag-icon">🚩</span> 重要客户项目方案路演</div>
              <div class="card-desc">带上演示电脑、激光笔与纸质合同副本三份，现场提前15分钟调试投屏。</div>
              <div class="chips-row">
                <span class="chip blue">14:30</span>
                <span class="chip gray">📍 市中心国际会议中心</span>
                <span class="chip cyan">🚗 智能提醒 (建议出发 13:42)</span>
                <span class="chip green">📋 2/4 准备清单</span>
              </div>
            </div>
          </div>
          <div class="group-header">📅 明天</div>
          <div class="todo-card">
            <div class="check-btn"></div>
            <div class="card-body">
              <div class="card-title"><span class="flag-icon">🚩</span> 年度健康体检预约</div>
              <div class="card-desc">前一天晚上8点后禁食水，带好身份证和医保卡。</div>
              <div class="chips-row">
                <span class="chip blue">09:00</span>
                <span class="chip gray">📍 瑞金医院 体检中心</span>
                <span class="chip amber">⏰ 提前1小时</span>
                <span class="chip green">📋 0/3 准备清单</span>
              </div>
            </div>
          </div>\`;
      } else if (type === 'progress') {
        document.getElementById('tab-2').classList.add('active');
        badge.innerText = '进行中';
        badge.style.background = '#2563eb';
        fab.style.display = 'flex';
        fab.style.background = '#2563eb';
        content.innerHTML = \`
          <div class="group-header">📅 今天</div>
          <div class="todo-card">
            <div class="check-btn"></div>
            <div class="card-body">
              <div class="card-title">产品与研发双周对齐例会</div>
              <div class="card-desc">同步本季度核心里程碑推进度及下周排期。</div>
              <div class="chips-row">
                <span class="chip blue">16:00</span>
                <span class="chip gray">📍 创新空间 6F</span>
                <span class="chip amber">⏰ 提前15分钟</span>
                <span class="chip green">📋 0/2 准备清单</span>
              </div>
            </div>
          </div>\`;
      } else {
        document.getElementById('tab-3').classList.add('active');
        badge.innerText = '已完成';
        badge.style.background = '#059669';
        fab.style.display = 'none';
        content.innerHTML = \`
          <div class="group-header">📅 昨天</div>
          <div class="todo-card" style="opacity: 0.65;">
            <div class="check-btn checked">✓</div>
            <div class="card-body">
              <div class="card-title" style="text-decoration: line-through;">提交北京出差差旅发票报销</div>
              <div class="card-desc">高铁车票及酒店住宿发票已粘好送至财务。</div>
              <div class="chips-row">
                <span class="chip gray">15:00</span>
                <span class="chip gray">📍 财务部 305室</span>
                <span class="chip green">✓ 已完成</span>
              </div>
            </div>
          </div>\`;
      }
    }
  </script>
</body>
</html>`;

const server = http.createServer((req, res) => {
  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-cache'
  });
  res.end(htmlContent);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Smart Todo dev companion listening on http://0.0.0.0:${PORT}`);
});
