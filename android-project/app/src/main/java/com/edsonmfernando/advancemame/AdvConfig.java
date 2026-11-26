package com.edsonmfernando.advancemame;

/**
 * Configuracao padrao do AdvanceMAME/AdvMenu EMBUTIDA NO CODIGO-FONTE.
 *
 * Nada fica solto: na primeira execucao estas linhas viram os arquivos
 * .advance/advmame.rc e .advance/advmenu.rc no diretorio do app — os mesmos
 * arquivos que o AdvanceMAME/AdvMenu classicos leem. Para personalizar sem
 * recompilar, faca um advmenu.rc ou advmenu.ini na raiz do cartao de memoria:
 * o app reconhece e usa ELE no lugar deste padrao (e injeta as pastas de
 * ROMs do cartao no dir_rom automaticamente).
 */
final class AdvConfig {

    private AdvConfig() {
    }

    static final String ADVMAME_RC = "\n"
            + "# ===================================================\n"
            + "# Advmenu Plus Android - configuracao padrao (codigo)\n"
            + "# ===================================================\n"
            + "# Pastas de dados (relativas ao diretorio do app):\n"
            + "dir_rom rom\n"
            + "dir_image image\n"
            + "dir_sample sample\n"
            + "dir_artwork artwork\n"
            + "dir_nvram nvram\n"
            + "dir_memcard memcard\n"
            + "dir_inp inp\n"
            + "dir_sta sta\n"
            + "dir_snap snap\n"
            + "dir_hi hi\n"
            + "dir_diff diff\n";

    static final String ADVMENU_RC = "\n"
            + "# ===================================================\n"
            + "# Advmenu Plus Android - configuracao padrao do menu\n"
            + "# ===================================================\n"
            + "# Pastas de dados (relativas ao diretorio do app):\n"
            + "dir_rom rom\n"
            + "dir_preview preview\n"
            + "dir_icon icon\n"
            + "dir_snap snap\n";
}
