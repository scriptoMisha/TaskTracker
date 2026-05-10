package com.example.minitasks.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByOwnerIdAndStatus(UUID ownerId, TaskStatus status);

    @Query("""
            select distinct t from Task t
            left join t.tags tag
            where t.owner.id = :ownerId
              and (:status is null or t.status = :status)
              and (:tagName is null or tag.name = :tagName)
            """)
    Page<Task> search(@Param("ownerId") UUID ownerId,
                      @Param("status") TaskStatus status,
                      @Param("tagName") String tagName,
                      Pageable pageable);
}
