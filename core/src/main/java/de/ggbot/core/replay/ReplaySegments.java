package de.ggbot.core.replay;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Reads the packet log out of a segment of the GGBot replay storage format 2
 * (replay-core segments/format.ts): a header, compressed blocks of whole
 * ReplayMod records, an index and a trailer with a checksum.
 */
public final class ReplaySegments {

  private static final int SEGMENT_MAGIC = 0x47475253;
  private static final int BLOCK_MAGIC = 0x4747424b;
  private static final int TRAILER_MAGIC = 0x47475245;
  private static final int FORMAT_VERSION = 2;
  private static final int HEADER_SIZE = 56;
  private static final int BLOCK_HEADER_SIZE = 16;
  private static final int INDEX_ENTRY_SIZE = 20;
  private static final int TRAILER_SIZE = 8;
  private static final int CODEC_STORED = 0;
  private static final int CODEC_DEFLATE_RAW = 1;

  private ReplaySegments() {}

  /**
   * Writes a segment's records sequentially.
   *
   * @param segment the full segment
   * @param out     receives the records
   * @throws IOException if the segment is corrupted or is not a segment
   */
  public static void writeRecords(byte[] segment, OutputStream out) throws IOException {
    ByteBuffer buffer = ByteBuffer.wrap(segment);

    if (segment.length < HEADER_SIZE + TRAILER_SIZE || buffer.getInt(0) != SEGMENT_MAGIC) {
      throw new IOException("Not a replay segment");
    }

    int version = Short.toUnsignedInt(buffer.getShort(4));
    if (version != FORMAT_VERSION) throw new IOException("Unsupported segment version " + version);

    int blockCount = buffer.getInt(12);
    int indexOffset = buffer.getInt(40);
    int eventsOffset = buffer.getInt(48);
    long eventsLength = Integer.toUnsignedLong(buffer.getInt(eventsOffset + 5));
    long size = (long) eventsOffset + ReplayEvents.SECTION_HEADER_SIZE + eventsLength
        + TRAILER_SIZE;
    if (size > segment.length) throw new IOException("Replay segment is damaged");

    int checked = (int) size - TRAILER_SIZE;
    long checksum = Integer.toUnsignedLong(buffer.getInt(checked));
    if (buffer.getInt(checked + 4) != TRAILER_MAGIC || checksum != crc(segment, 0, checked)) {
      throw new IOException("Replay segment is damaged");
    }

    for (int i = 0; i < blockCount; i++) {
      int entry = indexOffset + i * INDEX_ENTRY_SIZE;
      int offset = buffer.getInt(entry);
      int dataLength = buffer.getInt(entry + 4);
      if (buffer.getInt(offset) != BLOCK_MAGIC) throw new IOException("Replay block is damaged");

      int codec = segment[offset + 4];
      int rawLength = buffer.getInt(offset + 8);
      long expected = Integer.toUnsignedLong(buffer.getInt(offset + 12));
      byte[] raw = decode(codec, segment, offset + BLOCK_HEADER_SIZE, dataLength, rawLength);
      if (crc(raw, 0, raw.length) != expected) throw new IOException("Replay block is damaged");

      out.write(raw);
    }
  }

  private static byte[] decode(int codec, byte[] data, int offset, int length, int rawLength)
      throws IOException {
    if (codec == CODEC_STORED) {
      if (length != rawLength) throw new IOException("Replay block is damaged");

      byte[] raw = new byte[length];
      System.arraycopy(data, offset, raw, 0, length);
      return raw;
    }

    if (codec != CODEC_DEFLATE_RAW) throw new IOException("Unknown replay block codec " + codec);

    return inflate(data, offset, length, rawLength);
  }

  /**
   * Decompresses raw deflate data whose size is known, matching how the replay storage
   * compresses it.
   *
   * @return the data, exactly {@code rawLength} bytes
   * @throws IOException if it is corrupted or has a different size
   */
  static byte[] inflate(byte[] data, int offset, int length, int rawLength) throws IOException {
    Inflater inflater = new Inflater(true);

    try {
      inflater.setInput(data, offset, length);
      byte[] raw = new byte[rawLength];
      int read = 0;

      while (read < rawLength) {
        int n = inflater.inflate(raw, read, rawLength - read);
        if (n == 0 && (inflater.finished() || inflater.needsInput())) break;

        read += n;
      }

      if (read != rawLength) throw new IOException("Replay data is damaged");
      return raw;
    } catch (DataFormatException e) {
      throw new IOException("Replay data is damaged", e);
    } finally {
      inflater.end();
    }
  }

  private static long crc(byte[] data, int offset, int length) {
    CRC32 crc = new CRC32();
    crc.update(data, offset, length);
    return crc.getValue();
  }
}
