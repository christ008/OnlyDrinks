package uy.christian.security.ui;

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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Validator;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import uy.christian.base.ui.FormValidation;
import uy.christian.base.ui.NotificationUtils;
import uy.christian.base.ui.UiMessages;
import uy.christian.security.Role;
import uy.christian.security.User;
import uy.christian.security.UserService;

import java.util.Optional;

@Route("users")
@PageTitle("Users | OnlyDrinks")
@Menu(order = 4, icon = "vaadin:user-card", title = "User Management")
@RolesAllowed("ADMIN")
public class UserView extends VerticalLayout {

    private final transient UserService userService;
    private final transient AuthenticationContext authenticationContext;
    private final transient Validator validator;
    private final transient UiMessages uiMessages;
    private final Grid<User> grid = new Grid<>(User.class, false);

    private final TextField username = new TextField();
    private final TextField name = new TextField();
    private final PasswordField password = new PasswordField();
    private final RadioButtonGroup<Role> role = new RadioButtonGroup<>();
    private final H3 formTitle = new H3();
    private final Span statusLabel = new Span();

    private final VerticalLayout form;
    private @Nullable User selectedUser;

    public UserView(UserService userService, AuthenticationContext authenticationContext, Validator validator, UiMessages uiMessages) {
        this.userService = userService;
        this.authenticationContext = authenticationContext;
        this.validator = validator;
        this.uiMessages = uiMessages;
        setSizeFull();

        username.setLabel(uiMessages.get("users.form.username"));
        name.setLabel(uiMessages.get("users.form.name"));
        password.setLabel(uiMessages.get("users.form.password"));
        role.setLabel(uiMessages.get("users.form.role"));

        add(new H2(uiMessages.get("users.title")));

        configureGrid();
        configureForm();

        Button addUserButton = new Button(uiMessages.get("users.action.new"), e -> newUser());
        addUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        this.form = createForm();
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.setSizeFull();

        add(addUserButton, content);

        refreshGrid();
        editUser(null);
    }

    private void configureGrid() {
        grid.addColumn(User::getUsername).setHeader(uiMessages.get("users.form.username"));
        grid.addColumn(User::getName).setHeader(uiMessages.get("users.form.name"));
        grid.addColumn(user -> user.getRole().name()).setHeader(uiMessages.get("users.form.role"));
        grid.setSizeFull();

        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.isFromClient()) {
                editUser(event.getValue());
            }
        });
    }

    private void configureForm() {
        role.setItems(Role.values());
        role.setItemLabelGenerator(r -> switch (r) {
            case ADMIN -> uiMessages.get("users.role.admin");
            case MANAGER -> uiMessages.get("users.role.manager");
            case BARMAN -> uiMessages.get("users.role.barman");
            case USER -> uiMessages.get("users.role.user");
        });

        role.setHelperText(uiMessages.get("users.role.helper"));
        role.setTooltipText(uiMessages.get("users.role.tooltip"));
        role.getTooltip().setPosition(Tooltip.TooltipPosition.TOP);
    }

    private VerticalLayout createForm() {
        formTitle.addClassNames(LumoUtility.Margin.Top.NONE);
        statusLabel.setVisible(false);
        statusLabel.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        FormLayout formLayout = new FormLayout(username, name, password, role);
        
        Button save = new Button(uiMessages.get("common.action.save"), e -> saveUser());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(uiMessages.get("common.action.delete"), e -> deleteUser());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancel = new Button(uiMessages.get("common.action.cancel"), e -> editUser(null));

        HorizontalLayout buttons = new HorizontalLayout(save, delete, cancel);
        
        VerticalLayout layout = new VerticalLayout(formTitle, statusLabel, formLayout, buttons);
        layout.setWidth("350px");
        return layout;
    }

    private void refreshGrid() {
        grid.setItems(userService.findAll());
    }

    private void newUser() {
        grid.asSingleSelect().clear();
        editUser(new User());
    }

    private void editUser(@Nullable User user) {
        if (user == null) {
            selectedUser = null;
            form.setEnabled(false);
            form.addClassNames("opacity-50");
            formTitle.setText(uiMessages.get("users.form.select"));
            grid.asSingleSelect().clear();
            password.setHelperText(uiMessages.get("users.password.helper.none"));
            password.setRequiredIndicatorVisible(false);
            username.clear();
            name.clear();
            role.clear();
        } else {
            selectedUser = user;
            form.setEnabled(true);
            form.removeClassNames("opacity-50");
            boolean isNew = user.getId() == null;
            formTitle.setText(isNew ? uiMessages.get("users.form.new") : uiMessages.get("users.form.edit"));
            password.setHelperText(isNew
                    ? uiMessages.get("users.password.helper.new")
                    : uiMessages.get("users.password.helper.edit"));
            password.setRequiredIndicatorVisible(isNew);
            username.setValue(user.getUsername());
            name.setValue(user.getName());
            role.setValue(user.getRole());
        }
        form.setVisible(true);
        statusLabel.setVisible(false);
        password.clear();
    }

    private void saveUser() {
        if (selectedUser == null) {
            return;
        }

        UserForm formData = new UserForm(
                username.getValue().trim(),
                name.getValue().trim(),
                role.getValue(),
                normalizeOptional(password.getValue())
        );
        Optional<String> validationMessage = FormValidation.firstViolationMessage(validator, formData);
        if (validationMessage.isPresent()) {
            statusLabel.setText(validationMessage.get());
            statusLabel.setVisible(true);
            return;
        }
        statusLabel.setVisible(false);

        boolean isCurrentUser = authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(u -> u.getUsername().equals(formData.username()))
                .orElse(false);

        if (isCurrentUser && formData.role() != Role.ADMIN) {
            NotificationUtils.showError(uiMessages.get("users.error.self.demote"));
            return;
        }

        selectedUser.setUsername(formData.username());
        selectedUser.setName(formData.name());
        selectedUser.setRole(formData.role());

        if (formData.password() != null) {
            selectedUser.setHashedPassword(userService.encodePassword(formData.password()));
        } else if (selectedUser.getId() == null) {
            NotificationUtils.showError(uiMessages.get("users.error.password.required"));
            return;
        }

        userService.save(selectedUser);
        refreshGrid();
        editUser(null);
        NotificationUtils.showSuccess(uiMessages.get("users.notify.saved"));
    }

    private void deleteUser() {
        if (selectedUser != null && selectedUser.getId() != null) {
            boolean isCurrentUser = authenticationContext.getAuthenticatedUser(UserDetails.class)
                    .map(u -> u.getUsername().equals(selectedUser.getUsername()))
                    .orElse(false);

            if (isCurrentUser) {
                NotificationUtils.showError(uiMessages.get("users.error.self.delete"));
                return;
            }

            ConfirmDialog dialog = getConfirmDialog();
            dialog.open();
        }
    }

    private ConfirmDialog getConfirmDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(uiMessages.get("users.delete.header"));
        dialog.setText(uiMessages.get("users.delete.text"));

        dialog.setCancelable(true);
        dialog.setConfirmText(uiMessages.get("common.action.delete"));
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());

        dialog.addConfirmListener(e -> {
            if (selectedUser != null && selectedUser.getId() != null) {
                userService.delete(selectedUser.getId());
                refreshGrid();
                editUser(null);
                NotificationUtils.showSuccess(uiMessages.get("users.notify.deleted"));
            }
        });
        return dialog;
    }

    private static @Nullable String normalizeOptional(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
