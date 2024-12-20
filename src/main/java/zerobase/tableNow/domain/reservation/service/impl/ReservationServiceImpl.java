package zerobase.tableNow.domain.reservation.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zerobase.tableNow.domain.constant.Status;
import zerobase.tableNow.domain.reservation.dto.ApprovalDto;
import zerobase.tableNow.domain.reservation.dto.ReservationDto;
import zerobase.tableNow.domain.reservation.entity.ReservationEntity;
import zerobase.tableNow.domain.reservation.mapper.ReservationMapper;
import zerobase.tableNow.domain.reservation.repository.ReservationRepository;
import zerobase.tableNow.domain.reservation.service.ReservationService;
import zerobase.tableNow.domain.store.entity.StoreEntity;
import zerobase.tableNow.domain.store.repository.StoreRepository;
import zerobase.tableNow.domain.user.entity.UsersEntity;
import zerobase.tableNow.domain.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ReservationMapper reservationMapper;

    /**
     * 예약 요청
     * @param reservationDto
     * @return
     */
    @Override
    public ReservationDto request(ReservationDto reservationDto) {
        // 사용자와 매장 정보 조회
        UsersEntity users = userRepository
                .findByUser(reservationDto.getUserId())
                .orElseThrow(() -> new RuntimeException("해당 ID가 없습니다."));

        StoreEntity store = storeRepository
                .findByStore(reservationDto.getStore())
                .orElseThrow(() -> new RuntimeException("해당 가게가 없습니다."));

        // 10분 단위 시간 검증
        validateTimeInterval(reservationDto.getReserDateTime());

        // 영업시간 체크
        validateBusinessHours(store, reservationDto.getReserDateTime());

        // 휴무일 체크
        validateStoreHoliday(store, reservationDto.getReserDateTime());

        // 중복 예약 체크
        validateDuplicateReservation(store, reservationDto.getReserDateTime(), users);

        // 예약 저장
        ReservationEntity reservationEntity = reservationMapper.toReserEntity(reservationDto, users, store);
        ReservationEntity saveEntity = reservationRepository.save(reservationEntity);

        return reservationMapper.toReserDto(saveEntity);
    }

    // 10분 단위 시간 검증
    private void validateTimeInterval(LocalDateTime reservationTime) {
        int minute = reservationTime.getMinute();
        if (minute % 10 != 0) {
            throw new RuntimeException("예약은 10분 단위로만 가능합니다. 예: 00, 10, 20, 30, 40, 50분");
        }
    }

    // 영업시간 검증
    private void validateBusinessHours(StoreEntity store, LocalDateTime reservationTime) {
        LocalTime reservationLocalTime = reservationTime.toLocalTime();

        // 매장 영업시간 파싱 (HH:mm 형식 가정)
        LocalTime openTime = LocalTime.parse(store.getStoreOpen());
        LocalTime closeTime = LocalTime.parse(store.getStoreClose());

        if (reservationLocalTime.isBefore(openTime) ||
                reservationLocalTime.isAfter(closeTime)) {
            throw new RuntimeException(
                    String.format("영업시간 외 예약입니다. 영업시간: %s ~ %s",
                            store.getStoreOpen(),
                            store.getStoreClose())
            );
        }
    }

    // 휴무일 검증
    private void validateStoreHoliday(StoreEntity store, LocalDateTime reservationTime) {
        String dayOfWeek = reservationTime.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.KOREAN);

        if (store.getStoreWeekOff().contains(dayOfWeek)) {
            throw new RuntimeException("해당 날짜는 매장 휴무일입니다.");
        }
    }

    // 중복 예약 검증
    private void validateDuplicateReservation(StoreEntity store, LocalDateTime reservationDateTime, UsersEntity user) {
        List<ReservationEntity> existingReservations = reservationRepository.findByStoreAndReserDateTimeBetween(
                store,
                reservationDateTime.minusMinutes(10),
                reservationDateTime.plusMinutes(10)
        );

        // 예약 시간이 겹치는 예약이 있는지 검사
        for (ReservationEntity reservation : existingReservations) {
            // 동일한 사용자의 예약인 경우 제외
            if (reservation.getUser().equals(user)) {
                continue;
            }

            // 예약 시간이 겹치는 경우 예외 발생
            if (reservation.getReserDateTime().isEqual(reservationDateTime)
                    || (reservation.getReserDateTime().isAfter(reservationDateTime.minusMinutes(10))
                    && reservation.getReserDateTime().isBefore(reservationDateTime.plusMinutes(10)))) {
                throw new RuntimeException("해당 시간대에 이미 예약이 존재합니다.");
            }
        }
    }

    /**
     * 예약확정
     * @param phone
     * @return 예약목록
     */
    @Override
    public ApprovalDto approve(String phone) {
        Optional<ReservationEntity> optionalReservation = reservationRepository.findByPhone(phone);
        log.info("서비스 예약 번호 확인 + {}", optionalReservation);
        if (optionalReservation.isEmpty()) {
            throw new IllegalArgumentException("예약 정보를 찾을 수 없습니다.");
        }

        ReservationEntity reservation = optionalReservation.get();
        log.info("예약 내용 확인  + {}", reservation);

        LocalDateTime reservationTime = reservation.getReserDateTime();
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime cutoffTime = reservationTime.minusMinutes(10);

        // 예약 시간 10분 전까지는 ING, 그 이후에는 STOP
        Status status;
        if (currentTime.isBefore(cutoffTime)) {
            status = Status.ING;
        } else {
            status = Status.STOP;
        }

        reservation.setReservationStatus(status);
        reservationRepository.save(reservation);

        return new ApprovalDto(phone, status, currentTime.isBefore(cutoffTime));
    }


}
