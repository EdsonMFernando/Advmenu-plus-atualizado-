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
 * $HOME = /data/data/com.edsonmfernando.advancemame/files
 *   $HOME/.advance/advmame.rc  = configuracao do emulador (padrao no codigo:
 *                                classe AdvConfig; ou advmenu.rc/.ini do cartao)
 *   $HOME/.advance/advmenu.rc  = configuracao do menu classico (mesma base)
 *   $HOME/rom                  = ROMs internas
 *
 * As pastas de ROMs do cartao de memoria (ex.: /sdcard/ROMs) passadas pelo
 * front-end no extra "romDirs" sao injetadas no `dir_rom' (o padrão do
 * AdvanceMAME aceita multiplas pastas separadas por ':').
 *
 * Para rodar um jogo especifico: adb shell am start -n \
 *   com.edsonmfernando.advancemame/.MainActivity --es rom gridlee
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
     * Gera .advance/advmame.rc e .advance/advmenu.rc a partir de, nesta ordem:
     * 1. advmenu.rc/.ini (ou advmame.rc/.ini) na raiz do cartao de memoria —
     *    reconhecido diretamente, sem script;
     * 2. o advmame.rc ja existente (salvo pelo proprio emulador);
     * 3. o padrao embutido no codigo (AdvConfig).
     * Em qualquer caso, `dir_rom' e reescrito com "rom" + as pastas do cartao.
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
        ArrayList<String> base = AssetInstaller.readCardConfig(this);
        boolean fromCard = base != null;
        try {
            File advDir = new File(getFilesDir(), ".advance");
            advDir.mkdirs();
            if (base == null) {
                File rc = new File(advDir, "advmame.rc");
                if (rc.isFile()) {
                    base = new ArrayList<>();
                    BufferedReader in = new BufferedReader(new FileReader(rc));
                    for (String line; (line = in.readLine()) != null; ) {
                        base.add(line);
                    }
                    in.close();
                } else {
                    base = splitLines(AdvConfig.ADVMAME_RC);
                }
            }
            ArrayList<String> lines = new ArrayList<>();
            for (String line : base) {
                if (!line.trim().startsWith("dir_rom")) {
                    lines.add(line);
                }
            }
            lines.add("dir_rom " + paths);
            writeRc(new File(advDir, "advmame.rc"), lines);
            ArrayList<String> menuBase = fromCard ? base : splitLines(AdvConfig.ADVMENU_RC);
            ArrayList<String> menuLines = new ArrayList<>();
            for (String line : menuBase) {
                if (!line.trim().startsWith("dir_rom")) {
                    menuLines.add(line);
                }
            }
            menuLines.add("dir_rom " + paths);
            writeRc(new File(advDir, "advmenu.rc"), menuLines);
        } catch (IOException e) {
            Log.e("AdvanceMAME", "erro configurando advmame.rc/advmenu.rc", e);
        }
    }

    private static ArrayList<String> splitLines(String s) {
        ArrayList<String> out = new ArrayList<>();
        for (String line : s.split("\n")) {
            out.add(line);
        }
        return out;
    }

    private static void writeRc(File rc, ArrayList<String> lines) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(rc));
        for (String line : lines) {
            out.write(line);
            out.newLine();
        }
        out.close();
    }
}
