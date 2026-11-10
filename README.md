# FotoMetaDaten

Kleine Java-Werkzeuge rund um das EXIF-Aufnahmedatum (`DateTimeOriginal`) von Fotos.

> **English:** Small Java CLI tools to set or filter the EXIF capture date of JPG photos,
> and to convert a BeReal data export (WebP) to JPGs with the correct capture date.

| Klasse | Zweck |
|---|---|
| `SetDateTime` | Setzt bei allen JPGs eines Ordners (rekursiv) dasselbe Aufnahmedatum |
| `GetFilesDateAfter` | Listet alle JPGs eines Ordners, die nach einem Datum aufgenommen wurden (und solche ohne Datum) |
| `BeRealSetDateTime` | Wandelt die WebP-Fotos eines BeReal-Datenexports in JPGs um und setzt das Datum aus `posts.json` |

**Achtung:** `SetDateTime` überschreibt die Originaldateien. Vorher eine Kopie anlegen.

## Voraussetzungen

Java 21 und Maven.

## Benutzung

```bash
mvn compile

# Aufnahmedatum aller JPGs in einem Ordner setzen
mvn -q exec:java -Dexec.mainClass=SetDateTime -Dexec.args="'C:\Fotos\Ordner' '2024:07:06 12:00:00'"

# Fotos auflisten, die nach dem 1.12.2024 aufgenommen wurden
mvn -q exec:java -Dexec.mainClass=GetFilesDateAfter -Dexec.args="'C:\Fotos\Ordner' '2024:12:01 00:00:00'"

# BeReal-Export (entpackt) in JPGs mit richtigem Datum umwandeln
mvn -q exec:java -Dexec.mainClass=BeRealSetDateTime -Dexec.args="'C:\BeReal\posts.json' 'C:\BeReal' 'C:\BeReal\jpg'"
```

Datumsformat immer `yyyy:MM:dd HH:mm:ss` (EXIF-Format). Bei `BeRealSetDateTime` wird
`takenAt` aus `posts.json` in die lokale Zeitzone umgerechnet.

## Abhängigkeiten

- [Apache Commons Imaging](https://commons.apache.org/proper/commons-imaging/) zum Lesen/Schreiben von EXIF
- [Gson](https://github.com/google/gson) zum Lesen von `posts.json`
- [TwelveMonkeys ImageIO WebP](https://github.com/haraldk/TwelveMonkeys) zum Lesen von WebP

## Lizenz

[MIT](LICENSE)
