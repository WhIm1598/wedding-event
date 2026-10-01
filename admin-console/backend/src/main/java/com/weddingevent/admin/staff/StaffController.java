package com.weddingevent.admin.staff;

import static com.weddingevent.admin.security.SecurityConfig.ADMIN;

import com.weddingevent.admin.staff.StaffService.CreateStaffRequest;
import com.weddingevent.admin.staff.StaffService.CreateTaskRequest;
import com.weddingevent.admin.staff.StaffService.StaffDto;
import com.weddingevent.admin.staff.StaffService.TaskDto;
import com.weddingevent.admin.staff.StaffService.UpdateTaskRequest;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Staff & task assignment (feature spec §5.6). Listing is open to staff (booking form needs it). */
@RestController
@RequestMapping("/api/v1/admin/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<StaffDto>> list() {
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<StaffDto> create(@Valid @RequestBody CreateStaffRequest req) {
        return ApiResponse.ok(service.create(req), "Đã thêm nhân sự");
    }

    @GetMapping("/{staffId}/tasks")
    @PreAuthorize(ADMIN)
    public ApiResponse<List<TaskDto>> tasks(@PathVariable UUID staffId) {
        return ApiResponse.ok(service.tasks(staffId));
    }

    @PostMapping("/{staffId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN)
    public ApiResponse<TaskDto> createTask(@PathVariable UUID staffId, @Valid @RequestBody CreateTaskRequest req) {
        return ApiResponse.ok(service.createTask(staffId, req), "Đã giao việc");
    }

    @PatchMapping("/tasks/{taskId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<TaskDto> updateTask(@PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest req) {
        return ApiResponse.ok(service.updateTask(taskId, req));
    }

    @DeleteMapping("/tasks/{taskId}")
    @PreAuthorize(ADMIN)
    public ApiResponse<Void> deleteTask(@PathVariable UUID taskId) {
        service.deleteTask(taskId);
        return ApiResponse.ok(null);
    }
}
