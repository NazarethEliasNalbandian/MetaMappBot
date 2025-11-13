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

    try {
      switch (conv.pasoActual) {
        case DESCRIPCION -> {
          conv.datos.put("descripcion", text);
          conv.pasoActual = ConversacionPdi.Paso.LUGAR;
          msg.setText("📍 Ingresá el lugar:");
        }
        case LUGAR -> {
          conv.datos.put("lugar", text);
          conv.pasoActual = ConversacionPdi.Paso.MOMENTO;
          msg.setText("🕒 Ingresá el momento (formato ISO 8601, ej: 2025-09-28T13:00:00):");
        }
        case MOMENTO -> {
          conv.datos.put("momento", text);
          conv.pasoActual = ConversacionPdi.Paso.CONTENIDO;
          msg.setText("📝 Ingresá el contenido del PDI:");
        }
        case CONTENIDO -> {
          conv.datos.put("contenido", text);
          conv.pasoActual = ConversacionPdi.Paso.IMAGE_URL;
          msg.setText("🖼️ Ingresá la URL de la imagen:");
        }
        case IMAGE_URL -> {
          conv.datos.put("image_url", text);
          conv.pasoActual = ConversacionPdi.Paso.COMPLETO;

          // Construir JSON final
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

          String respuesta = fuentesClient.agregarPdi(conv.hechoId, jsonBody);
          msg.setText("✅ PDI agregado correctamente al hecho *" + conv.hechoId + "* ✅\n" + respuesta);
          conversaciones.remove(chatId);
        }
      }
    } catch (Exception e) {
      System.out.println("Error al procesar el paso: " + e.getMessage());
      msg.setText("❌ Error al procesar el paso. Intentá nuevamente.");
    }

    return msg;
  }
}
