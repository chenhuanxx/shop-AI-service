package com.pythonadmin.api;

import com.pythonadmin.common.ApiResponse;
import com.pythonadmin.domain.ApprovalEntity;
import com.pythonadmin.domain.ApprovalRepository;
import com.pythonadmin.domain.BookingCourseEntity;
import com.pythonadmin.domain.BookingCourseRepository;
import com.pythonadmin.domain.MessageEntity;
import com.pythonadmin.domain.MessageRepository;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.BookingService;
import com.pythonadmin.service.CurrentUserService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.validation.constraints.NotBlank;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessagesController {
    private final CurrentUserService currentUserService;
    private final AdminService adminService;
    private final ApprovalRepository approvalRepo;
    private final BookingCourseRepository courseRepo;
    private final MessageRepository messageRepo;
    private final BookingService bookingService;

    public MessagesController(
        CurrentUserService currentUserService,
        AdminService adminService,
        ApprovalRepository approvalRepo,
        BookingCourseRepository courseRepo,
        MessageRepository messageRepo,
        BookingService bookingService
    ) {
        this.currentUserService = currentUserService;
        this.adminService = adminService;
        this.approvalRepo = approvalRepo;
        this.courseRepo = courseRepo;
        this.messageRepo = messageRepo;
        this.bookingService = bookingService;
    }

    public record ListWrapper<T>(List<T> list) {}
    public record OkWrapper(boolean ok) {}
    public record CountWrapper(int count) {}

    public record ApprovalItem(String id, String title, String state, String createdAt) {}
    public record MessageItem(String id, String title, String content, String createdAt, boolean read) {}
    public record PendingCheckinCourseItem(String id, String title, String time, String speaker, String status) {}

    public record ApprovalDetail(
        String id,
        String title,
        String state,
        String createdAt,
        String applyReason,
        String officeOpinion,
        String departmentOpinion,
        String courseId,
        String name,
        String startAt,
        String teacherName,
        int enrolledCount,
        int capacity,
        String academicYear,
        String term,
        String department,
        String className,
        String office,
        String location,
        String course,
        String topic,
        String reservationState,
        boolean canReserve,
        boolean canCancel
    ) {}

    public record ApprovalAuditRequest(@NotBlank String id, @NotBlank String action, String opinion) {}

    @GetMapping("/messages/approvals")
    public ApiResponse<?> listApprovals() {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        List<ApprovalItem> list = approvalRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .map(x -> new ApprovalItem(x.getId(), x.getTitle(), x.getState(), x.getCreatedAtStr()))
            .toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/messages/approval-detail")
    public ApiResponse<?> getApprovalDetail(@RequestParam(name = "id", required = false) String id) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        String approvalId = id == null ? "" : id.trim();
        if (approvalId.isBlank()) {
            return ApiResponse.fail("缺少审批 id");
        }
        ApprovalEntity approval = approvalRepo.findById(approvalId).orElse(null);
        if (approval == null) {
            return ApiResponse.fail("审批不存在");
        }
        BookingCourseEntity course = courseRepo.findById(approval.getCourseId()).orElse(null);
        if (course == null) {
            return ApiResponse.fail("课程不存在");
        }
        BookingService.ReservationState state = bookingService.getReservationState(user.getId(), course);
        // 从数据库实时查询已预约人数
        int enrolledCount = bookingService.getEnrolledCount(course.getId());
        ApprovalDetail detail = new ApprovalDetail(
            approval.getId(),
            approval.getTitle(),
            approval.getState(),
            approval.getCreatedAtStr(),
            safe(approval.getApplyReason()),
            safe(approval.getOfficeOpinion()),
            safe(approval.getDepartmentOpinion()),
            course.getId(),
            safe(course.getName()),
            safe(course.getStartAt()),
            safe(course.getTeacherName()),
            enrolledCount,
            course.getCapacity(),
            safe(course.getAcademicYear()),
            safe(course.getTerm()),
            safe(course.getDepartment()),
            safe(course.getClassName()),
            safe(course.getOffice()),
            safe(course.getLocation()),
            safe(course.getCourse()),
            safe(course.getTopic()),
            state.reservationState(),
            state.canReserve(),
            state.canCancel()
        );
        return ApiResponse.ok(detail);
    }

    @PostMapping("/messages/approval-audit")
    public ApiResponse<?> auditApproval(@RequestBody ApprovalAuditRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        String approvalId = payload == null || payload.id() == null ? "" : payload.id().trim();
        String action = payload == null || payload.action() == null ? "" : payload.action().trim();
        if (approvalId.isBlank()) {
            return ApiResponse.fail("缺少审批 id");
        }
        if (!"approve".equals(action) && !"reject".equals(action)) {
            return ApiResponse.fail("非法操作");
        }
        ApprovalEntity approval = approvalRepo.findById(approvalId).orElse(null);
        if (approval == null) {
            return ApiResponse.fail("审批不存在");
        }
        approval.setState("approve".equals(action) ? "已通过" : "已拒绝");
        approval.setDepartmentOpinion(payload == null || payload.opinion() == null ? "" : payload.opinion().trim());
        approvalRepo.save(approval);
        return ApiResponse.ok(new OkWrapper(true));
    }

    @GetMapping("/messages/list")
    public ApiResponse<?> listMessages() {
        UserEntity user = currentUserService.requireUser();
        List<MessageItem> list = messageRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(x -> new MessageItem(x.getId(), x.getTitle(), x.getContent(), x.getCreatedAtStr(), x.isRead()))
            .toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/messages/pending-checkins")
    public ApiResponse<?> listPendingCheckins() {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        List<PendingCheckinCourseItem> list = courseRepo.findAll(Sort.by(Sort.Direction.ASC, "startAt")).stream()
            .filter(c -> bookingService.getEnrolledCount(c.getId()) > 0) // 实时查询已预约人数
            .filter(c -> isUpcoming(safe(c.getStartAt())))
            .map(c -> new PendingCheckinCourseItem(
                c.getId(),
                safe(c.getName()),
                safe(c.getStartAt()),
                safe(c.getTeacherName()),
                "待签到"
            ))
            .toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @PostMapping("/messages/approvals/cleanup")
    public ApiResponse<?> cleanupApprovedApprovals() {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        List<ApprovalEntity> done = approvalRepo.findByStateIn(List.of("已通过", "已拒绝"));
        int count = done.size();
        if (count > 0) {
            approvalRepo.deleteAllInBatch(done);
        }
        return ApiResponse.ok(new CountWrapper(count));
    }

    @GetMapping(value = "/messages/pending-checkins/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getPendingCheckinQrCode(@RequestParam(name = "id", required = false) String id) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        String courseId = id == null ? "" : id.trim();
        if (courseId.isBlank()) {
            return new byte[0];
        }

        long timestamp = System.currentTimeMillis();
        String token = DigestUtils.md5DigestAsHex((courseId + "_" + timestamp + "_secret").getBytes());
        String qrText = "CHECKIN|" + courseId + "|" + timestamp + "|" + token;

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(qrText, BarcodeFormat.QR_CODE, 260, 260, Map.of());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", os);
            return os.toByteArray();
        } catch (WriterException | java.io.IOException e) {
            return new byte[0];
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isUpcoming(String startAtStr) {
        String s = startAtStr == null ? "" : startAtStr.trim();
        if (s.isBlank()) {
            return true;
        }
        try {
            LocalDateTime startAt = LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            return startAt.isAfter(LocalDateTime.now(ZoneId.systemDefault()));
        } catch (Exception ignored) {
            return true;
        }
    }
}
