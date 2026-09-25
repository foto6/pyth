#!/usr/bin/env python3
from pathlib import Path
import re, sys, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]; errors=[]
def fail(x): errors.append(x)
for p in (ROOT/'app/src/main').rglob('*.xml'):
    try: ET.parse(p)
    except Exception as e: fail(f'XML parse failed: {p.relative_to(ROOT)}: {e}')
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
if 'android.permission.INTERNET' in manifest: fail('INTERNET permission must not be present')
if 'android.permission.PACKAGE_USAGE_STATS' not in manifest: fail('PACKAGE_USAGE_STATS permission missing')
if 'android:allowBackup="false"' not in manifest: fail('app backup must stay disabled')
for pkg in ('org.mozilla.firefox','com.openai.chatgpt'):
    if pkg not in manifest: fail(f'package query missing: {pkg}')
allowed={'LinearLayout','TextView','ProgressBar','ImageView'}
layouts=list((ROOT/'app/src/main/res/layout').glob('widget_daily_focus*.xml'))
if len(layouts)<3: fail('expected full/compact/narrow widget layouts')
code=(ROOT/'app/src/main/java/com/foto6/dailyfocus/widget/DailyFocusWidgetProvider.kt').read_text()
ids=set(re.findall(r'R\.id\.([A-Za-z0-9_]+)',code))
for p in layouts:
    tree=ET.parse(p).getroot()
    for el in tree.iter():
        tag=el.tag.split('}')[-1]
        if tag not in allowed: fail(f'unsupported RemoteViews tag {tag} in {p.name}')
    txt=p.read_text(); xmlids=set(re.findall(r'@\+id/([A-Za-z0-9_]+)',txt))
    for x in sorted(ids-xmlids): fail(f'{p.name} missing widget id: {x}')
A='{http://schemas.android.com/apk/res/android}'
for p in layouts:
    byid={}
    for el in ET.parse(p).getroot().iter():
        rid=el.attrib.get(A+'id','')
        if rid.startswith('@+id/'): byid[rid.split('/',1)[1]]=el
    for i in range(1,5):
        row=byid.get(f'widget_task_row_{i}')
        if row is None: fail(f'{p.name}: task row {i} missing')
        else:
            h=row.attrib.get(A+'layout_height','0dp')
            if not h.endswith('dp') or int(h[:-2])<48: fail(f'{p.name}: task row {i} below 48dp')
if 'views.setOnClickPendingIntent(R.id.widget_root' in code: fail('root click must not compete with task toggles')
for t in ('taskRows[index]','taskMarks[index]','taskTitles[index]'):
    if f'views.setOnClickPendingIntent({t}, toggle)' not in code: fail(f'missing task toggle binding: {t}')
info=(ROOT/'app/src/main/res/xml/daily_focus_widget_info.xml').read_text()
if 'android:minResizeWidth="72dp"' not in info: fail('narrow resize support missing')
if 'android:minResizeHeight="110dp"' not in info: fail('short resize support missing')
for rel in ['app/build.gradle.kts','app/src/main/AndroidManifest.xml','settings.gradle.kts','build.gradle.kts']:
    if not (ROOT/rel).is_file(): fail(f'missing required file: {rel}')
if errors:
    print('STATIC VERIFY FAILED'); [print(' -',x) for x in errors]; sys.exit(1)
print('STATIC VERIFY PASS: 3 adaptive layouts, 48dp+ task targets, resources/privacy')
