const fs = require('fs');
const zlib = require('zlib');
const path = require('path');

function inspectDatFile(filePath) {
    console.log("=== Inspecting:", filePath);
    try {
        const buffer = fs.readFileSync(filePath);
        let decompressed;
        try {
            decompressed = zlib.gunzipSync(buffer);
        } catch(e) {
            decompressed = buffer;
        }
        
        console.log("Decompressed byte length:", decompressed.length);
        
        // Find strings in latin1
        const str = decompressed.toString('latin1');
        
        // Search for SpawnX, SpawnY, SpawnZ
        let spawnX = null, spawnY = null, spawnZ = null;
        let posIdx = str.indexOf('Pos');
        
        console.log("Pos index in file:", posIdx);
        
        // Print all printable ASCII text snippets that look like tags or names
        const matches = str.match(/[a-zA-Z0-9_\:\-\.]{4,}/g);
        if (matches) {
            console.log("Tags/Names found:", Array.from(new Set(matches)).slice(0, 30));
        }
        
        // Search for SpawnX, SpawnY, SpawnZ integer values
        const xIdx = str.indexOf('SpawnX');
        const yIdx = str.indexOf('SpawnY');
        const zIdx = str.indexOf('SpawnZ');
        if (xIdx !== -1 && yIdx !== -1 && zIdx !== -1) {
            spawnX = decompressed.readInt32BE(xIdx + 6 + 4); // TagInt type 3: 1 byte tag + 2 byte name len + 6 byte name ('SpawnX') + 4 byte int
        }
        
        // Search for 'Pos' TagList (type 6 Double)
        if (posIdx !== -1) {
            console.log("Bytes around Pos:", decompressed.subarray(posIdx, posIdx + 40));
            // Read 3 doubles after Pos tag header
            try {
                // Pos tag header: 1 byte tag type (9 = List), 2 byte name len (3), 3 bytes ("Pos"), 1 byte element type (6 = Double), 4 bytes list size (3)
                let offset = posIdx + 1 + 2 + 3 + 1 + 4;
                let d1 = decompressed.readDoubleBE(offset);
                let d2 = decompressed.readDoubleBE(offset + 8);
                let d3 = decompressed.readDoubleBE(offset + 16);
                console.log("FOUND PLAYER POS DOUBLES:", d1, d2, d3);
            } catch(e) {
                console.error("Pos read error:", e.message);
            }
        }

    } catch (e) {
        console.error("Error inspecting:", e.message);
    }
}

inspectDatFile(path.join(__dirname, 'MAP-DONT-LIE', 'level.dat'));
inspectDatFile(path.join(__dirname, 'MAP-DONT-LIE', 'playerdata', '5df9e42b-3bb0-4f53-a926-10e7fab544a5.dat'));
