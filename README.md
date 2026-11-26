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

**Passo 1 — preparo das arvores vendor (AUTOMATICO):**
o Gradle roda sozinho o `tools/sync-vendor.sh` antes de compilar (tarefa `syncVendor`
registrada em `app/build.gradle` — `preBuild.dependsOn`). O script e idempotente:
depois da primeira vez e instantaneo. Nada e rodado a mao; se quiser, pode rodar
`cd android-project && bash tools/sync-vendor.sh` isoladamente.

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

O build nativo roda automaticamente para quatro ABIs: `arm64-v8a`, `armeabi-v7a`,
`x86_64` e `x86` (cobertura total de CPU: Snapdragon/MediaTek/Exynos/Unisoc no ARM,
Mali/Adreno/PowerVR na GPU, e ainda tablets/Chromebooks Intel e AMD).
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

### Temas (EmulationStation) — vem pronto, nativamente, e aceita temas avulsos

O front-end já vem com **DOIS temas nativos embutidos no APK** (funcionam offline, sem baixar
nada, e são ativos já na primeira abertura — não precisa permissão de cartão):

- **`NATIVO: adv-multi`** (PADRÃO) — tema **autoral criado do zero para o Advmenu Android**, no
  mesmo estilo dos multijogos de PC do canal Clandoordie: fundo grafite escuro com trama sutil,
  item selecionado branco e maior, vizinhos acinzentados e destaque **azul**. Esse tema foi
  desenhado para este projeto (não existe em nenhum site).
- **`NATIVO: carbon`** — o tema clássico do EmulationStation/RetroPie, com fundo textura de
  carbono e destaque vermelho (crédito e licença CC BY-NC-SA em `LICENCA.txt` dentro da pasta).

- Botao **TEMA** no rodape: lista todos os temas encontrados e troca na hora (escolha salva).
- Temas avulsos: baixe qualquer tema do EmulationStation e jogue em `layout/<nome-do-tema>/theme.xml`
  (subpastas ate 2 niveis) — o app le as cores (`background`, `selected`, `primary`, `secondary`,
  `textColor`), variaveis `${}` e includes, e **auto-ajusta**: nada para editar manualmente.
  Tema solto na raiz de `layout/` aparece como `(na pasta layout)`.
- O tema pinta TUDO: fundo do menu (imagem `background` do tema ou `background.jpg` avulso),
  nome do sistema, jogos selecionados/vizinhos, relogio, contador, mensagem de lista vazia —
  e tambem o **menu de navegacao de SISTEMAS/EMULADORES** e o menu de **jogos de cada sistema**
  (mesmo menu, mesmo visual, para cada emulador/sistema da navegacao).

- **Fundo por sistema** (ex.: o olho de dragao de fogo preto do HBMAME): quando voce entra na
  lista de jogos de um sistema, o app procura na ordem: `art/background_<sistema>.png|jpg` dentro
  do tema ativo, depois `layout/backgrounds/<sistema>.png|jpg` no cartao (coloca ali o seu fundo
  propio para qualquer sistema, sem editar nada). Saindo da lista, volta o fundo global do tema.
- **Temas nativos de TODOS os emuladores** (estilo dos multijogos dos videos): o tema nativo
  `adv-multi` ja vem com fundo Full HD proprio para os 125+ sistemas da navegacao
  (`art/background_<sistema>.jpg` — arte ORIGINAL deste projeto: gradiente escuro por familia,
  mascote e silhueta de console/portatil/CD/gabinete/pinball; o HBMAME usa o dragao em fogo do
  usuario). Na tela so aparece o **nome do emulador** no topo e o **nome do jogo + snapshot**
  (video AVI/MP4 ou imagem JPG) na lista — sem fabricante, ano ou genero.
  Para regenerar a arte: `python3 android-project/tools/gen-themes.py`.

### Navegacao estilo multijogos (SISTEMAS + FAVORITOS)

Como nos multijogos de PC (estilo Clandoordie/AdvMenu Plus):

