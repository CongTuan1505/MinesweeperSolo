package domin.ui;

import domin.SoundEffects;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/** Hooks actual button actions, including keyboard activation, once per component. */
public final class UiSounds implements AWTEventListener {
    private static final String WIRED = UiSounds.class.getName() + ".wired";
    private static boolean installed;
    private final Runnable click;

    UiSounds(Runnable click) { this.click = click; }

    public static synchronized void install() {
        if (installed) return;
        UiSounds listener = new UiSounds(() -> SoundEffects.shared().play(SoundEffects.Cue.CLICK));
        Toolkit.getDefaultToolkit().addAWTEventListener(listener,
                AWTEvent.CONTAINER_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
        for (Window window : Window.getWindows()) listener.attach(window);
        installed = true;
    }

    @Override public void eventDispatched(AWTEvent event) {
        if (event instanceof ContainerEvent && event.getID() == ContainerEvent.COMPONENT_ADDED)
            attach(((ContainerEvent)event).getChild());
        else if (event instanceof WindowEvent && event.getID() == WindowEvent.WINDOW_OPENED)
            attach(((WindowEvent)event).getWindow());
    }

    void attach(Component component) {
        if (component instanceof JComponent) {
            JComponent control = (JComponent)component;
            if (!Boolean.TRUE.equals(control.getClientProperty(WIRED))) {
                control.putClientProperty(WIRED, true);
                if (control instanceof AbstractButton && !(control instanceof SoundToggleButton)) {
                    AbstractButton button = (AbstractButton)control;
                    button.addActionListener(event -> {
                        // Combo arrows already have a popup cue. Never add a second one.
                        if (button.isEnabled() && SwingUtilities.getAncestorOfClass(JComboBox.class, button) == null)
                            click.run();
                    });
                } else if (control instanceof JComboBox<?>) {
                    JComboBox<?> combo = (JComboBox<?>)control;
                    boolean[] popup = {false};
                    combo.addPopupMenuListener(new PopupMenuListener() {
                        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                            popup[0] = true;
                            if (combo.isEnabled()) click.run();
                        }
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { popup[0] = false; }
                        public void popupMenuCanceled(PopupMenuEvent e) { popup[0] = false; }
                    });
                    combo.addItemListener(event -> {
                        if (event.getStateChange() == ItemEvent.SELECTED && combo.isEnabled()
                                && (popup[0] || combo.isFocusOwner())) click.run();
                    });
                }
            }
        }
        if (component instanceof Container)
            for (Component child : ((Container)component).getComponents()) attach(child);
    }
}
