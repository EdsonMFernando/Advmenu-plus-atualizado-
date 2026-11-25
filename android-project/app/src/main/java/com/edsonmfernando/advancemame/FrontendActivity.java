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
import android.view.MotionEvent;
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
import android.widget.ScrollView;
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

    private static int COR_FUNDO = Color.rgb(16, 24, 32);
    private static int COR_TEXTO = Color.rgb(255, 255, 255);
    private static int COR_NEIGHBOR = Color.rgb(120, 144, 156);
    private static int COR_DETALHE = Color.rgb(138, 180, 200);

    private static final int COR_FUNDO_PADRAO = Color.rgb(16, 24, 32);
    private static final int COR_TEXTO_PADRAO = Color.rgb(255, 255, 255);
    private static final int COR_NEIGHBOR_PADRAO = Color.rgb(120, 144, 156);
    private static final int COR_DETALHE_PADRAO = Color.rgb(138, 180, 200);

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
            {"org.vpinball.VPinballX", "Visual Pinball X — pinball"},
            {"org.vpinball.app.gl", "Visual Pinball X Android — pinball"},
            {"com.retroeki.pinball", "Pocket Pinball (VPX Standalone) — pinball"},
            {"com.zenstudios.PinballFx", "Pinball FX (Zen) — pinball"},
            {"com.zenstudios.ZenPinballWorld", "Pinball FX Android (Zen) — pinball"},
            {"com.farsight.PinballArcade", "Pinball Arcade (Farsight)"},
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
        SYSTEM_FOLDERS.put("pinball", "Pinball (Visual/Future Pinball)");
        SYSTEM_FOLDERS.put("pinballfx", "Pinball FX");
        SYSTEM_FOLDERS.put("pinballfx2", "Pinball FX 2");
        SYSTEM_FOLDERS.put("pinballfx3", "Pinball FX 3");
        SYSTEM_FOLDERS.put("pinballarcade", "Pinball Arcade");
        SYSTEM_FOLDERS.put("futurepinball", "Future Pinball (antes Winlator)");
        SYSTEM_FOLDERS.put("arcade", "Arcade");
        SYSTEM_FOLDERS.put("nesicaxlive", "Nesica X Live (arcade SEGA)");
        SYSTEM_FOLDERS.put("nesica", "Nesica X Live (arcade SEGA)");
        SYSTEM_FOLDERS.put("coinops", "CoinOps (multijogos de PC)");
        SYSTEM_FOLDERS.put("retroarch", "RetroArch (multi)");
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
        SYSTEM_PACKAGES.put("pinball", "org.vpinball.VPinballX");
        SYSTEM_PACKAGES.put("pinballfx", "com.zenstudios.PinballFx");
        SYSTEM_PACKAGES.put("pinballfx2", "com.zenstudios.zenpinball2");
        SYSTEM_PACKAGES.put("pinballfx3", "com.zenstudios.PinballFX3");
        SYSTEM_PACKAGES.put("pinballarcade", "com.farsight.PinballArcade");
        SYSTEM_PACKAGES.put("futurepinball", "com.winlator");
        SYSTEM_PACKAGES.put("pc", "com.winlator");
        SYSTEM_PACKAGES.put("windows", "com.winlator");
        SYSTEM_PACKAGES.put("winlator", "com.winlator");
        SYSTEM_PACKAGES.put("mugen", "com.winlator");
        SYSTEM_PACKAGES.put("teknoparrot", "com.winlator");
        SYSTEM_PACKAGES.put("taitotypex", "com.winlator");
        SYSTEM_PACKAGES.put("typex", "com.winlator");
        SYSTEM_PACKAGES.put("lindbergh", "com.winlator");
        SYSTEM_PACKAGES.put("nesicaxlive", "com.winlator");
        SYSTEM_PACKAGES.put("nesica", "com.winlator");
        SYSTEM_PACKAGES.put("coinops", "com.winlator");
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
            ".vpx", ".vpt", ".fpt", ".b2s",
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
    private TextView topTitle;
    private TextView topSystem;
    private View padView;
    private boolean storageGranted;
    private LinkedHashMap<String, File> themeDirs = new LinkedHashMap<>();
    private String activeTheme;
    private boolean systemsMode;
    private boolean systemGamesMode;
    private boolean showFavs;
    private SystemEntry currentSystem;
    private TextView favToggle;
    private TextView sysToggle;
    private TextView gearBtn;
    private android.hardware.input.InputManager.InputDeviceListener deviceListener;

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

        String savedTheme = getSharedPreferences("advmenu", Context.MODE_PRIVATE)
                .getString("theme", null);
        activeTheme = savedTheme == null ? "NATIVO: adv-multi"
                : (savedTheme.isEmpty() ? null : savedTheme);

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
                if (systemsMode) {
                    openSystemAt(position);
                } else if (systemGamesMode) {
                    openGameFile(adapter.fileAt(position), currentSystem.key);
                } else {
                    launch(adapter.romAt(position));
                }
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (systemsMode || systemGamesMode) {
                    return false;
                }
                showGameMenu(position);
                return true;
            }
        });
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.selected = position;
                list.invalidateViews();
                snapshotHost.show(systemsMode ? null : adapter.romAt(position));
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
        topTitle = title;

        TextView system = new TextView(this);
        system.setText("ARCADE  •  ADVANCEMAME");
        system.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        system.setTextSize(13);
        system.setTextColor(COR_TEXTO);
        system.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        topbar.addView(system);
        topSystem = system;

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

        TextView themeBtn = new TextView(this);
        themeBtn.setText("TEMA");
        themeBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        themeBtn.setTextSize(14);
        themeBtn.setTextColor(COR_DETALHE);
        themeBtn.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        themeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemePicker();
            }
        });
        root.addView(themeBtn, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));

        TextView sysBtn = new TextView(this);
        sysBtn.setText("SISTEMAS");
        sysBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        sysBtn.setTextSize(14);
        sysBtn.setTextColor(COR_DETALHE);
        sysBtn.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        sysBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSystemsView();
            }
        });
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        slp.rightMargin = dp(4);
        slp.bottomMargin = dp(40);
        root.addView(sysBtn, slp);
        sysToggle = sysBtn;
        sysToggle.setAlpha(0.45f);

        TextView favBtn = new TextView(this);
        favBtn.setText("FAVORITOS");
        favBtn.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        favBtn.setTextSize(14);
        favBtn.setTextColor(COR_DETALHE);
        favBtn.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        favBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFavs();
            }
        });
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        flp.rightMargin = dp(4);
        flp.bottomMargin = dp(76);
        root.addView(favBtn, flp);
        favToggle = favBtn;

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
        padBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showKeyMapDialog();
                return true;
            }
        });
        padToggle = padBtn;

        updateEmptyState();

        setContentView(root);

        SharedPreferences prefs = getSharedPreferences("advmenu", Context.MODE_PRIVATE);
        boolean padOn = prefs.getBoolean("pad", false);
        loadKeyMap();
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

        TextView gear = new TextView(this);
        gear.setText("\u2699");
        gear.setTextSize(16);
        gear.setTextColor(COR_DETALHE);
        gear.setAlpha(0.6f);
        gear.setShadowLayer(dp(2), 0, 0, Color.BLACK);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsMenu();
            }
        });
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        glp.leftMargin = dp(8);
        glp.topMargin = dp(6);
        root.addView(gear, glp);
        gearBtn = gear;

        android.hardware.input.InputManager im =
                (android.hardware.input.InputManager) getSystemService(Context.INPUT_SERVICE);
        if (im != null) {
            deviceListener = new android.hardware.input.InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                    updateControllerUi();
                }

                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    updateControllerUi();
                }

                @Override
                public void onInputDeviceChanged(int deviceId) {
                    updateControllerUi();
                }
            };
            im.registerInputDeviceListener(deviceListener, null);
        }

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
        updateControllerUi();
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

    /** D-pad vermelho com centro redondo (esquerda) + botoes redondos azuis (direita). */
    private View buildPadView() {
        FrameLayout overlay = new FrameLayout(this);

        LinearLayout cross = new LinearLayout(this);
        cross.setOrientation(LinearLayout.VERTICAL);
        cross.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.HORIZONTAL);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        cross.addView(padArrow("\u25B2", KeyEvent.KEYCODE_DPAD_UP));
        mid.addView(padArrow("\u25C0", KeyEvent.KEYCODE_DPAD_LEFT));
        mid.addView(padCenter());
        mid.addView(padArrow("\u25B6", KeyEvent.KEYCODE_DPAD_RIGHT));
        cross.addView(mid);
        cross.addView(padArrow("\u25BC", KeyEvent.KEYCODE_DPAD_DOWN));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.START);
        clp.leftMargin = dp(10);
        clp.bottomMargin = dp(6);
        overlay.addView(cross, clp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.VERTICAL);
        buttons.setGravity(Gravity.CENTER);
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        r1.addView(padRound("X", KeyEvent.KEYCODE_PAGE_UP));
        r1.addView(padRound("Y", KeyEvent.KEYCODE_PAGE_DOWN));
        r2.addView(padRound("A", KeyEvent.KEYCODE_DPAD_CENTER));
        r2.addView(padRound("B", KeyEvent.KEYCODE_BACK));
        buttons.addView(r1);
        buttons.addView(r2);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        blp.rightMargin = dp(10);
        blp.bottomMargin = dp(6);
        overlay.addView(buttons, blp);
        return overlay;
    }

    private TextView padArrow(String arrow, final int keyCode) {
        final TextView b = new TextView(this);
        b.setText(arrow);
        b.setTextSize(30);
        b.setTextColor(Color.rgb(220, 40, 40));
        b.setGravity(Gravity.CENTER);
        int s = dp(44);
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

    private TextView padCenter() {
        final TextView b = new TextView(this);
        b.setGravity(Gravity.CENTER);
        int s = dp(46);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        b.setLayoutParams(lp);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(Color.rgb(204, 32, 32));
        b.setBackground(bg);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            }
        });
        return b;
    }

    private TextView padRound(final String label, final int keyCode) {
        final TextView b = new TextView(this);
        b.setText(label);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        int s = dp(52);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(lp);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(Color.argb(215, 42, 106, 204));
        b.setBackground(bg);
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

    /** Mapeamento do gamepad fisico (menu): aperte CONTROLE por tempo longo para mapear. */
    private static final String[] PAD_ACTIONS = {
            "CIMA", "BAIXO", "ESQUERDA", "DIREITA", "A (CONFIRMAR)", "B (VOLTAR)",
            "PAGINA ACIMA", "PAGINA ABAIXO",
    };
    private int mapTarget = -1;
    private final int[] keyMap = {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_PAGE_DOWN,
    };

    private void showKeyMapDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("MAPEAR GAMEPAD F\u00cdSICO")
                .setItems(PAD_ACTIONS, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        mapTarget = which;
                        android.widget.Toast.makeText(FrontendActivity.this,
                                "Aperte no gamepad a tecla para " + PAD_ACTIONS[which],
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void loadKeyMap() {
        SharedPreferences p = getSharedPreferences("advmenu", Context.MODE_PRIVATE);
        for (int i = 0; i < keyMap.length; i++) {
            keyMap[i] = p.getInt("map" + i, keyMap[i]);
        }
    }

    private int actionForKey(int keyCode) {
        for (int i = 0; i < keyMap.length; i++) {
            if (keyMap[i] == keyCode) {
                return i;
            }
        }
        return -1;
    }

    /** Rodinha/scroll de mouse e outros aparelhos genericos (USB/Bluetooth/wireless). */
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (list != null && (ev.getSource() & android.view.InputDevice.SOURCE_CLASS_POINTER) != 0
                && ev.getAction() == MotionEvent.ACTION_SCROLL) {
            return list.dispatchGenericMotionEvent(ev);
        }
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mapTarget >= 0) {
            keyMap[mapTarget] = keyCode;
            getSharedPreferences("advmenu", Context.MODE_PRIVATE).edit()
                    .putInt("map" + mapTarget, keyCode).apply();
            mapTarget = -1;
            return true;
        }
        if (list != null) {
            int act = actionForKey(keyCode);
            if (act == 0) { sendKey(KeyEvent.KEYCODE_DPAD_UP); return true; }
            if (act == 1) { sendKey(KeyEvent.KEYCODE_DPAD_DOWN); return true; }
            if (act == 2) { sendKey(KeyEvent.KEYCODE_DPAD_LEFT); return true; }
            if (act == 3) { sendKey(KeyEvent.KEYCODE_DPAD_RIGHT); return true; }
            if (act == 4) { sendKey(KeyEvent.KEYCODE_DPAD_CENTER); return true; }
            if (act == 5) { onBackPressed(); return true; }
            if (act == 6) { sendKey(KeyEvent.KEYCODE_PAGE_UP); return true; }
            if (act == 7) { sendKey(KeyEvent.KEYCODE_PAGE_DOWN); return true; }
        }
        return super.onKeyDown(keyCode, event);
    }

    /** Menu de navegacao de SISTEMAS/EMULADORES com o tema ativo; volta com o mesmo botao. */
    private void toggleSystemsView() {
        if (systemGamesMode) {
            backToSystems();
            return;
        }
        if (systemsMode) {
            backToArcade();
            return;
        }
        systemsMode = true;
        sysToggle.setAlpha(1f);
        topSystem.setText("SISTEMAS  \u2022  EMULADORES");
        adapter.load(this);
        list.setSelection(0);
        snapshotHost.show(null);
        updateEmptyState();
        updateCount(0);
    }

    private void backToSystems() {
        systemGamesMode = false;
        currentSystem = null;
        systemsMode = true;
        sysToggle.setAlpha(1f);
        topSystem.setText("SISTEMAS  \u2022  EMULADORES");
        adapter.load(this);
        list.setSelection(0);
        snapshotHost.show(null);
        updateEmptyState();
        updateCount(0);
    }

    private void backToArcade() {
        systemsMode = false;
        systemGamesMode = false;
        currentSystem = null;
        sysToggle.setAlpha(0.45f);
        topSystem.setText("ARCADE  \u2022  ADVANCEMAME");
        adapter.load(this);
        list.setSelection(0);
        if (adapter.getCount() > 0) {
            snapshotHost.show(adapter.romAt(0));
        } else {
            snapshotHost.show(null);
        }
        updateEmptyState();
        updateCount(0);
    }

    private void openSystemAt(int position) {
        if (position >= systems.size()) {
            return;
        }
        SystemEntry e = systems.get(position);
        if (e.games.isEmpty()) {
            android.widget.Toast.makeText(this,
                    e.label + ": nenhum jogo (coloque ROMs na pasta " + e.key.toUpperCase(Locale.US)
                            + ")", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        systemGamesMode = true;
        currentSystem = e;
        topSystem.setText(e.label.toUpperCase(Locale.US) + "  (" + e.games.size() + " JOGOS)");
        adapter.load(this);
        list.setSelection(0);
        updateEmptyState();
        updateCount(0);
    }

    private void toggleFavs() {
        showFavs = !showFavs;
        favToggle.setAlpha(showFavs ? 1f : 0.45f);
        adapter.load(this);
        list.setSelection(0);
        updateEmptyState();
        updateCount(0);
    }

    private boolean isFavorite(String rom) {
        return getSharedPreferences("advmenu", Context.MODE_PRIVATE)
                .getBoolean("fav:" + rom, false);
    }

    /** Toque longo no jogo: marca/desmarca favorito. */
    private void showGameMenu(final int position) {
        final String rom = adapter.romAt(position);
        boolean fav = isFavorite(rom);
        new android.app.AlertDialog.Builder(this)
                .setTitle(adapter.titles.get(position))
                .setItems(fav
                                ? new String[]{"REMOVER DOS FAVORITOS"}
                                : new String[]{"ADICIONAR AOS FAVORITOS"},
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                boolean now = !isFavorite(rom);
                                getSharedPreferences("advmenu", Context.MODE_PRIVATE)
                                        .edit().putBoolean("fav:" + rom, now).apply();
                                adapter.load(FrontendActivity.this);
                                list.invalidateViews();
                            }
                        })
                .show();
    }

    /**
     * Controle fisico conectado: gamepad USB/Bluetooth/Type-C/WiFi, handhelds Android
     * com gamepad embutido e encoders de arcade (placa JAMMA / Zero Delay), que aparecem
     * como joystick ou como teclado USB fisico.
     */
    private boolean hasPhysicalController() {
        int[] ids = android.view.InputDevice.getDeviceIds();
        for (int id : ids) {
            android.view.InputDevice d = android.view.InputDevice.getDevice(id);
            if (d == null || d.isVirtual()) {
                continue;
            }
            int src = d.getSources();
            if ((src & android.view.InputDevice.SOURCE_GAMEPAD) != 0
                    || (src & android.view.InputDevice.SOURCE_JOYSTICK) != 0
                    || (src & android.view.InputDevice.SOURCE_DPAD) != 0
                    || (src & android.view.InputDevice.SOURCE_KEYBOARD) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Com controle fisico conectado o controle virtual some (fica so a engrenagem
     * de configuracao no canto); sem controle, volta como estava nas preferencias.
     */
    private void updateControllerUi() {
        if (padView == null || padToggle == null || gearBtn == null) {
            return;
        }
        boolean phys = hasPhysicalController();
        if (phys) {
            padView.setVisibility(View.GONE);
            padToggle.setVisibility(View.GONE);
            gearBtn.setAlpha(1f);
        } else {
            padToggle.setVisibility(View.VISIBLE);
            boolean padOn = getSharedPreferences("advmenu", Context.MODE_PRIVATE)
                    .getBoolean("pad", false);
            padView.setVisibility(padOn ? View.VISIBLE : View.GONE);
            padToggle.setAlpha(padOn ? 1f : 0.45f);
            gearBtn.setAlpha(0.6f);
        }
    }

    /** Menu de configuracao (engrenagem) no estilo do EmulationStation. */
    private void showSettingsMenu() {
        final ArrayList<String> labels = new ArrayList<>();
        labels.add("TEMA (EMULATIONSTATION)...");
        labels.add("CONTROLE VIRTUAL: "
                + (padView != null && padView.getVisibility() == View.VISIBLE
                        ? "LIGADO" : "DESLIGADO"));
        labels.add("MAPEAR GAMEPAD FISICO...");
        labels.add("SISTEMAS / EMULADORES");
        labels.add("FAVORITOS: " + (showFavs ? "LIGADO" : "DESLIGADO"));
        labels.add(hasPhysicalController()
                ? "GAMEPAD FISICO: DETECTADO" : "GAMEPAD FISICO: NAO DETECTADO");
        labels.add("RECARREGAR JOGOS");
        themedDialog("CONFIGURACAO", labels, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                if (which == 0) {
                    showThemePicker();
                } else if (which == 1) {
                    togglePad();
                } else if (which == 2) {
                    showKeyMapDialog();
                } else if (which == 3) {
                    toggleSystemsView();
                } else if (which == 4) {
                    toggleFavs();
                } else if (which == 6) {
                    reloadGames();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (systemGamesMode) {
            backToSystems();
            return;
        }
        if (systemsMode) {
            backToArcade();
            return;
        }
        super.onBackPressed();
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
                ? "SISTEMAS + EMULADORES  (" + gameTotal + " JOGOS)"
                : "EMULADORES / PC";
        themedDialog(title, labels, new android.content.DialogInterface.OnClickListener() {
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
        });
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

    /** Fundo do menu: imagem do tema ES (background) ou background.(jpg|png|...) da pasta layout. */
    private void applyLayoutBackground() {
        COR_FUNDO = COR_FUNDO_PADRAO;
        COR_TEXTO = COR_TEXTO_PADRAO;
        COR_NEIGHBOR = COR_NEIGHBOR_PADRAO;
        COR_DETALHE = COR_DETALHE_PADRAO;
        File themeDir = activeTheme != null ? themeDirs.get(activeTheme) : null;
        ThemeColors tc = new ThemeColors();
        if (themeDir != null && themeDir.isDirectory()) {
            parseThemeXml(new File(themeDir, "theme.xml"), tc, new HashMap<String, String>());
        }
        if (tc.bg != null) {
            COR_FUNDO = tc.bg;
        }
        if (tc.text != null) {
            COR_TEXTO = tc.text;
        }
        if (tc.neighbor != null) {
            COR_NEIGHBOR = tc.neighbor;
        }
        if (tc.detail != null) {
            COR_DETALHE = tc.detail;
        } else if (tc.accent != null) {
            COR_DETALHE = tc.accent;
        }
        recolorViews();

        if (layoutBg != null) {
            root.removeView(layoutBg);
            layoutBg = null;
        }
        File img = null;
        if (tc.bgImage != null) {
            File cand = new File(tc.bgImage);
            if (!cand.isAbsolute() && themeDir != null) {
                cand = new File(themeDir, tc.bgImage);
            }
            if (cand.isFile()) {
                img = cand;
            }
        }
        if (img == null && layoutDir != null) {
            File known = new File(layoutDir, "background.jpg");
            if (!known.isFile()) {
                known = new File(layoutDir, "background.png");
            }
            if (known.isFile()) {
                img = known;
            } else {
                File[] fs = layoutDir.listFiles();
                if (fs != null) {
                    for (File f : fs) {
                        String n = f.getName().toLowerCase(Locale.US);
                        if (f.isFile()) {
                            for (String ext : IMAGE_EXTS) {
                                if (n.endsWith(ext)) {
                                    img = f;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (img != null) {
            Bitmap bmp = decodeCover(img, Math.max(
                    getResources().getDisplayMetrics().widthPixels / 2,
                    getResources().getDisplayMetrics().heightPixels / 2));
            if (bmp != null) {
                layoutBg = new ImageView(this);
                layoutBg.setImageBitmap(bmp);
                layoutBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
                layoutBg.setAlpha(0.55f);
                root.addView(layoutBg, 1, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
    }

    /** Recolore o menu inteiro (barra, relogio, rodape, itens) com o tema ativo. */
    private void recolorViews() {
        root.setBackgroundColor(COR_FUNDO);
        if (topTitle != null) {
            topTitle.setTextColor(COR_DETALHE);
        }
        if (topSystem != null) {
            topSystem.setTextColor(COR_TEXTO);
        }
        if (clockView != null) {
            clockView.setTextColor(COR_TEXTO);
        }
        if (countView != null) {
            countView.setTextColor(COR_DETALHE);
        }
        if (emptyView != null) {
            emptyView.setTextColor(COR_NEIGHBOR);
        }
        if (emuButton != null) {
            emuButton.setTextColor(COR_DETALHE);
        }
        if (padToggle != null) {
            padToggle.setTextColor(COR_DETALHE);
        }
        if (favToggle != null) {
            favToggle.setTextColor(COR_DETALHE);
        }
        if (sysToggle != null) {
            sysToggle.setTextColor(COR_DETALHE);
        }
        if (list != null) {
            list.invalidateViews();
        }
    }

    /** Seletor de temas: padrao Advmenu + temas ES (pastas nativas ou do cartao). */
    private void showThemePicker() {
        ArrayList<String> names = new ArrayList<>();
        final ArrayList<String> keys = new ArrayList<>();
        names.add("PADRAO (ADVMENU)");
        keys.add(null);
        for (String k : themeDirs.keySet()) {
            names.add(k.toUpperCase(Locale.US));
            keys.add(k);
        }
        final String[] arr = names.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this)
                .setTitle("TEMA (EMULATIONSTATION)")
                .setItems(arr, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        activeTheme = keys.get(which);
                        getSharedPreferences("advmenu", Context.MODE_PRIVATE)
                                .edit().putString("theme", activeTheme == null ? "" : activeTheme)
                                .apply();
                        applyLayoutBackground();
                    }
                })
                .show();
    }

    /**
     * Dialogo com as cores do tema ativo: menus de SISTEMAS/EMULADORES e de jogos
     * de cada sistema ficam no mesmo visual do menu principal (tema aplicado).
     */
    private android.app.AlertDialog themedDialog(String title, ArrayList<String> labels,
            final android.content.DialogInterface.OnClickListener onClick) {
        final ListView lv = new ListView(this);
        lv.setDivider(null);
        lv.setVerticalScrollBarEnabled(false);
        lv.setVerticalFadingEdgeEnabled(true);
        lv.setFadingEdgeLength(dp(40));
        TextView head = new TextView(this);
        head.setText(title);
        head.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        head.setTextSize(18);
        head.setTextColor(COR_DETALHE);
        int hp = dp(14);
        head.setPadding(hp, hp, hp, hp / 2);
        lv.addHeaderView(head, null, false);
        lv.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return labels.size();
            }

            @Override
            public Object getItem(int position) {
                return labels.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = new TextView(FrontendActivity.this);
                tv.setText(labels.get(position));
                tv.setSingleLine(true);
                tv.setTypeface(Typeface.create("sans-serif-condensed",
                        position == lv.getSelectedItemPosition() ? Typeface.BOLD : Typeface.NORMAL));
                tv.setTextSize(position == lv.getSelectedItemPosition() ? 20 : 16);
                tv.setTextColor(position == lv.getSelectedItemPosition() ? COR_TEXTO : COR_NEIGHBOR);
                tv.setShadowLayer(dp(2), 0, dp(1), Color.BLACK);
                int p = dp(14);
                tv.setPadding(p, p / 2, p, p / 2);
                return tv;
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int real = position - lv.getHeaderViewsCount();
                if (real >= 0) {
                    onClick.onClick(null, real);
                }
            }
        });
        android.app.AlertDialog dlg = new android.app.AlertDialog.Builder(this).create();
        dlg.setView(lv);
        dlg.show();
        dlg.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(COR_FUNDO));
        return dlg;
    }
    private void findThemes(ArrayList<File> roots) {
        themeDirs.clear();
        for (File rootDir : roots) {
            File[] children = rootDir.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                String n = child.getName().toLowerCase(Locale.US);
                File ld = null;
                for (String want : LAYOUT_DIR_NAMES) {
                    if (n.equals(want)) {
                        ld = child;
                    }
                }
                if (ld == null || !ld.isDirectory()) {
                    continue;
                }
                if (new File(ld, "theme.xml").isFile()) {
                    themeDirs.put("(na pasta layout)", ld);
                }
                File[] subs = ld.listFiles();
                if (subs == null) {
                    continue;
                }
                for (File sub : subs) {
                    if (!sub.isDirectory()) {
                        continue;
                    }
                    File td = findThemeXmlDir(sub, 2);
                    if (td != null) {
                        themeDirs.put(sub.getName(), td);
                    }
                }
            }
        }
        File nativeThemes = new File(getFilesDir(), "themes");
        File[] nat = nativeThemes.listFiles();
        if (nat != null) {
            for (File t : nat) {
                if (!t.isDirectory()) {
                    continue;
                }
                String name = "NATIVO: " + t.getName();
                File td = findThemeXmlDir(t, 1);
                if (td != null && !themeDirs.containsKey(name)) {
                    themeDirs.put(name, td);
                }
            }
        }
    }

    private File findThemeXmlDir(File dir, int depth) {
        if (new File(dir, "theme.xml").isFile()) {
            return dir;
        }
        if (depth <= 0) {
            return null;
        }
        File[] fs = dir.listFiles();
        if (fs == null) {
            return null;
        }
        for (File f : fs) {
            if (f.isDirectory()) {
                File td = findThemeXmlDir(f, depth - 1);
                if (td != null) {
                    return td;
                }
            }
        }
        return null;
    }

    /** Cores/imagem extraidas de qualquer theme.xml ES (auto-ajusta, nada manual). */
    private static final class ThemeColors {
        Integer bg;
        Integer text;
        Integer neighbor;
        Integer detail;
        Integer accent;
        String bgImage;
    }

    private static String substVars(String v, java.util.HashMap<String, String> vars) {
        if (v == null) {
            return null;
        }
        for (int i = 0; i < 3 && vars.size() > 0; i++) {
            String out = v;
            for (java.util.Map.Entry<String, String> e : vars.entrySet()) {
                out = out.replace("${" + e.getKey() + "}", e.getValue());
            }
            if (out.equals(v)) {
                break;
            }
            v = out;
        }
        return v;
    }

    private static Integer parseColor(String v) {
        if (v == null) {
            return null;
        }
        String h = v.trim().replace("#", "");
        if (h.equals("transparent")) {
            return 0;
        }
        if (h.length() != 6 && h.length() != 8) {
            return null;
        }
        try {
            long val = Long.parseLong(h, 16);
            return h.length() == 6 ? (int) (0xFF000000L | val) : (int) val;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void themeColor(ThemeColors tc, String name, Integer c) {
        if (c == null || name == null) {
            return;
        }
        name = name.toLowerCase(Locale.US);
        if (name.contains("background")) {
            tc.bg = c;
        } else if (name.contains("selected")) {
            tc.detail = c;
        } else if (name.contains("selector")) {
            tc.detail = c & 0xFFFFFF | 0x33000000;
        } else if (name.contains("primary")) {
            tc.text = c;
        } else if (name.contains("secondary")) {
            tc.neighbor = c;
        } else if (name.contains("textcolor")) {
            tc.text = c;
        }
    }

    private void parseThemeXml(File xml, ThemeColors tc, java.util.HashMap<String, String> vars) {
        try {
            org.xmlpull.v1.XmlPullParser p = android.util.Xml.newPullParser();
            p.setInput(new java.io.FileReader(xml));
            int evt = p.getEventType();
            while (evt != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (evt == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    String tag = p.getName();
                    if ("variables".equals(tag)) {
                        while (p.next() == org.xmlpull.v1.XmlPullParser.TEXT
                                || p.getEventType() == org.xmlpull.v1.XmlPullParser.START_TAG) {
                            if (p.getEventType() == org.xmlpull.v1.XmlPullParser.START_TAG) {
                                vars.put(p.getName(), p.nextText());
                            }
                        }
                    } else if ("colorproperty".equals(tag) || "color".equals(tag)) {
                        themeColor(tc, p.getAttributeValue(null, "name"),
                                parseColor(substVars(p.nextText(), vars)));
                    } else if ("imageproperty".equals(tag) || "image".equals(tag)
                            || "staticimage".equals(tag)) {
                        String nm = p.getAttributeValue(null, "name");
                        boolean isBg = nm != null
                                && nm.toLowerCase(Locale.US).contains("background");
                        String tagEnd = tag;
                        int inner = p.getEventType();
                        while (inner != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                            inner = p.next();
                            if (inner == org.xmlpull.v1.XmlPullParser.START_TAG) {
                                String ct = p.getName();
                                if ("path".equals(ct)) {
                                    String path = substVars(p.nextText(), vars);
                                    if (isBg && path != null) {
                                        File img = new File(path);
                                        if (!img.isAbsolute()) {
                                            img = new File(xml.getParentFile(), path);
                                        }
                                        if (img.isFile()) {
                                            tc.bgImage = img.getAbsolutePath();
                                        }
                                    }
                                } else if ("color".equals(ct)) {
                                    Integer c = parseColor(substVars(p.nextText(), vars));
                                    if (c != null && (c >>> 24) >= 0xC0 && tc.accent == null) {
                                        tc.accent = c;
                                    }
                                }
                            } else if (inner == org.xmlpull.v1.XmlPullParser.END_TAG
                                    && tagEnd.equals(p.getName())) {
                                break;
                            }
                        }
                    } else if ("include".equals(tag)) {
                        String inc = substVars(p.nextText(), vars);
                        if (inc != null) {
                            File f = new File(xml.getParentFile(), inc);
                            if (!f.isFile()) {
                                f = new File(xml.getParentFile(), inc.replaceFirst("^\\./", ""));
                            }
                            if (f.isFile()) {
                                parseThemeXml(f, tc, vars);
                            }
                        }
                    }
                }
                evt = p.next();
            }
        } catch (Throwable ignored) {
            // tema malformado: segue com o que ja foi lido (auto-ajuste, sem travar)
        }
    }

    /** Raizes de armazenamento: memoria interna emulada + cartao de memoria fisico. */
    private ArrayList<File> storageRoots() {
        return AssetInstaller.storageRoots(this);
    }

    /**
     * Diretorios de jogo/midia: internos primeiro (rom/, media/) e depois as
     * pastas ROMs/, snapshot/ e layout/ de cada armazenamento.
     */
    /** Pastas basicas criadas automaticamente no cartao (nada manual). */
    private static final String[] CARD_DIR_NAMES = {
            "ROMs", "snapshot", "layout", "emuladores",
    };

    /** Cria (uma unica vez, se faltando) as pastas de sistema + basicas em cada armazenamento. */
    private static final ArrayList<String> CANONICAL_SYSTEM_DIRS = new ArrayList<>();
    static {
        HashSet<String> seen = new HashSet<>();
        for (java.util.Map.Entry<String, String> e : SYSTEM_FOLDERS.entrySet()) {
            if (seen.add(e.getValue())) {
                CANONICAL_SYSTEM_DIRS.add(e.getKey());
            }
        }
    }

    private void ensureCardDirs(ArrayList<File> roots) {
        if (!storageGranted) {
            return;
        }
        for (File r : roots) {
            if (!r.canWrite()) {
                continue;
            }
            for (String n : CARD_DIR_NAMES) {
                new File(r, n).mkdirs();
            }
            for (String n : CANONICAL_SYSTEM_DIRS) {
                new File(r, n).mkdirs();
            }
        }
    }

    private void computeMediaDirs() {
        findThemes(storageRoots());
        romDirs = new ArrayList<>();
        romDirs.add(new File(getFilesDir(), "rom"));
        snapDirs = new ArrayList<>();
        snapDirs.add(new File(getFilesDir(), "media"));
        layoutDir = null;
        emuDirs = new ArrayList<>();
        ArrayList<File> roots = storageRoots();
        ensureCardDirs(roots);
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
                if (PINBALL_SCAN_KEYS.contains(n)) {
                    label = "PINBALL (tudo)";
                }
                SystemEntry entry = byLabel.get(label);
                if (entry == null) {
                    entry = new SystemEntry(n, label);
                    byLabel.put(label, entry);
                }
                scanGameDir(child, entry.games, PC_SCAN_SYSTEMS.contains(n) ? 3 : 1);
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

    /** Pastas de pinball fundem numa tela unica; a rota por jogo escolhe o emulador. */
    private static final HashSet<String> PINBALL_SCAN_KEYS = new HashSet<>(java.util.Arrays.asList(
            "pinball", "pinballfx", "pinballfx2", "pinballfx3", "pinballarcade", "futurepinball"));

    /** Sistemas de Windows: varrem mais fundo (jogos ficam aninhados de verdade). */
    private static final HashSet<String> PC_SCAN_SYSTEMS = new HashSet<>(java.util.Arrays.asList(
            "pc", "windows", "winlator", "mugen", "teknoparrot", "taitotypex", "typex",
            "nesicaxlive", "nesica", "lindbergh", "coinops", "dos", "pcdos",
            "pinballfx", "pinballfx2", "pinballfx3", "pinballarcade", "futurepinball"));

    /** Exes de instalacao/limpeza que nao sao jogos. */
    private static final String[] EXE_SKIP = {
            "uninstall", "setup", "dxsetup", "vc_redist", "redist", "directx",
            "installscript", "crashreport", "crashhandler", "updater", "launcher_installer",
    };

    private void scanGameDir(File dir, ArrayList<File> out, int depth) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.US);
            if (f.isFile()) {
                if (!isGameFile(n) || n.endsWith(".exe") && isInstallerExe(n)) {
                    continue;
                }
                out.add(f);
            } else if (f.isDirectory() && depth > 0
                    && !n.startsWith(".") && !n.equals("__macosx")) {
                scanGameDir(f, out, depth - 1);
            }
        }
    }

    private static boolean isInstallerExe(String nameLower) {
        for (String s : EXE_SKIP) {
            if (nameLower.contains(s)) {
                return true;
            }
        }
        return false;
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

    /** Jogos de Windows (TeknoParrot/MUGEN/PC/Type X/DOS): tenta o protocolo de shortcut
     *  do Winlator CMod (shortcut_path) e cai para o menu de shortcuts do Winlator oficial. */
    private boolean launchWinlatorCombo(File f) {
        PackageManager pm = getPackageManager();
        if (pm.getLaunchIntentForPackage("com.winlator") == null) {
            return false;
        }
        try {
            Intent i = new Intent();
            i.setClassName("com.winlator", "com.winlator.ContentActivity");
            i.putExtra("shortcut_path", f.getAbsolutePath());
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
            return true;
        } catch (Throwable ignored) {
            android.widget.Toast.makeText(this,
                    "Winlator aberto — crie/abra o shortcut do " + f.getName()
                            + " (setup unico, so na primeira vez)",
                    android.widget.Toast.LENGTH_LONG).show();
            startActivity(pm.getLaunchIntentForPackage("com.winlator"));
            return true;
        }
    }

    /** App de pinball (Zen/Farsight/VPX) instalado: casa por prefixo de pacote. */
    private String pinballAppPackage(String prefix) {
        PackageManager pm = getPackageManager();
        try {
            for (android.content.pm.PackageInfo pi : pm.getInstalledPackages(0)) {
                String p = pi.packageName.toLowerCase(Locale.US);
                if (p.startsWith(prefix.toLowerCase(Locale.US))
                        && pm.getLaunchIntentForPackage(pi.packageName) != null) {
                    return pi.packageName;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** App do Visual Pinball (VPX) instalado: port Android oficial ou forks conhecidos. */
    private String pinballPackage() {
        PackageManager pm = getPackageManager();
        String[] known = {"org.vpinball.VPinballX", "org.vpinball.app.gl", "com.retroeki.pinball"};
        for (String k : known) {
            if (pm.getLaunchIntentForPackage(k) != null) {
                return k;
            }
        }
        try {
            for (android.content.pm.PackageInfo pi : pm.getInstalledPackages(0)) {
                String p = pi.packageName.toLowerCase(Locale.US);
                if (p.contains("vpinball") || p.startsWith("com.retroeki.pinball")) {
                    if (pm.getLaunchIntentForPackage(pi.packageName) != null) {
                        return pi.packageName;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Abre o jogo no emulador do sistema (app preferido) ou, se nao houver, no registrado para o tipo de arquivo. */
    private void openGameFile(File f, String sysKey) {
        String nl = f.getName().toLowerCase(Locale.US);
        if (nl.endsWith(".exe") && sysKey != null
                && (sysKey.startsWith("pinballfx") || "pinballarcade".equals(sysKey)
                        || "futurepinball".equals(sysKey))) {
            if (launchWinlatorCombo(f)) {
                return;
            }
        }
        String pkg = sysKey == null ? null : SYSTEM_PACKAGES.get(sysKey);
        if ("pinball".equals(sysKey) && !nl.endsWith(".exe")) {
            if (nl.endsWith(".vpx") || nl.endsWith(".vpt")) {
                String vpx = pinballPackage();
                if (vpx != null) {
                    pkg = vpx;
                }
            } else {
                pkg = "com.winlator";
            }
        }
        if ("pinball".equals(sysKey)) {
            String vpx = pinballPackage();
            if (vpx != null) {
                pkg = vpx;
            }
        } else if (sysKey != null && sysKey.startsWith("pinballfx")) {
            String zen = pinballAppPackage("com.zenstudios");
            if (zen != null) {
                pkg = zen;
            }
        } else if ("pinballarcade".equals(sysKey)) {
            String farsight = pinballAppPackage("com.farsight");
            if (farsight != null) {
                pkg = farsight;
            }
        }
        if ("com.winlator".equals(pkg) && launchWinlatorCombo(f)) {
            return;
        }
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
        themedDialog(system.label + "  (" + names.size() + " JOGOS)", names,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        openGameFile(system.games.get(which), system.key);
                    }
                });
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
        private final ArrayList<File> files = new ArrayList<>();
        private int selected;
        private final HashMap<String, Bitmap> thumbs = new HashMap<>();

        void load(FrontendActivity act) {
            roms.clear();
            titles.clear();
            files.clear();
            if (act.systemsMode) {
                for (SystemEntry e : act.systems) {
                    roms.add(e.key);
                    titles.add(e.label + "  (" + e.games.size() + ")");
                }
                return;
            }
            if (act.systemGamesMode && act.currentSystem != null) {
                for (File g : act.currentSystem.games) {
                    String n = g.getName();
                    int dot = n.lastIndexOf('.');
                    String b = dot > 0 ? n.substring(0, dot) : n;
                    roms.add(b);
                    titles.add(prettyName(n));
                    files.add(g);
                }
                return;
            }
            HashSet<String> seen = new HashSet<>();
            SharedPreferences pfav = act.getSharedPreferences("advmenu", Context.MODE_PRIVATE);
            for (File dir : act.romDirs) {
                scanRomDir(dir, seen, 2);
            }
            Collections.sort(roms);
            ArrayList<String> keep = new ArrayList<>();
            for (String rom : roms) {
                boolean fav = pfav.getBoolean("fav:" + rom, false);
                if (act.showFavs && !fav) {
                    continue;
                }
                titles.add((fav ? "\u2605 " : "") + prettyName(rom));
                keep.add(rom);
            }
            roms.clear();
            roms.addAll(keep);
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

        File fileAt(int position) {
            return files.get(position);
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
