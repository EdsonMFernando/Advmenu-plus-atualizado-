package com.edsonmfernando.advancemame;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
}
