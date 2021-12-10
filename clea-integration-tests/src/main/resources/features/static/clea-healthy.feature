Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk

  Background:
    Given users Hugo, Heather, Henry, Laure, Anne, Julie, Mahe, Yaël are registered on TAC
    Given "McDonald's" manager generated qrcodes at 11:00, 13 days ago has configuration: venue type 1, venue category 1 1, venue category 2 1
    Given "NRFight Club Olympiades" manager generated qrcodes at 11:00, 13 days ago has configuration: venue type 4, venue category 1 2, venue category 2 2
    Given "OrangeBleue" manager generated qrcodes at 11:00, 8 days ago has configuration: venue type 4, venue category 1 1, venue category 2 2

  Scenario: One location with duplicated visits - Exposure Time 30 min
    Given Hugo recorded a visit to "McDonald's" at 12:30, 6 days ago
    Given Hugo recorded a visit to "McDonald's" at 12:35, 6 days ago
    Given Hugo recorded a visit to "McDonald's" at 15:35, 6 days ago
    Given Heather recorded a visit to "McDonald's" at 13:30, 6 days ago
    Given Henry recorded a visit to "McDonald's" at 11:45, 6 days ago
    Given Laure recorded a visit to "McDonald's" at 12:59, 6 days ago
    Given Anne recorded a visit to "McDonald's" at 20:30, 6 days ago
    Given Julie recorded a visit to "McDonald's" at 16:00, 6 days ago
    Given Mahe recorded a visit to "McDonald's" at 12:13, 6 days ago

    When Hugo declares himself sick with a 14 days ago pivot date
    When Heather declares himself sick with a 12 days ago pivot date
    When Henry declares himself sick with a 8 days ago pivot date
    When Laure declares herself sick with a 7 days ago pivot date
    When cluster detection triggered

    When Anne asks for exposure status
    When Julie asks for exposure status
    When Mahe asks for exposure status

    Then Hugo sends his visits
    Then Heather sends her visits
    Then Henry sends her visits
    Then Laure sends her visits
    And Hugo has 1 rejected visit

    Then exposure status should reports Anne as not being at risk
    Then exposure status should reports Julie as being at risk of 2.0
    Then exposure status should reports Mahe as being at risk of 2.0

  Scenario: One location with duplicated visits - Exposure Time 120 min
    Given Hugo recorded a visit to "NRFight Club Olympiades" at 12:30, 13 days ago
    Given Hugo recorded a visit to "NRFight Club Olympiades" at 12:32, 13 days ago
    Given Hugo recorded a visit to "NRFight Club Olympiades" at 12:56, 13 days ago
    Given Hugo recorded a visit to "NRFight Club Olympiades" at 12:30, 2 days ago
    Given Heather recorded a visit to "NRFight Club Olympiades" at 13:30, 13 days ago
    Given Henry recorded a visit to "NRFight Club Olympiades" at 11:46, 13 days ago
    Given Anne recorded a visit to "NRFight Club Olympiades" at 12:31, 13 days ago
    Given Julie recorded a visit to "NRFight Club Olympiades" at 13:00, 2 days ago
    Given Mahe recorded a visit to "NRFight Club Olympiades" at 20:13, 13 days ago

    When Hugo declares himself sick with a 8 days ago pivot date
    When Heather declares himself sick with a 6 days ago pivot date
    When Henry declares himself sick with a 7 days ago pivot date
    When cluster detection triggered

    When Anne asks for exposure status
    When Julie asks for exposure status
    When Mahe asks for exposure status

    Then Hugo sends his visits
    Then Heather sends her visits
    Then Henry sends her visits
    And Hugo has 2 rejected visits

    Then exposure status should reports Anne as being at risk of 3.0
    Then exposure status should reports Julie as being at risk of 2.0
    Then exposure status should reports Mahe as not being at risk

  Scenario: One location with duplicated visits - Exposure Time 60 min
    Given Hugo recorded a visit to "OrangeBleue" at 15:30, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 15:46, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 15:56, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 15:57, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 15:58, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 15:59, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 16:00, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 16:01, 8 days ago
    Given Heather recorded a visit to "OrangeBleue" at 13:30, 8 days ago
    Given Henry recorded a visit to "OrangeBleue" at 13:46, 8 days ago
    Given Anne recorded a visit to "OrangeBleue" at 14:31, 8 days ago
    Given Julie recorded a visit to "OrangeBleue" at 21:00, 8 days ago
    Given Mahe recorded a visit to "OrangeBleue" at 19:13, 7 days ago

    When Hugo declares himself sick with a 5 days ago pivot date
    When Heather declares himself sick with a 4 days ago pivot date
    When Henry declares himself sick with a 5 days ago pivot date
    When cluster detection triggered

    When Anne asks for exposure status
    When Julie asks for exposure status
    When Mahe asks for exposure status

    Then Hugo sends his visits
    Then Heather sends her visits
    Then Henry sends her visits
    And Hugo has 6 rejected visits

    Then exposure status should reports Anne as being at risk of 3.0
    Then exposure status should reports Julie as not being at risk
    Then exposure status should reports Mahe as not being at risk

  Scenario: Overlaps - 3 days with 3 different location and malformed pivot date for 1 person

    Given Anne recorded a visit to "McDonald's" at 11:50, 6 days ago
    Given Hugo recorded a visit to "McDonald's" at 12:30, 6 days ago
    Given Laure recorded a visit to "McDonald's" at 12:45, 6 days ago
    Given Heather recorded a visit to "McDonald's" at 12:58, 6 days ago
    Given Julie recorded a visit to "McDonald's" at 12:50, 6 days ago

    Given Anne recorded a visit to "NRFight Club Olympiades" at 11:58, 4 days ago
    Given Mahe recorded a visit to "NRFight Club Olympiades" at 11:50, 4 days ago
    Given Henry recorded a visit to "NRFight Club Olympiades" at 11:46, 4 days ago
    Given Hugo recorded a visit to "NRFight Club Olympiades" at 12:30, 4 days ago
    Given Julie recorded a visit to "NRFight Club Olympiades" at 13:45, 4 days ago
    Given Yaël recorded a visit to "NRFight Club Olympiades" at 13:45, 10 days ago
    Given Anne recorded a visit to "NRFight Club Olympiades" at 14:06, 4 days ago

    Given Anne recorded a visit to "OrangeBleue" at 14:03, 8 days ago
    Given Mahe recorded a visit to "OrangeBleue" at 14:12, 8 days ago
    Given Hugo recorded a visit to "OrangeBleue" at 15:30, 8 days ago
    Given Heather recorded a visit to "OrangeBleue" at 16:30, 8 days ago

    When Henry declares himself sick with a 3 days ago pivot date
    When Laure declares herself sick with a 5 days ago pivot date
    When cluster detection triggered
    When Julie asks for exposure status
    When Mahe asks for exposure status
    Then Henry sends her visits
    Then Laure sends her visits
    Then exposure status should reports Julie as not being at risk
    Then exposure status should reports Mahe as not being at risk

    When Hugo declares himself sick with a 7 days ago pivot date
    When Heather declares himself sick with a 5 days ago pivot date
    When cluster detection triggered
    When Julie asks for exposure status
    When Mahe asks for exposure status
    Then Hugo sends his visits
    Then Heather sends her visits
    Then exposure status should reports Julie as being at risk of 2.0
    Then exposure status should reports Mahe as being at risk of 2.0

    When Anne declares herself sick with a 5 days ago pivot date
    When Yaël declares himself sick with malformed pivot date
    When cluster detection triggered
    When Julie asks for exposure status
    When Mahe asks for exposure status
    Then Anne sends her visits
    Then Yaël cannot send his visits
    Then exposure status should reports Julie as being at risk of 2.0
    Then exposure status should reports Mahe as being at risk of 2.0
