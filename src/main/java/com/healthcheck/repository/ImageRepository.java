package com.healthcheck.repository;

import com.healthcheck.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByProductId(Long productId);
    List<Image> findByUserId(Long userId);
    Optional<Image> findByImageIdAndProductId(Long imageId, Long productId);
}