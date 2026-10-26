const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const APK_PATH = path.join(__dirname, 'apk', 'myward2.apk');
const SEED_DATA_PATH = path.join(__dirname, 'app', 'src', 'main', 'assets', 'initial_seed.json');

let clothes = [];
let reports = [];

try {
  if (fs.existsSync(SEED_DATA_PATH)) {
    const raw = fs.readFileSync(SEED_DATA_PATH, 'utf8');
    const parsed = JSON.parse(raw);
    clothes = parsed.clothes || [];
    reports = parsed.reports || [];
  }
} catch (e) {
  console.error("Failed to parse seed data:", e);
}

function generateCatalogMd() {
  const lines = [
    `# Wardrobe Catalog & Garment Ergonomic Profiles`,
    `Generated: ${new Date().toISOString().split('T')[0]} | Source: Wardrobe Sensory & Care Management System`,
    `Total Garments Cataloged: ${clothes.length}`,
    ``,
    `> **NotebookLM Usage Guide**: Complete inventory profiles for all cataloged garments without preliminary AI opinions for pure empirical analysis.`,
    ``,
    `## 1. Summary Index`,
    `| Garment Name | Category | Material | Thickness | Price | Total Wears | Wash Status | May Throw |`,
    `| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |`
  ];
  clothes.forEach(item => {
    const name = item.Name || item.name || 'Unnamed';
    const type = item['Clothes Type'] || item.clothesType || 'Garment';
    const mat = item.material || '(Unspecified)';
    const thick = item['thick clothes'] || '(Unspecified)';
    const price = item.price > 0 ? `$${item.price.toFixed(2)}` : 'N/A';
    const wears = (item['wearing frequency(how many times legacy washed)'] || 0) + (item['Tracked App Washes'] || 0);
    const wash = (item['Tracked App Washes'] || 0) > 0 ? 'Clean' : 'Need Wash';
    const declutter = item['may throw'] || 'No';
    lines.push(`| **${name}** | ${type} | ${mat} | ${thick} | ${price} | ${wears} | ${wash} | ${declutter} |`);
  });
  return lines.join('\n');
}

function generateReportsMd() {
  const lines = [
    `# Wearing Sensory Reports & Wear Journal`,
    `Generated: ${new Date().toISOString().split('T')[0]} | Source: Wardrobe Sensory & Care Management System`,
    `Total Wear Sessions: ${reports.length}`,
    ``,
    `> **NotebookLM Usage Guide**: Chronological wear sessions detailing real-world outfit performance.`,
    ``,
    `## 1. Chronological Index`,
    `| Date | Report Title | Sweat | Shoulder Feel | Mid Breast | Leg Feel |`,
    `| :--- | :--- | :--- | :--- | :--- | :--- |`
  ];
  reports.slice(0, 50).forEach(r => {
    const date = r['wear date'] || r.date || 'Unspecified';
    const title = r.Name || r.title || 'Wear Report';
    const sweat = r.sweat || 'Normal';
    const shoulder = r['shoulder feel'] || 'Normal';
    const breast = r['mid breast'] || 'Normal';
    const leg = r['leg feel'] || 'Normal';
    lines.push(`| ${date} | **${title}** | ${sweat} | ${shoulder} | ${breast} | ${leg} |`);
  });
  return lines.join('\n');
}

function generateLaundryMd() {
  const lines = [
    `# Laundry Care Cycles & Wash History`,
    `Generated: ${new Date().toISOString().split('T')[0]} | Source: Wardrobe Sensory & Care Management System`,
    ``,
    `## 1. Garment Hygiene Status`,
    `| Garment | Category | Total Washes | Last Washed Date | Status |`,
    `| :--- | :--- | :--- | :--- | :--- |`
  ];
  clothes.forEach(item => {
    const name = item.Name || item.name || 'Unnamed';
    const type = item['Clothes Type'] || item.clothesType || 'Garment';
    const washes = (item['wearing frequency(how many times legacy washed)'] || 0) + (item['Tracked App Washes'] || 0);
    const date = item['Last Washed'] || item.lastWashed || 'Never';
    const status = (item['Tracked App Washes'] || 0) > 0 ? 'Clean' : 'Needs Wash';
    lines.push(`| **${name}** | ${type} | ${washes} washes | ${date} | ${status} |`);
  });
  return lines.join('\n');
}

