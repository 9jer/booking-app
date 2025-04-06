package com.example.propertyservice;

import com.example.propertyservice.services.PropertyServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PropertyServiceIntegrationTest {

    @Autowired
    private PropertyServiceImpl propertyService;

    @Test
    void testSaveAndRetrieveProperty() {

    }
}
