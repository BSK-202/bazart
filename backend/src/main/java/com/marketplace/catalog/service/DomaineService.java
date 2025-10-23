package com.marketplace.catalog.service;

import com.marketplace.catalog.entity.Domaine;

import com.marketplace.catalog.repository.DomaineRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
}