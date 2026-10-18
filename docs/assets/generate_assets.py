"""Genera el icono y las infografias de JPShopKeeper para la pagina de SpigotMC.

Mismo sistema de diseno que JPPlayerVaults (cabecera con barra verde, tarjetas,
pilas y pie con la serie de versiones), en la identidad verde del plugin.

Uso:   python generate_assets.py
Salida: docs/assets/icon.png  (512x512, sin texto)
        docs/assets/<idioma>/features.png, versions.png, trades.png,
        npcs.png, setup.png, commands.png   (1280x720, sin codigo)
"""

import math
import os

from PIL import Image, ImageDraw, ImageFont

BRAND = "JPShopKeeper"
VERSION = "v1.1.0"
SERIES = "1.8 \u2192 1.21+"
OUT = os.path.dirname(os.path.abspath(__file__))

W, H = 1280, 720

# ------------------------------------------------------------------ paleta
GREEN = (74, 222, 128)
GREEN_SOFT = (32, 138, 74)
TEXT = (236, 246, 238)
MUTED = (156, 178, 162)
DIM = (110, 132, 116)
CARD = (20, 34, 26)
CARD_BORDER = (42, 64, 50)
ICON_BG = (16, 27, 21)
SLOT_BG = (14, 24, 18)
BG_TOP = (13, 21, 17)
BG_BOTTOM = (10, 44, 24)
GOLD = (240, 196, 92)
STEEL = (126, 148, 130)

BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
SEMI = r"C:\Windows\Fonts\seguisb.ttf"
REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

_cache = {}


def font(path, size):
    key = (path, size)
    if key not in _cache:
        try:
            _cache[key] = ImageFont.truetype(path, size)
        except OSError:
            _cache[key] = ImageFont.load_default()
    return _cache[key]


def base(title, subtitle, footer):
    image = Image.new("RGB", (W, H), BG_TOP)
    draw = ImageDraw.Draw(image)
    for y in range(H):
        ratio = y / (H - 1)
        draw.line([(0, y), (W, y)],
                  fill=tuple(round(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * ratio) for i in range(3)))

    texture = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    tdraw = ImageDraw.Draw(texture)
    for x in range(-H, W, 52):
        tdraw.line([(x, H), (x + H, 0)], fill=(255, 255, 255, 6), width=1)
    image = Image.alpha_composite(image.convert("RGBA"), texture)

    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle([80, 74, 146, 82], radius=4, fill=GREEN)
    draw.text((80, 100), title, font=font(BOLD, 52), fill=TEXT)
    draw.text((82, 168), subtitle, font=font(REGULAR, 24), fill=MUTED)

    draw.line([(80, 664), (1200, 664)], fill=(38, 56, 44), width=2)
    draw.text((80, 682), footer, font=font(REGULAR, 17), fill=DIM)
    draw.text((1200, 682), SERIES, font=font(SEMI, 17), fill=GREEN, anchor="ra")
    return image, draw


def wrap(draw, text, fnt, max_width):
    lines, current = [], ""
    for word in text.split():
        trial = (current + " " + word).strip()
        if draw.textlength(trial, font=fnt) <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def card(draw, x, y, w, h, icon_fn, title, body, body_lines=3):
    draw.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=CARD, outline=CARD_BORDER, width=2)
    cx, cy, r = x + 62, y + 62, 30
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ICON_BG, outline=(38, 56, 44), width=2)
    icon_fn(draw, cx, cy, r - 6)
    # El titulo se encoge si no cabe (los textos espanoles son mas largos).
    size = 25
    while size > 18 and draw.textlength(title, font=font(SEMI, size)) > w - 132:
        size -= 1
    draw.text((x + 108, y + 40), title, font=font(SEMI, size), fill=TEXT)
    yy = y + 104
    body_font = font(REGULAR, 18)
    for line in wrap(draw, body, body_font, w - 72)[:body_lines]:
        draw.text((x + 36, yy), line, font=body_font, fill=MUTED)
        yy += 27


def pill(draw, x, y, text, outline=GREEN, color=None, pad=22, height=42, fnt=None):
    fnt = fnt or font(SEMI, 18)
    width = draw.textlength(text, font=fnt) + pad * 2
    draw.rounded_rectangle([x, y, x + width, y + height], radius=height // 2,
                           fill=(17, 29, 22), outline=outline, width=2)
    draw.text((x + width / 2, y + height / 2), text, font=fnt, fill=color or TEXT, anchor="mm")
    return width


def check(draw, x, y, r=13):
    draw.ellipse([x - r, y - r, x + r, y + r], fill=(22, 45, 34), outline=GREEN, width=2)
    draw.line([(x - 5, y), (x - 1, y + 5), (x + 6, y - 5)], fill=GREEN, width=3, joint="curve")


def slot(draw, x, y, size, label=None, glyph=None):
    draw.rounded_rectangle([x, y, x + size, y + size], radius=12, fill=SLOT_BG,
                           outline=(48, 70, 54), width=3)
    draw.rounded_rectangle([x + 8, y + 8, x + size - 8, y + size - 8], radius=8,
                           fill=(11, 19, 15), outline=(30, 46, 36), width=2)
    if glyph:
        glyph(draw, x + size / 2, y + size / 2, size * 0.26)
    if label:
        draw.text((x + size / 2, y + size + 22), label, font=font(REGULAR, 16), fill=DIM, anchor="mm")


