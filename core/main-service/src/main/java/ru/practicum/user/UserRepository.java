package ru.practicum.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Сохраняет пользователя в базе данных.
     *
     * @param user сущность пользователя
     * @return сохранённая сущность с ID
     */
    @Override
    User save(User user);

    /**
     * Находит пользователя по ID.
     *
     * @param userId ID пользователя
     * @return Optional с найденным пользователем, пустой Optional, если пользователь не найден
     */
    Optional<User> findById(Long userId);

    /**
     * Удаляет пользователя по ID и возвращает количество удалённых записей.
     *
     * @param userId ID пользователя
     * @return 1 — если удалён, 0 — если не найден
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :userId")
    int deleteByIdAndReturnRow(@Param("userId") Long userId);

    /**
     * Получает всех пользователей с пагинацией.
     *
     * @param offset индекс начала выборки (OFFSET)
     * @param size   размер страницы (LIMIT)
     * @return список пользователей в пределах пагинации
     */
    @Query(value = "SELECT * FROM users ORDER BY id LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<User> findAllWithOffset(
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * Получает пользователей по списку ID (без пагинации).
     *
     * @param ids список ID для фильтрации
     * @return отфильтрованный список пользователей
     */
    @Query("SELECT u FROM User u WHERE u.id IN :ids ORDER BY u.id")
    List<User> findByIds(@Param("ids") List<Long> ids);

    boolean existsByEmail(String email);
}
