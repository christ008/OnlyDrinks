package uy.christian.base.ui;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;
import uy.christian.security.UserRepository;
import uy.christian.security.ui.UserInfoDialog;

@Layout
@PermitAll
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final H1 viewTitle = new H1();
    private final transient AuthenticationContext authenticationContext;
    private final transient UserRepository userRepository;

    MainLayout(AuthenticationContext authenticationContext, UserRepository userRepository) {
        this.authenticationContext = authenticationContext;
        this.userRepository = userRepository;
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
            menuItem.getSubMenu().addItem("Profile", _ -> new UserInfoDialog(user).open());
            menuItem.getSubMenu().addItem("Logout", _ -> authenticationContext.logout());

            layout.add(userMenu);
        });
        return layout;
    }

    private Component createNavbar() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        HorizontalLayout header = new HorizontalLayout(toggle, viewTitle, createUserMenu());
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setFlexGrow(1, viewTitle);

        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        header.setWidthFull();
        return header;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        var annotation = getContent().getClass().getAnnotation(PageTitle.class);
        if (annotation == null) {
            viewTitle.setText("OnlyDrinks");
            return;
        }
        viewTitle.setText(annotation.value().split(" \\|")[0]);
    }

    private Component createHeader() {
        var appLogo = VaadinIcon.GLASS.create();
        appLogo.addClassNames(LumoUtility.TextColor.PRIMARY);
        appLogo.addClassNames(LumoUtility.Width.LARGE);
        appLogo.addClassNames(LumoUtility.Height.LARGE);

        var appName = new Span("OnlyDrinks");
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
        mainNav.setLabel("General");
        mainNav.setWidthFull();

        SideNav adminNav = new SideNav();
        adminNav.setLabel("Administration");
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
        return new SideNavItem(menuEntry.title(), menuEntry.path(), icon);
    }
}
