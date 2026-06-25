package de.ggbot.core.utils;

import java.util.HashSet;
import java.util.Set;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.client.input.KeyEvent;

/**
 * Rising-edge detector for a multi-key hotkey combo.
 *
 * <p>Fixes the "toggle" bug where, after a screen opened by the combo grabs input
 * focus, the key-release events never reach the world key handler and a naive
 * "all keys pressed" check (based on the global {@link Key#isPressed()} state) then
 * re-fires when only a single combo key is pressed.
 *
 * <p>To be robust against that stale global state, this trigger tracks its own
 * pressed-key set purely from the events it observes and fires only once per full
 * combo press (rising edge), re-arming when any combo key is released. Callers
 * should additionally {@link #reset()} it whenever a screen is open (the global key
 * state can no longer be trusted there), so a fresh full combo press is required
 * afterwards.
 */
public class KeyComboTrigger {

  /** Keys we have seen pressed (and not yet released) - our own, trustworthy state. */
  private final Set<Key> pressed = new HashSet<>();
  private boolean triggered = false;

  /**
   * Feeds a key event and returns {@code true} exactly once when the full combo
   * transitions to "all pressed".
   *
   * @param e     the key event
   * @param combo the configured combo keys
   * @return {@code true} if the combo just completed and should fire
   */
  public boolean test(KeyEvent e, Key[] combo) {
    if (combo == null || combo.length == 0) {
      return false;
    }

    Key key = e.key();
    boolean inCombo = false;
    for (Key k : combo) {
      if (key.equals(k)) {
        inCombo = true;
        break;
      }
    }
    if (!inCombo) {
      return false;
    }

    boolean isPress = e.state().name().equalsIgnoreCase("PRESS");
    if (!isPress) {
      // Any release of a combo key re-arms the trigger.
      pressed.remove(key);
      triggered = false;
      return false;
    }

    pressed.add(key);
    for (Key k : combo) {
      if (!pressed.contains(k)) {
        return false;
      }
    }
    if (triggered) {
      return false;
    }
    triggered = true;
    return true;
  }

  /** Clears all tracked state so a fresh full combo press is required to fire. */
  public void reset() {
    pressed.clear();
    triggered = false;
  }
}
