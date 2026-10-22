# Order Desk plugin: coverage

Surveyed at: 1be3fe5 (2026-10-22)

Reach checked on the live app (agent jar 0.1.0-SNAPSHOT, `-Dorderdesk.today=2026-10-12`) with scoped `read`,
`expect`, `describe_ui` and scoped `snapshot` calls. Locators are written as the CLI takes them. `tab:New order*`
is any new order's tab; `<order>` is an order number such as `ORD-1640`; `"window":"dialog"` is the checkout dialog
or the message showing. A row marked `R<n>` waits for that reach fix (below) before its step can be written.

## Main window (menus, toolbar, status bar)

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The app starts on the Orders list, no other tab open (no login, nothing to navigate to by name) | start | `reset` |
| Open a new order (File ▸ New order, Ctrl/Cmd+N, toolbar New order) | act | `openNewOrder` |
| Open a new order for a recent customer (toolbar ▾, one of the 4 customers with the newest orders) | act | `openNewOrder` |
| The recent customers the toolbar ▾ offers, newest first | check | to write: `click {"role":"button","name":"New order for a recent customer"}`, then the items as `pick` lists them (a `read` of the open popup was not tried) |
| Close the selected tab (File ▸ Close tab, Ctrl/Cmd+W, the tab's X) | act | to write: `menu {"path":["File","Close tab"]}` (the tab's X has no node; the menu covers it) |
| Closing a new order with something in its cart asks "Discard this new order?" (Discard, Cancel) | act | to write: `menu {"path":["File","Close tab"]}`, then `answer {"button":"Discard"}` or `{"button":"Cancel"}` |
| Close tab is disabled on the Orders tab, which never closes | check | to write: `describe_ui {"fields":["menuPaths"]}` (`File > Close tab [disabled]`) |
| Switch to an open tab (click it) | act | to write: `click {"role":"tab","name":"<title>"}`; two new orders for one customer share a title: the step names which (the newest) or fails naming both |
| Which tabs are open and which one is selected | check | to write: `describe_ui {"fields":["activeTab"]}`; one tab: `read {"role":"tab","name":"<title>"}` (state `selected`) |
| Order ▸ Checkout… and Order ▸ Remove from cart are enabled only when the selected new order can check out / has a selected cart line | check | to write: `describe_ui {"fields":["menuPaths"]}` (`[disabled]` marks) |
| The status line: "Ready", "Order ORD-1640 placed", "Order ORD-1635 cancelled", "2 orders cancelled", "Copied ORD-1633", "Receipt saved to <path>" | check | to write: R1 (the message label has no name; only its own text finds it) |
| The open orders count ("4 open orders", "1 open order") | check | to write: `read {"window":"main","role":"label","name":"open order","match":"contains"}` |
| Exit asks "Discard the open new orders?" while a new order has something in its cart (File ▸ Exit, closing the window, Cmd+Q) | check | to write: `menu {"path":["File","Exit"]}` (its `pending` carries the question; the item is listed by `read {"form":"menuItems","path":["File"]}`, not driven live) |
| Keep working after all: Cancel on that question leaves the app and its tabs as they were | act | to write: `menu {"path":["File","Exit"]}`, then `answer {"button":"Cancel"}` |
| Exit the app (Discard and exit; File ▸ Exit with no unsaved new order) | act | Gap: not a tester's concern (it ends the app under test: no run continues past it) |
| Switch the theme (View ▸ Theme ▸ Light, Dark, Lagoon) | act | Gap: not a tester's concern |
| About Order Desk (Help ▸ About Order Desk, a message, OK) | act | Gap: not a tester's concern |
| The splash while the app starts | check | Gap: not a tester's concern |

## Orders list

