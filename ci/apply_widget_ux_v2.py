#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path('.').resolve()
ANDROID = 'http://schemas.android.com/apk/res/android'
A = '{' + ANDROID + '}'
ET.register_namespace('android', ANDROID)


def replace_once(path, old, new):
    p = ROOT / path
    s = p.read_text(encoding='utf-8')
    if s.count(old) != 1:
        raise SystemExit(f'expected exactly one block in {path}: {old[:60]!r}')
    p.write_text(s.replace(old, new), encoding='utf-8')

# 1) Interaction ownership: no root click; task row, mark and title all toggle.
replace_once(
    'app/src/main/java/com/foto6/dailyfocus/widget/DailyFocusWidgetProvider.kt',
    '''            val open = openAppIntent(context)\n            views.setOnClickPendingIntent(R.id.widget_root, open)\n            views.setOnClickPendingIntent(R.id.widget_goal_pill, open)\n            views.setOnClickPendingIntent(R.id.widget_empty_tasks, open)\n            views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent(context))\n''',
    '''            val open = openAppIntent(context)\n            // Explicit zones: background taps do nothing, so task toggles cannot\n            // be shadowed by a root-level open-app action.\n            views.setOnClickPendingIntent(R.id.widget_title_area, open)\n            views.setOnClickPendingIntent(R.id.widget_goal_pill, open)\n            views.setOnClickPendingIntent(R.id.widget_tasks_header, open)\n            views.setOnClickPendingIntent(R.id.widget_empty_tasks, open)\n            views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent(context))\n''')
replace_once(
    'app/src/main/java/com/foto6/dailyfocus/widget/DailyFocusWidgetProvider.kt',
    '''                    views.setOnClickPendingIntent(taskRows[index], toggleTaskIntent(context, task.id))\n''',
    '''                    val toggle = toggleTaskIntent(context, task.id)\n                    // Entire row is the primary 48dp target. Bind the mark and\n                    // title too for consistent launcher hit-testing.\n                    views.setOnClickPendingIntent(taskRows[index], toggle)\n                    views.setOnClickPendingIntent(taskMarks[index], toggle)\n                    views.setOnClickPendingIntent(taskTitles[index], toggle)\n''')

# 2) RemoteViews layout: keep the existing wallpaper-matched look, but make all
# task rows and key header controls finger-sized.
layout_path = ROOT / 'app/src/main/res/layout/widget_daily_focus.xml'
tree = ET.parse(layout_path)
root = tree.getroot()
root.set(A+'padding', '6dp')

by_id = {}
for el in root.iter():
    rid = el.get(A+'id', '')
    if rid.startswith('@+id/'):
        by_id[rid.split('/', 1)[1]] = el

header = list(root)[0]
header.set(A+'layout_height', '48dp')
title = by_id['widget_title']
for candidate in header:
    if title in list(candidate):
        candidate.set(A+'id', '@+id/widget_title_area')
        candidate.set(A+'paddingLeft', '4dp')
        candidate.set(A+'paddingRight', '6dp')
        break
else:
    raise SystemExit('title container not found')

goal = by_id['widget_goal_pill']
goal.set(A+'layout_width', '80dp')
goal.set(A+'layout_height', '48dp')
refresh = by_id['widget_refresh']
refresh.set(A+'layout_width', '48dp')
refresh.set(A+'layout_height', '48dp')
refresh.set(A+'layout_marginLeft', '4dp')
refresh.set(A+'textSize', '22sp')

# Tasks header is the direct child containing widget_tasks_count.
for child in root:
    found = False
    for desc in child.iter():
        if desc.get(A+'id') == '@+id/widget_tasks_count':
            found = True
            break
    if found:
        child.set(A+'id', '@+id/widget_tasks_header')
        break
else:
    raise SystemExit('tasks header not found')

empty = by_id['widget_empty_tasks']
empty.set(A+'layout_height', '48dp')
empty.set(A+'background', '@drawable/widget_task_row')
empty.set(A+'paddingLeft', '12dp')

for i in range(1, 5):
    row = by_id[f'widget_task_row_{i}']
    mark = by_id[f'widget_task_mark_{i}']
    title_el = by_id[f'widget_task_title_{i}']
    row.set(A+'layout_height', '48dp')
    row.set(A+'background', '@drawable/widget_task_row')
    row.set(A+'paddingLeft', '4dp')
    row.set(A+'paddingRight', '8dp')
    mark.set(A+'layout_width', '40dp')
    mark.set(A+'textSize', '24sp')
    title_el.set(A+'layout_height', 'match_parent')
    title_el.set(A+'gravity', 'center_vertical')

