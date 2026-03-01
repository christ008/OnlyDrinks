package uy.christian.customers.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Validator;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import uy.christian.base.ui.FormValidation;
import uy.christian.base.ui.MessageSupport;
import uy.christian.base.ui.NotificationUtils;
import uy.christian.customers.Customer;
import uy.christian.customers.CustomerService;

import java.util.Optional;

@Route("customers")
@PageTitle("Customers | OnlyDrinks")
@Menu(order = 2, icon = "vaadin:users", title = "Customers")
@RolesAllowed("BARMAN")
public class CustomerView extends VerticalLayout implements MessageSupport {

    private final CustomerService customerService;
    private final transient Validator validator;
    private final transient MessageSource messageSource;
    private final Grid<Customer> grid = new Grid<>(Customer.class, false);

    private final TextField name = new TextField();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final H3 formTitle = new H3();
    private final Span statusLabel = new Span();

    private final VerticalLayout form;
    private transient @Nullable Customer selectedCustomer;

    public CustomerView(CustomerService customerService, Validator validator, MessageSource messageSource) {
        this.customerService = customerService;
        this.validator = validator;
        this.messageSource = messageSource;
        setSizeFull();

        name.setLabel(msg("customers.form.name"));
        email.setLabel(msg("customers.form.email"));
        phone.setLabel(msg("customers.form.phone"));
        address.setLabel(msg("customers.form.address"));

        add(new H2(msg("customers.title")));

        configureGrid();
        Button addCustomerButton = new Button(msg("customers.action.new"), e -> newCustomer());
        addCustomerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        this.form = createForm();
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.setSizeFull();

        add(addCustomerButton, content);

        refreshGrid();
        editCustomer(null);
    }

    @Override
    public MessageSource getMessageSource() {
        return messageSource;
    }

    private void configureGrid() {
        grid.addColumn(Customer::getName).setHeader(msg("customers.form.name"));
        grid.addColumn(Customer::getEmail).setHeader(msg("customers.form.email"));
        grid.addColumn(Customer::getPhone).setHeader(msg("customers.form.phone"));
        grid.addColumn(Customer::getAddress).setHeader(msg("customers.form.address"));
        grid.setSizeFull();

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.isFromClient()) {
                editCustomer(event.getValue());
            }
        });
    }

    private VerticalLayout createForm() {
        formTitle.addClassNames(LumoUtility.Margin.Top.NONE);
        statusLabel.setVisible(false);
        statusLabel.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        FormLayout formLayout = new FormLayout(name, email, phone, address);
        
        Button save = new Button(msg("common.action.save"), _ -> saveCustomer());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(msg("common.action.delete"), _ -> deleteCustomer());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancel = new Button(msg("common.action.cancel"), _ -> editCustomer(null));

        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        
        VerticalLayout layout = new VerticalLayout(formTitle, statusLabel, formLayout, buttons);
        layout.setWidth("350px");
        return layout;
    }

    private void refreshGrid() {
        grid.setItems(customerService.findAll());
    }

    private void newCustomer() {
        grid.asSingleSelect().clear();
        editCustomer(new Customer());
    }

    private void editCustomer(@Nullable Customer customer) {
        if (customer == null) {
            selectedCustomer = null;
            form.setEnabled(false);
            form.addClassNames("opacity-50");
            formTitle.setText(msg("customers.form.select"));
            grid.asSingleSelect().clear();
            clearFormFields();
        } else {
            selectedCustomer = customer;
            form.setEnabled(true);
            form.removeClassNames("opacity-50");
            formTitle.setText(customer.getId() == null
                    ? msg("customers.form.new")
                    : msg("customers.form.edit"));
            name.setValue(customer.getName());
            email.setValue(valueOrEmpty(customer.getEmail()));
            phone.setValue(valueOrEmpty(customer.getPhone()));
            address.setValue(valueOrEmpty(customer.getAddress()));
        }
        form.setVisible(true);
        statusLabel.setVisible(false);
    }

    private void saveCustomer() {
        if (selectedCustomer == null) {
            return;
        }

        CustomerForm formData = new CustomerForm(
                name.getValue().trim(),
                normalizeOptional(email.getValue()),
                normalizeOptional(phone.getValue()),
                normalizeOptional(address.getValue())
        );
        Optional<String> validationMessage = FormValidation.firstViolationMessage(validator, formData);
        if (validationMessage.isPresent()) {
            statusLabel.setText(validationMessage.get());
            statusLabel.setVisible(true);
            return;
        }
        statusLabel.setVisible(false);

        selectedCustomer.setName(formData.name());
        selectedCustomer.setEmail(formData.email());
        selectedCustomer.setPhone(formData.phone());
        selectedCustomer.setAddress(formData.address());

        customerService.save(selectedCustomer);
        refreshGrid();
        editCustomer(null);
        NotificationUtils.showSuccess(msg("customers.notify.saved"));
    }

    private void deleteCustomer() {
        if (selectedCustomer != null && selectedCustomer.getId() != null) {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader(msg("customers.delete.header"));
            dialog.setText(msg("customers.delete.text"));

            dialog.setCancelable(true);
            dialog.setConfirmText(msg("common.action.delete"));
            dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());

            dialog.addConfirmListener(e -> {
                if (selectedCustomer != null && selectedCustomer.getId() != null) {
                    customerService.delete(selectedCustomer.getId());
                    refreshGrid();
                    editCustomer(null);
                    NotificationUtils.showSuccess(msg("customers.notify.deleted"));
                }
            });
            dialog.open();
        }
    }

    private static @Nullable String normalizeOptional(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String valueOrEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private void clearFormFields() {
        name.clear();
        email.clear();
        phone.clear();
        address.clear();
    }

}
