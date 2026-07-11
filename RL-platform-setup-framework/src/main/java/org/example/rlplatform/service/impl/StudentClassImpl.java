package org.example.rlplatform.service.impl;

import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.example.rlplatform.Repository.ClassGroupPlanMemberRepository;
import org.example.rlplatform.Repository.ClassGroupPlanRepository;
import org.example.rlplatform.Repository.StudentClassRepository;
import org.example.rlplatform.Repository.UserRepository;
import org.example.rlplatform.entity.*;
import org.example.rlplatform.service.StudentClassService;
import org.example.rlplatform.utils.Md5Util;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.criteria.Predicate;

import static java.time.LocalDateTime.now;

@Service
public class StudentClassImpl implements StudentClassService {

    @Autowired
    private StudentClassRepository studentClassRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClassGroupPlanRepository classGroupPlanRepository;
    @Autowired
    private ClassGroupPlanMemberRepository classGroupPlanMemberRepository;

    private static final String DEFAULT_STUDENT_PASSWORD = "Abcd1234";
    private static final DateTimeFormatter PLAN_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public StudentClass findByName(String name) {
        return studentClassRepository.findByName(name);
    }

    @Override
    public StudentClass findByIdAndIsDeletedFalse(Integer id) {
        return getByIdAndNotDeleted(id);
    }

    @Override
    public StudentClass findByCodeAndIsDeletedFalse(String code) {
        return studentClassRepository.findByCodeAndIsDeletedFalse(code);
    }

