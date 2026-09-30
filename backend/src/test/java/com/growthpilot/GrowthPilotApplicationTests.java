package com.growthpilot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = GrowthPilotApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class GrowthPilotApplicationTests {

    @Test
    void contextLoads() {
    }

}
