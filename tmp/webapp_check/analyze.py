import re

with open('/tmp/webapp_check/bundle.js', 'r', errors='ignore') as f:
    content = f.read()

print('Bundle size:', len(content))

# Look for navigation, views, tabs
tab_matches = re.findall(r'\{[^{}]*id:\s*["\']([a-zA-Z0-9_-]+)["\'][^{}]*label:\s*["\']([^"\']+)["\'][^{}]*\}', content)
print('--- Tabs / Nav buttons ---')
for t in tab_matches[:20]:
    print(t)

# Find any tab or screen titles
views = set(re.findall(r'activeTab\s*===?\s*["\']([a-zA-Z0-9_-]+)["\']', content))
print('--- Active Tabs ---', views)

current_view = set(re.findall(r'currentView\s*===?\s*["\']([a-zA-Z0-9_-]+)["\']', content))
print('--- Current Views ---', current_view)

# Look for major section headers or modal titles
sections = set(re.findall(r'["\']([\u4e00-\u9fffA-Za-z0-9\s/&·\-]{2,30})["\']', content))
# Filter chinese headers
cn_headers = [s for s in sections if any('\u4e00' <= c <= '\u9fff' for c in s) and len(s) >= 3]
print('--- Chinese text samples (count %d) ---' % len(cn_headers))
print([s for s in cn_headers if any(k in s for k in ['衣', '穿', '洗', '顾问', 'AI', '统计', '日记', '字典', '搭配', '配方', '柜', '分析', '管理', '记录'])][:60])
