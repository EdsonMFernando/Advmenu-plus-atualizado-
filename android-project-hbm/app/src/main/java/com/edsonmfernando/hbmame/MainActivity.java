package com.edsonmfernando.hbmame;

import org.libsdl.app.SDLActivity;

/**
 * APK avulso HBMAME Android (port nativo 0.289 — hbm-port).
 * O SDLActivity carrega o libmain.so (alvo "main" do hbm-port/CMakeLists.txt
 * com HBM_APK_GLUE=ON), cujo glue ajusta HOME/cwd e repassa para o main do
 * OSD SDL do HBMAME. A ROM pode vir por intent (VIEW application/zip) — o
 * Advmenu Plus Android detecta este app e abre a pasta hbmame/ nele.
 */
public class MainActivity extends SDLActivity {

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "main"
        };
    }
}
