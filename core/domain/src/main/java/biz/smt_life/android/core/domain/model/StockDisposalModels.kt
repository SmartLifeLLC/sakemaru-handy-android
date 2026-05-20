package biz.smt_life.android.core.domain.model

data class StockDisposalItem(
    val itemId: Int,
    val itemCode: String,
    val itemName: String,
    val displayName: String?,
    val packaging: String?,
    val displayPackaging: String?,
    val nameWithPackaging: String?,
    val orderJanCode: String?,
    val capacityCase: Int?,
    val capacityCarton: Int?,
    val matchedField: String?,
    val matchedValue: String?,
    val stock: StockSummary
)

data class StockSummary(
    val actualQuantity: Int,
    val theoreticalQuantity: Int
)

data class StockDisposalQueueResult(
    val requestId: String,
    val queueId: Int,
    val status: String,
    val duplicated: Boolean
)

enum class StockDisposalReason(val label: String, val value: String) {
    EXPIRED("賞味期限切れ", "EXPIRED"),
    DAMAGED("破損", "DAMAGED"),
    STORE_PROMOTION_GIFT("店内販促（景品）", "STORE_PROMOTION_GIFT"),
    STORE_PROMOTION_TASTING("店内販促（試飲・試食）", "STORE_PROMOTION_TASTING"),
    CUSTOMER_PROMOTION_COOP("得意先販促（協賛）", "CUSTOMER_PROMOTION_COOP"),
    ENTERTAINMENT_CONDOLENCE("交際費（慶弔）", "ENTERTAINMENT_CONDOLENCE"),
    LOST("紛失", "LOST"),
    OTHER("その他", "OTHER")
}

enum class DisposalQuantityType(val value: String) {
    CASE("CASE"),
    PIECE("PIECE"),
    CARTON("CARTON")
}
