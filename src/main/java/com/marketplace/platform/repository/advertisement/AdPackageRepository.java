package com.marketplace.platform.repository.advertisement;

import com.marketplace.platform.domain.advertisement.AdPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdPackageRepository extends JpaRepository<AdPackage, Long> {
    @Query("SELECT p FROM AdPackage p WHERE p.visibilityLevel <> 'HIDDEN'")
    List<AdPackage> findAllActive();

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM AdPackage p WHERE p.name = :name AND p.visibilityLevel <> 'HIDDEN'")
    boolean existsByNameAndVisible(String name);

    @Query("SELECT p FROM AdPackage p WHERE p.price <= :maxPrice AND p.visibilityLevel <> 'HIDDEN'")
    List<AdPackage> findActiveByMaxPrice(BigDecimal maxPrice);
}