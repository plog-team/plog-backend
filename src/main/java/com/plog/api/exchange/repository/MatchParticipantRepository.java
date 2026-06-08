package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    Optional<MatchParticipant> findByExchangeMatchId(Long matchId);

    List<MatchParticipant> findAllByExchangeMatchId(Long matchId);

    @Query("SELECT mp FROM MatchParticipant mp JOIN mp.exchangeMatch em WHERE mp.userId = :userId AND em.status IN ('PENDING', 'MATCHED') ORDER BY mp.id DESC")
    List<MatchParticipant> findActiveMatchesByUserId(@Param("userId") Long userId);
}