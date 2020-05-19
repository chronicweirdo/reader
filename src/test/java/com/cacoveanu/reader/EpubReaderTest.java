package com.cacoveanu.reader;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.List;

class EpubReaderTest {

    @Test
    void testReadEpubContents() throws IOException {
        String path = "C:\\Users\\silvi\\Dropbox\\books\\The Expanse\\Corey, James S. A_\\5.0 - Nemesis Games\\Nemesis Games - James S. A. Corey.epub";
        EpubReader epubReader = new EpubReader();
        Book book = epubReader.readEpub(new FileInputStream(path));

        List<Resource> contents = book.getContents();
        contents.forEach(System.out::println);
    }

    @Test
    void readPartOfBook() throws IOException {
        String path = "C:\\Users\\silvi\\Dropbox\\books\\The Expanse\\Corey, James S. A_\\5.0 - Nemesis Games\\Nemesis Games - James S. A. Corey.epub";
        EpubReader epubReader = new EpubReader();
        Book book = epubReader.readEpub(new FileInputStream(path));

        Resource chapter = book.getContents().get(2);
        try(OutputStream outputStream = new FileOutputStream("test.html")) {
            IOUtils.copy(chapter.getInputStream(), outputStream);
        }
    }
}
