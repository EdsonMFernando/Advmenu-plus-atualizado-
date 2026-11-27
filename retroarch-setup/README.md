# RetroArch 2026 — setup pronto para o Advmenu Plus

O Advmenu Plus abre o RetroArch instalado no aparelho; o APK OFICIAL
(libretro buildbot, versao 1.22.2) sai como artefato
**RetroArch-apk** do workflow "RetroArch oficial (download e publicacao)".

## 1. Instalar
- Baixe o artefato e instale o APK do seu aparelho:
  - `RetroArch-*-arm64.apk` — smartphones/handhelds modernos (Snapdragon, Tensor, Dimensity)
  - `RetroArch-*-armv7-neon.apk` — aparelhos antigos MediaTek/Unisoc
- Primeira abertura: Settings > Directory > Configure (padrao ok).

## 2. Nucleos (cores) — Online Updater > Core Downloader
Baixe uma vez; o Advmenu Plus abre cada sistema com o nucleo certo:

| Sistema (pasta do cartao) | Nucleo |
|---|---|
| arcade / mame / cps1 / cps2 / cps3 | MAME 2003-Plus ou FBNeo |
| hbmame / fba / fbneo | **libretro_hbmame** (NOSSO core, artefato `hbmame-libmain-*` do workflow HBMAME port) + FinalBurn Neo |
| zinc, model1, model2, pgm, pgm2 | MAME (Current) |
| mastersystem/sms, gamegear, megadrive, segacd, saturn, sg1000 | Genesis Plus GX / Picodrive / Yabause |
| nes/famicom/fds | Mesen / Nestopia UE |
| snes | Snes9x / bsnes |
| n64 | Mupen64Plus-Next |
| gameboy/gbc/gba | mGBA / VBA-M |
| nds | melonDS / DeSmuME |
| psx/ps1 | Beetle PSX / SwanStation |
| psp | PPSSPP (libretro) |
| dreamcast, naomi, naomi2, atomiswave | Flycast |
| pcengine/turbografx, supergrafx, pcfx | Beetle PCE / PCE Fast / PC-FX |
| neogeo, neogeocd | FinalBurn Neo / Neo CD |
| neogeopocket(+) | Beetle NeoPop |
| wonderswan(+) | Beetle Cygne |
| 3do | Opera |
| virtualboy | Beetle VB |
| amiga | PUAE |
| c64/vic20 | VICE |
| amstradcpc | Cap32 |
| msx/msx2 | blueMSX / fMSX |
| spectrum/zx81 | Fuse |
| dos/pcdos | DOSBox Pure |
| scummvm | ScummVM |

## 3. Como instalar o NOSSO core HBMAME (quando o build fechar)
1. Aba Actions > HBMAME port (CI) > artefato `hbmame-libmain-<abi>`
2. Copie o `libhbmame_libretro.so` para
   `Android/data/com.retroarch/files/cores/` (renomeie para
   `hbmame_libretro_libretro.so`) e o `hbmame_libretro.info` para
   `Android/data/com.retroarch/files/info/` (do repo: hbm-port/libretro/)
3. Load Core > HBMAME (Advmenu Plus port) — roda os drivers homebrew/hacks
   que o MAME Current nao tem.

## 4. BIOS/firmware (VOCE instala manualmente)
Pasta: `Android/data/com.retroarch/files/system/` — coloque as BIOS de cada
nucleo ali (ex.: `bios` do PS1, `gbc_bios`,.saturn bios, Dreamcast
`dc_boot.bin`/`dc_flash.bin`, PSP em `ppsspp`, etc.). O Advmenu Plus nao
distribui BIOS (copyright) — por isso o setup deixa a pasta so esperando os
arquivos que VOCE baixa.

## 5. Emuladores avulsos (standalone) — o que o Advmenu Plus ja abre sozinho
Winlator, GameHub, Eden (Switch), AetherSX2, PPSSPP, Dolphin, DuckStation,
RPCS3?, Vita3K, OpenBOR, melonDS, DraStic, Citra, Flycast, VPX/pinball
(Zen, Farsight, Zaccaria, Space Cadet). Basta instalar o APK do emulador e
colocar os jogos na pasta de sistema do cartao — o Advmenu Plus detecta e
abre. BIOS/firmware de cada um sao configurados dentro do proprio emulador.
