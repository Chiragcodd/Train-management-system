package com.chirag.train_management_system.repository;

import com.chirag.train_management_system.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    Optional<Station> findByCode(String code);

    boolean existsByCode(String code);

    List<Station> findByNameContainingIgnoreCase(String name);

    List<Station> findByCityIgnoreCase(String city);
}