package com.recallradar.user;

import com.recallradar.user.dto.AdminUserResponse;
import com.recallradar.user.dto.RejectRequestDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/analyst-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalystRequestController {

    private final AdminUserService adminUserService;

    public AdminAnalystRequestController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAnalystRequests(
            @RequestParam(required = false, defaultValue = "PENDING_APPROVAL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.getAnalystRequests(status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getAnalystRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminUserResponse> approveAnalystRequest(
            @PathVariable Long id,
            Authentication authentication) {
        String performedBy = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(adminUserService.approveAnalystRequest(id, performedBy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminUserResponse> rejectAnalystRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejectRequestDto rejectDto,
            Authentication authentication) {
        String performedBy = authentication != null ? authentication.getName() : "ADMIN";
        String reason = rejectDto != null ? rejectDto.rejectionReason() : null;
        return ResponseEntity.ok(adminUserService.rejectAnalystRequest(id, reason, performedBy));
    }
}
