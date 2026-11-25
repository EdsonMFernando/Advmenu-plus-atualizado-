package com.edsonmfernando.advancemame;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Extrai na primeira execucao os assets embutidos no APK (dados do emulador
 * e ROMs livres) para o diretorio do aplicativo, usado como $HOME pelo
 * AdvanceMAME ($HOME/.advance = configuracao; $HOME/rom = ROMs).
 */
final class AssetInstaller {

    private static final String TAG = "AdvanceMAME";

    private AssetInstaller() {
    }

    static void install(Context ctx) {
        File marker = new File(ctx.getFilesDir(), ".installed");
        if (marker.exists()) {
            return;
        }
        try {
            new File(ctx.getFilesDir(), "rom").mkdirs();
            unpackDir(ctx, "data", ctx.getFilesDir());
            unpackDir(ctx, "rom", new File(ctx.getFilesDir(), "rom"));
            marker.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "erro extraindo assets", e);
        }
    }

    private static void unpackDir(Context ctx, String assetDir, File dst) throws IOException {
        dst.mkdirs();
        String[] names = ctx.getAssets().list(assetDir);
        if (names == null) {
            return;
        }
        for (String name : names) {
            File out = new File(dst, name);
            String asset = assetDir + "/" + name;
            if (ctx.getAssets().list(asset).length > 0) {
                unpackDir(ctx, asset, out);
            } else {
                try (InputStream in = ctx.getAssets().open(asset);
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

    /**
     * Raizes de armazenamento do aparelho: memoria interna emulada (/sdcard),
     * cartao de memoria fisico (/storage/XXXX-XXXX) e legado SECONDARY_STORAGE.
     */
    static ArrayList<File> storageRoots(Context ctx) {
        ArrayList<File> roots = new ArrayList<>();
        File primary = Environment.getExternalStorageDirectory();
        if (primary != null) {
            roots.add(primary);
        }
        for (File f : ctx.getExternalFilesDirs(null)) {
            File dir = f;
            int depth = 0;
            while (dir != null && depth < 5) {
                if ("Android".equals(dir.getName())) {
                    File vol = dir.getParentFile();
                    if (vol != null && !roots.contains(vol)) {
                        roots.add(vol);
                    }
                    break;
                }
                dir = dir.getParentFile();
                ++depth;
            }
        }
        String secondary = System.getenv("SECONDARY_STORAGE");
        if (secondary != null) {
            for (String p : secondary.split(":")) {
                if (p.length() == 0) {
                    continue;
                }
                File f = new File(p);
                if (!roots.contains(f)) {
                    roots.add(f);
                }
            }
        }
        return roots;
    }

    /**
     * Procura advmenu.rc /.ini ou advmame.rc/.ini no cartao de memoria
     * (raiz de cada armazenamento e na pasta advmenu/). Retorna as linhas
     * do primeiro arquivo achado, ou null se nao houver.
     */
    static ArrayList<String> readCardConfig(Context ctx) {
        String[] names = {"advmenu.ini", "advmenu.rc", "advmame.ini", "advmame.rc"};
        ArrayList<File> bases = new ArrayList<>(storageRoots(ctx));
        ArrayList<File> bases2 = new ArrayList<>();
        for (File b : bases) {
            bases2.add(b);
            File adv = new File(b, "advmenu");
            if (adv.isDirectory()) {
                bases2.add(adv);
            }
        }
        for (File base : bases2) {
            File[] children = base.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                String n = child.getName().toLowerCase(java.util.Locale.US);
                for (String want : names) {
                    if (n.equals(want) && child.isFile()) {
                        try {
                            ArrayList<String> lines = new ArrayList<>();
                            java.io.BufferedReader in = new java.io.BufferedReader(
                                    new java.io.FileReader(child));
                            for (String line; (line = in.readLine()) != null; ) {
                                lines.add(line);
                            }
                            in.close();
                            Log.i(TAG, "config do cartao usada: " + child);
                            return lines;
                        } catch (IOException e) {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
}
