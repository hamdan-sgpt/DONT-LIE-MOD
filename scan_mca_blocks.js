const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const regionDir = path.join(__dirname, 'MAP-DONT-LIE', 'region');
let log = [];

if (fs.existsSync(regionDir)) {
    const files = fs.readdirSync(regionDir).filter(f => f.endsWith('.mca'));
    log.push(`Total MCA files in MAP-DONT-LIE/region: ${files.length}`);

    // Sort files by size
    const fileInfos = files.map(f => {
        const stat = fs.statSync(path.join(regionDir, f));
        return { name: f, size: stat.size };
    }).sort((a, b) => b.size - a.size);

    log.push("\nTOP 30 LARGEST MCA FILES:");
    fileInfos.slice(0, 30).forEach(fi => {
        const parts = fi.name.replace('.mca', '').split('.');
        const rx = parseInt(parts[1], 10);
        const rz = parseInt(parts[2], 10);
        const bx = rx * 512;
        const bz = rz * 512;
        log.push(`${fi.name} (${(fi.size/1024/1024).toFixed(2)} MB) -> Center X: ${bx + 256}, Z: ${bz + 256} | Range: X[${bx}..${bx+511}], Z[${bz}..${bz+511}]`);
    });
} else {
    log.push("MAP-DONT-LIE/region directory not found!");
}

fs.writeFileSync(path.join(__dirname, 'mca_analysis.txt'), log.join('\n'));
