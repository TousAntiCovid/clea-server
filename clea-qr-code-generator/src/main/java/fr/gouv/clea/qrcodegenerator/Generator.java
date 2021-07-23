package fr.gouv.clea.qrcodegenerator;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfWriter;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.SneakyThrows;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
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
    // server authority public key (Check that it is "04d....5d" for production)
    private static final String PK_SA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    // manual contact tracing authority public key (Check that it is "04c...c5" for production)
    private static final String PK_MCTA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    // number of codes to generate ( Change for the number of QRCodes to generate)
    private static final int PLACES_NUMBER = 1;

    // location configuration '(1 0 0 for Restaurants, ask others values for Discothèques, etc.)
    private static final int VENUE_TYPE = 1;
    private static final int VENUE_CAT_1 = 0;
    private static final int VENUE_CAT_2 = 0;
    private static final int UNLIMITED_PERIOD_DURATION = 255;
    private static final int NO_RENEWAL_INTERVAL = 0x1F;

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
                try {
                    generateQrCodeAndCreatePdf(i, deepLink);
                } catch (DocumentException | FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                urlsList.add(deepLink);
            }
        });
        if (!generateQrCodes) {
            generateCsvFromList(urlsList);
        }
    }

    private static void generateCsvFromList(ArrayList<URL> urlsList) throws IOException {
        try (var writer = new FileWriter(OUTPUT_DIR + "/codes.csv")) {
            final String collect = urlsList.stream()
                    .map(URL::toString)
                    .collect(Collectors.joining(",\n"));
            writer.write(collect);
        }
    }

    private static void generateQrCodeAndCreatePdf(int i, URL deepLink) throws DocumentException, FileNotFoundException {
        final var qrCode = new BarcodeQRCode(deepLink.toString(), QR_SIZE_AS_PX, QR_SIZE_AS_PX, null);
        final var filename = format("qrcode-%d.pdf", i);
        final var targetFile = OUTPUT_DIR.resolve(filename).toFile();
        createPdf(targetFile, qrCode);
    }

    private static void createPdf(File targetFile, BarcodeQRCode qrCode) throws DocumentException, FileNotFoundException {
            var document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            var image = qrCode.getImage();
            image.setAbsolutePosition((PageSize.A4.getWidth() - image.getScaledWidth())/2, (PageSize.A4.getHeight() - image.getScaledHeight())/2);
            document.open();
            document.add(image);
            document.close();
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
                .periodDuration(UNLIMITED_PERIOD_DURATION /* unlimited */)
                .qrCodeRenewalIntervalExponentCompact(NO_RENEWAL_INTERVAL /* no renewal */)
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
