# PORT NATIVO HBMAME 0.289 (hbm-port) — Fase 2: nucleo via CMake (substitui o
# GENie). Compila as bibliotecas de suporte do nucleo MAME (util, formats,
# netlist) em .a estatico, para o NDK (arm64-v8a / armeabi-v7a). Nao afeta o
# build do APK enquanto o port nao estiver linkado (Fase 5); build manual:
#
#   cmake -G Ninja -B android-project/hbm-port/build/arm64-v8a \
#         -S android-project/hbm-port \
#         -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
#         -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-24
#   cmake --build android-project/hbm-port/build/arm64-v8a -t hbmcore
#
# Requisito: fonte do HBMAME vendored em hbmame/ (android-project/tools/
# sync-hbmame.sh, do PC durante o desenvolvimento; nada disso roda no APK).
cmake_minimum_required(VERSION 3.22)
project(hbm-port VERSION 0.289.0 LANGUAGES C CXX)

set(HBM_DIR "${CMAKE_CURRENT_LIST_DIR}/../../hbmame")
file(TO_CMAKE_PATH "${HBM_DIR}" HBM_DIR)

if(NOT EXISTS "${HBM_DIR}/src/lib/util/corestr.cpp")
    message(FATAL_ERROR
        "Fonte do HBMAME nao encontrada em ${HBM_DIR}. Baixe com "
        "android-project/tools/sync-hbmame.sh (do PC, uma vez).")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# CRLF=3: mesmo valor do smoke test; os drivers usam a infraestrutura de
# codigo do nucleo sem depender de features x86 do host.
add_compile_definitions(CRLF=3)

include_directories(
    "${HBM_DIR}/src/lib"
    "${HBM_DIR}/src/lib/netlist"
    "${HBM_DIR}/src/lib/netlist/core"
    "${HBM_DIR}/src/lib/netlist/devices"
    "${HBM_DIR}/src/lib/netlist/prg"
    "${HBM_DIR}/src/lib/formats"
    "${HBM_DIR}/src/osd"
    "${HBM_DIR}/src/devices"
    "${HBM_DIR}/src/emu"
    "${HBM_DIR}/3rdparty"
    "${HBM_DIR}/3rdparty/flac/include"
    "${HBM_DIR}/3rdparty/zstd/lib"
    "${HBM_DIR}/3rdparty/expat/lib"
    "${HBM_DIR}/3rdparty/utf8proc"
    "${HBM_DIR}/3rdparty/lzma/C"
    "${HBM_DIR}/3rdparty/aes128cbc"
    "${HBM_DIR}/3rdparty/aes192cbc"
    "${HBM_DIR}/3rdparty/aes256cbc"
    "${HBM_DIR}/3rdparty/libjpeg"
    "${HBM_DIR}/3rdparty/expat"
    "${HBM_DIR}/3rdparty/pugixml/src"
    "${HBM_DIR}/3rdparty/softfloat3/include"
    "${HBM_DIR}/3rdparty/glm"
    "${HBM_DIR}/3rdparty/rapidjson/include"
    "${HBM_DIR}/3rdparty/lua"
    "${HBM_DIR}/3rdparty/zlib"
)

