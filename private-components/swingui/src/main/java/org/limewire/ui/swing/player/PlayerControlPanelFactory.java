package org.limewire.ui.swing.player;

import org.limewire.ui.swing.components.decorators.SliderBarDecorator;
import org.limewire.ui.swing.library.LibraryMediator;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class PlayerControlPanelFactory {
    private final Provider<PlayerMediator> playerMediator;
    private final SliderBarDecorator sliderBarDecorator;
    private final LibraryMediator libraryMediator;
    
    @Inject
    public PlayerControlPanelFactory(Provider<PlayerMediator> playerMediator,
                                     LibraryMediator libraryMediator,
                                     SliderBarDecorator sliderBarDecorator) {
        this.playerMediator = playerMediator;
        this.sliderBarDecorator = sliderBarDecorator;
        this.libraryMediator = libraryMediator;
    }

    public PlayerControlPanel createAudioControlPanel() {
        return new PlayerControlPanel(playerMediator, libraryMediator, sliderBarDecorator, true);
    }

    public PlayerControlPanel createVideoControlPanel() {
        return new PlayerControlPanel(playerMediator, libraryMediator, sliderBarDecorator, false);
    }
}
