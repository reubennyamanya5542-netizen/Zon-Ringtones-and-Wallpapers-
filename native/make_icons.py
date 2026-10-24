"""Builds the Zon launcher icon (adaptive + legacy) from native/icon.png.
The artwork is zoomed in so its dark rounded border and transparent corners are cropped away
and the picture fills the whole icon shape. Run after `npx cap add android`."""
import os, sys
from PIL import Image, ImageDraw, ImageFilter

SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'icon.png')
RES = sys.argv[1] if len(sys.argv) > 1 else 'android/app/src/main/res'
VIEW = 0.88          # share of the artwork that stays visible inside the launcher mask (smaller = more zoom)
DENS = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

src = Image.open(SRC).convert('RGBA')
W = src.width
# fill the transparent corners with the artwork's own dark edge colour
base = Image.new('RGBA', src.size, (14, 16, 44, 255))
base.alpha_composite(src)
solid = base.convert('RGB')

def fg(px):
    # artwork placed so that the 72dp visible window shows VIEW of it
    art = int(round(px * (72 / 108) / VIEW))
    c = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    a = src.resize((art, art), Image.LANCZOS)
    c.alpha_composite(a, ((px - art) // 2, (px - art) // 2))
    return c

def bg(px):
    big = solid.resize((px, px), Image.LANCZOS)
    z = big.crop((px // 4, px // 4, px * 3 // 4, px * 3 // 4)).resize((px, px), Image.LANCZOS)
    return z.filter(ImageFilter.GaussianBlur(px / 14))

def legacy(px, round_):
    k = int(round(W * VIEW))
    o = (W - k) // 2
    im = solid.crop((o, o, o + k, o + k)).resize((px, px), Image.LANCZOS).convert('RGBA')
    m = Image.new('L', (px * 4, px * 4), 0)
    d = ImageDraw.Draw(m)
    if round_: d.ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    else: d.rounded_rectangle((0, 0, px * 4 - 1, px * 4 - 1), radius=int(px * 4 * .22), fill=255)
    im.putalpha(m.resize((px, px), Image.LANCZOS))
    return im

for d, s in DENS.items():
    folder = os.path.join(RES, 'mipmap-' + d)
    os.makedirs(folder, exist_ok=True)
    fg(int(108 * s)).save(os.path.join(folder, 'zon_fg.png'))
    bg(int(108 * s)).save(os.path.join(folder, 'zon_bg.png'))
    legacy(int(48 * s), False).save(os.path.join(folder, 'ic_launcher.png'))
    legacy(int(48 * s), True).save(os.path.join(folder, 'ic_launcher_round.png'))
    # remove Capacitor's default foreground so nothing else can win
    for n in ('ic_launcher_foreground.png',):
        p = os.path.join(folder, n)
        if os.path.exists(p): os.remove(p)

xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/zon_bg"/>
    <foreground android:drawable="@mipmap/zon_fg"/>
</adaptive-icon>
'''
v = os.path.join(RES, 'mipmap-anydpi-v26')
os.makedirs(v, exist_ok=True)
for n in ('ic_launcher.xml', 'ic_launcher_round.xml'):
    open(os.path.join(v, n), 'w').write(xml)
print('icons written to', RES)
