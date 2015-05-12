import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;

public class SlidingPuzzleSolver {

	private static class Board {
		private byte tiles[];
		private byte emptyTileIndex, N;
		
		Board(byte[] tiles) {
			this.tiles = Arrays.copyOf(tiles, tiles.length);
			for (byte i = 0; i < tiles.length; i++) 
				if (tiles[i] == 0) {
					emptyTileIndex = i;
					break;
				}
			N = (byte) Math.sqrt(tiles.length);
		}
		
		byte[] getTiles() {
			return tiles;
		}

		byte getEmptyTileIndex() {
			return emptyTileIndex;
		}

		/**
		 * Moves a specified tile in a board in a given direction.
		 * @param board the board
		 * @param tileIndex index of the tile, counting from top-left
		 * @param direction character key of the direction
		 * @return
		 */
		private byte[] moveTile(byte[] board, byte tileIndex, char direction) {
			byte temp = board[tileIndex];
			
			switch (Character.toLowerCase(direction)) {
			case 'l':
				if (tileIndex - 1 < 0 || (tileIndex - 1) % N  == N - 1)  
					return null;
				board[tileIndex] = board[tileIndex - 1];
				board[tileIndex - 1] = temp;
				break;
			case 'r':
				if ((tileIndex + 1) % N  == 0)
					return null;
				board[tileIndex] = board[tileIndex + 1];
				board[tileIndex + 1] = temp;
				break;
			case 'u':
				if (tileIndex - N < 0)  
					return null;
				board[tileIndex] = board[tileIndex - N];
				board[tileIndex - N] = temp;
				break;
			case 'd':
				if (tileIndex + N >= board.length)  
					return null;
				board[tileIndex] = board[tileIndex + N];
				board[tileIndex + N] = temp;
				break;
			default:
				return null;
			}
			
			return board;
		}
		
