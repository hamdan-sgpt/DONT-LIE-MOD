import shutil
import os

src_png = r"d:\codingan\A-project-corazon\kamish-dagger-1-20-1-e1560\assets\minecraft\optifine\cit\kamish_dagger\kamish_dagger.png"
dst_dir = r"d:\codingan\A-project-corazon\src\main\resources\assets\corazonmod\textures\item"

os.makedirs(dst_dir, exist_ok=True)

# Copy to mafia_dagger.png
dst_mafia = os.path.join(dst_dir, "mafia_dagger.png")
shutil.copyfile(src_png, dst_mafia)
print(f"[OK] Copied {src_png} -> {dst_mafia}")

# Copy to kamish_dagger.png
dst_kamish = os.path.join(dst_dir, "kamish_dagger.png")
shutil.copyfile(src_png, dst_kamish)
print(f"[OK] Copied {src_png} -> {dst_kamish}")
