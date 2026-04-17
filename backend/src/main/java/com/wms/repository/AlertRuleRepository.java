package com.wms.repository;

import com.wms.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByEnabledTrueOrderByIdAsc();

    Optional<AlertRule> findByRuleCode(String ruleCode);
}
