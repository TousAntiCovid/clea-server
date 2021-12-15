Feature: Several healthy visitors visit different places
  Visits are simultaneous or not
  The healthy visitors must not be warned being at risk

  Background:
    Given users Hugo, Heather, Henry are registered on TAC
    Given "Chez Gusto" manager configured qrcode generators at 04:00, 10 days ago with venue type 1, venue category 1 1, venue category 2 1, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours
    Given "La fontaine aux perles" manager configured qrcode generators at 04:00, 10 days ago with venue type 1, venue category 1 1, venue category 2 1, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours

  Scenario: One healthy visitor alone
    Given Hugo recorded a visit to "Chez Gusto" at 12:30, 2 days ago
    When cluster detection triggered
    Then exposure status should reports Hugo as not being at risk

  Scenario: One healthy visitor alone
    Given Heather recorded a visit to "Chez Gusto" at 12:00, 2 days ago
    When cluster detection triggered
    Then exposure status should reports Heather as not being at risk

  Scenario: two simultaneous healthy visitors (same location)
    Given Hugo recorded a visit to "Chez Gusto" at 12:30, 2 days ago
    Given Henry recorded a visit to "Chez Gusto" at 12:30, 2 days ago
    When cluster detection triggered
    Then exposure status should reports Hugo as not being at risk
    Then exposure status should reports Henry as not being at risk

  Scenario: two simultaneous healthy visitors (different location)
    Given Heather recorded a visit to "La fontaine aux perles" at 12:30, 2 days ago
    Given Hugo recorded a visit to "Chez Gusto" at 12:30, 2 days ago
    When cluster detection triggered
    Then exposure status should reports Heather as not being at risk
    Then exposure status should reports Hugo as not being at risk

  Scenario: two overlapping healthy visitors (within the same hour)
    Given Heather recorded a visit to "La fontaine aux perles" at 12:30, 2 days ago
    Given Hugo recorded a visit to "Chez Gusto" at 11:45, 2 days ago
    When cluster detection triggered
    Then exposure status should reports Heather as not being at risk
    Then exposure status should reports Hugo as not being at risk

  Scenario: Multiple scans of the deepLink by same visitor within the dupScanThreshold of 3 hours
    Given Heather recorded a visit to "La fontaine aux perles" at 12:30, 2 days ago
    Given Heather recorded a visit to "La fontaine aux perles" at 12:47, 2 days ago
    When Heather declares himself sick
    When cluster detection triggered
    Then Heather has 1 rejected visit

  Scenario: Multiple scans of the deepLink by same visitor outside of the dupScanThreshold of 3 hours
    Given Heather recorded a visit to "La fontaine aux perles" at 12:30, 2 days ago
    Given Heather recorded a visit to "La fontaine aux perles" at 19:47, 2 days ago
    When Heather declares himself sick
    When cluster detection triggered
    Then Heather has 0 rejected visit
