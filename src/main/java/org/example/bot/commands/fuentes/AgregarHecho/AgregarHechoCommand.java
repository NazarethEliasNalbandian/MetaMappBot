package org.example.bot.commands.fuentes.AgregarHecho;

import org.example.bot.clients.FuentesClient;
import org.example.bot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

public class AgregarHechoCommand implements BotCommand {

  private final FuentesClient fuentesClient = new FuentesClient();

  // Conversaciones activas por chat
  private final Map<Long, ConversacionHecho> conversaciones = new HashMap<>();

  @Override
  public boolean matches(String text) {
    return text.startsWith("/agregarhecho");
  }

  @Override
  public SendMessage handle(Update update) throws Exception {
    Long chatId = update.getMessage().getChatId();
    String text = update.getMessage().getText().trim();
    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());

    // Si la conversación ya está en curso → seguirla
    if (conversaciones.containsKey(chatId) && !text.startsWith("/agregarhecho")) {
      return manejarConversacion(chatId, text);
    }

    // Si es el inicio del comando
    String[] parts = text.split(" ", 2);
    if (parts.length < 2) {
      msg.setText("Uso: /agregarhecho <nombre_coleccion>");
      return msg;
    }

    ConversacionHecho conv = new ConversacionHecho();
    conv.coleccion = parts[1];
    conv.pasoActual = ConversacionHecho.Paso.ID;
    conversaciones.put(chatId, conv);

    msg.setText("🆔 Iniciando carga para colección *" + parts[1] + "*.\nPor favor, ingresá el ID del hecho (por ejemplo: h100):");
    return msg;
  }

  private SendMessage manejarConversacion(Long chatId, String text) {
    ConversacionHecho conv = conversaciones.get(chatId);
    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());
    msg.enableHtml(true);

    try {
      switch (conv.pasoActual) {

        // 1) ID
        case ID -> {
          conv.datos.put("id", text.trim());
          conv.pasoActual = ConversacionHecho.Paso.TITULO;

          msg.setText("""
              📝 <b>Perfecto.</b>
              Ahora ingresá el <b>título del hecho</b>:
              """);
        }

        // 2) TITULO
        case TITULO -> {
          conv.datos.put("titulo", text.trim());
          conv.pasoActual = ConversacionHecho.Paso.CATEGORIA;

          msg.setText("""
              📚 Ingresá la <b>categoría</b> del hecho.
                                  
              Ejemplos: <code>DELITO</code>, <code>SOCIAL</code>, <code>SALUD</code>, <code>DESASTRE</code>
              """);
        }

        // 3) CATEGORIA
        case CATEGORIA -> {
          conv.datos.put("categoria", text.trim().toUpperCase());
          conv.pasoActual = ConversacionHecho.Paso.UBICACION;

          msg.setText("""
              📍 Ingresá la <b>ubicación</b> del hecho:
              """);
        }

        // 4) UBICACION
        case UBICACION -> {
          conv.datos.put("ubicacion", text.trim());
          conv.pasoActual = ConversacionHecho.Paso.FECHA;

          msg.setText("""
              📅 Ingresá la <b>fecha</b> del hecho.
                                  
              Formato ISO 8601:
              <code>2025-08-05T15:00:00</code>
              """);
        }

        // 5) FECHA
        case FECHA -> {
          conv.datos.put("fecha", text.trim());
          conv.pasoActual = ConversacionHecho.Paso.ORIGEN;

          msg.setText("""
              🧾 Ingresá el <b>origen</b> del hecho:
                                  
              Ejemplos:
              <code>manual</code>,
              <code>dataset</code>,
              <code>colaborativo</code>
              """);
        }

        // 6) ORIGEN → crear hecho
        case ORIGEN -> {
          conv.datos.put("origen", text.trim());
          conv.pasoActual = ConversacionHecho.Paso.COMPLETO;

          // Crear JSON para la API
          String jsonBody = String.format("""
                  {
                    "id": "%s",
                    "titulo": "%s",
                    "etiquetas": [],
                    "categoria": "%s",
                    "ubicacion": "%s",
                    "fecha": "%s",
                    "origen": "%s"
                  }
                  """,
              conv.datos.get("id"),
              conv.datos.get("titulo"),
              conv.datos.get("categoria"),
              conv.datos.get("ubicacion"),
              conv.datos.get("fecha"),
              conv.datos.get("origen")
          );

          String respuestaApi = fuentesClient.agregarHecho(conv.coleccion, jsonBody);

          // parsear JSON de respuesta
          org.json.JSONObject json = new org.json.JSONObject(respuestaApi);
          String hechoId = json.optString("id", conv.datos.get("id"));

          // Respuesta linda
          msg.setText("""
              ✅ <b>Hecho agregado correctamente</b>

              🗂️ <b>Colección:</b> %s
              🆔 <b>ID:</b> <code>%s</code>
              📝 <b>Título:</b> %s
              📚 <b>Categoría:</b> %s
              📍 <b>Ubicación:</b> %s
              📅 <b>Fecha:</b> %s
              🧾 <b>Origen:</b> %s

              ✔ El hecho fue registrado en el sistema.
              """.formatted(
              conv.coleccion,
              hechoId,
              conv.datos.get("titulo"),
              conv.datos.get("categoria"),
              conv.datos.get("ubicacion"),
              conv.datos.get("fecha"),
              conv.datos.get("origen")
          ));

          conversaciones.remove(chatId);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      msg.setText("❌ Ocurrió un error al procesar los datos. Intentá nuevamente.");
    }

    return msg;
  }


  @Override
  public boolean hasConversation(Long chatId) {
    return conversaciones.containsKey(chatId);
  }

}
