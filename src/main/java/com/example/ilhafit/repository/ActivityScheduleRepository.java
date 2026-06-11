package com.example.ilhafit.repository;

import com.example.ilhafit.entity.ActivitySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityScheduleRepository extends JpaRepository<ActivitySchedule, Long> {
}

