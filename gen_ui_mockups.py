#!/usr/bin/env python3
"""
AppDex UI Mockup Generator
Generates high-fidelity UI screenshots based on Compose code.
Output: AppDex_UI_Overview.png (or parts)
"""
from PIL import Image, ImageDraw, ImageFont
import os
import math

# ── Colors from Color.kt ──
C_BG          = (0x0B, 0x14, 0x26)  # DeepSpaceBlue
C_BG_OUTER    = (0x06, 0x0D, 0x18)  # DeepSpaceOuter
C_SURFACE     = (0x10, 0x1B, 0x2E)  # SatelliteBlue
C_SURFACE_ALT = (0x10, 0x1D, 0x32)  # SurfaceAlt
C_SURFACE_DEEP= (0x10, 0x1C, 0x30)  # SurfaceDeep
C_SURFACE_IN  = (0x11, 0x1E, 0x33)  # SurfaceInput
C_TEXT_PRI    = (0xE5, 0xED, 0xF7)  # TextPrimary
C_TEXT_SEC    = (0x84, 0x96, 0xAE)  # TextSecondary
C_TEXT_TER    = (0x82, 0x93, 0xAA)  # TextTertiary
C_TEXT_MUTED  = (0x7F, 0x90, 0xA9)  # TextMuted
C_AMBER       = (0xE8, 0xB5, 0x47)  # AmberGold
C_AMBER_DK    = (0x28, 0x1D, 0x06)  # AmberGoldDark
C_AMBER_HL    = (0xFF, 0xE0, 0x97)  # AmberGoldHighlight
C_AMBER_SEL   = (0x3D, 0x30, 0x12)  # AmberGoldSelectedBg
C_BLUE        = (0x5B, 0x9B, 0xD5)  # NebulaBlue
C_ICON_BLUE   = (0x8F, 0xCD, 0xF7)  # IconBlue
C_ICON_BRIGHT = (0x9C, 0xD7, 0xFF)  # IconBlueBright
C_GREEN       = (0x7D, 0xD3, 0xC0)  # AuroraGreen
C_GREEN_BR    = (0x8C, 0xE2, 0xC8)  # AuroraGreenBright
C_RED         = (0xFF, 0x6B, 0x6B)  # RedSupergiant
C_BORDER_LT   = (0x2C, 0x3F, 0x5A)  # BorderLight
C_BORDER_MD   = (0x34, 0x49, 0x64)  # BorderMedium
C_BORDER_DEF  = (0x29, 0x3B, 0x55)  # BorderDefault
C_SECTION_LBL = (0xA4, 0xB5, 0xC9)  # SectionLabelColor
C_SCORE_BG    = (0x10, 0x29, 0x40)  # ScoreCardBg
C_SCORE_LBL   = (0x94, 0xD2, 0xFC)  # ScoreLabelBlue
C_SCORE_BAR   = (0x29, 0x48, 0x65)  # ScoreBarBg
C_HERO_S      = (0x14, 0x28, 0x42)  # HeroGradientStart
C_HERO_E      = (0x10, 0x1B, 0x30)  # HeroGradientEnd
C_ICON_BOX_B  = (0x14, 0x34, 0x51)  # IconBoxBlue
C_ICON_BOX_DB = (0x13, 0x2D, 0x49)  # IconBoxDeepBlue
C_TOGGLE_OFF  = (0x46, 0x56, 0x6E)  # ToggleOffBg
C_TOGGLE_THUMB= (0xE5, 0xED, 0xF7)  # ToggleThumbColor
C_TERM_BG     = (0x07, 0x10, 0x1D)  # TerminalBg
C_TERM_TEXT   = (0xB9, 0xC9, 0xDC)  # TerminalText
C_TERM_PROMPT = (0xE8, 0xB5, 0x47)  # TerminalPrompt
C_TOAST_BG    = (0x33, 0x27, 0x0E)  # ToastBg
C_TOAST_BRD   = (0x85, 0x69, 0x2B)  # ToastBorder
C_TOAST_TXT   = (0xFF, 0xE4, 0xA0)  # ToastText

# ── Dimensions ──
W, H = 1080, 2400  # Phone screen
SCALE = 0.62       # Scale for overview
OW, OH = int(W * SCALE), int(H * SCALE)
COLS = 3
SPACING = 40
LABEL_H = 50
BG_WHITE = (255, 255, 255)

# ── Fonts ──
FONT_DIR = "C:/Windows/Fonts"
def font(size, bold=False, mono=False):
    if mono:
        name = "consolab.ttf" if bold else "consola.ttf"
    else:
        name = "msyhbd.ttc" if bold else "msyh.ttc"
    path = os.path.join(FONT_DIR, name)
    if not os.path.exists(path):
        path = os.path.join(FONT_DIR, "msyh.ttc")
    return ImageFont.truetype(path, size)

# ── Helpers ──
def new_screen():
    img = Image.new("RGB", (W, H), C_BG)
    return img, ImageDraw.Draw(img)

def gradient(draw, x0, y0, x1, y1, c1, c2):
    for y in range(y0, y1):
        t = (y - y0) / max(1, y1 - y0)
        r = int(c1[0] + (c2[0]-c1[0])*t)
        g = int(c1[1] + (c2[1]-c1[1])*t)
        b = int(c1[2] + (c2[2]-c1[2])*t)
        draw.line([(x0,y),(x1,y)], fill=(r,g,b))

def rrect(draw, xy, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)

def text(draw, xy, s, fnt, fill):
    draw.text(xy, s, font=fnt, fill=fill)

