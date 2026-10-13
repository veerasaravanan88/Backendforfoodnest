package com.foodnest.report;

import com.foodnest.common.ApiResponse;
import com.foodnest.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurant/me")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<RestaurantDashboardReportDto>> getDashboardStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        RestaurantDashboardReportDto report = reportService.getDashboardReport(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<RestaurantDashboardReportDto>> getReports(
            @AuthenticationPrincipal UserPrincipal principal) {
        RestaurantDashboardReportDto report = reportService.getDashboardReport(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
