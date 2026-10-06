package br.univesp.pi.expedientes.feature.tracking

enum class PermissionPrompt {
    Ask,
    OpenSettings,
}

fun permissionPromptAfterDenial(shouldShowRationale: Boolean): PermissionPrompt =
    if (shouldShowRationale) PermissionPrompt.Ask else PermissionPrompt.OpenSettings
