package com.example.ilhafit.repository;

import com.example.ilhafit.entity.GradeAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GradeAtividadeRepository extends JpaRepository<GradeAtividade, Long> {
}
