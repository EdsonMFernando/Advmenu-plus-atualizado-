package com.edsonmfernando.advancemame;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Front-end no estilo do ES-DE: carrossel de jogos (item selecionado maior,
 * vizinhos esmaecidos), capa do jogo como fundo quando existir, barra
 * superior com relogio e contagem de jogos. Navega por toque ou d-pad
 * (D-pad move, A/Enter joga, B/Back volta). Inicia o AdvanceMAME na ROM.
 *
 * Capas opcionais (estilo "media" do ES-DE):
 *   Android/data/com.edsonmfernando.advancemame/files/media/&lt;rom&gt;.png|jpg|jpeg|webp
 */
public class FrontendActivity extends Activity {

    private static final int COR_FUNDO = Color.rgb(16, 24, 32);
    private static final int COR_TEXTO = Color.rgb(255, 255, 255);
    private static final int COR_NEIGHBOR = Color.rgb(120, 144, 156);
    private static final int COR_DETALHE = Color.rgb(138, 180, 200);

    private static final long WIPE_DELAY_MS = 250;

    private FrameLayout root;
    private TextView clockView;
    private TextView countView;
    private ListView list;
    private GameAdapter adapter;
    private final Handler clockHandler = new Handler();
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AssetInstaller.install(this);

        root = new FrameLayout(this);
        root.setBackgroundColor(COR_FUNDO);

        adapter = new GameAdapter();
        adapter.load(this);

        list = new ListView(this);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setVerticalFadingEdgeEnabled(true);
        list.setFadingEdgeLength(dp(48));
        list.setVerticalScrollBarEnabled(false);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(adapter.romAt(position));
            }
        });
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.selected = position;
                list.invalidateViews();
                updateBackground(adapter.romAt(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        root.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        int pad = dp(14);
        LinearLayout topbar = new LinearLayout(this);
        topbar.setOrientation(LinearLayout.VERTICAL);
        topbar.setGravity(Gravity.CENTER_HORIZONTAL);
        topbar.setPadding(pad, pad, pad, 0);

        TextView title = new TextView(this);
        title.setText("ADVMENU PLUS");
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setTextSize(22);
        title.setLetterSpacing(-0.01f);
        title.setTextColor(COR_DETALHE);
        title.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        topbar.addView(title);

        TextView system = new TextView(this);
        system.setText("ARCADE  •  ADVANCEMAME");
        system.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        system.setTextSize(13);
        system.setTextColor(COR_TEXTO);
        system.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        topbar.addView(system);

        root.addView(topbar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        clockView = new TextView(this);
        clockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        clockView.setTextSize(18);
        clockView.setTextColor(COR_TEXTO);
        clockView.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        clockView.setPadding(0, pad, pad, 0);
        root.addView(clockView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));

        countView = new TextView(this);
        countView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        countView.setTextSize(13);
        countView.setTextColor(COR_DETALHE);
        countView.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        countView.setPadding(pad, 0, 0, pad);
        root.addView(countView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START));

        if (adapter.getCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Nenhum jogo.\n\nCopie seus ROMs (zip) em\nAndroid/data/com.edsonmfernando.advancemame/files/rom/");
            empty.setGravity(Gravity.CENTER);
            empty.setTextSize(16);
            empty.setTextColor(COR_NEIGHBOR);
            root.addView(empty, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }

        setContentView(root);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        list.post(new Runnable() {
            @Override
            public void run() {
                if (adapter.getCount() > 0) {
                    list.setSelection(0);
                    updateBackground(adapter.romAt(0));
                }
                updateCount(0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
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

    private void updateClock() {
        clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    }

    private void updateCount(int position) {
        if (adapter.getCount() > 0) {
            countView.setText((position + 1) + "/" + adapter.getCount());
        }
    }

    private void updateBackground(String rom) {
        updateCount(adapter.selected);
        Bitmap cover = adapter.loadCover(rom, Math.max(
                getResources().getDisplayMetrics().widthPixels / 2,
                getResources().getDisplayMetrics().heightPixels / 2));
        if (root.findViewWithTag("bg") != null) {
            root.removeView((View) root.findViewWithTag("bg"));
        }
        if (cover == null) {
            return;
        }
        ImageView bg = new ImageView(this);
        bg.setTag("bg");
        bg.setImageBitmap(cover);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAlpha(0.20f);
        root.addView(bg, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void launch(final String rom) {
        View wipe = new View(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(wipe, lp);
        wipe.setAlpha(0f);
        wipe.animate().alpha(1f).setDuration(WIPE_DELAY_MS).withEndAction(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(FrontendActivity.this, MainActivity.class).putExtra("rom", rom));
                overridePendingTransition(0, 0);
            }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private final class GameAdapter extends BaseAdapter {

        private final ArrayList<String> roms = new ArrayList<>();
        private final ArrayList<String> titles = new ArrayList<>();
        private int selected;

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
            TextView tv = new TextView(FrontendActivity.this);
            boolean sel = position == selected;
            tv.setText(titles.get(position));
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            tv.setTypeface(Typeface.create("sans-serif-condensed", sel ? Typeface.BOLD : Typeface.NORMAL));
            tv.setTextSize(sel ? 24 : 16);
            tv.setTextColor(sel ? COR_TEXTO : COR_NEIGHBOR);
            tv.setShadowLayer(dp(2), 0, dp(1), Color.BLACK);
            int pad = dp(sel ? 6 : 2);
            tv.setPadding(dp(24), pad, dp(24), pad);
            return tv;
        }

        private Bitmap loadCover(String rom, int maxPx) {
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
                    int bigger = Math.max(opts.outWidth, opts.outHeight);
                    opts.inSampleSize = bigger > maxPx ? bigger / maxPx : 1;
                    opts.inJustDecodeBounds = false;
                    return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
                } catch (Throwable t) {
                    return null;
                }
            }
            return null;
        }
    }

    private static String prettyName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String name = dot > 0 ? fileName.substring(0, dot) : fileName;
        return name.replace('_', ' ').toUpperCase();
    }
}
