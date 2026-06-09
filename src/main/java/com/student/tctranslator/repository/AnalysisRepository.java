package com.student.tctranslator.repository;

import com.student.tctranslator.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// spring data jpa handles all the SQL for me, which is great
// because SQL is not my strong suit lol
@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // lowest trust score first = most predatory
    List<Analysis> findTop10ByOrderByTrustScoreAsc();

    // highest trust score first = most honest
    List<Analysis> findTop10ByOrderByTrustScoreDesc();
}
