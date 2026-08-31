const fs = require('fs');
const path = require('path');

const mapDir = path.join(__dirname, 'MAP-DONT-LIE');
const regionDir = path.join(mapDir, 'region');

let log = [];
log.push(`Checking map directory: ${mapDir}`);

if (fs.existsSync(mapDir)) {
    const rootFiles = fs.readdirSync(mapDir);
    log.push(`Root files: ${rootFiles.join(', ')}`);
} else {
    log.push("MAP-DONT-LIE directory does not exist!");
}

if (fs.existsSync(regionDir)) {
    const mcaFiles = fs.readdirSync(regionDir).filter(f => f.endsWith('.mca'));
    log.push(`Total MCA files in region: ${mcaFiles.length}`);

    const fileInfos = mcaFiles.map(f => {
        const stat = fs.statSync(path.join(regionDir, f));
        const parts = f.replace('.mca', '').split('.');
        const rx = parseInt(parts[1], 10);
        const rz = parseInt(parts[2], 10);
        return {
            name: f,
            size: stat.size,
            rx: rx,
            rz: rz,
            minX: rx * 512,
            maxX: rx * 512 + 511,
            minZ: rz * 512,
            maxZ: rz * 512 + 511
        };
    });

    fileInfos.sort((a, b) => b.size - a.size);

    log.push("\nTOP 20 LARGEST MCA FILES:");
    fileInfos.slice(0, 20).forEach(fi => {
        log.push(`${fi.name} | Size: ${(fi.size / 1024 / 1024).toFixed(2)} MB | Center X: ${fi.minX + 256}, Z: ${fi.minZ + 256} | Bounds: X[${fi.minX}..${fi.maxX}] Z[${fi.minZ}..${fi.maxZ}]`);
    });
} else {
    log.push("Region directory does not exist!");
}

fs.writeFileSync(path.join(__dirname, 'map_check_output.txt'), log.join('\n'));
