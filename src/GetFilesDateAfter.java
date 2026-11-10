import org.apache.commons.imaging.*;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GetFilesDateAfter {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Aufruf: GetFilesDateAfter <ordner> \"<yyyy:MM:dd HH:mm:ss>\"");
            System.exit(2);
        }
        String folderPath = args[0];
        String targetDateString = args[1]; // Format "yyyy:MM:dd HH:mm:ss", z. B. "2024:12:01 00:00:00"

        try {
            // Ziel-Datum parsen
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            Date targetDate = dateFormat.parse(targetDateString);

            // Liste aller JPG-Dateien im Ordner
            List<Path> jpgFiles = Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".jpg"))
                    .toList();

            System.out.println("Fotos mit Aufnahmedatum nach " + targetDateString + ":");
            for (Path jpgFile : jpgFiles) {
                Date photoDate = getPhotoDate(jpgFile.toFile());
                if (photoDate == null) {
                    System.out.println("Ordner: " + jpgFile.getParent() + " | Datei: " + jpgFile.getFileName() + " - Kein Aufnahmedatum gefunden.");
                } else if (photoDate.after(targetDate)) {
                    System.out.println("Ordner: " + jpgFile.getParent() + " | Datei: " + jpgFile.getFileName() + " - Aufnahmedatum: " + dateFormat.format(photoDate));
                }
            }
        } catch (IOException | ParseException e) {
            System.err.println("Fehler: " + e.getMessage());
        }
    }

    private static Date getPhotoDate(File file) {
        try {
            // Metadaten der Datei abrufen
            ImageMetadata metadata = Imaging.getMetadata(file);
            if (metadata instanceof JpegImageMetadata jpegMetadata) {
                // EXIF-Datum auslesen
                TagInfo tagInfo = org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL;
                TiffField field = jpegMetadata.getExif().findField(tagInfo);
                if (field != null) {
                    String dateString = field.getStringValue();

                    // Datum im Format "YYYY:MM:DD HH:MM:SS" parsen
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    return dateFormat.parse(dateString);
                }
            }
        } catch (IOException | ParseException e) {
            System.err.println("Fehler beim Lesen der Metadaten von " + file.getName() + ": " + e.getMessage());
        }
        return null;
    }
}
