package com.cacoveanu;

import org.assertj.core.internal.Arrays;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class ChecksumTest {
    public static String computeChecksum(MessageDigest digest, String file) throws IOException {
        FileInputStream fis = new FileInputStream(file);

        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        while ((bytesCount = fis.read(byteArray)) != -1)
        {
            digest.update(byteArray, 0, bytesCount);
        };

        fis.close();

        byte[] bytes = digest.digest();

        // this array of bytes has bytes in decimal format
        // so we need to convert it into hexadecimal format

        // for this we create an object of StringBuilder
        // since it allows us to update the string i.e. its
        // mutable
        StringBuilder sb = new StringBuilder();

        // loop through the bytes array
        for (int i = 0; i < bytes.length; i++) {

            // the following line converts the decimal into
            // hexadecimal format and appends that to the
            // StringBuilder object
            sb.append(Integer
                    .toString((bytes[i] & 0xff) + 0x100, 16)
                    .substring(1));
        }

        return sb.toString();
    }

    /*
    private def getFileCreationDate(path: String): Date = {
    try {
      val pathObject = Paths.get(path)
      val attributes: BasicFileAttributes = Files.readAttributes(pathObject, classOf[BasicFileAttributes])
      val creationTime: FileTime = attributes.creationTime()
      new Date(creationTime.toMillis)
    } catch {
      case _: Throwable => new Date()
    }
  }
     */
    public static Date getFileCreationDate(String file) throws IOException {
        Path pathObject = Paths.get(file);
        BasicFileAttributes attributes = Files.readAttributes(pathObject, BasicFileAttributes.class);
        FileTime creationTime = attributes.creationTime();
        return new Date(creationTime.toMillis());
    }

    public static Date[] getFileDates(String file) throws IOException {
        Path pathObject = Paths.get(file);
        BasicFileAttributes attributes = Files.readAttributes(pathObject, BasicFileAttributes.class);
        FileTime creationTime = attributes.creationTime();
        FileTime modifiedTime = attributes.lastModifiedTime();
        return new Date[]{new Date(creationTime.toMillis()), new Date(modifiedTime.toMillis())};
    }

    public static String getFileKey(String file) throws IOException {
        Path pathObject = Paths.get(file);
        BasicFileAttributes attributes = Files.readAttributes(pathObject, BasicFileAttributes.class);
        Object fileKey = attributes.fileKey();
        return fileKey.toString();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        String file = args[0];

        long md5Start = System.currentTimeMillis();
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        String md5Checksum = computeChecksum(md5Digest, file);
        long md5End = System.currentTimeMillis();
        System.out.println("computed md5 checksum " + md5Checksum + " in " + (md5End - md5Start) + " milliseconds");

        long sha1Start = System.currentTimeMillis();
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
        String sha1Checksum = computeChecksum(sha1Digest, file);
        long sha1End = System.currentTimeMillis();
        System.out.println("computed sha1 checksum " + sha1Checksum + " in " + (sha1End - sha1Start) + " milliseconds");

        long sha256Start = System.currentTimeMillis();
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
        String sha256Checksum = computeChecksum(sha256Digest, file);
        long sha256End = System.currentTimeMillis();
        System.out.println("computed sha256 checksum " + sha256Checksum + " in " + (sha256End - sha256Start) + " milliseconds");

        /*long creationDateStart = System.currentTimeMillis();
        Date creationDate = getFileCreationDate(file);
        Long creationDateEnd = System.currentTimeMillis();
        System.out.println("found creation date " + creationDate + " in " + (creationDateEnd - creationDateStart) + " milliseconds");*/

        long datesStart = System.currentTimeMillis();
        Date[] dates = getFileDates(file);
        Long datesEnd = System.currentTimeMillis();
        System.out.println("found file creation date " + dates[0] + " and modified date " + dates[1] + " in " + (datesEnd - datesStart) + " milliseconds");

        try {
            long fileKeyStart = System.currentTimeMillis();
            String fileKey = getFileKey(file);
            Long fileKeyEnd = System.currentTimeMillis();
            System.out.println("found file key " + fileKey + " in " + (fileKeyEnd - fileKeyStart) + " milliseconds");
        } catch (NullPointerException e) {
            System.out.printf("no file key");
        }
    }
}
