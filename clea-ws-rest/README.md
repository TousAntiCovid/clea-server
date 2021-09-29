## Clea Ws Rest

Exposed [REST APIs]("src/main/resources/openapi-clea-server-v1.yml") for reporting 1..\* visits.

### Validity verifications:

When a report is sent, the following verifications are applied to the request:

- if any field does not respect its type, all the [request]("src/main/java/fr/gouv/clea/ws/vo/ReportRequest.java") is
  rejected
- Individual [visits]("src/main/java/fr/gouv/clea/ws/vo/Visit.java") that are not valid, will be pruned from the
  request, while other valid visits will be kept.

### Reporting verifications:

Pivot Date must be contained between the retention date and today, if not it will be set to the retention Date.

After the validity of the report is checked, all visits are decoded, and the following verifications are applied:

- if there's an error while decoding a specific visit, it will be individually purged.
- if a visit has its scan time before the retention date, it will be considered as outdated and rejected individually.
- if a visit has its scan time after today, it will be considered in future and rejected individually.
- Multiple visits for the same location(based on temporary id) will be considered as duplicates, if their qr scan time
  is withing the configured duplicate scan threshold. Only 1 is kept.

### Publishing reports:

After all verifications are applied, the decoded visits are sent to a kafka queue, to be processed by the next chain.

### Run

```bash
mvn spring-boot:run
```

Display the Clea API and use it: http://localhost:8088/swagger-ui/

Display the Clea OpenAPI generated from the code: http://localhost:8088/v3/api-docs?group=clea
