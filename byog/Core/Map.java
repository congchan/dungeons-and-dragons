package byog.Core;
import byog.TileEngine.TERenderer;
import byog.TileEngine.TETile;
import byog.TileEngine.Tileset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Map implements java.io.Serializable {
    private static final long serialVersionUID = 154934524234354L;
    long SEED;
    Random RANDOM;
    int WIDTH;
    int HEIGHT;
    int NROOM;
    double mu;
    double sigma;
    public TETile[][] canvas;
    Player player;
    Position door;


    Map(int seed, int width, int height) {
        SEED = seed;
        RANDOM = new Random(SEED);
        WIDTH = width;
        HEIGHT = height;
        NROOM = (int) RandomUtils.gaussian(RANDOM, 25, 5);
        mu = 5;
        sigma = 4;
    }


    void initCanvas(TERenderer ter, int offHead){
        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        ter.initialize(WIDTH, HEIGHT + offHead, 0, 2);
    }

    TETile[][] buildMap(int offHead){
        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        initCanvas(ter, offHead);

        // initialize tiles
        canvas = new TETile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                canvas[x][y] = Tileset.NOTHING;
            }
        }

        // make rooms
        ArrayList<Room> roomsList = makeRooms(canvas, NROOM);

        //connect rooms
        connectRooms(canvas, roomsList);

        // build wall
        buildWall(canvas);

        // add door
        door = addDoor(canvas);

        // add players
        player = addPlayer(canvas, 1);

        return canvas;
    }






    /** fill the rectangular space with specific TETile
     * p specify the lower left corner */
    void makeSpace(TETile[][] world, Position p, int width, int height, TETile t){

        for (int i=0; i<width; i++){
            for (int j=0; j<height; j++){
                if (world[i+p.x][j+p.y] == Tileset.NOTHING){
                    world[i+p.x][j+p.y] = t;
                }
            }
        }
    }


    /** connect two rooms*/
    void connectRooms(TETile[][] world, ArrayList<Room> roomsList){
        for (int i=0; i < roomsList.size() - 1; i++){
            Room ra = roomsList.get(i);
            Room rb = roomsList.get(i+1);
            Position pa = new Position(ra.p.x + RANDOM.nextInt(ra.width), ra.p.y + RANDOM.nextInt(ra.height));
            Position pb = new Position(rb.p.x + RANDOM.nextInt(rb.width), rb.p.y + RANDOM.nextInt(rb.height));
            connectPositions(world, pa, pb);
        }
    }

    /** connect two positions*/
    void connectPositions(TETile[][] world, Position a, Position b){
        if (a.x == b.x){
            makeSpace(world, new Position(a.x, Math.min(a.y, b.y)), 1, Math.abs(a.y - b.y) + 1, Tileset.HALLWAY);
        } else if (a.y == b.y) {
            makeSpace(world, new Position(Math.min(a.x, b.x), a.y), Math.abs(a.x - b.x) + 1, 1, Tileset.HALLWAY);
        } else {
            Position dummy = new Position(a.x, b.y);
            connectPositions(world, a, dummy);
            connectPositions(world, b, dummy);
        }

    }

    /** build the walls*/
    void buildWall(TETile[][] world){
        for (int i=0; i < WIDTH; i++){
            for (int j=0; j<HEIGHT; j++){
                if (world[i][j] == Tileset.NOTHING && checkNeighbours(world, i, j, 1)){
                    world[i][j] = Tileset.WALL;
                }
            }
        }
    }


    /** check if new room overlaps with current rooms
     * return true if overlap with anyone of current rooms
     * https://stackoverflow.com/questions/306316/determine-if-two-rectangles-overlap-each-other */
    boolean overlap(ArrayList<Room> rooms, Room ra){
        for (Room rb : rooms){
            if (ra.x1 < rb.x2 && ra.x2 > rb.x1 && ra.y1 > rb.y2 + 1 && ra.y2 + 1 < rb.y1){
                return  true;
            }
        }
        return false;
    }


    /** make rooms */
    ArrayList<Room> makeRooms(TETile[][] world,int num){
        int curNumRooms = 0;
        ArrayList<Room> roomsList = new ArrayList();
        while(curNumRooms < num){
            int px = RandomUtils.uniform(RANDOM,2,WIDTH - 2);
            int py = RandomUtils.uniform(RANDOM,2,HEIGHT - 2);
            int width = (int) Math.max(Math.min(RandomUtils.gaussian(RANDOM,mu,sigma), WIDTH - px - 1), 2);
            int height = (int) Math.max(Math.min(RandomUtils.gaussian(RANDOM,mu,sigma), HEIGHT - py - 1),2);
            Room r = new Room(curNumRooms, new Position(px, py), width, height);
            if (!overlap(roomsList, r)){
                roomsList.add(r);
                makeSpace(world, new Position(px, py), width, height, Tileset.FLOOR);
                curNumRooms += 1;
            }
        }
        Collections.sort(roomsList);
        return roomsList;
    }

    /** Add a locked door */
    Position addDoor(TETile[][] world){
        boolean added = false;
        int startx = 0;
        int starty = 0;
        while (!added) {
            startx = (int) RandomUtils.gaussian(RANDOM, WIDTH/2, WIDTH/5);
            starty = 1;
            while (world[startx][starty]!=Tileset.WALL){
                starty += 1;
            }
            if (checkNeighbours(world, startx, starty, 2)){
                world[startx][starty] = Tileset.LOCKED_DOOR;
                added = true;
            }
        }
        return new Position(startx, starty);
    }

    Player addPlayer(TETile[][] world, int numPlayers){
        int added = 0;
        int px = 0;
        int py = 0;
        while (added < numPlayers){
            px = RandomUtils.uniform(RANDOM,2,WIDTH - 2);
            py = RandomUtils.uniform(RANDOM,2,HEIGHT - 2);
            if(world[px][py] == Tileset.FLOOR){
                world[px][py] = Tileset.PLAYER;
                added += 1;
            }
        }
        return new Player(new Position(px, py));
    }


    /** Check a given position is a valid position for wall or closed door
     * determined by the number of Tileset.FLOOR in all eight neighbours */
    boolean checkNeighbours(TETile[][] world, int x, int y, int numFloors){
        int checked = 0;
        int xLeft = Math.max(0,x - 1);
        int xRight = Math.min(x + 1,WIDTH - 1);
        int yUp = Math.min(y + 1, HEIGHT - 1);
        int yLow = Math.max(0, y - 1);
        for (int i = xLeft; i <= xRight; i++){
            for (int j = yLow; j <= yUp; j++){
                if (world[i][j] == Tileset.FLOOR || world[i][j] == Tileset.HALLWAY){
                    checked += 1;
                    if (checked == numFloors){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int SEED = 2018;

        Random RANDOM = new Random();
        int WIDTH = 80;
        int HEIGHT = 40;
        int NROOM = RandomUtils.poisson(RANDOM, 25);
        double mu = 5;
        double sigma = 4;
        Map map = new Map(SEED, WIDTH, HEIGHT);

        // initialize the tile rendering engine with a window of size WIDTH x HEIGHT
        TERenderer ter = new TERenderer();
        ter.initialize(WIDTH, HEIGHT);

        // initialize tiles
        TETile[][] world = new TETile[WIDTH][HEIGHT];
        for (int x = 0; x < WIDTH; x += 1) {
            for (int y = 0; y < HEIGHT; y += 1) {
                world[x][y] = Tileset.NOTHING;
            }
        }

        // make rooms
        ArrayList<Room> roomsList = map.makeRooms(world, NROOM);

        //connect rooms
        map.connectRooms(world, roomsList);

        // build wall
        map.buildWall(world);

        // add door
        map.addDoor(world);

        // add players
        map.addPlayer(world, 1);

        // draws the world to the screen
        ter.renderFrame(world);
    }

}