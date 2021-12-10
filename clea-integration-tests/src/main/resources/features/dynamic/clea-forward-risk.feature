Feature: Several healthy visitors and a single sick visitor visit different places
  Visits are simultaneous or not
  The healthy visitors must be warned being at risk

  Background:
    Given users Hugo, Heather, Henry are registered on TAC
    Given "Chez Gusto" manager configured qrcode generators at 04:00, 10 days ago with venue type 1, venue category 1 1, venue category 2 1, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours

  Scenario: One sick and two persons at risk (same location and average RiskLevel)
    Given Hugo recorded a visit to "Chez Gusto" at 14:45, 4 days ago
    Given Henry recorded a visit to "Chez Gusto" at 14:00, 4 days ago
    Given Heather recorded a visit to "Chez Gusto" at 14:00, 4 days ago

    When Heather declares himself sick
    When cluster detection triggered

    Then Heather sends his visits
    Then exposure status should reports Hugo as being at risk of 2.0
    Then exposure status should reports Henry as being at risk of 2.0
