# Alongamento de Tela

App Android para **esticar a tela do sistema** em jogos (FOV / HUD) **sem modificar o APK, OBB ou arquivos do jogo**.

Usa `wm size` e `wm density` com permissão de shell via **Shizuku** ou **depuração Wi‑Fi (ADB no próprio aparelho)**. Sem root.

## O que faz

- Presets por proporção calculados neste celular: 16:9, 18:9, 20:9, 21:9, 32:9
- Largura, altura e DPI manuais
- Seletor de qualquer jogo
- Aplicar ao abrir o jogo e restaurar ao sair (acesso ao uso)
- Confirmação com temporizador (desfaz se a tela ficar ruim)
- Tiles de ajustes rápidos: Alongar e Restaurar
- Interface 100% em português, tema escuro neon

## Ativação

1. **Shizuku** (recomendado): instale o [Shizuku](https://shizuku.rikka.app/download/), inicie com depuração sem fio e autorize o app.
2. **Depuração Wi‑Fi**: Android 11+, pareie com o código de 6 dígitos na aba Ativação e conecte. Depois o app executa os mesmos comandos `wm`.

## APK (GitHub Actions)

Cada push/PR gera o artefato **AlongamentoDeTela** (APK release, assinado com a chave de debug para instalação direta).

Em [Actions](../../actions) → workflow **Build APK** → *AlongamentoDeTela*.

## Aviso

Alterar resolução pode deixar a tela estranha em alguns aparelhos. Use **Restaurar**, o tile, ou espere o temporizador. Uso por sua conta e risco.
