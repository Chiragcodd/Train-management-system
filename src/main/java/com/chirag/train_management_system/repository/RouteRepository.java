package com.chirag.train_management_system.repository;

import com.chirag.train_management_system.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByTrainIdOrderByStopOrderAsc(Long trainId);

    Optional<Route> findByTrainIdAndStationId(Long trainId, Long stationId);

    boolean existsByTrainIdAndStationId(Long trainId, Long stationId);

    @Modifying
    @Query("DELETE FROM Route r WHERE r.train.id = :trainId")
    void deleteByTrainId(@Param("trainId") Long trainId);

    @Query("""
        SELECT r FROM Route r
        WHERE r.train.id       = :trainId
          AND r.station.code   = :code
    """)
    Optional<Route> findByTrainIdAndStationCode(
            @Param("trainId") Long trainId,
            @Param("code")    String code
    );

    boolean existsByStationId(Long stationId);
}