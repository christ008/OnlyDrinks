package uy.christian.inventory.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.context.MessageSource;
import java.time.format.DateTimeFormatter;
import uy.christian.base.ui.MessageSupport;
import uy.christian.base.ui.NotificationUtils;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductService;

@Route("inventory/archived")
@PageTitle("Archived Products | OnlyDrinks")
@Menu(order = 5, icon = "vaadin:archive", title = "Archived Products")
@RolesAllowed("MANAGER")
public class ArchivedProductView extends VerticalLayout implements MessageSupport {

    private final transient ProductService productService;
    private final transient MessageSource messageSource;
    private final Grid<Product> grid = new Grid<>(Product.class, false);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ArchivedProductView(ProductService productService, MessageSource messageSource) {
        this.productService = productService;
        this.messageSource = messageSource;
        setSizeFull();

        add(new H2(msg("app.menu.archivedInventory")));

        configureGrid();
        add(grid);

        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Product::getName).setHeader(msg("inventory.form.name"));
        grid.addColumn(Product::getDescription).setHeader(msg("inventory.form.description"));
        grid.addColumn(Product::getStockQuantity).setHeader(msg("inventory.form.stock"));
        grid.addColumn(Product::getPrice).setHeader(msg("inventory.form.price"));
        grid.addColumn(product -> product.getDeletedAt() != null 
                ? product.getDeletedAt().format(FORMATTER) 
                : "").setHeader(msg("inventory.archived.form.archivedAt"));
        
        grid.addComponentColumn(product -> {
            Button restore = new Button(msg("inventory.action.restore"), e -> {
                if (product.getId() != null) {
                    productService.restore(product.getId());
                    refreshGrid();
                    NotificationUtils.showSuccess(msg("inventory.notify.restored"));
                }
            });
            restore.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            return restore;
        }).setHeader(msg("common.grid.actions"));
        
        grid.setSizeFull();
    }

    private void refreshGrid() {
        grid.setItems(productService.findAllArchived());
    }

    @Override
    public MessageSource getMessageSource() {
        return messageSource;
    }
}
