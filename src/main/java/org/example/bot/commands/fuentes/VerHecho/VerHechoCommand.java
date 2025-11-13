package org.example.bot.commands.fuentes.VerHecho;

import org.example.bot.clients.FuentesClient;
import org.example.bot.clients.ProcesadorPdiClient;
import org.example.bot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.json.JSONArray;
import org.json.JSONObject;

public class VerHechoCommand implements BotCommand {

  private final FuentesClient fuentesClient = new FuentesClient();
  private final ProcesadorPdiClient procesadorPdiClient = new ProcesadorPdiClient();

  @Override
  public boolean matches(String text) {
    return text.startsWith("/ver");
  }

  @Override
  public SendMessage handle(Update update) throws Exception {
    String[] parts = update.getMessage().getText().split(" ", 2);
    SendMessage msg = new SendMessage();
    msg.setChatId(update.getMessage().getChatId().toString());

    if (parts.length < 2) {
      msg.setText("Uso: /ver <id_hecho>");
      return msg;
    }

    String hechoId = parts[1];

    // 1️⃣ Llamada al servicio de Fuentes
    String hechoData = fuentesClient.verHecho(hechoId);

    // 2️⃣ Llamada al ProcesadorPdI
    String pdisData = procesadorPdiClient.obtenerPdisPorHecho(hechoId);

    // 3️⃣ Formatear ambas respuestas
    String respuesta = formatearRespuesta(hechoData, pdisData);
    msg.setText(respuesta);
    msg.enableHtml(true); // para que se vea más lindo con negritas, etc.

    return msg;
  }

  private String formatearRespuesta(String hechoData, String pdisData) {
    try {
      JSONObject hecho = new JSONObject(hechoData);
      StringBuilder sb = new StringBuilder();

      sb.append("🧾 <b>Detalles del Hecho</b>\n")
          .append("🆔 <b>ID:</b> ").append(hecho.optString("id", "(sin id)")).append("\n")
          .append("📌 <b>Título:</b> ").append(hecho.optString("titulo", "(sin título)")).append("\n")
          .append("🏷️ <b>Categoría:</b> ").append(hecho.optString("categoria", "(sin categoría)")).append("\n")
          .append("📍 <b>Ubicación:</b> ").append(hecho.optString("ubicacion", "(sin ubicación)")).append("\n")
          .append("📅 <b>Fecha:</b> ").append(hecho.optString("fecha", "(sin fecha)")).append("\n")
          .append("🧩 <b>Origen:</b> ").append(hecho.optString("origen", "(sin origen)")).append("\n\n");

      // Mostrar los PDIs asociados
      sb.append("📷 <b>PDIs asociados:</b>\n");

      JSONArray pdisArray = new JSONArray(pdisData);
      if (pdisArray.isEmpty()) {
        sb.append("❌ No hay PDIs asociados a este hecho.");
      } else {
        for (int i = 0; i < pdisArray.length(); i++) {
          JSONObject pdi = pdisArray.getJSONObject(i);
          sb.append("— ").append(pdi.optString("descripcion", "(sin descripción)")).append("\n");
          sb.append("📍 ").append(pdi.optString("lugar", "(sin lugar)")).append("\n");
          sb.append("🕒 ").append(pdi.optString("momento", "(sin momento)")).append("\n");

          // Mostrar URL de imagen si existe
          String imgUrl = pdi.optString("image_url", "");
          if (!imgUrl.isBlank()) {
            sb.append("🖼️ <a href=\"").append(imgUrl).append("\">Ver imagen</a>\n");
          }
          sb.append("\n");
        }
      }

      return sb.toString();
    } catch (Exception e) {
      System.out.println("Error al procesar los datos del hecho o sus PDIs: " + e.getMessage());
      return "⚠️ Error al procesar los datos del hecho o sus PDIs.\nHecho:\n" + hechoData + "\n\nPDIs:\n" + pdisData;
    }
  }
}
