package org.example.customersupport.repository;

import org.example.customersupport.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    
    Optional<ChatSession> findBySessionId(String sessionId);
    
    List<ChatSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<ChatSession> findByStatusOrderByCreatedAtDesc(ChatSession.SessionStatus status);
    
    @Query("SELECT cs FROM ChatSession cs WHERE cs.userId = :userId AND cs.status = :status ORDER BY cs.updatedAt DESC")
    Optional<ChatSession> findActiveSessionByUserId(@Param("userId") Long userId, @Param("status") ChatSession.SessionStatus status);
}