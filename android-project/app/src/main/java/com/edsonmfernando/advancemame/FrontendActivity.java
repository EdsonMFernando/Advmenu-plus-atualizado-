package com.edsonmfernando.advancemame;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
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

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 *
 * Tambem aceita pastas na memoria interna ou no CARTAO DE MEMORIA (estilo
 * AdvMenu Plus). Em cada raiz de armazenamento (/storage/emulated/0, /storage/XXXX-XXXX):
 *   ROMs/  (ou roms/rom)      ROMs em .zip/.7z (com subpastas ate 2 niveis)
 *   snapshot/ (ou snap/snaps) snapshot avulso: &lt;rom&gt;.mp4|.avi|.mkv|.webm|.jpg|.png ...
 *   layout/  (ou layouts)     background.(jpg|png|...) usado como fundo do menu
 *                         e qualquer imagem solta na pasta
 * As pastas fora do app precisam da permissao "Acesso a todos os arquivos"
 * (pedida automaticamente na primeira abertura).
 */
public class FrontendActivity extends Activity {

    private static final int COR_FUNDO = Color.rgb(16, 24, 32);
    private static final int COR_TEXTO = Color.rgb(255, 255, 255);
    private static final int COR_NEIGHBOR = Color.rgb(120, 144, 156);
    private static final int COR_DETALHE = Color.rgb(138, 180, 200);

