package com.plog.api.exchange.repository;

import com.plog.api.exchange.domain.ExchangeRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExchangeRoomRepository extends JpaRepository<ExchangeRoom, Long> {

    @Query("SELECT er FROM ExchangeRoom er WHERE er.exchangeMatch.id IN " +
            "(SELECT mp.exchangeMatch.id FROM MatchParticipant mp WHERE mp.userId = :userId) " +
            "AND er.status = 'ACTIVE' ORDER BY er.id DESC")
    List<ExchangeRoom> findActiveRoomsByUserId(@Param("userId") Long userId);
}