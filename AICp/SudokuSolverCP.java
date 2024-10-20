package AICp;
import java.awt.Point;
import java.util.*;

public class SudokuSolverCP {
    private static final int SIZE = 9;
    private static final int SUBGRID_SIZE = 3;
    private static Set<Integer>[][] domains;
    private static int[][] grid;

    public static boolean solveSudokuConstraint(int[][] inputGrid) {
        grid = inputGrid;
        initializeDomains();
        // Only modify grid during backtracking, not during AC-3
        return ac3() && backtrack();
    }

    private static void initializeDomains() {
        domains = new HashSet[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                domains[row][col] = new HashSet<>();
                if (grid[row][col] == 0) {
                    // Initialize with valid values only
                    for (int num = 1; num <= SIZE; num++) {
                        if (isValidInitial(num, row, col)) {
                            domains[row][col].add(num);
                        }
                    }
                } else {
                    domains[row][col].add(grid[row][col]);
                }
            }
        }
    }

    // New method to check initial validity
    private static boolean isValidInitial(int num, int row, int col) {
        // Check row
        for (int i = 0; i < SIZE; i++) {
            if (grid[row][i] == num) return false;
        }
        // Check column
        for (int i = 0; i < SIZE; i++) {
            if (grid[i][col] == num) return false;
        }
        // Check box
        int startRow = row - row % SUBGRID_SIZE;
        int startCol = col - col % SUBGRID_SIZE;
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            for (int j = 0; j < SUBGRID_SIZE; j++) {
                if (grid[i + startRow][j + startCol] == num) return false;
            }
        }
        return true;
    }

    private static boolean ac3() {
        Queue<Arc> queue = new LinkedList<>();
        
        // Initialize queue with all arcs
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == 0) {
                    for (Point neighbor : getNeighbors(row, col)) {
                        queue.add(new Arc(new Point(row, col), neighbor));
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            Arc arc = queue.poll();
            if (revise(arc)) {
                Point source = arc.source;
                if (domains[source.x][source.y].isEmpty()) {
                    return false;
                }
                // Don't modify grid during AC-3, only update domains
                for (Point neighbor : getNeighbors(source.x, source.y)) {
                    if (!neighbor.equals(arc.target)) {
                        queue.add(new Arc(neighbor, source));
                    }
                }
            }
        }
        return true;
    }

    private static boolean revise(Arc arc) {
        boolean revised = false;
        Point source = arc.source;
        Point target = arc.target;
        
        Set<Integer> toRemove = new HashSet<>();
        for (int x : domains[source.x][source.y]) {
            boolean hasValidValue = false;
            for (int y : domains[target.x][target.y]) {
                if (isConsistent(x, y, source, target)) {
                    hasValidValue = true;
                    break;
                }
            }
            if (!hasValidValue) {
                toRemove.add(x);
                revised = true;
            }
        }
        
        domains[source.x][source.y].removeAll(toRemove);
        return revised;
    }

    private static boolean forwardCheck(int row, int col) {
        Map<Point, Set<Integer>> removedValues = new HashMap<>();
        
        for (Point neighbor : getNeighbors(row, col)) {
            if (grid[neighbor.x][neighbor.y] == 0) {
                if (!removedValues.containsKey(neighbor)) {
                    removedValues.put(neighbor, new HashSet<>());
                }
                if (domains[neighbor.x][neighbor.y].contains(grid[row][col])) {
                    removedValues.get(neighbor).add(grid[row][col]);
                    domains[neighbor.x][neighbor.y].remove(grid[row][col]);
                }
                if (domains[neighbor.x][neighbor.y].isEmpty()) {
                    // Restore removed values before returning false
                    for (Map.Entry<Point, Set<Integer>> entry : removedValues.entrySet()) {
                        Point p = entry.getKey();
                        domains[p.x][p.y].addAll(entry.getValue());
                    }
                    return false;
                }
            }
        }
        return true;
    }

    // Rest of the methods remain the same
    private static boolean isConsistent(int value1, int value2, Point pos1, Point pos2) {
        if (pos1.x == pos2.x || pos1.y == pos2.y) {
            return value1 != value2;
        }
        
        int subgridRow1 = pos1.x / SUBGRID_SIZE;
        int subgridCol1 = pos1.y / SUBGRID_SIZE;
        int subgridRow2 = pos2.x / SUBGRID_SIZE;
        int subgridCol2 = pos2.y / SUBGRID_SIZE;
        
        if (subgridRow1 == subgridRow2 && subgridCol1 == subgridCol2) {
            return value1 != value2;
        }
        
        return true;
    }

    private static boolean backtrack() {
        Point emptyCell = findEmptyCell();
        if (emptyCell == null) {
            return true;
        }

        int row = emptyCell.x;
        int col = emptyCell.y;

        for (int num : new HashSet<>(domains[row][col])) {
            if (isValid(num, row, col)) {
                grid[row][col] = num;
                Set<Integer> oldDomain = new HashSet<>(domains[row][col]);
                domains[row][col] = new HashSet<>(Collections.singleton(num));
                
                if (forwardCheck(row, col) && backtrack()) {
                    return true;
                }
                
                grid[row][col] = 0;
                domains[row][col] = oldDomain;
            }
        }
        return false;
    }

    private static boolean isValid(int num, int row, int col) {
        for (int i = 0; i < SIZE; i++) {
            if ((i != col && grid[row][i] == num) || 
                (i != row && grid[i][col] == num)) {
                return false;
            }
        }
        
        int startRow = row - row % SUBGRID_SIZE;
        int startCol = col - col % SUBGRID_SIZE;
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            for (int j = 0; j < SUBGRID_SIZE; j++) {
                if ((startRow + i != row || startCol + j != col) && 
                    grid[i + startRow][j + startCol] == num) {
                    return false;
                }
            }
        }
        return true;
    }

    // Other supporting methods remain the same
    private static class Arc {
        Point source;
        Point target;

        Arc(Point source, Point target) {
            this.source = source;
            this.target = target;
        }
    }

    private static Point findEmptyCell() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == 0) {
                    return new Point(row, col);
                }
            }
        }
        return null;
    }

    private static Set<Point> getNeighbors(int row, int col) {
        Set<Point> neighbors = new HashSet<>();
        
        for (int i = 0; i < SIZE; i++) {
            if (i != col) neighbors.add(new Point(row, i));
        }
        
        for (int i = 0; i < SIZE; i++) {
            if (i != row) neighbors.add(new Point(i, col));
        }
        
        int startRow = row - row % SUBGRID_SIZE;
        int startCol = col - col % SUBGRID_SIZE;
        for (int r = 0; r < SUBGRID_SIZE; r++) {
            for (int c = 0; c < SUBGRID_SIZE; c++) {
                if (startRow + r != row || startCol + c != col) {
                    neighbors.add(new Point(startRow + r, startCol + c));
                }
            }
        }
        return neighbors;
    }
}