#FIXME
  Scenario: Visits staff trigger a cluster of no STAFF visits
    Given Hugo recorded a visit as a STAFF to "McDonald's" at 20:32, 6 days ago
    Given Henry recorded a visit as a STAFF to "McDonald's" at 20:40, 6 days ago
    Given Laure recorded a visit as a STAFF to "McDonald's" at 20:30, 6 days ago
    Given Julie recorded a visit to "McDonald's" at 20:55, 6 days ago

    When Hugo declares himself sick with a 5 days ago pivot date
    When Henry declares himself sick with a 3 days ago pivot date
    When Laure declares herself sick with a 5 days ago pivot date
    When cluster detection triggered

    When Julie asks for exposure status

    Then Hugo sends his visits
    Then Henry sends his visits
    Then Laure sends her visits

    Then exposure status should reports Julie as being at risk of 3.0

  Scenario: Nominal case
    Given Hugo recorded a visit to "McDonald's" at 12:30, 4 days ago
    Given Henry recorded a visit to "McDonald's" at 11:30, 4 days ago
    Given Heather recorded a visit to "McDonald's" at 13:35, 4 days ago

    When Heather declares himself sick with a 5 days ago pivot date
    When cluster detection triggered
    When Hugo asks for exposure status
    When Henry asks for exposure status

    Then Heather sends his visits
    Then exposure status should reports Hugo as not being at risk
    Then exposure status should reports Henry as not being at risk

  Scenario: Duplicated deepLink
    Given Hugo recorded a visit to "McDonald's" at 12:30, 6 days ago
    Given Hugo recorded a visit to "McDonald's" at 12:35, 6 days ago
    Given Laure recorded a visit to "McDonald's" at 12:59, 6 days ago
    When Hugo declares himself sick with a 14 days ago pivot date
    When cluster detection triggered
    Then Hugo sends his visits
    And Hugo has 1 rejected visit
    Then exposure status should reports Laure as being at risk of 2.0

  Scenario: Malformed pivot date (not in timestamp)
    Given Yaël recorded a visit to "McDonald's" at 13:45, 4 days ago
    Given Julie recorded a visit to "McDonald's" at 13:40, 4 days ago
    When Yaël declares himself sick with malformed pivot date
    When cluster detection triggered
    Then Yaël cannot send his visits
    Then exposure status should reports Julie as not being at risk

  Scenario: Malformed deepLink
    Given Yaël recorded a visit to "McDonald's" at 13:45, 4 days ago
    Given Julie recorded a visit to "McDonald's" at 13:40, 4 days ago
    When Yaël declares himself sick with malformed deepLink
    When cluster detection triggered
    Then Yaël cannot send his visits
    Then exposure status should reports Julie as not being at risk

  Scenario: No deepLink
    Given Julie recorded a visit to "McDonald's" at 13:45, 4 days ago
    When Yaël declares himself sick with a 5 days ago pivot date with no deepLink
    When cluster detection triggered
    Then Yaël cannot send his visits
    Then exposure status should reports Julie as not being at risk

  Scenario: Malformed scan time
    Given Yaël recorded a visit to "McDonald's" at 13:45, 4 days ago
    Given Julie recorded a visit to "McDonald's" at 13:40, 4 days ago
    When Yaël declares himself sick with malformed scan time
    When cluster detection triggered
    Then Yaël has 1 rejected visit
    Then exposure status should reports Julie as not being at risk

  Scenario: No scan time
    Given Yaël recorded a visit to "McDonald's" at 13:45, 4 days ago
    Given Julie recorded a visit to "McDonald's" at 13:40, 4 days ago
    When Yaël declares himself sick with no scan time
    When cluster detection triggered
    Then Yaël cannot send his visits
    Then exposure status should reports Julie as not being at risk

  Scenario: Pivot date in the past
    Given Yaël recorded a visit to "McDonald's" at 13:45, 4 days ago
    Given Julie recorded a visit to "McDonald's" at 13:40, 4 days ago
    When Yaël declares himself sick with a 18 days ago pivot date
    When cluster detection triggered
    Then Yaël sends his visits
    Then exposure status should reports Julie as being at risk of 2

  Scenario: Pivot date in the future
    Given Yaël recorded a visit to "McDonald's" at 13:45, 4 days ago
    Given Julie recorded a visit to "McDonald's" at 13:40, 4 days ago
    When Yaël declares himself sick with a in 3 days pivot date
    When cluster detection triggered
    Then Yaël sends his visits
    Then exposure status should reports Julie as being at risk of 2

    #FIXME
  Scenario: Duplicated deepLink STAFF and NO STAFF
    Given Hugo recorded a visit as a STAFF to "McDonald's" at 12:30, 6 days ago
    Given Hugo recorded a visit to "McDonald's" at 12:35, 6 days ago
    Given Laure recorded a visit to "McDonald's" at 12:56, 6 days ago
    When Hugo declares himself sick with a 14 days ago pivot date
    When cluster detection triggered
    Then Hugo sends his visits
    #rejected visit should be the "not staff" deepLink
    And Hugo has 1 rejected visit
    Then exposure status should reports Laure as being at risk of 2.0

#FIXME: step "with malformed token" not yet implemented
#     Scenario: ERROR - A DEVELOPPER malformed Robert Token (validity period passed)
#     Given Yaël recorded a visit to "McDonald's" at 13:45, 10 days ago
#     Given Julie recorded a visit to "McDonald's" at 13:45, 4 days ago
#     When Yaël declares himself sick with malformed token
#     When cluster detection triggered
#     Then Yaël cannot send his visits
#     Then exposure status should reports Julie as not being at risk
