# PORT NATIVO — HBMAME 0.289 (nucleo MAME 0.289) para o Advmenu Plus Android

Projeto de LONGO PRAZO, em fases. O fluxo pratico de hoje (pasta `hbmame/` no
cartao abrindo no RetroArch com core MAME Current) JA funciona e continua.

## Por que e um projeto grande

O HBMAME 0.289 e o MAME moderno inteiro + drivers de homebrew: 15.945 arquivos
C/C++ (484 MB), build GENie/Lua (nao CMake), C++17 com templates pesados. Os
drivers dele NAO compilam no nucleo 0.106 do Advmenu Plus (API moderna de
devices). Nao ha atalho de "colar": e preciso portar o nucleo novo.

## Fases

1. **Vendor + configuracao** (feito): `tools/sync-hbmame.sh` baixa a fonte fixada
   (0.289.6, commit `fa040eced0d`) para `hbmame/` (fora do git, como o SDL2).
2. **CMake do nucleo**: substituir GENie por CMake para NDK — comecar por um
   subtarget MINIMO (MAME tem `SUBTARGET=tiny` com poucos drivers; o HBMAME usa
   o subtarget `hbmame`) e crescer conjunto por conjunto.
3. **OSD**: portar `src/osd/sdl` moderno sobre o nosso SDL2 2.30.11 — os
   modulos (render/threads/arquivo) e as glue de pthreads/poll.
4. **Glue Android**: mesmo esquema do port atual — `main()` renomeado,
   `dir_rom`/config pelo `AdvConfig`, assets e permissoes ja existentes.
5. **Iteracao de compilacao**: corrigir erros por lotes (C++17/clang/NDK),
   ate linkar `libhbmame.so` e o front-end passa a listar os jogos de
   `hbmame/` nativamente.
6. **FBA/FBNeo**: repetir o modelo (o FinalBurn Neo moderno e muito menor).
7. **Core libretro do HBMAME** (PROPRIO, Fase 2.6): o libretro NAO tem core
   oficial do HBMAME (o core "MAME Current" e do MAME puro — nao roda os
   drivers homebrew/hacks do HBMAME). Vamos gerar o NOSSO core libretro
   (`libretro_hbmame.so`) a partir DESTE port: reuso as libs `hbmcore`,
   `hbmemu` e `hbmosd` ja concluidas e encaixo a interface `retro_*` no
   lugar do OSD de janela; o Advmenu Android deteta o core e o RetroArch
   passa a abrir os drivers do HBMAME nativamente (ate la: APK avulso e
   RetroArch com core MAME Current seguem funcionando).

## Estado

- [x] Fase 1 (vendor script fixado)
- [ ] Fase 2 (CMake do nucleo) — ALCANCADA (sem script solto): o
  `hbm-port/CMakeLists.txt` compila e linka, com o clang do NDK r26d,
  Release, para arm64-v8a E armeabi-v7a (0 falhas, `libhbmcore.a`):
  - `src/lib/util`: **54/54** arquivos
  - `src/lib/formats`: **231/231** arquivos — o `has_formats.h` e
    GERADO PELO PROPRIO CMake (no lugar do GENie), dentro da arvore de
    build, sem tocar na fonte vendored
  - `src/lib/netlist` (nucleo, sem `examples/`/`tests/`/`prg/`): **101/101**
    arquivos; `examples/`/`tests/`/`prg/` sao demos standalone fora da
    biblioteca (5 demos tem erros proprios no fonte upstream, nao
    usados pelo nucleo do MAME)
  Include baseline dentro do CMakeLists; unico ajuste de codigo e o
  `patches-hbm/01-android-zlib-const.patch`, aplicado automaticamente
  pelo `tools/sync-hbmame.sh` (vendor no PC; o APK contem apenas o resultado).

- [ ] Fase 3 (OSD SDL) — ALCANCADA (sem script solto): o mesmo
  `hbm-port/CMakeLists.txt` tem o alvo `hbmosd` (src/osd sobre o SDL2 2.30.11
  vendored no app), Release, arm64-v8a E armeabi-v7a:
  - **126/147** arquivos do OSD compilam e linkam em `libhbmosd.a`
    (~46 MB arm64 / ~32 MB armv7) — inclui sdlmain/window/android_main,
    som/input/render SDL e o render **BGFX completo** (12 arquivos + drawbgfx)
  - as 21 exclusoes sao modules de hosts desktop, fora do build Android de
    proposito (Windows: win*/winrt*/drawd3d/d3dhlsl, macOS, pulse/pipewire/
    portaudio/portmidi, asio/network), e `/modules/file/stdfile.cpp`:
    `posixfile.cpp` cobre Android, `stdfile` precisa de ajuste proprio
  - definicoes: `-DOSD_SDL -DUSE_OPENGL=0 -DUSE_BGFX=1 -DBX_CONFIG_DEBUG=0`;
    o <SDL2/SDL.h> do SDL vendored e exposto por link `sdl2-shim/SDL2` gerado
    pelo proprio CMake na arvore de build