    @Override
    @Transactional
    public Map<String, Object> importStudents(Integer classId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请上传 Excel 文件");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls")) {
            throw new RuntimeException("仅支持 .xlsx / .xls 文件");
        }

        StudentClass studentClass = getByIdAndNotDeleted(classId);
        Integer teacherId = currentUserId();

        int createdCount = 0;
        int existingCount = 0;
        int joinedCount = 0;
        List<Map<String, Object>> failedRows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ClassGroupPlanMember> pendingMembers = new ArrayList<>();
        Map<String, Integer> groupOrderMap = new HashMap<>();
        boolean hasGroupNoColumn;

        try (InputStream in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new RuntimeException("Excel 为空或缺少数据行");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            Map<String, Integer> headerIndex = resolveHeaderIndex(header, formatter);
            Integer nameCol = firstHeader(headerIndex, "name", "姓名");
            Integer studentNoCol = firstHeader(headerIndex, "studentno", "student_no", "学号");
            Integer groupNoCol = firstHeader(headerIndex, "groupno", "group_no", "组号");
            hasGroupNoColumn = groupNoCol != null;
            if (nameCol == null) {
                throw new RuntimeException("Excel 缺少姓名列");
            }
            if (studentNoCol == null) {
                throw new RuntimeException("Excel 缺少学号列");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String name = readCell(row, nameCol, formatter);
                String studentNo = readCell(row, studentNoCol, formatter);
                String groupNo = groupNoCol == null ? "" : readCell(row, groupNoCol, formatter);
                if (name.isBlank() && studentNo.isBlank() && groupNo.isBlank()) {
                    continue;
                }
                if (name.isBlank() || studentNo.isBlank()) {
                    failedRows.add(rowFailure(i + 1, "姓名或学号为空"));
                    continue;
                }

                String email = studentNo + "@bjtu.edu.cn";
                User user = userRepository.findByEmail(email);
                boolean created = false;
                if (user == null) {
                    user = new User();
                    user.setUsername(name);
                    user.setNickname(name);
                    user.setEmail(email);
                    user.setStudentNo(studentNo);
                    user.setRole(UserRole.STUDENT);
                    user.setPassword(Md5Util.getMD5String(DEFAULT_STUDENT_PASSWORD));
                    user.setStudentClass(studentClass);
                    user.setCreateTime(LocalDateTime.now());
                    user.setUpdateTime(LocalDateTime.now());
                    user.setIsDeleted(false);
                    user = userRepository.save(user);
                    createdCount++;
                    joinedCount++;
                    created = true;
                } else {
                    existingCount++;
                    if (Boolean.TRUE.equals(user.getIsDeleted())) {
                        failedRows.add(rowFailure(i + 1, "账号已被删除，邮箱：" + email));
                        continue;
                    }
                    if (user.getUsername() != null && !user.getUsername().isBlank() && !user.getUsername().equals(name)) {
                        warnings.add("第 " + (i + 1) + " 行邮箱已存在，但姓名不同：" + email);
                    }
                    if (user.getStudentNo() == null || user.getStudentNo().isBlank()) {
                        user.setStudentNo(studentNo);
                    }
                    if (user.getStudentClass() == null) {
                        user.setStudentClass(studentClass);
                        user.setUpdateTime(LocalDateTime.now());
                        userRepository.save(user);
                        joinedCount++;
                    } else if (!user.getStudentClass().getId().equals(classId)) {
                        failedRows.add(rowFailure(i + 1, "账号已属于其他班级，未强制移动：" + email));
                        continue;
                    }
                }

                if (hasGroupNoColumn && !groupNo.isBlank()) {
                    int order = groupOrderMap.merge(groupNo, 1, Integer::sum);
                    ClassGroupPlanMember member = new ClassGroupPlanMember();
                    member.setGroupNo(groupNo);
                    member.setStudentId(user.getId());
                    member.setStudentNameSnapshot(created ? name : (user.getUsername() == null ? name : user.getUsername()));
                    member.setStudentNoSnapshot(studentNo);
                    member.setMemberOrder(order);
                    pendingMembers.add(member);
                }
            }

            ClassGroupPlan plan = null;
            if (hasGroupNoColumn && !pendingMembers.isEmpty()) {
                plan = new ClassGroupPlan();
                plan.setClassId(classId);
                plan.setPlanName("分组导入 " + LocalDateTime.now().format(PLAN_TIME_FORMATTER));
                plan.setSourceFileName(filename);
                plan.setCreatedBy(teacherId);
                plan.setCreatedAt(LocalDateTime.now());
                plan.setActive(true);
                plan = classGroupPlanRepository.save(plan);
                for (ClassGroupPlanMember member : pendingMembers) {
                    member.setPlanId(plan.getId());
                }
                classGroupPlanMemberRepository.saveAll(pendingMembers);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("createdCount", createdCount);
            result.put("existingCount", existingCount);
            result.put("joinedCount", joinedCount);
            result.put("savedGroupPlan", plan != null);
            result.put("groupPlanId", plan == null ? null : plan.getId());
            result.put("groupPlanName", plan == null ? null : plan.getPlanName());
            result.put("failedRows", failedRows);
            result.put("warnings", warnings);
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Excel 导入失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> listGroupPlans(Integer classId) {
        getByIdAndNotDeleted(classId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ClassGroupPlan plan : classGroupPlanRepository.findByClassIdAndActiveTrueOrderByCreatedAtDesc(classId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", plan.getId());
            row.put("planName", plan.getPlanName());
            row.put("sourceFileName", plan.getSourceFileName());
            row.put("createdAt", plan.getCreatedAt() == null ? "" : plan.getCreatedAt().format(PLAN_TIME_FORMATTER));
            list.add(row);
        }
        return list;
    }

    @Override
    public void create(StudentClass studentClass) {
        studentClass.setIsDeleted(false);
        studentClass.setCreateTime(now());
        if (studentClass.getCode() == null || studentClass.getCode().isBlank()) {
            studentClass.setCode(generateClassCode());
        }
        studentClassRepository.save(studentClass);
    }

    @Override
    public void update(StudentClass studentClass, Integer id) {
        StudentClass sc = getByIdAndNotDeleted(id);
        sc.setName(studentClass.getName());
        sc.setCode(studentClass.getCode());
        studentClassRepository.save(sc);
    }

    @Override
    public void softDelete(Integer id) {
        StudentClass sc = getByIdAndNotDeleted(id);
        sc.setIsDeleted(true);
        studentClassRepository.save(sc);
    }

    @Override
    public Page<StudentClass> listPage(Integer pageNum, Integer pageSize, Boolean isDeleted) {
        Specification<StudentClass> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isDeleted != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDeleted"), isDeleted));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return studentClassRepository.findAll(spec, PageRequest.of(pageNum, pageSize));
    }

    private StudentClass getByIdAndNotDeleted(Integer id) {
        StudentClass sc = studentClassRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("班级不存在"));
        if (sc.getIsDeleted()) {
            throw new RuntimeException("班级已删除");
        }
        return sc;
    }

    private String generateClassCode() {
        String prefix = "RL" + LocalDate.now().getYear() + "A";

        StudentClass last = studentClassRepository
                .findTopByCodeStartingWithOrderByCodeDesc(prefix);
        int nextSeq = 1;
        if (last != null) {
            String lastCode = last.getCode();         
            String seqStr = lastCode.substring(prefix.length()); 
            try {
                nextSeq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }
        String seq = String.format("%02d", nextSeq);
        return prefix + seq;
    }

    private Map<String, Integer> resolveHeaderIndex(Row header, DataFormatter formatter) {
        Map<String, Integer> result = new HashMap<>();
        if (header == null) {
            return result;
        }
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String value = readCell(header, i, formatter);
            if (!value.isBlank()) {
                result.put(normalizeHeader(value), i);
            }
        }
        return result;
    }

    private Integer firstHeader(Map<String, Integer> headerIndex, String... names) {
        for (String name : names) {
            Integer idx = headerIndex.get(normalizeHeader(name));
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().replace(" ", "").replace("-", "_").toLowerCase();
    }

    private String readCell(Row row, Integer index, DataFormatter formatter) {
        if (row == null || index == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        return formatter.formatCellValue(cell).trim();
    }

    private Map<String, Object> rowFailure(int rowNumber, String reason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("row", rowNumber);
        row.put("reason", reason);
        return row;
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Object id = claims == null ? null : claims.get("id");
        if (id instanceof Integer integerId) {
            return integerId;
        }
        if (id instanceof Long longId) {
            return longId.intValue();
        }
        if (id instanceof String stringId) {
            return Integer.valueOf(stringId);
        }
        throw new RuntimeException("当前用户信息无效");
    }

}
