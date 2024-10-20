package AICp;
import java.util.*;

public class SudokuGA {
    private static final int SIZE = 9;
    private static final int SUBGRID = 3;
    private static final int POPULATION_SIZE = 1000;
    private static final int MAX_GENERATIONS = 1000;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.95;
    private static final int STAGNATION_LIMIT = 50;
    private static final Random rand = new Random();

    public static boolean solve(int[][] board) {
        try {
            // Create a copy of the initial board
            int[][] initialBoard = new int[SIZE][SIZE];
            for (int i = 0; i < SIZE; i++) {
                System.arraycopy(board[i], 0, initialBoard[i], 0, SIZE);
            }

            List<int[][]> population = initializePopulation(initialBoard);
            int bestFitness = Integer.MIN_VALUE;
            int stagnationCounter = 0;
            int[][] lastBestSolution = null;

            for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
                // Get mating pool using tournament selection
                List<int[][]> matingPool = getMatingPool(population);
                Collections.shuffle(matingPool);
                
                // Create new population through crossover and mutation
                population = evolvePopulation(matingPool, initialBoard);
                
                // Find best solution
                int[][] bestSolution = getBestSolution(population);
                int currentFitness = calculateFitness(bestSolution);
                
                // Print progress every 10 generations
                if (generation % 10 == 0) {
                    System.out.println("Generation " + generation + 
                                     ", Best Fitness: " + currentFitness + 
                                     ", Conflicts: " + (-currentFitness) +
                                     ", Population Size: " + population.size());
                    printBoard(bestSolution);
                }

                // Check for improvement
                if (currentFitness > bestFitness) {
                    bestFitness = currentFitness;
                    lastBestSolution = bestSolution;
                    stagnationCounter = 0;
                } else {
                    stagnationCounter++;
                }

                // Solution found
                if (currentFitness == 0) {
                    System.out.println("Solution found at generation " + generation);
                    for (int i = 0; i < SIZE; i++) {
                        System.arraycopy(bestSolution[i], 0, board[i], 0, SIZE);
                    }
                    return true;
                }

                // Break if stuck
                if (stagnationCounter >= STAGNATION_LIMIT) {
                    System.out.println("Stuck in local optimum. Restarting with new population...");
                    population = initializePopulation(initialBoard);
                    stagnationCounter = 0;
                }
            }
            
            // If we didn't find a perfect solution, use the best one we found
            if (lastBestSolution != null) {
                System.out.println("Best solution found (not perfect):");
                printBoard(lastBestSolution);
                for (int i = 0; i < SIZE; i++) {
                    System.arraycopy(lastBestSolution[i], 0, board[i], 0, SIZE);
                }
            }
            
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void printBoard(int[][] board) {
        System.out.println("Current best board:");
        for (int i = 0; i < SIZE; i++) {
            if (i % 3 == 0 && i != 0) {
                System.out.println("-".repeat(21));
            }
            for (int j = 0; j < SIZE; j++) {
                if (j % 3 == 0 && j != 0) {
                    System.out.print("| ");
                }
                System.out.print(board[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private static List<int[][]> initializePopulation(int[][] initialBoard) {
        List<int[][]> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(generateCandidate(initialBoard));
        }
        return population;
    }

    private static int[][] generateCandidate(int[][] initialBoard) {
        int[][] candidate = new int[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            List<Integer> available = new ArrayList<>();
            for (int i = 1; i <= SIZE; i++) {
                available.add(i);
            }
            Collections.shuffle(available);
            
            // Copy fixed numbers from initial board
            for (int col = 0; col < SIZE; col++) {
                if (initialBoard[row][col] != 0) {
                    candidate[row][col] = initialBoard[row][col];
                    available.remove(Integer.valueOf(initialBoard[row][col]));
                }
            }
            
            // Fill remaining cells
            int availableIndex = 0;
            for (int col = 0; col < SIZE; col++) {
                if (candidate[row][col] == 0) {
                    candidate[row][col] = available.get(availableIndex++);
                }
            }
        }
        return candidate;
    }

    private static List<int[][]> getMatingPool(List<int[][]> population) {
        List<int[][]> matingPool = new ArrayList<>();
        
        // Sort population by fitness
        population.sort((a, b) -> calculateFitness(b) - calculateFitness(a));
        
        // Tournament selection
        while (matingPool.size() < population.size()) {
            int[][] selected = tournamentSelect(population, 5);
            matingPool.add(cloneBoard(selected));
        }
        
        return matingPool;
    }

    private static int[][] tournamentSelect(List<int[][]> population, int tournamentSize) {
        List<int[][]> tournament = new ArrayList<>();
        for (int i = 0; i < tournamentSize; i++) {
            tournament.add(population.get(rand.nextInt(population.size())));
        }
        return Collections.max(tournament, Comparator.comparingInt(SudokuGA::calculateFitness));
    }

    private static List<int[][]> evolvePopulation(List<int[][]> matingPool, int[][] initialBoard) {
        List<int[][]> newPopulation = new ArrayList<>();
        
        // Keep best solution (elitism)
        newPopulation.add(cloneBoard(getBestSolution(matingPool)));
        
        for (int i = 1; i < POPULATION_SIZE; i += 2) {
            int[][] parent1 = matingPool.get(i - 1);
            int[][] parent2 = matingPool.get(i);
            
            if (rand.nextDouble() < CROSSOVER_RATE) {
                int[][][] children = crossover(parent1, parent2);
                mutate(children[0], initialBoard);
                mutate(children[1], initialBoard);
                newPopulation.add(children[0]);
                if (newPopulation.size() < POPULATION_SIZE) {
                    newPopulation.add(children[1]);
                }
            } else {
                newPopulation.add(cloneBoard(parent1));
                if (newPopulation.size() < POPULATION_SIZE) {
                    newPopulation.add(cloneBoard(parent2));
                }
            }
        }
        
        return newPopulation;
    }

    private static int[][] getBestSolution(List<int[][]> population) {
        return Collections.max(population, Comparator.comparingInt(SudokuGA::calculateFitness));
    }

    private static int[][] cloneBoard(int[][] board) {
        int[][] clone = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(board[i], 0, clone[i], 0, SIZE);
        }
        return clone;
    }

    private static int[][] mutate(int[][] candidate, int[][] initialBoard) {
        for (int row = 0; row < SIZE; row++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                // Only mutate rows that don't contain fixed numbers from initial board
                boolean hasFixed = false;
                for (int col = 0; col < SIZE; col++) {
                    if (initialBoard[row][col] != 0) {
                        hasFixed = true;
                        break;
                    }
                }
                if (!hasFixed) {
                    // Swap two random positions in the row
                    int pos1 = rand.nextInt(SIZE);
                    int pos2 = rand.nextInt(SIZE);
                    int temp = candidate[row][pos1];
                    candidate[row][pos1] = candidate[row][pos2];
                    candidate[row][pos2] = temp;
                }
            }
        }
        return candidate;
    }

    private static int[][][] crossover(int[][] parent1, int[][] parent2) {
        int[][] child1 = new int[SIZE][SIZE];
        int[][] child2 = new int[SIZE][SIZE];
        
        for (int row = 0; row < SIZE; row++) {
            if (rand.nextBoolean()) {
                System.arraycopy(parent1[row], 0, child1[row], 0, SIZE);
                System.arraycopy(parent2[row], 0, child2[row], 0, SIZE);
            } else {
                System.arraycopy(parent2[row], 0, child1[row], 0, SIZE);
                System.arraycopy(parent1[row], 0, child2[row], 0, SIZE);
            }
        }
        
        return new int[][][] { child1, child2 };
    }

    private static int calculateFitness(int[][] candidate) {
        int conflicts = 0;
        
        // Check rows (not needed as we maintain row validity in generation)
        for (int row = 0; row < SIZE; row++) {
            conflicts += countConflicts(getRow(candidate, row));
        }
        
        // Check columns
        for (int col = 0; col < SIZE; col++) {
            conflicts += countConflicts(getColumn(candidate, col));
        }
        
        // Check 3x3 subgrids
        for (int blockRow = 0; blockRow < SIZE; blockRow += SUBGRID) {
            for (int blockCol = 0; blockCol < SIZE; blockCol += SUBGRID) {
                conflicts += countConflicts(getBlock(candidate, blockRow, blockCol));
            }
        }
        
        return -conflicts; // Return negative conflicts as fitness (0 is perfect)
    }

    private static int countConflicts(List<Integer> numbers) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int num : numbers) {
            counts.merge(num, 1, Integer::sum);
        }
        
        int conflicts = 0;
        for (int count : counts.values()) {
            if (count > 1) {
                conflicts += (count - 1);
            }
        }
        return conflicts;
    }

    private static List<Integer> getRow(int[][] board, int row) {
        List<Integer> numbers = new ArrayList<>();
        for (int col = 0; col < SIZE; col++) {
            numbers.add(board[row][col]);
        }
        return numbers;
    }

    private static List<Integer> getColumn(int[][] board, int col) {
        List<Integer> numbers = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            numbers.add(board[row][col]);
        }
        return numbers;
    }

    private static List<Integer> getBlock(int[][] board, int blockRow, int blockCol) {
        List<Integer> numbers = new ArrayList<>();
        for (int row = blockRow; row < blockRow + SUBGRID; row++) {
            for (int col = blockCol; col < blockCol + SUBGRID; col++) {
                numbers.add(board[row][col]);
            }
        }
        return numbers;
    }
}