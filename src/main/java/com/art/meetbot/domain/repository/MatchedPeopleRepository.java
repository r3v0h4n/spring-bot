package com.art.meetbot.domain.repository;

import com.art.meetbot.domain.entity.MatchedPeople;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchedPeopleRepository extends JpaRepository<MatchedPeople, UUID> {
    List<MatchedPeople> findAllByTelegramIdFirstIsOrTelegramIdSecondIs(String id1, String id2);
}
