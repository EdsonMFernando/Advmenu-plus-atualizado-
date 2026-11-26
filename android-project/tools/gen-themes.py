#!/usr/bin/env python3
# Gerador dos fundos de tema nativos do Advmenu Android (arte ORIGINAL):
# padrao dos videos de multijogos - bonequinhos grandes dos dois lados,
# centro limpo para o snapshot grande do topo e faixa escura embaixo para
# a lista de jogos. Rode da raiz do repo:
#   python3 android-project/tools/gen-themes.py
# O fundo do HBMAME (dragao do usuario) nao e regenerado aqui.
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import os

OUT = "/workspace/github__EdsonMFernando__Advmenu-plus-atualizado-/android-project/app/src/main/assets/themes/adv-multi/art"
W, H = 1920, 1080

FAMS = {
    "nintendo":  ((168, 32, 26), (10, 6, 8), (255, 68, 56)),
    "nes":       ((150, 30, 30), (10, 6, 8), (240, 200, 110)),
    "snes":      ((90, 60, 170), (8, 6, 20), (120, 200, 255)),
    "n64":       ((30, 60, 150), (5, 8, 24), (90, 230, 130)),
    "gameboy":   ((110, 140, 60), (10, 14, 7), (180, 230, 120)),
    "nds":       ((60, 70, 90), (7, 9, 13), (190, 210, 255)),
    "sega":      ((16, 60, 160), (5, 10, 24), (80, 190, 255)),
    "sony":      ((50, 45, 120), (7, 7, 18), (170, 160, 255)),
    "psp":       ((30, 40, 70), (7, 9, 15), (150, 200, 255)),
    "atari":     ((190, 90, 20), (16, 7, 3), (255, 170, 60)),
    "arcade":    ((70, 20, 120), (7, 3, 16), (0, 230, 200)),
    "pinball":   ((20, 60, 90), (3, 9, 15), (60, 220, 255)),
    "microsoft": ((24, 70, 40), (4, 11, 7), (90, 230, 120)),
    "retroarch": ((40, 40, 60), (7, 7, 11), (160, 160, 255)),
    "computer":  ((90, 70, 30), (13, 11, 5), (255, 210, 120)),
    "mugen":     ((120, 20, 80), (11, 3, 11), (255, 100, 200)),
    "openbor":   ((150, 80, 20), (15, 9, 3), (255, 190, 90)),
}

def fam_for(k):
    if k.startswith("pinball"): return "pinball"
    if k in ("mame", "fba", "fbneo", "finalburn", "arcade", "cps1", "cps2", "cps3",
             "zinc", "model1", "model2", "model3", "naomi", "naomi2", "atomiswave",
             "stv", "lindbergh", "daphne", "neogeo", "neogeocd", "neogeopocket",
             "neogeopocketcolor", "ngpc", "taitotypex", "typex", "nesicaxlive",
             "nesica", "coinops", "pcengine", "turbografx", "turbografx16",
             "supergrafx", "pcfx"): return "arcade"
    if k in ("gb", "gameboy", "gbc", "gba"): return "gameboy"
    if k in ("nes", "famicom", "fds"): return "nes"
    if k in ("snes", "virtualboy"): return "snes"
    if k == "n64": return "n64"
    if k in ("nds", "3ds"): return "nds"
    if k in ("mastersystem", "sms", "megadrive", "genesis", "sega32x", "segacd",
             "gamegear", "sg1000", "dreamcast", "saturn"): return "sega"
    if k in ("psx", "ps1", "ps2", "ps3", "ps4", "ps5"): return "sony"
    if k in ("psp", "psvita", "vita"): return "psp"
    if k.startswith("atari") or k == "astrocade": return "atari"
    if k in ("xbox", "xbox360", "xbla", "pc", "windows", "winlator", "dos", "pcdos"): return "microsoft"
    if k == "mugen": return "mugen"
    if k == "openbor": return "openbor"
    return "retroarch" if k == "retroarch" else "computer"

def outline_poly(d, pts, fill, width=10):
    d.polygon(pts, fill=(12, 10, 16, 255))
    d.polygon(pts, fill=fill)

