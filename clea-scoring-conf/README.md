## CLEA Scoring configuration

CLEA need to be flexible and apply various scoring configuration depending on the venue type, category1 and category2 of
a specific location.

- Exposure time configuration is injected and used by the clea-consumer
- Risk configuration is injected and used by the clea-batch

# Format

Scoring rules are defined according to the pattern :

- Risk rule :
    - venueType
    - venueCat1
    - venueCat2
    - clusterThresholdBackward
    - clusterThresholdForward
    - riskLevelBackward
    - riskLevelForward

- Exposure time rule :
    - venueType
    - venueCat1
    - venueCat2
    - exposureTimeBackward
    - exposureTimeForward
    - exposureTimeStaffBackward
    - exposureTimeStaffForward

A wildcard is available (*) and mean "any of".

Example for exposure time :

- '3,WILDCARD,WILDCARD,3,3,3,3' means : for venueType 3, any venueCat1 and any venueCat2, all values are 3.

# Configuration

Rules are configured in the application-"env".yml of both *clea-venue-consumer* and *clea-batch*

The *enabled* parameter is used to prevent Spring to try resolve configuration.

```
@ConfigurationProperties(prefix = "clea.conf.exposure")
@ConditionalOnProperty(value = "clea.conf.exposure.enabled", havingValue = "true")
```

example :

```
clea:
  conf:
    exposure:
      enabled: "true"
      rules:
        # venueType, venueCat1, venueCat2, exposureTimeBackward, exposureTimeForward, exposureTimeStaffBackward, exposureTimeStaffForward
        - "*,*,*,3,11,22,31"
        - "1,1,1,3,13,23,33"
        - "3,*,*,1,11,21,31"
        - "1,2,3,2,12,22,32"
    risk:
      enabled: "true"
      rules:
        # venueType, venueCat1, venueCat2, clusterThresholdBackward, clusterThresholdForward, riskLevelBackward, riskLevelForward
        - "*,*,*,3,1,3.0,2.0"
        - "1,1,1,3,1,3.0,2.0"
        - "1,2,3,3,1,3.0,2.0"
        - "3,*,*,3,1,3.0,2.0"
        - "3,1,*,3,1,3.0,2.0"
        - "3,*,2,3,1,3.0,2.0"
        - "3,1,2,3,1,3.0,2.0"

```

# Validation

For a venue (tuple of venueType, venueCat1 and venueCat2), only one rule is returned, the most specific one.

Priority is venueType > venueCat1 > venueCat2, it is implemented in **ScoringRuleComparator**

A **NoScoringConfigurationFoundException** is thrown when no matching rules were found for a tuples This case is never
supposed to happen according to the following validators.

Validators ensure that at least a default rule is available to be applied (**validators/CheckDefaultRulePresence**) and
VenueType must be specified if it is not the default rule (**validators/ValidateWildcards**).

A third validator ensure that there is no duplicates in the rules list (**validators/NoDuplicates**).

If at least one of those are not satisfied, the Spring Boot fail to load.

TODO:

- Simplify the configuration format (ex: allow copy/paste from Excel)
- Split ExposureTime specific configuration in clea-venue-consumer
- Split RiskRule specific configuration in clea-batch