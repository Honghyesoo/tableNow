package zerobase.tableNow.domain.review.service;

import zerobase.tableNow.domain.review.dto.ReviewDto;
import zerobase.tableNow.domain.review.dto.UpdateDto;

import java.util.List;

public interface ReviewService {
    // 리뷰등록
    ReviewDto register(String store, ReviewDto reviewDto);

    //리뷰 목록
    List<ReviewDto> list();

    //리뷰 수정
    UpdateDto update(Long id, ReviewDto reviewDto);

    //리뷰 삭제
    void delete(Long id);
}