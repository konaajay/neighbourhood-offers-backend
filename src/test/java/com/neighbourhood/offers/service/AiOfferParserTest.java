package com.neighbourhood.offers.service;

import com.neighbourhood.offers.dto.AiParseRequest;
import com.neighbourhood.offers.dto.AiParseResponseDto;
import com.neighbourhood.offers.exception.InvalidAiParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class AiOfferParserTest {

    @Autowired
    private AiOfferParserService aiOfferParserService;

    @Test
    @DisplayName("AI Parser correctly extracts percentage, category, min bill, and festival dates")
    void testExtractSareeOffer() {
        AiParseRequest request = AiParseRequest.builder()
                .text("flat 20% off all sarees till diwali, min bill 1500")
                .build();

        AiParseResponseDto response = aiOfferParserService.parseOffer(request);

        assertNotNull(response);
        assertEquals("PERCENTAGE", response.getDiscountType());
        assertEquals(new BigDecimal("20"), response.getDiscountValue());
        assertEquals(new BigDecimal("1500"), response.getMinBillAmount());
        assertTrue(response.getApplicableCategory().contains("Sarees") || response.getApplicableCategory().contains("Apparel"));
        assertNotNull(response.getTitle());
        assertNotNull(response.getEndDate());
        assertFalse(response.getTermsAndConditions().isEmpty());
    }

    @Test
    @DisplayName("AI Parser correctly extracts flat amount discount and groceries category")
    void testExtractFlatAmountOffer() {
        AiParseRequest request = AiParseRequest.builder()
                .text("save rs 200 on grocery purchase above 1000 till sunday")
                .build();

        AiParseResponseDto response = aiOfferParserService.parseOffer(request);

        assertNotNull(response);
        assertEquals("FLAT_AMOUNT", response.getDiscountType());
        assertEquals(new BigDecimal("200"), response.getDiscountValue());
        assertEquals(new BigDecimal("1000"), response.getMinBillAmount());
        assertTrue(response.getApplicableCategory().contains("Groceries"));
    }

    @Test
    @DisplayName("AI Parser correctly extracts offer from uploaded saree chalkboard image")
    void testExtractImageOffer_SareeChalkboard() throws Exception {
        Path sampleImgPath = Paths.get("d:/Neighbourhood Offers/sample image for extract test.png");
        if (!Files.exists(sampleImgPath)) {
            sampleImgPath = Paths.get("../sample image for extract test.png");
        }

        if (Files.exists(sampleImgPath)) {
            byte[] imgBytes = Files.readAllBytes(sampleImgPath);
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes);

            AiParseRequest request = AiParseRequest.builder()
                    .imageBase64(base64)
                    .build();

            AiParseResponseDto response = aiOfferParserService.parseOffer(request);

            assertNotNull(response);
            assertEquals("PERCENTAGE", response.getDiscountType());
            assertEquals(new BigDecimal("20"), response.getDiscountValue());
            assertEquals(new BigDecimal("1500"), response.getMinBillAmount());
            assertTrue(response.getApplicableCategory().contains("Sarees") || response.getApplicableCategory().contains("Apparel"));
            assertTrue(response.getTitle().contains("20%") || response.getTitle().contains("Sarees") || response.getTitle().contains("Festive"));
        }
    }

    @Test
    @DisplayName("AI Parser throws user-friendly error when image has no readable discount")
    void testExtractImageOffer_UnreadableImageThrows() {
        // 1x1 transparent PNG Base64
        String emptyPngBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
        AiParseRequest request = AiParseRequest.builder()
                .imageBase64(emptyPngBase64)
                .build();

        InvalidAiParseException ex = assertThrows(InvalidAiParseException.class, () -> {
            aiOfferParserService.parseOffer(request);
        });

        assertEquals("Could not read the offer image. Please try another image or enter the offer details manually.", ex.getMessage());
    }

    @Test
    @DisplayName("AI Parser throws validation error when input is empty and no image provided")
    void testEmptyInputThrows() {
        AiParseRequest request = AiParseRequest.builder()
                .text("")
                .build();

        InvalidAiParseException ex = assertThrows(InvalidAiParseException.class, () -> {
            aiOfferParserService.parseOffer(request);
        });

        assertEquals("Please enter offer details or upload an offer image.", ex.getMessage());
    }
}
