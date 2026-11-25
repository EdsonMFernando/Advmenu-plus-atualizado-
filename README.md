# Advmenu-plus-atualizado- (AdvanceMAME para Android)

Porta do [AdvanceMAME](http://www.advancemame.it/) (fork `amadvance`) para Android, compilada
direto do código-fonte open source para gerar o APK com o menu (`advmenu`) e o emulador
(`advmame`) do MAME 0.106.

## Estrutura

```
advancemame/            codigo-fonte original do AdvanceMAME (vendored)
android-project/        projeto Gradle que gera o APK (SDL2 2.30.11 vendored)
  app/src/main/cpp/     CMakeLists + glue Android (main.c) + config.h
```

- `android-project/app/src/main/cpp/config/config.h` — config.h manual (substitui o do autoconf; resolve os tamanhos por `__LP64__`)
- `android-project/app/src/main/cpp/config/config.h` e `advancemame/advance/lib/config.h` — mesma copia (o `portable.h` inclui `config.h` a partir do proprio diretorio `advance/lib`, igual ao build autoconf original)
- `android-project/app/src/main/cpp/AdvSources.cmake` / `AdvDefines.cmake` — listas de fontes e defines HAS_* geradas a partir do sistema de make original (advancemame.mak)
- `android-project/app/src/main/cpp/main.c` — parte Android; chama `advmame_main()` (o `main` original do `advance/linux/os.c` e renomeado para `advmame_main`)

## Compilando o APK

Necessario: JDK 17, Android SDK (API 34), NDK 26.3, CMake >= 3.22 e Ninja.

```bash
cd android-project
./gradlew assembleDebug        # gera app/build/outputs/apk/debug/app-debug.apk
```

O build nativo roda automaticamente para as duas ABIs: `arm64-v8a` e `armeabi-v7a`.
As duas cobrem os smartphones modernos (Snapdragon/Kryo) e os antigos MediaTek (Cortex-A7/A15) —
GPU e transparente (o video e feito pelo SDL2 com fallback de software, funciona em Mali e Adreno).

Variaveis usadas pelo build local (ajuste se necessario):

```bash
export JAVA_HOME=/caminho/do/jdk-17
export ANDROID_HOME=/caminho/do/android-sdk
export GRADLE_USER_HOME=/tmp/gradle-home   # opcional
./gradlew assembleDebug
```

A versao release (`assembleRelease`) precisa de `local.properties` com `storeFile/storePassword`
assim como qualquer projeto Android.

## Rodando

Instale o APK — o binario nativo cria os arquivos de configuracao em
`/data/data/<pkg>/files` (`advmame.rc`, `advmenu.rc`). Coloque as ROMs na pasta
`Android/data/com.edsonmfernando.advancemame/files/rom` (configure o caminho no `advmame.rc`
em `dir_rom`).

## Divergencias do build original (manter em mente)

- As flags seguem o `advance/emu.mak` original: `advance/osd/*.c` compila com `-DMAME` e os
  includes do mame (é o que traz `osdepend.h`/`UINT32`); `advance/lib` compila sem; a camada
  do MAME ganha `PI=M_PI` e tem `-has MAME`.
- `thmono.c`/`thfifo.c` fora da lib de thread (com `USE_SMP` o upstream usa apenas `thdouble.c`).
- `advance/linux/os.c` usa `main=advmame_main;` para o `entrypoint` SDL.
- Vicdual (mame/sndhrdw/vicdual.c): blocos `#ifdef __ANDROID__`, porque clang nao calcula `sqrt()`
  em inicializador estático (gcc faz).
- `alien/deps/miniz-3.1.2/miniz_export.h`: feito; `MINIZ_EXPORT` vazio para build estático.
- O jogo do objeto SDL2 usa C++ (hidapi), por isso `ANDROID_STL=c++_static` e o `c++_static`/`c++abi`
  ligados por último na `libmain.so`.
