"""
ARTIFEX MASTERPIECE 3D FORGE ENGINE
Generates professional-grade 3D models and 64x64 pixel art texture atlases
for all 71 Artifex RPG items, ores, blocks, weapons, and accessories.
Follows Blockbench & Minecraft Java Edition 26.3 / 1.21.4+ specifications.
"""

import os
import json
import math
import shutil
from PIL import Image, ImageDraw

SERVER_PACK = r'D:\server\Artifex-ResourcePack'
CLIENT_PACK = r'C:\Users\Anime\AppData\Roaming\ElyPrismLauncher\instances\26.3\minecraft\resourcepacks\Artifex-ResourcePack'

# ==============================================================================
# 1. HUE-SHIFTED PALETTES (5-6 Tones each)
# ==============================================================================
PALETTES = {
    'ember': [
        (38, 7, 5, 255),       # Deep volcanic shadow
        (115, 20, 10, 255),    # Dark red
        (191, 40, 12, 255),    # Magma orange-red
        (245, 90, 16, 255),    # Fiery orange
        (255, 175, 45, 255),   # Bright flame yellow
        (255, 240, 180, 255)   # Specular white-hot
    ],
    'frost': [
        (8, 24, 45, 255),      # Abyssal frost depth
        (18, 55, 95, 255),     # Glacier blue
        (35, 115, 175, 255),   # Ice base
        (75, 185, 240, 255),   # Crystalline cyan
        (165, 235, 255, 255),  # Glint light
        (240, 252, 255, 255)   # Diamond frost gleam
    ],
    'storm': [
        (28, 22, 10, 255),     # Thundercloud shadow
        (75, 58, 20, 255),     # Brass bronze dark
        (155, 125, 30, 255),   # Storm gold base
        (230, 195, 45, 255),   # Lightning gold
        (255, 235, 110, 255),  # Electric yellow
        (255, 255, 215, 255)   # White lightning flash
    ],
    'void': [
        (12, 8, 22, 255),      # Event horizon abyss
        (32, 18, 54, 255),     # Dark void violet
        (65, 30, 105, 255),    # Void purple base
        (115, 55, 180, 255),   # Arcane amethyst
        (180, 105, 245, 255),  # Rift lavender
        (245, 215, 255, 255)   # Celestial void gleam
    ],
    'ruby': [
        (40, 6, 12, 255),      # Deep carmine shadow
        (105, 14, 28, 255),    # Dark garnet
        (185, 25, 48, 255),    # Rich royal ruby
        (240, 50, 80, 255),    # Bright crimson facet
        (255, 125, 150, 255),  # Facet reflection
        (255, 230, 235, 255)   # Specular diamond cut
    ],
    'sapphire': [
        (6, 15, 48, 255),      # Midnight trench blue
        (14, 38, 110, 255),    # Deep royal sapphire
        (25, 75, 185, 255),    # Brilliant cobalt
        (55, 135, 245, 255),   # Azure facet
        (140, 200, 255, 255),  # Sky glint
        (235, 245, 255, 255)   # Crystalline specular
    ],
    'topaz': [
        (45, 22, 4, 255),      # Umber amber shadow
        (110, 58, 10, 255),    # Golden topaz base
        (195, 115, 18, 255),   # Radiant imperial amber
        (245, 165, 35, 255),   # Honey golden facet
        (255, 215, 100, 255),  # Sunlight gleam
        (255, 250, 210, 255)   # Crystal point spark
    ],
    'adamantite': [
        (25, 8, 18, 255),      # Blood iron shadow
        (65, 16, 42, 255),     # Deep crimson iron
        (135, 28, 85, 255),    # Adamantite base
        (205, 55, 130, 255),   # Polished magenta steel
        (250, 125, 185, 255),  # Chiseled bevel light
        (255, 225, 240, 255)   # Titanium glint
    ],
    'mithril': [
        (14, 25, 35, 255),     # Silvery shadow
        (35, 60, 80, 255),     # Pale elven steel
        (75, 115, 145, 255),   # Mithril sheen
        (130, 185, 215, 255),  # Luminous azure silver
        (195, 235, 250, 255),  # Sacred star silver
        (245, 253, 255, 255)   # Pure mithril gleam
    ],
    'runestone': [
        (20, 18, 26, 255),     # Ancient stone shadow
        (45, 42, 56, 255),     # Weathered basalt
        (80, 75, 96, 255),     # Chiseled slate
        (130, 122, 148, 255),  # Stone highlight
        (190, 120, 240, 255),  # Glowing rune purple
        (240, 210, 255, 255)   # Rune magic core
    ],
    'gold': [
        (48, 30, 8, 255),      # Ancient bronze shadow
        (105, 68, 14, 255),    # Dark gold
        (185, 132, 25, 255),   # Royal gold base
        (245, 192, 45, 255),   # Polished gold
        (255, 235, 120, 255),  # Gold rim light
        (255, 252, 220, 255)   # Pure gold glint
    ],
    'steel': [
        (18, 22, 30, 255),     # Carbon iron shadow
        (38, 46, 60, 255),     # Gunmetal
        (72, 84, 105, 255),    # Forged steel base
        (125, 142, 168, 255),  # Bevel edge
        (190, 205, 225, 255),  # Razor shine
        (255, 255, 255, 255)   # Mirror reflection
    ],
    'nature': [
        (6, 28, 14, 255),      # Deep forest shadow
        (16, 68, 32, 255),     # Jade dark
        (35, 125, 60, 255),    # Emerald moss base
        (65, 195, 95, 255),    # Living leaf green
        (145, 245, 165, 255),  # Druidic light
        (230, 255, 235, 255)   # Spring dew shimmer
    ],
    'dark_wood': [
        (22, 14, 10, 255),
        (46, 28, 18, 255),
        (82, 52, 32, 255),
        (124, 82, 50, 255),
        (170, 120, 78, 255),
        (215, 165, 120, 255)
    ],
    'parchment': [
        (65, 52, 38, 255),
        (115, 96, 72, 255),
        (175, 150, 115, 255),
        (220, 198, 160, 255),
        (245, 230, 200, 255),
        (255, 248, 230, 255)
    ]
}

STONE_DARK = (30, 30, 36, 255)
STONE_MID = (54, 54, 62, 255)
STONE_LIGHT = (88, 88, 98, 255)
DEEPSLATE_DARK = (16, 16, 22, 255)
DEEPSLATE_MID = (32, 32, 40, 255)
DEEPSLATE_LIGHT = (56, 56, 68, 255)

# ==============================================================================
# 2. ADVANCED PIXEL ART TEXTURE GENERATOR
# ==============================================================================
def draw_gradient_rect(draw, box, col_top, col_bot):
    x0, y0, x1, y1 = box
    h = max(1, y1 - y0)
    for y in range(y0, y1 + 1):
        t = (y - y0) / float(h)
        r = int(col_top[0] * (1 - t) + col_bot[0] * t)
        g = int(col_top[1] * (1 - t) + col_bot[1] * t)
        b = int(col_top[2] * (1 - t) + col_bot[2] * t)
        draw.line([(x0, y), (x1, y)], fill=(r, g, b, 255))

