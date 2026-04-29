package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingReservationRepository extends JpaRepository<BookingReservationEntity, Integer> {
    List<BookingReservationEntity> findByCourseId(String courseId);
}
