import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Wandelt die WebP-Fotos aus einem BeReal-Datenexport (DSGVO-Export) in JPGs um
 * und setzt das EXIF-Aufnahmedatum auf den Zeitpunkt aus posts.json.
 *
 * Aufruf: BeRealSetDateTime <posts.json> <export-ordner> <ausgabe-ordner>
 * WebP-Unterstützung kommt über das ImageIO-Plugin von TwelveMonkeys (siehe pom.xml).
 */
public class BeRealSetDateTime {
    static class Post {
        ImageDetails primary;
        String takenAt;

        static class ImageDetails {
            String path;
        }
    }

    private static final DateTimeFormatter EXIF_FORMAT =
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Aufruf: BeRealSetDateTime <posts.json> <export-ordner> <ausgabe-ordner>");
            System.exit(2);
        }
        Path jsonFilePath = Paths.get(args[0]);
        Path imageFolderPath = Paths.get(args[1]);
        Path outputFolderPath = Paths.get(args[2]);
        Files.createDirectories(outputFolderPath);

        List<Post> posts = readJsonFile(jsonFilePath);
        if (posts == null) {
            System.exit(1);
        }

        for (Post post : posts) {
            if (post.primary == null || post.primary.path == null || post.takenAt == null) {
                continue;
            }
            // Pfade im Export beginnen mit "/", relativ zum Export-Ordner auflösen
            Path imagePath = imageFolderPath.resolve(post.primary.path.replaceFirst("^/+", ""));
            if (!Files.exists(imagePath)) {
                System.err.println("Nicht gefunden: " + imagePath);
                continue;
            }
            try {
                BufferedImage webpImage = ImageIO.read(imagePath.toFile());
                if (webpImage == null) {
                    System.err.println("Kein lesbares Bild (WebP-Plugin fehlt?): " + imagePath);
                    continue;
                }
                Path outputPath = outputFolderPath.resolve(
                        imagePath.getFileName().toString().replaceFirst("(?i)\\.webp$", ".jpg"));
                ImageIO.write(toRgb(webpImage), "jpg", outputPath.toFile());

                String newDate = EXIF_FORMAT.format(Instant.parse(post.takenAt));
                changeExifDate(outputPath.toFile(), newDate);
            } catch (IOException | RuntimeException e) {
                System.err.println("Fehler beim Konvertieren von: " + imagePath.getFileName() + " - " + e.getMessage());
            }
        }
    }

    /** JPG kann keinen Alphakanal speichern, daher auf weißen Hintergrund zeichnen. */
    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.drawImage(src, 0, 0, Color.WHITE, null);
        g.dispose();
        return rgb;
    }

    private static List<Post> readJsonFile(Path jsonFilePath) {
        try (Reader reader = Files.newBufferedReader(jsonFilePath)) {
            Type listType = new TypeToken<List<Post>>() {}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (IOException e) {
            System.err.println("Konnte " + jsonFilePath + " nicht lesen: " + e.getMessage());
            return null;
        }
    }

    private static void changeExifDate(File file, String newDate) {
        try (FileInputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            TiffOutputSet outputSet = null;
            try {
                ImageMetadata metadata = Imaging.getMetadata(inputStream, file.getName());
                if (metadata instanceof JpegImageMetadata jpegMetadata && jpegMetadata.getExif() != null) {
                    outputSet = jpegMetadata.getExif().getOutputSet();
                }
            } catch (NullPointerException e) {
                // keine EXIF-Daten vorhanden
            }
            if (outputSet == null) {
                outputSet = new TiffOutputSet();
            }

            TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, newDate);

            try (FileInputStream original = new FileInputStream(file)) {
                new ExifRewriter().updateExifMetadataLossless(original, outputStream, outputSet);
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                outputStream.writeTo(fileOutputStream);
            }

            System.out.println("Metadaten für Datei " + file.getName() + " erfolgreich geändert (" + newDate + ").");
        } catch (Exception e) {
            System.err.println("Fehler beim Bearbeiten der Datei " + file.getName() + ": " + e.getMessage());
        }
    }
}
