Feature: Manual cluster declaration

  As manual contact tracing authority,
  I want to report a location to be a cluster at some time,
  In order to warn people they may be at risk.

  Scenario: Manual cluster declaration for static qrcode
    Given "Chez McDonald's" manager generated qrcodes at 11:00, 18 days ago has configuration: venue type 1, venue category 1 1, venue category 2 1
    And Hugo registered on TAC
    And Hugo recorded a visit to "Chez McDonald's" at 13:00, 2 days ago
    When a manual cluster report is made for "Chez McDonald's" at 12:00, 2 days ago
    And cluster detection triggered
    Then exposure status should reports Hugo as being at risk of 2.0
