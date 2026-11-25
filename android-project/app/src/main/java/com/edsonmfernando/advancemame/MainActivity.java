package com.edsonmfernando.advancemame;

import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * AdvanceMAME rodando sobre SDL2.
 *
 * Na primeira execucao extrai do APK os arquivos de dados e as ROMs livres
 * (gridlee, polyplay, robby) para o diretorio do aplicativo em
 * /data/data/com.edsonmfernando.advancemame/files, que tambem e usado
 * como $HOME pelo emulador ($HOME/.advance = configuracao; $HOME/rom = ROMs).
 *
 * As pastas de ROMs do cartao de memoria (ex.: /sdcard/ROMs), passadas pelo
 * front-end no extra "romDirs", sao adicionadas ao `dir_rom' do
 * $HOME/.advance/advmame.rc para o emulador tambem enxerga-las.
 *
 * Para rodar um jogo especifico: adb shell am start -n \
 *   com.edsonmfernando.advancemame/.MainActivity --es rom gridlee
 *
 * ROMs adicionais podem ser copiadas para:
 *   /sdcard/ROMs/  (cartao de memoria)
 *   /data/data/com.edsonmfernando.advancemame/files/rom/
 */
public class MainActivity extends SDLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AssetInstaller.install(this);
        configureDirRom();
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

    /**
     * Escreve `dir_rom' no advmame.rc (multi-valor, separado por ':'):
     * "rom" (diretorio interno) + as pastas de ROMs do cartao de memoria.
     * Sobrescreve quaisquer `dir_rom' anteriores; as demais linhas sao mantidas.
     */
    private void configureDirRom() {
        ArrayList<String> extra = getIntent() != null
                ? getIntent().getStringArrayListExtra("romDirs") : null;
        StringBuilder paths = new StringBuilder("rom");
        if (extra != null) {
            ArrayList<String> seen = new ArrayList<>();
            seen.add("rom");
            for (String p : extra) {
                if (p != null && p.length() > 0 && !seen.contains(p)) {
                    seen.add(p);
                    paths.append(':').append(p);
                }
            }
        }
        try {
            File advDir = new File(getFilesDir(), ".advance");
            advDir.mkdirs();
            File rc = new File(advDir, "advmame.rc");
            ArrayList<String> lines = new ArrayList<>();
            if (rc.isFile()) {
                BufferedReader in = new BufferedReader(new FileReader(rc));
                for (String line; (line = in.readLine()) != null; ) {
                    if (!line.trim().startsWith("dir_rom")) {
                        lines.add(line);
                    }
                }
                in.close();
            }
            lines.add("dir_rom " + paths);
            BufferedWriter out = new BufferedWriter(new FileWriter(rc));
            for (String line : lines) {
                out.write(line);
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            Log.e("AdvanceMAME", "erro configurando dir_rom", e);
        }
    }
}
