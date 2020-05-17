package com.cacoveanu.reader.controller;

import com.cacoveanu.reader.util.CbrUtil;
import com.github.junrar.exception.RarException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
}
