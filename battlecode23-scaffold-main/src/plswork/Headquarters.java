package plswork;
import battlecode.common.*;
import static plswork.RobotPlayer.*;

public class Headquarters extends Base{
    public Headquarters(RobotController rc) {
        super(rc);
    }
    static MapLocation newLoc;
    @Override
    public void runPls() throws GameActionException {
        if(turnCount <= 3){
            initializeComms();
            findWellsUpdateComms();
        }
        for(int i=0; i<64; i++){
            System.out.print(rc.readSharedArray(i));
        }
        System.out.println(" turn: " + turnCount);
        System.out.println();

        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        newLoc = rc.getLocation().add(dir);

        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            // If we can build an anchor do it!
            rc.buildAnchor(Anchor.STANDARD);
            rc.setIndicatorString("Building anchor! ");

        }
        if (rc.canBuildRobot(RobotType.CARRIER, newLoc)){
            rc.buildRobot(RobotType.CARRIER, newLoc);
            rc.setIndicatorString("building carrier");
        }
        if(rc.canBuildRobot(RobotType.LAUNCHER, newLoc)){
            rc.buildRobot(RobotType.LAUNCHER, newLoc);
            rc.setIndicatorString("building launcher");
        }

    }

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
                        break;
                    }
                }
            }
        }
    }

    void initializeComms() throws GameActionException{
        for(int i=0; i<64; i++){
            if(rc.readSharedArray(i) == 65535) break;
            rc.writeSharedArray(i, 65535);
        }
        for(int i=60; i<64; i++){
            if(rc.readSharedArray(i) == 65535){
                rc.writeSharedArray(i, combineCoords(rc.getLocation()));
                rc.setIndicatorString(" " + rc.readSharedArray(i));
                break;
            }
        }
    }

    public static int combineCoords(MapLocation loc) {
        return (loc.x << 8) | loc.y;
    }
    //TODO: add search for nearest wells
}
