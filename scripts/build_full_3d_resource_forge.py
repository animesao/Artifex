"""
Full 3D Resource Forge System Generator
Creates 64x64 HD textures, 3D Blockbench models, and modern Minecraft 26.3 definitions
for all Ores, Storage Blocks, Ingots, Gems, Raw Shards, Runes, and Equipment.
"""

import os
import json
import math
import shutil
from PIL import Image, ImageDraw

SERVER_PACK = r'D:\server\Artifex-ResourcePack'
CLIENT_PACK = r'C:\Users\Anime\AppData\Roaming\ElyPrismLauncher\instances\26.3\minecraft\resourcepacks\Artifex-ResourcePack'
REPO_MODELS = r'D:\Artifex\blockbench_models'

os.makedirs(REPO_MODELS, exist_ok=True)

# =====================================================================
# 1. COLOR PALETTES & THEMES
# =====================================================================
PALETTES = {
    'ruby': {
        'cavity': (40, 8, 14),
        'dark': (138, 14, 26),
        'base': (211, 47, 47),
        'light': (255, 82, 82),
        'glint': (255, 230, 230),
        'sparkle': (255, 23, 68)
    },
    'sapphire': {
        'cavity': (5, 16, 38),
        'dark': (13, 71, 161),
        'base': (25, 118, 210),
        'light': (66, 165, 245),
        'glint': (232, 251, 255),
        'sparkle': (41, 121, 255)
    },
    'topaz': {
        'cavity': (43, 29, 4),
        'dark': (230, 81, 0),
        'base': (255, 152, 0),
        'light': (255, 183, 77),
        'glint': (255, 248, 225),
        'sparkle': (255, 167, 38)
    },
    'adamantite': {
        'cavity': (24, 4, 12),
        'dark': (136, 14, 79),
        'base': (194, 24, 91),
        'light': (233, 30, 99),
        'glint': (252, 228, 236),
        'sparkle': (240, 98, 146)
    },
    'mithril': {
        'cavity': (6, 29, 36),
        'dark': (0, 96, 100),
        'base': (0, 151, 167),
        'light': (0, 188, 212),
        'glint': (224, 247, 250),
        'sparkle': (128, 222, 234)
    },
    'ember': {
        'cavity': (46, 10, 0),
        'dark': (191, 54, 12),
        'base': (230, 74, 25),
        'light': (255, 87, 34),
        'glint': (255, 255, 141),
        'sparkle': (255, 61, 0)
    },
    'frost': {
        'cavity': (5, 28, 44),
        'dark': (2, 119, 189),
        'base': (2, 136, 209),
        'light': (41, 182, 246),
        'glint': (240, 253, 255),
        'sparkle': (0, 229, 255)
    },
    'storm': {
        'cavity': (26, 26, 0),
        'dark': (245, 127, 23),
        'base': (251, 192, 45),
        'light': (255, 238, 88),
        'glint': (255, 255, 255),
        'sparkle': (255, 235, 59)
    },
    'void': {
        'cavity': (10, 2, 20),
        'dark': (49, 27, 146),
        'base': (81, 45, 168),
        'light': (126, 87, 194),
        'glint': (243, 229, 245),
        'sparkle': (171, 71, 188)
    }
}

STONE_PALETTE = {
    'deepslate': [(24, 24, 26), (32, 32, 36), (42, 42, 48), (52, 52, 58)],
    'stone': [(80, 80, 82), (98, 98, 102), (116, 116, 120), (135, 135, 140)]
}

# =====================================================================
# 2. PROCEDURAL 64x64 PIXEL ART TEXTURE GENERATORS
# =====================================================================

def create_noise_background(width, height, colors):
    img = Image.new("RGBA", (width, height))
    pixels = img.load()
    import random
    random.seed(42)
    for y in range(height):
        for x in range(width):
            c_idx = (x * 7 + y * 13 + random.randint(0, 3)) % len(colors)
            base_col = colors[c_idx]
            # Add subtle dithering
            noise = random.randint(-4, 4)
            r = max(0, min(255, base_col[0] + noise))
            g = max(0, min(255, base_col[1] + noise))
            b = max(0, min(255, base_col[2] + noise))
            pixels[x, y] = (r, g, b, 255)
    return img

