package xyz.jmullin.prospector.bot;

import org.jetbrains.annotations.NotNull;
import xyz.jmullin.prospector.game.Coord;
import xyz.jmullin.prospector.game.Probe;
import xyz.jmullin.prospector.game.ProspectorBot;

import java.util.*;

public class StinkyPeteBot implements ProspectorBot {

  // Map to hold values for previously queried coordinates and the coords corresponding to those values
  // Keys are descending to ensure the first entry holds the highest previously queried value/coords
  private TreeMap<Integer, Deque<Coord>> queriedCoordsOrderedByValue;

  int gridDim = 8;
  int strideLength = 12;

  int lastPopValue;
  static LinkedList<OrderedDirection> cardinalDirections = new LinkedList<>();

  static OrderedDirection north = new OrderedDirection(CardinalDirection.NORTH);
  static OrderedDirection east = new OrderedDirection(CardinalDirection.EAST);
  static OrderedDirection south = new OrderedDirection(CardinalDirection.SOUTH);
  static OrderedDirection west = new OrderedDirection(CardinalDirection.WEST);
  OrderedDirection lastDirection;


  /**
   * Return the name of your bot.
   */
  @NotNull
  @Override
  public String getName() {
    return "StinkyPeteBot";
  }

  /**
   * Prospect the plot by calling .query(coord) on the provided probe instance with the desired
   * coordinate to query. Coordinates are in the range of 0 <= x < 512, 0 <= y < 512.
   * Each query will return the value of the plot at the given coordinate. A maximum of 100 queries
   * are allowed per plot; queries after this will return a value of 0.
   * After returning from this method, your score for the plot will be the value of the largest
   * query you made from the plot.
   */
  @Override
  public void prospect(@NotNull Probe probe) {
    queriedCoordsOrderedByValue = new TreeMap<>(Collections.reverseOrder());
    lastPopValue = 0;
    lastDirection = north;
    // Query initial distribution of coords in a grid sized gridDim x gridDim
    queryInitialGridCoords(probe);

    // Use remaining queries to search for higher values near those already sampled
    while (probe.getQueriesRemaining() > 0) {
      // Retrieve previously queried coord with highest value, query around it
      Map.Entry<Integer, Deque<Coord>> maxProspectValueCoords = queriedCoordsOrderedByValue.firstEntry();
      Coord walkCoord = maxProspectValueCoords.getValue().pop();

      // Save latest popped coords value for comparison with value of coords about to be queried around it
      lastPopValue = queriedCoordsOrderedByValue.firstEntry().getKey();

      // If there are no remaining mapped coords sharing the last popped value, remove map entry for that value
      if (maxProspectValueCoords.getValue().size() == 0) {
        queriedCoordsOrderedByValue.remove(queriedCoordsOrderedByValue.firstKey());
      }
      // Check points in cardinal directions around last popped;
      //  return as soon as one is found with higher value
      queryInPreferredCardinalDirections(walkCoord, probe);
    }
  }

  private void queryInitialGridCoords(@NotNull Probe probe) {
    for (int i = 0; i < gridDim; i++) {
      for (int j = 0; j < gridDim; j++) {
        int x = limitStrideToWithinPlotBounds((int) Math.ceil(i * 512 / (gridDim - 1)));
        int y = limitStrideToWithinPlotBounds((int) Math.ceil(j * 512 / (gridDim - 1)));
        queryAndTrackNewCoord(x, y, probe);
      }
    }
  }

  private int queryAndTrackNewCoord(int x, int y, Probe probe) {
    Integer value = 0;
    Coord coord = new Coord(x, y);
    if (!hasCoordAlreadyBeenQueried(coord, probe)) {
      value = probe.query(coord);
      addCoordToTrackingMap(value, coord);
    }
    return value;
  }

  private int limitStrideToWithinPlotBounds(int z) {
    if (z < 0) {
      return 0;
    } else if (z > 511) {
      return 511;
    } else {
      return z;
    }
  }


  // Choose first direction to step based upon last step taken in a positive direction
  private void queryInPreferredCardinalDirections(Coord startingCoord, Probe probe) {
    OrderedDirection directionToStep = lastDirection;
    OrderedDirection steppedDirection;
    int steppedValue;
    do {
      switch(directionToStep.direction) {
        case EAST:
          steppedDirection = east;
          steppedValue = stepEast(startingCoord, probe);
          break;
        case SOUTH:
          steppedDirection = south;
          steppedValue = stepSouth(startingCoord, probe);
          break;
        case WEST:
          steppedDirection = west;
          steppedValue = stepWest(startingCoord, probe);
          break;
        case NORTH:
        default:
          steppedDirection = north;
          steppedValue = stepNorth(startingCoord, probe);
          break;
      }

      if (steppedValue > lastPopValue) {
        lastDirection = steppedDirection;
        return;
      } else {
        directionToStep = directionToStep.nextDirection;
      }

    } while (directionToStep.direction != lastDirection.direction);

  }

  private int stepWest(Coord startingCoord, Probe probe) {
    return queryAndTrackNewCoord(limitStrideToWithinPlotBounds(startingCoord.getX() - strideLength),
        startingCoord.getY(),
        probe);
  }

  private int stepEast(Coord startingCoord, Probe probe) {
    return queryAndTrackNewCoord(limitStrideToWithinPlotBounds(startingCoord.getX() + strideLength),
        startingCoord.getY(),
        probe);
  }

  private int stepSouth(Coord startingCoord, Probe probe) {
    return queryAndTrackNewCoord(startingCoord.getX(),
        limitStrideToWithinPlotBounds(startingCoord.getY() - strideLength),
        probe);
  }

  private int stepNorth(Coord startingCoord, Probe probe) {
    return queryAndTrackNewCoord(startingCoord.getX(),
        limitStrideToWithinPlotBounds(startingCoord.getY() + strideLength),
        probe);
  }


  private boolean hasCoordAlreadyBeenQueried(Coord coord, Probe probe) {
    return probe.getQueryHistory().containsKey(coord);
  }

  private void addCoordToTrackingMap(Integer queryValue, Coord coord) {
    if (queriedCoordsOrderedByValue.containsKey(queryValue)) {
      queriedCoordsOrderedByValue.get(queryValue).add(coord);
    } else {
      //TreeSet<Coord> coordSet = new TreeSet<>();
      Deque<Coord> stack = new ArrayDeque<>();
      stack.add(coord);
      queriedCoordsOrderedByValue.put(queryValue, stack);
    }
  }

  private enum CardinalDirection {
    NORTH, EAST, SOUTH, WEST
  }

  private static class OrderedDirection {
    private CardinalDirection direction;
    private OrderedDirection nextDirection;

    public OrderedDirection(CardinalDirection direction) {
      this.direction = direction;
    }
  }

  static {
    north.nextDirection = east;
    east.nextDirection = south;
    south.nextDirection = west;
    west.nextDirection = north;
  }
}
