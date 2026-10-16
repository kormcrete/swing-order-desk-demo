# Order Desk

The demo app of [swing-ui-agent](https://github.com/kormcrete/swing-ui-agent): a small Swing order desk where a shop
employee takes an order for a customer, made for a coding agent to drive and test. It has the parts real Swing
business apps have:

- closable tabs and a status bar;
- sortable, filterable tables, and a cart with an editable quantity spinner;
- a category tree, popup and context menus, keyboard shortcuts;
- a four-step checkout wizard with rules (credit limit, missing postal code, shipping and payment options);
- a background task, confirm dialogs, a file chooser, and a light and a dark theme.

Java 8, FlatLaf, MigLayout. Customers, products and orders are invented and live in memory: each start begins with the
same data.

**Status:** the app is complete; swing-ui-agent, which it demonstrates, is pre-release (`0.1.0-SNAPSHOT`).

![Order Desk: the Orders tab](docs/order-desk.png)

**Tour and recorded tests on the site: https://kormcrete.github.io/swing-ui-agent/demo/tour/**

## Download

Try it without building anything: each download starts Order Desk with the agent on port 8770. The Mac and Windows
downloads include Java (Zulu JRE 8); the Java zip runs on your own Java 8 or newer, on any computer (Linux too).

| Your computer | Download |
|---|---|
| Mac with Apple Silicon (M1 and later) | [kormcrete-order-desk-macos-arm64.dmg](https://github.com/kormcrete/swing-order-desk-demo/releases/latest/download/kormcrete-order-desk-macos-arm64.dmg) |
| Mac with Intel | [kormcrete-order-desk-macos-x64.dmg](https://github.com/kormcrete/swing-order-desk-demo/releases/latest/download/kormcrete-order-desk-macos-x64.dmg) |
| Windows (x64) | [kormcrete-order-desk-windows-x64.zip](https://github.com/kormcrete/swing-order-desk-demo/releases/latest/download/kormcrete-order-desk-windows-x64.zip) |
| Linux, or any computer with Java 8 or newer | [kormcrete-order-desk-java.zip](https://github.com/kormcrete/swing-order-desk-demo/releases/latest/download/kormcrete-order-desk-java.zip) |

The downloads are not signed yet, so the first launch takes a few extra clicks:

- **macOS:** open the `.dmg` and drag **Kormcrete Order Desk** to Applications. The app is not notarized by Apple yet,
  so macOS refuses to open it ("Apple could not verify…"). Clear the download mark once, in Terminal, then open it:

  ```bash
  xattr -dr com.apple.quarantine "/Applications/Kormcrete Order Desk.app"
  ```

- **Windows:** unzip, open the **Kormcrete Order Desk** folder and run **Kormcrete Order Desk.exe**. If SmartScreen
  stops it, click **More info → Run anyway**.
- **Java zip:** unzip, then run `order-desk` in the **Kormcrete Order Desk** folder (`order-desk.cmd` on Windows). It
  uses `JAVA_HOME` when set, else the `java` on your PATH, and prints the agent's lines in the terminal.

Then connect your coding agent while the app runs:

```bash
claude mcp add --transport http order-desk http://127.0.0.1:8770/mcp
```

Other MCP clients: [MCP clients](https://kormcrete.github.io/swing-ui-agent/agents/mcp-clients/). If your agent cannot
connect, another app may be using port 8770; the agent's log says so: `~/Library/Logs/Kormcrete Order Desk/agent.log`
(macOS), `%LOCALAPPDATA%\Kormcrete Order Desk\agent.log` (Windows), the terminal (Java zip).
If the app does not open at all on macOS, the reason is in `~/Library/Logs/Kormcrete Order Desk/order-desk.log`
(for example an Intel Mac with the Apple Silicon download).

## Build

swing-ui-agent is not on Maven Central yet. Clone both repositories side by side and install it first:

```bash
git clone https://github.com/kormcrete/swing-ui-agent.git
git clone https://github.com/kormcrete/swing-order-desk-demo.git
(cd swing-ui-agent && mvn -B install -DskipTests)
cd swing-order-desk-demo
mvn -B -f order-desk-plugin/pom.xml install
mvn -B package
```

`order-desk-plugin/` is Order Desk's plugin, its own small Maven project: a vocabulary of the app's steps ("Add Gel
pen, black to the cart"). The app's tests use it, so install it before `mvn package`. See
[The vocabulary plugin](#the-vocabulary-plugin).

## Run it in IntelliJ

Open the folder in IntelliJ. Its run configurations are ready to use:

- **Order Desk:** the app with the agent on port 8770, the vocabulary plugin and the palette (the bar for recording and replaying tests
  yourself; `palette=off` leaves it out for agent-only runs). It sets the date the recorded tests expect.
- **UI tests:** the recorded tests in `uitest/` (through `RecordedUiIT`) and the Java tests, against the running app
  (JUnit tag `ui`). The gutter icons work too.
- **All tests:** the unit tests (tag `!ui`), no app needed.

To replay a recorded test: run **Order Desk**, press **Load recorded test…** in the palette, pick `place-order` and
press **▶ Run**. To run them all as JUnit tests, run **UI tests** with the app up.

To record one yourself: press **● Record** in the palette, use the app, add checks with **Check**, press **Done**,
then **Export…** to save it as a recorded test (a JSON file, nothing to compile). The same panel can write a JUnit
class into `src/test/java` instead. Step by step:
https://kormcrete.github.io/swing-ui-agent/start/intellij/

## Start it from a shell

```bash
java -javaagent:target/swing-ui-agent-agent.jar=port=8770,plugins=order-desk-plugin/target/classes -Dorderdesk.today=2026-10-12 -jar target/order-desk.jar
```

- `-javaagent:…` lets a coding agent, or the UI tests, drive the app on port 8770. Without it, Order Desk is a normal
  app.
- `-Dorderdesk.today=2026-10-12` fixes the date the app uses, so every run looks the same.
- `plugins=order-desk-plugin/target/classes` loads the vocabulary plugin (after `mvn -f order-desk-plugin/pom.xml
  install`). It is optional for driving and recording; the recorded test `uitest/cart-with-steps.json` needs it.
- The palette (recording bar) is on by default; add `,palette=off` after the port for agent-only runs.

## Drive it with a coding agent

Tell your coding agent what the test does, in words; it drives the running app, records each step and check, names
the steps in plain words and saves a recorded test. You can watch the steps appear in the palette and undo what you do
not like. Underneath, it calls the agent jar's command line, one call per step:

```bash
UI="java -jar target/swing-ui-agent-agent.jar --port 8770"
$UI session '{"op":"reset"}'
$UI menu '{"path":["File","New order"]}'
$UI type '{"target":{"role":"text","name":"Customer"},"text":"Ada"}'
$UI press '{"keys":"Enter"}'
# … the steps, and expect checks of what is on screen …
$UI export_recorded_test '{"name":"ada-new-order"}'
```

Give your agent the skills in swing-ui-agent's
[`skills/`](https://github.com/kormcrete/swing-ui-agent/tree/main/skills) folder first. With Claude Code:
https://kormcrete.github.io/swing-ui-agent/start/claude-code/. Recording together, step by step:
https://kormcrete.github.io/swing-ui-agent/start/record-together/

## The recorded tests

A recorded test is a JSON file in `uitest/`, one step per action with its checks under it, exact visible values only.
Nobody writes it by hand: a person shows it with the palette, or the agent records it from words.

Order Desk ships two, both recorded by an agent and named in plain words:

- [`uitest/place-order.json`](uitest/place-order.json): a new order for Ada Shop Ltd, from File ▸ New order through
  the four checkout steps to the placed order (17 steps). It checks what is predictable: the totals, the review total
  €37.20, the placed order's receipt and the PO line. The order number is not checked; it is the app's next one.
- [`uitest/cart-totals.json`](uitest/cart-totals.json): Ada's cart with 3 × A4 paper and 10 × Gel pen, checking the
  line total, subtotal, volume discount, VAT and the €31.32 total (11 steps). The swing-ui-agent home page plays it.

The receipt check holds a date, so start the app the way this README does, with `-Dorderdesk.today=2026-10-12` (the
IntelliJ configuration **Order Desk** does).

- **Replay one:** press **Load recorded test…** in the palette, pick it and press **▶ Run**, or ask your agent to run
  it (`run_recorded_test`).
- **Fix one:** in the palette's **Steps**, replay, redo from a step, delete a step or give it a sentence, then save it
  back.
- **In Maven and CI:** [`RecordedUiIT`](src/test/java/io/github/kormcrete/orderdesk/RecordedUiIT.java), one small
  JUnit class, runs every file in `uitest/` against the running app:
  https://kormcrete.github.io/swing-ui-agent/start/recorded-tests/

## The vocabulary plugin

`order-desk-plugin/` names the app's own steps once: `openNewOrder`, `addToCart`, `setQuantity`, `addQtyToCart`,
`checkoutWithPo`, `placeOrder`, `openOrder` and the check `cartTotalIs`. It drives the app through the agent's client
only, so it needs no app classes.

- **The agent** runs a step in one call, for example "Add 10 × Gel pen, black to the cart".
- **A recording** folds the clicks that make a step into it, so a person's recorded test reads in the app's words:
  [`uitest/cart-with-steps.json`](uitest/cart-with-steps.json) is Ada's cart (the same as `cart-totals`) in `do`
  steps. It needs the plugin to run: start the app with `plugins=…` as above.
- **A Java test** calls the same code as a method: `CartTotalsUiIT` uses `addQtyToCart(ui, "Gel pen, black", 10)`.
- **One place per name:** each name the app shows (a button, a column, a tab) is written once, in `OrderDeskSteps`.

In IntelliJ, after you edit `OrderDeskSteps`, install the plugin (`mvn -f order-desk-plugin/pom.xml install`) or import
`order-desk-plugin/pom.xml` as a second Maven project before you run `CartTotalsUiIT` from the gutter; otherwise it
uses the last installed jar. How it works:
https://kormcrete.github.io/swing-ui-agent/plugins/vocabulary/

### Java tests, for more checks

When a test needs to read a value that changes per run, or check something the screen does not show, the agent writes
a Java test instead. Order Desk has two, made by an agent and reviewed by a person:
[`PlaceOrderUiIT`](src/test/java/io/github/kormcrete/orderdesk/PlaceOrderUiIT.java) takes an order for Ada Shop Ltd
through checkout and reads the new order's number, with a 2x screenshot after each step;
[`CartTotalsUiIT`](src/test/java/io/github/kormcrete/orderdesk/CartTotalsUiIT.java) fills Ada's cart with 3 × A4
paper and 10 × Gel pen, checks a line total and the €31.32 total, and discards the cart (the swing-ui-agent home page
plays the same steps).

- **Run them:** with the app running, use **UI tests** in IntelliJ or `mvn -B -Pui verify`. The tests find the agent
  through `src/test/resources/junit-platform.properties`. `mvn -B verify` runs only the unit tests.
- **In CI:** the `build` workflow runs them under Xvfb and uploads the screenshots as the `ui-screenshots` artifact.

The same order as a recorded test and as a Java test, side by side:
https://kormcrete.github.io/swing-ui-agent/demo/two-files/. The Java test step by step:
https://kormcrete.github.io/swing-ui-agent/demo/recorded-test/

## Things to try

Ask your agent to do one of these, or record it as a test with the palette or the agent:

- ▾ next to New order, then Ada Shop Ltd: a New order tab with Ada's account terms next to the Customer field.
- Add 3 × A4 paper and 10 × Gel pen, black: the 5% volume discount appears and the cart total is €31.32.
- Checkout… with Standard shipping and a PO number: the review shows €37.20, and Place order makes order ORD-1640.
- Birch & Co with an Espresso machine on invoice: the credit limit warning, then Pay by card instead.
- Elm Street Studio at checkout: the postal code is missing, so Next stays disabled until it is filled in.
- Café Lumen at checkout: no Express to the Netherlands, and Café Lumen pays by card only.
- 5 Staplers when 4 are in stock: the cart says so and Checkout… stays disabled; the Desk lamp cannot be added.
- The Orders tab: search for "birch", filter by status, right-click an order to cancel it.
- An order's tab: Save receipt… writes a plain-text receipt through a file chooser.
- File ▸ Close tab, ⌘W (Ctrl+W elsewhere) and File ▸ Exit, which ask first while a cart has products.
- View ▸ Theme ▸ Dark and back.

## License

Apache-2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). The libraries bundled in `order-desk.jar` and their
licenses are listed in `META-INF/THIRD-PARTY-NOTICES.txt` (in the jar, and in `src/main/resources`).
