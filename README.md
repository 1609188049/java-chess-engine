# Java Chess Engine

A Java chess engine that parses coordinate moves (e.g., `e2 e4`) and enforces full rule legality with game-state tracking and board simulation to prevent self-check.

## Features
- Legal move validation for all pieces
- Check / checkmate detection
- Castling (king-side & queen-side)
- En passant
- Pawn promotion (default: Queen; supports Q/R/B/N)
- Draw offers and resignations

## Project Structure
Chess.java
PlayChess.java
ReturnPiece.java
ReturnPlay.java


## How to Run (Windows PowerShell)
From the repository root:

```powershell
javac -d out chess\*.java
java -cp out chess.PlayChess

Move Format

Examples:
e2 e4
g8 f6
b7 b8
resign
reset
draw?
draw