    private static final String[] VIDEO_EXTS = {".mp4", ".webm", ".mkv", ".avi"};
    private static final String[] IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".gif"};
    private static final String[] ROM_DIR_NAMES = {"roms", "rom"};
    private static final String[] SNAP_DIR_NAMES = {"snapshot", "snap", "snaps"};
    private static final String[] LAYOUT_DIR_NAMES = {"layout", "layouts"};
    private static final String[] EMU_DIR_NAMES = {"emuladores", "emulators"};
    private static final String[][] EMU_ALIASES = {
            {"com.winlator", "Winlator — jogos de PC (Windows)"},
            {"com.gamesir.gamehub", "GameHub — jogos de PC"},
            {"com.retroarch", "RetroArch — todos os sistemas"},
            {"dev.eden.eden_emulator", "Eden — Nintendo Switch"},
            {"net.rpcs3.rpcs3", "RPCS3 — PlayStation 3"},
            {"org.vita3k.emulator", "Vita3K — PS Vita"},
            {"org.openbor.engine", "OpenBOR — beat'em ups"},
            {"xyz.aethersx2.android", "AetherSX2 — PlayStation 2"},
            {"org.ppsspp.ppsspp", "PPSSPP — PSP"},
            {"org.dolphinemu.dolphinemu", "Dolphin — GameCube/Wii"},
    };

    /** Pastas de sistema no armazenamento (nome da pasta, no cartao) -> nome bonito. */
    private static final LinkedHashMap<String, String> SYSTEM_FOLDERS = new LinkedHashMap<>();
    static {
        SYSTEM_FOLDERS.put("atari2600", "Atari 2600");
        SYSTEM_FOLDERS.put("a2600", "Atari 2600");
        SYSTEM_FOLDERS.put("atari5200", "Atari 5200");
        SYSTEM_FOLDERS.put("atari7800", "Atari 7800");
        SYSTEM_FOLDERS.put("atari8bit", "Atari 8-bit");
        SYSTEM_FOLDERS.put("atarilynx", "Atari Lynx");
        SYSTEM_FOLDERS.put("atarijaguar", "Atari Jaguar");
        SYSTEM_FOLDERS.put("colecovision", "ColecoVision");
        SYSTEM_FOLDERS.put("intellivision", "Intellivision");
        SYSTEM_FOLDERS.put("vectrex", "Vectrex");
        SYSTEM_FOLDERS.put("mastersystem", "Master System");
        SYSTEM_FOLDERS.put("sms", "Master System");
        SYSTEM_FOLDERS.put("megadrive", "Mega Drive");
        SYSTEM_FOLDERS.put("genesis", "Mega Drive (Genesis)");
        SYSTEM_FOLDERS.put("sega32x", "Mega Drive 32X");
        SYSTEM_FOLDERS.put("segacd", "Sega CD");
        SYSTEM_FOLDERS.put("gamegear", "Game Gear");
        SYSTEM_FOLDERS.put("nes", "Nintendo NES");
        SYSTEM_FOLDERS.put("famicom", "Nintendo Famicom");
        SYSTEM_FOLDERS.put("snes", "Super Nintendo");
        SYSTEM_FOLDERS.put("n64", "Nintendo 64");
        SYSTEM_FOLDERS.put("gameboy", "Game Boy");
        SYSTEM_FOLDERS.put("gb", "Game Boy");
        SYSTEM_FOLDERS.put("gbc", "Game Boy Color");
        SYSTEM_FOLDERS.put("gba", "Game Boy Advance");
        SYSTEM_FOLDERS.put("nds", "Nintendo DS");
        SYSTEM_FOLDERS.put("3ds", "Nintendo 3DS");
        SYSTEM_FOLDERS.put("psx", "PlayStation 1");
        SYSTEM_FOLDERS.put("ps1", "PlayStation 1");
        SYSTEM_FOLDERS.put("ps2", "PlayStation 2");
        SYSTEM_FOLDERS.put("psp", "PSP");
        SYSTEM_FOLDERS.put("psvita", "PS Vita");
        SYSTEM_FOLDERS.put("vita", "PS Vita");
        SYSTEM_FOLDERS.put("dreamcast", "Dreamcast");
        SYSTEM_FOLDERS.put("saturn", "Sega Saturn");
        SYSTEM_FOLDERS.put("neogeo", "Neo Geo");
        SYSTEM_FOLDERS.put("neogeopocket", "Neo Geo Pocket");
        SYSTEM_FOLDERS.put("wonderswan", "WonderSwan");
        SYSTEM_FOLDERS.put("wonderswancolor", "WonderSwan Color");
        SYSTEM_FOLDERS.put("switch", "Nintendo Switch");
        SYSTEM_FOLDERS.put("switch2", "Nintendo Switch 2");
        SYSTEM_FOLDERS.put("gamecube", "Nintendo GameCube");
        SYSTEM_FOLDERS.put("gc", "Nintendo GameCube");
        SYSTEM_FOLDERS.put("wii", "Nintendo Wii");
        SYSTEM_FOLDERS.put("wiiu", "Nintendo Wii U");
        SYSTEM_FOLDERS.put("virtualboy", "Virtual Boy");
        SYSTEM_FOLDERS.put("fds", "Famicom Disk System");
        SYSTEM_FOLDERS.put("ps3", "PlayStation 3");
        SYSTEM_FOLDERS.put("ps4", "PlayStation 4");
        SYSTEM_FOLDERS.put("ps5", "PlayStation 5");
        SYSTEM_FOLDERS.put("xbox", "Xbox");
        SYSTEM_FOLDERS.put("xbox360", "Xbox 360");
        SYSTEM_FOLDERS.put("xbla", "Xbox Live Arcade");
        SYSTEM_FOLDERS.put("dreamcast", "Dreamcast");
        SYSTEM_FOLDERS.put("saturn", "Sega Saturn");
        SYSTEM_FOLDERS.put("sg1000", "SG-1000");
        SYSTEM_FOLDERS.put("pcengine", "PC Engine (TurboGrafx-16)");
        SYSTEM_FOLDERS.put("turbografx", "TurboGrafx-16");
        SYSTEM_FOLDERS.put("turbografx16", "TurboGrafx-16");
        SYSTEM_FOLDERS.put("supergrafx", "PC Engine SuperGrafx");
        SYSTEM_FOLDERS.put("pcfx", "PC-FX");
        SYSTEM_FOLDERS.put("neogeocd", "Neo Geo CD");
        SYSTEM_FOLDERS.put("neogeopocketcolor", "Neo Geo Pocket Color");
        SYSTEM_FOLDERS.put("ngpc", "Neo Geo Pocket Color");
        SYSTEM_FOLDERS.put("3do", "3DO");
        SYSTEM_FOLDERS.put("megaduck", "Mega Duck");
        SYSTEM_FOLDERS.put("supervision", "Watara Supervision");
        SYSTEM_FOLDERS.put("gamate", "Gamate");
        SYSTEM_FOLDERS.put("pv1000", "Casio PV-1000");
        SYSTEM_FOLDERS.put("astrocade", "Bally Astrocade");
        SYSTEM_FOLDERS.put("openbor", "OpenBOR");
        SYSTEM_FOLDERS.put("mugen", "MUGEN");
        SYSTEM_FOLDERS.put("teknoparrot", "TeknoParrot (arcade 3D)");
        SYSTEM_FOLDERS.put("taitotypex", "Arcade Taito Type X");
        SYSTEM_FOLDERS.put("typex", "Arcade Taito Type X");
        SYSTEM_FOLDERS.put("hbmame", "Arcade HBMAME (homebrew/hacks)");
        SYSTEM_FOLDERS.put("fba", "Arcade FinalBurn Neo");
        SYSTEM_FOLDERS.put("fbneo", "Arcade FinalBurn Neo");
        SYSTEM_FOLDERS.put("finalburn", "Arcade FinalBurn Neo");
        SYSTEM_FOLDERS.put("mame", "Arcade MAME");
        SYSTEM_FOLDERS.put("cps1", "Arcade Capcom CPS-1");
        SYSTEM_FOLDERS.put("cps2", "Arcade Capcom CPS-2");
        SYSTEM_FOLDERS.put("cps3", "Arcade Capcom CPS-3");
        SYSTEM_FOLDERS.put("naomi", "Arcade Sega Naomi");
        SYSTEM_FOLDERS.put("naomi2", "Arcade Sega Naomi 2");
        SYSTEM_FOLDERS.put("atomiswave", "Arcade Atomiswave");
        SYSTEM_FOLDERS.put("stv", "Arcade Sega ST-V");
        SYSTEM_FOLDERS.put("lindbergh", "Arcade Sega Lindbergh");
        SYSTEM_FOLDERS.put("daphne", "Arcade LaserDisc (Daphne)");
        SYSTEM_FOLDERS.put("pinball", "Pinball (Future/Visual Pinball)");
        SYSTEM_FOLDERS.put("arcade", "Arcade");
        SYSTEM_FOLDERS.put("amiga", "Commodore Amiga");
        SYSTEM_FOLDERS.put("amigacd32", "Amiga CD32");
        SYSTEM_FOLDERS.put("c64", "Commodore 64");
        SYSTEM_FOLDERS.put("c128", "Commodore 128");
        SYSTEM_FOLDERS.put("vic20", "Commodore VIC-20");
        SYSTEM_FOLDERS.put("amstradcpc", "Amstrad CPC");
        SYSTEM_FOLDERS.put("cpc", "Amstrad CPC");
        SYSTEM_FOLDERS.put("msx", "MSX");
        SYSTEM_FOLDERS.put("msx2", "MSX 2");
        SYSTEM_FOLDERS.put("spectrum", "ZX Spectrum");
        SYSTEM_FOLDERS.put("zxspectrum", "ZX Spectrum");
        SYSTEM_FOLDERS.put("zx81", "Sinclair ZX-81");
        SYSTEM_FOLDERS.put("dos", "PC (DOS)");
        SYSTEM_FOLDERS.put("pcdos", "PC (DOS)");
        SYSTEM_FOLDERS.put("macintosh", "Macintosh");
        SYSTEM_FOLDERS.put("apple2", "Apple II");
        SYSTEM_FOLDERS.put("atarist", "Atari ST");
        SYSTEM_FOLDERS.put("x68000", "Sharp X68000");
        SYSTEM_FOLDERS.put("pc88", "NEC PC-8801");
        SYSTEM_FOLDERS.put("pc98", "NEC PC-9801");
        SYSTEM_FOLDERS.put("scummvm", "ScummVM");
        SYSTEM_FOLDERS.put("pc", "PC (Windows)");
        SYSTEM_FOLDERS.put("windows", "PC (Windows)");
        SYSTEM_FOLDERS.put("winlator", "PC Windows (Winlator)");
    }

    /**
     * Rota do sistema -> app Android preferido (slash: MUGEN/TeknoParrot/Taito Type X e
     * qualquer jogo de Windows vao pelo Winlator; consoles usam o emulador nativo; se o
     * app preferido nao estiver instalado o jogo abre pelo escolhedor normal do Android).
     */
    private static final HashMap<String, String> SYSTEM_PACKAGES = new HashMap<>();
    static {
        SYSTEM_PACKAGES.put("psp", "org.ppsspp.ppsspp");
        SYSTEM_PACKAGES.put("ps2", "xyz.aethersx2.android");
        SYSTEM_PACKAGES.put("ps3", "net.rpcs3.rpcs3");
        SYSTEM_PACKAGES.put("psvita", "org.vita3k.emulator");
        SYSTEM_PACKAGES.put("vita", "org.vita3k.emulator");
        SYSTEM_PACKAGES.put("switch", "dev.eden.eden_emulator");
        SYSTEM_PACKAGES.put("gamecube", "org.dolphinemu.dolphinemu");
        SYSTEM_PACKAGES.put("gc", "org.dolphinemu.dolphinemu");
        SYSTEM_PACKAGES.put("wii", "org.dolphinemu.dolphinemu");
        SYSTEM_PACKAGES.put("openbor", "org.openbor.engine");
        SYSTEM_PACKAGES.put("pc", "com.winlator");
        SYSTEM_PACKAGES.put("windows", "com.winlator");
        SYSTEM_PACKAGES.put("winlator", "com.winlator");
        SYSTEM_PACKAGES.put("mugen", "com.winlator");
        SYSTEM_PACKAGES.put("teknoparrot", "com.winlator");
        SYSTEM_PACKAGES.put("taitotypex", "com.winlator");
        SYSTEM_PACKAGES.put("typex", "com.winlator");
        SYSTEM_PACKAGES.put("lindbergh", "com.winlator");
        SYSTEM_PACKAGES.put("dos", "com.winlator");
        SYSTEM_PACKAGES.put("pcdos", "com.winlator");
    }

    private static final String[] GAME_FILE_EXTS = {
            ".zip", ".7z", ".rom", ".bin", ".iso", ".img", ".cue", ".chd", ".cso",
            ".nsp", ".xci", ".nca", ".pak", ".a26", ".a78", ".lnx", ".jag", ".j64",
            ".nes", ".fds", ".sfc", ".smc", ".gb", ".gbc", ".gba", ".nds", ".3ds",
            ".cia", ".v64", ".z64", ".n64", ".md", ".sms", ".gg", ".32x", ".pbp",
            ".vec", ".col", ".int", ".ws", ".wsc", ".ngp", ".ngc", ".dsk", ".cas",
            ".atr", ".exe",
            ".pce", ".sg", ".vpk", ".xex", ".wad", ".wbfs",
            ".gcm", ".rvz", ".adf", ".d64", ".t64", ".tap", ".tzx", ".m3u", ".prg",
            ".rar", ".cab", ".gcz", ".ciso", ".dol", ".elf", ".mdf",
    };
    private static final long WIPE_DELAY_MS = 250;

    private ArrayList<File> romDirs = new ArrayList<>();
    private ArrayList<File> snapDirs = new ArrayList<>();
    private ArrayList<File> emuDirs = new ArrayList<>();
    private ArrayList<SystemEntry> systems = new ArrayList<>();
    private File layoutDir;
    private ImageView layoutBg;
    private TextView emptyView;
    private TextView emuButton;
    private TextView padToggle;
    private View padView;
    private boolean storageGranted;

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

        computeMediaDirs();
        storageGranted = hasStoragePermission();

        adapter = new GameAdapter();
        adapter.load(this);

        applyLayoutBackground();

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

        TextView padBtn = new TextView(this);
        padBtn.setText("CONTROLE");
        padBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        padBtn.setTextSize(14);
        padBtn.setTextColor(COR_DETALHE);
        padBtn.setPadding(dp(6), 0, 0, dp(14));
        padBtn.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        padBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePad();
            }
        });
        padToggle = padBtn;

        updateEmptyState();

        setContentView(root);

        SharedPreferences prefs = getSharedPreferences("advmenu", Context.MODE_PRIVATE);
        boolean padOn = prefs.getBoolean("pad", false);
        padView = buildPadView();
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        plp.bottomMargin = dp(84);
        root.addView(padView, plp);
        padView.setVisibility(padOn ? View.VISIBLE : View.GONE);
        padToggle.setAlpha(padOn ? 1f : 0.45f);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        tlp.bottomMargin = dp(40);
        root.addView(padToggle, tlp);

        if (!storageGranted) {
            requestStoragePermission();
        }

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
        boolean granted = hasStoragePermission();
        if (granted != storageGranted) {
            storageGranted = granted;
            reloadGames();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean granted = hasStoragePermission();
        if (granted != storageGranted) {
            storageGranted = granted;
            reloadGames();
        }
    }

    /** Recarrega ROMs/snapshots/layout/emuladores depois da permissao de armazenamento. */
    private void reloadGames() {
        computeMediaDirs();
        applyLayoutBackground();
        applyEmulatorsButton();
        adapter.load(this);
        if (adapter.selected >= adapter.getCount()) {
            adapter.selected = 0;
        }
        list.invalidateViews();
        updateEmptyState();
        if (adapter.getCount() > 0) {
            list.setSelection(adapter.selected);
            snapshotHost.show(adapter.romAt(adapter.selected));
        } else {
            snapshotHost.show(null);
        }
    }

    /** Botao canto inferior-direito: EMULADORES (apps de PC instalados + APKs da pasta emuladores). */
    private void applyEmulatorsButton() {
        if (emuButton != null) {
            root.removeView(emuButton);
            emuButton = null;
        }
        TextView btn = new TextView(this);
        btn.setText("EMULADORES");
        btn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        btn.setTextSize(14);
        btn.setTextColor(COR_DETALHE);
        btn.setPadding(0, 0, dp(14), dp(14));
        btn.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEmulatorsList();
            }
        });
        root.addView(btn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END));
        emuButton = btn;
    }

    /** D-pad + A/B na tela, dispara as mesmas teclas de um gamepad fisico no menu. */
    private View buildPadView() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(6), dp(6), dp(6));
        row.setBackgroundColor(Color.argb(110, 20, 30, 42));

        LinearLayout cross = new LinearLayout(this);
        cross.setOrientation(LinearLayout.VERTICAL);
        cross.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.HORIZONTAL);
        cross.addView(padBtn("^", KeyEvent.KEYCODE_DPAD_UP));
        mid.addView(padBtn("<", KeyEvent.KEYCODE_DPAD_LEFT));
        mid.addView(padBtn(">", KeyEvent.KEYCODE_DPAD_RIGHT));
        cross.addView(mid);
        cross.addView(padBtn("v", KeyEvent.KEYCODE_DPAD_DOWN));
        row.addView(cross);

        TextView a = padBtn("A", KeyEvent.KEYCODE_DPAD_CENTER);
        TextView b = padBtn("B", KeyEvent.KEYCODE_BACK);
        LinearLayout ab = new LinearLayout(this);
        ab.setOrientation(LinearLayout.VERTICAL);
        ab.setGravity(Gravity.CENTER);
        ab.addView(a);
        ab.addView(b);
        row.addView(ab);
        return row;
    }

    private TextView padBtn(String label, final int keyCode) {
        final TextView b = new TextView(this);
        b.setText(label);
        b.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        b.setTextSize(18);
        b.setTextColor(COR_TEXTO);
        b.setGravity(Gravity.CENTER);
        int s = dp(46);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        b.setLayoutParams(lp);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendKey(keyCode);
            }
        });
        return b;
    }

    private void sendKey(int code) {
        long now = SystemClock.uptimeMillis();
        list.dispatchKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, 0));
        list.dispatchKeyEvent(new KeyEvent(now, now + 40, KeyEvent.ACTION_UP, code, 0, 0));
    }

    private void togglePad() {
        if (padView == null || padToggle == null) {
            return;
        }
        boolean visible = padView.getVisibility() != View.VISIBLE;
        padView.setVisibility(visible ? View.VISIBLE : View.GONE);
        padToggle.setAlpha(visible ? 1f : 0.45f);
        getSharedPreferences("advmenu", Context.MODE_PRIVATE)
                .edit().putBoolean("pad", visible).apply();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (list == null) {
            return super.onKeyDown(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_ENTER) {
            sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showEmulatorsList() {
        ArrayList<String> labels = new ArrayList<>();
        final ArrayList<String> pkgs = new ArrayList<>();
        final ArrayList<File> apks = new ArrayList<>();
        final ArrayList<SystemEntry> sysHits = new ArrayList<>();
        int gameTotal = 0;
        for (SystemEntry e : systems) {
            if (!e.games.isEmpty()) {
                sysHits.add(e);
                gameTotal += e.games.size();
                labels.add(e.label + "  (" + e.games.size() + ")");
                pkgs.add(null);
                apks.add(null);
            }
        }
        if (!sysHits.isEmpty()) {
            labels.add("— APPS INSTALADOS —");
            pkgs.add("");
            apks.add(null);
        }
        PackageManager pm = getPackageManager();
        for (String[] alias : EMU_ALIASES) {
            if (pm.getLaunchIntentForPackage(alias[0]) != null && !pkgs.contains(alias[0])) {
                pkgs.add(alias[0]);
                apks.add(null);
                labels.add("APP: " + alias[1]);
            }
        }
        HashSet<String> seenPkgs = new HashSet<>(pkgs);
        for (File dir : emuDirs) {
            File[] files = dir.listFiles();
            if (files == null) {
                continue;
            }
            for (File f : files) {
                if (!f.isFile() || !f.getName().toLowerCase(Locale.US).endsWith(".apk")) {
                    continue;
                }
                android.content.pm.PackageInfo pi =
                        pm.getPackageArchiveInfo(f.getAbsolutePath(), 0);
                String pkg = pi != null && pi.packageName != null
                        ? pi.packageName : "apk:" + f.getName();
                if (seenPkgs.contains(pkg)) {
                    continue;
                }
                seenPkgs.add(pkg);
                boolean installed = pm.getLaunchIntentForPackage(pkg) != null;
                pkgs.add(pkg);
                apks.add(installed ? null : f);
                labels.add((installed ? "\u25b6 " : "[instalar] ")
                        + f.getName().replaceFirst("(?i)\\.apk$", ""));
            }
        }
        if (labels.isEmpty()) {
            labels.add("Pasta emuladores vazia (copie Winlator/GameHub .apk no cartao)");
            pkgs.add(null);
            apks.add(null);
            sysHits.add(null);
        }
        String title = gameTotal > 0
                ? "SISTEMAS + EMULADORES  (" + gameTotal + " jogos)"
                : "EMULADORES / PC";
        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels.toArray(new String[0]),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                String pkg = pkgs.get(which);
                                if ("".equals(pkg)) {
                                    return;
                                }
                                if (pkg == null && apks.get(which) == null && which < sysHits.size()
                                        && sysHits.get(which) != null) {
                                    showSystemGames(sysHits.get(which));
                                    return;
                                }
                                launchEmulator(pkg, apks.get(which));
                            }
                        })
                .show();
    }

    private void launchEmulator(String pkg, File apk) {
        if (pkg != null && !pkg.startsWith("apk:")) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                startActivity(launch);
                return;
            }
        }
        if (apk != null) {
            android.widget.Toast.makeText(this,
                    "Instale o APK manualmente: " + apk.getAbsolutePath(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        if (Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                startActivity(new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Throwable e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (Throwable ignored) {
                }
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void updateEmptyState() {
        if (emptyView != null) {
            root.removeView(emptyView);
            emptyView = null;
        }
        if (adapter.getCount() == 0) {
            emptyView = new TextView(this);
            emptyView.setText("Nenhum jogo.\n\nColoque as ROMs (zip/7z) no cartao de memoria em\n/sdcard/ROMs\nou em Android/data/com.edsonmfernando.advancemame/files/rom/");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setTextSize(16);
            emptyView.setTextColor(COR_NEIGHBOR);
            root.addView(emptyView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        }
    }

    /** Fundo do menu da pasta layout: background.(jpg|png|...) ou imagem solta. */
    private void applyLayoutBackground() {
        if (layoutBg != null) {
            root.removeView(layoutBg);
            layoutBg = null;
        }
        if (layoutDir == null) {
            return;
        }
        File found = null;
        for (String ext : IMAGE_EXTS) {
            File f = new File(layoutDir, "background" + ext);
            if (f.isFile()) {
                found = f;
                break;
            }
        }
        if (found == null) {
            File[] files = layoutDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    String n = f.getName().toLowerCase(Locale.US);
                    for (String ext : IMAGE_EXTS) {
                        if (f.isFile() && n.endsWith(ext)) {
                            found = f;
                            break;
                        }
                    }
                    if (found != null) {
                        break;
                    }
                }
            }
        }
        if (found == null) {
            return;
        }
        Bitmap bmp = decodeCover(found, Math.max(
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels));
        if (bmp == null) {
            return;
        }
        ImageView bg = new ImageView(this);
        bg.setImageBitmap(bmp);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bg.setAlpha(0.55f);
        layoutBg = bg;
        root.addView(bg, 1, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** Raizes de armazenamento: memoria interna emulada + cartao de memoria fisico. */
    private ArrayList<File> storageRoots() {
        return AssetInstaller.storageRoots(this);
    }

    /**
     * Diretorios de jogo/midia: internos primeiro (rom/, media/) e depois as
     * pastas ROMs/, snapshot/ e layout/ de cada armazenamento.
     */
    private void computeMediaDirs() {
        romDirs = new ArrayList<>();
        romDirs.add(new File(getFilesDir(), "rom"));
        snapDirs = new ArrayList<>();
        snapDirs.add(new File(getFilesDir(), "media"));
        layoutDir = null;
        emuDirs = new ArrayList<>();
        ArrayList<File> roots = storageRoots();
        systems = collectSystems(roots);
        addNamedDirs(romDirs, roots, ROM_DIR_NAMES);
        addNamedDirs(snapDirs, roots, SNAP_DIR_NAMES);
        addNamedDirs(emuDirs, roots, EMU_DIR_NAMES);
        ArrayList<File> layouts = new ArrayList<>();
        addNamedDirs(layouts, roots, LAYOUT_DIR_NAMES);
        if (!layouts.isEmpty()) {
            layoutDir = layouts.get(0);
        }
    }

    /** Pastas de sistema (atari2600, nes, snes, ps1, psp, switch, hbmame, fba...) em cada armazenamento. */
    private ArrayList<SystemEntry> collectSystems(ArrayList<File> roots) {
        LinkedHashMap<String, SystemEntry> byLabel = new LinkedHashMap<>();
        for (File rootDir : roots) {
            File[] children = rootDir.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (!child.isDirectory()) {
                    continue;
                }
                String n = child.getName().toLowerCase(Locale.US);
                String label = SYSTEM_FOLDERS.get(n);
                if (label == null) {
                    continue;
                }
                SystemEntry entry = byLabel.get(label);
                if (entry == null) {
                    entry = new SystemEntry(n, label);
                    byLabel.put(label, entry);
                }
                scanGameDir(child, entry.games, 1);
            }
        }
        ArrayList<SystemEntry> out = new ArrayList<>(byLabel.values());
        for (SystemEntry e : out) {
            Collections.sort(e.games, new java.util.Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
        }
        return out;
    }

    private void scanGameDir(File dir, ArrayList<File> out, int depth) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.US);
            if (f.isFile() && isGameFile(n)) {
                out.add(f);
            } else if (f.isDirectory() && depth > 0) {
                scanGameDir(f, out, depth - 1);
            }
        }
    }

    private static boolean isGameFile(String nameLower) {
        for (String ext : GAME_FILE_EXTS) {
            if (nameLower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static String mimeFor(File f) {
        String n = f.getName().toLowerCase(Locale.US);
        if (n.endsWith(".zip")) {
            return "application/zip";
        }
        if (n.endsWith(".7z")) {
            return "application/x-7z-compressed";
        }
        if (n.endsWith(".iso")) {
            return "application/x-iso9660-image";
        }
        if (n.endsWith(".rar")) {
            return "application/x-rar-compressed";
        }
        return "application/octet-stream";
    }

    /** Abre o jogo no emulador do sistema (app preferido) ou, se nao houver, no registrado para o tipo de arquivo. */
    private void openGameFile(File f, String sysKey) {
        String pkg = sysKey == null ? null : SYSTEM_PACKAGES.get(sysKey);
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeFor(f));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (pkg != null && getPackageManager().getLaunchIntentForPackage(pkg) != null) {
                intent.setPackage(pkg);
            }
            startActivity(intent);
        } catch (Throwable t) {
            android.widget.Toast.makeText(this,
                    "Nenhum emulador abriu " + f.getName()
                            + ". Instale o app do sistema (EMULADORES) ou RetroArch.",
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void showSystemGames(final SystemEntry system) {
        ArrayList<String> names = new ArrayList<>();
        for (File g : system.games) {
            String n = g.getName();
            int dot = n.lastIndexOf('.');
            names.add(dot > 0 ? n.substring(0, dot) : n);
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(system.label + " (" + names.size() + " jogos)")
                .setItems(names.toArray(new String[0]),
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                openGameFile(system.games.get(which), system.key);
                            }
                        })
                .show();
    }

    private static final class SystemEntry {
        final String key;
        final String label;
        final ArrayList<File> games = new ArrayList<>();

        SystemEntry(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    private void addNamedDirs(ArrayList<File> out, ArrayList<File> roots, String[] names) {
        for (File rootDir : roots) {
            File[] children = rootDir.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (!child.isDirectory() || out.contains(child)) {
                    continue;
                }
                String n = child.getName().toLowerCase(Locale.US);
                for (String want : names) {
                    if (n.equals(want)) {
                        out.add(child);
                        break;
                    }
                }
            }
        }
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
                Intent intent = new Intent(FrontendActivity.this, MainActivity.class)
                        .putExtra("rom", rom);
                ArrayList<String> paths = new ArrayList<>();
                for (File d : romDirs) {
                    paths.add(d.getAbsolutePath());
                }
                intent.putStringArrayListExtra("romDirs", paths);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private File mediaFile(String rom, String[] exts) {
        for (File dir : snapDirs) {
            for (String ext : exts) {
                File f = new File(dir, rom + ext);
                if (f.isFile()) {
                    return f;
                }
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
            roms.clear();
            titles.clear();
            HashSet<String> seen = new HashSet<>();
            for (File dir : act.romDirs) {
                scanRomDir(dir, seen, 2);
            }
            Collections.sort(roms);
            for (String rom : roms) {
                titles.add(prettyName(rom));
            }
        }

        private void scanRomDir(File dir, HashSet<String> seen, int depth) {
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                String n = f.getName().toLowerCase(Locale.US);
                if (f.isFile() && (n.endsWith(".zip") || n.endsWith(".7z"))) {
                    String rom = n.substring(0, n.lastIndexOf('.'));
                    if (seen.add(rom)) {
                        roms.add(rom);
                    }
                } else if (f.isDirectory() && depth > 0) {
                    scanRomDir(f, seen, depth - 1);
                }
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
