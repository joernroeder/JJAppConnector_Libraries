package jj.appconnector;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.commons.codec.binary.Base64;

import processing.core.PImage;

public class ImageHelper {
  public static PImage createImageFromBytes(byte[] b) {
    return new PImage(new ImageIcon(b).getImage());
  }

  public static byte[] extractBytes (PImage img) throws IOException {
    BufferedImage bufferedImage = (BufferedImage) img.getImage();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write( bufferedImage, "jpg", baos );
    baos.flush();
    byte[] imageInByte = baos.toByteArray();
    baos.close();

    return imageInByte;
  }
  
  public static String encodeBase64 (byte[] b) {
    return Base64.encodeBase64URLSafeString(b);
  }
  
  public static byte[] decodeBase64 (String s) {
    return Base64.decodeBase64(s);
  }
}

