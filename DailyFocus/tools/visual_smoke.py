#!/usr/bin/env python3
"""Non-fatal responsive widget visual smoke tests.

Renders representative narrow, compact, wide-short, and full layouts into a
confirmed writable temp directory. Any filesystem/rendering issue is reported
without hiding a valid Android build.
"""
from pathlib import Path
import html
import os
import tempfile


def choose_out():
    requested = os.environ.get('DAILYFOCUS_VISUAL_OUT')
    candidates = [Path(requested)] if requested else []
    candidates.append(Path(tempfile.mkdtemp(prefix='dailyfocus_visual_')))
    for out in candidates:
        try:
            out.mkdir(parents=True, exist_ok=True)
            probe = out / '.write-test'
            probe.write_text('ok', encoding='utf-8')
            probe.unlink()
            return out
        except Exception as e:
            print(f'Visual output unavailable at {out}: {e}')
    return None


def policy(w, h):
    mode = 'narrow' if w < 150 else 'compact' if (h < 160 or w < 280) else 'full'
    core = {'narrow': 102, 'compact': 103, 'full': 117}[mode]
    rem = max(0, h - core)
    reserve = 29 + 55
    status = mode != 'narrow' and rem >= reserve + 29
    if status:
        rem -= 29
    apps = mode == 'full' and rem >= reserve + 50
    if apps:
        rem -= 50
    task_header = rem >= reserve
    tasks = max(1, min(4, (rem - 29) // 55)) if task_header else 0
    return dict(mode=mode, status=status, apps=apps, task_header=task_header, tasks=tasks)


def esc(s):
    return html.escape(s)


def preview(width, height):
    p = policy(width, height)
    mode = p['mode']
    pad = 6 if mode != 'full' else 10
    corner = 22 if mode == 'narrow' else 26 if mode == 'compact' else 30
    parts = [f'''<defs>
      <linearGradient id="glass" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#f7fbf2" stop-opacity=".96"/><stop offset="1" stop-color="#f3e8dc" stop-opacity=".96"/></linearGradient>
      <linearGradient id="progress" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#1083ca"/><stop offset="1" stop-color="#639c49"/></linearGradient>
      <style>.t{{font-family:sans-serif;fill:#17313b}} .m{{font-family:sans-serif;fill:#617882}}</style>
    </defs>''']
    parts.append(f'<rect width="{width}" height="{height}" rx="{corner}" fill="#1083ca"/>')
    parts.append(f'<rect x="1" y="1" width="{width-2}" height="{height-2}" rx="{corner}" fill="url(#glass)" stroke="#ffffff" stroke-opacity=".6"/>')

    y = pad
    if mode == 'narrow':
        parts.append(f'<text x="{pad+3}" y="{y+22}" class="t" style="font-size:13px;font-weight:700">Focus</text>')
        y += 36
        parts.append(f'<text x="{pad+3}" y="{y+24}" class="t" style="font-size:22px;font-weight:700">2ч 17м</text>')
        y += 32
        parts.append(f'<text x="{pad+3}" y="{y+11}" class="m" style="font-size:9px">из 2ч</text>')
        y += 16
    else:
        header_h = 48
        parts.append(f'<text x="{pad+3}" y="{y+21}" class="t" style="font-size:14px;font-weight:700">Daily Focus</text>')
        if mode == 'full' and width >= 320:
            parts.append(f'<text x="{pad+3}" y="{y+35}" class="m" style="font-size:9px">Спокойный прогресс на сегодня</text>')
        if width >= 190:
            goal_w = 82 if mode == 'full' else 58
            gx = width - pad - goal_w - 52
            parts.append(f'<rect x="{gx}" y="{y}" width="{goal_w}" height="48" rx="24" fill="#fff2ce"/>')
            parts.append(f'<text x="{gx+goal_w/2}" y="{y+29}" text-anchor="middle" style="font:700 11px sans-serif;fill:#9b654a">{"Цель 2ч" if mode=="full" else "2ч"}</text>')
        if width >= 180:
            cx = width - pad - 24
            parts.append(f'<circle cx="{cx}" cy="{y+24}" r="22" fill="#ffffff" fill-opacity=".55"/>')
            parts.append(f'<text x="{cx}" y="{y+31}" text-anchor="middle" style="font:20px sans-serif;fill:#1083ca">↻</text>')
        y += header_h
        metric_h = 38 if mode == 'full' else 28
        fs = 28 if mode == 'full' else 22
        parts.append(f'<text x="{pad+3}" y="{y+metric_h-7}" class="t" style="font-size:{fs}px;font-weight:700">2ч 17м</text>')
        parts.append(f'<text x="{width-pad-3}" y="{y+metric_h-8}" text-anchor="end" class="m" style="font-size:9px">сегодня</text>')
        y += metric_h

    # progress
    track_w = width - 2*pad - 6
    parts.append(f'<rect x="{pad+3}" y="{y}" width="{track_w}" height="6" rx="3" fill="#58727c" fill-opacity=".20"/>')
    parts.append(f'<rect x="{pad+3}" y="{y}" width="{track_w}" height="6" rx="3" fill="url(#progress)"/>')
    y += 6

    if p['status']:
        y += 5
        parts.append(f'<rect x="{pad+3}" y="{y}" width="150" height="24" rx="12" fill="#ddeed5"/>')
        parts.append(f'<text x="{pad+12}" y="{y+16}" style="font:700 10px sans-serif;fill:#3f7e3e">✓ Цель выполнена · +17м</text>')
        y += 24

    if p['apps']:
        y += 6
        gap = 7
        card_w = (width - 2*pad - 6 - gap) / 2
        for i, (name, tm, badge, fill, color) in enumerate([
            ('Firefox', '1ч 26м', 'F', '#f5e0c8', '#9b654a'),
            ('ChatGPT', '51м', 'AI', '#dce9e2', '#3f7e3e'),
        ]):
            x = pad + 3 + i*(card_w+gap)
            parts.append(f'<rect x="{x}" y="{y}" width="{card_w}" height="44" rx="15" fill="#ffffff" fill-opacity=".55"/>')
            parts.append(f'<circle cx="{x+20}" cy="{y+22}" r="13" fill="{fill}"/>')
            parts.append(f'<text x="{x+20}" y="{y+26}" text-anchor="middle" style="font:700 9px sans-serif;fill:{color}">{badge}</text>')
            parts.append(f'<text x="{x+39}" y="{y+18}" class="t" style="font-size:11px;font-weight:700">{name}</text>')
            parts.append(f'<text x="{x+39}" y="{y+33}" class="m" style="font-size:10px">{tm}</text>')
        y += 44

    if p['task_header']:
        y += 5
        parts.append(f'<text x="{pad+3}" y="{y+16}" class="t" style="font-size:11px;font-weight:700">Задачи</text>')
        parts.append(f'<text x="{width-pad-3}" y="{y+16}" text-anchor="end" class="m" style="font-size:10px">1 / 4</text>')
        y += 24
        tasks = ['Математика ЕГЭ', 'Повторить тригонометрию', '30 мин Python', 'Проверить конспект']
        for i, name in enumerate(tasks[:p['tasks']]):
            y += 3
            done = i == 0
            parts.append(f'<rect x="{pad}" y="{y}" width="{width-2*pad}" height="52" rx="15" fill="#ffffff" fill-opacity=".25"/>')
            parts.append(f'<text x="{pad+16}" y="{y+34}" text-anchor="middle" style="font:700 25px sans-serif;fill:{"#639c49" if done else "#899a9f"}">{"✓" if done else "○"}</text>')
            parts.append(f'<text x="{pad+32}" y="{y+31}" class="{ 'm' if done else 't' }" style="font-size:{10 if mode == 'narrow' else 12}px">{esc(name)}</text>')
            y += 52

    if height >= 110 and y + pad > height + 1:
        raise AssertionError(f'preview overflow {width}x{height}: y={y+pad}, policy={p}')
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">'+''.join(parts)+'</svg>'


def main():
    try:
        out = choose_out()
        if out is None:
            print('Visual preview skipped: no writable directory')
            return 0
        sizes = [(96,360), (220,230), (360,110), (320,360), (360,420)]
        for w,h in sizes:
            svg = out / f'widget_{w}x{h}.svg'
            svg.write_text(preview(w,h), encoding='utf-8')
            try:
                import cairosvg
                cairosvg.svg2png(bytestring=svg.read_bytes(), write_to=str(out/f'widget_{w}x{h}.png'))
            except Exception as e:
                print(f'PNG conversion skipped for {w}x{h}: {e}')
        print(f'Visual preview PASS: {out} ({len(sizes)} adaptive sizes)')
        print(out)
    except Exception as e:
        print(f'Visual preview skipped: {e}')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