# ------------------------------------------------------------------ iconos
def ic_jar(draw, cx, cy, s):
    draw.rounded_rectangle([cx - s * 0.62, cy - s * 0.62, cx + s * 0.62, cy + s * 0.72],
                           radius=5, outline=GREEN, width=3)
    draw.line([(cx - s * 0.62, cy - s * 0.22), (cx + s * 0.62, cy - s * 0.22)], fill=GREEN, width=3)
    draw.rounded_rectangle([cx - s * 0.3, cy - s * 0.88, cx + s * 0.3, cy - s * 0.66],
                           radius=3, fill=GREEN)


def ic_no_deps(draw, cx, cy, s):
    draw.rounded_rectangle([cx - s * 0.6, cy - s * 0.6, cx + s * 0.6, cy + s * 0.6],
                           radius=4, outline=DIM, width=3)
    draw.line([(cx - s * 0.75, cy + s * 0.75), (cx + s * 0.75, cy - s * 0.75)], fill=GREEN, width=4)


def ic_grid(draw, cx, cy, s):
    size = s * 0.62
    for row in range(2):
        for col in range(2):
            x = cx - size + col * (size + s * 0.12)
            y = cy - size + row * (size + s * 0.12)
            draw.rounded_rectangle([x, y, x + size * 0.9, y + size * 0.9], radius=3,
                                   fill=GREEN if (row, col) == (0, 0) else None,
                                   outline=GREEN_SOFT if (row, col) != (0, 0) else GREEN, width=3)


def ic_swap(draw, cx, cy, s):
    draw.line([(cx - s * 0.85, cy - s * 0.32), (cx + s * 0.6, cy - s * 0.32)], fill=GREEN, width=4)
    draw.line([(cx + s * 0.25, cy - s * 0.6), (cx + s * 0.65, cy - s * 0.32), (cx + s * 0.25, cy - s * 0.04)],
              fill=GREEN, width=4, joint="curve")
    draw.line([(cx + s * 0.85, cy + s * 0.32), (cx - s * 0.6, cy + s * 0.32)], fill=GREEN_SOFT, width=4)
    draw.line([(cx - s * 0.25, cy + s * 0.04), (cx - s * 0.65, cy + s * 0.32), (cx - s * 0.25, cy + s * 0.6)],
              fill=GREEN_SOFT, width=4, joint="curve")


def ic_tag(draw, cx, cy, s):
    draw.polygon([(cx - s * 0.75, cy - s * 0.1), (cx - s * 0.1, cy - s * 0.75),
                  (cx + s * 0.78, cy - s * 0.75), (cx + s * 0.78, cy + s * 0.12),
                  (cx + s * 0.1, cy + s * 0.78), (cx - s * 0.75, cy + s * 0.78)],
                 outline=GREEN, width=3)
    draw.ellipse([cx - s * 0.45, cy - s * 0.45, cx - s * 0.1, cy - s * 0.1], fill=GREEN)


def ic_globe(draw, cx, cy, s):
    draw.ellipse([cx - s * 0.78, cy - s * 0.78, cx + s * 0.78, cy + s * 0.78], outline=GREEN, width=3)
    draw.ellipse([cx - s * 0.34, cy - s * 0.78, cx + s * 0.34, cy + s * 0.78], outline=GREEN_SOFT, width=2)
    draw.line([(cx - s * 0.78, cy), (cx + s * 0.78, cy)], fill=GREEN, width=3)


def ic_shield(draw, cx, cy, s):
    points = [(cx, cy - s * 0.85), (cx + s * 0.72, cy - s * 0.5), (cx + s * 0.72, cy + s * 0.15),
              (cx, cy + s * 0.85), (cx - s * 0.72, cy + s * 0.15), (cx - s * 0.72, cy - s * 0.5)]
    draw.polygon(points, fill=(20, 34, 28), outline=GREEN)
    draw.line(points + [points[0]], fill=GREEN, width=3, joint="curve")
    draw.line([(cx - s * 0.34, cy), (cx - s * 0.08, cy + s * 0.28), (cx + s * 0.36, cy - s * 0.3)],
              fill=GREEN, width=4, joint="curve")


def ic_download(draw, cx, cy, s):
    draw.line([(cx, cy - s * 0.8), (cx, cy + s * 0.25)], fill=GREEN, width=4)
    draw.line([(cx - s * 0.42, cy - s * 0.15), (cx, cy + s * 0.3), (cx + s * 0.42, cy - s * 0.15)],
              fill=GREEN, width=4, joint="curve")
    draw.line([(cx - s * 0.8, cy + s * 0.62), (cx + s * 0.8, cy + s * 0.62)], fill=GREEN, width=4)


