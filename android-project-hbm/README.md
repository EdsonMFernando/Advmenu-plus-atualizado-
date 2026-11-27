# HBMAME Android (APK avulso) — port nativo 0.289

APK standalone do emulador HBMAME, gerado a partir do port nativo
`android-project/hbm-port/` (mesmas libs: hbmcore, hbmemu, hbmosd,
hbmfrontend) com a glue `HBM_APK_GLUE=ON`.

- Pacote: `com.edsonmfernando.hbmame`
- Launch: `MainActivity` (SDLActivity → libmain.so)
- ABIs: arm64-v8a e armeabi-v7a
- Vendor automatico: HBMAME 0.289.6 (`tools/sync-hbmame.sh`) + SDL2
  (`android-project/tools/sync-vendor.sh`) antes de compilar
- O Advmenu Plus Android detecta o app (nome/pacote `hbmame`) e abre a
  pasta `hbmame/` do cartao nele automaticamente

Build:
```bash
cd android-project-hbm
gradle assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

O core libretro do HBMAME (para o RetroArch) e gerado pelo mesmo CMake
(`HBM_LIBRETRO=ON`): `libhbmame_libretro.so` — copie para a pasta `cores/`
do RetroArch com o `hbmame_libretro.info` de `hbm-port/libretro/`.