def create_atlas_texture(main_theme, secondary_theme='steel', accent_theme='gold', glow_theme=None):
    """
    Creates a 64x64 texture atlas divided into 4 specialized zones:
    Zone 0: [0..31, 0..31]   - Primary material (body, blade, metal face)
    Zone 1: [32..63, 0..31]  - Secondary material (grip, shaft, base)
    Zone 2: [0..31, 32..63]  - Accent material (guard, gold inlays, runes)
    Zone 3: [32..63, 32..63] - Glow / Facet highlight (crystals, power cores)
    """
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    p_main = PALETTES.get(main_theme, PALETTES['ember'])
    p_sec = PALETTES.get(secondary_theme, PALETTES['steel'])
    p_acc = PALETTES.get(accent_theme, PALETTES['gold'])
    p_glow = PALETTES.get(glow_theme if glow_theme else main_theme, p_main)

    # 1. ZONE 0: Primary Material [0..31, 0..31]
    draw_gradient_rect(draw, (0, 0, 31, 31), p_main[4], p_main[1])
    # Bevel borders
    draw.line([(0, 0), (31, 0)], fill=p_main[5])
    draw.line([(0, 0), (0, 31)], fill=p_main[5])
    draw.line([(31, 0), (31, 31)], fill=p_main[0])
    draw.line([(0, 31), (31, 31)], fill=p_main[0])
    # Specular diagonal gleam
    for d in range(6, 16):
        draw.point((d, d), fill=p_main[5])
    # Inner fuller/core
    draw.rectangle([8, 8, 23, 23], fill=p_main[2], outline=p_main[3])

    # 2. ZONE 1: Secondary Material [32..63, 0..31] (Grip / Shaft / Bevel)
    draw_gradient_rect(draw, (32, 0, 63, 31), p_sec[3], p_sec[0])
    # Ribbed wraps / bindings
    for y in range(2, 30, 4):
        draw.line([(33, y), (62, y)], fill=p_sec[4])
        draw.line([(33, y + 1), (62, y + 1)], fill=p_sec[1])
    draw.rectangle([32, 0, 63, 31], outline=p_sec[0])

    # 3. ZONE 2: Accent Material [0..31, 32..63] (Gold / Rune / Filigree)
    draw_gradient_rect(draw, (0, 32, 31, 63), p_acc[4], p_acc[1])
    draw.rectangle([0, 32, 31, 63], outline=p_acc[5])
    # Inscribed runes / filigree pattern
    draw.line([(6, 38), (25, 57)], fill=p_acc[5], width=1)
    draw.line([(25, 38), (6, 57)], fill=p_acc[5], width=1)
    draw.ellipse([11, 43, 20, 52], outline=p_acc[5], fill=p_acc[3])

    # 4. ZONE 3: Glow / Special [32..63, 32..63] (Crystalline Gem Facets)
    draw_gradient_rect(draw, (32, 32, 63, 63), p_glow[3], p_glow[0])
    # Faceted crystal diamond in center
    poly = [(48, 34), (60, 48), (48, 61), (35, 48)]
    draw.polygon(poly, fill=p_glow[2], outline=p_glow[4])
    # Inner bright star
    draw.line([(48, 38), (48, 58)], fill=p_glow[5], width=2)
    draw.line([(38, 48), (58, 48)], fill=p_glow[5], width=2)
    draw.point((48, 48), fill=(255, 255, 255, 255))

    return img

