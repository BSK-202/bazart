package com.marketplace.catalog.dto;

import java.util.List;

public class DomaineDTO {
    private String nomDomaine;
    private String description;
    private String image;
    private List<CategorieRequest> categories;

    // getters / setters
    public String getNomDomaine() {
        return nomDomaine;
    }
    public void setNomDomaine(String nomDomaine) {
        this.nomDomaine = nomDomaine;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public List<CategorieRequest> getCategories() {
        return categories;
    }
    public void setCategories(List<CategorieRequest> categories) {
        this.categories = categories;
    }

    public static class CategorieRequest {
        private String nomCategorie;
        private String description;
        private String image;

        public String getNomCategorie() {
            return nomCategorie;
        }
        public void setNomCategorie(String nomCategorie) {
            this.nomCategorie = nomCategorie;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getImage() {
            return image;
        }
        public void setImage(String image) {
            this.image = image;
        }
    }
}