def ic_villager(draw, cx, cy, s):
    """Cara de aldeano: ceja corrida, ojos y nariz larga. Se lee a cualquier tamano."""
    head_w, head_h = s * 0.74, s * 0.92
    draw.rounded_rectangle([cx - head_w, cy - head_h, cx + head_w, cy + head_h],
                           radius=max(4, int(s * 0.12)), fill=(20, 34, 28), outline=GREEN, width=3)
    draw.line([(cx - head_w * 0.66, cy - head_h * 0.42), (cx + head_w * 0.66, cy - head_h * 0.42)],
              fill=GREEN, width=max(3, int(s * 0.09)))
    eye_w = head_w * 0.28
    eye_h = head_h * 0.14
    for side in (-1, 1):
        eye_cx = cx + side * head_w * 0.38
        draw.rectangle([eye_cx - eye_w / 2, cy - head_h * 0.26, eye_cx + eye_w / 2, cy - head_h * 0.26 + eye_h],
                       fill=GREEN)
    nose_w = head_w * 0.24
    draw.rounded_rectangle([cx - nose_w / 2, cy - head_h * 0.12, cx + nose_w / 2, cy + head_h * 0.62],
                           radius=max(3, int(s * 0.06)), fill=(30, 48, 38), outline=GREEN, width=3)


def ic_edit(draw, cx, cy, s):
    draw.rounded_rectangle([cx - s * 0.8, cy - s * 0.8, cx + s * 0.35, cy + s * 0.8],
                           radius=4, outline=GREEN_SOFT, width=3)
    draw.line([(cx - s * 0.15, cy + s * 0.45), (cx + s * 0.72, cy - s * 0.42)], fill=GREEN, width=4)
    draw.line([(cx - s * 0.4, cy + s * 0.72), (cx - s * 0.1, cy + s * 0.48)], fill=GREEN, width=3)


def ic_terminal(draw, cx, cy, s):
    draw.line([(cx - s * 0.6, cy - s * 0.45), (cx - s * 0.1, cy), (cx - s * 0.6, cy + s * 0.45)],
              fill=GREEN, width=4, joint="curve")
    draw.line([(cx + s * 0.1, cy + s * 0.45), (cx + s * 0.65, cy + s * 0.45)], fill=GREEN, width=4)


def ic_key(draw, cx, cy, s):
    draw.ellipse([cx - s * 0.8, cy - s * 0.55, cx - s * 0.1, cy + s * 0.15], outline=GREEN, width=3)
    draw.line([(cx - s * 0.1, cy - s * 0.2), (cx + s * 0.8, cy + s * 0.7)], fill=GREEN, width=4)
    draw.line([(cx + s * 0.42, cy + s * 0.32), (cx + s * 0.62, cy + s * 0.12)], fill=GREEN, width=4)
    draw.line([(cx + s * 0.66, cy + s * 0.56), (cx + s * 0.86, cy + s * 0.36)], fill=GREEN, width=4)


def ic_coin(draw, cx, cy, s):
    draw.ellipse([cx - s * 0.75, cy - s * 0.75, cx + s * 0.75, cy + s * 0.75], outline=GOLD, width=3)
    draw.ellipse([cx - s * 0.42, cy - s * 0.42, cx + s * 0.42, cy + s * 0.42], fill=(38, 32, 14),
                 outline=GOLD, width=2)


def ic_clock(draw, cx, cy, s):
    draw.ellipse([cx - s * 0.8, cy - s * 0.8, cx + s * 0.8, cy + s * 0.8], outline=GREEN, width=3)
    draw.line([(cx, cy), (cx, cy - s * 0.45)], fill=GREEN, width=3)
    draw.line([(cx, cy), (cx + s * 0.4, cy + s * 0.2)], fill=GREEN, width=3)


def ic_chest(draw, cx, cy, s):
    draw.rounded_rectangle([cx - s * 0.9, cy - s * 0.85, cx + s * 0.9, cy - s * 0.05],
                           radius=5, fill=(20, 34, 28), outline=GREEN, width=3)
    draw.rounded_rectangle([cx - s * 0.9, cy - s * 0.1, cx + s * 0.9, cy + s * 0.9],
                           radius=5, fill=(20, 34, 28), outline=GREEN, width=3)
    draw.line([(cx - s * 0.9, cy - s * 0.07), (cx + s * 0.9, cy - s * 0.07)], fill=GREEN, width=3)
    draw.rounded_rectangle([cx - s * 0.18, cy - s * 0.3, cx + s * 0.18, cy + s * 0.22],
                           radius=3, fill=GREEN)


def ic_emerald(draw, cx, cy, s):
    draw.polygon([(cx, cy - s * 0.85), (cx + s * 0.75, cy - s * 0.15), (cx + s * 0.45, cy + s * 0.8),
                  (cx - s * 0.45, cy + s * 0.8), (cx - s * 0.75, cy - s * 0.15)],
                 fill=(28, 96, 54), outline=GREEN)
    draw.line([(cx - s * 0.75, cy - s * 0.15), (cx + s * 0.75, cy - s * 0.15)], fill=GREEN, width=2)


def ic_bar(draw, cx, cy, s):
    draw.polygon([(cx - s * 0.3, cy - s * 0.7), (cx + s * 0.7, cy - s * 0.45),
                  (cx + s * 0.3, cy + s * 0.7), (cx - s * 0.7, cy + s * 0.45)],
                 fill=(60, 66, 76), outline=STEEL)


