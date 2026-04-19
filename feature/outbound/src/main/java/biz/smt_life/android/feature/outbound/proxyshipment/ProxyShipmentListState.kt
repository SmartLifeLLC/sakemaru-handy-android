package biz.smt_life.android.feature.outbound.proxyshipment

import biz.smt_life.android.core.domain.model.ProxyShipmentAllocation
import biz.smt_life.android.core.domain.model.ProxyShipmentCourseSummary
import biz.smt_life.android.core.domain.model.ProxyShipmentStatus

data class ProxyShipmentListState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLoadedOnce: Boolean = false,
    val errorMessage: String? = null,
    val warehouseId: Int = 0,
    val warehouseName: String = "",
    val selectedDateApi: String? = null,
    val businessDateApi: String? = null,
    val selectedCourseId: Int? = null,
    val selectedTab: ProxyShipmentTab = ProxyShipmentTab.RESERVED,
    val items: List<ProxyShipmentAllocation> = emptyList(),
    val courseOptions: List<ProxyShipmentCourseSummary> = emptyList(),
    val totalCount: Int = 0
) {
    internal val groups: List<ProxyShipmentCourseGroup>
        get() = items.toProxyShipmentCourseGroups()

    internal val visibleGroups: List<ProxyShipmentCourseGroup>
        get() = groups.filter { group ->
            when (selectedTab) {
                ProxyShipmentTab.RESERVED -> group.status == ProxyShipmentStatus.RESERVED
                ProxyShipmentTab.PICKING -> group.status == ProxyShipmentStatus.PICKING
            }
        }

    val selectedDateDisplay: String
        get() = selectedDateApi?.let(ProxyShipmentDateFormatter::toDisplay) ?: "--/--/--"

    val selectedCourseName: String
        get() = courseOptions.firstOrNull { it.id == selectedCourseId }?.name ?: "すべて"
}

enum class ProxyShipmentTab {
    RESERVED,
    PICKING
}
