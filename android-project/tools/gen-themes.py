#!/usr/bin/env python3
# Gerador dos fundos de tema nativos do Advmenu Android (arte original):
# gradiente escuro por familia + mascote + silhueta de console. Rode da
# raiz do repo:  python3 android-project/tools/gen-themes.py
# O fundo do HBMAME e o background_hbmame.jpg (arte do usuario) e nao e
# regenerado aqui.
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import math, os

OUT = "/workspace/github__EdsonMFernando__Advmenu-plus-atualizado-/android-project/app/src/main/assets/themes/adv-multi/art"
W, H = 1920, 1080

# paletas (topo, base, acento) por familia de sistema
FAMS = {
    "nintendo":  ((168, 32, 26), (14, 8, 10), (255, 68, 56)),
    "nes":       ((150, 30, 30), (12, 8, 10), (230, 200, 120)),
    "snes":      ((90, 60, 170), (10, 8, 22), (120, 200, 255)),
    "n64":       ((30, 60, 150), (6, 10, 26), (80, 220, 120)),
    "gameboy":   ((110, 140, 60), (12, 16, 8), (170, 220, 110)),
    "nds":       ((60, 70, 90), (8, 10, 14), (190, 210, 255)),
    "sega":      ((16, 60, 160), (6, 12, 26), (90, 190, 255)),
    "sony":      ((50, 45, 120), (8, 8, 20), (170, 160, 255)),
    "psp":       ((30, 40, 70), (8, 10, 16), (150, 200, 255)),
    "atari":     ((190, 90, 20), (18, 8, 4), (255, 170, 60)),
    "arcade":    ((70, 20, 120), (8, 4, 18), (0, 230, 200)),
    "hbmame":    ((90, 20, 10), (10, 3, 2), (255, 120, 40)),
    "pinball":   ((20, 60, 90), (4, 10, 16), (60, 220, 255)),
    "pc":        ((40, 90, 70), (6, 12, 10), (120, 255, 170)),
    "microsoft": ((24, 70, 40), (5, 12, 8), (90, 230, 120)),
    "retroarch": ((40, 40, 60), (8, 8, 12), (160, 160, 255)),
    "computer":  ((90, 70, 30), (14, 12, 6), (255, 210, 120)),
    "mugen":     ((120, 20, 80), (12, 4, 12), (255, 100, 200)),
    "openbor":   ((150, 80, 20), (16, 10, 4), (255, 190, 90)),
}

def fam_for(k):
    if k in ("hbmame",): return "hbmame"
    if k.startswith("pinball") or k == "pinball": return "pinball"
    if k in ("mame", "fba", "fbneo", "finalburn", "arcade", "cps1", "cps2", "cps3",
             "zinc", "model1", "model2", "model3", "naomi", "naomi2", "atomiswave",
             "stv", "lindbergh", "daphne", "neogeo", "neogeocd", "neogeopocket",
             "neogeopocketcolor", "ngpc", "taitotypex", "typex", "nesicaxlive",
             "nesica", "coinops"): return "arcade"
    if k in ("gb", "gameboy", "gbc", "gba"): return "gameboy"
    if k in ("nes", "famicom", "fds"): return "nes"
    if k in ("snes", "virtualboy"): return "snes"
    if k in ("n64",): return "n64"
    if k in ("nds", "3ds"): return "nds"
    if k in ("mastersystem", "sms", "megadrive", "genesis", "sega32x", "segacd",
             "gamegear", "sg1000", "dreamcast", "saturn"): return "sega"
    if k in ("psx", "ps1", "ps2", "ps3", "ps4", "ps5"): return "sony"
    if k in ("psp", "psvita", "vita"): return "psp"
    if k.startswith("atari") or k in ("astrocade",): return "atari"
    if k in ("pcengine", "turbografx", "turbografx16", "supergrafx", "pcfx"): return "arcade"
    if k in ("xbox", "xbox360", "xbla", "pc", "windows", "winlator", "dos", "pcdos"): return "microsoft"
    if k == "retroarch": return "retroarch"
    if k in ("mugen",): return "mugen"
    if k in ("openbor",): return "openbor"
    if k in ("c64", "c128", "vic20", "amiga", "amigacd32", "amstradcpc", "cpc", "msx",
             "msx2", "spectrum", "zxspectrum", "zx81", "macintosh", "apple2", "atarist",
             "x68000", "pc88", "pc98", "scummvm", "megaduck", "supervision", "gamate",
             "pv1000", "colecovision", "intellivision", "vectrex", "switch",
             "switch2", "gamecube", "gc", "wii", "wiiu", "teknoparrot"): return "computer"
    return "retroarch"