ICONS = {
    "jar": ic_jar, "nodeps": ic_no_deps, "grid": ic_grid, "swap": ic_swap, "tag": ic_tag,
    "globe": ic_globe, "shield": ic_shield, "download": ic_download, "villager": ic_villager,
    "edit": ic_edit, "terminal": ic_terminal, "key": ic_key, "coin": ic_coin, "clock": ic_clock,
    "chest": ic_chest, "emerald": ic_emerald, "bar": ic_bar,
}


# ------------------------------------------------------------------ icono
def draw_chest_mark(draw, cx, cy, size, glow_draw=None):
    r = size // 2
    if glow_draw is not None:
        for step in range(10, 0, -1):
            alpha = int(3 + step * 1.4)
            pad = int(size * 0.10) + step * 6
            glow_draw.ellipse([cx - r - pad, cy - r - pad, cx + r + pad, cy + r + pad],
                              fill=(74, 222, 128, alpha))

    outline_w = max(4, size // 85)
    seam = cy - r + int(size * 0.34)          # linea de union tapa / cuerpo
    body_top = seam - int(size * 0.04)

    draw.rounded_rectangle([cx - r, cy - r, cx + r, seam],
                           radius=size // 8, fill=(28, 44, 34), outline=STEEL, width=outline_w)
    draw.rounded_rectangle([cx - r, body_top, cx + r, cy + r],
                           radius=size // 9, fill=(24, 38, 29), outline=STEEL, width=outline_w)
    draw.line([(cx - r + 8, seam - 2), (cx + r - 8, seam - 2)], fill=GREEN, width=outline_w)

    # Bisagras metalicas a los lados de la union.
    band = max(12, size // 10)
    for side in (-1, 1):
        x = cx + side * int(r - size * 0.02)
        draw.rounded_rectangle([x - band // 2, seam - band, x + band // 2, seam + band],
                               radius=band // 2, fill=(46, 66, 52), outline=STEEL, width=2)

    # Pestillo frontal con la luz verde del plugin.
    latch_w = int(size * 0.22)
    latch_h = int(size * 0.30)
    draw.rounded_rectangle([cx - latch_w // 2, seam - latch_h // 2, cx + latch_w // 2, seam + latch_h // 2],
                           radius=latch_w // 5, fill=(22, 34, 26), outline=GREEN, width=max(3, size // 95))
    knob = max(5, size // 45)
    draw.ellipse([cx - knob, seam - knob, cx + knob, seam + knob], fill=GREEN)


def icon():
    size = 512
    image = Image.new("RGB", (size, size), BG_TOP)
    gradient = ImageDraw.Draw(image)
    for y in range(size):
        ratio = y / (size - 1)
        gradient.line([(0, y), (size, y)],
                      fill=tuple(round(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * ratio) for i in range(3)))
    # El Draw se crea despues de convertir: convert() devuelve otra imagen.
    image = image.convert("RGBA")
    draw = ImageDraw.Draw(image)
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_chest_mark(draw, size // 2, size // 2, 300, ImageDraw.Draw(glow))
    image = Image.alpha_composite(image, glow)
    image.convert("RGB").save(os.path.join(OUT, "icon.png"), "PNG", optimize=True)
    print("icon.png", image.size)


# ------------------------------------------------------------------ textos
LANGS = {
    "es": {
        "footer": f"{BRAND}  \u00b7  {VERSION}  \u00b7  MIT  \u00b7  Spigot & Paper",
        "features": {
            "title": "Todo lo que incluye",
            "subtitle": "NPCs comerciantes completos, sin dependencias y editables en el juego.",
            "cards": [
                ("jar", "Un JAR, todas las versiones", "El mismo archivo funciona de la 1.8 a la ultima en Spigot y Paper."),
                ("nodeps", "Sin dependencias", "Ni Citizens, ni NMS, ni librerias externas. Vault es opcional."),
                ("edit", "Editor en menus", "Paginas, tradeos y precios sin tocar un solo archivo."),
                ("swap", "Tradeos completos", "Hasta 3 items de coste, dinero, resultado, permiso y enfriamiento."),
                ("tag", "Soporte NBT", "Tokens y vouchers de otros plugins se guardan tal cual."),
                ("globe", "Espanol e ingles", "Cambia el idioma con una linea de la configuracion."),
            ],
        },
        "modern": {
            "title": "Lo que usan los servidores de hoy",
            "subtitle": "Extras modernos sin romper ni una version antigua.",
            "cards": [
                ("tag", "Colores hex", "&#RRGGBB en todos los textos: nativo en 1.16+ y aproximado en versiones antiguas."),
                ("grid", "CustomModelData", "Texturas de tu resource pack en los iconos de los menus (1.14+)."),
                ("villager", "Cabezas de jugador", "Iconos con la cabeza de un jugador, por su nombre y sin consultas externas."),
                ("edit", "MiniMessage", "Etiquetas como <red> o <gradient> en Paper 1.16+ (opcional, con una linea de config)."),
                ("terminal", "PlaceholderAPI", "Placeholders %jpsk_...% para scoreboards, hologramas y tablists."),
                ("globe", "Titulos y mobs modernos", "Titulos largos en 1.13+ y mobs de versiones nuevas en npc.allowed-types."),
            ],
        },
        "versions": {
            "title": "Un JAR. Todas las versiones.",
            "subtitle": "Compilado contra la API de Spigot 1.8.8 con bytecode Java 8 y sin NMS.",
            "big_left": "1.8", "big_right": "1.21+", "big_note": "y todo lo que hay en medio",
            "tested": "probado", "pills": ["Spigot & Paper", "Java 8", "Sin dependencias", "Sin conexiones de red"],
            "note": "Los materiales, sonidos y entidades se resuelven por nombre en tiempo de ejecucion.",
        },
        "trades": {
            "title": "Cada tradeo, a tu medida",
            "subtitle": "Lo que el jugador entrega, lo que recibe y las reglas que tu decides.",
            "give": "El jugador entrega", "get": "El jugador recibe",
            "money": "monedas", "checks": [
                "Coste de hasta 3 items distintos, con la cantidad que quieras por slot.",
                "Precio en monedas con Vault: lo cobra y lo entrega al instante.",
                "Permiso propio por tradeo: perfecto para ofertas VIP.",
                "Enfriamiento por jugador en segundos, con aviso en vivo.",
                "Comparacion de items configurable: nombre, lore, encantamientos, durabilidad y NBT.",
                "Si el inventario esta lleno, el resultado cae al suelo o se cancela (tu eliges).",
            ],
        },
        "npcs": {
            "title": "NPCs que no se rompen",
            "subtitle": "Mobs nativos con todas las protecciones activadas de fabrica.",
            "checks": [
                "Invulnerables: el dano, el fuego, los portales y los lazos no les afectan.",
                "Sin IA, silenciosos y anclados a su punto: ni el knockback los mueve.",
                "Giran la cabeza hacia el jugador mas cercano (radio configurable).",
                "Si el NPC muere o desaparece, se regenera solo en unos segundos.",
                "Sobreviven a los reinicios: se guardan su UUID y su posicion exacta.",
                "Si una proteccion cancela la aparicion, la consola avisa con las coordenadas.",
            ],
        },
        "setup": {
            "title": "Listo en tres pasos",
            "subtitle": "Sin bases de datos, sin compilaciones y sin configurar nada a mano.",
            "steps": [
                ("download", "1", "Instalar", "Suelta JPShopKeeper-1.1.0.jar en plugins/ y arranca el servidor."),
                ("villager", "2", "Crear el NPC", "Trader create mi_tienda donde quieras el comerciante."),
                ("edit", "3", "Montar las ofertas", "Un clic en un slot, un clic en un item y guardar."),
            ],
            "note": "Opcionales: Vault para el dinero y PlaceholderAPI para los placeholders.",
            "pills": ["config.yml", "messages.yml", "menus.yml", "traders/<id>.yml", "Vault opcional", "PlaceholderAPI opcional"],
        },
        "commands": {
            "title": "Comandos y permisos",
            "subtitle": "Con autocompletado y permisos separados para cada tarea.",
            "rows": [
                ("/trader create <id> [mob]", "Crea el NPC donde estas y abre su panel.", "jpshopkeeper.admin.create"),
                ("/trader edit <id>", "Abre el editor de un trader existente.", "jpshopkeeper.admin.edit"),
                ("/trader remove <id>", "Elimina el trader, su NPC y todos sus datos.", "jpshopkeeper.admin.remove"),
                ("/trader tp <id>", "Te teletransporta al NPC del trader.", "jpshopkeeper.admin.teleport"),
                ("/trader reload", "Recarga configuracion, textos y datos.", "jpshopkeeper.admin.reload"),
            ],
            "chat_title": "asi se ve en el juego",
            "chat": [
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (120, 230, 150), "Tradeo completado: Diamante."),
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (240, 110, 110), "Te faltan 250 monedas (tienes 120)."),
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (240, 110, 110), "Te faltan items: 3x Diamante."),
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (240, 210, 110), "Debes esperar 12 segundos."),
            ],
            "notes": [
                "Todo el texto sale de messages.yml:",
                "\u2022  colores legacy con & (compatible 1.8+)",
                "\u2022  prefijo, avisos y errores a tu gusto",
                "\u2022  espanol e ingles incluidos",
                "\u2022  cada tradeo puede pedir su propio permiso",
                "\u2022  alias /traders y /jpsk",
            ],
        },
    },
    "en": {
        "footer": f"{BRAND}  \u00b7  {VERSION}  \u00b7  MIT  \u00b7  Spigot & Paper",
        "features": {
            "title": "What you get",
            "subtitle": "Complete trade NPCs with no dependencies, edited in game.",
            "cards": [
                ("jar", "One jar, every version", "The same file runs from 1.8 to the latest on Spigot and Paper."),
                ("nodeps", "No dependencies", "No Citizens, no NMS, no extra libraries. Vault is optional."),
                ("edit", "Menu editor", "Pages, trades and prices without touching a single file."),
                ("swap", "Complete trades", "Up to 3 cost items, money, result, permission and cooldown."),
                ("tag", "NBT support", "Tokens and vouchers from other plugins are stored as they are."),
                ("globe", "English & Spanish", "Switch language with one line of the configuration."),
            ],
        },
        "modern": {
            "title": "What today's servers use",
            "subtitle": "Modern extras without breaking a single old version.",
            "cards": [
                ("tag", "Hex colours", "&#RRGGBB everywhere: native on 1.16+, approximated on older versions."),
                ("grid", "CustomModelData", "Resource pack textures for the menu icons (1.14+)."),
                ("villager", "Player heads", "Icons with a player's head, by name and with no external lookups."),
                ("edit", "MiniMessage", "Tags such as <red> or <gradient> on Paper 1.16+ (optional, one config line)."),
                ("terminal", "PlaceholderAPI", "%jpsk_...% placeholders for scoreboards, holograms and tablists."),
                ("globe", "Modern titles & mobs", "Long menu titles on 1.13+ and newer mobs in npc.allowed-types."),
            ],
        },
        "versions": {
            "title": "One jar. Every version.",
            "subtitle": "Compiled against the Spigot 1.8.8 API with Java 8 bytecode and no NMS.",
            "big_left": "1.8", "big_right": "1.21+", "big_note": "and everything in between",
            "tested": "tested", "pills": ["Spigot & Paper", "Java 8", "No dependencies", "No network calls"],
            "note": "Materials, sounds and entities are resolved by name at runtime.",
        },
        "trades": {
            "title": "Every trade, your rules",
            "subtitle": "What the player gives, what the player gets and the rules you decide.",
            "give": "The player gives", "get": "The player gets",
            "money": "coins", "checks": [
                "Up to 3 different cost items, with any amount per slot.",
                "Coin price through Vault: charged and paid instantly.",
                "Its own permission per trade: great for VIP offers.",
                "Per-player cooldown in seconds, with live feedback.",
                "Configurable item matching: name, lore, enchants, durability and NBT.",
                "Full inventory? The result drops on the ground or the trade cancels.",
            ],
        },
        "npcs": {
            "title": "NPCs that never break",
            "subtitle": "Native mobs with every protection enabled out of the box.",
            "checks": [
                "Invulnerable: damage, fire, portals and leads do not affect them.",
                "No AI, silent and anchored: not even knockback moves them.",
                "They look at the nearest player (configurable radius).",
                "If an NPC dies or vanishes, it regenerates itself in seconds.",
                "They survive restarts: UUID and exact position are saved.",
                "If a protection cancels the spawn, the console warns with coordinates.",
            ],
        },
        "setup": {
            "title": "Ready in three steps",
            "subtitle": "No databases, no compiling and nothing to set up by hand.",
            "steps": [
                ("download", "1", "Install", "Drop JPShopKeeper-1.1.0.jar in plugins/ and start the server."),
                ("villager", "2", "Create the NPC", "Trader create my_shop where you want the merchant."),
                ("edit", "3", "Build the offers", "One click on a slot, one click on an item, save."),
            ],
            "note": "Optional: Vault for money and PlaceholderAPI for the placeholders.",
            "pills": ["config.yml", "messages.yml", "menus.yml", "traders/<id>.yml", "Vault optional", "PlaceholderAPI optional"],
        },
        "commands": {
            "title": "Commands and permissions",
            "subtitle": "With tab completion and a separate permission for each task.",
            "rows": [
                ("/trader create <id> [mob]", "Creates the NPC where you stand and opens its panel.", "jpshopkeeper.admin.create"),
                ("/trader edit <id>", "Opens the editor of an existing trader.", "jpshopkeeper.admin.edit"),
                ("/trader remove <id>", "Deletes the trader, its NPC and all its data.", "jpshopkeeper.admin.remove"),
                ("/trader tp <id>", "Teleports you to the trader NPC.", "jpshopkeeper.admin.teleport"),
                ("/trader reload", "Reloads configuration, texts and data.", "jpshopkeeper.admin.reload"),
            ],
            "chat_title": "in-game feedback",
            "chat": [
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (120, 230, 150), "Trade completed: Diamond."),
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (240, 110, 110), "You are missing 250 coins (you have 120)."),
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (240, 110, 110), "You are missing items: 3x Diamond."),
                ((150, 170, 155), "[JPShopKeeper] \u00bb ", (240, 210, 110), "You must wait 12 seconds."),
            ],
            "notes": [
                "Every text comes from messages.yml:",
                "\u2022  legacy & colours (1.8+ compatible)",
                "\u2022  prefix, warnings and errors your way",
                "\u2022  English and Spanish included",
                "\u2022  each trade can require its own permission",
                "\u2022  aliases /traders and /jpsk",
            ],
        },
    },
}


# ------------------------------------------------------------------ laminas
def img_features(lang):
    data = LANGS[lang]["features"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])
    x0, y0, cw, ch, gap = 80, 226, 360, 196, 40
    for index, (icon_key, title, body) in enumerate(data["cards"]):
        col, row = index % 3, index // 3
        card(draw, x0 + col * (cw + gap), y0 + row * (ch + gap), cw, ch, ICONS[icon_key], title, body)
    save(image, lang, "features")


def img_modern(lang):
    data = LANGS[lang]["modern"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])
    x0, y0, cw, ch, gap = 80, 226, 360, 196, 40
    for index, (icon_key, title, body) in enumerate(data["cards"]):
        col, row = index % 3, index // 3
        card(draw, x0 + col * (cw + gap), y0 + row * (ch + gap), cw, ch, ICONS[icon_key], title, body)
    save(image, lang, "modern")


def img_versions(lang):
    data = LANGS[lang]["versions"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])

    big_font = font(BOLD, 60)
    x = 140
    draw.text((x, 200), data["big_left"], font=big_font, fill=TEXT)
    x += draw.textlength(data["big_left"], font=big_font) + 30
    draw.line([(x, 235), (x + 74, 235)], fill=GREEN, width=6)
    draw.polygon([(x + 112, 235), (x + 76, 217), (x + 76, 253)], fill=GREEN)
    x += 150
    draw.text((x, 200), data["big_right"], font=big_font, fill=TEXT)
    x += draw.textlength(data["big_right"], font=big_font) + 42
    draw.text((x, 224), data["big_note"], font=font(REGULAR, 25), fill=MUTED)

    track_y = 388
    draw.rounded_rectangle([140, track_y - 8, 1140, track_y + 8], radius=8,
                           fill=(20, 36, 27), outline=(46, 68, 52), width=2)

    marks = [("1.8", 0.0, True), ("1.12", 0.28, False), ("1.16", 0.52, False),
             ("1.20", 0.78, False), ("1.21+", 1.0, True)]
    for label, position, tested in marks:
        x = 140 + (1140 - 140) * position
        color = GREEN if tested else (86, 108, 92)
        draw.ellipse([x - 11, track_y - 11, x + 11, track_y + 11], fill=(14, 24, 18), outline=color, width=4)
        draw.text((x, track_y + 42), label, font=font(SEMI, 24), fill=TEXT if tested else MUTED, anchor="mm")
        if tested:
            draw.text((x, track_y + 76), data["tested"], font=font(REGULAR, 17), fill=GREEN, anchor="mm")

    x = 140
    for text in data["pills"]:
        x += pill(draw, x, 520, text) + 18
    draw.text((140, 604), data["note"], font=font(REGULAR, 19), fill=DIM)
    save(image, lang, "versions")


