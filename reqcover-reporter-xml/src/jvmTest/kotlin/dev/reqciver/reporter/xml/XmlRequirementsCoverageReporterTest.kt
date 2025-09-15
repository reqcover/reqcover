package dev.reqciver.reporter.xml

import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.reporter.xml.XmlRequirementsCoverageReporter
import org.junit.jupiter.api.Test
import org.xmlunit.assertj.XmlAssert.assertThat
import java.io.ByteArrayOutputStream

class XmlRequirementsCoverageReporterTest {
    private val reporter = XmlRequirementsCoverageReporter()
    private val tracker = RequirementsCoverageTracker()

    @Test
    fun emptyReport() {
        val expected = """
            <requirements-coverage total="0" covered="0" coverage="100.0">
              <expected/>
              <unexpected/>
            </requirements-coverage>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }

    @Test
    fun singleUncoveredRequirement() {
        tracker.expect("REQ-1")
        val expected = """
            <requirements-coverage total="1" covered="0" coverage="0.0">
              <expected>
                <requirement id="REQ-1" covered="false"/>
              </expected>
              <unexpected/>
            </requirements-coverage>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }

    @Test
    fun singleCoveredRequirement() {
        tracker.expect("REQ-1")
        tracker.verified("REQ-1")
        val expected = """
            <requirements-coverage total="1" covered="1" coverage="100.0">
              <expected>
                <requirement id="REQ-1" covered="true"/>
              </expected>
              <unexpected/>
            </requirements-coverage>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }

    @Test
    fun singleUnexpectedRequirement() {
        tracker.verified("REQ-1")
        val expected = """
            <requirements-coverage total="0" covered="0" coverage="100.0">
              <expected/>
              <unexpected>
                <requirement id="REQ-1"/>
              </unexpected>
            </requirements-coverage>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }

    @Test
    fun twoExpectedOneCoveredOneUncoveredOneUnexpected() {
        tracker.expect("REQ-1")
        tracker.expect("REQ-2")
        tracker.verified("REQ-1")
        tracker.verified("REQ-3")
        val expected = """
            <requirements-coverage total="2" covered="1" coverage="50.0">
              <expected>
                <requirement id="REQ-1" covered="true"/>
                <requirement id="REQ-2" covered="false"/>
              </expected>
              <unexpected>
                <requirement id="REQ-3"/>
              </unexpected>
            </requirements-coverage>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }
}
