//Morgan Wei && Zihan Li

package chess;

import java.util.ArrayList;

public class Chess {

        enum Player { white, black }
    
	/**
	 * Plays the next move for whichever player has the turn.
	 * 
	 * @param move String for next move, e.g. "a2 a3"
	 * 
	 * @return A ReturnPlay instance that contains the result of the move.
	 *         See the section "The Chess class" in the assignment description for details of
	 *         the contents of the returned ReturnPlay instance.
	 */
	public static ReturnPlay play(String move) {

		/* FILL IN THIS METHOD */



        if (board == null) start();


        if (gameOver) {
            return buildReturn(ReturnPlay.Message.ILLEGAL_MOVE);
        }

        move = move.trim();


        if (move.equals("draw") && drawOffered) {
            gameOver = true;
            return buildReturn(ReturnPlay.Message.DRAW);
        }


        if (move.equals("draw")) {
            return buildReturn(ReturnPlay.Message.ILLEGAL_MOVE);
        }


        drawOffered = false;

        ParsedMove m;
        try {
            m = parse(move);
        } catch (Exception e) {
            return buildReturn(ReturnPlay.Message.ILLEGAL_MOVE);
        }

        if (m.resign) {
            gameOver = true;
            return buildReturn(turn == Player.white
                    ? ReturnPlay.Message.RESIGN_BLACK_WINS
                    : ReturnPlay.Message.RESIGN_WHITE_WINS);
        }

        // legality
        if (!isLegalMoveForTurn(m)) {
            return buildReturn(ReturnPlay.Message.ILLEGAL_MOVE);
        }

        // execute on real board
        Piece p = board.get(m.r1, m.c1);
        applyMove(board, m, p);


        if (m.drawRequest) {
            drawOffered = true;
        }

        // switch turn
        turn = other(turn);
        Color enemy = playerColor(turn);

        boolean enemyInCheck = isInCheck(board, enemy);
        boolean enemyHasMoves = hasAnyLegalMove(enemy);

        if (enemyInCheck && !enemyHasMoves) {
            gameOver = true;
            return buildReturn(enemy == Color.WHITE
                    ? ReturnPlay.Message.CHECKMATE_BLACK_WINS
                    : ReturnPlay.Message.CHECKMATE_WHITE_WINS);
        }

        if (enemyInCheck) {
            return buildReturn(ReturnPlay.Message.CHECK);
        }

        return buildReturn(null);


		
		/* FOLLOWING LINE IS A PLACEHOLDER TO MAKE COMPILER HAPPY */
		/* WHEN YOU FILL IN THIS METHOD, YOU NEED TO RETURN A ReturnPlay OBJECT */
		// return null;
	}
	
	
	/**
	 * This method should reset the game, and start from scratch.
	 */
	public static void start() {
		/* FILL IN THIS METHOD */

        board = new Board();
        turn = Player.white;
        gameOver = false;
        drawOffered = false;
        epTargetR = epTargetC = epPawnR = epPawnC = -1;

        // pawns
        for (int c = 0; c < 8; c++) {
            board.set(6, c, new Piece('p', Color.WHITE));
            board.set(1, c, new Piece('p', Color.BLACK));
        }
        // back rank
        char[] order = {'r','n','b','q','k','b','n','r'};
        for (int c = 0; c < 8; c++) {
            board.set(7, c, new Piece(order[c], Color.WHITE));
            board.set(0, c, new Piece(order[c], Color.BLACK));
        }

	}



    static Board board;
    static Player turn;
    static boolean gameOver;
    static boolean drawOffered;

    // en passant tracking
    // target square is where capturing pawn would land
    static int epTargetR = -1, epTargetC = -1;
    // pawn square to remove if en passant capture happens
    static int epPawnR = -1, epPawnC = -1;

    static int fileToCol(char f) { return f - 'a'; }
    static int rankToRow(char r) { return 8 - (r - '0'); }
    static boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    static Color playerColor(Player p) { return p == Player.white ? Color.WHITE : Color.BLACK; }
    static Player other(Player p) { return p == Player.white ? Player.black : Player.white; }

    //  parsing
    static class ParsedMove {
        boolean resign;
        boolean drawRequest;
        int r1, c1, r2, c2;
        Character promotion; // 'Q','R','B','N' or null
    }

    static ParsedMove parse(String moveRaw) {
        String move = moveRaw.trim();
        ParsedMove pm = new ParsedMove();

        if (move.equals("resign")) {
            pm.resign = true;
            return pm;
        }

        String[] parts = move.split("\\s+");
        // expected: 2 tokens, or 3 tokens , promotion piece OR draw?
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid move format");
        }
        String from = parts[0];
        String to = parts[1];

