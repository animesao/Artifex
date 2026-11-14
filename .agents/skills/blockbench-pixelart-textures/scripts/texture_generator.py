"""
Procedural Pixel-Art Texture Generator for Blockbench Models
Generates 64x64 multi-zone textures with rich palettes, metallic highlights, and faceted shading.
"""

import sys
import os
import argparse
from PIL import Image, ImageDraw

THEMES = {
    "infernal": {
        "primary": [(54, 8, 8), (115, 19, 19), (191, 36, 36), (255, 92, 92), (255, 230, 230)],
        "metal": [(30, 20, 20), (60, 40, 35), (120, 80, 50), (190, 140, 70), (255, 210, 110)],
        "accent": [(255, 120, 0), (255, 200, 50), (255, 255, 150)]
    },
    "frost": {
        "primary": [(10, 37, 64), (19, 78, 122), (33, 138, 199), (92, 210, 255), (232, 251, 255)],
        "metal": [(20, 30, 40), (45, 60, 75), (80, 105, 130), (140, 175, 205), (220, 240, 255)],
        "accent": [(80, 220, 255), (180, 245, 255), (255, 255, 255)]
    },
    "void": {
        "primary": [(11, 8, 20), (28, 19, 46), (53, 29, 90), (108, 56, 184), (194, 139, 255)],
        "metal": [(18, 15, 24), (35, 30, 45), (65, 55, 80), (115, 100, 140), (190, 175, 220)],
        "accent": [(170, 70, 255), (230, 150, 255), (255, 230, 255)]
    },
    "nature": {
        "primary": [(8, 43, 20), (17, 94, 45), (31, 168, 82), (88, 242, 141), (230, 255, 240)],
        "metal": [(40, 30, 15), (75, 55, 25), (135, 105, 45), (200, 165, 75), (250, 225, 130)],
        "accent": [(60, 230, 110), (160, 255, 170), (240, 255, 220)]
    },
    "storm": {
        "primary": [(40, 35, 10), (85, 75, 15), (150, 130, 25), (235, 205, 40), (255, 245, 140)],
        "metal": [(25, 30, 40), (50, 60, 75), (90, 105, 130), (150, 170, 200), (225, 235, 250)],
        "accent": [(100, 200, 255), (200, 240, 255), (255, 255, 255)]
    }
}

def generate_texture(output_path, theme_name="frost", size=64):
    theme = THEMES.get(theme_name.lower(), THEMES["frost"])
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    half = size // 2
    pri = theme["primary"]
    met = theme["metal"]
    acc = theme["accent"]

    # Quadrant 1: Top-Left [0..half, 0..half] - Blade / Main Core
    for y in range(half):
        for x in range(half):
            dist = (x + y) / (half * 2)
            idx = min(len(pri) - 1, int(dist * len(pri)))
            c = pri[idx]
            # Specular edge on top and left
            if x == 0 or y == 0:
                c = pri[-1]
            elif x == half - 1 or y == half - 1:
                c = pri[0]
            draw.point((x, y), fill=c)

    # Quadrant 2: Top-Right [half..size, 0..half] - Handle / Shaft / Leather
    for y in range(half):
        for x in range(half, size):
            # Stripe / Wrap pattern
            stripe = ((x + y) // 3) % 2
            base_col = met[1] if stripe == 0 else met[2]
            if (x - half) == 0 or (x - half) == half - 1:
                base_col = met[0]
            draw.point((x, y), fill=base_col)

    # Quadrant 3: Bottom-Left [0..half, half..size] - Guard / Bevels
    for y in range(half, size):
        for x in range(half):
            dist = (x + (y - half)) / (half * 2)
            idx = min(len(met) - 1, int(dist * len(met)))
            col = met[idx]
            if x == 0 or (y - half) == 0:
                col = met[-1]
            draw.point((x, y), fill=col)

    # Quadrant 4: Bottom-Right [half..size, half..size] - Gem / Crystal / Energy
    center_x, center_y = half + half // 2, half + half // 2
    radius = half // 2 - 2
    for y in range(half, size):
        for x in range(half, size):
            dx = x - center_x
            dy = y - center_y
            d = (dx*dx + dy*dy) ** 0.5
            if d <= radius:
                # Facet cut
                angle_sector = int((dx + dy) // 3) % len(acc)
                c = acc[angle_sector]
                if dx < 0 and dy < 0: # Highlight
                    c = acc[-1]
            else:
                c = (0, 0, 0, 0)
            draw.point((x, y), fill=c)

    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    img.save(output_path, "PNG")
    print(f"Generated {size}x{size} pixel art texture: {output_path} (Theme: {theme_name})")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate 64x64 Blockbench Texture")
    parser.add_argument("--output", "-o", default="texture.png", help="Output PNG path")
    parser.add_argument("--theme", "-t", default="frost", choices=list(THEMES.keys()), help="Material theme")
    parser.add_argument("--size", "-s", type=int, default=64, help="Texture resolution (default 64)")
    args = parser.parse_args()

    generate_texture(args.output, args.theme, args.size)
