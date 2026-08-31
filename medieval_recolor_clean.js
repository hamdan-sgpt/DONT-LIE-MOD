const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function decodePNG(srcPath) {
    const data = fs.readFileSync(srcPath);
    let offset = 8;
    let width = 0, height = 0;
    let idatBuffers = [];

    while (offset < data.length) {
        const length = data.readUInt32BE(offset);
        const type = data.toString('ascii', offset + 4, offset + 8);
        const chunkData = data.slice(offset + 8, offset + 8 + length);

        if (type === 'IHDR') {
            width = chunkData.readUInt32BE(0);
            height = chunkData.readUInt32BE(4);
        } else if (type === 'IDAT') {
            idatBuffers.push(chunkData);
        }
        offset += 12 + length;
    }

    const raw = zlib.inflateSync(Buffer.concat(idatBuffers));
    const bpp = 4;
    const stride = width * bpp;
    const pixels = Buffer.alloc(height * stride);

    let rawOffset = 0;
    for (let y = 0; y < height; y++) {
        const filterType = raw[rawOffset++];
        const scanline = raw.slice(rawOffset, rawOffset + stride);
        rawOffset += stride;

        const destOffset = y * stride;
        for (let x = 0; x < stride; x++) {
            const filt = scanline[x];
            const left = x >= bpp ? pixels[destOffset + x - bpp] : 0;
            const up = y > 0 ? pixels[destOffset - stride + x] : 0;
            const upleft = (y > 0 && x >= bpp) ? pixels[destOffset - stride + x - bpp] : 0;

            let val = filt;
            if (filterType === 1) val = (filt + left) & 0xff;
            else if (filterType === 2) val = (filt + up) & 0xff;
            else if (filterType === 3) val = (filt + Math.floor((left + up) / 2)) & 0xff;
            else if (filterType === 4) {
                const p = left + up - upleft;
                const pa = Math.abs(p - left);
                const pb = Math.abs(p - up);
                const pc = Math.abs(p - upleft);
                let pr = left;
                if (pa <= pb && pa <= pc) pr = left;
                else if (pb <= pc) pr = up;
                else pr = upleft;
                val = (filt + pr) & 0xff;
            }
            pixels[destOffset + x] = val;
        }
    }
    return { width, height, pixels };
}

function encodePNG(width, height, pixels, dstPaths) {
    const stride = width * 4;
    const rawData = [];
    for (let y = 0; y < height; y++) {
        rawData.push(0); // filter 0
        for (let x = 0; x < stride; x++) {
            rawData.push(pixels[y * stride + x]);
        }
    }
    const idatData = zlib.deflateSync(Buffer.from(rawData));

    function createChunk(type, chunkData) {
        const len = Buffer.alloc(4);
        len.writeUInt32BE(chunkData.length, 0);
        const typeBuf = Buffer.from(type, 'ascii');
        const body = Buffer.concat([typeBuf, chunkData]);

        let crc = 0xFFFFFFFF;
        for (let i = 0; i < body.length; i++) {
            crc ^= body[i];
            for (let j = 0; j < 8; j++) {
                crc = (crc >>> 1) ^ (crc & 1 ? 0xEDB88320 : 0);
            }
        }
        crc = (crc ^ 0xFFFFFFFF) >>> 0;

        const crcBuf = Buffer.alloc(4);
        crcBuf.writeUInt32BE(crc, 0);
        return Buffer.concat([len, body, crcBuf]);
    }

    const signature = Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]);
    const ihdrBuf = Buffer.alloc(13);
    ihdrBuf.writeUInt32BE(width, 0);
    ihdrBuf.writeUInt32BE(height, 4);
    ihdrBuf[8] = 8; ihdrBuf[9] = 6; ihdrBuf[10] = 0; ihdrBuf[11] = 0; ihdrBuf[12] = 0;

    const newPNG = Buffer.concat([
        signature,
        createChunk('IHDR', ihdrBuf),
        createChunk('IDAT', idatData),
        createChunk('IEND', Buffer.alloc(0))
    ]);

    for (const dstPath of dstPaths) {
        fs.mkdirSync(path.dirname(dstPath), { recursive: true });
        fs.writeFileSync(dstPath, newPNG);
        console.log(`[OK] Created Clean Semi-Medieval PNG: ${dstPath}`);
    }
}

function processRecolor() {
    const srcPng = path.join(__dirname, 'kamish-dagger-1-20-1-e1560', 'assets', 'minecraft', 'optifine', 'cit', 'kamish_dagger', 'kamish_dagger.png');
    const itemDir = path.join(__dirname, 'src', 'main', 'resources', 'assets', 'corazonmod', 'textures', 'item');

    const { width, height, pixels } = decodePNG(srcPng);

    for (let i = 0; i < pixels.length; i += 4) {
        const r = pixels[i];
        const g = pixels[i+1];
        const b = pixels[i+2];
        const a = pixels[i+3];

        if (a < 10) continue;

        const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;

        const isRedPurple = (r > 120 && b > 100) || (r > 150 && g < 100);
        const isYellowGold = (r > 140 && g > 120 && b < 100);

        let nr, ng, nb;

        if (isRedPurple) {
            if (lum > 0.6) {
                nr = Math.min(255, Math.floor(255 * lum));
                ng = Math.min(255, Math.floor(215 * lum));
                nb = Math.min(255, Math.floor(90 * lum));
            } else {
                nr = Math.min(255, Math.floor(200 * lum + 30));
                ng = Math.min(255, Math.floor(40 * lum));
                nb = Math.min(255, Math.floor(50 * lum));
            }
        } else if (isYellowGold) {
            nr = Math.min(255, Math.floor(lum * 230 + 30));
            ng = Math.min(255, Math.floor(lum * 170 + 20));
            nb = Math.min(255, Math.floor(lum * 60 + 10));
        } else {
            if (lum > 0.7) {
                nr = Math.min(255, Math.floor(lum * 240 + 15));
                ng = Math.min(255, Math.floor(lum * 245 + 10));
                nb = Math.min(255, Math.floor(lum * 255));
            } else if (lum > 0.35) {
                nr = Math.floor(lum * 170 + 20);
                ng = Math.floor(lum * 180 + 20);
                nb = Math.floor(lum * 200 + 25);
            } else {
                nr = Math.floor(lum * 100 + 15);
                ng = Math.floor(lum * 85 + 10);
                nb = Math.floor(lum * 75 + 10);
            }
        }

        pixels[i] = nr;
        pixels[i+1] = ng;
        pixels[i+2] = nb;
    }

    encodePNG(width, height, pixels, [
        path.join(itemDir, 'mafia_dagger.png'),
        path.join(itemDir, 'kamish_dagger.png')
    ]);
}

processRecolor();