The pinned Orders tab (`OrdersPanel`). Its table is the only table on the tab: `{"scope":["tab:Orders"],"role":"table"}`
(found only while the Orders tab is selected).

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Open a placed order (double click its row, Enter on it, or its row menu ▸ Open; an order already open is selected again, not opened twice) | act | `openOrder` |
| Open several selected orders at once (row menu ▸ Open, Enter) | act | to write: `click` rows with `"modifiers":["Shift"]` (or Cmd/Ctrl), then `context_menu ... "path":["Open"]` |
| Search orders by number or customer (Search; its X clears it) | act | to write: `type {"scope":["tab:Orders"],"role":"text","name":"Search"}`; clear: `click {"scope":["tab:Orders","text:Search"],"id":"clear"}` |
| Filter by status (Status: All, Placed, Shipped, Cancelled) | act | to write: `select {"scope":["tab:Orders"],"role":"combo","name":"Status"},"item":"<status>"` |
| Sort by a column (click its header; again reverses; Order, newest first, at start) | act | to write: `click {"scope":["tab:Orders"],"role":"table","column":"<header>"}` |
| An order's Date, Customer, Items, Total or Status (one check for any order and column) | check | to write: `read {"scope":["tab:Orders"],"role":"table","row":{"Order":"<order>"},"column":"<header>"}` (Status reads "Placed", "Shipped", "Cancelled") |
| Which orders are listed, in which order (after a search, filter or sort) | check | to write: `read {"scope":["tab:Orders"],"role":"table"}` (`rows`, `rowCount`) |
| Nothing listed: "No orders match ‘zzz’", "No orders", "No cancelled orders" | check | to write: R2 (the empty-state label's text has no fixed part) |
| Select one or several orders | act | to write: `click` on `{"scope":["tab:Orders"],"role":"table","row":{"Order":"<order>"}}`, more with `"modifiers":["Shift"]`; check `expect {"selected":[…]}` |
| Cancel orders (row menu ▸ Cancel orders…, then "Cancel order ORD-1635?" or "Cancel 2 orders?" with Cancel order(s) / Keep) | act | to write: `context_menu {"scope":["tab:Orders"],"role":"table","row":{"Order":"<order>"}},"path":["Cancel orders…"]`, then `answer {"button":"Cancel order"}` (`"Cancel orders"` for several, `"Keep"`) |
| Cancel orders… is disabled unless every selected order is Placed; "Only placed orders can be cancelled" shows beside the filter | check | to write: `expect {"target":{"scope":["tab:Orders"],"role":"label","name":"Only placed orders can be cancelled"},"shown":true}` (fixed text) |
| Copy order numbers (row menu ▸ Copy order number; the status line says "Copied ORD-1633, ORD-1634") | act | to write: `context_menu ... "path":["Copy order number"]`; the status line: R1 |
| The clipboard holds the copied numbers | check | Gap: the OS owns it |

## New order

A new order's tab (`NewOrderPanel`, `CustomerField`, `ProductsPanel`, `CartPanel`, `QuantityEditor`): scope
`["tab:New order*"]`. It holds two unnamed tables: the products (`SKU` column) and the cart (`Line total` column);
both have a `Product` column, so a row locator on either must also name a column only it has.

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Pick the customer: type a name or city, Enter takes the highlighted suggestion (Down/Up move it, a click picks one, Esc hides them) | act | `openNewOrder` |
| A tester's partial name that fits two customers fails naming both, never takes the first by chance (live: "am" fits Café Lumen in Amsterdam and Fjord Supplies in Hamburg; the app matches anywhere in the name or the city). Note: the example "Ada" fits only Ada Shop Ltd: Ada Lindqvist is the contact on its delivery address, not a customer | act | to write (Task 7, in `openNewOrder`): `read {"window":"popup","role":"list"}` after typing (items read "Café Lumen Amsterdam · Card only") |
| A name that fits no customer: "No customer matches" | check | to write: `expect {"target":{"window":"popup","role":"label","name":"No customer matches"},"shown":true}` |
| The suggestions for what was typed (name, then "Berlin · Account" or "… · Card only") | check | to write: `read {"window":"popup","role":"list"}` |
| Editing the Customer text forgets the chosen customer (tab back to "New order", Checkout… blocked) | act | to write: `type {"scope":["tab:New order*"],"role":"text","name":"Customer"}` |
| The tab's title: "New order", then "New order · Ada Shop Ltd" | check | to write: `describe_ui {"fields":["activeTab"]}` or `read {"role":"tab","name":"New order · <customer>"}` |
| The customer's terms beside the field ("Account · Invoice 30 days · Credit limit €2,000.00 · Owes €450.00 · PO number required", "Card only") | check | to write: R3 (the terms card has no name) |
| Browse a category (Categories: All products, Office ▸ Paper, Pens, Desk, Coffee ▸ Beans, Machines, Cleaning) | act | to write: `click {"scope":["tab:New order*"],"role":"tree","item":["Office","Desk"]}`; selected one: `read` of the tree (`selected`) |
| Search products by SKU or name (Search; its X clears it) | act | to write: `type {"scope":["tab:New order*"],"role":"text","name":"Search"}`; clear: `click {"scope":["tab:New order*","text:Search"],"id":"clear"}` |
| The products listed, with SKU, Product, Price and Stock ("Out of stock" at 0, the row greyed) | check | to write: `read {"scope":["tab:New order*"],"role":"table","column":"SKU"}`; one cell: `"row":{"Product":"<product>"},"column":"Stock"` |
| Low stock: a warning icon in Stock at 5 or fewer | check | to write: R5 (the icon is only painted; nothing reads it) |
| How many products show and how many are selected ("22 products · 2 selected") | check | to write: R4 (no name; `contains "products"` also matches the cart's empty text) |
| Nothing matches: "No products match ‘zzz’" | check | to write: `read {"scope":["tab:New order*"],"role":"label","name":"No products match","match":"contains"}` |
| Add a product to the cart (select it, Add to cart; double click it; its row menu ▸ Add to cart; again adds 1 to its line) | act | `addToCart` |
| Add a product with a quantity | act | `addQtyToCart` |
| Add several selected products at once (out-of-stock ones are left out) | act | to write: `click` product rows (`"column":"SKU"`) with `"modifiers":["Shift"]`, then `click {"scope":["tab:New order*"],"role":"button","name":"Add to cart"}` |
| Add to cart is disabled, with its reason: "Select a product first", "The selected products are out of stock" | check | to write: button state via `read {"scope":["tab:New order*"],"role":"button","name":"Add to cart"}`; the reason: R4 |
| Show a product in its category (its row menu ▸ Show in category) | act | to write: `context_menu {"scope":["tab:New order*"],"role":"table","row":{"Product":"<product>"},"column":"SKU"},"path":["Show in category"]` |
| Change a cart line's quantity (its Qty cell: digits and Enter, or the spinner's arrows) | act | `setQuantity` |
| A quantity below 1 or not a whole number is ignored: the line keeps its quantity | check | to write: `read {"scope":["tab:New order*"],"role":"table","row":{"Product":"<product>"},"column":"Qty"}` after `setQuantity` |
| Remove a line (select it, Remove; its row menu ▸ Remove; Delete or Backspace in the cart; Order ▸ Remove from cart) | act | to write: `click {"scope":["tab:New order*"],"role":"table","row":{"Product":"<product>"},"column":"Line total"}`, then `click {"scope":["tab:New order*"],"role":"button","name":"Remove"}` |
| Removing several lines asks "Remove 2 lines from the cart?" (Remove lines, Keep) | act | to write: as above with `"modifiers":["Shift"]`, then `answer {"button":"Remove lines"}` or `{"button":"Keep"}` |
| Remove is disabled with "Select a cart line first" while lines are in the cart and none is selected | check | to write: `expect {"target":{"scope":["tab:New order*"],"role":"label","name":"Select a cart line first"},"shown":true}` |
| A cart line's quantity, price and line total (one check for any line and column) | check | to write: `read {"scope":["tab:New order*"],"role":"table","row":{"Product":"<product>"},"column":"Line total"}` (or `Qty`, `Price`) |
| The cart's lines, in the order they were added | check | to write: `read {"scope":["tab:New order*"],"role":"table","column":"Line total"}` (`rows`) |
| An empty cart says "Select products and press Add to cart." | check | to write: `expect {"target":{"scope":["tab:New order*"],"role":"label","name":"Select products and press Add to cart."},"shown":true}` |
| A line over stock: "Only 4 Staplers in stock" under the cart (one per line, joined by "; "), its Qty outlined red | check | to write: `read {"scope":["tab:New order*"],"role":"label","name":"in stock","match":"contains"}` (the red outline is painted; the message names the line) |
| The cart's Subtotal, Volume discount (5% off a line of 10 or more; shown only then), Shipping ("chosen at checkout") and VAT (20%) | check | to write: `read {"scope":["tab:New order*"],"role":"label","name":"Subtotal"}` (`value`; also `Volume discount`, `Shipping`, `VAT (20%)`) |
| The cart total | check | `cartTotalIs` |
| Checkout… is disabled, with its reason: "Choose a customer first", "The cart is empty", "Fix quantities above stock" | check | to write: button state via `read {"scope":["tab:New order*"],"role":"button","name":"Checkout…"}`; the reason: R6 |
| Check out (Checkout…, Order ▸ Checkout…) | act | `checkoutWithPo` (PO number customers only; the other ways to pay are in Checkout below) |

## Checkout

The modal "Checkout — <customer>" dialog (`CheckoutPanel`, `AddressStep`, `ShippingStep`, `PaymentStep`,
`ReviewStep`): `"window":"dialog"`. Only the showing step's fields are in the tree.

### The whole wizard

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The dialog's title names the customer ("Checkout — Ada Shop Ltd") | check | to write: `read {"window":"dialog:Checkout — <customer>","role":"button","name":"Cancel"}` found (or `pending.title` of the Checkout… click) |
| Which step shows (1 Address · 2 Shipping · 3 Payment · 4 Review; done steps ticked, the current one bold) | check | to write: R7 (the indicator's state is only painted; the Payment heading has the same text as its indicator label) |
| Go back or on (Back, Next or Enter) | act | to write: `click {"window":"dialog","role":"button","name":"Next"}` (`Back`) |
| Leave the checkout (Cancel, Esc, closing the window): no question, the new order stays as it was | act | to write: `click {"window":"dialog","role":"button","name":"Cancel"}` |
| Next is disabled, with its reason: "Fix the fields marked in red", the first error ("Postal code is required"), "Enter the PO number", "Complete the card details" | check | to write: Next's state via `read {"window":"dialog","role":"button","name":"Next"}`; the reason: R7 |

### Address step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The delivery address, filled in from the customer: Name, Street, Postal code, City, Country, Phone (optional) | check | to write: `read {"window":"dialog","role":"text","name":"Street"}` (`value`; `combo` `Country`) |
| Change the delivery address (any field; Country: Germany, Austria, Netherlands, France) | act | to write: `type {"window":"dialog","role":"text","name":"<caption>"}`; `select {"window":"dialog","role":"combo","name":"Country"},"item":"<country>"` |
| A field's error under it ("Street is required", "Germany uses a 5-digit postal code", "The Netherlands use the form 1234 AB", "Only +, digits and spaces, 6–20 characters"), once the field was left, or at once when the customer's own address is incomplete (Elm Street Studio has no postal code) | check | to write: R8 (the error labels have no name and no tie to their field) |

### Shipping step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Choose Standard or Express | act | to write: `click {"window":"dialog","role":"radio","name":"Express · next business day"}` (`Standard · 3–5 business days`) |
| The prices: Standard €4.90, free from €100.00 after the discount; Express €14.90 | check | to write: R9 (the price labels have no name) |
| Express is disabled outside Germany and Austria: "Express delivers to Germany and Austria only" | check | to write: `read {"window":"dialog","role":"radio","name":"Express · next business day"}` (state `disabled`); `expect {"target":{"window":"dialog","role":"label","name":"Express delivers to Germany and Austria only"},"shown":true}` |
| Write a delivery note | act | to write: `type {"window":"dialog","role":"text","name":"Delivery note (optional)"}` |

### Payment step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Pay on invoice with a PO number (customers that require one: "PO number") | act | `checkoutWithPo` |
| Pay on invoice, PO number optional ("PO number (optional)": Birch & Co, Elm Street Studio) | act | to write: `type {"window":"dialog","role":"text","name":"PO number (optional)"}` (`checkoutWithPo` types into "PO number" only) |
| Pay by card: Card, then Card number (shown in groups of four), Expiry (MM/YY), Name on card | act | to write: `click {"window":"dialog","role":"radio","name":"Card"}`; `type {"window":"dialog","role":"text","name":"Card number"}` (`Expiry`, `Name on card`) |
| A card-only customer: Invoice is disabled, "Café Lumen pays by card", no PO number field | check | to write: `read {"window":"dialog","role":"radio","name":"Invoice (30 days)"}` (state `disabled`); the note: R9 |
| The invoice note: "Credit limit €2,000.00 · owes €450.00" | check | to write: R9 (the note label has no name) |
| A field's error: "PO number is required", "Enter the 16 digits of the card", "Use MM/YY", "Month must be 01–12", "This card has expired", "Name on card is required" | check | to write: R8 |
| Next on an invoice over the credit limit warns ("This order takes Birch & Co to €1,036.60 of its €1,000.00 credit limit." / "Owes €940.00 now, this order adds €96.60."): Pay by card instead (Card gets chosen) or Continue with invoice | act | to write: `click {"window":"dialog","role":"button","name":"Next"}` (its `pending` carries the message), then `answer {"button":"Pay by card instead"}` or `{"button":"Continue with invoice"}` |

### Review step and placing

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The Delivery address, Shipping ("Standard · 3–5 business days · €4.90", "Note: …") and Payment ("Invoice (30 days) · PO PO-2026-131", "Card ending 4242") sections | check | to write: `read {"window":"dialog","role":"section","name":"Delivery address"}` (its label children); `read {"window":"dialog","scope":["section:Shipping"],"role":"label"}` (`section:Payment`; the Shipping section has a second label with the note) |
| Edit a section (its Edit goes back to that step) | act | to write: `click {"window":"dialog","scope":["section:Payment"],"role":"button","name":"Edit"}` |
| The order's lines: Product, Qty, Price, Line total | check | to write: `read {"window":"dialog","role":"table"}` (a cell: `"row":{"Product":"<product>"},"column":"Line total"`) |
| The order's Subtotal, Volume discount, Shipping, VAT (20%) and Total | check | to write: `read {"window":"dialog","role":"label","name":"Total"}` (`Subtotal`, `Volume discount`, `VAT (20%)`); Shipping: R10 (`label 'Shipping'` also matches the step indicator) |
| Place the order and confirm it ("Place order for €122.34?" / "Order for Ada Shop Ltd, 11 items.") | act | `placeOrder` |
| Back out at the confirmation (Cancel): still on Review | act | to write: `click {"window":"dialog","role":"button","name":"Place order"}`, `answer {"button":"Cancel"}` |
| The confirmation's total and item count | check | to write: the Place order click's `pending.message` |
| Placing: "Placing order…", "Order for €122.34 · Ada Shop Ltd", every button disabled, about 2 s | check | to write: `expect {"target":{"window":"dialog","role":"label","name":"Placing order…"},"shown":true}` |
| Placing fails: "The order could not be placed." (OK; back on Review) | check | Gap: needs app internals even after an app change (provoking it needs stock to change inside the modal 2 s placing window) |
| Once placed: the new order's tab takes the new order's place, titled with its number; the status line says "Order ORD-1640 placed" | check | `placeOrder` (returns the number) |

## Order tab

An order's own tab (`OrderPanel`, `StatusPill`): scope `["tab:<order>"]`; its Items, Delivery and Invoice sub-tabs
add `tab:Items` and so on.

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The order's number (also the tab's title), its status (Placed, Shipped, Cancelled) and facts ("Ada Shop Ltd · 12 Oct 2026 · €122.34") | check | to write: number: `read {"role":"tab","name":"<order>"}`; status and facts: R11 (no names; only their own text finds them) |
| Switch between Items, Delivery and Invoice | act | to write: `click {"scope":["tab:<order>"],"role":"tab","name":"Delivery"}` |
| Items: SKU, Product, Qty, Price, Line total | check | to write: `read {"scope":["tab:<order>","tab:Items"],"role":"table"}` (a cell: `"row":{"Product":"<product>"},"column":"Qty"`) |
| "11 units in 2 lines" under the items | check | to write: R11 |
| Delivery: the address, the shipping method ("Standard · 3–5 business days · €4.90"), the delivery note ("—" when none) | check | to write: R11 (the captions are not tied to their values) |
| Invoice: the payment ("Invoice (30 days) · PO PO-T3"), the order date | check | to write: R11 |
| Invoice: Subtotal, Volume discount, Shipping, VAT (20%), Total | check | to write: `read {"scope":["tab:<order>","tab:Invoice"],"role":"label","name":"Total"}` (`value`) |
| Save the receipt (Save receipt…, a file chooser "Save receipt", ".txt" added when missing; the status line says "Receipt saved to <path>") | act | to write: `click {"scope":["tab:<order>"],"role":"button","name":"Save receipt…"}`, then `choose_file {"path":"<file>"}`; the status line: R1 |
| Saving over a file asks "ORD-1640.txt already exists." / "Do you want to replace it?" (Replace, Cancel) | act | to write: as above, then `answer {"button":"Replace"}` or `{"button":"Cancel"}` |
| Saving fails: "The receipt could not be saved: …" | check | to write: the `choose_file` result's `pending` (not provoked live: needs a path the app cannot write) |
| The saved receipt's text | check | Gap: not a tester's concern (every fact in the file shows on the order tab: number, date, status and customer in its header, the lines on Items, address, shipping method and note on Delivery, payment and totals on Invoice; covered there. The file only words some differently: a title line "Order Desk — receipt", free shipping as "€0.00" where the tab says "Free", the method without its price, no SKU, no Note line when there is none) |
| Cancel the order (Cancel order, then "Cancel order ORD-1640?" with Cancel order / Keep; the status turns Cancelled, the status line says "Order ORD-1640 cancelled") | act | to write: `click {"scope":["tab:<order>"],"role":"button","name":"Cancel order"}`, `answer {"button":"Cancel order"}` (`"Keep"`) |
| Cancel order is disabled for a shipped or cancelled order: "Only placed orders can be cancelled — this one is shipped" | check | to write: `read {"scope":["tab:<order>"],"role":"button","name":"Cancel order"}` (state `disabled`); `read {"scope":["tab:<order>"],"role":"label","name":"Only placed orders can be cancelled","match":"contains"}` |

## Notes for the steps (found while surveying)

- `openNewOrder` types the name and presses Enter, which takes the highlighted (first, by name) suggestion: "am"
  opens an order for Café Lumen though Fjord Supplies fits too. See the row above (Task 7).
- `openNewOrder`'s `choices` read the Customer column of the Orders list: every customer with an order (6), not
  the toolbar ▾'s 4 recent ones, and they fail while another tab is selected.
- `checkoutWithPo` presses Next on the Address step without looking: for a customer whose address is incomplete
  (Elm Street Studio) Next is disabled. It types into "PO number", which exists only for customers that require a
  PO number; for Birch & Co and Elm Street Studio the field is "PO number (optional)", and card-only customers
  have none.
- `placeOrder` finds the status line by `contains "placed"`; once R1 is made it can read it by id.

## Reach fixes needed

| Component | What is missing | Fix | Invisible or visible |
|---|---|---|---|
| R1 `StatusBar` message | no name; its text changes with every message | `setName("statusMessage")` | invisible |
| R2 `OrdersPanel` empty-state label | no name; "No orders match ‘…’", "No orders", "No <status> orders" share no fixed text | `setName("ordersEmpty")` | invisible |
| R3 `NewOrderPanel` terms card | no name, no caption | `setName("customerTerms")` | invisible |
| R4 `ProductsPanel` count and Add to cart reason | no names; the count's text changes, `contains "products"` also matches the cart's empty text; the reason has two texts | `setName("productCount")`, `setName("addToCartReason")` | invisible |
| R5 `StockRenderer` low-stock warning | an icon only: no text, no tooltip | a tooltip on the Stock cell, e.g. "Low stock: 4 left" | visible (needs the person's OK) |
| R6 `CartPanel` Checkout… reason | no name; three texts | `setName("checkoutReason")` | invisible |
| R7 `CheckoutPanel` Next's reason; `StepIndicator` current step | the reason has no name and many texts; the current step is shown only by a painted icon and a bold font, and the Payment step's heading reads like its indicator label | `setName("nextReason")`; `setName("checkoutStep")` on each step's heading (Delivery address, Shipping method, Payment), the Review step known by its Place order button | invisible |
| R8 `AddressStep`, `PaymentStep` field errors | the red text under each field has no name and no tie to its field | `setName("<field>Error")` per field: `nameError`, `streetError`, `postalCodeError`, `cityError`, `phoneError`, `poNumberError`, `cardNumberError`, `expiryError`, `nameOnCardError` | invisible |
| R9 `ShippingStep` prices; `PaymentStep` invoice note | no names | `setName("standardPrice")`, `setName("expressPrice")`, `setName("invoiceNote")` | invisible |
| R10 `TotalsPanel` values (Review step) | `label 'Shipping'` in the checkout dialog matches the total's value and the step indicator's Shipping | `setName` on each value: `subtotal`, `volumeDiscount`, `shipping`, `vat`, `total` | invisible |
| R11 `OrderPanel` status, facts, units, Delivery and Invoice values | no names; the Delivery and Invoice captions (`Ui.hint`) are not tied to their values | `setName("orderStatus")`, `setName("orderFacts")`, `setName("itemsSummary")`; `setLabelFor` from each caption to its value (`setName("deliveryAddress")` on the address column) | invisible |

## Changes made to the app

| Change | For row | Visible | Person's OK |
|---|---|---|---|
