package com.project.appeal.repo;

import com.project.appeal.entity.Appeal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 申诉记录数据访问层。
 */
public interface AppealRepository extends JpaRepository<Appeal, Long> {

    /** 查询某用户提交的所有申诉（按创建时间倒序） */
    List<Appeal> findByAppellantIdOrderByCreatedAtDesc(Long appellantId);

    /** 分页查询所有申诉（管理员使用，按创建时间倒序） */
    Page<Appeal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 分页查询指定状态的申诉（管理员筛选用） */
    Page<Appeal> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