def img_trades(lang):
    data = LANGS[lang]["trades"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])

    def panel(x, y, w, h, label):
        draw.rounded_rectangle([x, y, x + w, y + h], radius=18, fill=CARD, outline=CARD_BORDER, width=2)
        draw.text((x + 28, y + 22), label, font=font(SEMI, 22), fill=TEXT)

    panel(80, 232, 320, 214, data["give"])
    panel(880, 232, 320, 214, data["get"])

    slot_size = 74
    glyphs = [ic_emerald, ic_bar, ic_tag]
    for index in range(3):
        slot(draw, 115 + index * (slot_size + 14), 288, slot_size, None, glyphs[index])
    draw.rounded_rectangle([115, 380, 285, 416], radius=18, fill=(17, 29, 22), outline=GOLD, width=2)
    ic_coin(draw, 142, 398, 12)
    draw.text((166, 398), data["money"], font=font(SEMI, 17), fill=GOLD, anchor="lm")

    draw.line([(440, 339), (818, 339)], fill=GREEN, width=5)
    draw.polygon([(860, 339), (824, 320), (824, 358)], fill=GREEN)

    slot(draw, 988, 288, 108, None, ic_chest)

    y = 476
    for text in data["checks"]:
        check(draw, 96, y + 10)
        draw.text((126, y), text, font=font(REGULAR, 19), fill=MUTED)
        y += 32
    save(image, lang, "trades")


