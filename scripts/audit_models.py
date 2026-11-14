import os
import json

models_dir = r"D:\server\Artifex-ResourcePack\assets\artifex\models\item"
print("AUDITING MODELS IN:", models_dir)

files = sorted(os.listdir(models_dir))
for f in files:
    if not f.endswith(".json"):
        continue
    with open(os.path.join(models_dir, f), "r", encoding="utf-8") as fp:
        try:
            data = json.load(fp)
        except Exception as e:
            print(f"ERROR {f}: {e}")
            continue
    elements = data.get("elements", [])
    parent = data.get("parent")
    has_display = "display" in data
    
    # Check UV validity (0..16) and rotation constraints
    uv_ok = True
    rot_ok = True
    for el in elements:
        rot = el.get("rotation")
        if rot:
            axis = rot.get("axis")
            angle = rot.get("angle")
            if angle not in [-45, -22.5, 0, 22.5, 45] or axis not in ["x", "y", "z"]:
                rot_ok = False
        faces = el.get("faces", {})
        for face_name, face_data in faces.items():
            uv = face_data.get("uv")
            if uv:
                if any(x < 0 or x > 16 for x in uv):
                    uv_ok = False
                    
    print(f"{f:25} | elems: {len(elements):2} | parent: {str(parent):26} | disp: {has_display} | rot_ok: {rot_ok} | uv_ok: {uv_ok}")
