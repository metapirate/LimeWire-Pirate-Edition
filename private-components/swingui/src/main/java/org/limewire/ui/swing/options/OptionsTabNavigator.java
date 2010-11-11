package org.limewire.ui.swing.options;

import com.google.inject.Provider;


public interface OptionsTabNavigator {

    OptionTabItem addOptionTab(String title, OptionsTabNavigator navigator, Provider<? extends OptionPanel> optionProvider);
    
    void select(String title);
}
