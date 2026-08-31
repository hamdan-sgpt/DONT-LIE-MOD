const fs = require('fs');
const path = require('path');

const baseDir = __dirname;
const srcPng = path.join(baseDir, 'kamish-dagger-1-20-1-e1560', 'assets', 'minecraft', 'optifine', 'cit', 'kamish_dagger', 'kamish_dagger.png');
const dstDir = path.join(baseDir, 'src', 'main', 'resources', 'assets', 'corazonmod', 'textures', 'item');

fs.mkdirSync(dstDir, { recursive: true });

fs.copyFileSync(srcPng, path.join(dstDir, 'mafia_dagger.png'));
fs.copyFileSync(srcPng, path.join(dstDir, 'kamish_dagger.png'));

console.log('[RESTORED] Original Kamish Dagger PNG texture restored successfully!');
