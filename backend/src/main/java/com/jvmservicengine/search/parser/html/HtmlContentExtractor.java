package com.jvmservicengine.search.parser.html;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HtmlContentExtractor {

    private static final Logger logger = LoggerFactory.getLogger(HtmlContentExtractor.class);

    public String extractTitle(Document document) {
        String title = document.title();

        if(title == null || title.isBlank()) {
            Element h1 = document.selectFirst("h1");
            title = (h1 != null) ? h1.text() : "Untitled Page";
        }

        return title.trim();
    }

    public String extractBodyText(Document document) {
        if(document.body() == null) {
            logger.warn("Document has no <body> tag");
            return "";
        }

        return document.body().text().trim();
    }
}
