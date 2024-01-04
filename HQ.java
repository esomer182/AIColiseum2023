package myplayer;

import aic2023.user.*;

public class HQ {
    UnitController uc;
    float priority_pitcher, priority_batter;
    int curr_round;
    UnitType next_type;
    public void run(UnitController uc1) {
        uc = uc1;
        uc.write(0, uc.getLocation().x);
        uc.write(1, uc.getLocation().y);
        uc.write(2, 0);
        uc.write(3, -1);
        uc.write(4, -1);
        priority_pitcher = 33;
        priority_batter = 67;
        next_type = UnitType.BATTER;
        Direction[] all_directions = Direction.values();
        boolean[] used = new boolean[8];
        int cnt = 0;
        while(cnt < 8){
            int ind = (int)(Math.random() * 8);
            if(!used[ind]){
                uc.write(10 + cnt, ind);
                cnt++;
                used[ind] = true;
                Direction dir2 = all_directions[ind].opposite();
                for(int i = 0; i < 8; i++){
                    if(all_directions[i] == dir2){
                        used[i] = true;
                        uc.write(10 + cnt, i);
                        cnt++;
                    }
                }
            }
        }
        while(true){
            int rand = (int)(Math.random() * 1000000);
            if(rand == 0) uc.killSelf();
            int cnt_pitchers = uc.read(18);
            int cnt_batters = uc.read(19);
            uc.write(18, 0); uc.write(19, 0);
            uc.write(24, cnt_batters);
            uc.write(7, 0);
            curr_round = uc.getRound();

            if(curr_round % 10 == 0){
                reset_bases_and_stadiums(5, 30000);
                reset_bases_and_stadiums(6, 50000);
            }

            float ratio = uc.read(5) + uc.read(6);
            ratio /= (float)(uc.read(7));

            priority_pitcher = priority_pitcher * ratio;
            if(priority_pitcher > 45) priority_pitcher = 45;
            priority_batter = 100 - priority_pitcher;

            if(cnt_batters == 0){
                next_type = UnitType.BATTER;
            }else{
                double total = cnt_pitchers + cnt_batters;
                double ratio_batters = (cnt_batters / total) * 100.0;
                if(ratio_batters < priority_batter) next_type = UnitType.BATTER;
                else next_type = UnitType.PITCHER;
            }

            while(try_recruit()){
                if(next_type == UnitType.BATTER) cnt_batters++;
                else cnt_pitchers++;
                double total = cnt_pitchers + cnt_batters;
                double ratio_batters = (cnt_batters / total) * 100.0;
                if(ratio_batters < priority_batter) next_type = UnitType.BATTER;
                else next_type = UnitType.PITCHER;
            }
            discover();
            priority_pitcher = 33;
            priority_batter = 67;
            uc.yield();
        }
    }

    protected void reset_bases_and_stadiums(int ind1, int ind){
        int cnt = uc.read(ind1);
        for(int i = ind + 2; i < ind + cnt * 3; i += 3) {
            if(uc.read(i) == 1) uc.write(i, 2);
        }
        uc.write(7, 0);
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
        if(uc.getEnergyLeft() >= 500 && curr_round <= 5) discover_not_random(type);
        if(uc.getEnergyLeft() >= 500 && curr_round <= 5) discover_not_random(1 - type);
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
        if(uc.read(coord) == 1) return true;
        else return false;
    }

    protected void mark(int locx, int locy, int number) {
        int coord = locx * 120 + locy + 10000;
        uc.write(coord, number);
    }

    public boolean try_recruit() {
        UnitType type = next_type;
        Direction[] directions = Direction.values();
        for(int t = 0; t < 16; t++){
            Direction dir = directions[(int)(Math.random() * 8)];
            if(uc.canRecruitUnit(type, dir)){
                uc.recruitUnit(type, dir);
                return true;
            }
        }
        return false;
    }
}
