package com.dainis.spring_boot_library.dao;

import com.dainis.spring_boot_library.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
