#!/usr/bin/env python3
# Gerador dos logos circulares de cada sistema (carrossel do menu de
# SISTEMAS, padrao dos videos): recorte circular da arte do tema com anel
# branco + anel colorido derivado do tema. Rode da raiz do repo:
#   python3 android-project/tools/gen-logos.py
from PIL import Image, ImageDraw, ImageFilter
import os, glob

ART = "/workspace/github__EdsonMFernando__Advmenu-plus-atualizado-/android-project/app/src/main/assets/themes/adv-multi/art"
OUT = os.path.join(ART)
PX = 320  # logo interno em alta, o app escala

def ring_colors(syskey):
    # cor do anel: pega o pixel medio do fundo do sistema como referencia
    return None

for p in sorted(glob.glob(os.path.join(ART, "background_*.jpg"))):
    key = os.path.basename(p)[len("background_"):-len(".jpg")]
    src = Image.open(p).convert("RGB")
    w, h = src.size
    # crop do bonequinho da esquerda (o centro do fundo agora e a moldura
    # metalica do snapshot, que nao serve de logo)
    cx, cy, half = 310, 420, 300
    sq = src.crop((cx - half, cy - half, cx + half, cy + half))
    logo = sq.resize((PX, PX), Image.LANCZOS)
    out = Image.new("RGBA", (PX + 40, PX + 40), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.ellipse([6, 6, PX + 34, PX + 34], fill=(12, 10, 16, 255))
    mask = Image.new("L", (PX * 4, PX * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, PX * 4 - 1, PX * 4 - 1), fill=255)
    mask = mask.resize((PX, PX), Image.LANCZOS)
    out.paste(logo, (20, 20), mask)
    d.ellipse([20, 20, PX + 20, PX + 20], outline=(250, 248, 244, 255), width=7)
    # anel colorido externo derivado da media do logo
    small = logo.resize((1, 1))
    ac = small.getpixel((0, 0))
    acc = (min(255, ac[0] + 60), min(255, ac[1] + 60), min(255, ac[2] + 60), 255)
    d.ellipse([6, 6, PX + 34, PX + 34], outline=acc, width=8)
    out.save(os.path.join(OUT, "logo_%s.png" % key), "PNG", optimize=True)
print("logos ok")
