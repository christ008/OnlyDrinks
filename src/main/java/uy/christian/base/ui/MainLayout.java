package uy.christian.base.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import uy.christian.customers.ui.CustomerView;
import uy.christian.inventory.ui.ArchivedProductView;
import uy.christian.inventory.ui.ProductView;
import uy.christian.sales.ui.POSView;
import uy.christian.sales.ui.SalesView;
import uy.christian.security.ui.UserView;
import uy.christian.security.UserRepository;
import uy.christian.security.ui.UserInfoDialog;

import java.util.Locale;

@Layout
@PermitAll
public class MainLayout extends AppLayout implements AfterNavigationObserver, MessageSupport {

    private final H1 viewTitle = new H1();
    private final transient AuthenticationContext authenticationContext;
    private final transient UserRepository userRepository;
    private final transient MessageSource messageSource;

    MainLayout(AuthenticationContext authenticationContext, UserRepository userRepository, MessageSource messageSource) {
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
        setPrimarySection(Section.DRAWER);
        addToNavbar(true, createNavbar());

        VerticalLayout drawer = new VerticalLayout();
        drawer.addClassNames(LumoUtility.Background.CONTRAST_5);
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);

        drawer.add(createHeader());

        Scroller scroller = new Scroller(createSideNav());
        scroller.setWidthFull();

        drawer.add(scroller);
        drawer.setFlexGrow(1, scroller);

        addToDrawer(drawer);
    }

    private Component createUserMenu() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        authenticationContext.getAuthenticatedUser(UserDetails.class).flatMap(userDetails -> userRepository.findByUsername(userDetails.getUsername())).ifPresent(user -> {
            Icon userIcon = VaadinIcon.USER.create();

            MenuBar userMenu = new MenuBar();
            userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

            Span name = new Span(user.getName());
            name.addClassNames(LumoUtility.FontWeight.MEDIUM, LumoUtility.Margin.Right.SMALL);

            HorizontalLayout itemLayout = new HorizontalLayout(userIcon, name);
            itemLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            itemLayout.setSpacing(true);

            var menuItem = userMenu.addItem(itemLayout);
            menuItem.getSubMenu().addItem(msg("app.user.profile"), _ -> new UserInfoDialog(user).open());
            menuItem.getSubMenu().addItem(msg("app.user.logout"), _ -> authenticationContext.logout());

            layout.add(userMenu);
        });
        return layout;
    }

    private Component createNavbar() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout(toggle, viewTitle, createLanguageSelector(), createUserMenu());
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setFlexGrow(1, viewTitle);

        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        header.setWidthFull();
        return header;
    }

    private Component createLanguageSelector() {
        Select<LocaleOption> selector = new Select<>();
        selector.setItems(LocaleOption.values());
        selector.setItemLabelGenerator(option -> msg(option.labelKey()));
        selector.setLabel(msg("app.language.label"));
        selector.setAriaLabel(msg("app.language.label"));
        selector.setWidth("6rem");

        Locale sessionLocale = VaadinSession.getCurrent().getLocale();
        selector.setValue(LocaleOption.fromLocale(sessionLocale != null ? sessionLocale : Locale.ENGLISH));

        selector.addValueChangeListener(event -> {
            LocaleOption selected = event.getValue();
            if (selected == null) {
                return;
            }
            UI currentUi = UI.getCurrent();
            VaadinSession.getCurrent().setLocale(selected.locale());
            currentUi.setLocale(selected.locale());
            currentUi.getPage().reload();
        });
        return selector;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String title = getLocalizedViewTitle();
        viewTitle.setText(title);
        UI.getCurrent().getPage().setTitle(title + " | " + msg("app.title.brand"));
    }

    private Component createHeader() {
        var appLogo = VaadinIcon.GLASS.create();
        appLogo.addClassNames(LumoUtility.TextColor.PRIMARY);
        appLogo.addClassNames(LumoUtility.Width.LARGE);
        appLogo.addClassNames(LumoUtility.Height.LARGE);

        var appName = new Span(msg("app.title.brand"));
        appName.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.XLARGE);

        var header = new HorizontalLayout(appLogo, appName);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10);
        header.setWidthFull();
        return header;
    }

    private Component createSideNav() {
        AccessAnnotationChecker checker = new AccessAnnotationChecker();

        SideNav mainNav = new SideNav();
        mainNav.setLabel(msg("app.nav.general"));
        mainNav.setWidthFull();

        SideNav adminNav = new SideNav();
        adminNav.setLabel(msg("app.nav.administration"));
        adminNav.setWidthFull();

        MenuConfiguration.getMenuEntries().forEach(entry -> {
            SideNavItem item = createSideNavItem(entry);
            String path = entry.path().replaceAll("^/+", "");
            var hasAccess = checker.hasAccess(entry.menuClass());
            boolean adminItem = "users".equals(path) || "inventory/archived".equals(path);
            if (hasAccess && adminItem) {
                adminNav.addItem(item);
            } else if (hasAccess) {
                mainNav.addItem(item);
            }
        });

        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.SMALL);

        layout.add(mainNav);
        if (!adminNav.getItems().isEmpty()) {
            layout.add(adminNav);
        }
        return layout;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        Icon icon = menuEntry.icon() != null ? new Icon(menuEntry.icon()) : null;
        return new SideNavItem(resolveMenuTitle(menuEntry), menuEntry.path(), icon);
    }

    private String resolveMenuTitle(MenuEntry menuEntry) {
        String path = menuEntry.path().replaceAll("^/+", "");
        return switch (path) {
            case "" -> msg("app.menu.dashboard");
            case "pos" -> msg("app.menu.pos");
            case "inventory" -> msg("app.menu.inventory");
            case "customers" -> msg("app.menu.customers");
            case "sales" -> msg("app.menu.sales");
            case "users" -> msg("app.menu.users");
            case "inventory/archived" -> msg("app.menu.archivedInventory");
            default -> menuEntry.title();
        };
    }

    private String getLocalizedViewTitle() {
        Class<?> viewClass = getContent().getClass();
        if (DashboardView.class.equals(viewClass)) {
            return msg("app.menu.dashboard");
        }
        if (POSView.class.equals(viewClass)) {
            return msg("app.menu.pos");
        }
        if (ProductView.class.equals(viewClass)) {
            return msg("app.menu.inventory");
        }
        if (CustomerView.class.equals(viewClass)) {
            return msg("app.menu.customers");
        }
        if (SalesView.class.equals(viewClass)) {
            return msg("app.menu.sales");
        }
        if (UserView.class.equals(viewClass)) {
            return msg("app.menu.users");
        }
        if (ArchivedProductView.class.equals(viewClass)) {
            return msg("app.menu.archivedInventory");
        }
        return msg("app.title.brand");
    }

    @Override
    public MessageSource getMessageSource() {
        return messageSource;
    }

    private enum LocaleOption {
        ENGLISH(Locale.ENGLISH, "app.language.english"),
        SPANISH(Locale.of("es"), "app.language.spanish");

        private final Locale locale;
        private final String labelKey;

        LocaleOption(Locale locale, String labelKey) {
            this.locale = locale;
            this.labelKey = labelKey;
        }

        Locale locale() {
            return locale;
        }

        String labelKey() {
            return labelKey;
        }

        static LocaleOption fromLocale(Locale locale) {
            return "es".equalsIgnoreCase(locale.getLanguage()) ? SPANISH : ENGLISH;
        }
    }
}
