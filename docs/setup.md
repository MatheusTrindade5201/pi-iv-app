# Setup do ambiente (etapa 0)

O **código é desenvolvido no WSL** (`~/projects/personal/univesp/pi-iv-app`); o **Android Studio e o emulador rodam no Windows**, numa cópia sincronizada via Git. A API (`../pi-iv`) roda no **WSL**.

## 0. O que baixar

| Ferramenta | Obrigatória? | Onde baixar | Observação |
|---|---|---|---|
| **Android Studio** | Sim | <https://developer.android.com/studio> | Instalador `.exe` para Windows. Já inclui o Java (JBR); o SDK, o emulador e o `adb` são baixados **por dentro** dele (SDK Manager), sem download separado. |
| **Git para Windows** | Sim | <https://git-scm.com/download/win> | Para a cópia do projeto no Windows (`git clone` / `git pull`), ver "Duas cópias do projeto", abaixo. |
| **Plataforma do Hipervisor do Windows** | Sim | Já vem no Windows — só ativar (passo 1) | Aceleração do emulador. |
| WSL2 + Docker + uv | Já instalados | WSL: <https://learn.microsoft.com/windows/wsl/install> · Docker: <https://docs.docker.com/get-started/get-docker/> · uv: <https://docs.astral.sh/uv/getting-started/installation/> | Só para referência: é o que roda a API (`pi-iv`). |
| Driver USB do celular | Só se usar celular | Pixel: *Google USB Driver* pelo SDK Manager · Outras marcas: <https://developer.android.com/studio/run/oem-usb> | Samsung, Motorola etc. têm driver próprio no site do fabricante. |
| scrcpy | Opcional | <https://github.com/Genymobile/scrcpy> ou `winget install Genymobile.scrcpy` | Espelha a tela do celular no PC (apresentação). |
| Platform-Tools avulso | Não | <https://developer.android.com/tools/releases/platform-tools> | Só se quiser o `adb` sem o Android Studio; com o Studio instalado não precisa. |

Documentação de apoio: emulador <https://developer.android.com/studio/run/emulator> · simular localização <https://developer.android.com/studio/run/emulator-extended-controls#location> · depuração USB <https://developer.android.com/studio/debug/dev-options>.

### Duas cópias do projeto (WSL ↔ Windows via Git)

```
WSL  ~/projects/personal/univesp/pi-iv-app      ← desenvolvimento, build e testes pela linha de comando
        │ git push
        ▼
     repositório remoto (GitHub)
        │ git pull
        ▼
Windows  C:\Users\mathe\projects\pi-iv-app      ← Android Studio + emulador (só para rodar)
```

- **Edite só no WSL.** A cópia do Windows serve para abrir no Android Studio e rodar no emulador; editar nas duas gera conflito.
- O Android Studio do Windows **não** abre o projeto direto do WSL (`\\wsl.localhost\...`): o Gradle fica muito lento lendo arquivos pela rede do WSL. Por isso a cópia via Git.
- `local.properties` (caminho do SDK, diferente em cada lado) e a pasta `build/` não vão para o Git; cada lado gera os seus.
- O `.gitattributes` força finais de linha LF, para o `gradlew` continuar funcionando no WSL mesmo com commits feitos no Windows.
- Sem remoto (ou para testar rápido), o Windows pode puxar direto do WSL:
  ```powershell
  git remote add wsl \\wsl.localhost\Ubuntu\home\matheus\projects\personal\univesp\pi-iv-app
  git pull wsl main
  ```

Clonar no Windows (uma vez, depois que o repositório remoto existir), no PowerShell:

```powershell
mkdir $HOME\projects -Force; cd $HOME\projects
git clone <url-do-repositorio> pi-iv-app
```

Depois: **Android Studio → Open →** `C:\Users\mathe\projects\pi-iv-app`. A cada atualização: `git pull` (ou **Git → Pull** no Studio) e ▶ Run.

## 1. Preparar o Windows

