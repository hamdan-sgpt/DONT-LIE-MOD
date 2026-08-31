const fs = require('fs');
const path = require('path');

const regionDir = path.join(__dirname, 'MAP-DONT-LIE', 'region');
let log = [];

if (fs.existsSync(regionDir)) {
    const files = fs.readdirSync(regionDir);
    const mcaFiles = files.filter(f => f.endsWith('.mca')).map(f => {
        const stat = fs.statSync(path.join(regionDir, f));
        const parts = f.replace('.mca', '').split('.');
        const rx = parseInt(parts[1], 10);
        const rz = parseInt(parts[2], 10);
        return {
            name: f,
            size: stat.size,
            rx: rx,
            rz: rz,
            minBlockX: rx * 512,
            maxBlockX: rx * 512 + 511,
            minBlockZ: rz * 512,
            maxBlockZ: rz * 512 + 511
        };
    });

    mcaFiles.sort((a, b) => b.size - a.size);

    log.push("TOP 20 LARGEST REGION FILES IN MAP-DONT-LIE:");
    mcaFiles.slice(0, 20).forEach(m => {
        log.push(`${m.name} | Size: ${(m.size / 1024 / 1024).toFixed(2)} MB | Center Block X: ${m.minBlockX + 256}, Z: ${m.minBlockZ + 256} | Bounds: X[${m.minBlockX}..${m.maxBlockX}] Z[${m.minBlockZ}..${m.maxBlockZ}]`);
    });
} else {
    log.push("Region dir not found!");
}

fs.writeFileSync(path.join(__dirname, 'mansion_coords.txt'), log.join('\n'));
