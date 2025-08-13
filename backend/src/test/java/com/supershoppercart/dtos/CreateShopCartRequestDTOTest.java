package com.supershoppercart.dtos;

import com.supershoppercart.models.GroceryItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateShopCartRequestDTO Tests")
class CreateShopCartRequestDTOTest {

    private CreateShopCartRequestDTO dto;
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        dto = new CreateShopCartRequestDTO();
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid data")
        void shouldPassValidationWithValidData() {
            // Given
            dto.setName("Weekly Shopping");
            dto.setDateKey("2024-01-15");

            // When
            Set<ConstraintViolation<CreateShopCartRequestDTO>> violations = validator.validate(dto);

            // Then
            assertTrue(violations.isEmpty());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should fail validation when name is null, empty or blank")
        void shouldFailValidationWhenNameIsInvalid(String invalidName) {
            // Given
            dto.setName(invalidName);
            dto.setDateKey("2024-01-15");

            // When
            Set<ConstraintViolation<CreateShopCartRequestDTO>> violations = validator.validate(dto);

            // Then
            assertEquals(1, violations.size());
            ConstraintViolation<CreateShopCartRequestDTO> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should fail validation when dateKey is null, empty or blank")
        void shouldFailValidationWhenDateKeyIsInvalid(String invalidDateKey) {
            // Given
            dto.setName("Weekly Shopping");
            dto.setDateKey(invalidDateKey);

            // When
            Set<ConstraintViolation<CreateShopCartRequestDTO>> violations = validator.validate(dto);

            // Then
            assertEquals(1, violations.size());
            ConstraintViolation<CreateShopCartRequestDTO> violation = violations.iterator().next();
            assertEquals("dateKey", violation.getPropertyPath().toString());
        }

        @Test
        @DisplayName("Should fail validation when both name and dateKey are invalid")
        void shouldFailValidationWhenBothRequiredFieldsAreInvalid() {
            // Given
            dto.setName("");
            dto.setDateKey(null);

            // When
            Set<ConstraintViolation<CreateShopCartRequestDTO>> violations = validator.validate(dto);

            // Then
            assertEquals(2, violations.size());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set name correctly")
        void shouldGetAndSetNameCorrectly() {
            // Given
            String expectedName = "Weekly Shopping List";

            // When
            dto.setName(expectedName);

            // Then
            assertEquals(expectedName, dto.getName());
        }

        @Test
        @DisplayName("Should get and set dateKey correctly")
        void shouldGetAndSetDateKeyCorrectly() {
            // Given
            String expectedDateKey = "2024-01-15";

            // When
            dto.setDateKey(expectedDateKey);

            // Then
            assertEquals(expectedDateKey, dto.getDateKey());
        }

        @Test
        @DisplayName("Should get and set items list correctly")
        void shouldGetAndSetItemsListCorrectly() {
            // Given
            List<GroceryItem> expectedItems = Arrays.asList(
                    new GroceryItem(),
                    new GroceryItem()
            );

            // When
            dto.setItems(expectedItems);

            // Then
            assertEquals(expectedItems, dto.getItems());
            assertEquals(2, dto.getItems().size());
        }

        @Test
        @DisplayName("Should get and set shopperIds list correctly")
        void shouldGetAndSetShopperIdsListCorrectly() {
            // Given
            List<String> expectedShopperIds = Arrays.asList("shopper1", "shopper2", "shopper3");

            // When
            dto.setShopperIds(expectedShopperIds);

            // Then
            assertEquals(expectedShopperIds, dto.getShopperIds());
            assertEquals(3, dto.getShopperIds().size());
        }

        @Test
        @DisplayName("Should get and set isPublic flag correctly")
        void shouldGetAndSetIsPublicFlagCorrectly() {
            // When/Then
            assertFalse(dto.isPublic()); // default value

            // When
            dto.setPublic(true);

            // Then
            assertTrue(dto.isPublic());
        }

        @Test
        @DisplayName("Should get and set isTemplate flag correctly")
        void shouldGetAndSetIsTemplateFlagCorrectly() {
            // When/Then
            assertFalse(dto.isTemplate()); // default value

            // When
            dto.setTemplate(true);

            // Then
            assertTrue(dto.isTemplate());
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            // Given - fresh instance from setUp()

            // Then
            assertNull(dto.getName());
            assertNull(dto.getDateKey());
            assertNotNull(dto.getItems());
            assertTrue(dto.getItems().isEmpty());
            assertNotNull(dto.getShopperIds());
            assertTrue(dto.getShopperIds().isEmpty());
            assertFalse(dto.isPublic());
            assertFalse(dto.isTemplate());
        }

        @Test
        @DisplayName("Should initialize lists as empty ArrayLists")
        void shouldInitializeListsAsEmptyArrayLists() {
            // Then
            assertInstanceOf(ArrayList.class, dto.getItems());
            assertInstanceOf(ArrayList.class, dto.getShopperIds());
            assertEquals(0, dto.getItems().size());
            assertEquals(0, dto.getShopperIds().size());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null lists gracefully")
        void shouldHandleNullListsGracefully() {
            // When
            dto.setItems(null);
            dto.setShopperIds(null);

            // Then
            assertNull(dto.getItems());
            assertNull(dto.getShopperIds());
        }

        @Test
        @DisplayName("Should handle empty lists")
        void shouldHandleEmptyLists() {
            // Given
            List<GroceryItem> emptyItems = new ArrayList<>();
            List<String> emptyShopperIds = new ArrayList<>();

            // When
            dto.setItems(emptyItems);
            dto.setShopperIds(emptyShopperIds);

            // Then
            assertTrue(dto.getItems().isEmpty());
            assertTrue(dto.getShopperIds().isEmpty());
        }

        @Test
        @DisplayName("Should handle large lists")
        void shouldHandleLargeLists() {
            // Given
            List<GroceryItem> largeItemsList = new ArrayList<>();
            List<String> largeShopperIdsList = new ArrayList<>();

            for (int i = 0; i < 1000; i++) {
                largeItemsList.add(new GroceryItem());
                largeShopperIdsList.add("shopper" + i);
            }

            // When
            dto.setItems(largeItemsList);
            dto.setShopperIds(largeShopperIdsList);

            // Then
            assertEquals(1000, dto.getItems().size());
            assertEquals(1000, dto.getShopperIds().size());
        }

        @Test
        @DisplayName("Should handle special characters in string fields")
        void shouldHandleSpecialCharactersInStringFields() {
            // Given
            String specialName = "Shopping List with émojis 🛒 and symbols @#$%";
            String specialDateKey = "2024-01-15T10:30:00+02:00";

            // When
            dto.setName(specialName);
            dto.setDateKey(specialDateKey);

            // Then
            assertEquals(specialName, dto.getName());
            assertEquals(specialDateKey, dto.getDateKey());
        }
    }

    @Nested
    @DisplayName("State Mutation Tests")
    class StateMutationTests {

        @Test
        @DisplayName("Should allow modification of items list after setting")
        void shouldAllowModificationOfItemsListAfterSetting() {
            // Given
            List<GroceryItem> items = new ArrayList<>();
            items.add(new GroceryItem());
            dto.setItems(items);

            // When
            dto.getItems().add(new GroceryItem());

            // Then
            assertEquals(2, dto.getItems().size());
        }

        @Test
        @DisplayName("Should allow modification of shopperIds list after setting")
        void shouldAllowModificationOfShopperIdsListAfterSetting() {
            // Given
            List<String> shopperIds = new ArrayList<>();
            shopperIds.add("shopper1");
            dto.setShopperIds(shopperIds);

            // When
            dto.getShopperIds().add("shopper2");

            // Then
            assertEquals(2, dto.getShopperIds().size());
            assertTrue(dto.getShopperIds().contains("shopper1"));
            assertTrue(dto.getShopperIds().contains("shopper2"));
        }

        @Test
        @DisplayName("Should allow toggling boolean flags multiple times")
        void shouldAllowTogglingBooleanFlagsMultipleTimes() {
            // Given
            assertFalse(dto.isPublic());
            assertFalse(dto.isTemplate());

            // When/Then
            dto.setPublic(true);
            assertTrue(dto.isPublic());

            dto.setTemplate(true);
            assertTrue(dto.isTemplate());

            dto.setPublic(false);
            assertFalse(dto.isPublic());
            assertTrue(dto.isTemplate()); // should remain unchanged

            dto.setTemplate(false);
            assertFalse(dto.isTemplate());
            assertFalse(dto.isPublic()); // should remain unchanged
        }
    }
}