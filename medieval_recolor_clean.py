import struct
import zlib
import os

def decode_png(filepath):
    with open(filepath, 'rb') as f:
        data = f.read()
    
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    
    offset = 8
    width = 0
    height = 0
    idat_chunks = []
    
    while offset < len(data):
        length = struct.unpack('>I', data[offset:offset+4])[0]
        tag = data[offset+4:offset+8]
        body = data[offset+8:offset+8+length]
        if tag == b'IHDR':
            width, height, bitdepth, colortype, compression, filter_method, interlace = struct.unpack('>IIBBBBB', body)
        elif tag == b'IDAT':
            idat_chunks.append(body)
        offset += 12 + length
        
    raw = zlib.decompress(b''.join(idat_chunks))
    
    bpp = 4 # RGBA
    stride = width * bpp
    pixels = bytearray(height * stride)
    
    raw_offset = 0
    for y in range(height):
        filter_type = raw[raw_offset]
        raw_offset += 1
        scanline = raw[raw_offset:raw_offset+stride]
        raw_offset += stride
        
        dest_offset = y * stride
        
        for x in range(stride):
            filt = scanline[x]
            left = pixels[dest_offset + x - bpp] if x >= bpp else 0
            up = pixels[dest_offset - stride + x] if y > 0 else 0
            upleft = pixels[dest_offset - stride + x - bpp] if (y > 0 and x >= bpp) else 0
            
            if filter_type == 0:
                val = filt
            elif filter_type == 1: # Sub
                val = (filt + left) & 0xff
            elif filter_type == 2: # Up
                val = (filt + up) & 0xff
            elif filter_type == 3: # Average
                val = (filt + ((left + up) // 2)) & 0xff
            elif filter_type == 4: # Paeth
                p = left + up - upleft
                pa = abs(p - left)
                pb = abs(p - up)
                pc = abs(p - upleft)
                if pa <= pb and pa <= pc:
                    pr = left
                elif pb <= pc:
                    pr = up
                else:
                    pr = upleft
                val = (filt + pr) & 0xff
            else:
                val = filt
                
            pixels[dest_offset + x] = val
            
    return width, height, pixels

def encode_png(width, height, pixels, filepath):
    def chunk(tag, body):
        return struct.pack('>I', len(body)) + tag + body + struct.pack('>I', zlib.crc32(tag + body) & 0xffffffff)
        
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    raw_data = bytearray()
    
    stride = width * 4
    for y in range(height):
        raw_data.append(0) # Filter type 0 None
        raw_data.extend(pixels[y*stride:(y+1)*stride])
        
    idat = zlib.compress(bytes(raw_data))
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')
    
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'wb') as f:
        f.write(png)

def recolor_medieval_clean():
    src_png = r"d:\codingan\A-project-corazon\kamish-dagger-1-20-1-e1560\assets\minecraft\optifine\cit\kamish_dagger\kamish_dagger.png"
    dst_mafia = r"d:\codingan\A-project-corazon\src\main\resources\assets\corazonmod\textures\item\mafia_dagger.png"
    dst_kamish = r"d:\codingan\A-project-corazon\src\main\resources\assets\corazonmod\textures\item\kamish_dagger.png"
    
    width, height, pixels = decode_png(src_png)
    
    for i in range(0, len(pixels), 4):
        r, g, b, a = pixels[i], pixels[i+1], pixels[i+2], pixels[i+3]
        if a < 10:
            continue
            
        lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        
        is_red_purple = (r > 120 and b > 100) or (r > 150 and g < 100)
        is_yellow_gold = (r > 140 and g > 120 and b < 100)
        
        if is_red_purple:
            if lum > 0.6:
                nr, ng, nb = int(min(255, 255 * lum)), int(min(255, 215 * lum)), int(min(255, 90 * lum))
            else:
                nr, ng, nb = int(min(255, 200 * lum + 30)), int(min(255, 40 * lum)), int(min(255, 50 * lum))
        elif is_yellow_gold:
            nr = int(min(255, lum * 230 + 30))
            ng = int(min(255, lum * 170 + 20))
            nb = int(min(255, lum * 60 + 10))
        else:
            if lum > 0.7:
                nr = int(min(255, lum * 240 + 15))
                ng = int(min(255, lum * 245 + 10))
                nb = int(min(255, lum * 255))
            elif lum > 0.35:
                nr = int(min(255, lum * 170 + 20))
                ng = int(min(255, lum * 180 + 20))
                nb = int(min(255, lum * 200 + 25))
            else:
                nr = int(min(255, lum * 100 + 15))
                ng = int(min(255, lum * 85 + 10))
                nb = int(min(255, lum * 75 + 10))
                
        pixels[i] = nr
        pixels[i+1] = ng
        pixels[i+2] = nb
        
    encode_png(width, height, pixels, dst_mafia)
    encode_png(width, height, pixels, dst_kamish)
    print("[SUCCESS] Clean PNG un-filtered recolor completed!")

if __name__ == '__main__':
    recolor_medieval_clean()