def mascot(d, cx, cy, s, accent, flip=False):
    # bonequinho original estilo cartoon: robo oval com contorno preto,
    # olhos grandes, antena e braco; espelhado pelo paramentro flip
    def X(x):
        return cx + (-x if flip else x)
    black = (12, 10, 16, 255)
    body = accent + (255,)
    belly = tuple(min(255, int(c * 1.35) + 30) for c in accent) + (255,)
    dark = tuple(int(c * 0.55) for c in accent) + (255,)
    # pernas
    d.rounded_rectangle([X(-s*0.42) - s*0.13, cy + s*0.85, X(-s*0.42) + s*0.13, cy + s*1.35], radius=int(s*0.1), fill=black)
    d.rounded_rectangle([X(s*0.42) - s*0.13, cy + s*0.85, X(s*0.42) + s*0.13, cy + s*1.35], radius=int(s*0.1), fill=black)
    d.ellipse([X(-s*0.42) - s*0.2, cy + s*1.2, X(-s*0.42) + s*0.2, cy + s*1.42], fill=black)
    d.ellipse([X(s*0.42) - s*0.2, cy + s*1.2, X(s*0.42) + s*0.2, cy + s*1.42], fill=black)
    # corpo
    d.ellipse([cx - s*0.78, cy - s*0.55, cx + s*0.78, cy + s*1.05], fill=black)
    d.ellipse([cx - s*0.7, cy - s*0.47, cx + s*0.7, cy + s*0.97], fill=body)
    d.ellipse([cx - s*0.4, cy + s*0.05, cx + s*0.4, cy + s*0.72], fill=belly)
    # bracos
    ax = X(s*0.95) if not flip else X(-s*0.95)
    d.line([cx + (s*0.6 if not flip else -s*0.6), cy + s*0.1, ax, cy - s*0.25], fill=black, width=int(s*0.22))
    d.ellipse([ax - s*0.2, cy - s*0.42, ax + s*0.2, cy - s*0.02], fill=black)
    d.ellipse([ax - s*0.14, cy - s*0.36, ax + s*0.14, cy - s*0.08], fill=dark)
    # cabeca
    hy = cy - s*0.85
    d.ellipse([cx - s*0.62, hy - s*0.55, cx + s*0.62, hy + s*0.6], fill=black)
    d.ellipse([cx - s*0.55, hy - s*0.48, cx + s*0.55, hy + s*0.53], fill=body)
    d.ellipse([cx - s*0.4, hy - s*0.05, cx + s*0.4, hy + s*0.4], fill=(250, 235, 210, 255))
    # antena
    d.line([cx, hy - s*0.5, cx, hy - s*0.85], fill=black, width=int(s*0.09))
    d.ellipse([cx - s*0.1, hy - s*1.05, cx + s*0.1, hy - s*0.85], fill=belly)
    # olhos grandes
    for ex in (-0.26, 0.26):
        exi = s * ex
        d.ellipse([cx + exi - s*0.17, hy - s*0.24, cx + exi + s*0.17, hy + s*0.16], fill=(255, 255, 255, 255))
        d.ellipse([cx + exi - s*0.08, hy - s*0.13, cx + exi + s*0.08, hy + s*0.13], fill=(14, 12, 18, 255))
        d.ellipse([cx + exi + s*0.01, hy - s*0.1, cx + exi + s*0.06, hy - s*0.03], fill=(255, 255, 255, 255))
    # sorriso
    d.arc([cx - s*0.16, hy + s*0.12, cx + s*0.16, hy + s*0.3], 20, 160, fill=(60, 30, 30, 255), width=int(s*0.05))

def base_gradient(top, base):
    img = Image.new("RGB", (W, H))
    px = img.load()
    for y in range(H):
        t = y / (H - 1)
        c = tuple(int(top[i] * (1 - t) + base[i] * t) for i in range(3))
        for x in range(W):
            px[x, y] = c
    return img

def make(key):
    fam = fam_for(key)
    top, base, accent = FAMS[fam]
    img = base_gradient(top, base).convert("RGBA")

    # brilho central-suave (area do snapshot fica limpa)
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse([W//2 - 700, 40, W//2 + 700, 760], fill=accent + (30,))
    ov = ov.filter(ImageFilter.GaussianBlur(160))
    img = Image.alpha_composite(img, ov)

    # feixes de luz diagonais discretos
    bd = ImageDraw.Draw(ov)
    for i in range(5):
        x = int(W * (0.05 + 0.22 * i))
        bd.polygon([(x, 0), (x + 120, 0), (x + 480, H), (x + 360, H)], fill=accent + (12,))
    img = Image.alpha_composite(img, ov.filter(ImageFilter.GaussianBlur(6)))

    # BONEQUINHOS GRANDES dos dois lados (padrao dos videos)
    md = ImageDraw.Draw(img)
    s = 150
    mascot(md, 250, 400, s, accent, flip=False)   # esquerda
    mascot(md, W - 250, 400, s, accent, flip=True)  # direita

    # faixa inferior mais escura (a lista de jogos roda por aqui)
    bd2 = ImageDraw.Draw(img)
    bd2.rectangle([0, H - 300, W, H], fill=(5, 5, 10, 120))
    bd2.line([(0, H - 300), (W, H - 300)], fill=accent + (90,), width=5)

    # vinheta leve
    vmask = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(vmask)
    vd.ellipse([-W//3, -H//4, W + W//3, H + H//3], fill=90)
    vmask = vmask.filter(ImageFilter.GaussianBlur(200))
    vlay = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    vlay.putalpha(vmask)
    img = Image.alpha_composite(img, vlay)

    img = img.convert("RGB")
    img = ImageEnhance.Color(img).enhance(1.12)
    p = os.path.join(OUT, "background_%s.jpg" % key)
    img.save(p, "JPEG", quality=82, optimize=True, progressive=True)
    return p

keys = """atari2600 a2600 atari5200 atari7800 atari8bit atarilynx atarijaguar colecovision
intellivision vectrex mastersystem sms megadrive genesis sega32x segacd gamegear nes
famicom snes n64 gameboy gb gbc gba nds 3ds psx ps1 ps2 psp psvita vita dreamcast
saturn neogeo neogeopocket wonderswan wonderswancolor switch switch2 gamecube gc wii
wiiu virtualboy fds ps3 ps4 ps5 xbox xbox360 xbla sg1000 pcengine turbografx
turbografx16 supergrafx pcfx neogeocd neogeopocketcolor ngpc 3do megaduck supervision
gamate pv1000 astrocade openbor mugen teknoparrot taitotypex typex fba fbneo finalburn
mame cps1 cps2 cps3 zinc model1 model2 model3 naomi naomi2 atomiswave stv lindbergh
daphne pinball pinballfx pinballfx2 pinballfx3 pinballarcade futurepinball arcade
nesicaxlive nesica coinops retroarch amiga amigacd32 c64 c128 vic20 amstradcpc cpc msx
msx2 spectrum zxspectrum zx81 dos pcdos macintosh apple2 atarist x68000 pc88 pc98
scummvm pc windows winlator""".split()
seen, n, tot = set(), 0, 0
for k in keys:
    if k in seen or k == "hbmame":
        continue
    seen.add(k)
    p = make(k)
    n += 1
    tot += os.path.getsize(p)
print("gerados:", n, "%.1f MB" % (tot / 1e6))
