package com.uh.rainbow.controller;

import com.uh.rainbow.dto.course.CourseDTO;
import com.uh.rainbow.dto.identifier.IdentifierDTO;
import com.uh.rainbow.dto.response.*;
import com.uh.rainbow.service.HTMLParserService;
import com.uh.rainbow.util.SourceURL;
import com.uh.rainbow.util.filter.CourseFilter;
import com.uh.rainbow.util.logging.Logger;
import com.uh.rainbow.util.logging.MessageBuilder;
import org.jsoup.HttpStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * <b>File:</b> CampusController.java
 * <p>
 * <b>Description:</b> Controller that handles request for course and subject information at UH campuses
 *
 * @author Derek Garcia
 */

@RequestMapping("/v1/campuses")
@RestController(value = "CampusController")
public class CampusController {

    private final static Logger LOGGER = new Logger(CampusController.class);
    private final HTMLParserService htmlParserService = new HTMLParserService();

    /**
     * Util logging method for reporting HTTP failures
     *
     * @param type Log type
     * @param e    HttpStatusException
     */
    private void reportHTTPAccessError(MessageBuilder.Type type, HttpStatusException e) {
        MessageBuilder mb = new MessageBuilder(type)
                .addDetails("Failed to fetch HTML")
                .addDetails(e.getStatusCode());
        LOGGER.warn(mb);
        LOGGER.debug(mb.addDetails(e));
    }

