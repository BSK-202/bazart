package com.marketplace.user.service;

import com.marketplace.user.entity.Expert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExpertService {

    List<Expert> getAllExperts();

    Optional<Expert> getExpertById(Long id);

    Expert saveExpert(Expert expert);

    Expert createExpert(Expert expert);

    Expert createExpertWithClient(Long clientId, Expert expert);

    boolean expertExistsById(Long id);

    void deleteExpert(Long id);

    Optional<Expert> getExpertByClientId(Long clientId);

    List<Expert> getExpertsByDomaine(Long domaineId);

    List<Expert> getInactiveExperts();
    List<Expert> getInactiveExpertsWithVerifiedEmail();

    // 🔥 MÉTHODE DÉPRÉCIÉE - L'upload est géré dans le Controller
    @Deprecated
    List<String> uploadSignatureFiles(Long expertId, List<MultipartFile> signatureFiles) throws IOException;

    Map<String, Object> checkExpertStatusByClientId(Long clientId);


    boolean activateExpert(Long id);
    List<Expert> findByIsActiveTrue();


}