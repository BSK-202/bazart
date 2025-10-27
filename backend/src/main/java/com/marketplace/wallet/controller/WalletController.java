package com.marketplace.wallet.controller;

import com.marketplace.wallet.dto.RechargeRequest;
import com.marketplace.wallet.dto.DebitRequest;
import com.marketplace.wallet.dto.WalletResponse;
import com.marketplace.wallet.entity.Wallet;
import com.marketplace.wallet.entity.HistoriqueWallet;
import com.marketplace.user.entity.Client;
import com.marketplace.user.service.ClientService;
import com.marketplace.wallet.service.Walletservice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "http://localhost:4200")
public class WalletController {

    private final Walletservice walletService;
    private final ClientService clientService;

    public WalletController(Walletservice walletService, ClientService clientService) {
        this.walletService = walletService;
        this.clientService = clientService;
    }

    private Client getAuthenticatedClient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("Utilisateur non authentifié");
        }

        String email = authentication.getName();
        return clientService.findByEmail(email)
                .orElseThrow(() -> new SecurityException("Utilisateur non trouvé"));
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getWalletBalance() {
        try {
            Client user = getAuthenticatedClient();
            Wallet wallet = walletService.getWalletByUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Solde récupéré avec succès");
            response.put("balance", wallet.getBalance());
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "Non authentifié"
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la récupération du solde"
                    ));
        }
    }

    @PostMapping("/recharge")
    public ResponseEntity<Map<String, Object>> rechargeWallet(@RequestBody RechargeRequest request) {
        try {
            Client user = getAuthenticatedClient();
            double amount = request.getAmount();

            if (amount <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "error", "Montant invalide"
                        ));
            }

            Wallet wallet = walletService.rechargeWallet(user, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Recharge du portefeuille effectuée avec succès !");
            response.put("newBalance", wallet.getBalance());
            response.put("amountAdded", amount);
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "Non authentifié"
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur serveur lors de la recharge"
                    ));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getWalletHistory() {
        try {
            Client user = getAuthenticatedClient();
            List<HistoriqueWallet> history = walletService.getUserHistory(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Historique récupéré avec succès");
            response.put("history", history);
            response.put("count", history.size());
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "Non authentifié"
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la récupération de l'historique"
                    ));
        }
    }

    @GetMapping("/history/{operationType}")
    public ResponseEntity<Map<String, Object>> getWalletHistoryByType(@PathVariable String operationType) {
        try {
            Client user = getAuthenticatedClient();
            List<HistoriqueWallet> history = walletService.getUserHistoryByType(user, operationType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Historique filtré récupéré avec succès");
            response.put("history", history);
            response.put("count", history.size());
            response.put("operationType", operationType);
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "Non authentifié"
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la récupération de l'historique"
                    ));
        }
    }

    @PostMapping("/debit")
    public ResponseEntity<Map<String, Object>> debitWallet(@RequestBody DebitRequest request) {
        try {
            Client user = getAuthenticatedClient();
            double amount = request.getAmount();
            String description = request.getDescription();

            if (amount <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "error", "Montant invalide"
                        ));
            }

            Wallet wallet = walletService.debitWallet(user, amount, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Débit effectué avec succès");
            response.put("newBalance", wallet.getBalance());
            response.put("amountDebited", amount);
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "Non authentifié"
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur serveur lors du débit"
                    ));
        }
    }
}