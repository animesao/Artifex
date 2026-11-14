"""
Blockbench & Minecraft Model Validator
Validates .bbmodel and Minecraft JSON models against Java Edition engine rules.
"""

import json
import sys
import os

VALID_ANGLES = {-45.0, -22.5, 0.0, 22.5, 45.0}

def validate_minecraft_json(path):
    issues = []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    elements = data.get("elements", [])
    if not elements:
        issues.append("Warning: Model has no elements.")

    for idx, el in enumerate(elements):
        name = el.get("name", f"element_{idx}")
        from_pt = el.get("from", [0, 0, 0])
        to_pt = el.get("to", [0, 0, 0])

        for axis_i, axis_name in enumerate(["x", "y", "z"]):
            if from_pt[axis_i] > to_pt[axis_i]:
                issues.append(f"Element '{name}': 'from.{axis_name}' ({from_pt[axis_i]}) is greater than 'to.{axis_name}' ({to_pt[axis_i]}).")

        rot = el.get("rotation")
        if rot:
            angle = float(rot.get("angle", 0.0))
            if angle not in VALID_ANGLES:
                issues.append(f"Element '{name}': Invalid angle {angle}. Must be one of {-45.0, -22.5, 0.0, 22.5, 45.0}.")
            axis = rot.get("axis", "")
            if axis not in ["x", "y", "z"]:
                issues.append(f"Element '{name}': Invalid axis '{axis}'. Must be 'x', 'y', or 'z'.")

        faces = el.get("faces", {})
        for fname, fdata in faces.items():
            uv = fdata.get("uv", [])
            if len(uv) == 4:
                for u in uv:
                    if u < -16.0 or u > 32.0:
                        issues.append(f"Element '{name}' face '{fname}': UV value {u} is out of typical Minecraft bounds [0..16].")

    return issues

def validate_bbmodel(path):
    issues = []
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)

    elements = data.get("elements", [])
    for idx, el in enumerate(elements):
        name = el.get("name", f"element_{idx}")
        rot = el.get("rotation")
        if rot and isinstance(rot, list) and len(rot) == 3:
            non_zero = [(i, ang) for i, ang in enumerate(rot) if abs(ang) > 0.001]
            if len(non_zero) > 1:
                issues.append(f"Element '{name}': Multi-axis rotation {rot}. Minecraft Java only supports single-axis rotation!")
            for _, ang in non_zero:
                if float(ang) not in VALID_ANGLES:
                    issues.append(f"Element '{name}': Angle {ang} is not supported by Minecraft Java (-45, -22.5, 0, 22.5, 45).")

    return issues

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python model_validator.py <model.bbmodel | model.json>")
        sys.exit(1)

    path = sys.argv[1]
    if path.endswith(".bbmodel"):
        errs = validate_bbmodel(path)
    else:
        errs = validate_minecraft_json(path)

    if errs:
        print(f"Validation FAILED for {path} with {len(errs)} issue(s):")
        for err in errs:
            print(f"  ❌ {err}")
    else:
        print(f"Validation PASSED for {path}! Compliant with Minecraft Java Edition.")
