package de.ggbot.core.listener;

import de.ggbot.core.GGBot;
import de.ggbot.core.gui.onboarding.OnboardingActivity;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/**
 * Shows the first-time setup guide once: shortly after the user joins a server, or
 * right away if the addon is (re)loaded while the user is already on a server. After
 * it has been completed (or cancelled) the persistent flag prevents it from
 * reappearing; it can be reopened from the settings at any time.
 *
 * <p>The display is delayed so it runs after the world has finished loading,
 * otherwise the loading sequence would immediately close the screen.
 */
public class OnboardingListener {

  private static final long SHOW_DELAY_MS = 2500L;

  private final GGBot addon;

  public OnboardingListener(GGBot addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onJoin(ServerJoinEvent event) {
    showGuideDelayed();
  }

  /** Shows the guide now (delayed) if it is still pending and a server is connected. */
  public void showIfPendingOnServer() {
    if (Laby.labyAPI().serverController().getCurrentServerData() == null) {
      return;
    }
    showGuideDelayed();
  }

  private void showGuideDelayed() {
    if (Boolean.TRUE.equals(addon.configuration().generalSub.onboardingCompleted.get())) {
      return;
    }
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(SHOW_DELAY_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      if (Boolean.TRUE.equals(addon.configuration().generalSub.onboardingCompleted.get())) {
        return;
      }
      Laby.labyAPI().minecraft().executeOnRenderThread(() ->
          Laby.labyAPI().minecraft().minecraftWindow().displayScreen(new OnboardingActivity()));
    }, "ggbot-onboarding");
    thread.setDaemon(true);
    thread.start();
  }
}
