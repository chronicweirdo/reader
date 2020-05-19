package com.cacoveanu.reader;

import com.cacoveanu.reader.util.FolderUtil;
import org.junit.jupiter.api.Test;

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
}
