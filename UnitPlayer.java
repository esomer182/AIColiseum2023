package myplayer;

import aic2023.user.UnitController;
import aic2023.user.UnitType;

/*
Array:
{0, 1} = Location of my base.
2 = General id.
{3, 4} = His base.
5 = Nº of stadiums.
6 = Nº of bases.
7 = Nº of stadiums I have.
8 = Nº of bases I have.
9 = ID OF BATTER.
[10, 17], the order of directions.
18 = Curr_number of pitchers.
19 = Curr_number of batters.
20 = smaller x coordinate that's out of map.
21 = bigger x coordinate that's out of map.
22 = smaller y coordinate that's out of map.
23 = bigger y coordinate that's out of map.
24 = lst_cnt batters.

From 10000, info about the map.
From 30000, info about discovered stadiums.
From 50000, info about discovered bases.
 */

/*
Ideas for the next day:
1) Get better radar, for both objects and enemies.
2) Make the bats go towards enemies.
3) Sometimes there are inaccessible things, make the initial state 0, instead of 2. (THIS IS DONE).
4) My pathfinding is horrible

/*
TODO:
The main thing now is improving the bats, so I probably want to improve the
try_escape function. The problem is that I don't know how to complement it with
my path-finding.

I also need to fix some bug with the pitchers' path-finding, see https://www.coliseum.ai/games/1799285?lang=ca
Also adjust batter's strategy, so that they also go to the bases when it's 0.
 */
public class UnitPlayer {

    public void run(UnitController uc) {
        /*Insert here the code that should be executed only at the beginning of the unit's lifespan*/

        HQ hq = new HQ();
        PITCHER pitch = new PITCHER();
        BATTER batt = new BATTER();
        if (uc.getType() == UnitType.HQ) {
            hq.run(uc);
        } else if (uc.getType() == UnitType.PITCHER) {
            pitch.run(uc);
        } else if(uc.getType() == UnitType.BATTER) {
            batt.run(uc);
        }
    }
}