package uy.christian.base.ui;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationUtils {

    public static void showSuccess(String message) {
        show(message, NotificationVariant.LUMO_SUCCESS);
    }

    public static void showError(String message) {
        show(message, NotificationVariant.LUMO_ERROR);
    }

    public static void showWarning(String message) {
        show(message, NotificationVariant.LUMO_WARNING);
    }

    private static void show(String message, NotificationVariant variant) {
        Notification notification = new Notification();
        notification.addThemeVariants(variant);
        notification.setPosition(Notification.Position.BOTTOM_CENTER);
        notification.setDuration(5000);

        Text text = new Text(message);
        
        Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> notification.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        closeButton.setAriaLabel("Close");

        HorizontalLayout layout = new HorizontalLayout(text, closeButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        
        notification.add(layout);
        notification.open();
    }
}
