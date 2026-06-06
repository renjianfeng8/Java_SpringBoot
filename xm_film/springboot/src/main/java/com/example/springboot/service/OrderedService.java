package com.example.springboot.service;

import com.example.springboot.common.BaseMapper;
import com.example.springboot.common.BaseService;
import com.example.springboot.entity.Film;
import com.example.springboot.entity.Ordered;
import com.example.springboot.entity.Record;
import com.example.springboot.exception.CustomException;
import com.example.springboot.mapper.FilmMapper;
import com.example.springboot.mapper.OrderedMapper;
import com.example.springboot.mapper.RecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderedService extends BaseService<Ordered> {

    @Resource
    private OrderedMapper orderedMapper;

    @Resource
    private RecordMapper recordMapper;

    @Resource
    private FilmMapper filmMapper;

    @Override
    protected BaseMapper<Ordered> mapper() {
        return orderedMapper;
    }

    public void applyScope(Ordered ordered, String role, Integer userId) {
        if (ordered == null || role == null || userId == null) {
            return;
        }
        if ("USER".equals(role)) {
            ordered.setUserId(userId);
        } else if ("CINEMA".equals(role)) {
            ordered.setCinemaId(userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Ordered ordered) {
        createOrder(ordered, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Ordered ordered, String role, Integer tokenUserId) {
        if (role != null && !"USER".equals(role)) {
            throw new CustomException("403", "仅普通用户可创建订单");
        }
        if (ordered == null || ordered.getRecordId() == null) {
            throw new CustomException("400", "缺少放映场次");
        }
        if (tokenUserId != null) {
            ordered.setUserId(tokenUserId);
        }
        if (ordered.getUserId() == null) {
            throw new CustomException("400", "缺少购票用户");
        }

        Record record = recordMapper.selectById(ordered.getRecordId());
        if (record == null) {
            throw new CustomException("400", "放映场次不存在");
        }

        List<String> seats = normalizeSeats(ordered.getSeat());
        int number = seats.size();
        for (String seat : seats) {
            if (orderedMapper.countSeatInUse(record.getId(), seat) > 0) {
                throw new CustomException("409", "座位已售: " + seat);
            }
        }

        Film film = filmMapper.selectById(record.getFilmId());
        BigDecimal price = parsePrice(record.getPrice());
        BigDecimal total = price.multiply(BigDecimal.valueOf(number)).setScale(2, RoundingMode.HALF_UP);

        ordered.setOrders(generateOrderNo());
        ordered.setRecordId(record.getId());
        ordered.setFilmId(record.getFilmId());
        ordered.setCinemaId(record.getCinemaId());
        ordered.setRoomId(record.getRoomId());
        ordered.setAppointment("场次ID:" + record.getId());
        ordered.setStart(record.getStart());
        ordered.setNumber(number);
        ordered.setTotal(total.doubleValue());
        ordered.setStatus("待取票");
        ordered.setSeat(String.join(",", seats));
        if (film != null) {
            ordered.setImg(film.getImg());
        }

        orderedMapper.insert(ordered);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Ordered ordered) {
        ordered.setStatus(null);
        mapper().updateById(ordered);
    }

    public List<Ordered> selectActiveByRecordId(Integer recordId) {
        return orderedMapper.selectActiveByRecordId(recordId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteScoped(Integer id, String role, Integer userId) {
        Ordered ordered = selectById(id);
        ensureOrderAccess(ordered, role, userId);
        delete(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatchScoped(List<Integer> ids, String role, Integer userId) {
        if (ids == null || ids.isEmpty()) {
            throw new CustomException("400", "请选择订单");
        }
        for (Integer id : ids) {
            Ordered ordered = selectById(id);
            ensureOrderAccess(ordered, role, userId);
        }
        deleteBatch(ids);
    }

    private void ensureOrderAccess(Ordered ordered, String role, Integer userId) {
        if (ordered == null) {
            throw new CustomException("404", "订单不存在");
        }
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("USER".equals(role) && userId != null && userId.equals(ordered.getUserId())) {
            return;
        }
        if ("CINEMA".equals(role) && userId != null && userId.equals(ordered.getCinemaId())) {
            return;
        }
        throw new CustomException("403", "无权操作该订单");
    }

    private List<String> normalizeSeats(String rawSeats) {
        if (rawSeats == null || rawSeats.trim().isEmpty()) {
            throw new CustomException("400", "请选择座位");
        }
        Set<String> seats = Arrays.stream(rawSeats.replace('，', ',').split(","))
                .map(String::trim)
                .filter(seat -> !seat.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (seats.isEmpty()) {
            throw new CustomException("400", "请选择座位");
        }
        for (String seat : seats) {
            if (!seat.matches("[1-8]排[1-8]座")) {
                throw new CustomException("400", "座位格式错误: " + seat);
            }
        }
        return List.copyOf(seats);
    }

    private BigDecimal parsePrice(String price) {
        try {
            return new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new CustomException("500", "场次票价配置错误");
        }
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return date + suffix;
    }
}