def create_ore_texture(ore_name, theme, is_deepslate=False):
    """
    Creates a 64x64 HD Ore Texture matching the CurseForge reference:
    Rich stone/deepslate rock strata with multi-faceted protruding crystal clusters and veins.
    """
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    s_dark = DEEPSLATE_DARK if is_deepslate else STONE_DARK
    s_mid = DEEPSLATE_MID if is_deepslate else STONE_MID
    s_light = DEEPSLATE_LIGHT if is_deepslate else STONE_LIGHT

    p = PALETTES.get(theme, PALETTES['ember'])

    # 1. Base rock background with strata
    for y in range(64):
        for x in range(64):
            # Noise-like rock texture
            val = ((x * 7 + y * 13 + (x ^ y)) % 17) / 17.0
            if val < 0.35:
                col = s_dark
            elif val < 0.75:
                col = s_mid
            else:
                col = s_light
            draw.point((x, y), fill=col)

    # 2. Rock cracks and fissures
    cracks = [
        [(10, 8), (22, 18), (35, 22), (52, 16)],
        [(14, 42), (28, 46), (44, 40), (56, 50)],
        [(25, 25), (32, 36), (28, 48)]
    ]
    for crack in cracks:
        draw.line(crack, fill=(s_dark[0]//2, s_dark[1]//2, s_dark[2]//2, 255), width=2)

    # 3. Crystal Clusters & Veins (CurseForge style multi-faceted glowing gems)
    gem_nodes = [
        (18, 18, 9),
        (46, 22, 11),
        (26, 44, 12),
        (48, 48, 8),
        (34, 30, 7)
    ]
    for cx, cy, rad in gem_nodes:
        # Dark border/recess
        draw.ellipse([cx - rad - 2, cy - rad - 2, cx + rad + 2, cy + rad + 2], fill=s_dark)
        # Gem body
        draw.ellipse([cx - rad, cy - rad, cx + rad, cy + rad], fill=p[1], outline=p[3])
        # Facets
        draw.polygon([(cx, cy - rad), (cx + rad, cy), (cx, cy + rad), (cx - rad, cy)], fill=p[2], outline=p[4])
        # Inner facet
        half = rad // 2
        draw.polygon([(cx, cy - half), (cx + half, cy), (cx, cy + half), (cx - half, cy)], fill=p[3])
        # Specular glint
        draw.rectangle([cx - 1, cy - rad + 2, cx + 1, cy - 1], fill=p[5])
        draw.point((cx, cy - 2), fill=(255, 255, 255, 255))

    return img

def create_storage_block_texture(block_name, theme):
    """Creates a 64x64 HD storage metal/crystal block texture with heavy beveled border & emblem."""
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    p = PALETTES.get(theme, PALETTES['ember'])

    # Background gradient
    draw_gradient_rect(draw, (0, 0, 63, 63), p[3], p[1])

    # Outer 4px beveled rim
    for i in range(4):
        draw.rectangle([i, i, 63 - i, 63 - i], outline=p[4 if i == 0 else 2])
    draw.line([(0, 0), (63, 0)], fill=p[5], width=2)
    draw.line([(0, 0), (0, 63)], fill=p[5], width=2)
    draw.line([(63, 0), (63, 63)], fill=p[0], width=2)
    draw.line([(0, 63), (63, 63)], fill=p[0], width=2)

    # 4 Heavy corner rivets
    corners = [(4, 4), (54, 4), (4, 54), (54, 54)]
    for cx, cy in corners:
        draw.rectangle([cx, cy, cx + 5, cy + 5], fill=p[4], outline=p[5])
        draw.point((cx + 2, cy + 2), fill=p[0])

    # Inner recessed panel
    draw_gradient_rect(draw, (12, 12, 51, 51), p[2], p[0])
    draw.rectangle([12, 12, 51, 51], outline=p[4])

    # Central emblem diamond
    draw.polygon([(32, 18), (46, 32), (32, 46), (18, 32)], fill=p[3], outline=p[5])
    draw.polygon([(32, 23), (41, 32), (32, 41), (23, 32)], fill=p[4])
    draw.point((32, 32), fill=(255, 255, 255, 255))

    return img

print("Texture generator initialized.")
# ==============================================================================
# 3. 3D MODEL GEOMETRY BUILDER (Blockbench & Minecraft 26.3 compliant)
# ==============================================================================
UV_ZONES = {
    0: [0.0, 0.0, 8.0, 8.0],     # Primary body / blade / stone
    1: [8.0, 0.0, 16.0, 8.0],    # Secondary grip / bevel / base
    2: [0.0, 8.0, 8.0, 16.0],    # Accent gold / filigree / runes
    3: [8.0, 8.0, 16.0, 16.0]    # Glow / crystal / core
}

def make_el(name, p_from, p_to, uv=0, rot_axis=None, rot_angle=0.0, rot_origin=None):
    uv_coords = UV_ZONES.get(uv, uv if isinstance(uv, list) else [0.0, 0.0, 16.0, 16.0])
    # clamp uv to 0..16
    uv_coords = [max(0.0, min(16.0, float(x))) for x in uv_coords]

    faces = {}
    for face in ['north', 'east', 'south', 'west', 'up', 'down']:
        faces[face] = {'uv': uv_coords, 'texture': '#0'}

    el = {
        'name': name,
        'from': [round(x, 2) for x in p_from],
        'to': [round(x, 2) for x in p_to],
        'faces': faces
    }
    if rot_axis and rot_angle != 0.0:
        el['rotation'] = {
            'origin': [round(x, 2) for x in (rot_origin if rot_origin else p_from)],
            'axis': rot_axis,
            'angle': float(rot_angle)
        }
    return el

# Display templates
DISPLAY_WEAPON = {
    "thirdperson_righthand": { "rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85] },
    "thirdperson_lefthand": { "rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85] },
    "firstperson_righthand": { "rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68] },
    "firstperson_lefthand": { "rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68] },
    "gui": { "rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.75, 0.75, 0.75] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.5, 0.5, 0.5] },
    "fixed": { "rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8] }
}

DISPLAY_INGOT = {
    "thirdperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 2.5, 1.0], "scale": [0.65, 0.65, 0.65] },
    "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 3.0, 1.0], "scale": [0.65, 0.65, 0.65] },
    "gui": { "rotation": [30, 225, 0], "translation": [0, 1.5, 0], "scale": [0.95, 0.95, 0.95] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.6, 0.6, 0.6] },
    "fixed": { "rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8] }
}

DISPLAY_GEM = {
    "thirdperson_righthand": { "rotation": [0, 0, 0], "translation": [0, 3.0, 1.0], "scale": [0.7, 0.7, 0.7] },
    "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 3.0, 1.0], "scale": [0.7, 0.7, 0.7] },
    "gui": { "rotation": [30, 45, 0], "translation": [0, 0.5, 0], "scale": [0.9, 0.9, 0.9] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.6, 0.6, 0.6] },
    "fixed": { "rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8] }
}

DISPLAY_JEWELRY = {
    "thirdperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 2.5, 1.0], "scale": [0.7, 0.7, 0.7] },
    "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 2.5, 1.0], "scale": [0.7, 0.7, 0.7] },
    "gui": { "rotation": [15, -25, -10], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.6, 0.6, 0.6] },
    "fixed": { "rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85] },
    "head": { "rotation": [0, 0, 0], "translation": [0, 6.0, -4.5], "scale": [0.85, 0.85, 0.85] }
}

DISPLAY_BLOCK_ITEM = {
    "gui": { "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625] },
    "ground": { "rotation": [0, 0, 0], "translation": [0, 3.0, 0], "scale": [0.375, 0.375, 0.375] },
    "thirdperson_righthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375] },
    "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4] },
    "fixed": { "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5] }
}

def build_model_dict(elements, display, texture_name):
    return {
        "credit": "Artifex Masterpiece 3D Engine",
        "texture_size": [64, 64],
        "textures": {
            "0": f"artifex:item/{texture_name}",
            "particle": f"artifex:item/{texture_name}"
        },
        "elements": elements,
        "display": display
    }

# ==============================================================================
# GEOMETRY GENERATORS
# ==============================================================================

def geom_ingot(tex_name):
    """9 elements: Trapezoidal bullion bar with 4 beveled slopes & stamped rune seal."""
    elems = [
        make_el("base_plate", [2.5, 4.0, 3.5], [13.5, 5.5, 12.5], uv=0),
        make_el("body", [3.2, 5.5, 4.2], [12.8, 7.5, 11.8], uv=0),
        make_el("top_deck", [4.0, 7.5, 5.0], [12.0, 8.8, 11.0], uv=0),
        # 4 Beveled chamfers
        make_el("bevel_north", [4.0, 5.5, 3.8], [12.0, 8.0, 5.0], uv=1, rot_axis='x', rot_angle=45.0, rot_origin=[8.0, 5.5, 5.0]),
        make_el("bevel_south", [4.0, 5.5, 11.0], [12.0, 8.0, 12.2], uv=1, rot_axis='x', rot_angle=-45.0, rot_origin=[8.0, 5.5, 11.0]),
        make_el("bevel_west", [3.0, 5.5, 4.5], [4.2, 8.0, 11.5], uv=1, rot_axis='z', rot_angle=-45.0, rot_origin=[4.0, 5.5, 8.0]),
        make_el("bevel_east", [11.8, 5.5, 4.5], [13.0, 8.0, 11.5], uv=1, rot_axis='z', rot_angle=45.0, rot_origin=[12.0, 5.5, 8.0]),
        # Central Stamped Insignia Seal
        make_el("insignia_seal", [6.0, 8.7, 6.5], [10.0, 9.3, 9.5], uv=2),
        make_el("insignia_core", [7.0, 9.2, 7.5], [9.0, 9.5, 8.5], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_INGOT, tex_name)

def geom_faceted_gem(tex_name):
    """10 elements: Octahedral brilliant-cut gemstone with table, crown, and pavilion."""
    elems = [
        make_el("table_core", [5.5, 7.0, 5.5], [10.5, 9.0, 10.5], uv=3),
        make_el("girdle_band", [4.5, 6.0, 4.5], [11.5, 7.0, 11.5], uv=0),
        # Crown Facets (slanted 45 deg)
        make_el("crown_north", [5.5, 7.0, 4.2], [10.5, 8.8, 5.5], uv=3, rot_axis='x', rot_angle=-45.0, rot_origin=[8.0, 7.0, 5.5]),
        make_el("crown_south", [5.5, 7.0, 10.5], [10.5, 8.8, 11.8], uv=3, rot_axis='x', rot_angle=45.0, rot_origin=[8.0, 7.0, 10.5]),
        make_el("crown_west", [4.2, 7.0, 5.5], [5.5, 8.8, 10.5], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[5.5, 7.0, 8.0]),
        make_el("crown_east", [10.5, 7.0, 5.5], [11.8, 8.8, 10.5], uv=3, rot_axis='z', rot_angle=-45.0, rot_origin=[10.5, 7.0, 8.0]),
        # Pavilion Facets (tapering to lower culet)
        make_el("pavilion_north", [5.5, 4.2, 4.8], [10.5, 6.0, 6.0], uv=0, rot_axis='x', rot_angle=45.0, rot_origin=[8.0, 6.0, 6.0]),
        make_el("pavilion_south", [5.5, 4.2, 10.0], [10.5, 6.0, 11.2], uv=0, rot_axis='x', rot_angle=-45.0, rot_origin=[8.0, 6.0, 10.0]),
        make_el("pavilion_west", [4.8, 4.2, 5.5], [6.0, 6.0, 10.5], uv=0, rot_axis='z', rot_angle=-45.0, rot_origin=[6.0, 6.0, 8.0]),
        make_el("pavilion_east", [10.0, 4.2, 5.5], [11.2, 6.0, 10.5], uv=0, rot_axis='z', rot_angle=45.0, rot_origin=[10.0, 6.0, 8.0])
    ]
    return build_model_dict(elems, DISPLAY_GEM, tex_name)

def geom_raw_cluster(tex_name):
    """9 elements: Stone mineral matrix with protruding asymmetric crystal spires."""
    elems = [
        make_el("rock_base", [4.0, 2.0, 4.0], [12.0, 6.0, 12.0], uv=1),
        make_el("matrix_chunk", [5.0, 5.5, 5.0], [11.0, 8.5, 11.0], uv=1),
        # Central Main Crystal Spire
        make_el("spire_main", [6.5, 7.5, 6.5], [9.5, 13.5, 9.5], uv=3),
        make_el("spire_tip", [7.0, 13.0, 7.0], [9.0, 15.0, 9.0], uv=3),
        # Angled Crystal Cluster 1 (22.5 deg on Z)
        make_el("crystal_left", [4.5, 6.0, 6.5], [7.0, 11.5, 9.0], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[6.0, 6.0, 7.5]),
        # Angled Crystal Cluster 2 (-22.5 deg on X)
        make_el("crystal_north", [6.5, 6.0, 4.5], [9.5, 11.0, 7.0], uv=0, rot_axis='x', rot_angle=-22.5, rot_origin=[8.0, 6.0, 6.0]),
        # Angled Crystal Cluster 3 (45 deg on X)
        make_el("crystal_south", [6.8, 5.5, 9.5], [9.2, 10.0, 11.5], uv=3, rot_axis='x', rot_angle=45.0, rot_origin=[8.0, 5.5, 9.5]),
        # Angled Crystal Cluster 4 (-45 deg on Z)
        make_el("crystal_right", [9.5, 5.5, 6.8], [11.5, 10.0, 9.2], uv=0, rot_axis='z', rot_angle=-45.0, rot_origin=[9.5, 5.5, 8.0]),
        make_el("glowing_vein", [6.0, 4.5, 6.0], [10.0, 6.5, 10.0], uv=2)
    ]
    return build_model_dict(elems, DISPLAY_GEM, tex_name)

def geom_ring(tex_name):
    """14 elements: 8-segment circular signet band, 4 prongs collet, and faceted gemstone."""
    elems = [
        # 4 Orthogonal Ring Segments
        make_el("band_bottom", [6.0, 2.5, 6.5], [10.0, 4.0, 9.5], uv=2),
        make_el("band_top",    [6.0, 8.5, 6.5], [10.0, 10.0, 9.5], uv=2),
        make_el("band_left",   [3.0, 5.0, 6.5], [4.5, 8.0, 9.5], uv=2),
        make_el("band_right",  [11.5, 5.0, 6.5], [13.0, 8.0, 9.5], uv=2),
        # 4 Corner 45 deg Curvature Segments (Z axis rotation)
        make_el("band_bl", [4.0, 3.2, 6.5], [5.8, 5.2, 9.5], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[5.0, 4.0, 8.0]),
        make_el("band_br", [10.2, 3.2, 6.5], [12.0, 5.2, 9.5], uv=2, rot_axis='z', rot_angle=-45.0, rot_origin=[11.0, 4.0, 8.0]),
        make_el("band_tl", [4.0, 7.8, 6.5], [5.8, 9.8, 9.5], uv=2, rot_axis='z', rot_angle=-45.0, rot_origin=[5.0, 9.0, 8.0]),
        make_el("band_tr", [10.2, 7.8, 6.5], [12.0, 9.8, 9.5], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[11.0, 9.0, 8.0]),
        # Gem Collet Setting
        make_el("gem_collet", [5.5, 9.5, 6.0], [10.5, 11.0, 10.0], uv=2),
        # 4 Prongs / Claws
        make_el("prong_nw", [5.2, 10.5, 5.8], [6.2, 12.8, 6.8], uv=2),
        make_el("prong_ne", [9.8, 10.5, 5.8], [10.8, 12.8, 6.8], uv=2),
        make_el("prong_sw", [5.2, 10.5, 9.2], [6.2, 12.8, 10.2], uv=2),
        make_el("prong_se", [9.8, 10.5, 9.2], [10.8, 12.8, 10.2], uv=2),
        # Faceted Royal Gemstone Center
        make_el("gemstone", [6.0, 10.8, 6.5], [10.0, 13.5, 9.5], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_JEWELRY, tex_name)

def geom_amulet(tex_name):
    """14 elements: Octagonal medallion bezel, suspension bail, draped chain, and cut gem."""
    elems = [
        # Medallion Backing
        make_el("medallion_back", [4.5, 4.5, 7.0], [11.5, 11.5, 8.0], uv=1),
        # 4 Orthogonal Bezel Walls
        make_el("bezel_top",    [5.5, 11.0, 7.5], [10.5, 12.2, 8.8], uv=2),
        make_el("bezel_bottom", [5.5, 3.8, 7.5], [10.5, 5.0, 8.8], uv=2),
        make_el("bezel_left",   [3.8, 5.5, 7.5], [5.0, 10.5, 8.8], uv=2),
        make_el("bezel_right",  [11.0, 5.5, 7.5], [12.2, 10.5, 8.8], uv=2),
        # 4 Corner 45 deg Bevels
        make_el("bezel_tl", [4.0, 10.5, 7.5], [5.5, 12.0, 8.8], uv=2, rot_axis='z', rot_angle=-45.0, rot_origin=[4.8, 11.0, 8.0]),
        make_el("bezel_tr", [10.5, 10.5, 7.5], [12.0, 12.0, 8.8], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[11.2, 11.0, 8.0]),
        make_el("bezel_bl", [4.0, 4.0, 7.5], [5.5, 5.5, 8.8], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[4.8, 5.0, 8.0]),
        make_el("bezel_br", [10.5, 4.0, 7.5], [12.0, 5.5, 8.8], uv=2, rot_axis='z', rot_angle=-45.0, rot_origin=[11.2, 5.0, 8.0]),
        # Central Elevated Gemstone
        make_el("center_gem", [6.0, 6.0, 8.0], [10.0, 10.0, 9.5], uv=3),
        # Suspension Bail
        make_el("bail_loop", [7.0, 12.2, 7.2], [9.0, 14.0, 8.5], uv=2),
        # Draped Chain Links
        make_el("chain_left",  [3.5, 12.5, 7.5], [7.2, 14.5, 8.2], uv=2, rot_axis='z', rot_angle=22.5, rot_origin=[7.0, 13.5, 7.8]),
        make_el("chain_right", [8.8, 12.5, 7.5], [12.5, 14.5, 8.2], uv=2, rot_axis='z', rot_angle=-22.5, rot_origin=[9.0, 13.5, 7.8]),
        make_el("rune_glow",   [7.0, 7.0, 9.4], [9.0, 9.0, 9.8], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_JEWELRY, tex_name)

def geom_runic_slate(tex_name):
    """10 elements: Chiseled stone tablet with 4 corner chamfers, raised border & floating rune."""
    elems = [
        make_el("stone_slab", [3.5, 2.5, 7.0], [12.5, 13.5, 9.0], uv=0),
        # 4 Corner Chamfers
        make_el("chamfer_tl", [2.8, 12.5, 7.0], [4.5, 14.2, 9.0], uv=1, rot_axis='z', rot_angle=-45.0, rot_origin=[3.5, 13.0, 8.0]),
        make_el("chamfer_tr", [11.5, 12.5, 7.0], [13.2, 14.2, 9.0], uv=1, rot_axis='z', rot_angle=45.0, rot_origin=[12.5, 13.0, 8.0]),
        make_el("chamfer_bl", [2.8, 1.8, 7.0], [4.5, 3.5, 9.0], uv=1, rot_axis='z', rot_angle=45.0, rot_origin=[3.5, 3.0, 8.0]),
        make_el("chamfer_br", [11.5, 1.8, 7.0], [13.2, 3.5, 9.0], uv=1, rot_axis='z', rot_angle=-45.0, rot_origin=[12.5, 3.0, 8.0]),
        # Raised Edge Border
        make_el("rim_left",   [3.0, 3.5, 8.8], [4.2, 12.5, 9.5], uv=1),
        make_el("rim_right",  [11.8, 3.5, 8.8], [13.0, 12.5, 9.5], uv=1),
        make_el("rim_top",    [4.0, 12.2, 8.8], [12.0, 13.2, 9.5], uv=1),
        make_el("rim_bottom", [4.0, 2.8, 8.8], [12.0, 3.8, 9.5], uv=1),
        # 3D Inscribed Glowing Rune Sigil
        make_el("carved_rune", [5.5, 5.0, 8.9], [10.5, 11.0, 9.8], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_INGOT, tex_name)

def geom_sword(tex_name):
    """16 elements: Pommel, wrapped grip, winged quillon guard, 3-segment curved blade, edges."""
    elems = [
        # Pommel
        make_el("pommel_core", [7.0, 0.5, 7.0], [9.0, 2.2, 9.0], uv=2),
        make_el("pommel_ring", [6.8, 2.0, 6.8], [9.2, 2.8, 9.2], uv=2),
        # Grip
        make_el("grip_shaft", [7.2, 2.8, 7.2], [8.8, 7.5, 8.8], uv=1),
        make_el("grip_collar", [6.8, 7.2, 6.8], [9.2, 8.0, 9.2], uv=2),
        # Crossguard Center & Gem
        make_el("guard_block", [5.8, 8.0, 6.5], [10.2, 9.2, 9.5], uv=2),
        make_el("guard_gem",   [7.2, 8.2, 6.2], [8.8, 9.0, 9.8], uv=3),
        # Winged Quillons (45 deg)
        make_el("guard_wing_left", [3.5, 8.5, 7.0], [6.0, 9.5, 9.0], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[5.8, 8.6, 8.0]),
        make_el("guard_wing_right", [10.0, 8.5, 7.0], [12.5, 9.5, 9.0], uv=2, rot_axis='z', rot_angle=-45.0, rot_origin=[10.2, 8.6, 8.0]),
        # Blade Segment 1 (Ricasso & Lower Fuller) - 0 deg
        make_el("blade_seg1", [7.0, 9.2, 7.5], [9.0, 14.5, 8.5], uv=0),
        make_el("edge_left1",  [6.4, 9.5, 7.7], [7.1, 14.5, 8.3], uv=0),
        make_el("edge_right1", [8.9, 9.5, 7.7], [9.6, 14.5, 8.3], uv=0),
        # Blade Segment 2 (Curved midsection) - 22.5 deg
        make_el("blade_seg2", [7.0, 14.3, 7.5], [9.0, 20.0, 8.5], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 14.5, 8.0]),
        make_el("edge_left2",  [6.4, 14.5, 7.7], [7.1, 20.0, 8.3], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 14.5, 8.0]),
        make_el("edge_right2", [8.9, 14.5, 7.7], [9.6, 20.0, 8.3], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 14.5, 8.0]),
        # Blade Segment 3 (Curved tip) - 45 deg
        make_el("blade_tip", [7.2, 19.5, 7.6], [8.8, 24.5, 8.4], uv=0, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 19.5, 8.0]),
        make_el("blade_point", [7.5, 24.2, 7.8], [8.5, 26.0, 8.2], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 19.5, 8.0])
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_pickaxe(tex_name):
    """14 elements: Spiked pommel, wrapped shaft, head bracket, dual curved horns & magma core."""
    elems = [
        make_el("pommel_spike", [7.5, 0.5, 7.5], [8.5, 2.0, 8.5], uv=2),
        make_el("shaft_lower",  [7.3, 2.0, 7.3], [8.7, 8.0, 8.7], uv=1),
        make_el("shaft_mid",    [7.3, 8.0, 7.3], [8.7, 14.0, 8.7], uv=1),
        make_el("shaft_upper",  [7.3, 14.0, 7.3], [8.7, 18.0, 8.7], uv=1),
        # Head Bracket & Magma Core
        make_el("head_bracket", [6.2, 17.5, 6.2], [9.8, 19.8, 9.8], uv=2),
        make_el("magma_core",   [7.0, 18.0, 5.8], [9.0, 19.5, 10.2], uv=3),
        # Left Pick Horn (Curved 22.5 & 45 deg)
        make_el("horn_left_base", [4.5, 18.0, 7.2], [6.5, 19.5, 8.8], uv=0),
        make_el("horn_left_mid",  [1.8, 17.0, 7.3], [4.8, 19.0, 8.7], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[4.5, 18.5, 8.0]),
        make_el("horn_left_tip",  [-1.0, 14.5, 7.5], [2.2, 17.0, 8.5], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[2.0, 17.0, 8.0]),
        # Right Pick Horn (Curved -22.5 & -45 deg)
        make_el("horn_right_base", [9.5, 18.0, 7.2], [11.5, 19.5, 8.8], uv=0),
        make_el("horn_right_mid",  [11.2, 17.0, 7.3], [14.2, 19.0, 8.7], uv=0, rot_axis='z', rot_angle=-22.5, rot_origin=[11.5, 18.5, 8.0]),
        make_el("horn_right_tip",  [13.8, 14.5, 7.5], [17.0, 17.0, 8.5], uv=3, rot_axis='z', rot_angle=-45.0, rot_origin=[14.0, 17.0, 8.0]),
        # Top Reinforcing Crest
        make_el("top_crest", [7.0, 19.8, 7.0], [9.0, 22.0, 9.0], uv=2),
        make_el("top_spike", [7.5, 22.0, 7.5], [8.5, 23.5, 8.5], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_battleaxe(tex_name):
    """16 elements: Double-headed bearded battleaxe with runic socket & top thrusting spear."""
    elems = [
        make_el("shaft", [7.3, 1.0, 7.3], [8.7, 18.5, 8.7], uv=1),
        make_el("butt_cap", [6.8, 0.5, 6.8], [9.2, 1.8, 9.2], uv=2),
        make_el("grip_binding", [7.0, 4.0, 7.0], [9.0, 10.0, 9.0], uv=2),
        # Axe Socket & Ice Gem
        make_el("axe_socket", [6.2, 18.0, 6.2], [9.8, 21.0, 9.8], uv=2),
        make_el("socket_gem", [6.8, 18.5, 5.8], [9.2, 20.5, 10.2], uv=3),
        # Left Crescent Bearded Blade
        make_el("blade_l_hub",   [4.5, 18.5, 7.5], [6.5, 20.5, 8.5], uv=0),
        make_el("blade_l_flare", [1.0, 17.5, 7.6], [4.8, 21.5, 8.4], uv=0),
        make_el("beard_l_top",   [0.5, 21.0, 7.6], [3.5, 24.0, 8.4], uv=0, rot_axis='z', rot_angle=-45.0, rot_origin=[3.0, 21.0, 8.0]),
        make_el("beard_l_bot",   [0.5, 15.0, 7.6], [3.5, 18.0, 8.4], uv=0, rot_axis='z', rot_angle=45.0, rot_origin=[3.0, 18.0, 8.0]),
        make_el("cutting_edge_l", [-0.2, 16.0, 7.8], [1.2, 23.0, 8.2], uv=3),
        # Right Crescent Bearded Blade
        make_el("blade_r_hub",   [9.5, 18.5, 7.5], [11.5, 20.5, 8.5], uv=0),
        make_el("blade_r_flare", [11.2, 17.5, 7.6], [15.0, 21.5, 8.4], uv=0),
        make_el("beard_r_top",   [12.5, 21.0, 7.6], [15.5, 24.0, 8.4], uv=0, rot_axis='z', rot_angle=45.0, rot_origin=[13.0, 21.0, 8.0]),
        make_el("beard_r_bot",   [12.5, 15.0, 7.6], [15.5, 18.0, 8.4], uv=0, rot_axis='z', rot_angle=-45.0, rot_origin=[13.0, 18.0, 8.0]),
        make_el("cutting_edge_r", [14.8, 16.0, 7.8], [16.2, 23.0, 8.2], uv=3),
        # Top Spear Spike
        make_el("spear_spike", [7.5, 21.0, 7.5], [8.5, 26.0, 8.5], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_warhammer(tex_name):
    """14 elements: Heavy rectangular hammer head with beveled faces, rear war spike, lightning spark."""
    elems = [
        make_el("haft", [7.3, 1.0, 7.3], [8.7, 19.0, 8.7], uv=1),
        make_el("haft_rings", [7.0, 5.0, 7.0], [9.0, 12.0, 9.0], uv=2),
        make_el("head_socket", [6.0, 18.0, 6.0], [10.0, 21.0, 10.0], uv=2),
        # Front Striking Block
        make_el("striking_block", [5.0, 17.5, 10.0], [11.0, 21.5, 15.5], uv=0),
        make_el("impact_plate",   [5.2, 17.7, 15.4], [10.8, 21.3, 16.2], uv=3),
        # 4 Striking Studs
        make_el("stud_1", [5.5, 18.0, 16.1], [7.0, 19.5, 16.8], uv=2),
        make_el("stud_2", [9.0, 18.0, 16.1], [10.5, 19.5, 16.8], uv=2),
        make_el("stud_3", [5.5, 19.5, 16.1], [7.0, 21.0, 16.8], uv=2),
        make_el("stud_4", [9.0, 19.5, 16.1], [10.5, 21.0, 16.8], uv=2),
        # Rear Raven's Beak War Spike
        make_el("spike_base", [6.5, 18.2, 2.5], [9.5, 20.8, 6.0], uv=0),
        make_el("spike_mid",  [7.0, 18.5, 0.0], [9.0, 20.5, 2.8], uv=0),
        make_el("spike_tip",  [7.5, 19.0, -2.5], [8.5, 20.0, 0.2], uv=3),
        # Top Lightning Rod Spike
        make_el("lightning_rod", [7.6, 21.0, 7.6], [8.4, 25.5, 8.4], uv=2),
        make_el("spark_node",    [7.2, 25.0, 7.2], [8.8, 26.5, 8.8], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_block_item(tex_name, is_ore=True):
    """
    12 elements: Rock/storage matrix block with protruding 3D crystal clusters and bevels!
    This renders in 3D in GUI and Hand (matches CurseForge reference).
    """
    elems = [
        # Main Block Core
        make_el("block_core", [1.0, 1.0, 1.0], [15.0, 15.0, 15.0], uv=0),
        # Beveled Corner Brackets
        make_el("corner_c1", [0.5, 0.5, 0.5], [3.5, 3.5, 3.5], uv=2),
        make_el("corner_c2", [12.5, 0.5, 0.5], [15.5, 3.5, 3.5], uv=2),
        make_el("corner_c3", [0.5, 12.5, 0.5], [3.5, 15.5, 3.5], uv=2),
        make_el("corner_c4", [12.5, 12.5, 0.5], [15.5, 15.5, 3.5], uv=2),
        make_el("corner_c5", [0.5, 0.5, 12.5], [3.5, 3.5, 15.5], uv=2),
        make_el("corner_c6", [12.5, 0.5, 12.5], [15.5, 3.5, 15.5], uv=2),
        make_el("corner_c7", [0.5, 12.5, 12.5], [3.5, 15.5, 15.5], uv=2),
        make_el("corner_c8", [12.5, 12.5, 12.5], [15.5, 15.5, 15.5], uv=2),
    ]
    if is_ore:
        # Protruding 3D Crystal Clusters breaking out of rock faces
        elems += [
            make_el("crystal_outcrop_front", [4.5, 4.5, 14.8], [11.5, 11.5, 16.8], uv=3),
            make_el("crystal_outcrop_top",   [5.0, 14.8, 5.0], [11.0, 16.8, 11.0], uv=3),
            make_el("crystal_outcrop_side",  [14.8, 5.0, 5.0], [16.8, 11.0, 11.0], uv=3)
        ]
    else:
        # Storage Block Inner Panels & Central Crest
        elems += [
            make_el("panel_front", [3.0, 3.0, 14.8], [13.0, 13.0, 15.4], uv=1),
            make_el("panel_top",   [3.0, 14.8, 3.0], [13.0, 15.4, 13.0], uv=1),
            make_el("center_seal", [6.0, 6.0, 15.3], [10.0, 10.0, 16.2], uv=3)
        ]
    return build_model_dict(elems, DISPLAY_BLOCK_ITEM, tex_name)

print("Geometry builders initialized.")
# ==============================================================================
# GEOMETRY GENERATORS: JAVA API & SPECIAL RPG RELICS
# ==============================================================================

def geom_katana(tex_name, sheathed=False):
    """16 elements: Japanese Samurai Katana with Tsuba guard, curved blade & Hamon line."""
    elems = [
        # Kashira (Pommel)
        make_el("kashira", [7.0, 0.5, 7.0], [9.0, 1.8, 9.0], uv=2),
        # Tsuka (Handle with Samegawa & Ito wrap)
        make_el("tsuka", [7.2, 1.8, 7.2], [8.8, 7.8, 8.8], uv=1),
        make_el("menuki", [7.0, 4.0, 7.0], [9.0, 5.0, 9.0], uv=2),
        make_el("fuchi_collar", [7.0, 7.5, 7.0], [9.0, 8.2, 9.0], uv=2),
        # Tsuba (Guard)
        make_el("tsuba_disk", [5.5, 8.2, 5.5], [10.5, 8.8, 10.5], uv=2),
        make_el("tsuba_rim",  [5.0, 8.3, 5.0], [11.0, 8.7, 11.0], uv=2),
        # Habaki (Blade Collar)
        make_el("habaki", [7.1, 8.8, 7.3], [8.9, 9.8, 8.7], uv=2),
    ]
    if sheathed:
        # Saya (Scabbard)
        elems += [
            make_el("saya_upper", [6.9, 9.8, 7.1], [9.1, 16.0, 8.9], uv=1),
            make_el("kurikata_knob", [6.5, 11.0, 6.8], [7.2, 12.5, 9.2], uv=2),
            make_el("sageo_cord", [6.8, 10.5, 6.9], [9.2, 13.0, 9.1], uv=3),
            make_el("saya_mid",   [6.9, 15.8, 7.1], [9.1, 22.0, 8.9], uv=1, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 16.0, 8.0]),
            make_el("saya_lower", [7.0, 21.5, 7.2], [9.0, 27.0, 8.8], uv=1, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 22.0, 8.0]),
            make_el("kojiri_end", [7.1, 26.5, 7.3], [8.9, 28.0, 8.7], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 22.0, 8.0])
        ]
    else:
        # Curved Katana Blade (Nagasa)
        elems += [
            make_el("blade_base", [7.2, 9.8, 7.6], [8.8, 15.5, 8.4], uv=0),
            make_el("ha_edge1",   [6.7, 9.8, 7.8], [7.3, 15.5, 8.2], uv=3),
            make_el("blade_mid",  [7.2, 15.2, 7.6], [8.8, 21.5, 8.4], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 15.5, 8.0]),
            make_el("ha_edge2",   [6.7, 15.2, 7.8], [7.3, 21.5, 8.2], uv=3, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 15.5, 8.0]),
            make_el("blade_top",  [7.3, 21.0, 7.7], [8.7, 26.0, 8.3], uv=0, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 21.5, 8.0]),
            make_el("kissaki_tip", [7.4, 25.8, 7.8], [8.6, 27.5, 8.2], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 21.5, 8.0])
        ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_staff(tex_name, is_infernal=False):
    """16 elements: Magic staff with twisted shaft, crown headpiece & floating elemental core."""
    elems = [
        make_el("staff_shaft", [7.3, 0.5, 7.3], [8.7, 18.0, 8.7], uv=1),
        make_el("staff_butt",  [7.0, 0.0, 7.0], [9.0, 1.2, 9.0], uv=2),
        make_el("grip_binding", [7.1, 6.0, 7.1], [8.9, 11.0, 8.9], uv=2),
        make_el("head_collar", [6.8, 17.5, 6.8], [9.2, 19.0, 9.2], uv=2),
        # Crown Prongs / Headpiece
        make_el("prong_north", [7.5, 18.5, 5.5], [8.5, 24.0, 6.8], uv=2, rot_axis='x', rot_angle=-22.5, rot_origin=[8.0, 19.0, 6.5]),
        make_el("prong_south", [7.5, 18.5, 9.2], [8.5, 24.0, 10.5], uv=2, rot_axis='x', rot_angle=22.5, rot_origin=[8.0, 19.0, 9.5]),
        make_el("prong_west",  [5.5, 18.5, 7.5], [6.8, 24.0, 8.5], uv=2, rot_axis='z', rot_angle=22.5, rot_origin=[6.5, 19.0, 8.0]),
        make_el("prong_east",  [9.2, 18.5, 7.5], [10.5, 24.0, 8.5], uv=2, rot_axis='z', rot_angle=-22.5, rot_origin=[9.5, 19.0, 8.0]),
        # Floating Arcane / Fire Core
        make_el("arcane_core", [6.5, 20.0, 6.5], [9.5, 23.0, 9.5], uv=3),
        make_el("core_facet1", [6.0, 20.5, 7.0], [10.0, 22.5, 9.0], uv=3),
        make_el("core_facet2", [7.0, 20.5, 6.0], [9.0, 22.5, 10.0], uv=3),
        make_el("aura_halo",   [5.5, 21.0, 5.5], [10.5, 22.0, 10.5], uv=0)
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_dagger(tex_name, is_kris=False):
    """14 elements: Serrated curved assassin dagger with ring pommel & double-edge blade."""
    elems = [
        make_el("ring_pommel", [7.0, 0.5, 7.0], [9.0, 2.5, 9.0], uv=2),
        make_el("grip",        [7.3, 2.5, 7.3], [8.7, 6.5, 8.7], uv=1),
        make_el("guard",       [5.5, 6.5, 6.5], [10.5, 7.5, 9.5], uv=2),
        make_el("guard_tooth_l", [4.5, 7.0, 7.0], [5.8, 8.5, 9.0], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[5.5, 7.0, 8.0]),
        make_el("guard_tooth_r", [10.2, 7.0, 7.0], [11.5, 8.5, 9.0], uv=2, rot_axis='z', rot_angle=-45.0, rot_origin=[10.5, 7.0, 8.0]),
        # Blade Base
        make_el("blade_base", [7.0, 7.5, 7.5], [9.0, 12.0, 8.5], uv=0),
        make_el("edge_l1",    [6.2, 7.8, 7.7], [7.1, 12.0, 8.3], uv=3),
        make_el("edge_r1",    [8.9, 7.8, 7.7], [9.8, 12.0, 8.3], uv=3),
        # Curved Upper Blade (22.5 deg)
        make_el("blade_upper", [7.0, 11.8, 7.5], [9.0, 16.5, 8.5], uv=0, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 12.0, 8.0]),
        make_el("edge_l2",     [6.2, 12.0, 7.7], [7.1, 16.5, 8.3], uv=3, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 12.0, 8.0]),
        make_el("edge_r2",     [8.9, 12.0, 7.7], [9.8, 16.5, 8.3], uv=3, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 12.0, 8.0]),
        # Piercing Point (45 deg)
        make_el("blade_point", [7.4, 16.0, 7.7], [8.6, 18.5, 8.3], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 16.0, 8.0])
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

def geom_potion(tex_name):
    """12 elements: Volumetric potion flask with cork, neck, lip & glowing boiling fluid."""
    elems = [
        # Flask Body
        make_el("bottle_base", [5.0, 1.0, 5.0], [11.0, 7.0, 11.0], uv=1),
        # 4 Chamfered Flask Corners
        make_el("corner_c1", [4.2, 1.5, 4.2], [5.8, 6.5, 5.8], uv=1, rot_axis='y', rot_angle=45.0, rot_origin=[5.0, 4.0, 5.0]),
        make_el("corner_c2", [10.2, 1.5, 4.2], [11.8, 6.5, 5.8], uv=1, rot_axis='y', rot_angle=-45.0, rot_origin=[11.0, 4.0, 5.0]),
        make_el("corner_c3", [4.2, 1.5, 10.2], [5.8, 6.5, 11.8], uv=1, rot_axis='y', rot_angle=-45.0, rot_origin=[5.0, 4.0, 11.0]),
        make_el("corner_c4", [10.2, 1.5, 10.2], [11.8, 6.5, 11.8], uv=1, rot_axis='y', rot_angle=45.0, rot_origin=[11.0, 4.0, 11.0]),
        # Inner Glowing Fluid Layer
        make_el("elixir_liquid", [5.5, 1.5, 5.5], [10.5, 6.5, 10.5], uv=3),
        # Shoulder Slopes
        make_el("shoulder", [5.5, 7.0, 5.5], [10.5, 8.5, 10.5], uv=1),
        # Neck & Lip
        make_el("bottle_neck", [6.8, 8.5, 6.8], [9.2, 11.0, 9.2], uv=1),
        make_el("bottle_lip",  [6.2, 11.0, 6.2], [9.8, 12.0, 9.8], uv=2),
        # Cork Stopper
        make_el("cork_plug",   [6.8, 12.0, 6.8], [9.2, 13.8, 9.2], uv=2),
        make_el("magic_bubble", [7.5, 3.5, 7.5], [8.5, 4.5, 8.5], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_GEM, tex_name)

def geom_scroll(tex_name):
    """11 elements: Rolled parchment roll, wooden spindles, wax seal medallion & ribbon."""
    elems = [
        make_el("scroll_core", [4.5, 4.0, 6.5], [11.5, 11.0, 9.5], uv=1),
        make_el("parchment_curl", [4.0, 4.5, 6.0], [12.0, 10.5, 10.0], uv=1),
        # Wooden Spindle Rods
        make_el("spindle_left",  [3.0, 7.0, 7.5], [4.5, 8.0, 8.5], uv=2),
        make_el("spindle_right", [11.5, 7.0, 7.5], [13.0, 8.0, 8.5], uv=2),
        make_el("knob_left",     [2.2, 6.7, 7.2], [3.2, 8.3, 8.8], uv=2),
        make_el("knob_right",    [12.8, 6.7, 7.2], [13.8, 8.3, 8.8], uv=2),
        # Golden Ribbon Binding
        make_el("ribbon_band",   [7.2, 3.8, 5.8], [8.8, 11.2, 10.2], uv=2),
        # Wax Seal Medallion
        make_el("wax_seal",      [6.8, 6.2, 5.2], [9.2, 8.8, 6.0], uv=3),
        make_el("seal_stamp",    [7.2, 6.6, 4.9], [8.8, 8.4, 5.3], uv=3),
        make_el("ribbon_dangle1", [7.0, 3.0, 5.5], [7.8, 5.0, 5.9], uv=2, rot_axis='z', rot_angle=22.5, rot_origin=[7.5, 5.0, 5.7]),
        make_el("ribbon_dangle2", [8.2, 3.0, 5.5], [9.0, 5.0, 5.9], uv=2, rot_axis='z', rot_angle=-22.5, rot_origin=[8.5, 5.0, 5.7])
    ]
    return build_model_dict(elems, DISPLAY_INGOT, tex_name)

def geom_bomb(tex_name):
    """12 elements: Cryo bomb sphere, detonator cap, frozen rivets, and ice crystal spikes."""
    elems = [
        make_el("sphere_core", [4.5, 2.5, 4.5], [11.5, 9.5, 11.5], uv=0),
        # 4 Spherical Bevels
        make_el("sphere_n", [5.5, 3.5, 3.5], [10.5, 8.5, 4.8], uv=0),
        make_el("sphere_s", [5.5, 3.5, 11.2], [10.5, 8.5, 12.5], uv=0),
        make_el("sphere_w", [3.5, 3.5, 5.5], [4.8, 8.5, 10.5], uv=0),
        make_el("sphere_e", [11.2, 3.5, 5.5], [12.5, 8.5, 10.5], uv=0),
        # Detonator Cap & Fuse
        make_el("detonator_cap", [6.5, 9.5, 6.5], [9.5, 11.0, 9.5], uv=2),
        make_el("fuse_stem",     [7.5, 11.0, 7.5], [8.5, 13.0, 8.5], uv=1),
        make_el("spark_node",    [7.2, 12.8, 7.2], [8.8, 14.2, 8.8], uv=3),
        # 4 Protruding Ice Crystal Spikes
        make_el("spike_top",   [7.0, 9.0, 7.0], [9.0, 12.5, 9.0], uv=3),
        make_el("spike_front", [7.0, 5.0, 11.5], [9.0, 7.0, 14.5], uv=3),
        make_el("spike_left",  [1.5, 5.0, 7.0], [4.5, 7.0, 9.0], uv=3),
        make_el("spike_right", [11.5, 5.0, 7.0], [14.5, 7.0, 9.0], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_GEM, tex_name)

def geom_feather(tex_name):
    """11 elements: Curved golden quill shaft with radiating flame feather vanes."""
    elems = [
        # Curved Quill Shaft (3 segments)
        make_el("quill_base", [7.5, 1.0, 7.5], [8.5, 6.0, 8.5], uv=2),
        make_el("quill_mid",  [7.5, 5.8, 7.5], [8.5, 12.0, 8.5], uv=2, rot_axis='z', rot_angle=22.5, rot_origin=[8.0, 6.0, 8.0]),
        make_el("quill_tip",  [7.5, 11.5, 7.5], [8.5, 18.0, 8.5], uv=2, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 12.0, 8.0]),
        # Flame Vanes (Left Flange)
        make_el("flame_v1_l", [4.5, 4.0, 7.8], [7.5, 8.0, 8.2], uv=0),
        make_el("flame_v2_l", [3.5, 7.5, 7.8], [7.5, 12.5, 8.2], uv=3, rot_axis='z', rot_angle=22.5, rot_origin=[7.5, 8.0, 8.0]),
        make_el("flame_v3_l", [2.5, 11.5, 7.8], [7.5, 16.5, 8.2], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[7.0, 12.0, 8.0]),
        # Flame Vanes (Right Flange)
        make_el("flame_v1_r", [8.5, 4.0, 7.8], [11.5, 8.0, 8.2], uv=0),
        make_el("flame_v2_r", [8.5, 7.5, 7.8], [12.5, 12.5, 8.2], uv=3, rot_axis='z', rot_angle=-22.5, rot_origin=[8.5, 8.0, 8.0]),
        make_el("flame_v3_r", [8.5, 11.5, 7.8], [13.5, 16.5, 8.2], uv=3, rot_axis='z', rot_angle=-45.0, rot_origin=[9.0, 12.0, 8.0]),
        # Specular Apex
        make_el("feather_apex", [7.6, 17.5, 7.8], [8.4, 20.5, 8.2], uv=3, rot_axis='z', rot_angle=45.0, rot_origin=[7.5, 12.0, 8.0]),
        make_el("fire_core",    [7.0, 8.0, 7.4], [9.0, 12.0, 8.6], uv=3)
    ]
    return build_model_dict(elems, DISPLAY_WEAPON, tex_name)

print("Relic geometry generators initialized.")
# ==============================================================================
# 4. MASTER CATALOG & DISPATCH ENGINE
# ==============================================================================

ITEM_REGISTRY = {
    # Elemental Materials & Blocks
    'ember_ore_raw':        {'geom': geom_raw_cluster, 'tex': ('ember', 'steel', 'gold', 'ember')},
    'ember_shard':          {'geom': geom_raw_cluster, 'tex': ('ember', 'ember', 'gold', 'ember')},
    'ember_ingot':          {'geom': geom_ingot,       'tex': ('ember', 'steel', 'gold', 'ember')},
    'ore_ember':            {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('ember', 'steel', 'gold', 'ember'), 'is_block': True, 'deepslate': False},
    'block_ember':          {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('ember', 'steel', 'gold', 'ember'), 'is_block': True, 'is_storage': True},

    'frost_ore_raw':        {'geom': geom_raw_cluster, 'tex': ('frost', 'steel', 'mithril', 'frost')},
    'frost_crystal_shard':  {'geom': geom_raw_cluster, 'tex': ('frost', 'frost', 'mithril', 'frost')},
    'frost_ingot':          {'geom': geom_ingot,       'tex': ('frost', 'steel', 'mithril', 'frost')},
    'ore_frost':            {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('frost', 'steel', 'mithril', 'frost'), 'is_block': True, 'deepslate': False},
    'block_frost':          {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('frost', 'steel', 'mithril', 'frost'), 'is_block': True, 'is_storage': True},

    'storm_shard':          {'geom': geom_raw_cluster, 'tex': ('storm', 'storm', 'gold', 'storm')},
    'storm_ingot':          {'geom': geom_ingot,       'tex': ('storm', 'steel', 'gold', 'storm')},
    'ore_storm':            {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('storm', 'steel', 'gold', 'storm'), 'is_block': True, 'deepslate': False},
    'block_storm':          {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('storm', 'steel', 'gold', 'storm'), 'is_block': True, 'is_storage': True},

    'void_shard':           {'geom': geom_raw_cluster, 'tex': ('void', 'void', 'steel', 'void')},
    'void_ingot':           {'geom': geom_ingot,       'tex': ('void', 'steel', 'void', 'void')},
    'ore_void':             {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('void', 'steel', 'void', 'void'), 'is_block': True, 'deepslate': True},
    'block_void':           {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('void', 'steel', 'void', 'void'), 'is_block': True, 'is_storage': True},

    # Gemstones & Expansion Metals
    'ruby_ore':             {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('ruby', 'steel', 'gold', 'ruby'), 'is_block': True, 'deepslate': False},
    'ruby_rough':           {'geom': geom_raw_cluster, 'tex': ('ruby', 'steel', 'gold', 'ruby')},
    'ruby_flawless':        {'geom': geom_faceted_gem, 'tex': ('ruby', 'ruby', 'gold', 'ruby')},
    'ruby_block':           {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('ruby', 'steel', 'gold', 'ruby'), 'is_block': True, 'is_storage': True},

    'sapphire_ore':         {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('sapphire', 'steel', 'mithril', 'sapphire'), 'is_block': True, 'deepslate': False},
    'sapphire_rough':       {'geom': geom_raw_cluster, 'tex': ('sapphire', 'steel', 'mithril', 'sapphire')},
    'sapphire_flawless':    {'geom': geom_faceted_gem, 'tex': ('sapphire', 'sapphire', 'mithril', 'sapphire')},
    'sapphire_block':       {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('sapphire', 'steel', 'mithril', 'sapphire'), 'is_block': True, 'is_storage': True},

    'topaz_ore':            {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('topaz', 'steel', 'gold', 'topaz'), 'is_block': True, 'deepslate': False},
    'topaz_rough':          {'geom': geom_raw_cluster, 'tex': ('topaz', 'steel', 'gold', 'topaz')},
    'topaz_flawless':       {'geom': geom_faceted_gem, 'tex': ('topaz', 'topaz', 'gold', 'topaz')},

    'adamantite_ore':       {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('adamantite', 'steel', 'gold', 'adamantite'), 'is_block': True, 'deepslate': True},
    'adamantite_ore_raw':   {'geom': geom_raw_cluster, 'tex': ('adamantite', 'steel', 'gold', 'adamantite')},
    'adamantite_ingot':     {'geom': geom_ingot,       'tex': ('adamantite', 'steel', 'gold', 'adamantite')},

    'mithril_ore':          {'geom': lambda t: geom_block_item(t, is_ore=True),  'tex': ('mithril', 'steel', 'mithril', 'mithril'), 'is_block': True, 'deepslate': False},
    'mithril_ore_raw':      {'geom': geom_raw_cluster, 'tex': ('mithril', 'steel', 'mithril', 'mithril')},
    'mithril_ingot':        {'geom': geom_ingot,       'tex': ('mithril', 'steel', 'mithril', 'mithril')},

    'ancient_runestone':    {'geom': lambda t: geom_block_item(t, is_ore=False), 'tex': ('runestone', 'steel', 'runestone', 'runestone'), 'is_block': True, 'deepslate': True},
    'runestone_slate':      {'geom': geom_runic_slate, 'tex': ('runestone', 'steel', 'gold', 'runestone')},
    'rune_of_power':        {'geom': geom_runic_slate, 'tex': ('ruby', 'runestone', 'gold', 'ruby')},
    'rune_of_protection':   {'geom': geom_runic_slate, 'tex': ('sapphire', 'runestone', 'mithril', 'sapphire')},
    'rune_of_haste':        {'geom': geom_runic_slate, 'tex': ('storm', 'runestone', 'gold', 'storm')},
    'rune_of_vitality':     {'geom': geom_runic_slate, 'tex': ('nature', 'runestone', 'gold', 'nature')},

    # Crafted Weapons & Armor Accessories
    'ember_blade':          {'geom': geom_sword,      'tex': ('ember', 'steel', 'gold', 'ember')},
    'ember_pickaxe':        {'geom': geom_pickaxe,    'tex': ('ember', 'steel', 'gold', 'ember')},
    'frost_battleaxe':      {'geom': geom_battleaxe,  'tex': ('frost', 'steel', 'mithril', 'frost')},
    'storm_warhammer':      {'geom': geom_warhammer,  'tex': ('storm', 'steel', 'gold', 'storm')},
    'ruby_amulet':          {'geom': geom_amulet,     'tex': ('ruby', 'gold', 'gold', 'ruby')},
    'sapphire_ring':        {'geom': geom_ring,       'tex': ('sapphire', 'gold', 'gold', 'sapphire')},

    # Java API Items
    'katana_drawn':         {'geom': lambda t: geom_katana(t, False), 'tex': ('steel', 'dark_wood', 'gold', 'steel')},
    'katana_sheathed':      {'geom': lambda t: geom_katana(t, True),  'tex': ('steel', 'dark_wood', 'gold', 'steel')},
    'storm_staff':          {'geom': geom_staff,      'tex': ('storm', 'dark_wood', 'gold', 'storm')},
    'shadow_dagger':        {'geom': geom_dagger,     'tex': ('void', 'steel', 'void', 'void')},
    'abyssal_amulet':       {'geom': geom_amulet,     'tex': ('void', 'steel', 'gold', 'void')},
    'soul_reaper':          {'geom': geom_sword,      'tex': ('void', 'steel', 'void', 'void')},
    'storm_spear':          {'geom': geom_battleaxe,  'tex': ('storm', 'steel', 'gold', 'storm')},
    'frost_dagger':         {'geom': geom_dagger,     'tex': ('frost', 'steel', 'mithril', 'frost')},
    'infernal_staff':       {'geom': lambda t: geom_staff(t, True), 'tex': ('ember', 'dark_wood', 'gold', 'ember')},
    'nature_ring':          {'geom': geom_ring,       'tex': ('nature', 'dark_wood', 'gold', 'nature')},
    'wind_pendant':         {'geom': geom_amulet,     'tex': ('frost', 'gold', 'gold', 'frost')},
    'eye_of_the_abyss':     {'geom': geom_amulet,     'tex': ('void', 'steel', 'gold', 'void')},
    'phoenix_feather':      {'geom': geom_feather,    'tex': ('ember', 'gold', 'gold', 'ember')},
    'crystal_drill':        {'geom': geom_pickaxe,    'tex': ('frost', 'steel', 'gold', 'frost')},
    'builders_wand':        {'geom': geom_staff,      'tex': ('gold', 'dark_wood', 'sapphire', 'gold')},
    'treasure_seeker':      {'geom': geom_ring,       'tex': ('gold', 'steel', 'gold', 'ruby')},
    'artisan_hammer':       {'geom': geom_warhammer,  'tex': ('steel', 'dark_wood', 'gold', 'steel')},
    'recall_scroll':        {'geom': geom_scroll,     'tex': ('parchment', 'dark_wood', 'gold', 'ruby')},
    'frost_bomb':           {'geom': geom_bomb,       'tex': ('frost', 'steel', 'mithril', 'frost')},
    'berserker_elixir':     {'geom': geom_potion,     'tex': ('ruby', 'steel', 'gold', 'ruby')},
    'ancient_seed':         {'geom': geom_raw_cluster, 'tex': ('nature', 'dark_wood', 'gold', 'nature')},
    'dragon_scale':         {'geom': geom_runic_slate, 'tex': ('void', 'steel', 'gold', 'void')},
    'void_essence':         {'geom': geom_potion,     'tex': ('void', 'steel', 'void', 'void')},
    'ancient_ingot':        {'geom': geom_ingot,      'tex': ('gold', 'runestone', 'gold', 'runestone')}
}

def export_all():
    print(f"Total registered items to forge: {len(ITEM_REGISTRY)}")

    for pack_dir in [SERVER_PACK, CLIENT_PACK]:
        if not os.path.exists(pack_dir):
            continue

        item_tex_dir = os.path.join(pack_dir, 'assets', 'artifex', 'textures', 'item')
        block_tex_dir = os.path.join(pack_dir, 'assets', 'artifex', 'textures', 'block')
        item_model_dir = os.path.join(pack_dir, 'assets', 'artifex', 'models', 'item')
        block_model_dir = os.path.join(pack_dir, 'assets', 'artifex', 'models', 'block')

        for d in [item_tex_dir, block_tex_dir, item_model_dir, block_model_dir]:
            os.makedirs(d, exist_ok=True)

        for item_id, cfg in ITEM_REGISTRY.items():
            tex_themes = cfg['tex']
            geom_fn = cfg['geom']
            is_block = cfg.get('is_block', False)

            # 1. Generate 64x64 HD Texture Atlas
            atlas_img = create_atlas_texture(tex_themes[0], tex_themes[1], tex_themes[2], tex_themes[3])
            tex_path = os.path.join(item_tex_dir, f"{item_id}.png")
            atlas_img.save(tex_path)

            # 2. If it's a block, also generate its block texture and block model
            if is_block:
                if cfg.get('is_storage', False):
                    b_img = create_storage_block_texture(item_id, tex_themes[0])
                else:
                    b_img = create_ore_texture(item_id, tex_themes[0], cfg.get('deepslate', False))
                b_tex_path = os.path.join(block_tex_dir, f"{item_id}.png")
                b_img.save(b_tex_path)

                # World Block Model (cube_all pointing to block texture)
                block_model = {
                    "parent": "minecraft:block/cube_all",
                    "textures": {
                        "all": f"artifex:block/{item_id}"
                    }
                }
                with open(os.path.join(block_model_dir, f"{item_id}.json"), 'w', encoding='utf-8') as f:
                    json.dump(block_model, f, indent=2)

            # 3. Generate High-Detail 3D Item Model
            model_json = geom_fn(item_id)
            with open(os.path.join(item_model_dir, f"{item_id}.json"), 'w', encoding='utf-8') as f:
                json.dump(model_json, f, indent=2)

    print("Successfully built all 3D models and HD textures for Server & Client!")

if __name__ == '__main__':
    export_all()
