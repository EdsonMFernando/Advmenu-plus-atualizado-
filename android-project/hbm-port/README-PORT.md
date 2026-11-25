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
- [ ] Fase 2 (CMake do nuclei)
- [ ] Fase 3 (OSD SDL)
- [ ] Fase 4 (glue)
- [ ] Fase 5 (build verde)

Toda mudanca de codigo fica em `#ifdef __ANDROID__` como no port atual; os
arquivos do port 0.106 (funcionando) NAO sao tocados por este projeto.
