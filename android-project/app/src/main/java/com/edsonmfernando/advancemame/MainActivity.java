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

    private static final String TAG = "AdvanceMAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            extractAssets();
        } catch (IOException e) {
            Log.e(TAG, "erro extraindo assets", e);
        }
        super.onCreate(savedInstanceState);
    }

    private void extractAssets() throws IOException {
        File marker = new File(getFilesDir(), ".installed");
        if (marker.exists()) {
            return;
        }
        new File(getFilesDir(), "rom").mkdirs();
        unpackDir("data", getFilesDir());
        unpackDir("rom", new File(getFilesDir(), "rom"));
        marker.createNewFile();
    }

    private void unpackDir(String assetDir, File dst) throws IOException {
        dst.mkdirs();
        String[] names = getAssets().list(assetDir);
        if (names == null) {
            return;
        }
        for (String name : names) {
            File out = new File(dst, name);
            String asset = assetDir + "/" + name;
            if (getAssets().list(asset).length > 0) {
                unpackDir(asset, out);
            } else {
                try (InputStream in = getAssets().open(asset);
                     OutputStream outStream = new FileOutputStream(out)) {
                    byte[] buffer = new byte[65536];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        outStream.write(buffer, 0, n);
                    }
                }
            }
        }
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
