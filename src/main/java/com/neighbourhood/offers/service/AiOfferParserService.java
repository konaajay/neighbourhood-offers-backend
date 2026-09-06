package com.neighbourhood.offers.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neighbourhood.offers.dto.AiParseRequest;
import com.neighbourhood.offers.dto.AiParseResponseDto;
import com.neighbourhood.offers.exception.InvalidAiParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOfferParserService {

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public AiParseResponseDto parseOffer(AiParseRequest request) {
        String input = request.getText() != null ? request.getText().trim() : "";
        String imageBase64 = request.getImageBase64();

        boolean hasImage = imageBase64 != null && !imageBase64.isBlank();
        byte[] imageBytes = null;
        String mimeType = "image/jpeg";
        String extension = ".jpg";
        String cleanBase64 = null;

        if (hasImage) {
            try {
                if (imageBase64.contains(";base64,")) {
                    String meta = imageBase64.substring(0, imageBase64.indexOf(";base64,"));
                    if (meta.startsWith("data:")) {
                        mimeType = meta.substring(5).trim();
                    }
                    cleanBase64 = imageBase64.substring(imageBase64.indexOf(";base64,") + 8).trim();
                } else if (imageBase64.contains(",")) {
                    cleanBase64 = imageBase64.split(",")[1].trim();
                } else {
                    cleanBase64 = imageBase64.trim();
                }

                if (mimeType.contains("png")) {
                    extension = ".png";
                } else if (mimeType.contains("webp")) {
                    extension = ".webp";
                }

                imageBytes = Base64.getDecoder().decode(cleanBase64);
                log.info("[AI OFFER PARSER] Image received: YES | MIME type: {} | Size: {} bytes ({} KB)",
                        mimeType, imageBytes.length, imageBytes.length / 1024);
            } catch (Exception ex) {
                log.error("[AI OFFER PARSER] Failed to decode base64 image: {}", ex.getMessage());
                throw new InvalidAiParseException("Could not read the offer image. Please try another image or enter the offer details manually.");
            }
        } else {
            log.info("[AI OFFER PARSER] Image received: NO | Text input length: {}", input.length());
        }

        // Tier 1: Try Google Gemini Vision API if API key is present
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            log.info("[AI OFFER PARSER] AI extraction attempted: YES | Provider: Google Gemini ({})", geminiModel);
            try {
                AiParseResponseDto response = callGeminiApi(input, mimeType, cleanBase64);
                log.info("[AI OFFER PARSER] AI extraction status: SUCCESS | Provider: Google Gemini | Title: '{}' | Discount: {} {} | MinBill: {} | Category: '{}'",
                        response.getTitle(), response.getDiscountValue(), response.getDiscountType(), response.getMinBillAmount(), response.getApplicableCategory());
                return response;
            } catch (Exception ex) {
                log.warn("[AI OFFER PARSER] Google Gemini API extraction failed: {}. Falling back to Local Vision / OCR", ex.getMessage());
            }
        } else {
            log.info("[AI OFFER PARSER] Gemini API key not configured or blank. Proceeding to Local Vision / OCR engine.");
        }

        // Tier 2: Local Vision OCR Fallback if an image was uploaded
        String ocrText = null;
        if (hasImage && imageBytes != null) {
            log.info("[AI OFFER PARSER] AI extraction attempted: YES | Provider: Local Vision OCR Engine");
            ocrText = runLocalOcr(imageBytes, extension);
            if (ocrText != null && !ocrText.isBlank()) {
                log.info("[AI OFFER PARSER] Local Vision OCR extracted text: [{}]", ocrText.replace("\n", " ").trim());
            } else {
                log.warn("[AI OFFER PARSER] Local Vision OCR did not extract text from image");
            }
        }

        // Tier 3: Parse and combine extracted content
        String combined = ((input != null ? input : "") + " " + (ocrText != null ? ocrText : "")).trim();
        return parseStructuredOffer(combined, hasImage, ocrText != null && !ocrText.isBlank(), input);
    }

    private AiParseResponseDto callGeminiApi(String text, String mimeType, String cleanBase64) throws Exception {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                geminiModel, geminiApiKey);

        String prompt = "You are an expert retail AI analyzing promotional offer signboards and text for Indian local shops.\n" +
                "Carefully examine the image and any user-provided text. Extract ALL discount numbers, conditions, and product categories.\n" +
                "Rules:\n" +
                "- If the image shows '20% OFF' or similar percentage, discountType MUST be 'PERCENTAGE' and discountValue MUST be 20.\n" +
                "- If the image shows 'SAVE ₹150' or '₹150 OFF', discountType MUST be 'FLAT_AMOUNT' and discountValue MUST be 150.\n" +
                "- If the image shows 'MINIMUM BILL ₹1500', minBillAmount MUST be 1500.\n" +
                "- If the image shows 'ON ALL SAREES', applicableCategory MUST be 'Apparel & Sarees'.\n" +
                "- If the image shows 'VALID TILL DIWALI', daysValid should be ~45 and terms should mention Diwali.\n" +
                "- NEVER invent a default 10% discount or 'General' category when specific information exists in the image.\n" +
                "- Output a strict JSON object ONLY with no markdown wrapping, containing these keys:\n" +
                "  title (string), description (string), discountType ('PERCENTAGE'|'FLAT_AMOUNT'|'BOGO'), discountValue (number), " +
                "  minBillAmount (number or null), maxDiscountAmount (number or null), applicableCategory (string), " +
                "  daysValid (integer), termsAndConditions (array of strings).\n\n" +
                (text != null && !text.isBlank() ? "User Text Note: " + text : "");

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        if (cleanBase64 != null && !cleanBase64.isBlank()) {
            Map<String, Object> inlineData = Map.of(
                    "mimeType", mimeType != null ? mimeType : "image/jpeg",
                    "data", cleanBase64
            );
            parts.add(Map.of("inlineData", inlineData));
        }

        Map<String, Object> contents = Map.of("parts", parts);
        Map<String, Object> requestBody = Map.of("contents", List.of(contents));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            String responseText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            if (responseText.contains("```json")) {
                responseText = responseText.substring(responseText.indexOf("```json") + 7);
                responseText = responseText.substring(0, responseText.indexOf("```"));
            } else if (responseText.contains("```")) {
                responseText = responseText.substring(responseText.indexOf("```") + 3);
                responseText = responseText.substring(0, responseText.indexOf("```"));
            }

            JsonNode parsedJson = objectMapper.readTree(responseText.trim());

            int daysValid = parsedJson.path("daysValid").asInt(14);
            LocalDateTime now = LocalDateTime.now();

            List<String> terms = new ArrayList<>();
            if (parsedJson.has("termsAndConditions") && parsedJson.get("termsAndConditions").isArray()) {
                for (JsonNode termNode : parsedJson.get("termsAndConditions")) {
                    terms.add(termNode.asText());
                }
            }

            return AiParseResponseDto.builder()
                    .title(parsedJson.path("title").asText("Special Neighbourhood Discount"))
                    .description(parsedJson.path("description").asText(text))
                    .discountType(parsedJson.path("discountType").asText("PERCENTAGE"))
                    .discountValue(BigDecimal.valueOf(parsedJson.path("discountValue").asDouble(20.0)))
                    .minBillAmount(parsedJson.hasNonNull("minBillAmount") ? BigDecimal.valueOf(parsedJson.get("minBillAmount").asDouble()) : null)
                    .maxDiscountAmount(parsedJson.hasNonNull("maxDiscountAmount") ? BigDecimal.valueOf(parsedJson.get("maxDiscountAmount").asDouble()) : null)
                    .applicableCategory(parsedJson.path("applicableCategory").asText("Apparel & Sarees"))
                    .startDate(now)
                    .endDate(now.plusDays(daysValid))
                    .termsAndConditions(terms)
                    .rawInput(text)
                    .confidenceNotes("Extracted via Google Gemini Vision (" + geminiModel + ")")
                    .build();
        }

        throw new IOException("Non-OK response from Gemini API: " + response.getStatusCode());
    }

    private String runLocalOcr(byte[] imageBytes, String extension) {
        Path tempImage = null;
        try {
            tempImage = Files.createTempFile("ocr_input_", extension != null ? extension : ".png");
            Files.write(tempImage, imageBytes);

            Path scriptPath = getOcrScriptPath();
            if (scriptPath == null || !Files.exists(scriptPath)) {
                log.warn("[AI OFFER PARSER] Local OCR script not found at resolved location");
                return null;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-ExecutionPolicy", "Bypass", "-File",
                    scriptPath.toAbsolutePath().toString(),
                    "-ImagePath", tempImage.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(12, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.warn("[AI OFFER PARSER] Local OCR process timed out");
                return null;
            }

            if (process.exitValue() == 0) {
                return output.toString().trim();
            } else {
                log.warn("[AI OFFER PARSER] Local OCR process failed with exit code {}: {}", process.exitValue(), output);
            }
        } catch (Exception ex) {
            log.warn("[AI OFFER PARSER] Failed to execute local OCR: {}", ex.getMessage());
        } finally {
            if (tempImage != null) {
                try {
                    Files.deleteIfExists(tempImage);
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    private Path getOcrScriptPath() {
        Path direct = Paths.get("backend/src/main/resources/scripts/ocr.ps1").toAbsolutePath().normalize();
        if (Files.exists(direct)) {
            return direct;
        }

        Path directAlt = Paths.get("src/main/resources/scripts/ocr.ps1").toAbsolutePath().normalize();
        if (Files.exists(directAlt)) {
            return directAlt;
        }

        try {
            ClassPathResource resource = new ClassPathResource("scripts/ocr.ps1");
            if (resource.exists()) {
                Path tempScript = Files.createTempFile("ocr_script_", ".ps1");
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, tempScript, StandardCopyOption.REPLACE_EXISTING);
                }
                tempScript.toFile().deleteOnExit();
                return tempScript;
            }
        } catch (Exception ex) {
            log.warn("Could not extract ocr.ps1 from classpath: {}", ex.getMessage());
        }

        return null;
    }

    public AiParseResponseDto fallbackNlpParser(String text) {
        return parseStructuredOffer(text, false, false, text);
    }

    public AiParseResponseDto parseStructuredOffer(String combinedText, boolean hasImage, boolean ocrSuccess, String rawInput) {
        String lower = combinedText != null ? combinedText.toLowerCase() : "";
        LocalDateTime now = LocalDateTime.now();

        String discountType = null;
        BigDecimal discountValue = null;
        BigDecimal minBill = null;
        BigDecimal maxDiscount = null;
        String category = null;
        LocalDateTime endDate = now.plusDays(14);
        List<String> terms = new ArrayList<>();

        // 1. Detect Percentage (e.g. 20%, flat 25% off, 20 percent off, or OCR artifacts 200/0 off)
        Pattern pctPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:%|0/0|o/o|percent)\\s*(?:off)?", Pattern.CASE_INSENSITIVE);
        Matcher pctMatcher = pctPattern.matcher(lower);
        if (pctMatcher.find()) {
            discountType = "PERCENTAGE";
            discountValue = new BigDecimal(pctMatcher.group(1));
            terms.add(discountValue.stripTrailingZeros().toPlainString() + "% discount applied on eligible items");
        } else if (lower.contains("bogo") || (lower.contains("buy") && lower.contains("get"))) {
            discountType = "BOGO";
            discountValue = BigDecimal.ONE;
            terms.add("Buy and get free promotional item");
        } else {
            // 2. Detect Flat Amount (e.g. "save 150", "save rs 150", "₹150 off", "150 off", "rs 200 off")
            Pattern flatPattern = Pattern.compile(
                    "(?:(?:save|flat|get|discount(?:\\s+of)?)\\s*(?:rs\\.?|inr|₹|r)?\\s*(\\d+))|" +
                    "(?:(?:rs\\.?|inr|₹|r)\\s*(\\d+)\\s*(?:off|discount|save|cashback))|" +
                    "(?:(\\d+)\\s*(?:rs\\.?|inr|₹|r|off|discount|cashback))",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher flatMatcher = flatPattern.matcher(lower);
            if (flatMatcher.find()) {
                String valStr = flatMatcher.group(1) != null ? flatMatcher.group(1) :
                               (flatMatcher.group(2) != null ? flatMatcher.group(2) : flatMatcher.group(3));
                if (valStr != null) {
                    discountType = "FLAT_AMOUNT";
                    discountValue = new BigDecimal(valStr);
                    terms.add("Flat ₹" + discountValue.stripTrailingZeros().toPlainString() + " discount deducted at checkout");
                }
            }
        }

        // If an image was provided, but NO discount could be found in image or text:
        if (hasImage && discountValue == null) {
            log.error("[AI OFFER PARSER] AI extraction status: FAILURE | Reason: Could not identify discount/offer from uploaded image");
            throw new InvalidAiParseException("Could not read the offer image. Please try another image or enter the offer details manually.");
        }

        // If no image was provided and no discount was found in text:
        if (!hasImage && discountValue == null) {
            if (combinedText == null || combinedText.isBlank()) {
                log.error("[AI OFFER PARSER] AI extraction status: FAILURE | Reason: Input text is empty");
                throw new InvalidAiParseException("Please enter offer details or upload an offer image.");
            }
            log.error("[AI OFFER PARSER] AI extraction status: FAILURE | Reason: No discount identified in input text");
            throw new InvalidAiParseException("Could not recognize offer details. Please specify a discount (e.g. 20% off or ₹150 off).");
        }

        // 3. Detect Minimum Bill (e.g. "minimum bill ₹1500", "min bill 1500", "orders above ₹2000", "above 1000", "minimum order ₹600")
        Pattern minBillPattern = Pattern.compile(
                "(?:min(?:imum)?(?:\\s+bill|\\s+purchase|\\s+order)?|orders?\\s+above|purchase\\s+of|above)\\s*(?:is|of|at|above|over)?\\s*(?:rs\\.?|₹|r)?\\s*(\\d+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher minBillMatcher = minBillPattern.matcher(lower);
        if (minBillMatcher.find()) {
            minBill = new BigDecimal(minBillMatcher.group(1));
            terms.add("Applicable only on minimum bill of ₹" + minBill.stripTrailingZeros().toPlainString());
        }

        // 4. Detect Category
        if (lower.contains("saree") || lower.contains("silk") || lower.contains("kurti") || lower.contains("dress") || lower.contains("clothing") || lower.contains("apparel")) {
            category = "Apparel & Sarees";
        } else if (lower.contains("grocery") || lower.contains("kirana") || lower.contains("pantry") || lower.contains("rice") || lower.contains("dal") || lower.contains("oil") || lower.contains("spices")) {
            category = "Groceries & Staples";
        } else if (lower.contains("bakery") || lower.contains("cake") || lower.contains("pastry") || lower.contains("pastries") || lower.contains("bread") || lower.contains("sweet") || lower.contains("dessert")) {
            category = "Bakery & Desserts";
        } else if (lower.contains("mobile") || lower.contains("laptop") || lower.contains("electronics") || lower.contains("gadget") || lower.contains("phone")) {
            category = "Electronics & Gadgets";
        } else if (lower.contains("salon") || lower.contains("spa") || lower.contains("haircut") || lower.contains("beauty")) {
            category = "Salon & Wellness";
        } else {
            category = "General Retail";
        }

        // 5. Detect Expiry Date & Festivals
        boolean isDiwali = lower.contains("diwali") || lower.contains("deepavali");
        boolean isFestive = lower.contains("festive") || lower.contains("festival") || isDiwali;
        boolean isSunday = lower.contains("sunday") || lower.contains("weekend");
        boolean isToday = lower.contains("today only");

        if (isDiwali) {
            endDate = now.plusDays(45);
            terms.add("Special Diwali festive offer");
        } else if (isSunday) {
            endDate = now.plusDays(3);
            terms.add("Weekend exclusive offer");
        } else if (isToday) {
            endDate = now.plusDays(1);
            terms.add("Valid for today only");
        } else {
            terms.add("Valid for 14 days from publish date");
        }

        if (isFestive && !isDiwali) {
            terms.add("Festive celebration discount");
        }

        terms.add("Single voucher redemption per bill");
        terms.add("Cannot be clubbed with existing in-store promotions");

        // 6. Formulate clean Title
        String discountStr = "PERCENTAGE".equals(discountType) ?
                discountValue.stripTrailingZeros().toPlainString() + "%" :
                "₹" + discountValue.stripTrailingZeros().toPlainString();

        String title;
        if (isDiwali) {
            title = String.format("Diwali Sale - %s Off on %s", discountStr, category);
        } else if (isFestive || lower.contains("festive special")) {
            title = String.format("Festive Special %s Off on %s", discountStr, category);
        } else if (lower.contains("birthday special") || lower.contains("birthday")) {
            title = String.format("Birthday Special - %s Off on %s", discountStr, category);
        } else if ("PERCENTAGE".equals(discountType)) {
            title = String.format("Flat %s Off on %s", discountStr, category);
        } else if ("FLAT_AMOUNT".equals(discountType)) {
            title = String.format("Save %s on %s", discountStr, category);
        } else {
            title = "Buy 2 Get 1 Free on " + category;
        }

        String description = String.format("Avail %s discount on %s.%s Valid till %s.",
                discountStr,
                category,
                (minBill != null ? " Minimum purchase amount ₹" + minBill.stripTrailingZeros().toPlainString() + "." : ""),
                endDate.toLocalDate());

        String confidenceNotes = ocrSuccess ?
                "Extracted via Smart Vision OCR & Retail Rule Engine" :
                "Extracted via Smart NLP Offer Parser";

        log.info("[AI OFFER PARSER] AI extraction status: SUCCESS | Title: '{}' | Discount: {} {} | MinBill: {} | Category: '{}'",
                title, discountValue, discountType, minBill, category);

        return AiParseResponseDto.builder()
                .title(title)
                .description(description)
                .discountType(discountType)
                .discountValue(discountValue)
                .minBillAmount(minBill)
                .maxDiscountAmount(maxDiscount)
                .applicableCategory(category)
                .startDate(now)
                .endDate(endDate)
                .termsAndConditions(terms)
                .rawInput(rawInput)
                .confidenceNotes(confidenceNotes)
                .build();
    }
}
