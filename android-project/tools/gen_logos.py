#!/usr/bin/env python3
# Gerador das bolinhas do carrossel do menu de SISTEMAS (padrao das imagens
# de referencia do usuario): circulo escuro com ANEL METALICO prateado e um
# bonequinho cartoon ORIGINAL diferente dentro de cada emulador (lutador,
# ninja, heroi, cavaleiro, robo, alien, samurai, dragao etc. — a arte e
# propria, sem personagens de terceiros). O que muda por emulador e o boneco
# e a paleta; a estrutura da bolinha e sempre a mesma. Rode da raiz do repo:
#   python3 android-project/tools/gen-logos.py
# O logo do HBMAME (dragao do usuario) nao e regenerado aqui.
import os
import re
import hashlib

from PIL import Image, ImageDraw, ImageFilter

from gen_themes import FAMS, fam_for

ART = "/workspace/github__EdsonMFernando__Advmenu-plus-atualizado-/android-project/app/src/main/assets/themes/adv-multi/art"
SRC = "/workspace/github__EdsonMFernando__Advmenu-plus-atualizado-/android-project/app/src/main/java/com/edsonmfernando/advancemame/FrontendActivity.java"
PX = 320
R = PX // 2

EXTRA_FAM = {
    "fairchild": "atari", "channelf": "atari", "odyssey": "atari",
    "odyssey2": "atari", "videopac": "atari", "o2": "atari",
    "apfm1000": "atari", "arcadia": "atari", "vc4000": "atari",
    "scv": "nintendo", "cassettevision": "nintendo", "gamecom": "microsoft",
    "microvision": "microsoft", "pokemini": "nintendo", "arduboy": "microsoft",
    "uzebox": "atari", "vsmile": "nintendo", "vflash": "nintendo",
    "playdia": "nintendo", "cdi": "sony", "pico": "sega", "gamepock": "nintendo",
    "gameking": "gameboy", "pocketchallenge": "gameboy", "gp32": "sega",
    "gp2x": "sega", "gizmondo": "microsoft", "loopy": "sony", "rca": "atari",
    "studio2": "atari", "gmaster": "nintendo", "myvision": "nes",
    "xegs": "atari", "atarijaguarcd": "atari", "superacan": "nintendo",
    "satellaview": "snes", "sufami": "snes", "sfc": "snes", "n3ds": "nds",
    "n64dd": "n64", "gameandwatch": "nintendo", "apple2gs": "computer",
    "apple1": "computer", "pet": "computer", "c16": "computer",
    "plus4": "computer", "samcoupe": "computer", "sordm5": "computer",
    "sorcerer": "computer", "trs80": "computer", "coco": "computer",
    "dragon32": "computer", "atom": "computer", "bbc": "computer",
    "electron": "computer", "archimedes": "computer", "thomson": "computer",
    "tomytutor": "computer", "pyuuta": "nintendo", "mtx": "computer",
    "vz200": "computer", "laser200": "computer", "laser310": "computer",
    "laser500": "computer", "uk101": "computer", "jupiter": "computer",
    "aquarius": "computer", "x1": "computer", "mz": "computer",
    "fm7": "computer", "fm77av": "computer", "fmtowns": "computer",
    "galaksija": "computer", "enterprise": "computer", "creativision": "atari",
    "einstein": "computer", "cpm": "computer", "adam": "computer",
    "sc3000": "sega", "ti99": "computer", "sol20": "computer", "nabu": "computer",
    "spectrumnext": "computer", "vg5k": "computer", "pv2000": "atari",
    "pc6001": "computer", "tvc": "computer", "comx": "computer",
    "mc1000": "computer", "spectravideo": "microsoft", "amstradnc": "computer",
    "z88": "computer", "newbrain": "computer", "sinclairql": "computer",
    "amigacdtv": "computer", "apricot": "computer", "kaypro": "computer",
    "interact": "computer", "mc10": "computer", "vector06c": "computer",
    "socrates": "computer", "chihiro": "microsoft", "triforce": "nintendo",
    "gaelco": "arcade", "hng64": "arcade", "megaplay": "sega", "megatech": "sega",
    "playchoice10": "nintendo", "pcecd": "pcengine", "tg16": "pcengine",
    "pico8": "retroarch", "tic80": "retroarch", "zmachine": "computer",
    "flash": "microsoft", "j2me": "microsoft", "hypervision": "microsoft",
    "atari": "atari", "sega": "sega", "nintendo": "nintendo", "sony": "sony",
    "palm": "computer", "wince": "microsoft", "megajet": "sega",
    "wondermega": "sega", "teradrive": "sega",
    "corrida": "racing", "outrun": "racing", "daytona": "racing",
    "segarally": "racing", "cruisn": "racing", "ridgeracer": "racing",
    "harddrivin": "racing", "tiro": "shooting", "lightgun": "shooting",
    "timecrisis": "shooting", "hotd": "shooting", "virtuacop": "shooting",
    "pointblank": "shooting", "opwolf": "shooting", "atarixl": "atari",
    "atari800": "atari", "megacd": "sega", "32x": "sega", "pce": "arcade",
    "sufamiturbo": "snes", "ereader": "nintendo", "nsw": "nintendo",
    "psxone": "sony", "playstation": "sony", "fliperama": "arcade",
}