- **SISTEMAS**: abre o menu de navegacao com TODAS as pastas de sistema/emuladores detectadas
  (ex.: `PSP (90)`, `PS2 (30)`, `PC GAMES (325)`) — clique num sistema e a lista de jogos abre
  **dentro do menu principal, com o tema aplicado** (nome completo do jogo + snapshot de
  video/imagem como fundo). Tecla B/botao voltar retorna um nivel.
- **FAVORITOS**: filtra a lista so com os favoritos.
- Toque longo num jogo do menu principal: marca/desmarca **favoritos** (aparece com ★).
- O jogo sempre aparece com o NOME COMPLETO e o snapshot (video MP4/AVI/WebM/MKV ou imagem
  JPG/PNG/WebP) toca como fundo do menu, igual no video.
Pastas de sistema aceitas em qualquer armazenamento (o NOME da pasta define o sistema
— nomes no mesmo espirito da base ScreenScraper www.screenscraper.fr, mais de 100 sistemas):

- **Consoles e portateis:** `atari2600`/`a2600`, `atari5200`, `atari7800`, `atari8bit`,
  `atarilynx`, `atarijaguar`, `colecovision`, `intellivision`, `vectrex`, `astrocade`,
  `mastersystem`/`sms`, `megadrive`/`genesis`, `sega32x`, `segacd`, `gamegear`, `sg1000`,
  `saturn`, `dreamcast`, `nes`/`famicom`, `fds`, `snes`, `n64`, `gameboy`/`gb`, `gbc`,
  `gba`, `nds`, `3ds`, `virtualboy`, `gamecube`/`gc`, `wii`, `wiiu`, `switch`, `switch2`,
  `pcengine`/`turbografx`/`turbografx16`, `supergrafx`, `pcfx`, `neogeo`, `neogeocd`,
  `neogeopocket`, `neogeopocketcolor`/`ngpc`, `wonderswan`, `wonderswancolor`, `3do`,
  `megaduck`, `supervision`, `gamate`, `pv1000`, `psx`/`ps1`, `ps2`, `psp`, `psvita`/`vita`,
  `ps3`, `ps4`, `ps5`, `xbox`, `xbox360`, `xbla` (Xbox Live Arcade)
- **Arcade:** `arcade`, `mame`, `hbmame`, `fba`/`fbneo`/`finalburn`, `cps1`, `cps2`, `cps3`,
  `naomi`, `naomi2`, `atomiswave`, `stv`, `lindbergh`, `daphne` (LaserDisc),
  `taitotypex`/`typex` (Taito Type X), `nesicaxlive`/`nesica` (Nesica X Live),
  `teknoparrot`, `mugen`, `openbor`, `pinball`, `coinops`, `retroarch`
- **Computadores:** `amiga`, `amigacd32`, `c64`, `c128`, `vic20`, `amstradcpc`/`cpc`,
  `msx`, `msx2`, `spectrum`/`zxspectrum`, `zx81`, `dos`/`pcdos`, `macintosh`, `apple2`,
  `atarist`, `x68000`, `pc88`, `pc98`, `scummvm`, `pc`/`windows`/`winlator`

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
| Sega Model 1 / 2 | RetroArch (core MAME Current) | roda (MAME emula essas placas) |
| Sega Model 3 | via **Winlator** (Supermodel PC) | experimental: nao existe port Android do Supermodel |
| Zinc (ZN1/ZN2) | RetroArch (core MAME Current) | roda (MAME emula essas placas) |
| PS Vita | Vita3K (nao oficial) | parcial, em desenvolvimento |
| OpenBOR | OpenBOR (`org.openbor.engine`) | leve (2D), roda bem; mods `.pak` escolhidos dentro do app |
| MUGEN | via **Winlator** | leve (2D): roda bem em aparelho medio/forte |
| TeknoParrot | via **Winlator** | experimental: nao existe versao Android; todo Windows emulado |
| PS4 / PS5 | PS4: ports experimentais de shadPS4 (Bachata-S4, shandroidPS4, SharpEmu) — PS5: Kyty/PS5Droid/OmniEmu/AnyPS5 | experimental: poucos jogos rodam, exige aparelho forte; a rota abre o emulador instalado pela pasta `ps4/` ou `ps5/` |
| Switch 2 | **nao existe emulador Android** (no PC: Eden-NX2, so Windows e experimental) | a pasta `switch2/` ja esta pronta no Advmenu; quando um fork do Eden com Switch 2 sair para Android, entra pela mesma rota |

