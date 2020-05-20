package com.cacoveanu.reader.service;

import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;

public class ComicPage {

    private MediaType mediaType;
    private ByteArrayOutputStream data;

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public ByteArrayOutputStream getData() {
        return data;
    }

    public void setData(ByteArrayOutputStream data) {
        this.data = data;
    }
}