tree.write(layout_path, encoding='utf-8', xml_declaration=True)

(ROOT/'app/src/main/res/drawable/widget_task_row.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n    <solid android:color="#38FFFFFF" />\n    <corners android:radius="15dp" />\n</shape>\n''', encoding='utf-8')

# 3) Responsive policy: preserve 48dp rows first, hide secondary content second.
(ROOT/'app/src/main/java/com/foto6/dailyfocus/core/WidgetLayoutPolicy.kt').write_text('''package com.foto6.dailyfocus.core\n\ndata class WidgetLayout(\n    val showSubtitle: Boolean,\n    val showStatus: Boolean,\n    val showApps: Boolean,\n    val maxTasks: Int\n)\n\nobject WidgetLayoutPolicy {\n    fun forSize(widthDp: Int, heightDp: Int): WidgetLayout {\n        val w = widthDp.coerceAtLeast(0)\n        val h = heightDp.coerceAtLeast(0)\n        val narrow = w < 280\n        return when {\n            h < 230 -> WidgetLayout(false, false, false, 1)\n            h < 259 -> WidgetLayout(!narrow, false, false, 2)\n            h < 309 -> WidgetLayout(!narrow, true, false, 2)\n            h < 357 -> WidgetLayout(!narrow, true, true, 2)\n            h < 405 -> WidgetLayout(!narrow, true, true, 3)\n            else -> WidgetLayout(!narrow, true, true, 4)\n        }\n    }\n    fun forHeight(heightDp: Int): WidgetLayout = forSize(320, heightDp)\n}\n''', encoding='utf-8')

(ROOT/'app/src/test/java/com/foto6/dailyfocus/core/WidgetLayoutPolicyTest.kt').write_text('''package com.foto6.dailyfocus.core\n\nimport org.junit.Assert.*\nimport org.junit.Test\nimport kotlin.random.Random\n\nclass WidgetLayoutPolicyTest {\n    @Test fun minimumWidgetKeepsOneLargeTaskTarget() = assertEquals(1, WidgetLayoutPolicy.forSize(250, 185).maxTasks)\n    @Test fun boundary230ShowsTwoTasks() = assertEquals(2, WidgetLayoutPolicy.forSize(320, 230).maxTasks)\n    @Test fun boundary259ShowsStatus() = assertTrue(WidgetLayoutPolicy.forSize(320, 259).showStatus)\n    @Test fun boundary309ShowsApps() = assertTrue(WidgetLayoutPolicy.forSize(320, 309).showApps)\n    @Test fun boundary357ShowsThreeTasks() = assertEquals(3, WidgetLayoutPolicy.forSize(320, 357).maxTasks)\n    @Test fun boundary405ShowsFourTasks() = assertEquals(4, WidgetLayoutPolicy.forSize(320, 405).maxTasks)\n    @Test fun narrowHidesSubtitle() = assertFalse(WidgetLayoutPolicy.forSize(250, 450).showSubtitle)\n    @Test fun stressPolicyInvariants() {\n        val r = Random(0xD411F0C)\n        repeat(100_000) {\n            val w = r.nextInt(-200, 1200); val h = r.nextInt(-200, 1600)\n            val x = WidgetLayoutPolicy.forSize(w, h)\n            assertTrue(x.maxTasks in 1..4)\n            if (h < 230) { assertFalse(x.showStatus); assertFalse(x.showApps); assertEquals(1, x.maxTasks) }\n            if (w < 280) assertFalse(x.showSubtitle)\n            if (x.showApps) assertTrue(x.showStatus)\n        }\n    }\n}\n''', encoding='utf-8')

