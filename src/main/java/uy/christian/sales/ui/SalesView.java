package uy.christian.sales.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import uy.christian.sales.Order;
import uy.christian.sales.OrderItem;
import uy.christian.sales.OrderService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Route("sales")
@PageTitle("Sales | OnlyDrinks")
@Menu(order = 3, icon = "vaadin:chart", title = "Sales")
@RolesAllowed("MANAGER")
public class SalesView extends VerticalLayout {

    private final transient OrderService orderService;
    private final Grid<Order> grid = new Grid<>(Order.class, false);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String MONEY_FORMAT = "$%.2f";

    public SalesView(OrderService orderService) {
        this.orderService = orderService;
        setSizeFull();

        add(new H2("Sales & Analytics"));

        BigDecimal totalRevenue = orderService.getTotalRevenue();
        Span revenue = new Span(String.format("Total Revenue: $%.2f", totalRevenue));
        revenue.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        add(revenue);

        addSalesByProduct();

        configureGrid();
        add(grid);

        refreshGrid();
    }

    private void addSalesByProduct() {
        Map<String, BigDecimal> salesByProduct = orderService.getSalesByProduct();
        if (salesByProduct.isEmpty()) {
            return;
        }
        add(new H3("Sales by Product"));
        Grid<Map.Entry<String, BigDecimal>> productGrid = new Grid<>();
        productGrid.addColumn(Map.Entry::getKey).setHeader("Product");
        productGrid.addColumn(entry -> String.format(MONEY_FORMAT, entry.getValue())).setHeader("Total Sales");
        productGrid.setItems(salesByProduct.entrySet());
        productGrid.setAllRowsVisible(true);
        add(productGrid);
    }

    private void configureGrid() {
        grid.addColumn(order -> order.getOrderDate() != null
                ? order.getOrderDate().format(FORMATTER)
                : "")
                .setHeader("Date");
        grid.addColumn(order -> order.getCustomer() != null ? order.getCustomer().getName() : "Anonymous")
                .setHeader("Customer");
        grid.addColumn(order -> String.format(MONEY_FORMAT, order.getTotalAmount())).setHeader("Total Amount");
        grid.addComponentColumn(order -> {
            Button details = new Button("Details", e -> showOrderDetails(order));
            details.addThemeVariants(ButtonVariant.LUMO_SMALL);
            return details;
        }).setHeader("Actions");
        grid.setSizeFull();
    }

    private void showOrderDetails(Order order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Order Details - " + (order.getCustomer() != null ? order.getCustomer().getName() : "Anonymous"));

        Grid<OrderItem> itemsGrid = new Grid<>(OrderItem.class, false);
        itemsGrid.addColumn(item -> item.getProduct().getName()).setHeader("Product");
        itemsGrid.addColumn(OrderItem::getQuantity).setHeader("Quantity");
        itemsGrid.addColumn(item -> String.format(MONEY_FORMAT, item.getUnitPrice())).setHeader("Unit Price");
        itemsGrid.addColumn(item -> String.format(MONEY_FORMAT, item.getSubtotal())).setHeader("Subtotal");
        itemsGrid.setItems(order.getItems());
        itemsGrid.setAllRowsVisible(true);

        dialog.add(itemsGrid);

        Button closeButton = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(orderService.findAll());
    }
}
