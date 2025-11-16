package com.marketplace.user.repository;

import com.marketplace.user.entity.Expert;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertRepository extends JpaRepository<Expert, Long> {

    Optional<Expert> findByClientId(Long clientId);

    // 🔥 CORRECTION : Utiliser la propriété correcte - domaine.idDomaine
    @Query("SELECT e FROM Expert e WHERE e.domaine.idDomaine = :domaineId")
    List<Expert> findByDomaineId(@Param("domaineId") Long domaineId);

    List<Expert> findByIsActiveFalse();

    @Query("SELECT e FROM Expert e WHERE e.client.email = :email")
    Optional<Expert> findByClientEmail(@Param("email") String email);

    boolean existsByClientId(Long clientId);

    //  Alternative avec le nom correct de la propriété
    List<Expert> findByDomaineIdDomaine(Long idDomaine);


    @Query("SELECT e FROM Expert e WHERE e.isActive = false AND e.client.emailVerified = true")
    List<Expert> findByIsActiveFalseAndClientEmailVerifiedTrue();

    @Query("SELECT e.isActive FROM Expert e WHERE e.client.idclient = :clientId")
    Optional<Boolean> findActiveStatusByClientId(@Param("clientId") Long clientId);

    @Modifying
    @Transactional
    @Query("UPDATE Expert e SET e.isActive = true WHERE e.id = :id")
    int activateExpert(@Param("id") Long id);


    List<Expert> findByIsActiveTrue();


}