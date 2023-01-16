package plswork;

import battlecode.common.*;

import java.util.SortedMap;
import java.util.TreeMap;

import static plswork.RobotPlayer.*;

public class Launcher extends Base {

    private static MapLocation currentLoc;
    public Launcher(RobotController rc) {
        super(rc);
    }

    public void runPls() throws GameActionException {
        currentLoc = rc.getLocation();

        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.setIndicatorString("Attacking");
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        MapLocation currLoc = rc.getLocation();
        WellInfo[] wells = rc.senseNearbyWells();
        if (wells.length > 0) {
            // TODO: sort by dist to well
            rc.setIndicatorString(" num wells near me " + wells.length);
            MapLocation well_one = findNearestWell();
            currLoc = rc.getLocation();
            Direction dir = currLoc.directionTo(well_one.add(Direction.NORTHEAST).add(Direction.NORTHEAST));
            if (rc.canMove(dir) && !withinRange(rc.getLocation(), well_one, 9)) { // checks if too close to well
                rc.move(dir);
            } else if (rc.canMove(dir.rotateLeft())) {
                rc.move(dir.rotateLeft());

            } else if (rc.canMove(dir.rotateRight())) {
                rc.move(dir.rotateRight());

            } else {
                dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }
        else {
//              If we don't see anything nearby go randomly TODO: optimize this to go towards unexplored areas
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }

    static boolean withinRange(MapLocation a, MapLocation b, int range) {
        return a.distanceSquaredTo(b) < range;
    }

    public static MapLocation findNearestWell() throws GameActionException{
        WellInfo[] wells = rc.senseNearbyWells();
        SortedMap<Integer, MapLocation> distWell = new TreeMap<>();
        boolean goAd = true;

        int calcDist = -1;
        if (wells.length > 0) {
            for (int i = 0; i < wells.length; i++) {
                calcDist = currentLoc.distanceSquaredTo(wells[i].getMapLocation());
                distWell.put(new Integer(calcDist), wells[i].getMapLocation());
            }
        }
        for (int i = 0; i < 60; i++) {
            if (rc.readSharedArray(i) == 65535) break;
            calcDist = currentLoc.distanceSquaredTo(separateCoords(rc.readSharedArray(i)));
            distWell.put(new Integer(calcDist), separateCoords(rc.readSharedArray(i)));
        }
        if(distWell.isEmpty()) return new MapLocation(-1, -1);
        MapLocation targetWell = null;
        for(MapLocation wellLoc : distWell.values()){
            targetWell = wellLoc;
            break;
        }
        // goAd is do we want to go for AD or MN, we randomly choose which to go for rn
        //TODO: change to faster random
        // TODO: reIMPLEMENT GOING FOR AD/MN
//        if (Math.floor(Math.random() * (3 - 1 + 1) + 1) % 3 == 0) {
//            goAd = false;
//        }
//        WellInfo well_one = null;
//        for (WellInfo well : distWell.values()) {
//            if (goAd && well.getResourceType() == ResourceType.ADAMANTIUM) {
//                well_one = well;
//                break;
//            } else if (!goAd && well.getResourceType() == ResourceType.MANA) {
//                well_one = well;
//                break;
//            }
//        }
        return targetWell;
    }

    public static int combineCoords(MapLocation loc) {
        return (loc.x << 8) | loc.y;
    }

    public static MapLocation separateCoords(int loc) {
        return new MapLocation(loc >> 8, loc & 0b11111111);
    }
}
