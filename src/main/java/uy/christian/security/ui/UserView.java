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
import org.springframework.context.MessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import uy.christian.base.ui.FormValidation;
import uy.christian.base.ui.MessageSupport;
import uy.christian.base.ui.NotificationUtils;
import uy.christian.security.Role;
import uy.christian.security.User;
import uy.christian.security.UserService;

import java.util.Optional;

@Route("users")
@PageTitle("Users | OnlyDrinks")
@Menu(order = 4, icon = "vaadin:user-card", title = "User Management")
@RolesAllowed("ADMIN")
public class UserView extends VerticalLayout implements MessageSupport {

    private final transient UserService userService;
    private final transient AuthenticationContext authenticationContext;
    private final transient Validator validator;
    private final transient MessageSource messageSource;
    private final Grid<User> grid = new Grid<>(User.class, false);

    private final TextField username = new TextField();
    private final TextField name = new TextField();
    private final PasswordField password = new PasswordField();
    private final RadioButtonGroup<Role> role = new RadioButtonGroup<>();
    private final H3 formTitle = new H3();
    private final Span statusLabel = new Span();

    private final VerticalLayout form;
    private @Nullable User selectedUser;

    public UserView(UserService userService, AuthenticationContext authenticationContext, Validator validator, MessageSource messageSource) {
        this.userService = userService;
        this.authenticationContext = authenticationContext;
        this.validator = validator;
        this.messageSource = messageSource;
        setSizeFull();

        username.setLabel(msg("users.form.username"));
        name.setLabel(msg("users.form.name"));
        password.setLabel(msg("users.form.password"));
        role.setLabel(msg("users.form.role"));

        add(new H2(msg("users.title")));

        configureGrid();
        configureForm();

        Button addUserButton = new Button(msg("users.action.new"), e -> newUser());
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

    @Override
    public MessageSource getMessageSource() {
        return messageSource;
    }

    private void configureGrid() {
        grid.addColumn(User::getUsername).setHeader(msg("users.form.username"));
        grid.addColumn(User::getName).setHeader(msg("users.form.name"));
        grid.addColumn(user -> user.getRole().name()).setHeader(msg("users.form.role"));
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
            case ADMIN -> msg("users.role.admin");
            case MANAGER -> msg("users.role.manager");
            case BARMAN -> msg("users.role.barman");
            case USER -> msg("users.role.user");
        });

        role.setHelperText(msg("users.role.helper"));
        role.setTooltipText(msg("users.role.tooltip"));
        role.getTooltip().setPosition(Tooltip.TooltipPosition.TOP);
    }

    private VerticalLayout createForm() {
        formTitle.addClassNames(LumoUtility.Margin.Top.NONE);
        statusLabel.setVisible(false);
        statusLabel.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
        FormLayout formLayout = new FormLayout(username, name, password, role);
        
        Button save = new Button(msg("common.action.save"), e -> saveUser());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(msg("common.action.delete"), e -> deleteUser());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancel = new Button(msg("common.action.cancel"), e -> editUser(null));

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
            formTitle.setText(msg("users.form.select"));
            grid.asSingleSelect().clear();
            password.setHelperText(msg("users.password.helper.none"));
            password.setRequiredIndicatorVisible(false);
            username.clear();
            name.clear();
            role.clear();
        } else {
            selectedUser = user;
            form.setEnabled(true);
            form.removeClassNames("opacity-50");
            boolean isNew = user.getId() == null;
            formTitle.setText(isNew ? msg("users.form.new") : msg("users.form.edit"));
            password.setHelperText(isNew
                    ? msg("users.password.helper.new")
                    : msg("users.password.helper.edit"));
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
            NotificationUtils.showError(msg("users.error.self.demote"));
            return;
        }

        selectedUser.setUsername(formData.username());
        selectedUser.setName(formData.name());
        selectedUser.setRole(formData.role());

        if (formData.password() != null) {
            selectedUser.setHashedPassword(userService.encodePassword(formData.password()));
        } else if (selectedUser.getId() == null) {
            NotificationUtils.showError(msg("users.error.password.required"));
            return;
        }

        userService.save(selectedUser);
        refreshGrid();
        editUser(null);
        NotificationUtils.showSuccess(msg("users.notify.saved"));
    }

    private void deleteUser() {
        if (selectedUser != null && selectedUser.getId() != null) {
            boolean isCurrentUser = authenticationContext.getAuthenticatedUser(UserDetails.class)
                    .map(u -> u.getUsername().equals(selectedUser.getUsername()))
                    .orElse(false);

            if (isCurrentUser) {
                NotificationUtils.showError(msg("users.error.self.delete"));
                return;
            }

            ConfirmDialog dialog = getConfirmDialog();
            dialog.open();
        }
    }

    private ConfirmDialog getConfirmDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(msg("users.delete.header"));
        dialog.setText(msg("users.delete.text"));

        dialog.setCancelable(true);
        dialog.setConfirmText(msg("common.action.delete"));
        dialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());

        dialog.addConfirmListener(e -> {
            if (selectedUser != null && selectedUser.getId() != null) {
                userService.delete(selectedUser.getId());
                refreshGrid();
                editUser(null);
                NotificationUtils.showSuccess(msg("users.notify.deleted"));
            }
        });
        return dialog;
    }

    private static @Nullable String normalizeOptional(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
