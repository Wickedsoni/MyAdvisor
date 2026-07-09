# Curated Reward Dataset

`cards.json` is the single, versioned dataset consumed by the import pipeline
(JSON → validator → parser → domain objects → repository → Room, per ADR-004).
It ships bundled with the app in v1.

Rules for editing (see `data-curation-guide.md` in the design docs):

- Every rule must have a research-trail entry under `research/<issuer>/<card>/`.
- Bump `dataVersion` (semver) on every change — the validator rejects
  non-monotonic versions.
- Update the card's `lastVerified` date when you re-confirm its T&Cs.
- Never hand-edit the database; the importer is the only writer of the
  catalog zone.

Format: Rule Engine Specification §3. Validation gates: Spec §3.1
(enforced in CI and again before import — Phase 1 builds the validator).
