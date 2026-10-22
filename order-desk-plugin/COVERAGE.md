# Order Desk plugin: coverage

Surveyed at: 1be3fe5 (2026-10-22)

Reach checked on the live app (agent jar 0.1.0-SNAPSHOT, `-Dorderdesk.today=2026-10-12`) with scoped `read`,
`expect`, `describe_ui` and scoped `snapshot` calls. Locators are written as the CLI takes them. `tab:New order*`
is any new order's tab; `<order>` is an order number such as `ORD-1640`; `"window":"dialog"` is the checkout dialog
or the message showing. The reach fixes R1-R11 (below) are made (see "Changes made to the app"); a label whose text
is empty (no reason, no error shown) is not in the tree, so its read is NOT_FOUND: check that with `expect … "absent"`.

## Main window (menus, toolbar, status bar)

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The app starts on the Orders list, no other tab open (no login, nothing to navigate to by name) | start | `reset` |
| Open a new order (File ▸ New order, Ctrl/Cmd+N, toolbar New order) | act | `openNewOrder` |
| Open a new order for a recent customer (toolbar ▾, one of the 4 customers with the newest orders) | act | `openNewOrder` |
| The recent customers the toolbar ▾ offers, newest first | check | `recentCustomersAre` |
| Close the selected tab (File ▸ Close tab, Ctrl/Cmd+W, the tab's X) | act | to write: `menu {"path":["File","Close tab"]}` (the tab's X has no node; the menu covers it) |
| Closing a new order with something in its cart asks "Discard this new order?" (Discard, Cancel) | act | to write: `menu {"path":["File","Close tab"]}`, then `answer {"button":"Discard"}` or `{"button":"Cancel"}` |
| Close tab is disabled on the Orders tab, which never closes | check | to write: `describe_ui {"fields":["menuPaths"]}` (`File > Close tab [disabled]`) |
| Switch to an open tab (click it) | act | to write: `click {"role":"tab","name":"<title>"}`; two new orders for one customer share a title: the step names which (the newest) or fails naming both |
| Which tabs are open and which one is selected | check | to write: `describe_ui {"fields":["activeTab"]}`; one tab: `read {"role":"tab","name":"<title>"}` (state `selected`) |
| Order ▸ Checkout… and Order ▸ Remove from cart are enabled only when the selected new order can check out / has a selected cart line | check | to write: `describe_ui {"fields":["menuPaths"]}` (`[disabled]` marks) |
| The status line: "Ready", "Order ORD-1640 placed", "Order ORD-1635 cancelled", "2 orders cancelled", "Copied ORD-1633", "Receipt saved to <path>" | check | to write: `read {"window":"main","role":"label","id":"statusMessage"}` |
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
| Nothing listed: "No orders match ‘zzz’", "No orders", "No cancelled orders" | check | to write: `read {"scope":["tab:Orders"],"role":"label","id":"ordersEmpty"}` |
| Select one or several orders | act | to write: `click` on `{"scope":["tab:Orders"],"role":"table","row":{"Order":"<order>"}}`, more with `"modifiers":["Shift"]`; check `expect {"selected":[…]}` |
| Cancel orders (row menu ▸ Cancel orders…, then "Cancel order ORD-1635?" or "Cancel 2 orders?" with Cancel order(s) / Keep) | act | to write: `context_menu {"scope":["tab:Orders"],"role":"table","row":{"Order":"<order>"}},"path":["Cancel orders…"]`, then `answer {"button":"Cancel order"}` (`"Cancel orders"` for several, `"Keep"`) |
| Cancel orders… is disabled unless every selected order is Placed; "Only placed orders can be cancelled" shows beside the filter | check | to write: `expect {"target":{"scope":["tab:Orders"],"role":"label","name":"Only placed orders can be cancelled"},"shown":true}` (fixed text) |
| Copy order numbers (row menu ▸ Copy order number; the status line says "Copied ORD-1633, ORD-1634") | act | to write: `context_menu ... "path":["Copy order number"]`; the status line: `read {"window":"main","role":"label","id":"statusMessage"}` |
| The clipboard holds the copied numbers | check | Gap: the OS owns it |

## New order

