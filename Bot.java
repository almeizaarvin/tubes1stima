package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;


public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    private static int BananaCounter = 0;
    private static int SnowballCounter = 0;

    private static int freezeCooldown = 0;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        freezeCooldown--;

        if (currentWorm.id == 3 && SnowballCounter < 3)
        {
            if (freezeCooldown <= 0)
            {
                for (Worm enemy : opponent.worms) 
                {
                    float dis = Distance(enemy.position, currentWorm.position);
                    if (dis <= 5 && dis > 2 && enemy.health > 0)
                    {
                        SnowballCounter++;
                        freezeCooldown = 4;
                        return new SnowballCommand(enemy.position.x, enemy.position.y);
                    }
                }
            }
        }


        if (currentWorm.id == 2 && BananaCounter < 3)
        {
            for (Worm enemy : opponent.worms) 
            {
                float dis = Distance(enemy.position, currentWorm.position);
                if (dis <= 5 && dis > 2 && enemy.health > 0)
                {
                    BananaCounter++;
                    return new BananaCommand(enemy.position.x, enemy.position.y);
                }
            }
        }
        
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            
            return new ShootCommand(direction);
        }

        // List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        // int cellIdx = random.nextInt(surroundingBlocks.size());

        // Cell block = surroundingBlocks.get(0);

        int xTarget = 17;
        int yTarget = 17;

        boolean found = false;
        int i = 2;
        while(i > -1 && !found)
        {
            if (opponent.worms[i].health > 0)
            {
                xTarget = opponent.worms[i].position.x -1;
                yTarget = opponent.worms[i].position.y +1;
                found = true;
            }

            i--;
        }

        int xDir = xTarget - currentWorm.position.x;
        int yDir = yTarget - currentWorm.position.y;

        int xDel = (int) Math.round(xDir / Math.sqrt(xDir*xDir + yDir*yDir));
        int yDel = (int) Math.round(yDir / Math.sqrt(xDir*xDir + yDir*yDir));

        int xtar = currentWorm.position.x + xDel;
        int ytar = currentWorm.position.y + yDel;
        Cell block = gameState.map[ytar][xtar];

        
       
        if (block.type == CellType.AIR) 
        {
            return new MoveCommand(block.x, block.y);
        } 
        else if (block.type == CellType.DIRT) {

            int xPrev = block.x;
            int yPrev = block.y;
            
            int xFinalTarget = block.x;
            int yFinalTarget = block.y;

            int xArahTarget = block.x - currentWorm.position.x;
            int yArahTarget = block.y - currentWorm.position.y;

            if (xArahTarget * yArahTarget == 0)
            {
                if (xArahTarget == 0)
                {
                    Cell c1 = gameState.map[yFinalTarget][xFinalTarget-1];
                    Cell c2 = gameState.map[yFinalTarget][xFinalTarget+1];
                    
                    if (c1.type == CellType.AIR)
                        xFinalTarget -= 1;
                    else if (c2.type == CellType.AIR)
                        xFinalTarget += 1;
                }
                else
                {
                    Cell c1 = gameState.map[yFinalTarget-1][xFinalTarget];
                    Cell c2 = gameState.map[yFinalTarget+1][xFinalTarget];
                    
                    if (c1.type == CellType.AIR)
                        yFinalTarget -= 1;
                    else if (c2.type == CellType.AIR)
                        yFinalTarget += 1;
                }
            }
            else
            {
                Cell c1 = gameState.map[yFinalTarget][xFinalTarget + xArahTarget*-1];
                Cell c2 = gameState.map[yFinalTarget + yArahTarget*-1][xFinalTarget];
            
                if (c1.type == CellType.AIR)
                    xFinalTarget += xArahTarget * -1;
                else if (c2.type == CellType.AIR)
                    yFinalTarget += yArahTarget * -1;
            }

            if (xPrev != xFinalTarget || yPrev != yFinalTarget)
                return new MoveCommand(xFinalTarget, yFinalTarget);
            else
                return new DigCommand(xFinalTarget, yFinalTarget);
        }

        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                
                boolean adaWormSaya = false;
                int i = 0;
                while(i < 3 && !adaWormSaya)
                {
                    String adaWormSayaPosition = String.format("%d_%d", gameState.myPlayer.worms[i].position.x, gameState.myPlayer.worms[i].position.y);
                    if (cells.contains(adaWormSayaPosition) && gameState.myPlayer.worms[i].health > 0)
                        adaWormSaya = true;
                    
                    i++;
                }
                
                if (adaWormSaya)
                {
                    float enemyDistance = Distance(currentWorm.position, enemyWorm.position);
                    float allyDistance = Distance(gameState.myPlayer.worms[i-1].position, currentWorm.position);

                    if (enemyDistance < allyDistance)
                        return enemyWorm;
                }
                else
                    return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private float Distance(Position a, Position b)
    {
        int xDir = b.x - a.x;
        int yDir = b.y - a.y;

        return (float)Math.sqrt(xDir*xDir + yDir*yDir);
    }
}
