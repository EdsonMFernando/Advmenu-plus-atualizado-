#!/usr/bin/env python3
# Gerador dos fundos de tema nativos do Advmenu Android (arte ORIGINAL):
# padrao final conforme as imagens de referencia do usuario - bonequinhos
# cartoon GRANDES nas duas laterais, MOLDURA METALICA central vazia para o
# snapshot/video do jogo, FILEIRA DE SLOTS embaixo para a lista de jogos e
# rodape escuro para o nome do jogo selecionado. A estrutura e fixa; o que
# muda por sistema sao os bonecos e a paleta. Rode da raiz do repo:
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
    "racing":    ((200, 30, 30), (14, 4, 4), (255, 220, 60)),
    "shooting":  ((25, 70, 40), (4, 11, 6), (255, 240, 150)),
}

def fam_for(k):
    if k.startswith("pinball"): return "pinball"
    if k in ("corrida", "outrun", "daytona", "segarally", "cruisn",
             "cruisnusa", "ridgeracer", "harddrivin"): return "racing"
    if k in ("tiro", "lightgun", "timecrisis", "hotd", "virtuacop",
             "pointblank", "opwolf"): return "shooting"
    if k in ("mame", "fba", "fbneo", "finalburn", "arcade", "cps1", "cps2", "cps3",
             "zinc", "model1", "model2", "model3", "naomi", "naomi2", "atomiswave",
             "stv", "lindbergh", "daphne", "neogeo", "neogeocd", "neogeopocket",
             "neogeopocketcolor", "ngpc", "taitotypex", "typex", "nesicaxlive",
             "nesica", "coinops", "pcengine", "pce", "pcecd", "turbografx",
             "turbografx16", "tg16", "supergrafx", "pcfx", "fliperama", "kof", "streetfighter",
             "mortalkombat", "marvelvscapcom"): return "arcade"
    if k in ("gb", "gameboy", "gbc", "gba"): return "gameboy"
    if k in ("nes", "famicom", "fds"): return "nes"
    if k in ("snes", "sfc", "sufami", "sufamiturbo", "satellaview", "virtualboy"): return "snes"
    if k == "n64" or k == "n64dd": return "n64"
    if k in ("nds", "n3ds", "3ds"): return "nds"
    if k in ("mastersystem", "sms", "megadrive", "genesis", "sega32x", "32x",
             "segacd", "megacd", "gamegear", "sg1000", "sc3000", "dreamcast",
             "saturn", "gp32", "gp2x", "pico", "megajet", "wondermega",
             "teradrive", "sega"): return "sega"
    if k in ("psx", "ps1", "psxone", "ps2", "ps3", "ps4", "ps5", "playstation",
             "sony"): return "sony"
    if k in ("psp", "psvita", "vita"): return "psp"
    if k.startswith("atari") or k == "astrocade" or k == "xegs": return "atari"
    if k in ("xbox", "xbox360", "xbla", "pc", "windows", "winlator", "dos", "pcdos"): return "microsoft"
    if k == "mugen": return "mugen"
    if k == "openbor": return "openbor"
    if k in ("switch", "switch2", "nsw", "nintendo", "gmaster", "gamepock",
             "gameandwatch", "pokemini", "ereader"): return "nintendo"
    if k == "retroarch" or k == "pico8" or k == "tic80": return "retroarch"
    return "computer"

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

