package org.example.bot.commands.agregador;

import org.example.bot.clients.AgregadorClient;
import org.example.bot.commands.BotCommand;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public class ListarHechosCommand implements BotCommand {

  private final AgregadorClient agregadorClient = new AgregadorClient();

  @Override
  public boolean matches(String text) {
    return text.startsWith("/listar");
  }

  @Override
  public SendMessage handle(Update update) throws Exception {
    Long chatId = update.getMessage().getChatId();
    String text = update.getMessage().getText().trim();

    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());
    msg.enableHtml(true);

    String[] parts = text.split(" ", 2);
    if (parts.length < 2) {
      msg.setText("⚠️ Uso: <b>/listar &lt;nombre_coleccion&gt;</b>\n\nEjemplo: <code>/listar delitos</code>");
      return msg;
    }

    String coleccion = parts[1];

    try {
      String respuesta = agregadorClient.listarHechos(coleccion);

      JSONArray hechos = new JSONArray(respuesta);
      if (hechos.isEmpty()) {
        msg.setText("❕ No se encontraron hechos en la colección <b>" + coleccion + "</b>.");
        return msg;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("📋 <b>Hechos en la colección:</b> ").append(coleccion).append("\n");
      sb.append("──────────────────────────────\n\n");

      for (int i = 0; i < hechos.length(); i++) {
        JSONObject h = hechos.getJSONObject(i);

        String titulo = h.optString("titulo", "—");
        String categoria = h.optString("categoria", "—");
        String ubicacion = h.optString("ubicacion", "—");
        String origen = h.optString("origen", "—");
        String fecha = h.optString("fecha", "—");
        JSONArray etiquetasArray = h.optJSONArray("etiquetas");

        String etiquetas = (etiquetasArray != null && !etiquetasArray.isEmpty())
            ? String.join(", ", etiquetasArray.toList().stream().map(Object::toString).toList())
            : "—";

        sb.append("🟦 <b>").append(titulo).append("</b>\n");
        sb.append("🏷️ Categoría: <code>").append(categoria).append("</code>\n");
        sb.append("📍 Ubicación: ").append(ubicacion).append("\n");
        sb.append("🕓 Fecha: ").append(fecha).append("\n");
        sb.append("🌐 Origen: ").append(origen).append("\n");
        sb.append("🔖 Etiquetas: ").append(etiquetas).append("\n");
        sb.append("──────────────────────────────\n\n");
      }

      msg.setText(sb.toString());

    } catch (Exception e) {
      msg.setText("❌ Error al obtener los hechos de la colección <b>" );
      System.out.println("Error al obtener los hechos de la colección: " + coleccion + " " + e.getMessage());
    }

    return msg;
  }
}