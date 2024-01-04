package myplayer;

import aic2023.user.*;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Math.abs;

public class BATTER {
    UnitController uc;
    int current_objective, rotation_dir;
    Location curr_rand, past1, past2, past3;
    Direction rand_dir;
    Direction[] all_directions;
    UnitInfo[] units_round;
    public void run(UnitController uc1) {
        current_objective = -1;
        uc = uc1;
        int id = uc.read(9);
        uc.write(9, id + 1);
        int id_general = uc.read(2); uc.write(2, id_general + 1);
        all_directions = Direction.values();
        rand_dir = all_directions[uc.read(10 + id_general % 8)];
        rotation_dir = (int)(Math.random() * 2);
        curr_rand = null;
        past1 = null;
        past2 = null;
        past3 = null;
        while(true){
            units_round = null;
            uc.write(19, uc.read(19) + 1);
            Location mine = uc.getLocation();
            MapObject place = uc.senseObjectAtLocation(mine, false);
            if(place == MapObject.BASE || place == MapObject.STADIUM) {
                int val = get_val(mine);
                if(val == 0){
                    int number = 1;
                    int coord_base_x = uc.read(0);
                    int coord_base_y = uc.read(1);
                    int locx = mine.x - coord_base_x;
                    int locy = mine.y - coord_base_y;
                    if(place == MapObject.STADIUM){
                        int have = uc.read(5);
                        uc.write(5, have + 1);
                        uc.write(30000 + have * 3, locx + coord_base_x);
                        uc.write(30000 + have * 3 + 1, locy + coord_base_y);
                        int state = 0;
                        number = 30000 + have * 3;
                        uc.write(30000 + have * 3 + 2, state);
                    }
                    if(place == MapObject.BASE){
                        int have = uc.read(6);
                        uc.write(6, have + 1);
                        uc.write(50000 + have * 3, locx + coord_base_x);
                        uc.write(50000 + have * 3 + 1, locy + coord_base_y);
                        int state = 0;
                        number = 50000 + have * 3;
                        uc.write(50000 + have * 3 + 2, state);
                    }
                    mark(locx + 60, locy + 60, number);
                }else{
                    uc.write(val + 2, 0);
                }
                //move_random();
            }
            int curr_round = uc.getRound();
            int number_first = 5;
            int start_check = 30000;
            int number_second = 6;
            int start_check_second = 50000;

            try_to_bat_with_move();
            if(current_objective == -1 || uc.read(current_objective + 2) != 2){
                if(!try_move(number_first, start_check, number_second, start_check_second)) {
                    go_enemy_base();
                    move_random();
                    current_objective = -1;
                }
            }else{
                check_state(current_objective);
                moveTo(new Location(uc.read(current_objective), uc.read(current_objective + 1)));
            }
            try_to_bat();
            discover();
            past1 = past2;
            past2 = past3;
            past3 = uc.getLocation();
            uc.yield();
        }
    }

    protected void try_to_bat(){
        if(!uc.canAct()) return;
        try_to_kill();
        if(!uc.canAct()) return;
        Location mylocation = uc.getLocation();
        Team myTeam = uc.getTeam();
        for(Direction dir : Direction.values()) {
            Location nw = mylocation.add(dir);
            UnitInfo unit = null;
            if (uc.canSenseLocation(nw)) unit = uc.senseUnitAtLocation(nw);
            if (unit != null && unit.getTeam() != myTeam && uc.canBat(dir, 3)) {
                bat(uc.getLocation(), dir, 3);
            }
        }
    }

    public boolean iKill(Location loc, Direction dir){
        Location enemy = loc.add(dir);
        UnitInfo unit = null;
        if(uc.canSenseLocation(enemy)) unit = uc.senseUnitAtLocation(enemy);
        if(unit == null || unit.getTeam() == uc.getTeam()) return false;
        enemy = enemy.add(dir);
        Location mine = uc.getLocation();
        UnitType myType = uc.getType();
        for(int t = 0; t < 3; t++){
            if(uc.canSenseLocation(enemy)){
                unit = uc.senseUnitAtLocation(enemy);
                if(unit != null) return true;
                MapObject obj = uc.senseObjectAtLocation(enemy, false);
                if(obj == MapObject.WATER) return true;
            }else{
                if(mine.distanceSquared(enemy) <= vision_range(myType)) update(enemy);
                if(isOutOfMap(enemy)) return true;
            }
            enemy = enemy.add(dir);
        }
        return false;
    }

    public boolean isOutOfMap(Location loc){
        int x = loc.x;
        int y = loc.y;
        if(uc.read(20) <= x && uc.read(21) >= x && uc.read(22) <= y && uc.read(23) >= y) return false;
        else return true;
    }

