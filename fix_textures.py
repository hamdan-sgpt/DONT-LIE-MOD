import os
import struct
import zlib

def create_png(width, height, pixels, filepath):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # Filter type 0
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            raw_data.extend([r, g, b, a])

    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw_data))

    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'wb') as f:
        f.write(png)
    print(f"[OK] Created PNG: {filepath}")

# Color Palette (RGBA)
_ = (0, 0, 0, 0)                  # Transparent
K = (40, 20, 10, 255)             # Dark Outline
B = (140, 75, 25, 255)            # Base Brown Leather
L = (185, 110, 45, 255)           # Light Brown Highlight
D = (90, 45, 15, 255)             # Dark Brown Shadow
G = (255, 215, 0, 255)            # Bright Gold
Y = (255, 245, 140, 255)          # Light Gold Highlight
O = (190, 140, 10, 255)           # Gold Rope/Shadow

pouch_16x16 = [
    _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _,
    _, _, _, _, _, K, K, K, K, K, _, _, _, _, _, _,
    _, _, _, _, K, L, L, L, L, L, K, _, _, _, _, _,
    _, _, _, _, K, D, L, L, L, D, K, _, _, _, _, _,
    _, _, _, _, K, O, G, Y, G, O, K, _, _, _, _, _,
    _, _, _, K, D, B, B, B, B, B, D, K, _, _, _, _,
    _, _, K, D, B, L, L, L, L, B, B, D, K, _, _, _,
    _, _, K, B, L, Y, G, G, G, L, B, D, K, _, _, _,
    _, _, K, B, L, G, Y, G, Y, L, B, D, K, _, _, _,
    _, _, K, B, L, G, G, Y, G, L, B, D, K, _, _, _,
    _, _, K, B, L, Y, G, G, G, L, B, D, K, _, _, _,
    _, _, K, D, B, L, L, L, L, B, B, D, K, _, _, _,
    _, _, _, K, D, B, B, B, B, B, D, K, _, _, _, _,
    _, _, _, _, K, K, D, D, D, K, K, _, _, _, _, _,
    _, _, _, _, _, _, K, K, K, _, _, _, _, _, _, _,
    _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _
]

paths = [
    r"src/main/resources/assets/corazonmod/textures/item/money_pouch.png",
    r"bin/main/assets/corazonmod/textures/item/money_pouch.png",
    r"build/resources/main/assets/corazonmod/textures/item/money_pouch.png"
]

for p in paths:
    create_png(16, 16, pouch_16x16, p)
