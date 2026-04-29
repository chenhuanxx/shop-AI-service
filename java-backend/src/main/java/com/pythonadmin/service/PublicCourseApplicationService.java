package com.pythonadmin.service;

import com.pythonadmin.domain.PublicCourseApplicationEntity;
import com.pythonadmin.domain.PublicCourseApplicationRepository;
import com.pythonadmin.domain.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicCourseApplicationService {
    private final PublicCourseApplicationRepository repo;
    private final AdminService adminService;

    public PublicCourseApplicationService(PublicCourseApplicationRepository repo, AdminService adminService) {
        this.repo = repo;
        this.adminService = adminService;
    }

    @Transactional
    public String updateApplication(
        UserEntity actor,
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
        Integer weekNo,
        String weekday,
        String section,
        String applyReason
    ) {
        if (actor == null || actor.getId() == null) {
            throw new IllegalArgumentException("用户信息异常");
        }

        String applicationId = safe(id).trim();
        if (applicationId.isBlank()) {
            throw new IllegalArgumentException("缺少申请 id");
        }

        PublicCourseApplicationEntity e = repo.findById(applicationId).orElse(null);
        if (e == null) {
            throw new IllegalArgumentException("申请不存在");
        }

        boolean isAdmin = adminService.isAdmin(actor);
        Integer ownerId = e.getUserId();
        if (!isAdmin && (ownerId == null || !actor.getId().equals(ownerId))) {
            throw new IllegalArgumentException("无权修改此申请");
        }

        String state = safe(e.getState());
        if ("已发布".equals(state)) {
            throw new IllegalArgumentException("当前状态不可修改");
        }
        if (!"已通过".equals(state) && !"已拒绝".equals(state)) {
            throw new IllegalArgumentException("只有已审批且未发布的课程才允许修改");
        }

        String cat = safe(category).trim();
        String ay = safe(academicYear).trim();
        String t = safe(term).trim();
        String dep = safe(department).trim();
        String cn = safe(className).trim();
        String off = safe(office).trim();
        String loc = safe(location).trim();
        String cName = safe(courseName).trim();
        String tp = safe(topic).trim();
        String sd = safe(startDate).trim();
        Integer wn = weekNo;
        String wd = safe(weekday).trim();
        String sec = safe(section).trim();
        String ar = safe(applyReason).trim();

        if (cat.isBlank()) throw new IllegalArgumentException("公开课类别不能为空");
        if (ay.isBlank()) throw new IllegalArgumentException("开课学年不能为空");
        if (t.isBlank()) throw new IllegalArgumentException("开课学期不能为空");
        if (dep.isBlank()) throw new IllegalArgumentException("开课系部不能为空");
        if (cn.isBlank()) throw new IllegalArgumentException("开课班级不能为空");
        if (off.isBlank()) throw new IllegalArgumentException("教研室不能为空");
        if (loc.isBlank()) throw new IllegalArgumentException("开课地点不能为空");
        if (cName.isBlank()) throw new IllegalArgumentException("课程名称不能为空");
        if (tp.isBlank()) throw new IllegalArgumentException("课题不能为空");
        if (sd.isBlank()) throw new IllegalArgumentException("开课时间不能为空");
        if (wn == null || wn < 1) throw new IllegalArgumentException("周次不能为空");
        if (wd.isBlank()) throw new IllegalArgumentException("星期不能为空");
        if (sec.isBlank()) throw new IllegalArgumentException("节次不能为空");
        if (ar.isBlank()) throw new IllegalArgumentException("申报理由不能为空");

        e.setCategory(cat);
        e.setAcademicYear(ay);
        e.setTerm(t);
        e.setDepartment(dep);
        e.setClassName(cn);
        e.setOffice(off);
        e.setLocation(loc);
        e.setCourseName(cName);
        e.setTopic(tp);
        e.setStartDate(sd);
        e.setWeekNo(wn);
        e.setWeekday(wd);
        e.setSection(sec);
        e.setApplyReason(ar);

        e.setState("待审批");
        e.setAuditOpinion("");
        e.setAuditedAtStr("");

        repo.save(e);
        return e.getId();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