# 4) Fatal static interaction contract.
static_path = ROOT/'tools/static_verify.py'
s = static_path.read_text(encoding='utf-8')
if 'task rows must remain full-size tap targets' not in s:
    marker = "if errors:\n"
    block = '''# UX contract: task rows must remain full-size tap targets.\nANDROID_NS = '{http://schemas.android.com/apk/res/android}'\nwidget_tree2=ET.parse(ROOT/'app/src/main/res/layout/widget_daily_focus.xml').getroot()\nby_id2={}\nfor element in widget_tree2.iter():\n    rid=element.attrib.get(ANDROID_NS+'id','')\n    if rid.startswith('@+id/'):\n        by_id2[rid.split('/',1)[1]]=element\nfor i in range(1,5):\n    row=by_id2.get(f'widget_task_row_{i}')\n    if row is None or row.attrib.get(ANDROID_NS+'layout_height') != '48dp': fail(f'task row {i} must stay 48dp high')\nfor control in ('widget_goal_pill','widget_refresh'):\n    el=by_id2.get(control)\n    if el is None or el.attrib.get(ANDROID_NS+'layout_height') != '48dp': fail(f'{control} must keep a 48dp touch height')\nif 'views.setOnClickPendingIntent(R.id.widget_root' in widget_code: fail('root click must not compete with task toggles')\nfor target in ('taskRows[index]','taskMarks[index]','taskTitles[index]'):\n    if f'views.setOnClickPendingIntent({target}, toggle)' not in widget_code: fail(f'missing task toggle binding: {target}')\n\n'''
    if marker not in s:
        raise SystemExit('static_verify marker missing')
    s = s.replace(marker, block + marker, 1)
    static_path.write_text(s, encoding='utf-8')

# 5) Fatal randomized geometry test using the real fixed block heights.
(ROOT/'tools/widget_layout_stress.py').write_text('''#!/usr/bin/env python3\nfrom random import Random\nPADDING_Y=12; HEADER=48; USAGE=38; PROGRESS=9; STATUS=29; APPS=50; TASK_HEADER=27; TASK_ROW=48\ndef policy(w,h):\n    w=max(w,0); h=max(h,0); narrow=w<280\n    if h<230:return (False,False,False,1)\n    if h<259:return (not narrow,False,False,2)\n    if h<309:return (not narrow,True,False,2)\n    if h<357:return (not narrow,True,True,2)\n    if h<405:return (not narrow,True,True,3)\n    return (not narrow,True,True,4)\ndef need(w,h):\n    _,status,apps,n=policy(w,h); x=PADDING_Y+HEADER+USAGE+PROGRESS+TASK_HEADER+n*TASK_ROW\n    if status:x+=STATUS\n    if apps:x+=APPS\n    return x\ndef check(w,h):\n    x=policy(w,h); required=need(w,h)\n    if w>=250 and h>=185 and required>h: raise AssertionError(f'overflow {w}x{h}: need {required}, {x}')\n    if TASK_ROW<48: raise AssertionError('task target below 48dp')\n    if x[2] and not x[1]: raise AssertionError('apps without status')\n    if w<280 and x[0]: raise AssertionError('subtitle on narrow widget')\ndef main():\n    for w in [250,260,279,280,320,360,420,520]:\n        for h in [185,200,229,230,258,259,308,309,356,357,404,405,460,520]:check(w,h)\n    r=Random(0xD411F0C)\n    for _ in range(100_000):check(r.randint(0,900),r.randint(0,1200))\n    print('WIDGET STRESS PASS: 48dp full-row targets + 100000 randomized sizes')\nif __name__=='__main__':main()\n''', encoding='utf-8')

# 6) Keep the existing visual smoke, but make its content policy and task row
# geometry mirror the new interaction layout.
vp = ROOT/'tools/visual_smoke.py'
v = vp.read_text(encoding='utf-8')
for old,new in [
    ('if height < 210:', 'if height < 230:'),
    ('if height < 250:', 'if height < 259:'),
    ('if height < 280:', 'if height < 309:'),
    ('if height < 300:', 'if height < 357:'),
    ('if height < 320:', 'if height < 405:'),
    ("base=y+i*28", "base=y+i*48"),
    ("bottom=y+l['tasks']*28+pad", "bottom=y+l['tasks']*48+pad"),
    ("style=\"font:700 17px sans-serif;fill:{color}\"", "style=\"font:700 24px sans-serif;fill:{color}\""),
    ("y=\"{base+19}\"", "y=\"{base+31}\""),
    ("y=\"{base+18}\"", "y=\"{base+29}\""),
    ("x=\"{pad+34}\"", "x=\"{pad+44}\""),
    ("sizes=[(250,185),(320,280),(320,320),(360,360)]", "sizes=[(250,185),(320,259),(320,309),(360,405)]")
]:
    if old not in v:
        raise SystemExit(f'visual_smoke expected text missing: {old}')
    v=v.replace(old,new)
vp.write_text(v, encoding='utf-8')

print('APPLY WIDGET UX V2 PASS')
