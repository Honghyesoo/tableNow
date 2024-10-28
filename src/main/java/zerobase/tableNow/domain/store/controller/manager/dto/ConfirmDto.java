package zerobase.tableNow.domain.store.controller.manager.dto;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.*;
import zerobase.tableNow.domain.constant.Status;
import zerobase.tableNow.domain.store.entity.StoreEntity;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfirmDto {
    private String storeName; // 상점이름
    private String phone; // 예약자 번호
    private LocalDateTime reserDateTime;// 예약 날짜, 시간
    private Integer peopleNb;// 예약인원
    private Status reservationStatus; //이용가능한 상태, 정지상태
}