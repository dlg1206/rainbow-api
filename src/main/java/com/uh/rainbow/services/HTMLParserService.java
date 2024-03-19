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
     */
    public List<Identifier> parseInstitutions() {
        List<Identifier> ids = new ArrayList<>();
        try{
            Document doc = Jsoup.connect(UH_ROOT).get();
            Elements institutions = doc.select("ul.institutions").select("li");

            for(Element item : institutions)
                ids.add(new Identifier(item.className(), item.text()));
        } catch (IOException ignored){

        }

        return ids;
    }

    /**
     * Parse the list of available terms for an institution
     *
     * @param instID Institution ID
     * @return List of term ids and names
     */
    public List<Identifier> parseTerms(String instID) {
        List<Identifier> ids = new ArrayList<>();
        try{
            Pattern pattern = Pattern.compile("t=([0-9]*)");

            // Get terms
            Document doc = Jsoup.connect(UH_ROOT).data("i", instID).get();
            Elements terms = doc.select("ul.terms").select("li");

            for(Element item : terms){
                // Extract term ID from url
                item = item.selectFirst("a");
                String termID = "";
                Matcher matcher = pattern.matcher( item.attr("href"));

                if (matcher.find())
                    termID = matcher.group(1);

                ids.add(new Identifier(termID, item.text()));
            }
        } catch (IOException ignored){

        }

        return ids;
    }

    public List<Identifier> parseSubjects(String instID, String termID) {
        List<Identifier> ids = new ArrayList<>();
        try{
            Document doc = Jsoup.connect(UH_ROOT)
                    .data("i", instID)
                    .data("t", termID)
                    .get();
            Elements leftSubjects = doc
                    .select("div.leftcolumn")
                    .select("ul.subjects")
                    .select("li");

            Elements rightSubjects = doc
                    .select("div.rightcolumn")
                    .select("ul.subjects")
                    .select("li");

            ids.addAll(parseSubjects(leftSubjects));
            ids.addAll(parseSubjects(rightSubjects));

        } catch (IOException ignored){

        }

        return ids;
    }

    private List<Identifier> parseSubjects(Elements subjects){
        List<Identifier> ids = new ArrayList<>();
        Pattern pattern = Pattern.compile("s=(\\w*)");

        for(Element item : subjects){
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
