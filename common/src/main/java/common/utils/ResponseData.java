package common.utils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ResponseData
 * 用于封装接口响应数据
 */
public class ResponseData {

    private int status;
    private String message;
    private Map<String, String> map;

    public ResponseData(int status, String message) {
        this.status = status;
        this.message = message;
        this.map = Map.of();
    }

    public ResponseData(int status, String message, Map<String, String> map) {
        this.status = status;
        this.message = message;
        this.map = map;
    }

    public static ResponseData success(String message, Map<String, String> map) {
        return new ResponseData(200, message, map);
    }

    public static ResponseData error(String message) {
        return new ResponseData(500, message, null);
    }

    /** 输出 JSON 到响应 */
    public void sendJson(HttpServletResponse resp) throws IOException {
        resp.setStatus(this.status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(toJson());
        resp.getWriter().flush();
    }

    /** 转 JSON 字符串 */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"message\":\"").append(escape(message)).append("\"");

        if (map != null && !map.isEmpty()) {
            sb.append(",\"map\":").append(mapToJson(map));
        }

        sb.append("}");
        return sb.toString();
    }

    private String mapToJson(Map<String, String> map) {
        return "{" + map.entrySet().stream()
                .map(e -> "\"" + escape(e.getKey()) + "\":\"" + escape(e.getValue()) + "\"")
                .collect(Collectors.joining(",")) + "}";
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // Getter / Setter
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public Map<String, String> getMap() { return map; }

    public void setStatus(int status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setMap(Map<String, String> map) { this.map = map; }
}
