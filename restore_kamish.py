import shutil
import os

base_dir = r"d:\codingan\A-project-corazon"
src_png = os.path.join(base_dir, "kamish-dagger-1-20-1-e1560", "assets", "minecraft", "optifine", "cit", "kamish_dagger", "kamish_dagger.png")
dst_dir = os.path.join(base_dir, "src", "main", "resources", "assets", "corazonmod", "textures", "item")

dst_mafia = os.path.join(dst_dir, "mafia_dagger.png")
dst_kamish = os.path.join(dst_dir, "kamish_dagger.png")

shutil.copyfile(src_png, dst_mafia)
shutil.copyfile(src_png, dst_kamish)

print("[RESTORED] Original Kamish Dagger PNG texture restored successfully!")
