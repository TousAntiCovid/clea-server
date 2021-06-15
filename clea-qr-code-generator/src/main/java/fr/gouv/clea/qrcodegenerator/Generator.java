package fr.gouv.clea.qrcodegenerator;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfWriter;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.SneakyThrows;
import org.bouncycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

public class Generator {
    // server authority public key
    private static final String PK_SA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    // manual contact tracing authority public key
    private static final String PK_MCTA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    // number of codes to generate
    private static final int PLACES_NUMBER = 300000;

    // location configuration
    private static final int VENUE_TYPE = 1;
    private static final int VENUE_CAT_1 = 0;
    private static final int VENUE_CAT_2 = 0;
    private static final int PERIOD_DURATION = 255;
    private static final int QR_CODE_RENEWAL_INTERVAL = 0x1F;

    // output directory path
    private static final Path OUTPUT_DIR = Path.of("/tmp/codes");

    // true: generate qr code pdfs
    // false: generate csv with deeplinks
    private static final boolean generateQrCodes = true;

    // qr code size as px
    private static final int QR_SIZE_AS_PX = 277;

    public static void main(String[] args) throws IOException {
        final Generator generator = new Generator();
        final var urlsList = new ArrayList<URL>();
        Files.createDirectories(OUTPUT_DIR);
        IntStream.rangeClosed(1, PLACES_NUMBER).forEach(i -> {
            URL deepLink = generator.generateDeepLinkForRandomLocation();
            if (generateQrCodes) {
                generateQrCodeAndCreatePdf(i, deepLink);
            } else {
                urlsList.add(deepLink);
            }
        });
        if (!generateQrCodes) {
            generateCsvFromList(urlsList);
        }
    }

    private static void generateCsvFromList(ArrayList<URL> urlsList) throws IOException {
        FileWriter writer = new FileWriter(OUTPUT_DIR + "/codes.csv");
        final String collect = urlsList.stream()
                .map(URL::toString)
                .collect(Collectors.joining(",\n"));
        writer.write(collect);
        writer.close();
    }

    private static void generateQrCodeAndCreatePdf(int i, URL deepLink) {
        final var qrCode = new BarcodeQRCode(deepLink.toString(), QR_SIZE_AS_PX, QR_SIZE_AS_PX, null);
        final var filename = format("qrcode-%d.pdf", i);
        final File targetFile = OUTPUT_DIR.resolve(filename).toFile();
        createPdf(targetFile, qrCode);
    }

    private static void createPdf(File targetFile, BarcodeQRCode qrCode) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            Image image = qrCode.getImage();
            document.open();
            document.add(image);
            document.close();
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private URL generateDeepLinkForRandomLocation() {
        final var instant = Instant.now()
                .minus(365, DAYS)
                .truncatedTo(HOURS);
        final var location = createRandomLocation();
        return new URL(location.newDeepLink(instant));
    }

    private Location createRandomLocation() {
        return Location.builder()
                .manualContactTracingAuthorityPublicKey(PK_MCTA)
                .serverAuthorityPublicKey(PK_SA)
                .permanentLocationSecretKey(generatePermanentLocationSecretKey())
                .locationSpecificPart(createRandomLocationSpecificPart())
                .build();
    }

    private LocationSpecificPart createRandomLocationSpecificPart() {
        return LocationSpecificPart.builder()
                .staff(false)
                .periodDuration(PERIOD_DURATION /* unlimited */)
                .qrCodeRenewalIntervalExponentCompact(QR_CODE_RENEWAL_INTERVAL /* no renewal */)
                .venueType(VENUE_TYPE)
                .venueCategory1(VENUE_CAT_1)
                .venueCategory2(VENUE_CAT_2)
                .build();
    }

    private String generatePermanentLocationSecretKey() {
        final var permanentLocationSecretKey = new byte[LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE];
        new Random().nextBytes(permanentLocationSecretKey);
        return Hex.toHexString(permanentLocationSecretKey);
    }
}