def make(key, sys_variant=False):
    fam = fam_for(key)
    top, base, accent = FAMS[fam]
    img = base_gradient(top, base).convert("RGBA")

    # brilho central-suave (area do snapshot fica limpa)
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    od.ellipse([W//2 - 700, 40, W//2 + 700, 760], fill=accent + (30,))
    ov = ov.filter(ImageFilter.GaussianBlur(160))
    img = Image.alpha_composite(img, ov)

    # textura industrial discreta: engrenagens grandes ao fundo
    td = ImageDraw.Draw(img)
    for gx, gy, gr in ((180, 140, 130), (960, 80, 170), (1740, 160, 140),
                       (1500, 940, 120), (420, 980, 110)):
        for k in range(10):
            a = k * 36
            td.pieslice([gx - gr, gy - gr, gx + gr, gy + gr], a, a + 16,
                        fill=(255, 255, 255, 10))
        td.ellipse([gx - gr * 0.45, gy - gr * 0.45, gx + gr * 0.45, gy + gr * 0.45],
                   outline=(255, 255, 255, 14), width=8)

    # BONEQUINHOS GRANDES nas laterais (altura quase toda, padrao das imagens)
    md = ImageDraw.Draw(img)
    s = 185
    mascot(md, 300, 470, s, accent, flip=False)
    mascot(md, W - 300, 470, s, accent, flip=True)

    if sys_variant:
        # variante SEM quadro (menu de SISTEMAS): area central limpa
        md.ellipse([W//2 - 520, 150, W//2 + 520, 700],
                   outline=(255, 255, 255, 22), width=6)
    else:
        # MOLDURA CENTRAL do snapshot/video: retangulo com borda metalica
        fx0, fy0, fx1, fy1 = 560, 130, 1360, 650
        md.rounded_rectangle([fx0 - 16, fy0 - 16, fx1 + 16, fy1 + 16], radius=30,
                             fill=accent + (80,))
        md.rounded_rectangle([fx0 - 10, fy0 - 10, fx1 + 10, fy1 + 10], radius=24,
                             fill=(205, 208, 216, 255))
        md.rounded_rectangle([fx0 - 3, fy0 - 3, fx1 + 3, fy1 + 3], radius=18,
                             fill=(88, 92, 102, 255))
        md.rounded_rectangle([fx0, fy0, fx1, fy1], radius=14, fill=(8, 8, 12, 255))
        md.line([(fx0 + 26, fy0 + 14), (fx1 - 26, fy0 + 14)],
                fill=(255, 255, 255, 40), width=4)

        # FILEIRA DE SLOTS da lista de jogos (embaixo da moldura, largura toda)
        y0, y1, n, gap, margin = 760, 856, 6, 26, 84
        slotw = (W - 2 * margin - (n - 1) * gap) // n
        for i in range(n):
            x0 = margin + i * (slotw + gap)
            md.rounded_rectangle([x0 - 7, y0 - 7, x0 + slotw + 7, y1 + 7], radius=16,
                                 fill=accent + (70,))
            md.rounded_rectangle([x0 - 3, y0 - 3, x0 + slotw + 3, y1 + 3], radius=12,
                                 fill=(190, 194, 202, 255))
            md.rounded_rectangle([x0, y0, x0 + slotw, y1], radius=9,
                                 fill=(10, 10, 16, 255))
            md.rounded_rectangle([x0 + 8, y1 - 12, x0 + slotw - 8, y1 - 6], radius=4,
                                 fill=accent + (130,))

    # rodape escuro para o nome do jogo selecionado
    md.rectangle([0, 900, W, H], fill=(5, 5, 10, 110))
    md.line([(0, 900), (W, 900)], fill=accent + (90,), width=4)

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
    if sys_variant:
        img = img.resize((1600, 900), Image.LANCZOS)
        p = os.path.join(OUT, "background_%s_sys.jpg" % key)
        img.save(p, "JPEG", quality=78, optimize=True, progressive=True)
    else:
        p = os.path.join(OUT, "background_%s.jpg" % key)
        img.save(p, "JPEG", quality=82, optimize=True, progressive=True)
    return p

def system_keys_from_java():
    # fonte da verdade: SYSTEM_FOLDERS no FrontendActivity.java
    import re
    java = ("/workspace/github__EdsonMFernando__Advmenu-plus-atualizado-/"
            "android-project/app/src/main/java/com/edsonmfernando/advancemame/"
            "FrontendActivity.java")
    src = open(java, encoding="utf-8").read()
    keys, seen = [], set()
    for k in re.findall(r'SYSTEM_FOLDERS\.put\("([^"]+)"', src):
        if k not in seen and k != "hbmame":
            seen.add(k)
            keys.append(k)
    return keys


def main():
    keys = system_keys_from_java()
    seen, n, tot = set(), 0, 0
    for k in keys:
        if k in seen or k == "hbmame":
            continue
        seen.add(k)
        p = make(k)
        n += 1
        tot += os.path.getsize(p)
        tot += os.path.getsize(make(k, sys_variant=True))
        n += 1
    print("gerados:", n, "%.1f MB" % (tot / 1e6))


if __name__ == "__main__":
    main()
