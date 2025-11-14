package org.example.bot.clients;

import org.example.bot.utils.HttpUtils;

public class FuentesClient {
    private static final String BASE_URL = "https://fuentes.onrender.com";
//    private static final String BASE_URL = "https://tp-anual-dds-fuentes.onrender.com";

    public String verHecho(String id) throws Exception {
        String url = BASE_URL + "/api/hecho/" + id;
        return HttpUtils.get(url);
    }

    public String agregarHecho(String nombreColeccion, String jsonBody) throws Exception {
        String url = BASE_URL + "/api/colecciones/" + nombreColeccion + "/hechos";
        return HttpUtils.post(url, jsonBody);
    }

    public String agregarPdi(String hechoId, String jsonBody) throws Exception {
        String url = BASE_URL + "/api/hecho/" + hechoId + "/pdis";
        return HttpUtils.post(url, jsonBody);
    }


}

