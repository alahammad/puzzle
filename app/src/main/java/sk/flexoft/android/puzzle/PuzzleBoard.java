package sk.flexoft.android.puzzle;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;

import sk.flexoft.android.puzzle.util.AndroidExtensions;

/**
 * @author Vladimir Iszer
 * Represents a board for a game
 */
/**
 * @author izi
 *
 */
public class PuzzleBoard {
	
	/** Constant describing empty field index. */
	public static final int EMPTY_FIELD_IDX = -1;
	
    /** Left index identifier*/
    private static int LeftIdx = 0;
    
    /** Right index identifier*/
    private static int RightIdx = 1;
    
    /** Up index identifier*/
    private static int UpIdx = 2;
    
    /** Down index identifier*/
    private static int DownIdx = 3;
    
    /** The collection of opposite moves. */
    private static final TreeMap<Integer, Integer> opposites;
    
    /** The map of level to shuffle moves mapping. */
    private static final TreeMap<Integer, Integer> shuffleMoves;
    
    /** Random number generator. */
    private static final Random rand = new Random();
    
    private static final TreeMap<Integer, PuzzleBoard> boards;
	
	/** The puzzle activity. */
	private PuzzleActivity puzzleActivity;
	
	/** The main game board keeping location of puzzles. */
	private int[][] board;
	
	/** The main game board keeping location of puzzles. */
	private int size;
	
	/** Indicates the user finished the game */
	private boolean hasFinished = true; 

	/** Static constructor **/
	static {
		opposites = new TreeMap<Integer, Integer>();
		opposites.put(LeftIdx, RightIdx);
		opposites.put(RightIdx, LeftIdx);
		opposites.put(UpIdx, DownIdx);
		opposites.put(DownIdx, UpIdx);
		
		shuffleMoves = new TreeMap<Integer, Integer>();
		shuffleMoves.put(R.id.menu_level_easy, 3);
		shuffleMoves.put(R.id.menu_level_medium, 5);
		shuffleMoves.put(R.id.menu_level_hard, 7);
		
		boards = new TreeMap<Integer, PuzzleBoard>();
    };
	
	/**
	 * Instantiates a new puzzle board.
	 *
	 * @param activity The PuzzleActivity instance 
	 * @param size The required size of the board
	 */
	public PuzzleBoard(PuzzleActivity activity, int size)
	{
		if (activity == null)
		{
			throw new IllegalArgumentException("activity");
		}
		
		if (size < 2)
		{
			throw new IllegalArgumentException("size");
		}
		
		puzzleActivity = activity;
		this.size = size; 
		board = new int[size][size];
	}
	
	/**
	 * Returns board of appropriate size.
	 * @param activity The PuzzleActivity instance.
	 * @param size Required size of the board.
	 * @return A PuzzleBoard instance containing board of required size.
	 */
	public static PuzzleBoard getBoard(PuzzleActivity activity, int size)
	{
		if (!boards.containsKey(size))
		{
			boards.put(size, new PuzzleBoard(activity, size));	
		}
		
		return boards.get(size);
	}
	
	/**
	 * Shuffles the board.
	 */
	public void shuffle()
	{
		int lastOperation = -1;
        int[] mask = new int[4];

        for (int i = 0; i < size * size; i++)
        {
            board[i / size][i % size] = i;
        }
        
        board[size - 1][size - 1] = EMPTY_FIELD_IDX;
        
        int emptyX = size - 1;
        int emptyY = size - 1;
        for (int i = 0; i < Math.pow(shuffleMoves.get(PuzzleConfiguration.level), 2); i++)
        {
        	Arrays.fill(mask, 0);
            if (emptyX > 0)
            {
                mask[LeftIdx] = 1;
            }
            if (emptyX < size - 1)
            {
                mask[RightIdx] = 1;
            }
            if (emptyY > 0)
            {
                mask[UpIdx] = 1;
            }
            if (emptyY < size - 1)
            {
                mask[DownIdx] = 1;
            }
            
            while (true)
            {
                int operation = rand.nextInt(4);
                assert(operation < mask.length);
                
                // is the operation allowed and not reverting previous one ?
                if (mask[operation] != 0 && lastOperation != opposites.get(operation))
                {
                    lastOperation = operation;
                    break;
                }
            }
            
            int x = emptyX;
            int y = emptyY;
            // move the empty field according last operation
            if (lastOperation == LeftIdx)
            {
                emptyX--;
            }
            else if (lastOperation == RightIdx)
            {
                emptyX++;
            }
            else if (lastOperation == UpIdx)
            {
                emptyY--;
            }
            else if (lastOperation == DownIdx)
            {
                emptyY++;
            }
            
            board[x][y] = board[emptyX][emptyY];
            board[emptyX][emptyY] = EMPTY_FIELD_IDX;
        }
        
        hasFinished = false;
	}
	