def cat_for(k):
    if any(k in x or k.startswith("pinball") for x in ()): pass
    if k.startswith("pinball"): return "pinball"
    if k in ("gb","gbc","gameboy","gba","nds","3ds","psp","psvita","vita","gamegear",
             "atarilynx","wonderswan","wonderswancolor","ngpc","neogeopocket",
             "neogeopocketcolor","megaduck","supervision","gamate"): return "handheld"
    if k in ("psx","ps1","ps2","saturn","dreamcast","segacd","3do","pcfx","neogeocd",
             "gamecube","gc","wii","amigacd32","xbox","xbox360","xbla"): return "disc"
    if k in ("mame","hbmame","fba","fbneo","finalburn","arcade","cps1","cps2","cps3",
             "zinc","model1","model2","model3","naomi","naomi2","atomiswave","stv",
             "lindbergh","taitotypex","typex","nesicaxlive","nesica","coinops",
             "teknoparrot","mugen","openbor","daphne"): return "arcade"
    if k in ("c64","c128","vic20","amiga","amstradcpc","cpc","msx","msx2","spectrum",
             "zxspectrum","zx81","macintosh","apple2","atarist","x68000","pc88",
             "pc98","dos","pcdos","scummvm"): return "computer"
    return "cart"

def base_gradient(top, base):
    img = Image.new("RGB", (W, H))
    px = img.load()
    for y in range(H):
        t = y / (H - 1)
        r = int(top[0] * (1 - t) + base[0] * t)
        g = int(top[1] * (1 - t) + base[1] * t)
        b = int(top[2] * (1 - t) + base[2] * t)
        for x in range(W):
            px[x, y] = (r, g, b)
    return img

def beams(img, accent):
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for i in range(6):
        x = int(W * (0.08 + 0.16 * i))
        a = 26 if i % 2 == 0 else 14
        d.polygon([(x, 0), (x + 140, 0), (x - 320 + 700, H), (x - 320 + 560, H)],
                  fill=accent + (a,))
    return Image.alpha_composite(img.convert("RGBA"), ov)

def glow(img, accent, cx, cy, rad, alpha):
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    d.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=accent + (alpha,))
    ov = ov.filter(ImageFilter.GaussianBlur(180))
    return Image.alpha_composite(img, ov)

def scanlines(img):
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for y in range(0, H, 4):
        d.line([(0, y), (W, y)], fill=(0, 0, 0, 16))
    return Image.alpha_composite(img, ov)

