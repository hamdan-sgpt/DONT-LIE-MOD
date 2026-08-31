const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function recolorPNG(srcPath, dstPaths) {
    const data = fs.readFileSync(srcPath);

    // Read PNG chunks
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

    const compressedIDAT = Buffer.concat(idatBuffers);
    const uncompressed = zlib.inflateSync(compressedIDAT);

    const bytesPerPixel = 4;
    const stride = width * bytesPerPixel + 1; // +1 filter byte per scanline
    const outputBuffer = Buffer.from(uncompressed);

    for (let y = 0; y < height; y++) {
        const lineOffset = y * stride + 1; // skip filter byte
        for (let x = 0; x < width; x++) {
            const p = lineOffset + x * 4;
            const r = outputBuffer[p];
            const g = outputBuffer[p+1];
            const b = outputBuffer[p+2];
            const a = outputBuffer[p+3];

            if (a < 10) continue;

            // Calculate luminance
            const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;

            const isRedPurple = (r > 120 && b > 100) || (r > 150 && g < 100);
            const isYellowGold = (r > 140 && g > 120 && b < 100);

            let nr, ng, nb;

            if (isRedPurple) {
                // Antique Gold & Crimson Ruby Accent
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
                // Antique Bronze / Gold Trim
                nr = Math.min(255, Math.floor(lum * 230 + 30));
                ng = Math.min(255, Math.floor(lum * 170 + 20));
                nb = Math.min(255, Math.floor(lum * 60 + 10));
            } else {
                // Damascus Steel Blade (Dark Steel -> Silver Edge)
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

            outputBuffer[p] = nr;
            outputBuffer[p+1] = ng;
            outputBuffer[p+2] = nb;
        }
    }

    const newIDAT = zlib.deflateSync(outputBuffer);

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
        createChunk('IDAT', newIDAT),
        createChunk('IEND', Buffer.alloc(0))
    ]);

    for (const dstPath of dstPaths) {
        fs.mkdirSync(path.dirname(dstPath), { recursive: true });
        fs.writeFileSync(dstPath, newPNG);
        console.log(`[OK] Created Semi-Medieval PNG: ${dstPath}`);
    }
}

const srcPng = path.join(__dirname, 'kamish-dagger-1-20-1-e1560', 'assets', 'minecraft', 'optifine', 'cit', 'kamish_dagger', 'kamish_dagger.png');
const itemDir = path.join(__dirname, 'src', 'main', 'resources', 'assets', 'corazonmod', 'textures', 'item');

recolorPNG(srcPng, [
    path.join(itemDir, 'mafia_dagger.png'),
    path.join(itemDir, 'kamish_dagger.png')
]);
