package biz.smt_life.android.feature.settings

data class SettingsState(
    val hostUrl: String = "",
    val isCustomUrl: Boolean = false,
    val presetUrls: List<String> = PresetUrls.list,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

object PresetUrls {
    val list = listOf(
        "https://wms.lw-hana.net",
        "https://wms.sakemaru.click",
        "http://10.0.2.2:8000"
    )
}
