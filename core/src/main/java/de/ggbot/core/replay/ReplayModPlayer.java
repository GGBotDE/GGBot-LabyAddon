package de.ggbot.core.replay;

import com.replaymod.replay.ReplayModReplay;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/** Starts replays in ReplayMod. Only loaded when ReplayMod is, see {@link ReplayModSupport}. */
final class ReplayModPlayer {

  private ReplayModPlayer() {}

  static void start(Path file) throws IOException {
    ReplayModReplay.instance.startReplay(file.toFile());
  }

  static void start(Map<String, byte[]> entries) throws IOException {
    ReplayModReplay.instance.startReplay(new MemoryReplayFile(entries));
  }
}
