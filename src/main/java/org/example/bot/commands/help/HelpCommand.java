package org.example.bot.commands.help;

import org.example.bot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public class HelpCommand implements BotCommand {

    @Override
    public boolean matches(String text) {
        return text.equalsIgnoreCase("/help");
    }

    @Override
    public SendMessage handle(Update update) {
        Long chatId = update.getMessage().getChatId();
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.enableHtml(true);

        msg.setText("""
                🤖 <b>Comandos disponibles</b>
                ──────────────────────────────
                               \s
                🔹 <b>🧩 Fuentes</b>
                • <code>/ver &lt;id_hecho&gt;</code> — Visualiza un hecho y sus PDIs asociados. \s
                • <code>/agregarhecho &lt;coleccion&gt;</code> — Crea un nuevo hecho en la colección indicada. \s
                • <code>/agregarpdi &lt;id_hecho&gt;</code> — Agrega un nuevo PDI al hecho especificado.
                               \s
                ──────────────────────────────
                🔹 <b>🗂️ Agregador</b>
                • <code>/listar &lt;coleccion&gt;</code> — Lista los hechos de una colección. \s
                • <code>/busqueda [filtros]</code> — Busca hechos con filtros y paginación.
                               \s
                🧭 <b>Filtros disponibles:</b> \s
                <code>titulo</code> — texto del hecho \s
                <code>categoria</code> — categoría (ej: DELITO, SERVICIO, EVENTO) \s
                <code>etiquetas</code> — palabras clave separadas por coma \s
                <code>ubicacion</code> — ciudad o provincia \s
                <code>origen</code> — fuente del hecho \s
                               \s
                📖 <b>Ejemplo:</b> \s
                <code>/busqueda titulo=robo categoria=DELITO page=1</code> \s
                <code>/busqueda ubicacion=cordoba etiquetas=energia,page=2</code>
                               \s
                📄 <b>Paginación:</b> \s
                Usá <code>page=1</code>, <code>page=2</code>, etc., para recorrer los resultados.
                               \s
                ──────────────────────────────
                🔹 <b>🧾 Solicitudes</b>
                • <code>/solicitarborrado &lt;id_hecho&gt;</code> — Crea una solicitud de borrado para un hecho. \s
                • <code>/cambiarestado &lt;id_solicitud&gt;</code> — Modifica el estado de una solicitud existente.
                               \s
                ──────────────────────────────
                💡 <b>Consejo:</b> \s
                Si no recordás un comando, escribí <code>/help</code> para volver a ver esta lista.
               \s""");

        return msg;
    }
}
