@smoke
Feature: Smoke tests

  As a developer,
  I need to be sure all software components are ready,
  So I can run tests on a healthy platform

  Scenario: clea-ws-rest is ready
    Given application clea ws rest is ready
    Given Place named "McDonald's" has configuration: venue type 1, venue category 1 1, venue category 2 1
    Given "McDonald's" created a static deeplink at 11:00, 13 days ago
    And Hugo registered on TAC
    And Hugo recorded a visit to "McDonald's" at 12:30, 6 days ago
    When Hugo declares himself sick with a 14 days ago pivot date
    Then Hugo sends his visits

  Scenario: s3 bucket is ready
    Given Hugo registered on TAC
    When Cluster detection triggered
    Then Exposure status should reports Hugo as not being at risk

  Scenario: elasticsearch is ready
    Then elasticsearch is ready