### Eden (emulador unico Switch 1, pronto para o Switch 2)

- O **Eden** (`git.eden-emu.dev/eden-emu/eden`, open source) e o emulador unico de
  Nintendo Switch do Advmenu Android: rota padrao `switch/` **e** `switch2/`,
  deteccao automatica por pacote (`dev.eden.eden_emulator`) ou por nome/prefixo
  "eden" em emuladores avulsos, com RetroArch como segunda opcao.
- O APK oficial Android do Eden e **arm64** (Snapdragon, MediaTek Dimensity e
  qualquer aparelho com GPU Mali/Adreno via Vulkan) — baixe em
  `eden-emu.dev/downloads` ou nos releases oficiais. Nao existe build armv7
  (o JIT do Switch exige arm64), entao aparelhos MediaTek muito antigos ficam
  fora do Switch; todos os demais sistemas do Advmenu seguem funcionando neles.
- **Switch 2 hoje**: nenhum fork do Eden com Switch 2 (Eden-NX2 e variantes)
  publica build Android — sao experimentais e so para Windows. O Advmenu ja
  reconhece `.nsp/.xci/.nsz/.xcz/.nca` na pasta `switch2/`; assim que existir
  um Eden Android com Switch 2, os jogos abrem sem mudar nada no Advmenu.
- Chaves (`prod.keys`) e firmware NUNCA vêm embundados (nem no Eden, nem aqui)
  — forneca os seus dentro do proprio Eden.

O Advmenu Plus ABRE esses apps — a configuracao dos jogos (BIOS/keys/ firmware, pastas
de ROMs de cada sistema, graficos) e feita dentro de cada emulador. Nenhum front-end
garante FPS de outro app: os jogos do PROPRIO APK (arcade MAME) e que rodam sem
travamento e sem perda de frames, pois sao processados pelo nucleo embutido.

### Como nos videos dos multijogos de PC (RetroArch/TeknoParrot/Naomi/Type X/NesicaXLive)

Os pacotes de multijogos de PC com AdvMenu Plus (tutoriais do YouTube com RetroArch,
TeknoParrot, Naomi, MAME, Taito Type X e NesicaXLive) tem equivalente 1:1 no Advmenu
Android:

- `arcade`/`mame` → nucleo MAME embutido no proprio APK (Mais Rapido, sem aquecer)
- `retroarch` → jogos soltos no cartao abrem direto no RetroArch
- `naomi`/`naomi2` → RetroArch (Flycast) ou Winlator/Demul
- `teknoparrot`, `nesicaxlive`/`nesica`, `taitotypex`/`typex`, `lindbergh`, `coinops`
  → **Winlator** automaticamente (sao bibliotecas de Windows)
- `mugen`, `pc`/`windows`, `dos` → Winlator

### Emuladores "de Windows" no Android (TeknoParrot, Taito Type X, MUGEN, Lindbergh, DOS)

TeknoParrot, Taito Type X e Lindbergh sao CARREGADORES de Windows (codigo fechado,
amarrados as APIs do Windows) — nao existe source para portar, entao a rota correta
no Advmenu Android e o **Winlator** (Windows emulado dentro do Android): as pastas
`teknoparrot`, `taitotypex`/`typex`, `lindbergh`, `mugen`, `dos`/`pcdos` e `pc`/`windows`
do cartao abrem os jogos DIRETO no Winlator automaticamente (o menu escolhe o app
preferido por sistema; se o Winlator nao estiver instalado, abre o escolhedor normal).

**Setup automatico de pastas (nada manual):** na primeira abertura o Advmenu cria
em cada armazenamento as pastas `ROMs`, `snapshot`, `layout`, `emuladores` E **todas
as pastas de sistema** (as ~100 + pastas listadas acima, incluindo `teknoparrot`,
`mugen`, `pc` etc.) — so e preciso jogar os jogos dentro delas.

