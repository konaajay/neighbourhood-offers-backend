package com.neighbourhood.offers.controller;

import com.neighbourhood.offers.dto.AiParseRequest;
import com.neighbourhood.offers.dto.AiParseResponseDto;
import com.neighbourhood.offers.service.AiOfferParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiParserController {

    private final AiOfferParserService aiOfferParserService;

    @PostMapping("/parse-offer")
    public ResponseEntity<AiParseResponseDto> parseOffer(@RequestBody AiParseRequest request) {
        return ResponseEntity.ok(aiOfferParserService.parseOffer(request));
    }
}
