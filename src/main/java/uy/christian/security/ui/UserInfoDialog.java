package uy.christian.security.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
import org.jspecify.annotations.NullMarked;
import uy.christian.security.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@NullMarked
public class UserInfoDialog extends Dialog {

    public UserInfoDialog(User user) {
        setHeaderTitle("User Information");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        //content.setSpacing(true);

        FormLayout userForm = getFormLayout(user);

        content.add(new H4("Account Details"), userForm);

        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            FormLayout sessionForm = new FormLayout();
            
            TextField sessionId = new TextField("Session ID");
            sessionId.setValue(session.getSession().getId());
            sessionId.setReadOnly(true);

            TextField lastAccessed = new TextField("Last Accessed");
            long time = session.getSession().getLastAccessedTime();
            String formattedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(time));
            lastAccessed.setValue(formattedTime);
            lastAccessed.setReadOnly(true);

            sessionForm.add(sessionId, lastAccessed);
            content.add(new H4("Session Information"), sessionForm);
        }

        add(content);

        Button closeButton = new Button("Close", _ -> close());
        getFooter().add(closeButton);
    }

    private static FormLayout getFormLayout(User user) {
        FormLayout userForm = new FormLayout();

        TextField username = new TextField("Username");
        username.setValue(user.getUsername());
        username.setReadOnly(true);

        TextField name = new TextField("Full Name");
        name.setValue(user.getName());
        name.setReadOnly(true);

        TextField roleField = new TextField("Role");
        roleField.setValue(user.getRole().name());
        roleField.setReadOnly(true);

        userForm.add(username, name, roleField);
        return userForm;
    }
}
