package web;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Game2048 {
    private Random random = new Random();
    private static final int MAX_VAL = 2048; // 胜利目标值
    private int[] form;     // 棋盘（行优先存储的一维数组）
    private int rowCount;   // 行数
    private int colCount;   // 列数
    private String status;  // 游戏状态（Wait / Over / Building）

    /**
     * 默认构造函数（4x4 棋盘）
     */
    public Game2048() {
        this(4, 4);
    }

    /**
     * 指定行列数的构造函数
     */
    public Game2048(int rowCount, int colCount) {
        if (rowCount < 3 || colCount < 3) throw new IllegalArgumentException("Row/Col count must be >= 3");
        if (rowCount > 9 || colCount > 9) throw new IllegalArgumentException("Row/Col count must be <= 9");
        this.rowCount = rowCount;
        this.colCount = colCount;
        this.status = "Building";
        this.form = new int[rowCount * colCount];
    }

    /**
     * 初始化游戏，在棋盘上随机生成 3 个数字
     */
    public void start() {
        this.form = new int[rowCount * colCount];
        Set<Integer> positionList = new HashSet<>();
        while (positionList.size() < 3) {
            int position = random.nextInt(form.length);
            positionList.add(position);
        }
        for (int i = 0; i < form.length; i++) {
            if (positionList.contains(i)) {
                form[i] = randomVal();
            } else {
                form[i] = 0;
            }
        }
        this.status = "Wait";
    }

    /**
     * 随机生成一个初始值 (60% 概率=1, 30%=2, 10%=4)
     */
    private int randomVal() {
        int probability = random.nextInt(100);
        if (probability < 60) return 1;
        if (probability < 90) return 2;
        return 4;
    }

    /**
     * 执行移动后，生成新数字并检测游戏状态
     */
    public void next() {
        status = "Over"; // 默认设为结束
        List<Integer> spaceList = new ArrayList<>();
        for (int i = 0; i < form.length; i++) {
            if (form[i] == 0) {
                spaceList.add(i);
                status = "Wait"; // 仍有空格，游戏继续
            }
            if (form[i] == MAX_VAL) {
                status = "Over"; // 出现 2048，游戏胜利
                return;
            }
        }
        if (status.equals("Wait") && !spaceList.isEmpty()) {
            int pos = spaceList.get(random.nextInt(spaceList.size()));
            form[pos] = randomVal();
        }
    }

    /** 向左移动并合并 */
    public void left() {
        for (int row = 0; row < rowCount; row++) {
            int[] positions = new int[colCount];
            for (int col = 0; col < colCount; col++) {
                positions[col] = row * colCount + col;
            }
            compressLine(positions);
        }
    }

    /** 向右移动并合并 */
    public void right() {
        for (int row = 0; row < rowCount; row++) {
            int[] positions = new int[colCount];
            for (int col = 0; col < colCount; col++) {
                positions[col] = row * colCount + (colCount - 1 - col);
            }
            compressLine(positions);
        }
    }

    /** 向上移动并合并 */
    public void up() {
        for (int col = 0; col < colCount; col++) {
            int[] positions = new int[rowCount];
            for (int row = 0; row < rowCount; row++) {
                positions[row] = row * colCount + col;
            }
            compressLine(positions);
        }
    }

    /** 向下移动并合并 */
    public void down() {
        for (int col = 0; col < colCount; col++) {
            int[] positions = new int[rowCount];
            for (int row = 0; row < rowCount; row++) {
                positions[row] = (rowCount - 1 - row) * colCount + col;
            }
            compressLine(positions);
        }
    }

    /**
     * 压缩并合并一行/列（移动逻辑的核心）
     */
    private void compressLine(int[] positions) {
        int target = 0;
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            if (form[pos] == 0) continue;

            if (target > 0 && form[positions[target - 1]] == form[pos]) {
                // 相邻且相等 → 合并
                form[positions[target - 1]] *= 2;
                form[pos] = 0;
            } else {
                // 向前压缩
                if (target != i) {
                    form[positions[target]] = form[pos];
                    form[pos] = 0;
                }
                target++;
            }
        }
    }

    /**
     * 控制台打印棋盘
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int cellWidth = 4; // 每格宽度固定为4
        String line = "+" + IntStream.range(0, colCount)
                .mapToObj(i -> "-".repeat(cellWidth) + "+")
                .collect(Collectors.joining());

        for (int row = 0; row < rowCount; row++) {
            sb.append(line).append("\n");

            for (int col = 0; col < colCount; col++) {
                sb.append("|");
                int val = form[row * colCount + col];
                String str = val == 0 ? " ".repeat(cellWidth) : String.format("%" + cellWidth + "d", val);
                sb.append(str);
            }
            sb.append("|\n");
        }

        sb.append(line).append("\n");
        return sb.toString();
    }

    /**
     * 将棋盘转为 Map 对象，用于前端交互
     */
    public Map<String, Object> toJsonObject() {
        Map<String, Object> map = new HashMap<>();
        map.put("RowCount", rowCount);
        map.put("ColCount", colCount);
        map.put("Status", status);
        map.put("Form", form);
        return map;
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(toJsonObject()); // ✅ 调用已有的 toJsonObject()
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    // ================== Getter ==================
    public String getStatus() {
        return status;
    }

    public int[] getForm() {
        return form;
    }
}
