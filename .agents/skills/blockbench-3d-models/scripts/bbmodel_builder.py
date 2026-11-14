"""
Blockbench Model Builder & Generator
Utility script to programmatically create .bbmodel projects with valid Minecraft Java geometry.
"""

import json
import base64
import os

class BBModelBuilder:
    def __init__(self, model_name, resolution=(64, 64)):
        self.name = model_name
        self.res_w, self.res_h = resolution
        self.elements = []
        self.textures = []
        self.display = {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
            "gui": {"rotation": [0, 0, -45], "translation": [0, 0, 0], "scale": [0.7, 0.7, 0.7]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.8, 0.8, 0.8]}
        }

    def set_display(self, display_dict):
        self.display = display_dict
        return self

    def add_texture(self, texture_id="0", name=None, png_path=None, base64_png=None):
        if name is None:
            name = f"{self.name}.png"
        
        if png_path and os.path.exists(png_path):
            with open(png_path, "rb") as f:
                b64 = "data:image/png;base64," + base64.b64encode(f.read()).decode("utf-8")
        elif base64_png:
            b64 = base64_png if base64_png.startswith("data:image/png;base64,") else "data:image/png;base64," + base64_png
        else:
            b64 = ""

        self.textures.append({
            "name": name,
            "folder": "item",
            "namespace": "artifex",
            "id": str(texture_id),
            "particle": False,
            "render_mode": "default",
            "visible": True,
            "mode": "bitmap",
            "saved": True,
            "uuid": f"tex-{texture_id}",
            "source": b64
        })
        return self

    def add_cube(self, name, origin_box, size_box, uv_rect, rotation=None, rot_origin=None, texture_id=0):
        """
        origin_box: [x, y, z] minimum point
        size_box: [w, h, d]
        uv_rect: [u1, v1, u2, v2] pixel coordinates in texture resolution
        rotation: (axis, angle) where axis is 'x', 'y', 'z' and angle in [-45, -22.5, 0, 22.5, 45]
        rot_origin: [ox, oy, oz] pivot point
        """
        x, y, z = origin_box
        w, h, d = size_box
        to_box = [round(x + w, 2), round(y + h, 2), round(z + d, 2)]

        faces = {}
        for face in ["north", "east", "south", "west", "up", "down"]:
            faces[face] = {
                "uv": list(uv_rect),
                "texture": texture_id
            }

        el = {
            "name": name,
            "box_uv": False,
            "rescale": False,
            "locked": False,
            "render_order": "default",
            "allow_mirror_modeling": True,
            "from": [round(x, 2), round(y, 2), round(z, 2)],
            "to": to_box,
            "autouv": 0,
            "color": 0,
            "origin": rot_origin if rot_origin else [round(x + w/2, 2), round(y + h/2, 2), round(z + d/2, 2)],
            "faces": faces,
            "type": "cube",
            "uuid": f"el-{len(self.elements)}"
        }

        if rotation:
            axis, angle = rotation
            # Ensure Minecraft Java compliance
            rot_vec = [0.0, 0.0, 0.0]
            if axis.lower() == 'x': rot_vec[0] = float(angle)
            elif axis.lower() == 'y': rot_vec[1] = float(angle)
            elif axis.lower() == 'z': rot_vec[2] = float(angle)
            el["rotation"] = rot_vec

        self.elements.append(el)
        return self

    def build_bbmodel(self):
        return {
            "meta": {
                "format_version": "4.10",
                "creation_time": 1700000000,
                "model_format": "free",
                "box_uv": False
            },
            "name": self.name,
            "model_identifier": self.name,
            "geometry_name": self.name,
            "resolution": {
                "width": self.res_w,
                "height": self.res_h
            },
            "elements": self.elements,
            "outliner": [el["uuid"] for el in self.elements],
            "textures": self.textures,
            "display": self.display
        }

    def save(self, filepath):
        os.makedirs(os.path.dirname(os.path.abspath(filepath)), exist_ok=True)
        data = self.build_bbmodel()
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
        print(f"Saved BBModel: {filepath} ({len(self.elements)} elements)")
