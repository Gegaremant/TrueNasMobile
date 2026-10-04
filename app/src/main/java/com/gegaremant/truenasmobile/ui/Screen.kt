package com.gegaremant.truenasmobile.ui

/**
 * Navigation destinations.
 *
 * Only `route` is meaningful: it is the NavHost key and must stay stable.
 * Screen titles are NOT stored here - they live in the locale dictionaries
 * (`res/values/strings.xml` + `res/values-ru/strings.xml`) and are looked
 * up with `stringResource(R.string.…)` at the point of display. An earlier
 * revision carried a `title` field, but it was never read and drifted into
 * hardcoded Russian, which is exactly what the dictionaries exist to prevent.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Storage : Screen("storage")
    object Tasks : Screen("tasks")
    object Apps : Screen("apps")
    object ServicesScreen : Screen("services")
    object ServicesDetailScreen: Screen("service_detail")
    object Containers : Screen ("containers")
    object Vms : Screen("vms")
    object Login : Screen("login")
    object Main: Screen("main")
    object Performance : Screen("performance")
    object Settings : Screen("settings")
    object PushSettings : Screen("push_settings")
    object ShareInfo : Screen ("share_info")
    object AppLogging : Screen("app_logging")
    object Theme : Screen("theme")
    object AccountSwitcher : Screen("account_switcher")
    object PoolDetails : Screen("pool_details")
    object Files : Screen("file_explorer")
    object Marketplace : Screen("marketplace")
    object MarketplaceAppDetails : Screen("marketplace_app_details")
    object ContainerInfo : Screen("container_info")
    object VmDetails : Screen("vm_details")
    object ChangePassword : Screen("change_password")
    object Profile : Screen("profile")
    object DiskInfo : Screen("disk_info")
    object AppConfigScreen : Screen("app_config")
    object InstanceConfigScreen : Screen("instance_config")
    object AppDetailsScreen : Screen("app_details")
    object AppAdvancedInfoScreen : Screen("app_advanced_info/{appId}") {
        fun createRoute(appId: String): String {
            return "app_advanced_info/$appId"
        }
    }
    object SystemUpdateScreen : Screen("system_update")
    object MarketplaceCategory : Screen("marketplace?category={category}") {
        fun createRoute(category: String): String {
            return "marketplace?category=$category"
        }
    }

    object AppUpgrade : Screen("app_upgrade/{appName}") {
        fun createRoute(appName: String): String {
            return "app_upgrade/$appName"
        }
    }

    object CatalogInstall : Screen("catalog_install/{appName}/{train}") {
        fun createRoute(appName: String, train: String): String {
            return "catalog_install/$appName/$train"
        }
    }

    object DatasetExplorer : Screen("${Files.route}/{poolName}") {
        fun createRoute(poolName: String): String {
            return "${Files.route}/$poolName"
        }
    }
    object RollbackVersion : Screen("rollback/{appName}") {
        fun createRoute(appName: String): String {
            return "rollback/$appName"
        }
    }
    object AlertServicesList : Screen("alert_services_list")
    object AlertServiceDetail : Screen("alert_service_detail/{serviceId}") {
        fun createRoute(serviceId: Int) = "alert_service_detail/$serviceId"
    }

    object AlertServiceCreate : Screen("alert_service_create")
    object AlertClassesConfig : Screen("alert_classes_config")
    object UserListScreen : Screen("user_list")
    object UserDetailScreen : Screen("user_detail/{userId}") {
        fun createRoute(userId: Int) = "user_detail/$userId"
    }
    object UserCreateScreen : Screen("user_create")
    object LocalAdminSetupScreen : Screen("local_admin_setup")
    object ApiKeyListScreen : Screen("api_key_list")
    object ApiKeyDetailScreen : Screen("api_key_detail/{keyId}") {
        fun createRoute(keyId: Int) = "api_key_detail/$keyId"
    }
    object ApiKeyCreateScreen : Screen("api_key_create")
    object GeneralSystemSettingsScreen : Screen("general_system_settings")
    object GeneralSystemSettingsEditScreen : Screen("general_system_settings_edit")
    object AdvancedSystemSettingsScreen : Screen("advanced_system_settings")
    object AdvancedSystemSettingsEditScreen : Screen("advanced_system_settings_edit")
    object AuditConfigScreen : Screen("audit_config")
    object AuditLogsScreen : Screen("audit_logs")
    object NetworkScreen : Screen("network")
    object NetworkEditScreen : Screen("network_edit")
    object BootScreen : Screen("boot")
    object BootPoolScreen : Screen("boot_pool")
    object BootEnvironmentsScreen : Screen("boot_environments")
    object BootEnvironmentDetailScreen : Screen("boot_environment/{environmentId}") {
        fun createRoute(environmentId: String) = "boot_environment/$environmentId"
    }
    object SystemInformationScreen : Screen("system_information")
    object SoftwareInformationScreen : Screen("software_information")
    object HardwareInformationScreen : Screen("hardware_information")
    object TrueNasConnectScreen : Screen("truenas_connect")
    object TrueCommandScreen : Screen("truecommand")
    object AppImageManagementScreen : Screen("app_image_management")
    object IxVolumeListScreen : Screen("ix_volume_list")
    object DockerImageListScreen : Screen("docker_image_list")

}