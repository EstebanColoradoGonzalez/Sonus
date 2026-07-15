package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Visual theme preference of the Listener (domain_and_state_model §4). A pure configuration value,
 * never behavioural ([RNF-07] / Invariante 3). Persisted by stable name (not ordinal) in the
 * `AppSettings` singleton; only the default `SYSTEM` is exercised in US-004, the rest arrives with
 * US-039 (`TRG-CFG-01`).
 */
enum class ThemePreference {
    /** Follow the operating-system theme. */
    SYSTEM,
    LIGHT,
    DARK,
}
