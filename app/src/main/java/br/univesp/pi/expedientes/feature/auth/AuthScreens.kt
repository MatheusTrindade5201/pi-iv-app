package br.univesp.pi.expedientes.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.univesp.pi.expedientes.R

@Composable
fun LoginEntry(
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onChange = viewModel::onChange,
        onSubmit = viewModel::submit,
        onRegisterClick = onRegisterClick,
    )
}

@Composable
fun LoginScreen(
    state: AuthFormState,
    onChange: (AuthField, String) -> Unit,
    onSubmit: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthForm(
        title = stringResource(R.string.login_title),
        fields = listOf(EmailSpec, PasswordSpec),
        state = state,
        onChange = onChange,
        onSubmit = onSubmit,
        submitLabel = stringResource(R.string.login_enter),
        linkLabel = stringResource(R.string.login_go_register),
        onLink = onRegisterClick,
        modifier = modifier,
    )
}

@Composable
fun RegisterEntry(
    onLoginClick: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterScreen(
        state = state,
        onChange = viewModel::onChange,
        onSubmit = viewModel::submit,
        onLoginClick = onLoginClick,
    )
}

@Composable
fun RegisterScreen(
    state: AuthFormState,
    onChange: (AuthField, String) -> Unit,
    onSubmit: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthForm(
        title = stringResource(R.string.register_title),
        fields = listOf(NameSpec, EmailSpec, PasswordSpec, PasswordConfirmSpec),
        state = state,
        onChange = onChange,
        onSubmit = onSubmit,
        submitLabel = stringResource(R.string.register_submit),
        linkLabel = stringResource(R.string.register_go_login),
        onLink = onLoginClick,
        modifier = modifier,
    )
}
