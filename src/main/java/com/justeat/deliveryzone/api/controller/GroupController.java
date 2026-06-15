package com.justeat.deliveryzone.api.controller;

import com.justeat.deliveryzone.api.dto.GroupDetailResponse;
import com.justeat.deliveryzone.api.dto.GroupsResponse;
import com.justeat.deliveryzone.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/groups")
    public ResponseEntity<GroupsResponse> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }
}
