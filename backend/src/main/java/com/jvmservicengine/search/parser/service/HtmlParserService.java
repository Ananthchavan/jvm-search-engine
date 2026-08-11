package com.jvmservicengine.search.parser.service;

import com.jvmservicengine.search.api.dto.ParsedPageData;
import com.jvmservicengine.search.parser.html.HtmlContentExtractor;
import com.jvmservicengine.search.parser.html.HtmlSanitizer;
import com.jvmservicengine.search.parser.links.LinkExtractor;
import com.jvmservicengine.search.parser.metadata.MetadataExtractor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class HtmlParserService {

    private static final Logger logger = LoggerFactory.getLogger(HtmlParserService.class);

    private final HtmlSanitizer htmlSanitizer;
    private final HtmlContentExtractor htmlContentExtractor;
    private final MetadataExtractor metadataExtractor;
    private final LinkExtractor linkExtractor;

    // Constructor Injection for all 4 domain components
    public HtmlParserService(HtmlSanitizer htmlSanitizer,
                             HtmlContentExtractor htmlContentExtractor,
                             MetadataExtractor metadataExtractor,
                             LinkExtractor linkExtractor) {
        this.htmlSanitizer = htmlSanitizer;
        this.htmlContentExtractor = htmlContentExtractor;
        this.metadataExtractor = metadataExtractor;
        this.linkExtractor = linkExtractor;
    }

    public ParsedPageData parse(String rawHtml, String url) {
        if (rawHtml == null || rawHtml.isBlank()) {
            logger.warn("Received empty HTML for URL: {}", url);
            return null;
        }

        // parse into DOM tree
        Document document = Jsoup.parse(rawHtml, url);

        // extract context before we delete all the tags
        String metaDescription = metadataExtractor.extractMetaDescription(document);
        Set<String> outgoingLinks = linkExtractor.extractOutgoingLinks(document);

        // sanitize
        htmlSanitizer.sanitize(document);

        // core text extraction (after sanitization)
        String title = htmlContentExtractor.extractTitle(document);
        String bodyText = htmlContentExtractor.extractBodyText(document);

        logger.info("Successfully completed full extraction pipeline for: {}", url);

        // populate dto and return
        return new ParsedPageData(title, bodyText, metaDescription, outgoingLinks);
    }
}