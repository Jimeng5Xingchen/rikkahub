package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.LabSetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CardGroupValue
import me.rerere.rikkahub.ui.components.ui.switchItem
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun SettingPreferencesLabPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var labSetting by remember(settings) { mutableStateOf(settings.labSetting) }
    val context = LocalContext.current

    fun updateLabSetting(setting: LabSetting) {
        labSetting = setting
        vm.updateSettings { it.copy(labSetting = setting) }
    }

    // 内置 rootfs 文件是否随 APK 嵌入
    val rootfsAssetEmbedded = remember(context) {
        context.assets.list(LabSetting.ROOTFS_ASSET_DIR)?.contains(LabSetting.ROOTFS_ASSET_NAME) == true
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_page_lab))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 实验室功能总开关
            item {
                CardGroup {
                    switchItem(
                        checked = labSetting.labFeaturesEnabled,
                        onCheckedChange = { updateLabSetting(labSetting.copy(labFeaturesEnabled = it)) },
                        supportingContent = {
                            Text(stringResource(R.string.setting_page_lab_master_desc))
                        },
                        headlineContent = { Text(stringResource(R.string.setting_page_lab_master_title)) },
                    )
                    if (!labSetting.labFeaturesEnabled) {
                        formItem(
                            headlineContent = { Text(stringResource(R.string.setting_page_lab_disabled_hint_title)) },
                            supportingContent = { Text(stringResource(R.string.setting_page_lab_disabled_hint_desc)) },
                        ) {}
                    }
                }
            }

            // 实验特性子开关（仅总开关开启时展示）
            if (labSetting.labFeaturesEnabled) {
                item {
                    CardGroup(
                        title = { Text(stringResource(R.string.setting_page_lab_features_title)) },
                    ) {
                        switchItem(
                            checked = labSetting.agentPreTerminalFileEnabled,
                            onCheckedChange = {
                                updateLabSetting(labSetting.copy(agentPreTerminalFileEnabled = it))
                            },
                            supportingContent = {
                                Text(stringResource(R.string.setting_page_lab_agent_pre_terminal_file_desc))
                            },
                            headlineContent = {
                                Text(stringResource(R.string.setting_page_lab_agent_pre_terminal_file_title))
                            },
                        )
                        formItem(
                            headlineContent = {
                                Text(stringResource(R.string.setting_page_lab_embedded_rootfs_title))
                            },
                            supportingContent = {
                                Text(
                                    stringResource(
                                        R.string.setting_page_lab_embedded_rootfs_desc,
                                        LabSetting.ROOTFS_ASSET_NAME,
                                        formatBytes(LabSetting.ROOTFS_ASSET_SIZE_BYTES),
                                    )
                                )
                            },
                            trailingContent = {
                                CardGroupValue(
                                    if (rootfsAssetEmbedded) {
                                        stringResource(R.string.setting_page_lab_embedded_rootfs_embedded)
                                    } else {
                                        stringResource(R.string.setting_page_lab_embedded_rootfs_missing)
                                    }
                                )
                            },
                        ) {}
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble() / 1024
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}
