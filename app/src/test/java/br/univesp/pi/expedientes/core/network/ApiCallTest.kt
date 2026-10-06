package br.univesp.pi.expedientes.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiCallTest {
    private fun httpError(
        code: Int,
        body: String,
    ): HttpException = HttpException(Response.error<Unit>(code, body.toResponseBody()))

    @Test
    fun successWrapsValue() =
        runTest {
            assertEquals(ApiResult.Success(42), apiCall { 42 })
        }

    @Test
    fun stringDetailBecomesMessage() =
        runTest {
            val result = apiCall<Unit> { throw httpError(409, """{"detail":"Já existe um expediente em andamento"}""") }

            assertEquals(ApiResult.HttpError(409, "Já existe um expediente em andamento"), result)
        }

    @Test
    fun validationListBecomesFieldList() =
        runTest {
            val body =
                """{"detail":[{"loc":["body","amount"],"msg":"x"},{"loc":["body"],"msg":"y"},{"loc":["body","amount"],"msg":"z"}]}"""

            val result = apiCall<Unit> { throw httpError(422, body) }

            assertEquals(ApiResult.HttpError(422, "Dados inválidos: amount"), result)
        }

    @Test
    fun validationWithoutFieldsUsesGenericMessage() {
        assertEquals("Dados inválidos", errorMessage("""{"detail":[{"loc":["body"],"msg":"y"}]}""", 422))
    }

    @Test
    fun nonJsonBodyFallsBackToCode() =
        runTest {
            val result = apiCall<Unit> { throw httpError(502, "<html>Bad Gateway</html>") }

            assertEquals(ApiResult.HttpError(502, "Erro inesperado (502)"), result)
        }

    @Test
    fun networkFailureIsNetworkError() =
        runTest {
            assertEquals(ApiResult.NetworkError, apiCall<Unit> { throw IOException("offline") })
        }

    @Test
    fun malformedSuccessBodyIsUnexpected() =
        runTest {
            assertEquals(ApiResult.UnexpectedError, apiCall<Unit> { throw SerializationException("bad") })
        }
}
