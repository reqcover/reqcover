package dev.reqcover.reporter.xml

import dev.reqcover.engine.RequirementsCoverageTracker
import dev.reqcover.engine.spi.RequirementsCoverageReporter
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

class XmlRequirementsCoverageReporter : RequirementsCoverageReporter {
    override fun report(tracker: RequirementsCoverageTracker) {
        val path = Path.of(System.getProperty("user.dir"), "build", "reports", "tests", "reqcover", "requirements-coverage.xml")
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
        val document = domImplementationLS.createDocument(null, "requirements-coverage", null)
        val rootElement = document.documentElement
        rootElement.setAttribute("total", tracker.expectedRequirements().size.toString())
        rootElement.setAttribute("covered", tracker.coveredRequirements().size.toString())
        rootElement.setAttribute(
            "coverage",
            if (tracker.expectedRequirements().isEmpty()) {
                "100.0"
            } else {
                "%.1f".format((tracker.coveredRequirements().size.toDouble() / tracker.expectedRequirements().size) * 100)
            }
        )

        val expectedElement = document.createElement("expected")
        for (requirementId in tracker.expectedRequirements()) {
            val requirementElement = document.createElement("requirement")
            requirementElement.setAttribute("id", requirementId)
            requirementElement.setAttribute(
                "covered",
                tracker.verifiedRequirements().contains(requirementId).toString()
            )
            expectedElement.appendChild(requirementElement)
        }
        rootElement.appendChild(expectedElement)

        val unexpectedElement = document.createElement("unexpected")
        for (requirementId in tracker.unexpectedRequirements()) {
            val requirementElement = document.createElement("requirement")
            requirementElement.setAttribute("id", requirementId)
            unexpectedElement.appendChild(requirementElement)
        }
        rootElement.appendChild(unexpectedElement)

        val serializer = domImplementationLS.createLSSerializer()
        val output = domImplementationLS.createLSOutput()
        output.byteStream = outputStream
        return serializer.write(document, output)
    }
}
