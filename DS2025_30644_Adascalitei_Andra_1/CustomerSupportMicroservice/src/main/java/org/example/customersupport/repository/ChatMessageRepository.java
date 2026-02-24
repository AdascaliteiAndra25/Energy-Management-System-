package org.example.customersupport.repository;

import org.example.customersupport.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    
    List<ChatMessage> findByUserIdOrderByTimestampDesc(Long userId);
    
    List<ChatMessage> findTop10BySessionIdOrderByTimestampDesc(String sessionId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId ORDER BY cm.timestamp DESC")
    List<ChatMessage> findRecentMessagesBySession(@Param("sessionId") String sessionId);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.sessionId = :sessionId")
    Long countMessagesBySession(@Param("sessionId") String sessionId);
}