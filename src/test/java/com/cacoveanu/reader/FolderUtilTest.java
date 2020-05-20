package com.cacoveanu.reader;

import com.cacoveanu.reader.util.FolderUtil;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

public class FolderUtilTest {

    @Test
    void folderScan() {
        String path = "C:\\Users\\silvi\\Dropbox\\comics";
        FolderUtil.scanAllFiles(path).forEach(System.out::println);
    }

    @Test
    void folderScanForCbr() {
        String path = "C:\\Users\\silvi\\Dropbox\\comics";
        FolderUtil.scanSpecificFiles(path, "cbr").forEach(System.out::println);
    }

    @Test
    void findCorrectExtensionsRegex() {
        String fileOne = "fileOne.cbr";
        String fileTwo = "fileTwo.cbz";
        String fileThree = "fileThree.pdf";

        Pattern pattern = Pattern.compile(".+\\.(cbr|cbz)$");
        System.out.println(pattern.matcher(fileOne).matches());
        System.out.println(pattern.matcher(fileTwo).matches());
        System.out.println(pattern.matcher(fileThree).matches());
    }

    @Test
    void scanForComics() {
        String path = "C:\\Users\\silvi\\Dropbox\\comics";
        FolderUtil.scanFilesRegex(path, ".+\\.(cbr|cbz)$").forEach(System.out::println);
    }
}
