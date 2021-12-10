Feature: Record health statistics

  Background:
    Given Laure registered on TAC
    And Henry registered on TAC
    And Yaël registered on TAC
    And Place named "Burger King" with venue type 1, venue category 1 1, venue category 2 1, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours
    And Place named "Le Klub" with venue type 2, venue category 1 0, venue category 2 2, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours
    And Place named "OrangeBleue" with venue type 4, venue category 1 4, venue category 2 2, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours
    And "Burger King" created a dynamic deeplink at 04:00, 10 days ago
    And "Le Klub" created a dynamic deeplink at 04:00, 10 days ago
    And "OrangeBleue" created a static deeplink at 04:00, 10 days ago

  Scenario: Sanitary statistics

    Given Laure recorded a visit to "Burger King" at 12:45, 6 days ago
    And Henry recorded a visit to "Burger King" at 12:57, 6 days ago
    And Laure recorded a visit to "Burger King" at 12:45, 4 days ago
    And Henry recorded a visit to "Burger King" at 20:57, 2 days ago

    Given Henry recorded a visit to "Le Klub" at 11:46, 4 days ago
    And Henry recorded a visit to "Le Klub" at 11:47, 4 days ago

    # Laure scanned two different deepLinks in less than "duplicateScanThresholdInSeconds" (default 1800)
    Given Laure recorded a visit to "Le Klub" at 12:59, 4 days ago

    Given Yaël recorded a visit to "OrangeBleue" at 16:30, 18 days ago

    When Laure declares herself sick with a 5 days ago pivot date
    Then Laure sends her visits
    And statistics by wreport are
      | reported | rejected | close | backwards | forwards |
      | 3        | 0        | 1     | 1         | 2        |
    And statistics by location are
      | venue type | venue category1 | venue category2 | backward visits | forward visits | period start      |
      | 1          | 1               | 1               | 1               | 0              | 12:00, 6 days ago |
      | 1          | 1               | 1               | 0               | 1              | 12:00, 4 days ago |
      | 2          | 0               | 2               | 0               | 1              | 12:00, 4 days ago |

    When Henry declares himself sick with a 3 days ago pivot date

    Then Henry sends his visits
    And statistics by wreport are
      | reported | rejected | close | backwards | forwards |
      | 3        | 0        | 1     | 1         | 2        |
      | 4        | 1        | 0     | 2         | 1        |
    And statistics by location are
      | venue type | venue category1 | venue category2 | backward visits | forward visits | period start      |
      | 2          | 0               | 2               | 1               | 0              | 11:00, 4 days ago |
      | 2          | 0               | 2               | 0               | 1              | 12:00, 4 days ago |
      | 1          | 1               | 1               | 2               | 0              | 12:00, 6 days ago |
      | 1          | 1               | 1               | 0               | 1              | 12:00, 4 days ago |
      | 1          | 1               | 1               | 0               | 1              | 20:00, 2 days ago |

    When Yaël declares himself sick with a 5 days ago pivot date
    Then Yaël sends his visits
    And statistics by wreport are
      | reported | rejected | close | backwards | forwards |
      | 3        | 0        | 1     | 1         | 2        |
      | 4        | 1        | 0     | 2         | 1        |
      | 1        | 1        | 0     | 0         | 0        |
    And statistics by location are
      | venue type | venue category1 | venue category2 | backward visits | forward visits | period start      |
      | 2          | 0               | 2               | 1               | 0              | 11:00, 4 days ago |
      | 2          | 0               | 2               | 0               | 1              | 12:00, 4 days ago |
      | 1          | 1               | 1               | 2               | 0              | 12:00, 6 days ago |
      | 1          | 1               | 1               | 0               | 1              | 12:00, 4 days ago |
      | 1          | 1               | 1               | 0               | 1              | 20:00, 2 days ago |