    /**
     * GET Endpoint: /campuses
     * Get list of University of Hawaii Campuses
     *
     * @return List of University of Hawaii Campuses and their ID's
     */
    @GetMapping(value = "")
    public ResponseEntity<ResponseDTO> getAllCampuses() {
        try {
            // Get all campuses
            return new ResponseEntity<>(
                    new IdentifierResponseDTO(new SourceURL(), this.htmlParserService.parseInstitutions()),
                    HttpStatus.OK
            );
        } catch (HttpStatusException e) {
            // Report and return html access failure
            reportHTTPAccessError(MessageBuilder.Type.INST, e);
            return new ResponseEntity<>(new BadAccessResponseDTO(e), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // Internal server error
            LOGGER.error(new MessageBuilder(MessageBuilder.Type.INST).addDetails(e));
            return new ResponseEntity<>(new APIErrorResponseDTO(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * GET Endpoint: /campuses/{instID}/terms
     * Get list of terms for a campus
     *
     * @param instID Inst ID to search for terms
     * @return List of term names and their ID's
     */
    @GetMapping(value = "/{instID}/terms")
    public ResponseEntity<ResponseDTO> getAllTerms(@PathVariable String instID) {
        try {
            // Get all terms
            return new ResponseEntity<>(
                    new IdentifierResponseDTO(new SourceURL(instID), this.htmlParserService.parseTerms(instID)),
                    HttpStatus.OK
            );
        } catch (HttpStatusException e) {
            // Report and return html access failure
            reportHTTPAccessError(MessageBuilder.Type.TERM, e);
            return new ResponseEntity<>(new BadAccessResponseDTO(e), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // Internal server error
            LOGGER.error(new MessageBuilder(MessageBuilder.Type.TERM).addDetails(e));
            return new ResponseEntity<>(new APIErrorResponseDTO(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * GET Endpoint: /campuses/{instID}/terms/{termID}/subjects
     * Get list of subjects for a given campus and term
     *
     * @param instID Inst ID to search for subjects
     * @param termID Term ID to search for subjects
     * @return List of subjects for a given campus and term
     */
    @GetMapping(value = "/{instID}/terms/{termID}/subjects")
    public ResponseEntity<ResponseDTO> getSubjects(@PathVariable String instID, @PathVariable String termID) {
        try {
            // Get all subjects
            return new ResponseEntity<>(
                    new IdentifierResponseDTO(new SourceURL(instID, termID), this.htmlParserService.parseSubjects(instID, termID)),
                    HttpStatus.OK
            );
        } catch (HttpStatusException e) {
            // Report and return html access failure
            reportHTTPAccessError(MessageBuilder.Type.SUBJECT, e);
            return new ResponseEntity<>(new BadAccessResponseDTO(e), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // Internal server error
            LOGGER.error(new MessageBuilder(MessageBuilder.Type.SUBJECT).addDetails(e));
            return new ResponseEntity<>(new APIErrorResponseDTO(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET Endpoint: /campuses/{instID}/terms/{termID}/subjects/{subjectID}
     * Get all courses for a given campus, term, and subject
     * Best used for finding courses for a single subject
     *
     * @param instID      Inst ID to search for courses
     * @param termID      Term ID to search for courses
     * @param subjectID   Subject ID to search for courses
     * @param crn         List of Course Reference Numbers to filter by
     * @param code        List of course codes to filter by. '*' wild card can be used ie 1** -> 101, 102, 110 etc
     * @param start_after Earliest time a class can start in 24hr format
     * @param end_before  Latest time a class can run in 24hr format
     * @param online      Only classes online sections
     * @param sync        Only synchronous sections
     * @param day         UH day of week codes to filter by. Append with '!' to inverse search ie !M -> get all sections not on Monday
     * @param instructor  Instructors to filter by. Append with '!' to inverse search ie !foo -> get all sections that don't have instructor 'foo'
     * @param keyword     Keywords to filter course names by. Append with '!' to inverse search ie !foo -> get all courses that don't have 'foo' in the name
     * @return List of courses for a given campus, term, and subject that pass filters
     */
    @GetMapping(value = "/{instID}/terms/{termID}/subjects/{subjectID}")
    public ResponseEntity<ResponseDTO> getCourses(
            @PathVariable String instID,
            @PathVariable String termID,
            @PathVariable String subjectID,
            @RequestParam(required = false) List<String> crn,
            @RequestParam(required = false) List<String> code,
            @RequestParam(required = false) String start_after,
            @RequestParam(required = false) String end_before,
            @RequestParam(required = false) String online,
            @RequestParam(required = false) String sync,
            @RequestParam(required = false) List<String> day,
            @RequestParam(required = false) List<String> instructor,
            @RequestParam(required = false) List<String> keyword) {
        try {
            // Build filter
            CourseFilter cf = new CourseFilter.Builder()
                    .setCRNs(crn)
                    .setCourseNumbers(code)
                    .setStartAfter(start_after)
                    .setEndBefore(end_before)
                    .setOnline(online)
                    .setSynchronous(sync)
                    .setDays(day)
                    .setInstructors(instructor)
                    .setKeywords(keyword)
                    .build();
            // Get all courses for subject
            List<CourseDTO> courseDTOs = this.htmlParserService.parseCourses(cf, instID, termID, subjectID);
            return new ResponseEntity<>(
                    new CourseResponseDTO(courseDTOs),
                    HttpStatus.OK
            );
        } catch (HttpStatusException e) {
            // Report and return html access failure
            reportHTTPAccessError(MessageBuilder.Type.SUBJECT, e);
            return new ResponseEntity<>(new BadAccessResponseDTO(e), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // Internal Server Error
            LOGGER.error(new MessageBuilder(MessageBuilder.Type.SUBJECT).addDetails(e));
            return new ResponseEntity<>(new APIErrorResponseDTO(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET Endpoint: /campuses/{instID}/terms/{termID}/courses
     * Get all courses for a given campus and term
     * Best used for finding courses for multiple subjects
     *
     * @param instID      Inst ID to search for courses
     * @param termID      Term ID to search for courses
     * @param sub         List of Subjects to filter by
     * @param crn         List of Course Reference Numbers to filter by
     * @param code        List of course codes to filter by. '*' wild card can be used ie 1** -> 101, 102, 110 etc
     * @param start_after Earliest time a class can start in 24hr format
     * @param end_before  Latest time a class can run in 24hr format
     * @param online      Only classes online sections
     * @param sync        Only synchronous sections
     * @param day         UH day of week codes to filter by. Append with '!' to inverse search ie !M -> get all sections not on Monday
     * @param instructor  Instructors to filter by. Append with '!' to inverse search ie !foo -> get all sections that don't have instructor 'foo'
     * @param keyword     Keywords to filter course names by. Append with '!' to inverse search ie !foo -> get all courses that don't have 'foo' in the name
     * @return List of courses for a given campus and term that pass filters
     */
    @GetMapping(value = "/{instID}/terms/{termID}/courses")
    public ResponseEntity<ResponseDTO> getCourses(
            @PathVariable String instID,
            @PathVariable String termID,
            @RequestParam(required = false) List<String> crn,
            @RequestParam(required = false) List<String> sub,
            @RequestParam(required = false) List<String> code,
            @RequestParam(required = false) String start_after,
            @RequestParam(required = false) String end_before,
            @RequestParam(required = false) String online,
            @RequestParam(required = false) String sync,
            @RequestParam(required = false) List<String> day,
            @RequestParam(required = false) List<String> instructor,
            @RequestParam(required = false) List<String> keyword) {
        try {
            // Get all available subjects
            Instant start = Instant.now();
            List<IdentifierDTO> subjects = this.htmlParserService.parseSubjects(instID, termID);

            // Build filter
            CourseFilter cf = new CourseFilter.Builder()
                    .setCRNs(crn)
                    .setSubjects(sub)
                    .setCourseNumbers(code)
                    .setStartAfter(start_after)
                    .setEndBefore(end_before)
                    .setOnline(online)
                    .setSynchronous(sync)
                    .setDays(day)
                    .setInstructors(instructor)
                    .setKeywords(keyword)
                    .build();

            // Parse each subject for courses
            List<String> failedSources = new ArrayList<>();
            List<CompletableFuture<List<CourseDTO>>> futures = new ArrayList<>();
            for (IdentifierDTO s : subjects) {

                // skip if not in filter
                if (!cf.validSubject(s.id()))
                    continue;

                // Add async job to queue
                SourceURL source = new SourceURL(instID, termID, s.id());
                futures.add(CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                // Attempt to parse
                                return this.htmlParserService.parseCourses(cf, instID, termID, s.id());
                            } catch (HttpStatusException e) {
                                // Report html access failure, add to failed sources and continue
                                reportHTTPAccessError(MessageBuilder.Type.COURSE, e);
                                LOGGER.warn(new MessageBuilder(MessageBuilder.Type.COURSE).addDetails("Skipping %s".formatted(source)));
                                failedSources.add(source.toString());
                            } catch (IOException e) {
                                // Internal server error, add to failed sources and continue
                                LOGGER.error(new MessageBuilder(MessageBuilder.Type.COURSE).addDetails(e));
                                failedSources.add(source.toString());
                            }
                            return new ArrayList<>();   // empty results
                        }));
            }
            // Join each thread / wait for each to finish
            futures.forEach(CompletableFuture::join);

            // Get all results
            List<CourseDTO> courseDTOs = new ArrayList<>();
            for (CompletableFuture<List<CourseDTO>> result : futures) {
                try {
                    courseDTOs.addAll(result.get());
                } catch (ExecutionException | InterruptedException e) {
                    LOGGER.error(new MessageBuilder(MessageBuilder.Type.COURSE).addDetails(e));
                }
            }

            // Report Success and return results
            int numSites = futures.size();
            LOGGER.info(new MessageBuilder(MessageBuilder.Type.COURSE)
                    .addDetails("Parsed %s site%s".formatted(numSites, numSites == 1 ? "" : "s"))
                    .setDuration(start));
            return new ResponseEntity<>(new CourseResponseDTO(courseDTOs, failedSources), HttpStatus.OK);
        } catch (HttpStatusException e) {
            // Report and return html access failure
            reportHTTPAccessError(MessageBuilder.Type.COURSE, e);
            return new ResponseEntity<>(new BadAccessResponseDTO(e), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // Internal Server error
            LOGGER.error(new MessageBuilder(MessageBuilder.Type.COURSE).addDetails(e));
            return new ResponseEntity<>(new APIErrorResponseDTO(e), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
