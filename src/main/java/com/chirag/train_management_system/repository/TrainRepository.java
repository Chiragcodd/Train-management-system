package com.chirag.train_management_system.repository;

import com.chirag.train_management_system.entity.Train;
import com.chirag.train_management_system.enums.TrainStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    Optional<Train> findByTrainNumber(String trainNumber);

    boolean existsByTrainNumber(String trainNumber);

    @Query("""
        SELECT t FROM Train t
        LEFT JOIN FETCH t.routes r
        LEFT JOIN FETCH r.station
        WHERE t.id = :id
    """)
    Optional<Train> findByIdWithRoutes(@Param("id") Long id);

    @Query("""
    SELECT t FROM Train t
    LEFT JOIN FETCH t.routes r
    LEFT JOIN FETCH r.station
    WHERE t.trainNumber = :trainNumber
    """)
    Optional<Train> findByTrainNumberWithRoutes(@Param("trainNumber") String trainNumber);

    @Query("""
        SELECT DISTINCT t FROM Train t
        LEFT JOIN FETCH t.routes r
        LEFT JOIN FETCH r.station
        WHERE t.id IN (
            SELECT t2.id FROM Train t2
            JOIN t2.routes r1
            JOIN t2.routes r2
            WHERE r1.station.id = :fromStationId
              AND r2.station.id = :toStationId
              AND t2.status = :status
        )
    """)
    List<Train> findTrainsBetweenStations(
            @Param("fromStationId") Long fromStationId,
            @Param("toStationId") Long toStationId,
            @Param("status") TrainStatus status
    );
}