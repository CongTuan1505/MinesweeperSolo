package domin.ui;

import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.event.*;

/** Button event wiring tests with no real audio or preferences writes. */
public class TestUiSounds {
    static int checks;
    static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message);checks++; }
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger count=new AtomicInteger();
            UiSounds sounds=new UiSounds(count::incrementAndGet);
            JPanel root=new JPanel();
            JButton button=new JButton("Play");
            JToggleButton toggle=new JToggleButton("Mode");
            JCheckBox radar=new JCheckBox("Radar");
            JMenuItem menu=new JMenuItem("Logout");
            JComboBox<String> combo=new JComboBox<>(new String[]{"Basic","Advanced","Expert"});
            root.add(button);root.add(toggle);root.add(radar);root.add(menu);root.add(combo);
            sounds.attach(root);sounds.attach(root);
            button.doClick(0);check(count.get()==1,"One sound per button action despite repeated attach");
            toggle.doClick(0);radar.doClick(0);menu.doClick(0);
            check(count.get()==4,"Toggle, checkbox and menu items covered");
            button.setEnabled(false);button.doClick(0);
            check(count.get()==4,"Disabled button silent");
            button.setEnabled(true);
            Action keyAction=button.getActionMap().get("pressed");
            keyAction.actionPerformed(new ActionEvent(button,ActionEvent.ACTION_PERFORMED,"pressed"));
            button.getActionMap().get("released").actionPerformed(new ActionEvent(button,ActionEvent.ACTION_PERFORMED,"released"));
            check(count.get()==5,"Keyboard activation plays a click");
            combo.setSelectedIndex(1);check(count.get()==5,"Programmatic combo refresh silent");
            PopupMenuEvent popup=new PopupMenuEvent(combo);
            for(PopupMenuListener listener:combo.getPopupMenuListeners())listener.popupMenuWillBecomeVisible(popup);
            check(count.get()==6,"Opening mode picker plays click");
            combo.setSelectedIndex(2);check(count.get()==7,"Selecting a mode plays click");
            for(PopupMenuListener listener:combo.getPopupMenuListeners())listener.popupMenuWillBecomeInvisible(popup);
            combo.setSelectedIndex(0);check(count.get()==7,"Closed, unfocused mode picker silent");
            JButton dynamic=new JButton("Dialog confirm");
            sounds.eventDispatched(new ContainerEvent(root,ContainerEvent.COMPONENT_ADDED,dynamic));
            dynamic.doClick(0);check(count.get()==8,"Dynamically created dialog controls covered");
            JButton arrow=new JButton("Arrow");combo.add(arrow);
            sounds.attach(arrow);arrow.doClick(0);
            check(count.get()==8,"Combo arrow does not duplicate popup sound");
        });
        System.out.println("PASS: "+checks+" UI click wiring checks.");
    }
}