def generate_ore_texture(ore_name, palette_key, is_deepslate=False):
    """
    Generates authentic 64x64 Minecraft ore texture matching reference cave scene:
    Stone/Deepslate rocky background + natural protruding crystal clusters + sparkling glints.
    """
    bg_colors = STONE_PALETTE['deepslate' if is_deepslate else 'stone']
    img = create_noise_background(64, 64, bg_colors)
    draw = ImageDraw.Draw(img)
    pal = PALETTES[palette_key]

    # Add deep horizontal/diagonal stone fissures
    import random
    random.seed(hash(ore_name))
    for _ in range(5):
        sx = random.randint(4, 55)
        sy = random.randint(4, 55)
        length = random.randint(8, 18)
        for i in range(length):
            px = min(63, sx + i)
            py = min(63, sy + (i // 3))
            dark_stone = (bg_colors[0][0] - 10, bg_colors[0][1] - 10, bg_colors[0][2] - 10, 255)
            img.putpixel((px, py), dark_stone)

    # Place 6-8 distinct crystal clusters across the face
    cluster_centers = [
        (14, 16), (42, 12), (28, 34), (50, 40), (18, 48), (38, 54), (54, 22), (8, 32)
    ]
    for cx, cy in cluster_centers:
        radius = random.randint(3, 6)
        # 1. Cavity shadow border
        for dx in range(-radius - 1, radius + 2):
            for dy in range(-radius - 1, radius + 2):
                dist = math.sqrt(dx*dx + dy*dy)
                if radius <= dist <= radius + 1.2:
                    px, py = cx + dx, cy + dy
                    if 0 <= px < 64 and 0 <= py < 64:
                        img.putpixel((px, py), pal['cavity'] + (255,))

        # 2. Crystal mass with directional lighting (top-left light, bottom-right dark)
        for dx in range(-radius, radius + 1):
            for dy in range(-radius, radius + 1):
                dist = math.sqrt(dx*dx + dy*dy)
                if dist <= radius:
                    px, py = cx + dx, cy + dy
                    if 0 <= px < 64 and 0 <= py < 64:
                        # Angle determines light/shadow
                        if dx < 0 and dy < 0:
                            col = pal['light']
                        elif dx <= 0 and dy <= 0:
                            col = pal['base']
                        else:
                            col = pal['dark']
                        img.putpixel((px, py), col + (255,))

        # 3. Specular highlight at top-left facet
        if 0 <= cx - 1 < 64 and 0 <= cy - 1 < 64:
            img.putpixel((cx - 1, cy - 1), pal['glint'] + (255,))
        if 0 <= cx < 64 and 0 <= cy - 1 < 64:
            img.putpixel((cx, cy - 1), pal['light'] + (255,))

    return img

def generate_storage_block_texture(block_name, palette_key):
    """
    Generates luxury polished 64x64 storage block texture:
    Beveled outer metallic/crystal frame + internal cut gemstone facets.
    """
    img = Image.new("RGBA", (64, 64))
    pixels = img.load()
    pal = PALETTES[palette_key]

    for y in range(64):
        for x in range(64):
            # Outer 4px bevel frame
            dist_left = x
            dist_top = y
            dist_right = 63 - x
            dist_bottom = 63 - y
            min_dist = min(dist_left, dist_top, dist_right, dist_bottom)

            if min_dist < 4:
                # Top/Left light, Bottom/Right shadow
                if min(dist_left, dist_top) < min(dist_right, dist_bottom):
                    col = pal['light']
                else:
                    col = pal['dark']
            elif min_dist == 4:
                col = pal['glint']
            else:
                # Inner geometric pattern (cross-cut gemstone facet)
                center_dist = math.sqrt((x - 31.5)**2 + (y - 31.5)**2)
                d_diag1 = abs(x - y)
                d_diag2 = abs(x - (63 - y))
                
                if d_diag1 < 2 or d_diag2 < 2:
                    col = pal['light']
                elif center_dist < 12:
                    col = pal['glint'] if center_dist < 5 else pal['base']
                else:
                    col = pal['dark'] if (x > 31 and y > 31) else pal['base']

            pixels[x, y] = col + (255,)

    return img

def generate_ingot_texture(palette_key):
    """
    64x64 texture atlas for 3D Ingot:
    Top face with insignia, beveled metallic sides, specular highlights.
    """
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pal = PALETTES[palette_key]
    draw = ImageDraw.Draw(img)

    # Ingot base gradient [0..48, 0..32]
    for y in range(32):
        for x in range(48):
            if y < 4: col = pal['glint']
            elif y < 14: col = pal['light']
            elif y < 24: col = pal['base']
            else: col = pal['dark']
            img.putpixel((x, y), col + (255,))

    # Stamped crest on top [12..36, 6..22]
    draw.rectangle([12, 6, 36, 22], outline=pal['dark'] + (255,), width=1)
    draw.line([(24, 8), (24, 20)], fill=pal['glint'] + (255,), width=1)
    draw.line([(16, 14), (32, 14)], fill=pal['glint'] + (255,), width=1)

    # Side faces and bevels [0..63, 32..63]
    for y in range(32, 64):
        for x in range(64):
            col = pal['base'] if (x + y) % 6 != 0 else pal['light']
            if y > 56 or x < 4 or x > 60: col = pal['dark']
            img.putpixel((x, y), col + (255,))

    return img

def generate_gem_texture(palette_key):
    """
    64x64 texture atlas for 3D Faceted Gemstone:
    Table facet, pavilion diagonals, chromatic reflections.
    """
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pal = PALETTES[palette_key]

    for y in range(64):
        for x in range(64):
            cx, cy = 31.5, 31.5
            dist = math.sqrt((x - cx)**2 + (y - cy)**2)
            angle = math.atan2(y - cy, x - cx)
            # Octagonal faceted shading
            sector = int((angle + math.pi) / (math.pi / 4)) % 8
            
            if dist < 10:
                col = pal['glint']
            elif dist < 22:
                if sector in [1, 2]: col = pal['glint']
                elif sector in [0, 3]: col = pal['light']
                elif sector in [4, 7]: col = pal['base']
                else: col = pal['dark']
            elif dist < 28:
                col = pal['cavity']
            else:
                col = (0, 0, 0, 0)
                
            if col[3] != 0 if len(col) == 4 else True:
                img.putpixel((x, y), (col[0], col[1], col[2], 255))

    return img

def generate_raw_shard_texture(palette_key):
    """
    64x64 texture atlas for Raw Ore Shards & Mineral Geodes.
    """
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pal = PALETTES[palette_key]
    stone = STONE_PALETTE['deepslate']

    for y in range(64):
        for x in range(64):
            # Spire crystal pattern
            if 8 <= x <= 56 and 8 <= y <= 56:
                if x < 28:
                    col = pal['light'] if y < 32 else pal['base']
                elif x < 46:
                    col = pal['base'] if y < 40 else pal['dark']
                else:
                    col = stone[1]
                # High specular edge
                if x == 28 or y == 16:
                    col = pal['glint']
                img.putpixel((x, y), col + (255,))
    return img

def generate_rune_slate_texture(palette_key, rune_type):
    """
    64x64 texture for 3D Runic Slate Tablet:
    Dark slate background + glowing neon runic emblem.
    """
    img = Image.new("RGBA", (64, 64))
    pixels = img.load()
    pal = PALETTES[palette_key]

    # Slate stone background
    for y in range(64):
        for x in range(64):
            c_idx = (x * 3 + y * 7) % 4
            base = STONE_PALETTE['deepslate'][c_idx]
            pixels[x, y] = base + (255,)

    draw = ImageDraw.Draw(img)
    # Beveled slate rim
    draw.rectangle([4, 4, 59, 59], outline=(15, 15, 18, 255), width=3)
    draw.rectangle([7, 7, 56, 56], outline=(70, 70, 78, 255), width=1)

    # Glowing Rune Core
    neon = pal['sparkle']
    glow = pal['light']
    white = (255, 255, 255, 255)

    if rune_type == 'power':
        # Flame / Sword runic glyph
        draw.polygon([(32, 12), (22, 42), (32, 36), (42, 42)], fill=neon + (255,), outline=glow + (255,))
        draw.line([(32, 16), (32, 34)], fill=white, width=2)
    elif rune_type == 'protection':
        # Shield rune
        draw.polygon([(32, 12), (48, 20), (44, 44), (32, 52), (20, 44), (16, 20)], fill=neon + (255,), outline=glow + (255,))
        draw.polygon([(32, 18), (42, 24), (32, 44), (22, 24)], fill=glow + (255,), outline=white)
    elif rune_type == 'haste':
        # Lightning glyph
        draw.line([(36, 12), (22, 30), (34, 30), (26, 52), (44, 28), (32, 28), (40, 12)], fill=neon + (255,), width=3)
        draw.line([(36, 14), (24, 30), (34, 30), (28, 48), (42, 28), (32, 28)], fill=white, width=1)
    elif rune_type == 'vitality':
        # Heart crystal
        draw.polygon([(32, 48), (18, 30), (20, 18), (32, 24), (44, 18), (46, 30)], fill=neon + (255,), outline=white)
    else:
        # Ancient Runestone Slate
        draw.ellipse([18, 18, 46, 46], outline=glow + (255,), width=2)
        draw.line([(32, 14), (32, 50)], fill=neon + (255,), width=2)
        draw.line([(14, 32), (50, 32)], fill=neon + (255,), width=2)

    return img

def generate_weapon_texture(weapon_id, palette_key):
    """
    64x64 texture atlas for 3D Weapons:
    Hilt, guard, 3D blade edge, energy channels.
    """
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    pal = PALETTES[palette_key]
    draw = ImageDraw.Draw(img)

    # Blade section [0..31, 0..31]
    for y in range(32):
        for x in range(32):
            if x < 4: col = pal['glint']
            elif x < 12: col = pal['light']
            elif x < 24: col = pal['base']
            else: col = pal['dark']
            img.putpixel((x, y), col + (255,))

    # Hilt / Grip [32..63, 0..31] (Dark leather & wrap)
    for y in range(32):
        for x in range(32, 64):
            col = (45, 30, 20) if (x + y) % 4 != 0 else (25, 15, 10)
            img.putpixel((x, y), col + (255,))

    # Guard & Pommel [0..31, 32..63] (Golden / Steel fittings)
    for y in range(32, 64):
        for x in range(32):
            col = (212, 149, 34) if x < 20 else (150, 96, 19)
            img.putpixel((x, y), col + (255,))

    # Core Gemstone [32..63, 32..63]
    for y in range(32, 64):
        for x in range(32, 64):
            dist = math.sqrt((x - 47.5)**2 + (y - 47.5)**2)
            if dist < 6: col = pal['glint']
            elif dist < 12: col = pal['light']
            elif dist < 15: col = pal['base']
            else: col = (0, 0, 0, 0)
            if col[3] != 0 if len(col) == 4 else True:
                img.putpixel((x, y), (col[0], col[1], col[2], 255))

    return img

# =====================================================================
# 3. 3D MINECRAFT JAVA GEOMETRY GENERATORS
# =====================================================================

def make_cube(from_box, to_box, tex_name, rot=None, origin=None):
    cube = {
        "from": from_box,
        "to": to_box,
        "faces": {
            "north": {"uv": [0, 0, 16, 16], "texture": f"#{tex_name}"},
            "east":  {"uv": [0, 0, 16, 16], "texture": f"#{tex_name}"},
            "south": {"uv": [0, 0, 16, 16], "texture": f"#{tex_name}"},
            "west":  {"uv": [0, 0, 16, 16], "texture": f"#{tex_name}"},
            "up":    {"uv": [0, 0, 16, 16], "texture": f"#{tex_name}"},
            "down":  {"uv": [0, 0, 16, 16], "texture": f"#{tex_name}"}
        }
    }
    if rot:
        axis, angle = rot
        cube["rotation"] = {
            "origin": origin if origin else [8.0, 8.0, 8.0],
            "axis": axis,
            "angle": float(angle)
        }
    return cube

def build_3d_ingot_model(tex_path):
    """
    Volumetric 3D Ingot Model (Trapezoidal prism with 3 stepped beveled layers).
    """
    return {
        "credit": "Artifex 3D Forge",
        "texture_size": [64, 64],
        "textures": {
            "0": tex_path,
            "particle": tex_path
        },
        "elements": [
            # Base plate
            make_cube([4.0, 5.0, 2.0], [12.0, 6.5, 14.0], "0"),
            # Middle bevel
            make_cube([4.5, 6.5, 2.5], [11.5, 8.5, 13.5], "0"),
            # Top stamped crown
            make_cube([5.0, 8.5, 3.0], [11.0, 10.0, 13.0], "0")
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 3.0, 1.0], "scale": [0.75, 0.75, 0.75]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.0, 3.0, 1.0], "scale": [0.65, 0.65, 0.65]},
            "gui": {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.5, 0.5, 0.5]}
        }
    }

