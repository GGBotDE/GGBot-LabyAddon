package de.ggbot.core.listener;

import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

public class MovementTest {
  public boolean isInMove = false;

  @Subscribe
  public void KeyEvent(KeyEvent e){
    if(!isInMove){
      return;
    }
    if(!Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()){
      if(e.state().name().equalsIgnoreCase("PRESS")){
        if(e.key().getTranslationKey().equalsIgnoreCase("W")){
          //TODO: Movement
        }
      }
    }
  }

}
