# pi-iv-app

App Android (Kotlin + Jetpack Compose) para motociclistas registrarem seus expedientes: coleta o GPS durante o trabalho, lança os ganhos e mostra o lucro parcial, o resumo final e o histórico por dia. Nome provisório: **Expedientes** (`br.univesp.pi.expedientes`).

Os cálculos (distância, tempo parado com motor ligado, combustível, lucro) são feitos pela API, no repositório irmão **[`pi-iv`](../pi-iv)**:

```
univesp/
  pi-iv/        # API (FastAPI + PostgreSQL) — contrato em pi-iv/docs/api-spec.md
  pi-iv-app/    # este repositório
```

## Status

Etapas 1–7 de 10 concluídas (scaffold; rede e sessão; Login e Cadastro; Perfil; Home com último registro e início de expediente; rastreamento de GPS com fila offline; Home em andamento com ganhos e encerramento). Plano em [`docs/app-plan.md`](docs/app-plan.md), protótipos em [`docs/prototypes/`](docs/prototypes/), setup da máquina em [`docs/setup.md`](docs/setup.md).

## Stack

| | |
|---|---|
| Linguagem / UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 |
| Build | Gradle 9.8 (wrapper com SHA-256 fixado), Android Gradle Plugin 9.4, version catalog (`gradle/libs.versions.toml`) |
| SDK | minSdk 26 (Android 8.0), compile/target 37 |
| DI | Hilt 2.60 (KSP) |
| Rede | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Navegação | Navigation Compose com rotas tipadas |
| Qualidade | ktlint (`ktlint_official`), Android Lint |
| Testes | JUnit 4, kotlinx-coroutines-test, Turbine, MockWebServer, Robolectric + Compose UI Test |

## Desenvolvimento (WSL) e execução (Windows)

O código é editado no WSL e roda no Windows numa cópia clonada (detalhes em [`docs/setup.md`](docs/setup.md#duas-cópias-do-projeto-wsl--windows-via-git)).

**No WSL** — compilar e testar (precisa de JDK 21 e `ANDROID_HOME`, ver `setup.md` §9):

```bash
./gradlew ktlintCheck lint testDebugUnitTest assembleDebug
./gradlew ktlintFormat
```

**No Windows** — rodar no emulador:

1. Subir a API no WSL:
   ```bash
   cd ../pi-iv && docker compose up -d db && cd api
   uv run alembic upgrade head
   uv run fastapi dev app/main.py --host 0.0.0.0 --port 8000
   ```
2. `git pull` em `C:\Users\mathe\projects\pi-iv-app` → Android Studio → **Open** (primeira vez) → aguardar o *Gradle Sync* → ▶ **Run** no emulador.

Documentação interativa da API: `http://localhost:8000/docs`.

### URL da API

| Build | URL | Como mudar |
|---|---|---|
| debug | `http://10.0.2.2:8000/api/v1/` (emulador → Windows) | `api.baseUrl=...` no `local.properties` (não versionado) |
| release | placeholder HTTPS | definida quando houver deploy |

Para celular via USB: `adb reverse tcp:8000 tcp:8000` e, no `local.properties`, `api.baseUrl=http://localhost:8000/api/v1/`.

HTTP sem TLS só é permitido no build **debug** e só para `10.0.2.2`, `localhost` e `127.0.0.1` (`app/src/debug/res/xml/network_security_config.xml`).

## Contrato com a API

`app/src/test/resources/api/*.json` são respostas reais da API, e o `ApiContractTest` garante que os DTOs do app as leem corretamente. Como regenerar: [`docs/app-plan.md` §6.1](docs/app-plan.md#61-atualizar-o-contrato-quando-a-api-mudar).

## CI

[`.github/workflows/android.yml`](.github/workflows/android.yml): a cada push na `main` e em pull requests roda ktlint, Android Lint, testes unitários e o build debug; o APK fica como artefato da execução.

## Créditos

Ícones da barra inferior e do launcher baseados em [Lucide](https://lucide.dev) (licença ISC).
