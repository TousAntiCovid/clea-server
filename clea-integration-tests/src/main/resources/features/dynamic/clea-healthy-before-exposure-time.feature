Feature: One healthy and one sick visitors visit the same location
  the healthy visitor visit the location after the exposure time
  The healthy visitor must not be warned being at risk

  Background:
    Given "Hugo" registered on TAC
    Given "Heather" registered on TAC
    Given VType of 1, VCategory1 of 1 and VCategory2 of 1
    Given "Chez Gusto" created a dynamic QRCode at 04:00, 10 days ago with VType as 1, with VCategory1 as 1, with VCategory2 as 1, with a renewal time of "15 minutes" and with a periodDuration of "24 hours"

  Scenario: One person visiting the same location as one sick, before exposure time
    Given "Heather" recorded a visit to "Chez Gusto" at 15:00, 4 days ago
    Given "Hugo" recorded a visit to "Chez Gusto" at 8:00, 4 days ago

    When "Heather" declares himself sick
    When Cluster detection triggered

    Then "Heather" sends his visits
    Then Exposure status should reports "Hugo" as not being at risk