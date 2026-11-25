# Advmenu-plus-atualizado- (AdvanceMAME para Android)

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
`/data/data/<pkg>/files` (`advmame.rc`, `advmenu.rc`). Como o nome visual (capa) do jogo, se
quiser, coloque uma imagem em `Android/data/com.edsonmfernando.advancemame/files/media/<rom>.png`
(tambem aceita `.jpg/.jpeg/.webp`).

As ROMs de exemplo (zips binarios) nao vao no repositorio. Para jogar, coloque as ROMs em
`Android/data/com.edsonmfernando.advancemame/files/rom` OU, antes de compilar, coloque os zips
em `android-project/app/src/main/assets/rom/` para irem embutidas no APK (o app extrai para o
`filesDir` na primeira execucao; a ROM padrao, se nenhuma for passada, e `gridlee`).

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