    public void try_to_kill(){
        Location mine = uc.getLocation();
        for(Direction dir : all_directions){
            if(dir != Direction.ZERO && !uc.canMove(dir)) continue;
            Location nw = mine;
            if(dir != Direction.ZERO) nw = mine.add(dir);
            for(Direction dir2 : all_directions){
                if(dir2 == Direction.ZERO) continue;
                if(iKill(nw, dir2)){
                    if(dir != Direction.ZERO) {uc.move(dir); resetPathfinding();}
                    if(uc.canBat(dir2, 3)){
                        bat(uc.getLocation(), dir2, 3);
                    }
                    return;
                }
            }
        }
    }

    public boolean heKills(Location loc, Direction dir, Location my_location){
        Location enemy = loc.add(dir);
        boolean me = false;
        if(!enemy.isEqual(my_location)){
            UnitInfo unit = null;
            if(uc.canSenseLocation(enemy)) unit = uc.senseUnitAtLocation(enemy);
            if(unit == null || unit.getTeam() != uc.getTeam()) return false;
        }else me = true;
        enemy = enemy.add(dir);
        Location mine = uc.getLocation();
        UnitType myType = UnitType.BATTER;
        for(int t = 0; t < 3; t++){
            if(enemy.isEqual(my_location)) return true;
            if(uc.canSenseLocation(enemy)){
                UnitInfo unit = uc.senseUnitAtLocation(enemy);
                if(unit != null){
                    return false;
                }
                MapObject obj = uc.senseObjectAtLocation(enemy, false);
                if(obj == MapObject.WATER && me) return true;
            }else{
                if(mine.distanceSquared(enemy) <= vision_range(myType)) update(enemy);
                if(isOutOfMap(enemy) && me) return true;
            }
            enemy = enemy.add(dir);
        }
        return false;
    }

    public boolean canKill(UnitInfo unit, Location mine){
        if(unit.getCurrentActionCooldown() >= 1) return false;
        float movement_cooldown = unit.getCurrentMovementCooldown();
        Location his = unit.getLocation();
        for(Direction dir : all_directions){
            if(dir != Direction.ZERO && movement_cooldown >= 1) continue;
            Location nw = his;
            if(dir != Direction.ZERO) nw = his.add(dir);
            if(nw.isEqual(mine)) continue;
            Direction dir2 = nw.directionTo(mine);
            if(heKills(nw, dir2, mine)) return true;
        }
        return false;
    }

    public boolean my_can_move(Direction dir){
        if(!uc.canMove(dir)) return false;
        Location nw = uc.getLocation().add(dir);
        if(units_round == null) units_round = uc.senseUnits(100, uc.getOpponent());
        for(UnitInfo unit : units_round){
            if(unit.getType() != UnitType.BATTER) continue;
            if(canKill(unit, nw)) return false;
        }
        return true;
    }

    protected void try_to_bat_with_move(){
        if(!uc.canAct()) return;
        try_to_kill();
        if(!uc.canAct()) return;
        Location mine = uc.getLocation();
        Team myTeam = uc.getTeam();
        for(Direction dir : all_directions){
            Location nw = mine.add(dir);
            UnitInfo unit = null;
            if(uc.canSenseLocation(nw)) unit = uc.senseUnitAtLocation(nw);
            if(unit != null && unit.getTeam() != myTeam && uc.canBat(dir, 3)) {
                bat(mine, dir, 3);
            }
        }
        if(!uc.canAct() || !uc.canMove()) return;
        UnitInfo[] units = uc.senseUnits(100, uc.getOpponent());
        int smaller_dist = 100000000;
        UnitInfo target = null;
        for(UnitInfo unit : units){
            UnitType his_type = unit.getType();
            if(his_type == UnitType.HQ || his_type == UnitType.BATTER || unit.getTeam() == uc.getTeam()) continue;
            int dist = mine.distanceSquared(unit.getLocation());
            if(dist < smaller_dist){
                smaller_dist = dist;
                target = unit;
            }
        }
        if(target == null) return;
        moveTo(target.getLocation());
        Direction dir = mine.directionTo(target.getLocation());
        UnitInfo nw_unit = null;
        if(uc.canSenseLocation(mine.add(dir))) nw_unit = uc.senseUnitAtLocation(mine.add(dir));
        if(nw_unit != null && nw_unit.getTeam() != uc.getTeam() && uc.canBat(dir, 3)){
           bat(uc.getLocation(), dir, 3);
        }
    }

    public void bat(Location loc, Direction dir, int strength){
        uc.bat(dir, strength);
        Location his = loc.add(dir);
        int val = get_val(his);
        if(val > 1){
            uc.write(val + 2, 0);
        }
    }

