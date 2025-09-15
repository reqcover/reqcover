package dev.reqcover.reporter.html

import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.spi.RequirementsCoverageReporter
import org.w3c.dom.Element
import org.w3c.dom.Text
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

class HtmlRequirementsCoverageReporter : RequirementsCoverageReporter {
    companion object {
        const val XMLNS_XHTML = "http://www.w3.org/1999/xhtml"
    }

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
        val document = domImplementationLS.createDocument(XMLNS_XHTML, "html", domImplementationLS.createDocumentType("html", null, null))
        val rootElement = document.documentElement
        val headElement = rootElement.appendElement("head")
        headElement.appendElementWithText("title", "Requirements Coverage Report")
        val bodyElement = rootElement.appendElement("body")
        bodyElement.appendElementWithText("h1", "Requirements Coverage Report")

        val tableElement = bodyElement.appendElement("table")
        val theadElement = tableElement.appendElement("thead")
        val headerRowElement = theadElement.appendElement("tr")
        headerRowElement.appendElementWithText("th", "Requirement ID")
        headerRowElement.appendElementWithText("th", "Test")

        val tbodyElement = tableElement.appendElement("tbody")
        for (requirementId in tracker.expectedRequirements()) {
            val rowElement = tbodyElement.appendElement("tr")
            rowElement.appendElementWithText("td", requirementId)
            val tdTest = rowElement.appendElement("td")
            val testText = if (tracker.verifiedRequirements().contains(requirementId)) {
                "Covered"
            } else {
                "- (Not Covered)"
            }
            tdTest.appendText(testText)
        }

        for (requirementId in tracker.unexpectedRequirements()) {
            val rowElement = tbodyElement.appendElement("tr")
            rowElement.appendElementWithText("td", requirementId)
            rowElement.appendElementWithText("td", "Unexpected")
        }

        val serializer = domImplementationLS.createLSSerializer()
        val output = domImplementationLS.createLSOutput()
        output.byteStream = outputStream
        return serializer.write(document, output)
    }

    fun Element.appendElement(tagName: String): Element {
        val newChild = ownerDocument.createElementNS(XMLNS_XHTML, tagName)
        appendChild(newChild)
        return newChild
    }

    fun Element.appendText(text: String): Text {
        val textNode = ownerDocument.createTextNode(text)
        appendChild(textNode)
        return textNode
    }

    fun Element.appendElementWithText(tagName: String, text: String): Element {
        val newChild = ownerDocument.createElementNS(XMLNS_XHTML, tagName)
        newChild.appendChild(ownerDocument.createTextNode(text))
        appendChild(newChild)
        return newChild
    }
}
