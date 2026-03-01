package uy.christian.base.ui;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.GrantedAuthority;

@Route("")
@PageTitle("Dashboard | OnlyDrinks")
@Menu(order = 0, icon = "vaadin:dashboard", title = "Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    public DashboardView(AuthenticationContext authenticationContext) {
        setPadding(true);

        add(new H2("Welcome to OnlyDrinks"));

        // --- Debugging Section Start ---
        add(new H3("Security Debug Info"));
        add(new Paragraph("Logged in as: " + authenticationContext.getPrincipalName().orElse("Unknown user")));
        add(new Paragraph("Effective Authorities (Hierarchical):"));

        UnorderedList authoritiesList = new UnorderedList();
        authenticationContext.getGrantedAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .forEach(name -> authoritiesList.add(new ListItem(name)));
        add(authoritiesList);
        // --- Debugging Section End ---

        add(new Paragraph("Self-managed drinks business management system."));
        add(new Paragraph("Use the sidebar to navigate through Inventory, Customers, and Sales."));
    }
}
