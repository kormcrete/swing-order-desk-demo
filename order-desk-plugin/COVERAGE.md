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
| Open a new order for a recent customer (toolbar ▾, one of the 4 customers with the newest orders) | act | `openNewOrderFromRecent` (the ▾, then the customer picked from its menu; ends on a new tab, selected; a person's ▾ and pick fold into it; `openNewOrder` reaches any customer through File ▸ New order; its choices are the recent customers as the app takes them from the orders, read from the Orders list) |
| The recent customers the toolbar ▾ offers, newest first | check | `recentCustomersAre` |
| Close the selected tab (File ▸ Close tab, Ctrl/Cmd+W, the tab's X) | act | `closeTab` (File ▸ Close tab; the tab's X has no node, the menu covers it; another tab: `selectTab` first) |
| Closing a new order with something in its cart asks "Discard this new order?" (Discard, Cancel) | act | `closeTab` (Discard), `keepTab` (Cancel) |
| Close tab is disabled on the Orders tab, which never closes | check | `menuItemIs` (Close tab, disabled: opens File, reads, Escape); `closeTab` on the Orders tab fails saying so |
| Switch to an open tab (click it) | act | `selectTab` (title or the start of a word of it; two new orders for one customer share a title: it takes the newest, the rightmost; also an order tab's Items, Delivery, Invoice) |
| Which tabs are open and which one is selected | check | `openTabsAre`, `selectedTabIs` (an order tab's inner tabs left out of the open tabs, counted as selected) |
| Order ▸ Checkout… and Order ▸ Remove from cart are enabled only when the selected new order can check out / has a selected cart line | check | `menuItemIs` |
| The status line: "Ready", "Order ORD-1640 placed", "Order ORD-1635 cancelled", "2 orders cancelled", "Copied ORD-1633", "Receipt saved to <path>" | check | `statusLineIs` (by id `statusMessage`) |
| The open orders count ("4 open orders", "1 open order") | check | `openOrdersCountIs` ("4" or the words; the tester gives the count: it moves as orders are placed and cancelled) |
| Exit asks "Discard the open new orders?" while a new order has something in its cart (File ▸ Exit, closing the window, Cmd+Q) | check | `exitQuestionIs` (chooses File ▸ Exit only while a new order with something in its cart shows, reads, answers Cancel; else fails and chooses nothing) |
| Keep working after all: Cancel on that question leaves the app and its tabs as they were | act | `cancelExit` (same guard: never ends the app) |
| Exit the app (Discard and exit; File ▸ Exit with no unsaved new order) | act | Gap: not a tester's concern (it ends the app under test: no run continues past it) |
| Switch the theme (View ▸ Theme ▸ Light, Dark, Lagoon) | act | `switchTheme` (round 1, S24: a tester asks for it; only the look changes, a screenshot shows it, `showOrders` before and after shows the data stayed). The reset puts Light back after `switchTheme` chose another theme in the app's JVM (recorded tests, the agent's steps); a theme chosen in the menu by hand, or by a Java test's own JVM, stays until switched back: the menu cannot say which is chosen without opening it, and choosing Light on every reset costs 0.6-1 s |
| About Order Desk (Help ▸ About Order Desk, a message, OK): its text | check | `aboutShows` (ruling R23: a check of the text, whole words of it; opens, reads, OK) |
| The splash while the app starts | check | Gap: not a tester's concern |

## Orders list

The pinned Orders tab (`OrdersPanel`). Its table is the only table on the tab: `{"scope":["tab:Orders"],"role":"table"}`
(found only while the Orders tab is selected).

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Open a placed order (double click its row, Enter on it, or its row menu ▸ Open; an order already open is selected again, not opened twice) | act | `openOrder` (number, digits, or a customer with one order listed; waits for its tab) |
| Open several selected orders at once (row menu ▸ Open, Enter) | act | `openOrder` with several parted by `;` (selected with Cmd/Ctrl, then the row menu's Open) |
| Search orders by number or customer (Search; its X clears it) | act | `searchOrders`, `clearOrderSearch` |
| Filter by status (Status: All, Placed, Shipped, Cancelled) | act | `filterOrders` ("placed", "cancel") |
| Sort by a column (click its header; again reverses; Order, newest first, at start) | act | `sortOrders` (clicks the header until the column's values read in the asked direction; outcome: the column and direction left, `either` when every value is the same) |
| An order's Date, Customer, Items, Total or Status (one check for any order and column) | check | `orderIs` (Status any case: "placed"; the Order cell too, as the Check menu offers it) |
| Which orders are listed, in which order (after a search, filter or sort) | check | `ordersListedAre` (past 20 rows read one by one) |
| What the list shows, to tell or to compare (every order's Order, Date, Customer, Items, Total, Status, top to bottom; the empty text instead) | check | the outcome of `searchOrders`, `clearOrderSearch`, `filterOrders`, `sortOrders` (with `"outcome": true`), and `showOrders` (goes to the Orders tab, changes nothing) for a read before and after something else (round 1: S01, S04, S24 read the table raw). `orders` is keyed by number, in the list's order: a recording links a later step to a value at its path, and a place in a list would name another order once the list changes |
| How many orders are listed (after a search or a filter: how many are left) | check | `ordersCountIs` (0 while the empty text shows; an agent's row count of the Orders table folds into it) |
| Nothing listed: "No orders match ‘zzz’", "No orders", "No cancelled orders" | check | `ordersEmptyMessageIs` (by id `ordersEmpty`), `ordersListedAre` with `none` |
| Select one or several orders | act | `selectOrders` (Cmd/Ctrl for the others), `selectedOrdersAre` |
| Cancel orders (row menu ▸ Cancel orders…, then "Cancel order ORD-1635?" or "Cancel 2 orders?" with Cancel order(s) / Keep) | act | `cancelOrders`, `keepOrders` |
| The question's text: "Cancel order ORD-1635?" / "Cancel 2 orders?", then "Stock is restored and invoice totals are taken off what the customer owes." | check | `cancelQuestionIs` (asks, reads, answers Keep; the whole text or one line; then the orders and the tab selected before, selected again, also when it fails) |
| Cancel orders… is disabled unless every selected order is Placed; "Only placed orders can be cancelled" shows beside the filter | check | `menuItemIs` (Cancel orders…, disabled: opens the selected orders' row menu, reads, Escape; checks the hint too); `cancelOrders` fails with that reason before clicking the list |
| Copy order numbers (row menu ▸ Copy order number; the status line says "Copied ORD-1633, ORD-1634") | act | `copyOrderNumbers`, then `statusLineIs` |
| The clipboard holds the copied numbers | check | Gap: the OS owns it |

## New order

A new order's tab (`NewOrderPanel`, `CustomerField`, `ProductsPanel`, `CartPanel`, `QuantityEditor`): scope
`["tab:New order*"]`. It holds two unnamed tables: the products (`SKU` column) and the cart (`Line total` column);
both have a `Product` column, so a row locator on either must also name a column only it has.

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Pick the customer: type a name or city, Enter takes the highlighted suggestion (Down/Up move it, a click picks one, Esc hides them) | act | `openNewOrder`, `chooseCustomer` (the suggestion whose name starts with what was typed, or the only one) |
| A tester's partial name that fits two customers fails naming both, never takes the first by chance (live: "am" fits Café Lumen in Amsterdam and Fjord Supplies in Hamburg; the app matches anywhere in the name or the city). Note: the example "Ada" fits only Ada Shop Ltd: Ada Lindqvist is the contact on its delivery address, not a customer | act | `openNewOrder`, `chooseCustomer` (fail as AMBIGUOUS naming the suggestions that fit; no fit: NOT_FOUND naming every customer the field offers) |
| A name that fits no customer: "No customer matches" | check | `typeCustomer`, then `customerSuggestionsAre` with `none` or the text itself (round 1, S10: the text failed although the step's own words offered it; fixed) |
| The suggestions for what was typed (name, then "Berlin · Account" or "… · Card only") | check | `customerSuggestionsAre` |
| Editing the Customer text forgets the chosen customer (tab back to "New order", Checkout… blocked) | act | `typeCustomer` (then `newOrderIsFor` `none`, `buttonIsDisabled` Checkout…) |
| The tab's title: "New order", then "New order · Ada Shop Ltd" | check | `newOrderIsFor` |
| The customer's terms beside the field ("Account · Invoice 30 days · Credit limit €2,000.00 · Owes €450.00 · PO number required", "Card only") | check | `customerTermsAre` (the whole text or one part between the dots: "Account") |
| Browse a category (Categories: All products, Office ▸ Paper, Pens, Desk, Coffee ▸ Beans, Machines, Cleaning) | act | `browseCategory`, `selectedCategoryIs` |
| Search products by SKU or name (Search; its X clears it) | act | `searchProducts`, `clearProductSearch` |
| The products listed, with SKU, Product, Price and Stock ("Out of stock" at 0, the row greyed) | check | `productsListedAre`, `productIs` (the product by name or SKU) |
| Low stock: a warning icon in Stock at 5 or fewer, its tooltip "Low stock: 4 left" | check | `lowStockWarningIs` |
| How many products show and how many are selected ("22 products · 2 selected") | check | `productCountIs` |
| Nothing matches: "No products match ‘zzz’" | check | `productsListedAre` with `none` |
| Add a product to the cart (select it, Add to cart; double click it; its row menu ▸ Add to cart; again adds 1 to its line) | act | `addToCart` |
| Add a product with a quantity | act | `addQtyToCart` |
| Add several selected products at once (out-of-stock ones are left out) | act | `addProductsToCart` (the outcome lists the products left out) |
| Select products without adding them (a click on a row; Cmd/Ctrl for more): Add to cart then shows whether it can add them | act | `selectProducts` (round 2, S13: the runner clicked the Desk lamp row raw; suggest-only: a click on a product row is also the first half of `addToCart`, and a recording folds a run as soon as it fits) |
| Add to cart is disabled, with its reason: "Select a product first", "The selected products are out of stock" | check | `buttonIsDisabled`, `buttonIsEnabled` (after `selectProducts`); `addToCart` fails with the reason |
| Show a product in its category (its row menu ▸ Show in category) | act | `showInCategory` |
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
| Check out (Checkout…, Order ▸ Checkout…) | act | `startCheckout`, `checkoutWithPo` (see Checkout below) |

## Checkout

The modal "Checkout — <customer>" dialog (`CheckoutPanel`, `AddressStep`, `ShippingStep`, `PaymentStep`,
`ReviewStep`): `"window":"dialog"`. Only the showing step's fields are in the tree. Every checkout step first looks for
the checkout (`dialog:Checkout — *`) and the step it needs, and fails fast naming where the checkout is and the step
that gets there; while the credit-limit warning or Place order's question is open over it, it says "answer it first".

### The whole wizard

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Check out (Checkout…, Order ▸ Checkout…) | act | `startCheckout` (a disabled Checkout… fails with its reason); `checkoutWithPo` (whole checkout on invoice with a PO number) |
| The dialog's title names the customer ("Checkout — Ada Shop Ltd") | check | `checkoutCustomerIs` |
| Which step shows (1 Address · 2 Shipping · 3 Payment · 4 Review; done steps ticked, the current one bold) | check | `checkoutStepIs` (reads the `checkoutStep` heading; Review: no heading, Place order instead) |
| Go back or on (Back, Next or Enter) | act | `checkoutNext` (a disabled Next fails with its reason and the fields' errors), `checkoutBack` |
| Leave the checkout (Cancel, Esc, closing the window): no question, the new order stays as it was | act | `cancelCheckout` |
| Next is disabled, with its reason: "Fix the fields marked in red", the first error ("Postal code is required"), "Enter the PO number", "Complete the card details" | check | `buttonIsDisabled` / `buttonIsEnabled` with Next (reason by id `nextReason`) |

### Address step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The delivery address, filled in from the customer: Name, Street, Postal code, City, Country, Phone (optional) | check | `checkoutFieldIs` (a text field's content; Country: the combo's value) |
| Change the delivery address (any field; Country: Germany, Austria, Netherlands, France) | act | `setCheckoutField` (types, then Tab so the field's error shows; Country: chosen from its list; `none` empties) |
| A field's error under it ("Street is required", "Germany uses a 5-digit postal code", "The Netherlands use the form 1234 AB", "Only +, digits and spaces, 6–20 characters"), once the field was left, or at once when the customer's own address is incomplete (Elm Street Studio has no postal code) | check | `checkoutFieldErrorIs` (by id `<field>Error`; `none` or nothing: absent) |

### Shipping step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Choose Standard or Express | act | `chooseShipping` ("express", "standard" or the radio's label; Express outside Germany/Austria fails with the app's reason) |
| The prices: Standard €4.90, free from €100.00 after the discount; Express €14.90 | check | `shippingPriceIs` (ids `standardPrice`, `expressPrice`) |
| Express is disabled outside Germany and Austria: "Express delivers to Germany and Austria only" | check | `buttonIsDisabled` with Express (radio state; the reason by its fixed text) |
| Which shipping method is chosen (Standard at first) | check | `checkoutChoiceIs` (a person's "Is checked" on the radio folds into it) |
| Write a delivery note | act | `setCheckoutField` with Delivery note; read back with `checkoutFieldIs` |

### Payment step

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| Pay on invoice with a PO number (customers that require one: "PO number") | act | `payByInvoice`, `checkoutWithPo` |
| Pay on invoice, PO number optional ("PO number (optional)": Birch & Co, Elm Street Studio) | act | `payByInvoice` (`none`: left empty), `checkoutWithPo` (types into whichever caption shows) |
| Pay by card: Card, then Card number (shown in groups of four), Expiry (MM/YY), Name on card | act | `payByCard`; the grouping: `checkoutFieldIs` Card number ("4242 4242 4242 4242") |
| The Card choice's hint "Debit or credit card" (fixed text under Card) | check | `paymentNoteIs` with Card (found by its text) |
| A card-only customer: Invoice is disabled, "Café Lumen pays by card", no PO number field | check | `buttonIsDisabled` with Invoice (reason: the invoice note), `paymentNoteIs` Invoice; `payByInvoice` and `checkoutWithPo` fail with that note |
| The invoice note: "Credit limit €2,000.00 · owes €450.00" | check | `paymentNoteIs` with Invoice (id `invoiceNote`) |
| Which way to pay is chosen (Invoice at first; Card for a card-only customer and after "Pay by card instead") | check | `checkoutChoiceIs` |
| A field's error: "PO number is required", "Enter the 16 digits of the card", "Use MM/YY", "Month must be 01–12", "This card has expired", "Name on card is required" | check | `checkoutFieldErrorIs` (`poNumberError`, `cardNumberError`, `expiryError`, `nameOnCardError`) |
| Next on an invoice over the credit limit warns ("This order takes Birch & Co to €1,036.60 of its €1,000.00 credit limit." / "Owes €940.00 now, this order adds €96.60."): Pay by card instead (Card gets chosen) or Continue with invoice | act | `checkoutNext` (or `checkoutWithPo`) ends on the warning; `answerCreditLimit` ("card", "continue"); `creditLimitWarningIs` reads it (message, detail or both) |

### Review step and placing

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The Delivery address, Shipping ("Standard · 3–5 business days · €4.90", "Note: …") and Payment ("Invoice (30 days) · PO PO-2026-131", "Card ending 4242") sections | check | `reviewSectionIs` (the section's lines parted by `;`) |
| Edit a section (its Edit goes back to that step) | act | `editReviewSection` |
| The order's lines: Product, Qty, Price, Line total | check | `reviewLinesAre`, `reviewLineIs` |
| The order's Subtotal, Volume discount, Shipping, VAT (20%) and Total | check | `reviewAmountIs` (ruling R20: by caption inside `id:totals`; `none` for a Volume discount not shown) |
| Place the order and confirm it ("Place order for €122.34?" / "Order for Ada Shop Ltd, 11 items.") | act | `placeOrder` |
| Back out at the confirmation (Cancel): still on Review | act | `cancelPlaceOrder` |
| The confirmation's total and item count | check | `placeOrderConfirmationIs` (presses Place order, reads, answers Cancel) |
| Placing: "Placing order…", "Order for €122.34 · Ada Shop Ltd", every button disabled, about 2 s | check | `placeOrder`'s outcome `placing` ("Order for €49.20 · Ada Shop Ltd"); a checkout step during placing fails saying the order is being placed. The disabled buttons: not checked (2 s window, inside `placeOrder`) |
| Placing fails: "The order could not be placed." (OK; back on Review) | check | Gap: needs app internals even after an app change (provoking it needs stock to change inside the modal 2 s placing window). `placeOrder` fails with the app's message if it ever shows |
| Once placed: the new order's tab takes the new order's place, titled with its number; the status line says "Order ORD-1640 placed" | check | `placeOrder` (outcome `number`, read from the status line by id `statusMessage` once that number's tab is selected) |

## Order tab

An order's own tab (`OrderPanel`, `StatusPill`): scope `["tab:<order>"]`; its Items, Delivery and Invoice sub-tabs
add `tab:Items` and so on.

| What the user does or sees | Kind (act/check/start) | Covered by (step id) or Gap (reason) |
|---|---|---|
| The order's number (also the tab's title), its status (Placed, Shipped, Cancelled) and facts ("Ada Shop Ltd · 12 Oct 2026 · €122.34") | check | `orderStatusIs` (the status, any case), `orderFactsAre` (the whole line or one part: customer, date or total); the number is the tab's title: `selectedTabIs`, and every order tab step finds the order by it (number or digits) |
| Switch between Items, Delivery and Invoice | act | `selectTab` (while the order's tab shows); each check of Items, Delivery or Invoice also shows that tab for its read, then the one that showed before |
| Items: SKU, Product, Qty, Price, Line total | check | `orderItemsAre` (the lines in order, by name or its start; no pattern), `orderItemIs` (one cell; the product by name, start or SKU; the Check menu keys a line by SKU) |
| "11 units in 2 lines" under the items | check | `orderItemsSummaryIs` (whole, or "11 units" / "2 lines") |
| Delivery: the address, the shipping method ("Standard · 3–5 business days · €4.90"), the delivery note ("—" when none) | check | `orderDeliveryIs` (address: its lines parted by ; or one line; shipping method, whole or one part: "Standard"; note, none or "—" for none). Suggest-only: see the note below |
| Invoice: the payment ("Invoice (30 days) · PO PO-T3"), the order date | check | `orderInvoiceIs` (Payment, Order date) |
| Invoice: Subtotal, Volume discount, Shipping, VAT (20%), Total | check | `orderInvoiceIs` (an amount by its caption; Volume discount none while it does not show) |
| Save the receipt (Save receipt…, a file chooser "Save receipt", ".txt" added when missing; the status line says "Receipt saved to <path>") | act | `saveReceipt` (outcome: path, replaced; suggest-only, see the folding note); the status line: `statusLineIs` |
| The name the file chooser suggests ("ORD-1640.txt", in the user's home) | check | `suggestedReceiptFileIs` (Save receipt…, reads the chooser, Cancel; the name with or without .txt, or the whole path) |
| Saving over a file asks "ORD-1640.txt already exists." / "Do you want to replace it?" (Replace, Cancel) | act | `saveReceipt` answers Replace; `keepExistingReceipt` answers Cancel (fails, saying the receipt was saved, when the file did not exist: the app asks nothing then) |
| Saving fails: "The receipt could not be saved: …" | check | `receiptSaveErrorIs` (whole text, the sentence before the colon or the reason after it; answers OK); `saveReceipt` fails with the app's message. Provoked live: a path in a folder that does not exist |
| The saved receipt's text | check | Gap: not a tester's concern (every fact in the file shows on the order tab: number, date, status and customer in its header, the lines on Items, address, shipping method and note on Delivery, payment and totals on Invoice; covered there. The file only words some differently: a title line "Order Desk — receipt", free shipping as "€0.00" where the tab says "Free", the method without its price, no SKU, no Note line when there is none) |
| Cancel the order (Cancel order, then "Cancel order ORD-1640?" with Cancel order / Keep; the status turns Cancelled, the status line says "Order ORD-1640 cancelled") | act | `cancelOrder` (waits for Cancelled), `keepOrder` (Keep); the status line: `statusLineIs` |
| Cancel order is disabled for a shipped or cancelled order: "Only placed orders can be cancelled — this one is shipped" | check | `buttonIsDisabled` with Cancel order (on the selected order's tab; the reason whole or "this one is shipped"), `buttonIsEnabled` (Cancel order, Save receipt…); `cancelOrder` on such an order fails with the reason before clicking |

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
- App bug found writing the steps, fixed with the person's OK (2026-10-22, see "Changes made to the app"): choosing
  a category, by a click in Categories or by Show in category, while the selected product's row lay past the new
  category's rows threw `ArrayIndexOutOfBoundsException` on the event thread. It came from the rows-updated event the
  tab fired each time it showed (`ProductsPanel.refresh`).

- Orders list and main window (sub-task d): an Orders list act goes to the Orders tab itself (a click on it; after a
  miss it selects the tab that was selected again); a check needs the Orders tab showing and says so otherwise. An
  order is given by its number, its digits ("1640") or a customer with one order listed; a customer with several fails
  naming their numbers. `openOrder` keeps its id, sentence and steps for one order (tab click, double click) and now
  fails fast, resolves the order, takes several, and waits for the order's tab. Sorting cannot be undone in the app (a
  third click reverses again), so `sortOrders` clicks the header until the column's values, read as shown (amounts,
  dates, the Status filter's order; Order and Customer as text with the collator of the app's locale, as the app's
  table sorter compares a text column: "ORD-1000" before "ORD-999"), are in the asked direction, and says what it left.
  A sort that never reads so fails after three clicks and puts the list back in the order it had (header clicks on a
  column that read sorted then, Order first), then shows the tab that was selected before; an `openOrder` whose tab
  does not open closes the tabs it opened, selects the orders that were selected before (or unselects its rows) and
  shows the tab that was selected before. No step ends the app:
  `cancelExit` and `exitQuestionIs` choose File ▸ Exit only while a new order with something in its cart shows, so the
  app asks; otherwise they fail and choose nothing. The open orders count moves with every placed or cancelled order:
  `openOrdersCountIs` takes the count the tester expects.
- Checkout (sub-task c): `placeOrder` read the status line as "a label containing ‘placed’" and cut the number out of
  its name. Since R1 that label's name is its code name `statusMessage`, so the cut threw "String index out of range:
  -7" after the order was placed (runs against an app before R1 passed). It now reads the label by id and its shown text
  ("Order ORD-1640 placed"), and waits until that number's tab is selected. `checkoutWithPo` no longer presses Next
  blind: a disabled Next fails with the app's reason and the fields' errors, a card-only customer fails with the invoice
  note, the PO goes into "PO number" or "PO number (optional)", and on a failure the checkout it opened is cancelled.
- A person's check of a table cell (fix round 1 of sub-task d): the Check menu records it as the bare table and a
  `cell` (the row by its key, the table's first column with unique values: Order, the cart's Product, the products'
  SKU, the review's Product; the column; the value), not as a target with a row and column. `orderIs`, `cartLineIs`,
  `productIs` and `reviewLineIs` match that shape (pinned by `CheckMenuForms`, a test helper that runs the library's
  own Check menu code on a table), and keep the shape the agent and the step bodies record (the cell as the target,
  then its text; bundled `cart-totals.json`'s "The A4 paper line comes to €14.70") as a second pattern (`matchAlso`).
  A check of the key cell itself is offered too, so each of them also reads its key column (Order, Product) and
  `productIs` takes a SKU for the product. Each fits only its own table: the Orders tab's scope with the Order key; a
  new order's scope with the Product key for the cart, the SKU key for the products (an order tab's Items have neither
  scope); the review's table by its code name `reviewLines` (R12: a pattern never compares the window, and the cart's
  table has the same columns). `reviewLineIs` still only suggests itself: its body reads the dialog's only table by
  role, so the body's steps carry no name to compare with its patterns.
- A checkout field, step, way to pay, section or answer is given by the tester's words: the name, its start, a word of
  it ("note": Delivery note), the label the app shows or the id ("postalCode"); "PO" is the PO number.

- Order tab (sub-task e): an act goes to the order's tab itself (a click on it; the order's tab must be open: "Open
  order …" first), and after a miss selects the tab that was selected before; a check needs the order's tab selected and
  says so otherwise ("Switch to the tab ORD-1639"). An order is given by its number or its digits, among the open order
  tabs. A check of Items, Delivery or Invoice clicks that tab for its read (their labels are in the tree only while their
  tab shows) and the one that showed before after it, also when it fails. Switching tabs is `selectTab`; Cancel order's
  state and reason are `buttonIsDisabled` / `buttonIsEnabled`, which take an order tab's buttons too (as they take the
  checkout's), on the selected order's tab. No reach fix was needed.
- Save receipt (sub-task e): the error "The receipt could not be saved: <path>" is long, so the option pane breaks it into
  two labels; the first ends with a colon, so the agent's tree takes it for the caption of the second and drops it (the
  second, named "OptionPane.label", holds only the path). The steps read the whole message from what the action changed
  (`Client.outcomeOf` around the file chooser's choice, its outcome's `message`), not from the labels; a person's Check on
  the error's label records the path only, which `receiptSaveErrorIs` takes too. A file chooser choice that saves at once
  shows no message: the step then reads the status line. Provoked live with a path in a folder that does not exist.
- Folding an order's tab (fix round 1 of sub-task e): a recording folds a run of a person's act steps as soon as it
  fits a step, and a check is a barrier no run crosses. `saveReceipt` only suggests itself: Save receipt… and the file
  chooser's choice fit it before the app has said whether it saved, asks "ORD-1640.txt already exists." or fails, and a
  person's Check of that question or error stops the run there; folded, the replay would answer Replace (over a file the
  person kept) or fail on the error the person answered OK. `keepExistingReceipt` folds: its run holds the Cancel that
  decides it (only the replace question has a Cancel; the error has OK only), so it folds only when that Cancel follows
  in the same run, never across a check. `cancelOrder` and `keepOrder` hold their deciding answer the same way.
- `orderDeliveryIs` only suggests itself: the Delivery tab's caption "Delivery address" and the address lines are labels
  named by their own text, which a pattern on a captioned label's name cannot tell from Shipping method or Delivery note
  (a check of an address line would capture the line as the detail). `orderInvoiceIs` folds: every label on the Invoice
  tab is a captioned value. `orderItemsAre` has no pattern: a recording holds one row of the lines it reads, and that
  row's pattern would offer "The lines on order ORD-1639's Items tab are Gel pen, black" for an order of several lines.
  `receiptSaveErrorIs` is a check of several steps: a check never folds, and the Check menu offers only checks of one
  step, so its pattern is never used (an agent runs it as a do step).
- Cancel order's state: the Check menu offers `buttonIsEnabled` for an order tab's enabled Cancel order or Save receipt…
  (one step). A disabled Cancel order with its reason is two reads, which no single check the menu records can be:
  `buttonIsDisabled` reads both when an agent runs it.

- Round 1 of the evaluation (Task 9a): a tester's agent read the Orders list raw (`read`) to say which orders a search
  left, which order a sort put first and last, and that a theme switch changed no data. The acts that change what the
  list shows (`searchOrders`, `clearOrderSearch`, `filterOrders`, `sortOrders`) now say in their outcome what it then
  shows (count, every order's six values, the empty text), and `showOrders` reads it without changing it (an act: it
  goes to the Orders tab; a check's outcome is always empty). `cancelQuestionIs` and `suggestedReceiptFileIs` are checks
  of several steps (they ask, read and answer Cancel or Keep): like `orderItemsAre` they have no pattern, and
  `showOrders` has none either (a person's click on the Orders tab reads nothing).

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
| R12 `ReviewStep` lines table | the cart's table has the same columns (Product, Qty, Price, Line total) and a pattern never compares the window, so a person's check of a cart line looked like a review line | `setName("reviewLines")` | invisible |

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
| R12 `ReviewStep`: the lines table `setName("reviewLines")` | The order's lines (Review): `reviewLineIs` offered on the review's table only | invisible | - |
| Bug fix `ProductsPanel.refresh`: the stock refresh repaints the products table instead of firing a rows-updated event (after it, choosing a category with a product selected below the category's rows failed) | Browse a category; Show a product in its category | no (a bug fix) | user OK 2026-10-22 |

## Windows the written patterns were meant for

A written pattern does not compare `window` yet (the library lists a pattern that names one under `session`'s
`vocabularyProblems`), so none of them names one. Each affected step keeps its pattern without it and matches in any
window; the window it is meant for, to restore when a pattern can compare windows:

| Step(s) | Window the pattern is meant for |
|---|---|
| `setCheckoutField`, `chooseShipping`, `payByInvoice`, `payByCard`, `checkoutNext`, `checkoutBack`, `editReviewSection`, `cancelCheckout`, `cancelPlaceOrder`, `checkoutWithPo` | the checkout dialog (`dialog`) |
| `checkoutCustomerIs`, `checkoutStepIs`, `checkoutFieldIs`, `checkoutFieldErrorIs`, `shippingPriceIs`, `paymentNoteIs`, `checkoutChoiceIs` | the checkout dialog (`dialog`); `checkoutFieldIs` is the one that would otherwise fit the Customer and Search fields too |
| `reviewSectionIs`, `reviewLinesAre`, `reviewLineIs`, `reviewAmountIs` | the checkout dialog (`dialog`), its Review step |
| `answerCreditLimit`, `creditLimitWarningIs` | the credit-limit warning (`dialog:` + its title) |
| `placeOrderConfirmationIs` | Place order's question (`dialog:` + its title); its first entry, the Place order click, the checkout dialog |
| `receiptSaveErrorIs` | the message dialog showing the error (`dialog`) |
| `customerSuggestionsAre` | the Customer field's suggestion list (`popup`) |
| `recentCustomersAre` | the toolbar ▾ menu (`popup`) |
