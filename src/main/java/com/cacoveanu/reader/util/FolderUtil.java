package com.cacoveanu.reader.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FolderUtil {

    private static List<File> scan(String path) {
        List<File> files = new ArrayList<>();
        files.add(new File(path));
        int processed = 0;
        while (processed < files.size()) {
            File current = files.get(processed);
            if (current.exists() && current.isDirectory()) {
                files.addAll(Arrays.asList(current.listFiles()));
            }
            processed++;
        }

        return files;
    }

    public static List<String> scanAllFiles(String path) {
        return scan(path).stream()
                .filter(f -> f.isFile())
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
    }

    public static List<String> scanSpecificFiles(String path, String extension) {
        return scan(path).stream()
                .filter(f -> f.isFile())
                .filter(f -> f.getName().endsWith(extension))
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
    }

    public static List<String> scanFilesRegex(String path, String regex) {
        Pattern pattern = Pattern.compile(regex);
        return scan(path).stream()
                .filter(f -> f.isFile())
                .filter(f -> pattern.matcher(f.getAbsolutePath()).matches())
                .map(f -> f.getAbsolutePath())
                .collect(Collectors.toList());
    }
}
