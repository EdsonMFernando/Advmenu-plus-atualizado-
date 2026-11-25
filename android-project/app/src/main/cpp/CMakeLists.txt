cmake_minimum_required(VERSION 3.22)
project(advancemame-android C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

get_filename_component(REPO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../.." ABSOLUTE)
set(ADV_SRC "${REPO_ROOT}/advancemame")

set(SDL_SHARED OFF CACHE BOOL "" FORCE)
set(SDL_STATIC ON CACHE BOOL "" FORCE)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/SDL2 SDL2-build EXCLUDE_FROM_ALL)

include(${CMAKE_CURRENT_SOURCE_DIR}/AdvSources.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/AdvDefines.cmake)

set(CPP_DIR ${CMAKE_CURRENT_SOURCE_DIR})

# advance/osd usa osdepend.h do MAME, entao compila como no upstream:
# com -DMAME e os includes do mame (regra $(OBJ)/advance/osd/%.o do emu.mak)
file(GLOB ADV_OSD "${ADV_SRC}/advance/osd/*.c")
# thmono e thfifo sao variantes alternativas do thread backend; o upstream
# (ADVANCEOBJS com pthread) usa apenas thdouble.c
list(REMOVE_ITEM ADV_OSD
  "${ADV_SRC}/advance/osd/thmono.c"
  "${ADV_SRC}/advance/osd/thfifo.c"
)
set(ADV_ADVANCE_REST ${ADV_ADVANCE})
list(REMOVE_ITEM ADV_ADVANCE_REST ${ADV_OSD})

# ----- camada advance/osd (glue do emulador) -----
add_library(advosd OBJECT
  ${ADV_OSD}
)
target_include_directories(advosd PRIVATE
  ${ADV_SRC}
  ${ADV_SRC}/advance/osd
  ${ADV_SRC}/advance/lib
  ${ADV_SRC}/advance/blit
  ${ADV_SRC}/advance/sdl
  ${ADV_SRC}/advance/linux
  ${CPP_DIR}/SDL2/include
  ${ADV_SRC}/mame
  ${ADV_SRC}/mame/includes
  ${ADV_SRC}/mame/debug
  ${ADV_SRC}/mame/cpu/m68000
  ${ADV_SRC}/alien/zlib
  ${ADV_SRC}/alien/expat
  ${ADV_SRC}/alien/lzma/include
  ${ADV_SRC}/alien/lzma/src
  ${ADV_SRC}/alien/zstd
  ${ADV_SRC}/alien/libchdr/include
  ${ADV_SRC}/alien/libchdr/src
  ${ADV_SRC}/alien/sljit
  ${ADV_SRC}/alien/minimp3
  ${ADV_SRC}/alien/nanojpeg
  ${ADV_SRC}/alien/dr_flac
  ${CPP_DIR}/config
)

# ----- camada advance (lib + blit + drivers SDL2) + alien -----
add_library(advlib OBJECT
  ${ADV_ADVANCE_REST}
  ${ADV_ALIEN}
)
target_include_directories(advlib PRIVATE
  ${ADV_SRC}
  ${ADV_SRC}/advance/osd
  ${ADV_SRC}/advance/lib
  ${ADV_SRC}/advance/blit
  ${ADV_SRC}/advance/sdl
  ${ADV_SRC}/advance/linux
  ${CPP_DIR}/SDL2/include
  ${ADV_SRC}/alien/zlib
  ${ADV_SRC}/alien/expat
  ${ADV_SRC}/alien/lzma/include
  ${ADV_SRC}/alien/lzma/src
  ${ADV_SRC}/alien/zstd
  ${ADV_SRC}/alien/libchdr/include
  ${ADV_SRC}/alien/libchdr/src
  ${ADV_SRC}/alien/sljit
  ${ADV_SRC}/alien/minimp3
  ${ADV_SRC}/alien/nanojpeg
  ${ADV_SRC}/alien/dr_flac
  ${CPP_DIR}/config
)

# ----- nucleo MAME (core + cpus + sons + drivers) -----
add_library(mamelib OBJECT
  ${ADV_MAME_CORE}
  ${ADV_MAME_DRIVERS}
  ${ADV_MAME_MACHINE}
  ${ADV_MAME_VIDHRDW}
  ${ADV_MAME_SNDHRDW}
  ${ADV_MAME_CPU}
  ${ADV_MAME_SOUND}
)
target_include_directories(mamelib PRIVATE
  ${ADV_SRC}
  ${ADV_SRC}/mame
  ${ADV_SRC}/mame/includes
  ${ADV_SRC}/advance/osd
  ${ADV_SRC}/advance/lib
  ${ADV_SRC}/advance/blit
  ${ADV_SRC}/alien/nanojpeg
  ${ADV_SRC}/alien/zlib
  ${ADV_SRC}/alien/minimp3
  ${ADV_SRC}/alien/dr_flac
  ${ADV_SRC}/alien/libchdr/include
  ${ADV_SRC}/alien/libchdr/src
  ${ADV_SRC}/alien/zstd
  ${ADV_SRC}/alien/lzma/include
  ${ADV_SRC}/alien/lzma/src
  ${ADV_SRC}/alien/expat
  ${ADV_SRC}/alien/sljit
  ${CPP_DIR}/config
)

foreach(tgt advlib advosd mamelib)
  target_compile_definitions(${tgt} PRIVATE
    NDEBUG
    _REENTRANT
    HAVE_CONFIG_H
    USE_VIDEO_SDL
    USE_SOUND_SDL
    USE_KEYBOARD_SDL
    USE_MOUSE_SDL
    USE_JOYSTICK_SDL
    USE_SMP
    USE_LCD
    LSB_FIRST
    asm=__asm__
    stricmp=strcasecmp
    strnicmp=strncasecmp
    ADV_DATADIR="/data/data/com.edsonmfernando.advancemame/files"
    ADV_SYSCONFDIR="/data/data/com.edsonmfernando.advancemame/files"
    ADV_VERSION="5.0-android"
    ${HAS_DEFS}
  )
  target_compile_options(${tgt} PRIVATE
    -O2
    -fno-strict-aliasing
    -fno-math-errno
    -w
  )
  set_property(TARGET ${tgt} PROPERTY POSITION_INDEPENDENT_CODE ON)
endforeach()

# o osd_stretch_palett16to32 em video.c usa UINT32 tipado pelo MAME
# (osdepend.h), pre-incluimos osd_cpu.h so para este arquivo
set(ADV_OSD_CPU_INC "-include")
set_source_files_properties(${ADV_SRC}/advance/osd/video.c PROPERTIES
  COMPILE_OPTIONS "${ADV_OSD_CPU_INC};${ADV_SRC}/advance/osd/osd_cpu.h")

set_source_files_properties(${ADV_SRC}/advance/linux/os.c PROPERTIES
  COMPILE_DEFINITIONS "main=advmame_main;SDL_MAIN_HANDLED")

# advance/osd compila com -DMAME e PI=M_PI e ADV_EMU (como EMUCFLAGS+MAMECFLAGS do upstream)
target_compile_definitions(advosd PRIVATE
  MAME
  ADV_EMU
  PI=M_PI
)

# o PI sem espacos: usado pelos fontes do MAME (o advance/blit usa PI
# como nome de variavel nos arquivos NEON, entao nao pode ser global)
target_compile_definitions(mamelib PRIVATE
  MAME
  PI=M_PI
)

# o INLINE com espaco precisa passar como opcao direta
foreach(tgt advlib advosd mamelib)
  target_compile_options(${tgt} PRIVATE "-DINLINE=static __inline__")
endforeach()

add_library(main SHARED
  ${CPP_DIR}/main.c
  $<TARGET_OBJECTS:advosd>
  $<TARGET_OBJECTS:advlib>
  $<TARGET_OBJECTS:mamelib>
)

target_include_directories(main PRIVATE
  ${CPP_DIR}/config
  ${ADV_SRC}
  ${ADV_SRC}/mame
  ${ADV_SRC}/mame/includes
  ${ADV_SRC}/advance/osd
  ${ADV_SRC}/advance/lib
  ${ADV_SRC}/advance/blit
  ${ADV_SRC}/advance/sdl
)

target_compile_definitions(main PRIVATE
  HAVE_CONFIG_H
  ADV_DATADIR="/data/data/com.edsonmfernando.advancemame/files"
  VERSION="5.0-android"
)

target_link_libraries(main
  SDL2::SDL2-static
  android
  log
  m
  # runtime C++ (o hidapi do SDL2 usa C++); ligado por ultimo para resolver os
  # simbolos do libSDL2.a estatico
  c++_static
  c++abi
)