- [ ] Fase 2.5 (src/emu) — ALCANÇADA (sem script solto): o mesmo
  `hbm-port/CMakeLists.txt` tem o alvo `hbmemu` (nucleo de emulacao MAME
  moderno inteiro), Release -O1, arm64-v8a E armeabi-v7a:
  - **116/116** arquivos compila e linka em `libhbmemu.a` (~98 MB arm64 /
    ~91 MB armv7) — inclui render/rendlay/rendfont completo, ioport,
    emumem (todos os handlers), debug completo, audio_effects e video
  - os 2 unicos fora do alvo, de proposito: `src/emu/http.cpp`
    (servidor de rede host-only, precisa asio) e
    `src/emu/drivers/testcpu.cpp` (ferramenta de teste de CPU, nunca
    linkada no nucleo)
  - headers gerados (layout comprimido `internal_layout` e fontes da UI)
    SAO GERADOS PELO PROPRIO CMake com os scripts oficiais do HBMAME
    (`complay.py`, `png2bdc.py`, `file2str.py`), dentro da arvore
    `build/` — igual o `has_formats.h` da Fase 2
  - unico ajuste de codigo: `patches-hbm/02-android-clang17-devdelegate.patch`
    (SFINAE para o clang 17 do NDK r26), aplicado automaticamente pelo
    `tools/sync-hbmame.sh` (vendor no PC)

- [x] Fase 4 (parte 1 — frontend) **ALCANÇADA**: `src/frontend/mame` inteiro
    compila pelo alvo CMake `hbmfrontend` (75/76; o `ui/slider.cpp` fica fora
    de proposito — arquivo morto, ausente do build upstream do GENie).
    `libhbmfrontend.a` gerado nas 2 ABIs (arm64 ~57 MB, armv7 ~48 MB).
    Detalhes que destravaram: (a) o `copying.ipp` (COPYING via
    `file2lines.py`) virou dependencia do `hbmgen-emu`; (b) a ordem de
    includes upstream (`frontend.lua`: `frontend/mame` ANTES de `lib/util`)
    foi replicada POR ALVO — o `include_directories()` global do CMake entra
    antes dos includes de alvo no clang, e `lib/util` no bloco global fazia o
    `"language.h"` de `ui/ui.h` resolver no util (sem o
    `using util::lang_translate`) e `luaengine.cpp` falhar; agora `lib/util`
    esta so nos blocos de alvo, depois de `frontend/mame`.
- [ ] Fase 4 (parte 2 — glue)
  - **fundo do menu de navegacao interno**: a UI interna do MAME/HBMAME
    carrega `background.jpg` (depois `background.png`) DA RAIZ do emulador
    quando `use_background = 1` (padrao ja ligado,`moptions.cpp:77`) na tela
    de selecao de jogos (`menu.cpp:88-110`, tela `___empty`). O `hbm-port/`
    ja guarda o `background.jpg` oficial (dragao em fogo, Full HD, Full-bleed)
    para ser instalado na raiz de arquivos do APK avulso na Fase 4 — igual ao
    que o Advmenu usa no tema (`art/background_hbmame.jpg`)
- [ ] Fase 5 (build verde)

- [ ] Fase 5 (build verde) — CI pronto e atualizado: o workflow
  `.github/workflows/hbm-port-build.yml` (manual, aba Actions) baixa o
  vendor fixado e compila `hbmcore + hbmemu + hbmosd + hbmfrontend` E o
  `libmain.so` do APK avulso (`-DHBM_APK_GLUE=ON`) com o clang do NDK
  em arm64-v8a e armeabi-v7a, subindo o log de erros como artefato para
  corrigir por lotes

Toda mudanca de codigo fica em `#ifdef __ANDROID__` como no port atual; os
arquivos do port 0.106 (funcionando) NAO sao tocados por este projeto.
