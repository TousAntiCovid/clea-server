@smoke
Feature: Smoke tests

  As a developer,
  I need to be sure all software components are ready,
  So I can run tests on a healthy platform

  Scenario: clea-ws-rest is ready
    Given "Chez McDonald's" created a static QRCode at 11:00, 13 days ago with VType as 1, with VCategory1 as 1, with VCategory2 as 1 and with a periodDuration of 24 hours
    And Hugo registered on TAC
    And Hugo recorded a visit to "Chez McDonald's" at 12:30, 6 days ago
    When Hugo declares himself sick with a 14 days ago pivot date
    Then Hugo sends his visits

  Scenario: s3 bucket is ready
    Given Hugo registered on TAC
    When Cluster detection triggered
    Then Exposure status should reports Hugo as not being at risk

  Scenario: elasticsearch is ready
    Then elasticsearch is ready
