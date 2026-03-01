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
import java.time.format.DateTimeFormatter;
import uy.christian.base.ui.NotificationUtils;
import uy.christian.inventory.Product;
import uy.christian.inventory.ProductService;

@Route("inventory/archived")
@PageTitle("Archived Products | OnlyDrinks")
@Menu(order = 5, icon = "vaadin:archive", title = "Archived Products")
@RolesAllowed("MANAGER")
public class ArchivedProductView extends VerticalLayout {

    private final transient ProductService productService;
    private final Grid<Product> grid = new Grid<>(Product.class, false);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ArchivedProductView(ProductService productService) {
        this.productService = productService;
        setSizeFull();

        add(new H2("Archived Products"));

        configureGrid();
        add(grid);

        refreshGrid();
    }

    private void configureGrid() {
        grid.addColumn(Product::getName).setHeader("Name");
        grid.addColumn(Product::getDescription).setHeader("Description");
        grid.addColumn(Product::getStockQuantity).setHeader("Stock");
        grid.addColumn(Product::getPrice).setHeader("Price");
        grid.addColumn(product -> product.getDeletedAt() != null 
                ? product.getDeletedAt().format(FORMATTER) 
                : "").setHeader("Archived At");
        
        grid.addComponentColumn(product -> {
            Button restore = new Button("Restore", e -> {
                if (product.getId() != null) {
                    productService.restore(product.getId());
                    refreshGrid();
                    NotificationUtils.showSuccess("Product restored");
                }
            });
            restore.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            return restore;
        }).setHeader("Actions");
        
        grid.setSizeFull();
    }

    private void refreshGrid() {
        grid.setItems(productService.findAllArchived());
    }
}