**Combo TeknoParrot/PC → Winlator:** ao tocar num jogo `.exe` dessas pastas, o menu
abre o Winlator (tenta o protocolo de shortcut dos forks CMod com `shortcut_path`;
no Winlator oficial abre a tela de shortcuts, onde o shortcut criado uma unica vez
por jogo dispara direto). Instalar o TeknoParrot e os jogos DENTRO do container do
Winlator e o setup unico que nenhum front-end pode pular — o resto e automático.

- **MUGEN nativo:** existe o motor open-source **Ikemen GO** (sucessor do MUGEN, tem
  build para Android na comunidade) — pode deitar na pasta `emuladores/` como APK e
  usar a pasta `mugen` do cartao para os chars/stages.
- **OpenBOR:** JA existe versao Android oficial (`org.openbor.engine`) — a pasta
  `openbor` abre os mods `.pak` direto no app.
- **PS4/PS5/Switch 2:** nao existe emulador Android (nem no PC completo) — impossivel
  hoje; documentado de forma honesta porque emuladores "PS4" na Play Store sao falsos.

### RetroArch 1.22.2 (oficial) no Advmenu Android

- Instale o **RetroArch 1.22.2** por uma via oficial: Google Play, ou
  `retroarch.com` / GitHub do libretro (APK oficial assinado). Nao reempacotamos esse
  APK dentro do Advmenu (app de terceiros, licenca propria).
- O Advmenu detecta **sozinho** o RetroArch instalado (pelo pacote `com.retroarch` ou
  pelo nome do app, incluindo variantes 64-bit/forks) e abre nele a pasta `retroarch/`
  do cartao e TODO sistema que nao tiver emulador nativo instalado.
- Nucleos: dentro do RetroArch > Online Updater > Core Downloader (tabela abaixo).
- BIOS/system: coloque os SEUS arquivos de BIOS em `Android/data/com.retroarch/files/system`.
  Por licenca o Advmenu nao vem (nem viram) com BIOSes — cada um provisiona a sua.
Sistemas suportados pelo botao de sistemas do Advmenu e o nucleo libretro de cada um:

| Pasta do sistema | Nucleo RetroArch (libretro) |
|---|---|
| atari2600 | Stella |
| atari7800 | ProSystem |
| atarilynx | Handy |
| atarijaguar | Virtual Jaguar |
| colecovision | blueMSX |
| intellivision | FreeIntv |
| vectrex | vecx |
| mastersystem/sms, gamegear, megadrive/genesis, segacd, saturn, sg1000 | Genesis Plus GX / Picodrive / Yabause |
| nes/famicom/fds | Mesen / Nestopia UE |
| snes | Snes9x / bsnes |
| n64 | Mupen64Plus-Next |
| gameboy/gbc/gba | mGBA / VBA-M |
| nds | DeSmuME / melonDS |
| psx/ps1 | Beetle PSX / SwanStation |
| psp | PPSSPP (nucleo libretro oficial) |
| psvita | Vita3K (fora do RetroArch; app proprio) |
| dreamcast | Flycast |
| pcengine/turbografx, supergrafx | Beetle PCE / Beetle PCE Fast |
| pcfx | Beetle PC-FX |
| neogeo/neogeocd | FinalBurn Neo / Neo CD (libretro) |
| neogeopocket | Beetle NeoPop |
| wonderswan | Beetle Cygne |
| 3do | Opera |
| virtualboy | Beetle VB |
| gamecube/wii | Dolphin (fora do RetroArch) |
| amiga | PUAE |
| c64/vic20 |VICE|
| amstradcpc | Cap32 |
| msx/msx2 | blueMSX / fMSX |
| spectrum/zx81 | Fuse |
| dos/pcdos | DOSBox Pure |
| mame/arcade | MAME 2003-Plus / FBNeo |
| hbmame/fba/fbneo | FinalBurn Neo / MAME 2003-Plus |
| scummvm | ScummVM (nucleo libretro) |

Isso cobre o recorte pratico do ScreenScraper no Android: o jogo da pasta de sistema
abre no app configurado pelo Advmenu (app preferido) e, dentro do RetroArch, cada
sistema ja tem nucleo pronto — sem baixar nem compilar nada separado.

### Controles (tela + gamepad fisico)

