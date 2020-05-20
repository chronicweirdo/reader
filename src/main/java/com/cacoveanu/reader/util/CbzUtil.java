package com.cacoveanu.reader.util;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class CbzUtil {

    private static List<? extends ZipEntry> getImageContentsInOrder(ArrayList<? extends ZipEntry> entries, String extension) {
        return entries.stream()
                .filter(e -> !((ZipEntry) e).isDirectory())
                .filter(e -> ((ZipEntry) e).getName().endsWith(extension))
                .sorted((e1, e2) -> e1.getName().compareTo(e2.getName()))
                .collect(Collectors.toList());
    }

    public static ByteArrayOutputStream read(String path, int page) throws IOException {
        ZipFile zipFile = new ZipFile(path);

        List<? extends ZipEntry> images = getImageContentsInOrder(Collections.list(zipFile.entries()), ".jpg");

        if (page >= 0 && page < images.size()) {
            InputStream inputStream = zipFile.getInputStream(images.get(page));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, bos);
            zipFile.close();
            return bos;
        } else {
            zipFile.close();
            return null;
        }
    }
}
