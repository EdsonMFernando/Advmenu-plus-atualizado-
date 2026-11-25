package com.edsonmfernando.advancemame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
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
import java.util.HashMap;
import java.util.Locale;

/**
 * Front-end no estilo do ES-DE: carrossel de jogos, barra superior com relogio
 * e nome do sistema, contador no rodape. O snapshot do jogo selecionado aparece
 * como fundo: VIDEO em MP4/WebM/MKV (e AVI se o aparelho tiver decodificador —
 * se falhar, cai na imagem) com looping mutado, ou IMAGEM em JPG/PNG/WebP.
 *
 * Midias (estilo "media"/snaps do ES-DE), por jogo <rom>:
 *   Android/data/com.edsonmfernando.advancemame/files/media/&lt;rom&gt;.mp4|.webm|.mkv|.avi
 *   Android/data/com.edsonmfernando.advancemame/files/media/&lt;rom&gt;.jpg|.jpeg|.png|.webp|.gif
 */
public class FrontendActivity extends Activity {

    private static final int COR_FUNDO = Color.rgb(16, 24, 32);
    private static final int COR_TEXTO = Color.rgb(255, 255, 255);
    private static final int COR_NEIGHBOR = Color.rgb(120, 144, 156);
    private static final int COR_DETALHE = Color.rgb(138, 180, 200);

