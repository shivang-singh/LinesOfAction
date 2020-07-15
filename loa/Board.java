/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static java.lang.Math.min;
import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author Shivang Singh
 */
class Board {

    /** Default number of moves for each side that results in a draw. */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /** Pattern describing a valid square designator (cr). */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /** A Board whose initial contents are taken from INITIALCONTENTS
     *  and in which the player playing TURN is to move. The resulting
     *  Board has
     *        get(col, row) == INITIALCONTENTS[row][col]
     *  Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     *
     *  CAUTION: The natural written notation for arrays initializers puts
     *  the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /** A new board in the standard initial position. */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /** A Board whose initial contents and state are copied from
     *  BOARD. */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /** Set my state to CONTENTS with SIDE to move. */
    void initialize(Piece[][] contents, Piece side) {
        int curBoardPos = 0;
        for (int i = 0; i < contents.length; i++) {
            for (int j = 0; j < contents[i].length; j++) {
                set(sq(j, i), contents[i][j]);
                _board[curBoardPos] = get(sq(j, i));
                curBoardPos++;
            }
        }
        _moves.clear();
        legalMoves().clear();
        turnThatJustWent = null;
        _winnerKnown = false;
        _winner = null;
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        _subsetsInitialized = false;
        _turn = side;
        _moveLimit = DEFAULT_MOVE_LIMIT;
    }

    /** Set me to the initial configuration. */
    void clear() {
        turnThatJustWent = null;
        _turn = null;
        _winner = null;
        _winnerKnown = false;
        _subsetsInitialized = false;

        initialize(INITIAL_PIECES, BP);
    }

    /** Set my state to a copy of BOARD. */
    void copyFrom(Board board) {
        if (board == this) {
            return;
        }
        Piece[] toCopyFrom = board._board;
        for (int i = 0; i < toCopyFrom.length; i++) {
            this._board[i] = toCopyFrom[i];
        }
        this._turn = board._turn;
        this._moveLimit = board._moveLimit;
        this._winnerKnown = board._winnerKnown;
        this._winner = board._winner;
        this._blackRegionSizes.clear();
        for (Integer a: board._blackRegionSizes) {
            this._blackRegionSizes.add(a);
        }
        this._whiteRegionSizes.clear();
        for (Integer b: board._whiteRegionSizes) {
            this._whiteRegionSizes.add(b);
        }
        this._moves.clear();
        for (int m = 0; m < board._moves.size(); m++) {
            this._moves.add(m, board._moves.get(m));
        }
        this._subsetsInitialized = board._subsetsInitialized;
    }

    /** Return the contents of the square at SQ. */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /** Set the square at SQ to V and set the side that is to move next
     *  to NEXT, if NEXT is not null. */
    void set(Square sq, Piece v, Piece next) {
        if (next != null) {
            this._turn = next;
        }
        int index = sq.col() + (sq.row() * 8);
        _board[index] = v;
    }

    /** Set the square at SQ to V, without modifying the side that
     *  moves next. */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /** Set limit on number of moves by each side that results in a tie to
     *  LIMIT, where 2 * LIMIT > movesMade(). */
    void setMoveLimit(int limit) {
        if (2 * limit <= movesMade()) {
            throw new IllegalArgumentException("move limit too small");
        }
        _moveLimit = 2 * limit;
    }

    /** Assuming isLegal(MOVE), make MOVE. Assumes MOVE.isCapture()
     *  is false. */
    void makeMove(Move move) {
        turnThatJustWent = turn();
        if (turn() == WP) {
            this._turn = BP;
        } else if (turn() == BP) {
            this._turn = WP;
        }
        assert isLegal(move);
        Piece p = _board[move.getFrom().col()
                + (8 * move.getFrom().row())];
        int newIndex = move.getTo().col() + (8 * move.getTo().row());
        if (!_board[newIndex].equals(EMP)) {
            move = move.captureMove();
        }
        _board[newIndex] = p;
        _board[move.getFrom().col() + (8 * move.getFrom().row())] = EMP;
        _moves.add(move);
        _subsetsInitialized = false;
    }

