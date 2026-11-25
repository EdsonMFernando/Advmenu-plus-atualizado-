package com.edsonmfernando.advancemame;

import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AdvanceMAME rodando sobre SDL2.
 *
 * Na primeira execucao extrai do APK os arquivos de dados e as ROMs livres
 * (gridlee, polyplay, robby) para o diretorio do aplicativo em
 * /data/data/com.edsonmfernando.advancemame/files, que tambem e usado
 * como $HOME pelo emulador ($HOME/.advance = configuracao; $HOME/rom = ROMs).
 *
 * Para rodar um jogo especifico: adb shell am start -n \
 *   com.edsonmfernando.advancemame/.MainActivity --es rom gridlee
 *
 * ROMs adicionais podem ser copiadas para:
 *   /data/data/com.edsonmfernando.advancemame/files/rom/
 */
public class MainActivity extends SDLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AssetInstaller.install(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String[] getLibraries() {
        return new String[]{"main"};
    }

    @Override
    protected String[] getArguments() {
        String rom = getIntent() != null ? getIntent().getStringExtra("rom") : null;
        if (rom == null || rom.length() == 0) {
            rom = "gridlee";
        }
        return new String[]{"advmame", rom};
    }
}
