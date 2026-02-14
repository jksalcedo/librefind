package com.jksalcedo.librefind.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jksalcedo.librefind.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_privacy_policy)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.privacy_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_last_updated),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.privacy_agreement),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_1))
            Text(
                text = stringResource(R.string.privacy_section_1_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle(stringResource(R.string.privacy_subsection_1a))
            Text(
                text = stringResource(R.string.privacy_subsection_1a_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.privacy_bullet_device_id))
            BulletPoint(stringResource(R.string.privacy_bullet_app_stats))
            Text(
                text = stringResource(R.string.privacy_note_app_names),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BulletPoint(stringResource(R.string.privacy_bullet_app_version))
            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle(stringResource(R.string.privacy_subsection_1b))
            Text(
                text = stringResource(R.string.privacy_subsection_1b_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.privacy_bullet_user_email))
            BulletPoint(stringResource(R.string.privacy_bullet_password))
            BulletPoint(stringResource(R.string.privacy_bullet_submissions))
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_2))
            Text(
                text = stringResource(R.string.privacy_section_2_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.privacy_bullet_provide_service))
            BulletPoint(stringResource(R.string.privacy_bullet_improve_service))
            BulletPoint(stringResource(R.string.privacy_bullet_security))
            BulletPoint(stringResource(R.string.privacy_bullet_community))
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_3))
            Text(
                text = stringResource(R.string.privacy_section_3_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle(stringResource(R.string.privacy_subsection_supabase))
            Text(
                text = stringResource(R.string.privacy_subsection_supabase_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.privacy_bullet_supabase_data))
            BulletPoint(stringResource(R.string.privacy_bullet_supabase_policy))
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_4))
            Text(
                text = stringResource(R.string.privacy_section_4_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_5))
            BulletPoint(stringResource(R.string.privacy_bullet_retention))
            BulletPoint(stringResource(R.string.privacy_bullet_deletion))
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_6))
            Text(
                text = stringResource(R.string.privacy_section_6_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_7))
            Text(
                text = stringResource(R.string.privacy_section_7_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.privacy_section_8))
            Text(
                text = stringResource(R.string.privacy_section_8_desc),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint(stringResource(R.string.privacy_bullet_email))
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SubsectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("• ")
        }
        append(text)
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
    )
}
