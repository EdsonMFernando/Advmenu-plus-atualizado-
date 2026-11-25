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

## Estado

- [x] Fase 1 (vendor script fixado)
- [ ] Fase 2 (CMake do nucleo) — INICIADA: smoke test com clang do NDK r26d
  compila, para arm64-v8a E armeabi-v7a (script `tools/smoke-hbm-lib.sh`;
  saida em `hbm-port/build-smoke/<abi>/erros.log`):
  - `src/lib/util`: **54/54** arquivos
  - `src/lib/formats`: **231/231** arquivos (incluindo o `all.cpp`,
    com o `has_formats.h` gerado pelo `sync-hbmame.sh` no lugar do GENie)
  - `src/lib/netlist` (nucleo, sem `examples/`/`tests/`/`prg/`): **101/101**
    arquivos — 0 falhas nas duas ABIs; linkam em `libhbmcore-<abi>.a`
    (~34 MB arm64 / ~30 MB armv7, llvm-ar)
  Os `examples/`/`tests/`/`prg/` do netlist sao demos standalone fora da
  biblioteca (compilam, mas ficam fora do .a); 5 demos de `examples/`
  (`congo_bongo`, `cmos_inverter*`, `turkey_shoot`, `lostfound`) tem erros
  proprios no fonte upstream e nao sao usados pelo nucleo do MAME.
  Include baseline: `-I3rdparty` + dirs pontuais (flac/include, zstd/lib,
  expat/lib, utf8proc, nanosvg/src, lzma/C, aes*, softfloat3/include, glm,
  rapidjson/include, lua, zlib).
  O unico ajuste de codigo ate agora e o `patches-hbm/01-android-zlib-const.patch`
  (validado com `git apply --check`), aplicado automaticamente pelo
  `tools/sync-hbmame.sh` — re-sync limpo reproduz a arvore identica.

- [ ] Fase 3 (OSD SDL)
- [ ] Fase 4 (glue)
- [ ] Fase 5 (build verde)

Toda mudanca de codigo fica em `#ifdef __ANDROID__` como no port atual; os
arquivos do port 0.106 (funcionando) NAO sao tocados por este projeto.
