package example;

import dev.reqcover.api.ForRequirement;
import org.junit.jupiter.api.Test;

class LegacyRequirementsPropertyTest {
    @Test
    @ForRequirement(id = "#reqOne")
    void verifiesRequirement() {
    }
}
