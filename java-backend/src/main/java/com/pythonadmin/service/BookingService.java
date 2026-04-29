package com.pythonadmin.service;

import com.pythonadmin.domain.ApprovalEntity;
import com.pythonadmin.domain.ApprovalRepository;
import com.pythonadmin.domain.BookingCourseEntity;
import com.pythonadmin.domain.BookingCourseRepository;
import com.pythonadmin.domain.CourseCheckinRepository;
import com.pythonadmin.domain.MessageEntity;
import com.pythonadmin.domain.MessageRepository;
import com.pythonadmin.domain.PublicCourseApplicationEntity;
import com.pythonadmin.domain.PublicCourseApplicationRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    public static final String RESERVATION_TITLE = "课程预约";
    private static final DateTimeFormatter NOW_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingCourseRepository courseRepo;
    private final ApprovalRepository approvalRepo;
    private final MessageRepository messageRepo;
    private final PublicCourseApplicationRepository publicCourseApplicationRepo;
    private final CourseCheckinRepository checkinRepo;

    public BookingService(
        BookingCourseRepository courseRepo,
        ApprovalRepository approvalRepo,
        MessageRepository messageRepo,
        PublicCourseApplicationRepository publicCourseApplicationRepo,
        CourseCheckinRepository checkinRepo
    ) {
        this.courseRepo = courseRepo;
        this.approvalRepo = approvalRepo;
        this.messageRepo = messageRepo;
        this.publicCourseApplicationRepo = publicCourseApplicationRepo;
        this.checkinRepo = checkinRepo;
    }

    public record ReservationState(String reservationState, boolean canReserve, boolean canCancel) {}

    /**
     * 获取课程的当前已预约人数（从数据库查询状态为"已预约"的记录）
     */
    public int getEnrolledCount(String courseId) {
        if (courseId == null || courseId.isBlank()) return 0;
        try {
            List<ApprovalEntity> reservations = approvalRepo.findByCourseIdAndTitleAndState(
                courseId, RESERVATION_TITLE, "已预约");
            return reservations.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public ReservationState getReservationState(int userId, BookingCourseEntity course) {
        // 检查是否存在状态为"已预约"的记录
        List<ApprovalEntity> reservations = approvalRepo.findByUserIdAndCourseIdAndTitleAndState(
            userId,
            course.getId(),
            RESERVATION_TITLE,
            "已预约"
        );
        
        // 调试日志
        System.out.println("=== getReservationState ===");
        System.out.println("userId: " + userId + ", courseId: " + course.getId());
        System.out.println("RESERVATION_TITLE: " + RESERVATION_TITLE);
        System.out.println("Found reservations: " + reservations.size());
        for (ApprovalEntity r : reservations) {
            System.out.println("  - id: " + r.getId() + ", state: " + r.getState() + ", title: " + r.getTitle());
        }
        
        if (!reservations.isEmpty()) {
            // 检查是否已签到
            boolean checkedIn = !checkinRepo.findByUserIdAndCourseId(userId, course.getId()).isEmpty();
            // 未签到且课程未开始才能取消
            boolean canCancel = !checkedIn && isUpcoming(safe(course.getStartAt()));
            return new ReservationState("已预约", false, canCancel);
        }
        if (isPublicCourseNotPublished(course.getId())) {
            return new ReservationState("未发布", false, false);
        }
        // 从数据库实时查询已预约人数
        int enrolledCount = getEnrolledCount(course.getId());
        if (enrolledCount >= course.getCapacity()) {
            return new ReservationState("已约满", false, false);
        }
        return new ReservationState("未预约", true, false);
    }

    @Transactional
    public void reserve(int userId, String courseId) {
        BookingCourseEntity course = courseRepo.findById(courseId).orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        ReservationState state = getReservationState(userId, course);
        if ("已预约".equals(state.reservationState())) {
            throw new IllegalArgumentException("已预约");
        }
        if (isPublicCourseNotPublished(courseId)) {
            throw new IllegalArgumentException("课程尚未发布，暂不可预约");
        }
        // 从数据库实时查询已预约人数
        int enrolledCount = getEnrolledCount(courseId);
        if (enrolledCount >= course.getCapacity()) {
            throw new IllegalArgumentException("课程已满");
        }

        String nowStr = nowStr();
        ApprovalEntity approval = new ApprovalEntity();
        approval.setId("a_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        approval.setTitle(RESERVATION_TITLE);
        approval.setState("已预约");
        approval.setCreatedAtStr(nowStr);
        approval.setApplyReason("申请预约该课程");
        approval.setOfficeOpinion("");
        approval.setDepartmentOpinion("");
        approval.setCourseId(courseId);
        approval.setUserId(userId);

        MessageEntity message = new MessageEntity();
        message.setId("m_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        message.setTitle("预约申请已提交");
        message.setContent("你已提交《" + (course.getName() == null ? "" : course.getName().trim()) + "》的预约申请。");
        message.setCreatedAtStr(nowStr);
        message.setRead(false);
        message.setUserId(userId);

        courseRepo.save(course);
        approvalRepo.save(approval);
        messageRepo.save(message);
    }

    @Transactional
    public void cancel(int userId, String courseId) {
        BookingCourseEntity course = courseRepo.findById(courseId).orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        ReservationState state = getReservationState(userId, course);
        if (!"已预约".equals(state.reservationState())) {
            throw new IllegalArgumentException("暂无可取消的预约");
        }
        // 课程已开始或已签到不能取消
        if (!state.canCancel()) {
            throw new IllegalArgumentException("课程已开始或已签到，无法取消");
        }

        String nowStr = nowStr();
        
        // 找到原来的"已预约"记录，将状态改为"已取消"
        List<ApprovalEntity> existingReservations = approvalRepo.findByUserIdAndCourseIdAndTitleAndState(
            userId, courseId, RESERVATION_TITLE, "已预约");
        for (ApprovalEntity existing : existingReservations) {
            existing.setState("已取消");
            existing.setApplyReason("取消预约");
            approvalRepo.save(existing);
        }

        MessageEntity message = new MessageEntity();
        message.setId("m_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        message.setTitle("已取消预约");
        message.setContent("你已取消《" + (course.getName() == null ? "" : course.getName().trim()) + "》的预约。");
        message.setCreatedAtStr(nowStr);
        message.setRead(false);
        message.setUserId(userId);

        courseRepo.save(course);
        messageRepo.save(message);
    }

    private static String nowStr() {
        return LocalDateTime.now().format(NOW_FMT);
    }

    public boolean isPublicCourseNotPublished(String courseId) {
        if (courseId == null || !courseId.startsWith("pca_")) return false;
        PublicCourseApplicationEntity e = publicCourseApplicationRepo.findById(courseId).orElse(null);
        if (e == null) return true;
        return !"已发布".equals(e.getState());
    }

    // 判断课程是否未开始
    public static boolean isUpcoming(String startAt) {
        if (startAt == null || startAt.isBlank()) return false;
        try {
            LocalDateTime start = LocalDateTime.parse(startAt, NOW_FMT);
            return start.isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
