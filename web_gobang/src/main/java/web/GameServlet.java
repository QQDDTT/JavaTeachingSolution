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
                Map<String, String> map = GAME_MAP.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getId()));
                ResponseData.success("list", map).sendJson(resp);
            }
            case "create" -> {
                String gameId = req.getParameter("id");
                if (gameId == null) {
                    Room room = new Room();
                    String newId = room.getId();
                    GAME_MAP.put(newId, room);
                    ResponseData.success("Create success", Map.of("room", room.getId())).sendJson(resp);
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
                if (color == null || color.length() > 1 || (color.charAt(0) != GameGobang.WHITE && color.charAt(0) != GameGobang.BLACK)) {
                    ResponseData.error("Color is error").sendJson(resp);
                    return;
                }
                GAME_MAP.get(gameId).join(sessionId, color).sendJson(resp);
                return;
            }
            case "start" -> {
                String gameId = req.getParameter("id");
                if (gameId == null || !GAME_MAP.keySet().contains(gameId)) {
                    ResponseData.error("Game is not exist").sendJson(resp);
                    return;
                }
                GAME_MAP.get(gameId).start().sendJson(resp);
                return;
            }
            case "data" -> {
                String gameId = req.getParameter("id");
                if (gameId == null || !GAME_MAP.keySet().contains(gameId)) {
                    ResponseData.error("Game is not exist").sendJson(resp);;
                    return;
                }
                GAME_MAP.get(gameId).getData().sendJson(resp);
                return;
            }
            default -> {
                ResponseData.error("Unknow action : " + action).sendJson(resp);
                return;
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
                if (color == null || color.length() > 1 || (color.charAt(0) != GameGobang.WHITE && color.charAt(0) != GameGobang.BLACK)) {
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
                if (color == null || color.length() > 1 || (color.charAt(0) != GameGobang.WHITE && color.charAt(0) != GameGobang.BLACK)) {
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
        private final String id;
        private String blackSession;
        private String whiteSession;
        private GameGobang game;
        private String status; // 新增：游戏状态

        public Room() {
            this(LocalDateTime.now().toString());
        }

        public Room(String id) {
            this.id = id;
            this.status = "waiting"; // 初始状态：等待玩家
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
        
        public String getStatus() {
            return this.status;
        }

        public ResponseData join(String sessionId, String color) {
            if (color.charAt(0) == GameGobang.BLACK && (this.blackSession == null || this.blackSession.isEmpty())) {
                this.blackSession = sessionId;
                // 检查是否双方都已加入
                if (this.whiteSession != null && !this.whiteSession.isEmpty()) {
                    this.status = "ready"; // 双方都已加入，准备开始
                }
                return ResponseData.success("Join success", Map.of("room", this.id));
            } else if (color.charAt(0) == GameGobang.WHITE && (this.whiteSession == null || this.whiteSession.isEmpty())) {
                this.whiteSession = sessionId;
                // 检查是否双方都已加入
                if (this.blackSession != null && !this.blackSession.isEmpty()) {
                    this.status = "ready"; // 双方都已加入，准备开始
                }
                return ResponseData.success("Join success", Map.of("room", this.id));
            } else {
                return ResponseData.error("Player is already exist");
            }
        }

        public ResponseData start() {
            if (this.blackSession == null || this.whiteSession == null) {
                return ResponseData.error("Player is not ready");
            }
            this.game = new GameGobang();
            this.status = "playing"; // 游戏进行中
            return ResponseData.success("Started", Map.of("game", this.game.toJson()));
        }

        public ResponseData getData() {
            Map<String, String> result = new HashMap<>();
            result.put("roomId", this.id);
            result.put("blackPlayer", this.blackSession != null ? this.blackSession : "");
            result.put("whitePlayer", this.whiteSession != null ? this.whiteSession : "");
            result.put("roomStatus", this.status); // 新增：返回房间状态
            
            if (this.game != null) {
                result.put("game", this.game.toJson());
                // 检查游戏是否结束
                String gameStatus = this.game.toJsonObject().get("status").toString();
                if (gameStatus.toLowerCase().contains("over")) {
                    this.status = "finished"; // 游戏已结束
                    result.put("roomStatus", this.status);
                }
            }
            
            return ResponseData.success("Room data", result);
        }

        public ResponseData next(String sessionId, int index) {
            if (this.game == null) {
                return ResponseData.error("Game not started");
            }
            
            if (sessionId.equals(this.blackSession)) {
                if (this.game.next(GameGobang.BLACK, index)) {
                    return ResponseData.success("Put down", Map.of("color", "B", "index", "" + index));
                }
            } else if (sessionId.equals(this.whiteSession)) {
                if (this.game.next(GameGobang.WHITE, index)) {
                    return ResponseData.success("Put down", Map.of("color", "W", "index", "" + index));
                }
            }
            return ResponseData.error("Put down failed");
        }
    }
}
