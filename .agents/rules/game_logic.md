# MathyMove - Game Logic & Scoring Mechanics

This document outlines the core rules, graph topology, move mechanics, solvability guarantees, and scoring system of **MathyMove**.

---

## 1. Overview & Core Concept

**MathyMove** is a graph-based mathematical puzzle game. The player starts at a root number node and navigates an expanding tree of radiating mathematical nodes (alternating between operators and numbers) to transform their `currentValue` into a `targetNumber` within a limited move budget.

---

## 2. Graph Topology & Node Mechanics

### Node Types
- **`NUMBER` Nodes**: Contain an integer value. When visited following an operator, they perform arithmetic evaluation on `currentValue`.
- **`OPERATOR` Nodes**: Contain a mathematical operator symbol (`+`, `-`, `x`, `÷`). When visited, they set the `pendingOperator` for the next move.

### Tree Radiance & Spacing
- **Spacing**: Node centers are separated by a constant distance of `346f` units (`NODE_DISTANCE`).
- **Radiating Angles**:
  - **Root Node (Depth 0)**: Radiates 3 branches at absolute angles: `0°`, `120°`, and `240°`.
  - **Child Nodes (Depth > 0)**: Radiate 3 sub-branches relative to parent direction angle: `parentAngle - 60°`, `parentAngle`, and `parentAngle + 60°`.

### Branch Diversity Constraints
- **Operator Diversity**: The 3 child operator nodes branching from a number node select distinct operators from `{ "+", "-", "x", "÷" }` to prevent duplicate choices.
- **Number Diversity**: The 3 child number nodes branching from an operator node select distinct integers from `1..10`.

### Node States & Memory Pruning
- **Visited State**: Tapping an adjacent unvisited node marks it as `visited = true`. Visited nodes are greyed out and cannot be re-tapped.
- **BFS Tree Pruning**:
  - Distances from `activeNodeId` are computed using Breadth-First Search (BFS).
  - Nodes within distance \(\le 3\) are retained to allow smooth visual fading.
  - Nodes at distance \(\ge 4\) are automatically pruned from state and memory.

---

## 3. Move Execution & Arithmetic Flow

### Step-by-Step Traversal
1. **Adjacency Check**: A node can only be tapped if it is unvisited and directly connected (parent or child) to the `activeNodeId`.
2. **Operator Tapping**:
   - Tapping an `OPERATOR` node updates `pendingOperator` to that operator.
   - Increment total moves taken and moves taken for the current target.
3. **Number Tapping**:
   - Tapping a `NUMBER` node applies the `pendingOperator` to `currentValue` and the tapped number.
   - Reset `pendingOperator` to `null`.
   - Update the tapped node's displayed value to the newly calculated `currentValue`.

### Division & Remainder Handling
- Standard arithmetic supported: Addition (`+`), Subtraction (`-`), Multiplication (`x`/`*`), Division (`÷`/`/`).
- **Division with Remainder**:
  - If dividing `currentValue` by a number results in a non-integer decimal:
    - Round calculation to 1 decimal place.
    - Set integer floor as the new `currentValue`.
    - Extract fractional decimal remainder (e.g., `"0.3"`) and drop it as an animated `DroppedRemainder` visual toast element on screen.
- Division by zero returns `currentValue` unchanged without error.

---

## 4. Solvability Engine & Golden Path

### Guaranteed Solvability
- When expanding new child branches, `SolvabilityEngine.solvePath()` generates a sequence of \((Operator, Number)\) steps that bridges `currentValue` to `targetNumber`.
- At least **one golden branch** among the 3 generated child paths is guaranteed to follow this calculated solution path, ensuring every target is always mathematically solvable within the move budget.

### Difficulty Scaling
- **Total Moves < 200**:
  - Target Number Range: `1..100`
  - Moves Budget Range: `6..10` pairs (12..20 moves)
- **Total Moves ≥ 200**:
  - Target Number Range: `100..400`
  - Moves Budget Range: `10..14` pairs (20..28 moves)

---

## 5. Scoring Mechanics & High Scores

### Target Achievement & Score Addition
- A target is achieved when `currentValue == targetNumber`.
- **Score Calculation**: When the target is reached, the target's numeric value is added directly to the total score:
  $$\text{Score}_{\text{new}} = \text{Score}_{\text{old}} + \text{TargetNumber}$$

### High Score Persistence
- High scores are saved to `DataStore` key `high_scores_json` tagged with `gameTimestamp` and formatted date/time.
- The repository retains and presents the **Top 10 highest scores**.

---

## 6. Move Budgeting & Reload Rules

### Initial Budget
- Each new game begins with a move budget (default `14` moves).

### Target Reload & Bonus Moves
When `currentValue == targetNumber`:
1. Calculate remaining unused moves for the completed target:
   $$\text{Moves}_{\text{remaining}} = \max(0, \text{MovesBeforeCalculation} - \text{MovesTakenForTarget})$$
2. Add **8 bonus moves** to the remaining moves to form the new target's move budget:
   $$\text{MovesBeforeCalculation}_{\text{new}} = \text{Moves}_{\text{remaining}} + 8$$
3. Reset `movesTakenForTarget` to `0`.
4. Generate a new `targetNumber`.

---

## 7. Defeat Condition (Game Over)

A player loses the game (`isGameOver = true`) when both of the following conditions are met:
1. `movesRemainingForTarget == 0` (where \(\text{movesRemainingForTarget} = \max(0, \text{movesBeforeCalculation} - \text{movesTakenForTarget})\))
2. `currentValue != targetNumber`

### Consequences of Game Over
- The final score is recorded in the High Scores list.
- Active saved game state is cleared from `DataStore`.
- UI displays the Game Over screen presenting the final score and option to start a new game.
