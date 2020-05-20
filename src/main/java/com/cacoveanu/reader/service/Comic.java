package com.cacoveanu.reader.service;

import java.io.ByteArrayOutputStream;

public class Comic {

    private String path;
    private String title;
    private ComicPage cover;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ComicPage getCover() {
        return cover;
    }

    public void setCover(ComicPage cover) {
        this.cover = cover;
    }
}
