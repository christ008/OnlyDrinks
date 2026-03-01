package uy.christian.base.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public interface MessageSupport {

    MessageSource getMessageSource();

    default String msg(String key, Object... args) {
        return getMessageSource().getMessage(key, args, resolveLocale());
    }

    private static Locale resolveLocale() {
        UI currentUi = UI.getCurrent();
        if (currentUi != null && currentUi.getLocale() != null) {
            return currentUi.getLocale();
        }
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && session.getLocale() != null) {
            return session.getLocale();
        }
        return LocaleContextHolder.getLocale();
    }
}
