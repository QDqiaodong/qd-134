package com.diving.base.controller;

import com.diving.base.dto.request.TeamCreateRequest;
import com.diving.base.dto.response.ApiResponse;
import com.diving.base.dto.response.PageResponse;
import com.diving.base.entity.Team;
import com.diving.base.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/team")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Team>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        PageResponse<Team> result = teamService.findAll(page, size, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Team>> getById(@PathVariable Long id) {
        Team team = teamService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(team));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Team>> create(@Valid @RequestBody TeamCreateRequest request) {
        Team team = teamService.create(request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", team));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Team>> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamCreateRequest request) {
        Team team = teamService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", team));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}