package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.HistoryEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.HistoryRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.service.AppSupportService;
import com.classpethouse.backend.util.AuthSupport;
import com.classpethouse.backend.util.RequestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StudentRepository studentRepository;
    private final HistoryRepository historyRepository;
    private final AppSupportService appSupportService;

    public AiController(StudentRepository studentRepository, HistoryRepository historyRepository, AppSupportService appSupportService) {
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.appSupportService = appSupportService;
    }

    @PostMapping("/generate-pet-name")
    public Map<String, Object> generatePetName(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        String studentName = RequestUtils.string(body, "studentName");
        String petType = RequestUtils.string(body, "petType");
        if (studentName == null || studentName.isBlank() || petType == null || petType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数不完整");
        }

        String surname = studentName.substring(0, 1);
        String suffix = switch (Math.abs((studentName + petType).hashCode()) % 6) {
            case 0 -> "团子";
            case 1 -> "小宝";
            case 2 -> "星星";
            case 3 -> "队长";
            case 4 -> "糖糖";
            default -> "元气";
        };
        return Map.of("name", surname + petType.replaceAll("\\s+", "") + suffix);
    }

    @PostMapping("/evaluate")
    public ResponseEntity<StreamingResponseBody> evaluate(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer studentId = RequestUtils.integer(body, "studentId");
        Integer classId = RequestUtils.integer(body, "classId");
        ClassEntity currentClass = appSupportService.requireOwnedClass(classId, user.getId());
        StudentEntity student = studentRepository.findByIdAndClassId(studentId, currentClass.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "学生不存在"));

        List<HistoryEntity> history = historyRepository
                .findByClassIdAndStudentIdOrderByCreatedAtDesc(classId, studentId, PageRequest.of(0, 10))
                .getContent();

        String text = buildStudentEvaluation(student, history);
        return streamText(text);
    }

    @PostMapping("/weekly-report")
    public ResponseEntity<StreamingResponseBody> weeklyReport(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "classId");
        Integer rawDays = RequestUtils.integer(body, "days");
        int lookbackDays = (rawDays == null || rawDays <= 0) ? 7 : rawDays;

        ClassEntity currentClass = appSupportService.requireOwnedClass(classId, user.getId());
        List<StudentEntity> students = studentRepository.findByClassId(currentClass.getId());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(lookbackDays);
        List<HistoryEntity> recent = historyRepository
                .findByClassIdOrderByCreatedAtDesc(currentClass.getId(), PageRequest.of(0, 200))
                .getContent()
                .stream()
                .filter(item -> item.getCreatedAt() == null || item.getCreatedAt().isAfter(cutoff))
                .collect(Collectors.toList());

        String text = buildClassReport(currentClass, students, recent, lookbackDays);
        return streamText(text);
    }

    private ResponseEntity<StreamingResponseBody> streamText(String text) {
        StreamingResponseBody body = outputStream -> {
            for (String chunk : chunks(text, 22)) {
                String json = OBJECT_MAPPER.writeValueAsString(Map.of("content", chunk));
                outputStream.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
            outputStream.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(body);
    }

    private List<String> chunks(String text, int size) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < text.length(); i += size) {
            list.add(text.substring(i, Math.min(text.length(), i + size)));
        }
        return list;
    }

    private String buildStudentEvaluation(StudentEntity student, List<HistoryEntity> history) {
        long positive = history.stream().filter(item -> "score".equals(item.getType()) && item.getValue() > 0).count();
        long negative = history.stream().filter(item -> "score".equals(item.getType()) && item.getValue() < 0).count();
        int badges = student.getBadges() == null ? 0 : student.getBadges().size();
        StringBuilder builder = new StringBuilder();
        builder.append("这位同学最近的成长状态很清晰。");
        builder.append(student.getName()).append(" 目前累计食物值为 ").append(student.getFoodCount() == null ? 0 : student.getFoodCount()).append("，");
        builder.append("已经获得 ").append(badges).append(" 枚徽章。");
        if (positive >= negative) {
            builder.append("从最近记录看，正向表现明显更多，课堂投入度和任务完成度都不错。");
        } else {
            builder.append("最近波动稍微多一些，建议老师用更细颗粒度的鼓励，把好表现及时强化。");
        }
        if (!history.isEmpty()) {
            builder.append("最近一次记录是“").append(history.get(0).getRuleName()).append("”，可以围绕这件事继续做具体反馈。");
        }
        builder.append("建议接下来继续关注习惯稳定性，同时给他一个明确、可达成的小目标，会更容易形成持续进步。");
        return builder.toString();
    }

    private String buildClassReport(ClassEntity currentClass, List<StudentEntity> students, List<HistoryEntity> recent, int days) {
        long positive = recent.stream().filter(item -> "score".equals(item.getType()) && item.getValue() > 0).count();
        long negative = recent.stream().filter(item -> "score".equals(item.getType()) && item.getValue() < 0).count();
        int withPets = (int) students.stream().filter(student -> student.getPetType() != null && !student.getPetType().isBlank()).count();
        double avgFood = students.stream().mapToInt(student -> student.getFoodCount() == null ? 0 : student.getFoodCount()).average().orElse(0);

        StringBuilder builder = new StringBuilder();
        builder.append("班级「").append(currentClass.getName()).append("」最近 ").append(days).append(" 天整体运行平稳。");
        builder.append("当前共有 ").append(students.size()).append(" 名学生，其中 ").append(withPets).append(" 名已经拥有宠物伙伴。");
        builder.append("全班平均食物值约为 ").append(String.format("%.1f", avgFood)).append("。");
        if (positive >= negative) {
            builder.append("从操作记录看，正向激励占主导，说明班级氛围整体偏积极。");
        } else {
            builder.append("近期负向提醒略多，建议老师把规则再可视化一点，让学生更容易理解行为边界。");
        }
        builder.append("最近 ").append(days).append(" 天共产生 ").append(recent.size()).append(" 条关键记录，");
        builder.append("其中正向记录 ").append(positive).append(" 条，提醒类记录 ").append(negative).append(" 条。");
        builder.append("建议下一阶段聚焦两件事：一是持续表扬稳定表现的学生，二是给波动学生设计更容易达成的小任务，帮助他们尽快形成正反馈。");
        return builder.toString();
    }
}
