package biz.smt_life.android.core.domain.model

/**
 * Domain models for Picking Tasks.
 * UI-friendly representation mapped from API responses.
 */

/**
 * Represents a picking task grouped by delivery course and picking area.
 */
data class PickingTask(
    val taskId: Int,
    val waveId: Int,
    val courseName: String,
    val courseCode: String,
    val pickingAreaName: String,
    val pickingAreaCode: String,
    val items: List<PickingTaskItem>,
    val totalItems: Int,
    val completedItems: Int,
    val progressText: String // e.g., "5/10"
) {
    val isCompleted: Boolean
        get() = completedItems == totalItems && totalItems > 0

    val isInProgress: Boolean
        get() = completedItems > 0 && completedItems < totalItems
}

/**
 * Individual item in a picking task.
 */
data class PickingTaskItem(
    val id: Int,
    val itemId: Int,
    val itemName: String,
    val plannedQtyType: QuantityType,
    val plannedQty: Double,
    val pickedQty: Double,
    val slipNumber: Int
) {
    val isCompleted: Boolean
        get() = pickedQty >= plannedQty
}

/**
 * Quantity type for picking items.
 */
enum class QuantityType {
    CASE,
    PIECE;

    companion object {
        fun fromString(value: String): QuantityType = when (value.uppercase()) {
            "CASE" -> CASE
            "PIECE" -> PIECE
            else -> PIECE // Default fallback
        }
    }
}