1. Recomendado: 16 GB de RAM (8 GB funciona, mas o emulador fica lento) e ~20 GB livres em disco.
2. Ativar a aceleração do emulador: **Painel de Controle → Programas → Ativar ou desativar recursos do Windows** → marcar **Plataforma do Hipervisor do Windows** → OK → **reiniciar**.
   - A virtualização na BIOS já está ligada (o WSL2 depende dela).

## 2. Instalar o Android Studio (no Windows)

1. Baixar a versão estável mais recente em **developer.android.com/studio** e instalar com as opções padrão.
2. Na primeira abertura, escolher **Standard** no assistente (instala SDK, emulador e ferramentas). O Java já vem junto.
3. Abrir **More Actions → SDK Manager** (ou *Settings → Languages & Frameworks → Android SDK*):
   - **SDK Platforms:** marcar a versão estável mais recente do Android.
   - **SDK Tools:** conferir **Android SDK Build-Tools**, **Android SDK Platform-Tools** e **Android Emulator**. Se for usar celular Pixel, marcar também **Google USB Driver**.
   - **Apply**.
4. Colocar o `adb` no PATH do Windows:
   - Pesquisar **"Editar as variáveis de ambiente do sistema"** → **Variáveis de Ambiente** → *Path* (do usuário) → **Novo** → `%LOCALAPPDATA%\Android\Sdk\platform-tools`
   - Abrir um PowerShell **novo** e conferir:
     ```powershell
     adb version
     ```
   - Usar sempre o `adb` do Windows, não instalar no WSL (dois servidores `adb` entram em conflito).

## 3. Criar o emulador

1. **More Actions → Virtual Device Manager → Create Virtual Device**.
2. **Phone → Pixel** (modelo recente) → Next.
3. Imagem: versão mais recente **com o ícone da Play Store** (imagem **Google Play** — o GPS do app usa o Google Play Services) → baixar → Next.
4. **Show Advanced Settings** → RAM **4096 MB** → Finish.
5. ▶ para ligar. Deve abrir com a Play Store disponível.

## 4. Subir a API (no WSL)

```bash
cd ~/projects/personal/univesp/pi-iv
docker compose up -d db
cd api
uv run alembic upgrade head
uv run fastapi dev app/main.py --host 0.0.0.0 --port 8000
```

O `--host 0.0.0.0` é obrigatório; sem ele a API só aceita conexões de dentro do WSL.

Testar no **PowerShell do Windows**:

```powershell
curl.exe http://localhost:8000/api/v1/health
```

Esperado: `{"status":"ok","database":"ok"}`.

Se não responder:
1. Confirmar que a API subiu com `--host 0.0.0.0`.
2. Conferir se `%UserProfile%\.wslconfig` não tem `localhostForwarding=false`.
3. `wsl --shutdown`, abrir o WSL de novo e subir a API novamente.

## 5. Testar a API pelo emulador

1. No emulador, abrir o **Chrome** em:
   ```
   http://10.0.2.2:8000/api/v1/health
   ```
   (`10.0.2.2` é o Windows visto de dentro do emulador.)
2. Abrir também `http://10.0.2.2:8000/docs` — o Swagger deve carregar.

Se não abrir: quase sempre é a API sem `--host 0.0.0.0`; a segunda causa é o Firewall do Windows — na primeira vez ele pode perguntar, permitir em **redes privadas**.

## 6. Testar o GPS simulado no emulador

1. Barra lateral do emulador → **⋯ (Extended controls) → Location**.
2. **Ponto fixo:** pesquisar "Praça da Sé, São Paulo" → **Set location**. Abrir o Google Maps no emulador: o ponto azul deve estar na Sé.
3. **Rota:** aba **Routes** → clicar num ponto de partida e num de chegada no mapa → **Save route** → **Play route** com velocidade **1x a 3x**. O ponto azul deve andar no Maps.

> Acima de 3x a "moto" passa de 150 km/h e a API descarta os pontos como salto de GPS (`GPS_MAX_SPEED_KMH`).