	/**
	 * @return Raster size of this board 
	 */
	public int getSize()
	{
		return size;
	}
	
	/**
	 * @return true if the user finished successfully the puzzle on this board; otherwise false
	 */
	public boolean isFinished()
	{
		return hasFinished;
	}
	
	/**
	 * Gets the puzzle index at specified board position.
	 *
	 * @param x The horizontal board position
	 * @param y The vertical board position
	 * @return The puzzle index at given board position.
	 */
	public int getPuzzleIndexAt(int x, int y)
	{
		if (x >= size || y >= size || x < 0 || y < 0)
		{
			throw new IllegalArgumentException("x or y");
		}
		
		return board[x][y];
	}

	/**
	 * Checks if a field at a position is exchangeable, i.e. empty field is in next to it.
	 *
	 * @param index The packed field index.
	 * @return true, if field is exchangeable; otherwise false
	 */
	public boolean isFieldExchangeable(int index) {
		if (index == -1)
		{
			return false;
		}
		
		int [] indexes = PuzzleActivity.int2Indexes(index, size);
		return isFieldExchangeable(indexes[0], indexes[1]);
	}
	
	
	/**
	 * Checks if a field at a position is exchangeable, i.e. empty field is in next to it.
	 *
	 * @param i The horizontal index.
	 * @param j The vertical index.
	 * @return true, if field is exchangeable; otherwise false
	 */
	public boolean isFieldExchangeable(int i, int j) {
		if (hasFinished)
		{
			return false;
		}
		
		if (i > 0 && getPuzzleIndexAt(i - 1, j) == EMPTY_FIELD_IDX)
		{
			return true;
		}
		
		if (i < size - 1 && getPuzzleIndexAt(i + 1, j) == EMPTY_FIELD_IDX)
		{
			return true;
		}
		
		if (j > 0 && getPuzzleIndexAt(i, j - 1) == EMPTY_FIELD_IDX)
		{
			return true;
		}
		
		if (j < size - 1 && getPuzzleIndexAt(i, j + 1) == EMPTY_FIELD_IDX)
		{
			return true;
		}
		
		return false;
	}

	/**
	 * Performs the exchange of field at specified index and empty field.
	 *
	 * @param index The index
	 */
	public void exchange(int index) {
		int [] indexes = PuzzleActivity.int2Indexes(index, size);
		assert isFieldExchangeable(indexes[0], indexes[1]);
		
		int x = indexes[0];
		int y = indexes[1];
		
		if (x > 0 && getPuzzleIndexAt(x - 1, y) == EMPTY_FIELD_IDX)
		{
			board[x - 1][y] = board[x][y];
		} 
		else if (x < size -1 && getPuzzleIndexAt(x + 1, y) == EMPTY_FIELD_IDX)
		{
			board[x + 1][y] = board[x][y];
		}
		else if (y > 0 && getPuzzleIndexAt(x, y - 1) == EMPTY_FIELD_IDX)
		{
			board[x][y - 1] = board[x][y];
		}
		else if (y < size - 1 && getPuzzleIndexAt(x, y + 1) == EMPTY_FIELD_IDX)
		{
			board[x][y + 1] = board[x][y];
		}
		else
		{
			assert false;
		}
		
		board[x][y] = EMPTY_FIELD_IDX;
		
		checkAndHandleGameOver();
	}
	
	/**
	 * Checks if the state of the board means successful finish of the puzzle and if yes shows congratulations. 
	 */
	private void checkAndHandleGameOver()
	{
		if (board[size - 1][size - 1] != EMPTY_FIELD_IDX)
		{
			// nothing to do - the board is correct when the empty field is right bottom
			return;
		}
		
		Boolean checkOk = true;
		for (int i = 0; i < (size * size) - 1; i++)
        {
            if (board[i / size][i % size] != i)
            {
            	checkOk = false;
            	break;
            }
        }
		
		if (checkOk)
		{
			// the game is over
			hasFinished = true;
			
			// show toast for congratulations
			AndroidExtensions.showCenteredToast(puzzleActivity.getApplicationContext(), R.string.congratulations, true);
		}
	}
}
