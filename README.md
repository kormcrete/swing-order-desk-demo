# Order Desk

The demo app of [swing-ui-agent](https://github.com/kormcrete/swing-ui-agent). Order Desk is a small Swing order desk
where a shop employee takes an order for a customer: pick the customer, add products to the cart, check out (address,
shipping, payment, review) and place the order. It has the parts real Swing business apps have, such as closable
tabs, sortable tables, a tree, popup menus, a wizard dialog with rules, a background task and a file chooser, so a
coding agent has real work to drive and check. Java 8, FlatLaf, MigLayout. All customers, products and orders are
invented, and everything lives in memory: each start begins with the same data.

![Order Desk: the Orders tab](docs/order-desk.png)

## Build

swing-ui-agent is not on Maven Central yet, so clone both repositories side by side and install it first:

```bash
git clone https://github.com/kormcrete/swing-ui-agent.git
git clone https://github.com/kormcrete/swing-order-desk-demo.git
(cd swing-ui-agent && mvn -B install -DskipTests)
```

Then build Order Desk:

```bash
cd swing-order-desk-demo
mvn -B package
```

This writes `target/order-desk.jar` (the app, runnable) and copies the agent to `target/swing-ui-agent-agent.jar`.
The build runs on Java 8 in CI.

## Start

```bash
java -javaagent:target/swing-ui-agent-agent.jar=port=8765 -Dorderdesk.today=2026-10-12 -jar target/order-desk.jar
```

`-javaagent:…` lets a coding agent drive the app on port 8765; without it Order Desk is a normal app.
`-Dorderdesk.today=2026-10-12` fixes the date the app uses for new orders and the card expiry check, so every run
looks the same. Leave it out to use today's date.

## Drive it with your coding agent

Your coding agent drives the running app through the agent jar's command line, one call per step, and reads the
answers as JSON:

```bash
java -jar target/swing-ui-agent-agent.jar --port 8765 <tool> '<json>'
```

`tools` lists the tools and `tools <name>` shows one tool's arguments. Three calls to start with:

```bash
UI="java -jar target/swing-ui-agent-agent.jar --port 8765"

# what is on screen: the main window, three levels down, without table rows
$UI snapshot '{"window":"main","depth":3,"compact":true}'

# open the ▾ menu next to New order, then start a new order for Ada Shop Ltd
$UI click '{"target":{"role":"button","name":"New order for a recent customer"}}'
$UI pick '{"path":["Ada Shop Ltd"]}'
```

Point your agent at the skills in swing-ui-agent's
[`skills/`](https://github.com/kormcrete/swing-ui-agent/tree/main/skills) folder: they explain how to drive a Swing
app, how to turn what you see into a target, and how to record a UI test.

## Things to try

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
