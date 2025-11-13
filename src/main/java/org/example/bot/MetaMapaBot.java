package org.example.bot;

import org.example.bot.commands.*;
import org.example.bot.commands.agregador.ListarHechosCommand;
import org.example.bot.commands.busquedas.BusquedasCommand;
import org.example.bot.commands.fuentes.AgregarHecho.AgregarHechoCommand;
import org.example.bot.commands.fuentes.AgregarPdi.AgregarPdiCommand;
import org.example.bot.commands.fuentes.VerHecho.VerHechoCommand;
import org.example.bot.commands.help.HelpCommand;
import org.example.bot.commands.solicitudes.cambiarEstado.CambiarEstadoCommand;
import org.example.bot.commands.solicitudes.solicitarBorrado.SolicitarBorradoCommand;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class MetaMapaBot extends TelegramLongPollingBot {

    private final List<BotCommand> commands = List.of(
            // Fuentes
            new VerHechoCommand(),
            new AgregarHechoCommand(),  // este manejará el flujo conversacional que ya tenés
            new AgregarPdiCommand(),

            // Agregador
            new ListarHechosCommand(),

            // Solicitudes
            new SolicitarBorradoCommand(),
            new CambiarEstadoCommand(),

            // Busquedas
            new BusquedasCommand(),

            new HelpCommand()
    );

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        String text = update.getMessage().getText();

        Long chatId = update.getMessage().getChatId();

        for (BotCommand cmd : commands) {
            if (cmd.hasConversation(chatId)) {
                try {
                    execute(cmd.handle(update));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }

        for (BotCommand cmd : commands) {
            if (cmd.matches(text)) {
                try {
                    execute(cmd.handle(update));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }


        // Si ningún comando coincide:
        SendMessage fallback = new SendMessage();
        fallback.setChatId(update.getMessage().getChatId().toString());
        fallback.setText("Comando no reconocido. Usá /help para ver las opciones.");
        try {
            execute(fallback);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return System.getenv("NOMBRE_BOT");
    }

    @Override
    public String getBotToken() {
        return System.getenv("TOKEN_BOT");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new MetaMapaBot());
        System.out.println("✅ Bot MetaMapa iniciado correctamente");
    }
}