def img_npcs(lang):
    data = LANGS[lang]["npcs"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])
    ic_villager(draw, 236, 420, 118)
    y = 250
    for text in data["checks"]:
        check(draw, 430, y + 10)
        draw.text((460, y), text, font=font(REGULAR, 21), fill=TEXT)
        y += 52
    save(image, lang, "npcs")


def img_setup(lang):
    data = LANGS[lang]["setup"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])
    x0, y0, cw, ch, gap = 80, 238, 360, 250, 40
    for index, (icon_key, number, title, body) in enumerate(data["steps"]):
        x = x0 + index * (cw + gap)
        draw.rounded_rectangle([x, y0, x + cw, y0 + ch], radius=18, fill=CARD,
                               outline=CARD_BORDER, width=2)
        cx, cy, r = x + 62, y0 + 62, 30
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=ICON_BG, outline=(38, 56, 44), width=2)
        ICONS[icon_key](draw, cx, cy, r - 6)
        draw.text((x + 108, y0 + 42), title, font=font(SEMI, 22), fill=TEXT)
        draw.text((x + cw - 30, y0 + 38), number, font=font(BOLD, 26), fill=(48, 74, 56), anchor="ra")
        yy = y0 + 116
        for line in wrap(draw, body, font(REGULAR, 19), cw - 72)[:4]:
            draw.text((x + 36, yy), line, font=font(REGULAR, 19), fill=MUTED)
            yy += 28

    draw.text((80, 546), data["note"], font=font(REGULAR, 21), fill=MUTED)
    x = 80
    for text in data["pills"]:
        x += pill(draw, x, 596, text, outline=(48, 70, 54), color=MUTED, fnt=font(SEMI, 17)) + 14
    save(image, lang, "setup")