A new order's tab (`NewOrderPanel`, `CustomerField`, `ProductsPanel`, `CartPanel`, `QuantityEditor`): scope
`["tab:New order*"]`. It holds two unnamed tables: the products (`SKU` column) and the cart (`Line total` column);
both have a `Product` column, so a row locator on either must also name a column only it has.

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Pick the customer: type a name or city, Enter takes the highlighted suggestion (Down/Up move it, a click picks one, Esc hides them) | act | `openNewOrder`, `chooseCustomer` (the suggestion whose name starts with what was typed, or the only one) |
| A tester's partial name that fits two customers fails naming both, never takes the first by chance (live: "am" fits Café Lumen in Amsterdam and Fjord Supplies in Hamburg; the app matches anywhere in the name or the city). Note: the example "Ada" fits only Ada Shop Ltd: Ada Lindqvist is the contact on its delivery address, not a customer | act | `openNewOrder`, `chooseCustomer` (fail as AMBIGUOUS naming the suggestions that fit; no fit: NOT_FOUND naming every customer the field offers) |
| A name that fits no customer: "No customer matches" | check | `typeCustomer`, then `customerSuggestionsAre` with `none` |
| The suggestions for what was typed (name, then "Berlin · Account" or "… · Card only") | check | `customerSuggestionsAre` |
| Editing the Customer text forgets the chosen customer (tab back to "New order", Checkout… blocked) | act | `typeCustomer` (then `newOrderIsFor` `none`, `buttonIsDisabled` Checkout…) |
| The tab's title: "New order", then "New order · Ada Shop Ltd" | check | `newOrderIsFor` |
| The customer's terms beside the field ("Account · Invoice 30 days · Credit limit €2,000.00 · Owes €450.00 · PO number required", "Card only") | check | `customerTermsAre` |
| Browse a category (Categories: All products, Office ▸ Paper, Pens, Desk, Coffee ▸ Beans, Machines, Cleaning) | act | `browseCategory`, `selectedCategoryIs` (app bug, see Notes: a category chosen while a product further down the list is selected) |
| Search products by SKU or name (Search; its X clears it) | act | `searchProducts`, `clearProductSearch` |
| The products listed, with SKU, Product, Price and Stock ("Out of stock" at 0, the row greyed) | check | `productsListedAre`, `productIs` |
| Low stock: a warning icon in Stock at 5 or fewer, its tooltip "Low stock: 4 left" | check | `lowStockWarningIs` |
| How many products show and how many are selected ("22 products · 2 selected") | check | `productCountIs` |
| Nothing matches: "No products match ‘zzz’" | check | `productsListedAre` with `none` |
| Add a product to the cart (select it, Add to cart; double click it; its row menu ▸ Add to cart; again adds 1 to its line) | act | `addToCart` |
| Add a product with a quantity | act | `addQtyToCart` |
| Add several selected products at once (out-of-stock ones are left out) | act | `addProductsToCart` (the outcome lists the products left out) |
| Add to cart is disabled, with its reason: "Select a product first", "The selected products are out of stock" | check | `buttonIsDisabled`, `buttonIsEnabled`; `addToCart` fails with the reason |
| Show a product in its category (its row menu ▸ Show in category) | act | `showInCategory` (app bug, see Notes: fails unless the product's row number fits the target category) |
| Change a cart line's quantity (its Qty cell: digits and Enter, or the spinner's arrows) | act | `setQuantity` |
| A quantity below 1 or not a whole number is ignored: the line keeps its quantity | check | `setQuantity`, then `cartLineIs` (Qty) |
| Remove a line (select it, Remove; its row menu ▸ Remove; Delete or Backspace in the cart; Order ▸ Remove from cart) | act | `removeFromCart` |
| Removing several lines asks "Remove 2 lines from the cart?" (Remove lines, Keep) | act | `removeFromCart` (Remove lines), `keepCartLines` (Keep) |
| Remove is disabled with "Select a cart line first" while lines are in the cart and none is selected | check | `buttonIsDisabled` |
| A cart line's quantity, price and line total (one check for any line and column) | check | `cartLineIs` |
| The cart's lines, in the order they were added | check | `cartLinesAre` |
| An empty cart says "Select products and press Add to cart." | check | `cartLinesAre` with `none` |
| A line over stock: "Only 4 Staplers in stock" under the cart (one per line, joined by "; "), its Qty outlined red | check | `cartStockWarningIs` |
| The cart's Subtotal, Volume discount (5% off a line of 10 or more; shown only then), Shipping ("chosen at checkout") and VAT (20%) | check | `cartAmountIs` |
| The cart total | check | `cartTotalIs` |
| Checkout… is disabled, with its reason: "Choose a customer first", "The cart is empty", "Fix quantities above stock" | check | `buttonIsDisabled`, `buttonIsEnabled` |
| Check out (Checkout…, Order ▸ Checkout…) | act | `checkoutWithPo` (PO number customers only; the other ways to pay are in Checkout below) |

