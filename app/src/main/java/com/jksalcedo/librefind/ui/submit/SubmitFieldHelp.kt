import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jksalcedo.librefind.R

object SubmitFieldHelp {

    @Composable
    fun getAppName(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_app_name_title),
        description = stringResource(R.string.help_app_name_desc),
        tip = stringResource(R.string.help_app_name_tip)
    )

    @Composable
    fun getPackageName(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_package_name_title),
        description = stringResource(R.string.help_package_name_desc),
        tip = stringResource(R.string.help_package_name_tip)
    )

    @Composable
    fun getDescription(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_description_title),
        description = stringResource(R.string.help_description_desc),
        tip = null
    )

    @Composable
    fun getRepoUrl(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_repo_url_title),
        description = stringResource(R.string.help_repo_url_desc),
        tip = stringResource(R.string.help_repo_url_tip)
    )

    @Composable
    fun getFdroidId(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_fdroid_id_title),
        description = stringResource(R.string.help_fdroid_id_desc),
        tip = stringResource(R.string.help_fdroid_id_tip)
    )

    @Composable
    fun getLicense(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_license_title),
        description = stringResource(R.string.help_license_desc),
        tip = stringResource(R.string.help_license_tip)
    )

    @Composable
    fun getTargetProprietaryApps(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_target_apps_title),
        description = stringResource(R.string.help_target_apps_desc),
        tip = stringResource(R.string.help_target_apps_tip)
    )

    @Composable
    fun getSearchAlternatives(): FieldHelpContent = FieldHelpContent(
        title = stringResource(R.string.help_search_alternatives_title),
        description = stringResource(R.string.help_search_alternatives_desc),
        tip = null
    )
}

data class FieldHelpContent(
    val title: String,
    val description: String,
    val tip: String?
)