## 7. Celular físico (opcional, recomendado para a demonstração)

1. **Configurações → Sobre o telefone** → tocar **7 vezes** em *Número da versão* (libera as **Opções do desenvolvedor**).
2. **Opções do desenvolvedor** → ativar **Depuração USB**.
3. Ligar no PC pelo cabo e aceitar "Permitir depuração USB" no celular.
4. No PowerShell:
   ```powershell
   adb devices
   adb reverse tcp:8000 tcp:8000
   ```
   `adb devices` deve listar o aparelho como `device` (se aparecer `unauthorized`, aceitar o pedido no celular).
5. No Chrome do celular: `http://localhost:8000/api/v1/health`.

- O `adb reverse` some quando o cabo desconecta — rodar de novo a cada reconexão.
- Se o Windows não reconhecer o celular: instalar o driver USB do fabricante (ou o *Google USB Driver*, para Pixel).
- Opcional: **scrcpy** espelha a tela do celular no PC (`winget install Genymobile.scrcpy`).

## 8. Checklist final

- [ ] `adb version` funciona no PowerShell
- [ ] Emulador Pixel com Google Play liga
- [ ] `curl.exe http://localhost:8000/api/v1/health` no Windows → `ok`
- [ ] `http://10.0.2.2:8000/api/v1/health` abre no Chrome do emulador
- [ ] Swagger abre em `http://10.0.2.2:8000/docs` no emulador
- [ ] Ponto fixo e rota simulada movem o ponto azul no Google Maps do emulador
- [ ] (celular) `adb devices` mostra o aparelho e o health abre no celular após `adb reverse`
- [ ] No WSL: `java -version` (21) e `sdkmanager --list` funcionam
- [ ] Repositório remoto criado e clonado no Windows em `C:\Users\mathe\projects\pi-iv-app`

## 9. Build pela linha de comando no WSL

Para compilar e rodar os testes no WSL (sem Android Studio nem emulador), antes de mandar para o Windows:

1. **JDK 21:**
   ```bash
   sudo apt update && sudo apt install -y openjdk-21-jdk-headless unzip
   java -version
   ```
2. **Android SDK (só linha de comando):** baixar o pacote *Command line tools only* para **Linux** em <https://developer.android.com/studio#command-line-tools-only> e:
   ```bash
   mkdir -p ~/Android/Sdk/cmdline-tools
   unzip ~/Downloads/commandlinetools-linux-*_latest.zip -d ~/Android/Sdk/cmdline-tools
   mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
   ```
3. **Variáveis** (bloco no fim do `~/.bashrc`, depois `source ~/.bashrc`):
   ```bash
   # >>> android sdk (pi-iv-app) >>>
   export ANDROID_HOME="$HOME/Android/Sdk"
   export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
   # <<< android sdk (pi-iv-app) <<<
   ```
   O `platform-tools` (o `adb` do Linux) fica **fora do PATH** de propósito — ver aviso abaixo.
4. **Pacotes do SDK** (mesmas versões do SDK Manager do Windows):
   ```bash
   yes | sdkmanager --licenses
   sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
   sdkmanager --list_installed
   ```
5. Com o projeto criado (etapa 1), conferir:
   ```bash
   cd ~/projects/personal/univesp/pi-iv-app
   ./gradlew assembleDebug testDebugUnitTest
   ```

Instalado nesta máquina em 2026-10-05: OpenJDK 21.0.12 (`apt`), command-line tools `commandlinetools-linux-15859902` (SHA-256 conferido com o publicado pelo Google), Platform-Tools 37.0.1, Platform 37.0 e Build-Tools 37.0.0.

Não rode o `adb` do WSL: ele subiria um segundo servidor `adb` que briga com o do **Windows**, que é quem fala com o emulador e o celular.

Tudo marcado → etapa 0 concluída; próxima: etapa 1 (scaffold do projeto Compose), ver [`app-plan.md`](app-plan.md#7-etapas).
