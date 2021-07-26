## CLEA Venue Consumer

Listener intended to anonymously retrieve decoded visits from the kafka queue and calculate exposures.

In addition, this module processes statistics on reports issued by clea-ws and generates its own statistics by location type.
Statistics are stored on an ELK stack.

### Visit inputs

The structure of a visit received from Kafka is:

- qrCodeScanTime: scan date as NTP Timestamp
- isBackward: A visit can be backward (scanned before pivot/reference date) or forward (scanned after pivot date)
- version: (internally used by the decoder library)
- type: (internally used by the decoder library)
- locationTemporaryPublicId: UUID as string
- encryptedLocationMessage: an array of bytes to be decrypted

An EncryptedLocationMessage is decrypted as :

- staff: The qrcode was generate for a staff (with longueur exposure time)
- qrCodeRenewalIntervalExponentCompact: 2^n seconds of validity of this qrCode before the need to create a new one with same UUID, new qrCodeValidityStartTime
- compressedPeriodStartTime: stored validity date as Hour (periodStartTimeAsNtpTimestamp = compressedPeriodStartTime\*3600)
- locationTemporarySecretKey: used to check the validity of unencrypted locationTemporaryPublicId
- encryptedLocationContactMessage: currently ignored, can only be decrypted by MSS
- venueType: used to determine the number of exposure period (slots) to consider, used for statistics
- venueCategory1: used to determine the number of exposure period (slots) to consider, used for statistics
- venueCategory2: used to determine the number of exposure period (slots) to consider, used for statistics

The LocationSpecificPart class combines both structures

### Verifications

For each record read from the kafka queue, the visit will be decrypted using the configured secret key and the following
verifications will be applied:

- if there an error happens while decrypting a specific visit, it will be rejected.
- if
  the [drift check]("https://hal.inria.fr/hal-03146022v3/document#processing-of-a-user-location-record-by-the-backend-server")
  fails, it will be rejected.
- if the public location temporary id is different from the one calculated from the location specific part, it
  will be rejected (see [hasValidTemporaryLocationPublicId]("src/main/java/fr/gouv/clea/consumer/service/impl/DecodedVisitService.java)).

### Exposition slots calculation

After a visit is decrypted and verified, a combination of exposition slots will be generated and stored in DB.

for more details on the
specs https://gitlab.inria.fr/stopcovid19/backend-server/-/blob/clea-doc/documentation/clea-specs.MD

### Purge

Each day, at 01:00 PM UTC (configurable), a scheduled job will launch to purge all outdated entries from DB.

Outdated entries are processed following the retention date (14 days).

### Statistics

#### By Reports

Stats pushed by clea-ws in a specific topic are moved without transformation to ElasticSearch.

If the transfert fails (ElasticSearch not available), clea-ws constantly tries to transfer it again.

#### By Location

Each valid decrypted visit is recorded in ElasticSearch (without locationTemporaryPublicId).
Document sent to ElasticSearch is:

- qrCodeScanTime
- venueType
- venueCategory1
- venueCategory2
- backward: 0/1
- forward: 0/1

After 14 days (retention date), we save an aggregate sum of backward and forward visits per period (1/2 hours),
and the detailed documents are removed from Elasticsearch.

If a visit cannot be pushed to ElasticSearch (ElasticSearch is not available), a failback pushes the document to an error topic.
A scheduled job may try to process this topic periodically.

### Run

This module is an uber-jar. You can use this command to execute it:

```bash
java -jar target/clea-venue-consumer*-exec.jar
```

This module can be executed by maven:

```bash
mvn spring-boot:run
```

the docker-compose.yml at the root of clea-server execute this service

```bash
$ docker-compose up
#or
$ docker-compose up clea-venue-consumer
```
