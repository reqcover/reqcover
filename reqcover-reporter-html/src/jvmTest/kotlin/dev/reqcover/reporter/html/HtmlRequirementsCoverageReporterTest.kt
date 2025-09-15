package dev.reqcover.reporter.html

import dev.reqcover.engine.RequirementsCoverageTracker
import org.junit.jupiter.api.Test
import org.xmlunit.assertj.XmlAssert.assertThat
import java.io.ByteArrayOutputStream

class HtmlRequirementsCoverageReporterTest {
    private val reporter = HtmlRequirementsCoverageReporter()
    private val tracker = RequirementsCoverageTracker()

    @Test
    fun emptyReport() {
        val expected = """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Requirements Coverage Report</title></head>
            <body>
            <h1>Requirements Coverage Report</h1>
            <table>
            <thead><tr><th>Requirement ID</th><th>Test</th></tr></thead>
            <tbody>
            </tbody>
            </table>
            </body>
            </html>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }

    @Test
    fun singleUncoveredRequirement() {
        tracker.expect("REQ-1")
        val expected = """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Requirements Coverage Report</title></head>
            <body>
            <h1>Requirements Coverage Report</h1>
            <table>
            <thead><tr><th>Requirement ID</th><th>Test</th></tr></thead>
            <tbody>
            <tr><td>REQ-1</td><td>- (Not Covered)</td></tr>
            </tbody>
            </table>
            </body>
            </html>
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
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Requirements Coverage Report</title></head>
            <body>
            <h1>Requirements Coverage Report</h1>
            <table>
            <thead><tr><th>Requirement ID</th><th>Test</th></tr></thead>
            <tbody>
            <tr><td>REQ-1</td><td>Covered</td></tr>
            </tbody>
            </table>
            </body>
            </html>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }

    @Test
    fun singleUnexpectedRequirement() {
        tracker.verified("REQ-1")
        val expected = """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Requirements Coverage Report</title></head>
            <body>
            <h1>Requirements Coverage Report</h1>
            <table>
            <thead><tr><th>Requirement ID</th><th>Test</th></tr></thead>
            <tbody>
            <tr><td>REQ-1</td><td>Unexpected</td></tr>
            </tbody>
            </table>
            </body>
            </html>
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
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Requirements Coverage Report</title></head>
            <body>
            <h1>Requirements Coverage Report</h1>
            <table>
            <thead><tr><th>Requirement ID</th><th>Test</th></tr></thead>
            <tbody>
            <tr><td>REQ-1</td><td>Covered</td></tr>
            <tr><td>REQ-2</td><td>- (Not Covered)</td></tr>
            <tr><td>REQ-3</td><td>Unexpected</td></tr>
            </tbody>
            </table>
            </body>
            </html>
        """.trimIndent()
        val out = ByteArrayOutputStream()
        reporter.report(tracker, out)
        assertThat(out.toString()).and(expected).ignoreWhitespace().areIdentical()
    }
}