def build_3d_gem_model(tex_path):
    """
    Faceted 3D Octagonal Gemstone Model with pavilion and table facets.
    """
    return {
        "credit": "Artifex 3D Forge",
        "texture_size": [64, 64],
        "textures": {
            "0": tex_path,
            "particle": tex_path
        },
        "elements": [
            # Core central prism
            make_cube([6.0, 5.0, 6.0], [10.0, 11.0, 10.0], "0"),
            # Top Table facet
            make_cube([6.5, 11.0, 6.5], [9.5, 12.5, 9.5], "0"),
            # Bottom Culet point
            make_cube([7.0, 3.5, 7.0], [9.0, 5.0, 9.0], "0"),
            # 4 Side Facet wings (angled 45 degrees around single axis)
            make_cube([6.0, 6.0, 4.5], [10.0, 10.0, 6.0], "0", rot=('x', 22.5), origin=[8.0, 8.0, 8.0]),
            make_cube([6.0, 6.0, 10.0], [10.0, 10.0, 11.5], "0", rot=('x', -22.5), origin=[8.0, 8.0, 8.0]),
            make_cube([4.5, 6.0, 6.0], [6.0, 10.0, 10.0], "0", rot=('z', 22.5), origin=[8.0, 8.0, 8.0]),
            make_cube([10.0, 6.0, 6.0], [11.5, 10.0, 10.0], "0", rot=('z', -22.5), origin=[8.0, 8.0, 8.0])
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 3.0, 1.0], "scale": [0.8, 0.8, 0.8]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.0, 3.0, 1.0], "scale": [0.7, 0.7, 0.7]},
            "gui": {"rotation": [20, 45, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.55, 0.55, 0.55]}
        }
    }

