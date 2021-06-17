## CLEA Venue Consumer

Listener meant to retrieve decoded visits from the kafka queue anonymously, and calculating expositions.

### Verifications

For each record read from the kafka queue, the visit will be decrypted using the configured secret key and the following
verifications will be applied:

- if there's an error while decrypting a specific visit, it will be rejected.
- if
  the [drift check]("https://gitlab.inria.fr/stopcovid19/CLEA-exposure-verification/-/blob/master/documents/CLEA-specification-EN.md#processing-of-a-user-location-record-by-the-backend-server")
  fails, it will be rejected.
- if the temporary public if of the location, is different from the one calculated from the location specific part, it
  will be rejected.

### Exposition slots calculation

After a visit is decrypted and verified, a combination of exposition slots will be generated and stored in DB.

for more details on the
specs https://gitlab.inria.fr/stopcovid19/backend-server/-/blob/clea-doc/documentation/clea-specs.MD

### Purge

Each day, at 01:00 PM UTC (configurable), a croned job will launch to purge all outdated entries from DB.

Outdated entries are processed following the retention date (14 days).
