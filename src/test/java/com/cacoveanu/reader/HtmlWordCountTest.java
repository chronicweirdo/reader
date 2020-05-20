package com.cacoveanu.reader;

/*import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;*/
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class HtmlWordCountTest {

    /*@Test
    void openDocument() throws IOException {
        Document doc = Jsoup.connect("https://en.wikipedia.org/").get();
        System.out.println(doc.title());
        Elements newsHeadlines = doc.select("#mp-itn b a");
        for (Element headline : newsHeadlines) {
            System.out.printf("%s\n\t%s",
                    headline.attr("title"), headline.absUrl("href"));
        }
    }

    @Test
    void extractText() throws IOException {
        Document doc = Jsoup.connect("https://en.wikipedia.org/").get();
        System.out.println(doc.text());
    }

    String handleNodes(List<Node> nodes, String prefix) {
        StringBuilder builder = new StringBuilder();
        for (Node node : nodes) {
            System.out.println(prefix + node.nodeName() + " " + node.getClass());
            if (node.nodeName().equals("#text")) {
                builder.append(((TextNode) node).text());
            }
            builder.append(handleNodes(node.childNodes(), prefix + ">"));
        }
        return builder.toString();
    }

    @Test
    void goThroughText() throws IOException {
        Document doc = Jsoup.connect("https://en.wikipedia.org/").get();
        String content = handleNodes(doc.childNodes(), "");
        System.out.println(content);
        String otherContent = doc.text();
        System.out.println(otherContent);
        System.out.println(content.equals(otherContent));
        String trimmedContent = content.replaceAll("\\s+", " ");
        System.out.println(trimmedContent);
        System.out.println(trimmedContent.equals(otherContent));
    }*/
}
