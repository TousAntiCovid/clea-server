Feature: Manual cluster declaration

  As manual contact tracing authority,
  I want to report a location to be a cluster at some time,
  In order to warn people they may be at risk.

  Scenario: Manual cluster declaration for static qrcode
    Given "Chez McDonald's" created a static QRCode at 11:00, 18 days ago with VType as 1, with VCategory1 as 1, with VCategory2 as 1 and with a periodDuration of 24 hours
    And Hugo registered on TAC
    And Hugo recorded a visit to "Chez McDonald's" at 13:00, 2 days ago
    When a manual cluster report is made for "Chez McDonald's" at 12:00, 2 days ago
    And Cluster detection triggered
    Then Exposure status should reports Hugo as being at risk of 2.0

  Scenario: Manual cluster declaration for dynamic qrcode
    Given "Chez Gusto" created a dynamic QRCode at 04:00, 10 days ago with VType as 1, with VCategory1 as 1, with VCategory2 as 1, with a renewal time of 15 minutes and with a periodDuration of 24 hours
    And Hugo registered on TAC
    And Hugo recorded a visit to "Chez Gusto" at 13:00, 2 days ago
    When a manual cluster report is made for "Chez Gusto" at 12:00, 2 days ago
    And Cluster detection triggered
    Then Exposure status should reports Hugo as being at risk of 2.0