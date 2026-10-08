# pi-iv-app

App Android (Kotlin + Jetpack Compose) para motociclistas registrarem seus expedientes: coleta o GPS durante o trabalho, lança os ganhos e mostra o lucro parcial, o resumo final e o histórico por dia. Nome: **Expediente** (pacote `br.univesp.pi.expedientes`).

Os cálculos (distância, tempo parado com motor ligado, combustível, lucro) são feitos pela API, no repositório irmão **[`pi-iv`](../pi-iv)**:

```
univesp/
  pi-iv/        # API (FastAPI + PostgreSQL) — contrato em pi-iv/docs/api-spec.md
  pi-iv-app/    # este repositório
```

## Funcionalidades

- **Conta:** cadastro, login, sessão persistida, aviso de sessão expirada, sair.
- **Perfil:** nome, moto (modelo, combustível Gasolina/Etanol/Flex, consumo km/l, cilindrada, ano) e preço do combustível.
- **Expediente:** iniciar (Flex pergunta o combustível), GPS em *foreground service* com fila local (Room) e envio em lote mesmo após ficar sem internet, lucro parcial ao vivo, banner do GPS, lançar/sobrescrever ganhos, encerrar com aviso de GPS inativo.
- **Histórico e análise:** lucro por dia, resumo de 7/30/90 dias (ganhos, combustível, km, tempo, valor hora/km, média por dia), detalhes do dia.
- **Acabamento:** faixa "Sem internet", textos e números em pt-BR, contraste WCAG AA, TalkBack, fonte 2×.

## Status

As 10 etapas do plano estão concluídas; falta a validação manual no emulador/celular ([roteiro de demonstração](docs/demo.md)). Plano em [`docs/app-plan.md`](docs/app-plan.md), protótipos em [`docs/prototypes/`](docs/prototypes/), setup da máquina em [`docs/setup.md`](docs/setup.md).

## Stack

| | |
|---|---|
| Linguagem / UI | Kotlin 2.4, Jetpack Compose (BOM 2026.09), Material 3 |
| Build | Gradle 9.8 (wrapper com SHA-256 fixado), Android Gradle Plugin 9.4, version catalog (`gradle/libs.versions.toml`) |
| SDK | minSdk 26 (Android 8.0), compile/target 37 |
| DI | Hilt 2.60 (KSP) |
| Rede | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Navegação | Navigation Compose com rotas tipadas |
| GPS e fila | Fused Location Provider, *foreground service*, Room, WorkManager (Hilt Work) |
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
| release | `http://127.0.0.1:8000/api/v1/` (celular com `adb reverse`) | `api.releaseUrl=...` no `local.properties` ou variável `API_RELEASE_URL` |

Para celular via USB: `adb reverse tcp:8000 tcp:8000` e, no `local.properties`, `api.baseUrl=http://localhost:8000/api/v1/` (debug).

HTTP sem TLS só é permitido para `localhost`/`127.0.0.1` (`app/src/main/res/xml/network_security_config.xml`) e, no debug, também para `10.0.2.2` (`app/src/debug/res/xml/`). Uma API publicada deve usar HTTPS.

## APK de release

```bash
./scripts/create-keystore.sh          # uma vez: gera release.jks + keystore.properties (fora do git)
./gradlew assembleRelease             # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
adb reverse tcp:8000 tcp:8000         # celular acessa a API do PC
```

- R8 (minificação + remoção de recursos) ligado; APK com ~2,3 MB.
- Sem `keystore.properties`, o release é assinado com a chave de **debug** (aviso no build) — serve para testar, não para distribuir.
- Guarde a senha e um backup da `release.jks`: sem ela não dá para publicar atualizações do mesmo app.

## Contrato com a API

`app/src/test/resources/api/*.json` são respostas reais da API, e o `ApiContractTest` garante que os DTOs do app as leem corretamente. Como regenerar: [`docs/app-plan.md` §6.1](docs/app-plan.md#61-atualizar-o-contrato-quando-a-api-mudar).

## CI

[`.github/workflows/android.yml`](.github/workflows/android.yml): a cada push na `main` e em pull requests roda ktlint, Android Lint (avisos quebram o build), testes unitários e os builds debug e release; os dois APKs ficam como artefatos da execução. Um push de tag `v*` (ex.: `git tag v1.0.0 && git push --tags`) cria um **GitHub Release** com o APK.

Para o release do CI sair assinado com a sua chave, cadastre em *Settings → Secrets and variables → Actions*:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `RELEASE_KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| Secret | `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD` | senha usada no `create-keystore.sh` |
| Secret | `RELEASE_KEY_ALIAS` | `expediente` |
| Variable | `API_RELEASE_URL` | URL HTTPS da API publicada (opcional) |

Sem os secrets, o CI gera o release assinado com a chave de debug.

## Créditos

Ícones da barra inferior e do launcher baseados em [Lucide](https://lucide.dev) (licença ISC).