- **Controle digital na tela:** o botao **CONTROLE** (canto inferior-esquerdo do menu)
  mostra/oculta o gamepad na tela, no estilo do mockup: d-pad VERMELHO com centro
  redondo `A` na esquerda e botoes REDONDOS AZUIS `X Y A B` na direita (navegam o menu;
  `X`/`Y` sobem/descem paginas da lista). O estado (visivel/oculto) fica salvo e abre
  na ultima escolha.
- **Controle fisico conectado = controle virtual SOME sozinho:** quando o app detecta um
  gamepad/joystick fisico (USB, Bluetooth, Type-C, adaptador WiFi — incluindo os consoles
  portateis Android tipo Anbernic/Powkiddy/Retroid, que tem o gamepad embutido), o controle
  virtual e o botao CONTROLE desaparecem e fica so uma **engrenagem ⚙ discreta no canto**,
  que abre o menu **CONFIGURACAO** estilo EmulationStation: TEMA, CONTROLE VIRTUAL
  Liga/Desliga, MAPEAR GAMEPAD FISICO, SISTEMAS/EMULADORES, FAVORITOS e RECARREGAR JOGOS.
  Quando o controle fisico e desconectado, o controle virtual volta automaticamente.
  (**Engrenagem:** toque no ⚙ — nao precisa instalar nada, nao precisa ja ter controle.)
- **Gamepad fisico USB/Bluetooth/Type-C/adaptador WiFi** (PS DualShock/DualSense, Xbox,
  genéricos): Android entrega o controle pro app sem instalar nada — no menu, o
  direcional navega, botao A/Enter confirma, B volta.
- **Teclado e mouse USB, Bluetooth ou wireless (dongle):** reconhecidos nativamente —
  setas/Enter/Esc navegam o menu, o clique do mouse seleciona e a rodinha rola a lista;
  o teclado tambem pode ser mapeado no mapeador (86 teclas de arcade/encoder USB entram
  como teclado fisico).
- **Mapeamento fisico (qualquer modelo):** no menu **CONFIGURACAO** (engrenagem ⚙) ou com
  toque LONGO no botao **CONTROLE** abre o mapeador — escolha CIMA/BAIXO/ESQUERDA/DIREITA/
  A (CONFIRMAR)/B (VOLTAR)/PAGINA ACIMA/PAGINA ABAIXO e aperte a tecla desejada no seu
  controle fisico; o menu passa a usar suas teclas (fica salvo). Vale para gamepad PS/Xbox/
  genérico, handheld Android com gamepad embutido, encoder de arcade USB (placa JAMMA /
  Zero Delay, que aparece como joystick ou teclado USB), teclado e — nos teclados — qualquer
  combinacao de teclas. Nos jogos do ARCADE embutido, o SDL2 do AdvanceMAME reconhece o
  gamepad automaticamente; nos emuladores externos (RetroArch, PPSSPP etc.) o mapeamento
  in-game e feito dentro de cada app (e o padrao Android ja reconhece controles
  PS/Xbox direto).
- **Nada para instalar:** o front-end em si e standalone — reconhece jogos, pastas de
  sistema e controles sozinho; so os sistemas que precisam de emulador externo usam o
  app externo escolhido.

### Compatibilidade de aparelhos (controle embutido ou nao)

O Advmenu Android funciona em **qualquer aparelho Android**, com 4 ABIs buildadas
(arm64-v8a, armeabi-v7a, x86_64, x86) — cobre Mali/MediaTek, Snapdragon, Exynos,
Tensor, Unisoc e tambem x86.

- **Consoles portateis Android** (Anbernic RG*, Powkiddy, Retroid Pocket, Miyoo com
  Android, modelos Shopee genéricos), **com gamepad embutido ou sem**: o front-end detecta
  o gamepad via Android (`InputDevice/gamepad/joystick/dpad`); com controle fisico
  presente o controle virtual SOME e fica so a engrenagem ⚙ de CONFIGURACAO; sem,
  o controle virtual volta.
