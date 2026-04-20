package biz.smt_life.android.sakemaru_handy_denso.navigation

import android.net.Uri

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Main : Routes("main")
    object Settings : Routes("settings")
    object WarehouseSettings : Routes("warehouse_settings")
    object Inbound : Routes("inbound") // Legacy - kept for compatibility

    // Incoming routes
    object IncomingProductList : Routes("incoming_product_list")
    object IncomingScheduleList : Routes("incoming_schedule_list")
    object IncomingInput : Routes("incoming_input")
    object IncomingHistory : Routes("incoming_history")

    // Outbound routes (2.5.1 - 2.5.4 spec flow)
    object PickingList : Routes("picking_list") // 2.5.1 - コース選択
    object OutboundPicking : Routes("outbound_picking/{taskId}?editItemId={editItemId}") { // 2.5.2 - データ入力
        fun createRoute(taskId: Int, editItemId: Int? = null): String {
            return if (editItemId != null) {
                "outbound_picking/$taskId?editItemId=$editItemId"
            } else {
                "outbound_picking/$taskId"
            }
        }
    }
    object PickingHistory : Routes("picking_history/{taskId}") { // 2.5.3 - 履歴
        fun createRoute(taskId: Int) = "picking_history/$taskId"
    }

    object SlipEntry : Routes("slip_entry")

    object Move : Routes("move")
    object ProxyShipmentList : Routes("proxy_shipment_list")
    object ProxyShipmentPicking :
        Routes("proxy_shipment_picking?shipmentDate={shipmentDate}&courseKey={courseKey}") {
        fun createRoute(shipmentDate: String, courseKey: String): String {
            return "proxy_shipment_picking?shipmentDate=${Uri.encode(shipmentDate)}&courseKey=${Uri.encode(courseKey)}"
        }
    }
    object ProxyShipmentResult : Routes("proxy_shipment_result/{allocationId}") {
        fun createRoute(allocationId: Int) = "proxy_shipment_result/$allocationId"
    }
    object Inventory : Routes("inventory")
    object LocationSearch : Routes("location_search")
}
