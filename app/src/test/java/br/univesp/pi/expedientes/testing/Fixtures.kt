package br.univesp.pi.expedientes.testing

import mockwebserver3.MockResponse

object Fixtures {
    fun read(name: String): String =
        requireNotNull(Fixtures::class.java.classLoader?.getResource("api/$name.json")) {
            "fixture $name ausente"
        }.readText()

    fun response(
        code: Int,
        name: String,
    ): MockResponse = json(code, read(name))

    fun json(
        code: Int,
        body: String,
    ): MockResponse =
        MockResponse
            .Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
}
