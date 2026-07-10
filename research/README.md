# Research Audit Trail

One folder per issuer, one subfolder per card:

```
research/
  hdfc/
    swiggy/
      NOTES.md             # curation-guide §4 template: sources, rules-extracted table,
                            # exclusions, judgment calls, open questions
      tnc-2026-07-09.pdf  # snapshot of the T&Cs as verified (or screenshots)
```

Every rule in `data/cards.json` must be traceable to a source here — this is
the answer to "why do you say Swiggy is 10%?". Point valuations for points
cards (Spec D1) must document the redemption assumption behind the chosen
conservative value.

When a card's rules are re-verified, update its `lastVerified` in
`data/cards.json` and add/refresh the snapshot here.
