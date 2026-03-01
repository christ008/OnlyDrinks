package uy.christian.sales.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Validator;
import org.jspecify.annotations.Nullable;
import uy.christian.base.ui.FormValidation;
import uy.christian.base.ui.NotificationUtils;
import uy.christian.base.ui.UiMessages;
import uy.christian.customers.Customer;
import uy.christian.customers.CustomerService;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductService;
import uy.christian.sales.Order;
import uy.christian.sales.OrderItem;
import uy.christian.sales.POSService;

import java.util.ArrayList;
import java.util.Optional;

@Route("pos")
@PageTitle("POS | OnlyDrinks")
@Menu(order = 0, icon = "vaadin:cart", title = "POS")
@RolesAllowed("BARMAN")
public class POSView extends VerticalLayout {

    private final transient POSService posService;
    private final transient ProductService productService;
    private final transient CustomerService customerService;
    private final transient Validator validator;
    private final transient UiMessages uiMessages;

    private final ComboBox<Customer> customerComboBox = new ComboBox<>();
    private final ComboBox<Product> product = new ComboBox<>();
    private final IntegerField quantity = new IntegerField();
    private final Grid<OrderItem> grid = new Grid<>(OrderItem.class, false);
    private final Span statusLabel = new Span();
    private final Span totalSpan = new Span();
    private final Button placeOrderButton = new Button();

    private transient @Nullable Order currentOrder;

    public POSView(POSService posService, ProductService productService, CustomerService customerService, Validator validator, UiMessages uiMessages) {
        this.posService = posService;
        this.productService = productService;
        this.customerService = customerService;
        this.validator = validator;
        this.uiMessages = uiMessages;

        setSizeFull();
        customerComboBox.setLabel(uiMessages.get("pos.form.customer"));
        product.setLabel(uiMessages.get("pos.form.product"));
        quantity.setLabel(uiMessages.get("pos.form.quantity"));
        placeOrderButton.setText(uiMessages.get("pos.action.place"));
        placeOrderButton.addClickListener(e -> placeOrder());
        totalSpan.setText(uiMessages.get("pos.total", "0.00"));

        add(new H2(uiMessages.get("pos.title")));

        configureCustomerSelector();
        configureProductSelector();
        configureGrid();

        HorizontalLayout selectors = new HorizontalLayout(customerComboBox, product, quantity);
        selectors.setAlignItems(Alignment.BASELINE);

        Button addButton = new Button(uiMessages.get("pos.action.add"), e -> addToOrder());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        selectors.add(addButton);

        totalSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        
        statusLabel.setVisible(false);
        statusLabel.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);

        placeOrderButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footer = new HorizontalLayout(totalSpan, placeOrderButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(JustifyContentMode.BETWEEN);
        footer.setAlignItems(Alignment.CENTER);

        add(selectors, statusLabel, grid, footer);
        
        resetOrder();
    }

    private void configureCustomerSelector() {
        customerComboBox.setItems(customerService.findAll());
        customerComboBox.setItemLabelGenerator(Customer::getName);
        customerComboBox.setPlaceholder(uiMessages.get("pos.customer.anonymous"));
        customerComboBox.setClearButtonVisible(true);
    }

    private void configureProductSelector() {
        product.setItems(productService.findAll());
        product.setItemLabelGenerator(p -> p.getName() + " ($" + p.getPrice() + ")");
    }

    private void configureGrid() {
        grid.addColumn(item -> item.getProduct().getName()).setHeader(uiMessages.get("pos.form.product"));
        grid.addColumn(OrderItem::getQuantity).setHeader(uiMessages.get("pos.form.quantity"));
        grid.addColumn(item -> String.format("$%.2f", item.getUnitPrice())).setHeader(uiMessages.get("pos.grid.unitPrice"));
        grid.addColumn(item -> String.format("$%.2f", item.getSubtotal())).setHeader(uiMessages.get("pos.grid.subtotal"));
        grid.addComponentColumn(item -> {
            Button remove = new Button(uiMessages.get("common.action.remove"), e -> {
                if (currentOrder != null) {
                    posService.removeItemFromOrder(currentOrder, item);
                    refreshGrid();
                }
            });
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            return remove;
        }).setHeader(uiMessages.get("common.grid.actions"));
    }

    private void addToOrder() {
        if (currentOrder == null) {
            resetOrder();
        }
        Order order = currentOrder;
        if (order == null) {
            NotificationUtils.showError(uiMessages.get("pos.error.createOrder"));
            return;
        }

        PosItemForm formData = new PosItemForm(product.getValue(), quantity.getValue());
        Optional<String> validationMessage = FormValidation.firstViolationMessage(validator, formData);
        if (validationMessage.isPresent()) {
            String message = validationMessage.get();
            statusLabel.setText(message);
            statusLabel.setVisible(true);
            NotificationUtils.showError(message);
            return;
        }
        statusLabel.setVisible(false);

        try {
            posService.addItemToOrder(order, formData.product(), formData.quantity().longValue());
            refreshGrid();
            resetItemFields();
        } catch (IllegalArgumentException | IllegalStateException e) {
            NotificationUtils.showError(e.getMessage());
        }
    }

    private void refreshGrid() {
        if (currentOrder == null) {
            return;
        }
        grid.setItems(new ArrayList<>(currentOrder.getItems()));
        totalSpan.setText(uiMessages.get("pos.total", String.format("%.2f", currentOrder.getTotalAmount())));
        placeOrderButton.setEnabled(!currentOrder.getItems().isEmpty());
    }

    private void placeOrder() {
        if (currentOrder == null) {
            return;
        }
        try {
            currentOrder.setCustomer(customerComboBox.getValue());
            posService.placeOrder(currentOrder);
            NotificationUtils.showSuccess(uiMessages.get("pos.notify.placed"));
            resetOrder();
        } catch (IllegalStateException e) {
            NotificationUtils.showError(e.getMessage());
        }
    }

    private void resetOrder() {
        currentOrder = posService.createNewOrder();
        customerComboBox.clear();
        resetItemFields();
        refreshGrid();
    }

    private void resetItemFields() {
        product.clear();
        quantity.setValue(1);
    }
}
