Order Desk, the demo app of [swing-ui-agent](https://github.com/kormcrete/swing-ui-agent), ready to run: Java is
included, and the agent listens on `http://127.0.0.1:8770/mcp` as soon as the app starts.

| Your computer | Download |
|---|---|
| Mac with Apple Silicon (M1 and later) | `kormcrete-order-desk-macos-arm64.dmg` |
| Mac with Intel | `kormcrete-order-desk-macos-x64.dmg` |
| Windows (x64) | `kormcrete-order-desk-windows-x64.zip` |

**First launch.** The downloads are not signed yet.

- macOS: open the `.dmg`, drag **Kormcrete Order Desk** to Applications and open it. When macOS says it cannot verify
  the app: on macOS 15, open *System Settings → Privacy & Security* and click **Open Anyway**; on macOS 14 and
  earlier, right-click the app and choose **Open**.
- Windows: unzip, open the **Kormcrete Order Desk** folder and run **Kormcrete Order Desk.exe**. If SmartScreen stops
  it, click **More info → Run anyway**.

**Connect your coding agent** while the app runs:

    claude mcp add --transport http order-desk http://127.0.0.1:8770/mcp

Other MCP clients: https://kormcrete.github.io/swing-ui-agent/agents/mcp-clients/. If the agent cannot connect,
another app may be using port 8770; the agent's log says so: `~/Library/Logs/Kormcrete Order Desk/agent.log` (macOS),
`%LOCALAPPDATA%\Kormcrete Order Desk\agent.log` (Windows).
