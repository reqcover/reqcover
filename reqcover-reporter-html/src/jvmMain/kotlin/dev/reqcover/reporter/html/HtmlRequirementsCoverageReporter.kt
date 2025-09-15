package dev.reqcover.reporter.html

import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.spi.RequirementsCoverageReporter
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

class HtmlRequirementsCoverageReporter : RequirementsCoverageReporter {
    override fun report(tracker: RequirementsCoverageTracker) {
        val path = Path.of(System.getProperty("user.dir"), "build", "reports", "tests", "reqcover", "requirements-coverage.html")
        val parentDir = path.parent
        if (parentDir != null && !parentDir.toFile().exists())
            Files.createDirectories(parentDir)
        Files.newOutputStream(path).use { outputStream ->
            report(tracker, outputStream)
        }
    }

    fun report(
        tracker: RequirementsCoverageTracker,
        outputStream: OutputStream,
    ): Boolean {
        val domImplementationLS = DOMImplementationRegistry.newInstance().getDOMImplementation("LS")
        if (domImplementationLS !is DOMImplementationLS) throw IllegalStateException("Could not get DOMImplementationLS")
        val document = domImplementationLS.createDocument("http://www.w3.org/1999/xhtml", "html", domImplementationLS.createDocumentType("html", null, null))
        val rootElement = document.documentElement
        val headElement = document.createElementNS("http://www.w3.org/1999/xhtml", "head")
        rootElement.appendChild(headElement)
        val titleElement = document.createElementNS("http://www.w3.org/1999/xhtml", "title")
        headElement.appendChild(titleElement)
        val titleText = document.createTextNode("Requirements Coverage Report")
        titleElement.appendChild(titleText)
        val bodyElement = document.createElementNS("http://www.w3.org/1999/xhtml", "body")
        rootElement.appendChild(bodyElement)
        val h1Element = document.createElementNS("http://www.w3.org/1999/xhtml", "h1")
        bodyElement.appendChild(h1Element)
        val h1Text = document.createTextNode("Requirements Coverage Report")
        h1Element.appendChild(h1Text)

        val tableElement = document.createElementNS("http://www.w3.org/1999/xhtml", "table")
        bodyElement.appendChild(tableElement)
        val theadElement = document.createElementNS("http://www.w3.org/1999/xhtml", "thead")
        tableElement.appendChild(theadElement)
        val headerRowElement = document.createElementNS("http://www.w3.org/1999/xhtml", "tr")
        theadElement.appendChild(headerRowElement)
        val thRequirementId = document.createElementNS("http://www.w3.org/1999/xhtml", "th")
        headerRowElement.appendChild(thRequirementId)
        val thRequirementIdText = document.createTextNode("Requirement ID")
        thRequirementId.appendChild(thRequirementIdText)
        val thTest = document.createElementNS("http://www.w3.org/1999/xhtml", "th")
        headerRowElement.appendChild(thTest)
        val thTestText = document.createTextNode("Test")
        thTest.appendChild(thTestText)

        val tbodyElement = document.createElementNS("http://www.w3.org/1999/xhtml", "tbody")
        tableElement.appendChild(tbodyElement)
        for (requirementId in tracker.expectedRequirements()) {
            val rowElement = document.createElementNS("http://www.w3.org/1999/xhtml", "tr")
            tbodyElement.appendChild(rowElement)
            val tdRequirementId = document.createElementNS("http://www.w3.org/1999/xhtml", "td")
            rowElement.appendChild(tdRequirementId)
            val tdRequirementIdText = document.createTextNode(requirementId)
            tdRequirementId.appendChild(tdRequirementIdText)
            val tdTest = document.createElementNS("http://www.w3.org/1999/xhtml", "td")
            rowElement.appendChild(tdTest)
            val testText = if (tracker.verifiedRequirements().contains(requirementId)) {
                "Covered"
            } else {
                "- (Not Covered)"
            }
            val tdTestText = document.createTextNode(testText)
            tdTest.appendChild(tdTestText)
        }

        for (requirementId in tracker.unexpectedRequirements()) {
            val rowElement = document.createElementNS("http://www.w3.org/1999/xhtml", "tr")
            tbodyElement.appendChild(rowElement)
            val tdRequirementId = document.createElementNS("http://www.w3.org/1999/xhtml", "td")
            rowElement.appendChild(tdRequirementId)
            val tdRequirementIdText = document.createTextNode(requirementId)
            tdRequirementId.appendChild(tdRequirementIdText)
            val tdTest = document.createElementNS("http://www.w3.org/1999/xhtml", "td")
            rowElement.appendChild(tdTest)
            val tdTestText = document.createTextNode("Unexpected")
            tdTest.appendChild(tdTestText)
        }

        val serializer = domImplementationLS.createLSSerializer()
        val output = domImplementationLS.createLSOutput()
        output.byteStream = outputStream
        return serializer.write(document, output)
    }
}
