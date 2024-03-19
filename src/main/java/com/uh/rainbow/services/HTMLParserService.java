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

    /**
     * Regex parser that extracts params from a url
     */
    private static class URLParamExtractor{

        private final Pattern regex;

        /**
         * Create new extractor
         *
         * @param regex Regex to use to extract from url
         */
        public URLParamExtractor(String regex){
            this.regex = Pattern.compile(regex);
        }

        /**
         * Use the regex to extract the param
         *
         * @param url URL to extract param from
         * @return Value of param
         */
        public String extract(String url){
            Matcher matcher = this.regex.matcher(url);
            if (matcher.find())
                return matcher.group(1);
            return "";
        }

    }

    private static final String UH_ROOT = "https://www.sis.hawaii.edu/uhdad/avail.classes";

    private List<Identifier> processElements(Elements elements, URLParamExtractor upe){
        List<Identifier> ids = new ArrayList<>();
        // Process each element
        for(Element item : elements){
            // Extract ID from url
            item = item.selectFirst("a");
            String termID = upe.extract(item.attr("href"));

            ids.add(new Identifier(termID, item.text()));
        }

        return ids;
    }

    /**
     * Parse the list of UH institutions
     *
     * @return List of institution ids and names
     */
    public List<Identifier> parseInstitutions() {
        List<Identifier> ids = new ArrayList<>();
        try{
            // Get list
            Document doc = Jsoup.connect(UH_ROOT).get();
            Elements institutions = doc.select("ul.institutions").select("li");

            // Convert to key pair
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
            // Get terms
            Document doc = Jsoup.connect(UH_ROOT).data("i", instID).get();
            Elements terms = doc.select("ul.terms").select("li");

            ids.addAll(processElements(terms, new URLParamExtractor("t=([0-9]*)")));

        } catch (IOException ignored){

        }

        return ids;
    }

    /**
     * Parse the list of available subjects for an institution and term
     *
     * @param instID Institution ID
     * @param termID term ID
     * @return List of subject ids and names
     */
    public List<Identifier> parseSubjects(String instID, String termID) {
        List<Identifier> ids = new ArrayList<>();
        try{
            // Get each col
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

            // Process each list
            URLParamExtractor upe = new URLParamExtractor("s=(\\w*)");
            ids.addAll(processElements(leftSubjects, upe));
            ids.addAll(processElements(rightSubjects, upe));

        } catch (IOException ignored){

        }

        return ids;
    }
}
