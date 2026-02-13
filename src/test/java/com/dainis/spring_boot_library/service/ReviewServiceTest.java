package com.dainis.spring_boot_library.service;

import com.dainis.spring_boot_library.dao.ReviewRepository;
import com.dainis.spring_boot_library.entity.Review;
import com.dainis.spring_boot_library.requestmodels.ReviewRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void testPostReviewSuccess() {
        String userEmail = "test@example.com";
        Long bookId = 1L;

        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setBookId(bookId);
        reviewRequest.setRating(5);
        reviewRequest.setReviewDescription(Optional.empty());

        when(reviewRepository.findByUserEmailAndBookId(userEmail, bookId)).thenReturn(null);

        assertDoesNotThrow(() -> reviewService.postReview(userEmail, reviewRequest));

        verify(reviewRepository, times(1)).save(any(Review.class));
    }
}
