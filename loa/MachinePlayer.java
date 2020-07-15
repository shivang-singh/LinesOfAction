/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;


import java.util.List;


import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static loa.Piece.*;

/** An automated Player.
 *  @author Shivang Singh
 */
class MachinePlayer extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new MachinePlayer with no piece or controller (intended to produce
     *  a template). */
    MachinePlayer() {
        this(null, null);
    }

    /** A MachinePlayer that plays the SIDE pieces in GAME. */
    MachinePlayer(Piece side, Game game) {
        super(side, game);
    }

    @Override
    String getMove() {
        Move choice;

        assert side() == getGame().getBoard().turn();
        int depth;
        choice = searchForMove();
        getGame().reportMove(choice);
        return choice.toString();
    }

    @Override
    Player create(Piece piece, Game game) {
        return new MachinePlayer(piece, game);
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private Move searchForMove() {
        Board work = new Board(getBoard());
        int subFactor = work.getmoveLimit() - work.movesMade();
        int depth;
        if (subFactor < 3) {
            depth = subFactor;
        } else {
            depth = chooseDepth();
        }
        int value;
        assert side() == work.turn();
        _foundMove = null;
        if (side() == WP) {
            value = findMove(work, depth, true, 1, -INFTY, INFTY);
        } else {
            value = findMove(work, depth, true, -1, -INFTY, INFTY);
        }
        return _foundMove;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        Piece curTurn = board.turn();
        if (depth == 0) {
            return heuristic(board);
        }
        int bestScore = 0;
        List<Move> l = board.legalMoves();
        for (Move m: l) {

            board.makeMove(m);
            int score = findMove(board, depth - 1,
                        false, sense * -1, alpha, beta);
            int oldScore = bestScore;
            if (sense == -1) {
                bestScore = INFTY;
                bestScore = min(bestScore, score);
                beta = min(score, beta);
            } else if (sense == 1) {
                bestScore = -INFTY;
                bestScore = max(bestScore, score);
                alpha = max(score, alpha);
            }
            if (saveMove) {
                if (curTurn == WP && bestScore >= oldScore) {
                    _foundMove = m;
                } else if (curTurn == BP && bestScore <= oldScore) {
                    _foundMove = m;
                }
            }
            board.retract();
            if (beta <= alpha) {
                break;
            }
        }
        return bestScore;
    }

    /** Return a search depth for the current position. */
    private int chooseDepth() {
        return 3;
    }

    /** Calculate the best move based on turn */

    /** Assigns a heuristic value to the board.
     * @param board is the current board.
     * @return */
    private int heuristic(Board board) {
        Board curBoard = board;
        Piece curTurn = curBoard.turn();
        if (curBoard.piecesContiguous(curTurn)) {
            if (curTurn == BP) {
                return -INFTY;
            } else {
                return INFTY;
            }
        } else if (curBoard.piecesContiguous(curTurn.opposite())) {
            if (curTurn == BP) {
                return INFTY;
            } else {
                return -INFTY;
            }
        }
        double[] factor1 = board.avgDistanceToCOM();
        int sizeB = board.getRegionSizes(BP).size();
        int sizeW = board.getRegionSizes(WP).size();
        int maxB = board.getRegionSizes(BP).get(0);
        int maxW = board.getRegionSizes(WP).get(0);

        int heurB = (int) (100 * factor1[0]) - (sizeB * 10) + maxB;
        int heurW = (int) (100 * factor1[1]) - (sizeW * 10) + maxW;

        return heurB - heurW;
    }

    /** Used to convey moves discovered by findMove. */
    private Move _foundMove;



}