FAMS.update({
    "pcengine": ((100, 30, 90), (12, 4, 10), (255, 120, 210)),
})

SKINS = [
    (255, 220, 185),
    (240, 190, 150),
    (170, 120, 85),
]

BLACK = (12, 10, 16, 255)
SILVER = (206, 210, 220, 255)

FIXED = {
    "atari2600": "marine", "a2600": "marine", "nes": "cap", "famicom": "cap",
    "snes": "hero", "sfc": "hero", "n64": "cap", "megadrive": "fighter",
    "genesis": "fighter", "psx": "cyborg", "ps1": "cyborg", "ps2": "knight",
    "ps3": "marine", "ps4": "marine", "ps5": "marine", "psp": "racer",
    "psvita": "heroine", "vita": "heroine", "xbox": "marine",
    "xbox360": "marine", "mastersystem": "racer", "sms": "racer",
    "gameboy": "cap", "gb": "cap", "gbc": "cap", "gba": "cap",
    "dreamcast": "racer", "saturn": "fighter", "neogeo": "fighter",
    "pinball": "wizard", "mame": "arcadeguy",
    "fba": "fighter", "fbneo": "fighter", "arcade": "arcadeguy",
    "switch": "hero", "switch2": "hero", "wii": "hero", "wiiu": "hero",
    "gamecube": "knight", "gc": "knight", "3ds": "cap", "nds": "cap",
    "corrida": "racer", "outrun": "racer", "daytona": "racer",
    "segarally": "racer", "cruisn": "racer", "ridgeracer": "racer",
    "harddrivin": "racer", "tiro": "marine", "lightgun": "marine",
    "timecrisis": "marine", "hotd": "marine", "virtuacop": "marine",
    "pointblank": "marine", "opwolf": "marine", "fliperama": "arcadeguy",
    "atarixl": "marine", "atari800": "marine", "megacd": "racer",
    "32x": "racer", "pce": "cap", "sufamiturbo": "hero", "ereader": "cap",
    "nsw": "hero", "psxone": "cyborg", "playstation": "heroine",
}

KINDS = [
    "fighter", "ninja", "hero", "heroine", "knight", "robot", "mecha",
    "alien", "marine", "dragon", "boxer", "wizard", "pirate", "samurai",
    "cap", "cat", "racer", "sumo", "cyborg", "arcadeguy",
]


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))),
            max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), 255)


def torso(d, cx, y, w, c):
    d.polygon([(cx - w, y + int(w * 1.1)), (cx - int(w * 0.55), y),
               (cx + int(w * 0.55), y), (cx + w, y + int(w * 1.1))], fill=c)


def head(d, cx, cy, s, skin):
    d.ellipse([cx - s * 0.62, cy - s * 0.62, cx + s * 0.62, cy + s * 0.62], fill=skin)


