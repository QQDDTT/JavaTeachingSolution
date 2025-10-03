package web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GameServlet
 * 负责处理 /state, /restart, /move/* API
 */
public class GameServlet extends HttpServlet {
    private Game2048 game = new Game2048(4, 4);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        switch (path) {
            case "/state":
                writeJson(resp, game.toJson());
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (path.equals("/restart")) {
            int row = parseInt(req.getParameter("row"), 4);
            int col = parseInt(req.getParameter("col"), 4);
            game = new Game2048(row, col);
            game.start();
            writeJson(resp, game.toJson());

        } else if (path.startsWith("/move/")) {
            String dir = path.substring("/move/".length()).toLowerCase();
            switch (dir) {
                case "left": game.left(); break;
                case "right": game.right(); break;
                case "up": game.up(); break;
                case "down": game.down(); break;
                default:
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid direction");
                    return;
            }
            game.next();
            writeJson(resp, game.toJson());

        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private int parseInt(String v, int def) {
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }

    private void writeJson(HttpServletResponse resp, String json) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

}
