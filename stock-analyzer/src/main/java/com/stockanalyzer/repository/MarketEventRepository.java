package com.stockanalyzer.repository;

import com.stockanalyzer.entity.MarketEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketEventRepository extends JpaRepository<MarketEvent, Long> {
}
