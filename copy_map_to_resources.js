const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, 'MAP-DONT-LIE');
const destDir = path.join(__dirname, 'src', 'main', 'resources', 'maps', 'MAP-DONT-LIE');

function copyRecursiveSync(src, dest) {
    const exists = fs.existsSync(src);
    const stats = exists && fs.statSync(src);
    const isDirectory = exists && stats.isDirectory();
    if (isDirectory) {
        if (!fs.existsSync(dest)) {
            fs.mkdirSync(dest, { recursive: true });
        }
        fs.readdirSync(src).forEach((childItemName) => {
            // Skip unnecessary heavy folders to keep jar size optimal
            if (childItemName === 'playerdata' || childItemName === 'stats' || childItemName === 'advancements' || childItemName === 'session.lock') {
                return;
            }
            copyRecursiveSync(path.join(src, childItemName), path.join(dest, childItemName));
        });
    } else {
        fs.copyFileSync(src, dest);
    }
}

console.log("Copying MAP-DONT-LIE to src/main/resources/maps/MAP-DONT-LIE...");
copyRecursiveSync(srcDir, destDir);
console.log("Successfully copied map into mod resources!");
