package com.uh.rainbow.services;

import com.uh.rainbow.entities.Course;
import com.uh.rainbow.entities.Meeting;
import com.uh.rainbow.entities.Section;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
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

    /**
     * Process a list of li elements with href
     *
     * @param elements List of elements to process
     * @param upe URL Param extractor to use
     * @return List of extracted ids and names
     */
    private List<Identifier> processElements(Elements elements, URLParamExtractor upe){
        List<Identifier> ids = new ArrayList<>();
        // Process each element
        for(Element item : elements){
            // Extract ID from url
            item = Objects.requireNonNull(item.selectFirst("a"));
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
            // Get each subject col
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

    public List<Course> parseCourses(String instID, String termID, String subjectID){
        Map<String, Course> courses = new HashMap<>();

        try{
            // Get each subject col
            Document doc = Jsoup.connect(UH_ROOT)
                    .data("i", instID)
                    .data("t", termID)
                    .data("s", subjectID)
                    .get();

            Elements rows = Objects.requireNonNull(doc.selectFirst("tbody")).select("tr");

            Element row = rows.remove(0);   // prime query
            while(!rows.isEmpty()){

                if(row.select("td").size() < 13){
                    row = rows.remove(0);
                    continue;
                }

                // Add new course if dne
                String cid = row.select("td").get(2).text();
                if(courses.get(cid) == null)
                    courses.put(cid, new Course(
                            cid,                                                        // Course ID
                            row.select("td").get(4).text(),                     // Full course name
                            Integer.parseInt(row.select("td").get(5).text())    // Credits
                    ));
                // Parse section
                // public Section(int id, int crn, String instructor, int currEnrolled, int seatsAvailable)
                Section section = new Section(
                        Integer.parseInt(row.select("td").get(3).text()),   // Section Number
                        Integer.parseInt(row.select("td").get(1).text()),   // Course Ref Number
                        row.select("td").get(6).select("abbr").attr("title"),   // Instructor
                        Integer.parseInt(row.select("td").get(7).text()),   // Number Enrolled
                        Integer.parseInt(row.select("td").get(8).text())    // Seats Available
                );

                // Add Requirements & Designation Codes if any
                if(!row.select("td").get(0).text().isEmpty())
                    section.addReqDesCodes(Arrays.stream(row.select("td").get(0).text().split(",")).toList());

                // Keep processing rows until hit next section
                int offset = 0;
                do{
                    section.addMeeting(new Meeting(
                            row.select("td").get(9 + offset).text(),     // Day
                            row.select("td").get(10 + offset).text(),    // Start Time
                            row.select("td").get(10 + offset).text(),    // End Time
                            row.select("td").get(11 + offset).select("abbr").attr("title")  // Room
                    ));
                    // Title is missing empty string in rows with just times
                    if(offset == 0)
                        offset = -1;

                    row = rows.remove(0);

                } while (!rows.isEmpty() && row.select("td").size() > 2 && row.select("td").get(1).text().isEmpty());

                // Update course
                courses.get(cid).addSection(section);
            }

        } catch (IOException ignored){

        }

        return courses.values().stream().toList();
    }
}