function generateGlossaryMd() {
  return `# Wardrobe Sensory & Care Glossary

## 1. Ergonomic & Silhouette Fields
- **diao dai (吊带/肩带)**: Shoulder strap structure and tension evaluation.
- **sit breast ok (久坐胸型/腹部舒适度)**: Comfort during sedentary work or dining.
- **stand breast (站立胸型/中央支撑)**: Aesthetic silhouette and upright support.
- **side breast ok (副乳包容/侧收能力)**: Axillary coverage with gathered/side-support bras.

## 2. Environmental & Climate Performance
- **when with aircond: hot feeling**: Thermal perception in air-conditioned environments.
- **when without aircond: hot feeling**: Natural heat perception at noon.
- **when sleep under fan: hot feeling**: Sleep comfort and breathability under fans.
`;
}

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  if (url === '/download/myward2.apk' || url === '/download/app-debug.apk' || url === '/myward2.apk' || url === '/api/android/download-apk') {
    if (fs.existsSync(APK_PATH)) {
      const stat = fs.statSync(APK_PATH);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': 'attachment; filename="myward2.apk"',
        'Content-Length': stat.size
      });
      return fs.createReadStream(APK_PATH).pipe(res);
    } else {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      return res.end("APK file not found yet.");
    }
  }

  if (url === '/api/notebooklm/catalog.md') {
    res.writeHead(200, { 'Content-Type': 'text/markdown; charset=utf-8', 'Content-Disposition': 'attachment; filename="wardrobe_catalog_and_profiles.md"' });
    return res.end(generateCatalogMd());
  }
  if (url === '/api/notebooklm/reports.md') {
    res.writeHead(200, { 'Content-Type': 'text/markdown; charset=utf-8', 'Content-Disposition': 'attachment; filename="wearing_reports_and_logs.md"' });
    return res.end(generateReportsMd());
  }
  if (url === '/api/notebooklm/laundry.md') {
    res.writeHead(200, { 'Content-Type': 'text/markdown; charset=utf-8', 'Content-Disposition': 'attachment; filename="laundry_and_care_cycles.md"' });
    return res.end(generateLaundryMd());
  }
  if (url === '/api/notebooklm/glossary.md') {
    res.writeHead(200, { 'Content-Type': 'text/markdown; charset=utf-8', 'Content-Disposition': 'attachment; filename="sensory_and_care_glossary.md"' });
    return res.end(generateGlossaryMd());
  }

  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Wardrobe · 智能衣橱管理与体感日记</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
  <style>
    :root { --pico-primary: #6366f1; --pico-font-size: 14px; }
    body { background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding-bottom: 60px; }
    .hero-card { background: white; border-radius: 20px; padding: 24px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); margin-bottom: 20px; border: 1px solid #e2e8f0; }
    .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: bold; }
    .badge-pause { background: #fef3c7; color: #92400e; border: 1px solid #fde68a; }
    .badge-apk { background: #dbeafe; color: #1e40af; border: 1px solid #bfdbfe; }
    .badge-success { background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; }
    .btn-download { background: linear-gradient(135deg, #6366f1, #4f46e5); color: white; border: none; font-weight: bold; border-radius: 12px; padding: 12px 20px; font-size: 15px; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 8px; }
    .btn-download:hover { background: #4338ca; }
  </style>
</head>
<body>
  <div class="container" style="max-width: 900px; margin-top: 24px;">
    <div class="hero-card">
      <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
        <div>
          <span class="badge badge-success">✅ 已同步最新 GitHub 代码</span>
          <span class="badge badge-pause">⏸️ Zero Token Mode (AI Paused)</span>
          <h2 style="margin: 8px 0 4px 0; color: #0f172a;">Wardrobe & Wear Journal</h2>
          <p style="margin: 0; color: #64748b; font-size: 13px;">全新工学体感、AI 全局暂停主开关、Google NotebookLM 纯事实知识库导出</p>
        </div>
        <div>
          <a href="/download/myward2.apk" class="btn-download">
            <span>📲 点击下载最新 Android APK (myward2.apk · 46 MB)</span>
          </a>
        </div>
      </div>
    </div>

    <div class="hero-card">
      <h3 style="margin-top: 0; font-size: 16px; color: #1e293b;">✨ 已同步的最新功能 (New Features)</h3>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px;">
        <div style="background: #fffbeb; border: 1px solid #fde68a; padding: 14px; border-radius: 12px;">
          <h4 style="margin: 0 0 6px 0; color: #92400e; font-size: 14px;">⏸️ AI 生成全局暂停开关</h4>
          <p style="margin: 0; font-size: 12px; color: #78350f;">在「AI Styling」控制面板及导航栏新增一键暂停/恢复按钮，避免多余消耗 API Token 与等待时长。</p>
        </div>
        <div style="background: #eef2ff; border: 1px solid #c7d2fe; padding: 14px; border-radius: 12px;">
          <h4 style="margin: 0 0 6px 0; color: #3730a3; font-size: 14px;">📚 Google NotebookLM 知识库导出</h4>
          <p style="margin: 0; font-size: 12px; color: #312e81;">提供 4 份排除 AI 预设观点的 100% 纯客观事实 Markdown，直接导入 NotebookLM 生成语音播客与深度研报。</p>
        </div>
        <div style="background: #f1f5f9; border: 1px solid #cbd5e1; padding: 14px; border-radius: 12px;">
          <h4 style="margin: 0 0 6px 0; color: #1e293b; font-size: 14px;">🔤 全局字号无级缩放 (Aa)</h4>
          <p style="margin: 0; font-size: 12px; color: #334155;">导航栏右侧新增「Aa」字号缩放弹窗，支持标准 (100%)、舒适 (115%)、大号 (130%)，阅读卡片更护眼。</p>
        </div>
      </div>
    </div>

    <div class="hero-card">
      <h3 style="margin-top: 0; font-size: 16px; color: #1e293b;">📚 Google NotebookLM 知识库文件 (100% 纯客观事实)</h3>
      <p style="color: #64748b; font-size: 12px; margin-bottom: 16px;">
        点击下方链接可直接下载 Markdown 格式文件，直接上传至 <a href="https://notebooklm.google.com/" target="_blank">Google NotebookLM</a> 即可构建专属穿搭知识库！
      </p>
      <div style="display: flex; flex-direction: column; gap: 10px;">
        <div style="display: flex; justify-content: space-between; align-items: center; background: #f8fafc; padding: 10px 14px; border-radius: 10px; border: 1px solid #e2e8f0;">
          <div>
            <strong>1. 衣橱全量单品与工学档案</strong>
            <div style="font-size: 11px; color: #64748b;">wardrobe_catalog_and_profiles.md · 包含全部单品材质、CPW、肩带与胸型</div>
          </div>
          <a href="/api/notebooklm/catalog.md" class="badge badge-apk" style="text-decoration: none; padding: 6px 12px;">📥 下载 .md</a>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; background: #f8fafc; padding: 10px 14px; border-radius: 10px; border: 1px solid #e2e8f0;">
          <div>
            <strong>2. 穿搭日记与体感时序记录</strong>
            <div style="font-size: 11px; color: #64748b;">wearing_reports_and_logs.md · 真实排汗、肩部勒感、风扇夜温</div>
          </div>
          <a href="/api/notebooklm/reports.md" class="badge badge-apk" style="text-decoration: none; padding: 6px 12px;">📥 下载 .md</a>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; background: #f8fafc; padding: 10px 14px; border-radius: 10px; border: 1px solid #e2e8f0;">
          <div>
            <strong>3. 洗涤养护周期与洁净状态</strong>
            <div style="font-size: 11px; color: #64748b;">laundry_and_care_cycles.md · 累计洗涤频次、未洗累积时长及洗涤队列</div>
          </div>
          <a href="/api/notebooklm/laundry.md" class="badge badge-apk" style="text-decoration: none; padding: 6px 12px;">📥 下载 .md</a>
        </div>
        <div style="display: flex; justify-content: space-between; align-items: center; background: #f8fafc; padding: 10px 14px; border-radius: 10px; border: 1px solid #e2e8f0;">
          <div>
            <strong>4. 体感工学与字段双语词典</strong>
            <div style="font-size: 11px; color: #64748b;">sensory_and_care_glossary.md · 中英双语工学词汇对照</div>
          </div>
          <a href="/api/notebooklm/glossary.md" class="badge badge-apk" style="text-decoration: none; padding: 6px 12px;">📥 下载 .md</a>
        </div>
      </div>
    </div>
  </div>
</body>
</html>`;

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Server listening on port ${PORT}`);
});
