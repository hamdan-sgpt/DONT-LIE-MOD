====================================================
  MAP STRUCTURE NBT FOR DON'T LIE MOD
====================================================

Lokasi file NBT struktur map:
src/main/resources/assets/corazonmod/structures/map_dont_lie.nbt

Cara kerja:
1. Simpan/eksport struktur map Minecraft kamu menjadi file NBT bernama `map_dont_lie.nbt`.
2. Taruh file `map_dont_lie.nbt` di folder ini (assets/corazonmod/structures/).
3. Build mod (`./gradlew build`). File map akan otomatis ter-pack ke dalam `.jar` mod.
4. Di dalam game server manapun, ketika admin menekan tombol `GENERATE MAP ARENA` di GUI (tombol G), mod akan membaca file NBT dari dalam JAR dan langsung men-spawn map `map_dont_lie` di posisi admin server!