def text_centered(draw, cx, cy, s, fnt, fill):
    bbox = draw.textbbox((0,0), s, font=fnt)
    w, h = bbox[2]-bbox[0], bbox[3]-bbox[1]
    draw.text((cx-w//2, cy-h//2), s, font=fnt, fill=fill)

def draw_appbar(d, title, back=False, actions=None, subtitle=None):
    """Draw APPDEX top bar"""
    y = 0
    # APPDEX label
    f_amber = font(16, bold=False, mono=True)
    f_title = font(34, bold=True)
    f_sub = font(18)
    text(d, (38, 55), "APPDEX", f_amber, C_AMBER)
    text(d, (38, 78), title, f_title, C_TEXT_PRI)
    if subtitle:
        text(d, (38, 120), subtitle, f_sub, C_TEXT_SEC)
    if back:
        # Back arrow
        d.line([(45,75),(65,55),(45,35)], fill=C_TEXT_PRI, width=3)
    if actions:
        x = W - 50
        for a in actions:
            # Draw simple icon placeholder
            rrect(d, [x-20, 60, x+20, 100], 8, outline=C_AMBER, width=2)
            x -= 50

def draw_section(d, y, label):
    f = font(18, mono=True)
    text(d, (42, y), label.upper(), f, C_SECTION_LBL)
    return y + 32

def draw_card(d, x, y, w, h, fill=C_SURFACE_ALT, border=C_BORDER_LT):
    rrect(d, [x, y, x+w, y+h], 0, fill=fill, outline=border, width=1)
    return y + h

def draw_row(d, x, y, w, icon_color, title, detail=None, chevron=True, title_fz=24, detail_fz=18):
    f_t = font(title_fz)
    f_d = font(detail_fz)
    # Icon box
    rrect(d, [x+12, y+12, x+48, y+48], 0, fill=C_SURFACE)
    d.ellipse([x+18, y+18, x+42, y+42], fill=icon_color)
    # Title
    text(d, (x+72, y+12), title, f_t, C_TEXT_PRI)
    # Detail
    if detail:
        text(d, (x+72, y+42), detail, f_d, C_TEXT_SEC)
    # Chevron
    if chevron:
        cx = x + w - 30
        d.line([(cx-8,y+24),(cx,y+16),(cx+8,y+24)], fill=C_TEXT_TER, width=2)
    return y + 60

def draw_divider(d, x, y, w):
    d.line([(x, y), (x+w, y)], fill=C_BORDER_DEF, width=1)

def draw_bottom_nav(d, current="主页"):
    y = H - 180
    d.rectangle([0, y, W, H], fill=C_SURFACE_DEEP)
    tabs = [("主页",C_GREEN), ("分析",C_BLUE), ("文件",C_AMBER), ("工具",C_AMBER), ("设置",C_AMBER)]
    tw = W // 5
    f = font(18)
    f_icon = font(16, bold=True, mono=True)
    for i, (label, color) in enumerate(tabs):
        cx = tw * i + tw // 2
        sel = (label == current)
        bg = C_AMBER_SEL if sel else None
        if bg:
            rrect(d, [cx-28, y+24, cx+28, y+76], 0, fill=bg)
        # Icon placeholder
        ic_col = C_AMBER_HL if sel else C_TEXT_TER
        d.ellipse([cx-10, y+30, cx+10, y+50], fill=ic_col)
        tc = C_AMBER_HL if sel else C_TEXT_TER
        text_centered(d, cx, y+90, label, f, tc)

def draw_tab_bar(d, y, tabs, selected=0):
    tw = W // len(tabs)
    f = font(18, mono=True)
    for i, t in enumerate(tabs):
        cx = tw * i + tw // 2
        sel = (i == selected)
        tc = C_AMBER_HL if sel else C_TEXT_TER
        text_centered(d, cx, y+24, t, f, tc)
        if sel:
            rrect(d, [cx-12, y+48, cx+12, y+50], 0, fill=C_AMBER)
    d.line([(0, y+56), (W, y+56)], fill=C_BORDER_LT, width=1)

def draw_empty_state(d, title, subtitle):
    f_t = font(30)
    f_s = font(22)
    cy = H // 2 - 100
    # Icon placeholder
    d.ellipse([W//2-40, cy-60, W//2+40, cy+20], outline=C_TEXT_TER, width=3)
    text_centered(d, W//2, cy+60, title, f_t, C_TEXT_PRI)
    if subtitle:
        text_centered(d, W//2, cy+100, subtitle, f_s, C_TEXT_SEC)

def draw_loading(d):
    cy = H // 2
    d.ellipse([W//2-40, cy-40, W//2+40, cy+40], outline=C_AMBER, width=4)
    d.arc([W//2-40, cy-40, W//2+40, cy+40], 0, 120, fill=C_AMBER, width=4)

def draw_error(d, msg):
    f = font(24)
    text_centered(d, W//2, H//2, msg, f, C_RED)

def draw_fab(d, x, y):
    d.ellipse([x, y, x+56, y+56], fill=C_AMBER)
    d.line([(x+18,y+28),(x+38,y+28)], fill=C_AMBER_DK, width=3)
    d.line([(x+28,y+18),(x+28,y+38)], fill=C_AMBER_DK, width=3)

def draw_button(d, x, y, w, text_str, bg=C_AMBER, fg=C_AMBER_DK):
    h = 44
    rrect(d, [x, y, x+w, y+h], 0, fill=bg)
    f = font(22, bold=True)
    text_centered(d, x+w//2, y+h//2, text_str, f, fg)

def draw_toggle(d, x, y, checked):
    w, h = 48, 28
    bg = C_GREEN if checked else C_TOGGLE_OFF
    rrect(d, [x, y, x+w, y+h], 14, fill=bg)
    cx = x + (w-20) if checked else x + 4
    d.ellipse([cx, y+4, cx+20, y+24], fill=C_TOGGLE_THUMB)

def draw_snackbar(d, msg):
    y = H - 260
    rrect(d, [40, y, W-40, y+56], 8, fill=C_TOAST_BG, outline=C_TOAST_BRD, width=1)
    f = font(20)
    text(d, (64, y+18), msg, f, C_TOAST_TXT)

def draw_score_card(d, x, y, w, score, risk):
    rrect(d, [x, y, x+w, y+200], 0, fill=C_SCORE_BG, outline=(0x36,0x56,0x75), width=1)
    f_lbl = font(16, mono=True)
    f_score = font(90, bold=True)
    f_risk = font(22)
    text(d, (x+32, y+24), "安全评分", f_lbl, C_SCORE_LBL)
    text(d, (x+32, y+48), str(score), f_score, C_TEXT_PRI)
    text(d, (x+w-120, y+100), risk, f_risk, C_GREEN if score >= 80 else C_AMBER)
    # Bar
    bar_w = w - 64
    d.rectangle([x+32, y+160, x+32+bar_w, y+166], fill=C_SCORE_BAR)
    fill_w = int(bar_w * score / 100)
    d.rectangle([x+32, y+160, x+32+fill_w, y+166], fill=C_GREEN if score>=80 else C_AMBER)

def label_screen(img, label):
    """Add label above screenshot"""
    sw, sh = img.size
    labeled = Image.new("RGB", (sw, sh + LABEL_H), BG_WHITE)
    d = ImageDraw.Draw(labeled)
    f = font(28, bold=True)
    text_centered(d, sw//2, LABEL_H//2, label, f, (50,50,50))
    labeled.paste(img, (0, LABEL_H))
    return labeled

# ── Screen Generators ──

def screen_home():
    d_img, d = new_screen()
    draw_appbar(d, "工作台")
    # Hero card
    y = 160
    rrect(d, [42, y, W-42, y+220], 0, fill=None, outline=C_BORDER_LT, width=1)
    gradient(d, 42, y, W-42, y+220, C_HERO_S, C_HERO_E)
    # Icon
    rrect(d, [58, y+20, 102, y+64], 0, fill=C_ICON_BOX_B)
    d.ellipse([64, y+26, 96, y+58], fill=C_ICON_BRIGHT)
    text(d, (114, y+22), "AppDex", font(24, mono=True), C_TEXT_PRI)
    text(d, (114, y+52), "多功能 Android 逆向工程工具箱", font(18), C_TEXT_SEC)
    text(d, (W-120, y+30), "v1.0.0", font(34, bold=True, mono=True), C_GREEN_BR)
    # Start button
    rrect(d, [58, y+140, W-58, y+180], 0, fill=C_AMBER)
    text_centered(d, W//2, y+160, "开始分析", font(22, bold=True), C_AMBER_DK)
    # Quick tools
    y = 430
    y = draw_section(d, y, "快速工具")
    tools = [("快速扫描",C_BLUE), ("DEX 浏览器",C_GREEN), ("权限审计",C_RED), ("签名验证",C_AMBER), ("终端",C_GREEN), ("文本编辑器",C_BLUE)]
    for i, (name, color) in enumerate(tools):
        col = i % 2
        row = i // 2
        cx = 42 + col * 500
        cy = y + 10 + row * 170
        rrect(d, [cx, cy, cx+470, cy+150], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
        rrect(d, [cx+12, cy+12, cx+48, cy+48], 0, fill=C_ICON_BOX_DB)
        d.ellipse([cx+18, cy+18, cx+42, cy+42], fill=color)
        text(d, (cx+12, cy+60), name, font(22, bold=True), C_TEXT_PRI)
        text(d, (cx+12, cy+90), "打开工具", font(18), C_TEXT_SEC)
    # Recent analyses empty
    y = y + 10 + 3 * 170 + 20
    y = draw_section(d, y, "最近分析")
    rrect(d, [42, y, W-42, y+120], 0, fill=None, outline=C_BORDER_LT, width=1)
    text_centered(d, W//2, y+50, "暂无分析记录", font(22), C_TEXT_SEC)
    # FAB
    draw_fab(d, W-100, H-280)
    draw_bottom_nav(d, "主页")
    return d_img

def screen_analyzer_empty():
    d_img, d = new_screen()
    draw_appbar(d, "APK 分析")
    draw_empty_state(d, "选择 APK 文件进行分析", "点击下方按钮选择 APK")
    draw_button(d, 84, H-260, W-168, "选择 APK 文件进行分析")
    draw_bottom_nav(d, "分析")
    return d_img

def screen_analyzer_data():
    d_img, d = new_screen()
    draw_appbar(d, "APK 分析")
    y = 160
    # APK header card
    rrect(d, [42, y, W-42, y+100], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    rrect(d, [58, y+16, 106, y+64], 0, fill=C_ICON_BOX_B)
    d.ellipse([64, y+22, 100, y+58], fill=C_ICON_BRIGHT)
    text(d, (122, y+16), "com.appdex", font(22, mono=True), C_TEXT_PRI)
    text(d, (122, y+48), "1.0.0 · 12.16 MB", font(18), C_TEXT_SEC)
    # Sections
    y = 300
    y = draw_section(d, y, "基本信息")
    rrect(d, [42, y, W-42, y+280], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    rows = [("包名","com.appdex"),("版本","1.0.0 (7)"),("文件大小","12.16 MB"),("文件总数","439"),("Min SDK","26"),("Target SDK","35")]
    for i, (k, v) in enumerate(rows):
        ry = y + 10 + i * 44
        text(d, (54, ry), k, font(20, mono=True), C_TEXT_SEC)
        text(d, (W//2, ry), v, font(20, mono=True), C_TEXT_PRI)
        if i < len(rows)-1:
            draw_divider(d, 54, ry+38, W-54-42)
    y = y + 300
    y = draw_section(d, y, "签名 (1)")
    rrect(d, [42, y, W-42, y+200], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    sig_rows = [("算法","SHA256withRSA"),("主体","C=US,O=Android"),("SHA-256","807AB2B6..."),("有效期","2026-05-21")]
    for i, (k, v) in enumerate(sig_rows):
        ry = y + 10 + i * 44
        text(d, (54, ry), k, font(20, mono=True), C_TEXT_SEC)
        text(d, (W//2, ry), v, font(20, mono=True), C_TEXT_PRI)
        if i < len(sig_rows)-1:
            draw_divider(d, 54, ry+38, W-54-42)
    y = y + 220
    y = draw_section(d, y, "权限 (14)")
    rrect(d, [42, y, W-42, y+120], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    perms = ["android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.ACCESS_WIFI_STATE"]
    for i, p in enumerate(perms):
        ry = y + 10 + i * 36
        text(d, (54, ry), "• " + p, font(18, mono=True), C_AMBER)
    draw_button(d, 42, H-260, W-84, "查看详情")
    draw_bottom_nav(d, "分析")
    return d_img

def screen_apk_detail():
    d_img, d = new_screen()
    draw_appbar(d, "APK 详情", back=True, subtitle="1.0.0 · 12.16 MB · API 26+")
    # Tab bar
    draw_tab_bar(d, 160, ["概览","清单","DEX","资源","签名","文件"], 0)
    y = 230
    # Score card
    draw_score_card(d, 42, y, W-84, 84, "低风险")
    y += 220
    # Quick access
    y = draw_section(d, y, "快捷入口")
    rrect(d, [42, y, W-42, y+340], 0, fill=None, outline=C_BORDER_LT, width=1)
    entries = [("权限信息","14 个权限 · 1 个 Activity",C_RED),
               ("DEX / 类浏览","2 个 DEX 文件",C_BLUE),
               ("签名验证","v2",C_AMBER),
               ("回编译 / 反编","Smali -> DEX -> APK",C_GREEN),
               ("APK 文件列表","439 个文件",C_BLUE)]
    for i, (t, det, c) in enumerate(entries):
        ry = y + i * 68
        draw_row(d, 42, ry, W-84, c, t, det)
        if i < len(entries)-1:
            draw_divider(d, 54, ry+60, W-54-42)
    return d_img

def screen_files():
    d_img, d = new_screen()
    draw_appbar(d, "文件管理")
    # Path breadcrumb
    text(d, (42, 130), "/storage/emulated/0", font(20, mono=True), C_TEXT_SEC)
    # Files list
    y = 170
    files = [("Android","folder","2026-07-12"),("Download","folder","2026-07-14"),("DCIM","folder","2026-07-10"),
             ("Music","folder","2026-07-08"),("Pictures","folder","2026-07-11"),("test.apk","file","12.75 MB"),
             ("test.txt","file","25 B"),("Documents","folder","2026-07-09")]
    rrect(d, [42, y, W-42, y+len(files)*72], 0, fill=None, outline=C_BORDER_LT, width=1)
    for i, (name, typ, detail) in enumerate(files):
        ry = y + i * 72
        ic = C_AMBER if typ=="folder" else C_BLUE
        rrect(d, [54, ry+12, 90, ry+48], 0, fill=C_SURFACE)
        d.ellipse([60, ry+18, 84, ry+42], fill=ic)
        text(d, (102, ry+16), name, font(22), C_TEXT_PRI)
        text(d, (102, ry+44), detail, font(18), C_TEXT_SEC)
        if i < len(files)-1:
            draw_divider(d, 54, ry+68, W-54-42)
    draw_bottom_nav(d, "文件")
    return d_img

def screen_editor():
    d_img, d = new_screen()
    draw_appbar(d, "MainActivity.kt", back=True, subtitle="未保存修改", actions=["open","save"])
    y = 160
    # Line numbers + code
    f_code = font(22, mono=True)
    f_ln = font(18, mono=True)
    lines = [
        "package com.appdex",
        "",
        "import android.os.Bundle",
        "import androidx.activity.*",
        "",
        "class MainActivity : ComponentActivity() {",
        "    override fun onCreate(savedInstanceState: Bundle?) {",
        "        super.onCreate(savedInstanceState)",
        "        setContent {",
        "            AppDexTheme {",
        "                AppDexApp()",
        "            }",
        "        }",
        "    }",
        "}",
    ]
    for i, line in enumerate(lines):
        ly = y + i * 30
        text(d, (16, ly), str(i+1), f_ln, (0x50,0x60,0x70))
        text(d, (60, ly), line, f_code, C_TERM_TEXT)
    # Divider
    d.line([(0, H-60), (W, H-60)], fill=C_BORDER_LT, width=1)
    # Status bar
    text(d, (32, H-45), "325 chars · Ln 15", font(16, mono=True), C_TEXT_TER)
    text(d, (W-120, H-45), "UTF-8", font(16, mono=True), C_TEXT_TER)
    return d_img

def screen_terminal():
    d_img, d = Image.new("RGB", (W, H), C_TERM_BG), None
    d = ImageDraw.Draw(d_img)
    draw_appbar(d, "终端", back=True, actions=["copy","clear"])
    y = 160
    f = font(20, mono=True)
    fp = font(20, bold=True, mono=True)
    lines = [
        ("system", "APPDEX Terminal v1.0 — type 'exit' to close session"),
        ("prompt", "appdex@device:/data/data/com.appdex/files$ "),
        ("cmd", "ls -la"),
        ("output", "total 48"),
        ("output", "drwxrwxr-x 2 appdex 4096 ."),
        ("output", "drwxr-xr-x 3 appdex 4096 .."),
        ("output", "-rw-rw-r-- 1 appdex 1234 build.gradle.kts"),
        ("output", "-rw-rw-r-- 1 appdex 5678 settings.gradle.kts"),
        ("prompt", "appdex@device:/data/data/com.appdex/files$ "),
    ]
    for typ, line in lines:
        if typ == "system":
            text(d, (32, y), line, f, C_TEXT_TER)
        elif typ == "prompt":
            text(d, (32, y), "appdex@device:", fp, C_TERM_PROMPT)
            text(d, (200, y), line[17:], f, C_BLUE)
        elif typ == "cmd":
            text(d, (32, y), line, f, C_TERM_TEXT)
        else:
            text(d, (32, y), line, f, C_TERM_TEXT)
        y += 30
    # Input
    d.line([(0, H-140),(W, H-140)], fill=C_BORDER_LT, width=1)
    text(d, (32, H-110), "$ ", fp, C_TERM_PROMPT)
    rrect(d, [60, H-120, W-180, H-80], 4, outline=C_BORDER_MD, width=1)
    # Quick keys bar
    keys = ["Ctrl+C","Ctrl+Z","Ctrl+D","Tab","↑","↓","Esc","Enter"]
    kw = W // len(keys)
    for i, k in enumerate(keys):
        text_centered(d, kw*i+kw//2, H-45, k, font(16, mono=True), C_TEXT_TER)
    return d_img

def screen_tools():
    d_img, d = new_screen()
    draw_appbar(d, "工具集", back=True)
    y = 160
    y = draw_section(d, y, "分析与开发")
    rrect(d, [42, y, W-42, y+600], 0, fill=None, outline=C_BORDER_LT, width=1)
    tools1 = [("快速扫描","选择 APK 后执行快速结构扫描",C_BLUE),
              ("DEX 浏览器","类、方法与字符串索引",C_GREEN),
              ("权限审计","组件导出与危险权限检查",C_RED),
              ("签名验证","查看证书、摘要与签名方案",C_AMBER),
              ("APK 对比","双 APK 差异分析",C_BLUE),
              ("安全扫描","硬编码密钥/漏洞检测",C_RED),
              ("体积分析","可视化空间占用",C_GREEN),
              ("AXML 编辑器","二进制 XML 解码/编码",C_BLUE),
              ("ARSC 资源表","解析 resources.arsc",C_GREEN)]
    for i, (t, det, c) in enumerate(tools1):
        ry = y + i * 66
        draw_row(d, 42, ry, W-84, c, t, det)
        if i < len(tools1)-1:
            draw_divider(d, 54, ry+58, W-54-42)
    y += 620
    y = draw_section(d, y, "系统工具")
    rrect(d, [42, y, W-42, y+200], 0, fill=None, outline=C_BORDER_LT, width=1)
    tools2 = [("终端","本地 shell 会话",C_GREEN), ("文本编辑器","支持 .adx 语法定义",C_BLUE), ("远程管理","局域网文件访问",C_AMBER)]
    for i, (t, det, c) in enumerate(tools2):
        ry = y + i * 66
        draw_row(d, 42, ry, W-84, c, t, det)
        if i < len(tools2)-1:
            draw_divider(d, 54, ry+58, W-54-42)
    draw_bottom_nav(d, "工具")
    return d_img

def screen_settings():
    d_img, d = new_screen()
    draw_appbar(d, "设置")
    y = 160
    y = draw_section(d, y, "配置")
    rrect(d, [42, y, W-42, y+180], 0, fill=None, outline=C_BORDER_LT, width=1)
    draw_row(d, 42, y, W-84, C_BLUE, "文件管理", "隐藏文件、默认目录与操作确认")
    draw_divider(d, 54, y+60, W-54-42)
    draw_row(d, 42, y+66, W-84, C_AMBER, "外观", "深色模式、信息密度与无障碍")
    draw_divider(d, 54, y+126, W-54-42)
    draw_row(d, 42, y+132, W-84, C_GREEN, "高级选项", "缓存、日志与实验功能")
    y += 200
    y = draw_section(d, y, "编辑器")
    rrect(d, [42, y, W-42, y+180], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    text(d, (58, y+16), "字体大小: 14 sp", font(22), C_TEXT_PRI)
    # Slider
    d.line([(58, y+60), (W-58, y+60)], fill=C_BORDER_MD, width=2)
    d.ellipse([W//2-8, y+52, W//2+8, y+68], fill=C_AMBER)
    text(d, (58, y+90), "Tab 宽度: 4", font(22), C_TEXT_PRI)
    d.line([(58, y+134), (W-58, y+134)], fill=C_BORDER_MD, width=2)
    d.ellipse([W//2-8, y+126, W//2+8, y+142], fill=C_AMBER)
    y += 200
    y = draw_section(d, y, "关于")
    rrect(d, [42, y, W-42, y+240], 0, fill=None, outline=C_BORDER_LT, width=1)
    about = [("版本","1.0.0"),("协议","Apache 2.0"),("GitHub","github.com/niceyayale/appdex"),("引擎","Jetpack Compose"),("Min Android","8.0 (API 26)")]
    for i, (k, v) in enumerate(about):
        ry = y + 10 + i * 44
        text(d, (54, ry), k, font(22), C_TEXT_PRI)
        text(d, (W//2, ry), v, font(20, mono=True), C_TEXT_SEC)
        if i < len(about)-1:
            draw_divider(d, 54, ry+38, W-54-42)
    draw_bottom_nav(d, "设置")
    return d_img

def screen_webserver():
    d_img, d = new_screen()
    draw_appbar(d, "远程管理", back=True)
    draw_tab_bar(d, 160, ["Web 服务","FTP 客户端"], 0)
    y = 240
    # Server status
    rrect(d, [42, y, W-42, y+120], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    text(d, (58, y+16), "Server Running", font(26, bold=True), C_GREEN)
    text(d, (58, y+52), "Running on http://127.0.0.1:8080 (auth enabled)", font(18, mono=True), C_TEXT_SEC)
    # QR placeholder
    rrect(d, [W-140, y+16, W-58, y+98], 4, fill=C_TEXT_PRI)
    y += 140
    # Port
    text(d, (58, y), "Port", font(20, mono=True), C_TEXT_SEC)
    text(d, (200, y), "8080", font(20, mono=True), C_TEXT_PRI)
    y += 40
    text(d, (58, y), "Auth Token", font(20, mono=True), C_TEXT_SEC)
    text(d, (200, y), "7bab9f55ef874d83", font(20, mono=True), C_TEXT_PRI)
    y += 40
    text(d, (58, y), "Bearer Token Authentication", font(18), C_TEXT_TER)
    y += 40
    text(d, (58, y), "Generates a random token on start", font(18), C_TEXT_TER)
    y += 40
    # How to use
    y = draw_section(d, y, "How to use")
    steps = ["1. Make sure your phone and computer are on the same Wi-Fi network.",
             "2. Start the server and scan the QR code or enter the URL in a browser.",
             "3. Browse, download, and upload files from your computer."]
    for i, s in enumerate(steps):
        text(d, (58, y+i*36), s, font(18), C_TEXT_SEC)
    # Stop button
    draw_button(d, 84, H-200, W-168, "Stop Server", bg=C_RED, fg=(255,255,255))
    return d_img

def screen_signing():
    d_img, d = new_screen()
    draw_appbar(d, "APK 签名", back=True)
    y = 160
    y = draw_section(d, y, "签名信息")
    rrect(d, [42, y, W-42, y+360], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    rows = [("算法","SHA256withRSA"),("版本","v2"),("主体","C=US,O=Android,CN=Android Debug"),
            ("颁发者","C=US,O=Android,CN=Android Debug"),("序列号","2b4071ad"),
            ("SHA-256","807AB2B65E0D19F9..."),("SHA-1","80557D5C3311B270..."),
            ("MD5","2B4071AD810278ED...")]
    for i, (k, v) in enumerate(rows):
        ry = y + 10 + i * 44
        text(d, (54, ry), k, font(20, mono=True), C_TEXT_SEC)
        text(d, (W//2, ry), v, font(20, mono=True), C_TEXT_PRI)
        if i < len(rows)-1:
            draw_divider(d, 54, ry+38, W-54-42)
    y += 380
    y = draw_section(d, y, "有效期")
    rrect(d, [42, y, W-42, y+100], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    text(d, (54, y+16), "有效期起", font(20, mono=True), C_TEXT_SEC)
    text(d, (W//2, y+16), "2026-05-21 05:46", font(20, mono=True), C_TEXT_PRI)
    draw_divider(d, 54, y+48, W-54-42)
    text(d, (54, y+56), "有效期至", font(20, mono=True), C_TEXT_SEC)
    text(d, (W//2, y+56), "2056-05-13 05:46", font(20, mono=True), C_TEXT_PRI)
    return d_img

def screen_repack():
    d_img, d = new_screen()
    draw_appbar(d, "回编译", back=True)
    y = 160
    y = draw_section(d, y, "APK 文件")
    rrect(d, [42, y, W-42, y+100], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    text(d, (58, y+16), "test.apk", font(22, mono=True), C_TEXT_PRI)
    text(d, (58, y+48), "12.16 MB · 439 entries", font(18), C_TEXT_SEC)
    y += 120
    y = draw_section(d, y, "DEX 文件")
    rrect(d, [42, y, W-42, y+140], 0, fill=None, outline=C_BORDER_LT, width=1)
    draw_row(d, 42, y, W-84, C_BLUE, "classes.dex", "4.2 MB")
    draw_divider(d, 54, y+60, W-54-42)
    draw_row(d, 42, y+66, W-84, C_GREEN, "classes2.dex", "1.8 MB")
    y += 160
    y = draw_section(d, y, "操作")
    draw_button(d, 84, y, W-168, "开始重打包")
    y += 60
    text(d, (42, y), "Smali -> DEX -> APK -> 签名", font(18), C_TEXT_TER)
    return d_img

def screen_hex():
    d_img, d = new_screen()
    draw_appbar(d, "HEX Editor", back=True)
    y = 160
    f = font(20, mono=True)
    f_a = font(18, mono=True)
    # Offset | Hex | ASCII
    for row in range(20):
        ry = y + row * 28
        offset = f"{row*16:08X}"
        hexs = " ".join(f"{(row*16+i)%256:02X}" for i in range(16))
        ascii_str = "".join(chr(65+(row*16+i)%26) for i in range(16))
        text(d, (16, ry), offset, f_a, C_TEXT_TER)
        text(d, (140, ry), hexs, f, C_TEXT_PRI)
        text(d, (620, ry), ascii_str, f, C_GREEN)
    d.line([(0, y+20*28+10),(W, y+20*28+10)], fill=C_BORDER_LT, width=1)
    text(d, (32, H-80), "Offset: 0x00000000", f_a, C_TEXT_TER)
    text(d, (W-300, H-80), "256 bytes", f_a, C_TEXT_TER)
    return d_img

def screen_dex_browser():
    d_img, d = new_screen()
    draw_appbar(d, "DEX Browser", back=True)
    draw_tab_bar(d, 160, ["类列表","方法","字段","字符串"], 0)
    y = 230
    rrect(d, [42, y, W-42, y+400], 0, fill=None, outline=C_BORDER_LT, width=1)
    classes = ["com.appdex.AppDexApplication","com.appdex.AppDexActivity","com.appdex.nav.AppDexApp",
               "com.appdex.ui.HomeScreen","com.appdex.ui.Route","com.appdex.analyzer.ApkAnalyzerViewModel",
               "com.appdex.editor.EditorViewModel","com.appdex.files.FileManagerViewModel"]
    for i, cls in enumerate(classes):
        ry = y + 10 + i * 48
        rrect(d, [54, ry+8, 90, ry+40], 0, fill=C_SURFACE)
        d.ellipse([60, ry+14, 84, ry+38], fill=C_BLUE)
        text(d, (102, ry+12), cls, font(20, mono=True), C_TEXT_PRI)
        if i < len(classes)-1:
            draw_divider(d, 54, ry+44, W-54-42)
    return d_img

def screen_security():
    d_img, d = new_screen()
    draw_appbar(d, "安全扫描", back=True)
    y = 160
    draw_score_card(d, 42, y, W-84, 84, "低风险")
    y += 220
    y = draw_section(d, y, "发现的问题")
    rrect(d, [42, y, W-42, y+240], 0, fill=None, outline=C_BORDER_LT, width=1)
    issues = [("高危","android.permission.MANAGE_EXTERNAL_STORAGE",C_RED),
              ("中危","2 个 Activity 未设置 exported",C_AMBER),
              ("低危","android:debuggable 未设置",C_GREEN)]
    for i, (sev, desc, c) in enumerate(issues):
        ry = y + 10 + i * 76
        rrect(d, [54, ry, 120, ry+32], 0, fill=c)
        text_centered(d, 114, ry+16, sev, font(18, bold=True), (255,255,255))
        text(d, (140, ry+6), desc, font(20), C_TEXT_PRI)
        if i < len(issues)-1:
            draw_divider(d, 54, ry+68, W-54-42)
    return d_img

def screen_size_analyzer():
    d_img, d = new_screen()
    draw_appbar(d, "体积分析", back=True)
    y = 160
    y = draw_section(d, y, "APK 体积分布")
    # Pie chart placeholder
    cx, cy, r = W//2, y+200, 120
    d.ellipse([cx-r, cy-r, cx+r, cy+r], fill=C_BLUE)
    d.pieslice([cx-r, cy-r, cx+r, cy+r], 0, 120, fill=C_AMBER)
    d.pieslice([cx-r, cy-r, cx+r, cy+r], 120, 240, fill=C_GREEN)
    d.pieslice([cx-r, cy-r, cx+r, cy+r], 240, 360, fill=C_RED)
    y += 360
    # Legend
    items = [("DEX","42%",C_BLUE),("Resources","28%",C_AMBER),("Native","18%",C_GREEN),("Other","12%",C_RED)]
    for i, (name, pct, c) in enumerate(items):
        ry = y + i * 40
        rrect(d, [42, ry, 70, ry+24], 0, fill=c)
        text(d, (84, ry), name, font(20), C_TEXT_PRI)
        text(d, (W-120, ry), pct, font(20, mono=True), C_TEXT_SEC)
    return d_img

def screen_diff():
    d_img, d = new_screen()
    draw_appbar(d, "APK 对比", back=True)
    y = 160
    y = draw_section(d, y, "选择 APK")
    rrect(d, [42, y, W-42, y+100], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    text(d, (58, y+16), "APK A: test.apk", font(22, mono=True), C_TEXT_PRI)
    text(d, (58, y+48), "12.16 MB", font(18), C_TEXT_SEC)
    y += 120
    rrect(d, [42, y, W-42, y+100], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    text(d, (58, y+16), "APK B: (未选择)", font(22, mono=True), C_TEXT_TER)
    text(d, (58, y+48), "点击选择 APK B", font(18), C_TEXT_TER)
    y += 140
    draw_button(d, 84, y, W-168, "开始对比")
    return d_img

def screen_loading():
    d_img, d = new_screen()
    draw_appbar(d, "APK 分析")
    draw_loading(d)
    text_centered(d, W//2, H//2+80, "正在解析 APK...", font(24), C_TEXT_SEC)
    draw_bottom_nav(d, "分析")
    return d_img

def screen_error():
    d_img, d = new_screen()
    draw_appbar(d, "APK 分析")
    draw_error(d, "文件解析失败")
    text_centered(d, W//2, H//2+50, "请检查文件是否为有效的 APK", font(20), C_TEXT_SEC)
    draw_button(d, 84, H-260, W-168, "重试")
    draw_bottom_nav(d, "分析")
    return d_img

def screen_empty():
    d_img, d = new_screen()
    draw_appbar(d, "编辑器", back=True)
    draw_empty_state(d, "打开文件开始编辑", "点击右上角文件夹图标选择文件")
    return d_img

def screen_snackbar():
    d_img, d = new_screen()
    draw_appbar(d, "编辑器", back=True, subtitle="未保存修改", actions=["open","save"])
    # Some content
    y = 160
    f = font(22, mono=True)
    for i in range(15):
        text(d, (60, y), f"// line {i+1} of sample code", f, C_TEXT_TER)
        y += 30
    draw_snackbar(d, "已保存")
    return d_img

def screen_dialog():
    d_img, d = new_screen()
    # Dimmed background
    d_img2 = screen_settings()
    d_img.paste(d_img2, (0, 0))
    d = ImageDraw.Draw(d_img)
    # Overlay
    overlay = Image.new("RGB", (W, H), (0, 0, 0))
    d_img = Image.blend(d_img, overlay, 0.5)
    d = ImageDraw.Draw(d_img)
    # Dialog
    dw, dh = 800, 400
    dx, dy = (W-dw)//2, (H-dh)//2
    rrect(d, [dx, dy, dx+dw, dy+dh], 8, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    text(d, (dx+32, dy+32), "清除缓存", font(28, bold=True), C_TEXT_PRI)
    text(d, (dx+32, dy+80), "缓存大小: 12.5 MB", font(22), C_TEXT_SEC)
    text(d, (dx+32, dy+112), "", font(22), C_TEXT_SEC)
    text(d, (dx+32, dy+120), "确定要清除缓存吗？", font(22), C_TEXT_SEC)
    # Buttons
    draw_button(d, dx+dw-340, dy+dh-80, 140, "取消", bg=C_SURFACE, fg=C_TEXT_SEC)
    draw_button(d, dx+dw-180, dy+dh-80, 140, "清除")
    return d_img

def screen_hash_calc():
    d_img, d = new_screen()
    draw_appbar(d, "哈希计算器", back=True)
    y = 160
    y = draw_section(d, y, "输入")
    rrect(d, [42, y, W-42, y+80], 0, fill=C_SURFACE_IN, outline=C_BORDER_MD, width=1)
    text(d, (58, y+24), "Hello AppDex", font(22), C_TEXT_PRI)
    y += 100
    y = draw_section(d, y, "结果")
    rrect(d, [42, y, W-42, y+240], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    hashes = [("MD5","5D41402ABC4B2A76B9719D911017C592"),
              ("SHA-1","AAF4C61DDCC5E8A2DABEDEDBD4B6F9B1"),
              ("SHA-256","2CF24BDC5FB0A7B6AC812B9...")]
    for i, (k, v) in enumerate(hashes):
        ry = y + 10 + i * 76
        text(d, (54, ry), k, font(20, mono=True), C_TEXT_SEC)
        text(d, (54, ry+28), v, font(18, mono=True), C_TEXT_PRI)
        if i < len(hashes)-1:
            draw_divider(d, 54, ry+68, W-54-42)
    return d_img

def screen_encoding():
    d_img, d = new_screen()
    draw_appbar(d, "编码转换", back=True)
    y = 160
    y = draw_section(d, y, "输入")
    rrect(d, [42, y, W-42, y+80], 0, fill=C_SURFACE_IN, outline=C_BORDER_MD, width=1)
    text(d, (58, y+24), "Hello AppDex", font(22), C_TEXT_PRI)
    y += 100
    y = draw_section(d, y, "输出")
    rrect(d, [42, y, W-42, y+200], 0, fill=C_SURFACE_ALT, outline=C_BORDER_LT, width=1)
    encs = [("Base64","SGVsbG8gQXBwRGV4"),("URL","Hello%20AppDex"),("Hex","48656C6C6F")]
    for i, (k, v) in enumerate(encs):
        ry = y + 10 + i * 64
        text(d, (54, ry), k, font(20, mono=True), C_TEXT_SEC)
        text(d, (200, ry), v, font(20, mono=True), C_TEXT_PRI)
        if i < len(encs)-1:
            draw_divider(d, 54, ry+56, W-54-42)
    return d_img

def screen_json_formatter():
    d_img, d = new_screen()
    draw_appbar(d, "JSON 格式化", back=True)
    y = 160
    f = font(20, mono=True)
    json_lines = [
        '{',
        '  "name": "AppDex",',
        '  "version": "1.0.0",',
        '  "features": [',
        '    "analyzer",',
        '    "editor",',
        '    "terminal"',
        '  ],',
        '  "openSource": true',
        '}',
    ]
    for i, line in enumerate(json_lines):
        text(d, (42, y+i*28), line, f, C_GREEN)
    return d_img

def screen_device_info():
    d_img, d = new_screen()
    draw_appbar(d, "设备信息", back=True)
    y = 160
    y = draw_section(d, y, "硬件")
    rrect(d, [42, y, W-42, y+240], 0, fill=None, outline=C_BORDER_LT, width=1)
    info = [("型号","Pixel 8 Pro"),("品牌","Google"),("屏幕","1080×2400"),("CPU","Snapdragon")]
    for i, (k, v) in enumerate(info):
        ry = y + 10 + i * 56
        text(d, (54, ry), k, font(22), C_TEXT_PRI)
        text(d, (W//2, ry), v, font(22, mono=True), C_TEXT_SEC)
        if i < len(info)-1:
            draw_divider(d, 54, ry+48, W-54-42)
    return d_img

def screen_image_viewer():
    d_img, d = new_screen()
    draw_appbar(d, "图片查看", back=True)
    # Image placeholder
    rrect(d, [0, 160, W, H-100], 0, fill=(0x1A, 0x1A, 0x1A))
    rrect(d, [100, 300, W-100, H-300], 8, fill=(0x2A, 0x3A, 0x5A))
    text_centered(d, W//2, H//2, "📷 Image", font(40), C_TEXT_TER)
    return d_img

def screen_audio_player():
    d_img, d = new_screen()
    draw_appbar(d, "音频播放器", back=True)
    # Album art
    rrect(d, [W//2-150, 300, W//2+150, 600], 8, fill=C_SURFACE_DEEP)
    text_centered(d, W//2, 450, "🎵", font(60), C_AMBER)
    text_centered(d, W//2, 680, "track.mp3", font(24), C_TEXT_PRI)
    # Progress bar
    d.line([(100, 760), (W-100, 760)], fill=C_BORDER_MD, width=3)
    d.line([(100, 760), (W//2, 760)], fill=C_AMBER, width=3)
    text(d, (100, 780), "1:23", font(18), C_TEXT_SEC)
    text(d, (W-150, 780), "3:45", font(18), C_TEXT_SEC)
    # Controls
    d.ellipse([W//2-30, 880, W//2+30, 940], fill=C_AMBER)  # Play
    d.ellipse([W//2-130, 885, W//2-70, 935], outline=C_TEXT_TER, width=3)  # Prev
    d.ellipse([W//2+70, 885, W//2+130, 935], outline=C_TEXT_TER, width=3)  # Next
    return d_img

def screen_video_player():
    d_img, d = new_screen()
    # Full screen video
    d.rectangle([0, 0, W, H], fill=(0x0A, 0x0A, 0x0A))
    text_centered(d, W//2, H//2, "▶", font(60), C_TEXT_TER)
    text(d, (32, H-80), "00:15:30", font(18), C_TEXT_SEC)
    d.line([(200, H-70), (W-200, H-70)], fill=C_AMBER, width=3)
    text(d, (W-150, H-80), "01:30:00", font(18), C_TEXT_SEC)
    return d_img

def screen_about():
    d_img, d = new_screen()
    draw_appbar(d, "关于")
    y = 200
    # Logo
    rrect(d, [W//2-60, y, W//2+60, y+120], 0, fill=C_ICON_BOX_B)
    d.ellipse([W//2-40, y+20, W//2+40, y+100], fill=C_AMBER)
    text_centered(d, W//2, y+160, "APPDEX", font(40, bold=True, mono=True), C_TEXT_PRI)
    text_centered(d, W//2, y+210, "多功能 Android 逆向工程工具箱", font(22), C_TEXT_SEC)
    text_centered(d, W//2, y+250, "v1.0.0", font(26, bold=True, mono=True), C_GREEN_BR)
    y += 320
    rrect(d, [42, y, W-42, y+240], 0, fill=None, outline=C_BORDER_LT, width=1)
    about = [("版本","1.0.0"),("协议","Apache 2.0"),("GitHub","github.com/niceyayale/appdex"),("引擎","Jetpack Compose"),("Min Android","8.0 (API 26)")]
    for i, (k, v) in enumerate(about):
        ry = y + 10 + i * 44
        text(d, (54, ry), k, font(22), C_TEXT_PRI)
        text(d, (W//2, ry), v, font(20, mono=True), C_TEXT_SEC)
        if i < len(about)-1:
            draw_divider(d, 54, ry+38, W-54-42)
    return d_img

def screen_ftp():
    d_img, d = new_screen()
    draw_appbar(d, "远程管理", back=True)
    draw_tab_bar(d, 160, ["Web 服务","FTP 客户端"], 1)
    y = 240
    y = draw_section(d, y, "服务器")
    rrect(d, [42, y, W-42, y+200], 0, fill=C_SURFACE_DEEP, outline=C_BORDER_LT, width=1)
    fields = [("主机","192.168.1.100"),("端口","21"),("用户名","admin"),("密码","••••••••")]
    for i, (k, v) in enumerate(fields):
        ry = y + 10 + i * 44
        text(d, (58, ry), k, font(20), C_TEXT_SEC)
        rrect(d, [200, ry, W-58, ry+32], 0, fill=C_SURFACE_IN, outline=C_BORDER_MD, width=1)
        text(d, (212, ry+6), v, font(20, mono=True), C_TEXT_PRI)
    y += 220
    draw_button(d, 84, y, W-168, "连接")
    return d_img

# ── Generate All Screens ──
def generate_all():
    screens = [
        ("Home", screen_home),
        ("Analyzer (Empty)", screen_analyzer_empty),
        ("Analyzer (Data)", screen_analyzer_data),
        ("APK Detail", screen_apk_detail),
        ("Files", screen_files),
        ("Editor", screen_editor),
        ("Terminal", screen_terminal),
        ("Tools", screen_tools),
        ("Settings", screen_settings),
        ("Web Server", screen_webserver),
        ("Signing", screen_signing),
        ("Repack", screen_repack),
        ("HEX Editor", screen_hex),
        ("DEX Browser", screen_dex_browser),
        ("Security Scanner", screen_security),
        ("Size Analyzer", screen_size_analyzer),
        ("APK Diff", screen_diff),
        ("Loading", screen_loading),
        ("Error State", screen_error),
        ("Empty State", screen_empty),
        ("Snackbar", screen_snackbar),
        ("Dialog", screen_dialog),
        ("Hash Calculator", screen_hash_calc),
        ("Encoding Converter", screen_encoding),
        ("JSON Formatter", screen_json_formatter),
        ("Device Info", screen_device_info),
        ("Image Viewer", screen_image_viewer),
        ("Audio Player", screen_audio_player),
        ("Video Player", screen_video_player),
        ("About", screen_about),
        ("FTP Client", screen_ftp),
    ]

    labeled = []
    for name, func in screens:
        img = func()
        img_scaled = img.resize((OW, OH), Image.LANCZOS)
        labeled_img = label_screen(img_scaled, name)
        labeled.append(labeled_img)
        print(f"  ✓ {name}")

    # ── Assemble Overview ──
    rows = math.ceil(len(labeled) / COLS)
    total_w = COLS * OW + (COLS + 1) * SPACING
    total_h = rows * (OH + LABEL_H) + (rows + 1) * SPACING

    # Split into parts if too tall (max ~16000px per part)
    MAX_H = 16000
    rows_per_part = max(1, (MAX_H - SPACING) // (OH + LABEL_H + SPACING))
    parts = math.ceil(rows / rows_per_part)

    for part in range(parts):
        start_row = part * rows_per_part
        end_row = min(start_row + rows_per_part, rows)
        part_rows = end_row - start_row
        part_w = total_w
        part_h = part_rows * (OH + LABEL_H) + (part_rows + 1) * SPACING

        overview = Image.new("RGB", (part_w, part_h), BG_WHITE)
        for r in range(part_rows):
            for c in range(COLS):
                idx = (start_row + r) * COLS + c
                if idx >= len(labeled):
                    break
                x = SPACING + c * (OW + SPACING)
                y = SPACING + r * (OH + LABEL_H + SPACING)
                overview.paste(labeled[idx], (x, y))

        if parts > 1:
            fname = f"AppDex_UI_Overview_Part{part+1}.png"
        else:
            fname = "AppDex_UI_Overview.png"
        out_path = os.path.join("c:\\Users\\guangming\\Desktop\\catpaw\\mt-app", fname)
        overview.save(out_path, "PNG")
        print(f"  📄 Saved: {fname} ({part_w}×{part_h})")

    print(f"\n✅ Done! {len(screens)} screens, {parts} part(s)")

if __name__ == "__main__":
    import sys, io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    print("AppDex UI Mockup Generator")
    print("=" * 50)
    generate_all()
