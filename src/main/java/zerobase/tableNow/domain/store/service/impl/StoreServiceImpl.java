package zerobase.tableNow.domain.store.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zerobase.tableNow.domain.constant.SortType;
import zerobase.tableNow.domain.store.dto.StoreDto;
import zerobase.tableNow.domain.store.entity.StoreEntity;
import zerobase.tableNow.domain.store.mapper.StoreMapper;
import zerobase.tableNow.domain.store.repository.StoreRepository;
import zerobase.tableNow.domain.store.service.LocationService;
import zerobase.tableNow.domain.store.service.StoreService;
import zerobase.tableNow.domain.user.entity.UsersEntity;
import zerobase.tableNow.domain.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class StoreServiceImpl implements StoreService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final LocationService locationService;

    /**
     * 상점등록
     * @param storeDto
     * @return 상점 등록 내용
     */
    @Override
    public StoreDto register(StoreDto storeDto) {

        UsersEntity optionalUsers = userRepository.findByUser(storeDto.getUser())
                .orElseThrow(() -> new RuntimeException("아이디가 존재하지 않습니다."));
        Optional<StoreEntity> optionalStoreEntity = storeRepository.findByStore(storeDto.getStore());

        if (optionalStoreEntity.isPresent()){
            log.info("해당 상점이 존재합니다.");
            throw new RuntimeException("해당 상점이 존재합니다.");
        }
        // 주소를 위도, 경도로 변환
        double[] coordinates = locationService.getCoordinates(storeDto.getStoreLocation());
        storeDto.setLatitude(coordinates[0]);
        storeDto.setLongitude(coordinates[1]);

        // DTO -> Entity 변환 및 저장
        StoreEntity storeEntity = storeMapper.toStoreEntity(storeDto, optionalUsers);
        StoreEntity saveEntity = storeRepository.save(storeEntity);

        return storeMapper.toStoreDto(saveEntity);
    }

    /**
     * 상점 목록
     * @param keyword
     * @param sortType
     * @param userLat
     * @param userLon
     * @return 필터를 통한 상점 목록 반환
     */
    @Override
    public List<StoreDto> getAllStores(String keyword, SortType sortType, Double userLat, Double userLon) {
        List<StoreEntity> storeEntities;

        // 기본 데이터 조회
        if (keyword != null && !keyword.trim().isEmpty()) {
            storeEntities = storeRepository.findByStoreContainingIgnoreCase(keyword.trim());
        } else {
            storeEntities = storeRepository.findAll();
        }

        // 거리 계산 및 정렬
        if (SortType.DISTANCE.equals(sortType) && userLat != null && userLon != null) {
            // 각 상점의 거리를 계산하고 정렬
            storeEntities.forEach(store -> {
                double distance = calculateDistance(
                        userLat, userLon,
                        store.getLatitude(), store.getLongitude()
                );
                store.setDistance(distance);
            });

            // 거리순으로 정렬
            storeEntities.sort(Comparator.comparingDouble(StoreEntity::getDistance));
        } else {
            // 다른 정렬 조건 적용
            if (sortType != null) {
                switch (sortType) {
                    case RATING_HIGH:
                        storeEntities.sort((a, b) -> compareRatings(b.getRating(), a.getRating()));
                        break;
                    case RATING_LOW:
                        storeEntities.sort((a, b) -> compareRatings(a.getRating(), b.getRating()));
                        break;
                    case NAME_ASC:
                        storeEntities.sort(Comparator.comparing(StoreEntity::getStore));
                        break;
                    case NAME_DESC:
                        storeEntities.sort(Comparator.comparing(StoreEntity::getStore).reversed());
                        break;
                }
            }
        }

        // DTO 변환 시 거리 정보도 포함
        return storeEntities.stream()
                .map(entity -> {
                    StoreDto dto = storeMapper.convertToDto(entity);
                    dto.setDistance(entity.getDistance());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // 거리 계산 메서드 추가
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구의 반경 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // 거리 (km)
    }

    // Null 처리를 위한 헬퍼 메소드들
    private int compareRatings(Integer rating1, Integer rating2) {
        // null을 0으로 처리
        int r1 = rating1 == null ? 0 : rating1;
        int r2 = rating2 == null ? 0 : rating2;
        return Integer.compare(r1, r2);
    }


    /**
     * 상점 수정
     * @param id
     * @param storeDto
     * @return 상점 수정내용
     */
    @Override
    public StoreDto update(Long id, StoreDto storeDto) {
        StoreEntity storeUpdate = storeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 상점이 없습니다"));


        UsersEntity currentUser = storeUpdate.getUser();

        storeUpdate.setUser(currentUser); // 기존 사용자 정보 유지
        storeUpdate.setStore(storeDto.getStore());
        storeUpdate.setStoreLocation(storeDto.getStoreLocation());
        storeUpdate.setStoreImg(storeDto.getStoreImg());
        storeUpdate.setStoreContents(storeDto.getStoreContents());
        storeUpdate.setRating(storeDto.getRating());
        storeUpdate.setStoreOpen(storeDto.getStoreOpen());
        storeUpdate.setStoreClose(storeDto.getStoreClose());
        storeUpdate.setStoreWeekOff(storeDto.getStoreWeekOff());
        storeUpdate.setUpdateAt(LocalDateTime.now());


        StoreEntity updatedStore = storeRepository.save(storeUpdate);


        return storeMapper.convertToDto(updatedStore);
    }

    /**
     * 상점 상세정보
     * @param id
     * @return 상세정보
     */
    @Override
    public StoreDto detail(Long id) {
        StoreEntity storeDetail = storeRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("해당 상점이 없습니다."));

        return storeMapper.convertToDto(storeDetail);
    }

    /**
     * 상점 삭제
     * @param id
     */
    @Override
    public void delete(Long id) {
        storeRepository.deleteById(id);
    }


}