## Checkout

The modal "Checkout — <customer>" dialog (`CheckoutPanel`, `AddressStep`, `ShippingStep`, `PaymentStep`,
`ReviewStep`): `"window":"dialog"`. Only the showing step's fields are in the tree.

### The whole wizard

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The dialog's title names the customer ("Checkout — Ada Shop Ltd") | check | to write: `read {"window":"dialog:Checkout — <customer>","role":"button","name":"Cancel"}` found (or `pending.title` of the Checkout… click) |
| Which step shows (1 Address · 2 Shipping · 3 Payment · 4 Review; done steps ticked, the current one bold) | check | to write: `read {"window":"dialog","role":"label","id":"checkoutStep"}` ("Delivery address", "Shipping method", "Payment"; NOT_FOUND on Review, known by its Place order button) |
| Go back or on (Back, Next or Enter) | act | to write: `click {"window":"dialog","role":"button","name":"Next"}` (`Back`) |
| Leave the checkout (Cancel, Esc, closing the window): no question, the new order stays as it was | act | to write: `click {"window":"dialog","role":"button","name":"Cancel"}` |
| Next is disabled, with its reason: "Fix the fields marked in red", the first error ("Postal code is required"), "Enter the PO number", "Complete the card details" | check | to write: Next's state via `read {"window":"dialog","role":"button","name":"Next"}`; the reason: `read {"window":"dialog","role":"label","id":"nextReason"}` |

### Address step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The delivery address, filled in from the customer: Name, Street, Postal code, City, Country, Phone (optional) | check | to write: `read {"window":"dialog","role":"text","name":"Street"}` (`value`; `combo` `Country`) |
| Change the delivery address (any field; Country: Germany, Austria, Netherlands, France) | act | to write: `type {"window":"dialog","role":"text","name":"<caption>"}`; `select {"window":"dialog","role":"combo","name":"Country"},"item":"<country>"` |
| A field's error under it ("Street is required", "Germany uses a 5-digit postal code", "The Netherlands use the form 1234 AB", "Only +, digits and spaces, 6–20 characters"), once the field was left, or at once when the customer's own address is incomplete (Elm Street Studio has no postal code) | check | to write: `read {"window":"dialog","role":"label","id":"streetError"}` (`nameError`, `postalCodeError`, `cityError`, `phoneError`) |

### Shipping step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Choose Standard or Express | act | to write: `click {"window":"dialog","role":"radio","name":"Express · next business day"}` (`Standard · 3–5 business days`) |
| The prices: Standard €4.90, free from €100.00 after the discount; Express €14.90 | check | to write: `read {"window":"dialog","role":"label","id":"standardPrice"}` (`expressPrice`) |
| Express is disabled outside Germany and Austria: "Express delivers to Germany and Austria only" | check | to write: `read {"window":"dialog","role":"radio","name":"Express · next business day"}` (state `disabled`); `expect {"target":{"window":"dialog","role":"label","name":"Express delivers to Germany and Austria only"},"shown":true}` |
| Write a delivery note | act | to write: `type {"window":"dialog","role":"text","name":"Delivery note (optional)"}` |

