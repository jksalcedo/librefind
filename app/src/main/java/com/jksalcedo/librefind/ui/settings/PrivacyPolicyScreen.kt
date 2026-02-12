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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
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
                text = "Privacy Policy for LibreFind",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last updated: February 10, 2026",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This Privacy Policy describes how LibreFind (\"we\", \"us\", or \"our\") collects, uses, and discloses your information when you use our mobile application (the \"Service\").",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "By using the Service, you agree to the collection and use of information in accordance with this policy.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("1. Information We Collect")
            Text(
                text = "We collect the minimum amount of data necessary to provide our services.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle("A. Device & Usage Data (Anonymous)")
            Text(
                text = "When you scan your device, we collect the following anonymized data to generate your sovereignty score:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint("Device ID: A randomly generated unique identifier (UUID) stored locally on your device. This allows us to sync your preferences without tracking your real identity.")
            BulletPoint("App Statistics: The total count of installed apps, count of FOSS (Free and Open Source Software) apps, count of proprietary apps, and count of unknown/unclassified apps.")
            Text(
                text = "Note: We send your installed app package names to our servers solely for classification purposes (determining FOSS vs. proprietary status). We do not store or log these package names beyond what is necessary for the query.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BulletPoint("App Version: The version of LibreFind you are using.")
            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle("B. Account Data (Optional)")
            Text(
                text = "If you choose to create an account to contribute alternatives or sync data, we collect:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint("Username & Email: To identify you and prevent spam.")
            BulletPoint("Password: Securely hashed by our authentication provider. We never have access to your plaintext password.")
            BulletPoint("User Submissions: Any alternative app suggestions or votes you submit to the community database.")
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("2. How We Use Your Information")
            Text(
                text = "We use the collected data for the following purposes:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint("To Provide the Service: Calculating your \"Sovereignty Score\" and syncing your preferences across devices.")
            BulletPoint("To Improve Our Service: Your submissions help us identify proprietary apps that need FOSS alternatives.")
            BulletPoint("Security & Authentication: Your email and password are used strictly to secure your account.")
            BulletPoint("Community Features: To prevent vote manipulation (spam) on app recommendations.")
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("3. Third-Party Services")
            Text(
                text = "We use trusted third-party services to operate our infrastructure. We do not sell your data. We do not share your data with third parties for advertising purposes.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle("Supabase")
            Text(
                text = "We use Supabase as our backend database and authentication provider.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint("Data stored: User accounts, submissions, and anonymized stats.")
            BulletPoint("Privacy Policy: https://supabase.com/privacy")
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("4. Data Security")
            Text(
                text = "We implement industry-standard security measures to protect your data, including encrypted connections (HTTPS/TLS) and secure authentication via our backend provider.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("5. Data Retention and Deletion")
            BulletPoint("Retention: We retain account data as long as your account is active.")
            BulletPoint("Deletion: You have the right to delete your account at any time. You can request data deletion by contacting us or using the \"Delete Account\" option within the app settings. Upon deletion, all associated account data will be removed from our servers.")
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("6. Children's Privacy")
            Text(
                text = "This Service is not directed to anyone under the age of 13. We do not knowingly collect personal information from children under 13. If we discover that a child under 13 has provided us with personal information, we will promptly delete it.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("7. Changes to This Privacy Policy")
            Text(
                text = "We may update this Privacy Policy from time to time. We will notify you of any changes by updating the \"Last updated\" date at the top of this policy. You are advised to review this Privacy Policy periodically for any changes.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("8. Contact Us")
            Text(
                text = "If you have any questions about this Privacy Policy, please contact us:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            BulletPoint("By email: jks.create@gmail.com")
            
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