def build_3d_crystal_cluster_model(tex_path):
    """
    Volumetric 3D Crystal Cluster / Raw Mineral Geode with protruding angled spires.
    """
    return {
        "credit": "Artifex 3D Forge",
        "texture_size": [64, 64],
        "textures": {
            "0": tex_path,
            "particle": tex_path
        },
        "elements": [
            # Rocky Base
            make_cube([5.0, 2.0, 5.0], [11.0, 5.0, 11.0], "0"),
            # Main central spire
            make_cube([7.0, 4.0, 7.0], [9.0, 13.0, 9.0], "0"),
            make_cube([7.5, 13.0, 7.5], [8.5, 15.0, 8.5], "0"),
            # Left crystal shard (angled 22.5 on Z)
            make_cube([4.5, 4.0, 6.5], [6.5, 11.0, 8.5], "0", rot=('z', 22.5), origin=[5.5, 4.0, 7.5]),
            # Right crystal shard (angled -22.5 on X)
            make_cube([7.5, 4.0, 8.5], [9.5, 10.5, 10.5], "0", rot=('x', -22.5), origin=[8.5, 4.0, 9.5])
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 3.0, 1.0], "scale": [0.8, 0.8, 0.8]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.0, 3.0, 1.0], "scale": [0.7, 0.7, 0.7]},
            "gui": {"rotation": [25, 45, -15], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.55, 0.55, 0.55]}
        }
    }

