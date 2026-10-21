# Proposition B — Coffre: complete screen inventory

Phone (Compact) unless noted. IDs are used in file names (`screenshots/b/<ID>-<slug>.png`)
and in the spec. "SDK" = what the screen relies on; **new** = a capability the SDK lacks today.

## Navigation map

```
Bienvenue ─┬─ Créer le coffre ─ (Stockage indisponible) ─ Kit de récupération ─ Sources ─ Notifications ─┐
           └─ Connexion ─ (Trop d'appareils) ─ Ouverture sur cet appareil ──────────────────────────────┤
                                                                                                        ▼
Tab bar:  Coffre        Arrivées        [Scanner]        Sources         Moi
          │             │               │                │               │
          ├ Catégorie   ├ Historique    ├ Vérifier       ├ Catalogue     ├ Sécurité ─ Kit · Vérifier mdp · Serveur
          ├ Recherche   └ (code sheet)  └ (partage)      ├ Connecter     ├ Appareils
          │  └ Résultats                                 ├ Détail ─ Retirer          ├ Offre et usage
          └ Document ─ Lecteur · Actions · Reclasser    └ Échec         ├ Apparence et langue
                                                                         └ Se déconnecter
```

## Screens

| ID | Screen | Area | SDK |
|---|---|---|---|
| O1 | Bienvenue | Onboarding | — |
| O2 | Connexion (+ session expirée) | Onboarding | `users.login` |
| O3 | Créer le coffre | Onboarding | `users.create` |
| O4 | Stockage sécurisé indisponible | Onboarding | `SecureStorageUnavailable`, `allow_insecure_storage` |
| O5 | Trop d'appareils | Onboarding | `LoginOutcome::SessionLimitReached`, `replace_session_id` |
| O6 | Kit de récupération | Onboarding / Sécurité | **new**: recovery export |
| O7 | Choisir ses sources | Onboarding | `list_public_scripts`, `install_script` |
| O8 | Notifications | Onboarding | platform permission |
| O9 | Ouverture sur cet appareil | Onboarding | changelog replay progress (**new**: count) |
| V1 | Coffre | Coffre | `search_facets` (category counts), `search` |
| V2 | Coffre en remplissage | Coffre | scraper progress |
| V3 | Catégorie | Coffre | `search` + `search_facets` (subcategory, year) |
| V4 | Recherche | Coffre | local recents |
| V5 | Résultats | Coffre | `search`, `search_facets` |
| V6 | Document (carte) | Coffre | `get`, attributes |
| V7 | Lecteur | Coffre | `get` content, platform PDF renderer |
| V8 | Actions du document | Coffre | `update_metadata`; **new**: delete |
| V9 | Reclasser | Coffre | `update_metadata` |
| V10 | Modèles de cartes | Coffre | attributes per document type |
| V11 | Coffre (sombre) | Coffre | — |
| K1 | Scanner | Capture | platform camera / document scanner |
| K2 | Vérifier le scan | Capture | `upload` (Raw source) |
| K3 | Ajouter depuis une autre app | Capture | share extension → `upload` |
| K4 | Ajout en cours | Capture | `upload`, `DuplicateDocument` |
| R1 | Arrivées | Arrivées | **new**: unconfirmed flag; scraper question |
| R2 | Tout est rangé | Arrivées | — |
| R3 | Historique | Arrivées | **new**: run history |
| R4 | Notification de code | Arrivées | local notification |
| S1 | Sources | Sources | `list_installed_scripts`; **new**: last-run status |
| S2 | Catalogue | Sources | `list_public_scripts` |
| S3 | Connecter une source | Sources | `ScriptParameter`s, `allowed_domains`, `install_script` |
| S4 | Récupération en cours | Sources | `on_scraper_progress` |
| S5 | Détail d'une source | Sources | **new**: schedule, per-source counts |
| S6 | Source en échec | Sources | error codes 5xxx |
| S7 | Retirer la source | Sources | `disable_script` |
| M1 | Moi | Moi | `get_usage` |
| M2 | Sécurité | Moi | — |
| M3 | Ce que voit le serveur | Moi | — (static) |
| M4 | Vérifier mon mot de passe | Moi | **new**: local check |
| M5 | Appareils | Moi | `list_sessions`, `revoke_session`; **new**: last seen |
| M6 | Offre et usage | Moi | `get_usage` |
| M7 | Apparence et langue | Moi | settings |
| M8 | Se déconnecter | Moi | `logout` |
| X1 | Hors ligne | States | `server_state` |
| D1 | Bureau — Catégorie | Desktop | as V3 |
| D2 | Bureau — Document | Desktop | as V6/V7 |
| D3 | Bureau — Arrivées | Desktop | as R1 |
| D4 | Bureau — Sources | Desktop | as S1/S5 |
| T1 | Tablette — Coffre | Tablet | as V1 |