def vignette(img):
    ov = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(ov)
    d.ellipse([-W//3, -H//3, W + W//3, H + H//3], fill=78)
    ov = ov.filter(ImageFilter.GaussianBlur(220))
    black = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    black.putalpha(ov)
    return Image.alpha_composite(img, black)

def mascot(d, cx, cy, s, accent):
    # bonequinho original: robô oval com olhos e antena
    body = (accent[0], accent[1], accent[2])
    dark = tuple(int(c * 0.75) for c in body)
    d.ellipse([cx - s, cy - s, cx + s, cy + int(s*1.05)], fill=dark + (255,))
    d.rounded_rectangle([cx - int(s*0.55), cy - int(s*0.35), cx + int(s*0.55), cy + int(s*0.85)],
                        radius=int(s*0.4), fill=body + (255,))
    eye = (255, 255, 255, 235)
    pup = (10, 10, 14, 255)
    for ex in (-0.32, 0.32):
        exi = int(s * ex)
        d.ellipse([cx + exi - int(s*0.16), cy - int(s*0.28) - int(s*0.16),
                   cx + exi + int(s*0.16), cy - int(s*0.28) + int(s*0.16)], fill=eye)
        d.ellipse([cx + exi - int(s*0.07), cy - int(s*0.28) - int(s*0.07),
                   cx + exi + int(s*0.07), cy - int(s*0.28) + int(s*0.07)], fill=pup)
    d.line([cx, cy - s - int(s*0.5), cx, cy - int(s*0.8)], fill=body + (255,), width=max(2, s//8))
    d.ellipse([cx - int(s*0.12), cy - s - int(s*0.72), cx + int(s*0.12), cy - s - int(s*0.48)],
              fill=accent + (255,))

def motif(img, cat, accent):
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    ink = tuple(min(255, int(c * 1.1)) for c in accent) + (105,)
    ink2 = tuple(min(255, int(c * 0.8)) for c in accent) + (70,)
    cx, cy = W - 420, H - 380
    if cat == "handheld":
        d.rounded_rectangle([cx-260, cy-180, cx+260, cy+230], 46, outline=ink, width=10)
        d.rounded_rectangle([cx-180, cy-120, cx+180, cy+60], 14, outline=ink2, width=6)
        for bx in (cx-150, cx+150):
            d.ellipse([bx-24, cy+130-24, bx+24, cy+130+24], fill=ink2)
    elif cat == "disc":
        d.ellipse([cx-260, cy-260, cx+260, cy+260], outline=ink, width=12)
        d.ellipse([cx-95, cy-95, cx+95, cy+95], outline=ink2, width=8)
        d.ellipse([cx-30, cy-30, cx+30, cy+30], fill=ink2)
    elif cat == "arcade":
        d.rounded_rectangle([cx-200, cy-260, cx+200, cy+240], 30, outline=ink, width=10)
        d.rectangle([cx-140, cy-190, cx+140, cy-30], outline=ink2, width=6)
        d.ellipse([cx-40, cy+60-40, cx+40, cy+60+40], fill=ink2)
        d.line([cx, cy+60, cx+90, cy-20], fill=ink, width=10)
        d.ellipse([cx+70, cy-50, cx+110, cy-10], fill=ink)
    elif cat == "computer":
        d.rounded_rectangle([cx-300, cy-200, cx+300, cy+150], 18, outline=ink, width=10)
        d.rectangle([cx-240, cy-140, cx+240, cy+90], outline=ink2, width=5)
        d.rectangle([cx-300, cy+190, cx+300, cy+230], outline=ink2, width=8)
    elif cat == "pinball":
        d.polygon([(cx-160, cy-260), (cx+160, cy-260), (cx+110, cy+240), (cx-110, cy+240)],
                  outline=ink, width=10)
        d.ellipse([cx-30, cy-70, cx+30, cy-10], fill=ink2)
        d.line([cx-90, cy+170, cx+90, cy+170], fill=ink2, width=8)
    else:  # cart
        d.polygon([(cx-200, cy-170), (cx+200, cy-170), (cx+200, cy+200), (cx-200, cy+200)],
                  outline=ink, width=10)
        d.rectangle([cx-140, cy-170, cx+140, cy-60], outline=ink2, width=6)
        d.polygon([(cx-140, cy+200), (cx+140, cy+200), (cx+110, cy+130), (cx-110, cy+130)],
                  fill=ink2)
    return Image.alpha_composite(img, ov)

def make(key):
    fam = fam_for(key)
    top, base, accent = FAMS[fam]
    img = base_gradient(top, base).convert("RGBA")
    img = glow(img, accent, W//2, int(H*0.38), 620, 42)
    img = beams(img, accent)
    img = motif(img, cat_for(key), accent)
    # mascote no canto inferior esquerdo
    mov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    md = ImageDraw.Draw(mov)
    mascot(md, 300, H - 300, 120, accent)
    img = Image.alpha_composite(img, mov)
    img = scanlines(img)
    img = vignette(img)
    img = img.convert("RGB")
    img = ImageEnhance.Brightness(img).enhance(1.0)
    img = ImageEnhance.Color(img).enhance(1.18)
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
seen = set()
n = 0
tot = 0
for k in keys:
    if k in seen:
        continue
    seen.add(k)
    if k == "hbmame":
        continue  # dragao do usuario
    p = make(k)
    n += 1
    tot += os.path.getsize(p)
print("gerados:", n, "arquivos; total %.1f MB" % (tot / 1e6))
