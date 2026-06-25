package de.ggbot.core.api;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.ggbot.sdk.core.JSON;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Makes the generated GGBot SDK tolerant of API drift.
 *
 * <p>The OpenAPI-generated models do strict validation: each model registers a
 * {@code CustomTypeAdapterFactory} in {@link JSON} that throws when the JSON
 * contains a field not declared in the model (e.g. a newly added {@code available}
 * field on {@code Module}). Because the model classes are <em>not</em> annotated
 * with {@code @JsonAdapter}, that strictness lives entirely in the shared Gson
 * instance.
 *
 * <p>This replaces that Gson with one built from {@link JSON#createGson()} (the
 * GsonFire base, without the strict per-model factories) plus the date adapters
 * the models need. Reflection-based (de)serialisation then simply ignores unknown
 * fields, so the SDK keeps working when the backend adds properties.
 */
public final class SdkLeniency {

  private static boolean applied = false;

  private SdkLeniency() {}

  /** Installs the lenient Gson into the SDK. Safe to call multiple times. */
  public static synchronized void apply() {
    if (applied) return;
    applied = true;

    GsonBuilder builder = JSON.createGson();
    builder.registerTypeAdapter(OffsetDateTime.class, new TypeAdapter<OffsetDateTime>() {
      @Override
      public void write(JsonWriter out, OffsetDateTime value) throws IOException {
        out.value(value == null ? null : value.toString());
      }

      @Override
      public OffsetDateTime read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        try {
          return OffsetDateTime.parse(in.nextString());
        } catch (Exception e) {
          return null;
        }
      }
    }.nullSafe());

    builder.registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
      @Override
      public void write(JsonWriter out, LocalDate value) throws IOException {
        out.value(value == null ? null : value.toString());
      }

      @Override
      public LocalDate read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        try {
          return LocalDate.parse(in.nextString());
        } catch (Exception e) {
          return null;
        }
      }
    }.nullSafe());

    // The backend sometimes sends decimals where the generated models declare
    // integral types (e.g. a Long `price` of 12.4). Plain Gson throws on that, so
    // install tolerant Long/Integer adapters that accept any numeric string.
    builder.registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
      @Override
      public void write(JsonWriter out, Long value) throws IOException {
        if (value == null) out.nullValue(); else out.value(value);
      }

      @Override
      public Long read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        String s = in.nextString();
        try {
          return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
          try {
            return (long) Math.floor(Double.parseDouble(s.trim()));
          } catch (NumberFormatException e2) {
            return null;
          }
        }
      }
    }.nullSafe());

    builder.registerTypeAdapter(Integer.class, new TypeAdapter<Integer>() {
      @Override
      public void write(JsonWriter out, Integer value) throws IOException {
        if (value == null) out.nullValue(); else out.value(value);
      }

      @Override
      public Integer read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
          in.nextNull();
          return null;
        }
        String s = in.nextString();
        try {
          return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
          try {
            return (int) Math.floor(Double.parseDouble(s.trim()));
          } catch (NumberFormatException e2) {
            return null;
          }
        }
      }
    }.nullSafe());

    JSON.setGson(builder.create());
  }
}
