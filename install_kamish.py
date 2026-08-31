import os
import shutil
import json

base_dir = r"d:\codingan\A-project-corazon"
src_png = os.path.join(base_dir, "kamish-dagger-1-20-1-e1560", "assets", "minecraft", "optifine", "cit", "kamish_dagger", "kamish_dagger.png")
src_json = os.path.join(base_dir, "kamish-dagger-1-20-1-e1560", "assets", "minecraft", "optifine", "cit", "kamish_dagger", "kamish_dagger.json")

tex_dir = os.path.join(base_dir, "src", "main", "resources", "assets", "corazonmod", "textures", "item")
model_dir = os.path.join(base_dir, "src", "main", "resources", "assets", "corazonmod", "models", "item")

os.makedirs(tex_dir, exist_ok=True)
os.makedirs(model_dir, exist_ok=True)

# 1. Copy PNG Textures
shutil.copyfile(src_png, os.path.join(tex_dir, "mafia_dagger.png"))
shutil.copyfile(src_png, os.path.join(tex_dir, "kamish_dagger.png"))
print("[OK] Texture PNG files copied!")

# 2. Read and update model JSON for mafia_dagger.json
with open(src_json, "r", encoding="utf-8") as f:
    model_data = json.load(f)

# Update texture reference to corazonmod:item/mafia_dagger
model_data["textures"] = {
    "1": "corazonmod:item/mafia_dagger",
    "particle": "corazonmod:item/mafia_dagger"
}

target_mafia = os.path.join(model_dir, "mafia_dagger.json")
with open(target_mafia, "w", encoding="utf-8") as f:
    json.dump(model_data, f, indent=2)
print(f"[OK] {target_mafia} updated with 3D Kamish Dagger model!")

# Update texture reference for kamish_dagger.json
model_data["textures"] = {
    "1": "corazonmod:item/kamish_dagger",
    "particle": "corazonmod:item/kamish_dagger"
}
target_kamish = os.path.join(model_dir, "kamish_dagger.json")
with open(target_kamish, "w", encoding="utf-8") as f:
    json.dump(model_data, f, indent=2)
print(f"[OK] {target_kamish} updated with 3D Kamish Dagger model!")

print("\n🎉 SUCCESS! 3D Kamish Dagger model installed to corazonmod!")
