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

        try {
            switch (conv.pasoActual) {
                case ID -> {
                    conv.datos.put("id", text);
                    conv.pasoActual = ConversacionHecho.Paso.TITULO;
                    msg.setText("📝 Ingresá el título del hecho:");
                }
                case TITULO -> {
                    conv.datos.put("titulo", text);
                    conv.pasoActual = ConversacionHecho.Paso.CATEGORIA;
                    msg.setText("📚 Ingresá la categoría (por ej. DESASTRE, SALUD, SOCIAL, etc.):");
                }
                case CATEGORIA -> {
                    conv.datos.put("categoria", text.toUpperCase());
                    conv.pasoActual = ConversacionHecho.Paso.UBICACION;
                    msg.setText("📍 Ingresá la ubicación:");
                }
                case UBICACION -> {
                    conv.datos.put("ubicacion", text);
                    conv.pasoActual = ConversacionHecho.Paso.FECHA;
                    msg.setText("📅 Ingresá la fecha en formato ISO 8601 (ej: 2025-09-07T15:00:00):");
                }
                case FECHA -> {
                    conv.datos.put("fecha", text);
                    conv.pasoActual = ConversacionHecho.Paso.ORIGEN;
                    msg.setText("🧾 Ingresá el origen (ej: manual, dataset, colaborativo):");
                }
                case ORIGEN -> {
                    conv.datos.put("origen", text);
                    conv.pasoActual = ConversacionHecho.Paso.COMPLETO;

                    // Construir JSON
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

                    String respuesta = fuentesClient.agregarHecho(conv.coleccion, jsonBody);
                    msg.setText("✅ Hecho agregado correctamente:\n🆔 ID: "
                            + conv.datos.get("id") + "\n" + respuesta);

                    conversaciones.remove(chatId);
                }
            }
        } catch (Exception e) {
            System.out.println("Error al procesar el paso: " + e.getMessage());
            msg.setText("❌ Error al procesar el paso. Intentá nuevamente.");
        }

        return msg;
    }

    @Override
    public boolean hasConversation(Long chatId) {
        return conversaciones.containsKey(chatId);
    }

}
