package com.marketplace.catalog.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.marketplace.catalog.entity.ProduitImage;
public interface ProduitImageRepository extends JpaRepository<ProduitImage, Long> {
}