### Payment step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Pay on invoice with a PO number (customers that require one: "PO number") | act | `checkoutWithPo` |
| Pay on invoice, PO number optional ("PO number (optional)": Birch & Co, Elm Street Studio) | act | to write: `type {"window":"dialog","role":"text","name":"PO number (optional)"}` (`checkoutWithPo` types into "PO number" only) |
| Pay by card: Card, then Card number (shown in groups of four), Expiry (MM/YY), Name on card | act | to write: `click {"window":"dialog","role":"radio","name":"Card"}`; `type {"window":"dialog","role":"text","name":"Card number"}` (`Expiry`, `Name on card`) |
| A card-only customer: Invoice is disabled, "Café Lumen pays by card", no PO number field | check | to write: `read {"window":"dialog","role":"radio","name":"Invoice (30 days)"}` (state `disabled`); the note: `read {"window":"dialog","role":"label","id":"invoiceNote"}` |
| The invoice note: "Credit limit €2,000.00 · owes €450.00" | check | to write: `read {"window":"dialog","role":"label","id":"invoiceNote"}` |
| A field's error: "PO number is required", "Enter the 16 digits of the card", "Use MM/YY", "Month must be 01–12", "This card has expired", "Name on card is required" | check | to write: `read {"window":"dialog","role":"label","id":"cardNumberError"}` (`poNumberError`, `expiryError`, `nameOnCardError`) |
| Next on an invoice over the credit limit warns ("This order takes Birch & Co to €1,036.60 of its €1,000.00 credit limit." / "Owes €940.00 now, this order adds €96.60."): Pay by card instead (Card gets chosen) or Continue with invoice | act | to write: `click {"window":"dialog","role":"button","name":"Next"}` (its `pending` carries the message), then `answer {"button":"Pay by card instead"}` or `{"button":"Continue with invoice"}` |

### Review step and placing

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The Delivery address, Shipping ("Standard · 3–5 business days · €4.90", "Note: …") and Payment ("Invoice (30 days) · PO PO-2026-131", "Card ending 4242") sections | check | to write: `read {"window":"dialog","role":"section","name":"Delivery address"}` (its label children); `read {"window":"dialog","scope":["section:Shipping"],"role":"label"}` (`section:Payment`; the Shipping section has a second label with the note) |
| Edit a section (its Edit goes back to that step) | act | to write: `click {"window":"dialog","scope":["section:Payment"],"role":"button","name":"Edit"}` |
| The order's lines: Product, Qty, Price, Line total | check | to write: `read {"window":"dialog","role":"table"}` (a cell: `"row":{"Product":"<product>"},"column":"Line total"`) |
| The order's Subtotal, Volume discount, Shipping, VAT (20%) and Total | check | to write: `read {"window":"dialog","role":"label","name":"Total"}` (`Subtotal`, `Volume discount`, `VAT (20%)`); Shipping: `read {"window":"dialog","scope":["id:totals"],"role":"label","name":"Shipping"}` (unscoped, `label 'Shipping'` also matches the step indicator) |
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
| The order's number (also the tab's title), its status (Placed, Shipped, Cancelled) and facts ("Ada Shop Ltd · 12 Oct 2026 · €122.34") | check | to write: number: `read {"role":"tab","name":"<order>"}`; status and facts: `read {"scope":["tab:<order>"],"role":"label","id":"orderStatus"}` (`orderFacts`) |
| Switch between Items, Delivery and Invoice | act | to write: `click {"scope":["tab:<order>"],"role":"tab","name":"Delivery"}` |
| Items: SKU, Product, Qty, Price, Line total | check | to write: `read {"scope":["tab:<order>","tab:Items"],"role":"table"}` (a cell: `"row":{"Product":"<product>"},"column":"Qty"`) |
| "11 units in 2 lines" under the items | check | to write: `read {"scope":["tab:<order>","tab:Items"],"role":"label","id":"itemsSummary"}` |
| Delivery: the address, the shipping method ("Standard · 3–5 business days · €4.90"), the delivery note ("—" when none) | check | to write: `read {"scope":["tab:<order>","tab:Delivery"],"role":"label","name":"Shipping method"}` (`Delivery note`); the address lines: `read {"scope":["tab:<order>","tab:Delivery","id:deliveryAddress"],"role":"label","nth":<0-3>}` |
| Invoice: the payment ("Invoice (30 days) · PO PO-T3"), the order date | check | to write: `read {"scope":["tab:<order>","tab:Invoice"],"role":"label","name":"Payment"}` (`Order date`) |
| Invoice: Subtotal, Volume discount, Shipping, VAT (20%), Total | check | to write: `read {"scope":["tab:<order>","tab:Invoice"],"role":"label","name":"Total"}` (`value`) |
| Save the receipt (Save receipt…, a file chooser "Save receipt", ".txt" added when missing; the status line says "Receipt saved to <path>") | act | to write: `click {"scope":["tab:<order>"],"role":"button","name":"Save receipt…"}`, then `choose_file {"path":"<file>"}`; the status line: `read {"window":"main","role":"label","id":"statusMessage"}` |
| Saving over a file asks "ORD-1640.txt already exists." / "Do you want to replace it?" (Replace, Cancel) | act | to write: as above, then `answer {"button":"Replace"}` or `{"button":"Cancel"}` |
| Saving fails: "The receipt could not be saved: …" | check | to write: the `choose_file` result's `pending` (not provoked live: needs a path the app cannot write) |
| The saved receipt's text | check | Gap: not a tester's concern (every fact in the file shows on the order tab: number, date, status and customer in its header, the lines on Items, address, shipping method and note on Delivery, payment and totals on Invoice; covered there. The file only words some differently: a title line "Order Desk — receipt", free shipping as "€0.00" where the tab says "Free", the method without its price, no SKU, no Note line when there is none) |
| Cancel the order (Cancel order, then "Cancel order ORD-1640?" with Cancel order / Keep; the status turns Cancelled, the status line says "Order ORD-1640 cancelled") | act | to write: `click {"scope":["tab:<order>"],"role":"button","name":"Cancel order"}`, `answer {"button":"Cancel order"}` (`"Keep"`) |
| Cancel order is disabled for a shipped or cancelled order: "Only placed orders can be cancelled — this one is shipped" | check | to write: `read {"scope":["tab:<order>"],"role":"button","name":"Cancel order"}` (state `disabled`); `read {"scope":["tab:<order>"],"role":"label","name":"Only placed orders can be cancelled","match":"contains"}` |