def eyes(d, cx, cy, s):
    for ex in (-0.24, 0.24):
        d.ellipse([cx + s * ex - s * 0.15, cy - s * 0.18,
                   cx + s * ex + s * 0.15, cy + s * 0.16], fill=(255, 255, 255, 255))
        d.ellipse([cx + s * ex - s * 0.07, cy - s * 0.08,
                   cx + s * ex + s * 0.07, cy + s * 0.12], fill=(14, 12, 18, 255))
        d.ellipse([cx + s * ex, cy - s * 0.05, cx + s * ex + s * 0.04,
                   cy + s * 0.02], fill=(255, 255, 255, 255))


def mouth(d, cx, cy, s):
    d.arc([cx - s * 0.14, cy + s * 0.08, cx + s * 0.14, cy + s * 0.26],
          15, 165, fill=(60, 30, 30, 255), width=max(2, int(s * 0.05)))


def portrait(d, cx, cy, s, kind, acc, c2, skin):
    b = BLACK
    if kind == "fighter":
        head(d, cx, cy, s * 0.9, skin)
        d.polygon([(cx - s * 0.7, cy - s * 0.15), (cx - s * 0.3, cy - s * 0.5),
                   (cx - s * 0.05, cy - s * 0.15), (cx + s * 0.2, cy - s * 0.55),
                   (cx + s * 0.6, cy - s * 0.1), (cx + s * 0.3, cy - s * 0.02),
                   (cx - s * 0.3, cy - s * 0.02)], fill=(40, 28, 20, 255))
        d.rectangle([cx - s * 0.68, cy - s * 0.34, cx + s * 0.68, cy - s * 0.18],
                    fill=c2)
        eyes(d, cx, cy, s)
        mouth(d, cx, cy, s)
        torso(d, cx, cy + s * 0.55, s * 0.75, acc)
        d.rectangle([cx - s * 0.68, cy + s * 0.5, cx + s * 0.68, cy + s * 0.66], fill=c2)
    elif kind == "ninja":
        d.ellipse([cx - s * 0.68, cy - s * 0.68, cx + s * 0.68, cy + s * 0.68], fill=shade(acc, 0.45))
        d.rectangle([cx - s * 0.6, cy + s * 0.05, cx + s * 0.6, cy + s * 0.5], fill=shade(acc, 0.4))
        eyes(d, cx, cy - s * 0.05, s)
        d.line([cx - s * 0.66, cy + s * 0.05, cx + s * 0.66, cy + s * 0.05],
               fill=b, width=max(3, int(s * 0.06)))
        d.rectangle([cx - s * 0.2, cy - s * 0.68, cx + s * 0.12, cy - s * 0.1], fill=shade(acc, 0.8))
    elif kind == "hero":
        head(d, cx, cy, s * 0.9, skin)
        d.polygon([(cx - s * 0.66, cy - s * 0.3), (cx + s * 0.66, cy - s * 0.3),
                   (cx + s * 0.3, cy - s * 0.62), (cx - s * 0.3, cy - s * 0.62)],
                  fill=shade(c2, 0.8))
        for ex in (-0.24, 0.24):
            d.polygon([(cx + s * ex - s * 0.18, cy - s * 0.2), (cx + s * ex + s * 0.18, cy - s * 0.2),
                       (cx + s * ex + s * 0.12, cy - s * 0.06), (cx + s * ex - s * 0.12, cy - s * 0.06)],
                      fill=acc)
        eyes(d, cx, cy - s * 0.02, s * 0.82)
        mouth(d, cx, cy, s)
        torso(d, cx, cy + s * 0.55, s * 0.75, acc)
        d.polygon([(cx - s * 0.1, cy + s * 0.42), (cx + s * 0.1, cy + s * 0.42),
                   (cx + s * 0.18, cy + s * 0.62), (cx - s * 0.18, cy + s * 0.62)], fill=c2)
    elif kind == "heroine":
        head(d, cx, cy, s * 0.9, skin)
        d.polygon([(cx - s * 0.6, cy - s * 0.45), (cx - s * 0.85, cy + s * 0.5),
                   (cx - s * 0.5, cy + s * 0.4), (cx - s * 0.45, cy - s * 0.2)], fill=c2)
        d.polygon([(cx + s * 0.6, cy - s * 0.45), (cx + s * 0.85, cy + s * 0.5),
                   (cx + s * 0.5, cy + s * 0.4), (cx + s * 0.45, cy - s * 0.2)], fill=c2)
        for ex in (-0.24, 0.24):
            d.ellipse([cx + s * ex - s * 0.15, cy - s * 0.18, cx + s * ex + s * 0.15,
                       cy + s * 0.16], fill=(255, 255, 255, 255))
            d.ellipse([cx + s * ex - s * 0.07, cy - s * 0.08, cx + s * ex + s * 0.07,
                       cy + s * 0.12], fill=(60, 40, 100, 255))
        d.polygon([(cx - s * 0.35, cy - s * 0.6), (cx + s * 0.35, cy - s * 0.6),
                   (cx, cy - s * 0.75)], fill=(240, 200, 90, 255))
        d.ellipse([cx - s * 0.07, cy - s * 0.66, cx + s * 0.07, cy - s * 0.52],
                  fill=(255, 240, 150, 255))
        mouth(d, cx, cy, s)
        torso(d, cx, cy + s * 0.55, s * 0.7, c2)
    elif kind == "knight":
        d.rounded_rectangle([cx - s * 0.6, cy - s * 0.6, cx + s * 0.6, cy + s * 0.35],
                            radius=int(s * 0.25), fill=shade(SILVER, 0.85))
        d.rectangle([cx - s * 0.42, cy - s * 0.14, cx + s * 0.42, cy + s * 0.12], fill=(20, 22, 30, 255))
        eyes(d, cx, cy - s * 0.02, s * 0.8)
        d.rectangle([cx - s * 0.08, cy - s * 0.95, cx + s * 0.08, cy - s * 0.6], fill=c2)
        d.ellipse([cx - s * 0.14, cy - s * 1.08, cx + s * 0.14, cy - s * 0.82], fill=c2)
        torso(d, cx, cy + s * 0.55, s * 0.8, shade(SILVER, 0.9))
        d.rectangle([cx - s * 0.12, cy + s * 0.3, cx + s * 0.12, cy + s * 0.75], fill=acc)
    elif kind == "robot":
        d.ellipse([cx - s * 0.62, cy - s * 0.55, cx + s * 0.62, cy + s * 0.6], fill=BLACK)
        d.ellipse([cx - s * 0.55, cy - s * 0.48, cx + s * 0.55, cy + s * 0.53], fill=acc)
        for ex in (-0.24, 0.24):
            d.ellipse([cx + s * ex - s * 0.16, cy - s * 0.24, cx + s * ex + s * 0.16,
                       cy + s * 0.16], fill=(255, 255, 255, 255))
            d.ellipse([cx + s * ex - s * 0.07, cy - s * 0.13, cx + s * ex + s * 0.07,
                       cy + s * 0.13], fill=(14, 12, 18, 255))
        d.line([cx, cy - s * 0.5, cx, cy - s * 0.8], fill=BLACK, width=max(3, int(s * 0.08)))
        d.ellipse([cx - s * 0.09, cy - s * 0.95, cx + s * 0.09, cy - s * 0.77], fill=c2)
        torso(d, cx, cy + s * 0.6, s * 0.7, shade(acc, 0.8))
    elif kind == "mecha":
        d.rounded_rectangle([cx - s * 0.62, cy - s * 0.55, cx + s * 0.62, cy + s * 0.5],
                            radius=int(s * 0.12), fill=shade(acc, 0.9))
        d.rectangle([cx - s * 0.45, cy - s * 0.12, cx + s * 0.45, cy + s * 0.14],
                    fill=(255, 80, 60, 255))
        d.rectangle([cx - s * 0.62, cy + s * 0.1, cx - s * 0.45, cy + s * 0.4], fill=shade(acc, 0.6))
        d.rectangle([cx + s * 0.45, cy + s * 0.1, cx + s * 0.62, cy + s * 0.4], fill=shade(acc, 0.6))
        d.rectangle([cx - s * 0.1, cy - s * 0.75, cx + s * 0.1, cy - s * 0.5], fill=c2)
        torso(d, cx, cy + s * 0.6, s * 0.75, shade(acc, 0.7))
    elif kind == "alien":
        d.ellipse([cx - s * 0.72, cy - s * 0.75, cx + s * 0.72, cy + s * 0.45],
                  fill=shade(c2, 1.0))
        for ex in (-0.28, 0.28):
            d.ellipse([cx + s * ex - s * 0.2, cy - s * 0.25, cx + s * ex + s * 0.2,
                       cy + s * 0.2], fill=(20, 20, 30, 255))
        d.polygon([(cx, cy + s * 0.3), (cx - s * 0.1, cy + s * 0.45),
                   (cx + s * 0.1, cy + s * 0.45)], fill=shade(c2, 0.7))
        d.line([cx - s * 0.3, cy - s * 0.72, cx - s * 0.5, cy - s * 1.0], fill=BLACK,
               width=max(2, int(s * 0.05)))
        d.line([cx + s * 0.3, cy - s * 0.72, cx + s * 0.5, cy - s * 1.0], fill=BLACK,
               width=max(2, int(s * 0.05)))
        d.ellipse([cx - s * 0.56, cy - s * 1.06, cx - s * 0.44, cy - s * 0.94], fill=acc)
        d.ellipse([cx + s * 0.44, cy - s * 1.06, cx + s * 0.56, cy - s * 0.94], fill=acc)
        torso(d, cx, cy + s * 0.62, s * 0.65, shade(c2, 0.8))
    elif kind == "marine":
        d.ellipse([cx - s * 0.62, cy - s * 0.55, cx + s * 0.62, cy + s * 0.25],
                  fill=shade(acc, 0.75))
        d.rectangle([cx - s * 0.62, cy - s * 0.05, cx + s * 0.62, cy + s * 0.12],
                    fill=(30, 34, 44, 255))
        d.rectangle([cx - s * 0.55, cy + s * 0.06, cx + s * 0.55, cy + s * 0.3],
                    fill=shade(c2, 0.6))
        eyes(d, cx, cy + s * 0.12, s * 0.85)
        torso(d, cx, cy + s * 0.6, s * 0.8, shade(acc, 0.6))
    elif kind == "dragon":
        d.ellipse([cx - s * 0.65, cy - s * 0.5, cx + s * 0.65, cy + s * 0.55], fill=acc)
        d.polygon([(cx - s * 0.45, cy - s * 0.4), (cx - s * 0.25, cy - s * 0.85),
                   (cx - s * 0.05, cy - s * 0.4)], fill=c2)
        d.polygon([(cx + s * 0.05, cy - s * 0.4), (cx + s * 0.25, cy - s * 0.85),
                   (cx + s * 0.45, cy - s * 0.4)], fill=c2)
        for ex in (-0.26, 0.26):
            d.ellipse([cx + s * ex - s * 0.15, cy - s * 0.18, cx + s * ex + s * 0.15,
                       cy + s * 0.14], fill=(255, 220, 90, 255))
            d.ellipse([cx + s * ex - s * 0.07, cy - s * 0.1, cx + s * ex + s * 0.07,
                       cy + s * 0.1], fill=(15, 10, 5, 255))
        d.polygon([(cx - s * 0.12, cy + s * 0.22), (cx + s * 0.12, cy + s * 0.22),
                   (cx, cy + s * 0.42)], fill=(255, 200, 80, 255))
        for i in range(4):
            x0 = cx - s * 0.5 + i * s * 0.12
            d.polygon([(x0, cy + s * 0.55), (x0 + s * 0.06, cy + s * 0.35),
                       (x0 + s * 0.12, cy + s * 0.55)], fill=c2)
        torso(d, cx, cy + s * 0.72, s * 0.72, shade(acc, 0.8))
    elif kind == "boxer":
        head(d, cx, cy - s * 0.1, s * 0.8, skin)
        d.polygon([(cx - s * 0.66, cy - s * 0.35), (cx + s * 0.66, cy - s * 0.35),
                   (cx + s * 0.3, cy - s * 0.66), (cx - s * 0.3, cy - s * 0.66)],
                  fill=(40, 28, 20, 255))
        eyes(d, cx, cy - s * 0.12, s * 0.85)
        mouth(d, cx, cy - s * 0.12, s)
        torso(d, cx, cy + s * 0.6, s * 0.78, skin)
        d.ellipse([cx - s * 1.0, cy + s * 0.2, cx - s * 0.5, cy + s * 0.7], fill=acc)
        d.ellipse([cx + s * 0.5, cy + s * 0.2, cx + s * 1.0, cy + s * 0.7], fill=acc)
    elif kind == "wizard":
        d.polygon([(cx - s * 0.75, cy - s * 0.35), (cx + s * 0.75, cy - s * 0.35),
                   (cx + s * 0.05, cy - s * 1.1)], fill=acc)
        d.ellipse([cx - s * 0.8, cy - s * 0.42, cx + s * 0.8, cy - s * 0.22], fill=shade(acc, 0.8))
        head(d, cx, cy + s * 0.05, s * 0.75, skin)
        d.ellipse([cx - s * 0.4, cy + s * 0.1, cx + s * 0.4, cy + s * 0.55],
                  fill=(235, 235, 235, 255))
        eyes(d, cx, cy, s * 0.85)
        torso(d, cx, cy + s * 0.62, s * 0.78, c2)
    elif kind == "pirate":
        head(d, cx, cy, s * 0.85, skin)
        d.polygon([(cx - s * 0.72, cy - s * 0.25), (cx + s * 0.72, cy - s * 0.25),
                   (cx, cy - s * 0.85)], fill=acc)
        d.rectangle([cx - s * 0.72, cy - s * 0.3, cx + s * 0.72, cy - s * 0.12], fill=shade(acc, 0.7))
        d.ellipse([cx + s * 0.08, cy - s * 0.2, cx + s * 0.4, cy + s * 0.08], fill=BLACK)
        d.ellipse([cx - s * 0.35, cy - s * 0.2, cx - s * 0.08, cy + s * 0.12],
                  fill=(255, 255, 255, 255))
        d.ellipse([cx - s * 0.28, cy - s * 0.1, cx - s * 0.16, cy + s * 0.05], fill=(14, 12, 18, 255))
        torso(d, cx, cy + s * 0.58, s * 0.78, shade(c2, 0.85))
    elif kind == "samurai":
        d.ellipse([cx - s * 0.58, cy - s * 0.42, cx + s * 0.58, cy + s * 0.2], fill=shade(acc, 0.8))
        d.polygon([(cx - s * 0.72, cy - s * 0.42), (cx - s * 0.4, cy - s * 0.42),
                   (cx - s * 0.55, cy - s * 0.85)], fill=c2)
        d.polygon([(cx + s * 0.72, cy - s * 0.42), (cx + s * 0.4, cy - s * 0.42),
                   (cx + s * 0.55, cy - s * 0.85)], fill=c2)
        d.arc([cx - s * 0.2, cy - s * 0.95, cx + s * 0.2, cy - s * 0.55], 180, 360,
              fill=(240, 200, 90, 255), width=max(3, int(s * 0.07)))
        d.rectangle([cx - s * 0.5, cy + s * 0.02, cx + s * 0.5, cy + s * 0.2], fill=(25, 22, 30, 255))
        eyes(d, cx, cy + s * 0.1, s * 0.8)
        torso(d, cx, cy + s * 0.62, s * 0.8, shade(acc, 0.6))
    elif kind == "cap":
        head(d, cx, cy, s * 0.9, skin)
        d.pieslice([cx - s * 0.66, cy - s * 0.75, cx + s * 0.66, cy + s * 0.15], 180, 360, fill=acc)
        d.rectangle([cx - s * 0.95, cy - s * 0.28, cx + s * 0.05, cy - s * 0.14], fill=shade(acc, 0.75))
        eyes(d, cx, cy, s)
        mouth(d, cx, cy, s)
        torso(d, cx, cy + s * 0.58, s * 0.75, c2)
    elif kind == "cat":
        d.polygon([(cx - s * 0.55, cy - s * 0.25), (cx - s * 0.7, cy - s * 0.95),
                   (cx - s * 0.15, cy - s * 0.5)], fill=acc)
        d.polygon([(cx + s * 0.55, cy - s * 0.25), (cx + s * 0.7, cy - s * 0.95),
                   (cx + s * 0.15, cy - s * 0.5)], fill=acc)
        d.polygon([(cx - s * 0.5, cy - s * 0.3), (cx - s * 0.58, cy - s * 0.8),
                   (cx - s * 0.25, cy - s * 0.5)], fill=(255, 190, 210, 255))
        d.polygon([(cx + s * 0.5, cy - s * 0.3), (cx + s * 0.58, cy - s * 0.8),
                   (cx + s * 0.25, cy - s * 0.5)], fill=(255, 190, 210, 255))
        head(d, cx, cy, s * 0.75, shade(acc, 1.05))
        eyes(d, cx, cy + s * 0.02, s * 0.85)
        d.line([cx - s * 0.45, cy + s * 0.1, cx - s * 0.15, cy + s * 0.14], fill=BLACK,
               width=max(2, int(s * 0.03)))
        d.line([cx + s * 0.45, cy + s * 0.1, cx + s * 0.15, cy + s * 0.14], fill=BLACK,
               width=max(2, int(s * 0.03)))
        torso(d, cx, cy + s * 0.6, s * 0.7, c2)
    elif kind == "racer":
        d.ellipse([cx - s * 0.6, cy - s * 0.5, cx + s * 0.6, cy + s * 0.3], fill=acc)
        d.rounded_rectangle([cx - s * 0.5, cy - s * 0.12, cx + s * 0.5, cy + s * 0.2],
                            radius=int(s * 0.1), fill=(30, 40, 60, 255))
        d.line([cx - s * 0.4, cy + s * 0.04, cx + s * 0.4, cy + s * 0.04],
               fill=(120, 200, 255, 255), width=max(2, int(s * 0.04)))
        d.rectangle([cx - s * 0.62, cy - s * 0.24, cx + s * 0.62, cy - s * 0.1], fill=shade(acc, 1.25))
        d.rectangle([cx - s * 0.62, cy + s * 0.2, cx + s * 0.62, cy + s * 0.34], fill=shade(acc, 0.7))
        torso(d, cx, cy + s * 0.62, s * 0.78, shade(acc, 0.85))
    elif kind == "sumo":
        head(d, cx, cy, s * 0.95, skin)
        d.ellipse([cx - s * 0.16, cy - s * 0.85, cx + s * 0.16, cy - s * 0.45], fill=BLACK)
        eyes(d, cx, cy + s * 0.05, s)
        mouth(d, cx, cy, s)
        d.arc([cx - s * 0.85, cy + s * 0.05, cx + s * 0.85, cy + s * 1.1], 180, 360, fill=c2,
              width=max(6, int(s * 0.16)))
        torso(d, cx, cy + s * 0.75, s * 0.85, skin)
    elif kind == "cyborg":
        head(d, cx, cy, s * 0.9, skin)
        d.polygon([(cx, cy - s * 0.62), (cx + s * 0.62, cy - s * 0.62),
                   (cx + s * 0.62, cy + s * 0.5), (cx, cy + s * 0.5)], fill=SILVER)
        d.rectangle([cx + s * 0.12, cy - s * 0.28, cx + s * 0.45, cy + s * 0.0],
                    fill=(255, 60, 60, 255))
        d.line([cx + s * 0.1, cy - s * 0.4, cx + s * 0.1, cy + s * 0.3], fill=(70, 74, 84, 255),
               width=max(2, int(s * 0.04)))
        d.ellipse([cx - s * 0.36, cy - s * 0.18, cx - s * 0.08, cy + s * 0.14],
                  fill=(255, 255, 255, 255))
        d.ellipse([cx - s * 0.3, cy - s * 0.08, cx - s * 0.16, cy + s * 0.08], fill=(14, 12, 18, 255))
        d.line([cx - s * 0.2, cy - s * 0.85, cx - s * 0.05, cy - s * 0.62], fill=BLACK,
               width=max(3, int(s * 0.06)))
        torso(d, cx, cy + s * 0.58, s * 0.78, shade(acc, 0.7))
        d.rectangle([cx - s * 0.2, cy + s * 0.35, cx + s * 0.2, cy + s * 0.62], fill=SILVER)
    else:
        head(d, cx, cy, s * 0.88, skin)
        d.pieslice([cx - s * 0.64, cy - s * 0.72, cx + s * 0.64, cy + s * 0.18], 180, 360, fill=acc)
        d.rectangle([cx - s * 0.9, cy - s * 0.26, cx + s * 0.02, cy - s * 0.1], fill=shade(acc, 0.75))
        eyes(d, cx, cy, s)
        mouth(d, cx, cy, s)
        torso(d, cx, cy + s * 0.58, s * 0.75, c2)
        d.rectangle([cx - s * 0.1, cy + s * 0.3, cx + s * 0.1, cy + s * 0.62], fill=(90, 60, 30, 255))
        d.ellipse([cx - s * 0.22, cy + s * 0.52, cx + s * 0.22, cy + s * 0.72], fill=acc)


