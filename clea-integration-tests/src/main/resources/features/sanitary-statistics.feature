Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk

  Background:
    Given "Laure" registered on TAC
    And "Henry" registered on TAC
    And "Yaël" registered on TAC
    And VType of 1, VCategory1 of 1 and VCategory2 of 1
    And VType of 2, VCategory1 of 0 and VCategory2 of 2
    And "Burger king" created a dynamic QRCode at 04:00, 10 days ago with VType as 1, with VCategory1 as 1, with VCategory2 as 1, with a renewal time of "15 minutes" and with a periodDuration of "24 hours"
    And "Le Klub" created a dynamic QRCode at 04:00, 10 days ago with VType as 2, with VCategory1 as 0, with VCategory2 as 2, with a renewal time of "15 minutes" and with a periodDuration of "24 hours"
    And "OrangeBleue" created a static QRCode at 11:00, 8 days ago with VType as 4, with VCategory1 as 4, with VCategory2 as 2 and with a periodDuration of "24 hours"

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
      | venue_type | venue_category1 | venue_category2 | backward_visits | forward_visits | period_start      |
      | 1          | 1               | 1               | 1               | 0              | 12:00, 6 days ago |
      | 1          | 1               | 1               | 0               | 1              | 12:00, 4 days ago |
    When "Henry" declares himself sick with a 3 days ago pivot date

    Then "Henry" sends his visits
    And statistics by wreport are
      | reported | rejected | is_closed | backwards | forwards |
      | 2        | 0        | 0         | 1         | 1        |
      | 4        | 1        | 0         | 2         | 1        |
    And statistics by location are
      | venue_type | venue_category1 | venue_category2 | backward_visits | forward_visits | period_start      |
      | 2          | 0               | 2               | 1               | 0              | 11:00, 4 days ago |
      | 1          | 1               | 1               | 2               | 0              | 12:00, 6 days ago |
      | 1          | 1               | 1               | 0               | 1              | 12:00, 4 days ago |
      | 1          | 1               | 1               | 0               | 1              | 20:00, 2 days ago |

    When "Yaël" declares himself sick with a 5 days ago pivot date
    Then "Yaël" sends his visits
    And statistics by wreport are
      | reported | rejected | is_closed | backwards | forwards |
      | 2        | 0        | 0         | 1         | 1        |
      | 4        | 1        | 0         | 2         | 1        |
      | 1        | 1        | 0         | 0         | 0        |
    And statistics by location are
      | venue_type | venue_category1 | venue_category2 | backward_visits | forward_visits | period_start      |
      | 2          | 0               | 2               | 1               | 0              | 11:00, 4 days ago |
      | 1          | 1               | 1               | 2               | 0              | 12:00, 6 days ago |
      | 1          | 1               | 1               | 0               | 1              | 12:00, 4 days ago |
      | 1          | 1               | 1               | 0               | 1              | 20:00, 2 days ago |
