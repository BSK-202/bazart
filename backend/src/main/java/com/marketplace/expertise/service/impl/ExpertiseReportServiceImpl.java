package com.marketplace.expertise.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.expertise.entity.*;
import com.marketplace.expertise.repository.ExpertiseReportRepository;
import com.marketplace.expertise.repository.ExpertiseRequestRepository;
import com.marketplace.expertise.service.ExpertiseReportService;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.user.entity.Client;
import com.marketplace.user.entity.Expert;
import com.marketplace.user.repository.ExpertRepository;
import com.marketplace.wallet.service.Walletservice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertiseReportServiceImpl implements ExpertiseReportService {

    private final ExpertiseReportRepository reportRepository;
    private final ExpertiseRequestRepository requestRepository;
    private final ExpertRepository expertRepository;
    private final Walletservice walletservice;
    private final ProduitRepository produitRepository;
    private final NotificationService notificationService;

    private static final double EXPERT_PAYOUT_RATE = 0.7;
    private static final double ONLINE_PRICE = 50.0;
    private static final double ONSITE_PRICE = 100.0;
    private static final String REPORTS_DIR = "backend/assets/reports/";
    private static final String LOGO_PATH = "backend/assets/icon-bazart.png";
    private static final String PRODUCTS_IMAGES_DIR = "backend/assets/produits/";

    @Override
    public ExpertiseReport createReportAndGeneratePDF(
            Long expertiseRequestId,
            Long expertId,
            // AUTHENTICITÉ
            String authenticityLevel,
            Integer authenticityConfidence,
            String authenticityProof,
            // ÉTAT DU PRODUIT
            String productCondition,
            Integer conditionScore,
            String visualCondition,
            String functionalCondition,
            Boolean conformityDescription,
            // DESCRIPTION DÉTAILLÉE
            String detailedDescription,
            String testsPerformed,
            // ESTIMATION
            Double recommendedStartPrice,
            String priceJustification,
            // RECOMMANDATIONS
            String recommendation,
            String saleRecommendations,
            String buyerWarnings,
            // DOCUMENT
            MultipartFile uploadedDocument
    ) {
        ExpertiseRequest request = requestRepository.findById(expertiseRequestId)
                .orElseThrow(() -> new IllegalArgumentException("ExpertiseRequest not found"));
        Expert expert = expertRepository.findByClientId(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert not found"));

        // Créer le rapport
        ExpertiseReport report = new ExpertiseReport();
        report.setExpertiseRequest(request);
        report.setExpert(expert);

        // AUTHENTICITÉ
        report.setAuthenticityLevel(AuthenticityLevel.valueOf(authenticityLevel));
        report.setAuthenticityConfidence(authenticityConfidence);
        report.setAuthenticityProof(authenticityProof);

        // ÉTAT DU PRODUIT
        report.setProductCondition(ProductCondition.valueOf(productCondition));
        report.setConditionScore(conditionScore);
        report.setVisualCondition(visualCondition);
        report.setFunctionalCondition(functionalCondition);
        report.setConformityDescription(conformityDescription);

        // DESCRIPTION DÉTAILLÉE
        report.setDetailedDescription(detailedDescription);
        report.setTestsPerformed(testsPerformed);

        // ESTIMATION
        report.setRecommendedStartPrice(recommendedStartPrice);
        report.setPriceJustification(priceJustification);

        // RECOMMANDATIONS
        report.setRecommendation(ExpertiseRecommendation.valueOf(recommendation));
        report.setSaleRecommendations(saleRecommendations);
        report.setBuyerWarnings(buyerWarnings);

        report.setCreatedAt(LocalDateTime.now());

        // Génération PDF
        String pdfFilePath = generateProfessionalPDF(report, request);
        report.setReportPdfPath(pdfFilePath);

        ExpertiseReport saved = reportRepository.save(report);

        // Mise à jour du statut
        request.setStatus(ExpertiseStatus.EXPERTISED);
        request.setReportSubmissionDeadline(null);
        requestRepository.save(request);

        // Appliquer le rapport au produit
        applyReportToProduct(request, saved);

        // Envoyer notifications et créditer l'expert
        sendNotificationsAndCreditExpert(request, saved, expert);

        return saved;
    }

    // ========== GÉNÉRATION PDF PROFESSIONNELLE ==========
    private String generateProfessionalPDF(ExpertiseReport report, ExpertiseRequest request) {
        String filename = "rapport_expertise_" + request.getId() + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(REPORTS_DIR, filename);

        try {
            File dir = new File(REPORTS_DIR);
            if (!dir.exists()) dir.mkdirs();

            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Fonts professionnels
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(64, 45, 104));
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(64, 45, 104));
            Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);

            // ========== HEADER : LOGO + TITRE ==========
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 2});

            // Logo à gauche
            try {
                Image logo = Image.getInstance(LOGO_PATH);
                logo.scaleToFit(100, 60);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerTable.addCell(logoCell);
            } catch (Exception e) {
                PdfPCell emptyCell = new PdfPCell(new Phrase(""));
                emptyCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(emptyCell);
            }

            // Titre à droite
            Paragraph title = new Paragraph("RAPPORT D'EXPERTISE", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            PdfPCell titleCell = new PdfPCell(title);
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(titleCell);

            document.add(headerTable);
            document.add(new Paragraph(" "));

            // Ligne de séparation
            document.add(createSeparatorLine());
            document.add(new Paragraph(" "));

            // ========== INFORMATIONS VENDEUR ==========
            document.add(new Paragraph("INFORMATIONS DU VENDEUR", sectionFont));
            document.add(new Paragraph(" ", smallFont));

            Client vendeur = request.getVendeur();
            PdfPTable vendeurTable = createInfoTable();
            addInfoRow(vendeurTable, "Nom complet", vendeur.getPrenom() + " " + vendeur.getNom(), boldFont, normalFont);
            addInfoRow(vendeurTable, "Email", vendeur.getEmail(), boldFont, normalFont);
            addInfoRow(vendeurTable, "Téléphone", vendeur.getTel() != null ? vendeur.getTel() : "Non renseigné", boldFont, normalFont);
            addInfoRow(vendeurTable, "Ville", vendeur.getVille() != null ? vendeur.getVille() : "Non renseignée", boldFont, normalFont);
            document.add(vendeurTable);
            document.add(new Paragraph(" "));

            // Ligne de séparation
            document.add(createSeparatorLine());
            document.add(new Paragraph(" "));

            // ========== INFORMATIONS EXPERT ==========
            document.add(new Paragraph("INFORMATIONS DE L'EXPERT", sectionFont));
            document.add(new Paragraph(" ", smallFont));

            Client expertClient = report.getExpert().getClient();
            PdfPTable expertTable = createInfoTable();
            addInfoRow(expertTable, "Nom complet", expertClient.getPrenom() + " " + expertClient.getNom(), boldFont, normalFont);
            addInfoRow(expertTable, "Email", expertClient.getEmail(), boldFont, normalFont);
            addInfoRow(expertTable, "Date expertise", report.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")), boldFont, normalFont);
            addInfoRow(expertTable, "Méthode", request.getMethod() == ExpertiseMethod.ONLINE ? "Expertise en ligne" : "Expertise sur place", boldFont, normalFont);
            document.add(expertTable);
            document.add(new Paragraph(" "));

            // Ligne de séparation
            document.add(createSeparatorLine());
            document.add(new Paragraph(" "));

            // ========== INFORMATIONS PRODUIT + IMAGE ==========
            document.add(new Paragraph("INFORMATIONS DU PRODUIT", sectionFont));
            document.add(new Paragraph(" ", smallFont));

            Produit produit = request.getProduit();

            // Table avec 2 colonnes : infos à gauche, image à droite
            PdfPTable produitTable = new PdfPTable(2);
            produitTable.setWidthPercentage(100);
            produitTable.setWidths(new float[]{2, 1});

            // Colonne 1 : Informations
            PdfPTable infoTable = createInfoTable();
            addInfoRow(infoTable, "Nom du produit", produit.getNom(), boldFont, normalFont);
            addInfoRow(infoTable, "Catégorie", produit.getCategorie() != null ? produit.getCategorie().getNomCategorie() : "Non spécifiée", boldFont, normalFont);
            addInfoRow(infoTable, "État initial", produit.getEtat()!= null ? produit.getEtat() : "Non spécifié", boldFont, normalFont);
            if (produit.getDescription() != null && !produit.getDescription().isEmpty()) {
                addInfoRow(infoTable, "Description", produit.getDescription(), boldFont, normalFont);
            }
            PdfPCell infoCell = new PdfPCell(infoTable);
            infoCell.setBorder(Rectangle.NO_BORDER);
            produitTable.addCell(infoCell);

            // Colonne 2 : Image du produit
            try {
                String productImagePath = findProductImage(produit);
                if (productImagePath != null) {
                    Image productImage = Image.getInstance(productImagePath);
                    productImage.scaleToFit(150, 150);
                    PdfPCell imageCell = new PdfPCell(productImage);
                    imageCell.setBorder(Rectangle.BOX);
                    imageCell.setPadding(5);
                    imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    produitTable.addCell(imageCell);
                } else {
                    PdfPCell noImageCell = new PdfPCell(new Phrase("Image non disponible", smallFont));
                    noImageCell.setBorder(Rectangle.BOX);
                    noImageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    noImageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    noImageCell.setMinimumHeight(150);
                    produitTable.addCell(noImageCell);
                }
            } catch (Exception e) {
                PdfPCell errorCell = new PdfPCell(new Phrase("Erreur chargement image", smallFont));
                errorCell.setBorder(Rectangle.BOX);
                produitTable.addCell(errorCell);
            }

            document.add(produitTable);
            document.add(new Paragraph(" "));

            // Ligne de séparation
            document.add(createSeparatorLine());
            document.add(new Paragraph(" "));

            // ========== RÉSULTATS DE L'EXPERTISE ==========
            document.add(new Paragraph("RÉSULTATS DE L'EXPERTISE", sectionFont));
            document.add(new Paragraph(" ", smallFont));

            // SECTION 1: AUTHENTICITÉ
            addExpertiseSection(document, "1. AUTHENTICITÉ",
                    "Niveau : " + getAuthenticityLabelFr(report.getAuthenticityLevel()),
                    report.getAuthenticityConfidence() != null ? "Confiance : " + report.getAuthenticityConfidence() + "%" : null,
                    report.getAuthenticityProof(),
                    boldFont, normalFont, smallFont);

            // SECTION 2: ÉTAT DU PRODUIT
            addExpertiseSection(document, "2. ÉTAT DU PRODUIT",
                    "État général : " + getConditionLabelFr(report.getProductCondition()),
                    report.getConditionScore() != null ? "Note : " + report.getConditionScore() + "/10" : null,
                    combineConditionDetails(report),
                    boldFont, normalFont, smallFont);

            // SECTION 3: DESCRIPTION DÉTAILLÉE
            if (report.getDetailedDescription() != null || report.getTestsPerformed() != null) {
                addExpertiseSection(document, "3. DESCRIPTION DÉTAILLÉE",
                        report.getDetailedDescription(),
                        null,
                        report.getTestsPerformed() != null ? "Tests effectués : " + report.getTestsPerformed() : null,
                        boldFont, normalFont, smallFont);
            }

            // SECTION 4: ESTIMATION & RECOMMANDATIONS
            addExpertiseSection(document, "4. ESTIMATION ET RECOMMANDATION",
                    "Recommandation : " + getRecommendationLabelFr(report.getRecommendation()),
                    report.getRecommendedStartPrice() != null ?
                            String.format("Prix départ recommandé : %.2f DH", report.getRecommendedStartPrice()) : null,
                    combineRecommendationDetails(report),
                    boldFont, normalFont, smallFont);

            // Pied de page
            document.add(new Paragraph(" "));
            document.add(createSeparatorLine());
            Paragraph footer = new Paragraph("Document généré automatiquement le " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
                    smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return file.getAbsolutePath();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF: " + e.getMessage(), e);
        }
    }

    // ========== MÉTHODES UTILITAIRES POUR LE PDF ==========

    private Paragraph createSeparatorLine() {
        Paragraph separator = new Paragraph("_____________________________________________________________________________________");
        separator.setAlignment(Element.ALIGN_CENTER);
        separator.setSpacingAfter(5);
        return separator;
    }

    private PdfPTable createInfoTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{1, 2});
        } catch (DocumentException e) {
            // Ignore
        }
        return table;
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + " :", labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "Non renseigné", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);
        table.addCell(valueCell);
    }

    private void addExpertiseSection(Document document, String sectionTitle, String mainValue,
                                     String secondaryValue, String details,
                                     Font boldFont, Font normalFont, Font smallFont) throws DocumentException {
        // Titre de section
        Paragraph sectionPara = new Paragraph(sectionTitle, boldFont);
        sectionPara.setSpacingBefore(10);
        sectionPara.setSpacingAfter(5);
        document.add(sectionPara);

        // Valeur principale
        if (mainValue != null && !mainValue.trim().isEmpty()) {
            document.add(new Paragraph("• " + mainValue, normalFont));
        }

        // Valeur secondaire
        if (secondaryValue != null && !secondaryValue.trim().isEmpty()) {
            document.add(new Paragraph("• " + secondaryValue, normalFont));
        }

        // Détails
        if (details != null && !details.trim().isEmpty()) {
            Paragraph detailsPara = new Paragraph(details, smallFont);
            detailsPara.setIndentationLeft(15);
            detailsPara.setSpacingBefore(3);
            document.add(detailsPara);
        }

        document.add(new Paragraph(" ", smallFont));
    }

    private String findProductImage(Produit produit) {
        try {
            String produitDir = PRODUCTS_IMAGES_DIR + produit.getIdproduit() + "/";
            File dir = new File(produitDir);

            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) ->
                        name.toLowerCase().endsWith(".jpg") ||
                                name.toLowerCase().endsWith(".jpeg") ||
                                name.toLowerCase().endsWith(".png"));

                if (files != null && files.length > 0) {
                    return files[0].getAbsolutePath();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur recherche image produit: " + e.getMessage());
        }
        return null;
    }

    private String combineConditionDetails(ExpertiseReport report) {
        StringBuilder sb = new StringBuilder();
        if (report.getVisualCondition() != null && !report.getVisualCondition().isEmpty()) {
            sb.append("Aspect visuel : ").append(report.getVisualCondition()).append("\n");
        }
        if (report.getFunctionalCondition() != null && !report.getFunctionalCondition().isEmpty()) {
            sb.append("Aspect fonctionnel : ").append(report.getFunctionalCondition()).append("\n");
        }
        if (report.getConformityDescription() != null) {
            sb.append("Conforme à la description : ").append(report.getConformityDescription() ? "Oui" : "Non");
        }
        return sb.toString();
    }

    private String combineRecommendationDetails(ExpertiseReport report) {
        StringBuilder sb = new StringBuilder();
        if (report.getPriceJustification() != null && !report.getPriceJustification().isEmpty()) {
            sb.append("Justification prix : ").append(report.getPriceJustification()).append("\n");
        }
        if (report.getSaleRecommendations() != null && !report.getSaleRecommendations().isEmpty()) {
            sb.append("Recommandations vente : ").append(report.getSaleRecommendations()).append("\n");
        }
        if (report.getBuyerWarnings() != null && !report.getBuyerWarnings().isEmpty()) {
            sb.append("Points vigilance acheteur : ").append(report.getBuyerWarnings());
        }
        return sb.toString();
    }

    // Labels en français pour le PDF
    private String getAuthenticityLabelFr(AuthenticityLevel level) {
        return switch (level) {
            case AUTHENTIC -> "Authentique";
            case PROBABLE -> "Probablement authentique";
            case UNKNOWN -> "Inconnu";
            case FAKE -> "Contrefaçon";
        };
    }

    private String getConditionLabelFr(ProductCondition condition) {
        return switch (condition) {
            case EXCELLENT -> "Excellent";
            case VERY_GOOD -> "Très bon";
            case GOOD -> "Bon";
            case FAIR -> "Correct";
            case POOR -> "Mauvais";
        };
    }

    private String getRecommendationLabelFr(ExpertiseRecommendation recommendation) {
        return switch (recommendation) {
            case AUTHORISE_AUCTION -> "Autoriser la vente";
            case REFUSE -> "Refuser";
            case REQUEST_MORE_INFO -> "Demander plus d'informations";
        };
    }

    // ========== APPLICATION DU RAPPORT AU PRODUIT ==========
    private void applyReportToProduct(ExpertiseRequest request, ExpertiseReport report) {
        Produit produit = request.getProduit();

        // Construire un commentaire public qui combine les informations pertinentes
        StringBuilder publicComment = new StringBuilder();

        if (report.getDetailedDescription() != null && !report.getDetailedDescription().isEmpty()) {
            publicComment.append(report.getDetailedDescription());
        }

        if (report.getSaleRecommendations() != null && !report.getSaleRecommendations().isEmpty()) {
            if (publicComment.length() > 0) publicComment.append("\n\n");
            publicComment.append("Recommandations : ").append(report.getSaleRecommendations());
        }

        if (report.getBuyerWarnings() != null && !report.getBuyerWarnings().isEmpty()) {
            if (publicComment.length() > 0) publicComment.append("\n\n");
            publicComment.append("Points d'attention : ").append(report.getBuyerWarnings());
        }

        produit.setExpertisePublicComment(publicComment.toString());
        produit.setExpertiseAuthenticityLevel(
                report.getAuthenticityLevel() != null ? report.getAuthenticityLevel().name() : null
        );
        produit.setExpertiseProductCondition(
                report.getProductCondition() != null ? report.getProductCondition().name() : null
        );

        switch (report.getRecommendation()) {
            case AUTHORISE_AUCTION -> {
                produit.setEtat_expertise("expertise_validee");
                produit.setExpertiseApproved(true);
            }
            case REFUSE -> {
                produit.setEtat_expertise("expertise_refusee");
                produit.setExpertiseApproved(false);
            }
            case REQUEST_MORE_INFO -> {
                produit.setEtat_expertise("expertise_incomplete");
                produit.setExpertiseApproved(false);
            }
        }
        produitRepository.save(produit);
    }

    // ========== NOTIFICATIONS ET CRÉDIT EXPERT ==========
    private void sendNotificationsAndCreditExpert(ExpertiseRequest request,
                                                  ExpertiseReport report,
                                                  Expert expert) {
        String productName = request.getProduit().getNom();
        Long vendeurId = request.getVendeur().getIdclient();
        String expertFullName = expert.getClient().getPrenom() + " " + expert.getClient().getNom();

        // Construire le message vendeur
        StringBuilder vendeurMessage = new StringBuilder();
        vendeurMessage.append("Produit : \"").append(productName).append("\"\n");
        vendeurMessage.append("Expert : ").append(expertFullName).append("\n\n");
        vendeurMessage.append("📊 Résultats de l'expertise :\n");
        vendeurMessage.append("• État : ").append(getConditionLabelFr(report.getProductCondition())).append("\n");
        vendeurMessage.append("• Authenticité : ").append(getAuthenticityLabelFr(report.getAuthenticityLevel())).append("\n");

        if (report.getRecommendedStartPrice() != null) {
            vendeurMessage.append(String.format("• Prix départ recommandé : %.2f DH\n",
                    report.getRecommendedStartPrice()));
        }

        vendeurMessage.append("\n");

        // Ajouter selon la recommandation
        switch (report.getRecommendation()) {
            case AUTHORISE_AUCTION -> {
                vendeurMessage.insert(0, "✅ Expertise validée - Produit approuvé !\n\n");
                vendeurMessage.append("✨ Votre produit peut maintenant être mis aux enchères !\n");
                vendeurMessage.append("📄 Le rapport d'expertise complet est disponible dans votre espace vendeur.");
                if (report.getDetailedDescription() != null && !report.getDetailedDescription().trim().isEmpty()) {
                    vendeurMessage.append("\n\n📝 Commentaire de l'expert :\n").append(report.getDetailedDescription());
                }
            }
            case REFUSE -> {
                vendeurMessage.insert(0, "❌ Expertise - Produit non approuvé\n\n");
                vendeurMessage.append("Malheureusement, l'expert n'a pas pu valider votre produit pour la mise aux enchères.\n\n");
                if (report.getDetailedDescription() != null && !report.getDetailedDescription().trim().isEmpty()) {
                    vendeurMessage.append("📝 Motif du refus :\n").append(report.getDetailedDescription()).append("\n\n");
                }
                vendeurMessage.append("💡 Consultez le rapport d'expertise complet pour plus de détails.");
            }
            case REQUEST_MORE_INFO -> {
                vendeurMessage.insert(0, "ℹ️ Informations complémentaires requises\n\n");
                vendeurMessage.append("L'expert a besoin d'informations supplémentaires pour finaliser son expertise.\n\n");
                if (report.getDetailedDescription() != null && !report.getDetailedDescription().trim().isEmpty()) {
                    vendeurMessage.append("📝 Informations demandées :\n").append(report.getDetailedDescription()).append("\n\n");
                }
                vendeurMessage.append("⚡ Veuillez fournir les informations demandées dès que possible.");
            }
        }

        // Envoyer notification au vendeur
        Map<String, Object> vendeurNotif = new HashMap<>();
        vendeurNotif.put("message", vendeurMessage.toString());
        vendeurNotif.put("productId", request.getProduit().getIdproduit());
        vendeurNotif.put("expertiseRequestId", request.getId());
        notificationService.processEvent(NotificationType.MESSAGE, Set.of(vendeurId), vendeurNotif);

        // Notification + crédit expert
        if (expert.getClient() != null) {
            Long expertUserId = expert.getClient().getIdclient();

            double totalPrice = request.getPrice() != null ? request.getPrice() :
                    (request.getMethod() == ExpertiseMethod.ONLINE ? ONLINE_PRICE : ONSITE_PRICE);
            double expertShare = totalPrice * EXPERT_PAYOUT_RATE;

            StringBuilder expertMessage = new StringBuilder();
            expertMessage.append("✅ Rapport d'expertise soumis avec succès\n\n");
            expertMessage.append("Produit : \"").append(productName).append("\"\n");

            switch (report.getRecommendation()) {
                case AUTHORISE_AUCTION ->
                        expertMessage.append("Votre décision : Produit validé pour mise aux enchères\n\n")
                                .append("Le vendeur a été notifié que son produit est approuvé.");
                case REFUSE ->
                        expertMessage.append("Votre décision : Produit non approuvé\n\n")
                                .append("Le vendeur a été informé du refus avec vos commentaires.");
                case REQUEST_MORE_INFO ->
                        expertMessage.append("Votre décision : Informations complémentaires demandées\n\n")
                                .append("Le vendeur a été notifié et devra fournir les informations manquantes.");
            }

            expertMessage.append("\n\n💰 Rémunération : ")
                    .append(String.format("%.2f DH", expertShare))
                    .append("\nLe montant a été crédité sur votre portefeuille.");

            Map<String, Object> expertNotif = new HashMap<>();
            expertNotif.put("message", expertMessage.toString());
            expertNotif.put("expertiseRequestId", request.getId());
            expertNotif.put("payoutAmount", expertShare);
            notificationService.processEvent(NotificationType.MESSAGE, Set.of(expertUserId), expertNotif);

            // Crédit wallet
            String desc = "Rémunération expertise du produit \"" + request.getProduit().getNom() + "\"";
            walletservice.rechargeWallet(expert.getClient(), expertShare);

            // Notification de crédit wallet
            Map<String, Object> walletNotif = new HashMap<>();
            walletNotif.put("amount", expertShare);
            walletNotif.put("message",
                    "💰 Crédit portefeuille\n\n" +
                            "Montant : " + String.format("%.2f DH", expertShare) + "\n" +
                            "Motif : " + desc
            );
            notificationService.processEvent(NotificationType.PAYMENT_RECEIVED,
                    Set.of(expert.getClient().getIdclient()), walletNotif);
        }
    }

    @Override
    public Optional<ExpertiseReport> findByExpertiseRequestId(Long requestId) {
        return reportRepository.findByExpertiseRequestId(requestId);
    }
}