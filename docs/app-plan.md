# Plano do app Android (Kotlin)

Planejamento do app (este repositório, `pi-iv-app`) que consome a API do repositório irmão **`pi-iv`** (`../pi-iv`), especificada em `pi-iv/docs/api-spec.md`. Segue o mesmo modelo da API: cada etapa é planejada em detalhe, executada com testes e **commitada pelo usuário** ao final.

**Contrato entre os repositórios:** o `GET /openapi.json` da API (também visível em `/docs`) e o `docs/api-spec.md` do `pi-iv`. Mudanças de contrato começam no `pi-iv` (API) e depois são refletidas nos DTOs deste app.

- [1. Visão geral](#1-visão-geral)
- [2. Setup da máquina](#2-setup-da-máquina)
- [3. Arquitetura](#3-arquitetura)
- [4. Telas → endpoints](#4-telas--endpoints)
- [5. Rastreamento de GPS](#5-rastreamento-de-gps)
- [6. Estratégia de testes](#6-estratégia-de-testes)
- [7. Etapas](#7-etapas)
- [8. Roteiro de validação da entrega](#8-roteiro-de-validação-da-entrega)
- [9. Decisões em aberto](#9-decisões-em-aberto)

---

## 1. Visão geral

App Android nativo, tema escuro, com 3 abas (Home, Histórico, Perfil) e as telas de Login/Cadastro dos protótipos. O app:

1. autentica e guarda o token JWT;
2. mantém moto e preços dos combustíveis (Perfil);
3. inicia o expediente e roda um **serviço em primeiro plano** que coleta GPS e envia em lotes, mesmo com a tela apagada ou sem internet;
4. mostra o lucro parcial ao vivo (Home), lança/sobrescreve ganhos e encerra o expediente;
5. mostra o histórico por dia.

Todos os cálculos (distância, tempo parado, combustível, lucro) ficam **na API**. O app só coleta, envia e exibe.

---

## 2. Setup da máquina

O ambiente atual é **Windows + WSL2**. O código do app é desenvolvido **no WSL** (junto da API) e sincronizado via Git com uma cópia **no Windows**, onde rodam o Android Studio e o emulador (ver [`setup.md`](setup.md#duas-cópias-do-projeto-wsl--windows-via-git)).

### 2.1 Ferramentas

| Ferramenta | Onde | Para quê | Onde baixar |
|---|---|---|---|
| **Android Studio** (versão estável mais recente) | Windows | IDE, SDK, emulador, Gradle. Já traz o JDK (JetBrains Runtime) — não é preciso instalar Java à parte. | <https://developer.android.com/studio> |
| **Android SDK** (pelo SDK Manager do Studio) | Windows | Plataforma da API estável mais recente (compile/target), *Build-Tools*, *Platform-Tools* (`adb`), *Emulator* | Dentro do Android Studio (*SDK Manager*) |
| **Imagem de sistema "Google Play"** (x86_64) | Windows | Emulador com Google Play Services — necessário para o `FusedLocationProviderClient` | Dentro do Android Studio (*Device Manager*) |
| **Git para Windows** | Windows | Clonar/atualizar a cópia do projeto no Windows ([`setup.md`](setup.md#duas-cópias-do-projeto-wsl--windows-via-git)) | <https://git-scm.com/download/win> |
| **Virtualização** (Windows Hypervisor Platform) | Windows | Aceleração do emulador; já costuma estar ativa por causa do WSL2 | Recurso do Windows (só ativar) |
| Docker + uv + repositório **`pi-iv`** (API) | WSL | API e PostgreSQL rodando localmente para o app consumir | Já instalados |
| **JDK 21 + Android SDK (linha de comando)** | WSL | Compilar e rodar os testes do app no WSL antes de mandar para o Windows | `apt` + <https://developer.android.com/studio#command-line-tools-only> |
| Celular Android (opcional) | — | Teste em campo com GPS real; *Opções do desenvolvedor* + *Depuração USB* | Drivers: <https://developer.android.com/studio/run/oem-usb> |
| `scrcpy` (opcional) | Windows | Espelhar a tela do celular no PC durante a demonstração | <https://github.com/Genymobile/scrcpy> |

Passo a passo completo da instalação: [`setup.md`](setup.md).

### 2.2 Emulador (AVD)

- Criar um AVD **Pixel** com imagem **Google Play**, API estável mais recente, 4 GB de RAM.
- Em *Extended controls → Location* o emulador aceita ponto fixo e **rotas GPX/KML** com velocidade de reprodução — é assim que vamos simular o GPS (ver §2.5).

### 2.3 Fazer o app enxergar a API

A API precisa escutar em todas as interfaces:

```bash
cd api && uv run fastapi dev app/main.py --host 0.0.0.0 --port 8000
```

| Cenário | URL base no app | Observação |
|---|---|---|
| **Emulador** (Windows) → API (WSL) | `http://10.0.2.2:8000/api/v1` | `10.0.2.2` é o `localhost` do Windows dentro do emulador; o WSL2 encaminha o `localhost` do Windows para a API. |
| **Celular via USB** | `http://localhost:8000/api/v1` | Rodar `adb reverse tcp:8000 tcp:8000` (o `localhost` do celular passa a apontar para o PC). Mais simples que configurar rede. |
| Celular via Wi-Fi | `http://<IP do Windows>:8000/api/v1` | Exige `networkingMode=mirrored` no `%UserProfile%\.wslconfig` (Windows 11) **ou** `netsh interface portproxy`, e liberar a porta 8000 no Firewall do Windows. |

- A URL base fica em `BuildConfig` por *build type* (`debug` aponta para a máquina local; `release` para HTTPS).
- O Android bloqueia HTTP sem TLS por padrão: o build **debug** terá um `network_security_config` liberando *cleartext* só para `10.0.2.2` e `localhost`.

### 2.4 Checagem do ambiente (fim da etapa 0)

- [ ] `docker compose up -d db` + `uv run alembic upgrade head` + API com `--host 0.0.0.0`
- [ ] No Windows: `curl http://localhost:8000/api/v1/health` → `{"status":"ok","database":"ok"}`
- [ ] No navegador do **emulador**: `http://10.0.2.2:8000/api/v1/health` responde
- [ ] (se usar celular) `adb devices` lista o aparelho e, após `adb reverse`, o navegador do celular abre `http://localhost:8000/api/v1/health`
- [ ] Projeto vazio do app compila e roda no emulador

### 2.5 Simulação de GPS

| Onde | Como |
|---|---|
| Emulador — rota | *Extended controls → Location → Routes → Import GPX/KML* → *Play route*. Vamos versionar um GPX com uma rota real de São Paulo (Sé → Paulista → Vila Mariana, com paradas) neste repositório, em `docs/gps/`. |
| Emulador — ponto fixo | *Single points*, ou `adb emu geo fix <longitude> <latitude>` |
| Celular | *Opções do desenvolvedor → Selecionar app de local fictício* + um app de rota simulada |

> **Atenção à velocidade de reprodução:** a API descarta saltos acima de **150 km/h** (`GPS_MAX_SPEED_KMH`). Uma rota de moto a ~35 km/h reproduzida a 5× vira ~175 km/h e os pontos seriam descartados. Use **1× a 3×**.

---

## 3. Arquitetura

### 3.1 Stack

| Camada | Escolha | Motivo |
|---|---|---|
| UI | **Jetpack Compose** + Material 3 | Padrão atual do Android; protótipos são simples de reproduzir |
| Navegação | Navigation Compose | Fluxo auth → abas (Home / Histórico / Perfil) |
| Estado | MVVM + `StateFlow` (fluxo unidirecional) | Testável sem UI |
| DI | **Hilt** | Integra com ViewModel, WorkManager e Service |
| Rede | **Retrofit + OkHttp + kotlinx.serialization** | `Interceptor` para o Bearer, `MockWebServer` nos testes |
| Sessão | DataStore (token) | Persistência simples e assíncrona |
| Buffer offline de GPS | **Room** | Fila de pontos pendentes que sobrevive a fechar o app |
| Envio em segundo plano | **WorkManager** | Reenvio com *backoff* quando a internet volta |
| Localização | `FusedLocationProviderClient` + **Foreground Service** (`type=location`) | Coleta com tela apagada e notificação "Expediente em andamento" |
| Dinheiro | `BigDecimal` + `NumberFormat` pt-BR | A API devolve valores como string decimal; nunca `Double` |
| Qualidade | ktlint (ou Spotless) + Android Lint | Equivalente ao ruff da API |

### 3.2 Estrutura (um módulo, organizado por feature)

```
app/src/main/java/.../
  core/
    network/     # Retrofit, interceptors, ApiError ({"detail"}), DTOs comuns
    session/     # token no DataStore, logout em 401
    ui/          # tema (cores dos protótipos), componentes (cards, bottom sheets, botões)
    format/      # moeda BRL, "3h 42min", "Ontem" / "18/09/2026"
  feature/
    auth/        # Login, Cadastro
    profile/     # nome, moto, preços de combustível
    shift/       # Home, iniciar/encerrar, lançar ganho
    tracking/    # LocationService, Room (fila de pontos), SyncWorker
    history/     # lista por dia
  MainActivity, NavGraph
```

Um único módulo é suficiente para o tamanho do PI; a separação por pacote mantém a organização sem o custo de multi-módulo.

### 3.3 Tema (dos protótipos)

Cores medidas nos pixels dos protótipos (`docs/prototypes/`), implementadas em `app/src/main/java/br/univesp/pi/expedientes/core/ui/theme/Color.kt`:

| Token | Uso | Cor |
|---|---|---|
| `background` | fundo, lista interna dos cards, chips, cards de preço | `#000000` |
| `surfaceContainer` | cards, campos, barra inferior, bottom sheet, botão "Encerrar Expediente" | `#1E1E1E` |
| `outline` | borda dos campos | `#383838` |
| `primary` / `onPrimary` | botões principais, aba selecionada, "Como medir?" / texto sobre o laranja | `#D97757` / `#11181A` |
| `selectedContainer` | chip selecionado ("R$ 20,00", "Flex"), com borda `primary` | `#8B523F` |
| `positive` / `positiveContainer` | lucro, "Expediente em andamento", texto do GPS / fundo do banner "GPS ativo" | `#1CB950` / `#0A461E` |
| `warningContainer` / `onWarningContainer` | banner "GPS ficou inativo por X min" | `#6E6E13` / `#FFFF00` |
| `textSecondary` | placeholders, rótulos, divisória da barra inferior | `#A1A1A1` |
| `navUnselected` | ícones e rótulos das abas não selecionadas | `#D9D9D9` |

Protótipos: [login e cadastro](prototypes/01-login-cadastro.png) · [home sem expediente](prototypes/02-home-sem-expediente.png) · [home com expediente, lançar ganho e encerrar](prototypes/03-home-expediente.png) · [histórico](prototypes/04-historico.png) · [perfil, moto e combustível](prototypes/05-perfil.png).

---|---|---|
| `background` | fundo | `#000000` |
| `surface` | cards, campos, bottom sheet | `#1F1F1F` |
| `primary` | botões principais ("Entrar", "Lançar ganho") | `#D97757` |
| `positive` | lucro, "Expediente em andamento", GPS ativo | `#22C55E` |
| `warning` | "GPS ficou inativo por X min" | amarelo-oliva `#6B6B1A` / texto `#FACC15` |

---

## 4. Telas → endpoints

| Tela (protótipo) | Ação | Endpoint |
|---|---|---|
| **Entre** | Entrar | `POST /auth/login` |
| **Cadastre** | Cadastrar (nome, e-mail, senha, confirmar) | `POST /auth/register` → `POST /auth/login` |
| | *Divergência intencional:* o protótipo repete "Cadastrar-se" + "Entrar" da tela de login; no app o Cadastro tem link **"Entrar"** (volta ao login) e botão **"Cadastrar"** | — |
| **Perfil** | Nome | `GET /users/me`, `PATCH /users/me` |
| | "Cadastrar veículo" / "Minha moto" / "Atualizar" | `GET /vehicles`, `POST /vehicles`, `PATCH /vehicles/{id}` |
| | Combustível (Gasolina/Etanol/Flex) | `GET /fuels` |
| | "Valores do combustível" (ver/editar) | `GET /fuel-prices/current`, `PUT /fuel-prices` |
| **Histórico** | Lista "Ontem / 18/09/2026 — Lucro do dia" | `GET /analytics/daily` |
| | "Iniciar expediente" (moto Flex pergunta o combustível) | `POST /shifts` |
| **Home (sem expediente)** | "Sábado, 19 de Setembro", "Olá, {nome}" | data local + `GET /users/me` |
| | Card "Último registro — Ontem — Lucro do dia" com Ganhos, Combustível, KM, Tempo, Valor hora, Valor por KM | dia mais recente de `GET /analytics/daily` (valor hora/km calculados no app a partir dos totais, mesma regra `ROUND_HALF_UP` da API) |
| | "Iniciar expediente" (também na Home, não só no Histórico) | `POST /shifts` |
| **Home (em andamento)** | "Expediente em andamento 3h 42min", Lucro Parcial, Ganhos, Combustível, KM, Tempo, Valor hora, Valor por KM | `GET /shifts/current` (atualização periódica) |
| | "GPS ativo – último ponto há 4s" | relógio local + `gps.last_point_at` |
| | "Lançar ganho" (R$ 5 / 8 / 12 / 20) | `POST /shifts/{id}/earnings` |
| | "Sobrescrever ganho atual" | `PATCH /shifts/{id}/earnings/{último id}` |
| | "Encerrar Expediente" → sheet com "GPS ficou inativo por 6 min" + resumo → "Encerrar" / "Continuar" | `GET /shifts/current` (`gps.gaps`) → `POST /shifts/{id}/finish` |
| (contínuo) | Envio de pontos | `POST /shifts/{id}/track-points` |

Tratamento de erros comum: o corpo `{"detail": "..."}` vira mensagem na tela; **401** limpa a sessão e volta ao Login; **409** "Já existe um expediente em andamento" leva à Home.

---

## 5. Rastreamento de GPS

Ponto mais delicado do app — e onde a API já foi preparada (lote, deduplicação, `rejected`).

1. **Permissões:** `ACCESS_FINE_LOCATION` (em uso), `POST_NOTIFICATIONS` (Android 13+), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` (Android 14+). Como a coleta roda num *foreground service* iniciado com o app aberto, **não** é preciso localização em segundo plano (`ACCESS_BACKGROUND_LOCATION`), o que simplifica a aprovação.
2. **Coleta:** `LocationRequest` de alta precisão a cada **5–10 s**; cada leitura vira um ponto (`recorded_at` = horário do fix com fuso, lat/long, `accuracy_m`, `speed_ms`) gravado no **Room**.
3. **Envio:** a cada ~30 s ou 50 pontos, o app manda até 500 pontos pendentes. Pontos são apagados da fila **só depois** do 202. Se a resposta se perder, o reenvio é seguro: a API conta como `duplicated`.
4. **Sem internet:** a fila cresce no Room; o `SyncWorker` (WorkManager, restrição de rede) esvazia quando a conexão volta.
5. **Encerrar:** antes do `POST /finish`, o app força um último envio da fila para o resumo final considerar todos os pontos.
6. **App morto / celular reiniciado:** ao abrir, se `GET /shifts/current` devolver um expediente, o app religa o serviço.

---

## 6. Estratégia de testes

| Tipo | Ferramentas | O que cobre |
|---|---|---|
| Unitário (JVM) | JUnit, MockK, `kotlinx-coroutines-test`, Turbine | ViewModels (estados de carregamento/erro/sucesso), formatação de moeda e tempo, regra "sobrescrever o último ganho", fila de GPS |
| Rede | OkHttp `MockWebServer` | DTOs batem com o JSON da API (strings decimais, datas ISO), interceptor do token, 401 → logout, mapeamento de `{"detail"}` |
| Room | testes instrumentados com banco em memória | fila de pontos: inserir, pegar lote de 500, apagar após 202 |
| UI | Compose UI Test | Login, lançar ganho, bottom sheet de encerrar |
| Ponta a ponta (manual) | emulador + rota GPX + API local | roteiro da §8 |
| Contrato | `ApiContractTest` + `app/src/test/resources/api/*.json` | Respostas **reais** da API (capturadas por `scripts/capture_api_fixtures.py`) desserializadas nos DTOs do app |
| CI | GitHub Actions | `./gradlew lint ktlintCheck testDebugUnitTest assembleDebug` neste repositório (workflow próprio, independente do da API) |

---

### 6.1 Atualizar o contrato quando a API mudar

As fixtures em `app/src/test/resources/api/` são respostas reais do `pi-iv`. Se um endpoint mudar, regenere e rode os testes (o `ApiContractTest` aponta o que quebrou nos DTOs):

```bash
cd ../pi-iv && docker compose up -d db && cd api && uv run alembic upgrade head
uv run fastapi run app/main.py --port 8776 &
uv run python ../../pi-iv-app/scripts/capture_api_fixtures.py ../../pi-iv-app/app/src/test/resources/api
kill %1
cd ../../pi-iv-app && ./gradlew testDebugUnitTest
```

O script cria um usuário fictício (`fixture.app@example.com`), percorre todos os endpoints, troca o token real por um fictício e salva cada resposta; depois apague esse usuário do banco de desenvolvimento.

## 7. Etapas

Tudo abaixo acontece **neste repositório** (`pi-iv-app`). O `pi-iv` (API) só muda se o app precisar de algo novo no contrato.

| # | Etapa | Entregáveis | Pronto quando | Status |
|---|---|---|---|---|
| 0 | **Setup do ambiente** | Android Studio + SDK + AVD Google Play; API do `pi-iv` rodando e acessível do emulador; GPX de rota real em `docs/gps/` | Checklist da §2.4 completo | 🟡 falta clonar no Windows |
| 1 | **Scaffold** | Projeto Compose na raiz deste repositório, *version catalog*, Hilt, ktlint, tema dos protótipos, navegação (auth + 3 abas vazias), `BuildConfig.API_BASE_URL` por build type, `network_security_config` do debug | App abre no emulador com as 3 abas e chama `/health` com sucesso | ✅ |
| 2 | **Rede e sessão** | Retrofit/OkHttp/serialization, DTOs de todos os endpoints, `AuthInterceptor`, DataStore do token, `ApiError`, logout em 401; testes com `MockWebServer` | Testes de rede verdes | ✅ |
| 3 | **Login e Cadastro** | Telas "Entre" e "Cadastre", validação local (senhas iguais, ≥ 8 caracteres), erros da API na tela, sessão persistida | Cadastro → login → abas; reabrir o app mantém logado | ✅ |
| 4 | **Perfil** | Nome editável; card "Minha moto" / "Cadastrar veículo" com bottom sheet (modelo, combustível, consumo, cilindrada, ano; "Como medir?"); "Valores do combustível" com edição | Cadastrar moto e preços pelo app e conferir via Swagger | |
| 5 | **Iniciar expediente** | Botão "Iniciar expediente" (Home e Histórico); escolha Gasolina/Etanol para moto Flex; mensagens de 409 (sem preço / já em andamento) | Expediente criado aparece em `GET /shifts/current` | |
| 6 | **Rastreamento de GPS** | Permissões, *foreground service* com notificação, Room (fila), envio em lote, `SyncWorker`, religar serviço ao abrir o app | Rota GPX no emulador → pontos chegam na API; com Wi-Fi desligado a fila cresce e esvazia ao religar | |
| 7 | **Home em andamento** | Cronômetro, banner "GPS ativo – último ponto há Xs", card Lucro Parcial (atualização periódica), "Lançar ganho" (5/8/12/20 + valor livre), "Sobrescrever ganho atual", sheet de encerrar com aviso de GPS inativo, "Continuar" | Valores da Home batem com `GET /shifts/current`; encerrar para o serviço de GPS | |
| 8 | **Histórico** | Lista por dia ("Ontem", "dd/MM/yyyy"), lucro em verde/vermelho, estado vazio, *pull-to-refresh* | Dias e lucros batem com `GET /analytics/daily` | |
| 9 | **Polimento** | Estados de carregamento/erro/sem internet, formatação pt-BR, acessibilidade (tamanhos de toque, contraste, TalkBack), ícone e nome do app | Roteiro da §8 completo sem falhas | |
| 10 | **Entrega** | CI do app, build *release* (APK assinado), README com como rodar, vídeo/roteiro de demonstração | APK instalado num celular real executa o roteiro da §8 | |

---

## 8. Roteiro de validação da entrega

Executar com a API local + emulador reproduzindo a rota GPX (ou celular com `adb reverse`). Cada item tem o resultado esperado conferível no app **e** na API (Swagger ou `psql`).

1. **Cadastro e login** — cadastrar, sair, entrar de novo. E-mail repetido mostra "E-mail já cadastrado"; senha errada mostra "E-mail ou senha inválidos".
2. **Perfil** — cadastrar moto (Honda, Gasolina, 38 km/l) e preços (Gasolina 6,20 / Etanol 3,75). Card "Minha moto" mostra Gasolina e 38 km/l.
3. **Iniciar sem preço** (com outro usuário) — mensagem "Cadastre o preço do combustível antes de iniciar".
4. **Iniciar expediente** — Home muda para "Expediente em andamento"; notificação aparece.
5. **Rodar a rota** (GPX a 1×–3×) — "GPS ativo – último ponto há Xs" atualiza; KM e Combustível sobem; em paradas o KM para e o combustível sobe devagar (motor ligado).
6. **Lançar ganhos** R$ 12, R$ 8, R$ 20 — Ganhos = R$ 40,00; Lucro Parcial = 40 − combustível.
7. **Sobrescrever ganho atual** para R$ 12 — Ganhos = R$ 32,00.
8. **Sem internet** — desligar Wi-Fi/dados por ~2 min com a rota rodando, religar — nenhum ponto perdido nem duplicado (contagem em `GET /shifts/{id}/track-points`).
9. **GPS inativo** — pausar a rota por 6 min — sheet de encerrar mostra "GPS ficou inativo por 6 min".
10. **Encerrar** — resumo final igual ao `GET /shifts/{id}`; tentar lançar ganho depois é bloqueado; notificação some.
11. **Histórico** — "Hoje"/"Ontem" com o lucro do dia correto; iniciar um novo expediente pelo botão.
12. **Moto Flex** — trocar a moto para Flex; ao iniciar, o app pergunta o combustível; com Etanol, o custo usa R$ 3,75.
13. **App fechado à força** durante o expediente — ao reabrir, Home volta ao expediente em andamento e o GPS continua.

---

## 9. Decisões em aberto

- **Nome do app e `applicationId`** (ex.: `br.univesp.pi.expedientes`).
- **minSdk** — sugestão: **26 (Android 8.0)**, cobre a grande maioria dos aparelhos e simplifica APIs de data/notificação.
- **Atualização da Home** — sugestão: buscar `GET /shifts/current` a cada **15 s** com a Home visível (simples e suficiente); cronômetro e "último ponto há Xs" calculados localmente a cada segundo.
- **Tela de resumo por período** (`/analytics/summary`) — a API já oferece, mas não há protótipo; incluir como card no topo do Histórico?
- **Ganho com valor livre** — o protótipo mostra só 5/8/12/20; permitir digitar outro valor?
- **Arredondamento do valor por km** — alinhar com a API (2,40 vs 2,39 do protótipo).
- **Onde a API roda na apresentação** — local (WSL + emulador) ou publicada (exigiria Dockerfile/deploy e HTTPS).