## Notes for the steps (found while surveying)

- Fixed: `openNewOrder` (and `chooseCustomer`) read the suggestions after typing and take the one whose text starts
  with what was typed, or the only one; "am" fails naming Café Lumen and Fjord Supplies. A name that fits none
  fails naming every customer the field offers (emptied, Down lists them all; Escape closes the list).
- `openNewOrder`'s `choices` still read the Orders list's customers (every customer with an order), or the open
  Customer suggestions: no screen lists every customer without acting. The step itself resolves against the app's
  suggestions, so a customer outside the choices still works.
- A product, a cart line or a category is given by its name or the start of it, any case; several names in one param
  are parted by `;` (product names hold commas); `none` stands for "not shown" in every check.
- A read of a table gives its first 20 rows: the products past them (Cleaning) are read one by one (row index, then
  SKU).
- App bug (found writing the steps, NOT fixed: needs the person's OK): choosing a category, by a click in Categories
  or by Show in category, while the selected product's row number is past the new category's row count (e.g. the
  Espresso machine selected, then Desk; or Show in category on the Stapler from All products) throws
  `ArrayIndexOutOfBoundsException` on the event thread (`ProductsPanel.applyFilter` → `updateAdd` →
  `selectedProducts` → `JTable.convertRowIndexToModel`). `browseCategory` passes when no such product is selected;
  `showInCategory` passes only when the product's row fits its category (it already shows that category).
- `checkoutWithPo` presses Next on the Address step without looking: for a customer whose address is incomplete
  (Elm Street Studio) Next is disabled. It types into "PO number", which exists only for customers that require a
  PO number; for Birch & Co and Elm Street Studio the field is "PO number (optional)", and card-only customers
  have none.
- `placeOrder` finds the status line by `contains "placed"`; since R1 it can read it by id (`statusMessage`).

## Reach fixes needed

| Component | What is missing | Fix | Invisible or visible |
|---|---|---|---|
| R1 `StatusBar` message | no name; its text changes with every message | `setName("statusMessage")` | invisible |
| R2 `OrdersPanel` empty-state label | no name; "No orders match ‘…’", "No orders", "No <status> orders" share no fixed text | `setName("ordersEmpty")` | invisible |
| R3 `NewOrderPanel` terms card | no name, no caption | `setName("customerTerms")` | invisible |
| R4 `ProductsPanel` count and Add to cart reason | no names; the count's text changes, `contains "products"` also matches the cart's empty text; the reason has two texts | `setName("productCount")`, `setName("addToCartReason")` | invisible |
| R5 `StockRenderer` low-stock warning | an icon only: no text, no tooltip | a tooltip on the Stock cell, e.g. "Low stock: 4 left" | visible (user OK 2026-10-22) |
| R6 `CartPanel` Checkout… reason | no name; three texts | `setName("checkoutReason")` | invisible |
| R7 `CheckoutPanel` Next's reason; `StepIndicator` current step | the reason has no name and many texts; the current step is shown only by a painted icon and a bold font, and the Payment step's heading reads like its indicator label | `setName("nextReason")`; `setName("checkoutStep")` on each step's heading (Delivery address, Shipping method, Payment), the Review step known by its Place order button | invisible |
| R8 `AddressStep`, `PaymentStep` field errors | the red text under each field has no name and no tie to its field | `setName("<field>Error")` per field: `nameError`, `streetError`, `postalCodeError`, `cityError`, `phoneError`, `poNumberError`, `cardNumberError`, `expiryError`, `nameOnCardError` | invisible |
| R9 `ShippingStep` prices; `PaymentStep` invoice note | no names | `setName("standardPrice")`, `setName("expressPrice")`, `setName("invoiceNote")` | invisible |
| R10 `TotalsPanel` values (Review step) | `label 'Shipping'` in the checkout dialog matches the total's value and the step indicator's Shipping | first planned: `setName` on each value (`subtotal`, `volumeDiscount`, `shipping`, `vat`, `total`); made instead: `setName("totals")` on the panel (a code name replaces the caption name, so `label 'Total'` would stop finding the value: the 3 bundled recorded tests and `cartTotalIs` read it so) | invisible |
| R11 `OrderPanel` status, facts, units, Delivery and Invoice values | no names; the Delivery and Invoice captions (`Ui.hint`) are not tied to their values | `setName("orderStatus")`, `setName("orderFacts")`, `setName("itemsSummary")`; `setLabelFor` from each caption to its value (`setName("deliveryAddress")` on the address column) | invisible |

## Changes made to the app

| Change | For row | Visible | Person's OK |
|---|---|---|---|
| R1 `StatusBar`: the message label `setName("statusMessage")` | The status line; Copy order number; Save the receipt | invisible | - |
| R2 `OrdersPanel`: the empty-state label `setName("ordersEmpty")` | Nothing listed | invisible | - |
| R3 `NewOrderPanel`: the terms card `setName("customerTerms")` | The customer's terms | invisible | - |
| R4 `ProductsPanel`: `setName("productCount")` on the count, `setName("addToCartReason")` on Add to cart's reason | How many products show; Add to cart is disabled | invisible | - |
| R5 `StockRenderer`: a tooltip on a low-stock Stock cell (5 or fewer, as the icon), "Low stock: 4 left" | Low stock | visible (on hover only) | user OK 2026-10-22 |
| R6 `CartPanel`: Checkout…'s reason `setName("checkoutReason")` | Checkout… is disabled | invisible | - |
| R7 `CheckoutPanel`: Next's reason `setName("nextReason")`; each step's heading (Delivery address, Shipping method, Payment) `setName("checkoutStep")` (`Ui.stepHeading`) | Which step shows; Next is disabled | invisible | - |
| R8 `AddressStep`, `PaymentStep`: each field's error `setName`: `nameError`, `streetError`, `postalCodeError`, `cityError`, `phoneError`, `poNumberError`, `cardNumberError`, `expiryError`, `nameOnCardError` | A field's error (Address, Payment) | invisible | - |
| R9 `ShippingStep`: `setName("standardPrice")`, `setName("expressPrice")`; `PaymentStep`: `setName("invoiceNote")` | The prices; the invoice note; a card-only customer | invisible | - |
| R10 `TotalsPanel`: the panel `setName("totals")` (in the cart, the review and the order's Invoice tab); its values keep no name, so each stays found by its caption | The order's totals (Review) | invisible | - |
| R11 `OrderPanel`: `setName("orderStatus")` on the status pill, `setName("orderFacts")`, `setName("itemsSummary")`; each Delivery and Invoice caption `setLabelFor` its value (Delivery address, Shipping method, Delivery note, Payment, Order date), the address column `setName("deliveryAddress")` | The order's status and facts; units; Delivery; Invoice | invisible | - |
