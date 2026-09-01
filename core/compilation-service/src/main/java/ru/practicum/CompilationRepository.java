package ru.practicum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.entity.Compilation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @Query("SELECT c.id FROM Compilation c WHERE (:pinned IS NULL OR c.pinned = :pinned)")
    Page<Long> findByPinned(@Param("pinned") Boolean pinned, Pageable pageable);

    Page<Compilation> findAll(Pageable pageable);

    @Modifying
    @Query("DELETE FROM Compilation c WHERE c.id = :compId")
    int deleteCompilationById(@Param("compId") Long compId);

    @Query("SELECT c FROM Compilation c WHERE c.id IN :ids")
    List<Compilation> findAllDetailedByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT c FROM Compilation c WHERE c.id = :compId")
    Optional<Compilation> findDetailedById(@Param("compId") Long compId);
}