file(GLOB_RECURSE HBM_UTIL_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/lib/util/*.cpp")
file(GLOB HBM_FORMATS_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/lib/formats/*.cpp")
file(GLOB_RECURSE HBM_NETLIST_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/lib/netlist/*.cpp")
list(FILTER HBM_NETLIST_SOURCES EXCLUDE REGEX
    "/examples/|/tests/|/prg/")

# has_formats.h: o upstream gera pelo GENie; geramos direto no CMake
# (equivale a #define HAS_FORMATS_<NOME> para cada formato compilado),
# dentro da arvore de build — sem tocar na fonte vendored.
set(HAS_FORMATS_H "${CMAKE_CURRENT_BINARY_DIR}/generated/has_formats.h")
set(HAS_FORMATS_CONTENT "")
foreach(src ${HBM_FORMATS_SOURCES})
    get_filename_component(name "${src}" NAME_WE)
    string(TOUPPER "${name}" name)
    string(REGEX REPLACE "[^A-Z0-9]" "_" name "${name}")
    string(APPEND HAS_FORMATS_CONTENT "#define HAS_FORMATS_${name} 1\n")
endforeach()
file(WRITE "${HAS_FORMATS_H}" "${HAS_FORMATS_CONTENT}")

add_library(hbmcore STATIC
    ${HBM_UTIL_SOURCES} ${HBM_FORMATS_SOURCES} ${HBM_NETLIST_SOURCES})
target_include_directories(hbmcore PRIVATE
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
    "${HBM_DIR}/src/lib/util")

# Fase 3: OSD SDL (src/osd) sobre o SDL2 vendored (app/src/main/cpp/SDL2).
file(GLOB_RECURSE HBM_OSD_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/osd/*.cpp")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/osd/windows/|/osd/mac/|/osd/winui/|/osd/sdl3/|/osd/asio\\.cpp$")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/debugger/win/|/debugger/qt/")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/modules/file/(win|winrt|stdfile)")
list(FILTER HBM_OSD_SOURCES EXCLUDE REGEX
    "/modules/(midi/portmidi|sound/(pa|pulse|pipewire)_sound|output/network|opengl/gl_shader|lib/osdlib_(macosx|win32)|render/drawd3d|render/d3d/|lib/asio)")
# "SDL2/SDL.h": no CMake, o caminho SDL2/include do SDL vendored e exposto
# como <SDL2/...> por um link dentro da arvore de build.
file(MAKE_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim")
file(CREATE_LINK
    "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2/include"
    "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim/SDL2"
    SYMBOLIC)
add_library(hbmosd STATIC ${HBM_OSD_SOURCES})
target_include_directories(hbmosd PRIVATE
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
    "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim"
    "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2/include"
    "${HBM_DIR}/src/osd"
    "${HBM_DIR}/src/osd/sdl"
    "${HBM_DIR}/src/osd/modules/render"
    "${HBM_DIR}/src/frontend/mame"
    "${HBM_DIR}/src/lib/util"
    "${HBM_DIR}/3rdparty/bgfx/include"
    "${HBM_DIR}/3rdparty/bx/include"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/dear-imgui"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty"
    "${HBM_DIR}/3rdparty/bgfx/examples/common"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/khronos")
target_compile_definitions(hbmosd PRIVATE
    OSD_SDL USE_OPENGL=0 USE_BGFX=1 BX_CONFIG_DEBUG=0)

# Fase 2.5: nucleo de emulacao (src/emu). Os headers gerados upstream pelo
# GENie (layouts comprimidos e fontes da UI) sao reproduzidos aqui com os
# scripts oficiais do proprio HBMAME (complay.py, png2bdc.py, file2str.py),
# dentro da arvore de build do CMake — sem tocar na fonte vendored.
find_program(PYTHON3_EXECUTABLE python3 REQUIRED)

set(EMU_GEN_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated/emu")
file(MAKE_DIRECTORY "${EMU_GEN_DIR}/layout")
file(MAKE_DIRECTORY "${EMU_GEN_DIR}/ui")

set(HBM_EMU_GENS "")
foreach(lay noscreens monitors dualhsxs dualhovu dualhuov triphsxs quadhsxs)
    add_custom_command(OUTPUT "${EMU_GEN_DIR}/layout/${lay}.lh"
        COMMAND ${PYTHON3_EXECUTABLE} "${HBM_DIR}/scripts/build/complay.py"
                "${HBM_DIR}/src/emu/layout/${lay}.lay"
                "${EMU_GEN_DIR}/layout/${lay}.lh" "layout_${lay}"
        DEPENDS "${HBM_DIR}/src/emu/layout/${lay}.lay"
                "${HBM_DIR}/scripts/build/complay.py"
        VERBATIM)
    list(APPEND HBM_EMU_GENS "${EMU_GEN_DIR}/layout/${lay}.lh")
endforeach()

add_custom_command(OUTPUT "${EMU_GEN_DIR}/uismall.fh"
    COMMAND ${PYTHON3_EXECUTABLE} "${HBM_DIR}/scripts/build/file2str.py"
            "${HBM_DIR}/scripts/font/NotoSans-Bold.bdc"
            "${EMU_GEN_DIR}/uismall.fh" "font_uismall" "uint8_t"
    DEPENDS "${HBM_DIR}/scripts/font/NotoSans-Bold.bdc"
            "${HBM_DIR}/scripts/build/file2str.py"
    VERBATIM)
list(APPEND HBM_EMU_GENS "${EMU_GEN_DIR}/uismall.fh")

add_custom_command(OUTPUT "${EMU_GEN_DIR}/ui/uicmd14.fh"
    COMMAND ${CMAKE_COMMAND} -E make_directory "${CMAKE_CURRENT_BINARY_DIR}/tmp"
    COMMAND ${PYTHON3_EXECUTABLE} "${HBM_DIR}/scripts/build/png2bdc.py"
            "${HBM_DIR}/src/frontend/mame/ui/uicmd14.png"
            "${CMAKE_CURRENT_BINARY_DIR}/tmp/uicmd14.bdc"
    COMMAND ${PYTHON3_EXECUTABLE} "${HBM_DIR}/scripts/build/file2str.py"
            "${CMAKE_CURRENT_BINARY_DIR}/tmp/uicmd14.bdc"
            "${EMU_GEN_DIR}/ui/uicmd14.fh" "font_uicmd14" "uint8_t"
    DEPENDS "${HBM_DIR}/src/frontend/mame/ui/uicmd14.png"
            "${HBM_DIR}/scripts/build/png2bdc.py"
            "${HBM_DIR}/scripts/build/file2str.py"
    VERBATIM)
list(APPEND HBM_EMU_GENS "${EMU_GEN_DIR}/ui/uicmd14.fh")

# COPYING -> copying.ipp (file2lines.py), exigido pelo frontend (about.cpp)
# tem de entrar ANTES do add_custom_target para virar dependencia dele.
add_custom_command(OUTPUT "${EMU_GEN_DIR}/copying.ipp"
    COMMAND ${PYTHON3_EXECUTABLE} "${HBM_DIR}/scripts/build/file2lines.py"
            "${HBM_DIR}/COPYING" "${EMU_GEN_DIR}/copying.ipp" "copying_text"
    DEPENDS "${HBM_DIR}/COPYING"
            "${HBM_DIR}/scripts/build/file2lines.py"
    VERBATIM)
list(APPEND HBM_EMU_GENS "${EMU_GEN_DIR}/copying.ipp")

add_custom_target(hbmgen-emu DEPENDS ${HBM_EMU_GENS})

file(GLOB_RECURSE HBM_EMU_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/emu/*.cpp")
list(FILTER HBM_EMU_SOURCES EXCLUDE REGEX
    "/emu/http\\.cpp$|/emu/drivers/testcpu\\.cpp$")
add_library(hbmemu STATIC ${HBM_EMU_SOURCES})
add_dependencies(hbmemu hbmgen-emu)
target_include_directories(hbmemu PRIVATE
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
    "${EMU_GEN_DIR}"
    "${EMU_GEN_DIR}/layout"
    "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim"
    "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2/include"
    "${HBM_DIR}/src/osd"
    "${HBM_DIR}/src/osd/sdl"
    "${HBM_DIR}/src/osd/modules/render"
    "${HBM_DIR}/src/frontend/mame"
    "${HBM_DIR}/src/lib/util"
    "${HBM_DIR}/3rdparty/bgfx/include"
    "${HBM_DIR}/3rdparty/bx/include"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/dear-imgui"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty"
    "${HBM_DIR}/3rdparty/bgfx/examples/common"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/khronos")
target_compile_definitions(hbmemu PRIVATE
    OSD_SDL USE_OPENGL=0 USE_BGFX=1 BX_CONFIG_DEBUG=0)

# Fase 4 (parte 1): frontend do MAME moderno (src/frontend/mame) — o menu
# interno (lista de sistemas/jogos) e a glue principal. Ordem de includes
# IGUAL ao frontend.lua upstream (frontend/mame antes de lib/util — o
# "language.h" da UI tem de resolver no frontend, nao no util). Fora do
# alvo, de proposito: ui/slider.cpp (arquivo morto, nao esta no build
# upstream do GENie).

file(GLOB_RECURSE HBM_FRONTEND_SOURCES CONFIGURE_DEPENDS
    "${HBM_DIR}/src/frontend/mame/*.cpp")
list(FILTER HBM_FRONTEND_SOURCES EXCLUDE REGEX "/frontend/mame/ui/slider\\.cpp$")
add_library(hbmfrontend STATIC ${HBM_FRONTEND_SOURCES})
add_dependencies(hbmfrontend hbmgen-emu)
target_include_directories(hbmfrontend PRIVATE
    "${CMAKE_CURRENT_BINARY_DIR}/generated"
    "${EMU_GEN_DIR}"
    "${EMU_GEN_DIR}/layout"
    "${HBM_DIR}/src/osd"
    "${HBM_DIR}/src/emu"
    "${HBM_DIR}/src/frontend/mame"
    "${HBM_DIR}/src/devices"
    "${HBM_DIR}/src/lib"
    "${HBM_DIR}/src/lib/netlist"
    "${HBM_DIR}/src/lib/util"
    "${HBM_DIR}/src/lib/formats"
    "${HBM_DIR}/src/osd/modules/render"
    "${HBM_DIR}/src/osd/sdl"
    "${CMAKE_CURRENT_BINARY_DIR}/sdl2-shim"
    "${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2/include"
    "${HBM_DIR}/3rdparty"
    "${HBM_DIR}/3rdparty/sol2"
    "${HBM_DIR}/3rdparty/lua/src"
    "${HBM_DIR}/3rdparty/flac/include"
    "${HBM_DIR}/3rdparty/zstd/lib"
    "${HBM_DIR}/3rdparty/expat/lib"
    "${HBM_DIR}/3rdparty/utf8proc"
    "${HBM_DIR}/3rdparty/nanosvg/src"
    "${HBM_DIR}/3rdparty/lzma/C"
    "${HBM_DIR}/3rdparty/aes128cbc"
    "${HBM_DIR}/3rdparty/aes192cbc"
    "${HBM_DIR}/3rdparty/aes256cbc"
    "${HBM_DIR}/3rdparty/libjpeg"
    "${HBM_DIR}/3rdparty/expat"
    "${HBM_DIR}/3rdparty/pugixml/src"
    "${HBM_DIR}/3rdparty/softfloat3/include"
    "${HBM_DIR}/3rdparty/glm"
    "${HBM_DIR}/3rdparty/rapidjson/include"
    "${HBM_DIR}/3rdparty/zlib"
    "${HBM_DIR}/3rdparty/bgfx/include"
    "${HBM_DIR}/3rdparty/bx/include"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/dear-imgui"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty"
    "${HBM_DIR}/3rdparty/bgfx/examples/common"
    "${HBM_DIR}/3rdparty/bgfx/3rdparty/khronos")
target_compile_definitions(hbmfrontend PRIVATE
    OSD_SDL USE_OPENGL=0 USE_BGFX=1 BX_CONFIG_DEBUG=0)

# Fase 4 (parte 2): glue do APK avulso HBMAME Android. O libmain.so empacota
# os 4 alvos estaticos com --whole-archive (o SDL_main do OSD SDL e o registro
# de devices/drivers dependem dos construtores estaticos) + SDL2 estatico (o
# Java so carrega "main"). O glue/hbmmain.cpp so ajusta HOME/cwd no dlopen.
option(HBM_APK_GLUE "Gera o libmain.so do APK avulso HBMAME Android" OFF)
if(HBM_APK_GLUE)
    add_subdirectory("${CMAKE_CURRENT_LIST_DIR}/../app/src/main/cpp/SDL2"
        SDL2-build EXCLUDE_FROM_ALL)
    add_library(main SHARED glue/hbmmain.cpp)
    target_compile_definitions(main PRIVATE
        HBM_ANDROID_DATADIR="/data/data/com.edsonmfernando.hbmame/files")
    target_link_libraries(main PRIVATE
        -Wl,--whole-archive
        hbmfrontend hbmemu hbmosd hbmcore
        -Wl,--no-whole-archive
        SDL2::SDL2-static
        EGL GLESv2 android log m
        c++_static c++abi)
endif()
