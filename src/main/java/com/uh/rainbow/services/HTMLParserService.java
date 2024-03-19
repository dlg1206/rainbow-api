package com.uh.rainbow.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <b>File:</b> HTMLParser.java
 * <p>
 * <b>Description:</b>
 *
 * @author Derek Garcia
 */
@Service
public class HTMLParserService {

    public record Identifier(String id, String name) {
    }

    private static final String UH_CAMPUSES = "https://www.sis.hawaii.edu/uhdad/avail.classes";

    public List<Identifier> parseCampuses() throws IOException {
        List<Identifier> ids = new ArrayList<>();

        Document doc = Jsoup.connect(UH_CAMPUSES).get();
        Elements institutions = doc.select("ul.institutions").select("li");

        for(Element item : institutions)
            ids.add(new Identifier(item.className(), item.text()));

        return ids;
    }

}
