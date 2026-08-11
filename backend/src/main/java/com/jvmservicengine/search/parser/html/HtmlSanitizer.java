package com.jvmservicengine.search.parser.html;


import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(HtmlSanitizer.class);

    public void sanitize(Document document) {
        document.select("script, style, noscript, meta, link").remove();

        logger.debug("Document sanitized: scripts and styles removed");
    }
}
