package example;

import dev.reqcover.junit.jupiter.RequirementsCoverageListener;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

public class LegacyConfigMain {
    public static void main(String[] args) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass(LegacyRequirementsPropertyTest.class))
            .configurationParameter("reqCover.requirementsUri", "requirements-a.yaml")
            .build();
        LauncherFactory.create().execute(request, new RequirementsCoverageListener());
    }
}
