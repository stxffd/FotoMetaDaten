import org.apache.commons.imaging.*;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;

import java.io.*;
import java.nio.file.*;
import java.util.List;

public class SetDateTime {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Aufruf: SetDateTime <ordner> \"<yyyy:MM:dd HH:mm:ss>\"");
            System.exit(2);
        }
        String folderPath = args[0];
        String newDate = args[1]; // Format "yyyy:MM:dd HH:mm:ss", z. B. "2024:07:06 00:00:00"

        try {
            // Liste aller JPG-Dateien im Ordner
            List<Path> jpgFiles = Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".jpg"))
                    .toList();

            for (Path file : jpgFiles) {
                System.out.println("Bearbeite Datei: " + file);

                // Metadaten ändern
                changeExifDate(file.toFile(), newDate);
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Lesen der Dateien: " + e.getMessage());
        }
    }

    private static void changeExifDate(File file, String newDate) {
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // EXIF-Daten aus der Datei lesen
            TiffOutputSet outputSet = null;
            try {
                // Metadaten mit InputStream laden
                ImageMetadata metadata = Imaging.getMetadata(inputStream, file.getName());
                if (metadata instanceof JpegImageMetadata jpegMetadata) {
                    outputSet = jpegMetadata.getExif().getOutputSet();
                }
            } catch (NullPointerException e) {
                // keine EXIF-Daten vorhanden
            }
            if (outputSet == null) {
                // Wenn keine EXIF-Daten vorhanden sind, erstellen wir ein neues Set
                outputSet = new TiffOutputSet();
            }

            // Datum ändern
            if (outputSet != null) {
                TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL); // Altes Datum entfernen
                exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, newDate);

                // Neue EXIF-Daten in die Datei schreiben
                new ExifRewriter().updateExifMetadataLossless(new FileInputStream(file), outputStream, outputSet);

                // Originaldatei mit neuen Metadaten überschreiben
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    outputStream.writeTo(fileOutputStream);
                }

                System.out.println("Metadaten für Datei " + file.getName() + " erfolgreich geändert.");
            } else {
                System.err.println("Konnte keine EXIF-Daten für Datei " + file.getName() + " finden.");
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Bearbeiten der Datei " + file.getName() + ": " + e.getMessage());
        }
    }
}
