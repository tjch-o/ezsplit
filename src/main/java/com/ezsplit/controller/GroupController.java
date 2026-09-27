package com.ezsplit.controller;

import com.ezsplit.dto.request.AddGroupMemberRequest;
import com.ezsplit.dto.request.CreateGroupRequest;
import com.ezsplit.dto.response.GroupMemberResponse;
import com.ezsplit.dto.response.GroupResponse;
import com.ezsplit.entity.User;
import com.ezsplit.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest req, @AuthenticationPrincipal User caller) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(req, caller.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getGroupsByUserId(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(groupService.getAllGroups(caller.getUserId()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> get(@PathVariable UUID groupId, @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(groupService.getById(groupId, caller.getUserId()));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddGroupMemberRequest req,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(groupService.addMember(groupId, req, caller.getUserId()));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getMembers(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(groupService.getMembers(groupId, caller.getUserId()));
    }
}
