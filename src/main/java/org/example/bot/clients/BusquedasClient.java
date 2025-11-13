package org.example.bot.clients;

import java.util.Map;

public class BusquedasClient {

  public String construirQueryString(Map<String, String> filtros) {
    if (filtros.isEmpty()) return "";
    StringBuilder sb = new StringBuilder("?");
    filtros.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
    sb.deleteCharAt(sb.length() - 1);
    String BASE_URL = "http://localhost:8080/api/fuentes/busqueda";
    return BASE_URL + sb;
  }

}
