package com.weddingevent.admin.staff;

import com.weddingevent.admin.support.DateRange;
import com.weddingevent.common.domain.StaffMember;
import com.weddingevent.common.domain.StaffTask;
import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.domain.enums.StaffWorkStatus;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.BookingRepository;
import com.weddingevent.common.repository.BookingRepository.StaffBookingCount;
import com.weddingevent.common.repository.StaffRepository;
import com.weddingevent.common.repository.StaffTaskRepository;
import com.weddingevent.common.support.CodeGenerator;
import com.weddingevent.common.support.CodeGenerator.Code;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {

    public static final String VN_PHONE = "^0(3|5|7|8|9)[0-9]{8}$";

    public record StaffDto(String id, String code, String name, String position, String phone, StaffWorkStatus workStatus, long completedThisMonth) {}

    public record TaskDto(String id, String staffId, String title, LocalDate dueDate, String notes, boolean completed) {
        static TaskDto of(StaffTask t) {
            return new TaskDto(t.getId().toString(), t.getStaffMember().getId().toString(), t.getTitle(), t.getDueDate(), t.getNotes(), t.isCompleted());
        }
    }

    public record CreateStaffRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = VN_PHONE, message = "Số điện thoại không hợp lệ") String phone,
            @NotBlank @Size(max = 100) String position) {}

    public record CreateTaskRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull LocalDate dueDate,
            @Size(max = 1000) String notes) {}

    public record UpdateTaskRequest(@NotNull Boolean completed) {}

    private final StaffRepository staff;
    private final StaffTaskRepository tasks;
    private final BookingRepository bookings;
    private final CodeGenerator codes;
    private final Clock clock;

    public StaffService(StaffRepository staff, StaffTaskRepository tasks, BookingRepository bookings, CodeGenerator codes, Clock clock) {
        this.staff = staff;
        this.tasks = tasks;
        this.bookings = bookings;
        this.codes = codes;
        this.clock = clock;
    }

    /** completedThisMonth = non-cancelled assignments from the 1st of the month up to today (commission/KPI basis) */
    @Transactional(readOnly = true)
    public List<StaffDto> list() {
        LocalDate today = LocalDate.now(clock);
        DateRange month = DateRange.month(today);
        Map<UUID, Long> done = bookings.countPerStaffBetween(month.from(), today, BookingStatus.CANCELLED).stream()
                .collect(Collectors.toMap(StaffBookingCount::getStaffId, StaffBookingCount::getTotal));
        return staff.findAllByOrderByCodeAsc().stream()
                .map(s -> toDto(s, done.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional
    public StaffDto create(CreateStaffRequest req) {
        StaffMember s = new StaffMember();
        s.setCode(codes.next(Code.STAFF));
        s.setFullName(req.name().trim());
        s.setPhone(req.phone());
        s.setPosition(req.position().trim());
        return toDto(staff.save(s), 0);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> tasks(UUID staffId) {
        requireStaff(staffId);
        return tasks.findByStaffMemberIdOrderByDueDateAscCreatedAtAsc(staffId).stream().map(TaskDto::of).toList();
    }

    @Transactional
    public TaskDto createTask(UUID staffId, CreateTaskRequest req) {
        StaffTask t = new StaffTask();
        t.setStaffMember(requireStaff(staffId));
        t.setTitle(req.title().trim());
        t.setDueDate(req.dueDate());
        t.setNotes(req.notes() == null || req.notes().isBlank() ? null : req.notes().trim());
        return TaskDto.of(tasks.save(t));
    }

    @Transactional
    public TaskDto updateTask(UUID taskId, UpdateTaskRequest req) {
        StaffTask t = tasks.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("công việc", taskId));
        t.setCompleted(req.completed());
        return TaskDto.of(t);
    }

    @Transactional
    public void deleteTask(UUID taskId) {
        StaffTask t = tasks.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("công việc", taskId));
        tasks.delete(t);
    }

    private StaffMember requireStaff(UUID id) {
        return staff.findById(id).orElseThrow(() -> new ResourceNotFoundException("nhân sự", id));
    }

    private static StaffDto toDto(StaffMember s, long completed) {
        return new StaffDto(s.getId().toString(), s.getCode(), s.getFullName(), s.getPosition(), s.getPhone(), s.getWorkStatus(), completed);
    }
}
