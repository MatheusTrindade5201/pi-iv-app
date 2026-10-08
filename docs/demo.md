# Roteiro de demonstração

Roteiro para validar a entrega e gravar o vídeo (~8 min). Cada passo tem o que mostrar no app e onde conferir na API (Swagger em `http://localhost:8000/docs`). É a versão "para apresentar" do roteiro de validação da [§8 do plano](app-plan.md#8-roteiro-de-validação-da-entrega).

## Preparação (antes de gravar)

1. **API** no WSL:
   ```bash
   cd ../pi-iv && docker compose up -d db && cd api
   uv run alembic upgrade head
   uv run fastapi dev app/main.py --host 0.0.0.0 --port 8000
   ```
2. **Emulador** (Pixel com Google Play, Android 15+) aberto no Android Studio; app instalado pelo ▶ **Run** (debug) — ou celular com o APK de release + `adb reverse tcp:8000 tcp:8000`.
3. Em *Extended controls → Location → Routes*: importar [`gps/rota-sp.gpx`](gps/rota-sp.gpx), velocidade **2×**, ainda **sem** dar *Play*.
4. Usar um **e-mail novo** (ex.: `demo+1@exemplo.com`) para começar do zero.
5. Swagger aberto numa aba, já autorizado com o token do usuário de demonstração (pegar em `POST /auth/login`), para as conferências.

## Gravação

| # | Tempo | No app | Conferir |
|---|---|---|---|
| 1 | 0:00 | Ícone **Expediente** → tela **Entre** → "Cadastrar-se" → cadastrar nome, e-mail, senha | Entra direto na Home: "Olá, {nome}" e "Nenhum expediente registrado ainda." |
| 2 | 0:40 | **Iniciar expediente** sem moto → diálogo "Cadastre sua moto" → **Ir para o Perfil** | — |
| 3 | 1:00 | Perfil → **Cadastrar veículo**: Honda CG 160, Gasolina, 38 km/l, 160 cc, 2022 → **Valores do combustível**: Gasolina 6,20 | `GET /vehicles`, `GET /fuel-prices/current` |
| 4 | 1:40 | Home → **Iniciar expediente** → aceitar localização e notificações | Notificação fixa "Expediente em andamento"; banner "GPS ativo - aguardando sinal"; `GET /shifts/current` |
| 5 | 2:10 | *Play route* no emulador | Banner "GPS ativo - último ponto há Xs"; após ~30 s o KM e o Combustível sobem; nas paradas da rota (MASP 2 min, Paraíso 3 min) o KM para e o combustível sobe devagar (motor ligado) |
| 6 | 3:00 | **Lançar ganho** R$ 12 → R$ 8 → R$ 20 | Ganhos = R$ 40,00 e Lucro Parcial = 40 − combustível; `GET /shifts/{id}/earnings` |
| 7 | 3:40 | **Lançar ganho** → R$ 12 → **Sobrescrever ganho atual** | Ganhos = R$ 32,00 (o último, R$ 20, virou R$ 12) |
| 8 | 4:10 | Desligar Wi-Fi/dados do emulador ~1 min com a rota rodando | Faixa "Sem internet…"; ao religar, os pontos chegam sem duplicar (`GET /shifts/{id}/track-points`) |
| 9 | 5:10 | Fechar o app à força e abrir de novo | Volta em "Expediente em andamento" e o GPS retoma |
| 10 | 5:40 | Desligar a **Localização** do emulador (atalhos rápidos) por ~3 min e religar | Banner amarelo "GPS inativo - último ponto há 3 min"; ao religar volta a verde |
| 11 | 6:10 | **Encerrar Expediente** → sheet com "GPS ficou inativo por X min" e o resumo → **Encerrar** | Notificação some; Home volta para "Último registro — Hoje"; `GET /shifts/{id}` igual ao resumo |
| 12 | 6:50 | Aba **Histórico** → dia de hoje → tocar para ver detalhes; trocar 7/30/90 dias | `GET /analytics/daily` e `GET /analytics/summary` batem com a tela |
| 13 | 7:30 | Perfil → **Sair da conta** | Volta para **Entre** |

## Se algo der errado ao vivo

- **App não conecta:** `http://localhost:8000/health` no navegador do Windows; no emulador a API é `10.0.2.2:8000`.
- **GPS não começa:** confirmar a permissão de localização (Configurações do app) e que a rota está em *Play*; o botão **Ativar** do banner religa o serviço.
- **"Cadastre o preço do combustível antes de iniciar":** faltou o passo 3 (preço da Gasolina).
- **Pontos não aparecem:** a fila envia a cada 30 s ou 50 pontos; puxar para atualizar no Histórico depois de encerrar.

## Checklist de entrega

- [ ] Roteiro acima executado sem falhas no emulador
- [ ] APK de release instalado num celular real executa os passos 1–7 e 11–13 (com `adb reverse`)
- [ ] CI verde na `main` (ktlint, Lint, testes, builds debug e release)
- [ ] Vídeo gravado
