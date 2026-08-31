import os
import struct
import zlib

def create_png(width, height, pixels, filepath):
    """
    Creates a valid PNG file without external dependencies (pure Python stdlib).
    pixels: list of RGBA tuples (r, g, b, a) row by row.
    """
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # Filter type 0 (None)
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            raw_data.extend([r, g, b, a])

    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw_data))

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'wb') as f:
        f.write(png)
    print(f"[OK] Texture generated: {filepath}")

# Color palette (RGBA)
DARK_BROWN = (90, 48, 20, 255)
MID_BROWN  = (140, 75, 35, 255)
LIGHT_BROWN= (180, 105, 50, 255)
GOLD_DARK  = (180, 130, 15, 255)
GOLD_MID   = (245, 190, 30, 255)
GOLD_LIGHT = (255, 235, 90, 255)
TRANSPARENT= (0, 0, 0, 0)
STEEL_DARK = (60, 65, 75, 255)
STEEL_MID  = (160, 170, 185, 255)
STEEL_LIGHT= (230, 240, 255, 255)
RED_ACCENT = (210, 25, 40, 255)
BLACK_HILT = (30, 30, 35, 255)

# --- 1. MONEY POUCH (32x32 Texture Atlas) ---
pouch_pixels = [TRANSPARENT] * (32 * 32)
def set_p_pixel(x, y, color):
    if 0 <= x < 32 and 0 <= y < 32:
        pouch_pixels[y * 32 + x] = color

for y in range(32):
    for x in range(32):
        if y < 16 and x < 16:
            set_p_pixel(x, y, DARK_BROWN if (x in (0, 15) or y in (0, 15)) else (LIGHT_BROWN if (x+y)%3==0 else MID_BROWN))
        elif y < 8 and 16 <= x < 24:
            set_p_pixel(x, y, GOLD_MID if (x+y)%2==0 else GOLD_LIGHT)
        elif 16 <= y < 24 and x < 16:
            set_p_pixel(x, y, LIGHT_BROWN if y < 20 else MID_BROWN)
        elif 0 <= y < 16 and 24 <= x < 32:
            set_p_pixel(x, y, GOLD_LIGHT if (x==27 or y==8) else (GOLD_MID if (25<=x<=29 and 6<=y<=10) else GOLD_DARK))

create_png(32, 32, pouch_pixels, r"src/main/resources/assets/corazonmod/textures/item/money_pouch.png")

# --- 2. MAFIA DAGGER (16x16 Solid UV Atlas for 3D & 2D) ---
dagger_pixels = [TRANSPARENT] * (16 * 16)
def set_d_pixel(x, y, c): dagger_pixels[y * 16 + x] = c

for y in range(16):
    for x in range(16):
        if 0 <= x < 4: # Netherite steel blade
            set_d_pixel(x, y, STEEL_DARK if y % 2 == 0 else STEEL_MID)
        elif 4 <= x < 8: # Blade edge
            set_d_pixel(x, y, STEEL_LIGHT)
        elif 8 <= x < 12: # Gold guard & pommel
            set_d_pixel(x, y, GOLD_LIGHT if y < 4 else GOLD_MID)
        elif 12 <= x < 16: # Black hilt & red blood stripe
            set_d_pixel(x, y, RED_ACCENT if y < 6 else BLACK_HILT)

# Overlay diagonal dagger for 2D handheld rendering
for i in range(1, 15):
    set_d_pixel(i, i, STEEL_LIGHT)
    if i < 14: set_d_pixel(i+1, i, STEEL_DARK)

create_png(16, 16, dagger_pixels, r"src/main/resources/assets/corazonmod/textures/item/mafia_dagger.png")

# --- 3. CORAZON SWORD (16x16 Solid UV Atlas for 3D & 2D) ---
sword_pixels = [TRANSPARENT] * (16 * 16)
def set_s_pixel(x, y, c): sword_pixels[y * 16 + x] = c

for y in range(16):
    for x in range(16):
        if 0 <= x < 4: set_s_pixel(x, y, STEEL_MID)
        elif 4 <= x < 8: set_s_pixel(x, y, STEEL_LIGHT)
        elif 8 <= x < 12: set_s_pixel(x, y, GOLD_MID)
        elif 12 <= x < 16: set_s_pixel(x, y, RED_ACCENT if y < 4 else DARK_BROWN)

for i in range(1, 15):
    set_s_pixel(i, i, STEEL_LIGHT)

create_png(16, 16, sword_pixels, r"src/main/resources/assets/corazonmod/textures/item/corazon_sword.png")

print("\n🎉 ALL SOLID TEXTURE ATLASES CREATED SUCCESSFULLY!")
