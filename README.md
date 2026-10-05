# pi-iv-app

App Android (Kotlin + Jetpack Compose) para motociclistas registrarem seus expedientes: coleta o GPS durante o trabalho, lança os ganhos e mostra o lucro parcial, o resumo final e o histórico por dia.

Os cálculos (distância, tempo parado com motor ligado, combustível, lucro) são feitos pela API, no repositório irmão **[`pi-iv`](../pi-iv)**:

```
univesp/
  pi-iv/        # API (FastAPI + PostgreSQL) — contrato em pi-iv/docs/api-spec.md
  pi-iv-app/    # este repositório
```

## Status

Em planejamento. Etapas, arquitetura e roteiro de validação em [`docs/app-plan.md`](docs/app-plan.md); passo a passo do ambiente em [`docs/setup.md`](docs/setup.md).

## Rodando a API para o app

```bash
cd ../pi-iv
docker compose up -d db
cd api
uv run alembic upgrade head
uv run fastapi dev app/main.py --host 0.0.0.0 --port 8000
```

- Emulador Android: `http://10.0.2.2:8000/api/v1`
- Celular via USB: `adb reverse tcp:8000 tcp:8000` e `http://localhost:8000/api/v1`
- Documentação interativa da API: `http://localhost:8000/docs`
# pi-iv-app
