package com.uh.rainbow.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b>File:</b> HTMLParser.java
 * <p>
 * <b>Description:</b> Queries UH urls and processes the resulting HTML
 *
 * @author Derek Garcia
 */
@Service
public class HTMLParserService {

    /**
     * Util identifier
     *
     * @param id ID of resource
     * @param name Full or simple name of resorce
     */
    public record Identifier(String id, String name) {
    }

    private static final String UH_ROOT = "https://www.sis.hawaii.edu/uhdad/avail.classes";

    /**
     * Parse the list of UH institutions
     *
     * @return List of institution ids and names
     * @throws IOException Failed to access url
     */
    public List<Identifier> parseInstitutions() throws IOException {
        List<Identifier> ids = new ArrayList<>();

        Document doc = Jsoup.connect(UH_ROOT).get();
        Elements institutions = doc.select("ul.institutions").select("li");

        for(Element item : institutions)
            ids.add(new Identifier(item.className(), item.text()));

        return ids;
    }

    /**
     * Parse the list of available terms for an institution
     *
     * @param instID Institution ID
     * @return List of term ids and names
     * @throws IOException Failed to access url
     */
    public List<Identifier> parseTerms(String instID) throws IOException {
        List<Identifier> ids = new ArrayList<>();
        Pattern pattern = Pattern.compile("t=([0-9]*)");

        // Get terms
        Document doc = Jsoup.connect(UH_ROOT).data("i", instID).get();
        Elements institutions = doc.select("ul.terms").select("li");

        for(Element item : institutions){
            // Extract term ID from url
            item = item.selectFirst("a");
            String termID = "";
            Matcher matcher = pattern.matcher( item.attr("href"));

            if (matcher.find())
                termID = matcher.group(1);

            ids.add(new Identifier(termID, item.text()));
        }


        return ids;
    }

}
