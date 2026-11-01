package com.seniorinsight.telemetry.repository;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.seniorinsight.telemetry.domain.TelemetriaWearable;
public interface TelemetriaWearableRepository extends JpaRepository<TelemetriaWearable,UUID> {
 List<TelemetriaWearable> findByIdosoIdAndColetadoEmGreaterThanEqualAndColetadoEmLessThan(
	 UUID idosoId, java.time.Instant from, java.time.Instant to);
 List<TelemetriaWearable> findByIdosoIdOrderByColetadoEmDesc(UUID idosoId,Pageable pageable);
 Optional<TelemetriaWearable> findTopByIdosoIdOrderByColetadoEmDesc(UUID idosoId);
}
