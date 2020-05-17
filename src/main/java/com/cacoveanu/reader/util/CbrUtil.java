package com.cacoveanu.reader.util;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CbrUtil {

    public static ByteArrayOutputStream read(String path, int page) throws IOException, RarException {
        Archive archive = new Archive(new FileInputStream(path));
        List<FileHeader> fileHeaders = archive.getFileHeaders();
        Collections.sort(fileHeaders, Comparator.comparing(FileHeader::getFileNameString));
        FileHeader first = fileHeaders.get(page);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        archive.extractFile(first, bos);
        return bos;
    }
}
