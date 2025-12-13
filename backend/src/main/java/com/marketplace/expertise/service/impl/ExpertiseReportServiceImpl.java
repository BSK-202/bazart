package com.marketplace.expertise.service.impl;

import com.lowagie.text.Font;
import com.marketplace.catalog.entity.Produit;
import com.marketplace.catalog.repository.ProduitRepository;
import com.marketplace.expertise.entity.*;
import com.marketplace.expertise.repository.ExpertiseReportRepository;
import com.marketplace.expertise.repository.ExpertiseRequestRepository;
import com.marketplace.expertise.service.ExpertiseReportService;
import com.marketplace.notification.entity.NotificationType;
import com.marketplace.notification.service.NotificationService;
import com.marketplace.user.entity.Expert;
import com.marketplace.user.repository.ExpertRepository;
import com.marketplace.wallet.service.Walletservice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private static final double EXPERT_PAYOUT_RATE = 0.7; // 70% pour l’expert
    private static final double ONLINE_PRICE = 50.0;
    private static final double ONSITE_PRICE = 100.0;

    private static final String REPORTS_DIR = "backend/assets/reports/";

    @Override
    public ExpertiseReport createReportAndGeneratePDF(
            Long expertiseRequestId,
            Long expertId,
            String productCondition,
            String authenticityLevel,
            Double estimatedMinPrice,
            Double estimatedMaxPrice,
            Double recommendedStartPrice,
            String recommendation,
            String commentsPublic,
            String commentsInternal,
            MultipartFile uploadedDocument
    ) {
        ExpertiseRequest request = requestRepository.findById(expertiseRequestId)
                .orElseThrow(() -> new IllegalArgumentException("ExpertiseRequest not found"));
        Expert expert = expertRepository.findByClientId(expertId)
                .orElseThrow(() -> new IllegalArgumentException("Expert not found"));

        ExpertiseReport report = new ExpertiseReport();
        report.setExpertiseRequest(request);
        report.setExpert(expert);
        report.setProductCondition(ProductCondition.valueOf(productCondition));
        report.setAuthenticityLevel(AuthenticityLevel.valueOf(authenticityLevel));
        report.setEstimatedMinPrice(estimatedMinPrice);
        report.setEstimatedMaxPrice(estimatedMaxPrice);
        report.setRecommendedStartPrice(recommendedStartPrice);
        report.setRecommendation(ExpertiseRecommendation.valueOf(recommendation));
        report.setCommentsPublic(commentsPublic);
        report.setCommentsInternal(commentsInternal);
        report.setCreatedAt(LocalDateTime.now());

        // Document upload ou génération PDF
        String pdfFilePath;
        if (uploadedDocument != null && !uploadedDocument.isEmpty()) {
            try {
                File reportsDir = new File(REPORTS_DIR);
                if (!reportsDir.exists()) {
                    reportsDir.mkdirs();
                }
                String filename = "report_" + expertiseRequestId + "_" + System.currentTimeMillis() + ".pdf";
                File pdfFile = new File(reportsDir, filename);
                try (FileOutputStream out = new FileOutputStream(pdfFile)) {
                    out.write(uploadedDocument.getBytes());
                }
                pdfFilePath = pdfFile.getAbsolutePath();
                report.setReportPdfPath(pdfFilePath);
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'enregistrement du document PDF", e);
            }
        } else {
            pdfFilePath = generatePDFReport(report, request);
            report.setReportPdfPath(pdfFilePath);
        }

        ExpertiseReport saved = reportRepository.save(report);

        request.setStatus(ExpertiseStatus.EXPERTISED); // ou COMPLETED
        request.setReportSubmissionDeadline(null);
        requestRepository.save(request);


        // Project report to product visibility + state
        applyReportToProduct(request, saved);

// ========== NOTIFICATIONS SELON LA RECOMMANDATION ==========

        String productName = request.getProduit().getNom();
        Long vendeurId = request.getVendeur().getIdclient();

        switch (saved.getRecommendation()) {

            case AUTHORISE_AUCTION -> {
                // Notification au vendeur - Expertise validée ✅
                Map<String, Object> vendeurNotif = new HashMap<>();
                vendeurNotif.put("message",
                        "✅ Expertise validée - Produit approuvé !\n\n" +
                                "Produit : \"" + productName + "\"\n" +
                                "Expert : " + expert.getClient().getPrenom() + " " + expert.getClient().getNom() + "\n\n" +
                                "🎉 Félicitations ! Votre produit a été expertisé et validé.\n\n" +
                                "📊 Résultats de l'expertise :\n" +
                                "• État : " + report.getProductCondition() + "\n" +
                                "• Authenticité : " + report.getAuthenticityLevel() + "\n" +
                                (estimatedMinPrice != null && estimatedMaxPrice != null ?
                                        "• Estimation : " + String.format("%.2f - %.2f DH", estimatedMinPrice, estimatedMaxPrice) + "\n" : "") +
                                (recommendedStartPrice != null ?
                                        "• Prix de départ recommandé : " + String.format("%.2f DH", recommendedStartPrice) + "\n" : "") +
                                "\n" +
                                "✨ Votre produit peut maintenant être mis aux enchères !\n" +
                                "📄 Le rapport d'expertise complet est disponible dans votre espace vendeur.\n\n" +
                                "Prochaine étape : Configurez et lancez votre enchère."
                );
                notificationService.processEvent(NotificationType.MESSAGE, Set.of(vendeurId), vendeurNotif);
            }

            case REFUSE -> {
                // Notification au vendeur - Expertise refusée ❌
                Map<String, Object> vendeurNotif = new HashMap<>();
                vendeurNotif.put("message",
                        "❌ Expertise - Produit non approuvé\n\n" +
                                "Produit : \"" + productName + "\"\n" +
                                "Expert : " + expert.getClient().getPrenom() + " " + expert.getClient().getNom() + "\n\n" +
                                "Malheureusement, l'expert n'a pas pu valider votre produit pour la mise aux enchères.\n\n" +
                                "📊 Résultats de l'expertise :\n" +
                                "• État : " + report.getProductCondition() + "\n" +
                                "• Authenticité : " + report.getAuthenticityLevel() + "\n\n" +
                                "📝 Motif du refus :\n" +
                                (commentsPublic != null && !commentsPublic.trim().isEmpty()
                                        ? commentsPublic
                                        : "Consultez le rapport d'expertise pour plus de détails.") + "\n\n" +
                                "💡 Que faire maintenant ?\n" +
                                "• Consultez le rapport d'expertise complet dans votre espace\n" +
                                "• Vous pouvez contacter le support pour plus d'informations\n" +
                                "• Si possible, apportez des améliorations et resoumettez votre produit"
                );
                notificationService.processEvent(NotificationType.MESSAGE, Set.of(vendeurId), vendeurNotif);
            }

            case REQUEST_MORE_INFO -> {
                // Notification au vendeur - Informations complémentaires demandées ℹ️
                Map<String, Object> vendeurNotif = new HashMap<>();
                vendeurNotif.put("message",
                        "ℹ️ Informations complémentaires requises\n\n" +
                                "Produit : \"" + productName + "\"\n" +
                                "Expert : " + expert.getClient().getPrenom() + " " + expert.getClient().getNom() + "\n\n" +
                                "L'expert a besoin d'informations supplémentaires pour finaliser son expertise.\n\n" +
                                "📊 Analyse actuelle :\n" +
                                "• État : " + report.getProductCondition() + "\n" +
                                "• Authenticité : " + report.getAuthenticityLevel() + "\n\n" +
                                "📝 Informations demandées :\n" +
                                (commentsPublic != null && !commentsPublic.trim().isEmpty()
                                        ? commentsPublic
                                        : "Consultez le rapport d'expertise pour connaître les détails demandés.") + "\n\n" +
                                "📄 Le rapport d'expertise complet est disponible dans votre espace vendeur.\n\n" +
                                "⚡ Action requise :\n" +
                                "Veuillez fournir les informations demandées dès que possible.\n" +
                                "Une fois les informations ajoutées, l'expertise pourra être finalisée."
                );
                notificationService.processEvent(NotificationType.MESSAGE, Set.of(vendeurId), vendeurNotif);
            }
        }

        // ========== NOTIFICATION À L'EXPERT - CONFIRMATION ==========
        if (expert.getClient() != null) {
            Long expertUserId = expert.getClient().getIdclient();
            Map<String, Object> expertNotif = new HashMap<>();

            String expertMessage = "✅ Rapport d'expertise soumis avec succès\n\n" +
                    "Produit : \"" + productName + "\"\n" +
                    "Votre décision : ";

            switch (saved.getRecommendation()) {
                case AUTHORISE_AUCTION ->
                        expertMessage += "Produit validé pour mise aux enchères\n\n" +
                                "Le vendeur a été notifié que son produit est approuvé.";
                case REFUSE ->
                        expertMessage += "Produit non approuvé\n\n" +
                                "Le vendeur a été informé du refus avec vos commentaires.";
                case REQUEST_MORE_INFO ->
                        expertMessage += "Informations complémentaires demandées\n\n" +
                                "Le vendeur a été notifié et devra fournir les informations manquantes.";
            }

            expertMessage += "\n\n💰 Rémunération : " +
                    String.format("DH") +
                    "\nLe montant a été crédité sur votre portefeuille.";

            expertNotif.put("message", expertMessage);
            notificationService.processEvent(NotificationType.MESSAGE, Set.of(expertUserId), expertNotif);
        }

        // ========== CRÉDIT EXPERT ==========
        double totalPrice = request.getPrice() != null
                ? request.getPrice()
                : (request.getMethod() == ExpertiseMethod.ONLINE ? ONLINE_PRICE : ONSITE_PRICE);
        double expertShare = totalPrice * EXPERT_PAYOUT_RATE;

        if (expert.getClient() != null) {
            String desc = "Rémunération expertise du produit \"" + request.getProduit().getNom() + "\"";
            walletservice.rechargeWallet(expert.getClient(), expertShare);

            // Notification de crédit wallet (optionnelle, peut être gérée par WalletService)
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

        return saved;
    }
    private String generatePDFReport(ExpertiseReport report, ExpertiseRequest request) {
        String filename = "report_" + request.getId() + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(REPORTS_DIR, filename);

        try {
            File dir = new File(REPORTS_DIR);
            if (!dir.exists()) dir.mkdirs();

            Document pdfDoc = new Document(PageSize.A4, 48, 48, 24, 24);
            PdfWriter.getInstance(pdfDoc, new FileOutputStream(file));
            pdfDoc.open();

            // Logo
            try {
                String logoPath = "backend/assets/icon-bazart.png";
                Image logo = Image.getInstance(logoPath);
                logo.scaleToFit(120, 70);
                logo.setAlignment(Image.ALIGN_CENTER);
                logo.setSpacingAfter(16);
                pdfDoc.add(logo);
            } catch (Exception imgEx) {
                System.err.println("Erreur chargement logo PDF: " + imgEx.getMessage());
            }

            Font titleFont = new Font(Font.HELVETICA, 19, Font.BOLD, new Color(64,45,104));
            Font mainFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.BLACK);
            Font boldFont = new Font(Font.HELVETICA, 12, Font.BOLD);

            Paragraph title = new Paragraph("RAPPORT D'EXPERTISE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            pdfDoc.add(title);

            pdfDoc.add(new Paragraph("Produit : " + request.getProduit().getNom() +
                    " (ID: " + request.getProduit().getIdproduit() + ")", boldFont));
            pdfDoc.add(new Paragraph("Expert : " +
                    report.getExpert().getClient().getPrenom() +
                    " " + report.getExpert().getClient().getNom(), mainFont));

            pdfDoc.add(new Paragraph("\nÉtat du produit : " + report.getProductCondition(), mainFont));
            pdfDoc.add(new Paragraph("Authenticité : " + report.getAuthenticityLevel(), mainFont));
            pdfDoc.add(new Paragraph("Prix estimé : " +
                    valueOrNA(report.getEstimatedMinPrice()) + " - " +
                    valueOrNA(report.getEstimatedMaxPrice()), mainFont));
            pdfDoc.add(new Paragraph("Prix de départ recommandé : " +
                    valueOrNA(report.getRecommendedStartPrice()), mainFont));
            pdfDoc.add(new Paragraph("Recommandation : " + report.getRecommendation(), mainFont));

            pdfDoc.add(new Paragraph("\nCommentaire Public :", boldFont));
            pdfDoc.add(new Paragraph(report.getCommentsPublic() != null ? report.getCommentsPublic() : "Aucun", mainFont));

            pdfDoc.add(new Paragraph("\nCommentaire Interne :", boldFont));
            pdfDoc.add(new Paragraph(report.getCommentsInternal() != null ? report.getCommentsInternal() : "Aucun", mainFont));

            pdfDoc.add(new Paragraph("\nDate du rapport : " + report.getCreatedAt().toString(), mainFont));

            pdfDoc.close();
            return file.getAbsolutePath();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate PDF", ex);
        }
    }



    // Helper to show N/A if null
    private String valueOrNA(Double value) {
        return value != null ? String.format("%.2f DH", value) : "N/A";
    }

    private void applyReportToProduct(ExpertiseRequest request, ExpertiseReport report) {
        Produit produit = request.getProduit();
        produit.setExpertisePublicComment(report.getCommentsPublic());
        produit.setExpertiseAuthenticityLevel(report.getAuthenticityLevel() != null ? report.getAuthenticityLevel().name() : null);
        produit.setExpertiseProductCondition(report.getProductCondition() != null ? report.getProductCondition().name() : null);

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
                produit.setEtat_expertise("expertise_incomplete"); // needs more info, no badge
                produit.setExpertiseApproved(false);
            }
        }
        produitRepository.save(produit);
    }

    @Override
    public Optional<ExpertiseReport> findByExpertiseRequestId(Long requestId) {
        return reportRepository.findByExpertiseRequestId(requestId);
    }

}