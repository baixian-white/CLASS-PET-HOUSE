package com.classpethouse.backend.controller;

import com.classpethouse.backend.entity.ClassEntity;
import com.classpethouse.backend.entity.ExchangeRecordEntity;
import com.classpethouse.backend.entity.HistoryEntity;
import com.classpethouse.backend.entity.ShopItemEntity;
import com.classpethouse.backend.entity.StudentEntity;
import com.classpethouse.backend.entity.UserEntity;
import com.classpethouse.backend.exception.ApiException;
import com.classpethouse.backend.repository.ExchangeRecordRepository;
import com.classpethouse.backend.repository.HistoryRepository;
import com.classpethouse.backend.repository.ShopItemRepository;
import com.classpethouse.backend.repository.StudentRepository;
import com.classpethouse.backend.service.AppSupportService;
import com.classpethouse.backend.service.StudentRankingStatsService;
import com.classpethouse.backend.util.AuthSupport;
import com.classpethouse.backend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopItemRepository shopItemRepository;
    private final ExchangeRecordRepository exchangeRecordRepository;
    private final StudentRepository studentRepository;
    private final HistoryRepository historyRepository;
    private final AppSupportService appSupportService;
    private final StudentRankingStatsService studentRankingStatsService;

    public ShopController(
            ShopItemRepository shopItemRepository,
            ExchangeRecordRepository exchangeRecordRepository,
            StudentRepository studentRepository,
            HistoryRepository historyRepository,
            AppSupportService appSupportService,
            StudentRankingStatsService studentRankingStatsService
    ) {
        this.shopItemRepository = shopItemRepository;
        this.exchangeRecordRepository = exchangeRecordRepository;
        this.studentRepository = studentRepository;
        this.historyRepository = historyRepository;
        this.appSupportService = appSupportService;
        this.studentRankingStatsService = studentRankingStatsService;
    }

    @GetMapping("/class/{classId}")
    public List<ShopItemEntity> listItems(@PathVariable Integer classId, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());
        return shopItemRepository.findByClassIdOrderByCreatedAtAsc(classId);
    }

    @PostMapping
    public ShopItemEntity create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        appSupportService.requireOwnedClass(classId, user.getId());

        String name = safeTrim(RequestUtils.string(body, "name"));
        Integer price = RequestUtils.integer(body, "price");
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "商品名称不能为空");
        }
        if (price == null || price < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "价格至少为1");
        }
        if (shopItemRepository.countByClassId(classId) >= 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "最多创建100个商品");
        }

        ShopItemEntity item = new ShopItemEntity();
        item.setClassId(classId);
        item.setName(name);
        item.setDescription(defaultDescription(RequestUtils.string(body, "description")));
        item.setIcon(defaultIcon(RequestUtils.string(body, "icon")));
        item.setPrice(price);
        Integer stock = RequestUtils.integer(body, "stock");
        item.setStock(stock == null ? -1 : stock);
        return shopItemRepository.save(item);
    }

    @PutMapping("/{id}")
    public ShopItemEntity update(@PathVariable Integer id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        ShopItemEntity item = appSupportService.requireShopItem(id);
        appSupportService.requireOwnedClass(item.getClassId(), user.getId());

        if (body.containsKey("name")) {
            String name = safeTrim(RequestUtils.string(body, "name"));
            if (name == null || name.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "商品名称不能为空");
            }
            item.setName(name);
        }
        if (body.containsKey("description")) {
            item.setDescription(defaultDescription(RequestUtils.string(body, "description")));
        }
        if (body.containsKey("icon")) {
            item.setIcon(defaultIcon(RequestUtils.string(body, "icon")));
        }
        if (body.containsKey("price")) {
            Integer price = RequestUtils.integer(body, "price");
            if (price != null) {
                if (price < 1) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "价格至少为1");
                }
                item.setPrice(price);
            }
        }
        if (body.containsKey("stock")) {
            Integer stock = RequestUtils.integer(body, "stock");
            if (stock != null) {
                if (stock < -1) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "库存不能小于-1");
                }
                item.setStock(stock);
            }
        }
        return shopItemRepository.save(item);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        ShopItemEntity item = appSupportService.requireShopItem(id);
        appSupportService.requireOwnedClass(item.getClassId(), user.getId());
        shopItemRepository.delete(item);
        return Map.of("message", "删除成功");
    }

    @PostMapping("/exchange")
    @Transactional
    public Map<String, Object> exchange(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        Integer classId = RequestUtils.integer(body, "class_id");
        Integer studentId = RequestUtils.integer(body, "student_id");
        Integer itemId = RequestUtils.integer(body, "item_id");

        ClassEntity currentClass = appSupportService.requireOwnedClass(classId, user.getId());
        StudentEntity student = studentRepository.findByIdAndClassId(studentId, classId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "学生不存在"));
        ShopItemEntity item = shopItemRepository.findById(itemId)
                .filter(value -> value.getClassId().equals(classId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "商品不存在"));

        if (item.getStock() != null && item.getStock() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "商品库存不足");
        }
        List<Map<String, Object>> badges = student.getBadges() == null ? new ArrayList<>() : new ArrayList<>(student.getBadges());
        if (badges.size() < item.getPrice()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "徽章不足");
        }

        List<Map<String, Object>> remainingBadges = new ArrayList<>(badges.subList(item.getPrice(), badges.size()));
        student.setBadges(remainingBadges);
        // 兑换只减少当前可用勋章，不影响历史累计获得的总勋章。
        studentRankingStatsService.syncBadgeStats(student, badges.size());
        studentRepository.save(student);

        if (item.getStock() != null && item.getStock() > 0) {
            item.setStock(item.getStock() - 1);
            shopItemRepository.save(item);
        }

        ExchangeRecordEntity record = new ExchangeRecordEntity();
        record.setClassId(currentClass.getId());
        record.setStudentId(student.getId());
        record.setItemId(item.getId());
        record.setItemName(item.getName());
        record.setCost(item.getPrice());
        exchangeRecordRepository.save(record);

        HistoryEntity history = new HistoryEntity();
        history.setClassId(currentClass.getId());
        history.setStudentId(student.getId());
        history.setRuleId(null);
        history.setRuleName("兑换: " + item.getName());
        history.setValue(-item.getPrice());
        history.setType("exchange");
        historyRepository.save(history);

        return Map.of(
                "message", "兑换成功",
                "record", record
        );
    }

    @GetMapping("/exchange/{classId}")
    public List<ExchangeRecordEntity> exchangeRecords(@PathVariable Integer classId, HttpServletRequest request) {
        UserEntity user = AuthSupport.currentUser(request);
        AuthSupport.requireActivated(user);
        appSupportService.requireOwnedClass(classId, user.getId());
        List<ExchangeRecordEntity> rows = exchangeRecordRepository.findTop200ByClassIdOrderByCreatedAtDesc(classId);
        appSupportService.populateStudentRefsForExchange(rows);
        return rows;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultDescription(String value) {
        String text = safeTrim(value);
        return text == null ? "" : text;
    }

    private String defaultIcon(String value) {
        String text = safeTrim(value);
        return text == null || text.isEmpty() ? "🎁" : text;
    }
}