    /** Retract (unmake) one move, returning to the state immediately before
     *  that move.  Requires that movesMade () > 0. */
    void retract() {
        assert movesMade() > 0;
        if (turn() == WP) {
            this._turn = BP;
        } else if (turn() == BP) {
            this._turn = WP;
        }
        Move takeBack = this._moves.get(this._moves.size() - 1);
        this._moves.remove(_moves.size() - 1);
        int ogIndexOfMovedPiece = takeBack.getFrom().col()
                                   + (takeBack.getFrom().row() * 8);
        int curIndex = takeBack.getTo().col() + (takeBack.getTo().row() * 8);
        _board[ogIndexOfMovedPiece] = _board[curIndex];
        if (takeBack.isCapture()) {
            if (_board[ogIndexOfMovedPiece] == WP) {
                _board[curIndex] = BP;
            } else if (_board[ogIndexOfMovedPiece] == BP) {
                _board[curIndex] = WP;
            }
        } else {
            _board[curIndex] = EMP;
        }
        _subsetsInitialized = false;
    }

    /** Return the Piece representing who is next to move. */
    Piece turn() {
        return _turn;
    }

    /** Return move limit. */
    int getmoveLimit() {
        return _moveLimit;
    }

    /** Return true iff FROM - TO is a legal move for the player currently on
     *  move. */
    boolean isLegal(Square from, Square to) {
        if (!from.isValidMove(to)) {
            return false;
        } else if (from.distance(to) == 0) {
            return false;
        } else if (blocked(from, to)) {
            return false;
        } else {
            int distance = from.distance(to);
            int dir1 = from.direction(to);
            int dir2 = to.direction(from);
            int count = 1;
            for (int dist = 1; from.moveDest(dir1, dist) != null; dist++) {
                Square toCheck = from.moveDest(dir1, dist);
                if (_board[toCheck.col() + (8 * toCheck.row())] != EMP) {
                    count++;
                }
            }
            for (int dist = 1; from.moveDest(dir2, dist) != null; dist++) {
                Square toCheck = from.moveDest(dir2, dist);
                if (_board[toCheck.col() + (8 * toCheck.row())] != EMP) {
                    count++;
                }
            }
            if (distance == count) {
                return true;
            }
            return false;
        }
    }

    /** Return true iff MOVE is legal for the player currently on move.
     *  The isCapture() property is ignored. */
    boolean isLegal(Move move) {
        if (move == null) {
            return false;
        }
        return isLegal(move.getFrom(), move.getTo());
    }

    /** Return a sequence of all legal moves from this position. */
    List<Move> legalMoves() {
        List<Move> legalMoves = new ArrayList<>();
        for (Square s1: ALL_SQUARES) {
            if (_board[s1.col() + (s1.row() * 8)] == turn()) {
                for (Square s2 : ALL_SQUARES) {
                    if (s1 != s2) {
                        Move potentialMove = Move.mv(s1, s2);
                        if (potentialMove != null && isLegal(potentialMove)) {
                            legalMoves.add(potentialMove);
                        }
                    }
                }
            }
        }
        return legalMoves;
    }

    /** Return true iff the game is over (either player has all his
     *  pieces continguous or there is a tie). */
    boolean gameOver() {
        return winner() != null;
    }

    /** Return true iff SIDE's pieces are continguous. */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /** Return the winning side, if any.  If the game is not over, result is
     *  null.  If the game has ended in a tie, returns EMP. */
    Piece winner() {
        if (turnThatJustWent == BP) {
            if (piecesContiguous(BP)) {
                _winner =  BP;
                _winnerKnown = true;
            } else if (piecesContiguous(WP)) {
                _winner = WP;
                _winnerKnown = true;
            } else if (movesMade() < _moveLimit) {
                _winner = null;
            } else {
                _winner = EMP;
            }
        }
        if (turnThatJustWent == WP) {
            if (piecesContiguous(WP)) {
                _winner = WP;
                _winnerKnown = true;
            } else if (piecesContiguous(BP)) {
                _winner =  BP;
                _winnerKnown = true;
            } else if (movesMade() < _moveLimit) {
                return null;
            } else {
                return EMP;
            }
        }
        return _winner;
    }