def build_3d_rune_slate_model(tex_path):
    """
    3D Carved Stone Tablet with raised runic face.
    """
    return {
        "credit": "Artifex 3D Forge",
        "texture_size": [64, 64],
        "textures": {
            "0": tex_path,
            "particle": tex_path
        },
        "elements": [
            # Main slate tablet
            make_cube([4.0, 2.0, 6.5], [12.0, 14.0, 9.5], "0"),
            # Carved 3D border relief
            make_cube([4.5, 2.5, 6.0], [11.5, 13.5, 6.5], "0"),
            # Raised Rune Crest in 3D relief
            make_cube([6.0, 5.0, 5.6], [10.0, 11.0, 6.0], "0")
        ],
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 3.0, 1.0], "scale": [0.8, 0.8, 0.8]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.0, 3.0, 1.0], "scale": [0.7, 0.7, 0.7]},
            "gui": {"rotation": [15, 30, 0], "translation": [0, 0, 0], "scale": [0.85, 0.85, 0.85]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.55, 0.55, 0.55]}
        }
    }

def build_3d_weapon_model(weapon_id, tex_path):
    """
    Blockbench 3D Weapon Models (Ember Blade, Frost Battleaxe, Storm Warhammer, Ember Pickaxe, Ruby Amulet, Sapphire Ring).
    """
    elements = []
    display = {
        "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2.0, 0], "scale": [0.5, 0.5, 0.5]}
    }

    if weapon_id == 'ember_blade':
        # 3D Katana hilt & blade
        elements.append(make_cube([7.0, 1.0, 7.0], [9.0, 6.0, 9.0], "0")) # Hilt
        elements.append(make_cube([5.0, 6.0, 5.0], [11.0, 7.0, 11.0], "0")) # Tsuba Guard
        elements.append(make_cube([7.5, 7.0, 7.0], [8.5, 17.0, 9.0], "0")) # Blade lower
        elements.append(make_cube([7.5, 17.0, 7.5], [8.5, 25.0, 9.5], "0", rot=('x', 22.5), origin=[8.0, 17.0, 8.0])) # Blade curved tip
    elif weapon_id == 'frost_battleaxe':
        # 3D Double-bitted frost waraxe
        elements.append(make_cube([7.5, 1.0, 7.5], [8.5, 22.0, 8.5], "0")) # Ashwood shaft
        elements.append(make_cube([2.0, 14.0, 7.0], [7.5, 20.0, 9.0], "0")) # Left Ice blade
        elements.append(make_cube([8.5, 14.0, 7.0], [14.0, 20.0, 9.0], "0")) # Right Ice blade
        elements.append(make_cube([7.0, 13.5, 6.5], [9.0, 20.5, 9.5], "0")) # Central axe socket collar
    elif weapon_id == 'storm_warhammer':
        # 3D Massive runic warhammer
        elements.append(make_cube([7.5, 1.0, 7.5], [8.5, 19.0, 8.5], "0")) # Shaft
        elements.append(make_cube([5.0, 14.0, 4.0], [11.0, 20.0, 12.0], "0")) # Heavy warhammer head
        elements.append(make_cube([6.0, 13.0, 3.5], [10.0, 21.0, 4.0], "0")) # Front striking plate
        elements.append(make_cube([6.0, 13.0, 12.0], [10.0, 21.0, 12.5], "0")) # Rear striking plate
    elif weapon_id == 'ember_pickaxe':
        # 3D Volcanic Pickaxe
        elements.append(make_cube([7.5, 1.0, 7.5], [8.5, 19.0, 8.5], "0")) # Shaft
        elements.append(make_cube([2.0, 16.0, 7.0], [14.0, 18.5, 9.0], "0")) # Main cross pick
        elements.append(make_cube([1.5, 14.5, 7.5], [3.5, 16.5, 8.5], "0", rot=('z', 45.0), origin=[3.0, 16.0, 8.0])) # Left beak
        elements.append(make_cube([12.5, 14.5, 7.5], [14.5, 16.5, 8.5], "0", rot=('z', -45.0), origin=[13.0, 16.0, 8.0])) # Right beak
    elif weapon_id == 'ruby_amulet':
        # 3D Volumetric Golden Amulet
        elements.append(make_cube([4.0, 8.0, 7.5], [12.0, 15.0, 8.5], "0")) # Golden ring
        elements.append(make_cube([6.5, 4.0, 7.0], [9.5, 8.0, 9.0], "0")) # Hanging Faceted Ruby
        display["gui"] = {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]}
    elif weapon_id == 'sapphire_ring':
        # 3D Octagonal Signet Ring
        elements.append(make_cube([6.0, 4.0, 6.0], [10.0, 9.0, 10.0], "0")) # Gold band
        elements.append(make_cube([6.5, 9.0, 6.5], [9.5, 12.0, 9.5], "0")) # Elevated Sapphire setting
        display["gui"] = {"rotation": [30, 45, 0], "translation": [0, 0, 0], "scale": [1.2, 1.2, 1.2]}

    return {
        "credit": "Artifex 3D Forge",
        "texture_size": [64, 64],
        "textures": {
            "0": tex_path,
            "particle": tex_path
        },
        "elements": elements,
        "display": display
    }