    private static final String[] VIDEO_EXTS = {".mp4", ".webm", ".mkv", ".avi"};
    private static final String[] IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".gif"};
    private static final long WIPE_DELAY_MS = 250;

    private FrameLayout root;
    private TextView clockView;
    private TextView countView;
    private ListView list;
    private GameAdapter adapter;
    private SnapshotHost snapshotHost;
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

        snapshotHost = new SnapshotHost(this);
        root.addView(snapshotHost, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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
                snapshotHost.show(adapter.romAt(position));
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
                    snapshotHost.show(adapter.romAt(0));
                }
                updateCount(0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.post(clockTick);
        snapshotHost.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
        snapshotHost.pause();
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

    private void launch(final String rom) {
        View wipe = new View(this);
        wipe.setBackgroundColor(Color.BLACK);
        root.addView(wipe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wipe.setAlpha(0f);
        wipe.animate().alpha(1f).setDuration(WIPE_DELAY_MS).withEndAction(new Runnable() {
            @Override
            public void run() {
                snapshotHost.pause();
                startActivity(new Intent(FrontendActivity.this, MainActivity.class).putExtra("rom", rom));
                overridePendingTransition(0, 0);
            }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private File mediaFile(String rom, String[] exts) {
        File dir = new File(getFilesDir(), "media");
        for (String ext : exts) {
            File f = new File(dir, rom + ext);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private Bitmap decodeCover(File f, int maxPx) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
            int bigger = Math.max(opts.outWidth, opts.outHeight);
            opts.inSampleSize = Math.max(1, bigger / Math.max(1, maxPx));
            opts.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), opts);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Quadro de fundo: mostra o VIDEO (looping, muted) ou a IMAGEM do jogo
     * selecionado; MediaPlayer e liberado em selecao/pausa.
     */
    private final class SnapshotHost extends FrameLayout implements TextureView.SurfaceTextureListener {

        private final TextureView texture;
        private final ImageView image;
        private final ImageView dim;
        private MediaPlayer player;
        private String currentRom;
        private File pendingVideo;
        private boolean resumed;

        SnapshotHost(Context ctx) {
            super(ctx);
            texture = new TextureView(ctx);
            texture.setSurfaceTextureListener(this);
            texture.setVisibility(GONE);
            image = new ImageView(ctx);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setVisibility(GONE);
            dim = new ImageView(ctx);
            dim.setBackgroundColor(Color.BLACK);
            dim.setAlpha(0.20f);
            dim.setVisibility(GONE);
            addView(texture, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addView(image, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addView(dim, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        void show(String rom) {
            currentRom = rom;
            clear(false);
            if (rom == null) {
                return;
            }
            File video = mediaFile(rom, VIDEO_EXTS);
            if (video != null) {
                playVideo(video);
            } else {
                showImage(mediaFile(rom, IMAGE_EXTS));
            }
            updateCount(adapter.selected);
        }

        private void playVideo(File video) {
            SurfaceTexture tex = texture.getSurfaceTexture();
            if (tex == null) {
                pendingVideo = video;
                texture.setVisibility(VISIBLE);
                return;
            }
            startPlayer(video, new Surface(tex));
        }

        private void startPlayer(File video, Surface surface) {
            try {
                player = new MediaPlayer();
                player.setDataSource(video.getAbsolutePath());
                player.setSurface(surface);
                player.setLooping(true);
                player.setVolume(0f, 0f);
                player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        dim.setVisibility(VISIBLE);
                        texture.setVisibility(VISIBLE);
                        if (resumed) {
                            mp.start();
                        }
                    }
                });
                player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        clear(false);
                        showImage(mediaFile(currentRom, IMAGE_EXTS));
                        return true;
                    }
                });
                player.prepareAsync();
            } catch (Throwable t) {
                clear(false);
                showImage(mediaFile(currentRom, IMAGE_EXTS));
            }
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (pendingVideo != null) {
                File video = pendingVideo;
                pendingVideo = null;
                startPlayer(video, new Surface(surface));
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            clear(false);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

        private void showImage(File img) {
            Bitmap bmp = img != null ? decodeCover(img, Math.max(
                    getResources().getDisplayMetrics().widthPixels / 2,
                    getResources().getDisplayMetrics().heightPixels / 2)) : null;
            if (bmp != null) {
                image.setImageBitmap(bmp);
                image.setVisibility(VISIBLE);
                dim.setVisibility(VISIBLE);
            }
        }

        private void clear(boolean forPause) {
            if (player != null) {
                try {
                    player.stop();
                } catch (Throwable ignored) {
                }
                player.release();
                player = null;
            }
            pendingVideo = null;
            texture.setVisibility(GONE);
            image.setVisibility(GONE);
            dim.setVisibility(GONE);
        }

        void pause() {
            resumed = false;
            if (player != null) {
                player.pause();
            }
        }

        void resume() {
            resumed = true;
            if (player != null) {
                player.start();
            }
        }
    }

    private final class GameAdapter extends BaseAdapter {

        private final ArrayList<String> roms = new ArrayList<>();
        private final ArrayList<String> titles = new ArrayList<>();
        private int selected;
        private final HashMap<String, Bitmap> thumbs = new HashMap<>();

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

        private Bitmap thumb(String rom, int sizePx) {
            Bitmap cached = thumbs.get(rom);
            if (cached != null) {
                return cached;
            }
            File img = mediaFile(rom, IMAGE_EXTS);
            if (img == null) {
                return null;
            }
            Bitmap bmp = decodeCover(img, sizePx * 2);
            if (bmp == null) {
                return null;
            }
            thumbs.put(rom, bmp);
            return bmp;
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
            row.setGravity(Gravity.CENTER);
            boolean sel = position == selected;
            int pad = dp(sel ? 6 : 2);
            row.setPadding(dp(16), pad, dp(16), pad);

            Bitmap cover = thumb(roms.get(position), dp(48));
            if (cover != null) {
                ImageView img = new ImageView(FrontendActivity.this);
                img.setImageBitmap(cover);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
                lp.rightMargin = dp(10);
                row.addView(img, lp);
            }

            TextView tv = new TextView(FrontendActivity.this);
            tv.setText(titles.get(position));
            tv.setSingleLine(true);
            tv.setTypeface(Typeface.create("sans-serif-condensed", sel ? Typeface.BOLD : Typeface.NORMAL));
            tv.setTextSize(sel ? 24 : 16);
            tv.setTextColor(sel ? COR_TEXTO : COR_NEIGHBOR);
            tv.setShadowLayer(dp(2), 0, dp(1), Color.BLACK);
            row.addView(tv, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            return row;
        }
    }

    private static String prettyName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String name = dot > 0 ? fileName.substring(0, dot) : fileName;
        return name.replace('_', ' ').toUpperCase();
    }
}