		/**
		 * Generates up to four states of the legal movements of the empty tile.
		 * @return the resulting boards of all legal movements of the empty tile
		 */
		Board[] makeAdjacentBoards() {
			Board boards[] = new Board[4];
			
			byte temp[] = null;
			
			temp = moveTile(Arrays.copyOf(tiles, tiles.length), emptyTileIndex, 'l');
			boards[0] = temp == null ? null : new Board(temp);
			
			temp = moveTile(Arrays.copyOf(tiles, tiles.length), emptyTileIndex, 'r');
			boards[1] = temp == null ? null : new Board(temp);
			
			temp = moveTile(Arrays.copyOf(tiles, tiles.length), emptyTileIndex, 'u');
			boards[2] = temp == null ? null : new Board(temp);
			
			temp = moveTile(Arrays.copyOf(tiles, tiles.length), emptyTileIndex, 'd');
			boards[3] = temp == null ? null : new Board(temp);
			
			return boards;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(tiles);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Board))
				return false;
			Board other = (Board) obj;
			if (!Arrays.equals(tiles, other.getTiles()))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return Arrays.toString(tiles);
		}
	}
	
	private static class Node {
		
		private Board board;
		
		private short fScore;
		
		Node(Board board) {
			this.board = board;
			fScore = Short.MAX_VALUE;
		}

		Board getBoard() {
			return board;
		}

		short getfScore() {
			return fScore;
		}

		void setfScore(short s) {
			this.fScore = s;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Node))
				return false;
			Node other = (Node) obj;
			if (!board.equals(other.getBoard()))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return board.toString();
		}
	}
	
	private static class StateFScoreComparator implements Comparator<Node> {

		public int compare(Node s1, Node s2) {
			if (s1.getfScore() < s2.getfScore())
				return -1;
			if (s1.getfScore() == s2.getfScore())
				return 0;
			return 1;
		}
	}
	
	private byte N;
	private byte board[];

	private int statesVisited, statesCreated, statesUpdated;
	
	private File inputFile, outputFile;
	private PrintWriter out;
	
	/**
	 * Constructor for an instance of Exercise2. 
	 * Makes sure that the input file can be read, and that the output file can be created or deleted.
	 * @param infname	filename of the input file
	 * @param outfname	filename of the output file
	 */
	public SlidingPuzzleSolver(String infname, String outfname) throws FileNotFoundException {
		inputFile = new File(infname);
		if (!inputFile.canRead()) {
			System.out.println("File " + infname + " could be not found, or cannot be read!");
			System.exit(0);
		}
		outputFile = new File(outfname);
		if (outputFile.exists() && !outputFile.delete()) {
			System.out.println("File " + outfname + " could not be deleted!");
			System.exit(0);
		}
		out = new PrintWriter(outputFile);

		statesVisited = 0; statesCreated = 0; statesUpdated = 0;
	}
	
	private Board makeGoalBoard() {
		byte goalBoard[] = new byte[N * N];
		
		for (int i = 0; i < goalBoard.length; i++) 
			goalBoard[i] = (byte) (i + 1);
		
		goalBoard[goalBoard.length - 1] = 0;
		
		return new Board(goalBoard);
	}

	private short manhattanHeuristic(Board startBoard, HashMap<Board, Short> hScores) {
		Short s = hScores.get(startBoard);
		if (s != null) 
			return s;
		
		short cost = 0;
		byte[] startTiles = startBoard.getTiles();
		
		for (int i = 0; i < board.length; i++) {
			byte tileNumber = startTiles[i];
			if (tileNumber != i + 1 && (board[i] != 0 || i != board.length - 1))
				if (tileNumber == 0) 
					cost += N - 1 - (i / N) + N - 1 - (i % N);
				else 
					cost += Math.abs(((tileNumber - 1) / N) - (i / N)) + Math.abs((i % N) - ((tileNumber - 1) % N));
		}

		cost *= 2;
		hScores.put(startBoard, cost);
		return cost;
	}
	
	/**
	 * Recursive method for building a path string from start to goal using the map of parents.
	 * @param from current board/state, the given node
	 * @param parentStates map of the parent nodes
	 * @return the string describing the path to the given node
	 */
	private String tracePath(Board from, HashMap<Board, Node> parentStates) {
		Node current = parentStates.get(from);
		
		if (current != null) {
			byte fromSlot = from.getEmptyTileIndex(), currentSlot = current.getBoard().getEmptyTileIndex();
			
			if (fromSlot + 1 == currentSlot)
				return tracePath(current.getBoard(), parentStates) + "L";
			else if (fromSlot - 1 == currentSlot) 
				return tracePath(current.getBoard(), parentStates) + "R";
			else if (fromSlot + N == currentSlot) 
				return tracePath(current.getBoard(), parentStates) + "U";
			else 
				return tracePath(current.getBoard(), parentStates) + "D";
		} else
			return "";
	}
	
	/**
	 * Solves this board and outputs various statistics to the output file.
	 */
	public void solve() {
		PriorityQueue<Node> openStates = new PriorityQueue<Node>(N * N, new StateFScoreComparator());
		HashSet<Board> closedStates = new HashSet<Board>();
		HashMap<Board, Node> parentStates = new HashMap<Board, Node>();
		
		HashMap<Board, Short> gScores = new HashMap<Board, Short>(); 
		HashMap<Board, Short> hScores = new HashMap<Board, Short>(); 
		
		Board startBoard = new Board(board), goalBoard = makeGoalBoard();
		Node startNode = new Node(startBoard), goalNode = new Node(goalBoard), current; 
		short currentgScore;
		
		gScores.put(startBoard, (short) 0);
		startNode.setfScore(manhattanHeuristic(startBoard, hScores));
		
		openStates.offer(startNode);
		
		while (!openStates.isEmpty()) {
			current = openStates.poll();
			statesVisited++;
			
			if (current.equals(goalNode)) 
				break;
			
			closedStates.add(current.getBoard());
			currentgScore = gScores.get(current.getBoard());
			
			for (Board b : current.getBoard().makeAdjacentBoards()) {
				if (b == null || closedStates.contains(b))
					continue;
				
				Node n = new Node(b);
				Short g = gScores.get(b);
				
				if (!openStates.contains(n) || currentgScore + 1 < g) {
					parentStates.put(b, current);

					if (g != null && currentgScore + 1 < g) {
						statesUpdated++; 
						openStates.remove(n);
					} else 
						statesCreated++;

					gScores.put(b, (short) (currentgScore + 1));
					n.setfScore((byte) (currentgScore + 1 + manhattanHeuristic(b, hScores)));
					
					openStates.offer(n);
				} 
			}
		}
		
		String pathString = tracePath(goalBoard, parentStates);
		out.println(pathString.length());
		out.println(pathString);
		out.println(statesVisited);
		out.println(statesCreated);
		out.println(statesUpdated);
		
		out.close();
	}

	/**
	 * Checks if this board is solvable, using inversions.
	 * Based on formula from http://www.cs.bham.ac.uk/~mdr/teaching/modules04/java2/TilesSolvability.html
	 * @return true if the board is solvable, false otherwise
	 */
	public boolean isSolvable() {
		short inversions = 0;
		byte emptyTileIndex = -1;
		
		for (byte i = 0; i < board.length; i++) {
			if (board[i] == 0) {
				emptyTileIndex = i;
				continue;
			}
			for (byte j = (byte) (i + 1); j < board.length; j++) {
				if (board[j] == 0)
					continue;
				if (board[i] > board[j])
					inversions++;
			}
		}

		boolean oddN = N % 2 != 0, oddInversions = inversions % 2 != 0, 
		oddEmptyTileRowFromBottom = oddN ? ((emptyTileIndex / N) + 1) % 2 != 0 : ((emptyTileIndex / N) + 1) % 2 == 0;
		
		return (oddN && !oddInversions) || (!oddN && oddInversions && !oddEmptyTileRowFromBottom) || (!oddInversions && oddEmptyTileRowFromBottom);
	}
	
	/**
	 * Processes the input file.
	 */
	public void readInputFile() throws FileNotFoundException {
		Scanner s = null;
		s = new Scanner(inputFile);
		
		N = s.nextByte();
		board = new byte[N * N];
		
		for (int i = 0; i < board.length; i++) 
			board[i] = s.nextByte();
		
		s.close();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		if (args.length == 2) {
			SlidingPuzzleSolver p = new SlidingPuzzleSolver(args[0], args[1]);
			p.readInputFile();
			if (p.isSolvable())
				p.solve();
			else
				System.out.println("Board is not solvable!");
		} else {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			System.err.println("Invalid number of arguments:\n" + stack[stack.length - 1].getClassName() + " input_filename output_filename");
		}
	}

}
