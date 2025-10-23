from flask import Flask, request, jsonify
from transformers import AutoImageProcessor, AutoModelForImageClassification
from PIL import Image
import torch
import os
import tempfile

app = Flask(__name__)

# Configuration - Choisissez votre modèle
MODELS = {
    "organika": "Organika/sdxl-detector",  # Recommandé - Détecte SDXL, Midjourney 5, DALLE-3
    "dima": "dima806/ai_art_image_detection",  # Alternative populaire
    "ateeq": "Ateeqq/ai-vs-human-image-detector"  # Autre option
}

# Sélectionnez le modèle à utiliser
SELECTED_MODEL = "organika"

print(f"🔄 Chargement du modèle {MODELS[SELECTED_MODEL]}...")
processor = AutoImageProcessor.from_pretrained(MODELS[SELECTED_MODEL])
model = AutoModelForImageClassification.from_pretrained(MODELS[SELECTED_MODEL])
print("✅ Modèle chargé avec succès!\n")

@app.route("/", methods=["GET"])
def home():
    """Page d'accueil de l'API"""
    return jsonify({
        "service": "🤖 API de Détection d'Images IA",
        "version": "1.0",
        "modele_actuel": MODELS[SELECTED_MODEL],
        "endpoints": {
            "/verify-image": "POST - Vérifier si une image est générée par IA",
            "/health": "GET - Vérifier l'état du service"
        },
        "formats_supportes": ["JPG", "JPEG", "PNG", "WEBP", "BMP"],
        "utilisation": "Envoyez une image via POST avec le champ 'file'"
    })

@app.route("/verify-image", methods=["POST"])
def verify_image():
    """Endpoint principal pour vérifier une image"""
    
    # Validation du fichier
    if "file" not in request.files:
        return jsonify({"error": "❌ Aucun fichier fourni. Utilisez le champ 'file'"}), 400

    file = request.files["file"]
    
    if file.filename == "":
        return jsonify({"error": "❌ Nom de fichier vide"}), 400

    # Vérification de l'extension
    allowed_extensions = {'.jpg', '.jpeg', '.png', '.webp', '.bmp'}
    file_ext = os.path.splitext(file.filename)[1].lower()
    
    if file_ext not in allowed_extensions:
        return jsonify({
            "error": f"❌ Format '{file_ext}' non supporté",
            "formats_acceptes": list(allowed_extensions)
        }), 400

    # Sauvegarde temporaire
    with tempfile.NamedTemporaryFile(delete=False, suffix=file_ext) as tmp:
        file.save(tmp.name)
        img_path = tmp.name

    try:
        # Chargement de l'image
        img = Image.open(img_path).convert("RGB")
        
        # Prétraitement
        inputs = processor(images=img, return_tensors="pt")
        
        # Prédiction
        with torch.no_grad():
            outputs = model(**inputs)
            logits = outputs.logits
            probabilities = torch.nn.functional.softmax(logits, dim=-1)
        
        # Extraction des résultats
        predicted_class = logits.argmax(-1).item()
        confidence = probabilities[0][predicted_class].item()
        
        # Récupération des labels
        labels = model.config.id2label
        prediction = labels[predicted_class]
        
        # Calcul des probabilités pour chaque classe
        num_classes = probabilities.shape[1]
        all_probs = {}
        
        for i in range(num_classes):
            label = labels[i].lower()
            prob = probabilities[0][i].item()
            all_probs[label] = round(prob * 100, 2)
        
        # Détermination si c'est de l'IA
        # Les labels varient selon les modèles: artificial/real, ai/human, fake/real
        ai_keywords = ['artificial', 'ai', 'fake', 'generated']
        is_ai = any(keyword in prediction.lower() for keyword in ai_keywords)
        
        # Statut final
        if is_ai:
            status = "❌ REJETÉ - Image générée par IA"
            emoji = "🤖"
        else:
            status = "✅ APPROUVÉ - Photo réelle"
            emoji = "📷"

        # Réponse complète
        response = {
            "resultat": status,
            "emoji": emoji,
            "fichier": file.filename,
            "est_ia": is_ai,
            "confiance": round(confidence * 100, 2),
            "prediction_brute": prediction,
            "probabilites": all_probs,
            "dimensions": {
                "largeur": img.width,
                "hauteur": img.height
            },
            "modele_utilise": MODELS[SELECTED_MODEL]
        }
        
        print(f"\n{emoji} Analyse: {file.filename}")
        print(f"   → {status}")
        print(f"   → Confiance: {response['confiance']}%\n")
        
        return jsonify(response), 200
        
    except Exception as e:
        return jsonify({
            "error": f"❌ Erreur lors du traitement: {str(e)}",
            "details": "Vérifiez que l'image est valide et non corrompue"
        }), 500
        
    finally:
        # Nettoyage du fichier temporaire
        if os.path.exists(img_path):
            os.remove(img_path)

@app.route("/health", methods=["GET"])
def health():
    """Vérifier l'état du service"""
    return jsonify({
        "status": "✅ Service opérationnel",
        "modele_charge": model is not None,
        "modele_actuel": MODELS[SELECTED_MODEL]
    }), 200

@app.route("/models", methods=["GET"])
def list_models():
    """Lister les modèles disponibles"""
    return jsonify({
        "modeles_disponibles": MODELS,
        "modele_actuel": SELECTED_MODEL,
        "note": "Pour changer de modèle, modifiez SELECTED_MODEL dans app.py"
    }), 200

if __name__ == "__main__":
    print("=" * 60)
    print("🚀 Serveur de détection d'images IA")
    print("=" * 60)
    print(f"📍 URL: http://localhost:5000")
    print(f"🤖 Modèle: {MODELS[SELECTED_MODEL]}")
    print(f"📝 Documentation: http://localhost:5000")
    print("=" * 60 + "\n")
    
    app.run(host="0.0.0.0", port=5000, debug=True)