const fs = require('fs');
const path = require('path');

const baseDir = __dirname;
const srcPng = path.join(baseDir, 'kamish-dagger-1-20-1-e1560', 'assets', 'minecraft', 'optifine', 'cit', 'kamish_dagger', 'kamish_dagger.png');
const srcJson = path.join(baseDir, 'kamish-dagger-1-20-1-e1560', 'assets', 'minecraft', 'optifine', 'cit', 'kamish_dagger', 'kamish_dagger.json');

const texDir = path.join(baseDir, 'src', 'main', 'resources', 'assets', 'corazonmod', 'textures', 'item');
const modelDir = path.join(baseDir, 'src', 'main', 'resources', 'assets', 'corazonmod', 'models', 'item');

fs.mkdirSync(texDir, { recursive: true });
fs.mkdirSync(modelDir, { recursive: true });

// Copy Textures
fs.copyFileSync(srcPng, path.join(texDir, 'mafia_dagger.png'));
fs.copyFileSync(srcPng, path.join(texDir, 'kamish_dagger.png'));
console.log('[OK] Texture PNG files copied!');

// Read model JSON
const rawData = fs.readFileSync(srcJson, 'utf-8');
const modelData = JSON.parse(rawData);

// Update mafia_dagger.json
modelData.textures = {
    '1': 'corazonmod:item/mafia_dagger',
    'particle': 'corazonmod:item/mafia_dagger'
};
fs.writeFileSync(path.join(modelDir, 'mafia_dagger.json'), JSON.stringify(modelData, null, 2));
console.log('[OK] mafia_dagger.json updated!');

// Update kamish_dagger.json
modelData.textures = {
    '1': 'corazonmod:item/kamish_dagger',
    'particle': 'corazonmod:item/kamish_dagger'
};
fs.writeFileSync(path.join(modelDir, 'kamish_dagger.json'), JSON.stringify(modelData, null, 2));
console.log('[OK] kamish_dagger.json updated!');

console.log('🎉 SUCCESS! Kamish Dagger model installed successfully!');
