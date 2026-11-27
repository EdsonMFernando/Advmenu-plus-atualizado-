/*
 * libretro.h — subconjunto MINIMO da API libretro (frontend) usado pelo
 * core HBMAME. Apenas o que este core declara/usa hoje; quando o OSD
 * libretro (parte 2 da Fase 2.6) precisar de mais callbacks, amplie aqui.
 * Sintaxe e valores seguem a API publica libretro 1 (libretro-common).
 */
#ifndef HBM_LIBRETRO_H
#define HBM_LIBRETRO_H

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#define RETRO_API_VERSION 1

#define RETRO_REGION_NTSC 0

/* Formatos de pixel (retro_set_pixel_format) */
#define RETRO_PIXEL_FORMAT_0RGB1555 0
#define RETRO_PIXEL_FORMAT_XRGB8888 1
#define RETRO_PIXEL_FORMAT_RGB565   2

/* Device ids de input (pad classico do MAME) */
#define RETRO_DEVICE_NONE     0
#define RETRO_DEVICE_JOYPAD   1
#define RETRO_DEVICE_ANALOG   5
#define RETRO_DEVICE_KEYBOARD 3
#define RETRO_DEVICE_ID_JOYPAD_B      0
#define RETRO_DEVICE_ID_JOYPAD_Y      1
#define RETRO_DEVICE_ID_JOYPAD_SELECT 2
#define RETRO_DEVICE_ID_JOYPAD_START  3
#define RETRO_DEVICE_ID_JOYPAD_UP     4
#define RETRO_DEVICE_ID_JOYPAD_DOWN   5
#define RETRO_DEVICE_ID_JOYPAD_LEFT   6
#define RETRO_DEVICE_ID_JOYPAD_RIGHT  7
#define RETRO_DEVICE_ID_JOYPAD_A      8
#define RETRO_DEVICE_ID_JOYPAD_X      9
#define RETRO_DEVICE_ID_JOYPAD_L     10
#define RETRO_DEVICE_ID_JOYPAD_R     11

/* Environment commands (subconjunto, valores oficiais da API 1) */
#define RETRO_ENVIRONMENT_EXPERIMENTAL          0x10000
#define RETRO_ENVIRONMENT_GET_LOG_INTERFACE     (3 | RETRO_ENVIRONMENT_EXPERIMENTAL)
#define RETRO_ENVIRONMENT_GET_CAN_DUPE          4
#define RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL 8
#define RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY  9
#define RETRO_ENVIRONMENT_SET_PIXEL_FORMAT      10
#define RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO    12
#define RETRO_ENVIRONMENT_GET_VARIABLE          15
#define RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY    16

/* Log levels */
#define RETRO_LOG_ERROR 0
#define RETRO_LOG_INFO  1
#define RETRO_LOG_WARN  2

typedef void (*retro_log_printf_t)(unsigned level, const char* fmt, ...);
struct retro_log_callback { retro_log_printf_t log; };

struct retro_game_info
{
    const char* path;
    const void* data;
    size_t size;
    const char* meta;
};

struct retro_game_geometry
{
    unsigned base_width, base_height, max_width, max_height;
    float aspect_ratio;
};

struct retro_system_timing { double fps; double sample_rate; };

struct retro_system_av_info
{
    struct retro_game_geometry geometry;
    struct retro_system_timing timing;
};

struct retro_system_info
{
    const char* library_name;
    const char* library_version;
    const char* valid_extensions;
    bool need_fullpath;
    bool block_extract;
};

struct retro_variable { const char* key; const char* value; };

typedef void (*retro_environment_t)(unsigned cmd, void* data);
typedef void (*retro_video_refresh_t)(const void* data, unsigned width,
        unsigned height, size_t pitch);
typedef void (*retro_audio_sample_t)(int16_t left, int16_t right);
typedef size_t (*retro_audio_sample_batch_t)(const int16_t* data,
        size_t frames);
typedef void (*retro_input_poll_t)(void);
typedef int16_t (*retro_input_state_t)(unsigned port, unsigned device,
        unsigned index, unsigned id);

#endif /* HBM_LIBRETRO_H */
