package org.example.bot.commands.busquedas;

import org.example.bot.commands.BotCommand;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class BusquedasCommand implements BotCommand {

  private static final String BASE_URL = "https://two025-tp-entrega-2-lgo1980.onrender.com/api/fuentes/busqueda";

  @Override
  public SendMessage handle(Update update) {
    String text = update.getMessage().getText().trim();
    Long chatId = update.getMessage().getChatId();

    String[] parts = text.replace("/busqueda", "").trim().split("\\s+");
    String params = String.join("&", parts);
    String url = BASE_URL + "?" + params;

    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String body = response.body();

      JSONArray resultados = new JSONArray(body);
      if (resultados.isEmpty()) {
        msg.setText("⚠️ No se encontraron resultados para esa búsqueda.");
        return msg;
      }

      StringBuilder sb = new StringBuilder("🔎 *Resultados de búsqueda:*\n\n");
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

      for (int i = 0; i < resultados.length(); i++) {
        JSONObject h = resultados.getJSONObject(i);

        String id = h.optString("id", "-5");
        String nombreColeccion = h.optString("nombreColeccion", "N/D");
        String titulo = h.optString("titulo", "Sin título");
        String categoria = h.optString("categoria", "N/D");
        String ubicacion = h.optString("ubicacion", "N/D");
        String origen = h.optString("origen", "N/D");
        String fechaStr = h.optString("fecha", null);

        // ---- Etiquetas compactas tipo 🔹[tag] ----
        String etiquetasStr;
        if (h.has("etiquetas") && !h.isNull("etiquetas")) {
          JSONArray etiquetas = h.getJSONArray("etiquetas");

          if (etiquetas.isEmpty()) {
            etiquetasStr = "Ninguna";
          } else {
            etiquetasStr = etiquetas.toList().stream()
                .map(e -> "🔹[" + e.toString() + "]")
                .collect(Collectors.joining(" "));
          }
        } else {
          etiquetasStr = "Ninguna";
        }

        // ---- Fecha ----
        String fechaFormateada = "N/D";
        if (fechaStr != null && !fechaStr.isEmpty() && !"null".equalsIgnoreCase(fechaStr)) {
          try {
            LocalDateTime fecha = LocalDateTime.parse(fechaStr);
            fechaFormateada = fecha.format(formatter);
          } catch (Exception ignored) {
          }
        }

        // ---- Tarjeta sin bloques de color ----
        sb.append("🆔 *ID:* ").append(id).append("\n")
            .append("📰 *Título:* ").append(titulo).append("\n")
            .append("📚 *Colección:* ").append(nombreColeccion).append("\n")
            .append("🏷️ *Categoría:* ").append(categoria).append("\n")
            .append("📍 *Ubicación:* ").append(ubicacion).append("\n")
            .append("🗓️ *Fecha:* ").append(fechaFormateada).append("\n")
            .append("🔖 *Etiquetas:* ").append(etiquetasStr).append("\n")
            .append("🌐 *Origen:* ").append(origen).append("\n\n");

        // Separador entre registros (sin color)
        if (i < resultados.length() - 1) {
          sb.append("━━━━━━━━━━━━━━━━━━\n\n");
        } else {
          sb.append("\n"); // último sin línea final
        }
      }


      msg.setParseMode("Markdown");
      msg.setText(sb.toString());

    } catch (Exception e) {
      msg.setText("⚠️ Error al realizar la búsqueda");
      System.out.println("Error al realizar la búsqueda: " + e.getMessage());
    }

    return msg;
  }

  @Override
  public boolean matches(String input) {
    return input.startsWith("/busqueda");
  }
}