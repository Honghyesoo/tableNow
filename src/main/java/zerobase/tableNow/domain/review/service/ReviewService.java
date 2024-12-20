package zerobase.tableNow.domain.review.service;

import zerobase.tableNow.domain.review.dto.ReviewDto;
import zerobase.tableNow.domain.review.dto.UpdateDto;
import zerobase.tableNow.domain.user.entity.UsersEntity;

import java.util.List;

public interface ReviewService {
    // 리뷰등록
    ReviewDto register(String store, ReviewDto reviewDto);

    //리뷰 목록
    List<ReviewDto> listByStore(String store);

    //리뷰 수정
    UpdateDto update(String user, ReviewDto reviewDto);

    //리뷰 삭제
    void delete(String user,Long id);



}
