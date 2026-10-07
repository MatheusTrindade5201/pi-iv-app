package br.univesp.pi.expedientes.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.session.SessionExpiry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class AuthFormViewModel : ViewModel() {
    private val _state = MutableStateFlow(AuthFormState())
    val state: StateFlow<AuthFormState> = _state.asStateFlow()

    fun onChange(
        field: AuthField,
        value: String,
    ) {
        _state.update { it.edit(field, value) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.update { it.withValidation(errors) }
            return
        }
        _state.update { it.copy(isSubmitting = true, generalError = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            when (val result = send(current)) {
                AuthResult.Success -> _state.update { it.copy(isSubmitting = false) }
                is AuthResult.Failure -> _state.update { it.withFailure(result.failure) }
            }
        }
    }

    protected abstract fun validate(state: AuthFormState): Map<AuthField, ValidationError>

    protected abstract suspend fun send(state: AuthFormState): AuthResult
}

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val repository: AuthRepository,
        expiry: SessionExpiry,
    ) : AuthFormViewModel() {
        val sessionExpired: StateFlow<Boolean> = expiry.expired

        override fun validate(state: AuthFormState) =
            AuthValidation.validateLogin(state.value(AuthField.Email), state.value(AuthField.Password))

        override suspend fun send(state: AuthFormState) =
            repository.login(
                email = AuthValidation.normalizeEmail(state.value(AuthField.Email)),
                password = state.value(AuthField.Password),
            )
    }

@HiltViewModel
class RegisterViewModel
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) : AuthFormViewModel() {
        override fun validate(state: AuthFormState) =
            AuthValidation.validateRegister(
                name = state.value(AuthField.Name),
                email = state.value(AuthField.Email),
                password = state.value(AuthField.Password),
                passwordConfirm = state.value(AuthField.PasswordConfirm),
            )

        override suspend fun send(state: AuthFormState) =
            repository.register(
                name = state.value(AuthField.Name).trim(),
                email = AuthValidation.normalizeEmail(state.value(AuthField.Email)),
                password = state.value(AuthField.Password),
            )
    }
