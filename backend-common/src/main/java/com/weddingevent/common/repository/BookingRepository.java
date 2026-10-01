package com.weddingevent.common.repository;

import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.enums.BookingStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = {"staff", "servicePackage"})
    List<Booking> findByEventDateBetweenOrderByEventDateAscEventTimeAsc(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"staff", "servicePackage"})
    @Query("""
            select distinct b from Booking b join b.staff s
            where s.id = :staffId and b.eventDate between :from and :to
            order by b.eventDate, b.eventTime""")
    List<Booking> findForStaffBetween(@Param("staffId") UUID staffId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Names of staff already booked at the same date & time (FN-ADM-CAL-02 conflict check) */
    @Query("""
            select distinct s.fullName from Booking b join b.staff s
            where b.eventDate = :date and b.eventTime = :time and b.status <> :excluded and s.id in :staffIds""")
    List<String> findBusyStaffNames(
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            @Param("staffIds") Collection<UUID> staffIds,
            @Param("excluded") BookingStatus excluded);

    interface StaffBookingCount {
        UUID getStaffId();

        long getTotal();
    }

    @Query("""
            select s.id as staffId, count(b) as total from Booking b join b.staff s
            where b.eventDate between :from and :to and b.status <> :excluded
            group by s.id""")
    List<StaffBookingCount> countPerStaffBetween(
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("excluded") BookingStatus excluded);
}
