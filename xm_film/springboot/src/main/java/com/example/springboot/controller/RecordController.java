package com.example.springboot.controller;

import com.example.springboot.common.AuthContext;
import com.example.springboot.common.BaseController;
import com.example.springboot.common.Result;
import com.example.springboot.dto.request.RecordRequest;
import com.example.springboot.dto.response.RecordView;
import com.example.springboot.entity.Record;
import com.example.springboot.service.RecordService;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/records")
public class RecordController extends BaseController<Record, RecordRequest, RecordView> {
    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        super(recordService);
        this.recordService = recordService;
    }

    @Override
    protected Record toEntity(RecordRequest request) {
        Record record = new Record();
        record.setId(request.getId());
        record.setCinemaId(request.getCinemaId());
        record.setRoomId(request.getRoomId());
        record.setFilmId(request.getFilmId());
        record.setTitle(request.getTitle());
        record.setStart(request.getStart());
        record.setPrice(request.getPrice());
        record.setStatus(request.getStatus());
        record.setCinemaName(request.getCinemaName());
        record.setRoomName(request.getRoomName());
        record.setRoomSeatRows(request.getRoomSeatRows());
        record.setRoomSeatCols(request.getRoomSeatCols());
        return record;
    }

    @Override
    protected RecordView toView(Record record) {
        RecordView view = new RecordView();
        view.setId(record.getId());
        view.setCinemaId(record.getCinemaId());
        view.setRoomId(record.getRoomId());
        view.setFilmId(record.getFilmId());
        view.setTitle(record.getTitle());
        view.setStart(record.getStart());
        view.setPrice(record.getPrice());
        view.setStatus(record.getStatus());
        view.setCinemaName(record.getCinemaName());
        view.setRoomName(record.getRoomName());
        view.setRoomSeatRows(record.getRoomSeatRows());
        view.setRoomSeatCols(record.getRoomSeatCols());
        return view;
    }

    @Override
    @GetMapping
    public Result<List<RecordView>> list(RecordRequest query) {
        Record entity = toEntity(query);
        recordService.applyCinemaScope(entity, AuthContext.role(), AuthContext.userId());
        return Result.success(recordService.selectAll(entity).stream().map(this::toView).toList());
    }

    @Override
    @GetMapping("/page")
    public Result<PageInfo<RecordView>> page(RecordRequest query,
                       @RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize) {
        Record entity = toEntity(query);
        recordService.applyCinemaScope(entity, AuthContext.role(), AuthContext.userId());
        PageMethod.startPage(pageNum, pageSize);
        PageInfo<Record> src = new PageInfo<>(recordService.selectAll(entity));
        PageInfo<RecordView> dst = new PageInfo<>();
        BeanUtils.copyProperties(src, dst);
        dst.setList(src.getList().stream().map(this::toView).toList());
        return Result.success(dst);
    }

    @Override
    @GetMapping("/{id}")
    public Result<RecordView> getById(@PathVariable Integer id) {
        Record recordItem = recordService.selectByIdScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success(toView(recordItem));
    }

    @Override
    @PostMapping
    public Result<Void> add(@RequestBody RecordRequest request) {
        recordService.addScoped(toEntity(request), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @PutMapping
    public Result<Void> update(@RequestBody RecordRequest request) {
        recordService.updateScoped(toEntity(request), AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        recordService.deleteScoped(id, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

    @Override
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        recordService.deleteBatchScoped(ids, AuthContext.role(), AuthContext.userId());
        return Result.success();
    }

}