        pm.c1 = fileToCol(from.charAt(0));
        pm.r1 = rankToRow(from.charAt(1));
        pm.c2 = fileToCol(to.charAt(0));
        pm.r2 = rankToRow(to.charAt(1));

        if (parts.length >= 3) {
            if (parts[2].equals("draw?")) {
                pm.drawRequest = true;
            } else {
                char pr = Character.toUpperCase(parts[2].charAt(0));
                if (pr == 'Q' || pr == 'R' || pr == 'B' || pr == 'N') {
                    pm.promotion = pr;   // Only 4 valid promotion pieces
                } else {
                    // illegal promotion piece, but we won't reject as illegal here; just ignore the promotion part and let legality check handle it
                    pm.promotion = null;
                }
            }
        }
        // Sometimes "e7 e8 Q draw?" could exist; spec implies draw? is 3rd token,
        // but let's can be lenient:
        if (parts.length >= 4 && parts[3].equals("draw?")) pm.drawRequest = true;

        return pm;
    }

    // helpers
    static boolean pathClear(Board b, int r1, int c1, int r2, int c2) {
        int dr = Integer.signum(r2 - r1);
        int dc = Integer.signum(c2 - c1);
        int r = r1 + dr, c = c1 + dc;
        while (r != r2 || c != c2) {
            if (b.get(r, c) != null) return false;
            r += dr; c += dc;
        }
        return true;
    }

    static int[] findKing(Board b, Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = b.get(r, c);
                if (p != null && p.color == color && p.type == 'k') return new int[]{r,c};
            }
        return null;
    }

    static boolean isSquareAttacked(Board b, int tr, int tc, Color byColor) {
        // check if any piece of byColor attacks (tr, tc)
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                Piece p = b.get(r, c);
                if (p == null || p.color != byColor) continue;
                if (attacksSquare(b, r, c, tr, tc)) return true;
            }
        return false;
    }

    static boolean attacksSquare(Board b, int r1, int c1, int r2, int c2) {
        Piece p = b.get(r1, c1);
        if (p == null) return false;
        int dr = r2 - r1, dc = c2 - c1;

        switch (p.type) {
            case 'p': {
                int dir = (p.color == Color.WHITE) ? -1 : 1;
                return dr == dir && Math.abs(dc) == 1;
            }
            case 'n':
                return (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case 'b':
                return Math.abs(dr) == Math.abs(dc) && pathClear(b, r1, c1, r2, c2);
            case 'r':
                return (dr == 0 || dc == 0) && pathClear(b, r1, c1, r2, c2);
            case 'q':
                return ((dr == 0 || dc == 0) || Math.abs(dr) == Math.abs(dc)) && pathClear(b, r1, c1, r2, c2);
            case 'k':
                return Math.max(Math.abs(dr), Math.abs(dc)) == 1;
        }
        return false;
    }

    static boolean isInCheck(Board b, Color color) {
        int[] k = findKing(b, color);
        if (k == null) return false;
        Color opp = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return isSquareAttacked(b, k[0], k[1], opp);
    }

    // move legality (pseudo-legal)
    static boolean isCastlingAttempt(Piece king, int r1, int c1, int r2, int c2) {
        if (king == null || king.type != 'k') return false;
        return r1 == r2 && Math.abs(c2 - c1) == 2;
    }

    static boolean canCastle(Board b, Piece king, int r, int cFrom, int cTo) {
        if (king.hasMoved) return false;
        Color color = king.color;
        Color opp = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;

        // cannot castle out of check
        if (isInCheck(b, color)) return false;

        boolean kingSide = (cTo > cFrom);
        int rookCol = kingSide ? 7 : 0;
        Piece rook = b.get(r, rookCol);
        if (rook == null || rook.type != 'r' || rook.color != color || rook.hasMoved) return false;

        // squares between must be empty
        int step = kingSide ? 1 : -1;
        for (int c = cFrom + step; c != rookCol; c += step) {
            if (b.get(r, c) != null) return false;
        }

        // squares king passes through must not be attacked: from, through, to
        int cThrough = cFrom + step;
        if (isSquareAttacked(b, r, cThrough, opp)) return false;
        if (isSquareAttacked(b, r, cTo, opp)) return false;

        return true;
    }

    static boolean pseudoLegal(Board b, ParsedMove m, Piece p) {
        if (!inBounds(m.r1, m.c1) || !inBounds(m.r2, m.c2)) return false;

        Piece dest = b.get(m.r2, m.c2);
        if (dest != null && dest.color == p.color) return false;

        int dr = m.r2 - m.r1, dc = m.c2 - m.c1;

        // castling
        if (isCastlingAttempt(p, m.r1, m.c1, m.r2, m.c2)) {
            return canCastle(b, p, m.r1, m.c1, m.c2);
        }

        switch (p.type) {
            case 'p': {
                int dir = (p.color == Color.WHITE) ? -1 : 1;

                // forward
                if (dc == 0) {
                    if (dest != null) return false;
                    if (dr == dir) return true;
                    if (!p.hasMoved && dr == 2 * dir) {
                        int midR = m.r1 + dir;
                        if (b.get(midR, m.c1) == null) return true;
                    }
                    return false;
                }

                // diagonal capture
                if (Math.abs(dc) == 1 && dr == dir) {
                    if (dest != null) return true;

                    // en passant capture: move diagonally to empty square equals epTarget
                    if (m.r2 == epTargetR && m.c2 == epTargetC) return true;
                }
                return false;
            }
            case 'r':
                return (dr == 0 || dc == 0) && pathClear(b, m.r1, m.c1, m.r2, m.c2);
            case 'b':
                return Math.abs(dr) == Math.abs(dc) && pathClear(b, m.r1, m.c1, m.r2, m.c2);
            case 'q':
                return ((dr == 0 || dc == 0) || Math.abs(dr) == Math.abs(dc)) && pathClear(b, m.r1, m.c1, m.r2, m.c2);
            case 'n':
                return (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2);
            case 'k':
                return Math.max(Math.abs(dr), Math.abs(dc)) == 1;
        }
        return false;
    }

    // apply move
    static void applyMove(Board b, ParsedMove m, Piece p) {

        // save old en-passant state for legality checks and en-passant capture handling; we'll update the global en-passant state as we apply the move, but we want to keep the old state intact for legality checks and for determining if an en-passant capture is happening
        int oldEpTargetR = epTargetR, oldEpTargetC = epTargetC;
        int oldEpPawnR = epPawnR, oldEpPawnC = epPawnC;


        if (isCastlingAttempt(p, m.r1, m.c1, m.r2, m.c2)) {
            boolean kingSide = (m.c2 > m.c1);
            int rookFromC = kingSide ? 7 : 0;
            int rookToC = kingSide ? (m.c1 + 1) : (m.c1 - 1);

            Piece rook = b.get(m.r1, rookFromC);

            b.set(m.r2, m.c2, p);
            b.set(m.r1, m.c1, null);
            p.hasMoved = true;

            b.set(m.r1, rookToC, rook);
            b.set(m.r1, rookFromC, null);
            rook.hasMoved = true;


            epTargetR = epTargetC = epPawnR = epPawnC = -1;
            return;
        }

        // ---- en passant capture
        if (p.type == 'p' && m.c1 != m.c2 && b.get(m.r2, m.c2) == null) {
            if (m.r2 == oldEpTargetR && m.c2 == oldEpTargetC) {
                b.set(oldEpPawnR, oldEpPawnC, null);
            }
        }

        // normal move or capture: if this move creates a new en-passant target, we'll set the global en-passant variables accordingly; if not, we'll clear them
        epTargetR = epTargetC = epPawnR = epPawnC = -1;

        // normal move / capture
        b.set(m.r2, m.c2, p);
        b.set(m.r1, m.c1, null);
        p.hasMoved = true;

        // pawn double move -> New epTarget
        if (p.type == 'p' && Math.abs(m.r2 - m.r1) == 2) {
            int dir = (p.color == Color.WHITE) ? -1 : 1;
            epTargetR = m.r1 + dir;
            epTargetC = m.c1;
            epPawnR = m.r2;
            epPawnC = m.c2;
        }

        // promotion if pawn reaches last rank
        if (p.type == 'p') {
            if ((p.color == Color.WHITE && m.r2 == 0) || (p.color == Color.BLACK && m.r2 == 7)) {
                char promo = (m.promotion == null) ? 'Q' : Character.toUpperCase(m.promotion);
                char newType;
                switch (promo) {
                    case 'R': newType = 'r'; break;
                    case 'B': newType = 'b'; break;
                    case 'N': newType = 'n'; break;
                    case 'Q':
                    default:  newType = 'q'; break;
                }
                Piece np = new Piece(newType, p.color);
                np.hasMoved = true;
                b.set(m.r2, m.c2, np);
            }
        }
    }

    // full legality with self-check
    static boolean isLegalMoveForTurn(ParsedMove m) {
        if (gameOver) return false;

        Piece p = board.get(m.r1, m.c1);
        if (p == null) return false;

        Color me = playerColor(turn);
        if (p.color != me) return false;

        if (!pseudoLegal(board, m, p)) return false;

        // simulate on a copied board so the real board is never modified
        int saveEpTR = epTargetR, saveEpTC = epTargetC, saveEpPR = epPawnR, saveEpPC = epPawnC;

        Board sim = board.copy();
        Piece simP = sim.get(m.r1, m.c1);

        // IMPORTANT: applyMove uses the global en-passant vars, so we keep them the same
        applyMove(sim, m, simP);

        boolean leavesCheck = isInCheck(sim, me);

        // restore en-passant globals (in case applyMove changed them)
        epTargetR = saveEpTR; 
        epTargetC = saveEpTC; 
        epPawnR = saveEpPR; 
        epPawnC = saveEpPC;

        return !leavesCheck;
    }

    static boolean hasAnyLegalMove(Color color) {
        Player savedTurn = turn;
        int saveEpTR = epTargetR, saveEpTC = epTargetC, saveEpPR = epPawnR, saveEpPC = epPawnC;
        Board savedBoard = board;

        // set turn to match the color for legality check
        turn = (color == Color.WHITE) ? Player.white : Player.black;

        for (int r1 = 0; r1 < 8; r1++) {
            for (int c1 = 0; c1 < 8; c1++) {
                Piece p = board.get(r1, c1);
                if (p == null || p.color != color) continue;

                for (int r2 = 0; r2 < 8; r2++) {
                    for (int c2 = 0; c2 < 8; c2++) {
                        ParsedMove m = new ParsedMove();
                        m.r1 = r1; m.c1 = c1; m.r2 = r2; m.c2 = c2;

                        // if pawn promotion square, test with default (queen) to see existence of any legal
                        if (p.type == 'p' && ((color == Color.WHITE && r2 == 0) || (color == Color.BLACK && r2 == 7))) {
                            m.promotion = 'Q';
                        }

                        if (isLegalMoveForTurn(m)) {
                            // restore
                            turn = savedTurn;
                            epTargetR = saveEpTR; epTargetC = saveEpTC; epPawnR = saveEpPR; epPawnC = saveEpPC;
                            board = savedBoard;
                            return true;
                        }
                    }
                }
            }
        }

        // restore
        turn = savedTurn;
        epTargetR = saveEpTR; epTargetC = saveEpTC; epPawnR = saveEpPR; epPawnC = saveEpPC;
        board = savedBoard;
        return false;
    }

    // ReturnPlay builder
    static ReturnPlay buildReturn(ReturnPlay.Message msg) {
        ReturnPlay rp = new ReturnPlay();
        rp.piecesOnBoard = new ArrayList<>();
        rp.message = msg;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.get(r, c);
                if (p == null) continue;

                ReturnPiece out = new ReturnPiece();
                out.pieceRank = 8 - r;
                out.pieceFile = ReturnPiece.PieceFile.values()[c];

                String prefix = (p.color == Color.WHITE) ? "W" : "B";
                char up = Character.toUpperCase(p.type);
                // knight uses N
                String typeStr = prefix + (up == 'P' ? "P"
                                  : up == 'R' ? "R"
                                  : up == 'N' ? "N"
                                  : up == 'B' ? "B"
                                  : up == 'Q' ? "Q"
                                  : "K");
                out.pieceType = ReturnPiece.PieceType.valueOf(typeStr);

                rp.piecesOnBoard.add(out);
            }
        }
        return rp;
    }

}

/* ====== Added below to make Chess.java self-contained for Autolab (single-file compile) ====== */

enum Color { WHITE, BLACK }

class Piece {
    char type; // 'p','r','n','b','q','k'
    Color color;
    boolean hasMoved;

    Piece(char type, Color color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }
}

class Board {
    Piece[][] b = new Piece[8][8];

    Piece get(int r, int c) { return b[r][c]; }
    void set(int r, int c, Piece p) { b[r][c] = p; }

    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = this.get(r, c);
                if (p != null) {
                    b.set(r, c, new Piece(p.type, p.color));
                    b.get(r, c).hasMoved = p.hasMoved;
                }
            }
        }
        return b;
    }

    void loadFrom(Board src) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = src.get(r, c);
                if (p == null) {
                    this.set(r, c, null);
                } else {
                    Piece np = new Piece(p.type, p.color);
                    np.hasMoved = p.hasMoved;
                    this.set(r, c, np);
                }
            }
        }
    }
}