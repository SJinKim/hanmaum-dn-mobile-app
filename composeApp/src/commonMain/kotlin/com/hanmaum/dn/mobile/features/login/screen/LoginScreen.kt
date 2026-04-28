package com.hanmaum.dn.mobile.features.login.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hanmaum.dn.mobile.core.domain.model.NavRoute
import com.hanmaum.dn.mobile.features.login.presentation.LoginViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToPending: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val viewModel: LoginViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            when (route) {
                NavRoute.Home            -> onNavigateToHome()
                NavRoute.PendingApproval -> onNavigateToPending()
                else                     -> {}
            }
            viewModel.onNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(64.dp))

        Text(
            text  = "DN App",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text  = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Sign in to stay connected with your community.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        // Email field
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text     = "EMAIL ADDRESS",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            TextField(
                value         = username,
                onValueChange = { username = it },
                placeholder   = { Text("hello@community.com") },
                leadingIcon   = {
                    Icon(
                        imageVector     = Icons.Default.Email,
                        contentDescription = null,
                        tint            = MaterialTheme.colorScheme.outline,
                    )
                },
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true,
                shape      = MaterialTheme.shapes.small,
                colors     = TextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Password field
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "PASSWORD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick         = { /* stub */ },
                    contentPadding  = PaddingValues(0.dp),
                ) {
                    Text(
                        text  = "Forgot?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextField(
                value                = password,
                onValueChange        = { password = it },
                placeholder          = { Text("••••••••") },
                leadingIcon          = {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.outline,
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector        = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint               = MaterialTheme.colorScheme.outline,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier             = Modifier.fillMaxWidth(),
                singleLine           = true,
                shape                = MaterialTheme.shapes.small,
                colors               = TextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Keep me signed in (stub)
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked         = false,
                onCheckedChange = { /* stub */ },
                colors          = CheckboxDefaults.colors(
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Text(
                text  = "Keep me signed in",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Login CTA
        Button(
            onClick  = { viewModel.onLoginClicked(username, password) },
            enabled  = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = MaterialTheme.shapes.extraSmall,
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    "Login to DN App  →",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        state.error?.let { errorMsg ->
            Spacer(Modifier.height(12.dp))
            Text(
                text  = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Register link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                text  = "New to the collective?  ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick        = onRegisterClick,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text  = "Join the Community",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Support footer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = MaterialTheme.shapes.large,
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text  = "SUPPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text  = "Need help logging in?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Button(
                    onClick = { /* stub */ },
                    shape   = MaterialTheme.shapes.extraSmall,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor   = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text("Contact", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}
