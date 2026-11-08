package com.tourregister.data.entity

enum class VisitPurpose(val displayName: String) {
    GOVT_MEETING("Govt Meeting"),
    OFFICIAL_MEETING("Official Meeting"),
    CUSTOMER_MEETING("Customer Meeting"),
    PRE_SANCTION_INSPECTION("Pre-Sanction Inspection"),
    POST_SANCTION_INSPECTION("Post-Sanction Inspection"),
    LEAD_FOLLOWUP("Lead Follow-up"),
    NOTICE_SERVE("Notice Serve"),
    RECOVERY_VISIT("Recovery Visit"),
    OTHERS("Others");

    companion object {
        fun fromName(name: String): VisitPurpose = entries.find { it.name == name } ?: OTHERS
    }
}
