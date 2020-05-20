package com.cacoveanu.reader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CbzReaderTest {

    @Test
    void readContents() throws IOException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)\\The Legend of Korra - Turf Wars - Part 1 (2017) (Digital) (Raven).cbz";
        ZipFile zipFile = new ZipFile(path);

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while(entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            System.out.println(entry.getName());
        }

        zipFile.close();
    }

    @Test
    void readRelevantContentsInOrder() throws IOException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)\\The Legend of Korra - Turf Wars - Part 1 (2017) (Digital) (Raven).cbz";
        String extension = ".jpg";

        ZipFile zipFile = new ZipFile(path);

        ArrayList<? extends ZipEntry> entries = Collections.list(zipFile.entries());
        entries.stream()
                .filter(e -> ! ((ZipEntry) e).isDirectory())
                .filter(e -> ((ZipEntry) e).getName().endsWith(extension))
                .sorted((e1, e2) -> e1.getName().compareTo(e2.getName()))
                .map(ZipEntry::getName)
                .forEach(System.out::println);

        zipFile.close();
    }
}
