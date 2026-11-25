package com.edsonmfernando.advancemame;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Front-end no estilo do ES-DE (visao basica): lista os jogos disponiveis em
 * $HOME/rom, navega por toque ou d-pad (D-pad para mover, A/Enter para jogar)
 * e inicia o AdvanceMAME na ROM escolhida.
 *
 * Capas opcionais (estilo "media" do ES-DE):
 *   Android/data/com.edsonmfernando.advancemame/files/media/&lt;rom&gt;.png|jpg|jpeg|webp
 */
public class FrontendActivity extends Activity {

    private static final int COR_FUNDO = Color.rgb(16, 24, 32);
    private static final int COR_TITULO = Color.rgb(255, 255, 255);
    private static final int COR_SUBTITULO = Color.rgb(138, 180, 200);
    private static final int COR_TEXTO = Color.rgb(238, 238, 238);
    private static final int COR_SELEC = Color.argb(60, 33, 150, 243);

    private GameAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AssetInstaller.install(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COR_FUNDO);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setGravity(Gravity.CENTER);
        int padHead = dp(16);
        head.setPadding(padHead, padHead, padHead, dp(8));

        TextView title = new TextView(this);
        title.setText("ADVMENU PLUS");
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setTextSize(30);
        title.setTextColor(COR_TITULO);
        title.setGravity(Gravity.CENTER);
        head.addView(title);

        TextView sub = new TextView(this);
        sub.setTextColor(COR_SUBTITULO);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        head.addView(sub);

        adapter = new GameAdapter();
        adapter.load(this);

        sub.setText(adapter.getCount() == 0
                ? "Nenhum jogo. Copie seus ROMs em files/rom/"
                : adapter.getCount() + " jogos  •  d-pad move, A joga");

        list = new ListView(this);
        list.setSelector(new ColorDrawable(COR_SELEC));
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(adapter.romAt(position));
            }
        });

        root.addView(head);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    private void launch(String rom) {
        startActivity(new Intent(this, MainActivity.class).putExtra("rom", rom));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String prettyName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String name = dot > 0 ? fileName.substring(0, dot) : fileName;
        return name.replace('_', ' ').toUpperCase();
    }

    private final class GameAdapter extends BaseAdapter {

        private final ArrayList<String> roms = new ArrayList<>();
        private final ArrayList<String> titles = new ArrayList<>();

        void load(FrontendActivity act) {
            File romDir = new File(act.getFilesDir(), "rom");
            File[] files = romDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName().toLowerCase();
                    if (f.isFile() && (n.endsWith(".zip") || n.endsWith(".7z"))) {
                        roms.add(n.substring(0, n.lastIndexOf('.')));
                    }
                }
            }
            Collections.sort(roms);
            for (String rom : roms) {
                titles.add(prettyName(rom));
            }
        }

        String romAt(int position) {
            return roms.get(position);
        }

        @Override
        public int getCount() {
            return roms.size();
        }

        @Override
        public Object getItem(int position) {
            return roms.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = new LinearLayout(FrontendActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int pad = dp(10);
            row.setPadding(pad, pad, pad, pad);

            Bitmap cover = loadCover(roms.get(position), dp(44));
            if (cover != null) {
                ImageView img = new ImageView(FrontendActivity.this);
                img.setImageBitmap(cover);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
                lp.rightMargin = dp(10);
                row.addView(img, lp);
            }

            TextView tv = new TextView(FrontendActivity.this);
            tv.setText(titles.get(position));
            tv.setTextSize(17);
            tv.setTextColor(COR_TEXTO);
            tv.setSingleLine(true);
            row.addView(tv, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            return row;
        }

        private Bitmap loadCover(String rom, int sizePx) {
            String[] exts = {".png", ".jpg", ".jpeg", ".webp"};
            for (String ext : exts) {
                File f = new File(new File(getFilesDir(), "media"), rom + ext);
                if (!f.isFile()) {
                    continue;
                }
                try {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
                    opts.inSampleSize = Math.max(1, Math.max(opts.outWidth, opts.outHeight) / (sizePx * 2));
                    opts.inJustDecodeBounds = false;
                    return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
                } catch (Throwable t) {
                    return null;
                }
            }
            return null;
        }
    }

    private ListView list;
}
