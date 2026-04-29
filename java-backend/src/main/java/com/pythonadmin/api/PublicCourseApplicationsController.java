package com.pythonadmin.api;

import com.pythonadmin.common.ApiResponse;
import com.pythonadmin.domain.PublicCourseApplicationEntity;
import com.pythonadmin.domain.PublicCourseApplicationRepository;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.AdminService;
import com.pythonadmin.service.BookingService;
import com.pythonadmin.service.CurrentUserService;
import com.pythonadmin.service.PublicCourseApplicationService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicCourseApplicationsController {
    private final CurrentUserService currentUserService;
    private final PublicCourseApplicationRepository repo;
    private final AdminService adminService;
    private final PublicCourseApplicationService publicCourseApplicationService;
    private final BookingService bookingService;
    private final com.pythonadmin.domain.BookingCourseRepository courseRepo;
    private final com.pythonadmin.domain.UserRepository userRepo;
    private final com.pythonadmin.domain.ApprovalRepository approvalRepo;
    private final com.pythonadmin.domain.CourseCheckinRepository checkinRepo;

    public PublicCourseApplicationsController(
        CurrentUserService currentUserService,
        PublicCourseApplicationRepository repo,
        AdminService adminService,
        PublicCourseApplicationService publicCourseApplicationService,
        BookingService bookingService,
        com.pythonadmin.domain.BookingCourseRepository courseRepo,
        com.pythonadmin.domain.UserRepository userRepo,
        com.pythonadmin.domain.ApprovalRepository approvalRepo,
        com.pythonadmin.domain.CourseCheckinRepository checkinRepo
    ) {
        this.currentUserService = currentUserService;
        this.repo = repo;
        this.adminService = adminService;
        this.publicCourseApplicationService = publicCourseApplicationService;
        this.bookingService = bookingService;
        this.courseRepo = courseRepo;
        this.userRepo = userRepo;
        this.approvalRepo = approvalRepo;
        this.checkinRepo = checkinRepo;
    }

    public record ListWrapper<T>(List<T> list) {}
    public record PageWrapper<T>(List<T> list, long total) {}

    public record ApplicationItem(
        String id,
        String category,
        String academicYear,
        String term,
        String department,
        String className,
        String office,
        String location,
        String courseName,
        String topic,
        String startDate,
        int weekNo,
        String weekday,
        String section,
        String applyReason,
        String state,
        String createdAt,
        int userId,
        String userName,
        String auditOpinion,
        String auditedAt,
        int enrolledCount,
        int checkedInCount
    ) {}

    public record CreateRequest(
        @NotBlank String category,
        @NotBlank String academicYear,
        @NotBlank String term,
        @NotBlank String department,
        @NotBlank String className,
        @NotBlank String office,
        @NotBlank String location,
        @NotBlank String courseName,
        @NotBlank String topic,
        @NotBlank String startDate,
        @NotNull @Min(1) Integer weekNo,
        @NotBlank String weekday,
        @NotBlank String section,
        @NotBlank String applyReason
    ) {}

    public record UpdateRequest(
        @NotBlank String id,
        @NotBlank String category,
        @NotBlank String academicYear,
        @NotBlank String term,
        @NotBlank String department,
        @NotBlank String className,
        @NotBlank String office,
        @NotBlank String location,
        @NotBlank String courseName,
        @NotBlank String topic,
        @NotBlank String startDate,
        @NotNull @Min(1) Integer weekNo,
        @NotBlank String weekday,
        @NotBlank String section,
        @NotBlank String applyReason
    ) {}

    public record AuditRequest(@NotBlank String id, @NotBlank String action, String opinion) {}

    public record BatchPublishRequest(List<String> ids) {}

    public record BatchPublishResult(int successCount, int failCount) {}

    @PostMapping("/public-course/applications/batch-publish")
    public ApiResponse<?> batchPublish(@RequestBody(required = false) BatchPublishRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        if (payload == null || payload.ids() == null || payload.ids().isEmpty()) {
            return ApiResponse.fail("缺少要发布的课程ID列表");
        }
        int successCount = 0;
        int failCount = 0;
        for (String id : payload.ids()) {
            String safeId = safe(id).trim();
            if (safeId.isBlank()) continue;
            try {
                PublicCourseApplicationEntity e = repo.findById(safeId).orElse(null);
                if (e == null) {
                    failCount++;
                    continue;
                }
                String st = safe(e.getState());
                if ("已发布".equals(st)) {
                    successCount++;
                    continue;
                }
                if (!"已通过".equals(st)) {
                    failCount++;
                    continue;
                }
                com.pythonadmin.domain.BookingCourseEntity bc = courseRepo.findById(e.getId()).orElse(null);
                if (bc == null) {
                    bc = toBookingCourse(e);
                } else {
                    applyToBookingCourse(bc, e);
                }
                courseRepo.save(bc);
                e.setState("已发布");
                repo.save(e);
                successCount++;
            } catch (Exception ex) {
                failCount++;
            }
        }
        return ApiResponse.ok(new BatchPublishResult(successCount, failCount));
    }

    @GetMapping("/public-course/applications")
    public ApiResponse<?> list(@RequestParam(name = "userId", required = false) Integer queryUserId) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        List<PublicCourseApplicationEntity> entities;
        if (queryUserId != null) {
            entities = repo.findByUserIdOrderByCreatedAtDesc(queryUserId);
        } else {
            entities = repo.findAllByOrderByCreatedAtDesc();
        }
        Map<Integer, String> userNameById = resolveUserNames(entities);
        List<ApplicationItem> list = entities.stream().map(x -> toApplicationItem(x, userNameById)).toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/public-course/applications/page")
    public ApiResponse<?> listPage(
        @RequestParam(name = "userId", required = false) Integer queryUserId,
        @RequestParam(name = "scope", required = false) String scope,
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "state", required = false) String state,
        @RequestParam(name = "page", required = false, defaultValue = "1") int page,
        @RequestParam(name = "page_size", required = false, defaultValue = "10") int pageSize
    ) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String st = state == null ? "" : state.trim();
        boolean publishScope = scope != null && "publish".equalsIgnoreCase(scope.trim());

        List<PublicCourseApplicationEntity> entities;
        if (queryUserId != null) {
            entities = repo.findByUserIdOrderByCreatedAtDesc(queryUserId);
        } else {
            entities = repo.findAllByOrderByCreatedAtDesc();
        }

        Map<Integer, String> userNameById = resolveUserNames(entities);
        List<ApplicationItem> filtered = new ArrayList<>();
        for (PublicCourseApplicationEntity x : entities) {
            String rawState = safe(x.getState());
            if (publishScope && !"已通过".equals(rawState) && !"已发布".equals(rawState)) continue;
            if (!stateMatches(st, rawState)) continue;
            if (!kw.isBlank()) {
                String hay = (safe(x.getCourseName()) + " " + safe(x.getTopic()) + " " + safe(x.getDepartment()) + " " + safe(x.getClassName()) + " " + safe(x.getOffice()) + " " + safe(x.getLocation()))
                    .toLowerCase();
                if (!hay.contains(kw)) continue;
            }
            filtered.add(toApplicationItem(x, userNameById));
        }

        long total = filtered.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= filtered.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(filtered.size(), fromIndex + safePageSize);
        return ApiResponse.ok(new PageWrapper<>(filtered.subList(fromIndex, toIndex), total));
    }

    @GetMapping("/public-course/my-applications")
    public ApiResponse<?> myList() {
        UserEntity user = currentUserService.requireUser();
        List<PublicCourseApplicationEntity> entities = repo.findByUserIdOrderByCreatedAtDesc(user.getId());
        Map<Integer, String> userNameById = resolveUserNames(entities);
        List<ApplicationItem> list = entities.stream().map(x -> toApplicationItem(x, userNameById)).toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/public-course/my-applications/page")
    public ApiResponse<?> myListPage(
        @RequestParam(name = "keyword", required = false) String keyword,
        @RequestParam(name = "state", required = false) String state,
        @RequestParam(name = "page", required = false, defaultValue = "1") int page,
        @RequestParam(name = "page_size", required = false, defaultValue = "10") int pageSize
    ) {
        UserEntity user = currentUserService.requireUser();
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int safePage = Math.max(1, page);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String st = state == null ? "" : state.trim();

        List<PublicCourseApplicationEntity> entities = repo.findByUserIdOrderByCreatedAtDesc(user.getId());
        Map<Integer, String> userNameById = resolveUserNames(entities);
        List<ApplicationItem> filtered = new ArrayList<>();
        for (PublicCourseApplicationEntity x : entities) {
            String rawState = safe(x.getState());
            if (!stateMatches(st, rawState)) continue;
            if (!kw.isBlank()) {
                String hay = (safe(x.getCourseName()) + " " + safe(x.getTopic()) + " " + safe(x.getDepartment()) + " " + safe(x.getClassName()) + " " + safe(x.getOffice()) + " " + safe(x.getLocation()))
                    .toLowerCase();
                if (!hay.contains(kw)) continue;
            }
            filtered.add(toApplicationItem(x, userNameById));
        }

        long total = filtered.size();
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= filtered.size()) {
            return ApiResponse.ok(new PageWrapper<>(List.of(), total));
        }
        int toIndex = Math.min(filtered.size(), fromIndex + safePageSize);
        return ApiResponse.ok(new PageWrapper<>(filtered.subList(fromIndex, toIndex), total));
    }

    @GetMapping("/public-course/my-applications/detail")
    public ApiResponse<?> myDetail(@RequestParam(name = "id") String id) {
        UserEntity user = currentUserService.requireUser();
        String safeId = id == null ? "" : id.trim();
        if (safeId.isBlank()) return ApiResponse.fail("缺少申请 id");
        PublicCourseApplicationEntity e = repo.findById(safeId).orElse(null);
        if (e == null) return ApiResponse.fail("申请不存在");
        int ownerId = e.getUserId() == null ? 0 : e.getUserId();
        if (ownerId != user.getId() && !adminService.isAdmin(user)) {
            return ApiResponse.fail("无权限");
        }
        String rawState = safe(e.getState());
        String userName = resolveUserName(ownerId);
        int[] stats = getEnrolledAndCheckedIn(e.getId());
        return ApiResponse.ok(new ApplicationItem(
            safe(e.getId()),
            safe(e.getCategory()),
            safe(e.getAcademicYear()),
            safe(e.getTerm()),
            safe(e.getDepartment()),
            safe(e.getClassName()),
            safe(e.getOffice()),
            safe(e.getLocation()),
            safe(e.getCourseName()),
            safe(e.getTopic()),
            safe(e.getStartDate()),
            e.getWeekNo(),
            safe(e.getWeekday()),
            safe(e.getSection()),
            safe(e.getApplyReason()),
            rawState,
            safe(e.getCreatedAtStr()),
            ownerId,
            userName,
            safe(e.getAuditOpinion()),
            safe(e.getAuditedAtStr()),
            stats[0],
            stats[1]
        ));
    }

    private Map<Integer, String> resolveUserNames(Collection<PublicCourseApplicationEntity> entities) {
        Set<Integer> ids = new HashSet<>();
        for (PublicCourseApplicationEntity e : entities) {
            Integer uid = e.getUserId();
            if (uid != null && uid > 0) ids.add(uid);
        }
        Map<Integer, String> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        for (UserEntity u : userRepo.findAllById(ids)) {
            Integer id = u.getId();
            if (id == null) continue;
            map.put(id, safe(u.getUsername()));
        }
        return map;
    }

    private String resolveUserName(int userId) {
        if (userId <= 0) return "";
        return userRepo.findById(userId).map(x -> safe(x.getUsername())).orElse("");
    }

    private int[] getEnrolledAndCheckedIn(String courseId) {
        if (courseId == null || courseId.isBlank()) return new int[]{0, 0};
        try {
            var approvals = approvalRepo.findByCourseIdAndTitleAndState(courseId, BookingService.RESERVATION_TITLE, "已预约");
            int enrolled = approvals.size();
            var checkins = checkinRepo.findByCourseId(courseId);
            int checkedIn = checkins.size();
            return new int[]{enrolled, checkedIn};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    private ApplicationItem toApplicationItem(PublicCourseApplicationEntity x, Map<Integer, String> userNameById) {
        int[] stats = getEnrolledAndCheckedIn(x.getId());
        return new ApplicationItem(
            safe(x.getId()),
            safe(x.getCategory()),
            safe(x.getAcademicYear()),
            safe(x.getTerm()),
            safe(x.getDepartment()),
            safe(x.getClassName()),
            safe(x.getOffice()),
            safe(x.getLocation()),
            safe(x.getCourseName()),
            safe(x.getTopic()),
            safe(x.getStartDate()),
            x.getWeekNo(),
            safe(x.getWeekday()),
            safe(x.getSection()),
            safe(x.getApplyReason()),
            safe(x.getState()),
            safe(x.getCreatedAtStr()),
            x.getUserId() == null ? 0 : x.getUserId(),
            userNameById.getOrDefault(x.getUserId() == null ? 0 : x.getUserId(), ""),
            safe(x.getAuditOpinion()),
            safe(x.getAuditedAtStr()),
            stats[0],
            stats[1]
        );
    }

    public record PublishedCourseItem(
        String id,
        String courseName,
        String topic,
        String startDate,
        String department,
        String className,
        String location,
        String state
    ) {}

    @GetMapping("/courses/published")
    public ApiResponse<?> publishedCourses() {
        currentUserService.requireUser();
        List<PublishedCourseItem> list = repo.findAllByOrderByCreatedAtDesc().stream()
            .filter(x -> "已发布".equals(safe(x.getState())))
            .map(x -> new PublishedCourseItem(
                safe(x.getId()),
                safe(x.getCourseName()),
                safe(x.getTopic()),
                safe(x.getStartDate()),
                safe(x.getDepartment()),
                safe(x.getClassName()),
                safe(x.getLocation()),
                "已发布"
            ))
            .toList();
        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @PostMapping("/public-course/applications")
    public ApiResponse<?> create(@RequestBody(required = false) CreateRequest payload) {
        UserEntity user = currentUserService.requireUser();
        String category = payload == null ? "" : safe(payload.category()).trim();
        String academicYear = payload == null ? "" : safe(payload.academicYear()).trim();
        String term = payload == null ? "" : safe(payload.term()).trim();
        String department = payload == null ? "" : safe(payload.department()).trim();
        String className = payload == null ? "" : safe(payload.className()).trim();
        String office = payload == null ? "" : safe(payload.office()).trim();
        String location = payload == null ? "" : safe(payload.location()).trim();
        String courseName = payload == null ? "" : safe(payload.courseName()).trim();
        String topic = payload == null ? "" : safe(payload.topic()).trim();
        String startDate = payload == null ? "" : safe(payload.startDate()).trim();
        Integer weekNo = payload == null ? null : payload.weekNo();
        String weekday = payload == null ? "" : safe(payload.weekday()).trim();
        String section = payload == null ? "" : safe(payload.section()).trim();
        String applyReason = payload == null ? "" : safe(payload.applyReason()).trim();

        if (category.isBlank()) return ApiResponse.fail("公开课类别不能为空");
        if (academicYear.isBlank()) return ApiResponse.fail("开课学年不能为空");
        if (term.isBlank()) return ApiResponse.fail("开课学期不能为空");
        if (department.isBlank()) return ApiResponse.fail("开课系部不能为空");
        if (className.isBlank()) return ApiResponse.fail("开课班级不能为空");
        if (office.isBlank()) return ApiResponse.fail("教研室不能为空");
        if (location.isBlank()) return ApiResponse.fail("开课地点不能为空");
        if (courseName.isBlank()) return ApiResponse.fail("课程名称不能为空");
        if (topic.isBlank()) return ApiResponse.fail("课题不能为空");
        if (startDate.isBlank()) return ApiResponse.fail("开课时间不能为空");
        if (weekNo == null || weekNo < 1) return ApiResponse.fail("周次不能为空");
        if (weekday.isBlank()) return ApiResponse.fail("星期不能为空");
        if (section.isBlank()) return ApiResponse.fail("节次不能为空");
        if (applyReason.isBlank()) return ApiResponse.fail("申报理由不能为空");

        PublicCourseApplicationEntity e = new PublicCourseApplicationEntity();
        e.setId("pca_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        e.setCategory(category);
        e.setAcademicYear(academicYear);
        e.setTerm(term);
        e.setDepartment(department);
        e.setClassName(className);
        e.setOffice(office);
        e.setLocation(location);
        e.setCourseName(courseName);
        e.setTopic(topic);
        e.setStartDate(startDate);
        e.setWeekNo(weekNo);
        e.setWeekday(weekday);
        e.setSection(section);
        e.setApplyReason(applyReason);
        e.setState("待审批");
        e.setCreatedAtStr(nowStr());
        e.setUserId(user.getId());
        repo.save(e);
        return ApiResponse.ok(e.getId());
    }

    @PostMapping("/public-course/applications/update")
    public ApiResponse<?> update(@RequestBody(required = false) UpdateRequest payload) {
        UserEntity user = currentUserService.requireUser();
        try {
            String id = publicCourseApplicationService.updateApplication(
                user,
                payload == null ? null : payload.id(),
                payload == null ? null : payload.category(),
                payload == null ? null : payload.academicYear(),
                payload == null ? null : payload.term(),
                payload == null ? null : payload.department(),
                payload == null ? null : payload.className(),
                payload == null ? null : payload.office(),
                payload == null ? null : payload.location(),
                payload == null ? null : payload.courseName(),
                payload == null ? null : payload.topic(),
                payload == null ? null : payload.startDate(),
                payload == null ? null : payload.weekNo(),
                payload == null ? null : payload.weekday(),
                payload == null ? null : payload.section(),
                payload == null ? null : payload.applyReason()
            );
            return ApiResponse.ok(id);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("系统错误");
        }
    }

    @PostMapping("/public-course/applications/audit")
    public ApiResponse<?> audit(@RequestBody(required = false) AuditRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        String id = payload == null ? "" : safe(payload.id()).trim();
        String action = payload == null ? "" : safe(payload.action()).trim();
        String opinion = payload == null || payload.opinion() == null ? "" : payload.opinion().trim();
        if (id.isBlank()) return ApiResponse.fail("缺少申请 id");
        if (!"approve".equals(action) && !"reject".equals(action)) return ApiResponse.fail("非法操作");

        PublicCourseApplicationEntity e = repo.findById(id).orElse(null);
        if (e == null) return ApiResponse.fail("申请不存在");
        String currentState = safe(e.getState());
        if (!stateMatches("待审批", currentState)) return ApiResponse.fail("当前状态不可审核");

        boolean isApprove = "approve".equals(action);
        e.setState(isApprove ? "已通过" : "已拒绝");
        e.setAuditOpinion(opinion);
        e.setAuditedAtStr(nowStr());
        repo.save(e);

        return ApiResponse.ok(true);
    }

    public record PublishRequest(@NotBlank String id) {}

    @PostMapping("/public-course/applications/publish")
    public ApiResponse<?> publish(@RequestBody(required = false) PublishRequest payload) {
        UserEntity user = currentUserService.requireUser();
        adminService.requireAdmin(user);
        String id = payload == null ? "" : safe(payload.id()).trim();
        if (id.isBlank()) return ApiResponse.fail("缺少申请 id");
        PublicCourseApplicationEntity e = repo.findById(id).orElse(null);
        if (e == null) return ApiResponse.fail("申请不存在");

        String st = safe(e.getState());
        if ("已发布".equals(st)) return ApiResponse.ok(true);
        if (!"已通过".equals(st)) return ApiResponse.fail("只有已通过的申请才能发布");

        com.pythonadmin.domain.BookingCourseEntity bc = courseRepo.findById(e.getId()).orElse(null);
        if (bc == null) {
            bc = toBookingCourse(e);
        } else {
            applyToBookingCourse(bc, e);
        }
        courseRepo.save(bc);
        e.setState("已发布");
        repo.save(e);

        return ApiResponse.ok(true);
    }

    public record QrCodeInfo(String bookingUrl, String checkinUrl, String reviewUrl) {}

    @GetMapping("/public-course/applications/qrcodes")
    public ApiResponse<?> getQRCodes(@RequestParam("id") String id) {
        currentUserService.requireUser();
        if (id == null || id.isBlank()) return ApiResponse.fail("缺少申请 id");
        PublicCourseApplicationEntity e = repo.findById(id).orElse(null);
        if (e == null) return ApiResponse.fail("申请不存在");
        if (!"已发布".equals(safe(e.getState()))) {
            return ApiResponse.fail("请先发布后再查看二维码");
        }

        long timestamp = System.currentTimeMillis();
        String token = org.springframework.util.DigestUtils.md5DigestAsHex((id + "_" + timestamp + "_secret").getBytes());
        
        // In a real scenario, this domain would be configured in application.yml
        // For now, returning relative paths or a pseudo domain
        String bookingUrl = "https://h5.example.com/booking?courseId=" + id;
        String checkinUrl = "CHECKIN|" + id + "|" + timestamp + "|" + token;
        String reviewUrl = "https://h5.example.com/#/courses/" + id + "?tab=reviewing";

        return ApiResponse.ok(new QrCodeInfo(bookingUrl, checkinUrl, reviewUrl));
    }

    private com.pythonadmin.domain.BookingCourseEntity toBookingCourse(PublicCourseApplicationEntity e) {
        com.pythonadmin.domain.BookingCourseEntity bc = new com.pythonadmin.domain.BookingCourseEntity();
        bc.setId(e.getId());
        bc.setEnrolledCount(0);
        bc.setCapacity(50);
        applyToBookingCourse(bc, e);
        return bc;
    }

    private void applyToBookingCourse(com.pythonadmin.domain.BookingCourseEntity bc, PublicCourseApplicationEntity e) {
        String teacherName = "未知";
        if (e.getUserId() != null) {
            UserEntity publisher = userRepo.findById(e.getUserId()).orElse(null);
            if (publisher != null && publisher.getUsername() != null && !publisher.getUsername().trim().isBlank()) {
                teacherName = publisher.getUsername().trim();
            }
        }

        bc.setName(safe(e.getCourseName()));
        bc.setStartAt(buildStartAt(safe(e.getStartDate()), safe(e.getSection())));
        bc.setTeacherName(teacherName);
        bc.setAcademicYear(safe(e.getAcademicYear()));
        bc.setTerm(safe(e.getTerm()));
        bc.setDepartment(safe(e.getDepartment()));
        bc.setClassName(safe(e.getClassName()));
        bc.setOffice(safe(e.getOffice()));
        bc.setLocation(safe(e.getLocation()));
        bc.setCourse(safe(e.getCategory()));
        bc.setTopic(safe(e.getTopic()));
    }

    private static String buildStartAt(String startDate, String section) {
        String d = startDate == null ? "" : startDate.trim();
        if (d.isBlank()) return "";
        String time = sectionToStartTime(section);
        return d + " " + time;
    }

    private static String sectionToStartTime(String section) {
        String s = section == null ? "" : section.trim();
        if (s.equals("第一节")) return "08:00";
        if (s.equals("第二节")) return "10:00";
        if (s.equals("第三节")) return "14:00";
        if (s.equals("第四节")) return "16:00";
        if (s.equals("第五节")) return "19:00";
        if (s.equals("第六节")) return "20:00";
        return "08:00";
    }

    private static boolean stateMatches(String queryState, String rawState) {
        String q = queryState == null ? "" : queryState.trim();
        if (q.isBlank()) return true;
        String r = rawState == null ? "" : rawState.trim();
        if ("待审批".equals(q) || "pending".equalsIgnoreCase(q)) {
            return "待审批".equals(r) || "pending".equalsIgnoreCase(r);
        }
        if ("已通过".equals(q) || "approve".equalsIgnoreCase(q) || "同意".equals(q)) {
            return "已通过".equals(r) || "approve".equalsIgnoreCase(r) || "同意".equals(r);
        }
        if ("已拒绝".equals(q) || "reject".equalsIgnoreCase(q) || "驳回".equals(q)) {
            return "已拒绝".equals(r) || "reject".equalsIgnoreCase(r) || "驳回".equals(r);
        }
        if ("已发布".equals(q) || "published".equalsIgnoreCase(q)) {
            return "已发布".equals(r) || "published".equalsIgnoreCase(r);
        }
        return r.equals(q);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String nowStr() {
        Instant now = Instant.now();
        LocalDateTime dt = LocalDateTime.ofInstant(now, ZoneId.systemDefault());
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(dt);
    }
}