def img_commands(lang):
    data = LANGS[lang]["commands"]
    image, draw = base(data["title"], data["subtitle"], LANGS[lang]["footer"])

    y = 216
    for command, description, permission in data["rows"]:
        draw.rounded_rectangle([80, y, 700, y + 78], radius=14, fill=CARD,
                               outline=CARD_BORDER, width=2)
        draw.text((112, y + 13), command, font=font(SEMI, 24), fill=GREEN)
        draw.text((112, y + 49), description, font=font(REGULAR, 17), fill=MUTED)
        width = draw.textlength(permission, font=font(REGULAR, 15)) + 30
        draw.rounded_rectangle([700 - 22 - width, y + 23, 700 - 22, y + 55], radius=16,
                               fill=(17, 29, 22), outline=(46, 68, 52), width=2)
        draw.text((700 - 22 - width / 2, y + 39), permission, font=font(REGULAR, 15), fill=MUTED, anchor="mm")
        y += 86

    x0, y0, cw, ch = 760, 232, 440, 380
    draw.rounded_rectangle([x0, y0, x0 + cw, y0 + ch], radius=14, fill=(14, 24, 18),
                           outline=CARD_BORDER, width=2)
    draw.rounded_rectangle([x0, y0, x0 + cw, y0 + 48], radius=14, fill=(19, 32, 23))
    draw.rectangle([x0, y0 + 34, x0 + cw, y0 + 48], fill=(19, 32, 23))
    draw.text((x0 + 22, y0 + 24), data["chat_title"], font=font(SEMI, 17), fill=MUTED, anchor="lm")

    chat_font = font(REGULAR, 16)
    yy = y0 + 74
    for prefix_color, prefix, body_color, body in data["chat"]:
        draw.text((x0 + 22, yy), prefix, font=chat_font, fill=prefix_color)
        offset = draw.textlength(prefix, font=chat_font)
        draw.text((x0 + 22 + offset, yy), body, font=chat_font, fill=body_color)
        yy += 30

    yy += 12
    for index, note in enumerate(data["notes"]):
        draw.text((x0 + 22, yy), note, font=font(SEMI if index == 0 else REGULAR, 16),
                  fill=TEXT if index == 0 else MUTED)
        yy += 26
    save(image, lang, "commands")


