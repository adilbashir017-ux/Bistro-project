package logic;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Service class responsible for generating QR codes.
 * <p>
 * This class provides utility methods for creating QR code images
 * from textual data and returning them as byte arrays.
 * </p>
 */
public class QRService {


    /**
     * Generates a QR code image from the given data and returns it as a byte array.
     * <p>
     * The QR code is generated in PNG format with a fixed size of 200x200 pixels.
     * </p>
     *
     * @param data the data to encode in the QR code
     * @return a byte array representing the generated QR code image in PNG format
     * @throws Exception if an error occurs during QR code generation or image writing
     */
    public static byte[] generateQRImageBytes(String data) throws Exception {
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);

        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}