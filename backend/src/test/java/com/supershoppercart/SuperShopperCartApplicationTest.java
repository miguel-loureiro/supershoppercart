package com.supershoppercart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperShopperCartApplication Unit Tests using System.out capture")
class SuperShopperCartApplicationTest {

    @Mock
    private ConfigurableApplicationContext mockContext;

    @Mock
    private ConfigurableEnvironment mockEnvironment;

    private String captureSystemOut(Runnable action) {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));
        try {
            action.run();
        } finally {
            System.setOut(originalOut); // restore original output
        }
        return outContent.toString().trim();
    }

    @Test
    @DisplayName("Should print active profiles when single profile is active")
    void main_singleActiveProfile() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = mockStatic(SpringApplication.class)) {

            String[] args = {"--spring.profiles.active=dev-emulator"};
            String[] activeProfiles = {"dev-emulator"};

            mockedSpringApplication.when(() -> SpringApplication.run(SuperShopperCartApplication.class, args))
                    .thenReturn(mockContext);
            when(mockContext.getEnvironment()).thenReturn(mockEnvironment);
            when(mockEnvironment.getActiveProfiles()).thenReturn(activeProfiles);

            String output = captureSystemOut(() -> SuperShopperCartApplication.main(args));

            mockedSpringApplication.verify(() -> SpringApplication.run(SuperShopperCartApplication.class, args));
            verify(mockContext).getEnvironment();
            verify(mockEnvironment).getActiveProfiles();
            assertTrue(output.contains("Active Spring Profiles: [dev-emulator]"));
        }
    }

    @Test
    @DisplayName("Should print multiple active profiles separated by commas")
    void main_multipleActiveProfiles() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = mockStatic(SpringApplication.class)) {

            String[] args = {"--spring.profiles.active=dev-emulator,debug"};
            String[] activeProfiles = {"dev-emulator", "debug"};

            mockedSpringApplication.when(() -> SpringApplication.run(SuperShopperCartApplication.class, args))
                    .thenReturn(mockContext);
            when(mockContext.getEnvironment()).thenReturn(mockEnvironment);
            when(mockEnvironment.getActiveProfiles()).thenReturn(activeProfiles);

            String output = captureSystemOut(() -> SuperShopperCartApplication.main(args));

            mockedSpringApplication.verify(() -> SpringApplication.run(SuperShopperCartApplication.class, args));
            verify(mockContext).getEnvironment();
            verify(mockEnvironment).getActiveProfiles();
            assertTrue(output.contains("Active Spring Profiles: [dev-emulator, debug]"));
        }
    }

    @Test
    @DisplayName("Should handle empty active profiles array")
    void main_noActiveProfiles() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = mockStatic(SpringApplication.class)) {

            String[] args = {};
            String[] activeProfiles = {};

            mockedSpringApplication.when(() -> SpringApplication.run(SuperShopperCartApplication.class, args))
                    .thenReturn(mockContext);
            when(mockContext.getEnvironment()).thenReturn(mockEnvironment);
            when(mockEnvironment.getActiveProfiles()).thenReturn(activeProfiles);

            String output = captureSystemOut(() -> SuperShopperCartApplication.main(args));

            mockedSpringApplication.verify(() -> SpringApplication.run(SuperShopperCartApplication.class, args));
            verify(mockContext).getEnvironment();
            verify(mockEnvironment).getActiveProfiles();
            assertTrue(output.contains("Active Spring Profiles: []"));
        }
    }

    @Test
    @DisplayName("Should handle null arguments array")
    void main_nullArgs() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = mockStatic(SpringApplication.class)) {

            String[] args = null;
            String[] activeProfiles = {"default"};

            mockedSpringApplication.when(() -> SpringApplication.run(eq(SuperShopperCartApplication.class), isNull()))
                    .thenReturn(mockContext);
            when(mockContext.getEnvironment()).thenReturn(mockEnvironment);
            when(mockEnvironment.getActiveProfiles()).thenReturn(activeProfiles);

            String output = captureSystemOut(() -> SuperShopperCartApplication.main(args));

            mockedSpringApplication.verify(() -> SpringApplication.run(eq(SuperShopperCartApplication.class), isNull()));
            verify(mockContext).getEnvironment();
            verify(mockEnvironment).getActiveProfiles();
            assertTrue(output.contains("Active Spring Profiles: [default]"));
        }
    }
}