def save(image, lang, name):
    folder = os.path.join(OUT, lang)
    os.makedirs(folder, exist_ok=True)
    image.convert("RGB").save(os.path.join(folder, name + ".png"), "PNG", optimize=True)
    print(lang + "/" + name + ".png")


def banner():
    """Cabecera del recurso: misma identidad que las laminas, 1280x720."""
    width, height = 1280, 720
    image = Image.new("RGB", (width, height), BG_TOP)
    draw = ImageDraw.Draw(image)
    for y in range(height):
        ratio = (y / (height - 1)) ** 0.85
        draw.line([(0, y), (width, y)],
                  fill=tuple(round(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * ratio) for i in range(3)))

    cell, gap = 76, 9
    cols, rows = 3, 5
    gx = width - cols * (cell + gap) - 30
    gy = (height - rows * (cell + gap)) // 2
    for row in range(rows):
        for col in range(cols):
            x = gx + col * (cell + gap)
            y = gy + row * (cell + gap)
            fade = 1.0 - (col / (cols + 1.0)) * 0.45
            draw.rounded_rectangle([x, y, x + cell, y + cell], radius=9,
                                   fill=(int(22 * fade + 8), int(78 * fade + 10), int(40 * fade + 12)),
                                   outline=(int(58 * fade + 12), int(150 * fade + 25), int(78 * fade + 22)))

    x0 = 84
    draw.text((x0, 150), "S P I G O T   \u00b7   P A P E R   P L U G I N", font=font(BOLD, 24), fill=GREEN)

    title_font = font(BOLD, 94)
    draw.text((x0 + 3, 197), "JPShopKeeper", font=title_font, fill=(0, 0, 0))
    jp_width = draw.textlength("JP", font=title_font)
    draw.text((x0, 194), "JP", font=title_font, fill=(96, 220, 120))
    draw.text((x0 + jp_width, 194), "ShopKeeper", font=title_font, fill=TEXT)

    draw.text((x0, 340), "NPCs comerciantes con tradeos y paginas", font=font(REGULAR, 30), fill=(200, 218, 204))
    draw.text((x0, 384), "Trade NPCs with pages and offers", font=font(REGULAR, 30), fill=(143, 173, 151))

    chips = ["1.8 \u2192 1.21+", "No Citizens", "Vault optional", "NBT", "Hex + MiniMessage"]
    chip_font = font(SEMI, 18)
    cx, cy = x0, 512
    for text in chips:
        chip_width = draw.textlength(text, font=chip_font)
        draw.rounded_rectangle([cx, cy, cx + chip_width + 38, cy + 48], radius=24,
                               fill=(15, 58, 29), outline=(70, 190, 100), width=2)
        draw.text((cx + 19, cy + 12), text, font=chip_font, fill=(182, 240, 197))
        cx += chip_width + 38 + 12

    draw.text((x0, 636), "Un solo JAR de la 1.8 a la ultima version   \u00b7   Licencia MIT",
              font=font(REGULAR, 20), fill=(122, 152, 130))
    image.convert("RGB").save(os.path.join(OUT, "..", "banner.png"), "PNG", optimize=True)
    print("banner.png")


banner()
icon()
for language in ("es", "en"):
    img_features(language)
    img_modern(language)
    img_versions(language)
    img_trades(language)
    img_npcs(language)
    img_setup(language)
    img_commands(language)
print("listo")
