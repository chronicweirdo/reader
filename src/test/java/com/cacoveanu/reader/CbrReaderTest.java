package com.cacoveanu.reader;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class CbrReaderTest {

    @Test
    void openCbrArchive() throws IOException, RarException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Adventure Time\\Adventure Time (01 - 39) (ongoing) (2012-)\\Adventure Time 001 (2012) (5 covers) (digital).cbr";
        Archive archive = new Archive(new FileInputStream(path));
        List<FileHeader> fileHeaders = archive.getFileHeaders();
        Collections.sort(fileHeaders, Comparator.comparing(FileHeader::getFileNameString));
        for (FileHeader fileHeader : fileHeaders) {
            System.out.println(fileHeader.getFileNameString());
        }
    }

    @Test
    void extractImageFromCbrArchive() throws IOException, RarException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Adventure Time\\Adventure Time (01 - 39) (ongoing) (2012-)\\Adventure Time 001 (2012) (5 covers) (digital).cbr";
        Archive archive = new Archive(new FileInputStream(path));
        List<FileHeader> fileHeaders = archive.getFileHeaders();
        Collections.sort(fileHeaders, Comparator.comparing(FileHeader::getFileNameString));
        FileHeader first = fileHeaders.get(0);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        archive.extractFile(first, bos);

        try(OutputStream outputStream = new FileOutputStream("test.jpg")) {
            bos.writeTo(outputStream);
        }
    }
}
