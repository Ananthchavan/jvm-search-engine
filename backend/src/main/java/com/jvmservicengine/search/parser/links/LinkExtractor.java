package com.jvmservicengine.search.parser.links;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class LinkExtractor {

    private static final Logger logger = LoggerFactory.getLogger(LinkExtractor.class);

    public Set<String> extractOutgoingLinks(Document document) {
        Set<String> validLinks = new HashSet<>();

        Elements links = document.select("a[href]");

        for(Element link : links) {

            // forces JSoup to resolve relative links (like "/about")
            String absoluteUrl = link.attr("abs:href").trim();

            if(!absoluteUrl.isEmpty() && (absoluteUrl.startsWith("http://") || absoluteUrl.startsWith("https://"))) {
                // to avoid URL fragments, we strip everthing after #
                int hashIndex = absoluteUrl.indexOf('#');
                if(hashIndex != -1) {
                    absoluteUrl = absoluteUrl.substring(0, hashIndex);
                }

                validLinks.add(absoluteUrl);
            }
        }

        logger.debug("Extracted {} valid outgoing Links", validLinks.size());
        return validLinks;
    }
}
