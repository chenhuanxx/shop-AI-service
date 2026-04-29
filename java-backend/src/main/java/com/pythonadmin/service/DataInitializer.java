package com.pythonadmin.service;

import com.pythonadmin.domain.ApprovalEntity;
import com.pythonadmin.domain.ApprovalRepository;
import com.pythonadmin.domain.BookingCourseEntity;
import com.pythonadmin.domain.BookingCourseRepository;
import com.pythonadmin.domain.MessageEntity;
import com.pythonadmin.domain.MessageRepository;
import com.pythonadmin.domain.PublicCourseEntity;
import com.pythonadmin.domain.PublicCourseRepository;
import com.pythonadmin.domain.RoleEntity;
import com.pythonadmin.domain.RoleRepository;
import com.pythonadmin.domain.UserEntity;
import com.pythonadmin.domain.UserRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final BookingCourseRepository bookingCourseRepo;
    private final PublicCourseRepository publicCourseRepo;
    private final ApprovalRepository approvalRepo;
    private final MessageRepository messageRepo;
    private final RoleRepository roleRepo;

    public DataInitializer(
        UserRepository userRepo,
        PasswordEncoder passwordEncoder,
        BookingCourseRepository bookingCourseRepo,
        PublicCourseRepository publicCourseRepo,
        ApprovalRepository approvalRepo,
        MessageRepository messageRepo,
        RoleRepository roleRepo
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.bookingCourseRepo = bookingCourseRepo;
        this.publicCourseRepo = publicCourseRepo;
        this.approvalRepo = approvalRepo;
        this.messageRepo = messageRepo;
        this.roleRepo = roleRepo;
    }

    @Override
    public void run(String... args) {
        UserEntity admin = userRepo.findByUsername("admin").orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setUsername("admin");
            u.setHashedPassword(passwordEncoder.encode("admin123"));
            u.setActive(true);
            u.setRole("admin");
            return userRepo.save(u);
        });

        UserEntity user = userRepo.findByUsername("user").orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setUsername("user");
            u.setHashedPassword(passwordEncoder.encode("123456"));
            u.setActive(true);
            u.setRole("user");
            return userRepo.save(u);
        });

        if (admin.getRole() == null || admin.getRole().trim().isBlank()) {
            admin.setRole("admin");
            userRepo.save(admin);
        }
        if (user.getRole() == null || user.getRole().trim().isBlank()) {
            user.setRole("user");
            userRepo.save(user);
        }

        if (roleRepo.count() == 0) {
            roleRepo.saveAll(List.of(
                role("r_admin", "admin", "管理员：管理端功能"),
                role("r_user", "user", "普通用户：仅可查看和提交申请")
            ));
        }

        if (bookingCourseRepo.count() == 0) {
            bookingCourseRepo.saveAll(List.of(
                bookingCourse(
                    "c_1001",
                    "课堂管理与教学设计",
                    "2026-03-22 09:00",
                    "张老师",
                    18,
                    30,
                    "2025-2026",
                    "第二学期",
                    "教育学院",
                    "2024 级 教育学 1 班",
                    "教学研究室一",
                    "A-101",
                    "教育学基础",
                    "课堂组织与课堂管理"
                ),
                bookingCourse(
                    "c_1002",
                    "AI 辅助备课实战",
                    "2026-03-23 14:00",
                    "李老师",
                    46,
                    60,
                    "2025-2026",
                    "第二学期",
                    "信息工程学院",
                    "2023 级 计算机 2 班",
                    "教学研究室二",
                    "B-305",
                    "教学信息化",
                    "AI 工具在备课中的应用"
                ),
                bookingCourse(
                    "c_1003",
                    "课程评价体系与量表",
                    "2026-03-24 19:30",
                    "王老师",
                    27,
                    40,
                    "2025-2026",
                    "第二学期",
                    "教育学院",
                    "2024 级 教育学 2 班",
                    "教学研究室三",
                    "C-208",
                    "教育评价",
                    "量表设计与课堂评价"
                )
            ));
        }

        if (publicCourseRepo.count() == 0) {
            publicCourseRepo.saveAll(List.of(
                publicCourse("p_t_1", "teaching", "我开课：教学观摩课（示例）", "2026-03-25 10:00", "我", "进行中"),
                publicCourse("p_t_2", "teaching", "我开课：课堂提问设计（示例）", "2026-03-29 15:00", "我", "未开始"),
                publicCourse("p_l_1", "learning", "我听课：信息化课堂（示例）", "2026-03-21 09:30", "赵老师", "已结束"),
                publicCourse("p_l_2", "learning", "我听课：班级管理（示例）", "2026-03-28 13:30", "钱老师", "未开始"),
                publicCourse("p_r_1", "reviewing", "我评课：公开课评课（示例）", "2026-03-20 16:00", "孙老师", "待提交")
            ));
        }

        if (approvalRepo.count() == 0) {
            approvalRepo.saveAll(List.of(
                approval(
                    "a_1",
                    "公开课申报审批",
                    "待审批",
                    "2026-03-20 11:20",
                    "拟开展公开课示范教学，提升教研交流效果。",
                    "建议通过，注意课堂互动设计。",
                    "",
                    "c_1002",
                    user.getId()
                ),
                approval(
                    "a_2",
                    "公开课申报审批",
                    "已通过",
                    "2026-03-18 09:05",
                    "公开课教学展示与经验分享。",
                    "同意，建议补充课堂评价量表。",
                    "已审核通过。",
                    "c_1001",
                    user.getId()
                )
            ));
        }

        if (messageRepo.count() == 0) {
            messageRepo.saveAll(List.of(
                message(
                    "m_1",
                    "审批提醒",
                    "你有一条新的审批待处理。",
                    "2026-03-20 11:20",
                    false,
                    user.getId()
                ),
                message(
                    "m_2",
                    "审批结果",
                    "你的审批已通过。",
                    "2026-03-18 09:05",
                    true,
                    user.getId()
                )
            ));
        }

        if (admin.getId() == null) {
            throw new IllegalStateException("admin seed failed");
        }
    }

    private static BookingCourseEntity bookingCourse(
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
        String topic
    ) {
        BookingCourseEntity c = new BookingCourseEntity();
        c.setId(id);
        c.setName(name);
        c.setStartAt(startAt);
        c.setTeacherName(teacherName);
        c.setEnrolledCount(enrolledCount);
        c.setCapacity(capacity);
        c.setAcademicYear(academicYear);
        c.setTerm(term);
        c.setDepartment(department);
        c.setClassName(className);
        c.setOffice(office);
        c.setLocation(location);
        c.setCourse(course);
        c.setTopic(topic);
        return c;
    }

    private static RoleEntity role(String id, String name, String description) {
        RoleEntity r = new RoleEntity();
        r.setId(id);
        r.setName(name);
        r.setDescription(description);
        return r;
    }

    private static PublicCourseEntity publicCourse(
        String id,
        String tab,
        String title,
        String time,
        String speaker,
        String status
    ) {
        PublicCourseEntity p = new PublicCourseEntity();
        p.setId(id);
        p.setTab(tab);
        p.setTitle(title);
        p.setTime(time);
        p.setSpeaker(speaker);
        p.setStatus(status);
        return p;
    }

    private static ApprovalEntity approval(
        String id,
        String title,
        String state,
        String createdAtStr,
        String applyReason,
        String officeOpinion,
        String departmentOpinion,
        String courseId,
        Integer userId
    ) {
        ApprovalEntity a = new ApprovalEntity();
        a.setId(id);
        a.setTitle(title);
        a.setState(state);
        a.setCreatedAtStr(createdAtStr);
        a.setApplyReason(applyReason);
        a.setOfficeOpinion(officeOpinion);
        a.setDepartmentOpinion(departmentOpinion);
        a.setCourseId(courseId);
        a.setUserId(userId);
        return a;
    }

    private static MessageEntity message(
        String id,
        String title,
        String content,
        String createdAtStr,
        boolean read,
        Integer userId
    ) {
        MessageEntity m = new MessageEntity();
        m.setId(id);
        m.setTitle(title);
        m.setContent(content);
        m.setCreatedAtStr(createdAtStr);
        m.setRead(read);
        m.setUserId(userId);
        return m;
    }
}
