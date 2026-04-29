package com.pythonadmin.api;

import com.pythonadmin.common.ApiResponse;
import com.pythonadmin.domain.ApprovalEntity;
import com.pythonadmin.domain.ApprovalRepository;
import com.pythonadmin.domain.BookingCourseEntity;
import com.pythonadmin.domain.BookingCourseRepository;
import com.pythonadmin.domain.CourseCheckinEntity;
import com.pythonadmin.domain.CourseCheckinRepository;
import com.pythonadmin.domain.PublicCourseApplicationEntity;
import com.pythonadmin.domain.PublicCourseApplicationRepository;
import com.pythonadmin.domain.PublicCourseEntity;
import com.pythonadmin.domain.PublicCourseRepository;
import com.pythonadmin.domain.PublicCourseReviewEntity;
import com.pythonadmin.domain.PublicCourseReviewRepository;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.service.CurrentUserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicCoursesController {
    private final CurrentUserService currentUserService;
    private final PublicCourseApplicationRepository applicationRepo;
    private final ApprovalRepository approvalRepo;
    private final CourseCheckinRepository checkinRepo;
    private final PublicCourseReviewRepository reviewRepo;
    private final BookingCourseRepository bookingCourseRepo;
    private final PublicCourseRepository legacyRepo;

    public PublicCoursesController(
        CurrentUserService currentUserService,
        PublicCourseApplicationRepository applicationRepo,
        ApprovalRepository approvalRepo,
        CourseCheckinRepository checkinRepo,
        PublicCourseReviewRepository reviewRepo,
        BookingCourseRepository bookingCourseRepo,
        PublicCourseRepository legacyRepo
    ) {
        this.currentUserService = currentUserService;
        this.applicationRepo = applicationRepo;
        this.approvalRepo = approvalRepo;
        this.checkinRepo = checkinRepo;
        this.reviewRepo = reviewRepo;
        this.bookingCourseRepo = bookingCourseRepo;
        this.legacyRepo = legacyRepo;
    }

    public record PublicCourseItem(String id, String title, String time, String speaker, String status) {}
    public record CourseDetailInfo(
        String id, String name, String teacherName, int capacity,
        String academicYear, String term, String department, String className,
        String office, String location, String course, String topic,
        String startAt, String status
    ) {}
    public record ListWrapper<T>(List<T> list) {}
    public record ReviewItem(
        long id,
        int user_id,
        String content,
        String created_at
    ) {}

    public record DetailResponse(CourseDetailInfo course, List<ReviewItem> reviews, boolean hasCheckedIn, boolean hasReviewed) {}
    public record ReviewRequest(String id, String content) {}
    public record OkWrapper(boolean ok) {}

    @GetMapping("/courses/public")
    public ApiResponse<?> list(@RequestParam(name = "tab", defaultValue = "teaching") String tab) {
        UserEntity user = currentUserService.requireUser();
        String t = (tab == null ? "teaching" : tab.trim());
        if (t.isBlank()) t = "teaching";

        List<PublicCourseItem> list = new ArrayList<>();

        if ("teaching".equals(t)) {
            List<PublicCourseApplicationEntity> apps = applicationRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
            for (PublicCourseApplicationEntity app : apps) {
                list.add(new PublicCourseItem(
                    app.getId(),
                    app.getCourseName() == null ? "未知课程" : app.getCourseName(),
                    (app.getStartDate() == null ? "" : app.getStartDate()) + " " + (app.getSection() == null ? "" : app.getSection()),
                    user.getUsername(),
                    app.getState() == null ? "待审批" : app.getState()
                ));
            }
        } else if ("learning".equals(t)) {
            List<ApprovalEntity> approvals = approvalRepo.findByUserIdAndTitleAndState(user.getId(), "课程预约", "已预约");
            Set<String> checkedInCourseIds = checkinRepo.findByUserId(user.getId()).stream()
                .map(CourseCheckinEntity::getCourseId)
                .collect(Collectors.toSet());

            for (ApprovalEntity app : approvals) {
                if (app.getCourseId() == null) continue;
                if (checkedInCourseIds.contains(app.getCourseId())) continue;

                BookingCourseEntity bc = bookingCourseRepo.findById(app.getCourseId()).orElse(null);
                if (bc != null) {
                    list.add(new PublicCourseItem(
                        bc.getId(),
                        bc.getName() == null ? "未知课程" : bc.getName(),
                        bc.getStartAt() == null ? "" : bc.getStartAt(),
                        bc.getTeacherName() == null ? "未知" : bc.getTeacherName(),
                        "已预约未签到"
                    ));
                }
            }
        } else if ("reviewing".equals(t)) {
            List<CourseCheckinEntity> checkins = checkinRepo.findByUserId(user.getId());
            Set<String> reviewedCourseIds = reviewRepo.findByUserId(user.getId()).stream()
                .map(PublicCourseReviewEntity::getCourseId)
                .collect(Collectors.toSet());

            for (CourseCheckinEntity checkin : checkins) {
                if (checkin.getCourseId() == null) continue;
                if (reviewedCourseIds.contains(checkin.getCourseId())) continue;

                BookingCourseEntity bc = bookingCourseRepo.findById(checkin.getCourseId()).orElse(null);
                if (bc != null) {
                    list.add(new PublicCourseItem(
                        bc.getId(),
                        bc.getName() == null ? "未知课程" : bc.getName(),
                        bc.getStartAt() == null ? "" : bc.getStartAt(),
                        bc.getTeacherName() == null ? "未知" : bc.getTeacherName(),
                        "已签到待评价"
                    ));
                }
            }

            Set<String> reviewedBookingCourseIds = reviewedCourseIds.stream()
                .filter(courseId -> courseId != null && !courseId.isBlank())
                .collect(Collectors.toSet());

            for (String courseId : reviewedBookingCourseIds) {
                BookingCourseEntity bc = bookingCourseRepo.findById(courseId).orElse(null);
                if (bc != null) {
                    list.add(new PublicCourseItem(
                        bc.getId(),
                        bc.getName() == null ? "未知课程" : bc.getName(),
                        bc.getStartAt() == null ? "" : bc.getStartAt(),
                        bc.getTeacherName() == null ? "未知" : bc.getTeacherName(),
                        "已评价"
                    ));
                }
            }
        } else if ("reviewed".equals(t)) {
            Set<String> reviewedCourseIds = reviewRepo.findByUserId(user.getId()).stream()
                .map(PublicCourseReviewEntity::getCourseId)
                .filter(courseId -> courseId != null && !courseId.isBlank())
                .collect(Collectors.toSet());

            for (String courseId : reviewedCourseIds) {
                BookingCourseEntity bc = bookingCourseRepo.findById(courseId).orElse(null);
                if (bc != null) {
                    list.add(new PublicCourseItem(
                        bc.getId(),
                        bc.getName() == null ? "未知课程" : bc.getName(),
                        bc.getStartAt() == null ? "" : bc.getStartAt(),
                        bc.getTeacherName() == null ? "未知" : bc.getTeacherName(),
                        "已评价"
                    ));
                }
            }
        } else {
            // fallback to legacy or do nothing
            List<PublicCourseEntity> items = legacyRepo.findByTabOrderByTimeDesc(t);
            list = items.stream()
                .map(x -> new PublicCourseItem(x.getId(), x.getTitle(), x.getTime(), x.getSpeaker(), x.getStatus()))
                .collect(Collectors.toList());
        }

        return ApiResponse.ok(new ListWrapper<>(list));
    }

    @GetMapping("/courses/public/detail")
    public ApiResponse<?> detail(@RequestParam(name = "id", required = false) String id) {
        UserEntity user = currentUserService.requireUser();
        String courseId = id == null ? "" : id.trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }

        CourseDetailInfo course = null;
        BookingCourseEntity bc = bookingCourseRepo.findById(courseId).orElse(null);
        if (bc != null) {
            course = new CourseDetailInfo(
                bc.getId(), bc.getName(), bc.getTeacherName(), bc.getCapacity(),
                bc.getAcademicYear(), bc.getTerm(), bc.getDepartment(), bc.getClassName(),
                bc.getOffice(), bc.getLocation(), bc.getCourse(), bc.getTopic(),
                bc.getStartAt(), "已发布"
            );
        } else {
            PublicCourseApplicationEntity app = applicationRepo.findById(courseId).orElse(null);
            if (app != null) {
                course = new CourseDetailInfo(
                    app.getId(), app.getCourseName(), "我", 0,
                    "", "", "", "",
                    "", "", "", "",
                    app.getStartDate() + " " + app.getSection(), app.getState()
                );
            } else {
                PublicCourseEntity legacy = legacyRepo.findById(courseId).orElse(null);
                if (legacy != null) {
                    course = new CourseDetailInfo(
                        legacy.getId(), legacy.getTitle(), legacy.getSpeaker(), 0,
                        "", "", "", "",
                        "", "", "", "",
                        legacy.getTime(), legacy.getStatus()
                    );
                }
            }
        }

        if (course == null) {
            return ApiResponse.fail("公开课不存在");
        }

        List<ReviewItem> reviews = reviewRepo.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
            .map(r -> new ReviewItem(
                r.getId() == null ? 0L : r.getId(),
                r.getUserId() == null ? 0 : r.getUserId(),
                r.getContent() == null ? "" : r.getContent(),
                r.getCreatedAt() == null ? "" : r.getCreatedAt().toString()
            ))
            .toList();

        boolean hasCheckedIn = false;
        if (user != null) {
            hasCheckedIn = checkinRepo.findByUserId(user.getId()).stream()
                .anyMatch(c -> courseId.equals(c.getCourseId()));
        }

        boolean hasReviewed = false;
        if (user != null) {
            hasReviewed = !reviewRepo.findByUserIdAndCourseId(user.getId(), courseId).isEmpty();
        }

        return ApiResponse.ok(new DetailResponse(course, reviews, hasCheckedIn, hasReviewed));
    }

    @PostMapping("/courses/public/review")
    public ApiResponse<?> review(@RequestBody(required = false) ReviewRequest payload) {
        UserEntity user = currentUserService.requireUser();
        String courseId = payload == null || payload.id() == null ? "" : payload.id().trim();
        String content = payload == null || payload.content() == null ? "" : payload.content().trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        if (content.isBlank()) {
            return ApiResponse.fail("评课内容不能为空");
        }

        PublicCourseReviewEntity r = new PublicCourseReviewEntity();
        r.setCourseId(courseId);
        r.setUserId(user.getId() == null ? 0 : user.getId());
        r.setContent(content);
        reviewRepo.save(r);
        return ApiResponse.ok(new OkWrapper(true));
    }

    public record CheckinRequest(String courseId) {}
    public record QrCheckinRequest(String qr) {}

    @PostMapping("/public-course/applications/qrcodes/checkin")
    public ApiResponse<?> checkin(@RequestBody(required = false) CheckinRequest request) {
        return manualCheckin(request);
    }

    @PostMapping("/courses/public/checkin")
    public ApiResponse<?> manualCheckin(@RequestBody(required = false) CheckinRequest request) {
        UserEntity user = currentUserService.requireUser();
        String courseId = request == null || request.courseId() == null ? "" : request.courseId().trim();
        if (courseId.isBlank()) {
            return ApiResponse.fail("缺少课程 id");
        }
        
        boolean alreadyCheckedIn = checkinRepo.findByUserId(user.getId()).stream()
            .anyMatch(c -> courseId.equals(c.getCourseId()));
            
        if (alreadyCheckedIn) {
            return ApiResponse.fail("已经签到过了");
        }
        
        CourseCheckinEntity checkin = new CourseCheckinEntity();
        checkin.setUserId(user.getId());
        checkin.setCourseId(courseId);
        checkinRepo.save(checkin);
        return ApiResponse.ok(new OkWrapper(true));
    }

    @PostMapping("/courses/public/checkin-by-qr")
    public ApiResponse<?> checkinByQr(@RequestBody(required = false) QrCheckinRequest payload) {
        currentUserService.requireUser();
        String qr = payload == null || payload.qr() == null ? "" : payload.qr().trim();
        if (qr.isBlank()) {
            return ApiResponse.fail("缺少二维码内容");
        }
        String[] parts = qr.split("\\|");
        if (parts.length != 4 || !"CHECKIN".equals(parts[0])) {
            return ApiResponse.fail("二维码无效");
        }
        String courseId = parts[1] == null ? "" : parts[1].trim();
        String timestampStr = parts[2] == null ? "" : parts[2].trim();
        String token = parts[3] == null ? "" : parts[3].trim();
        if (courseId.isBlank() || timestampStr.isBlank() || token.isBlank()) {
            return ApiResponse.fail("二维码无效");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (Exception e) {
            return ApiResponse.fail("二维码无效");
        }

        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > 10 * 60 * 1000L) {
            return ApiResponse.fail("二维码已过期");
        }
        String expected = DigestUtils.md5DigestAsHex((courseId + "_" + timestamp + "_secret").getBytes());
        if (!expected.equalsIgnoreCase(token)) {
            return ApiResponse.fail("二维码无效");
        }

        return manualCheckin(new CheckinRequest(courseId));
    }
}
