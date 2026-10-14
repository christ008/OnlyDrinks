package uy.christian.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.MessageSource;
import org.springframework.security.core.GrantedAuthority;
import uy.christian.base.DashboardService;
import uy.christian.base.DashboardService.DashboardStats;
import uy.christian.inventory.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Route("")
@PageTitle("Dashboard | OnlyDrinks")
@Menu(order = 0, icon = "vaadin:dashboard", title = "Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout implements MessageSupport {

    private static final String MANAGER_AUTHORITY = "ROLE_MANAGER";
    private static final String MONEY_FORMAT = "$%.2f";
    private static final String CARD_WIDTH = "240px";

    private final transient MessageSource messageSource;

    public DashboardView(AuthenticationContext authenticationContext,
                         DashboardService dashboardService,
                         MessageSource messageSource) {
        this.messageSource = messageSource;
        setPadding(true);

        add(new H2(msg("dashboard.welcome", authenticationContext.getPrincipalName().orElse("-"))));

        boolean isManager = authenticationContext.getGrantedAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(MANAGER_AUTHORITY::equals);

        if (isManager) {
            add(new Paragraph(msg("dashboard.subtitle")));
            addStats(dashboardService.loadStats());
        } else {
            add(new Paragraph(msg("dashboard.hint")));
        }
    }

    @Override
    public MessageSource getMessageSource() {
        return messageSource;
    }

    private void addStats(DashboardStats stats) {
        FlexLayout kpis = cardRow(
                kpiCard(msg("dashboard.kpi.revenueToday"), String.format(MONEY_FORMAT, stats.revenueToday())),
                kpiCard(msg("dashboard.kpi.ordersToday"), String.valueOf(stats.ordersToday())));

        FlexLayout lists = cardRow(
                listCard(msg("dashboard.kpi.lowStock", DashboardService.LOW_STOCK_THRESHOLD),
                        msg("dashboard.kpi.lowStock.none"), lowStockRows(stats.lowStockProducts())),
                listCard(msg("dashboard.kpi.topProducts"),
                        msg("dashboard.kpi.topProducts.none"), topProductRows(stats.topProducts())));

        add(kpis, lists);
    }

    private List<Div> lowStockRows(List<Product> products) {
        return products.stream()
                .map(p -> row(p.getName(), msg("dashboard.units", p.getStockQuantity()),
                        p.getStockQuantity() == 0 ? LumoUtility.TextColor.ERROR : LumoUtility.TextColor.WARNING))
                .toList();
    }

    private List<Div> topProductRows(Map<String, BigDecimal> topProducts) {
        return topProducts.entrySet().stream()
                .map(e -> row(e.getKey(), String.format(MONEY_FORMAT, e.getValue()), LumoUtility.TextColor.BODY))
                .toList();
    }

    private static FlexLayout cardRow(Component... cards) {
        FlexLayout row = new FlexLayout(cards);
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.addClassNames(LumoUtility.Gap.LARGE);
        row.setWidthFull();
        return row;
    }

    private static Div card() {
        Div card = new Div();
        card.addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.LARGE);
        card.setMinWidth(CARD_WIDTH);
        return card;
    }

    private static Div kpiCard(String title, String value) {
        Div card = card();
        Span label = new Span(title);
        label.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Display.BLOCK);
        Span number = new Span(value);
        number.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD);
        card.add(label, number);
        return card;
    }

    private static Div listCard(String title, String emptyText, List<Div> rows) {
        Div card = card();
        card.setMinWidth("320px");
        H3 heading = new H3(title);
        heading.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.MEDIUM);
        card.add(heading);
        if (rows.isEmpty()) {
            Span empty = new Span(emptyText);
            empty.addClassNames(LumoUtility.TextColor.SECONDARY);
            card.add(empty);
        } else {
            rows.forEach(card::add);
        }
        return card;
    }

    private static Div row(String left, String right, String rightColorClass) {
        Div row = new Div();
        row.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Padding.Vertical.XSMALL);
        Span name = new Span(left);
        Span value = new Span(right);
        value.addClassNames(LumoUtility.FontWeight.SEMIBOLD, rightColorClass);
        row.add(name, value);
        return row;
    }
}
