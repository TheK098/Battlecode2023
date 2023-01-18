package plswork;

import battlecode.common.*;
import battlecode.world.Well;

import java.awt.*;
import java.util.*;

import static plswork.RobotPlayer.*;
public class Carrier {
    static int combineCoords(MapLocation loc) {
        return (loc.x << 8) | loc.y;
    }

    static MapLocation separateCoords(int loc) {
        return new MapLocation(loc >> 8, loc & 0b11111111);
    }

    static MapLocation decompressCoords(int x, int y) {
        return new MapLocation(5*x + 2, 5*y + 2);
    }
    static void runCarrier(RobotController rc) throws GameActionException {
        Team myTeam = rc.getTeam();

        MapLocation currLoc = rc.getLocation();
        int turn = rc.readSharedArray(0);
        MapLocation spawn = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
        //hardcoded to go to hq (+1,+1) TODO: change so its not hardcoded
        MapLocation adjSpawn = new MapLocation(rc.readSharedArray(1)+1, rc.readSharedArray(2)+1);
        //TODO: spawn near closest well
        //WHEN SPAWN TRY TO PICK UP THE ANCHOR AT ALL TIMES
        if(turn>=2 && rc.canTakeAnchor(currLoc.add(Direction.WEST), Anchor.STANDARD)){
            rc.takeAnchor(currLoc.add(Direction.WEST), Anchor.STANDARD);
        }
        // IF BOT SEES WELL ADD IT TO COMMS
        WellInfo[] wells = rc.senseNearbyWells();
        if(wells.length>0){

        }

            // IF BOT HAS ANCHOR:
        if (rc.getAnchor() != null) {
            // If I have an anchor, go towards sky islands

            int[] islands = rc.senseNearbyIslands();
            MapLocation[] arrIslands= {}; int unoccupiedIslandID=-1;
            for(int i=0; i<islands.length; i++){
                if(rc.senseTeamOccupyingIsland(islands[i])!=myTeam.opponent() && rc.senseTeamOccupyingIsland(islands[i])!=myTeam){
                    unoccupiedIslandID = islands[i];
                    break;
                }
            }
            if(islands.length>0 && unoccupiedIslandID!=-1){
                arrIslands = rc.senseNearbyIslandLocations(islands[0]);
            }

            if (arrIslands.length > 0) {
                while (!rc.getLocation().equals(arrIslands[0]) && islands.length>0) {
                    islands = rc.senseNearbyIslands();
                    unoccupiedIslandID=-1;
                    for(int i=0; i<islands.length; i++){
                        if(rc.senseTeamOccupyingIsland(islands[i])!=myTeam.opponent() && rc.senseTeamOccupyingIsland(islands[i])!=myTeam){
                            unoccupiedIslandID = islands[i];
                            break;
                        }
                    }
                    if(islands.length>0 && unoccupiedIslandID!=-1){
                        arrIslands = rc.senseNearbyIslandLocations(islands[0]);
                    }
                    currLoc = rc.getLocation();
                    Direction dir = currLoc.directionTo(arrIslands[0]);
                    rc.setIndicatorString("coords: x: " + arrIslands[0].x+ "y: "+ arrIslands[0].y);
                    //TODO: optimize path finding for obstacles, clouds, and currents (try 0/1 bfs)
                    if (rc.canMove(dir)) {
                        rc.setIndicatorString("yp: " + dir);
                        rc.move(dir);
                    }
                    else if (rc.canMove(dir.rotateLeft())){
                        rc.move(dir.rotateLeft());
                    }
                    if (rc.canPlaceAnchor()) {
                        rc.placeAnchor();
                    }
                    //REPEAT MOVE
                    dir = currLoc.directionTo(arrIslands[0]);
                    rc.setIndicatorString("coords: x: " + arrIslands[0].x+ "y: "+ arrIslands[0].y);
                    //TODO: optimize path finding for obstacles, clouds, and currents (try 0/1 bfs)
                    if (rc.canMove(dir)) {
                        rc.setIndicatorString("yp: " + dir);
                        rc.move(dir);
                    }
                    else if (rc.canMove(dir.rotateLeft())){
                        rc.move(dir.rotateLeft());
                    }
                    else if (rc.canMove(dir.rotateRight())){
                        rc.move(dir.rotateRight());
                    }
                    if (rc.canPlaceAnchor()) {
                        rc.placeAnchor();
                    }
                }
                if (rc.canPlaceAnchor()) {
                    rc.placeAnchor();
                }
            }
            else{
                Direction dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
                dir = directions[rng.nextInt(directions.length)];
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        }

        // IF NO ANCHOR:
        else if(rc.getAnchor()==null) {
            // If bot doesn't have an anchor, look for wells & move towards it
            if (rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ADAMANTIUM) < 39) {
                wells = rc.senseNearbyWells();
                if (wells.length > 0) {
                    // TODO: sort by dist to well
                    rc.setIndicatorString(" num wells near me " + wells.length);
                    currLoc = rc.getLocation();
                    SortedMap<Integer, WellInfo> distWell = new TreeMap<>();
                    int calcDist = -1;
                    for(int i=0; i<wells.length;i++){
                        calcDist = currLoc.distanceSquaredTo(wells[i].getMapLocation());
                        distWell.put(new Integer(calcDist), wells[i]);
                    }
                    // goAd is do we want to go for AD or MN
                    //TODO: change to faster random
                    boolean goAd = true;
                    if(Math.floor(Math.random() *(3 - 1 + 1) + 1) % 3==0){
                        goAd = false;
                    }
                    WellInfo well_one = wells[0];

                    for(WellInfo well : distWell.values()){
                        if(goAd && well.getResourceType()==ResourceType.ADAMANTIUM){
                            well_one = well;
                            break;
                        }
                        else if(!goAd && well.getResourceType()==ResourceType.MANA){
                            well_one = well;
                            break;
                        }
                    }
                    Direction dir = currLoc.directionTo(well_one.getMapLocation());
                    if (rc.canMove(dir)) {
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
                    dir = currLoc.directionTo(well_one.getMapLocation());
                    if (rc.canMove(dir)) {
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
                    dir = directions[rng.nextInt(directions.length)];
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                }



//      if bot is next to well, get resources until full
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        MapLocation wellLocation = new MapLocation(currLoc.x + dx, currLoc.y + dy);
                        if (rc.canCollectResource(wellLocation, -1)) {
                            rc.collectResource(wellLocation, -1);
                            rc.setIndicatorString("Collecting, now have, AD:" +
                                    rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                                    " MN: " + rc.getResourceAmount(ResourceType.MANA) +
                                    " EX: " + rc.getResourceAmount(ResourceType.ELIXIR));
                        }
                    }
                }
                RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, myTeam.opponent());
                if (enemyRobots.length > 0) {
                    if (rc.canAttack(enemyRobots[0].location)) {
                        rc.attack(enemyRobots[0].location);
                    }
                }
            }
            //  if bot is full on resources, head back to HQ & deposit
            else if (rc.getResourceAmount(ResourceType.MANA) + rc.getResourceAmount(ResourceType.ADAMANTIUM) >= 39) {
                currLoc = rc.getLocation();
                RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, myTeam.opponent());
                if (enemyRobots.length > 0) {
                    if (rc.canAttack(enemyRobots[0].location)) {
                        rc.attack(enemyRobots[0].location);
                    }
                }
                if (!currLoc.equals(adjSpawn)) {
                    rc.setIndicatorString("I'm going to x: " + adjSpawn.x + " y: " + adjSpawn.y);
                    currLoc = rc.getLocation();
                    Direction dir = currLoc.directionTo(adjSpawn);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                    else if(rc.canMove(dir.rotateLeft())){
                            rc.move(dir.rotateLeft());
                    }
                    else{
                        dir = directions[rng.nextInt(directions.length)];
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                        }
                    }
                    dir = currLoc.directionTo(adjSpawn);
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                    }
                    else if(rc.canMove(dir.rotateLeft())){
                        rc.move(dir.rotateLeft());
                    }
                    else{
                        dir = directions[rng.nextInt(directions.length)];
                        if (rc.canMove(dir)) {
                            rc.move(dir);
                        }
                    }
                }
                if (rc.canTransferResource(spawn, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM))){
                    rc.transferResource(spawn, ResourceType.ADAMANTIUM, rc.getResourceAmount(ResourceType.ADAMANTIUM));
                }
                if (rc.canTransferResource(spawn, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA))) {
                    rc.transferResource(spawn, ResourceType.MANA, rc.getResourceAmount(ResourceType.MANA));
                }
            }
        }
        // TODO: use communication array to store unusual tiles in map
        //  we can use it to find symmetry type -> atk enemy hq
        // we can also use it to explore unexplored areas. ideally we should just figure out the rotation of the map
        // then explore half the map & populate the map accordingly
    }
}

