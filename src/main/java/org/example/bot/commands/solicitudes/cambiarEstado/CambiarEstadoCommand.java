package org.example.bot.commands.solicitudes.cambiarEstado;

import org.example.bot.clients.SolicitudesClient;
import org.example.bot.commands.BotCommand;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import java.util.HashMap;
import java.util.Map;

public class CambiarEstadoCommand implements BotCommand {

    private final SolicitudesClient solicitudesClient = new SolicitudesClient();
    private final Map<Long, ConversacionEstado> conversaciones = new HashMap<>();

    @Override
    public boolean matches(String text) {
        return text != null && text.startsWith("/cambiarestado");
    }

    @Override
    public boolean hasConversation(Long chatId) {
        return conversaciones.containsKey(chatId);
    }

    @Override
    public SendMessage handle(Update update) throws Exception {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        // Si ya inició conversación, seguirla
        if (conversaciones.containsKey(chatId) && !text.startsWith("/cambiarestado")) {
            return continuarConversacion(chatId, text);
        }

        // Inicio del comando: /cambiarestado <id>
        String[] parts = text.split(" ", 2);

        SendMessage msg = new SendMessage(chatId.toString(), "");
        msg.enableMarkdown(true);

        if (parts.length < 2) {
            msg.setText("Uso correcto:\n`/cambiarestado <id_solicitud>`");
            return msg;
        }

        ConversacionEstado conv = new ConversacionEstado();
        conv.idSolicitud = parts[1].trim();
        conv.pasoActual = ConversacionEstado.Paso.ESTADO;
        conversaciones.put(chatId, conv);

        msg.setText("""
                🛠 *Cambiar estado de solicitud*

                ID seleccionada: `%s`

                Escribí el nuevo estado:
                • `ACEPTADA`
                • `RECHAZADA`
                • `PENDIENTE`

                _Tip_: Tocá un estado para copiarlo 👇
                `ACEPTADA`  `RECHAZADA`  `PENDIENTE`
                """.formatted(conv.idSolicitud));

        return msg;
    }

    private SendMessage continuarConversacion(Long chatId, String text) {
        ConversacionEstado conv = conversaciones.get(chatId);

        SendMessage msg = new SendMessage(chatId.toString(), "");
        msg.enableMarkdown(true);

        try {
            String estado = text.trim().toUpperCase();

            if (!estado.matches("ACEPTADA|RECHAZADA|PENDIENTE")) {
                msg.setText("""
                        ⚠️ *Estado no válido.*
                        Solo podés usar:
                        • `ACEPTADA`
                        • `RECHAZADA`
                        • `PENDIENTE`
                        """);
                return msg;
            }

            conv.estado = estado;

            // Armar JSON
            JSONObject json = new JSONObject()
                .put("id", conv.idSolicitud)
                .put("estado", conv.estado);

            // Llamar API
            String raw = solicitudesClient.cambiarEstado(json.toString());

            JSONObject obj = new JSONObject(raw);

            String descripcion = obj.optString("descripcion", "-");
            String estadoFinal = obj.optString("estado", estado).toUpperCase();
            String hechoId = obj.optString("hecho_id", "-");

            msg.setText("""
                    ✅ *Estado actualizado correctamente*

                    🆔 *Solicitud:* `%s`
                    📌 *Nuevo estado:* `%s`
                    📄 *Descripción:* %s
                    🧩 *Hecho:* `%s`
                    """
                .formatted(conv.idSolicitud, estadoFinal, descripcion, hechoId)
            );

            conversaciones.remove(chatId);
            return msg;

        } catch (Exception e) {
            conversaciones.remove(chatId);
            msg.setText("❌ *Error actualizando el estado.* Intentá nuevamente.");
            return msg;
        }
    }
}