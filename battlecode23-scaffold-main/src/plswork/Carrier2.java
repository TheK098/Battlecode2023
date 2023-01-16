package plswork;
import java.util.*;
//helsdlfaiodfa
import static plswork.RobotPlayer.*;
import plswork.Base;
import battlecode.common.*;
public class Carrier2 extends Base{
    private static MapLocation currentLoc = null;
    private static Team myTeam = null;
    public Carrier2(RobotController rc){
        super(rc);
    }
    @Override
    public void runPls() throws GameActionException {
        currentLoc = rc.getLocation();
        myTeam = rc.getTeam();
        // comms -> if sees well -> put into arr (TODO: add skyislands to comms & caching)
        findWellsUpdateComms();

        // if can pick anchor, pick & become anchor-carrier bot
        tryPickAnchor();

        // if anchor, find sky islands & go to them
        ifAnchorGoToIsland();

        // if no anchor, go to wells. come back when at 39.

    }


    // finds all nearby wells & updates it to comms
    // TODO: if out of range, store well location & bring back to base later
    static void findWellsUpdateComms() throws GameActionException{
        //TODO: this is complete brute force — will optimize later (also implement caches)
        WellInfo wells[] = rc.senseNearbyWells();
        if(wells.length>0){
            for(WellInfo well: wells){
                int wellCoords = combineCoords(well.getMapLocation());
                for(int i=0; i<60; i++){
                    int currCoords = rc.readSharedArray(i);
                    if(currCoords == wellCoords){
                        break;
                    }
                    else if(currCoords == 65535){
                        rc.writeSharedArray(i, wellCoords);
                    }
                }

            }
        }
    }
    public static int combineCoords(MapLocation loc) {
        return (loc.x << 8) | loc.y;
    }

    public static MapLocation separateCoords(int loc) {
        return new MapLocation(loc >> 8, loc & 0b11111111);
    }
    public static void tryPickAnchor() throws GameActionException{
        //WHEN SPAWN TRY TO PICK UP THE ANCHOR AT ALL TIMES
        rc.setIndicatorString(rc.readSharedArray(60) + " f " + rc.readSharedArray(61) + " f " + rc.readSharedArray(62) + " f " + rc.readSharedArray(63)+ " f ");
        for(int i=60; i<64; i++){
            if(rc.readSharedArray(i)==65535) {
                break;
            }
            MapLocation headquarters = separateCoords(rc.readSharedArray(i));
            if(rc.canTakeAnchor(headquarters, Anchor.STANDARD)){
                rc.takeAnchor(headquarters, Anchor.STANDARD);
                break;
            }
        }
    }

    //TODO: change this so it goes for the nearest island
    //TODO: add islands to comm array and then to this as well
    public static MapLocation findTargetIslandLocation() throws GameActionException{
        int[] nearbyIslandsArr = rc.senseNearbyIslands();

        int targetIslandID=-1;
        for(int i=0; i<nearbyIslandsArr.length; i++){
            //if island i is not taken by me or opponent
            if(rc.senseTeamOccupyingIsland(nearbyIslandsArr[i])!=myTeam.opponent()
                    && rc.senseTeamOccupyingIsland(nearbyIslandsArr[i]) != myTeam){
                targetIslandID = nearbyIslandsArr[i];
                break;
            }
        }
        MapLocation[] targetIslandLocs = {};
        if(targetIslandID !=-1){
            targetIslandLocs = rc.senseNearbyIslandLocations(targetIslandID);
            return targetIslandLocs[0];
        }
        return new MapLocation(-1, -1);
    }

    public static void ifAnchorGoToIsland() throws GameActionException{
        if (rc.getAnchor() != null) {
            // If I have an anchor, find islands, if found go towards one, else go random
            MapLocation targetIslandLocation = findTargetIslandLocation();
            // if island not found:
            if(targetIslandLocation.x <0){
                randomMove();
                //move again
                currentLoc = rc.getLocation();
                targetIslandLocation = findTargetIslandLocation();
                if(targetIslandLocation.x < 0){
                    randomMove();
                }
                else{
                    smartMove(targetIslandLocation);
                }
            }

            else{
                //if found island:
                smartMove(targetIslandLocation);
                currentLoc = rc.getLocation();
                smartMove(targetIslandLocation);
            }
        }
    }


    //TODO: optimize path finding for obstacles, clouds, and currents (try 0/1 bfs)
    public static void smartMove(MapLocation destination) throws GameActionException {
        Direction dir = currentLoc.directionTo(destination);
        if(rc.canMove(dir)){
            rc.move(dir);
        } else if (rc.canMove(dir.rotateLeft())) {
            rc.move(dir.rotateLeft());
        }
        else if(rc.canMove(dir.rotateRight())){
            rc.move(dir.rotateRight());
        }
        else{
            randomMove();
        }
        if (rc.canPlaceAnchor()) {
            rc.placeAnchor();
        }
    }

    public static void randomMove() throws GameActionException{
        Direction dir = directions[rng.nextInt(directions.length)];
        while(!rc.canMove(dir)){
            dir = directions[rng.nextInt(directions.length)];
        }
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        if (rc.getAnchor()!=null && rc.canPlaceAnchor()) {
            rc.placeAnchor();
        }
    }


}
