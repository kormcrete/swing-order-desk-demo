Order Desk, the demo app of [swing-ui-agent](https://github.com/kormcrete/swing-ui-agent), ready to run: Java is
included, and the agent listens on `http://127.0.0.1:8770/mcp` as soon as the app starts.

| Your computer | Download |
|---|---|
| Mac with Apple Silicon (M1 and later) | `kormcrete-order-desk-macos-arm64.dmg` |
| Mac with Intel | `kormcrete-order-desk-macos-x64.dmg` |
| Windows (x64) | `kormcrete-order-desk-windows-x64.zip` |

**First launch.** The downloads are not signed yet.

- macOS: open the `.dmg` and drag **Kormcrete Order Desk** to Applications. The app is not notarized by Apple yet, so
  macOS refuses to open it ("Apple could not verify…"). Clear the download mark once, in Terminal, then open it:

      xattr -dr com.apple.quarantine "/Applications/Kormcrete Order Desk.app"
- Windows: unzip, open the **Kormcrete Order Desk** folder and run **Kormcrete Order Desk.exe**. If SmartScreen stops
  it, click **More info → Run anyway**.

**Connect your coding agent** while the app runs:

    claude mcp add --transport http order-desk http://127.0.0.1:8770/mcp

Other MCP clients: https://kormcrete.github.io/swing-ui-agent/agents/mcp-clients/. If the agent cannot connect,
another app may be using port 8770; the agent's log says so: `~/Library/Logs/Kormcrete Order Desk/agent.log` (macOS),
`%LOCALAPPDATA%\Kormcrete Order Desk\agent.log` (Windows).
If the app does not open at all on macOS, the reason is in `~/Library/Logs/Kormcrete Order Desk/order-desk.log`
(for example an Intel Mac with the Apple Silicon download).