    public void check_state(int current_objective) {
        Location loc = new Location(uc.read(current_objective), uc.read(current_objective + 1));
        if(uc.canSenseLocation(loc)){
            UnitInfo unit = uc.senseUnitAtLocation(loc);
            if(unit != null && unit.getTeam() != uc.getTeam()) uc.write(current_objective + 2, 2);
            else if(unit == null) uc.write(current_objective + 2, 0);
            else uc.write(current_objective + 2, 1);
        }
    }

    protected void go_enemy_base(){
        if(uc.read(3) == -1) return;
        int dist = uc.getLocation().distanceSquared(new Location(uc.read(3), uc.read(4)));
        if(dist <= 13 || dist == 18) return;
        moveTo(new Location(uc.read(3), uc.read(4)));
    }
    protected int get_val(Location mine) {
        int coord_base_x = uc.read(0);
        int coord_base_y = uc.read(1);
        int locx = mine.x - coord_base_x;
        int locy = mine.y - coord_base_y;
        locx += 60; locy += 60;
        int coord = locx * 120 + locy + 10000;
        return uc.read(coord);
    }

    protected boolean try_move(int number_first, int start_check, int number_second, int start_check_second) {
        int num1 = uc.read(number_first);
        Location mine = uc.getLocation();
        int smallest_dist1 = 100000000;
        Location loc1 = null;
        int ind1 = -1;
        for(int i = start_check; i < start_check + 3 * num1; i += 3) {
            check_state(i);
            if(uc.read(i+2) == 2) {
                Location nw = new Location(uc.read(i), uc.read(i+1));
                int dist = mine.distanceSquared(nw);
                if(dist < smallest_dist1){
                    smallest_dist1 = dist;
                    loc1 = nw;
                    ind1 = i;
                }
            }
        }
        num1 = uc.read(number_second);
        start_check = start_check_second;
        for(int i = start_check; i < start_check + 3 * num1; i += 3) {
            check_state(i);
            if(uc.read(i+2) == 2) {
                Location nw = new Location(uc.read(i), uc.read(i+1));
                int dist = mine.distanceSquared(nw);
                if(dist < smallest_dist1){
                    smallest_dist1 = dist;
                    loc1 = nw;
                    ind1 = i;
                }
            }
        }
        if(loc1 != null){
            current_objective = ind1;
            moveTo(loc1);
            return true;
        }
        return false;
    }
    int sum_y(Direction dir){
        if(dir == Direction.NORTH || dir == Direction.NORTHEAST || dir == Direction.NORTHWEST) return 1;
        else if(dir == Direction.ZERO || dir == Direction.EAST || dir == Direction.WEST) return 0;
        else return -1;
    }

    int sum_x(Direction dir){
        if(dir == Direction.EAST || dir == Direction.NORTHEAST || dir == Direction.SOUTHEAST) return 1;
        else if(dir == Direction.ZERO || dir == Direction.SOUTH || dir == Direction.NORTH) return 0;
        else return -1;
    }

    protected void move_random() {
        if(!uc.canMove()) return;
        Location mine = uc.getLocation();
        if((past1 != null && past1.isEqual(mine) && past2 != null && past2.isEqual(mine)) || rand_dir == null){
            rand_dir = all_directions[(int)(Math.random() * 8)];
            curr_rand = new Location(mine.x + sum_x(rand_dir) * 100, mine.y + sum_y(rand_dir) * 100);
        }
        if(curr_rand == null) curr_rand = new Location(mine.x + sum_x(rand_dir) * 100, mine.y + sum_y(rand_dir) * 100);
        moveTo(curr_rand);
    }

