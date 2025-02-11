# Rainbow API v1
> API that parses course details for the University of Hawaii

**DISCLAIMER:** Front end development is hard, but there are some 'questionable' decisions in the layout of the UH tables.
I've done my best to parse all the information and report when failures occur, but some details may get lost.

## Quickstart Guide

### Bootstrap Script
```bash
# Uses gradle wrapper to build and execute jar
./bootstrap.sh
```

Launches API service available at http://localhost:8080/v1

API endpoint documentation can be found at [API Endpoints](#api-endpoints)

## Other Run Methods

### Gradle
```bash
gradle bootRun    # via Gradle
./gradlew bootRun # or via Gradle Wrapper
``` 

### Jar
```bash
# Build Jar
gradle bootJar    # via Gradle
./gradlew bootJar # or via Gradle Wrapper

# Run Jar
java -jar ./build/libs/rainbow-1.1.0.jar
```

### Docker
```bash
docker build -t rainbow-api:1.1.0 .
docker run --rm -p 8080:8080 rainbow-api:1.1.0
```

## API Endpoints
> Insomnia documentation is also available [here](docs/rainbow-api-v1-docs.yaml)

- [Get all Campuses](#get-all-campuses)
- [Get all Terms](#get-all-terms)
- [Get all Subjects](#get-all-courses-subjects)
- [Get all Sections (/subjects)](#get-all-sections-subjects)
- [Get all Sections (/courses)](#get-all-sections-courses)
- [Generate Potential Schedules](#section-scheduler)

### Get all Campuses
> Get list of University of Hawaii Campuses

**Endpoint:** `http://localhost:8080/v1/campuses`

**Request Method:** `GET`

**Responses**

| Response Code |        Type        |                    Description                    |
|:-------------:|:------------------:|:-------------------------------------------------:|
|      200      | IdentifierResponse | List of University of Hawaii campus names and IDs |
|      400      | BadAccessResponse  |    Failed to access resource at requested URL     |
|      500      |  APIErrorResponse  |       Internal server error during parsing        |

**Example**
```bash
curl http://localhost:8080/v1/campuses
```

### Get all Terms
> Get all terms for a University of Hawaii campus

**Endpoint:** `http://localhost:8080/v1/campuses/{instID}/terms`

**Request Method:** `GET`

**Path Variables**

| Variable |  Type  |            Description            | Is Required? |
|:--------:|:------:|:---------------------------------:|:------------:|
|  instID  | String | UH Campus ID to get the terms for |     YES      |


**Responses**

| Response Code |        Type        |                Description                 |
|:-------------:|:------------------:|:------------------------------------------:|
|      200      | IdentifierResponse |         List of term names and IDs         |
|      400      | BadAccessResponse  | Failed to access resource at requested URL |
|      500      |  APIErrorResponse  |    Internal server error during parsing    |

**Example**
```bash
# Get all terms for the University of Hawaii at Manoa
curl http://localhost:8080/v1/campuses/man/terms
```

### Get all Subjects
> Get all subjects for a University of Hawaii campus and term

**Endpoint:** `http://localhost:8080/v1/campuses/{instID}/terms/{termID}/subjects`

**Request Method:** `GET`

**Path Variables**

| Variable |  Type  |             Description              |
|:--------:|:------:|:------------------------------------:|
|  instID  | String | UH Campus ID to get the subjects for |
|  termID  | String |   Term ID to get the subjects for    |


**Responses**

| Response Code |        Type        |                Description                 |
|:-------------:|:------------------:|:------------------------------------------:|
|      200      | IdentifierResponse |       List of subject names and IDs        |
|      400      | BadAccessResponse  | Failed to access resource at requested URL |
|      500      |  APIErrorResponse  |    Internal server error during parsing    |

**Example**
```bash
# Get all subjects offered at the University of Hawaii at Manoa for Fall 2024
curl http://localhost:8080/v1/campuses/man/terms/202510/subjects
```

### Get all Sections (/subjects)
> Get all sections for a University of Hawaii campus, term, and specific subject
> 
> Best used for finding sections for a single subject

**Endpoint:** `http://localhost:8080/v1/campuses/{instID}/terms/{termID}/subjects/{subjectID}`

**Request Method:** `GET`

**Path Variables**

| Variable  |  Type  |             Description             |
|:---------:|:------:|:-----------------------------------:|
|  instID   | String | UH Campus ID to get the sections for |
|  termID   | String |   Term ID to get the sections for    |
| subjectID | String |  Subject ID to get the sections for  |

**Query Params**
> Optional filters

|  Variable   |   Type   |                                                                         Description                                                                         |   Examples   |
|:-----------:|:--------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------:|:------------:|
|     crn     | String[] |                                                Comma seperated list of Course Reference Numbers to filter by                                                | 75380, 75381 |
|    code     | String[] |                        Comma seperated list of course codes to filter by. '*' wild card can be used<br/>ie 1** -> 101, 102, 110 etc                         |   101, 1**   |
| start_after |  String  |                                                    Earliest time a class can start in 24hr, HHmm format                                                     |  0900, 1300  |
| end_before  |  String  |                                                      Latest time a class can run in 24hr, HHmm format                                                       |  1500, 1700  |
|   online    |   bool   |                                                                Only classes online sections                                                                 | true, false  |
|    sync     |   bool   |                                                                  Only synchronous sections                                                                  | true, false  |
|     day     | String[] |          Comma seperated list of UH day of week codes to filter by.<br/>Append with '!' to inverse search ie !M -> get all sections not on Monday           |    M, !T     |
| instructor  | String[] |    Comma seperated list of instructors to filter by.<br/>Append with '!' to inverse search ie !foo -> get all sections that don't have instructor 'foo'     |  foo, !foo   |
|   keyword   | String[] | Comma seperated list keywords to filter course names by.<br/>Append with '!' to inverse search ie !foo -> get all sections that don't have 'foo' in the name |  foo, !foo   |


**Responses**

| Response Code |       Type        |                Description                 |
|:-------------:|:-----------------:|:------------------------------------------:|
|      200      |  CourseResponse   |       List of subject names and IDs        |
|      400      | BadAccessResponse | Failed to access resource at requested URL |
|      500      | APIErrorResponse  |    Internal server error during parsing    |

**Examples**
```bash
# Get all sections for ICS offered at the University of Hawaii at Manoa for Fall 2024
curl http://localhost:8080/v1/campuses/man/terms/202510/subjects/ics

# Get ICS 101, 211, and any 300 level course offered at the University of Hawaii at Manoa for Fall 2024
curl http://localhost:8080/v1/campuses/man/terms/202510/subjects/ics?code=101,211,3**

# Get all sections for ICS offered at the University of Hawaii at Manoa for Fall 2024 that aren't on Monday and starts after 10:00 am
curl http://localhost:8080/v1/campuses/man/terms/202510/subjects/ics?day=!m&start_after=1000
```

### Get all Sections (/courses)
> Get all sections for a University of Hawaii campus and term
>
> Best used for finding sections for multiple subjects

**Endpoint:** `http://localhost:8080/v1/campuses/{instID}/terms/{termID}/courses`

**Request Method:** `GET`

**Path Variables**

| Variable  |  Type  |             Description             |
|:---------:|:------:|:-----------------------------------:|
|  instID   | String | UH Campus ID to get the sections for |
|  termID   | String |   Term ID to get the sections for    |

**Query Params**
> Optional filters

|  Variable   |   Type   |                                                                         Description                                                                          |    Examples     |
|:-----------:|:--------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------------:|
|     crn     | String[] |                                                Comma seperated list of Course Reference Numbers to filter by                                                 |  75380, 75381   |
|     sub     | String[] |                                                        Comma seperated list of subjects to filter by                                                         | ICS, HIST, ECON |
|    code     | String[] |                         Comma seperated list of course codes to filter by. '*' wild card can be used<br/>ie 1** -> 101, 102, 110 etc                         |    101, 1**     |
|     cid     | String[] |                       Comma seperated list of full course ids to filter by. '*' wild card can be used<br/>ie 1** -> 101, 102, 110 etc                        | ICS101, ICS1**  |
| start_after |  String  |                                                     Earliest time a class can start in 24hr, HHmm format                                                     |   0900, 1300    |
| end_before  |  String  |                                                       Latest time a class can run in 24hr, HHmm format                                                       |   1500, 1700    |
|   online    |   bool   |                                                                 Only classes online sections                                                                 |   true, false   |
|    sync     |   bool   |                                                                  Only synchronous sections                                                                   |   true, false   |
|     day     | String[] |           Comma seperated list of UH day of week codes to filter by.<br/>Append with '!' to inverse search ie !M -> get all sections not on Monday           |      M, !T      |
| instructor  | String[] |     Comma seperated list of instructors to filter by.<br/>Append with '!' to inverse search ie !foo -> get all sections that don't have instructor 'foo'     |    foo, !foo    |
|   keyword   | String[] | Comma seperated list keywords to filter course names by.<br/>Append with '!' to inverse search ie !foo -> get all sections that don't have 'foo' in the name |    foo, !foo    |


**Responses**

| Response Code |       Type        |                Description                 |
|:-------------:|:-----------------:|:------------------------------------------:|
|      200      |  CourseResponse   |       List of subject names and IDs        |
|      400      | BadAccessResponse | Failed to access resource at requested URL |
|      500      | APIErrorResponse  |    Internal server error during parsing    |


### Section Scheduler
> Filter sections and generate all possible schedules
>
> Warning: Increasing the number of sections increases the calculation time exponentially.
> 
> Benchmarking
> - 2 Classes (42 combinations) ~= 0.01s
> - 4 Classes (336 combinations) ~= 0.067s 
> - 6 Classes (2,688 combinations) ~= 15.308s.

**Endpoint:** `http://localhost:8080/v1/{instID}/terms/{termID}/scheduler`

**Request Method:** `GET`

**Path Variables**

| Variable |  Type  |             Description              |
|:--------:|:------:|:------------------------------------:|
|  instID  | String | UH Campus ID to get the sections for |
|  termID  | String |   Term ID to get the sections for    |

**Query Params**
> Optional filters. To use the additional filters, use the get sections endpoint and provide the crn

|  Variable   |   Type   |                                                                         Description                                                                          |    Examples     |
|:-----------:|:--------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------------:|
|     crn     | String[] |                                                Comma seperated list of Course Reference Numbers to filter by                                                 |  75380, 75381   |
|     cid     | String[] |                       Comma seperated list of full course ids to filter by. '*' wild card can be used<br/>ie 1** -> 101, 102, 110 etc                        | ICS101, ICS1**  |
| start_after |  String  |                                                     Earliest time a class can start in 24hr, HHmm format                                                     |   0900, 1300    |
| end_before  |  String  |                                                       Latest time a class can run in 24hr, HHmm format                                                       |   1500, 1700    |
|   online    |   bool   |                                                                 Only classes online sections                                                                 |   true, false   |
|    sync     |   bool   |                                                                  Only synchronous sections                                                                   |   true, false   |
|     day     | String[] |           Comma seperated list of UH day of week codes to filter by.<br/>Append with '!' to inverse search ie !M -> get all sections not on Monday           |      M, !T      |


**Responses**

| Response Code |       Type        |                   Description                   |
|:-------------:|:-----------------:|:-----------------------------------------------:|
|      200      | ScheduleResponse  | List of schedules that match the given criteria |
|      400      | BadAccessResponse |   Failed to access resource at requested URL    |
|      500      | APIErrorResponse  |      Internal server error during parsing       |

**Examples**
```bash
# Generate all schedules for ICS 101, ICS 111, and ICS 141 offered at the University of Hawaii at Manoa for Fall 2024
curl http://localhost:8080/v1/scheduler/MAN/terms/202510?cid=ICS101,ICS111,ICS141

# Generate all schedules for ICS 101 and ICS 111 that start after 12:00 pm offered at the University of Hawaii at Manoa for Fall 2024
curl http://localhost:8080/v1/scheduler/MAN/terms/202510?cid=ICS101,ICS111,ICS141&start_after=1200
```

## Response JSONs

### [IdentifierResponse](docs/example-responses/IdentifierResponse.json)
JSON response of identifiers
```json
{
  "timestamp": "timestamp",
  "source": "source url where the information was parsed",
  "identifiers": [
    {
      "id": "ID",
      "name": "NAME"
    }
  ]
}
```

### [CourseResponse](docs/example-responses/CourseResponse.json)
```json
{
  "timestamp": "timestamp",
  "courses": [
    {
      "source": "source url where the information was parsed",
      "cid": "Course ID ( ICS 101 )",
      "name": "Name of course",
      "credits": "Number of credits",
      "sections": [
        {
          "url": "URL with additional info about the section",
          "sid": "Section number",
          "crn": "Course Reference Number",
          "instructor": "Instructor name",
          "curr_enrolled": "Number of students enrolled",
          "seats_available": "Number of seats available",
          "failed_meetings": "Number of meetings that failed to parse",
          "additional_details": [
            "Additional details for the course"
          ],
          "meetings": [
            {
              "day": "Day of week",
              "room": "Name of room",
              "start_time": "Start time",
              "end_time": "End time",
              "start_date": "Start date",
              "end_date": "End date"
            }
          ]
        }
      ]
    }
  ]
}
```

### [ScheduleResponse](docs/example-responses/ScheduleResponse.json)
> `tba` is a special "day". Since no date / time has been supplied, it is reported by skipped when generating schedules.
> 
> Each day contains simplified course info
```json
{
  "timestamp": "timestamp",
  "schedules": [
    {
      "tba": [],
      "sunday": [],
      "monday": [
        {
          "course": "Name of course",
          "cid": "Course ID ( ICS 101 )",
          "sid": "Section number",
          "crn": "Course Reference Number",
          "instructor": "Instructor name",
          "room": "Name of room",
          "start_time": "Start time",
          "end_time": "End time",
          "url": "URL with additional info about the section"
        }
      ],
      "tuesday": [],
      "wednesday": [],
      "thursday": [],
      "friday": [],
      "saturday": []
    }
  ]
}
```

### [BadAccessResponse](docs/example-responses/BadAccessResponse.json)
```json
{
  "timestamp": "Timestamp",
  "source": "URL attempted to access",
  "response_code": "Response code for the source url",
  "response_message": "Response message for the source url",
  "error_msg": "Failed to access resource at source url; Check to make sure the source url is valid"
}
```

### [APIErrorResponse](docs/example-responses/APIErrorResponse.json)
```json
{
  "timestamp": "Timestamp",
  "error_message": "Something failed when processing request",
  "error": "Server Error message"
}
```
