/*
 * CORE LIBRETRO PROPRIETARIO DO HBMAME — Fase 2.6, parte 1 (esqueleto verde).
 *
 * Implementa a API retro_* obrigatoria para o RetroArch carregar
 * libretro_hbmame.so. Nesta parte 1 o core:
 *   - registra nome/versao/extensoes (.zip = formato dos sets do HBMAME);
 *   - recebe o caminho da ROM (need_fullpath: o RetroArch entrega o .zip
 *     inteiro, que o MAME abre depois);
 *   - recebe os diretorios system/saves e o log do frontend;
 *   - retro_run entrega video (XRGB8888, 320x240@60) e audio (48000 Hz,
 *     silencio) por callbacks libretro — o PADRAO VISUAL garante o
 *     contrato de video/audio antes da integracao do nucleo.
 *
 * PARTE 2 (proximo passo): o OSD libretro (src/osd adaptado), que aciona
 * hbmcore + hbmemu + hbmfrontend (ja compilados por este CMake) e troca o
 * padrao de teste pelo video real do driver.
 */

#include "libretro.h"

#include <stdio.h>
#include <string.h>

namespace {

retro_environment_t g_env;
retro_video_refresh_t g_video;
retro_audio_sample_batch_t g_audio;
retro_input_poll_t g_input_poll;
retro_input_state_t g_input_state;
retro_log_printf_t g_log;

char g_system_dir[1024];
char g_save_dir[1024];
char g_game_path[2048];
bool g_game_loaded;

const unsigned W = 320, H = 240;

void default_log(unsigned, const char*, ...) {}

/* Padrao de teste animado (contrato AV enquanto a parte 2 nao entra). */
void draw_test_pattern(void* frame, unsigned counter)
{
    uint32_t* px = (uint32_t*)frame;
    for (unsigned y = 0; y < H; y++) {
        for (unsigned x = 0; x < W; x++) {
            uint32_t r = (x * 255u) / W;
            uint32_t g = (y * 255u) / H;
            uint32_t b = counter & 0xFFu;
            px[y * W + x] = 0xFF000000u | (r << 16) | (g << 8) | b;
        }
    }
}

} // namespace

extern "C" {

void retro_set_environment(retro_environment_t cb) { g_env = cb; }
void retro_set_video_refresh(retro_video_refresh_t cb) { g_video = cb; }
void retro_set_audio_sample(retro_audio_sample_t) { /* usamos batch */ }
void retro_set_audio_sample_batch(retro_audio_sample_batch_t cb) { g_audio = cb; }
void retro_set_input_poll(retro_input_poll_t cb) { g_input_poll = cb; }
void retro_set_input_state(retro_input_state_t cb) { g_input_state = cb; }

void retro_init(void)
{
    g_log = default_log;
    struct retro_log_callback logcb;
    if (g_env && g_env(RETRO_ENVIRONMENT_GET_LOG_INTERFACE, &logcb) && logcb.log)
        g_log = logcb.log;
    g_log(RETRO_LOG_INFO, "[hbmame-libretro] init\n");

    const char* dir = nullptr;
    if (g_env && g_env(RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY, &dir) && dir) {
        snprintf(g_system_dir, sizeof(g_system_dir), "%s", dir);
        g_log(RETRO_LOG_INFO, "[hbmame-libretro] system dir: %s\n", dir);
    }
    if (g_env && g_env(RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY, &dir) && dir) {
        snprintf(g_save_dir, sizeof(g_save_dir), "%s", dir);
        g_log(RETRO_LOG_INFO, "[hbmame-libretro] save dir: %s\n", dir);
    }
    bool can_dupe = true;
    g_env(RETRO_ENVIRONMENT_GET_CAN_DUPE, &can_dupe);
    unsigned fmt = RETRO_PIXEL_FORMAT_XRGB8888;
    g_env(RETRO_ENVIRONMENT_SET_PIXEL_FORMAT, &fmt);
}

void retro_deinit(void)
{
    g_game_loaded = false;
    g_game_path[0] = 0;
}

unsigned retro_api_version(void) { return RETRO_API_VERSION; }

void retro_get_system_info(retro_system_info* info)
{
    memset(info, 0, sizeof(*info));
    info->library_name = "HBMAME (Advmenu Plus port)";
    info->library_version = "0.289.6";
    info->valid_extensions = "zip";
    info->need_fullpath = true;   /* o MAME abre o zip inteiro por caminho */
    info->block_extract = true;   /* nao deixar o frontend extrair */
}

void retro_get_system_av_info(retro_system_av_info* info)
{
    memset(info, 0, sizeof(*info));
    info->geometry.base_width = W;
    info->geometry.base_height = H;
    info->geometry.max_width = W;
    info->geometry.max_height = H;
    info->geometry.aspect_ratio = 4.0f / 3.0f;
    info->timing.fps = 60.0;
    info->timing.sample_rate = 48000.0;
}

void retro_set_controller_port_device(unsigned, unsigned) {}

void retro_reset(void)
{
    g_log(RETRO_LOG_INFO, "[hbmame-libretro] reset (soft reset do driver na parte 2)\n");
}

bool retro_load_game(const retro_game_info* game)
{
    if (!game || !game->path || !game->path[0])
        return false;
    snprintf(g_game_path, sizeof(g_game_path), "%s", game->path);
    g_game_loaded = true;
    g_log(RETRO_LOG_INFO, "[hbmame-libretro] load_game: %s (boot do driver na parte 2)\n",
        game->path);
    return true;
}

void retro_unload_game(void)
{
    g_game_loaded = false;
    g_game_path[0] = 0;
}

unsigned retro_get_region(void) { return RETRO_REGION_NTSC; }

void* retro_get_memory_data(unsigned) { return nullptr; }
size_t retro_get_memory_size(unsigned) { return 0; }

void retro_run(void)
{
    if (g_input_poll)
        g_input_poll();

    static uint32_t frame[W * H];
    static unsigned counter;
    draw_test_pattern(frame, counter++);

    if (g_video)
        g_video(frame, W, H, W * sizeof(uint32_t));

    /* silencio estereo 48000 Hz: 16 frames por callback de video @60Hz */
    if (g_audio) {
        static int16_t silence[16 * 2];
        memset(silence, 0, sizeof(silence));
        g_audio(silence, 16);
    }
}

size_t retro_serialize_size(void) { return 0; }  /* save states na parte 2 */
bool retro_serialize(void*, size_t) { return false; }
bool retro_unserialize(const void*, size_t) { return false; }

void retro_cheat_reset(void) {}
void retro_cheat_set(unsigned, bool, const char*) {}

} /* extern "C" */