# =====================================================================
# 4. MASTER BUILD & DEPLOY PIPELINE
# =====================================================================

def main():
    print("=== ARTIFEX RESOURCE FORGE: 3D MODEL & TEXTURE ENGINE ===")

    # 1. Generate Ores & Storage Blocks
    BLOCK_SPECS = [
        ('ember_ore', 'ore_ember', 'ember', False),
        ('frost_ore', 'ore_frost', 'frost', False),
        ('storm_ore', 'ore_storm', 'storm', False),
        ('void_ore', 'ore_void', 'void', True),
        ('ruby_ore', 'ore_ruby', 'ruby', False),
        ('sapphire_ore', 'ore_sapphire', 'sapphire', True),
        ('topaz_ore', 'ore_topaz', 'topaz', False),
        ('adamantite_ore', 'ore_adamantite', 'adamantite', True),
        ('mithril_ore', 'ore_mithril', 'mithril', False),
        ('ancient_runestone', 'ancient_runestone', 'void', True),
        
        ('ember_block', 'block_ember', 'ember', None),
        ('frost_block', 'block_frost', 'frost', None),
        ('storm_block', 'block_storm', 'storm', None),
        ('void_block', 'block_void', 'void', None),
        ('ruby_block', 'block_ruby', 'ruby', None),
        ('sapphire_block', 'block_sapphire', 'sapphire', None),
    ]

    for pack_dir in [SERVER_PACK, CLIENT_PACK]:
        block_tex_dir = os.path.join(pack_dir, 'assets', 'artifex', 'textures', 'block')
        item_tex_dir = os.path.join(pack_dir, 'assets', 'artifex', 'textures', 'item')
        block_model_dir = os.path.join(pack_dir, 'assets', 'artifex', 'models', 'block')
        item_model_dir = os.path.join(pack_dir, 'assets', 'artifex', 'models', 'item')
        os.makedirs(block_tex_dir, exist_ok=True)
        os.makedirs(item_tex_dir, exist_ok=True)
        os.makedirs(block_model_dir, exist_ok=True)
        os.makedirs(item_model_dir, exist_ok=True)

        # Build Block Textures & Models
        for blk_id, tex_name, pal_key, is_deepslate in BLOCK_SPECS:
            tex_file = os.path.join(block_tex_dir, f"{tex_name}.png")
            if is_deepslate is None:
                img = generate_storage_block_texture(tex_name, pal_key)
            elif tex_name == 'ancient_runestone':
                img = generate_rune_slate_texture('void', 'ancient')
            else:
                img = generate_ore_texture(tex_name, pal_key, is_deepslate)
            img.save(tex_file, "PNG")

            # Block Model (Full 3D cube_all)
            cube_model = {
                "parent": "minecraft:block/cube_all",
                "textures": {
                    "all": f"artifex:block/{tex_name}"
                }
            }
            with open(os.path.join(block_model_dir, f"{tex_name}.json"), "w", encoding="utf-8") as f:
                json.dump(cube_model, f, indent=2)
            with open(os.path.join(item_model_dir, f"{tex_name}.json"), "w", encoding="utf-8") as f:
                json.dump(cube_model, f, indent=2)

        # 2. Build 3D Ingots
        INGOTS = [
            ('ember_ingot', 'ember'),
            ('frost_ingot', 'frost'),
            ('storm_ingot', 'storm'),
            ('void_ingot', 'void'),
            ('adamantite_ingot', 'adamantite'),
            ('mithril_ingot', 'mithril')
        ]
        for ingot_id, pal_key in INGOTS:
            tex_file = os.path.join(item_tex_dir, f"{ingot_id}.png")
            img = generate_ingot_texture(pal_key)
            img.save(tex_file, "PNG")

            ingot_3d = build_3d_ingot_model(f"artifex:item/{ingot_id}")
            with open(os.path.join(item_model_dir, f"{ingot_id}.json"), "w", encoding="utf-8") as f:
                json.dump(ingot_3d, f, indent=2)

        # 3. Build 3D Faceted Gems
        GEMS = [
            ('ruby_flawless', 'ruby'),
            ('sapphire_flawless', 'sapphire'),
            ('topaz_flawless', 'topaz')
        ]
        for gem_id, pal_key in GEMS:
            tex_file = os.path.join(item_tex_dir, f"{gem_id}.png")
            img = generate_gem_texture(pal_key)
            img.save(tex_file, "PNG")

            gem_3d = build_3d_gem_model(f"artifex:item/{gem_id}")
            with open(os.path.join(item_model_dir, f"{gem_id}.json"), "w", encoding="utf-8") as f:
                json.dump(gem_3d, f, indent=2)

        # 4. Build 3D Raw Gems, Shards & Raw Ores
        SHARDS = [
            ('ruby_rough', 'ruby'),
            ('sapphire_rough', 'sapphire'),
            ('topaz_rough', 'topaz'),
            ('ember_shard', 'ember'),
            ('frost_crystal_shard', 'frost'),
            ('storm_shard', 'storm'),
            ('void_shard', 'void'),
            ('ember_ore_raw', 'ember'),
            ('frost_ore_raw', 'frost'),
            ('adamantite_ore_raw', 'adamantite'),
            ('mithril_ore_raw', 'mithril')
        ]
        for shard_id, pal_key in SHARDS:
            tex_file = os.path.join(item_tex_dir, f"{shard_id}.png")
            img = generate_raw_shard_texture(pal_key)
            img.save(tex_file, "PNG")

            cluster_3d = build_3d_crystal_cluster_model(f"artifex:item/{shard_id}")
            with open(os.path.join(item_model_dir, f"{shard_id}.json"), "w", encoding="utf-8") as f:
                json.dump(cluster_3d, f, indent=2)

        # 5. Build 3D Runes & Slates
        RUNES = [
            ('runestone_slate', 'void', 'ancient'),
            ('rune_of_power', 'ruby', 'power'),
            ('rune_of_protection', 'sapphire', 'protection'),
            ('rune_of_haste', 'storm', 'haste'),
            ('rune_of_vitality', 'frost', 'vitality')
        ]
        for rune_id, pal_key, rune_type in RUNES:
            tex_file = os.path.join(item_tex_dir, f"{rune_id}.png")
            img = generate_rune_slate_texture(pal_key, rune_type)
            img.save(tex_file, "PNG")

            slate_3d = build_3d_rune_slate_model(f"artifex:item/{rune_id}")
            with open(os.path.join(item_model_dir, f"{rune_id}.json"), "w", encoding="utf-8") as f:
                json.dump(slate_3d, f, indent=2)

        # 6. Build 3D Weapons & Equipment
        WEAPONS = [
            ('ember_blade', 'ember'),
            ('ember_pickaxe', 'ember'),
            ('frost_battleaxe', 'frost'),
            ('storm_warhammer', 'storm'),
            ('ruby_amulet', 'ruby'),
            ('sapphire_ring', 'sapphire')
        ]
        for weap_id, pal_key in WEAPONS:
            tex_file = os.path.join(item_tex_dir, f"{weap_id}.png")
            img = generate_weapon_texture(weap_id, pal_key)
            img.save(tex_file, "PNG")

            weapon_3d = build_3d_weapon_model(weap_id, f"artifex:item/{weap_id}")
            with open(os.path.join(item_model_dir, f"{weap_id}.json"), "w", encoding="utf-8") as f:
                json.dump(weapon_3d, f, indent=2)

    print("Successfully built all 64x64 textures and true 3D models for server & client!")

if __name__ == '__main__':
    main()
