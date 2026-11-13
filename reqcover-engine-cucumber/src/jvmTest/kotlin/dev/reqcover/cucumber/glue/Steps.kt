package dev.reqcover.cucumber.glue

import io.cucumber.java.en.Then

class Steps {
    @Then("it passes")
    fun itPasses() {
    }

    @Then("it fails")
    fun itFails() {
        throw AssertionError("boom")
    }
}
