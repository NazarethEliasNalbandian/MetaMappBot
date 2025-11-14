package org.example.bot.commands.solicitudes.solicitarBorrado;

import org.example.bot.clients.SolicitudesClient;
import org.example.bot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

public class SolicitarBorradoCommand implements BotCommand {

  private final SolicitudesClient solicitudesClient = new SolicitudesClient();
  private final Map<Long, ConversacionSolicitud> conversaciones = new HashMap<>();

  @Override
  public boolean matches(String text) {
    return text.startsWith("/solicitarborrado");
  }

  @Override
  public boolean hasConversation(Long chatId) {
    return conversaciones.containsKey(chatId);
  }

  @Override
  public SendMessage handle(Update update) throws Exception {
    Long chatId = update.getMessage().getChatId();
    String text = update.getMessage().getText().trim();

    SendMessage msg = new SendMessage(chatId.toString(), "");
    msg.enableHtml(true);

    // Si ya está en curso
    if (conversaciones.containsKey(chatId) && !text.startsWith("/solicitarborrado")) {
      return manejarConversacion(chatId, text);
    }

    // Inicio del flujo
    String[] parts = text.split(" ", 2);
    if (parts.length < 2) {
      msg.setText("""
           ❗ *Uso incorrecto*

           Correcto:\s
           <pre>/solicitarborrado &lt;id_hecho&gt;</pre>
          \s""");
      return msg;
    }

    ConversacionSolicitud conv = new ConversacionSolicitud();
    conv.hechoId = parts[1];
    conv.pasoActual = ConversacionSolicitud.Paso.DESCRIPCION;
    conversaciones.put(chatId, conv);

    msg.setText("""
            🧾 <b>Creación de Solicitud de Borrado</b>

            Hecho seleccionado: <b>%s</b>

            Por favor escribí una descripción o motivo del borrado.
            (Ejemplo: "El hecho fue reportado por error", "Ya no corresponde", etc.)
        """.formatted(conv.hechoId));

    return msg;
  }

  private SendMessage manejarConversacion(Long chatId, String text) {
    ConversacionSolicitud conv = conversaciones.get(chatId);
    SendMessage msg = new SendMessage();
    msg.setChatId(chatId.toString());
    msg.enableHtml(true);

    try {
      switch (conv.pasoActual) {
        case DESCRIPCION -> {
          conv.descripcion = text;

          // Construir JSON para API
          String jsonBody = String.format("""
                  {
                    "descripcion": "%s",
                    "hecho_id": "%s"
                  }
                  """,
              conv.descripcion,
              conv.hechoId
          );

          String respuesta = solicitudesClient.crearSolicitud(jsonBody);

          // Parsear el JSON devuelto
          org.json.JSONObject json = new org.json.JSONObject(respuesta);

          String solicitudId = json.optString("id", "desconocido");
          String estado = json.optString("estado", "desconocido");

          msg.setText("""
              🗑️ <b>Solicitud de borrado creada correctamente</b>

              🆔 <b>Hecho afectado:</b>
              %s

              📝 <b>Motivo del borrado:</b>
              %s

              📄 <b>Detalles de la solicitud:</b>
              • ID de solicitud: <code>%s</code>
              • Estado actual: <b>%s</b>

              ✔ Gracias por usar MetaMapa Bot
              """.formatted(
              conv.hechoId,
              conv.descripcion,
              solicitudId,
              estado.toUpperCase()
          ));

          conversaciones.remove(chatId);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      msg.setText("❌ Error al crear la solicitud. Intentá nuevamente.");
    }

    return msg;
  }

}
