Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk

  Background:
    Given "Laure" registered on TAC
    And "Henry" registered on TAC
    And "Yaël" registered on TAC
    And VType of "restaurant", VCategory1 of "fastfood" and VCategory2 of 1 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,60,3.0) and for forward (1,60,2.0)
    And VType of "discotheque", VCategory1 of "discotheque" and VCategory2 of 2 has risk configuration of (Threshold , ExposureTime, Risklevel) for backward (3,180,3.0) and for forward (1,180,2.0)
    And "Burger king" created a dynamic QRCode at 04:00, 10 days ago with VType as "restaurant" and with VCategory1 as "fastfood" and with VCategory2 as 1 and with a renewal time of "15 minutes" and with a periodDuration of "24 hours"
    And "Le Klub" created a dynamic QRCode at 04:00, 10 days ago with VType as "discotheque" and with VCategory1 as "discotheque" and with VCategory2 as 2 and with a renewal time of "15 minutes" and with a periodDuration of "24 hours"
    And "OrangeBleue" created a static QRCode at 11:00, 8 days ago with VType as "etablissements sportifs" and with VCategory1 as "salle de sport" and with VCategory2 as 2 and with a periodDuration of "24 hours"

  Scenario: Sanitary statistics

    Given "Laure" recorded a visit to "Burger king" at 12:45, 6 days ago
    And "Henry" recorded a visit to "Burger king" at 12:57, 6 days ago
    And "Laure" recorded a visit to "Burger king" at 12:45, 4 days ago
    And "Henry" recorded a visit to "Burger king" at 20:57, 2 days ago

    Given "Henry" recorded a visit to "Le Klub" at 11:46, 4 days ago
    And "Henry" recorded a visit to "Le Klub" at 11:47, 4 days ago

    Given "Yaël" recorded a visit to "OrangeBleue" at 16:30, 18 days ago

    When "Laure" declares herself sick with a 5 days ago pivot date
    Then "Laure" sends her visits
    And statistics by wreport are
      | reported | rejected | is_closed | backwards | forwards |
      | 2        | 0        | 0         | 1         | 1        |
    And statistics by location are
      | venue_type | venue_category1 | venue_category2 | backward_visits | forward_visits |
      | 1          | 1               | 1               | 1               | 0              |
      | 1          | 1               | 1               | 0               | 1              |
    When "Henry" declares himself sick with a 3 days ago pivot date

    Then "Henry" sends his visits
    And statistics by wreport are
      | reported | rejected | is_closed | backwards | forwards |
      | 2        | 0        | 0         | 1         | 1        |
      | 4        | 1        | 0         | 2         | 1        |
    And statistics by location are
      | venue_type | venue_category1 | venue_category2 | backward_visits | forward_visits |
      | 2          | 0               | 2               | 1               | 0              |
      | 1          | 1               | 1               | 2               | 0              |
      | 1          | 1               | 1               | 0               | 1              |
      | 1          | 1               | 1               | 0               | 1              |

    When "Yaël" declares himself sick with a 5 days ago pivot date
    Then "Yaël" sends his visits
    And statistics by wreport are
      | reported | rejected | is_closed | backwards | forwards |
      | 2        | 0        | 0         | 1         | 1        |
      | 4        | 1        | 0         | 2         | 1        |
      | 1        | 1        | 0         | 0         | 0        |
    And statistics by location are
      | venue_type | venue_category1 | venue_category2 | backward_visits | forward_visits |
      | 2          | 0               | 2               | 1               | 0              |
      | 1          | 1               | 1               | 2               | 0              |
      | 1          | 1               | 1               | 0               | 1              |
      | 1          | 1               | 1               | 0               | 1              |
