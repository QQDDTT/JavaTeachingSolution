package web;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * GameGobang - 五子棋核心逻辑类
 * -----------------------------------------------------
 * This class implements the core logic of a Gomoku (Five-in-a-Row) game.
 * It manages the board state, player turns, and win detection.
 * 
 * （本类实现了五子棋的主要逻辑，包括棋盘状态、回合控制和胜负判定。）
 */
public class GameGobang {

    /** 黑棋（Black stone） */
    public static final char BLACK = 'B';
    /** 白棋（White stone） */
    public static final char WHITE = 'W';
    /** 空格（Empty position） */
    private static final char SPACE = '_';
    /** 边界（Out of playable area） */
    private static final char OUT = 'O';

    /** 15×15 实际棋盘，用于逻辑显示（不含边界） */
    private char[] board = new char[15 * 15];

    /** 23×23 带边界棋盘，用于防止越界（外围为 OUT） */
    private char[] table = new char[23 * 23];

    /** 当前游戏状态（轮到谁下或游戏结束） */
    private Status status;

    /**
     * 初始化棋盘（填充空格和边界），默认黑棋先行。
     */
    public GameGobang() {
        this.status = Status.BLACK; // 黑棋先行

        // 初始化15×15棋盘为空
        for (int i = 0; i < this.board.length; i++) {
            this.board[i] = SPACE;
        }

        // 初始化23×23带边界棋盘
        // 边界（4行/列）设为 OUT，内部区域设为 SPACE
        for (int i = 0; i < this.table.length; i++) {
            if (i / 23 < 4 || i / 23 > 19 || i % 23 < 4 || i % 23 > 19) {
                this.table[i] = OUT;
            } else {
                this.table[i] = SPACE;
            }
        }
    }

    /**
     * 执行一步落子（place a stone on the board）
     *
     * @param color 棋子颜色（'B' 或 'W'）
     * @param index 棋盘索引（0～528，即 23×23 棋盘上的一维索引）
     * @return true 表示落子成功，false 表示无效操作
     */
    public boolean next(char color, int index) {

        // 检查该位置是否为空
        if (this.table[index] != SPACE) return false;

        if (this.status == Status.BLACK && color == BLACK || this.status == Status.WHITE && color == WHITE) {
            // 落子
            this.table[index] = color;

            // 检查胜负并切换回合
            run();
            return true;
        }

        return false;
    }

    /**
     * 将游戏状态转换为 JSON 对象（便于 Web 传输或调试）
     */
    public Map<String, Object> toJsonObject() {
        return Map.of(
            "status", this.status.getDescription(),
            "board", new String(this.board)
        );
    }

    /**
     * 输出 JSON 字符串（封装了 ObjectMapper 序列化）
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(toJsonObject());
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * 游戏状态枚举（Game state）
     * BLACK：轮到黑棋下
     * WHITE：轮到白棋下
     * OVER：游戏结束
     */
    public static enum Status {
        BLACK("Black's turn"),
        WHITE("White's turn"),
        OVER("Game over");

        private String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    /**
     * 核心逻辑：胜负判断 + 回合切换
     * 
     * 每次落子后调用，检查是否形成五连子。
     * 若五连子成立 → 设为 OVER，否则轮换玩家。
     */
    private void run() {
        if (this.status == Status.OVER) return;

        // 将带边界棋盘内容复制到15×15棋盘
        copy();

        // 遍历整个带边界棋盘（23×23）
        for (int i = 0; i < this.table.length; i++) {
            char c = this.table[i];
            if (c == OUT || c == SPACE) continue;

            // 方向向量：右、下、左下、右下
            int[] dirs = {1, 23, 22, 24};

            for (int d : dirs) {
                int count = 1;

                // 向正方向（例如右边）统计连续棋子
                for (int j = 1; j < 5; j++) {
                    if (this.table[i + d * j] == c) count++;
                    else break;
                }

                // 向反方向（例如左边）统计连续棋子
                for (int j = 1; j < 5; j++) {
                    if (this.table[i - d * j] == c) count++;
                    else break;
                }

                // 若连续数量 ≥ 5，则游戏结束
                if (count >= 5) {
                    this.status = Status.OVER;
                    return;
                }
            }
        }

        // 若无人获胜，则切换回合
        if (this.status == Status.BLACK)
            this.status = Status.WHITE;
        else if (this.status == Status.WHITE)
            this.status = Status.BLACK;
    }

    /**
     * 将带边界棋盘（23×23）中的核心15×15部分复制到 board。
     * 
     * 这样可以保留干净的逻辑棋盘（不含边界），
     * 方便未来在界面中显示或导出。
     */
    private void copy() {
        for (int i = 0; i < this.board.length; i++) {
            // 偏移 4 行 4 列（即跳过 OUT 区域）
            int x = i % 15;  // board 的列（0-14）
            int y = i / 15;  // board 的行（0-14）
            // table 中对应的位置：行 y+4，列 x+4
            int tableIndex = (y + 4) * 23 + (x + 4);
            this.board[i] = this.table[tableIndex];
        }
    }
}
