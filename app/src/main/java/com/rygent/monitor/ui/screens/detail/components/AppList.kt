package com.rygent.monitor.ui.screens.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rygent.monitor.domain.model.ApplicationData
import com.rygent.monitor.ui.theme.DarkSurface
import com.rygent.monitor.ui.theme.PrimaryBlue
import com.rygent.monitor.ui.theme.TextGray
import com.rygent.monitor.ui.theme.TextWhite

@Composable
fun AppList(
    apps: List<ApplicationData>,
    onAppClick: (ApplicationData) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Installed Applications (${apps.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        items(apps) { app ->
            AppItem(app, onClick = { onAppClick(app) })
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun AppItem(app: ApplicationData, onClick: () -> Unit) {
    com.rygent.monitor.ui.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder (Mock)
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = DarkSurface
            ) {
                Icon(
                    Icons.Default.Info, 
                    contentDescription = null, 
                    tint = PrimaryBlue,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextWhite
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
        }
    }
}
