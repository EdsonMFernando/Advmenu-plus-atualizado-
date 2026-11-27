/*
 * PORT NATIVO HBMAME 0.289 — Fase 4: glue Android (hbm-port).
 *
 * Mesmo esquema do port 0.106 (app/src/main/cpp/main.c): a SDL2 carrega a
 * lib e chama o entrypoint; aqui repassamos para o main do OSD SDL do
 * HBMAME (src/osd/sdl/sdlmain.cpp, compilado com -Dmain=hbmame_main).
 *
 * Este alvo e SEPARADO do APK enquanto o port nao linka (Fase 5); os
 * arquivos do port 0.106 em funcionamento nao sao tocados. Toda
 * divergencia fica em #ifdef __ANDROID__, como no resto do projeto.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#ifdef __ANDROID__
#include <SDL.h>
#endif

extern int hbmame_main(int argc, char* argv[]); /* -Dmain=hbmame_main no sdlmain.cpp */

int hbm_main(int argc, char* argv[])
{
#ifdef __ANDROID__
    const char* home = SDL_AndroidGetInternalStoragePath();
    if (home) {
        if (!getenv("HOME"))
            setenv("HOME", home, 1);
        chdir(home);
    }
#endif
    return hbmame_main(argc, argv);
}
