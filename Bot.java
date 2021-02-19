package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

// import javax.swing.text.Position;
// import javax.swing.text.StyledEditorKit.BoldAction;

// import jdk.javadoc.internal.doclets.toolkit.taglets.ReturnTaglet;


public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    private boolean ngumpul = false;

    private float SHOOTRANGE = 5;

    // private boolean adaWormSaya = false;
    // private Worm wormYangTerhalangi = null;

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
                    if (currentWorm.health <= 60)
                    {
                        BananaCounter++;
                        return new BananaCommand(enemy.position.x, enemy.position.y);
                    }
                    else
                    {
                        boolean adaEnemyLain = false;
                        int enemyID = enemy.id;
                        for(Worm e : opponent.worms)
                        {
                            if (e.id != enemyID)
                            {
                                float dista = Distance(e.position, enemy.position);
                                if (dista <= 3)
                                {
                                    BananaCounter++;
                                    return new BananaCommand((int)(enemy.position.x + e.position.x)/2, (int)(enemy.position.y + e.position.y)/2);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        boolean adaMusuhDiPosisiPlusEx = false;
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) {
            adaMusuhDiPosisiPlusEx = true;
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);

            Position pos = new Position(currentWorm.position.x + direction.x, currentWorm.position.y + direction.y);
            boolean adaDirtMenghalangi = false;
            while(Distance(pos, enemyWorm.position) != 0 && !adaDirtMenghalangi)
            {
                if (gameState.map[pos.y][pos.x].type == CellType.DIRT)
                {
                    adaDirtMenghalangi = true;
                }
                else
                {
                    pos.x += direction.x;
                    pos.y += direction.y;
                }
            }
            
            if (!adaDirtMenghalangi)
                return new ShootCommand(direction);
        }

        // List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        // int cellIdx = random.nextInt(surroundingBlocks.size());

        // Cell block = surroundingBlocks.get(0);

        int xTarget = 17;
        int yTarget = 17;

        boolean found = false;
        Worm tg = getEnemyTarget(currentWorm);

        if (tg != null)
        {
            xTarget = tg.position.x;
            yTarget = tg.position.y;
            found = true;
        }
        
        if (found)
        {

            int x = xTarget - currentWorm.position.x;
            int y = yTarget - currentWorm.position.y;
            
            if (!adaMusuhDiPosisiPlusEx)
            {
                if (x < y)
                    yTarget = currentWorm.position.y;
                else
                    xTarget = currentWorm.position.x;
            }
            
        }
        else
        {
            if (!ngumpul)
            {
                int n = getNumberOfWorm();
                int xCenter = 0;
                int yCenter = 0;
                
                if (n > 0)
                {    
                    for(int i = 0; i < 3; i++)
                    {
                        if (gameState.myPlayer.worms[i].health > 0)
                        {
                            xCenter += gameState.myPlayer.worms[i].position.x;
                            yCenter += gameState.myPlayer.worms[i].position.y;
                        }    
                    }

                    xCenter = xCenter / n;
                    yCenter = yCenter / n;
                }

                Position p = new Position(xCenter, yCenter);
                if (Distance(currentWorm.position, p) <= 7)
                {
                    ngumpul = true;
                }
                else
                {
                    xTarget = xCenter;
                    yTarget = yCenter;
                }
            }
            
            if (ngumpul)
            {
                Position healthPack = getNearHealthPack(currentWorm.position);
                if (healthPack != null)
                {
                    xTarget = healthPack.x;
                    yTarget = healthPack.y;
                }
                else
                {
                    Worm targetEnemy = getEnemyWormStillAlive();
                    xTarget = targetEnemy.position.x;
                    yTarget = targetEnemy.position.y;
                }
            }

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

    private Position getNearHealthPack(Position P)
    {
        try {
            ArrayList<Cell> healthPackCell = new ArrayList<>();
            for(int i = 0; i < gameState.mapSize; i++)
            {
                for(int j = 0; j < gameState.mapSize; j++)
                {
                    if (gameState.map[j][i].powerUp.value > 0)
                    {
                        healthPackCell.add(gameState.map[j][i]);
                    }
                }
            }

            if (healthPackCell.size() > 0)
            {
                int idxMin = 0;
                for (int i = 1; i < healthPackCell.size(); i++)
                {
                    Position cellPosition = new Position(healthPackCell.get(i).x, healthPackCell.get(i).y);
                    Position cellPositionMin = new Position(healthPackCell.get(idxMin).x, healthPackCell.get(idxMin).y);

                    float dis = Distance(P, cellPosition);
                    float disMin = Distance(P, cellPositionMin);
                    if (dis < disMin)
                    {
                        idxMin = i;
                    }
                }

                return new Position(healthPackCell.get(idxMin).x, healthPackCell.get(idxMin).y);
            }
            else
            {
                return null;
            }
        } 
        catch (Exception e) 
        {
            return null;
        }
        
    }

    private int getNumberOfWorm()
    {
        int n = 0;
        for(Worm w : gameState.myPlayer.worms)
        {
            if (w.health > 0)
                n++;
        }

        return n;
    }

    private Worm getEnemyWormStillAlive()
    {
        int i = 2;
        while(i > -1)
        {
            if (opponent.worms[i].health > 0)
                return opponent.worms[i];
            
            i--;
        }

        return null;
    }


    private int[] getOpponentWormHP()
    {
        int HP[] = new int[3];

        for (int i = 0; i < 3; i++) 
        {
            HP[i] = opponent.worms[i].health;
        }

        return HP;
    }

    private List<Integer> getSolutionSet(Position p)
    {
        List<Integer> solution = new ArrayList<>();

        for (Worm enemy : opponent.worms) 
        {
            float dis = Distance(p, enemy.position);
            
            if (dis <= 4 && enemy.health > 0)
                solution.add(enemy.id);
        }

        return solution;
    }

    private Worm getEnemyTarget(Worm p)
    {
        List<Integer> target = getSolutionSet(p.position);
        
        if (target.size() > 0)
        {
            int idxMin = 0;
            for(int i = 1; i < target.size()-1; i++)
            {
                if (opponent.worms[target.get(idxMin)-1].health > opponent.worms[target.get(i)-1].health)
                {
                    idxMin = i;
                }
            }

            return opponent.worms[target.get(idxMin)-1];
        }
        else
        {
            return null;
        }
    }


    private Worm getFirstWormInRange() 
    {
        
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
                    else
                    {
                        // wormYangTerhalangi = enemyWorm;
                    }
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
