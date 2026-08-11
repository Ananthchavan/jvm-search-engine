package com.jvmservicengine.search.parser.metadata;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class MetadataExtractor {

    public String extractMetaDescription(Document document) {
        Element metaDescription = document.selectFirst("meta[name=description]");

        if(metaDescription != null && metaDescription.hasAttr("content")){
            return metaDescription.attr("content").trim();
        }

        return ""; // return Empty if no description
    }
}
