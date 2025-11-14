package org.example.bot.commands.fuentes.AgregarPdi;

import org.example.bot.clients.FuentesClient;
import org.example.bot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

public class AgregarPdiCommand implements BotCommand {

  private final FuentesClient fuentesClient = new FuentesClient();
  private final Map<Long, ConversacionPdi> conversaciones = new HashMap<>();

  @Override
  public boolean matches(String text) {
    return text.startsWith("/agregarpdi");
  }

  @Override
  public boolean hasConversation(Long chatId) {
    return conversaciones.containsKey(chatId);
  }

  @Override
  public SendMessage handle(Update update) throws Exception {
    Long chatId = update.getMessage().getChatId();
    String text = update.getMessage().getText().trim();
    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());

    // Si ya hay una conversación en curso → continuarla
    if (conversaciones.containsKey(chatId) && !text.startsWith("/agregarpdi")) {
      return manejarConversacion(chatId, text);
    }

    // Inicio de flujo
    String[] parts = text.split(" ", 2);
    if (parts.length < 2) {
      msg.setText("Uso: /agregarpdi <id_hecho>");
      return msg;
    }

    ConversacionPdi conv = new ConversacionPdi();
    conv.hechoId = parts[1];
    conv.pasoActual = ConversacionPdi.Paso.DESCRIPCION;
    conversaciones.put(chatId, conv);

    msg.setText("🧩 Iniciando carga de PDI para hecho *" + conv.hechoId + "*.\nPor favor, ingresá la descripción:");
    return msg;
  }

  private SendMessage manejarConversacion(Long chatId, String text) {
    ConversacionPdi conv = conversaciones.get(chatId);
    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());
    msg.enableHtml(true);

    try {
      switch (conv.pasoActual) {

        // 1) DESCRIPCIÓN
        case DESCRIPCION -> {
          conv.datos.put("descripcion", text.trim());
          conv.pasoActual = ConversacionPdi.Paso.LUGAR;

          msg.setText("""
              📝 <b>Descripción registrada.</b>
              Ahora ingresá el <b>lugar</b> del PDI:
              """);
        }

        // 2) LUGAR
        case LUGAR -> {
          conv.datos.put("lugar", text.trim());
          conv.pasoActual = ConversacionPdi.Paso.MOMENTO;

          msg.setText("""
              🕒 Ingresá el <b>momento</b> del PDI.
              Formato recomendado:
              <code>2025-09-28T13:00:00</code>
              """);
        }

        // 3) MOMENTO
        case MOMENTO -> {
          conv.datos.put("momento", text.trim());
          conv.pasoActual = ConversacionPdi.Paso.CONTENIDO;

          msg.setText("""
              🧾 Ingresá el <b>contenido</b> del PDI:
              """);
        }

        // 4) CONTENIDO
        case CONTENIDO -> {
          conv.datos.put("contenido", text.trim());
          conv.pasoActual = ConversacionPdi.Paso.IMAGE_URL;

          msg.setText("""
              🖼️ Ingresá la <b>URL de la imagen</b> del PDI
              (o escribí <code>ninguna</code> si no tiene):
              """);
        }

        // 5) IMAGE_URL → crear PDI
        case IMAGE_URL -> {
          String imageUrl = text.trim();
          if (imageUrl.equalsIgnoreCase("ninguna")) imageUrl = "";
          conv.datos.put("image_url", imageUrl);

          // JSON final para enviar al servidor
          String jsonBody = String.format("""
                  {
                    "hecho_id": "%s",
                    "descripcion": "%s",
                    "lugar": "%s",
                    "momento": "%s",
                    "contenido": "%s",
                    "image_url": "%s"
                  }
                  """,
              conv.hechoId,
              conv.datos.get("descripcion"),
              conv.datos.get("lugar"),
              conv.datos.get("momento"),
              conv.datos.get("contenido"),
              conv.datos.get("image_url")
          );

          String respuestaApi = fuentesClient.agregarPdi(conv.hechoId, jsonBody);

          // parsear json
          org.json.JSONObject json = new org.json.JSONObject(respuestaApi);
          String pdiId = json.optString("id", "desconocido");

          String imagen = imageUrl.isBlank()
              ? "❌ <i>Sin imagen adjunta</i>"
              : "<a href=\"" + imageUrl + "\">🖼️ Ver imagen</a>";

          // Respuesta linda
          msg.setText("""
              ✅ <b>PDI agregado correctamente</b>

              🔗 <b>Hecho asociado:</b> <code>%s</code>
              🆔 <b>ID del PDI:</b> <code>%s</code>

              📝 <b>Descripción:</b> %s
              📍 <b>Lugar:</b> %s
              🕒 <b>Momento:</b> %s
              🧾 <b>Contenido:</b> %s
              🖼️ <b>Imagen:</b> %s

              ✔ El PDI fue registrado correctamente en el sistema.
              """.formatted(
              conv.hechoId,
              pdiId,
              conv.datos.get("descripcion"),
              conv.datos.get("lugar"),
              conv.datos.get("momento"),
              conv.datos.get("contenido"),
              imagen
          ));

          conversaciones.remove(chatId);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      msg.setText("❌ Ocurrió un error al procesar el PDI. Intentá nuevamente.");
    }

    return msg;
  }

}
