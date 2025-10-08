package web;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import common.utils.ResponseData;

/**
 * GameServlet
 */
public class GameServlet extends HttpServlet {

    private static final Map<String, Room> GAME_MAP = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter("action");
        String sessionId = req.getSession().getId();
        switch (action) {
            case "list" -> {
                Map<String, String> map = GAME_MAP.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toJson()));
                ResponseData.success("list", map).sendJson(resp);
            }
            case "create" -> {
                String gameId = req.getParameter("id");
                if (gameId == null) {
                    Room room = new Room();
                    String newId = room.getId();
                    GAME_MAP.put(newId, room);
                    ResponseData.success("Create success", Map.of("room", room.toJson())).sendJson(resp);
                    return;
                } else {
                    if (GAME_MAP.keySet().contains(gameId)) {
                        ResponseData.error("GameId is already exist").sendJson(resp);
                        return;
                    }   
                }
            }
            case "join" -> {
                String gameId = req.getParameter("id");
                String color = req.getParameter("color");
                if (gameId == null || !GAME_MAP.keySet().contains(gameId)) {
                    ResponseData.error("Game is not exist").sendJson(resp);
                    return;
                }
                if (color == null || !color.equals(GameGobang.WHITE) || !color.equals(GameGobang.BLACK)) {
                    ResponseData.error("Color is error").sendJson(resp);
                    return;
                }
                GAME_MAP.get(gameId).join(sessionId, color).sendJson(resp);
            }
            case "data" -> {
                String gameId = req.getParameter("id");
                if (gameId == null || !GAME_MAP.keySet().contains(gameId)) {
                    ResponseData.error("Game is not exist");
                    return;
                }
                GAME_MAP.get(gameId).getData().sendJson(resp);
            }
            default -> {
                ResponseData.error("Unknow action : " + action).sendJson(resp);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getParameter("action");
        String sessionId = req.getSession().getId();
        switch (action) {
            case "join" -> {
                String gameId = req.getParameter("id");
                String color = req.getParameter("color");
                if (gameId == null || !GAME_MAP.keySet().contains(gameId)) {
                    ResponseData.error("Game is not exist").sendJson(resp);
                    return;
                }
                if (color == null || !color.equals(GameGobang.WHITE) || !color.equals(GameGobang.BLACK)) {
                    ResponseData.error("Color is error").sendJson(resp);
                    return;
                }
                GAME_MAP.get(gameId).join(sessionId, color).sendJson(resp);
            }
            case "next" -> {
                String gameId = req.getParameter("id");
                String color = req.getParameter("color");
                if (gameId == null || !GAME_MAP.keySet().contains(gameId)) {
                    ResponseData.error("Game is not exist").sendJson(resp);
                    return;
                }
                if (color == null || !color.equals(GameGobang.WHITE) || !color.equals(GameGobang.BLACK)) {
                    ResponseData.error("Color is error").sendJson(resp);
                    return;
                }
                String index = req.getParameter("index");
                try {
                    GAME_MAP.get(gameId).next(sessionId, Integer.parseInt(index)).sendJson(resp);
                } catch (Exception e) {
                    ResponseData.error(e.getMessage()).sendJson(resp);
                }

            }
            default -> {
                ResponseData.error("Unknow post action : " + action);
            }
        }
    }

    public static class Room {
        private final  String id;
        private String blackSession;
        private String whiteSession;
        private GameGobang game;

        public Room() {
            this(LocalDateTime.now().toString());
        }

        public Room(String id) {
            this.id = id;
        }

        public String getBlakSession() {
            return this.blackSession;
        }
        public void setBlackSession(String session) {
            this.blackSession = session;
        }
        public String getWhiteSession() {
            return this.whiteSession;
        }
        public void setWhiteSession(String session) {
            this.whiteSession = session;    
        }
        public String getId() {
            return this.id;
        }

        public ResponseData join(String sessionId, String color) {
            if (color.equals(GameGobang.BLACK) && (this.blackSession == null || this.blackSession.isEmpty())) {
                this.blackSession = sessionId;
                return ResponseData.success("Join success", Map.of("room", this.toJson()));
            } else if (color.equals(GameGobang.WHITE) && (this.whiteSession == null || this.whiteSession.isEmpty())) {
                this.whiteSession =sessionId;
                return ResponseData.success("Join success", Map.of("room", this.toJson()));
            } else {
                return ResponseData.error("Player is already exist");
            }
        }

        public ResponseData start() {
            if (this.blackSession == null || this.whiteSession == null) return ResponseData.error("Player in not ready");
            this.game = new GameGobang();
            return ResponseData.success("Started", Map.of("game", this.game.toJson()));
        }

        public ResponseData getData() {
            if (this.game == null) {
                return ResponseData.error("Game is not ready");
            } else {
                return ResponseData.success("Data", Map.of("room", this.toJson(), "game", this.game.toJson()));
            }
        }

        public ResponseData next(String sessionId, int index) {
            if (sessionId == this.blackSession) {
                if (this.game.next(GameGobang.BLACK, index)) {
                    return ResponseData.success("Put down", Map.of("color", "B", "index", "" + index));
                }
            } 
            if (sessionId == this.whiteSession) {
                if (this.game.next(GameGobang.WHITE, index)) {
                    return ResponseData.success("Put down", Map.of("color", "W", "index", "" + index));
                }
            }
            return ResponseData.error("Put down failed");
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{id:\"").append(this.id).append("\",black:\"").append(this.blackSession).append("\",white:\"").append(this.whiteSession).append("\"}");
            return sb.toString();
        }
    }
}
