package br.univesp.pi.expedientes.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.network.RequestFailure

@Composable
fun RequestFailure.text(): String =
    when (this) {
        is RequestFailure.Api -> message
        RequestFailure.Network -> stringResource(R.string.error_network)
        RequestFailure.Unexpected -> stringResource(R.string.error_unexpected)
    }
