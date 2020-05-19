package com.cacoveanu.reader;

import com.github.mertakdut.BookSection;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;
import com.github.mertakdut.exception.ReadingException;
import org.junit.jupiter.api.Test;

public class OtherEpubReaderTest {

    @Test
    void read() throws ReadingException, OutOfPagesException {
        String path = "C:\\Users\\silvi\\Dropbox\\books\\The Expanse\\Corey, James S. A_\\5.0 - Nemesis Games\\Nemesis Games - James S. A. Corey.epub";
        Reader reader = new Reader();
        reader.setMaxContentPerSection(1100); // Max string length for the current page.
        reader.setIsIncludingTextContent(true); // Optional, to return the tags-excluded version.
        reader.setFullContent(path); // Must call before readSection.


        int pageIndex = 10;
        BookSection bookSection = reader.readSection(pageIndex);
        String sectionContent = bookSection.getSectionContent(); // Returns content as html.
        System.out.println(sectionContent);
        String sectionTextContent = bookSection.getSectionTextContent(); // Excludes html tags.
        System.out.println(sectionTextContent);
    }

    @Test
    void readSmaller() throws ReadingException, OutOfPagesException {
        String path = "C:\\Users\\silvi\\Dropbox\\books\\The Expanse\\Corey, James S. A_\\5.0 - Nemesis Games\\Nemesis Games - James S. A. Corey.epub";
        Reader reader = new Reader();
        reader.setMaxContentPerSection(100); // Max string length for the current page.
        reader.setIsIncludingTextContent(true); // Optional, to return the tags-excluded version.
        reader.setFullContent(path); // Must call before readSection.


        int pageIndex = 100;
        BookSection bookSection = reader.readSection(pageIndex);
        String sectionContent = bookSection.getSectionContent(); // Returns content as html.
        System.out.println(sectionContent);
        String sectionTextContent = bookSection.getSectionTextContent(); // Excludes html tags.
        System.out.println(sectionTextContent);
    }
}
