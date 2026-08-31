const fs = require('fs');
const path = require('path');

const srcPng = path.join(__dirname, 'kamish-dagger-1-20-1-e1560', 'assets', 'minecraft', 'optifine', 'cit', 'kamish_dagger', 'kamish_dagger.png');
const dstDir = path.join(__dirname, 'src', 'main', 'resources', 'assets', 'corazonmod', 'textures', 'item');

fs.mkdirSync(dstDir, { recursive: true });

const dstMafia = path.join(dstDir, 'mafia_dagger.png');
const dstKamish = path.join(dstDir, 'kamish_dagger.png');

fs.copyFileSync(srcPng, dstMafia);
console.log(`[OK] Copied to ${dstMafia}`);

fs.copyFileSync(srcPng, dstKamish);
console.log(`[OK] Copied to ${dstKamish}`);
