package uy.christian.inventory.ui;

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
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
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
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductService;

import java.util.Optional;

@Route("inventory")
@PageTitle("Inventory | OnlyDrinks")
@Menu(order = 1, icon = "vaadin:package", title = "Inventory")
@RolesAllowed("MANAGER")
public class ProductView extends VerticalLayout implements MessageSupport {

    private final transient ProductService productService;
    private final transient Validator validator;
    private final transient MessageSource messageSource;
    private final Grid<Product> grid = new Grid<>(Product.class, false);

    private final TextField name = new TextField();
    private final TextField description = new TextField();
    private final IntegerField stockQuantity = new IntegerField();
    private final BigDecimalField price = new BigDecimalField();
    private final H3 formTitle = new H3();
    private final Span statusLabel = new Span();

    private final VerticalLayout form;
    private transient @Nullable Product selectedProduct;

    public ProductView(ProductService productService, Validator validator, MessageSource messageSource) {
        this.productService = productService;
        this.validator = validator;
        this.messageSource = messageSource;
        setSizeFull();

        name.setLabel(msg("inventory.form.name"));
        description.setLabel(msg("inventory.form.description"));
        stockQuantity.setLabel(msg("inventory.form.stock"));
        price.setLabel(msg("inventory.form.price"));

        add(new H2(msg("inventory.title")));

        configureGrid();
        Button addProductButton = new Button(msg("inventory.action.new"), e -> newProduct());
        addProductButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        this.form = createForm();
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.setSizeFull();

        add(addProductButton, content);

        refreshGrid();
        editProduct(null);
    }

    @Override
    public MessageSource getMessageSource() {
        return messageSource;
    }

    private void configureGrid() {
        grid.addColumn(Product::getName).setHeader(msg("inventory.form.name"));
        grid.addColumn(Product::getDescription).setHeader(msg("inventory.form.description"));
        grid.addColumn(Product::getStockQuantity).setHeader(msg("inventory.form.stock"));
        grid.addColumn(Product::getPrice).setHeader(msg("inventory.form.price"));
        grid.setSizeFull();

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.isFromClient()) {
                editProduct(event.getValue());
            }
        });
    }

    private VerticalLayout createForm() {
        formTitle.addClassNames(LumoUtility.Margin.Top.NONE);
        statusLabel.setVisible(false);
        statusLabel.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        FormLayout formLayout = new FormLayout(name, description, stockQuantity, price);
        
        Button save = new Button(msg("common.action.save"), e -> saveProduct());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(msg("common.action.delete"), e -> deleteProduct());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancel = new Button(msg("common.action.cancel"), e -> editProduct(null));

        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        
        VerticalLayout layout = new VerticalLayout(formTitle, statusLabel, formLayout, buttons);
        layout.setWidth("350px");
        return layout;
    }

    private void refreshGrid() {
        grid.setItems(productService.findAll());
    }

    private void newProduct() {
        grid.asSingleSelect().clear();
        editProduct(new Product());
    }

    private void editProduct(@Nullable Product product) {
        if (product == null) {
            selectedProduct = null;
            form.setEnabled(false);
            form.addClassNames("opacity-50");
            formTitle.setText(msg("inventory.form.select"));
            grid.asSingleSelect().clear();
            clearFormFields();
        } else {
            selectedProduct = product;
            form.setEnabled(true);
            form.removeClassNames("opacity-50");
            formTitle.setText(product.getId() == null
                    ? msg("inventory.form.new")
                    : msg("inventory.form.edit"));
            name.setValue(product.getName());
            description.setValue(valueOrEmpty(product.getDescription()));
            stockQuantity.setValue(Math.toIntExact(product.getStockQuantity()));
            price.setValue(product.getPrice());
        }
        form.setVisible(true);
        statusLabel.setVisible(false);
    }

    private void saveProduct() {
        if (selectedProduct == null) {
            return;
        }

        Integer stock = stockQuantity.getValue();
        ProductForm formData = new ProductForm(
                name.getValue().trim(),
                normalizeOptional(description.getValue()),
                stock == null ? null : stock.longValue(),
                price.getValue()
        );
        Optional<String> validationMessage = FormValidation.firstViolationMessage(validator, formData);
        if (validationMessage.isPresent()) {
            statusLabel.setText(validationMessage.get());
            statusLabel.setVisible(true);
            return;
        }
        statusLabel.setVisible(false);

        selectedProduct.setName(formData.name());
        selectedProduct.setDescription(formData.description());
        selectedProduct.setStockQuantity(formData.stockQuantity());
        selectedProduct.setPrice(formData.price());

        productService.save(selectedProduct);
        refreshGrid();
        editProduct(null);
        NotificationUtils.showSuccess(msg("inventory.notify.saved"));
    }

    private void deleteProduct() {
        if (selectedProduct != null && selectedProduct.getId() != null) {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader(msg("inventory.archive.header"));
            dialog.setText(msg("inventory.archive.text"));

            dialog.setCancelable(true);
            dialog.setConfirmText(msg("inventory.action.archive"));
            dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());

            dialog.addConfirmListener(e -> {
                if (selectedProduct != null && selectedProduct.getId() != null) {
                    productService.archive(selectedProduct.getId());
                    refreshGrid();
                    editProduct(null);
                    NotificationUtils.showSuccess(msg("inventory.notify.archived"));
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
        description.clear();
        stockQuantity.clear();
        price.clear();
    }

}
