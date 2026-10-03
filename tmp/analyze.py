import re
import json

with open("/tmp/webapp/bundle.js", "r", encoding="utf-8") as f:
    content = f.read()

# Find localStorage keys
ls_keys = set(re.findall(r'localStorage\.(?:getItem|setItem|removeItem)\(["\']([^"\']+)["\']\)', content))
print("localStorage keys:", ls_keys)

# Look for tabs/nav
tabs = re.findall(r'id:["\']([a-zA-Z0-9_-]+)["\'],label:["\']([^"\']+)["\']', content)
print("Tabs/Nav:", tabs)

# Look for initialData or initial seed
match_seed = re.search(r'\{"garments":\[', content)
if match_seed:
    print("Found garments seed at:", match_seed.start())

# Find strings mentioning .tsx or components
print("Searching for component names or modals:")
patterns = [
    r'([A-Z][a-zA-Z0-9]+(?:Modal|Dialog|Screen|Card|Drawer|Report|Editor|View|Studio|Calendar|Archive|Manager|Toast|List))'
]
matches = set()
for p in patterns:
    for m in re.findall(p, content):
        if len(m) > 4:
            matches.add(m)

print("Detected UI components:", sorted(list(matches))[:40])
