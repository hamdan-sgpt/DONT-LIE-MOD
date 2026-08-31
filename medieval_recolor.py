import os
import sys

def recolor_medieval():
    src_png = r"d:\codingan\A-project-corazon\kamish-dagger-1-20-1-e1560\assets\minecraft\optifine\cit\kamish_dagger\kamish_dagger.png"
    dst_mafia = r"d:\codingan\A-project-corazon\src\main\resources\assets\corazonmod\textures\item\mafia_dagger.png"
    dst_kamish = r"d:\codingan\A-project-corazon\src\main\resources\assets\corazonmod\textures\item\kamish_dagger.png"

    try:
        from PIL import Image
        img = Image.open(src_png).convert('RGBA')
        pixels = img.load()
        width, height = img.size
        print(f"Processing {width}x{height} texture for Semi-Medieval theme...")

        for y in range(height):
            for x in range(width):
                r, g, b, a = pixels[x, y]
                if a < 10:
                    continue

                # Luminance / Brightness
                lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

                # Analyze original colors & recolor to Semi-Medieval theme:
                # 1. Purple/Magenta/Red Dragon Glow -> Medieval Crimson Ruby Gem & Antique Gold Trim
                # 2. Cyan/Blue/Dark -> Damascus Forged Steel (Silver-Blue Metallic Shading)
                # 3. Yellow/Brown -> Aged Leather Grip / Mahogany Wood
                
                is_red_purple = (r > 120 and b > 100) or (r > 150 and g < 100)
                is_yellow_gold = (r > 140 and g > 120 and b < 100)

                if is_red_purple:
                    # Antique Brass / Gold Guard & Crimson Ruby
                    if lum > 0.6:
                        nr, ng, nb = int(255 * lum), int(215 * lum), int(90 * lum) # Gold highlight
                    else:
                        nr, ng, nb = int(200 * lum + 30), int(40 * lum), int(50 * lum) # Crimson Ruby accent
                elif is_yellow_gold:
                    # Antique Bronze / Gold
                    nr = int(min(255, lum * 230 + 30))
                    ng = int(min(255, lum * 170 + 20))
                    nb = int(min(255, lum * 60 + 10))
                else:
                    # Damascus Steel Blade (Dark Iron -> Shiny Steel Edge)
                    if lum > 0.7: # Shiny steel edge
                        nr = int(min(255, lum * 240 + 15))
                        ng = int(min(255, lum * 245 + 10))
                        nb = int(min(255, lum * 255))
                    elif lum > 0.35: # Steel body
                        nr = int(lum * 170 + 20)
                        ng = int(lum * 180 + 20)
                        nb = int(lum * 200 + 25)
                    else: # Dark forged iron core & leather
                        nr = int(lum * 100 + 15)
                        ng = int(lum * 85 + 10)
                        nb = int(lum * 75 + 10)

                pixels[x, y] = (nr, ng, nb, a)

        img.save(dst_mafia)
        img.save(dst_kamish)
        print(f"[SUCCESS] Saved Semi-Medieval texture to:\n - {dst_mafia}\n - {dst_kamish}")
    except Exception as e:
        print(f"Error during recolor: {e}")

if __name__ == "__main__":
    recolor_medieval()
