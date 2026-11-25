# Advmenu-Android (AdvanceMAME para Android)

Porta do [AdvanceMAME](http://www.advancemame.it/) (fork `amadvance`) para Android, compilada
direto do codigo-fonte open source para gerar o APK com o menu (`advmenu`) e o emulador
(`advmame`) do MAME 0.106.

## Estrutura

```
advancemame/            codigo-fonte original do AdvanceMAME (vendored; ver sync-vendor.sh)
android-project/        projeto Gradle que gera o APK (SDL2 2.30.11 vendored; ver sync-vendor.sh)
  app/src/main/cpp/     CMakeLists + glue Android (main.c) + config.h
  tools/sync-vendor.sh  baixa/prepara as arvores vendor (advancemame, SDL2) e gera as tabelas 68000
  patches/              patches necessarios ao build Android
```

- `android-project/app/src/main/cpp/config/config.h` e `advancemame/advance/lib/config.h` —
  mesma copia do config.h manual (substitui o do autoconf; resolve os tamanhos por `__LP64__`).
  O `portable.h` inclui `config.h` a partir do proprio diretorio `advance/lib`, igual ao build
  autoconf original.
- `android-project/app/src/main/cpp/AdvSources.cmake` / `AdvDefines.cmake` — listas de fontes e
  defines HAS_* geradas a partir do sistema de make original (advancemame.mak)
- `android-project/app/src/main/cpp/main.c` — parte Android; chama `advmame_main()`
  (o `main` original do `advance/linux/os.c` renomeado para `advmame_main`)

## Compilando o APK

Necessario: JDK 17, Android SDK (API 34), NDK 26.3, CMake >= 3.22 e Ninja, e `git`.

**Passo 1 — preparar as arvores vendor (roda uma vez):**
o codigo-fonte do AdvanceMAME, o SDL2 e as tabelas geradas do 68000 nao estao no repositorio.
O script baixa tudo pinned (commit/versao exata), aplica o patch Android e gera o que falta:

```bash
cd android-project
./tools/sync-vendor.sh
```

**Passo 2 — compilar:**

```bash
export JAVA_HOME=/caminho/do/jdk-17
export ANDROID_HOME=/caminho/do/android-sdk
export GRADLE_USER_HOME=/tmp/gradle-home   # opcional

cd android-project
gradle assembleDebug        # Gradle 8.7+ do sistema; gera app/build/outputs/apk/debug/app-debug.apk
```

## Baixar o APK pronto (GitHub Actions)

O workflow **Build APK** (`.github/workflows/build-apk.yml`) compila o APK automaticamente a
cada push: va na aba **Actions** do repositorio → clique no run mais recente → baixe o artefato
**Advmenu-Plus-apk**. Tambem pode ser disparado manualmente pelo botao **Run workflow**.

Se nao tiver o Gradle do sistema, rode `gradle wrapper` uma vez dentro de `android-project/`
para gerar o `gradlew` local (o jar do wrapper tambem nao vai versionado).

O build nativo roda automaticamente para as duas ABIs: `arm64-v8a` e `armeabi-v7a`.
As duas cobrem os smartphones modernos (Snapdragon/Kryo) e os antigos MediaTek (Cortex-A7/A15) —
GPU e transparente (o video e feito pelo SDL2 com fallback de software, funciona em Mali e Adreno).

A versao release (`assembleRelease`) precisa de `local.properties` com `storeFile/storePassword`
assim como qualquer projeto Android.

## Rodando

Quando abre, o app mostra um **front-end no estilo do ES-DE**: carrossel de jogos com o item
selecionado maior e os vizinhos esmaecidos, capa do jogo como fundo (quando existir), barra
superior com relogio e o nome do sistema, e o contador de jogos no rodape. Navega por toque ou
pelo d-pad de gamepad/teclado, e ao tocar (ou apertar A/Enter) inicia o `advmame` na ROM escolhida.

O instalador extrai `assets/data` para o `filesDir` e cria os arquivos de configuracao em
`/data/data/<pkg>/files` (`advmame.rc`, `advmenu.rc`).

### Pastas no cartao de memoria / armazenamento (estilo AdvMenu Plus)

Alem das pastas internas do app, o front-end varre CADA armazenamento do aparelho
(memoria interna `/sdcard/` e cartao de memoria fisico `/storage/XXXX-XXXX/`) procurando:

| Pasta | Conteudo |
|---|---|
| `ROMs/` (ou `roms`, `rom`) | ROMs em `.zip`/`.7z` (subpastas ate 2 niveis tambem sao varridas) |
| `snapshot/` (ou `snap`, `snaps`) | snapshot avulso por jogo: `<rom>.mp4`, `<rom>.avi`, `<rom>.mkv`, `<rom>.webm`, `<rom>.jpg`, `<rom>.png` ... |
| `layout/` (ou `layouts`) | `background.jpg`/`background.png` (ou qualquer imagem solta) usada como fundo do menu |
| `emuladores/` (ou `emulators`) | APKs de emuladores/apps de PC avulsos (Winlator, GameHub etc.) — aparecem no botao **EMULADORES** |
| pastas de sistema (lista abaixo) | jogos soltos ou em zip — aparecem no botao **EMULADORES** e abrem no emulador instalado |
Pastas de sistema aceitas em qualquer armazenamento (o NOME da pasta define o sistema):
`atari2600`, `atari5200`, `atari7800`, `atari8bit`, `atarilynx`, `atarijaguar`,
`colecovision`, `intellivision`, `vectrex`, `mastersystem`/`sms`, `megadrive`/`genesis`,
`sega32x`, `segacd`, `gamegear`, `nes`/`famicom`, `snes`, `n64`, `gameboy`/`gb`, `gbc`,
`gba`, `nds`/`3ds`, `psx`/`ps1`, `ps2`, `psp`, `psvita`/`vita`, `dreamcast`, `saturn`,
`neogeo`, `neogeopocket`, `wonderswan`, `switch`, `openbor`, `hbmame`, `fba`/`fbneo`,
`arcade`, `pc`/`windows`.

Toque no sistema -> lista de jogos -> toque no jogo -> o Android abre no emulador
instalado para aquele tipo de arquivo (mesmo esquema do ES-DE: o jogo e passado por
intent, e voce escolhe o app na primeira vez). Se nada abrir, instale o emulador do
sistema (RetroArch cobre quase tudo).

**Arcade especial:** as ROMs de `hbmame` e `fba`/`fbneo` NAO sao do MAME 0.106 — sao
conjuntos diferentes (homebrew/hacks e FinalBurn). Essas rodarao no app externo escolhido
(FinalBurn Neo/RetroArch), nao no nucleo embutido do Advmenu Plus.

Na primeira abertura o app pede a permissao "Acesso a todos os arquivos" — conceda para
o menu enxergar o cartao de memoria. As ROMs encontradas no cartao sao somadas as internas
e o emulador tambem enxerga elas (o app adiciona as pastas ao `dir_rom` do `advmame.rc`).

Exemplo de organizacao no cartao:

```
ROMs/        concorone.zip  (pode ter subpastas por sistema)
snapshot/    concorone.mp4  (ou concorone.jpg / .avi ...)
layout/      background.jpg
emuladores/  Winlator.apk, GameHub.apk ...  (apps de PC avulsos)
```

## Sistemas e emuladores externos (botao EMULADORES)

O botao **EMULADORES** (canto inferior-direito do menu) lista e abre, com um toque,
todos os apps abaixo que estiverem instalados, mais os `.apk`s da pasta
`emuladores/` do cartao (nao instalado = o menu mostra o caminho para instalar):

| Sistema | App Android | Como e o desempenho |
|---|---|---|
| Arcade (MAME 0.106) | **embutido no proprio APK** | roda no proprio Advmenu Plus, leve e fluido |
| PC (Windows) | Winlator, GameHub | bom em Snapdragon; fraco em MediaTek basicos |
| Switch 1 | Eden (`dev.eden.eden_emulator`) | muito bom em aparelhos com Vulkan forte |
| PS2 | AetherSX2 | excelente em flagships |
| PSP | PPSSPP | excelente em quase todo aparelho |
| GameCube/Wii | Dolphin | bom em flagships |
| PS3 | RPCS3 (`net.rpcs3.rpcs3`) | pesado: so flagships e jogos especificos |
| PS Vita | Vita3K (nao oficial) | parcial, em desenvolvimento |
| OpenBOR | OpenBOR (`org.openbor.engine`) | leve (2D), roda bem; mods `.pak` escolhidos dentro do app |
| MUGEN | via **Winlator** | leve (2D): roda bem em aparelho medio/forte |
| TeknoParrot | via **Winlator** | experimental: nao existe versao Android; todo Windows emulado |
| PS4 / PS5 | **nao existe emulador Android** | impossivel hoje (emuladores "PS4" na Play Store sao falsos) |
| Switch 2 | **nao existe emulador utilizavel** | emulacao do Switch 2 esta no comeco mesmo no PC |

O Advmenu Plus ABRE esses apps — a configuracao dos jogos (BIOS/keys/ firmware, pastas
de ROMs de cada sistema, graficos) e feita dentro de cada emulador. Nenhum front-end
garante FPS de outro app: os jogos do PROPRIO APK (arcade MAME) e que rodam sem
travamento e sem perda de frames, pois sao processados pelo nucleo embutido.

### Snapshots (estilo ES-DE)

Por jogo `<rom>`, coloque as midias em `Android/data/com.edsonmfernando.advancemame/files/media/`
OU na pasta `snapshot/` do cartao (item acima):

- **Video de fundo** (looping, sem som): `<rom>.mp4`, `<rom>.webm` ou `<rom>.mkv`
- **AVI tambem e tentado** — o Android nativo nao garante AVI; se o aparelho nao tiver o
decodificador, o front-end cai automaticamente na imagem
- **Imagem de fundo e miniatura na lista**: `<rom>.jpg`, `.jpeg`, `.png`, `.webp` ou `.gif`
- Sem midia, o jogo aparece so com o nome

Prioridade: video primeiro, imagem como fallback.

As ROMs de exemplo (zips binarios) nao vao no repositorio. Para jogar, coloque as ROMs em
`/sdcard/ROMs`, em `Android/data/com.edsonmfernando.advancemame/files/rom` OU, antes de compilar,
coloque os zips em `android-project/app/src/main/assets/rom/` para irem embutidas no APK (o app
extrai para o `filesDir` na primeira execucao; a ROM padrao, se nenhuma for passada, e `gridlee`).

Voce tambem pode escolher a ROM por Intent (o launcher normal abre a lista do front-end):

```bash
adb shell am start -n com.edsonmfernando.advancemame/.MainActivity --es rom nomedarom
```

## Editando o codigo e recompilando

1. Edite o que quiser em `advancemame/` (nucleo, drivers, etc.) ou no glue Android
   (`android-project/app/src/main/cpp/`).
2. Se mudar algo dependente de plataforma, guarde dentro de `#ifdef __ANDROID__` (os pontos ja
   divergentes estao marcados assim, veja abaixo).
3. Rode `gradle assembleDebug` de novo — so recompila o que mudou.

## Divergencias do build original (manter em mente)

- As flags seguem o `advance/emu.mak` original: `advance/osd/*.c` compila com `-DMAME` e os
  includes do mame (e o que traz `osdepend.h`/`UINT32`); `advance/lib` compila sem; a camada
  do MAME ganha `PI=M_PI` e tem `MAME` habilitado.
- `thmono.c`/`thfifo.c` fora da lib de thread (com `USE_SMP` o upstream usa apenas `thdouble.c`).
- `advance/linux/os.c` usa `main=advmame_main;` para o entrypoint SDL.
- Vicdual (mame/sndhrdw/vicdual.c): blocos `#ifdef __ANDROID__`, porque clang nao calcula `sqrt()`
  em inicializador estatico (gcc faz).
- `alien/deps/miniz-3.1.2/miniz_export.h`: reescrito com `MINIZ_EXPORT` vazio para build estatico.
- O objeto SDL2 usa C++ (hidapi), por isso `ANDROID_STL=c++_static` e o `c++_static`/`c++abi`
  ligados por ultimo na `libmain.so`.
