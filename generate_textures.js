const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function createPNG(width, height, pixels, filePath) {
    const rawData = [];
    for (let y = 0; y < height; y++) {
        rawData.push(0); // Filter type 0
        for (let x = 0; x < width; x++) {
            const idx = (y * width + x) * 4;
            rawData.push(pixels[idx], pixels[idx+1], pixels[idx+2], pixels[idx+3]);
        }
    }

    const buffer = Buffer.from(rawData);
    const idatData = zlib.deflateSync(buffer);

    function createChunk(type, data) {
        const len = Buffer.alloc(4);
        len.writeUInt32BE(data.length, 0);
        const typeBuffer = Buffer.from(type, 'ascii');
        const body = Buffer.concat([typeBuffer, data]);
        
        let crc = 0xFFFFFFFF;
        for (let i = 0; i < body.length; i++) {
            crc ^= body[i];
            for (let j = 0; j < 8; j++) {
                crc = (crc >>> 1) ^ (crc & 1 ? 0xEDB88320 : 0);
            }
        }
        crc = (crc ^ 0xFFFFFFFF) >>> 0;
        
        const crcBuffer = Buffer.alloc(4);
        crcBuffer.writeUInt32BE(crc, 0);
        return Buffer.concat([len, body, crcBuffer]);
    }

    const signature = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8; ihdr[9] = 6; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;

    const png = Buffer.concat([
        signature,
        createChunk('IHDR', ihdr),
        createChunk('IDAT', idatData),
        createChunk('IEND', Buffer.alloc(0))
    ]);

    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    fs.writeFileSync(filePath, png);
    console.log(`[OK] PNG created: ${filePath}`);
}

const DARK_BROWN   = [90, 48, 20, 255];
const MID_BROWN    = [140, 75, 35, 255];
const LIGHT_BROWN  = [180, 105, 50, 255];
const GOLD_DARK    = [180, 130, 15, 255];
const GOLD_MID     = [245, 190, 30, 255];
const GOLD_LIGHT   = [255, 235, 90, 255];
const TRANSPARENT  = [0, 0, 0, 0];
const STEEL_LIGHT  = [230, 240, 255, 255];
const STEEL_MID    = [160, 170, 185, 255];
const STEEL_DARK   = [60, 65, 75, 255];
const RED_ACCENT   = [210, 25, 40, 255];
const BLACK_HILT   = [30, 30, 35, 255];

const itemDir = path.join(__dirname, 'src', 'main', 'resources', 'assets', 'corazonmod', 'textures', 'item');

// 1. MONEY POUCH (32x32)
const pouchPixels = new Array(32 * 32 * 4).fill(0);
function setPPixel(x, y, color) {
    if (x >= 0 && x < 32 && y >= 0 && y < 32) {
        const idx = (y * 32 + x) * 4;
        pouchPixels[idx] = color[0]; pouchPixels[idx+1] = color[1]; pouchPixels[idx+2] = color[2]; pouchPixels[idx+3] = color[3];
    }
}
for (let y = 0; y < 32; y++) {
    for (let x = 0; x < 32; x++) {
        if (y < 16 && x < 16) setPPixel(x, y, (x===0||x===15||y===0||y===15) ? DARK_BROWN : ((x+y)%3===0 ? LIGHT_BROWN : MID_BROWN));
        else if (y < 8 && x >= 16 && x < 24) setPPixel(x, y, (x+y)%2===0 ? GOLD_MID : GOLD_LIGHT);
        else if (y >= 16 && y < 24 && x < 16) setPPixel(x, y, y < 20 ? LIGHT_BROWN : MID_BROWN);
        else if (y < 16 && x >= 24 && x < 32) setPPixel(x, y, (x===27||y===8) ? GOLD_LIGHT : ((x>=25&&x<=29&&y>=6&&y<=10) ? GOLD_MID : GOLD_DARK));
    }
}
createPNG(32, 32, pouchPixels, path.join(itemDir, 'money_pouch.png'));

// 2. MAFIA DAGGER (16x16)
const daggerPixels = new Array(16 * 16 * 4).fill(0);
function setDPixel(x, y, color) {
    const idx = (y * 16 + x) * 4;
    daggerPixels[idx] = color[0]; daggerPixels[idx+1] = color[1]; daggerPixels[idx+2] = color[2]; daggerPixels[idx+3] = color[3];
}
for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
        if (x < 4) setDPixel(x, y, y%2===0 ? STEEL_DARK : STEEL_MID);
        else if (x < 8) setDPixel(x, y, STEEL_LIGHT);
        else if (x < 12) setDPixel(x, y, y<4 ? GOLD_LIGHT : GOLD_MID);
        else setDPixel(x, y, y<6 ? RED_ACCENT : BLACK_HILT);
    }
}
for (let i = 1; i < 15; i++) setDPixel(i, i, STEEL_LIGHT);
createPNG(16, 16, daggerPixels, path.join(itemDir, 'mafia_dagger.png'));

// 3. CORAZON SWORD (16x16)
const swordPixels = new Array(16 * 16 * 4).fill(0);
function setSPixel(x, y, color) {
    const idx = (y * 16 + x) * 4;
    swordPixels[idx] = color[0]; swordPixels[idx+1] = color[1]; swordPixels[idx+2] = color[2]; swordPixels[idx+3] = color[3];
}
for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
        if (x < 4) setSPixel(x, y, STEEL_MID);
        else if (x < 8) setSPixel(x, y, STEEL_LIGHT);
        else if (x < 12) setSPixel(x, y, GOLD_MID);
        else setSPixel(x, y, y<4 ? RED_ACCENT : DARK_BROWN);
    }
}
for (let i = 1; i < 15; i++) setSPixel(i, i, STEEL_LIGHT);
createPNG(16, 16, swordPixels, path.join(itemDir, 'corazon_sword.png'));

console.log('🎉 Solid Texture Atlases Generated Successfully!');
