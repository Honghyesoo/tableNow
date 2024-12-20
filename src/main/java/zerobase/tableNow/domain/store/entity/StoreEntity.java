package zerobase.tableNow.domain.store.entity;

import jakarta.persistence.*;
import lombok.*;
import zerobase.tableNow.domain.baseEntity.BaseEntity;
import zerobase.tableNow.domain.user.entity.UsersEntity;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "store")
public class StoreEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UsersEntity user;

    private String store;
    private String storeLocation;
    private String storeImg;
    private String storeContents;

    @Column(nullable = true)
    private Integer rating = 0; // 별점
    private String storeOpen;
    private String storeClose;
    private String storeWeekOff;

    private double latitude;  // 위도 추가
    private double longitude; // 경도 추가

    @Transient
    private Double distance; // 거리순
}
