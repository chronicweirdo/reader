package com.cacoveanu.reader.controller;

import com.cacoveanu.reader.util.CbrUtil;
import com.cacoveanu.reader.util.FolderUtil;
import com.github.junrar.exception.RarException;
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

    @RequestMapping("/collection")
    public String getComicCollection() throws IOException, RarException {
        String path = "C:\\Users\\silvi\\Dropbox\\comics\\Avatar The Last Airbender\\Avatar - The Last Airbender - Imbalance (2018-2019)";
        List<String> comics = FolderUtil.scanSpecificFiles(path, "cbr");

        StringBuilder page = new StringBuilder();
        page.append("<html><body>");
        for (String comic : comics) {
            page.append("<p>");
            page.append(comic);
            ByteArrayOutputStream cover = CbrUtil.read(comic, 0);
            String coverEncoded = new String(Base64.getEncoder().encode(cover.toByteArray()));
            page.append("<img style=\"max-width: 100px;\" src=\"data:image/jpeg;base64,");
            page.append(coverEncoded);
            page.append("\">");
            page.append("</p>");
        }
        page.append("</body></html>");

        return page.toString();
    }
}
