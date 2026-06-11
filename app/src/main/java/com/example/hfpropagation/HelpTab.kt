package com.example.hfpropagation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpTab(s: AppStrings) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = s.appTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = s.introTitle,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.rvsu),
            contentDescription = "RVSU Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Adjust height as preferred
                .padding(vertical = 8.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Credits ---
        HelpSectionCard(
            title = s.creditTitle,
            content = s.helpCredits
        )

        // --- Introduction ---
        HelpSectionCard(
            title = s.introTitleTab,
            content = s.helpIntroduction
        )

        // --- Location Tab ---
        HelpSectionCard(
            title = s.locationTitle,
            content = s.helpLocation
        )

        // --- Results Tab ---
        HelpSectionCard(
            title = s.resultsTitle,
            content = s.helpResults
        )
        // --- Online Tab ---
        HelpSectionCard(
            title = s.onlineTitle,
            content = s.onlineMessage
        )

        // --- Settings Tab ---
        HelpSectionCard(
            title = s.settingsTitle,
            content = s.helpSettings
        )

        // --- Glossary ---
        HelpSectionCard(
            title = s.glossaryTitle,
            content = s.helpGlossary
        )

        // --- Propagation Engine ---
        HelpSectionCard(
            title = s.engineTitle,
            content = s.helpEngine
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun HelpSectionCard(title: String, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )
        }
    }
}