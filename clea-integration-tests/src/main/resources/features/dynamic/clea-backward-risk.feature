Feature: One Healthy visitor meets two sick visitor
  Visits are simultaneous
  The healthy visitor must be warned being at risk

  Background:
    Given Hugo registered on TAC
    Given Heather registered on TAC
    Given Henry registered on TAC
    Given Laure registered on TAC
    Given Place named "Chez Gusto" with venue type 1, venue category 1 1, venue category 2 1, qr code renewal duration of 15 minutes, and a periodDuration of 24 hours
    Given "Chez Gusto" initialized dynamic deeplink at 04:00, 10 days ago

  Scenario: One sick and two persons at risk (same location and high RiskLevel)
    Given Heather recorded a visit to "Chez Gusto" at 14:00, 4 days ago
    Given Laure recorded a visit to "Chez Gusto" at 14:00, 4 days ago
    Given Henry recorded a visit to "Chez Gusto" at 14:30, 4 days ago
    Given Hugo recorded a visit to "Chez Gusto" at 15:00, 4 days ago

    When Heather declares herself sick with a 2 days ago pivot date
    When Henry declares himself sick with a 2 days ago pivot date
    When Laure declares herself sick with a 2 days ago pivot date
    When Cluster detection triggered

    Then Heather sends her visits
    Then Henry sends his visits
    Then Laure sends her visits
    Then Exposure status should reports Hugo as being at risk of 3.0
