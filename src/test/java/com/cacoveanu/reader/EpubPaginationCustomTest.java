package com.cacoveanu.reader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EpubPaginationCustomTest {

    private String getTagName(String tag) {
        int start = 1;
        if (tag.startsWith("</")) start = 2;
        int end = tag.length() - 1;
        int space = tag.indexOf(" ");
        if (space != -1) end = space;
        return tag.substring(start, end);
    }

    private boolean isEndChar(Character c) {
        return c == ' ' || c == '\t' || c == '.' || c == ',';
    }

    @Test
    void tryToPaginateInMemory() throws Exception {
        String pathToSection = "C:\\Users\\silvi\\Desktop\\text00001.html";
        String data = new String(Files.readAllBytes(Paths.get(pathToSection)));
        //System.out.println(data);
        String startTag = "<body>";
        String endTag = "</body>";
        int contentStart = data.indexOf(startTag) + startTag.length();
        int contentEnd = data.indexOf(endTag);
        String content = data.substring(contentStart, contentEnd);
        //System.out.println(content);

        StringBuilder tag = null;
        StringBuilder page = new StringBuilder();
        List<String> pages = new ArrayList<>();
        List<String> tagStack = new ArrayList<>();
        int pageChars = 0;
        int pageLimit = 1000;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '<') {
                tag = new StringBuilder();
                tag.append(c);
            } else if (c == '>') {
                tag.append(c);
                String fullTag = tag.toString();
                tag = null;
                if (! getTagName(fullTag).equals("img")) {

                    if (fullTag.startsWith("</")) {
                        String previousTag = tagStack.remove(tagStack.size() - 1);
                        if (!getTagName(previousTag).equals(getTagName(fullTag))) {
                            throw new Exception("mismatched ending tag");
                        }
                    } else {
                        tagStack.add(fullTag);
                    }
                }

            } else if (tag != null) {
                tag.append(c);
            } else {
                pageChars++;
            }
            page.append(c);
            if (pageChars > pageLimit) {
                if (isEndChar(c)) {
                    for (int j = tagStack.size() - 1; j >= 0; j--) {
                        String tagName = getTagName(tagStack.get(j));
                        page.append("</").append(tagName).append(">");
                    }
                    pages.add(page.toString());
                    page = new StringBuilder();
                    for (int j = 0; j < tagStack.size(); j++) {
                        page.append(tagStack.get(j));
                    }
                    pageChars = 0;
                }
            }
        }

        String outputPath = "C:\\Users\\silvi\\Desktop\\text00001\\";
        for (int i = 0; i < pages.size(); i++) {
            System.out.println(pages.get(i));
            System.out.println();
            PrintWriter out = new PrintWriter(outputPath + i + ".html");
            out.print("<html><body>");
            out.print(pages.get(i));
            out.print("</body></html>");
            out.close();
        }
        System.out.println(pages.size());
    }
}