def ball(key):
    fam = EXTRA_FAM.get(key, fam_for(key))
    top, base, acc = FAMS[fam]
    h = int(hashlib.md5(key.encode()).hexdigest(), 16)
    kind = FIXED.get(key, KINDS[h % len(KINDS)])
    skin = SKINS[(h >> 4) % len(SKINS)]
    c2 = shade(acc, 1.45 if sum(acc) < 420 else 0.6)

    img = Image.new("RGBA", (PX, PX), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    glow = Image.new("RGBA", (PX, PX), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([6, 6, PX - 6, PX - 6], fill=acc + (110,))
    glow = glow.filter(ImageFilter.GaussianBlur(14))
    img.alpha_composite(glow)

    d.ellipse([10, 10, PX - 10, PX - 10], fill=BLACK)

    inner = Image.new("RGBA", (PX, PX), (0, 0, 0, 0))
    idr = ImageDraw.Draw(inner)
    idr.ellipse([14, 14, PX - 14, PX - 14], fill=shade(base, 1.0))
    g2 = Image.new("RGBA", (PX, PX), (0, 0, 0, 0))
    ImageDraw.Draw(g2).ellipse([R - R // 2, 18, R + R // 2, PX // 2 + 20], fill=acc + (60,))
    inner.alpha_composite(g2.filter(ImageFilter.GaussianBlur(30)))
    mask = Image.new("L", (PX, PX), 0)
    ImageDraw.Draw(mask).ellipse([16, 16, PX - 16, PX - 16], fill=255)
    img.paste(inner, (0, 0), mask)

    portrait(d, R, R + 14, R * 0.62, kind, acc, c2, skin)

    d.ellipse([10, 10, PX - 10, PX - 10], outline=SILVER, width=12)
    d.arc([14, 14, PX - 14, PX - 14], 140, 330, fill=(250, 250, 252, 255), width=6)
    d.arc([12, 12, PX - 12, PX - 12], 300, 80, fill=(88, 92, 104, 255), width=6)
    d.ellipse([16, 16, PX - 16, PX - 16], outline=(40, 42, 50, 255), width=3)
    return img


def main():
    src = open(SRC, encoding="utf-8").read()
    keys = []
    seen = set()
    for k in re.findall(r'SYSTEM_FOLDERS\.put\("([^"]+)"', src):
        if k not in seen and k != "hbmame":
            seen.add(k)
            keys.append(k)
    n = 0
    for k in keys:
        ball(k).save(os.path.join(ART, "logo_%s.png" % k), "PNG", optimize=True)
        n += 1
    print("bolinhas:", n)


if __name__ == "__main__":
    main()
