/*
 * AdvanceMAME for Android - JNI glue.
 * SDL2 (via SDLActivity) carrega libmain.so e chama SDL_main().
 * Este arquivo repassa a chamada para o main do emulador
 * (advance/linux/os.c, compilado com -Dmain=advmame_main).
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <SDL.h>

extern int advmame_main(int argc, char* argv[]);

int main(int argc, char* argv[])
{
    const char* home = SDL_AndroidGetInternalStoragePath();
    if (home) {
        if (!getenv("HOME"))
            setenv("HOME", home, 1);
        chdir(home);
    }
    return advmame_main(argc, argv);
}
