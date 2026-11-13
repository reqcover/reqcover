Feature: Coverage

  @req:#reqOne
  Scenario: a passing scenario
    Then it passes

  @req:#reqTwo
  Scenario: a failing scenario
    Then it fails
