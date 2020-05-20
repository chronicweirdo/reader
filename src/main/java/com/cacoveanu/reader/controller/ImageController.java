package com.cacoveanu.reader.controller;

import com.cacoveanu.reader.service.Comic;
import com.cacoveanu.reader.service.ComicService;
import com.cacoveanu.reader.util.CbrUtil;
import com.cacoveanu.reader.util.CbzUtil;
import com.cacoveanu.reader.util.FolderUtil;
import com.github.junrar.exception.RarException;
import com.sun.xml.internal.ws.api.pipe.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
public class ImageController {

    @Autowired
    private ComicService comicService;

    @RequestMapping("/")
    public String index() {
        return "yo!";
    }

    @RequestMapping("/image")
    public void getImage(@RequestParam("page") int page, HttpServletResponse response) throws IOException, RarException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Adventure Time\\Adventure Time (01 - 39) (ongoing) (2012-)\\Adventure Time 001 (2012) (5 covers) (digital).cbr";

        ByteArrayOutputStream data = CbrUtil.read(path, page);

        response.setContentType("image/jpeg");
        data.writeTo(response.getOutputStream());
    }

    @RequestMapping("/cbz")
    public void getCbzImage(@RequestParam("page") int page, HttpServletResponse response) throws IOException, RarException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Legend Of Korra\\The Legend of Korra - Turf Wars (001-003)(2017-2018)(digital)(Raven)\\The Legend of Korra - Turf Wars - Part 1 (2017) (Digital) (Raven).cbz";

        ByteArrayOutputStream data = CbzUtil.read(path, page);

        response.setContentType("image/jpeg");
        data.writeTo(response.getOutputStream());
    }

    @RequestMapping("/collection")
    public String getComicCollection() throws IOException, RarException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics";
        List<Comic> comics = comicService.loadComicFiles(path);

        StringBuilder page = new StringBuilder();
        page.append("<html><body>");
        for (Comic comic : comics) {
            page.append("<p>");
            page.append(comic.getTitle());
            ByteArrayOutputStream cover = comic.getCover().getData();
            String coverEncoded = new String(Base64.getEncoder().encode(cover.toByteArray()));
            page.append("<img style=\"max-width: 100px;\" src=\"data:" + comic.getCover().getMediaType() + ";base64,");
            page.append(coverEncoded);
            page.append("\">");
            page.append("</p>");
        }
        page.append("</body></html>");

        return page.toString();
    }
}
