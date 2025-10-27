package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Domaine;

import com.marketplace.catalog.repository.DomaineRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DomaineService {

    private final DomaineRepository DomaineDAO;

    public DomaineService(DomaineRepository DomaineDAO) {
        this.DomaineDAO = DomaineDAO;
    }

    // Une seule fonction qui retourne la liste des domaines
    public List<Domaine> getDomaines() {
        return DomaineDAO.findAll();
    }

    public Domaine save(Domaine domaine) {
        return DomaineDAO.save(domaine);
    }

    public Optional<Domaine> findById(Long id) {
        return DomaineDAO.findById(id);
    }

    public void deleteById(Long id) {
        DomaineDAO.deleteById(id);
    }
    public Optional<Domaine> getDomaineById(Long id) {
        return DomaineDAO.findById(id);
    }
}