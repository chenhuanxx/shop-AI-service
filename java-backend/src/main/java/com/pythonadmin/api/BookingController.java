package com.pythonadmin.api;

import com.pythonadmin.common.ApiResponse;
import com.pythonadmin.domain.BookingCourseEntity;
import com.pythonadmin.domain.BookingCourseRepository;
import com.pythonadmin.domain.BookingReservationEntity;
import com.pythonadmin.domain.BookingReservationRepository;
import com.pythonadmin.domain.ApprovalEntity;
import com.pythonadmin.domain.ApprovalRepository;
import com.pythonadmin.domain.CourseCheckinEntity;
import com.pythonadmin.domain.CourseCheckinRepository;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.BookingService;
import com.pythonadmin.service.CurrentUserService;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {
    private final CurrentUserService currentUserService;
    private final BookingCourseRepository courseRepo;
    private final BookingReservationRepository reservationRepo;
    private final ApprovalRepository approvalRepo;
    private final CourseCheckinRepository checkinRepo;
    private final UserRepository userRepo;
    private final BookingService bookingService;
    private final AdminService adminService;

    public BookingController(
        CurrentUserService currentUserService,
        BookingCourseRepository courseRepo,
        BookingReservationRepository reservationRepo,
        ApprovalRepository approvalRepo,
        CourseCheckinRepository checkinRepo,
        UserRepository userRepo,
        BookingService bookingService,
        AdminService adminService
    ) {
        this.currentUserService = currentUserService;
        this.courseRepo = courseRepo;
        this.reservationRepo = reservationRepo;
        this.approvalRepo = approvalRepo;
        this.checkinRepo = checkinRepo;
        this.userRepo = userRepo;
        this.bookingService = bookingService;
        this.adminService = adminService;
    }

    public record BookingCourseListItem(
        String id,
        String name,
        String startAt,
        String teacherName,
        int enrolledCount,
        int capacity,
        String reservationState,
        String courseStatus,
        boolean canReserve,
        boolean canCancel
    ) {}

    public record BookingCourseDetailItem(
        String id,
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
        String courseStatus,
        boolean canReserve,
        boolean canCancel
    ) {}

    public record PageWrapper<T>(List<T> list, long total) {}
    public record IdPayload(@NotBlank String id) {}
    public record QrPayload(@NotBlank String qr) {}

    @GetMapping("/booking/courses")
    public ApiResponse<?> listCourses(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "userId", required = false) Integer queryUserId
    ) {
        UserEntity user = currentUserService.requireUser();
        Integer targetUserId = user.getId();
        if (queryUserId != null) {
            adminService.requireAdmin(user);
            targetUserId = queryUserId;
        }
        if (targetUserId == null) {
            return ApiResponse.fail("用户信息异常");
        }
        String kw = keyword == null ? "" : keyword.trim();
        String pattern = "%" + kw + "%";
        
        // Final or effectively final for lambda
        final Integer finalUserId = targetUserId;
        Set<String> checkedInCourseIds = checkinRepo.findByUserId(finalUserId).stream()
            .map(CourseCheckinEntity::getCourseId)
            .filter(courseId -> courseId != null && !courseId.isBlank())
            .collect(Collectors.toSet());

        List<BookingCourseListItem> list = courseRepo.search(kw, pattern).stream()
            .filter(c -> isUpcoming(safe(c.getStartAt())))
            .filter(c -> !checkedInCourseIds.contains(c.getId()))
            .filter(c -> !bookingService.isPublicCourseNotPublished(c.getId()))
            .map(c -> toListItem(c, finalUserId))
            .toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/booking/courses/page")
    public ApiResponse<?> listCoursesPage(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "userId", required = false) Integer queryUserId,
        @RequestParam(name = "page", required = false, defaultValue = "1") int page,
        @RequestParam(name = "page_size", required = false, defaultValue = "10") int pageSize
    ) {
        UserEntity user = currentUserService.requireUser();
        Integer targetUserId = user.getId();
        if (queryUserId != null) {
            adminService.requireAdmin(user);
            targetUserId = queryUserId;
        }
        if (targetUserId == null) {
            return ApiResponse.fail("用户信息异常");
        }
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);

        String kw = keyword == null ? "" : keyword.trim();
        String pattern = "%" + kw + "%";

        final Integer finalUserId = targetUserId;

        // 展示所有已发布的课程（包括已结束的），用 courseStatus 区分
        List<BookingCourseEntity> filtered = courseRepo.search(kw, pattern).stream()
            .filter(c -> !bookingService.isPublicCourseNotPublished(c.getId()))
            .toList();

        long total = filtered.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= filtered.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(filtered.size(), fromIndex + safePageSize);
        List<BookingCourseListItem> list = filtered.subList(fromIndex, toIndex).stream()
            .map(c -> toListItem(c, finalUserId))
            .toList();
        return ApiResponse.ok(new PageWrapper<>(list, total));
    }

    @GetMapping("/booking/course-detail")
    public ApiResponse<?> getCourseDetail(
        @RequestParam(name = "id", required = false) String id,
        @RequestParam(name = "userId", required = false) Integer queryUserId
    ) {
        UserEntity user = currentUserService.requireUser();
        String courseId = id == null ? "" : id.trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        try {
            BookingCourseEntity c = courseRepo.findById(courseId).orElse(null);
            if (c == null) {
                return ApiResponse.fail("课程不存在");
            }
            Integer targetUserId = user.getId();
            if (queryUserId != null) {
                adminService.requireAdmin(user);
                targetUserId = queryUserId;
            }
            if (targetUserId == null) {
                return ApiResponse.fail("用户信息异常");
            }
            
            // 检查用户是否已签到该课程
            boolean isCheckedIn = !checkinRepo.findByUserIdAndCourseId(targetUserId, courseId).isEmpty();
            
            // 检查用户是否已预约该课程（状态为"已预约"）
            List<ApprovalEntity> activeReservations = approvalRepo.findByUserIdAndCourseIdAndTitleAndState(
                targetUserId, courseId, BookingService.RESERVATION_TITLE, "已预约");
            boolean hasActiveReservation = !activeReservations.isEmpty();
            
            if (bookingService.isPublicCourseNotPublished(c.getId()) && !isCheckedIn) {
                return ApiResponse.fail("课程尚未发布");
            }
            
            String courseStatus = isUpcoming(safe(c.getStartAt())) ? "未开始" : "已结束";
            String reservationState;
            boolean canReserve;
            boolean canCancel;
            
            if (isCheckedIn) {
                reservationState = "已签到";
                canReserve = false;
                canCancel = false;
            } else if (hasActiveReservation) {
                reservationState = "已预约";
                canReserve = false;
                canCancel = isUpcoming(safe(c.getStartAt()));
            } else {
                reservationState = "未预约";
                int enrolledCount = bookingService.getEnrolledCount(courseId);
                canReserve = !bookingService.isPublicCourseNotPublished(c.getId()) 
                    && enrolledCount < c.getCapacity();
                canCancel = false;
            }
            
            // 从数据库实时查询已预约人数
            int enrolledCount = bookingService.getEnrolledCount(courseId);
            
            BookingCourseDetailItem detail = new BookingCourseDetailItem(
                c.getId(),
                safe(c.getName()),
                safe(c.getStartAt()),
                safe(c.getTeacherName()),
                enrolledCount,
                c.getCapacity(),
                safe(c.getAcademicYear()),
                safe(c.getTerm()),
                safe(c.getDepartment()),
                safe(c.getClassName()),
                safe(c.getOffice()),
                safe(c.getLocation()),
                safe(c.getCourse()),
                safe(c.getTopic()),
                reservationState,
                courseStatus,
                canReserve,
                canCancel
            );
            return ApiResponse.ok(detail);
        } catch (Exception e) {
            return ApiResponse.fail("系统错误");
        }
    }

    @PostMapping("/booking/reserve")
    public ApiResponse<?> reserve(@RequestBody IdPayload payload) {
        UserEntity user = currentUserService.requireUser();
        String courseId = payload == null || payload.id() == null ? "" : payload.id().trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        try {
            bookingService.reserve(user.getId(), courseId);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
        return ApiResponse.ok(new OkWrapper(true));
    }

    @PostMapping("/booking/cancel")
    public ApiResponse<?> cancel(@RequestBody IdPayload payload) {
        UserEntity user = currentUserService.requireUser();
        String courseId = payload == null || payload.id() == null ? "" : payload.id().trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        try {
            bookingService.cancel(user.getId(), courseId);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
        return ApiResponse.ok(new OkWrapper(true));
    }

    @PostMapping("/booking/reserve-by-qr")
    public ApiResponse<?> reserveByQr(@RequestBody QrPayload payload) {
        UserEntity user = currentUserService.requireUser();
        String qr = payload == null || payload.qr() == null ? "" : payload.qr().trim();
        if (qr.isBlank()) {
            return ApiResponse.fail("缺少二维码内容");
        }
        BookingReservationEntity r = new BookingReservationEntity();
        r.setUserId(user.getId());
        r.setQr(qr);
        reservationRepo.save(r);
        return ApiResponse.ok(new OkWrapper(true));
    }

    private BookingCourseListItem toListItem(BookingCourseEntity c, int userId) {
        BookingService.ReservationState state = bookingService.getReservationState(userId, c);
        String courseStatus = isUpcoming(safe(c.getStartAt())) ? "未开始" : "已结束";
        // 从数据库实时查询已预约人数
        int enrolledCount = bookingService.getEnrolledCount(c.getId());
        return new BookingCourseListItem(
            c.getId(),
            safe(c.getName()),
            safe(c.getStartAt()),
            safe(c.getTeacherName()),
            enrolledCount,
            c.getCapacity(),
            safe(state.reservationState()),
            courseStatus,
            state.canReserve(),
            state.canCancel()
        );
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

    public record ListWrapper<T>(List<T> list) {}
    public record OkWrapper(boolean ok) {}

    // 签到API
    @PostMapping("/booking/checkin")
    public ApiResponse<?> checkin(@RequestBody IdPayload payload) {
        UserEntity user = currentUserService.requireUser();
        String courseId = payload == null || payload.id() == null ? "" : payload.id().trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        try {
            BookingCourseEntity c = courseRepo.findById(courseId).orElse(null);
            if (c == null) {
                return ApiResponse.fail("课程不存在");
            }
            // 检查是否已签到
            List<CourseCheckinEntity> existing = checkinRepo.findByUserIdAndCourseId(user.getId(), courseId);
            if (!existing.isEmpty()) {
                return ApiResponse.fail("您已签到，无需重复签到");
            }
            // 创建签到记录
            CourseCheckinEntity checkin = new CourseCheckinEntity();
            checkin.setUserId(user.getId());
            checkin.setCourseId(courseId);
            checkinRepo.save(checkin);
            return ApiResponse.ok(new OkWrapper(true));
        } catch (Exception e) {
            return ApiResponse.fail("签到失败：" + e.getMessage());
        }
    }

    @GetMapping("/booking/checkin-status")
    public ApiResponse<?> checkinStatus(@RequestParam(name = "courseId") String courseId) {
        UserEntity user = currentUserService.requireUser();
        String id = courseId == null ? "" : courseId.trim();
        if (id.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        List<CourseCheckinEntity> existing = checkinRepo.findByUserIdAndCourseId(user.getId(), id);
        return ApiResponse.ok(new CheckinStatusWrapper(!existing.isEmpty()));
    }

    @GetMapping("/booking/my-checkins")
    public ApiResponse<?> myCheckins() {
        UserEntity user = currentUserService.requireUser();
        List<CourseCheckinEntity> checkins = checkinRepo.findByUserId(user.getId());
        // 获取课程详情
        List<CheckinItem> items = checkins.stream().map(c -> {
            BookingCourseEntity course = courseRepo.findById(c.getCourseId()).orElse(null);
            return new CheckinItem(
                c.getId(),
                c.getCourseId(),
                course != null ? safe(course.getName()) : "",
                course != null ? safe(course.getTeacherName()) : "",
                course != null ? safe(course.getStartAt()) : "",
                course != null ? safe(course.getLocation()) : "",
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : ""
            );
        }).toList();
        return ApiResponse.ok(new CheckinListWrapper(items));
    }

    public record CheckinStatusWrapper(boolean checkedIn) {}
    public record CheckinItem(Long id, String courseId, String courseName, String teacherName, String startAt, String location, String checkinTime) {}
    public record CheckinListWrapper(List<CheckinItem> list) {}

    // 获取课程预约用户列表
    @GetMapping("/booking/course/reserved-users")
    public ApiResponse<?> getReservedUsers(@RequestParam(name = "courseId") String courseId) {
        UserEntity user = currentUserService.requireUser();
        String id = courseId == null ? "" : courseId.trim();
        if (id.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        try {
            BookingCourseEntity course = courseRepo.findById(id).orElse(null);
            if (course == null) {
                return ApiResponse.fail("课程不存在");
            }
            // 查询该课程的所有已预约记录
            List<ApprovalEntity> approvedList = approvalRepo.findByCourseIdAndTitleAndState(id, BookingService.RESERVATION_TITLE, "已预约");
            
            List<ReservedUserItem> users = approvedList.stream().map(r -> {
                UserEntity u = userRepo.findById(r.getUserId()).orElse(null);
                String username = u != null ? safe(u.getUsername()) : "未知用户";
                String phone = u != null ? safe(u.getPhone()) : "";
                String reserveTime = r.getCreatedAtStr() != null ? r.getCreatedAtStr() : "";
                // 检查是否已签到
                boolean checkedIn = !checkinRepo.findByUserIdAndCourseId(r.getUserId(), id).isEmpty();
                return new ReservedUserItem(r.getUserId(), username, phone, reserveTime, checkedIn);
            }).toList();
            return ApiResponse.ok(new ReservedUsersWrapper(users));
        } catch (Exception e) {
            return ApiResponse.fail("获取预约用户失败：" + e.getMessage());
        }
    }

    public record ReservedUserItem(Integer userId, String username, String phone, String reserveTime, boolean checkedIn) {}
    public record ReservedUsersWrapper(List<ReservedUserItem> list) {}

    // 获取用户已预约的课程列表（已预约但未签到）
    @GetMapping("/booking/my-reserved-courses")
    public ApiResponse<?> getMyReservedCourses(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                                @RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize) {
        UserEntity user = currentUserService.requireUser();
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);

        // 获取用户已预约的课程ID
        List<ApprovalEntity> approvals = approvalRepo.findByUserIdAndTitleAndState(user.getId(), BookingService.RESERVATION_TITLE, "已预约");
        Set<String> reservedCourseIds = approvals.stream()
            .map(ApprovalEntity::getCourseId)
            .filter(courseId -> courseId != null && !courseId.isBlank())
            .collect(Collectors.toSet());

        if (reservedCourseIds.isEmpty()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), 0));
        }

        // 获取用户已签到的课程ID（需要排除这些）
        Set<String> checkedInCourseIds = checkinRepo.findByUserId(user.getId()).stream()
            .map(CourseCheckinEntity::getCourseId)
            .filter(courseId -> courseId != null && !courseId.isBlank())
            .collect(Collectors.toSet());

        // 获取已预约但未签到的课程信息
        List<BookingCourseListItem> allReserved = reservedCourseIds.stream()
            .filter(courseId -> !checkedInCourseIds.contains(courseId)) // 排除已签到的
            .map(courseId -> {
                BookingCourseEntity c = courseRepo.findById(courseId).orElse(null);
                if (c == null) return null;
                // 从数据库实时查询已预约人数
                int enrolledCount = bookingService.getEnrolledCount(courseId);
                return new BookingCourseListItem(
                    c.getId(),
                    safe(c.getName()),
                    safe(c.getStartAt()),
                    safe(c.getTeacherName()),
                    enrolledCount,
                    c.getCapacity(),
                    "已预约",
                    isUpcoming(safe(c.getStartAt())) ? "未开始" : "已结束",
                    false,
                    true
                );
            })
            .filter(item -> item != null)
            .toList();

        long total = allReserved.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= allReserved.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(allReserved.size(), fromIndex + safePageSize);
        List<BookingCourseListItem> list = allReserved.subList(fromIndex, toIndex);

        return ApiResponse.ok(new PageWrapper<>(list, total));
    }

    // 获取用户已签到的课程列表
    @GetMapping("/booking/my-checked-in-courses")
    public ApiResponse<?> getMyCheckedInCourses(@RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                                 @RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize) {
        UserEntity user = currentUserService.requireUser();
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);

        // 获取用户已签到的课程ID
        List<CourseCheckinEntity> checkins = checkinRepo.findByUserId(user.getId());
        Set<String> checkedInCourseIds = checkins.stream()
            .map(CourseCheckinEntity::getCourseId)
            .filter(courseId -> courseId != null && !courseId.isBlank())
            .collect(Collectors.toSet());

        if (checkedInCourseIds.isEmpty()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), 0));
        }

        // 获取这些课程的信息
        List<BookingCourseListItem> allCheckedIn = checkedInCourseIds.stream()
            .map(courseId -> {
                BookingCourseEntity c = courseRepo.findById(courseId).orElse(null);
                if (c == null) return null;
                // 从数据库实时查询已预约人数
                int enrolledCount = bookingService.getEnrolledCount(courseId);
                return new BookingCourseListItem(
                    c.getId(),
                    safe(c.getName()),
                    safe(c.getStartAt()),
                    safe(c.getTeacherName()),
                    enrolledCount,
                    c.getCapacity(),
                    "已签到",
                    isUpcoming(safe(c.getStartAt())) ? "未开始" : "已结束",
                    false,
                    false
                );
            })
            .filter(item -> item != null)
            .toList();

        long total = allCheckedIn.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= allCheckedIn.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(allCheckedIn.size(), fromIndex + safePageSize);
        List<BookingCourseListItem> list = allCheckedIn.subList(fromIndex, toIndex);

        return ApiResponse.ok(new PageWrapper<>(list, total));
    }

    // 获取可预约的课程列表（未开始、未满额、未预约的课程）
    @GetMapping("/booking/available-courses")
    public ApiResponse<?> getAvailableCourses(@RequestParam(name = "keyword", required = false) String keyword,
                                              @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                                              @RequestParam(name = "page_size", required = false, defaultValue = "20") int pageSize) {
        UserEntity user = currentUserService.requireUser();
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);

        String kw = keyword == null ? "" : keyword.trim();
        String pattern = "%" + kw + "%";

        // 获取用户已预约的课程ID
        List<ApprovalEntity> approvals = approvalRepo.findByUserIdAndTitleAndState(user.getId(), BookingService.RESERVATION_TITLE, "已预约");
        Set<String> reservedCourseIds = approvals.stream()
            .map(ApprovalEntity::getCourseId)
            .filter(courseId -> courseId != null && !courseId.isBlank())
            .collect(Collectors.toSet());

        // 获取用户已签到的课程ID
        Set<String> checkedInCourseIds = checkinRepo.findByUserId(user.getId()).stream()
            .map(CourseCheckinEntity::getCourseId)
            .filter(courseId -> courseId != null && !courseId.isBlank())
            .collect(Collectors.toSet());

        // 过滤出可预约的课程：未开始、未满额、未预约
        List<BookingCourseEntity> filtered = courseRepo.search(kw, pattern).stream()
            .filter(c -> !bookingService.isPublicCourseNotPublished(c.getId()))
            .filter(c -> isUpcoming(safe(c.getStartAt()))) // 必须是未开始的课程
            .filter(c -> !reservedCourseIds.contains(c.getId())) // 未预约
            .filter(c -> !checkedInCourseIds.contains(c.getId())) // 未签到
            .filter(c -> bookingService.getEnrolledCount(c.getId()) < c.getCapacity()) // 未满额（实时查询）
            .toList();

        long total = filtered.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= filtered.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(filtered.size(), fromIndex + safePageSize);
        List<BookingCourseListItem> list = filtered.subList(fromIndex, toIndex).stream()
            .map(c -> toListItem(c, user.getId()))
            .toList();

        return ApiResponse.ok(new PageWrapper<>(list, total));
    }
}
