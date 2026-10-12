package io.github.kormcrete.orderdesk.ui;

import io.github.kormcrete.orderdesk.model.Money;
import io.github.kormcrete.orderdesk.model.Order;
import io.github.kormcrete.orderdesk.model.OrderBook;
import io.github.kormcrete.orderdesk.model.OrderStatus;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;

/** Order, Date, Customer, Items, Total, Status. */
final class OrdersTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Order", "Date", "Customer", "Items", "Total", "Status"};
    private static final Class<?>[] TYPES = {String.class, LocalDate.class, String.class, Integer.class, Money.class, OrderStatus.class};

    private final OrderBook book;

    OrdersTableModel(OrderBook book) {
        this.book = book;
    }

    Order orderAt(int modelRow) {
        return book.orders().get(modelRow);
    }

    @Override
    public int getRowCount() {
        return book.orders().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return TYPES[column];
    }

    @Override
    public Object getValueAt(int row, int column) {
        Order order = book.orders().get(row);
        switch (column) {
            case 0:
                return order.number();
            case 1:
                return order.date();
            case 2:
                return order.customer().name();
            case 3:
                return order.units();
            case 4:
                return order.totals().total();
            default:
                return order.status();
        }
    }
}
