package com.gameStore.Bino.controllers;

import com.gameStore.Bino.dto.RewardsSummaryResponse;
import com.gameStore.Bino.models.Users;
import com.gameStore.Bino.service.RewardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rewards")
@RequiredArgsConstructor
public class RewardsController {

    private final RewardsService rewardsService;

    @GetMapping("/me")
    public ResponseEntity<RewardsSummaryResponse> myRewards(@AuthenticationPrincipal Users user) {
        return ResponseEntity.ok(rewardsService.summaryFor(user.getUsername()));
    }
}