    /** Return the total number of moves that have been made (and not
     *  retracted).  Each valid call to makeMove with a normal move increases
     *  this number by 1. */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /** Return true if a move from FROM to TO is blocked by an opposing
     *  piece or by a friendly piece on the target square. */
    private boolean blocked(Square from, Square to) {
        if (_board[from.col()
                + (8 * from.row())].equals(
                        _board[to.col()
                                + (8 * to.row())])) {
            return true;
        }
        int iters = from.distance(to) - 1;
        int dir = from.direction(to);
        boolean toRet = false;
        for (int i = iters; i > 0; i--) {
            Square toComp = from.moveDest(dir, i);
            if (!_board[toComp.col() + (8 * toComp.row())].equals(EMP)) {
                if (!_board[from.col() + (8 * from.row())].
                        equals(_board[toComp.col() + (8 * toComp.row())])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return the size of the as-yet unvisited cluster of squares
     *  containing P at and adjacent to SQ.  VISITED indicates squares that
     *  have already been processed or are in different clusters.  Update
     *  VISITED to reflect squares counted. */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        if (p == EMP) {
            return 0;
        } else if (this._board[sq.col() + (sq.row() * 8)] != p) {
            return 0;
        } else if (visited[sq.row()][sq.col()]) {
            return 0;
        } else {
            visited[sq.row()][sq.col()] = true;
            int count = 1;
            for (Square s: sq.adjacent()) {
                count += numContig(s, visited, p);
            }
            return  count;
        }
    }

    /** Set the values of _whiteRegionSizes and _blackRegionSizes. */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        boolean[][] visited1 = new boolean[8][8];
        boolean[][] visited2 = new boolean[8][8];
        for (Square s: ALL_SQUARES) {
            int val1 = numContig(s, visited1, BP);
            if (val1 != 0) {
                _blackRegionSizes.add(val1);
            }
            int val2 = numContig(s, visited2, WP);
            if (val2 != 0) {
                _whiteRegionSizes.add(val2);
            }

        }
        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /** Return the sizes of all the regions in the current union-find
     *  structure for side S. */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }

    /** Calculate position for COM of piece.
     *
     * @param p is current Side.
     * @return
     */
    double[] getCenterOfMass(Piece p) {
        double xPos = 0;
        double yPos = 0;
        int count = 0;
        for (Square s: ALL_SQUARES) {
            int index = s.col() + (8 * s.row());
            if (_board[index] == p) {
                xPos += s.row();
                yPos += s.col();
                count++;
            }
        }
        double toRet = min(xPos, yPos);
        double axis;
        if (toRet == xPos) {
            axis = 0;
        } else {
            axis = 1;
        }
        return new double[]{toRet, axis, count};
    }

    /** Determines avg distance of pieces to their
     * center of mass.
     * @return
     */
    double[] avgDistanceToCOM() {
        double[] whites = getCenterOfMass(WP);
        double[] blacks = getCenterOfMass(BP);
        double whiteDist = 0;
        double blackDist = 0;
        for (Square s: ALL_SQUARES) {
            int index = s.col() + (8 * s.row());
            if (_board[index] == WP) {
                if (whites[1] == 1) {
                    whiteDist += s.distance(whites[0], s.col());
                } else {
                    whiteDist += s.distance(s.row(), whites[0]);
                }

            } else if (_board[index] == BP) {
                if (blacks[1] == 1) {
                    blackDist += s.distance(blacks[0], s.col());
                } else {
                    blackDist += s.distance(s.row(), blacks[0]);
                }
            }
        }
        blackDist /= blacks[2];
        whiteDist /= whites[2];
        return new double[] {blackDist, whiteDist};
    }


    /** The standard initial configuration for Lines of Action (bottom row
     *  first). */
    static final Piece[][] INITIAL_PIECES = {
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
        { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }
    };

    /** Current contents of the board.  Square S is at _board[S.index()]. */
    private final Piece[] _board = new Piece[BOARD_SIZE  * BOARD_SIZE];

    /** List of all unretracted moves on this board, in order. */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /** Current side on move. */
    private Piece _turn;
    /** Limit on number of moves before tie is declared.  */
    private int _moveLimit;
    /** True iff the value of _winner is known to be valid. */
    private boolean _winnerKnown;
    /** Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     *  in progress).  Use only if _winnerKnown. */
    private Piece _winner;

    /** True iff subsets computation is up-to-date. */
    private boolean _subsetsInitialized;

    /** Piece that just went to calculate winner. */
    private Piece turnThatJustWent;

    /** List of the sizes of continguous clusters of pieces, by color. */
    private final ArrayList<Integer>
        _whiteRegionSizes = new ArrayList<>(),
        _blackRegionSizes = new ArrayList<>();
}
