package com.cacoveanu.reader.service;

import com.cacoveanu.reader.util.FolderUtil;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class ComicService {

    private static String COMIC_TYPE_CBR = "cbr";
    private static String COMIC_TYPE_CBZ = "cbz";
    private static String COMIC_FILE_REGEX = ".+\\.(" + COMIC_TYPE_CBR + "|" + COMIC_TYPE_CBZ + ")$";

    private String getComicType(String path) {
        if (path.endsWith(COMIC_TYPE_CBR)) return COMIC_TYPE_CBR;
        else if (path.endsWith(COMIC_TYPE_CBZ)) return COMIC_TYPE_CBZ;
        else return null;
    }

    public ComicPage readPage(String path, int pageNumber) {
        String comicType = getComicType(path);
        if (comicType == COMIC_TYPE_CBR) {
            return readCbrPage(path, pageNumber);
        } else if (comicType == COMIC_TYPE_CBZ) {
            return readCbzPage(path, pageNumber);
        }
        return null;
    }

    public List<Comic> loadComicFiles(String path) {
        List<String> files = FolderUtil.scanFilesRegex(path, COMIC_FILE_REGEX);
        return files.stream().map(file -> loadComic(file)).collect(Collectors.toList());
    }

    private Comic loadComic(String file) {
        Comic comic = new Comic();
        comic.setPath(file);
        comic.setTitle(getComicTitle(file));
        comic.setCover(readPage(file, 0));
        return comic;
    }

    private String getComicTitle(String path) {
        Path pathObject = Paths.get(path);
        String fileName = pathObject.getFileName().toString();
        String title = fileName.substring(0, fileName.lastIndexOf('.'));
        return title;
    }

    private MediaType getMediaTypeForFileName(String fileName) {
        String lowercaseFileName = fileName.toLowerCase();
        if (lowercaseFileName.endsWith(".jpg")) return MediaType.IMAGE_JPEG;
        else if (lowercaseFileName.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        else if (lowercaseFileName.endsWith(".png")) return MediaType.IMAGE_PNG;
        else if (lowercaseFileName.endsWith(".gif")) return MediaType.IMAGE_GIF;
        else return null;
    }

    private ComicPage readCbrPage(String path, int pageNumber) {
        try {
            Archive archive = new Archive(new FileInputStream(path));
            List<FileHeader> fileHeaders = archive.getFileHeaders().stream().filter(f -> !f.isDirectory()).collect(Collectors.toList());

            if (pageNumber >= 0 && pageNumber < fileHeaders.size()) {
                Collections.sort(fileHeaders, Comparator.comparing(FileHeader::getFileNameString));
                FileHeader archiveFile = fileHeaders.get(pageNumber);
                MediaType fileMediaType = getMediaTypeForFileName(archiveFile.getFileNameString());
                ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
                archive.extractFile(archiveFile, fileContents);
                archive.close();

                ComicPage page = new ComicPage();
                page.setData(fileContents);
                page.setMediaType(fileMediaType);
                return page;
            } else {
                archive.close();
                return null;
            }
        } catch (IOException | RarException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ComicPage readCbzPage(String path, int pageNumber) {
        try {
            ZipFile zipFile = new ZipFile(path);
            List<? extends ZipEntry> files = Collections.list(zipFile.entries()).stream().filter(f -> !((ZipEntry) f).isDirectory()).collect(Collectors.toList());

            if (pageNumber >= 0 && pageNumber < files.size()) {
                Collections.sort(files, Comparator.comparing(ZipEntry::getName));
                ZipEntry file = files.get(pageNumber);
                MediaType fileMediaType = getMediaTypeForFileName(file.getName());
                InputStream fileContents = zipFile.getInputStream(file);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copy(fileContents, bos);
                zipFile.close();

                ComicPage page = new ComicPage();
                page.setMediaType(fileMediaType);
                page.setData(bos);
                return page;
            } else {
                zipFile.close();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