- **Smartphones/tablets** Mali/MediaTek/Snapdragon/qualquer SoC: igual.
- **Controle fisico nos emuladores:** no menu do Advmenu o mapeamento da engrenagem
  vale; nos emuladores externos o Android entrega o gamepad direto ao app em foco —
  RetroArch, PPSSPP, Dolphin, Eden, AetherSX2, Winlator e OpenBOR reconhecem padroes
  PS/Xbox/genéricos automaticamente, sem instalar nada (a configuracao fina dentro
  desses apps e de cada um; apps fechados nao aceitam configuracao externa.
- **ROG Ally, Legion Go, Steam Deck:** NAO sao Android (sao Windows/SteamOS), um APK
  nao roda nativo neles; nesses e preciso uma camada Android (ex.: emulador de Android
  no Windows / Waydroid no SteamOS) sem garantia — o alvo real e Android puro.

### PINBALL (Visual Pinball / Pinball Arcade) — telas unicas

- Crie a pasta `pinball/` no cartao com as mesas (`.vpx`, `.vpt`, `.fpt` — aceitas como
  jogos); ela aparece no menu **SISTEMAS** como **PINBALL** e, entrando, todas as mesas
  ficam numa lista unica com o tema (igual ao modelo dos videos), com snapshot por mesa
  em `snapshot/` (`<mesa>.jpg/.mp4...`).
- Clicando na mesa, o Advmenu abre no **Visual Pinball X para Android** (port oficial —
  APKs em github.com/vpinball/vpinball/releases e o app do Play Store
  `com.retroeki.pinball`): o app detecta automaticamente qualquer VPX instalado
  (`org.vpinball.VPinballX`, `org.vpinball.app.gl`, `com.retroeki.pinball` ou pacote com
  "vpinball" no nome). Controle fisico (flipper + trabas) va direto via Android.
- Outros apps de pinball Android instalados tambem sao detectados automaticamente e
  aparecem no botao EMULADORES: **Pinball FX Android** (`com.zenstudios.ZenPinballWorld`),
  **Williams Pinball** e demais apps Zen (pacote `com.zenstudios.*`), **Pinball Arcade**
  (`com.farsight.*`), **Zaccaria Pinball** (`hu.magicpixel.Zaccaria`) e o classico
  **3D Pinball Space Cadet** (`com.fexed.spacecadetpinball`, github.com/fexed/
  Pinball-on-Android). Nao precisa instalar nada extra do Advmenu: se o app esta no
  aparelho, o Advmenu acha e abre.
- Mesas do **Pinball Arcade/Farsight** no PC (sistema `pc-windows`) continuam na rota
  Winlator, na pasta `pc/` da varredura funda.

### Onde baixar mesas de pinball (e como instalar)

| Site | O que tem |
|---|---|
| [VPUniverse](https://vpuniverse.com) | milhares de mesas `.vpx`,backglasses (`.directb2s`), roms de pinball (PinMAME) |
| [VPForums](https://www.vpforums.org) | mesas VPX/Future Pinball, tutoriais e utilitarios |
| [Virtual Pinball Spreadsheet](https://virtualpinballspreadsheet.github.io) | catalogo/busca: onde baixar cada mesa, rom e backglass |
| [github.com/vpinball/vpinball](https://github.com/vpinball/vpinball/releases) | APK Android oficial do VPX (10.8.0 arm64 + legacy) |
| [ravarcade.pl](https://www.ravarcade.pl) | site OFICIAL do Future Pinball (freeware, Windows — no Android roda pela rota Winlator) |

#### Projetos open source (código-fonte e downloads oficiais)

| Projeto | O que é | Fonte/download |
|---|---|---|
| Visual Pinball X | motor principal (mesas .vpx), port Android incluso | [github.com/vpinball/vpinball](https://github.com/vpinball/vpinball/releases) |
| PinMAME | emulador do hardware das máquinas (ja EMBUTIDO no VPX) | [github.com/vpinball/pinmame](https://github.com/vpinball/pinmame) |
| wpc-emu (web) | WPC no navegador, com T2 FreeWPC livre | [neophob.github.io/wpc-emu](https://neophob.github.io/wpc-emu/) |
| FreeWPC | sistema operacional livre de maquinas WPC (ROMs livres) | [github.com/bcd/freewpc](https://github.com/bcd/freewpc) |
| Pocket Pinball | app Android oficial que roda .vpx (VPX engine) | [Play Store](https://play.google.com/store/apps/details?id=com.retroeki.pinball) / [pocketpinball.com](https://pocketpinball.com) |
| 3D Pinball Space Cadet | port Android do classico do Windows | [github.com/fexed/Pinball-on-Android](https://github.com/fexed/Pinball-on-Android/releases) |
| Future Pinball | criador/rodador de mesas .fpt (Windows; roda via Winlator) | [ravarcade.pl](https://www.ravarcade.pl) |

Mesas .vpx (conteudo dos autores): baixe de [VPUniverse](https://vpuniverse.com),
[VPForums](https://www.vpforums.org) e use o catalogo do
[Virtual Pinball Spreadsheet](https://virtualpinballspreadsheet.github.io).

Como instalar a mesa baixada: solte o `NomeDaMesa.vpx` na pasta `pinball/` do cartao; se a
mesa tiver, junte na MESMA pasta `NomeDaMesa.directb2s` (backglass) e a rom dentro de
`pinball/roms/` (formato VPX android) ou `vpmroms/<nome>/` — o VPX Android resolve. O
Advmenu reconhece `.vpx`, `.vpt`, `.fpt` automaticamente e mostra na lista unica, com
snapshot se voce colocar `snapshot/NomeDaMesa.jpg|.mp4`.

### HBMAME 0.289 (tres vias)

1. **Hoje (pratico — recomendado):** ROMs de HBMAME rodam no **RetroArch** com o core
   *MAME (Current)*, que acompanha o MAME moderno. O fluxo ja funciona: pasta `hbmame/`
   no cartao → botao EMULADORES → toque no jogo → abre no RetroArch (e se o APK
   avulso do item 2 estiver instalado, ele tem PREFERENCIA na rota, com o RetroArch
   como alternativa).
2. **Emulador HBMAME Android avulso (em construcao):** o port nativo (Fase 2 núcleo
   e Fase 3 OSD já compilando via CMake no NDK, arm64 e armv7) vai virar um APK
   standalone — quando instalado, o Advmenu Plus Android o detecta
   automaticamente (prefixo/nome `hbmame`) e a pasta `hbmame/` passa a abrir
   nele sozinho, sem configurar nada.
3. **Core libretro PROPRIO do HBMAME (planejado — Fase 2.6 do port):** o libretro
   nao tem core oficial do HBMAME (o “MAME Current” e do MAME puro). A partir do
   port nativo (item 4) sera gerado o core `libretro_hbmame.so` do NOSSO port — o
   RetroArch do seu aparelho o carrega como qualquer outro core e as rotas do
   Advmenu passam a usa-lo automaticamente.
4. **Port nativo dentro do APK (projeto em andamento — longo):** o plano para
   portar o nucleo MAME 0.289/HBMAME para DENTRO do APK esta em
   `android-project/hbm-port/README-PORT.md`; a fonte fixada (0.289.6,
   `fa040eced0d`) e baixada por `tools/sync-hbmame.sh` (idempotente, fora do
   git — 973 MB / 15.945 arquivos).

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

## Configuracao (advmame.rc / advmenu.rc / .ini) — tudo dentro do codigo

A configuracao padrao do emulador e do menu esta ESCRITA NO CODIGO-FONTE, na classe
`android-project/app/src/main/java/com/edsonmfernando/advancemame/AdvConfig.java`
(constantes `ADVMAME_RC` e `ADVMENU_RC`). Na primeira execucao o app grava esses
conteudos em `files/.advance/advmame.rc` e `files/.advance/advmenu.rc` — os mesmos
arquivos que o AdvanceMAME/AdvMenu classicos leem.

Para personalizar SEM recompilar: coloque um `advmenu.rc` (ou `advmenu.ini`,
`advmame.rc`, `advmame.ini`) na RAIZ do cartao de memoria — o app reconhece
diretamente e usa ELE como configuracao (substitui o padrao; as pastas de ROMs
do cartao continuam sendo injetadas no `dir_rom` automaticamente). Nenhum script
solto: o que os scripts fazem, o Gradle (no build) e o app (na execucao) fazem sozinhos.

Edite `AdvConfig.java` para mudar os padroes de fabrica e recompile.

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
