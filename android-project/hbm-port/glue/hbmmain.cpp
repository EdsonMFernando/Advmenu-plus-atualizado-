/*
 * HBMAME Android (APK avulso) - glue.
 * O SDL_main real vem do OSD SDL (src/osd/sdl/sdlmain.cpp, ligado por
 * --whole-archive); este construtor roda no dlopen do libmain.so, ANTES do
 * SDLActivity chamar SDL_main, e aponta o diretorio corrente/HOME para o
 * storage privado do app (mesmo padrao ADV_DATADIR do port AdvanceMAME).
 * La ficam o background.jpg da UI interna, cfg/, nvram/, ini/, memcard/ etc.
 */

#include <stdlib.h>
#include <unistd.h>

namespace {

struct hbm_android_init
{
	hbm_android_init()
	{
		const char *home = HBM_ANDROID_DATADIR;
		setenv("HOME", home, 1);
		chdir(home);
	}
};

const hbm_android_init hbm_android_init_instance;

} // namespace
