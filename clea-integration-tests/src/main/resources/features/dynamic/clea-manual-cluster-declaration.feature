Feature: Manual cluster declaration

  As manual contact tracing authority,
  I want to report a location to be a cluster at some time,
  In order to warn people they may be at risk.

  Scenario: Manual cluster declaration for dynamic qrcode
    Given "Chez Gusto" manager configured qrcode generators at 04:00, 10 days ago with venue type 1, venue category 1 1, venue category 2 1, deepLink renewal duration of 15 minutes, and a periodDuration of 24 hours
    And Hugo registered on TAC
    And Hugo recorded a visit to "Chez Gusto" at 13:00, 2 days ago
    When a manual cluster report is made for "Chez Gusto" at 12:00, 2 days ago
    And cluster detection triggered
    Then exposure status should reports Hugo as being at risk of 2.0