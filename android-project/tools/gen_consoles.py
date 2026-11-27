# -*- coding: utf-8 -*-
"""
Gera as imagens de CONSOLE do menu de SISTEMAS (arte ORIGINAL estilizada,
um arquetype por familia, paleta da familia). Arquivo: art/console_<key>.png.

Uso (da raiz do repositorio):
    python3 android-project/tools/gen_consoles.py

Regra do projeto: nada de arte de terceiros - consoles desenhados do zero.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_themes import FAMS, fam_for, system_keys_from_java  # noqa: E402

from PIL import Image, ImageDraw  # noqa: E402

CW, CH = 640, 480
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "main", "assets", "themes", "adv-multi", "art")

BODY = (46, 50, 58)
BODY2 = (66, 70, 82)
SILVER = (206, 210, 220)
DARK = (16, 16, 22)

# familia -> arquétipo do desenho
ARCH = {
    "nes": "cartridge", "nintendo": "cartridge", "atari": "cartridge",
    "openbor": "cartridge", "snes": "cartridge", "n64": "cartridge",
    "gameboy": "handheld", "nds": "handheld", "psp": "handheld",
    "sega": "cdconsole", "sony": "cdconsole",
    "arcade": "cabinet", "mugen": "cabinet",
    "pinball": "pinball",
    "computer": "computer",
    "microsoft": "pctower",
    "retroarch": "gamepad",
    "racing": "wheel",
    "shooting": "lightgun",
}


def rr(d, box, r, fill, outline=None, width=6):
    d.rounded_rectangle(box, radius=r, fill=fill,
                        outline=outline, width=width if outline else 0)


def screen(d, box, accent):
    rr(d, box, 10, DARK, SILVER, 5)
    x0, y0, x1, y1 = box
    d.line([(x0 + 14, y0 + 10), (x1 - 14, y0 + 10)], fill=accent + (120,), width=4)
    d.ellipse([x1 - 40, y1 - 34, x1 - 18, y1 - 12], outline=accent + (160,), width=4)


def dpad(d, cx, cy, s, color=SILVER):
    d.rectangle([cx - s, cy - s // 3, cx + s, cy + s // 3], fill=color)
    d.rectangle([cx - s // 3, cy - s, cx + s // 3, cy + s], fill=color)


def cartridge(d, accent):
    d = d
    # corpo
    rr(d, [110, 170, 530, 400], 26, BODY, SILVER, 6)
    rr(d, [110, 170, 530, 240], 26, BODY2)
    # slot + cartucho
    rr(d, [180, 190, 460, 226], 8, DARK)
    rr(d, [210, 96, 430, 210], 12, (58, 62, 72), SILVER, 6)
    rr(d, [232, 118, 408, 178], 6, accent + (255,))
    # portas e led
    d.ellipse([160, 330, 216, 386], fill=DARK, outline=SILVER, width=5)
    d.ellipse([424, 330, 480, 386], fill=DARK, outline=SILVER, width=5)
    d.ellipse([300, 300, 340, 340], outline=accent + (255,), width=8)


def handheld(d, accent):
    rr(d, [226, 50, 414, 430], 36, BODY, SILVER, 6)
    screen(d, [252, 92, 388, 216], accent)
    dpad(d, 286, 300, 34)
    d.ellipse([344, 276, 382, 314], fill=accent + (255,))
    d.ellipse([362, 318, 400, 356], fill=(230, 80, 80))
    for i in range(4):
        d.ellipse([286 + i * 14, 388, 296 + i * 14, 398], fill=SILVER)


def pspwide(d, accent):
    rr(d, [90, 150, 550, 350], 70, BODY, SILVER, 6)
    screen(d, [220, 176, 420, 288], accent)
    dpad(d, 160, 250, 28)
    d.ellipse([470, 226, 502, 258], fill=accent + (255,))
    d.ellipse([496, 262, 528, 294], fill=(230, 80, 80))
    rr(d, [96, 160, 140, 200], 14, BODY2)
    rr(d, [500, 160, 544, 200], 14, BODY2)


def nds(d, accent):
    rr(d, [210, 60, 430, 216], 20, BODY, SILVER, 6)
    screen(d, [232, 80, 408, 196], accent)
    rr(d, [210, 226, 430, 424], 20, BODY, SILVER, 6)
    screen(d, [232, 246, 408, 362], accent)
    dpad(d, 258, 396, 18)
    d.ellipse([368, 384, 396, 412], fill=accent + (255,))


def cdconsole(d, accent):
    rr(d, [110, 190, 530, 400], 30, BODY, SILVER, 6)
    rr(d, [110, 190, 530, 262], 30, BODY2)
    d.ellipse([268, 226, 372, 330], fill=DARK, outline=SILVER, width=6)
    d.ellipse([296, 254, 344, 302], outline=accent + (255,), width=8)
    rr(d, [150, 340, 250, 372], 10, DARK)
    d.ellipse([420, 342, 460, 382], outline=accent + (255,), width=8)


def cabinet(d, accent):
    rr(d, [200, 50, 440, 430], 24, BODY, SILVER, 6)
    rr(d, [214, 66, 426, 128], 14, accent + (255,))
    screen(d, [228, 146, 412, 268], accent)
    rr(d, [214, 288, 426, 344], 12, BODY2, SILVER, 4)
    d.ellipse([246, 296, 296, 336], fill=DARK, outline=SILVER, width=5)
    d.rectangle([258, 306, 284, 326], fill=accent + (255,))
    for i in range(3):
        d.ellipse([330 + i * 26, 302, 352 + i * 26, 324], fill=(230, 80, 80))
    rr(d, [262, 358, 378, 410], 10, DARK, SILVER, 4)


def pinball(d, accent):
    rr(d, [190, 60, 450, 420], 40, BODY, SILVER, 8)
    rr(d, [216, 84, 424, 396], 26, DARK, accent + (180,), 5)
    for bx, by, br in ((290, 160, 26), (350, 160, 26), (320, 230, 26)):
        d.ellipse([bx - br, by - br, bx + br, by + br],
                  outline=accent + (255,), width=7)
    d.ellipse([306, 120, 334, 148], fill=SILVER)
    d.rectangle([238, 356, 318, 380], fill=SILVER)
    d.rectangle([322, 356, 402, 380], fill=SILVER)


def computer(d, accent):
    rr(d, [170, 80, 470, 300], 18, BODY, SILVER, 6)
    screen(d, [192, 102, 448, 278], accent)
    rr(d, [150, 320, 490, 396], 14, BODY2, SILVER, 5)
    for row in range(3):
        rr(d, [170, 332 + row * 20, 470, 344 + row * 20], 5, DARK)


def pctower(d, accent):
    rr(d, [250, 60, 390, 430], 16, BODY, SILVER, 6)
    rr(d, [262, 76, 378, 120], 8, BODY2)
    for i in range(3):
        rr(d, [262, 136 + i * 44, 378, 168 + i * 44], 6, DARK, SILVER, 3)
    d.ellipse([300, 330, 340, 370], outline=accent + (255,), width=8)
    rr(d, [262, 392, 378, 416], 6, DARK)


def gamepad(d, accent):
    d.ellipse([130, 190, 290, 350], fill=BODY, outline=SILVER, width=6)
    d.ellipse([350, 190, 510, 350], fill=BODY, outline=SILVER, width=6)
    rr(d, [210, 214, 430, 330], 40, BODY, SILVER, 6)
    dpad(d, 208, 268, 30)
    for bx, by in ((452, 238), (484, 268), (452, 298), (420, 268)):
        d.ellipse([bx - 13, by - 13, bx + 13, by + 13], fill=accent + (255,),
                  outline=DARK, width=3)


def wheel(d, accent):
    d.ellipse([120, 90, 520, 410], outline=accent + (255,), width=34)
    d.ellipse([236, 176, 404, 324], fill=BODY, outline=SILVER, width=8)
    d.ellipse([286, 216, 354, 284], fill=DARK, outline=accent + (255,), width=6)
    d.rectangle([196, 238, 262, 262], fill=BODY)
    d.rectangle([378, 238, 444, 262], fill=BODY)
    d.polygon([(320, 250), (300, 380), (340, 380)], fill=BODY)


def lightgun(d, accent):
    rr(d, [200, 120, 470, 220], 26, BODY, SILVER, 6)
    rr(d, [214, 134, 300, 206], 14, accent + (120,))
    d.polygon([(360, 220), (450, 220), (420, 400), (330, 400)],
             fill=BODY, outline=SILVER)
    d.rectangle([340, 240, 370, 280], fill=SILVER)
    d.ellipse([300, 130, 330, 160], fill=SILVER)
    d.ellipse([70, 300, 150, 380], outline=(230, 80, 80), width=8)
    d.ellipse([90, 320, 130, 360], fill=(230, 80, 80))
    d.ellipse([480, 300, 560, 380], outline=(80, 190, 255), width=8)
    d.ellipse([500, 320, 540, 360], fill=(80, 190, 255))


DRAWERS = {
    "cartridge": cartridge,
    "handheld": handheld,
    "pspwide": pspwide,
    "nds": nds,
    "cdconsole": cdconsole,
    "cabinet": cabinet,
    "pinball": pinball,
    "computer": computer,
    "pctower": pctower,
    "gamepad": gamepad,
    "wheel": wheel,
    "lightgun": lightgun,
}

FAM_ARCH = dict(ARCH)
FAM_ARCH["nds"] = "nds"
FAM_ARCH["psp"] = "pspwide"


def main():
    if not os.path.isdir(OUT):
        raise SystemExit("pasta de arte nao encontrada: " + OUT)
    keys = system_keys_from_java()
    n = 0
    for k in keys:
        fam = fam_for(k)
        accent = FAMS[fam][2]
        arch = FAM_ARCH.get(fam, "cartridge")
        img = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        # sombra suave no chao
        d.ellipse([150, 396, 490, 452], fill=(0, 0, 0, 90))
        img = img.filter(__import__("PIL.ImageFilter", fromlist=["GaussianBlur"])
                         .GaussianBlur(10))
        d = ImageDraw.Draw(img)
        DRAWERS[arch](d, accent)
        img.save(os.path.join(OUT, "console_%s.png" % k), "PNG", optimize=True)
        n += 1
    print("consoles gerados:", n)


if __name__ == "__main__":
    main()
