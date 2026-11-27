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
            if (!fromCard) {
                applyChipProfile(lines);
            }
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

    /**
     * Perfil de desempenho por chipset (config DENTRO do codigo).
     *
     * GPU drivers (Adreno/Mali/PowerVR) sao fechados dos fabricantes: nenhum
     * aplicativo pode cria-los ou troca-los. O que o Advmenu Plus controla sao
     * os padroes do advmame.rc gerado, ajustados automaticamente ao aparelho:
     * /misc_smp (blit em thread separada), /sound_samplerate (menos CPU de
     * audio), /sound_latency (evita estouro no som em chips lentos) e
     * /display_vsync — todas confirmadas no advance/osd do port (video.c,
     * sound.c). Prioridade: advmenu.rc/.ini do cartao (nao mexe) > perfil
     * automatico por chipset > padrao embutido.
     */
    private static final String[][] PROFILE_FULL = {
            {"misc_smp", "yes"},
            {"sound_samplerate", "44100"},
            {"sound_latency", "0.1"},
            {"display_vsync", "yes"},
    };

    private static final String[][] PROFILE_MID = {
            {"misc_smp", "yes"},
            {"sound_samplerate", "32000"},
            {"sound_latency", "0.2"},
            {"display_vsync", "yes"},
    };

    private static final String[][] PROFILE_ECO = {
            {"misc_smp", "yes"},
            {"sound_samplerate", "22050"},
            {"sound_latency", "0.3"},
            {"display_vsync", "yes"},
    };

    /** Detecta o chipset do aparelho e devolve o perfil de desempenho:
     *  "full" (Snapdragon/Qualcomm, Dimensity, Exynos 9/2, Kirin 9, Tegra),
     *  "eco" (Unisoc/Spreadtrum, MediaTek de entrada) ou "mid" (demais).
     */
    private static String chipProfile() {
        String hw = "";
        try {
            hw = android.os.Build.HARDWARE == null ? "" : android.os.Build.HARDWARE.toLowerCase();
        } catch (Throwable t) {
            hw = "";
        }
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            try {
                String soc = android.os.Build.SOC_MODEL;
                if (soc != null && soc.length() > 0) {
                    hw = hw + " " + soc.toLowerCase();
                }
            } catch (Throwable t) {
                // campo novo (Android 12+), ignorado em aparelhos antigos
            }
        }
        boolean snap = hw.contains("qcom") || hw.contains("qualcomm")
                || hw.contains("tegra")
                || hw.matches(".*(sm[0-9]{4}|sdm[0-9]+|msm[0-9]+|apq[0-9]+|qcs[0-9]+).*");
        if (snap) {
            return "full";
        }
        if (hw.contains("mediatek") || hw.contains("dimensity")
                || hw.matches(".*mt[0-9]{4}.*")) {
            if (hw.matches(".*mt6[89].*")) {
                return "full";
            }
            if (hw.matches(".*mt6[57].*")) {
                return "eco";
            }
            return "mid";
        }
        if (hw.contains("sprd") || hw.contains("unisoc")
                || hw.matches(".*(ums[0-9]|sp98|sc98|sc96|sc77).*")) {
            return "eco";
        }
        if (hw.contains("kirin") || hw.contains("hisi")
                || hw.contains("hi36") || hw.contains("hi6")) {
            return hw.matches(".*(kirin9|hi36[6-9]0).*") ? "full" : "mid";
        }
        if (hw.contains("exynos") || hw.contains("universal") || hw.contains("s5e")) {
            return hw.matches(".*(exynos9|exynos98|exynos99|exynos2|universal9|universal2|s5e99).*")
                    ? "full" : "mid";
        }
        if (hw.matches(".*rk3[0-9]{3}.*") || hw.contains("rockchip")) {
            return "mid";
        }
        if (hw.contains("meson") || hw.contains("amlogic")
                || hw.matches(".*s9[0-9]{2}[a-z]?.*")) {
            return "mid";
        }
        return "mid";
    }

    /** Injeta as linhas do perfil no advmame.rc gerado. NAO roda quando o
     *  advmame.rc/advmenu.rc veio do cartao (config do usuario manda). */
    private static void applyChipProfile(ArrayList<String> lines) {
        String profile = chipProfile();
        String[][] opts = "full".equals(profile) ? PROFILE_FULL
                : "eco".equals(profile) ? PROFILE_ECO : PROFILE_MID;
        for (int i = 0; i < opts.length; ++i) {
            String key = opts[i][0];
            for (int j = lines.size() - 1; j >= 0; --j) {
                String t = lines.get(j).trim();
                if (t.startsWith(key) && (t.length() == key.length()
                        || t.charAt(key.length()) == ' '
                        || t.charAt(key.length()) == '\t')) {
                    lines.remove(j);
                }
            }
        }
        lines.add("# Perfil de desempenho automatico (chipset " + profile + ")");
        for (int i = 0; i < opts.length; ++i) {
            lines.add(opts[i][0] + " " + opts[i][1]);
        }
        Log.i("AdvanceMAME", "perfil de chipset: " + profile);
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
