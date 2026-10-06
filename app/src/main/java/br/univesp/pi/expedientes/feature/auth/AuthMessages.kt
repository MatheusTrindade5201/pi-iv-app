package br.univesp.pi.expedientes.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.univesp.pi.expedientes.R

@Composable
fun FieldError.text(): String =
    when (this) {
        is FieldError.Server -> message
        is FieldError.Invalid ->
            stringResource(
                when (error) {
                    ValidationError.BlankName -> R.string.error_blank_name
                    ValidationError.LongName -> R.string.error_long_name
                    ValidationError.InvalidEmail -> R.string.error_invalid_email
                    ValidationError.BlankPassword -> R.string.error_blank_password
                    ValidationError.ShortPassword -> R.string.error_short_password
                    ValidationError.LongPassword -> R.string.error_long_password
                    ValidationError.PasswordMismatch -> R.string.error_password_mismatch
                },
            )
    }

@Composable
fun AuthFailure.text(): String =
    when (this) {
        is AuthFailure.Api -> message
        AuthFailure.Network -> stringResource(R.string.error_network)
        AuthFailure.Unexpected -> stringResource(R.string.error_unexpected)
    }