    protected void discover_not_random(int type){
        MapObject obj;
        if(type == 0) obj = MapObject.BASE;
        else obj = MapObject.STADIUM;
        Location[] objs = uc.senseObjects(obj, 100);
        int coord_base_x = uc.read(0);
        int coord_base_y = uc.read(1);
        for(Location loc : objs) {
            int locx = loc.x - coord_base_x;
            int locy = loc.y - coord_base_y;
            if(seen(locx + 60, locy + 60)) continue;
            int number = 1;
            if(obj == MapObject.STADIUM){
                int have = uc.read(5);
                uc.write(5, have + 1);
                uc.write(30000 + have * 3, locx + coord_base_x);
                uc.write(30000 + have * 3 + 1, locy + coord_base_y);
                int state = 0;
                number = 30000 + have * 3;
                uc.write(30000 + have * 3 + 2, state);
            }
            if(obj == MapObject.BASE){
                int have = uc.read(6);
                uc.write(6, have + 1);
                uc.write(50000 + have * 3, locx + coord_base_x);
                uc.write(50000 + have * 3 + 1, locy + coord_base_y);
                int state = 0;
                number = 50000 + have * 3;
                uc.write(50000 + have * 3 + 2, state);
            }
            mark(locx + 60, locy + 60, number);
        }
    }
    protected void discover() {
        int type = (int)(Math.random() * 2);
        if(uc.getEnergyLeft() >= 500) discover_not_random(type);
        if(uc.getEnergyLeft() >= 500) discover_not_random(1 - type);
        int coord_base_x = uc.read(0);
        int coord_base_y = uc.read(1);
        Location mine = uc.getLocation();
        UnitType uctype = uc.getType();
        while(uc.getEnergyLeft() >= 200){
            int locx = (int)(Math.random() * 120);
            int locy = (int)(Math.random() * 120);
            if(seen(locx, locy)) continue;
            locx -= 60;
            locy -= 60;
            Location loc = new Location(locx + coord_base_x, locy + coord_base_y);
            if(uc.canSenseLocation(loc)) {
                int number = 1;
                MapObject obj = uc.senseObjectAtLocation(loc, false);
                UnitInfo unit = uc.senseUnitAtLocation(loc);
                if(obj == MapObject.STADIUM){
                    int have = uc.read(5);
                    uc.write(5, have + 1);
                    uc.write(30000 + have * 3, locx + coord_base_x);
                    uc.write(30000 + have * 3 + 1, locy + coord_base_y);
                    int state = 0;
                    if(unit != null){
                        if(unit.getTeam() == uc.getTeam()) state = 1;
                        else state = 2;
                    }
                    number = 30000 + have * 3;
                    uc.write(30000 + have * 3 + 2, state);
                }
                if(obj == MapObject.BASE){
                    int have = uc.read(6);
                    uc.write(6, have + 1);
                    uc.write(50000 + have * 3, locx + coord_base_x);
                    uc.write(50000 + have * 3 + 1, locy + coord_base_y);
                    int state = 0;
                    if(unit != null){
                        if(unit.getTeam() == uc.getTeam()) state = 1;
                        else state = 2;
                    }
                    number = 50000 + have * 3;
                    uc.write(50000 + have * 3 + 2, state);
                }
                if(unit != null) {
                    if(unit.getTeam() != uc.getTeam() && unit.getType() == UnitType.HQ){
                        uc.write(3, locx + coord_base_x);
                        uc.write(4, locy + coord_base_y);
                    }
                }
                mark(locx + 60, locy + 60, number);
            }else{
                if(mine.distanceSquared(loc) <= vision_range(uctype)){
                    update(loc);
                }
            }
        }
    }

    public int vision_range(UnitType type){
        if(type == UnitType.HQ) return 64;
        else if(type == UnitType.CATCHER) return 32;
        else return 20;
    }

    public void update(Location loc){
        int x = loc.x;
        int y = loc.y;
        uc.write(20, min(uc.read(20), x));
        uc.write(21, max(uc.read(21), x));
        uc.write(22, min(uc.read(22), y));
        uc.write(23, max(uc.read(23), y));
    }

    public int min(int a, int b){
        if(a < b) return a;
        else return b;
    }

    public int max(int a, int b){
        if(a > b) return a;
        else return b;
    }

    protected boolean seen(int locx, int locy) {
        int coord = locx * 120 + locy + 10000;
        if(uc.read(coord) != 0) return true;
        else return false;
    }

    protected void mark(int locx, int locy, int number) {
        int coord = locx * 120 + locy + 10000;
        uc.write(coord, number);
    }

    final int INF = 1000000;

    boolean rotateRight = true; //if I should rotate right or left
    Location lastObstacleFound = null; //latest obstacle I've found in my way
    int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
    Location prevTarget = null; //previous target

    protected void moveTo(Location target){
        //No target? ==> bye!
        if (target == null || !uc.canMove()) return;

        //different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetPathfinding();

        //If I'm at a minimum distance to the target, I'm free!
        Location myLoc = uc.getLocation();
        int d = myLoc.distanceSquared(target);
        if (d <= minDistToEnemy) resetPathfinding();

        //Update data
        prevTarget = target;
        minDistToEnemy = Math.min(d, minDistToEnemy);

        //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);

        //This should not happen for a single unit, but whatever
        if (my_can_move(dir)) resetPathfinding();

        //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        for (int i = 0; i < 16; ++i){
            if (my_can_move(dir)){
                uc.move(dir);
                return;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);
            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        if (my_can_move(dir)) uc.move(dir);
    }

    //clear some of the previous data
    protected void resetPathfinding(){
        lastObstacleFound = null;
        minDistToEnemy = INF;
    }
}